package com.simplebuildingtools.component;

import com.mojang.serialization.Codec;
import com.simplebuildingtools.SimpleBuildingTools;
import com.simplebuildingtools.blueprint.BlueprintContent;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

/** The blueprint's components (until 07.11.2026 SimpleBuilding's {@code simplebuilding:blueprint}/{@code blueprint_rotation}). */
public final class BtComponents {
    /** Build code + title/author/signed (docs/BLUEPRINT.md). */
    public static DataComponentType<BlueprintContent> BLUEPRINT;
    /** Rotation in build mode (quarter turns clockwise, 0..3; Ctrl+mouse wheel). */
    public static DataComponentType<Integer> BLUEPRINT_ROTATION;
    private static boolean initialized;

    private BtComponents() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        BLUEPRINT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleBuildingTools.id("blueprint"),
                DataComponentType.<BlueprintContent>builder().persistent(BlueprintContent.CODEC)
                        .networkSynchronized(BlueprintContent.STREAM_CODEC).cacheEncoding().build());
        BLUEPRINT_ROTATION = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleBuildingTools.id("blueprint_rotation"),
                DataComponentType.<Integer>builder().persistent(Codec.intRange(0, 3))
                        .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT).build());
    }
}
