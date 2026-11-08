package com.simplebuilding.modules.simpletweaks.deed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.simplebuilding.modules.simpletweaks.deed.DeedLayout.Rect;
import com.simplebuilding.modules.simpletweaks.deed.DeedMapLayout.Chunk;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Geometry of the deed's card (WH26-9, variant A). <b>What breaks this test:</b> a part that leaves the card or overlaps
 * another, side tabs that do not stick out, a dimension page that loses or repeats a dimension, touching chunks that
 * do not glue (pitch = cell), separate claims that are not exactly 4 px apart, tiles that overlap or leave the compass
 * order, a grid that overflows while a smaller zoom step would fit, buy-tab parts that overlap.
 */
class DeedMapLayoutTest {
    private static final int[][] SCREENS = {{320, 200}, {320, 240}, {427, 240}, {480, 270}, {640, 360}, {960, 540}, {1920, 1080}};

    private static void apart(String where, List<Rect> parts) {
        for (int a = 0; a < parts.size(); a++)
            for (int b = a + 1; b < parts.size(); b++) assertFalse(parts.get(a).overlaps(parts.get(b)), where + ": " + parts.get(a) + " overlaps " + parts.get(b));
    }

    @Test
    void cardPartsInsideAndApart() {
        for (int[] s : SCREENS) {
            var l = DeedMapLayout.of(s[0], s[1]);
            String where = s[0] + "x" + s[1];
            assertTrue(l.panel.w() >= DeedMapLayout.MIN_W && l.panel.w() <= DeedMapLayout.MAX_W, where + ": width");
            assertTrue(new Rect(0, 0, s[0], s[1]).contains(new Rect(l.panel.x() - DeedMapLayout.TAB_OUT, l.panel.y(), l.panel.w() + 2 * DeedMapLayout.TAB_OUT, l.panel.h())),
                    where + ": card with its tabs on screen");
            // folds at a third and two thirds; management left of the second, locator right of it
            assertEquals(l.inner.x() + l.inner.w() / 3, l.fold1, where + ": first fold");
            assertEquals(l.inner.x() + 2 * l.inner.w() / 3, l.fold2, where + ": second fold");
            assertTrue(l.manage.right() <= l.fold2 && l.locator.x() > l.fold2, where + ": management | locator");
            var manage = List.of(l.title, l.sub, l.area, l.info, l.button(0), l.button(1));
            for (Rect r : manage) assertTrue(l.manage.contains(r), where + ": " + r + " in the management side");
            apart(where, manage);
            var locator = new ArrayList<>(List.of(l.badge, l.card, l.details));
            for (Rect r : locator) assertTrue(l.locator.contains(r), where + ": " + r + " in the locator");
            apart(where, locator);
            assertTrue(l.card.contains(l.picture()) && l.picture().w() == 16 * l.scale && l.scale >= 2, where + ": chunk picture in the card");
            assertTrue(l.locator.contains(l.hintClose()), where + ": hint close");
            assertTrue(l.button(0).w() >= 60, where + ": buttons wide enough for 'Claim here'");
            Rect c = l.confirm();
            assertTrue(l.area.contains(c) && c.contains(l.confirmButton(0)) && c.contains(l.confirmButton(1)) && !l.confirmButton(0).overlaps(l.confirmButton(1)), where + ": confirmation box");
            assertTrue(l.area.h() >= 60, where + ": grid area " + l.area);
        }
    }

    @Test
    void sideTabsStickOut() {
        var l = DeedMapLayout.of(640, 360);
        for (int i = 0; i < 3; i++) {
            Rect t = l.leftTab(i);
            assertEquals(l.panel.x() - DeedMapLayout.TAB_OUT, t.x(), "Left tab sticks out 24 px");
            assertTrue(t.right() > l.panel.x() && t.right() <= l.panel.x() + 6, "Inner end tucked under the frame");
            if (i > 0) assertFalse(t.overlaps(l.leftTab(i - 1)), "Left tabs apart");
        }
        for (int i = 0; i < l.rightSlots(); i++) {
            Rect t = l.rightTab(i);
            assertEquals(l.panel.right() + DeedMapLayout.TAB_OUT, t.right(), "Right tab sticks out 24 px");
            assertTrue(t.bottom() <= l.panel.bottom(), "Right tab " + i + " along the card");
            if (i > 0) assertFalse(t.overlaps(l.rightTab(i - 1)), "Right tabs apart");
        }
    }

    @Test
    void dimensionPagesEndWithNext() {
        assertArrayEquals(new int[]{0, 1, 2}, DeedMapLayout.tabPage(3, 6, 0), "Three Vanilla dimensions fit: no next tab");
        assertArrayEquals(new int[]{0, 1, 2, 3, 4, 5}, DeedMapLayout.tabPage(6, 6, 0), "Exactly full: no next tab");
        // 11 dimensions, 6 slots: 5 per page + "next"
        assertEquals(3, DeedMapLayout.tabPages(11, 6));
        var seen = new HashSet<Integer>();
        for (int p = 0; p < 3; p++) {
            int[] page = DeedMapLayout.tabPage(11, 6, p);
            assertEquals(-1, page[page.length - 1], "The last tab is next");
            assertTrue(page.length <= 6);
            for (int i = 0; i < page.length - 1; i++) assertTrue(seen.add(page[i]), "Each dimension once");
        }
        assertEquals(11, seen.size(), "Every dimension on some page");
        assertArrayEquals(DeedMapLayout.tabPage(11, 6, 0), DeedMapLayout.tabPage(11, 6, 3), "Next on the last page wraps to the first");
    }

    @Test
    void touchingChunksGlueSeparateOnesFourPixelsApart() {
        // a home 3x2 (glued), an outpost 30 chunks east, a farm 115 south of the home's west column
        var chunks = List.of(new Chunk(0, 0), new Chunk(1, 0), new Chunk(2, 0), new Chunk(0, 1), new Chunk(1, 1), new Chunk(30, 0), new Chunk(0, 115));
        var g = DeedMapLayout.grid(chunks, 400, 400, -1);
        assertEquals(16, g.cell(), "Few chunks: the largest step");
        var t00 = g.of(new Chunk(0, 0)); var t10 = g.of(new Chunk(1, 0)); var t20 = g.of(new Chunk(2, 0)); var t01 = g.of(new Chunk(0, 1));
        assertEquals(g.cell(), t10.x() - t00.x(), "East neighbour glues");
        assertEquals(g.cell(), t20.x() - t10.x(), "... and the next");
        assertEquals(g.cell(), t01.y() - t00.y(), "South neighbour glues");
        var out = g.of(new Chunk(30, 0)); var farm = g.of(new Chunk(0, 115));
        assertEquals(t20.x() + g.cell() + DeedMapLayout.GAP, out.x(), "Outpost: exactly 4 px after the home");
        assertEquals(t00.y(), out.y(), "Outpost on the same row (same z)");
        assertEquals(t01.y() + g.cell() + DeedMapLayout.GAP, farm.y(), "Farm: exactly 4 px below the home");
        assertEquals(t00.x(), farm.x(), "Farm in the home's column (same x)");
        assertEquals(3, g.groups().size(), "Three claims");
        assertEquals(g.groups().indexOf(g.groups().get(t00.group())), t10.group(), "Glued chunks are one group");
        assertEquals(4 * g.cell() + DeedMapLayout.GAP, g.width());
    }

    @Test
    void zoomStepsFitOrPan() {
        var row = new ArrayList<Chunk>();
        for (int i = 0; i < 20; i++) row.add(new Chunk(i, 0));
        assertEquals(9, DeedMapLayout.grid(row, 200, 100, -1).cell(), "20 glued chunks in 200 px: 9 px cells (12 px would be 240)");
        assertEquals(16, DeedMapLayout.grid(row, 200, 100, 0).cell(), "A chosen step is kept (then the grid pans)");
        var scattered = new ArrayList<Chunk>();
        for (int i = 0; i < 100; i++) scattered.add(new Chunk(i * 3, 0));
        var g = DeedMapLayout.grid(scattered, 200, 100, -1);
        assertEquals(4, g.cell(), "Nothing fits: the smallest step");
        assertTrue(g.width() > 200, "... and the grid pans");
    }

    @Test
    void randomLandNeverOverlapsAndKeepsOrder() {
        var rnd = new Random(7);
        for (int run = 0; run < 60; run++) {
            var chunks = new ArrayList<Chunk>();
            int n = 1 + rnd.nextInt(80);
            while (chunks.size() < n) {
                var c = rnd.nextInt(3) == 0 ? new Chunk(rnd.nextInt(8), rnd.nextInt(8)) : new Chunk(rnd.nextInt(4000) - 2000, rnd.nextInt(4000) - 2000);
                if (!chunks.contains(c)) chunks.add(c);
            }
            var g = DeedMapLayout.grid(chunks, 200, 130, -1);
            assertEquals(n, g.tiles().size(), "run " + run + ": every chunk one tile");
            for (var a : g.tiles()) {
                assertTrue(a.x() >= 0 && a.y() >= 0 && a.x() + g.cell() <= g.width() && a.y() + g.cell() <= g.height(), "inside the content");
                assertNotNull(g.at(a.x() + g.cell() / 2.0, a.y() + g.cell() / 2.0), "hit test");
                for (var b : g.tiles()) {
                    if (a == b) continue;
                    assertFalse(new Rect(a.x(), a.y(), g.cell(), g.cell()).overlaps(new Rect(b.x(), b.y(), g.cell(), g.cell())), "run " + run + ": tiles apart");
                    if (a.chunk().x() < b.chunk().x()) assertTrue(a.x() < b.x(), "west stays west");
                    if (a.chunk().z() < b.chunk().z()) assertTrue(a.y() < b.y(), "north stays north");
                    int dx = b.x() - a.x(), dz = b.chunk().x() - a.chunk().x();
                    if (dz == 1) assertEquals(g.cell(), dx, "world neighbours glue");
                    else if (dz > 1 && a.chunk().z() == b.chunk().z()) assertTrue(dx >= g.cell() + DeedMapLayout.GAP, "a jump leaves at least 4 px");
                }
            }
            if (g.cell() > DeedMapLayout.ZOOMS[DeedMapLayout.ZOOMS.length - 1]) assertTrue(g.width() <= 200 && g.height() <= 130, "an auto step fits");
        }
        assertTrue(DeedMapLayout.grid(List.of(), 200, 100, -1).tiles().isEmpty());
    }

    @Test
    void buyTabPartsApart() {
        for (int[] s : SCREENS) {
            var l = DeedMapLayout.of(s[0], s[1]);
            String where = s[0] + "x" + s[1];
            assertTrue(l.offerRows() >= 3, where + ": at least 3 offers visible (" + l.offerRows() + ")");
            var parts = new ArrayList<>(List.of(l.tradeTitle(), l.tradeBadge(), l.tradeDetail(), l.inventory()));
            for (int r = 0; r < l.offerRows(); r++) parts.add(l.offer(r));
            for (Rect r : parts) assertTrue(l.inner.contains(r), where + ": " + r + " inside the card");
            apart(where, parts);
            Rect d = l.tradeDetail();
            for (Rect r : List.of(l.priceSlot(), l.creditSlot(), l.tradeButton())) assertTrue(d.contains(r), where + ": " + r + " in the trade column " + d);
            apart(where, List.of(l.priceSlot(), l.creditSlot(), l.tradeButton()));
            for (int i = 0; i < 36; i++) assertTrue(l.inventory().contains(l.inventorySlot(i)), where + ": slot " + i);
            assertEquals(l.inventorySlot(9).y() + 3 * DeedMapLayout.SLOT + 4, l.inventorySlot(0).y(), "Hotbar 4 px below the main rows, like Vanilla");
        }
    }
}
