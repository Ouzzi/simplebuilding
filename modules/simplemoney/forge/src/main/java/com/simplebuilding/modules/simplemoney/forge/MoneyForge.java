package com.simplebuilding.modules.simplemoney.forge;
import com.simplemoney.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
@net.minecraftforge.fml.common.Mod("simplemoney")
public final class MoneyForge {
 public MoneyForge(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context){
  var bus=context.getModBusGroup();
  SimpleMoney.loadConfig(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
  ModuleForgeTests.register(bus);
  net.minecraftforge.registries.RegisterEvent.getBus(bus).addListener(event->{
   if(event.getRegistryKey().equals(Registries.ITEM))MoneyItems.register();
   if(event.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB))MoneyItems.registerTab(net.minecraft.world.item.CreativeModeTab.builder());
   if(event.getRegistryKey().equals(Registries.LOOT_FUNCTION_TYPE))Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE,Identifier.parse("simplemoney:weighted_enchant"),WeightedEnchantFunction.MAP_CODEC);
   event.register(net.minecraftforge.registries.ForgeRegistries.Keys.CONDITION_SERIALIZERS,Identifier.parse("simplemoney:config"),()->MoneyCondition.CODEC);
  });
 }
}
