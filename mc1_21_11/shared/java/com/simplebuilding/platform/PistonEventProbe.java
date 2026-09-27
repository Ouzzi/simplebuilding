package com.simplebuilding.platform;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;

/**
 * Lets the game tests cancel the loader's <em>real</em> events for the mod's pistons (audit N5):
 * every loader's {@link PistonBreakGuard} installer also registers an ordinary listener - Fabric
 * {@code PlayerBlockBreakEvents.BEFORE}, NeoForge {@code BreakBlockEvent} and {@code PistonEvent.Pre}
 * on {@code NeoForge.EVENT_BUS}, Forge {@code PistonEvent.Pre.BUS} - that refuses exactly the
 * positions registered here, the way a protection mod would. Empty outside the tests, so the
 * listeners never refuse anything in a real game.
 *
 * <p>The installer also {@link #declare declares} which events its guard fires, so a test running
 * on every loader knows which refusal has to keep the block: Fabric fires only the break event,
 * Forge only the piston event, NeoForge both.
 */
public final class PistonEventProbe {

    private static final Set<BlockPos> REFUSED_BREAKS = ConcurrentHashMap.newKeySet();
    private static final Set<BlockPos> REFUSED_PISTONS = ConcurrentHashMap.newKeySet();
    private static volatile boolean firesPistonEvent;
    private static volatile boolean firesBreakEvent;

    private PistonEventProbe() {
    }

    /** Called by the loader's guard installer. */
    public static void declare(boolean pistonEvent, boolean breakEvent) {
        firesPistonEvent = pistonEvent;
        firesBreakEvent = breakEvent;
    }

    /** Whether this loader's guard fires a piston event ({@code PistonEvent.Pre}). */
    public static boolean firesPistonEvent() {
        return firesPistonEvent;
    }

    /** Whether this loader's guard fires a block break event with a fake player. */
    public static boolean firesBreakEvent() {
        return firesBreakEvent;
    }

    /** The loader's block break listener refuses {@code pos} until the returned runnable runs. */
    public static Runnable refuseBreakAt(BlockPos pos) {
        BlockPos key = pos.immutable();
        REFUSED_BREAKS.add(key);
        return () -> REFUSED_BREAKS.remove(key);
    }

    /** The loader's piston event listener refuses the piston at {@code pos} until the runnable runs. */
    public static Runnable refusePistonAt(BlockPos pos) {
        BlockPos key = pos.immutable();
        REFUSED_PISTONS.add(key);
        return () -> REFUSED_PISTONS.remove(key);
    }

    /** Asked by the loaders' block break listeners. */
    public static boolean breakRefused(BlockPos pos) {
        return !REFUSED_BREAKS.isEmpty() && REFUSED_BREAKS.contains(pos);
    }

    /** Asked by the loaders' piston event listeners. */
    public static boolean pistonRefused(BlockPos pos) {
        return !REFUSED_PISTONS.isEmpty() && REFUSED_PISTONS.contains(pos);
    }
}
