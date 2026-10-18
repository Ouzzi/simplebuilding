package com.simplebuilding.modules.simpleoptrender;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * O1 leaf culling, own rule (technique as published by Cull Leaves/More Culling: leaves hide each other's inner faces;
 * implemented from the Vanilla face-culling code only). A leaf face towards {@code dir} is left out when the neighbor
 * there and the {@code depth} blocks behind it are all leaves ({@code #minecraft:leaves}): the face can then only be
 * seen through more than {@code depth} cutout leaf layers. A face next to anything that is not leaves always stays, so
 * no hole opens; depth 0 equals Vanilla's "fast leaves" culling while keeping the cutout look outside.
 */
public final class LeafCulling {
    private LeafCulling() {}

    /** {@code neighborPos} = the block next to the face (may be a mutable position; it is not changed). */
    public static boolean cull(BlockGetter level, BlockState state, Direction dir, BlockPos neighborPos, int depth) {
        if (!state.is(BlockTags.LEAVES) || !level.getBlockState(neighborPos).is(BlockTags.LEAVES)) return false;
        if (depth <= 0) return true;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(neighborPos.getX(), neighborPos.getY(), neighborPos.getZ());
        for (int i = 0; i < depth; i++) {
            p.move(dir);
            if (!level.getBlockState(p).is(BlockTags.LEAVES)) return false;
        }
        return true;
    }
}
