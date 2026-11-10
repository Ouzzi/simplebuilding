package com.simplebuilding.modules.simplelayers.backrooms.gen;

/**
 * One straight flight of stairs a level reports (BACK22 core §4.3): {@code steps} steps from (x, ly, z) going up one per block in
 * 2D direction {@code dir} (0 south, 1 west, 2 north, 3 east), {@code ladder} for a ladder shaft (steps = rungs).
 */
public record StairRun(int x, int ly, int z, int dir, int steps, boolean ladder) {}
