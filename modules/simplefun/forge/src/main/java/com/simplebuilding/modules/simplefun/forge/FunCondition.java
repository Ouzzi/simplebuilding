package com.simplebuilding.modules.simplefun.forge;
public record FunCondition() implements net.minecraftforge.common.crafting.conditions.ICondition {
 public static final com.mojang.serialization.MapCodec<FunCondition> CODEC=com.mojang.serialization.MapCodec.unit(new FunCondition());
 public boolean test(IContext c,com.mojang.serialization.DynamicOps<?> ops){return com.simplefun.SimplefunCommon.getConfig().fun.enableNoDamageTrades;}
 public com.mojang.serialization.MapCodec<? extends net.minecraftforge.common.crafting.conditions.ICondition> codec(){return CODEC;}
}
