package com.simplequestionary;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Questionary Table: a low oak table with an open book on a slanted stand and a lapis inkwell (model only, no
 * block entity). Use opens the menu like a crafting table; the item put in comes back when the menu closes.
 */
public class QuestionaryTableBlock extends HorizontalDirectionalBlock {
    public static final Component TITLE = Component.translatable("container.simplequestionary.questionary_table");
    private static final VoxelShape BASE = Shapes.or(Block.box(0, 11, 0, 16, 14, 16),
            Block.box(1, 0, 1, 3, 11, 3), Block.box(13, 0, 1, 15, 11, 3), Block.box(1, 0, 13, 3, 11, 15), Block.box(13, 0, 13, 15, 11, 15));
    private static final VoxelShape NORTH_SOUTH = Shapes.or(BASE, Block.box(2, 14, 4, 14, 16, 12));
    private static final VoxelShape EAST_WEST = Shapes.or(BASE, Block.box(4, 14, 2, 12, 16, 14));

    public QuestionaryTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NORTH_SOUTH : EAST_WEST;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!QuestionaryConfig.enabled) return InteractionResult.PASS;
        if (!level.isClientSide()) player.openMenu(state.getMenuProvider(level, pos));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inventory, player) -> new QuestionaryMenu(id, inventory, ContainerLevelAccess.create(level, pos)), TITLE);
    }
}
