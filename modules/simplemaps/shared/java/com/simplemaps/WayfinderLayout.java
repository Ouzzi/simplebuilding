package com.simplemaps;

/**
 * Owner N38/N65: the wayfinder screen's map is as large as the (scaled) window allows, in whole 16 px grid cells. Both sides carry
 * square bookmarks (tools left, waypoints right) whose size grows with the free height (20..32 px), so a large GUI scale uses
 * the room above and below. Plain maths, no client classes, so tests can check it for every window size.
 */
public record WayfinderLayout(int cols, int rows, int left, int top, int tab) {
    public static final int CELL = 16, FRAME = 8, TITLE_H = 12, FOOTER_H = 12, MARGIN = 4;
    /** Bookmark size limits, the gap between two, the part that hides under the frame, and how far a hovered one moves out. */
    public static final int TAB_MIN = 12, TAB_MAX = 32, TAB_GAP = 2, TAB_IN = 8, TAB_HOVER = 2;
    public static final int MIN_COLS = 8, MIN_ROWS = 6, LEFT_TABS = 8, RIGHT_TABS = 1 + Waypoint.SLOTS;

    /** The layout for a screen of {@code width x height} GUI pixels. */
    public static WayfinderLayout of(int width, int height) {
        int rows = Math.max(MIN_ROWS, (height - 2 * MARGIN - TITLE_H - FOOTER_H - 2 * FRAME) / CELL);
        int panelH = TITLE_H + rows * CELL + FOOTER_H + 2 * FRAME;
        int inner = panelH - 2 * FRAME;
        int tab = Math.max(TAB_MIN, Math.min(TAB_MAX, (inner + TAB_GAP) / RIGHT_TABS - TAB_GAP));
        int sides = 2 * (tab + TAB_HOVER);
        int cols = Math.max(MIN_COLS, (width - 2 * MARGIN - sides - 2 * FRAME) / CELL);
        int panelW = cols * CELL + 2 * FRAME;
        int left = (width - (panelW + sides)) / 2 + tab + TAB_HOVER;
        int top = (height - panelH) / 2;
        return new WayfinderLayout(cols, rows, left, top, tab);
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

    /** Width of a bookmark including the part under the frame. */
    public int tabW() {
        return tab + TAB_IN;
    }

    /** x where the left bookmarks stick out (their outer end), at rest. */
    public int leftTabX() {
        return left - tab;
    }

    /** x where the right bookmarks start to show, at rest. */
    public int rightTabX() {
        return left + panelW();
    }

    /** y of bookmark {@code i} of {@code count} in a column: the column is centred on the panel's inner height. */
    public int tabY(int i, int count) {
        int inner = panelH() - 2 * FRAME;
        return top + FRAME + (inner - (count * tab + (count - 1) * TAB_GAP)) / 2 + i * (tab + TAB_GAP);
    }

    public int leftTabY(int i) {
        return tabY(i, LEFT_TABS);
    }

    public int rightTabY(int i) {
        return tabY(i, RIGHT_TABS);
    }

    /** Leftmost and rightmost pixel the screen uses (hovered bookmarks included). */
    public int minX() {
        return leftTabX() - TAB_HOVER;
    }

    public int maxX() {
        return rightTabX() + tab + TAB_HOVER;
    }
}
