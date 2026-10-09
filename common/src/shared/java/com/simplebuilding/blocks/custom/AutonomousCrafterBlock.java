package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.blocks.entity.custom.AutonomousCrafterBlockEntity;
import com.simplebuilding.version.BlockCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Autonomous Crafter (owner 2026-10-09, queue N26): a crafter without a front that crafts by itself into the hopper
 * below it (see {@link AutonomousCrafterBlockEntity}). {@link #TRIGGERED} = a redstone signal reaches it, which stops
 * it; {@link #CRAFTING} lights the faces for a moment after each craft.
 */
public class AutonomousCrafterBlock extends BaseEntityBlock {
    public static final MapCodec<AutonomousCrafterBlock> CODEC = BlockCodecs.simple(AutonomousCrafterBlock::new);
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    public static final BooleanProperty CRAFTING = BlockStateProperties.CRAFTING;

    public AutonomousCrafterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TRIGGERED, false).setValue(CRAFTING, false));
    }

    // Nur 26.2 verlangt codec(); 26.3 kennt es nicht mehr (darum ohne @Override).
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TRIGGERED, CRAFTING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(TRIGGERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AutonomousCrafterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.AUTONOMOUS_CRAFTER_BE, AutonomousCrafterBlockEntity::serverTick);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(TRIGGERED)) {
            level.setBlock(pos, state.setValue(TRIGGERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AutonomousCrafterBlockEntity crafter) {
            player.openMenu(crafter);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof AutonomousCrafterBlockEntity crafter ? crafter.redstoneSignal() : 0;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }
}
