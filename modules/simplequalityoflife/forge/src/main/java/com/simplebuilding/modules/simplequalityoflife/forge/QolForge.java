package com.simplebuilding.modules.simplequalityoflife.forge;
import com.simplequalityoflife.Simplequalityoflife;
/** Super-mod entry: guide book, start quest, sub-mod switches and /simplequalityoflife; the features live in the bundled sub-mods. */
@net.minecraftforge.fml.common.Mod("simplequalityoflife")
public final class QolForge {
 public QolForge(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context){
  Simplequalityoflife.init();ModuleForgeTests.register(context.getModBusGroup());
  net.minecraftforge.registries.RegisterEvent.getBus(context.getModBusGroup()).addListener(e->{if(e.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM))com.simplequalityoflife.guide.QolGuide.register();});
  if(net.minecraftforge.fml.ModList.isLoaded("ftbquests"))com.simplequalityoflife.guide.QolGuide.installQuests(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
  net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(e->{if(!e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES)||!true)return;var book=new net.minecraft.world.item.ItemStack(com.simplequalityoflife.guide.QolGuide.book());var anchor=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);/* before the writable book: a tail append clashes with other mods' putAfter at the tail */if(e.getEntries().contains(anchor))e.getEntries().putBefore(anchor,book,net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);else e.accept(book);});
  net.minecraftforge.event.server.ServerStartedEvent.BUS.addListener(e->Simplequalityoflife.serverStarted());
  net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(e->com.simplequalityoflife.command.ModCommands.register(e.getDispatcher()));
 }
}
