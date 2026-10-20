package com.simplestorage.client;

import com.simplestorage.bundle.DyedStorage;
import com.simplestorage.bundle.PlacedBundleBlockEntity;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/** The leather layer of a placed dyed bundle in the colour of its block entity (split stage 8c-1, from SimpleBuilding's {@code BackpackBlockTint}). */
public final class PlacedBundleTint implements BlockTintSource {
    public static final PlacedBundleTint INSTANCE = new PlacedBundleTint();

    private PlacedBundleTint() {
    }

    public static int colorAt(BlockAndTintGetter level, BlockPos pos) {
        if (level != null && pos != null && level.getBlockEntity(pos) instanceof PlacedBundleBlockEntity bundle
                && bundle.dyeColor() != DyedStorage.UNDYED) {
            return ARGB.opaque(bundle.dyeColor());
        }
        return -1;
    }

    @Override
    public int color(BlockState state) {
        return -1;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return colorAt(level, pos);
    }
}
