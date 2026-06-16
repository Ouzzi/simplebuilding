package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class NetheritePistonHeadBlock extends DirectionalBlock {
    public static final MapCodec<NetheritePistonHeadBlock> CODEC = simpleCodec(NetheritePistonHeadBlock::new);

    protected static final VoxelShape EAST_HEAD_SHAPE = Block.box(0.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D); // Beispielwerte, anpassen!
    protected static final VoxelShape WEST_HEAD_SHAPE = Block.box(0.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D);

    public NetheritePistonHeadBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide() && player.isCreative()) {
            BlockPos behindPos = pos.relative(state.getValue(FACING).getOpposite());
            BlockState behindState = world.getBlockState(behindPos);
            if (behindState.is(ModBlocks.NETHERITE_PISTON)) {
                world.setBlock(behindPos, behindState.setValue(NetheriteBreakerPistonBlock.EXTENDED, false), 3);
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader world, net.minecraft.world.level.ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, net.minecraft.util.RandomSource random) {
        Direction facing = state.getValue(FACING);
        if (direction == facing.getOpposite() && !state.canSurvive(world, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader world, BlockPos pos) {
        BlockState backState = world.getBlockState(pos.relative(state.getValue(FACING).getOpposite()));
        return backState.is(ModBlocks.NETHERITE_PISTON) && backState.getValue(NetheriteBreakerPistonBlock.EXTENDED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.block(); // Vorerst voller Block als Platzhalter
    }

    @Override
    public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(ModBlocks.NETHERITE_PISTON); // Pick Block gibt die Basis
    }
}