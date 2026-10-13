package com.simplefun;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.resource.conditions.v1.*;
import net.minecraft.core.*;
import net.minecraft.resources.RegistryOps;

public record FunFabricData() implements ResourceCondition {
  public static final MapCodec<FunFabricData> CODEC = MapCodec.unit(new FunFabricData());
  public static final ResourceConditionType<FunFabricData> TYPE =
      ResourceConditionType.create(com.simplefun.registry.FunContent.id("trades_enabled"), CODEC);

  public ResourceConditionType<?> getType() {
    return TYPE;
  }

  public boolean test(RegistryOps.RegistryInfoLookup lookup) {
    return SimplefunCommon.on() && SimplefunCommon.getConfig().fun.enableNoDamageTrades;
  }

  public static void register() {
    ResourceConditions.register(TYPE);
  }
}
