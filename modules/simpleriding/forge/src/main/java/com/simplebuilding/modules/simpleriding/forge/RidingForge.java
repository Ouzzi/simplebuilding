package com.simplebuilding.modules.simpleriding.forge;
import com.simpleriding.*;
import net.minecraft.core.registries.*;
@net.minecraftforge.fml.common.Mod("simpleriding")
public final class RidingForge {
 public RidingForge(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context){
  var bus=context.getModBusGroup(); ModuleForgeTests.register(bus);
  Riding.CONFIG=RidingConfig.load(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
  Riding.SIMPLEBUILDING=net.minecraftforge.fml.ModList.isLoaded("simplebuilding");
  net.minecraftforge.registries.RegisterEvent.getBus(bus).addListener(e->{
   if(e.getRegistryKey().equals(Registries.LOOT_FUNCTION_TYPE))net.minecraft.core.Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE,Riding.id("weighted_enchant"),WeightedEnchantFunction.MAP_CODEC);
   e.register(net.minecraftforge.registries.ForgeRegistries.Keys.CONDITION_SERIALIZERS,Riding.id("trades_enabled"),()->RidingCondition.CODEC);
   if(e.getRegistryKey().equals(Registries.DATA_COMPONENT_TYPE))Riding.components();
   if(e.getRegistryKey().equals(Registries.ITEM))Riding.items();
   if(e.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB))Riding.tab();
  });
 }
}
