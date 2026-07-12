package com.sakyvo.nestedpackfix.archive;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ArchiveCandidates {
    private ArchiveCandidates() {
    }

    public static List<File> list(File directory) {
        if (directory == null || !directory.isDirectory()) {
            return Collections.emptyList();
        }

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }

        List<File> candidates = new ArrayList<File>();
        for (File file : files) {
            if (isCandidate(file)) {
                candidates.add(file);
            }
        }
        return Collections.unmodifiableList(candidates);
    }

    public static boolean isCandidate(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        String extension = extension(file);
        return "zip".equals(extension) || "rar".equals(extension) || "7z".equals(extension);
    }

    static String extension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot < 0 || dot == name.length() - 1
            ? ""
            : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
