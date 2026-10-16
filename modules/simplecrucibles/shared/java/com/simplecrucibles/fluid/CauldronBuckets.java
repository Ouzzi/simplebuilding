package com.simplecrucibles.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * Copper, Enderite and ceramic buckets on a cauldron, written against the matching Vanilla cauldron state: the Vanilla
 * cauldron calls it from {@code CauldronBucketMixin}, the wooden cauldron with its content mapped to the Vanilla block
 * (and mapping the result back). Decided by the item class ({@link CrucibleBucketItem}), never by item identity, so every
 * bucket kind and filling (half and full Enderite, all wear stages) works the same everywhere.
 */
public final class CauldronBuckets {
    private CauldronBuckets() {}

    /** The result of using {@code stack} on a (Vanilla-mapped) cauldron {@code state}, or null when this is not a bucket case. */
    public static @Nullable InteractionResult interact(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!(stack.getItem() instanceof CrucibleBucketItem bucket)) return null;
        Fluid content = bucket.getContent();
        if (content == Fluids.EMPTY) {
            Fluid take = state.is(Blocks.LAVA_CAULDRON) ? Fluids.LAVA
                    : state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == 3 ? Fluids.WATER : null;
            Item filled = take == null || !CrucibleBucketItem.canScoop(stack) ? null : CrucibleBucketItem.filled(bucket.kind(), take);
            if (filled == null) return null;
            if (!level.isClientSide()) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, CrucibleBucketItem.fill(stack, filled)));
                level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                level.playSound(null, pos, take == Fluids.LAVA ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        // Owner N21: a half Enderite bucket takes a full cauldron of its own fluid and becomes full.
        Item fuller = CrucibleFluids.fullEnderite(bucket);
        boolean same = content.isSame(Fluids.LAVA) ? state.is(Blocks.LAVA_CAULDRON)
                : content.isSame(Fluids.WATER) && state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == 3;
        if (fuller != null && same) {
            if (!level.isClientSide()) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, CrucibleBucketItem.fill(stack, fuller)));
                level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                level.playSound(null, pos, content.isSame(Fluids.LAVA) ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        if (!state.is(Blocks.CAULDRON)) return null;
        BlockState next = content.isSame(Fluids.WATER) ? Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3)
                : content.isSame(Fluids.LAVA) ? Blocks.LAVA_CAULDRON.defaultBlockState() : null;
        if (next == null) return null;
        if (!level.isClientSide()) {
            ItemStack after = player.hasInfiniteMaterials() ? stack : bucket.afterPour(stack);
            player.setItemInHand(hand, after);
            level.setBlockAndUpdate(pos, next);
            level.playSound(null, pos, content.isSame(Fluids.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (after.isEmpty()) level.playSound(null, pos, SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }
}
