package com.simplebuilding.util;

import com.simplebuilding.api.WorldPermissions;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * A landed anvil consumes one diamond block; automation always loses one diamond's value. On 26.3 SimpleLib does it
 * ({@code com.simplelib.material.AnvilCrushing}, same numbers); the constants stay here for the wiki/JEI export.
 */
public final class AnvilDiamondCrushing {
    /** Nine pebbles craft a cracked diamond, which blasts into one diamond. */
    public static final int PEBBLES = 72;
    public static final int MINIMUM_FALL_BLOCKS = 1;

    private AnvilDiamondCrushing() {}

    public static void onLand(Level level, BlockPos landed, FallingBlockEntity anvil) {
        // 26.3 (split stage 3d): SimpleLib crushes (shared material cracked_diamond, requested by SimpleBuilding).
        if (!McVersion.ANVIL_DIAMOND_CRUSH || McVersion.MATERIALS_IN_LIB || !(level instanceof ServerLevel server)
                || !anvil.getBlockState().is(BlockTags.ANVIL)
                || anvil.getStartPos().getY() - landed.getY() < MINIMUM_FALL_BLOCKS) return;
        BlockPos target = landed.below();
        if (!server.getBlockState(target).is(Blocks.DIAMOND_BLOCK)
                || !WorldPermissions.mayAutomate(server, anvil.getStartPos(), target)
                || !server.destroyBlock(target, false, anvil)) return;

        // Consume first, then drop legal stacks. Repeated falls cannot reuse the same input.
        for (int remaining = PEBBLES; remaining > 0;) {
            ItemStack pebbles = new ItemStack(ModItems.DIAMOND_PEBBLE);
            int count = Math.min(remaining, pebbles.getMaxStackSize());
            pebbles.setCount(count);
            Block.popResource(server, target, pebbles);
            remaining -= count;
        }
    }
}
