package com.sakyvo.nestedpackfix.forge1710.pack;

import com.sakyvo.nestedpackfix.archive.ArchiveKind;
import com.sakyvo.nestedpackfix.archive.ArchiveLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.client.resources.data.PackMetadataSection;
import net.minecraft.client.resources.data.PackMetadataSectionSerializer;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.util.ResourceLocation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CompatibleFileResourcePackTest {
    private static final byte[] METADATA = (
        "{\"pack\":{\"pack_format\":1,\"description\":\"NestedPackFix test\"}}"
    ).getBytes(StandardCharsets.UTF_8);

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void rootPackUsesVanillaPathWithoutInspectorCache() throws Exception {
        File file = this.writeZip("root.zip", entries(
            "pack.mcmeta", METADATA,
            "assets/minecraft/textures/test.txt", bytes("root")
        ));
        CompatibleFileResourcePack pack =
            (CompatibleFileResourcePack) CompatiblePackFactory.create(file);

        IMetadataSection metadata = pack.getPackMetadata(serializer(), "pack");

        assertTrue(metadata instanceof PackMetadataSection);
        assertFalse(pack.isNested());
        assertNull(ArchiveRegistry.getCurrent(file));
        assertEquals("root", read(pack.getInputStream(
            new ResourceLocation("minecraft", "textures/test.txt")
        )));
        assertTrue(pack.getResourceDomains().contains("minecraft"));
        pack.close();
    }

    @Test
    public void prefixPackRemapsMetadataAssetsAndReopensAfterClose() throws Exception {
        File file = this.writeZip("nested.zip", entries(
            "Wrapped Pack/pack.mcmeta", METADATA,
            "Wrapped Pack/pack.png", packImage(),
            "Wrapped Pack/assets/minecraft/textures/test.txt", bytes("nested")
        ));
        CompatibleFileResourcePack pack =
            (CompatibleFileResourcePack) CompatiblePackFactory.create(file);

        assertTrue(pack.getPackMetadata(serializer(), "pack") instanceof PackMetadataSection);
        assertTrue(pack.isNested());
        assertEquals(file.getName(), pack.getPackName());
        assertEquals("nested", read(pack.getInputStream(
            new ResourceLocation("minecraft", "textures/test.txt")
        )));
        assertEquals(2, pack.getPackImage().getWidth());
        assertEquals(2, pack.getPackImage().getHeight());
        assertTrue(pack.getResourceDomains().contains("minecraft"));
        assertEquals(ArchiveKind.PREFIX, ArchiveRegistry.getCurrent(file).getKind());

        pack.close();
        CompatibleFileResourcePack reopened =
            (CompatibleFileResourcePack) CompatiblePackFactory.create(file);
        assertTrue(reopened.isNested());
        assertTrue(reopened.getPackMetadata(serializer(), "pack") instanceof PackMetadataSection);
        assertEquals("nested", read(reopened.getInputStream(
            new ResourceLocation("minecraft", "textures/test.txt")
        )));
        reopened.close();
    }

    @Test
    public void commonsPrefixRemapsMetadataAndResources() throws Exception {
        File file = this.writeZip("commons-nested.zip", entries(
            "BrokenX/pack.mcmeta", METADATA,
            "BrokenX/assets/minecraft/textures/test.txt", bytes("commons")
        ));
        corruptPrefixMarker(file, "BrokenX/");
        CompatibleFileResourcePack pack =
            (CompatibleFileResourcePack) CompatiblePackFactory.create(file);

        assertTrue(pack.getPackMetadata(serializer(), "pack") instanceof PackMetadataSection);
        assertTrue(pack.isNested());
        assertEquals("commons", read(pack.getInputStream(
            new ResourceLocation("minecraft", "textures/test.txt")
        )));
        assertTrue(ArchiveRegistry.getCurrent(file).isCommonsZipRequired());
        pack.close();
    }

    @Test
    public void missingPackRootIsRecordedAsIllegal() throws Exception {
        File file = this.writeZip("missing.zip", entries(
            "readme.txt", bytes("not a pack")
        ));
        CompatibleFileResourcePack pack =
            (CompatibleFileResourcePack) CompatiblePackFactory.create(file);

        expectMetadataFailure(pack);

        assertEquals(ArchiveKind.ILLEGAL, ArchiveRegistry.getCurrent(file).getKind());
        assertArchiveHandlesClosed(pack);
        pack.close();

        CompatibleFileResourcePack cached =
            (CompatibleFileResourcePack) CompatiblePackFactory.create(file);
        assertEquals(
            ArchiveKind.ILLEGAL,
            ((ArchiveLayout) readField(
                CompatibleFileResourcePack.class,
                cached,
                "layout"
            )).getKind()
        );
        expectMetadataFailure(cached);
        assertArchiveHandlesClosed(cached);
    }

    @Test
    public void invalidMetadataIsRecordedAsIllegal() throws Exception {
        File file = this.writeZip("invalid-metadata.zip", entries(
            "pack.mcmeta", bytes("not json")
        ));
        CompatibleFileResourcePack pack =
            (CompatibleFileResourcePack) CompatiblePackFactory.create(file);

        expectMetadataFailure(pack);

        assertEquals(ArchiveKind.ILLEGAL, ArchiveRegistry.getCurrent(file).getKind());
        assertArchiveHandlesClosed(pack);
        pack.close();
    }

    @Test
    public void corruptZipIsRecordedAsIllegal() throws Exception {
        File file = this.temporaryFolder.newFile("corrupt.zip");
        Files.write(file.toPath(), bytes("not a zip"));
        CompatibleFileResourcePack pack =
            (CompatibleFileResourcePack) CompatiblePackFactory.create(file);

        expectMetadataFailure(pack);

        assertEquals(ArchiveKind.ILLEGAL, ArchiveRegistry.getCurrent(file).getKind());
        assertArchiveHandlesClosed(pack);
        pack.close();
    }

    @Test
    public void opaqueArchivesAreIllegalWithoutZipInspection() throws Exception {
        File rar = this.temporaryFolder.newFile("pack.rar");
        File sevenZip = this.temporaryFolder.newFile("pack.7z");
        Files.write(rar.toPath(), bytes("opaque rar data"));
        Files.write(sevenZip.toPath(), bytes("opaque 7z data"));

        List<File> illegal = ArchiveRegistry.findIllegalFiles(
            this.temporaryFolder.getRoot()
        );

        assertTrue(illegal.contains(rar));
        assertTrue(illegal.contains(sevenZip));
        for (File file : illegal) {
            ArchiveLayout layout = ArchiveRegistry.getCurrent(file);
            assertEquals(ArchiveKind.ILLEGAL, layout.getKind());
        }
    }

    private static void expectMetadataFailure(CompatibleFileResourcePack pack) {
        try {
            pack.getPackMetadata(serializer(), "pack");
            fail("Expected metadata loading to fail");
        } catch (IOException | RuntimeException expected) {
        }
    }

    private File writeZip(String name, Map<String, byte[]> entries) throws IOException {
        File file = new File(this.temporaryFolder.getRoot(), name);
        ZipOutputStream output = new ZipOutputStream(new FileOutputStream(file));
        try {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue());
                output.closeEntry();
            }
        } finally {
            output.close();
        }
        return file;
    }

    private static Map<String, byte[]> entries(Object... values) {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        for (int index = 0; index < values.length; index += 2) {
            entries.put((String) values[index], (byte[]) values[index + 1]);
        }
        return entries;
    }

    private static IMetadataSerializer serializer() {
        IMetadataSerializer serializer = new IMetadataSerializer();
        serializer.registerMetadataSectionType(
            new PackMetadataSectionSerializer(),
            PackMetadataSection.class
        );
        return serializer;
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static void assertArchiveHandlesClosed(CompatibleFileResourcePack pack)
        throws ReflectiveOperationException {
        assertNull(readField(FileResourcePack.class, pack, "resourcePackZipFile"));
        assertNull(readField(
            CompatibleFileResourcePack.class,
            pack,
            "compatibleZipFile"
        ));
        assertNull(readField(
            CompatibleFileResourcePack.class,
            pack,
            "commonsZipFile"
        ));
    }

    private static Object readField(Class<?> owner, Object target, String name)
        throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static byte[] packImage() throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFFFF5555);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static void corruptPrefixMarker(File file, String marker) throws IOException {
        byte[] data = Files.readAllBytes(file.toPath());
        byte[] needle = marker.getBytes(StandardCharsets.US_ASCII);
        for (int index = 0; index <= data.length - needle.length; ++index) {
            boolean match = true;
            for (int offset = 0; offset < needle.length; ++offset) {
                if (data[index + offset] != needle[offset]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                data[index + marker.indexOf('X')] = (byte) 0xA7;
            }
        }
        Files.write(file.toPath(), data);
    }

    private static String read(InputStream input) throws IOException {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[256];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            input.close();
        }
    }
}
