package com.simplebuilding.component;

import com.mojang.serialization.Codec;
import com.simplebuilding.Simplebuilding;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

import java.util.function.UnaryOperator;

public class ModDataComponentTypes {
    public static final ComponentType<Integer> OFFSET = register("offset", builder -> builder.codec(Codec.INT));

    public static final ComponentType<Integer> GLOW_LEVEL = register("glow_level", builder -> builder.codec(Codec.INT));

    // NEU: Visueller Glow (RGB Effekt)
    public static final ComponentType<Boolean> VISUAL_GLOW = register("visual_glow", builder -> builder.codec(Codec.BOOL));

    // NEU: Lichtquelle (Fackel-Effekt)
    public static final ComponentType<Boolean> LIGHT_SOURCE = register("light_source", builder -> builder.codec(Codec.BOOL));

    public static final ComponentType<BlockPos> COORDINATES =
            register("coordinates", builder -> builder.codec(BlockPos.CODEC));


    private static <T> ComponentType<T> register(String name, UnaryOperator<ComponentType.Builder<T>> builderOperator) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(Simplebuilding.MOD_ID, name), (builderOperator.apply(ComponentType.builder())).build());
    }

    public static void registerDataComponentTypes() {
        Simplebuilding.LOGGER.info("Registering Data Component Types for " + Simplebuilding.MOD_ID);
    }
}
