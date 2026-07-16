package com.sakyvo.nestedpackfix.forge189.client;

import com.sakyvo.nestedpackfix.NestedPackFixLog;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;

public final class ResourcePackGuiHandler {
    private final Map<GuiScreenResourcePacks, ResourcePackScreenState> states =
        new WeakHashMap<GuiScreenResourcePacks, ResourcePackScreenState>();

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        this.states.clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiScreenResourcePacks)) {
            return;
        }

        GuiScreenResourcePacks screen = (GuiScreenResourcePacks) event.gui;
        try {
            this.states.put(
                screen,
                ResourcePackScreenState.install(screen, event.buttonList)
            );
        } catch (Throwable failure) {
            NestedPackFixLog.errorOnce(
                "resource-pack-screen:init",
                "NestedPackFix disabled its resource-pack screen enhancement",
                failure
            );
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        ResourcePackScreenState state = this.getState(event);
        if (state != null) {
            state.beforeDraw();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        ResourcePackScreenState state = this.getState(event);
        if (state != null) {
            state.drawTooltip(event.mouseX, event.mouseY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        ResourcePackScreenState state = this.getState(event);
        if (state != null && state.handleButton(event.button)) {
            event.setCanceled(true);
        }
    }

    private ResourcePackScreenState getState(GuiScreenEvent event) {
        return event.gui instanceof GuiScreenResourcePacks
            ? this.states.get((GuiScreenResourcePacks) event.gui)
            : null;
    }
}
