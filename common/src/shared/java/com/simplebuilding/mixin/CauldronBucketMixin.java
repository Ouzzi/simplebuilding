package com.simplebuilding.mixin;

import com.simplebuilding.fluid.ModBucketItem;
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
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Copper and Enderite buckets on the Vanilla cauldron (Crucible P5, owner 33): pour water/lava into an empty
 * cauldron and take a full water/lava cauldron like the iron bucket - the copper bucket still breaks on lava and
 * oxidizes when pouring. Soul lava never goes into a normal cauldron (owner 30: only the reinforced one). A full
 * Enderite bucket pours one of its two buckets, a half one tops up from a full cauldron of its fluid (owner N21).
 */
@Mixin(AbstractCauldronBlock.class)
public abstract class CauldronBucketMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$crucibleBuckets(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(stack.getItem() instanceof ModBucketItem bucket)) return;
        Fluid content = bucket.getContent();
        if (content == Fluids.EMPTY) {
            Fluid take = state.is(Blocks.LAVA_CAULDRON) ? Fluids.LAVA
                    : state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == 3 ? Fluids.WATER : null;
            Item filled = take == null || !ModBucketItem.canScoop(stack) ? null : ModBucketItem.filled(bucket.kind(), take);
            if (filled == null) return;
            if (!level.isClientSide()) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, ModBucketItem.fill(stack, filled)));
                level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                level.playSound(null, pos, take == Fluids.LAVA ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
        // Owner N21: a half Enderite bucket takes a full cauldron of its own fluid and becomes full.
        Item fuller = com.simplebuilding.fluid.ModFluids.fullEnderite(bucket);
        boolean same = content.isSame(Fluids.LAVA) ? state.is(Blocks.LAVA_CAULDRON)
                : content.isSame(Fluids.WATER) && state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == 3;
        if (fuller != null && same) {
            if (!level.isClientSide()) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, ModBucketItem.fill(stack, fuller)));
                level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                level.playSound(null, pos, content.isSame(Fluids.LAVA) ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }
        if (!state.is(Blocks.CAULDRON)) return;
        BlockState next = content.isSame(Fluids.WATER) ? Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3)
                : content.isSame(Fluids.LAVA) ? Blocks.LAVA_CAULDRON.defaultBlockState() : null;
        if (next == null) return;
        if (!level.isClientSide()) {
            ItemStack after = player.hasInfiniteMaterials() ? stack : bucket.afterPour(stack);
            player.setItemInHand(hand, after);
            level.setBlockAndUpdate(pos, next);
            level.playSound(null, pos, content.isSame(Fluids.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (after.isEmpty()) level.playSound(null, pos, SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
