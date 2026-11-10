package com.simplebuilding.modules.simplelayers.backrooms.api;

/**
 * Pieces the core itself writes into every level (BACK22 core §4.1): air, the level's hull skin (solid filler next to the bedrock
 * between levels, door frames, the wall behind a door) and the door chamber's floor. Each level maps them in its palette.
 */
public enum CorePiece implements BandPiece {
    AIR, HULL, DOOR_SLOT;

    @Override
    public boolean passable() {
        return this == AIR || this == DOOR_SLOT;
    }
}
