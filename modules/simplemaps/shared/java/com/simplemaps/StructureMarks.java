package com.simplemaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Feature 3: structures the holder has entered or is close to (a piece within {@link #SEEN} blocks, sampled around
 * the player) are remembered on the map. Only structures that already exist in generated chunks are found, so
 * nothing is ever revealed ahead of the player. Runs every {@link #INTERVAL} ticks while the map is in a hand.
 */
public final class StructureMarks {
    public static final int INTERVAL = 40, SEEN = 24;
    private static final int[][] RING = {{0, 0}, {SEEN, 0}, {-SEEN, 0}, {0, SEEN}, {0, -SEEN},
            {SEEN / 2, SEEN / 2}, {-SEEN / 2, SEEN / 2}, {SEEN / 2, -SEEN / 2}, {-SEEN / 2, -SEEN / 2}};

    private StructureMarks() {}

    public static void step(ServerLevel level, Entity holder, WayfinderData data) {
        if (level.getGameTime() % INTERVAL != 0) return;
        BlockPos at = holder.blockPosition();
        for (int[] d : RING) scanAt(level, at.offset(d[0], 0, d[1]), data);
    }

    /** Records the structure with a piece at this position, if any. Public for the GameTest. */
    public static boolean scanAt(ServerLevel level, BlockPos pos, WayfinderData data) {
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, holder -> true);
        if (start == null || !start.isValid()) return false;
        var key = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(start.getStructure());
        if (key == null) return false;
        BoundingBox box = start.getBoundingBox();
        return data.addMark(key.toString(), box.getCenter().getX(), box.getCenter().getZ());
    }
}
