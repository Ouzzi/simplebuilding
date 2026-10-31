package com.simpleblocks.snow;

import com.simpleblocks.SimpleBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * SN30-1: a snow layer used on a fence, flower or grass ({@link SnowBlocks#SNOWLOGGABLE}) makes a {@link SnowloggedBlock} that
 * holds it, if a snow layer could stand there (full face below); used on such a cell it adds a layer (up to
 * {@link SnowloggedBlock#MAX_LAYERS}). Loader adapters call {@link #use} from their block-use event (Fabric
 * {@code UseBlockCallback}); switch {@link SnowConfig#snowlogging}.
 */
public final class Snowlogging {
    private Snowlogging() {}

    /** A block a snow layer may share its cell with: tagged, no fluid, no block entity of its own. */
    public static boolean canHold(BlockState state) {
        return state.is(SnowBlocks.SNOWLOGGABLE) && state.getFluidState().isEmpty() && !state.hasBlockEntity()
                && !(state.getBlock() instanceof SnowloggedBlock);
    }

    public static InteractionResult use(Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.SNOW) || !SnowConfig.snowlogging || SnowBlocks.SNOWLOGGED == null || !SimpleBlocks.on() || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        if (!player.mayUseItemAt(pos, hit.getDirection(), stack)) {
            return InteractionResult.PASS;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(SnowBlocks.SNOWLOGGED)) {
            int layers = state.getValue(SnowloggedBlock.LAYERS);
            if (layers >= SnowloggedBlock.MAX_LAYERS) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(SnowloggedBlock.LAYERS, layers + 1), Block.UPDATE_ALL);
                placed(level, player, pos, stack);
            }
            return InteractionResult.SUCCESS;
        }
        if (!canHold(state) || !SnowloggedBlock.snowSurvives(level, pos)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            snowlog(level, pos, state);
            placed(level, player, pos, stack);
        }
        return InteractionResult.SUCCESS;
    }

    /** Puts a snowlogged cell holding {@code held} at {@code pos} (the caller checked {@link #canHold}). */
    public static void snowlog(Level level, BlockPos pos, BlockState held) {
        BlockState cell = SnowBlocks.SNOWLOGGED.defaultBlockState().setValue(SnowloggedBlock.FENCE, SnowloggedBlock.Fence.of(held));
        level.setBlock(pos, cell, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof SnowloggedBlockEntity be) {
            be.setHeld(held);
        }
    }

    private static void placed(Level level, Player player, BlockPos pos, ItemStack stack) {
        SoundType sound = Blocks.SNOW.defaultBlockState().getSoundType();
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(player, GameEvent.BLOCK_PLACE, pos);
        stack.consume(1, player);
    }
}
