package com.simplebuilding.modules.simplequalityoflife;
import com.simplequalityoflife.*;
/** Super-mod entry: guide book, start quest, sub-mod switches and /simplequalityoflife; the features live in the bundled sub-mods. */
@net.neoforged.fml.common.Mod("simplequalityoflife")
public final class QualityOfLifeNeoForge {
 public QualityOfLifeNeoForge(net.neoforged.bus.api.IEventBus bus){
  Simplequalityoflife.init();
  bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e)->{if(e.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM))com.simplequalityoflife.guide.QolGuide.register();});
  if(net.neoforged.fml.ModList.get().isLoaded("ftbquests"))com.simplequalityoflife.guide.QolGuide.installQuests(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
  bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e)->{if(!e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES)||!true)return;var book=new net.minecraft.world.item.ItemStack(com.simplequalityoflife.guide.QolGuide.book());var anchor=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);/* before the writable book: a tail append clashes with other mods' insertAfter at the tail */if(e.getParentEntries().contains(anchor))e.insertBefore(anchor,book,net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);else e.accept(book);});
  ModuleNeoTests.register(bus);
  var game=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
  game.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e)->Simplequalityoflife.serverStarted());
  game.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e)->com.simplequalityoflife.command.ModCommands.register(e.getDispatcher()));
 }
}
