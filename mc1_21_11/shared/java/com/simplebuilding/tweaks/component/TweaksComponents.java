package com.simplebuilding.tweaks.component;

import com.mojang.serialization.Codec;
import com.simplebuilding.tweaks.SimpleTweaks;
import java.util.function.UnaryOperator;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

/** Item-Komponenten der Spawn-Elytra, 1:1 aus Simple Tweaks ({@code ModDataComponentTypes}). */
public final class TweaksComponents {

    /** Verbleibende Flugzeit in Ticks. */
    public static final DataComponentType<Integer> FLIGHT_TIME = register("flight_time",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Verbleibende Boost-Menge, 0.0 - 1.0. */
    public static final DataComponentType<Float> BOOST_LEVEL = register("boost_level",
            b -> b.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));

    /** Weltzeit, zu der der Traeger zuletzt im Bereich eines Elytra-Pads stand (Kulanzzeit). */
    public static final DataComponentType<Long> LAST_PAD_TICK = register("last_pad_tick",
            b -> b.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    /** Schuetzt vor Fall- und Kinetikschaden (Spawn: ja, Pad: nein). */
    public static final DataComponentType<Boolean> IS_SAFE_ELYTRA = register("is_safe_elytra",
            b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    private TweaksComponents() {
    }

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> op) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleTweaks.id(name), op.apply(DataComponentType.builder()).build());
    }

    public static void init() {
    }
}
