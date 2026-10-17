package com.simplebuilding.modules.simpletweaks.table;

import com.simplebuilding.modules.simpletweaks.deed.DeedLayout.Rect;

/**
 * Geometry of the claim table screen, free of Minecraft classes so "everything inside the panel, nothing overlaps" is
 * checked by a JUnit test ({@code TableLayoutTest}).
 *
 * <pre>
 * title
 * info (land n / max, dimension)
 * [ map (square) ]   [ owner list ]
 * hover line         [ ............ ]
 *                    [ Expand land ]
 * [-][+][Mine]            [<][>][Close]
 * </pre>
 */
public final class TableLayout {
    public static final int MAX_W = 380, MAX_H = 230, MIN_W = 240, MIN_H = 150, EDGE = 8, GAP = 4;
    public static final int TITLE_H = 10, BUTTON_H = 20, STATUS_H = 10, ROW_H = 11, SQUARE = 20;
    /** Cells per side of the three zoom levels (radius 8, 16 and 32 chunks). */
    public static final int[] ZOOM = {16, 32, 64};

    public final Rect panel, title, info, map, status, list, buttons, expand;
    public final int width, height;
    private final int zoomW = SQUARE, modeW = 56, closeW = 50;

    private TableLayout(Rect panel, int width, int height) {
        this.panel = panel; this.width = width; this.height = height;
        int x = panel.x() + EDGE, w = panel.w() - 2 * EDGE;
        title = new Rect(x, panel.y() + 7, w, TITLE_H);
        info = new Rect(x, title.bottom() + 2, w, TITLE_H);
        int top = info.bottom() + GAP + 2;
        int buttonsY = panel.bottom() - 10 - BUTTON_H;
        buttons = new Rect(x, buttonsY, w, BUTTON_H);
        int statusY = buttonsY - GAP - STATUS_H;
        int listW = Math.max(96, Math.min(140, w * 36 / 100));
        int side = Math.max(16, Math.min(statusY - GAP - top, w - listW - GAP));
        map = new Rect(x, top, side, side);
        status = new Rect(x, statusY, side, STATUS_H);
        int listH = buttonsY - GAP - top - BUTTON_H - GAP;
        list = new Rect(panel.right() - EDGE - listW, top, listW, listH);
        expand = new Rect(list.x(), list.bottom() + GAP, listW, BUTTON_H);
    }

    /** As large as the screen allows up to {@value #MAX_W} x {@value #MAX_H}. */
    public static TableLayout of(int screenW, int screenH) {
        int w = Math.max(MIN_W, Math.min(MAX_W, screenW - 16)), h = Math.max(MIN_H, Math.min(MAX_H, screenH - 16));
        return new TableLayout(new Rect((screenW - w) / 2, (screenH - h) / 2, w, h), screenW, screenH);
    }

    /** Left group: zoom out, zoom in, "only mine". */
    public Rect zoomOut() { return new Rect(buttons.x(), buttons.y(), zoomW, BUTTON_H); }
    public Rect zoomIn() { return new Rect(buttons.x() + zoomW + GAP, buttons.y(), zoomW, BUTTON_H); }
    public Rect mine() { return new Rect(buttons.x() + 2 * (zoomW + GAP), buttons.y(), modeW, BUTTON_H); }
    /** Right group: previous page, next page, close (at the right edge). */
    public Rect close() { return new Rect(buttons.right() - closeW, buttons.y(), closeW, BUTTON_H); }
    public Rect next() { return new Rect(close().x() - GAP - zoomW, buttons.y(), zoomW, BUTTON_H); }
    public Rect prev() { return new Rect(next().x() - GAP - zoomW, buttons.y(), zoomW, BUTTON_H); }

    /** Rows of the list that fit. */
    public int rows() { return Math.max(1, (list.h() - 2) / ROW_H); }
    public Rect row(int index) { return new Rect(list.x() + 1, list.y() + 1 + index * ROW_H, list.w() - 2, ROW_H); }

    /** Pixel size of one chunk cell on the map for {@code cells} per side (at least 1). */
    public int cell(int cells) { return Math.max(1, map.w() / cells); }
    /** The drawn grid, centered in the map square. */
    public Rect grid(int cells) {
        int size = cell(cells) * cells;
        return new Rect(map.x() + (map.w() - size) / 2, map.y() + (map.h() - size) / 2, size, size);
    }
    /** Column and row of the cell under a point, or null outside the grid. */
    public int[] cellAt(double px, double py, int cells) {
        Rect g = grid(cells);
        if (!g.contains(px, py)) return null;
        int c = cell(cells);
        return new int[]{(int) ((px - g.x()) / c), (int) ((py - g.y()) / c)};
    }
}
