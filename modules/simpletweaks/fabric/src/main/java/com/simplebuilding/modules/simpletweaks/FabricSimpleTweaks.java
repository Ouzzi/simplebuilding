package com.simplebuilding.modules.simpletweaks;
import net.fabricmc.api.ModInitializer;
public final class FabricSimpleTweaks implements ModInitializer {
    @Override public void onInitialize() {
        LegacyDeed.register();
  if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("ftbquests"))com.simplebuilding.modules.simpletweaks.guide.TweaksGuide.installQuests(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
  net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->out.accept(com.simplebuilding.modules.simpletweaks.guide.TweaksGuide.book()));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(com.simplebuilding.modules.simpletweaks.claims.Claims::start);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(com.simplebuilding.modules.simpletweaks.claims.Claims::stop);
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d,r,e)->com.simplebuilding.modules.simpletweaks.claims.ClaimCommands.register(d));
    }
}
