package com.simplebuilding.modules.simplelayers.backrooms.api;

/**
 * What a level's pure generator puts at a position (BACK22 core §4.1): every level has its own enum implementing this; the core
 * only asks whether a player can stand in it and whether it is a solid block (hull rule, door placement, tests). No Minecraft type.
 */
public interface BandPiece {
    /** A player can stand in it (air, water, ladder, carpet-thin things). */
    boolean passable();

    /** A full solid block (walls, floors, hull). */
    default boolean solid() {
        return !passable();
    }
}
