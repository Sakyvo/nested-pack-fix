package com.sakyvo.nestedpackfix.forge1710.pack;

import com.sakyvo.nestedpackfix.NestedPackFixLog;
import com.sakyvo.nestedpackfix.archive.ArchiveCandidates;
import com.sakyvo.nestedpackfix.archive.ArchiveKind;
import com.sakyvo.nestedpackfix.archive.ArchiveLayout;
import com.sakyvo.nestedpackfix.archive.ArchiveLayoutCache;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ArchiveRegistry {
    private static final ArchiveLayoutCache CACHE = new ArchiveLayoutCache();

    private ArchiveRegistry() {
    }

    public static ArchiveLayout inspect(File file) throws IOException {
        return CACHE.getOrInspect(file);
    }

    public static ArchiveLayout markIllegal(File file) throws IOException {
        return CACHE.markIllegal(file);
    }

    public static ArchiveLayout getCurrent(File file) throws IOException {
        return CACHE.getIfCurrent(file);
    }

    public static List<File> findIllegalFiles(File directory) {
        List<File> illegal = new ArrayList<File>();
        for (File file : ArchiveCandidates.list(directory)) {
            try {
                ArchiveLayout layout = isOpaqueArchive(file)
                    ? inspect(file)
                    : getCurrent(file);
                if (layout != null && layout.getKind() == ArchiveKind.ILLEGAL) {
                    illegal.add(file);
                }
            } catch (IOException failure) {
                NestedPackFixLog.errorOnce(
                    "archive-registry:" + file.getAbsolutePath(),
                    "Could not read resource-pack archive fingerprint: " + file,
                    failure
                );
            }
        }
        return Collections.unmodifiableList(illegal);
    }

    private static boolean isOpaqueArchive(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".rar") || name.endsWith(".7z");
    }
}
