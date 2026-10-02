package com.simplebuilding.modules.simpleriding;
import com.simpleriding.*;
import net.neoforged.fml.common.Mod;
import net.minecraft.core.registries.Registries;
@Mod("simpleriding") public final class NeoForgeExample {
 public NeoForgeExample(net.neoforged.bus.api.IEventBus bus){
  RidingNeoTests.register(bus);
  Riding.CONFIG=RidingConfig.load(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
  Riding.SIMPLEBUILDING=net.neoforged.fml.ModList.get().isLoaded("simplebuilding");
  bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e)->{
   if(e.getRegistryKey().equals(Registries.LOOT_FUNCTION_TYPE))net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.LOOT_FUNCTION_TYPE,Riding.id("weighted_enchant"),WeightedEnchantFunction.MAP_CODEC);
   if(e.getRegistryKey().equals(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.CONDITION_CODECS))e.register(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.CONDITION_CODECS,Riding.id("trades_enabled"),()->RidingNeoData.CODEC);
   if(e.getRegistryKey().equals(Registries.DATA_COMPONENT_TYPE))Riding.components();
   if(e.getRegistryKey().equals(Registries.ITEM))Riding.items();
   if(e.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB))Riding.tab();
  });
 }
}
