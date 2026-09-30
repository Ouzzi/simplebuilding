package com.simplemoney;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
public final class MoneyFabric implements ModInitializer {
 public void onInitialize() {
  MoneyLinks.loaded=FabricLoader.getInstance()::isModLoaded;
  SimpleMoney.loadConfig(FabricLoader.getInstance().getConfigDir()); MoneyItems.register(); MoneyItems.registerTab(FabricCreativeModeTab.builder());
  Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE,Identifier.fromNamespaceAndPath("simplemoney","weighted_enchant"),WeightedEnchantFunction.MAP_CODEC);
  MoneyCondition.register();
  LootTableEvents.MODIFY.register((key,builder,source,registries)->MoneyLoot.inject(key.identifier().toString(),builder::withPool));
 }
}
