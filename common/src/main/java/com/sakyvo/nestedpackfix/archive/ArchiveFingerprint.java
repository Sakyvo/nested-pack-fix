package com.sakyvo.nestedpackfix.archive;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public final class ArchiveFingerprint {
    private final String canonicalPath;
    private final long length;
    private final long lastModified;

    private ArchiveFingerprint(String canonicalPath, long length, long lastModified) {
        this.canonicalPath = canonicalPath;
        this.length = length;
        this.lastModified = lastModified;
    }

    public static ArchiveFingerprint from(File file) throws IOException {
        Objects.requireNonNull(file, "file");
        return new ArchiveFingerprint(file.getCanonicalPath(), file.length(), file.lastModified());
    }

    public String getCanonicalPath() {
        return this.canonicalPath;
    }

    public long getLength() {
        return this.length;
    }

    public long getLastModified() {
        return this.lastModified;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ArchiveFingerprint)) {
            return false;
        }
        ArchiveFingerprint that = (ArchiveFingerprint) other;
        return this.length == that.length
            && this.lastModified == that.lastModified
            && this.canonicalPath.equals(that.canonicalPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.canonicalPath, this.length, this.lastModified);
    }

    @Override
    public String toString() {
        return this.canonicalPath + ':' + this.length + ':' + this.lastModified;
    }
}
