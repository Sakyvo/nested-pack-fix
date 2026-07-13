package com.sakyvo.nestedpackfix;

import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class NestedPackFixLog {
    private static final Logger LOGGER = LogManager.getLogger("NestedPackFix");
    private static final Set<String> REPORTED_ERRORS = new HashSet<String>();

    private NestedPackFixLog() {
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void debug(String message) {
        LOGGER.debug(message);
    }

    public static synchronized void errorOnce(String key, String message, Throwable failure) {
        if (REPORTED_ERRORS.add(key)) {
            LOGGER.error(message, failure);
        }
    }
}
