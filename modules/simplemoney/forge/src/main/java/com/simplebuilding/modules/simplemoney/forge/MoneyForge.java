package com.simplebuilding.modules.simplemoney.forge;
import com.simplemoney.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
@net.minecraftforge.fml.common.Mod("simplemoney")
public final class MoneyForge {
 public MoneyForge(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context){
  var bus=context.getModBusGroup();
  MoneyLinks.loaded=net.minecraftforge.fml.ModList::isLoaded;
  SimpleMoney.loadConfig(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
  ModuleForgeTests.register(bus);
  if(net.minecraftforge.fml.ModList.isLoaded("ftbquests"))com.simplemoney.guide.MoneyGuide.installQuests(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
  net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(event->{
   var entries=event.getEntries();var anchor=new net.minecraft.world.item.ItemStack(MoneyItems.SEARCH_ANCHOR);
   if(!event.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.INGREDIENTS)||!entries.contains(anchor))return;
   var previous=anchor;
   for(var stack:MoneyItems.tabStacks()){entries.putAfter(previous,stack,net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);previous=stack;}
  });
  net.minecraftforge.registries.RegisterEvent.getBus(bus).addListener(event->{
   if(event.getRegistryKey().equals(Registries.ITEM)){MoneyItems.register();com.simplemoney.guide.MoneyGuide.register();}
   if(event.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB))MoneyItems.registerTab(net.minecraft.world.item.CreativeModeTab.builder());
   if(event.getRegistryKey().equals(Registries.LOOT_FUNCTION_TYPE))Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE,Identifier.parse("simplemoney:weighted_enchant"),WeightedEnchantFunction.MAP_CODEC);
   event.register(net.minecraftforge.registries.ForgeRegistries.Keys.CONDITION_SERIALIZERS,Identifier.parse("simplemoney:config"),()->MoneyCondition.CODEC);
  });
 }
}
