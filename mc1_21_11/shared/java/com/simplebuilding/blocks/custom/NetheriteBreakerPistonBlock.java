package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.platform.PlatformServices;
import com.simplebuilding.util.PistonBoreEffects;
import com.simplebuilding.util.PistonBreach;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;

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
     * <p>Jeder Block, den der Durchbruch zerstoert, geht vorher durch
     * {@link PlatformServices#mayPistonBreak} (Schutz-Mods, {@code PistonEvent.Pre}, Bruch-Ereignis
     * mit Fake-Spieler). Verweigert der Wächter schon den vordersten Block, passiert nichts: weder
     * der Redstoneblock noch der Kolben werden verbraucht.
     *
     * @return ob durchbrochen wurde; dann gibt es kein Ausfahren mehr
     */
    private boolean breachIfPaid(BlockState state, ServerLevel world, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        if (!PistonBreach.isBreachable(world, pos.relative(facing))) {
            return false;
        }
        BlockPos fuel = PistonBreach.findFuel(world, pos, facing);
        if (fuel == null) {
            return false;
        }
        if (!breach(world, pos, facing)) {
            return false;
        }
        world.destroyBlock(fuel, false);
        world.destroyBlock(pos, false);
        return true;
    }

    /**
     * Netheritkolben: nur der durchbrechbare Block direkt vorn, ohne Drop, mit Partikeln und Klang
     * ({@link PistonBoreEffects}).
     *
     * @return ob der vorderste Block zerstoert wurde; {@code false}, wenn der Wächter ablehnte
     */
    protected boolean breach(ServerLevel world, BlockPos pos, Direction facing) {
        BlockPos front = pos.relative(facing);
        if (!mayBreak(world, pos, facing, front)) {
            return false;
        }
        PistonBoreEffects.destroy(world, front, false);
        return true;
    }

    /** Fragt den Plattform-Wächter ({@link PlatformServices#mayPistonBreak}) für einen Block. */
    protected static boolean mayBreak(ServerLevel world, BlockPos piston, Direction facing, BlockPos target) {
        return PlatformServices.mayPistonBreak(world, piston, facing, target, world.getBlockState(target));
    }

    /**
     * Ausfahren mit Brechen, nur auf dem Server:
     * <ol>
     *   <li>ein bezahlter Durchbruch geht vor (dann kein Ausfahren);</li>
     *   <li>sonst wird nur gebrochen, wenn Vanilla gleich danach wirklich ausfaehrt
     *       ({@link #hasVanillaExtendSignal}), der Block hart genug fuer das Signal ist und der
     *       Plattform-Wächter zustimmt - erst dann, direkt vor {@code super.triggerEvent}. Nach dem
     *       Brechen liegt vorn Luft, {@code moveBlocks} kann also nicht mehr scheitern; ein
     *       NeoForge/Forge-{@code PistonEvent.Pre}, das den Zug absagt, hat der Wächter schon vorher
     *       gefragt.</li>
     * </ol>
     * Der Client bricht nie selbst (audit 2026-09-26 #47: Geisterbloecke, wenn Client und Server
     * verschieden entschieden). Damit er beim Nachspielen des Ausfahr-Ereignisses den Block nicht
     * mitschiebt, schickt {@link PistonBoreEffects#destroy} die Entfernung sofort, also vor dem
     * Block-Ereignis-Paket.
     */
    @Override
    public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int type, int data) {
        if (world instanceof ServerLevel server) {
            if (type == TRIGGER_EXTEND && breachIfPaid(state, server, pos)) {
                return false;
            }
            if (type == TRIGGER_EXTEND && !state.getValue(EXTENDED)
                    && hasVanillaExtendSignal(server, pos, state.getValue(FACING))) {
                Direction facing = state.getValue(FACING);
                BlockPos targetPos = pos.relative(facing);
                BlockState targetState = server.getBlockState(targetPos);
                if (!targetState.isAir()) {
                    float blockHardness = targetState.getDestroySpeed(server, targetPos);
                    float breakThreshold = (server.getBestNeighborSignal(pos) / 15.0f) * 50.0f;
                    if (blockHardness >= 0 && blockHardness <= breakThreshold
                            && targetState.getPistonPushReaction() != PushReaction.BLOCK
                            && mayBreak(server, pos, facing, targetPos)) {
                        // Mit Beute, Bruchpartikeln, Abbauklang und dem Bohrklang der Mod.
                        PistonBoreEffects.destroy(server, targetPos, true);
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