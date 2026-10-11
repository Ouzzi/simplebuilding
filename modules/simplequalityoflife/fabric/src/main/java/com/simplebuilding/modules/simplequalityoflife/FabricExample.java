package com.simplebuilding.modules.simplequalityoflife;
import com.simplequalityoflife.*;
import net.fabricmc.api.ModInitializer;
/** Super-mod entry: guide book, start quest, sub-mod switches and /simplequalityoflife; the features live in the bundled sub-mods. */
public final class FabricExample implements ModInitializer {
 public void onInitialize(){
  Simplequalityoflife.init();
  com.simplequalityoflife.guide.QolGuide.register();
  if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("ftbquests"))com.simplequalityoflife.guide.QolGuide.installQuests(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
  net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES).register(out->{if(!true)return;var book=new net.minecraft.world.item.ItemStack(com.simplequalityoflife.guide.QolGuide.book());/* before the writable book: a tail append clashes with other mods' insertAfter at the tail */if(out.getDisplayStacks().stream().anyMatch(s->s.is(net.minecraft.world.item.Items.WRITABLE_BOOK)))out.insertBefore(net.minecraft.world.item.Items.WRITABLE_BOOK,java.util.List.of(book),net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);else out.accept(book);});
  net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(s->Simplequalityoflife.serverStarted());
  net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d,r,e)->com.simplequalityoflife.command.ModCommands.register(d));
 }
}
