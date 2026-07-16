package com.sakyvo.nestedpackfix.forge189.client;

import java.io.File;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.ResourcePackListEntry;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

final class IllegalResourcePackEntry extends ResourcePackListEntry
    implements WarningPackEntry {
    private static final String TOOLTIP =
        "This archive is not a valid resource pack and cannot be enabled.";

    private final ResourcePackScreenState state;
    private final File file;
    private final ResourceLocation iconLocation;

    IllegalResourcePackEntry(
        GuiScreenResourcePacks screen,
        File file,
        ResourcePackScreenState state
    ) {
        super(screen);
        this.state = state;
        this.file = file;

        DynamicTexture icon;
        try {
            icon = new DynamicTexture(
                this.mc.getResourcePackRepository()
                    .rprDefaultResourcePack.getPackImage()
            );
        } catch (IOException failure) {
            icon = TextureUtil.missingTexture;
        }
        this.iconLocation = this.mc.getTextureManager()
            .getDynamicTextureLocation("nestedpackfix-illegal", icon);
    }

    @Override
    public void drawEntry(
        int index,
        int x,
        int y,
        int width,
        int height,
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
        return this.state.handleWarningClick(this, relativeX, relativeY);
    }

    @Override
    protected String func_148311_a() {
        return EnumChatFormatting.RED + "Invalid resource pack archive";
    }

    @Override
    protected int func_183019_a() {
        return 1;
    }

    @Override
    protected String func_148312_b() {
        return this.file.getName();
    }

    @Override
    protected void func_148313_c() {
        this.mc.getTextureManager().bindTexture(this.iconLocation);
    }

    @Override
    protected boolean func_148310_d() {
        return false;
    }

    @Override
    public PackWarningKind nestedpackfix$getWarningKind() {
        return PackWarningKind.ILLEGAL;
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
        return 0xFF5555;
    }

    @Override
    public int nestedpackfix$getWarningColor() {
        return 0xFF5555;
    }

    @Override
    public boolean nestedpackfix$drawIllegalBackground() {
        return true;
    }

    @Override
    public int nestedpackfix$getPackFormat() {
        return 1;
    }

    @Override
    public Minecraft nestedpackfix$getMinecraft() {
        return this.mc;
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
        return false;
    }

    @Override
    public boolean nestedpackfix$canAdd() {
        return false;
    }

    @Override
    public boolean nestedpackfix$canRemove() {
        return false;
    }

    @Override
    public boolean nestedpackfix$canMoveUp() {
        return false;
    }

    @Override
    public boolean nestedpackfix$canMoveDown() {
        return false;
    }
}
