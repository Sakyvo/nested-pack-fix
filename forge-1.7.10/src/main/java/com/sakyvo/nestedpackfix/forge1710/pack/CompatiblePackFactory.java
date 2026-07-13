package com.sakyvo.nestedpackfix.forge1710.pack;

import com.sakyvo.nestedpackfix.NestedPackFixLog;
import com.sakyvo.nestedpackfix.archive.ArchiveLayout;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import net.minecraft.client.resources.FileResourcePack;

public final class CompatiblePackFactory {
    private CompatiblePackFactory() {
    }

    public static FileResourcePack create(File file) {
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".zip")) {
            return new FileResourcePack(file);
        }

        ArchiveLayout cached = null;
        try {
            cached = ArchiveRegistry.getCurrent(file);
        } catch (IOException failure) {
            NestedPackFixLog.errorOnce(
                "factory-cache:" + file.getAbsolutePath(),
                "Could not read cached archive state for " + file,
                failure
            );
        }
        return new CompatibleFileResourcePack(file, cached);
    }
}
