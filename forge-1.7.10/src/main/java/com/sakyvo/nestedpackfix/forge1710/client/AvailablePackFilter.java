package com.sakyvo.nestedpackfix.forge1710.client;

import java.util.List;

@SuppressWarnings({"rawtypes", "unchecked"})
final class AvailablePackFilter {
    private AvailablePackFilter() {
    }

    static void refresh(List source, List target, PackWarningKind filter) {
        target.clear();
        for (Object entry : source) {
            if (filter == PackWarningKind.NONE || warningKind(entry) == filter) {
                target.add(entry);
            }
        }
    }

    static boolean contains(List entries, PackWarningKind kind) {
        for (Object entry : entries) {
            if (warningKind(entry) == kind) {
                return true;
            }
        }
        return false;
    }

    private static PackWarningKind warningKind(Object entry) {
        return entry instanceof PackWarningClassifier
            ? ((PackWarningClassifier) entry).nestedpackfix$getWarningKind()
            : PackWarningKind.NONE;
    }
}
