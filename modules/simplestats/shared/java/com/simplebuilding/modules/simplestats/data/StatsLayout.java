package com.simplebuilding.modules.simplestats.data;

/**
 * Geometry of the statistics screen. Pure Java so it can be tested for every window size: nothing leaves the screen, no two
 * parts overlap, the list always shows at least one row.
 */
public final class StatsLayout {
    public static final int ROW_H = 18, PAD = 6, TITLE_H = 24, BUTTON_H = 20, SCROLLBAR_W = 6, MARGIN = 6;
    public static final int MAX_W = 360, MAX_H = 260, MIN_W = 120, MIN_H = 90;

    public record Rect(int x, int y, int w, int h) {
        public int right() { return x + w; }
        public int bottom() { return y + h; }
        public boolean contains(double px, double py) { return px >= x && px < right() && py >= y && py < bottom(); }
        public boolean overlaps(Rect o) { return x < o.right() && o.x < right() && y < o.bottom() && o.y < bottom(); }
        public boolean inside(Rect o) { return x >= o.x && y >= o.y && right() <= o.right() && bottom() <= o.bottom(); }
    }

    public final Rect screen, panel, icon, title, list, scrollbar, done;
    public final int visibleRows;

    private StatsLayout(Rect screen, Rect panel, Rect icon, Rect title, Rect list, Rect scrollbar, Rect done) {
        this.screen = screen;
        this.panel = panel;
        this.icon = icon;
        this.title = title;
        this.list = list;
        this.scrollbar = scrollbar;
        this.done = done;
        this.visibleRows = Math.max(1, list.h() / ROW_H);
    }

    public static StatsLayout of(int screenW, int screenH) {
        Rect screen = new Rect(0, 0, screenW, screenH);
        int w = Math.max(MIN_W, Math.min(MAX_W, screenW - 2 * MARGIN));
        int h = Math.max(MIN_H, Math.min(MAX_H, screenH - 2 * MARGIN));
        Rect panel = new Rect((screenW - w) / 2, (screenH - h) / 2, w, h);
        int ix = panel.x() + PAD, iw = w - 2 * PAD;
        Rect icon = new Rect(ix, panel.y() + PAD, 16, 16);
        Rect title = new Rect(ix + 20, panel.y() + PAD, iw - 20, 16);
        int footerTop = panel.bottom() - PAD - BUTTON_H;
        int listTop = panel.y() + TITLE_H + PAD;
        int listH = Math.max(ROW_H, footerTop - PAD - listTop);
        Rect list = new Rect(ix, listTop, iw - SCROLLBAR_W - 2, listH);
        Rect bar = new Rect(list.right() + 2, listTop, SCROLLBAR_W, listH);
        int buttonW = Math.min(150, iw);
        Rect done = new Rect(panel.x() + (w - buttonW) / 2, footerTop, buttonW, BUTTON_H);
        return new StatsLayout(screen, panel, icon, title, list, bar, done);
    }

    public static int maxScroll(int rows, int visible) { return Math.max(0, rows - visible); }

    public static int clampScroll(int scroll, int rows, int visible) { return Math.max(0, Math.min(scroll, maxScroll(rows, visible))); }

    /** Scroll bar thumb height in pixels (at least 8, at most the track). */
    public static int thumbHeight(int track, int rows, int visible) {
        if (rows <= visible) return track;
        return Math.max(Math.min(8, track), (int) ((long) track * visible / rows));
    }

    /** Top of the thumb for the given scroll position. */
    public static int thumbTop(int track, int rows, int visible, int scroll) {
        int range = track - thumbHeight(track, rows, visible);
        int max = maxScroll(rows, visible);
        return max == 0 ? 0 : (int) ((long) range * clampScroll(scroll, rows, visible) / max);
    }

    /** Scroll position for a thumb dragged to {@code mouseY} (relative to the track top). */
    public static int scrollForThumb(int track, int rows, int visible, int mouseY) {
        int range = track - thumbHeight(track, rows, visible);
        int max = maxScroll(rows, visible);
        if (range <= 0 || max == 0) return 0;
        return clampScroll((int) Math.round((double) (mouseY - thumbHeight(track, rows, visible) / 2) * max / range), rows, visible);
    }
}
