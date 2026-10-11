package com.simplebuilding.modules.simpletweaks;
import net.neoforged.fml.common.Mod;
@Mod("simpletweaks")
public final class NeoForgeSimpleTweaks {
    public NeoForgeSimpleTweaks(net.neoforged.bus.api.IEventBus bus) {
        ModuleNeoTests.register(bus);
  if(net.neoforged.fml.ModList.get().isLoaded("ftbquests"))com.simplebuilding.modules.simpletweaks.guide.TweaksGuide.installQuests(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
  bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e)->{if(!e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES)||!com.simplebuilding.framework.api.SubMods.enabled("simpletweaks"))return;var book=com.simplebuilding.modules.simpletweaks.guide.TweaksGuide.book();var anchor=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);/* before the writable book: a tail append clashes with other mods' insertAfter at the tail */if(e.getParentEntries().contains(anchor))e.insertBefore(anchor,book,net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);else e.accept(book);});
        var events=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        events.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e)->com.simplebuilding.modules.simpletweaks.claims.Claims.start(e.getServer()));
        events.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e)->com.simplebuilding.modules.simpletweaks.claims.Claims.stop(e.getServer()));
        events.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e)->com.simplebuilding.modules.simpletweaks.claims.ClaimCommands.register(e.getDispatcher()));
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent event) -> {
            if (event.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) LegacyDeed.register();
        });
    }
}
