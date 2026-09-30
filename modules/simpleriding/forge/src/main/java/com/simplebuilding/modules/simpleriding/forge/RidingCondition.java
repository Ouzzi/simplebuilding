package com.simplebuilding.modules.simpleriding.forge;
public record RidingCondition() implements net.minecraftforge.common.crafting.conditions.ICondition {
 public static final com.mojang.serialization.MapCodec<RidingCondition> CODEC=com.mojang.serialization.MapCodec.unit(new RidingCondition());
 public boolean test(IContext context,com.mojang.serialization.DynamicOps<?> ops){return com.simpleriding.Riding.CONFIG.worldGen.enableVillagerTrades;}
 public com.mojang.serialization.MapCodec<? extends net.minecraftforge.common.crafting.conditions.ICondition> codec(){return CODEC;}
}
