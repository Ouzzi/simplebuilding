package com.simplebuilding.blocks.custom;

import com.simplebuilding.version.BlockCodecs;

import com.simplebuilding.version.McVersion;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.util.PistonBreach;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;

public class NetheriteBreakerPistonBlock extends PistonBaseBlock {
    public static final MapCodec<NetheriteBreakerPistonBlock> CODEC = BlockCodecs.simple(NetheriteBreakerPistonBlock::new);

    public NetheriteBreakerPistonBlock(Properties settings) {
        super(false, settings);
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    @SuppressWarnings("unchecked")
    public MapCodec<PistonBaseBlock> codec() {
        return (MapCodec<PistonBaseBlock>) (Object) CODEC;
    }

    /**
     * Vanilla reiht das Ausfahr-Ereignis nur ein, wenn {@code PistonStructureResolver#resolve}
     * gelingt, und der verweigert jeden durchbrechbaren Block ({@link PistonBreach}). Ein bezahlter
     * Durchbruch braucht deshalb sein eigenes Ereignis; es haengt bewusst nicht davon ab, was hinter
     * dem Ziel liegt (Schublimit, Obsidian, Bauhoehe). Doppelte Ereignisse fasst der Server zusammen,
     * und sobald der Kolben weg ist, verwirft er die uebrigen.
     */
    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, world, pos, block, orientation, movedByPiston);
        if (!world.isClientSide()) {
            queueBreachIfPaid(world, pos, state);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, world, pos, oldState, movedByPiston);
        if (!oldState.is(state.getBlock()) && !world.isClientSide()) {
            queueBreachIfPaid(world, pos, state);
        }
    }

    private void queueBreachIfPaid(Level world, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        if (PistonBreach.isBreachable(world, pos.relative(facing)) && PistonBreach.findFuel(world, pos, facing) != null) {
            world.blockEvent(pos, this, TRIGGER_EXTEND, facing.get3DDataValue());
        }
    }

    /**
     * Der Durchbruch: steht ein durchbrechbarer Block direkt vorn und bezahlt ein Redstoneblock
     * daneben ({@link PistonBreach#findFuel}), wird {@link #breach} ausgefuehrt, dann verschwinden
     * der Redstoneblock und der Kolben selbst, alles ohne Drop. {@code moveBlocks} laeuft nie, es
     * bleibt also weder Kopf noch bewegter Block zurueck. Kein EXTENDED-Waechter: ein ausgefahrener
     * Kolben ohne Kopf bezahlt genauso mit Redstoneblock und sich selbst.
     *
     * <p>Kein NeoForge-{@code PistonEvent} feuert dafuer (Vanilla feuert es erst in
     * {@code super.triggerEvent}), so wie auch schon beim normalen Brechen.
     *
     * @return ob durchbrochen wurde; dann gibt es kein Ausfahren mehr
     */
    private boolean breachIfPaid(BlockState state, Level world, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        if (!PistonBreach.isBreachable(world, pos.relative(facing))) {
            return false;
        }
        BlockPos fuel = PistonBreach.findFuel(world, pos, facing);
        if (fuel == null) {
            return false;
        }
        breach(world, pos, facing);
        world.destroyBlock(fuel, false);
        world.destroyBlock(pos, false);
        return true;
    }

    /** Netheritkolben: nur der durchbrechbare Block direkt vorn, ohne Drop. */
    protected void breach(Level world, BlockPos pos, Direction facing) {
        world.destroyBlock(pos.relative(facing), false);
    }

    @Override
    public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int type, int data) {
        if (type == TRIGGER_EXTEND && !world.isClientSide() && breachIfPaid(state, world, pos)) {
            return false;
        }

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

                        if (targetState.getPistonPushReaction() != McVersion.PUSH_BLOCKED) {
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