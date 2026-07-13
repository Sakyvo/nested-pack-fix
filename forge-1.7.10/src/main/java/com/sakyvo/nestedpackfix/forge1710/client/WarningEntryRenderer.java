package com.sakyvo.nestedpackfix.forge1710.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@SuppressWarnings("rawtypes")
final class WarningEntryRenderer {
    private static final ResourceLocation RESOURCE_PACK_TEXTURE =
        new ResourceLocation("textures/gui/resource_packs.png");
    private static final String WARNING = "[!]";
    private static final int PACK_ICON_SIZE = 32;
    private static final int TEXT_GAP = 2;
    private static final int WARNING_GAP = 4;
    private static final int VANILLA_TEXT_WIDTH = 157;

    private WarningEntryRenderer() {
    }

    static void draw(
        WarningPackEntry entry,
        int index,
        int x,
        int y,
        int width,
        int height,
        Tessellator tessellator,
        int mouseX,
        int mouseY,
        boolean hovered
    ) {
        Minecraft minecraft = entry.nestedpackfix$getMinecraft();
        if (entry.nestedpackfix$drawIllegalBackground()) {
            Gui.drawRect(x, y - 1, x + width - 8, y + 33, 0x55FF0000);
        }

        entry.nestedpackfix$bindIcon();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawModalRectWithCustomSizedTexture(
            x,
            y,
            0.0F,
            0.0F,
            PACK_ICON_SIZE,
            PACK_ICON_SIZE,
            32.0F,
            32.0F
        );
        drawMovementControls(entry, x, y, mouseX, mouseY, hovered);

        int warningX = x + PACK_ICON_SIZE + TEXT_GAP;
        int warningWidth = minecraft.fontRendererObj.getStringWidth(WARNING);
        minecraft.fontRendererObj.drawStringWithShadow(
            WARNING,
            warningX,
            y + 1,
            entry.nestedpackfix$getWarningColor()
        );

        int textX = warningX + warningWidth + WARNING_GAP;
        int textWidth = VANILLA_TEXT_WIDTH - warningWidth - WARNING_GAP;
        String title = trimTitle(minecraft, entry.nestedpackfix$getTitle(), textWidth);
        minecraft.fontRendererObj.drawStringWithShadow(
            title,
            textX,
            y + 1,
            entry.nestedpackfix$getTitleColor()
        );

        List lines = minecraft.fontRendererObj.listFormattedStringToWidth(
            entry.nestedpackfix$getDescription(),
            textWidth
        );
        for (int line = 0; line < 2 && line < lines.size(); ++line) {
            minecraft.fontRendererObj.drawStringWithShadow(
                (String) lines.get(line),
                textX,
                y + 12 + 10 * line,
                8421504
            );
        }

        entry.nestedpackfix$getScreenState().recordWarning(
            entry,
            warningX,
            y,
            warningWidth,
            16
        );
    }

    static boolean isWarningHit(Minecraft minecraft, int relativeX, int relativeY) {
        int warningX = PACK_ICON_SIZE + TEXT_GAP;
        int warningWidth = minecraft.fontRendererObj.getStringWidth(WARNING);
        return relativeX >= warningX
            && relativeX < warningX + warningWidth
            && relativeY >= 0
            && relativeY < 16;
    }

    private static String trimTitle(Minecraft minecraft, String title, int width) {
        if (minecraft.fontRendererObj.getStringWidth(title) <= width) {
            return title;
        }
        int ellipsisWidth = minecraft.fontRendererObj.getStringWidth("...");
        return minecraft.fontRendererObj.trimStringToWidth(title, width - ellipsisWidth) + "...";
    }

    private static void drawMovementControls(
        WarningPackEntry entry,
        int x,
        int y,
        int mouseX,
        int mouseY,
        boolean hovered
    ) {
        Minecraft minecraft = entry.nestedpackfix$getMinecraft();
        if ((!minecraft.gameSettings.touchscreen && !hovered) || !entry.nestedpackfix$isUsable()) {
            return;
        }

        minecraft.getTextureManager().bindTexture(RESOURCE_PACK_TEXTURE);
        Gui.drawRect(x, y, x + 32, y + 32, -1601138544);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        int relativeX = mouseX - x;
        int relativeY = mouseY - y;

        if (entry.nestedpackfix$canAdd()) {
            Gui.drawModalRectWithCustomSizedTexture(
                x,
                y,
                0.0F,
                relativeX < 32 ? 32.0F : 0.0F,
                32,
                32,
                256.0F,
                256.0F
            );
            return;
        }

        if (entry.nestedpackfix$canRemove()) {
            Gui.drawModalRectWithCustomSizedTexture(
                x,
                y,
                32.0F,
                relativeX < 16 ? 32.0F : 0.0F,
                32,
                32,
                256.0F,
                256.0F
            );
        }

        if (entry.nestedpackfix$canMoveUp()) {
            boolean active = relativeX < 32 && relativeX > 16 && relativeY < 16;
            Gui.drawModalRectWithCustomSizedTexture(
                x,
                y,
                96.0F,
                active ? 32.0F : 0.0F,
                32,
                32,
                256.0F,
                256.0F
            );
        }

        if (entry.nestedpackfix$canMoveDown()) {
            boolean active = relativeX < 32 && relativeX > 16 && relativeY > 16;
            Gui.drawModalRectWithCustomSizedTexture(
                x,
                y,
                64.0F,
                active ? 32.0F : 0.0F,
                32,
                32,
                256.0F,
                256.0F
            );
        }
    }
}
