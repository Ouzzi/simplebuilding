package com.simplebuilding.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * 26.2 twin of mc26_3's BuildingToolsBridge (bundle 133, checkOverlays). On 26.2 the building tools stay in
 * SimpleBuilding ({@code McVersion.BTOOLS_IN_SB}), so there is no Simple Building Tools to hook into: all no-ops.
 */
public final class BuildingToolsBridge {
    private BuildingToolsBridge() {
    }

    public static void openPlaced(BlockPos pos, ItemStack stack) {
    }

    public static void init() {
    }

    public static void configureClient() {
    }
}
