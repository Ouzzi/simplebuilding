package com.simplemaps;

/**
 * Which side of a block a waypoint shows (owner N65): {@link #MODEL} = the 3D block model, 1..6 = one side. The unfolded cube
 * ("net") is a cross of 4 x 3 cells with the front (north) in the middle of the second row; plain maths so JUnit can check it.
 */
public final class CubeNet {
    public static final int MODEL = 0, NORTH = 1, EAST = 2, SOUTH = 3, WEST = 4, UP = 5, DOWN = 6, FACES = 6;
    /** The default side: the front. */
    public static final int FRONT = NORTH;
    public static final int COLS = 4, ROWS = 3;
    /** Net cell (column, row) of each side, index = face - 1. */
    private static final int[][] CELLS = {{1, 1}, {2, 1}, {3, 1}, {0, 1}, {1, 0}, {1, 2}};

    private CubeNet() {}

    public static boolean validFace(int face) {
        return face >= MODEL && face <= DOWN;
    }

    public static int column(int face) {
        return CELLS[face - 1][0];
    }

    public static int row(int face) {
        return CELLS[face - 1][1];
    }

    /** The side at a cell, or 0 when the cell lies outside the cross. */
    public static int faceAtCell(int column, int row) {
        for (int f = NORTH; f <= DOWN; f++) if (column(f) == column && row(f) == row) return f;
        return 0;
    }

    /** The side under a pixel of a net drawn at {@code x, y} with square cells of {@code cell} px; 0 = none. */
    public static int faceAt(int x, int y, int cell, double px, double py) {
        double cx = (px - x) / cell, cy = (py - y) / cell;
        if (cx < 0 || cy < 0 || cx >= COLS || cy >= ROWS) return 0;
        return faceAtCell((int) cx, (int) cy);
    }
}
