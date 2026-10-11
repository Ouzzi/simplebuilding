package com.simplebuilding.modules.simplesandwiches;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;

/** Data condition {@code simplesandwiches:enabled}: recipes load only while the super-mod switch is on. */
public record SandwichesEnabledCondition() implements ICondition {
    public static final MapCodec<SandwichesEnabledCondition> CODEC = MapCodec.unit(new SandwichesEnabledCondition());
    @Override public boolean test(IContext context) { return com.simplesandwiches.Sandwiches.on(); }
    @Override public MapCodec<? extends ICondition> codec() { return CODEC; }
}
