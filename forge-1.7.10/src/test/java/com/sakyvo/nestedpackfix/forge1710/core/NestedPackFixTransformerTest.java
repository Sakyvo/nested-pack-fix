package com.sakyvo.nestedpackfix.forge1710.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.CheckClassAdapter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class NestedPackFixTransformerTest {
    private static final String FACTORY_DESCRIPTOR =
        "(Ljava/io/File;)Lnet/minecraft/client/resources/FileResourcePack;";

    private final NestedPackFixTransformer transformer = new NestedPackFixTransformer();

    @Test
    public void transformsRealForgeBinEntry() throws Exception {
        byte[] original = readClass(NestedPackFixTransformer.TARGET);
        byte[] transformed = transform(original);

        assertNotSame(original, transformed);
        MethodNode update = findUpdate(readNode(transformed));
        assertEquals(1, countCalls(
            update,
            Opcodes.INVOKESTATIC,
            NestedPackFixTransformer.FACTORY,
            "create",
            FACTORY_DESCRIPTOR
        ));
        assertEquals(0, countCalls(
            update,
            Opcodes.INVOKESPECIAL,
            NestedPackFixTransformer.FILE_RESOURCE_PACK,
            "<init>",
            "(Ljava/io/File;)V"
        ));
        assertEquals(1, countCalls(
            update,
            Opcodes.INVOKESPECIAL,
            "net/minecraft/client/resources/FolderResourcePack",
            "<init>",
            "(Ljava/io/File;)V"
        ));
        verifyClass(transformed);
    }

    @Test
    public void transformsSrgNamedEntryFixture() throws Exception {
        ClassNode fixture = readNode(readClass(NestedPackFixTransformer.TARGET));
        for (FieldNode field : fixture.fields) {
            if ("resourcePackFile".equals(field.name)) {
                field.name = "field_110523_b";
            }
        }
        for (MethodNode method : fixture.methods) {
            if ("updateResourcePack".equals(method.name)) {
                method.name = "func_110516_a";
            }
            for (AbstractInsnNode instruction = method.instructions.getFirst();
                 instruction != null;
                 instruction = instruction.getNext()) {
                if (instruction instanceof FieldInsnNode
                    && NestedPackFixTransformer.TARGET.equals(
                        ((FieldInsnNode) instruction).owner
                    )
                    && "resourcePackFile".equals(((FieldInsnNode) instruction).name)) {
                    ((FieldInsnNode) instruction).name = "field_110523_b";
                }
            }
        }
        byte[] original = writeNode(fixture);
        byte[] transformed = transform(original);

        assertNotSame(original, transformed);
        assertEquals(1, countCalls(
            findUpdate(readNode(transformed)),
            Opcodes.INVOKESTATIC,
            NestedPackFixTransformer.FACTORY,
            "create",
            FACTORY_DESCRIPTOR
        ));
        verifyClass(transformed);
    }

    @Test
    public void returnsOriginalBytesWhenConstructorMatchIsDuplicated() throws Exception {
        ClassNode fixture = readNode(readClass(NestedPackFixTransformer.TARGET));
        MethodNode update = findUpdate(fixture);
        InsnList duplicate = new InsnList();
        duplicate.add(new TypeInsnNode(
            Opcodes.NEW,
            NestedPackFixTransformer.FILE_RESOURCE_PACK
        ));
        duplicate.add(new InsnNode(Opcodes.DUP));
        duplicate.add(new VarInsnNode(Opcodes.ALOAD, 0));
        duplicate.add(new FieldInsnNode(
            Opcodes.GETFIELD,
            NestedPackFixTransformer.TARGET,
            "resourcePackFile",
            "Ljava/io/File;"
        ));
        duplicate.add(new MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            NestedPackFixTransformer.FILE_RESOURCE_PACK,
            "<init>",
            "(Ljava/io/File;)V",
            false
        ));
        duplicate.add(new InsnNode(Opcodes.POP));
        update.instructions.insert(duplicate);
        byte[] original = writeNode(fixture);

        byte[] result = transform(original);

        assertSame(original, result);
        assertArrayEquals(original, result);
    }

    @Test
    public void returnsOriginalBytesWhenPackFileShapeChanges() throws Exception {
        ClassNode fixture = readNode(readClass(NestedPackFixTransformer.TARGET));
        MethodNode update = findUpdate(fixture);
        for (AbstractInsnNode instruction = update.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof FieldInsnNode
                && "resourcePackFile".equals(((FieldInsnNode) instruction).name)) {
                ((FieldInsnNode) instruction).name = "changedField";
            }
        }
        byte[] original = writeNode(fixture);

        byte[] result = transform(original);

        assertSame(original, result);
        assertArrayEquals(original, result);
    }

    @Test
    public void ignoresUnrelatedClassesAndNullBytes() {
        byte[] bytes = new byte[] {1, 2, 3};
        assertSame(bytes, this.transformer.transform("example.Other", "example.Other", bytes));
        assertEquals(null, this.transformer.transform("example.Other", "example.Other", null));
    }

    private byte[] transform(byte[] bytes) {
        String className = NestedPackFixTransformer.TARGET.replace('/', '.');
        return this.transformer.transform(className, className, bytes);
    }

    private static byte[] readClass(String internalName) throws IOException {
        InputStream input = NestedPackFixTransformerTest.class.getClassLoader()
            .getResourceAsStream(internalName + ".class");
        assertNotNull("Missing ForgeBin class " + internalName, input);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        } finally {
            input.close();
        }
    }

    private static ClassNode readNode(byte[] bytes) {
        ClassNode classNode = new ClassNode();
        new ClassReader(bytes).accept(classNode, 0);
        return classNode;
    }

    private static byte[] writeNode(ClassNode classNode) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static MethodNode findUpdate(ClassNode classNode) {
        List<MethodNode> matches = new ArrayList<MethodNode>();
        for (MethodNode method : classNode.methods) {
            if ("()V".equals(method.desc)
                && ("updateResourcePack".equals(method.name)
                    || "func_110516_a".equals(method.name))) {
                matches.add(method);
            }
        }
        assertEquals(1, matches.size());
        return matches.get(0);
    }

    private static int countCalls(
        MethodNode method,
        int opcode,
        String owner,
        String name,
        String descriptor
    ) {
        int count = 0;
        for (AbstractInsnNode instruction = method.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.getOpcode() == opcode
                    && owner.equals(call.owner)
                    && name.equals(call.name)
                    && descriptor.equals(call.desc)) {
                    ++count;
                }
            }
        }
        return count;
    }

    private static void verifyClass(byte[] bytes) {
        StringWriter output = new StringWriter();
        CheckClassAdapter.verify(
            new ClassReader(bytes),
            NestedPackFixTransformerTest.class.getClassLoader(),
            false,
            new PrintWriter(output)
        );
        assertEquals(output.toString(), "", output.toString());
    }
}
