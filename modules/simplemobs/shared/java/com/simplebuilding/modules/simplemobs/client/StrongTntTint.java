package com.simplebuilding.modules.simplemobs.client;

import java.util.Set;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** Placeholder look of the Strong TNT (BS25-21): the Vanilla TNT texture with a constant tint, until the owner picks a texture. */
public final class StrongTntTint implements BlockTintSource {
    public static final StrongTntTint INSTANCE = new StrongTntTint();
    /** Same colour as the tint of the item models (data/assets items/strong_*.json). */
    public static final int COLOR = 0xFFFF9A3C;

    private StrongTntTint() {}

    @Override
    public int color(BlockState state) {
        return COLOR;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return COLOR;
    }

    @Override
    public int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return COLOR;
    }

    @Override
    public Set<Property<?>> relevantProperties() {
        return Set.of();
    }
}
