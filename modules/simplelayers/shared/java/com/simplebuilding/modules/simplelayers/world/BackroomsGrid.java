package com.simplebuilding.modules.simplelayers.world;

import com.simplebuilding.modules.simplelayers.world.gen.LobbyGen;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Level 0 of the Backrooms as block states (concept §2.2 variant B, LAYERS10/13). Since BACK18 the grid itself lives in the
 * Minecraft-free {@link LobbyGen} (same hash, same layout, plus exits, upper floor, stairwells and noclip spots); this class keeps
 * the block-state view and the old constants.
 */
public final class BackroomsGrid {
    public static final int CELL = LobbyGen.CELL, FLOOR_Y = LobbyGen.FLOOR_Y, CEILING_Y = LobbyGen.CEILING_Y, TOP_Y = LobbyGen.TOP_Y;
    /** First bedrock above the upper floor and the top of the hull (the dimension is 64 high). */
    public static final int HULL_FROM_Y = TOP_Y + LobbyGen.UPPER, HULL_TOP_Y = LobbyGen.HULL_TOP_Y;

    private BackroomsGrid() {}

    /** The block at (x, y, z) of Level 0. */
    public static BlockState at(long seed, int x, int y, int z) {
        return BackroomsBlocks.state(LobbyGen.at(LobbyGen.LOBBY, seed, x, y, z));
    }

    /** Centre of the cell holding (x, z) (travellers arrive there, see {@link LobbyGen#arrival}). */
    public static int cellCentre(int coordinate) {
        return Math.floorDiv(coordinate, CELL) * CELL + 4;
    }
}
