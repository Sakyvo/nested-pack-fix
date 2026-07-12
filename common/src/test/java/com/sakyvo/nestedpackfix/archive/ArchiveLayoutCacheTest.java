package com.sakyvo.nestedpackfix.archive;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ArchiveLayoutCacheTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void reusesLayoutForUnchangedFingerprint() throws Exception {
        File file = zip("root.zip", "pack.mcmeta");
        ArchiveLayoutCache cache = new ArchiveLayoutCache();

        ArchiveLayout first = cache.getOrInspect(file);
        ArchiveLayout second = cache.getOrInspect(file);

        assertSame(first, second);
        assertEquals(1, cache.size());
    }

    @Test
    public void invalidatesWhenFileLengthChanges() throws Exception {
        File file = this.temporaryFolder.newFile("pack.rar");
        Files.write(file.toPath(), new byte[] {1});
        ArchiveLayoutCache cache = new ArchiveLayoutCache();
        ArchiveLayout first = cache.getOrInspect(file);

        Files.write(file.toPath(), new byte[] {1, 2, 3, 4});
        ArchiveLayout second = cache.getOrInspect(file);

        assertNotSame(first, second);
        assertNotEquals(first.getFingerprint(), second.getFingerprint());
        assertEquals(1, cache.size());
    }

    @Test
    public void explicitIllegalMarkerReplacesCurrentLayout() throws Exception {
        File file = zip("pack.zip", "pack.mcmeta");
        ArchiveLayoutCache cache = new ArchiveLayoutCache();
        assertEquals(ArchiveKind.ROOT, cache.getOrInspect(file).getKind());

        ArchiveLayout illegal = cache.markIllegal(file);

        assertEquals(ArchiveKind.ILLEGAL, illegal.getKind());
        assertSame(illegal, cache.getIfCurrent(file));
    }

    @Test
    public void invalidateAndClearRemoveEntries() throws Exception {
        File first = zip("first.zip", "pack.mcmeta");
        File second = zip("second.zip", "Pack/pack.mcmeta");
        ArchiveLayoutCache cache = new ArchiveLayoutCache();
        cache.getOrInspect(first);
        cache.getOrInspect(second);

        cache.invalidate(first);
        assertNull(cache.getIfCurrent(first));
        assertEquals(1, cache.size());

        cache.clear();
        assertEquals(0, cache.size());
    }

    private File zip(String name, String entryName) throws Exception {
        File file = this.temporaryFolder.newFile(name);
        ZipOutputStream output = new ZipOutputStream(new FileOutputStream(file));
        try {
            output.putNextEntry(new ZipEntry(entryName));
            output.write(1);
            output.closeEntry();
        }
        finally {
            output.close();
        }
        return file;
    }
}
