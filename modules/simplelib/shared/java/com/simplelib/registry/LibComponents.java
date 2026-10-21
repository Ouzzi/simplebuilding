package com.simplelib.registry;

import com.simplelib.SimpleLib;
import com.simplelib.api.SimpleRarity;
import com.simplelib.warm.Warm;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class LibComponents {
    public static DataComponentType<Warm> WARM;
    /** Tier above Epic ({@link SimpleRarity}); built here so items may use it before the registry event ran. */
    public static final DataComponentType<SimpleRarity> RARITY =
            DataComponentType.<SimpleRarity>builder().persistent(SimpleRarity.CODEC).networkSynchronized(SimpleRarity.STREAM_CODEC).build();

    public static void register() {
        WARM = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleLib.id("warm"),
                DataComponentType.<Warm>builder().persistent(Warm.CODEC).networkSynchronized(Warm.STREAM_CODEC).build());
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleLib.id("rarity"), RARITY);
    }

    private LibComponents() {}
}
