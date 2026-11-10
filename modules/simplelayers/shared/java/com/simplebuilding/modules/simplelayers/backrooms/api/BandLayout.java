package com.simplebuilding.modules.simplelayers.backrooms.api;

import com.simplebuilding.modules.simplelayers.backrooms.gen.DoorPlacement;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A level's pure generator (BACK22 core §4.1): block piece per position in band-local height {@code ly 0..95} (0 = lowest open
 * layer; the bedrock between levels is the core's), x/z in world coordinates, a pure function of the world seed - no Minecraft
 * type, so tests and round images run it without the game. Only {@link #at} and {@link #arrival} are required.
 */
public interface BandLayout {
    int HEIGHT = 96;

    BandPiece at(long seed, int x, int ly, int z);

    /** A free standing spot {x, ly, z} near (x, z) (emergency arrival, commands, fallback). */
    int[] arrival(long seed, int x, int z);

    /**
     * Whether the whole 16-high section {@code section} (0..5) of chunk (chunkX, chunkZ) is {@link CorePiece#HULL}: the core then writes
     * it as one single-valued section (no 4096 single writes, less memory). Must agree with {@link #at}.
     */
    default boolean hullSection(long seed, int chunkX, int chunkZ, int section) {
        return false;
    }

    /** Writes the chunk (chunkX, chunkZ) into {@code out}; standard: {@link #at} per block, skipping hull sections. */
    default void fill(long seed, int chunkX, int chunkZ, BandWriter out) {
        int x0 = chunkX * 16, z0 = chunkZ * 16;
        for (int s = 0; s < HEIGHT / 16; s++) {
            if (hullSection(seed, chunkX, chunkZ, s)) continue;
            for (int ly = s * 16; ly < s * 16 + 16; ly++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) out.set(x, ly, z, at(seed, x0 + x, ly, z0 + z));
                }
            }
        }
    }

    /** Container spots {x, ly, z} (world x/z) in chunk (chunkX, chunkZ); the core places them with the level's loot. */
    default void containers(long seed, int chunkX, int chunkZ, List<int[]> out) {}

    /**
     * Where this level puts its end of a door pair in chunk (chunkX, chunkZ): {@code down} = the door to the level below (else the one
     * to the level above). Must lie so that the frame, the wall behind and the floor stay inside the chunk. Standard: the core's
     * {@link DoorPlacement} searches a free floor spot.
     */
    default @Nullable DoorEnd doorEnd(long seed, int chunkX, int chunkZ, boolean down) {
        return DoorPlacement.find(this, seed, chunkX, chunkZ, down ? 5 : 10, down ? 5 : 10, 1);
    }
}
