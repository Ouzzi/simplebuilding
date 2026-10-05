package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.blocks.entity.custom.HammockBlockEntity;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The rope layer of a hammock ({@link HammockLayout}): one block in every cell the line between the anchors crosses.
 * Invisible itself (the cloth head's renderer draws ropes and cloth); its block entity knows the hammock. Uncoloured and
 * shared by all hammocks; no item, no drops, no collision. A click on it rests in the hammock like a click on the cloth.
 */
public class HammockRopeBlock extends Block implements EntityBlock {
    public static final MapCodec<HammockRopeBlock> CODEC = com.simplebuilding.version.BlockCodecs.simple(HammockRopeBlock::new);
    private static final VoxelShape SHAPE = Block.box(4.0, 2.0, 4.0, 12.0, 10.0, 12.0);

    public HammockRopeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    public MapCodec<HammockRopeBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HammockBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** Server only: the client waits for the server (its block entity data may not have arrived yet). */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return level.isClientSide() || HammockLayout.intact(level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        HammockLayout.check(level, pos);
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
