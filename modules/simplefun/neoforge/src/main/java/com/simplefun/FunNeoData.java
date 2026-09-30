package com.simplefun;

import com.mojang.serialization.MapCodec;
import com.simplefun.heads.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.event.LootTableLoadEvent;

@EventBusSubscriber(modid = "simplefun")
public record FunNeoData() implements ICondition {
  public static final MapCodec<FunNeoData> CODEC = MapCodec.unit(new FunNeoData());

  public boolean test(IContext context) {
    return SimplefunCommon.getConfig().fun.enableNoDamageTrades;
  }

  public MapCodec<? extends ICondition> codec() {
    return CODEC;
  }

  @SubscribeEvent
  public static void loot(LootTableLoadEvent event) {
    FunLoot.apply(
        ResourceKey.create(Registries.LOOT_TABLE, event.getName()),
        p -> event.getTable().addPool(p.build()),
        event.getRegistries());
  }
}
