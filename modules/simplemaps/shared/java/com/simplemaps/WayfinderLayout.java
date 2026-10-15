package com.simplemaps;

/**
 * Owner N38: the wayfinder screen's map is as large as the (scaled) window allows, in whole 16 px grid cells, with the
 * tool buttons on the left and the waypoint bookmarks on the right. Plain maths, no client classes, so the server
 * tests can check it for every window size.
 */
public record WayfinderLayout(int cols, int rows, int left, int top, int ribStep, int ribH) {
    public static final int CELL = 16, FRAME = 8, TITLE_H = 12, FOOTER_H = 12, MARGIN = 4;
    /** Left tool buttons: Vanilla button sprite, square. */
    public static final int BTN = 20, BTN_STEP = 22, BTN_GAP = 3;
    /** Right bookmarks: guide-style ribbons; {@code RIB_IN} hides under the frame, a hovered one moves out by {@code RIB_HOVER}. */
    public static final int RIB_W = 30, RIB_IN = 8, RIB_OUT = RIB_W - RIB_IN, RIB_HOVER = 2;
    public static final int MIN_COLS = 8, MIN_ROWS = 6, LEFT_TABS = 7, RIGHT_TABS = 1 + Waypoint.SLOTS;

    /** The layout for a screen of {@code width x height} GUI pixels. */
    public static WayfinderLayout of(int width, int height) {
        int sides = BTN + BTN_GAP + RIB_OUT + RIB_HOVER;
        int cols = Math.max(MIN_COLS, (width - 2 * MARGIN - sides - 2 * FRAME) / CELL);
        int rows = Math.max(MIN_ROWS, (height - 2 * MARGIN - TITLE_H - FOOTER_H - 2 * FRAME) / CELL);
        int panelW = cols * CELL + 2 * FRAME, panelH = TITLE_H + rows * CELL + FOOTER_H + 2 * FRAME;
        int left = (width - (panelW + sides)) / 2 + BTN + BTN_GAP;
        int top = (height - panelH) / 2;
        int inner = panelH - 2 * FRAME;
        int ribStep = Math.min(24, inner / RIGHT_TABS);
        int ribH = Math.min(20, ribStep - (ribStep > 18 ? 2 : 1));
        return new WayfinderLayout(cols, rows, left, top, ribStep, ribH);
    }

    public int mapW() {
        return cols * CELL;
    }

    public int mapH() {
        return rows * CELL;
    }

    public int panelW() {
        return mapW() + 2 * FRAME;
    }

    public int panelH() {
        return TITLE_H + mapH() + FOOTER_H + 2 * FRAME;
    }

    public int mapX() {
        return left + FRAME;
    }

    public int mapY() {
        return top + FRAME + TITLE_H;
    }

    /** x of the left tool buttons. */
    public int buttonX() {
        return left - BTN_GAP - BTN;
    }

    public int buttonY(int i) {
        return mapY() + i * BTN_STEP;
    }

    /** y of the right bookmark {@code i}: the column is centred on the panel's inner height. */
    public int ribbonY(int i) {
        int inner = panelH() - 2 * FRAME;
        return top + FRAME + (inner - RIGHT_TABS * ribStep) / 2 + i * ribStep + (ribStep - ribH) / 2;
    }

    /** Leftmost and rightmost pixel the screen uses (buttons, hovered ribbons). */
    public int minX() {
        return buttonX();
    }

    public int maxX() {
        return left + panelW() + RIB_OUT + RIB_HOVER;
    }
}
