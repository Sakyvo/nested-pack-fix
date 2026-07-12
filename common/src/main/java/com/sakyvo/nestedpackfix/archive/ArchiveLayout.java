package com.sakyvo.nestedpackfix.archive;

import java.util.Objects;

public final class ArchiveLayout {
    private final ArchiveKind kind;
    private final String prefix;
    private final boolean commonsZipRequired;
    private final ArchiveFingerprint fingerprint;

    private ArchiveLayout(
        ArchiveKind kind,
        String prefix,
        boolean commonsZipRequired,
        ArchiveFingerprint fingerprint
    ) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.prefix = prefix;
        this.commonsZipRequired = commonsZipRequired;
        this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
    }

    public static ArchiveLayout root(
        ArchiveFingerprint fingerprint,
        boolean commonsZipRequired
    ) {
        return new ArchiveLayout(ArchiveKind.ROOT, null, commonsZipRequired, fingerprint);
    }

    public static ArchiveLayout prefix(
        ArchiveFingerprint fingerprint,
        String prefix,
        boolean commonsZipRequired
    ) {
        return new ArchiveLayout(
            ArchiveKind.PREFIX,
            normalizePrefix(prefix),
            commonsZipRequired,
            fingerprint
        );
    }

    public static ArchiveLayout illegal(ArchiveFingerprint fingerprint) {
        return new ArchiveLayout(ArchiveKind.ILLEGAL, null, false, fingerprint);
    }

    private static String normalizePrefix(String prefix) {
        String value = Objects.requireNonNull(prefix, "prefix").replace('\\', '/');
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.length() == 0) {
            throw new IllegalArgumentException("prefix must not be empty");
        }
        return value.endsWith("/") ? value : value + '/';
    }

    public ArchiveKind getKind() {
        return this.kind;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public boolean isCommonsZipRequired() {
        return this.commonsZipRequired;
    }

    public ArchiveFingerprint getFingerprint() {
        return this.fingerprint;
    }
}
