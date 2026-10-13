package com.simplefun;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;

public record FunNeoData() implements ICondition {
  public static final MapCodec<FunNeoData> CODEC = MapCodec.unit(new FunNeoData());

  public boolean test(IContext context) {
    return SimplefunCommon.on() && SimplefunCommon.getConfig().fun.enableNoDamageTrades;
  }

  public MapCodec<? extends ICondition> codec() {
    return CODEC;
  }
}
