package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FacingBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable; // Wichtig für 1.21

public class NetheriteBreakerPistonBlock extends FacingBlock {

    // 1. CODEC Definition (Wichtig für 1.21 Registries)
    public static final MapCodec<NetheriteBreakerPistonBlock> CODEC = createCodec(NetheriteBreakerPistonBlock::new);

    public NetheriteBreakerPistonBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends FacingBlock> getCodec() {
        return CODEC;
    }

    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable BlockPos sourcePos, boolean notify) {
        if (!world.isClient()) {
            boolean hasPower = world.isReceivingRedstonePower(pos);
            if (hasPower) {
                world.scheduleBlockTick(pos, this, 1);
            }
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        Direction facing = state.get(FACING);
        BlockPos targetPos = pos.offset(facing);
        BlockState targetState = world.getBlockState(targetPos);

        if (!targetState.isAir() && targetState.getHardness(world, targetPos) >= 0) {
            // Zerstört Block und droppt Items
            world.breakBlock(targetPos, true);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getPlayerLookDirection().getOpposite());
    }
}