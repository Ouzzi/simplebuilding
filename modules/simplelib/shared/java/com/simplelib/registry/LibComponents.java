package com.simplelib.registry;

import com.simplelib.SimpleLib;
import com.simplelib.warm.Warm;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class LibComponents {
    public static DataComponentType<Warm> WARM;

    public static void register() {
        WARM = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleLib.id("warm"),
                DataComponentType.<Warm>builder().persistent(Warm.CODEC).networkSynchronized(Warm.STREAM_CODEC).build());
    }

    private LibComponents() {}
}
