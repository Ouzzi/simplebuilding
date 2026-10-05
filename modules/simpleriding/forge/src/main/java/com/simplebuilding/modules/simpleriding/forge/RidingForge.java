package com.simplebuilding.modules.simpleriding.forge;
import com.simpleriding.*;
import net.minecraft.core.registries.*;
@net.minecraftforge.fml.common.Mod("simpleriding")
public final class RidingForge {
 public RidingForge(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context){
  var bus=context.getModBusGroup(); ModuleForgeTests.register(bus);
  Riding.CONFIG=RidingConfig.load(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
  Riding.SIMPLEBUILDING=net.minecraftforge.fml.ModList.isLoaded("simplebuilding");
  if(net.minecraftforge.fml.ModList.isLoaded("ftbquests"))com.simpleriding.guide.RidingGuide.installQuests(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
  net.minecraftforge.registries.RegisterEvent.getBus(bus).addListener(e->{
   if(e.getRegistryKey().equals(Registries.LOOT_FUNCTION_TYPE))net.minecraft.core.Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE,Riding.id("weighted_enchant"),WeightedEnchantFunction.MAP_CODEC);
   e.register(net.minecraftforge.registries.ForgeRegistries.Keys.CONDITION_SERIALIZERS,Riding.id("trades_enabled"),()->RidingCondition.CODEC);
   if(e.getRegistryKey().equals(Registries.DATA_COMPONENT_TYPE))Riding.components();
   if(e.getRegistryKey().equals(Registries.ITEM))Riding.items();
   if(e.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB))Riding.tab();
  });
  net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(event->{
   var anchorItem=Riding.vanillaTabAnchor(event.getTabKey());if(anchorItem==null)return;
   var entries=event.getEntries();var anchor=new net.minecraft.world.item.ItemStack(anchorItem);
   if(!entries.contains(anchor))return;
   for(var stack:Riding.vanillaTabStacks(event.getTabKey()))
    if(!entries.contains(stack))entries.putBefore(anchor,stack,net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
  });
 }
}
