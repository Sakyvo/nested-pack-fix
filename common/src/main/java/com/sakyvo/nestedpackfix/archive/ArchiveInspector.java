package com.sakyvo.nestedpackfix.archive;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ArchiveInspector {
    private static final String PACK_METADATA = "pack.mcmeta";

    private final ZipProbe zipProbe;

    public ArchiveInspector() {
        this(new DefaultZipProbe());
    }

    ArchiveInspector(ZipProbe zipProbe) {
        this.zipProbe = zipProbe;
    }

    public ArchiveLayout inspect(File file) throws IOException {
        ArchiveFingerprint fingerprint = ArchiveFingerprint.from(file);
        String extension = ArchiveCandidates.extension(file);

        if (!"zip".equals(extension)) {
            return ArchiveLayout.illegal(fingerprint);
        }

        return this.zipProbe.inspect(file, fingerprint);
    }

    interface ZipProbe {
        ArchiveLayout inspect(File file, ArchiveFingerprint fingerprint);
    }

    private static final class DefaultZipProbe implements ZipProbe {
        @Override
        public ArchiveLayout inspect(File file, ArchiveFingerprint fingerprint) {
            try {
                return inspectJava(file, fingerprint);
            }
            catch (IOException | IllegalArgumentException javaFailure) {
                try {
                    return inspectCommons(file, fingerprint);
                }
                catch (IOException | IllegalArgumentException commonsFailure) {
                    return ArchiveLayout.illegal(fingerprint);
                }
            }
        }

        private ArchiveLayout inspectJava(File file, ArchiveFingerprint fingerprint)
            throws IOException {
            ZipFile zipFile = new ZipFile(file);
            try {
                if (zipFile.getEntry(PACK_METADATA) != null) {
                    return ArchiveLayout.root(fingerprint, false);
                }

                PrefixAccumulator prefixes = new PrefixAccumulator();
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements() && !prefixes.isAmbiguous()) {
                    prefixes.accept(entries.nextElement().getName());
                }
                return prefixes.toLayout(fingerprint, false);
            }
            finally {
                zipFile.close();
            }
        }

        private ArchiveLayout inspectCommons(File file, ArchiveFingerprint fingerprint)
            throws IOException {
            org.apache.commons.compress.archivers.zip.ZipFile zipFile =
                new org.apache.commons.compress.archivers.zip.ZipFile(file, "UTF-8", false);
            try {
                if (zipFile.getEntry(PACK_METADATA) != null) {
                    return ArchiveLayout.root(fingerprint, true);
                }

                PrefixAccumulator prefixes = new PrefixAccumulator();
                Enumeration<org.apache.commons.compress.archivers.zip.ZipArchiveEntry> entries =
                    zipFile.getEntries();
                while (entries.hasMoreElements() && !prefixes.isAmbiguous()) {
                    org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry =
                        entries.nextElement();
                    prefixes.accept(entry.getName());
                }
                return prefixes.toLayout(fingerprint, true);
            }
            finally {
                zipFile.close();
            }
        }
    }

    private static final class PrefixAccumulator {
        private final Set<String> prefixes = new LinkedHashSet<String>();

        private void accept(String entryName) {
            String name = normalize(entryName);
            String prefix = directPackPrefix(name);
            if (prefix != null && !isIgnoredRoot(prefix)) {
                this.prefixes.add(prefix);
            }
        }

        private boolean isAmbiguous() {
            return this.prefixes.size() > 1;
        }

        private ArchiveLayout toLayout(
            ArchiveFingerprint fingerprint,
            boolean commonsZipRequired
        ) {
            return this.prefixes.size() == 1
                ? ArchiveLayout.prefix(
                    fingerprint,
                    this.prefixes.iterator().next(),
                    commonsZipRequired
                )
                : ArchiveLayout.illegal(fingerprint);
        }
    }

    private static String directPackPrefix(String name) {
        int slash = name.indexOf('/');
        if (slash <= 0 || !PACK_METADATA.equals(name.substring(slash + 1))) {
            return null;
        }
        return name.substring(0, slash) + '/';
    }

    private static boolean isIgnoredRoot(String prefix) {
        String root = prefix.substring(0, prefix.length() - 1).toLowerCase(Locale.ROOT);
        return "__macosx".equals(root)
            || ".ds_store".equals(root)
            || "thumbs.db".equals(root)
            || "desktop.ini".equals(root)
            || root.startsWith(".");
    }

    private static String normalize(String name) {
        String value = name.replace('\\', '/');
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        return value;
    }
}
