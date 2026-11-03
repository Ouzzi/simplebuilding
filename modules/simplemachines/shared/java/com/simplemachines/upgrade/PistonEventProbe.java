package com.simplemachines.upgrade;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;

/**
 * Lets the game tests cancel the loader's <em>real</em> block break event for the piston upgrades (audit N5): the Fabric guard
 * registers an ordinary {@code PlayerBlockBreakEvents.BEFORE} listener on first use that refuses exactly the positions registered
 * here, the way a protection mod would. Empty outside the tests.
 */
public final class PistonEventProbe {
    private static final Set<BlockPos> REFUSED_BREAKS = ConcurrentHashMap.newKeySet();
    private static volatile boolean firesBreakEvent;
    private static volatile Runnable breakActivator;

    private PistonEventProbe() {}

    /** The loader hands in its break listener registration; it runs once, with the first {@link #refuseBreakAt}. */
    public static void activateBreakOnFirstUse(Runnable register) {
        breakActivator = register;
    }

    /** Called by the loader's guard installer: whether it fires a block break event with a fake player. */
    public static void declare(boolean breakEvent) {
        firesBreakEvent = breakEvent;
    }

    public static boolean firesBreakEvent() {
        return firesBreakEvent;
    }

    /** Fabric has no piston event (NeoForge/Forge would fire {@code PistonEvent.Pre}); the shared tests ask. */
    public static boolean firesPistonEvent() {
        return false;
    }

    /** No piston event on Fabric: nothing to refuse; the returned runnable does nothing. */
    public static Runnable refusePistonAt(BlockPos pos) {
        return () -> {};
    }

    /** The loader's block break listener refuses {@code pos} until the returned runnable runs. */
    public static Runnable refuseBreakAt(BlockPos pos) {
        Runnable register = breakActivator;
        if (register != null) {
            breakActivator = null;
            register.run();
        }
        BlockPos key = pos.immutable();
        REFUSED_BREAKS.add(key);
        return () -> REFUSED_BREAKS.remove(key);
    }

    /** Asked by the loader's block break listener. */
    public static boolean breakRefused(BlockPos pos) {
        return !REFUSED_BREAKS.isEmpty() && REFUSED_BREAKS.contains(pos);
    }
}
