package com.simpletrims.placed;

import com.simpletrims.TrimsRegistry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A placed trim template (moved from SimpleBuilding's placed smithing template, {@link PlacedTemplates}): flat on the
 * floor, on a wall or under a ceiling, oriented like a button through {@link AttachFace} and the look direction. The
 * template (with all components) lives in the {@link PlacedTrimTemplateBlockEntity}; the client draws it as a punched
 * plate ({@code PlacedTrimTemplateRenderer}), the block model only carries the particle texture.
 *
 * <p>Waterloggable like a sign; breaking, explosions, pistons and a removed support give back exactly the stored stack
 * ({@link #getDrops}, no loot table). With the template tool in the main hand and a catalyst in the off hand it never
 * breaks; a right click is then a strike of the upgrade ({@link #useItemOn}).
 */
public class PlacedTrimTemplateBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock, SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    /** A hammer switched the 3D model ({@link PlacedSolids}) to the flat plate (owner D7). */
    public static final BooleanProperty FLAT = BooleanProperty.create("flat");

    public PlacedTrimTemplateBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FACE, AttachFace.FLOOR).setValue(WATERLOGGED, false).setValue(FLAT, false));
    }

    /** Direction of the visible side: away from the block it lies on. */
    public static Direction normal(BlockState state) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            case WALL -> state.getValue(FACING);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING, WATERLOGGED, FLAT);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        return state.setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    /** Exactly the drawn pixels of the plate ({@link PlacedPlate}); without block entity the whole plate. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        ItemStack stored = PlacedTemplates.templateAt(level, pos);
        return PlacedPlate.shape(stored.isEmpty() ? null : stored.getItem(), state.getValue(FACE), state.getValue(FACING), state.getValue(FLAT));
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlacedTrimTemplateBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != TrimsRegistry.PLACED_TEMPLATE_BE) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<PlacedTrimTemplateBlockEntity>) PlacedTrimTemplateBlockEntity::serverTick;
    }

    /** Template tool in the main hand + catalyst in the off hand: one strike (held right click repeats). */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!PlacedTemplates.isToolTarget(level, pos, player)) {
            return PlacedTemplates.toggleFlat(level, pos, player, stack, hand) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (hand == InteractionHand.MAIN_HAND && level instanceof net.minecraft.server.level.ServerLevel server) {
            PlacedTemplates.strike(server, pos, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof PlacedTrimTemplateBlockEntity placed && !placed.getTemplate().isEmpty()) {
            return List.of(placed.getTemplate().copy());
        }
        return List.of();
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return PlacedTemplates.templateAt(level, pos).copy();
    }

    /** With tool and catalyst in hand the template does not break (the strikes are right clicks). */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (PlacedTemplates.isToolStance(player)) return 0.0F;
        return super.getDestroyProgress(state, player, level, pos);
    }
}
