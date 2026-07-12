package com.sakyvo.nestedpackfix.archive;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class ArchiveLayoutCache {
    private final ArchiveInspector inspector;
    private final Map<String, ArchiveLayout> layoutsByPath = new HashMap<String, ArchiveLayout>();

    public ArchiveLayoutCache() {
        this(new ArchiveInspector());
    }

    ArchiveLayoutCache(ArchiveInspector inspector) {
        this.inspector = inspector;
    }

    public synchronized ArchiveLayout getOrInspect(File file) throws IOException {
        ArchiveFingerprint fingerprint = ArchiveFingerprint.from(file);
        ArchiveLayout cached = this.layoutsByPath.get(fingerprint.getCanonicalPath());
        if (cached != null && cached.getFingerprint().equals(fingerprint)) {
            return cached;
        }

        ArchiveLayout inspected = this.inspector.inspect(file);
        this.layoutsByPath.put(inspected.getFingerprint().getCanonicalPath(), inspected);
        return inspected;
    }

    public synchronized ArchiveLayout getIfCurrent(File file) throws IOException {
        ArchiveFingerprint fingerprint = ArchiveFingerprint.from(file);
        ArchiveLayout cached = this.layoutsByPath.get(fingerprint.getCanonicalPath());
        return cached != null && cached.getFingerprint().equals(fingerprint) ? cached : null;
    }

    public synchronized ArchiveLayout markIllegal(File file) throws IOException {
        ArchiveLayout illegal = ArchiveLayout.illegal(ArchiveFingerprint.from(file));
        this.layoutsByPath.put(illegal.getFingerprint().getCanonicalPath(), illegal);
        return illegal;
    }

    public synchronized void invalidate(File file) throws IOException {
        this.layoutsByPath.remove(ArchiveFingerprint.from(file).getCanonicalPath());
    }

    public synchronized void clear() {
        this.layoutsByPath.clear();
    }

    public synchronized int size() {
        return this.layoutsByPath.size();
    }
}
