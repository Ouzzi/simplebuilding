package com.simplebuilding.modules.simpleriding;
import com.simpleriding.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
public final class FabricExample implements ModInitializer {
 public void onInitialize(){Riding.CONFIG=RidingConfig.load(FabricLoader.getInstance().getConfigDir());Riding.components();Riding.SIMPLEBUILDING=FabricLoader.getInstance().isModLoaded("simplebuilding");Riding.items();Riding.tab();RidingFabricData.register();
  if(FabricLoader.getInstance().isModLoaded("ftbquests"))com.simpleriding.guide.RidingGuide.installQuests(FabricLoader.getInstance().getConfigDir());
  for(var key:java.util.List.of(net.minecraft.world.item.CreativeModeTabs.COMBAT,net.minecraft.world.item.CreativeModeTabs.INGREDIENTS))
   net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(key).register(out->{
    var anchor=Riding.vanillaTabAnchor(key);var stacks=Riding.vanillaTabStacks(key);
    if(!stacks.isEmpty()&&out.getDisplayStacks().stream().anyMatch(s->s.is(anchor)))out.insertBefore(anchor,stacks,net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
   });
 }
}
