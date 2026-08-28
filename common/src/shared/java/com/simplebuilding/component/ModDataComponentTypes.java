package com.simplebuilding.component;

import com.mojang.serialization.Codec;
import com.simplebuilding.Simplebuilding;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class ModDataComponentTypes {
    public static final DataComponentType<Integer> OFFSET = register("offset", builder -> builder.persistent(Codec.INT));

    public static final DataComponentType<Integer> GLOW_LEVEL = register("glow_level", builder -> builder.persistent(Codec.INT));

    // NEU: Visueller Glow (RGB Effekt)
    public static final DataComponentType<Boolean> VISUAL_GLOW = register("visual_glow", builder -> builder.persistent(Codec.BOOL));

    // NEU: Lichtquelle (Fackel-Effekt)
    public static final DataComponentType<Boolean> LIGHT_SOURCE = register("light_source", builder -> builder.persistent(Codec.BOOL));

    public static final DataComponentType<BlockPos> COORDINATES =
            register("coordinates", builder -> builder.persistent(BlockPos.CODEC));


    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name), (builderOperator.apply(DataComponentType.builder())).build());
    }

    public static void registerDataComponentTypes() {
        Simplebuilding.LOGGER.info("Registering Data Component Types for " + Simplebuilding.MOD_ID);
    }
}
