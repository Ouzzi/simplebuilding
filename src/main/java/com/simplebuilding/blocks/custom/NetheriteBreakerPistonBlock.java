package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;

public class NetheriteBreakerPistonBlock extends FacingBlock {
    public static final MapCodec<NetheriteBreakerPistonBlock> CODEC = createCodec(NetheriteBreakerPistonBlock::new);
    public static final BooleanProperty EXTENDED = Properties.EXTENDED; // Wir nutzen EXTENDED statt TRIGGERED für die Optik

    public NetheriteBreakerPistonBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(EXTENDED, false));
    }

    @Override
    protected MapCodec<? extends FacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        if (!world.isClient()) {
            boolean hasPower = world.isReceivingRedstonePower(pos) || world.isReceivingRedstonePower(pos.up());
            boolean isExtended = state.get(EXTENDED);

            if (hasPower && !isExtended) {
                // Ausfahren
                extend(world, pos, state);
            } else if (!hasPower && isExtended) {
                // Einfahren
                retract(world, pos, state);
            }
        }
    }

    private void extend(World world, BlockPos pos, BlockState state) {
        Direction facing = state.get(FACING);
        BlockPos targetPos = pos.offset(facing);
        BlockState targetState = world.getBlockState(targetPos);

        // 1. Prüfen ob wir ausfahren können (Ist der Block vor uns zerstörbar oder Luft?)
        // Härte >= 0 heißt zerstörbar. -1 ist Bedrock.
        if (targetState.getHardness(world, targetPos) >= 0) {

            // 2. Block zerstören (Breaker Funktion)
            if (!targetState.isAir()) {
                world.breakBlock(targetPos, true); // true = drops
            }

            // 3. Head Block platzieren (Verhindert Platzieren neuer Blöcke & rendert Head)
            // Wir prüfen nochmal, ob es jetzt Luft ist (sollte es sein)
            if (world.getBlockState(targetPos).isReplaceable()) {
                world.setBlockState(targetPos, ModBlocks.NETHERITE_PISTON_HEAD.getDefaultState().with(FacingBlock.FACING, facing));
            }

            // 4. Status auf Extended setzen (ändert Textur der Basis)
            world.setBlockState(pos, state.with(EXTENDED, true), 3);
        }
    }

    private void retract(World world, BlockPos pos, BlockState state) {
        Direction facing = state.get(FACING);
        BlockPos targetPos = pos.offset(facing);
        BlockState targetState = world.getBlockState(targetPos);

        // 1. Head Block entfernen, falls er da ist
        if (targetState.isOf(ModBlocks.NETHERITE_PISTON_HEAD)) {
            world.setBlockState(targetPos, Blocks.AIR.getDefaultState(), 3);
        }

        // 2. Status auf eingefahren setzen
        world.setBlockState(pos, state.with(EXTENDED, false), 3);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, EXTENDED);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getPlayerLookDirection().getOpposite()).with(EXTENDED, false);
    }
}