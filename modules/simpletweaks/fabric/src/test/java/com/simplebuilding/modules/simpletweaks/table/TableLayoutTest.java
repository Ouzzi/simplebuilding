package com.simplebuilding.modules.simpletweaks.table;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.simplebuilding.modules.simpletweaks.deed.DeedLayout.Rect;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Geometry and data helpers of the claim table. <b>What breaks this test:</b> a map, list, status line or button that
 * leaves the panel or overlaps another part, a list row below the list, a map cell outside the drawn grid, run-length
 * data that does not survive a round trip, owner colors that are unstable, or visibility rules that leak a claim.
 */
class TableLayoutTest {
    private static final int[][] SCREENS = {{240, 135}, {320, 180}, {320, 240}, {427, 240}, {480, 270}, {640, 360}, {960, 540}, {1920, 1080}};

    private static void apart(String where, List<Rect> parts) {
        for (int a = 0; a < parts.size(); a++)
            for (int b = a + 1; b < parts.size(); b++)
                assertFalse(parts.get(a).overlaps(parts.get(b)), where + ": parts " + a + " and " + b + " overlap: " + parts.get(a) + " " + parts.get(b));
    }

    @Test
    void everythingInsideThePanelNothingOverlaps() {
        for (int[] s : SCREENS) {
            var l = TableLayout.of(s[0], s[1]);
            String where = s[0] + "x" + s[1];
            if (s[0] >= TableLayout.MIN_W + 16 && s[1] >= TableLayout.MIN_H + 16) assertTrue(new Rect(0, 0, s[0], s[1]).contains(l.panel), where + ": panel on screen");
            var parts = new ArrayList<>(List.of(l.title, l.info, l.map, l.status, l.list, l.buttons));
            for (Rect r : parts) assertTrue(l.panel.contains(r), where + ": " + r + " inside the panel");
            // The button row sits below everything else; title/info stack; map and list stand side by side.
            apart(where, List.of(l.title, l.info, l.map, l.list, l.buttons));
            apart(where, List.of(l.status, l.list, l.buttons));
            assertTrue(l.map.bottom() <= l.status.y(), where + ": status under the map");
            assertEquals(l.map.w(), l.map.h(), where + ": square map");
            assertTrue(l.map.w() >= 16, where + ": map not degenerate");
        }
    }

    @Test
    void buttonsFitTheRowAndKeepApart() {
        for (int[] s : SCREENS) {
            var l = TableLayout.of(s[0], s[1]);
            String where = s[0] + "x" + s[1];
            var row = List.of(l.zoomOut(), l.zoomIn(), l.mine(), l.prev(), l.next(), l.close());
            for (Rect r : row) assertTrue(l.buttons.contains(r), where + ": " + r + " inside the button row");
            apart(where, row);
            assertEquals(l.buttons.right(), l.close().right(), where + ": close at the right edge");
        }
    }

    @Test
    void listRowsStayInTheList() {
        for (int[] s : SCREENS) {
            var l = TableLayout.of(s[0], s[1]);
            String where = s[0] + "x" + s[1];
            assertTrue(l.rows() >= 1, where + ": at least one row");
            var rows = new ArrayList<Rect>();
            for (int i = 0; i < l.rows(); i++) { assertTrue(l.list.contains(l.row(i)), where + ": row " + i + " inside the list"); rows.add(l.row(i)); }
            apart(where + " rows", rows);
        }
    }

    @Test
    void gridFitsTheMapAndCellsMapBack() {
        for (int[] s : SCREENS) {
            var l = TableLayout.of(s[0], s[1]);
            for (int cells : TableLayout.ZOOM) {
                String where = s[0] + "x" + s[1] + " cells " + cells;
                Rect g = l.grid(cells);
                if (l.map.w() >= cells) assertTrue(l.map.contains(g), where + ": grid inside the map");
                assertTrue(l.cell(cells) >= 1, where);
                int c = l.cell(cells);
                assertArrayEquals(new int[]{0, 0}, l.cellAt(g.x(), g.y(), cells), where + ": top left cell");
                if (l.map.w() >= cells) assertArrayEquals(new int[]{cells - 1, cells - 1}, l.cellAt(g.right() - 1, g.bottom() - 1, cells), where + ": bottom right cell");
                assertNull(l.cellAt(g.x() - 1, g.y(), cells), where + ": left of the grid");
                assertNull(l.cellAt(g.x(), g.bottom(), cells), where + ": below the grid");
                if (c >= 2) assertArrayEquals(new int[]{1, 0}, l.cellAt(g.x() + c, g.y(), cells), where + ": second cell");
            }
        }
    }

    @Test
    void runLengthRoundTripAndLimits() {
        byte[] cells = new byte[ClaimGrid.MAX_CELLS];
        assertTrue(ClaimGrid.encode(cells).length <= 2 * 16, "an empty 64x64 map is a few pairs");
        assertArrayEquals(cells, ClaimGrid.decode(ClaimGrid.encode(cells), cells.length));
        for (int i = 0; i < cells.length; i++) cells[i] = (byte) (i % 255 + 1 == 7 ? 0 : (i * 31) % 256);
        var enc = ClaimGrid.encode(cells);
        assertTrue(enc.length <= ClaimGrid.MAX_ENCODED, "worst case fits the packet bound");
        assertArrayEquals(cells, ClaimGrid.decode(enc, cells.length));
        byte[] runs = new byte[600];
        java.util.Arrays.fill(runs, (byte) 5);
        assertArrayEquals(runs, ClaimGrid.decode(ClaimGrid.encode(runs), 600), "runs longer than 256 are split");
        assertNull(ClaimGrid.decode(new byte[]{1, 4}, 4), "too few cells");
        assertNull(ClaimGrid.decode(new byte[]{1, 4}, 6), "too many cells");
        assertNull(ClaimGrid.decode(new byte[]{1}, 1), "odd data");
        assertNull(ClaimGrid.decode(new byte[0], ClaimGrid.MAX_CELLS + 1), "more than the maximum area");
        assertNull(ClaimGrid.decode(new byte[0], -1));
        assertNotNull(ClaimGrid.decode(new byte[0], 0));
    }

    @Test
    void ownerColorsAreStableAndSpread() {
        var id = UUID.fromString("12345678-1234-4234-8234-123456789abc");
        assertEquals(ClaimGrid.color(id), ClaimGrid.color(new UUID(id.getMostSignificantBits(), id.getLeastSignificantBits())));
        var seen = new HashSet<Integer>();
        for (int i = 0; i < 200; i++) {
            int c = ClaimGrid.color(new UUID(i * 0x9E3779B97F4A7C15L, i * 31L + 7));
            assertEquals(0, c & ~0xFFFFFF, "24 bit color");
            seen.add(c);
        }
        assertTrue(seen.size() > 150, "different owners mostly get different colors: " + seen.size());
    }

    @Test
    void visibilityRules() {
        for (var v : TableVisibility.values()) {
            assertTrue(v.shows(true, false) && v.showsName(true, false), v + ": an operator always sees all");
            assertEquals(v, TableVisibility.parse(v.key()));
            assertEquals(v, TableVisibility.byOrdinal(v.ordinal()));
        }
        assertEquals(TableVisibility.OFF, TableVisibility.byOrdinal(99), "unknown ordinal: nothing shown");
        assertEquals(TableVisibility.OFF, TableVisibility.byOrdinal(-1));
        assertNull(TableVisibility.parse("everything"));
        assertNull(TableVisibility.parse(null));
        assertFalse(TableVisibility.OFF.shows(false, true));
        assertFalse(TableVisibility.OWN.shows(false, false));
        assertTrue(TableVisibility.OWN.showsName(false, true));
        assertTrue(TableVisibility.OWNERS_HIDDEN.shows(false, false));
        assertFalse(TableVisibility.OWNERS_HIDDEN.showsName(false, false));
        assertTrue(TableVisibility.OWNERS_HIDDEN.showsName(false, true));
    }
}
