package com.simplebuilding.modules.simplemobs;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Force-loads chunks for a test and releases only what this mod forced: the GameTest framework force-loads the chunks of
 * every test structure itself, so a blind setChunkForced(false) unloads a neighbouring test, which then never ticks or times out.
 * The refcount only guards our own overlapping holds. We never un-force at the end: the framework force-loads a neighbour's
 * structure chunk after we did (our arenas reach into other tests' chunks), so un-forcing would stop that test for good and hang the
 * whole run until the 2400 s limit (gate d69b9a4fa). The test server is thrown away, so a few extra forced chunks cost nothing.
 */
final class ChunkHolds {
    private static final Map<Long, Integer> HELD = new HashMap<>();

    private ChunkHolds() {}

    static synchronized void hold(ServerLevel level, int x, int z) {
        long key = ChunkPos.pack(x, z);
        Integer n = HELD.get(key);
        if (n != null) {
            HELD.put(key, n + 1);
        } else if (!level.getForceLoadedChunks().contains(key)) {
            HELD.put(key, 1);
            level.setChunkForced(x, z, true);
        }
    }

    static synchronized void release(ServerLevel level, int x, int z) {
        long key = ChunkPos.pack(x, z);
        Integer n = HELD.get(key);
        if (n == null) {
            return;
        }
        if (n > 1) {
            HELD.put(key, n - 1);
        } else {
            HELD.remove(key);
        }
    }
}
