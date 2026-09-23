package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

public class NetheriteBreakerPistonBlock extends PistonBaseBlock {
    public static final MapCodec<NetheriteBreakerPistonBlock> CODEC = simpleCodec(NetheriteBreakerPistonBlock::new);

    public NetheriteBreakerPistonBlock(Properties settings) {
        super(false, settings);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<PistonBaseBlock> codec() {
        return (MapCodec<PistonBaseBlock>) (Object) CODEC;
    }

    @Override
    public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int type, int data) {
        // type 0 = Ausfahren. Nur brechen, wenn Vanilla gleich danach auch wirklich ausfaehrt: super
        if (type == 0 && (world.isClientSide() || hasVanillaExtendSignal(world, pos, state.getValue(FACING)))) {
            // Wir prüfen nur beim Ausfahren
            if (!state.getValue(EXTENDED)) {
                Direction facing = state.getValue(FACING);
                BlockPos targetPos = pos.relative(facing);
                BlockState targetState = world.getBlockState(targetPos);
                if (!targetState.isAir() && targetState.getDestroySpeed(world, targetPos) >= 0) {
                    int power = world.getBestNeighborSignal(pos);
                    float breakThreshold = (power / 15.0f) * 50.0f;
                    float blockHardness = targetState.getDestroySpeed(world, targetPos);

                    // Nur brechen, wenn das Signal stark genug ist!
                    if (blockHardness <= breakThreshold) {

                        if (targetState.getPistonPushReaction() != PushReaction.BLOCK) {
                            world.destroyBlock(targetPos, true);
                            if (!world.isClientSide()) {
                                world.playSound(null, pos, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.BLOCKS, 0.5f, 0.8f);
                            }
                        }
                    }
                }
            }
        }

        return super.triggerEvent(state, world, pos, type, data);
    }

    /**
     * Vanillas eigene Nachpruefung aus {@code PistonBaseBlock#triggerEvent} (dort private
     * {@code getNeighborSignal}): Signal von jeder Seite ausser der Schubrichtung, von unten, oder
     * ueber die Quasi-Konnektivitaet von oben. {@code super.triggerEvent} verwirft ein
     * Ausfahr-Ereignis ohne dieses Signal - der Brecher darf dann auch nichts zerstoert haben. Bis
     * 2026-09 brach er vorher, mit {@code getBestNeighborSignal}, das die Schubrichtung mitzaehlt:
     * ein Signal, das zwischen Einreihen und Ausfuehren des Block-Ereignisses verschwand, oder ein
     * Block davor, der selbst die einzige Signalquelle war, kostete den Block, ohne dass der Kolben
     * je ausfuhr.
     */
    private static boolean hasVanillaExtendSignal(Level level, BlockPos pos, Direction push) {
        for (Direction direction : Direction.values()) {
            if (direction != push && level.hasSignal(pos.relative(direction), direction)) {
                return true;
            }
        }
        if (level.hasSignal(pos, Direction.DOWN)) {
            return true;
        }
        BlockPos above = pos.above();
        for (Direction direction : Direction.values()) {
            if (direction != Direction.DOWN && level.hasSignal(above.relative(direction), direction)) {
                return true;
            }
        }
        return false;
    }
}