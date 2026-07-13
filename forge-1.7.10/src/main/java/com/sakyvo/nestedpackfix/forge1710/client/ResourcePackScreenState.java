package com.sakyvo.nestedpackfix.forge1710.client;

import com.sakyvo.nestedpackfix.NestedPackFixLog;
import com.sakyvo.nestedpackfix.forge1710.pack.ArchiveRegistry;
import com.sakyvo.nestedpackfix.forge1710.pack.CompatibleFileResourcePack;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackListEntry;
import net.minecraft.client.resources.ResourcePackListEntryFound;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.util.Util;
import org.lwjgl.Sys;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

@SuppressWarnings({"rawtypes", "unchecked"})
final class ResourcePackScreenState extends Gui {
    private static final int NESTED_BUTTON_ID = 0x4E5001;
    private static final int ILLEGAL_BUTTON_ID = 0x4E5002;
    private static final int ALL_BUTTON_ID = 0x4E5003;
    private static final int BUTTON_X = 2;
    private static final int BUTTON_Y = 3;
    private static final int BUTTON_WIDTH = 84;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 2;
    private static final int TOOLTIP_GAP = 12;
    private static final int TOOLTIP_MARGIN = 4;
    private static final int TOOLTIP_MIN_WIDTH = 80;
    private static final int TOOLTIP_MAX_WIDTH = 240;
    private static final long TOOLTIP_DELAY_MILLIS = 700L;

    private final GuiScreenResourcePacks screen;
    private final ResourcePackScreenBridge.Access bridge;
    private final List allAvailable;
    private final List selected;
    private final List displayedAvailable = new ArrayList();
    private final Map<WarningPackEntry, WarningBounds> warningBounds =
        new IdentityHashMap<WarningPackEntry, WarningBounds>();

    private final GuiButton nestedButton = createButton(
        NESTED_BUTTON_ID,
        "Nested",
        0xFFFF55
    );
    private final GuiButton illegalButton = createButton(
        ILLEGAL_BUTTON_ID,
        "Illegal",
        0xFF5555
    );
    private final GuiButton allButton = createButton(
        ALL_BUTTON_ID,
        "All",
        0x55FF55
    );

    private PackWarningKind filter = PackWarningKind.NONE;
    private WarningPackEntry hoveredWarning;
    private int lastWarningMouseX;
    private int lastWarningMouseY;
    private long warningHoverStartTime;
    private boolean warningTooltipVisible;

    private ResourcePackScreenState(
        GuiScreenResourcePacks screen,
        ResourcePackScreenBridge.Access bridge
    ) {
        this.screen = screen;
        this.bridge = bridge;
        this.allAvailable = bridge.getAvailableEntries();
        this.selected = bridge.getSelectedEntries();
    }

    static ResourcePackScreenState install(
        GuiScreenResourcePacks screen,
        List buttonList
    ) throws ReflectiveOperationException {
        ResourcePackScreenBridge.Access bridge = ResourcePackScreenBridge.open(screen);
        ResourcePackScreenState state = new ResourcePackScreenState(screen, bridge);
        state.installEntries();
        bridge.useDisplayEntries(state.displayedAvailable);
        buttonList.add(state.nestedButton);
        buttonList.add(state.illegalButton);
        buttonList.add(state.allButton);
        state.refreshDisplay();
        return state;
    }

    void beforeDraw() {
        this.warningBounds.clear();
        this.refreshDisplay();
    }

    void recordWarning(
        WarningPackEntry entry,
        int x,
        int y,
        int width,
        int height
    ) {
        boolean available = !this.screen.hasResourcePackEntry((ResourcePackListEntry) entry);
        this.warningBounds.put(
            entry,
            new WarningBounds(x, y, width, height, available)
        );
    }

    void drawTooltip(int mouseX, int mouseY) {
        WarningPackEntry hovered = this.findHoveredWarning(mouseX, mouseY);
        long now = System.currentTimeMillis();
        if (hovered != this.hoveredWarning
            || Math.abs(mouseX - this.lastWarningMouseX) > 5
            || Math.abs(mouseY - this.lastWarningMouseY) > 5) {
            this.hoveredWarning = hovered;
            this.lastWarningMouseX = mouseX;
            this.lastWarningMouseY = mouseY;
            this.warningHoverStartTime = now;
            this.warningTooltipVisible = false;
        }

        if (hovered != null && now >= this.warningHoverStartTime + TOOLTIP_DELAY_MILLIS) {
            this.warningTooltipVisible = true;
            this.drawTooltip(hovered, this.warningBounds.get(hovered), mouseX, mouseY);
        }
    }

    boolean handleWarningClick(
        WarningPackEntry entry,
        int relativeX,
        int relativeY
    ) {
        if (!this.warningTooltipVisible
            || entry != this.hoveredWarning
            || !WarningEntryRenderer.isWarningHit(
                entry.nestedpackfix$getMinecraft(),
                relativeX,
                relativeY
            )) {
            return false;
        }

        this.locateFile(entry.nestedpackfix$getFile());
        return true;
    }

    boolean handleButton(GuiButton button) {
        if (button.id == NESTED_BUTTON_ID) {
            this.filter = PackWarningKind.NESTED;
        } else if (button.id == ILLEGAL_BUTTON_ID) {
            this.filter = PackWarningKind.ILLEGAL;
        } else if (button.id == ALL_BUTTON_ID) {
            this.filter = PackWarningKind.NONE;
        } else {
            return false;
        }
        this.refreshDisplay();
        return true;
    }

    private void installEntries() {
        List preparedAvailable = this.wrapNestedEntries(this.allAvailable);
        List preparedSelected = this.wrapNestedEntries(this.selected);
        for (File file : ArchiveRegistry.findIllegalFiles(
            Minecraft.getMinecraft().getResourcePackRepository().getDirResourcepacks()
        )) {
            preparedAvailable.add(new IllegalResourcePackEntry(this.screen, file, this));
        }

        this.allAvailable.clear();
        this.allAvailable.addAll(preparedAvailable);
        this.selected.clear();
        this.selected.addAll(preparedSelected);
    }

    private List wrapNestedEntries(List source) {
        List wrapped = new ArrayList(source.size());
        for (Object value : source) {
            if (value instanceof ResourcePackListEntryFound
                && !(value instanceof NestedResourcePackEntry)) {
                ResourcePackRepository.Entry repositoryEntry =
                    ((ResourcePackListEntryFound) value).func_148318_i();
                IResourcePack resourcePack = repositoryEntry.getResourcePack();
                if (resourcePack instanceof CompatibleFileResourcePack
                    && ((CompatibleFileResourcePack) resourcePack).isNested()) {
                    wrapped.add(new NestedResourcePackEntry(
                        this.screen,
                        repositoryEntry,
                        (CompatibleFileResourcePack) resourcePack,
                        this
                    ));
                    continue;
                }
            }
            wrapped.add(value);
        }
        return wrapped;
    }

    private void refreshDisplay() {
        AvailablePackFilter.refresh(this.allAvailable, this.displayedAvailable, this.filter);
        this.updateButtons();
    }

    private void updateButtons() {
        this.resetButton(this.nestedButton);
        this.resetButton(this.illegalButton);
        this.resetButton(this.allButton);

        boolean filtering = this.filter != PackWarningKind.NONE;
        this.allButton.visible = filtering;
        this.nestedButton.visible = false;
        this.illegalButton.visible = false;
        if (filtering) {
            return;
        }

        int y = BUTTON_Y;
        if (AvailablePackFilter.contains(this.allAvailable, PackWarningKind.NESTED)) {
            this.nestedButton.visible = true;
            this.nestedButton.yPosition = y;
            y += BUTTON_HEIGHT + BUTTON_GAP;
        }
        if (AvailablePackFilter.contains(this.allAvailable, PackWarningKind.ILLEGAL)) {
            this.illegalButton.visible = true;
            this.illegalButton.yPosition = y;
        }
    }

    private void resetButton(GuiButton button) {
        button.xPosition = BUTTON_X;
        button.yPosition = BUTTON_Y;
        button.width = BUTTON_WIDTH;
        button.height = BUTTON_HEIGHT;
    }

    private WarningPackEntry findHoveredWarning(int mouseX, int mouseY) {
        for (Map.Entry<WarningPackEntry, WarningBounds> entry : this.warningBounds.entrySet()) {
            if (entry.getValue().contains(mouseX, mouseY)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void drawTooltip(
        WarningPackEntry entry,
        WarningBounds bounds,
        int mouseX,
        int mouseY
    ) {
        if (bounds == null) {
            return;
        }

        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
        int capacity = bounds.available
            ? this.screen.width - mouseX - TOOLTIP_GAP - TOOLTIP_MARGIN
            : mouseX - TOOLTIP_GAP - TOOLTIP_MARGIN;
        int wrapWidth = Math.max(
            TOOLTIP_MIN_WIDTH,
            Math.min(TOOLTIP_MAX_WIDTH, capacity)
        );
        wrapWidth = Math.max(
            1,
            Math.min(wrapWidth, this.screen.width - TOOLTIP_MARGIN * 2)
        );
        List lines = font.listFormattedStringToWidth(
            entry.nestedpackfix$getTooltip(),
            wrapWidth
        );
        int textWidth = tooltipWidth(font, lines);
        int textHeight = tooltipHeight(lines);
        int x = bounds.available
            ? mouseX + TOOLTIP_GAP
            : mouseX - TOOLTIP_GAP - textWidth;
        int maxX = Math.max(TOOLTIP_MARGIN, this.screen.width - textWidth - TOOLTIP_MARGIN);
        x = Math.max(TOOLTIP_MARGIN, Math.min(x, maxX));
        int maxY = Math.max(TOOLTIP_MARGIN, this.screen.height - textHeight - TOOLTIP_MARGIN);
        int y = Math.max(TOOLTIP_MARGIN, Math.min(mouseY - 12, maxY));
        this.drawTooltipAt(font, lines, x, y, textWidth, textHeight);
    }

    private void drawTooltipAt(
        FontRenderer font,
        List lines,
        int x,
        int y,
        int textWidth,
        int textHeight
    ) {
        if (lines.isEmpty()) {
            return;
        }

        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        this.zLevel = 300.0F;
        int background = 0xFF100010;
        this.drawGradientRect(x - 3, y - 4, x + textWidth + 3, y - 3,
            background, background);
        this.drawGradientRect(x - 3, y + textHeight + 3, x + textWidth + 3,
            y + textHeight + 4, background, background);
        this.drawGradientRect(x - 3, y - 3, x + textWidth + 3, y + textHeight + 3,
            background, background);
        this.drawGradientRect(x - 4, y - 3, x - 3, y + textHeight + 3,
            background, background);
        this.drawGradientRect(x + textWidth + 3, y - 3, x + textWidth + 4,
            y + textHeight + 3, background, background);
        int borderTop = 0x505000FF;
        int borderBottom = (borderTop & 0xFEFEFE) >> 1 | borderTop & 0xFF000000;
        this.drawGradientRect(x - 3, y - 2, x - 2, y + textHeight + 2,
            borderTop, borderBottom);
        this.drawGradientRect(x + textWidth + 2, y - 2, x + textWidth + 3,
            y + textHeight + 2, borderTop, borderBottom);
        this.drawGradientRect(x - 3, y - 3, x + textWidth + 3, y - 2,
            borderTop, borderTop);
        this.drawGradientRect(x - 3, y + textHeight + 2, x + textWidth + 3,
            y + textHeight + 3, borderBottom, borderBottom);

        int lineY = y;
        for (int line = 0; line < lines.size(); ++line) {
            font.drawStringWithShadow((String) lines.get(line), x, lineY, -1);
            if (line == 0) {
                lineY += 2;
            }
            lineY += 10;
        }

        this.zLevel = 0.0F;
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        RenderHelper.enableStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
    }

    private void locateFile(File file) {
        try {
            if (Util.getOSType() == Util.EnumOS.WINDOWS) {
                Runtime.getRuntime().exec(new String[] {
                    "explorer.exe",
                    "/select," + file.getAbsolutePath()
                });
                return;
            }
            if (Util.getOSType() == Util.EnumOS.OSX) {
                Runtime.getRuntime().exec(new String[] {
                    "/usr/bin/open",
                    "-R",
                    file.getAbsolutePath()
                });
                return;
            }
        } catch (IOException failure) {
            NestedPackFixLog.errorOnce(
                "locate:" + file.getAbsolutePath(),
                "Could not locate resource-pack archive " + file,
                failure
            );
        }

        File parent = file.getParentFile();
        if (parent != null) {
            Sys.openURL("file://" + parent.getAbsolutePath());
        }
    }

    private static GuiButton createButton(int id, String label, int color) {
        GuiButton button = new GuiButton(
            id,
            BUTTON_X,
            BUTTON_Y,
            BUTTON_WIDTH,
            BUTTON_HEIGHT,
            label
        );
        button.packedFGColour = color;
        return button;
    }

    private static int tooltipWidth(FontRenderer font, List lines) {
        int width = 0;
        for (Object line : lines) {
            width = Math.max(width, font.getStringWidth((String) line));
        }
        return width;
    }

    private static int tooltipHeight(List lines) {
        return lines.size() > 1 ? 10 + (lines.size() - 1) * 10 : 8;
    }

    private static final class WarningBounds {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final boolean available;

        private WarningBounds(int x, int y, int width, int height, boolean available) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.available = available;
        }

        private boolean contains(int mouseX, int mouseY) {
            return mouseX >= this.x
                && mouseX < this.x + this.width
                && mouseY >= this.y
                && mouseY < this.y + this.height;
        }
    }
}
