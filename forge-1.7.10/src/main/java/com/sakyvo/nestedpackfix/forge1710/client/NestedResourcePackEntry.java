package com.sakyvo.nestedpackfix.forge1710.client;

import com.sakyvo.nestedpackfix.forge1710.pack.CompatibleFileResourcePack;
import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.ResourcePackListEntryFound;
import net.minecraft.client.resources.ResourcePackRepository;

final class NestedResourcePackEntry extends ResourcePackListEntryFound
    implements WarningPackEntry {
    private static final String TOOLTIP =
        "This pack is nested inside an archive and may not work in other clients.";

    private final ResourcePackScreenState state;
    private final File file;

    NestedResourcePackEntry(
        GuiScreenResourcePacks screen,
        ResourcePackRepository.Entry repositoryEntry,
        CompatibleFileResourcePack pack,
        ResourcePackScreenState state
    ) {
        super(screen, repositoryEntry);
        this.state = state;
        this.file = pack.getFile();
    }

    @Override
    public void drawEntry(
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
        WarningEntryRenderer.draw(
            this,
            index,
            x,
            y,
            width,
            height,
            tessellator,
            mouseX,
            mouseY,
            hovered
        );
    }

    @Override
    public boolean mousePressed(
        int index,
        int mouseX,
        int mouseY,
        int mouseEvent,
        int relativeX,
        int relativeY
    ) {
        if (this.state.handleWarningClick(this, relativeX, relativeY)) {
            return true;
        }
        return super.mousePressed(
            index,
            mouseX,
            mouseY,
            mouseEvent,
            relativeX,
            relativeY
        );
    }

    @Override
    public PackWarningKind nestedpackfix$getWarningKind() {
        return PackWarningKind.NESTED;
    }

    @Override
    public String nestedpackfix$getTooltip() {
        return TOOLTIP;
    }

    @Override
    public File nestedpackfix$getFile() {
        return this.file;
    }

    @Override
    public String nestedpackfix$getTitle() {
        return this.func_148312_b();
    }

    @Override
    public String nestedpackfix$getDescription() {
        return this.func_148311_a();
    }

    @Override
    public int nestedpackfix$getTitleColor() {
        return 0xFFFFFF;
    }

    @Override
    public int nestedpackfix$getWarningColor() {
        return 0xFFFF55;
    }

    @Override
    public boolean nestedpackfix$drawIllegalBackground() {
        return false;
    }

    @Override
    public Minecraft nestedpackfix$getMinecraft() {
        return this.field_148317_a;
    }

    @Override
    public ResourcePackScreenState nestedpackfix$getScreenState() {
        return this.state;
    }

    @Override
    public void nestedpackfix$bindIcon() {
        this.func_148313_c();
    }

    @Override
    public boolean nestedpackfix$isUsable() {
        return this.func_148310_d();
    }

    @Override
    public boolean nestedpackfix$canAdd() {
        return this.func_148309_e();
    }

    @Override
    public boolean nestedpackfix$canRemove() {
        return this.func_148308_f();
    }

    @Override
    public boolean nestedpackfix$canMoveUp() {
        return this.func_148314_g();
    }

    @Override
    public boolean nestedpackfix$canMoveDown() {
        return this.func_148307_h();
    }
}
