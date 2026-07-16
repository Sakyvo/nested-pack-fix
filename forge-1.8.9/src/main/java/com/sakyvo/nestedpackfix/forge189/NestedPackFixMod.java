package com.sakyvo.nestedpackfix.forge189;

import com.sakyvo.nestedpackfix.NestedPackFixLog;
import com.sakyvo.nestedpackfix.forge189.client.ResourcePackGuiHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;

@Mod(
    modid = "nestedpackfix",
    name = "NestedPackFix",
    version = "1.0.0",
    acceptedMinecraftVersions = "[1.8.9]",
    acceptableRemoteVersions = "*"
)
public final class NestedPackFixMod {
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            MinecraftForge.EVENT_BUS.register(new ResourcePackGuiHandler());
            NestedPackFixLog.info("NestedPackFix 1.0.0 active for Minecraft 1.8.9");
        }
    }
}
