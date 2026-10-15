package com.simplemaps;

/**
 * Geometry of the "Configure waypoint" dialog (owner N65), plain maths for tests: name field, search field, the icon grid like
 * the creative search tab, then the display row (preview, 3D button, unfolded cube) and the two buttons. The grid takes as many
 * rows as the window leaves room for.
 */
public record IconPickerLayout(int x, int y, int w, int h, int gridRows) {
    public static final int PAD = 8, TITLE_H = 12, FIELD_H = 16, SLOT = 18, GRID_COLS = 9, MODE_H = 48, BUTTON_H = 18;
    public static final int MIN_ROWS = 2, MAX_ROWS = 5, NET_CELL = 12;

    public static IconPickerLayout of(int screenW, int screenH) {
        int w = 2 * PAD + GRID_COLS * SLOT + 6;
        int fixed = PAD + TITLE_H + FIELD_H + 3 + FIELD_H + 4 + 4 + MODE_H + 4 + BUTTON_H + PAD;
        int rows = Math.max(MIN_ROWS, Math.min(MAX_ROWS, (screenH - 2 * 4 - fixed) / SLOT));
        int h = fixed + rows * SLOT;
        return new IconPickerLayout((screenW - w) / 2, Math.max(4, (screenH - h) / 2), w, h, rows);
    }

    public int nameX() {
        return x + PAD;
    }

    public int nameY() {
        return y + PAD + TITLE_H;
    }

    public int fieldW() {
        return w - 2 * PAD;
    }

    public int searchY() {
        return nameY() + FIELD_H + 3;
    }

    public int gridX() {
        return x + PAD + 3;
    }

    public int gridY() {
        return searchY() + FIELD_H + 4;
    }

    public int gridW() {
        return GRID_COLS * SLOT;
    }

    public int gridH() {
        return gridRows * SLOT;
    }

    public int modeY() {
        return gridY() + gridH() + 4;
    }

    /** The 3D-model button, left of the cube net. */
    public int modeButtonX() {
        return x + PAD + 36;
    }

    public int modeButtonW() {
        return 62;
    }

    public int modeButtonY() {
        return modeY() + 4;
    }

    public int netX() {
        return x + w - PAD - CubeNet.COLS * NET_CELL;
    }

    public int netY() {
        return modeY() + (MODE_H - CubeNet.ROWS * NET_CELL) / 2;
    }

    public int previewX() {
        return x + PAD;
    }

    public int previewY() {
        return modeY() + (MODE_H - 32) / 2;
    }

    public int buttonY() {
        return y + h - PAD - BUTTON_H;
    }

    public int buttonW() {
        return (w - 2 * PAD - 4) / 2;
    }

    /** Index of the grid slot at a pixel, or -1. */
    public int slotAt(double px, double py) {
        double cx = (px - gridX()) / SLOT, cy = (py - gridY()) / SLOT;
        if (cx < 0 || cy < 0 || cx >= GRID_COLS || cy >= gridRows) return -1;
        return (int) cy * GRID_COLS + (int) cx;
    }
}
