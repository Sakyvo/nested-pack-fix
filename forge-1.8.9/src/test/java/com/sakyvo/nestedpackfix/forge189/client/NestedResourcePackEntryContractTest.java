package com.sakyvo.nestedpackfix.forge189.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.resources.ResourcePackListEntryFound;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NestedResourcePackEntryContractTest {
    @Test
    public void delegatesSelectionToVanillaPackFormatConfirmation() throws Exception {
        MethodNode method = findMethod("mousePressed", "(IIIIII)Z");

        assertEquals(1, countCalls(
            method,
            Opcodes.INVOKESPECIAL,
            ResourcePackListEntryFound.class.getName().replace('.', '/'),
            "mousePressed",
            "(IIIIII)Z"
        ));
    }

    @Test
    public void warningRendererReceivesRepositoryPackFormat() throws Exception {
        MethodNode method = findMethod("nestedpackfix$getPackFormat", "()I");

        assertEquals(1, countCalls(method, -1, null, "func_183019_a", "()I"));
    }

    private static MethodNode findMethod(String name, String descriptor) throws IOException {
        ClassNode classNode = new ClassNode();
        new ClassReader(readClass()).accept(classNode, 0);
        for (MethodNode method : classNode.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) {
                return method;
            }
        }
        throw new AssertionError("Missing method " + name + descriptor);
    }

    private static byte[] readClass() throws IOException {
        String resource = NestedResourcePackEntry.class.getName().replace('.', '/') + ".class";
        InputStream input = NestedResourcePackEntryContractTest.class
            .getClassLoader()
            .getResourceAsStream(resource);
        assertNotNull(resource, input);
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
            if (!(instruction instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode) instruction;
            if ((opcode < 0 || call.getOpcode() == opcode)
                && (owner == null || owner.equals(call.owner))
                && name.equals(call.name)
                && descriptor.equals(call.desc)) {
                ++count;
            }
        }
        return count;
    }
}
