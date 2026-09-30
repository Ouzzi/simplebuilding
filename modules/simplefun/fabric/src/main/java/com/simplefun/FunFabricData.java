package com.simplefun;
import com.simplefun.heads.*;
import net.fabricmc.fabric.api.resource.conditions.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import com.mojang.serialization.MapCodec;
public record FunFabricData() implements ResourceCondition {
 public static final MapCodec<FunFabricData> CODEC=MapCodec.unit(new FunFabricData());
 public static final ResourceConditionType<FunFabricData> TYPE=ResourceConditionType.create(AnimalHeads.id("trades_enabled"),CODEC);
 public ResourceConditionType<?> getType(){return TYPE;}
 public boolean test(RegistryOps.RegistryInfoLookup lookup){return SimplefunCommon.getConfig().fun.enableNoDamageTrades;}
 public static void register(){
  ResourceConditions.register(TYPE);
  net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY.register((key,table,source,registry)->FunLoot.apply(key,table::withPool,registry));
 }
}
