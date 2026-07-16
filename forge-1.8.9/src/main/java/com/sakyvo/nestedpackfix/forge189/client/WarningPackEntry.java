package com.sakyvo.nestedpackfix.forge189.client;

import java.io.File;
import net.minecraft.client.Minecraft;

interface WarningPackEntry extends PackWarningClassifier {
    String nestedpackfix$getTooltip();

    File nestedpackfix$getFile();

    String nestedpackfix$getTitle();

    String nestedpackfix$getDescription();

    int nestedpackfix$getTitleColor();

    int nestedpackfix$getWarningColor();

    boolean nestedpackfix$drawIllegalBackground();

    int nestedpackfix$getPackFormat();

    Minecraft nestedpackfix$getMinecraft();

    ResourcePackScreenState nestedpackfix$getScreenState();

    void nestedpackfix$bindIcon();

    boolean nestedpackfix$isUsable();

    boolean nestedpackfix$canAdd();

    boolean nestedpackfix$canRemove();

    boolean nestedpackfix$canMoveUp();

    boolean nestedpackfix$canMoveDown();
}
