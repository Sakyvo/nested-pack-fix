package com.sakyvo.nestedpackfix.archive;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ArchiveCandidatesTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void listsSupportedArchivesOnlyAtRoot() throws Exception {
        File root = this.temporaryFolder.newFolder("resourcepacks");
        new File(root, "one.zip").createNewFile();
        new File(root, "two.ZIP").createNewFile();
        new File(root, "three.rar").createNewFile();
        new File(root, "four.7Z").createNewFile();
        new File(root, "notes.txt").createNewFile();
        new File(root, "folder.zip").mkdir();
        File nested = new File(root, "nested");
        nested.mkdir();
        new File(nested, "hidden.zip").createNewFile();

        List<File> candidates = ArchiveCandidates.list(root);
        Set<String> names = new HashSet<String>();
        for (File candidate : candidates) {
            names.add(candidate.getName());
        }

        assertEquals(4, names.size());
        assertTrue(names.contains("one.zip"));
        assertTrue(names.contains("two.ZIP"));
        assertTrue(names.contains("three.rar"));
        assertTrue(names.contains("four.7Z"));
    }

    @Test
    public void returnsEmptyForMissingDirectory() {
        assertTrue(ArchiveCandidates.list(new File(this.temporaryFolder.getRoot(), "missing")).isEmpty());
    }

    @Test
    public void resultCannotBeMutated() throws Exception {
        File root = this.temporaryFolder.newFolder("packs");
        new File(root, "pack.zip").createNewFile();
        List<File> candidates = ArchiveCandidates.list(root);

        try {
            candidates.clear();
            fail("candidate result must be immutable");
        }
        catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }
}
