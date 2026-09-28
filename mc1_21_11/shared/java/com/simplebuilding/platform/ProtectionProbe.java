package com.simplebuilding.platform;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;

/**
 * Lets the game tests play a claim mod: every loader's {@link BuildGuard} installer also registers
 * ordinary listeners on the loader's real events - Fabric {@code PlayerBlockBreakEvents.BEFORE},
 * NeoForge/Forge the block break event and {@code BlockEvent.EntityPlaceEvent} - that refuse
 * exactly the positions registered here, with any player. Empty outside the tests, so the
 * listeners never refuse anything in a real game.
 *
 * <p>Break and place share one set: on Fabric the placement check fires the break event, so a test
 * that runs on every loader can only rely on "this position is protected".
 */
public final class ProtectionProbe {

    private static final Set<BlockPos> REFUSED = ConcurrentHashMap.newKeySet();

    private ProtectionProbe() {
    }

    /** The loader's break and place listeners refuse {@code pos} until the returned runnable runs. */
    public static Runnable refuseAt(BlockPos pos) {
        BlockPos key = pos.immutable();
        REFUSED.add(key);
        return () -> REFUSED.remove(key);
    }

    /** Asked by the loaders' listeners. */
    public static boolean refused(BlockPos pos) {
        return !REFUSED.isEmpty() && REFUSED.contains(pos);
    }
}
