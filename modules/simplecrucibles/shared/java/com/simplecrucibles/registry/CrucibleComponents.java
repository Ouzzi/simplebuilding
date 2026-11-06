package com.simplecrucibles.registry;

import com.mojang.serialization.Codec;
import com.simplecrucibles.SimpleCrucibles;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

/**
 * Data components of the buckets (split stage 5b: were {@code simplebuilding:oxidation|waxed|wax_worn};
 * stacks in old worlds migrate through the id moves). Registered in the DATA_COMPONENT_TYPE phase (Fabric directly).
 */
public final class CrucibleComponents {
    /** Copper bucket: oxidation stage 0-3 (looks) and waxed. */
    public static DataComponentType<Integer> OXIDATION;
    public static DataComponentType<Boolean> WAXED;
    /** Copper bucket: the wax was worn by filling it (Nachtrag 60); weathers at half pace until waxed again. */
    public static DataComponentType<Boolean> WAX_WORN;
    // WH26-7: ceramic_uses is Simple Tools' (simpletools:ceramic_uses) with the ceramic bucket.

    public static void register() {
        if (OXIDATION != null) return;
        OXIDATION = register("oxidation", DataComponentType.<Integer>builder().persistent(Codec.intRange(0, 3)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        WAXED = register("waxed", DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
        WAX_WORN = register("wax_worn", DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
        com.simplelib.bucket.Buckets.OXIDATION = OXIDATION;
        com.simplelib.bucket.Buckets.WAXED = WAXED;
        com.simplelib.bucket.Buckets.WAX_WORN = WAX_WORN;
    }

    private static <T> DataComponentType<T> register(String name, DataComponentType<T> type) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleCrucibles.id(name), type);
    }

    private CrucibleComponents() {}
}
