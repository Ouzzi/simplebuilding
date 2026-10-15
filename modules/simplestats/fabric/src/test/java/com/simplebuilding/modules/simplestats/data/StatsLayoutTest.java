package com.simplebuilding.modules.simplestats.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.simplebuilding.modules.simplestats.data.StatsLayout.Rect;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Geometry of the statistics screen. <b>What breaks this test:</b> a panel, list, scroll bar or button that sticks out of the
 * window or overlaps another part on any common window size, a list that cannot show a row, a scroll position that leaves the
 * list, or a scroll bar thumb outside its track.
 */
class StatsLayoutTest {
    private static final int[][] SCREENS = {{240, 135}, {320, 180}, {320, 240}, {427, 240}, {480, 270}, {640, 360}, {854, 480}, {960, 540}, {1280, 720}, {1920, 1080}, {3840, 2160}};

    @Test
    void everythingStaysOnScreenAndInsideThePanel() {
        for (int[] s : SCREENS) {
            StatsLayout l = StatsLayout.of(s[0], s[1]);
            String where = s[0] + "x" + s[1];
            for (Rect part : List.of(l.panel)) assertTrue(part.inside(l.screen), where + ": panel on screen " + part);
            for (Rect part : List.of(l.icon, l.title, l.list, l.scrollbar, l.done)) assertTrue(part.inside(l.panel), where + ": " + part + " inside panel " + l.panel);
        }
    }

    @Test
    void noPartsOverlap() {
        for (int[] s : SCREENS) {
            StatsLayout l = StatsLayout.of(s[0], s[1]);
            List<Rect> parts = List.of(l.icon, l.title, l.list, l.scrollbar, l.done);
            for (int a = 0; a < parts.size(); a++) {
                for (int b = a + 1; b < parts.size(); b++) {
                    assertFalse(parts.get(a).overlaps(parts.get(b)), s[0] + "x" + s[1] + ": parts " + a + " and " + b + " overlap: " + parts.get(a) + " " + parts.get(b));
                }
            }
        }
    }

    @Test
    void listShowsAtLeastOneRowAndGrowsWithTheWindow() {
        for (int[] s : SCREENS) assertTrue(StatsLayout.of(s[0], s[1]).visibleRows >= 1, s[0] + "x" + s[1]);
        assertTrue(StatsLayout.of(1920, 1080).visibleRows > StatsLayout.of(320, 180).visibleRows);
        assertTrue(StatsLayout.of(320, 180).list.h() >= StatsLayout.ROW_H);
    }

    @Test
    void tinyWindowsStillGetAUsablePanel() {
        StatsLayout l = StatsLayout.of(100, 60);
        assertTrue(l.panel.w() >= StatsLayout.MIN_W && l.panel.h() >= StatsLayout.MIN_H, "never smaller than the minimum");
        assertTrue(l.visibleRows >= 1);
    }

    @Test
    void scrollIsClampedToTheList() {
        assertEquals(0, StatsLayout.clampScroll(-5, 100, 10));
        assertEquals(90, StatsLayout.clampScroll(500, 100, 10));
        assertEquals(0, StatsLayout.clampScroll(7, 4, 10), "short lists do not scroll");
        assertEquals(0, StatsLayout.maxScroll(0, 10));
        assertEquals(5000 - 10, StatsLayout.maxScroll(5000, 10));
    }

    @Test
    void thumbStaysInsideTheTrackAndFollowsTheScroll() {
        int track = 200, visible = 11;
        for (int rows : new int[] {0, 5, 11, 12, 100, 5000, 100000}) {
            int height = StatsLayout.thumbHeight(track, rows, visible);
            assertTrue(height >= Math.min(8, track) && height <= track, "thumb height " + height + " for " + rows);
            for (int scroll : new int[] {0, 1, rows / 2, rows}) {
                int top = StatsLayout.thumbTop(track, rows, visible, scroll);
                assertTrue(top >= 0 && top + height <= track, "thumb inside track: rows " + rows + " scroll " + scroll);
            }
            int back = StatsLayout.scrollForThumb(track, rows, visible, StatsLayout.thumbTop(track, rows, visible, StatsLayout.maxScroll(rows, visible)) + height / 2);
            assertEquals(StatsLayout.maxScroll(rows, visible), back, "dragging to the bottom reaches the last row (rows " + rows + ")");
        }
        assertEquals(0, StatsLayout.scrollForThumb(track, 100, visible, -50), "dragging above the track gives the top");
        assertEquals(StatsLayout.maxScroll(100, visible), StatsLayout.scrollForThumb(track, 100, visible, 900), "dragging below the track gives the bottom");
    }
}
