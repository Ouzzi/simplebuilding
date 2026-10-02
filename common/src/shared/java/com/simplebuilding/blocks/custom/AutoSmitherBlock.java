package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.blocks.entity.custom.AutoSmitherBlockEntity;
import com.simplebuilding.version.BlockCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Auto-Schmied: der Crafter des Schmiedetischs (Besitzer 2026-10-02). Verhalten wie Vanillas {@code CrafterBlock}: eine
 * steigende Redstone-Flanke schmiedet nach {@link #DELAY_TICKS} Ticks genau einmal aus Vorlage, Basis und Material; das
 * Ergebnis geht in den Container vor der Front oder fliegt als Item heraus. Ohne passendes Rezept klickt es wie der Crafter
 * (Ereignis 1050). Dauersignal loest nicht erneut aus. Komparator: belegte Eingaenge (0, 5, 10, 15).
 */
public class AutoSmitherBlock extends BaseEntityBlock {
    public static final MapCodec<AutoSmitherBlock> CODEC = BlockCodecs.simple(AutoSmitherBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    public static final BooleanProperty CRAFTING = BlockStateProperties.CRAFTING;
    /** Wie der Crafter: 4 Ticks nach der Flanke, 6 Ticks "arbeitet" die Front. */
    public static final int DELAY_TICKS = 4;
    public static final int CRAFTING_TICKS = 6;

    public AutoSmitherBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TRIGGERED, false).setValue(CRAFTING, false));
    }

    // Nur 26.2 verlangt codec(); 26.3 kennt es nicht mehr (darum ohne @Override).
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TRIGGERED, CRAFTING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite())
                .setValue(TRIGGERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AutoSmitherBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.AUTO_SMITHER_BE, AutoSmitherBlockEntity::serverTick);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        boolean powered = level.hasNeighborSignal(pos);
        boolean triggered = state.getValue(TRIGGERED);
        if (powered && !triggered) {
            level.scheduleTick(pos, this, DELAY_TICKS);
            level.setBlock(pos, state.setValue(TRIGGERED, true), Block.UPDATE_CLIENTS);
        } else if (!powered && triggered) {
            level.setBlock(pos, state.setValue(TRIGGERED, false).setValue(CRAFTING, false), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        smith(state, level, pos);
    }

    /** Ein Schmiedevorgang; liefert, ob etwas entstanden ist. */
    public static boolean smith(BlockState state, ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof AutoSmitherBlockEntity smither)) {
            return false;
        }
        AutoSmitherBlockEntity.Outcome outcome = smither.outcome(level);
        if (outcome == null) {
            level.levelEvent(1050, pos, 0);
            return false;
        }
        smither.consume(outcome);
        smither.setCraftingTicksRemaining(CRAFTING_TICKS);
        level.setBlock(pos, state.setValue(CRAFTING, true), Block.UPDATE_CLIENTS);
        ItemStack result = outcome.result().copy();
        result.onCraftedBySystem(level);
        dispense(level, pos, smither, result, state.getValue(FACING));
        return true;
    }

    /** Wie {@code CrafterBlock#dispenseItem}: erst in den Container vor der Front, der Rest fliegt heraus. */
    private static void dispense(ServerLevel level, BlockPos pos, AutoSmitherBlockEntity smither, ItemStack result, Direction facing) {
        Container into = HopperBlockEntity.getContainerAt(level, pos.relative(facing));
        ItemStack remaining = result;
        if (into != null) {
            while (!remaining.isEmpty()) {
                int before = remaining.getCount();
                remaining = HopperBlockEntity.addItem(smither, into, remaining, facing.getOpposite());
                if (before == remaining.getCount()) {
                    break;
                }
            }
        }
        if (!remaining.isEmpty()) {
            Vec3 from = Vec3.atCenterOf(pos).relative(facing, 0.7);
            DefaultDispenseItemBehavior.spawnItem(level, remaining, 6, facing, from);
            level.levelEvent(1049, pos, 0);
            level.levelEvent(2010, pos, facing.get3DDataValue());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AutoSmitherBlockEntity smither) {
            player.openMenu(smither);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof AutoSmitherBlockEntity smither ? smither.redstoneSignal() : 0;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }
}
