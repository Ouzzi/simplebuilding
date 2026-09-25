package com.simplebuilding.version;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/** Loot number providers, 26.3 side of the version shim (see the 26.2 twin). */
public final class LootNumbers {

    private LootNumbers() {
    }

    public static Holder<ContextIntProvider> exactly(int value) {
        return ContextIntProviders.exactly(value);
    }

    public static Holder<ContextIntProvider> between(int min, int max) {
        return ContextIntProviders.between(min, max);
    }

    public static Holder<ContextIntProvider> binomial(int n, float p) {
        return ContextIntProviders.binomial(n, p);
    }

    public static JsonElement encodeBetween(DynamicOps<JsonElement> ops, int min, int max) {
        return ContextIntProviders.CODEC.encodeStart(ops, between(min, max)).getOrThrow();
    }
}
