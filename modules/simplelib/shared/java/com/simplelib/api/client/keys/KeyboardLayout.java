package com.simplelib.api.client.keys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Key positions of the keyboard picture on the key binding page (UI28-1 / WP-25), as pure data without Minecraft classes
 * (so game tests can check it): the 60 %, 75 % and 100 % layouts of the reference, US based. A cell is placed in quarter
 * keys ({@link #UNIT} = 4 per key), the key is named by its GLFW key code (the same numbers {@code InputConstants.KEY_*} has).
 * The arrow keys form the inverted T of the owner's picture (29-keybind-pfeile.png, ABN29b-8): the up key sits above the
 * down key, left and right beside it in the bottom row.
 */
public final class KeyboardLayout {
    /** Quarter keys per key: widths like 1.25 or 6.25 come out whole. */
    public static final int UNIT = 4;

    /** The three sizes of the switch; {@code percent} is the caption. */
    public enum Size {
        COMPACT("60 %"), TENKEYLESS("75 %"), FULL("100 %");

        public final String percent;

        Size(String percent) {
            this.percent = percent;
        }
    }

    /** One key cap: position and size in quarter keys, {@code label} is what it shows. */
    public record Cell(int code, String label, int x, int y, int w, int h) {
        public int right() {
            return x + w;
        }

        public int bottom() {
            return y + h;
        }
    }

    // GLFW key codes (stable numbers of the GLFW API).
    public static final int ESC = 256, ENTER = 257, TAB = 258, BACKSPACE = 259, INSERT = 260, DELETE = 261, RIGHT = 262, LEFT = 263,
            DOWN = 264, UP = 265, PAGE_UP = 266, PAGE_DOWN = 267, HOME = 268, END = 269, CAPS_LOCK = 280, SCROLL_LOCK = 281,
            NUM_LOCK = 282, PRINT_SCREEN = 283, PAUSE = 284, F1 = 290, KP_0 = 320, KP_DECIMAL = 330, KP_DIVIDE = 331,
            KP_MULTIPLY = 332, KP_SUBTRACT = 333, KP_ADD = 334, KP_ENTER = 335, LEFT_SHIFT = 340, LEFT_CONTROL = 341,
            LEFT_ALT = 342, LEFT_SUPER = 343, RIGHT_SHIFT = 344, RIGHT_CONTROL = 345, RIGHT_ALT = 346, RIGHT_SUPER = 347,
            MENU = 348, SPACE = 32;
    private static final int APOSTROPHE = 39, COMMA = 44, MINUS = 45, PERIOD = 46, SLASH = 47, SEMICOLON = 59, EQUAL = 61,
            LEFT_BRACKET = 91, BACKSLASH = 92, RIGHT_BRACKET = 93, GRAVE = 96;

    private KeyboardLayout() {}

    /** All cells of {@code size}; {@code german} swaps the captions of Y and Z (QWERTZ display only - the key codes stay US). */
    public static List<Cell> cells(Size size, boolean german) {
        List<Cell> out = new ArrayList<>();
        Row row = new Row(out, german);
        // row 0: Esc (60 %) or Esc and the function keys, 15 keys wide in all sizes but 60 %
        row.at(0, 0).key(ESC, 4);
        if (size != Size.COMPACT) {
            row.gap(4);
            for (int i = 0; i < 12; i++) {
                if (i == 4 || i == 8) row.gap(2);
                row.key(F1 + i, 4);
            }
        }
        // row 1: number row
        row.at(0, 1).key(GRAVE, 4);
        for (int c = '1'; c <= '9'; c++) row.key(c, 4);
        row.key('0', 4).key(MINUS, 4).key(EQUAL, 4).key(BACKSPACE, 8);
        // row 2: Tab row
        row.at(0, 2).key(TAB, 6);
        for (char c : "QWERTYUIOP".toCharArray()) row.key(c, 4);
        row.key(LEFT_BRACKET, 4).key(RIGHT_BRACKET, 4).key(BACKSLASH, 6);
        // row 3: Caps row
        row.at(0, 3).key(CAPS_LOCK, 7);
        for (char c : "ASDFGHJKL".toCharArray()) row.key(c, 4);
        row.key(SEMICOLON, 4).key(APOSTROPHE, 4).key(ENTER, 9);
        // row 4: Shift row (the arrow keys of the 75 % layout stand right of the main block, see below)
        row.at(0, 4).key(LEFT_SHIFT, 9);
        for (char c : "ZXCVBNM".toCharArray()) row.key(c, 4);
        row.key(COMMA, 4).key(PERIOD, 4).key(SLASH, 4);
        row.key(RIGHT_SHIFT, 11);
        // row 5: space row
        row.at(0, 5).key(LEFT_CONTROL, 5).key(LEFT_SUPER, 5).key(LEFT_ALT, 5).key(SPACE, 25);
        if (size == Size.TENKEYLESS) row.key(RIGHT_ALT, 4).key(RIGHT_CONTROL, 4);
        else row.key(RIGHT_ALT, 5).key(RIGHT_SUPER, 5).key(MENU, 5).key(RIGHT_CONTROL, 5);
        if (size == Size.TENKEYLESS) {
            // arrow block as on a real keyboard: a gap right of the main block (60 wide), the inverted T on the shift and space rows
            int arrows = 62;
            row.at(arrows + 4, 4).key(UP, 4);
            row.at(arrows, 5).key(LEFT, 4).key(DOWN, 4).key(RIGHT, 4);
            // one column right of the arrow block
            int nav = arrows + 14;
            row.at(nav, 0).key(DELETE, 4);
            row.at(nav, 1).key(HOME, 4);
            row.at(nav, 2).key(PAGE_UP, 4);
            row.at(nav, 3).key(PAGE_DOWN, 4);
            row.at(nav, 4).key(END, 4);
        }
        if (size == Size.FULL) {
            int nav = 62;
            row.at(nav, 0).key(PRINT_SCREEN, 4).key(SCROLL_LOCK, 4).key(PAUSE, 4);
            row.at(nav, 1).key(INSERT, 4).key(HOME, 4).key(PAGE_UP, 4);
            row.at(nav, 2).key(DELETE, 4).key(END, 4).key(PAGE_DOWN, 4);
            row.at(nav + 4, 4).key(UP, 4);
            row.at(nav, 5).key(LEFT, 4).key(DOWN, 4).key(RIGHT, 4);
            int pad = nav + 14;
            row.at(pad, 1).key(NUM_LOCK, 4).key(KP_DIVIDE, 4).key(KP_MULTIPLY, 4).key(KP_SUBTRACT, 4);
            row.at(pad, 2).key(KP_0 + 7, 4).key(KP_0 + 8, 4).key(KP_0 + 9, 4).tall(KP_ADD, 4, 8);
            row.at(pad, 3).key(KP_0 + 4, 4).key(KP_0 + 5, 4).key(KP_0 + 6, 4);
            row.at(pad, 4).key(KP_0 + 1, 4).key(KP_0 + 2, 4).key(KP_0 + 3, 4).tall(KP_ENTER, 4, 8);
            row.at(pad, 5).key(KP_0, 8).key(KP_DECIMAL, 4);
        }
        return out;
    }

    /** Width of the whole picture in quarter keys. */
    public static int width(List<Cell> cells) {
        return cells.stream().mapToInt(Cell::right).max().orElse(0);
    }

    /** Height of the whole picture in quarter keys. */
    public static int height(List<Cell> cells) {
        return cells.stream().mapToInt(Cell::bottom).max().orElse(0);
    }

    /**
     * Pixels per quarter key so that the picture fits {@code availW x availH}: whole numbers (crisp edges), at least 2
     * (an 8 px key), at most 5.
     */
    public static int scale(List<Cell> cells, int availW, int availH) {
        int w = Math.max(1, width(cells)), h = Math.max(1, height(cells));
        return Math.max(2, Math.min(5, Math.min(availW / w, availH / h)));
    }

    /** The cell under the pixel ({@code mx}, {@code my}) when the picture's corner is at ({@code ox}, {@code oy}) and a quarter key is {@code scale} px. */
    public static Optional<Cell> at(List<Cell> cells, int ox, int oy, int scale, double mx, double my) {
        for (Cell c : cells) {
            if (mx >= ox + c.x() * scale && mx < ox + c.right() * scale && my >= oy + c.y() * scale && my < oy + c.bottom() * scale) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    /** Keys one may sensibly move an action to: letters, digits, function keys, punctuation, number pad digits. */
    public static boolean plain(int code) {
        return code >= 'A' && code <= 'Z' || code >= '0' && code <= '9' || code >= F1 && code < F1 + 12
                || code == GRAVE || code == MINUS || code == EQUAL || code == LEFT_BRACKET || code == RIGHT_BRACKET || code == BACKSLASH
                || code == SEMICOLON || code == APOSTROPHE || code == COMMA || code == PERIOD || code == SLASH
                || code >= KP_0 && code <= KP_0 + 9;
    }

    /**
     * The free plain key nearest to {@code from} (centre distance on the picture; ties go to the earlier cell), or
     * empty when none is free. {@code used} holds the key codes already bound; the key itself counts as used.
     */
    public static Optional<Cell> nearestFree(List<Cell> cells, Set<Integer> used, int from) {
        Cell origin = cells.stream().filter(c -> c.code() == from).findFirst().orElse(null);
        double ox = origin == null ? 0 : origin.x() + origin.w() / 2.0, oy = origin == null ? 0 : origin.y() + origin.h() / 2.0;
        Cell best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Cell c : cells) {
            if (!plain(c.code()) || used.contains(c.code()) || c.code() == from) continue;
            double dx = c.x() + c.w() / 2.0 - ox, dy = c.y() + c.h() / 2.0 - oy;
            double d = dx * dx + dy * dy;
            if (d < bestDistance) {
                best = c;
                bestDistance = d;
            }
        }
        return Optional.ofNullable(best);
    }

    /** The caption of a key; {@code german} swaps Y and Z. */
    public static String label(int code, boolean german) {
        if (code >= 'A' && code <= 'Z') {
            char c = (char) code;
            if (german && c == 'Y') c = 'Z';
            else if (german && c == 'Z') c = 'Y';
            return String.valueOf(c);
        }
        if (code >= '0' && code <= '9') return String.valueOf((char) code);
        if (code >= F1 && code < F1 + 25) return "F" + (code - F1 + 1);
        if (code >= KP_0 && code <= KP_0 + 9) return String.valueOf(code - KP_0);
        return SPECIAL.getOrDefault(code, "?");
    }

    private static final Map<Integer, String> SPECIAL = Map.ofEntries(
            Map.entry(ESC, "Esc"), Map.entry(ENTER, "Enter"), Map.entry(TAB, "Tab"), Map.entry(BACKSPACE, "Bksp"),
            Map.entry(INSERT, "Ins"), Map.entry(DELETE, "Del"), Map.entry(RIGHT, ">"), Map.entry(LEFT, "<"),
            Map.entry(DOWN, "v"), Map.entry(UP, "^"), Map.entry(PAGE_UP, "PgUp"), Map.entry(PAGE_DOWN, "PgDn"),
            Map.entry(HOME, "Home"), Map.entry(END, "End"), Map.entry(CAPS_LOCK, "Caps"), Map.entry(SCROLL_LOCK, "ScrLk"),
            Map.entry(NUM_LOCK, "Num"), Map.entry(PRINT_SCREEN, "Prt"), Map.entry(PAUSE, "Pause"),
            Map.entry(KP_DECIMAL, "."), Map.entry(KP_DIVIDE, "/"), Map.entry(KP_MULTIPLY, "*"), Map.entry(KP_SUBTRACT, "-"),
            Map.entry(KP_ADD, "+"), Map.entry(KP_ENTER, "Ent"), Map.entry(LEFT_SHIFT, "Shift"), Map.entry(RIGHT_SHIFT, "Shift"),
            Map.entry(LEFT_CONTROL, "Ctrl"), Map.entry(RIGHT_CONTROL, "Ctrl"), Map.entry(LEFT_ALT, "Alt"), Map.entry(RIGHT_ALT, "Alt"),
            Map.entry(LEFT_SUPER, "Win"), Map.entry(RIGHT_SUPER, "Win"), Map.entry(MENU, "Menu"), Map.entry(SPACE, "Space"),
            Map.entry(APOSTROPHE, "'"), Map.entry(COMMA, ","), Map.entry(MINUS, "-"), Map.entry(PERIOD, "."), Map.entry(SLASH, "/"),
            Map.entry(SEMICOLON, ";"), Map.entry(EQUAL, "="), Map.entry(LEFT_BRACKET, "["), Map.entry(BACKSLASH, "\\"),
            Map.entry(RIGHT_BRACKET, "]"), Map.entry(GRAVE, "`"));

    /** Builds the cells row by row. */
    private static final class Row {
        private final List<Cell> out;
        private final boolean german;
        private int x, y;

        Row(List<Cell> out, boolean german) {
            this.out = out;
            this.german = german;
        }

        /** Next key at quarter-key column {@code x} of picture row {@code row} (the function row is separated by half a key). */
        Row at(int x, int row) {
            this.x = x;
            this.y = row == 0 ? 0 : row * UNIT + 2;
            return this;
        }

        Row gap(int w) {
            x += w;
            return this;
        }

        Row key(int code, int w) {
            return tall(code, w, UNIT);
        }

        Row tall(int code, int w, int h) {
            out.add(new Cell(code, label(code, german), x, y, w, h));
            x += w;
            return this;
        }
    }
}
