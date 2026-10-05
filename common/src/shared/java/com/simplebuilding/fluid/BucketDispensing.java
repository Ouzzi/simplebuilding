package com.simplebuilding.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Dispensers handle the crucible buckets like a player (owner 33: the copper bucket breaks on lava "also in the
 * dispenser"): a full bucket pours into the block in front and leaves what a player would keep (nothing when it
 * breaks), an empty copper/Enderite bucket scoops a source it can hold. Vanilla's own bucket already scoops soul lava.
 */
public final class BucketDispensing {
    public static void register() {
        for (Item item : ModFluids.buckets()) {
            if (!(item instanceof ModBucketItem bucket)) continue;
            DispenserBlock.registerBehavior(item, bucket.getContent() == Fluids.EMPTY ? scoop(bucket) : pour(bucket));
        }
    }

    private static BlockPos front(BlockSource source) {
        return source.pos().relative(source.state().getValue(DispenserBlock.FACING));
    }

    private static DefaultDispenseItemBehavior pour(ModBucketItem bucket) {
        return new DefaultDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource source, ItemStack dispensed) {
                Level level = source.level();
                BlockPos target = front(source);
                if (!bucket.emptyContents(null, level, target, null)) return super.execute(source, dispensed);
                ItemStack after = bucket.afterPour(dispensed);
                if (after.isEmpty()) level.playSound(null, target, SoundEvents.ITEM_BREAK.value(), SoundSource.BLOCKS, 0.8F, 1.0F);
                return consumeWithRemainder(source, dispensed, after);
            }
        };
    }

    private static DefaultDispenseItemBehavior scoop(ModBucketItem bucket) {
        return new DefaultDispenseItemBehavior() {
            @Override
            public ItemStack execute(BlockSource source, ItemStack dispensed) {
                Level level = source.level();
                BlockPos target = front(source);
                BlockState state = level.getBlockState(target);
                FluidState fluid = state.getFluidState();
                Item filled = fluid.isSource() ? ModBucketItem.filled(bucket.kind(), fluid.getType()) : null;
                if (filled == null || !(state.getBlock() instanceof BucketPickup pickup)) return super.execute(source, dispensed);
                if (pickup.pickupBlock(null, level, target, state).isEmpty()) return super.execute(source, dispensed);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, target);
                return consumeWithRemainder(source, dispensed, ModBucketItem.fill(dispensed, filled));
            }
        };
    }

    private BucketDispensing() {}
}
