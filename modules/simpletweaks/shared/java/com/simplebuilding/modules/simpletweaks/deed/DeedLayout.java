package com.simplebuilding.modules.simpletweaks.deed;

import java.util.ArrayList;
import java.util.List;

/**
 * Geometry of the claim deed screens, free of Minecraft classes so "everything inside the sheet, nothing overlaps"
 * is checked by a JUnit test ({@code DeedLayoutTest}) without booting the game.
 *
 * <pre>
 * title / owner line
 * [ code field ........................ ][scrollbar]
 * status ............................... (counter drawn by the field, right)
 * Online: [name +] [name +] ...  [>]
 * [?] [Copy] [Paste]            [Cancel] [Done]
 * </pre>
 */
public final class DeedLayout {
    public record Rect(int x, int y, int w, int h) {
        public int right() { return x + w; }
        public int bottom() { return y + h; }
        public boolean overlaps(Rect o) { return x < o.right() && o.x < right() && y < o.bottom() && o.y < bottom(); }
        public boolean contains(Rect o) { return o.x >= x && o.y >= y && o.right() <= right() && o.bottom() <= bottom(); }
        public boolean contains(double px, double py) { return px >= x && px < right() && py >= y && py < bottom(); }
    }

    public static final int MAX_W = 380, MAX_H = 250, MIN_W = 240, MIN_H = 150, EDGE = 8, GAP = 4;
    public static final int TITLE_H = 10, ROW_H = 18, BUTTON_H = 20, STATUS_H = 10, SCROLLBAR = 6;
    public static final int HELP_W = 20, COUNTER_W = 78;
    /** Rows below the code field: status, chips, buttons (plus gaps and the bottom frame). */
    private static final int BOTTOM = STATUS_H + GAP + ROW_H + GAP + BUTTON_H + 10;

    public final Rect panel, title, info, code, status, chipRow, buttons;
    /** The bounds the editable field itself gets (leaves the scrollbar column free). */
    public final Rect field;
    public final int width, height;
    private final boolean edit;

    private DeedLayout(Rect panel, boolean edit, int width, int height) {
        this.panel = panel; this.edit = edit; this.width = width; this.height = height;
        int x = panel.x() + EDGE, w = panel.w() - 2 * EDGE;
        title = new Rect(x, panel.y() + 7, w, TITLE_H);
        info = new Rect(x, panel.y() + 7 + TITLE_H + 2, w, TITLE_H);
        int top = info.bottom() + GAP + 2;
        int buttonsY = panel.bottom() - 10 - BUTTON_H;
        buttons = new Rect(x, buttonsY, w, BUTTON_H);
        if (edit) {
            chipRow = new Rect(x, buttonsY - GAP - ROW_H, w, ROW_H);
            status = new Rect(x, chipRow.y() - GAP - STATUS_H, w - COUNTER_W, STATUS_H);
            code = new Rect(x, top, w, Math.max(24, status.y() - GAP - top));
            field = new Rect(code.x(), code.y(), code.w() - SCROLLBAR, code.h());
        } else {
            chipRow = new Rect(x, buttonsY - GAP - ROW_H, 0, 0);
            status = new Rect(x, buttonsY - GAP - STATUS_H, w, STATUS_H);
            code = new Rect(x, top, w, Math.max(24, status.y() - GAP - top));
            field = code;
        }
    }

    /** The editor / read-only sheet: as large as the screen allows up to {@value #MAX_W} x {@value #MAX_H}. */
    public static DeedLayout editor(int screenW, int screenH) {
        int w = Math.max(MIN_W, Math.min(MAX_W, screenW - 16)), h = Math.max(MIN_H, Math.min(MAX_H, screenH - 16));
        return new DeedLayout(new Rect((screenW - w) / 2, (screenH - h) / 2, w, h), true, screenW, screenH);
    }

    /** The small "claim here" sheet. */
    public static DeedLayout claim(int screenW, int screenH) {
        int w = Math.max(MIN_W, Math.min(280, screenW - 16)), h = Math.min(Math.max(MIN_H, 118), Math.max(MIN_H, screenH - 16));
        return new DeedLayout(new Rect((screenW - w) / 2, (screenH - h) / 2, w, h), false, screenW, screenH);
    }

    /** True for the layout of the editor (chip row present). */
    public boolean hasChips() { return edit; }

    /** Left group of the button row: help, copy, paste. */
    public Rect help() { return new Rect(buttons.x(), buttons.y(), HELP_W, BUTTON_H); }
    public Rect left(int index) {
        int w = leftWidth();
        return new Rect(buttons.x() + HELP_W + GAP + index * (w + GAP), buttons.y(), w, BUTTON_H);
    }
    /** Right group, {@code count} buttons, the last one at the right edge. */
    public Rect right(int index, int count) {
        int w = rightWidth(count);
        return new Rect(buttons.right() - (count - index) * w - (count - 1 - index) * GAP, buttons.y(), w, BUTTON_H);
    }
    public int leftWidth() { return Math.min(56, (buttons.w() - rightGroupWidth(2) - 3 * GAP - HELP_W - 12) / 2); }
    /** The small sheet has no left group, so its buttons may use the row (VIS24: "Beanspruchen" was cut to 50 px). */
    private int rightWidth(int count) {
        return edit ? Math.min(count == 1 ? 80 : 60, (buttons.w() - 12) / (count + 3)) : Math.min(80, (buttons.w() - GAP) / count);
    }
    private int rightGroupWidth(int count) { return count * Math.min(60, (buttons.w() - 12) / (count + 3)) + (count - 1) * GAP; }

    /** The label of the chip row ("Online:") and where the first chip starts. */
    public Rect chipLabel(int labelWidth) { return new Rect(chipRow.x(), chipRow.y(), labelWidth, ROW_H); }

    /**
     * Chips for the names starting at {@code offset}; each gets its width from {@code widths}. Returns as many as fit
     * after the label and before the next-page button; the page button sits at the right end ({@link #nextChips}).
     */
    public List<Rect> chips(int labelWidth, int[] widths, int offset) {
        var out = new ArrayList<Rect>();
        int x = chipRow.x() + labelWidth + GAP, limit = nextChips().x() - GAP;
        for (int i = Math.max(0, offset); i < widths.length; i++) {
            if (x + widths[i] > limit) break;
            out.add(new Rect(x, chipRow.y() + 1, widths[i], ROW_H - 2));
            x += widths[i] + GAP;
        }
        return out;
    }
    public Rect nextChips() { return new Rect(chipRow.right() - 18, chipRow.y() + 1, 18, ROW_H - 2); }
}
