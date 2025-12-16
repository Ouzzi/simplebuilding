package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class NetheritePistonHeadBlock extends FacingBlock {
    public static final MapCodec<NetheritePistonHeadBlock> CODEC = createCodec(NetheritePistonHeadBlock::new);

    // Hitbox für den Piston Arm (ungefähr wie Vanilla)
    protected static final VoxelShape EAST_HEAD_SHAPE = Block.createCuboidShape(0.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D); // Beispielwerte, anpassen!
    protected static final VoxelShape WEST_HEAD_SHAPE = Block.createCuboidShape(0.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D);
    // ... Definiere Shapes für alle Richtungen für perfekte Hitboxen, oder nutze fullCube() für den Anfang.

    public NetheritePistonHeadBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends FacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // WICHTIG: Wenn der Kopf zerstört wird, muss die Basis informiert werden (optional)
    // Oder andersrum: Wenn der Kopf abgebaut wird, droppt nichts.
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient() && player.isCreative()) {
            BlockPos behindPos = pos.offset(state.get(FACING).getOpposite());
            BlockState behindState = world.getBlockState(behindPos);
            if (behindState.isOf(ModBlocks.NETHERITE_PISTON)) {
                world.setBlockState(behindPos, behindState.with(NetheriteBreakerPistonBlock.EXTENDED, false), 3);
            }
        }
        return super.onBreak(world, pos, state, player);
    }

    // Wenn der Block in der Luft schwebt (Basis weg), zerstören
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, net.minecraft.world.WorldView world, net.minecraft.world.tick.ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, net.minecraft.util.math.random.Random random) {
        Direction facing = state.get(FACING);
        if (direction == facing.getOpposite() && !state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public boolean canPlaceAt(BlockState state, net.minecraft.world.WorldView world, BlockPos pos) {
        BlockState backState = world.getBlockState(pos.offset(state.get(FACING).getOpposite()));
        return backState.isOf(ModBlocks.NETHERITE_PISTON) && backState.get(NetheriteBreakerPistonBlock.EXTENDED);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // Hier korrekte VoxelShape zurückgeben je nach Facing, damit man nicht durchlaufen kann
        return VoxelShapes.fullCube(); // Vorerst voller Block als Platzhalter
    }

    @Override
    public ItemStack getPickStack(net.minecraft.world.WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(ModBlocks.NETHERITE_PISTON); // Pick Block gibt die Basis
    }
}