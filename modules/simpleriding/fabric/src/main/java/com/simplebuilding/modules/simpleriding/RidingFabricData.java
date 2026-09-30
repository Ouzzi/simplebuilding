package com.simplebuilding.modules.simpleriding;
import com.simpleriding.*;
import net.fabricmc.fabric.api.resource.conditions.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import com.mojang.serialization.MapCodec;
public record RidingFabricData() implements ResourceCondition {
 public static final MapCodec<RidingFabricData> CODEC=MapCodec.unit(new RidingFabricData());
 public static final ResourceConditionType<RidingFabricData> TYPE=ResourceConditionType.create(Riding.id("trades_enabled"),CODEC);
 public ResourceConditionType<?> getType(){return TYPE;}
 public boolean test(RegistryOps.RegistryInfoLookup lookup){return Riding.CONFIG.worldGen.enableVillagerTrades;}
 public static void register(){
  ResourceConditions.register(TYPE); Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE,Riding.id("weighted_enchant"),WeightedEnchantFunction.MAP_CODEC);
  net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY.register((key,table,source,registry)->RidingLoot.apply(key,table::withPool,registry));
 }
}
