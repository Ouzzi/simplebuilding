package com.simplebuilding.modules.simplelayers.backrooms.api;

/**
 * Where a level puts the maintenance box of its doors (BACK22c §8.2). {@link #FRAME} (upper left frame block) is built by the core;
 * the other kinds are reserved for the level branches (until the core supports them they fall back to FRAME).
 */
public enum BoxPlacement {
    FRAME, BEHIND_PANEL, IN_ROOM, TRAIL
}
