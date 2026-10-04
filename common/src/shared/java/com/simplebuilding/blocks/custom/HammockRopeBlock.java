package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The upper layer of a hammock ({@link HammockLayout}): one block in every cell between the anchors. The two end cells
 * draw the ropes from the anchor down to the cloth's spreader, the cells between are invisible links. Uncoloured and
 * shared by all hammocks; no item, no drops, no collision. A click on it rests in the hammock like a click on the cloth.
 */
public class HammockRopeBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<HammockRopeBlock> CODEC = com.simplebuilding.version.BlockCodecs.simple(HammockRopeBlock::new);
    private static final VoxelShape END = Block.box(3.0, 0.0, 3.0, 13.0, 10.0, 13.0);
    private static final VoxelShape LINK = Block.box(6.0, 5.0, 6.0, 10.0, 9.0, 10.0);

    public HammockRopeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HammockLayout.DIAGONAL, false)
                .setValue(HammockLayout.GAP, HammockLayout.MIN_GAP).setValue(HammockLayout.INDEX, 0));
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    public MapCodec<HammockRopeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HammockLayout.DIAGONAL, HammockLayout.GAP, HammockLayout.INDEX);
    }

    /** End cells draw a rope; the others are links. */
    public static boolean isEnd(BlockState state) {
        int index = state.getValue(HammockLayout.INDEX);
        return index == 0 || index == state.getValue(HammockLayout.GAP) - 1;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return isEnd(state) ? END : LINK;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return HammockLayout.intact(level, pos, state) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        HammockLayout.check(level, pos, state);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        HammockLayout.scheduleChecks(level, pos, state);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        HammockLayout.quietlyRemoveLootOwner(level, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS_SERVER;
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && !HammockBlock.rest(serverPlayer, HammockLayout.clothHead(level, pos))) {
            HammockLayout.refuse(serverPlayer, null);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
