package com.simplemoney;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.event.LootTableLoadEvent;
@Mod("simplemoney")
public final class MoneyNeoForge {
 public MoneyNeoForge(IEventBus bus) {
  SimpleMoney.loadConfig(FMLPaths.CONFIGDIR.get()); MoneyGameTests.register(bus);
  var conditions=DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS,"simplemoney");
  conditions.register("config",()->MoneyCondition.CODEC);conditions.register(bus);
  bus.addListener((RegisterEvent event)-> {
   if(event.getRegistryKey().equals(Registries.ITEM)) MoneyItems.register();
   if(event.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB)) MoneyItems.registerTab(CreativeModeTab.builder());
   if(event.getRegistryKey().equals(Registries.LOOT_FUNCTION_TYPE)) Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE,Identifier.fromNamespaceAndPath("simplemoney","weighted_enchant"),WeightedEnchantFunction.MAP_CODEC);
  });
  NeoForge.EVENT_BUS.addListener((LootTableLoadEvent event)->MoneyLoot.inject(event.getName().toString(),pool->event.getTable().addPool(pool.build())));
 }
}
