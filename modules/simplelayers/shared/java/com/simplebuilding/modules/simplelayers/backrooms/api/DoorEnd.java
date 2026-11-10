package com.simplebuilding.modules.simplelayers.backrooms.api;

/**
 * One end of a door pair inside a level (BACK22 core §5.4): the door's lower half at (x, ly, z) in world x/z and band-local height,
 * {@code facing} = walking direction into the door as 2D data value (0 south, 1 west, 2 north, 3 east), {@code width} 1 or 2
 * (2 = double door, (x, z) = its left leaf as seen when walking in). The traveller stands/arrives one step before the door.
 */
public record DoorEnd(int x, int ly, int z, int facing, int width) {
    public static final int[] DX = {0, -1, 0, 1}, DZ = {1, 0, -1, 0};

    public int frontX() {
        return x - DX[facing];
    }

    public int frontZ() {
        return z - DZ[facing];
    }

    /** x of leaf i (0 = left; right of the walker = clockwise = next 2D value). */
    public int leafX(int i) {
        return x + DX[(facing + 1) & 3] * i;
    }

    public int leafZ(int i) {
        return z + DZ[(facing + 1) & 3] * i;
    }
}
