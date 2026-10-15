package com.simplebuilding.modules.simpletweaks.deed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.simplebuilding.modules.simpletweaks.deed.DeedLayout.Rect;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Geometry of the claim deed sheets. <b>What breaks this test:</b> a sheet that sticks out of the screen, a code
 * field, status line, chip row or button that overlaps another part or leaves the sheet, chips that overflow the row
 * or overlap the next-page button, left and right buttons that collide on a narrow screen.
 */
class DeedLayoutTest {
    private static final int[][] SCREENS = {{240, 135}, {320, 180}, {320, 240}, {427, 240}, {480, 270}, {640, 360}, {960, 540}, {1920, 1080}};

    private static void apart(String where, List<Rect> parts) {
        for (int a = 0; a < parts.size(); a++) {
            for (int b = a + 1; b < parts.size(); b++) assertFalse(parts.get(a).overlaps(parts.get(b)), where + ": parts " + a + " and " + b + " overlap: " + parts.get(a) + " " + parts.get(b));
        }
    }

    @Test
    void editorStaysOnScreenAndNothingOverlaps() {
        for (int[] s : SCREENS) {
            DeedLayout l = DeedLayout.editor(s[0], s[1]);
            String where = s[0] + "x" + s[1];
            if (s[0] >= DeedLayout.MIN_W + 16 && s[1] >= DeedLayout.MIN_H + 16)
                assertTrue(new Rect(0, 0, s[0], s[1]).contains(l.panel), where + ": panel on screen");
            List<Rect> parts = new ArrayList<>(List.of(l.title, l.info, l.code, l.status, l.chipRow, l.buttons));
            for (Rect r : parts) assertTrue(l.panel.contains(r), where + ": " + r + " inside the panel");
            assertTrue(l.code.contains(l.field), where + ": field inside the code area");
            assertEquals(DeedLayout.SCROLLBAR, l.code.w() - l.field.w(), where + ": scrollbar column");
            assertTrue(l.code.h() >= 24, where + ": code area keeps a few rows");
            apart(where, parts);
        }
    }

    @Test
    void buttonRowDoesNotCollide() {
        for (int[] s : SCREENS) {
            for (DeedLayout l : new DeedLayout[] {DeedLayout.editor(s[0], s[1]), DeedLayout.claim(s[0], s[1])}) {
                String where = s[0] + "x" + s[1];
                List<Rect> row = new ArrayList<>();
                if (l.hasChips()) row.addAll(List.of(l.help(), l.left(0), l.left(1)));
                row.addAll(List.of(l.right(0, 2), l.right(1, 2)));
                for (Rect r : row) assertTrue(l.buttons.contains(r), where + ": " + r + " inside the button row");
                apart(where + " two right buttons", row);
                List<Rect> one = new ArrayList<>(List.of(l.right(0, 1)));
                if (l.hasChips()) one.addAll(List.of(l.help(), l.left(0), l.left(1)));
                apart(where + " one right button", one);
                assertTrue(l.buttons.contains(l.right(0, 1)), where + ": single button inside");
                assertEquals(l.buttons.right(), l.right(1, 2).right(), where + ": last button at the right edge");
            }
        }
    }

    @Test
    void chipsFitTheRowAndKeepOffTheNextButton() {
        int[] widths = new int[30];
        for (int i = 0; i < widths.length; i++) widths[i] = 40 + (i * 7) % 30;
        for (int[] s : SCREENS) {
            DeedLayout l = DeedLayout.editor(s[0], s[1]);
            String where = s[0] + "x" + s[1];
            for (int offset : new int[] {0, 3, 29, 40}) {
                List<Rect> chips = l.chips(40, widths, offset);
                for (Rect c : chips) {
                    assertTrue(l.chipRow.contains(c), where + ": chip inside the row");
                    assertFalse(c.overlaps(l.nextChips()), where + ": chip clear of the next button");
                    assertTrue(c.x() >= l.chipLabel(40).right(), where + ": chip right of the label");
                }
                apart(where + " chips", chips);
            }
            assertTrue(l.chipRow.contains(l.nextChips()), where + ": next button inside the row");
        }
    }

    @Test
    void largeScreenDoesNotGrowPastTheMaximum() {
        DeedLayout l = DeedLayout.editor(1920, 1080);
        assertEquals(DeedLayout.MAX_W, l.panel.w());
        assertEquals(DeedLayout.MAX_H, l.panel.h());
        assertEquals((1920 - DeedLayout.MAX_W) / 2, l.panel.x());
    }

    @Test
    void claimSheetIsSmallerButComplete() {
        DeedLayout c = DeedLayout.claim(640, 360), e = DeedLayout.editor(640, 360);
        assertTrue(c.panel.h() < e.panel.h());
        assertTrue(c.panel.contains(c.code) && c.panel.contains(c.buttons) && c.panel.contains(c.status));
        assertFalse(c.code.overlaps(c.buttons));
        assertFalse(c.hasChips());
    }
}
