package com.sakyvo.nestedpackfix.archive;

import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class ArchiveLayoutTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void normalizesPrefixSeparators() throws Exception {
        File file = this.temporaryFolder.newFile("pack.zip");
        ArchiveLayout layout = ArchiveLayout.prefix(
            ArchiveFingerprint.from(file),
            "\\Pack\\",
            false
        );

        assertEquals(ArchiveKind.PREFIX, layout.getKind());
        assertEquals("Pack/", layout.getPrefix());
    }

    @Test
    public void illegalLayoutHasNoPrefixOrCommonsBackend() throws Exception {
        File file = this.temporaryFolder.newFile("pack.rar");
        ArchiveLayout layout = ArchiveLayout.illegal(ArchiveFingerprint.from(file));

        assertEquals(ArchiveKind.ILLEGAL, layout.getKind());
        assertNull(layout.getPrefix());
        assertFalse(layout.isCommonsZipRequired());
    }
}
