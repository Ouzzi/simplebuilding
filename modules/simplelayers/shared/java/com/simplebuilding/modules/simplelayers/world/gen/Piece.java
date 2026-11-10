package com.simplebuilding.modules.simplelayers.world.gen;

/**
 * What a Backrooms generator puts at a position (BACK18): a name per block state, no Minecraft type, so the generators run without
 * the game (round images, path checks). {@code BackroomsBlocks} maps every piece to its block state in one place.
 */
public enum Piece {
    AIR, BEDROCK, RIFT,
    // Level 0 (Lobby) and the exit level
    CARPET, WALLPAPER, STRIPED_WALLPAPER, DOTTED_WALLPAPER, CEILING_TILE, STAINED_CEILING_TILE,
    NEON_ON, NEON_FLICKER, NEON_BROKEN, EXIT_SIGN, EXIT_DOOR_LOWER, EXIT_DOOR_UPPER,
    LEVEL_DOOR_LOWER, LEVEL_DOOR_UPPER, SEALED_DOOR_LOWER, SEALED_DOOR_UPPER,
    COBWEB, DESK, CHAIR_N, CHAIR_S, LOGS_X, CRAFTING_TABLE, STEP_E, STEP_S,
    // BACK20: heights and rooms
    SLAB, DECK, STONE_SLAB, LADDER_N, SINK, COUNTER, SMOKER_N, SMOKER_S, RUG, POT, BOOKSHELF,
    END_FLOOR, END_WALL, END_CEILING,
    // Level 1 (warehouse)
    CONCRETE_FLOOR, PILLAR, CONCRETE_WALL, CONCRETE_CEILING, CRATE_OAK, CRATE_SPRUCE, SHELF, RACK_POST, MARKING, HAZARD_BLACK,
    // Level 2 (pipes)
    PIPE_WALL, PIPE_FLOOR, PIPE_CEILING, PIPE_X, PIPE_Z, PIPE_Y, COPPER_BOX, BULB, GRATE, LADDER_E, LADDER_S, LEVER_N, LEVER_E, LEVER_W,
    // Poolrooms
    TILE, TILE_FLOOR, POOL_TILE, WATER, SEA_LANTERN, ARCH_E, ARCH_W, ARCH_N, ARCH_S, TILE_COLUMN,
    // Entrance shaft (Overworld)
    PAD, GRAVEL, STONE, STONE_BRICKS, CRACKED_STONE_BRICKS, POST, WARNING_LAMP, TAPE_X, TAPE_Z,
    SIGN_N, SIGN_E, SIGN_S, SIGN_W, GLITCHED_BEDROCK, CHAIN;

    public boolean isAir() {
        return this == AIR;
    }

    /** Whether a player can stand in it (no collision worth mentioning): air, water, rift, cobweb, lever, ladder. */
    public boolean passable() {
        return switch (this) {
            case AIR, WATER, RIFT, COBWEB, LEVER_N, LEVER_E, LEVER_W, LADDER_E, LADDER_S, LADDER_N, CHAIN, RUG -> true;
            default -> false;
        };
    }
}
