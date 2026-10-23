package com.simplebuilding.modules.simpletweaks.deed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.simplebuilding.modules.simpletweaks.deed.DeedLayout.Rect;
import com.simplebuilding.modules.simpletweaks.deed.DeedMapLayout.Chunk;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Geometry of the deed's map view (SP22-8 branch 2). <b>What breaks this test:</b> a panel part that leaves the panel
 * or overlaps another, tiles that are not 36 px apart inside a group, groups that overlap, a gap label that sits on a
 * tile, wrong distances or compass directions, tabs that overflow, a detail map that does not fit the sheet.
 */
class DeedMapLayoutTest {
    private static final int[][] SCREENS = {{240, 135}, {320, 180}, {320, 240}, {427, 240}, {480, 270}, {640, 360}, {960, 540}, {1920, 1080}};
    /** About 6 px per character, like the Minecraft font. */
    private static int label(int blocks, String dir) { return 6 * (blocks + " blocks " + dir).length(); }

    private static void apart(String where, List<Rect> parts) {
        for (int a = 0; a < parts.size(); a++)
            for (int b = a + 1; b < parts.size(); b++) assertFalse(parts.get(a).overlaps(parts.get(b)), where + ": " + parts.get(a) + " overlaps " + parts.get(b));
    }

    @Test
    void panelPartsInsideAndApart() {
        for (int[] s : SCREENS) for (boolean hint : new boolean[]{false, true}) {
            var l = DeedMapLayout.of(s[0], s[1], hint);
            String where = s[0] + "x" + s[1] + (hint ? " hint" : "");
            assertTrue(l.panel.w() >= DeedMapLayout.MIN_W && l.panel.w() <= DeedMapLayout.MAX_W, where + ": width 240..380");
            if (s[0] >= DeedMapLayout.MIN_W + 16 && s[1] >= DeedMapLayout.MIN_H + 16) assertTrue(new Rect(0, 0, s[0], s[1]).contains(l.panel), where + ": on screen");
            var parts = new ArrayList<>(List.of(l.title, l.info, l.tabs, l.sheet, l.button(0), l.button(1), l.button(2), l.button(3)));
            if (hint) parts.add(l.hint);
            for (Rect r : parts) assertTrue(l.panel.contains(r), where + ": " + r + " inside the panel");
            apart(where, parts);
            assertTrue(l.button(2).w() >= 50, where + ": claim button wide enough");
            // detail: map and its column inside the sheet, buttons below the text column
            Rect map = l.detailMap(), info = l.detailInfo();
            assertTrue(l.sheet.contains(map) && l.sheet.contains(info) && !map.overlaps(info), where + ": detail map " + map + " / info " + info);
            assertEquals(16 * l.detailScale(), map.w(), where + ": 16 pixels at the scale");
            for (int row = 0; row < 2; row++) assertTrue(l.sheet.contains(l.detailButton(row)), where + ": detail button " + row);
            assertFalse(l.detailButton(0).overlaps(l.detailButton(1)), where + ": detail buttons apart");
            Rect c = l.confirm();
            assertTrue(l.sheet.contains(c) && c.contains(l.confirmButton(0)) && c.contains(l.confirmButton(1)) && !l.confirmButton(0).overlaps(l.confirmButton(1)), where + ": confirmation box");
        }
        assertEquals(8, DeedMapLayout.of(1920, 1080, false).detailScale(), "Large screens: 128 px detail map like a Vanilla map");
    }

    @Test
    void tabsFitOrPage() {
        var l = DeedMapLayout.of(320, 240, false);
        int[] three = {50, 40, 40};
        var tabs = l.tabs(three, 0);
        assertEquals(3, tabs.size(), "Three Vanilla tabs fit");
        int[] many = new int[12];
        java.util.Arrays.fill(many, 60);
        var first = l.tabs(many, 0);
        assertTrue(first.size() < 12 && !first.isEmpty(), "Many tabs: a page");
        for (Rect r : first) assertTrue(l.tabs.contains(r) && !r.overlaps(l.nextTabs()), "Tab " + r + " inside the row, not on the arrow");
        apart("tabs", first);
    }

    @Test
    void groupsAreTouchingChunks() {
        var chunks = List.of(new Chunk(0, 0), new Chunk(1, 0), new Chunk(0, 1), new Chunk(1, 1), new Chunk(30, -12), new Chunk(3, 115), new Chunk(4, 116));
        var groups = DeedMapLayout.groups(chunks);
        assertEquals(4, groups.size(), "Diagonal chunks are separate groups");
        assertEquals(4, groups.get(0).chunks().size(), "Largest group first");
        assertEquals(2, groups.get(0).w());
        assertEquals(0, DeedMapLayout.blocks(groups.get(0), groups.get(0)), "Distance to itself");
    }

    @Test
    void squeezedGapsWithDistanceAndDirection() {
        // Home 2x2 at 0..1, an outpost 30 chunks east and 12 north, a farm 115 south.
        var chunks = List.of(new Chunk(0, 0), new Chunk(1, 0), new Chunk(0, 1), new Chunk(1, 1), new Chunk(30, -12), new Chunk(0, 115));
        var o = DeedMapLayout.overview(chunks, DeedMapLayoutTest::label);
        assertEquals(6, o.tiles().size());
        assertEquals(2, o.links().size(), "Three groups: two links (spanning tree)");
        var home = o.groupRects()[0];
        assertEquals(2 * DeedMapLayout.TILE, home.w(), "Group at scale: 2 tiles of 36 px");
        // tiles of a group lie side by side, 36 px apart
        var t00 = o.tiles().stream().filter(t -> t.chunk().equals(new Chunk(0, 0))).findFirst().orElseThrow();
        var t10 = o.tiles().stream().filter(t -> t.chunk().equals(new Chunk(1, 0))).findFirst().orElseThrow();
        var t01 = o.tiles().stream().filter(t -> t.chunk().equals(new Chunk(0, 1))).findFirst().orElseThrow();
        assertEquals(DeedMapLayout.TILE, t10.x() - t00.x()); assertEquals(t00.y(), t10.y());
        assertEquals(DeedMapLayout.TILE, t01.y() - t00.y());
        for (var l : o.links()) {
            var to = o.groups().get(l.to());
            if (to.minX() == 30) {
                assertEquals("ne", l.direction(), "Outpost lies north-east");
                // gap: 28 chunks in x (2..29), 11 in z (-11..-1) -> sqrt(28^2+11^2)*16 = 481
                assertEquals(481, l.blocks());
                Rect r = o.groupRects()[l.to()];
                assertTrue(r.x() >= home.right() && r.bottom() <= home.y(), "Drawn north-east of home");
                assertTrue(r.x() - home.right() <= Math.max(DeedMapLayout.TILE, label(481, "ne") + 12), "Gap squeezed to the label width");
            } else {
                assertEquals("s", l.direction(), "Farm lies south");
                assertEquals(113 * 16, l.blocks());
                Rect r = o.groupRects()[l.to()];
                assertEquals(DeedMapLayout.TILE, r.y() - home.bottom(), "Vertical gap = one tile");
            }
        }
        checkClean(o);
    }

    @Test
    void manyScatteredGroupsNeverOverlap() {
        var rnd = new Random(7);
        for (int run = 0; run < 40; run++) {
            var chunks = new ArrayList<Chunk>();
            int n = 1 + rnd.nextInt(64);
            while (chunks.size() < n) {
                var c = rnd.nextInt(3) == 0 ? new Chunk(rnd.nextInt(8), rnd.nextInt(8)) : new Chunk(rnd.nextInt(4000) - 2000, rnd.nextInt(4000) - 2000);
                if (!chunks.contains(c)) chunks.add(c);
            }
            var o = DeedMapLayout.overview(chunks, DeedMapLayoutTest::label);
            assertEquals(n, o.tiles().size(), "run " + run + ": every chunk one tile");
            assertEquals(o.groups().size() - 1, o.links().size(), "run " + run + ": spanning tree");
            checkClean(o);
        }
        var empty = DeedMapLayout.overview(List.of(), DeedMapLayoutTest::label);
        assertTrue(empty.tiles().isEmpty() && empty.width() == 0);
        var one = DeedMapLayout.overview(List.of(new Chunk(5, 5)), DeedMapLayoutTest::label);
        assertEquals(DeedMapLayout.PAD, one.tiles().get(0).x(), "One chunk: at the padding");
        assertTrue(one.focus(new Chunk(5, 5))[0] > 0);
    }

    /** No two groups overlap, tiles are inside the content, labels inside the content and off the tiles of their own two groups. */
    private static void checkClean(DeedMapLayout.Overview o) {
        Rect content = new Rect(0, 0, o.width(), o.height());
        apart("groups", List.of(o.groupRects()));
        for (var t : o.tiles()) {
            assertTrue(content.contains(t.rect()), "Tile " + t + " inside " + content);
            assertTrue(o.groupRects()[t.group()].contains(t.rect()), "Tile inside its group");
            assertEquals(t, o.at(t.x() + 10, t.y() + 10), "Hit test finds the tile");
        }
        for (var l : o.links()) {
            assertTrue(content.contains(l.label()), "Label " + l.label() + " inside " + content);
            assertFalse(l.label().overlaps(o.groupRects()[l.from()]) || l.label().overlaps(o.groupRects()[l.to()]), "Label " + l.label() + " off its groups");
        }
    }
}
