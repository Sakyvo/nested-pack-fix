package com.sakyvo.nestedpackfix.forge1710;

import com.sakyvo.nestedpackfix.NestedPackFixLog;
import com.sakyvo.nestedpackfix.forge1710.client.ResourcePackGuiHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;

@Mod(
    modid = "nestedpackfix",
    name = "NestedPackFix",
    version = "1.0.0",
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*"
)
public final class NestedPackFixMod {
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            MinecraftForge.EVENT_BUS.register(new ResourcePackGuiHandler());
            NestedPackFixLog.info("NestedPackFix 1.0.0 active for Minecraft 1.7.10");
        }
    }
}
