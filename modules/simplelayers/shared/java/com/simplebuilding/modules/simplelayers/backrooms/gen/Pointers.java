package com.simplebuilding.modules.simplelayers.backrooms.gen;

/**
 * Pointers (BACK22 core K-R9.3, used by chalk arrows, scarecrows, signposts): the 16-step rotation (0 = south, 4 = west, 8 = north,
 * 12 = east, like Vanilla signs) that points from (x, z) toward (tx, tz). Pure.
 */
public final class Pointers {
    private Pointers() {}

    public static int toward(int x, int z, int tx, int tz) {
        if (x == tx && z == tz) return 0;
        double yaw = Math.toDegrees(Math.atan2(-(tx - x), tz - z)); // Minecraft yaw: 0 south, 90 west
        return Math.floorMod((int) Math.round(yaw / 22.5), 16);
    }
}
