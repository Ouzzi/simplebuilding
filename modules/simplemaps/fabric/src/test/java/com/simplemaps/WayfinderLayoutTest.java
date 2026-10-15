package com.simplemaps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Geometry of the wayfinder screen. <b>What breaks this test:</b> a bookmark that leaves the window or the panel, bookmarks that
 * touch, a map that wastes room, or bookmarks that do not use the free height on a large GUI.
 */
class WayfinderLayoutTest {
    private static final int[][] SCREENS = {{320, 180}, {320, 240}, {427, 240}, {480, 270}, {640, 360}, {854, 480}, {960, 540}, {1280, 720}, {1920, 1080}};

    @Test
    void everythingStaysInsideTheWindow() {
        for (int[] s : SCREENS) {
            WayfinderLayout l = WayfinderLayout.of(s[0], s[1]);
            String at = s[0] + "x" + s[1];
            assertTrue(l.minX() >= 0 && l.maxX() <= s[0], "horizontally at " + at + ": " + l.minX() + ".." + l.maxX());
            assertTrue(l.top() >= 0 && l.top() + l.panelH() <= s[1], "vertically at " + at);
            assertTrue(s[0] - (l.maxX() - l.minX()) < WayfinderLayout.CELL + 2 * WayfinderLayout.MARGIN, "map as wide as possible at " + at);
            assertTrue(s[1] - l.panelH() < WayfinderLayout.CELL + 2 * WayfinderLayout.MARGIN, "map as tall as possible at " + at);
        }
    }

    @Test
    void bookmarksLieAlongThePanelWithoutTouching() {
        for (int[] s : SCREENS) {
            if (s[1] < 240) continue; // the 180 px window is only checked for staying inside
            WayfinderLayout l = WayfinderLayout.of(s[0], s[1]);
            String at = s[0] + "x" + s[1];
            int lo = l.top() + WayfinderLayout.FRAME, hi = l.top() + l.panelH() - WayfinderLayout.FRAME;
            for (int count : new int[] {WayfinderLayout.LEFT_TABS, WayfinderLayout.RIGHT_TABS}) {
                assertTrue(l.tabY(0, count) >= lo && l.tabY(count - 1, count) + l.tab() <= hi, "column of " + count + " inside the frame at " + at);
                for (int i = 1; i < count; i++) {
                    assertTrue(l.tabY(i, count) - (l.tabY(i - 1, count) + l.tab()) >= WayfinderLayout.TAB_GAP, "gap between bookmarks at " + at);
                }
            }
            assertTrue(l.tab() >= 20, "bookmarks big enough for a 16 px icon at " + at + ": " + l.tab());
        }
    }

    @Test
    void bookmarksAreSquareAndTuckUnderTheFrame() {
        WayfinderLayout l = WayfinderLayout.of(854, 480);
        assertEquals(l.tab() + WayfinderLayout.TAB_IN, l.tabW());
        assertEquals(l.left() - l.tab(), l.leftTabX());
        assertEquals(l.left() + l.panelW(), l.rightTabX());
        assertTrue(WayfinderLayout.TAB_IN <= WayfinderLayout.FRAME, "the hidden part fits under the frame");
    }

    @Test
    void bookmarksGrowWithTheFreeHeightUpToTheLimit() {
        int small = WayfinderLayout.of(427, 240).tab(), big = WayfinderLayout.of(1920, 1080).tab();
        assertTrue(big > small, "larger window, larger bookmarks: " + small + " -> " + big);
        assertEquals(WayfinderLayout.TAB_MAX, big);
    }

    @Test
    void mapGrowsWithTheWindow() {
        WayfinderLayout a = WayfinderLayout.of(427, 240), b = WayfinderLayout.of(960, 540);
        assertTrue(b.cols() > a.cols() && b.rows() > a.rows());
        assertTrue(a.cols() > 12, "larger than the old 12 x 9 map at 854x480");
    }
}
