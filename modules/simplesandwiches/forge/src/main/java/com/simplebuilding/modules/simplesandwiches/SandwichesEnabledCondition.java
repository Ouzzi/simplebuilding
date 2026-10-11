package com.simplebuilding.modules.simplesandwiches;

/** Data condition {@code simplesandwiches:enabled}: recipes load only while the super-mod switch is on. */
public record SandwichesEnabledCondition() implements net.minecraftforge.common.crafting.conditions.ICondition {
    public static final com.mojang.serialization.MapCodec<SandwichesEnabledCondition> CODEC = com.mojang.serialization.MapCodec.unit(new SandwichesEnabledCondition());
    @Override public boolean test(IContext context, com.mojang.serialization.DynamicOps<?> ops) { return com.simplesandwiches.Sandwiches.on(); }
    @Override public com.mojang.serialization.MapCodec<? extends net.minecraftforge.common.crafting.conditions.ICondition> codec() { return CODEC; }
}
