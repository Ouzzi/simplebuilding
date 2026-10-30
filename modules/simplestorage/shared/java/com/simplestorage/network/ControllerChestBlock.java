package com.simplestorage.network;

import com.simplestorage.chest.ChestTier;
import com.simplestorage.chest.TieredChestBlock;
import com.simplestorage.registry.StorageBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The controller chest of the chest network (BS29b-3): a chest of its step with its own slots (they belong to the network) whose
 * click opens the network window instead - every chest of the network in one list. Never a double chest. Without a network (no
 * interface on it) the window simply shows its own contents. Switched off in the config it turns into the plain chest of its step
 * ({@link NetworkSubstitutes}).
 */
public class ControllerChestBlock extends TieredChestBlock {
    private final ControllerLevel step;

    public ControllerChestBlock(ControllerLevel step, SoundEvent open, SoundEvent close, BlockBehaviour.Properties properties) {
        super(step.slotTier(), open, close, () -> StorageBlockEntities.CONTROLLER_CHEST, properties);
        this.step = step;
    }

    public ControllerLevel step() {
        return this.step;
    }

    @Override
    public String textureName() {
        return this.step.id() + "_controller";
    }

    @Override
    public Component getContainerName() {
        return Component.translatable(getDescriptionId());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ControllerChestBlockEntity(pos, state);
    }

    /** A single chest, whatever stands beside it. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!NetworkSubstitutes.enabled()) return null;
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(ChestBlock.TYPE, ChestType.SINGLE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (NetworkSubstitutes.replaceIfDisabled(level, pos, state)) return InteractionResult.SUCCESS;
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof ControllerChestBlockEntity controller) {
            serverPlayer.openMenu(controller);
            player.awardStat(this.getOpenChestStat());
            PiglinAi.angerNearbyPiglins(serverLevel, player, true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ControllerChestBlockEntity controller ? controller : null;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   @Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        NetworkSubstitutes.replaceIfDisabled(level, pos, state);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        NetworkSubstitutes.replaceIfDisabled(level, pos, state);
    }
}
