package com.simplebuilding.modules.simpletweaks;
import net.fabricmc.api.ModInitializer;
public final class FabricSimpleTweaks implements ModInitializer {
    @Override public void onInitialize() {
        LegacyDeed.register();
  if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("ftbquests"))com.simplebuilding.modules.simpletweaks.guide.TweaksGuide.installQuests(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
  net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->{if(!com.simplebuilding.framework.api.SubMods.enabled("simpletweaks"))return;var book=new net.minecraft.world.item.ItemStack(com.simplebuilding.modules.simpletweaks.guide.TweaksGuide.book());/* before the writable book: a tail append clashes with other mods' insertAfter at the tail */if(out.getDisplayStacks().stream().anyMatch(s->s.is(net.minecraft.world.item.Items.WRITABLE_BOOK)))out.insertBefore(net.minecraft.world.item.Items.WRITABLE_BOOK,java.util.List.of(book),net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);else out.accept(book);});
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(com.simplebuilding.modules.simpletweaks.claims.Claims::start);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(com.simplebuilding.modules.simpletweaks.claims.Claims::stop);
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d,r,e)->com.simplebuilding.modules.simpletweaks.claims.ClaimCommands.register(d));
    }
}
