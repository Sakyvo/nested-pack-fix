package com.sakyvo.nestedpackfix.forge1710.core;

import com.sakyvo.nestedpackfix.NestedPackFixLog;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class NestedPackFixTransformer implements IClassTransformer {
    static final String TARGET =
        "net/minecraft/client/resources/ResourcePackRepository$Entry";
    static final String FILE_RESOURCE_PACK =
        "net/minecraft/client/resources/FileResourcePack";
    static final String FACTORY =
        "com/sakyvo/nestedpackfix/forge1710/pack/CompatiblePackFactory";

    private static final String UPDATE_DESCRIPTOR = "()V";
    private static final String FILE_CONSTRUCTOR_DESCRIPTOR = "(Ljava/io/File;)V";
    private static final String FACTORY_DESCRIPTOR =
        "(Ljava/io/File;)Lnet/minecraft/client/resources/FileResourcePack;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !isTarget(name, transformedName)) {
            return basicClass;
        }

        try {
            ClassNode classNode = new ClassNode();
            new ClassReader(basicClass).accept(classNode, 0);
            transformEntry(classNode);

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            classNode.accept(writer);
            NestedPackFixLog.debug("Applied resource-pack factory transformation");
            return writer.toByteArray();
        } catch (Throwable failure) {
            NestedPackFixLog.errorOnce(
                "transform:resource-pack-entry",
                "NestedPackFix left ResourcePackRepository.Entry unchanged",
                failure
            );
            return basicClass;
        }
    }

    private static boolean isTarget(String name, String transformedName) {
        String targetName = TARGET.replace('/', '.');
        return targetName.equals(normalize(transformedName))
            || targetName.equals(normalize(name));
    }

    private static String normalize(String name) {
        return name == null ? null : name.replace('/', '.');
    }

    private static void transformEntry(ClassNode classNode) {
        require(TARGET.equals(classNode.name), "unexpected class name " + classNode.name);
        MethodNode update = findUpdateMethod(classNode);
        ConstructorCall constructor = findFileResourcePackConstructor(update, classNode.name);

        update.instructions.remove(constructor.newInstruction);
        update.instructions.remove(constructor.duplicateInstruction);
        update.instructions.set(
            constructor.constructorCall,
            new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                FACTORY,
                "create",
                FACTORY_DESCRIPTOR,
                false
            )
        );
    }

    private static MethodNode findUpdateMethod(ClassNode classNode) {
        List<MethodNode> matches = new ArrayList<MethodNode>();
        for (MethodNode method : classNode.methods) {
            if (UPDATE_DESCRIPTOR.equals(method.desc)
                && ("updateResourcePack".equals(method.name)
                    || "func_110516_a".equals(method.name))) {
                matches.add(method);
            }
        }
        require(matches.size() == 1, "expected one updateResourcePack method");
        return matches.get(0);
    }

    private static ConstructorCall findFileResourcePackConstructor(
        MethodNode method,
        String entryOwner
    ) {
        List<ConstructorCall> matches = new ArrayList<ConstructorCall>();
        for (AbstractInsnNode instruction = method.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {
            if (!(instruction instanceof MethodInsnNode)) {
                continue;
            }

            MethodInsnNode call = (MethodInsnNode) instruction;
            if (call.getOpcode() != Opcodes.INVOKESPECIAL
                || !FILE_RESOURCE_PACK.equals(call.owner)
                || !"<init>".equals(call.name)
                || !FILE_CONSTRUCTOR_DESCRIPTOR.equals(call.desc)) {
                continue;
            }

            AbstractInsnNode fileReadInstruction = previousCode(call);
            AbstractInsnNode loadThisInstruction = previousCode(fileReadInstruction);
            AbstractInsnNode duplicateInstruction = previousCode(loadThisInstruction);
            AbstractInsnNode newInstruction = previousCode(duplicateInstruction);

            require(fileReadInstruction instanceof FieldInsnNode, "pack file read is missing");
            require(loadThisInstruction instanceof VarInsnNode, "entry load is missing");
            require(duplicateInstruction instanceof InsnNode, "constructor DUP is missing");
            require(newInstruction instanceof TypeInsnNode, "constructor NEW is missing");

            FieldInsnNode fileRead = (FieldInsnNode) fileReadInstruction;
            VarInsnNode loadThis = (VarInsnNode) loadThisInstruction;
            TypeInsnNode newType = (TypeInsnNode) newInstruction;
            require(fileRead.getOpcode() == Opcodes.GETFIELD, "pack file is not read with GETFIELD");
            require(entryOwner.equals(fileRead.owner), "pack file owner changed");
            require("Ljava/io/File;".equals(fileRead.desc), "pack file descriptor changed");
            require(
                "resourcePackFile".equals(fileRead.name)
                    || "field_110523_b".equals(fileRead.name),
                "pack file field name changed"
            );
            require(loadThis.getOpcode() == Opcodes.ALOAD && loadThis.var == 0,
                "unexpected Entry local");
            require(duplicateInstruction.getOpcode() == Opcodes.DUP, "constructor DUP changed");
            require(newInstruction.getOpcode() == Opcodes.NEW, "constructor NEW changed");
            require(FILE_RESOURCE_PACK.equals(newType.desc), "constructor type changed");
            matches.add(new ConstructorCall(newType, duplicateInstruction, call));
        }

        require(matches.size() == 1, "expected one FileResourcePack constructor");
        return matches.get(0);
    }

    private static AbstractInsnNode previousCode(AbstractInsnNode instruction) {
        AbstractInsnNode current = instruction == null ? null : instruction.getPrevious();
        while (current != null && current.getOpcode() < 0) {
            current = current.getPrevious();
        }
        return current;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new TransformException(message);
        }
    }

    private static final class ConstructorCall {
        private final TypeInsnNode newInstruction;
        private final AbstractInsnNode duplicateInstruction;
        private final MethodInsnNode constructorCall;

        private ConstructorCall(
            TypeInsnNode newInstruction,
            AbstractInsnNode duplicateInstruction,
            MethodInsnNode constructorCall
        ) {
            this.newInstruction = newInstruction;
            this.duplicateInstruction = duplicateInstruction;
            this.constructorCall = constructorCall;
        }
    }

    private static final class TransformException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private TransformException(String message) {
            super(message);
        }
    }
}
