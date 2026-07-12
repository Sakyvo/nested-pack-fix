package com.sakyvo.nestedpackfix.archive;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ArchiveInspectorTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final ArchiveInspector inspector = new ArchiveInspector();

    @Test
    public void detectsRootPackWithoutPrefix() throws Exception {
        ArchiveLayout layout = this.inspector.inspect(zip(
            "root.zip",
            entries("pack.mcmeta", "assets/minecraft/textures/a.png")
        ));

        assertEquals(ArchiveKind.ROOT, layout.getKind());
        assertNull(layout.getPrefix());
        assertFalse(layout.isCommonsZipRequired());
    }

    @Test
    public void detectsExactlyOneTopLevelPackFolder() throws Exception {
        ArchiveLayout layout = this.inspector.inspect(zip(
            "prefix.zip",
            entries("CoolPack/pack.mcmeta", "CoolPack/assets/minecraft/textures/a.png")
        ));

        assertEquals(ArchiveKind.PREFIX, layout.getKind());
        assertEquals("CoolPack/", layout.getPrefix());
    }

    @Test
    public void ignoresRootContainerJunk() throws Exception {
        ArchiveLayout layout = this.inspector.inspect(zip(
            "junk.zip",
            entries(
                "README.txt",
                ".DS_Store/pack.mcmeta",
                "__MACOSX/pack.mcmeta",
                ".hidden/pack.mcmeta",
                "CoolPack/pack.mcmeta"
            )
        ));

        assertEquals(ArchiveKind.PREFIX, layout.getKind());
        assertEquals("CoolPack/", layout.getPrefix());
    }

    @Test
    public void rejectsMultipleTopLevelPackFolders() throws Exception {
        ArchiveLayout layout = this.inspector.inspect(zip(
            "multiple.zip",
            entries("PackA/pack.mcmeta", "PackB/pack.mcmeta")
        ));

        assertEquals(ArchiveKind.ILLEGAL, layout.getKind());
    }

    @Test
    public void rejectsDeepPackRoot() throws Exception {
        ArchiveLayout layout = this.inspector.inspect(zip(
            "deep.zip",
            entries("Outer/Inner/pack.mcmeta")
        ));

        assertEquals(ArchiveKind.ILLEGAL, layout.getKind());
    }

    @Test
    public void rejectsZipInsideZipWithoutOpeningInnerBytes() throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        entries.put("InnerPack.zip", new byte[] {0, 1, 2, 3});

        ArchiveLayout layout = this.inspector.inspect(zip("nested.zip", entries));

        assertEquals(ArchiveKind.ILLEGAL, layout.getKind());
    }

    @Test
    public void rejectsCorruptZip() throws Exception {
        File file = this.temporaryFolder.newFile("corrupt.zip");
        Files.write(file.toPath(), new byte[] {1, 2, 3, 4});

        assertEquals(ArchiveKind.ILLEGAL, this.inspector.inspect(file).getKind());
    }

    @Test
    public void usesCommonsOnlyWhenJavaRejectsMalformedEntryNames() throws Exception {
        File file = zip(
            "malformed.zip",
            entries("BrokenX/pack.mcmeta", "BrokenX/assets/minecraft/textures/a.png")
        );
        corruptPrefixMarker(file, "BrokenX/");

        ArchiveLayout layout = this.inspector.inspect(file);
        assertEquals(ArchiveKind.PREFIX, layout.getKind());
        assertTrue(layout.isCommonsZipRequired());
        assertTrue(layout.getPrefix().endsWith("/"));
    }

    @Test
    public void classifiesRarAnd7zWithoutCallingZipProbe() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        ArchiveInspector extensionOnlyInspector = new ArchiveInspector(
            new ArchiveInspector.ZipProbe() {
                @Override
                public ArchiveLayout inspect(File file, ArchiveFingerprint fingerprint) {
                    calls.incrementAndGet();
                    throw new AssertionError("ZIP probe must not be called for " + file);
                }
            }
        );
        File rar = arbitraryFile("pack.rar");
        File sevenZip = arbitraryFile("pack.7z");

        assertEquals(ArchiveKind.ILLEGAL, extensionOnlyInspector.inspect(rar).getKind());
        assertEquals(ArchiveKind.ILLEGAL, extensionOnlyInspector.inspect(sevenZip).getKind());
        assertEquals(0, calls.get());
    }

    private File arbitraryFile(String name) throws IOException {
        File file = this.temporaryFolder.newFile(name);
        Files.write(file.toPath(), new byte[] {7, 8, 9});
        return file;
    }

    private Map<String, byte[]> entries(String... names) {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        for (String name : names) {
            entries.put(name, new byte[] {1});
        }
        return entries;
    }

    private File zip(String name, Map<String, byte[]> entries) throws IOException {
        File file = this.temporaryFolder.newFile(name);
        ZipOutputStream output = new ZipOutputStream(new FileOutputStream(file));
        try {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue());
                output.closeEntry();
            }
        }
        finally {
            output.close();
        }
        return file;
    }

    private void corruptPrefixMarker(File file, String marker) throws IOException {
        byte[] data = Files.readAllBytes(file.toPath());
        byte[] needle = marker.getBytes(StandardCharsets.US_ASCII);

        for (int i = 0; i <= data.length - needle.length; ++i) {
            boolean match = true;
            for (int j = 0; j < needle.length; ++j) {
                if (data[i + j] != needle[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                data[i + marker.indexOf('X')] = (byte) 0xA7;
            }
        }

        Files.write(file.toPath(), data);
    }
}
