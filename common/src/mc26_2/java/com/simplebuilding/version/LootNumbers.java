package com.simplebuilding.version;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * Loot number providers, 26.2 side of the version shim (see {@link McVersion}). 26.2 builds loot
 * pools from {@link NumberProvider}s; 26.3 from {@code Holder<ContextIntProvider>}. Shared code
 * passes the result straight into {@code setRolls} / {@code setCount} and never names the type.
 */
public final class LootNumbers {

    private LootNumbers() {
    }

    public static NumberProvider exactly(int value) {
        return ConstantValue.exactly(value);
    }

    public static NumberProvider between(int min, int max) {
        return UniformGenerator.between(min, max);
    }

    public static NumberProvider binomial(int n, float p) {
        return BinomialDistributionGenerator.binomial(n, p);
    }

    /** {@link #between} encoded with the codec a loot pool uses for its rolls. */
    public static JsonElement encodeBetween(DynamicOps<JsonElement> ops, int min, int max) {
        return NumberProviders.CODEC.encodeStart(ops, between(min, max)).getOrThrow();
    }
}
