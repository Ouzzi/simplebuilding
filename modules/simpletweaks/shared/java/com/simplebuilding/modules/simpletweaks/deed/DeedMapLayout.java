package com.simplebuilding.modules.simpletweaks.deed;

import com.simplebuilding.modules.simpletweaks.deed.DeedLayout.Rect;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Geometry of the deed's card (WH26-9, owner 08.11.: variant A, light parchment folded in thirds), free of Minecraft
 * classes so a JUnit test ({@code DeedMapLayoutTest}) checks "nothing overlaps, everything inside" without the game.
 *
 * <pre>
 *  [Map ]+---------- management (2/3) ----------+-- locator (1/3) --+[Overworld]
 *  [Buy ]| Deed of Ouzzi                         | (C) 3 credits     |[Nether   ]
 *  [Righ]| Overworld: 11 chunks                  | +--------------+  |[End      ]
 *        |  grid: chunks that touch glue, others | | Chunk 12/-4  |  |[ >  next ]
 *        |  4 px apart, in their real order      | |   picture    |  |
 *        | result / hint line                    | +--------------+  |
 *        | [Claim here] [Give up]                | owner, rights ... |
 *        +---------------------------------------+-------------------+
 * </pre>
 *
 * Grid: every distinct chunk column (x) and row (z) gets one cell; two neighbouring columns that are also neighbours
 * in the world lie edge to edge ("kleben"), any jump in between leaves {@value #GAP} px. So adjacent chunks glue,
 * separate claims stand 4 px apart, and everything keeps its real compass order. The cell size is the largest zoom
 * step that fits ({@link #ZOOMS}); only above the smallest step the grid pans (drag).
 *
 * <p>The buy tab ({@code DeedTradeScreen}) uses the same card: offers left, the chosen trade right, the player's
 * inventory below, like a villager's trade screen.
 */
public final class DeedMapLayout {
    public static final int MAX_W = 330, MIN_W = 250, MAX_H = 214, MIN_H = 184;
    /** Side tabs: {@code TAB_W} wide, {@code TAB_OUT} of it outside the card, one every {@code TAB_STEP} px. */
    public static final int TAB_W = 30, TAB_H = 24, TAB_OUT = 24, TAB_STEP = 26, TAB_TOP = 10;
    public static final int BORDER = 6, GAP = 4, BUTTON_H = 20, LINE = 10, BADGE_H = 16, SLOT = 18, OFFER_H = 20;
    /** Cell sizes of the grid, large to small. */
    public static final int[] ZOOMS = {16, 12, 9, 6, 4};

    public record Chunk(int x, int z) {}
    /** A group of touching chunks (4-neighbourhood); bounds in chunk coordinates (inclusive). */
    public record Group(List<Chunk> chunks, int minX, int minZ, int maxX, int maxZ) {
        public int w() { return maxX - minX + 1; }
        public int h() { return maxZ - minZ + 1; }
    }
    /** One chunk's cell at {@code x, y} (content pixels), {@code group} = index into {@link Grid#groups}. */
    public record Tile(Chunk chunk, int group, int x, int y) {}
    /** The laid-out grid: {@code cell} px per chunk, content {@code width x height}. */
    public record Grid(List<Group> groups, List<Tile> tiles, int cell, int width, int height) {
        /** The tile under a content point, or null. */
        public Tile at(double x, double y) {
            for (Tile t : tiles) if (x >= t.x() && x < t.x() + cell && y >= t.y() && y < t.y() + cell) return t;
            return null;
        }
        public Tile of(Chunk c) {
            for (Tile t : tiles) if (t.chunk().equals(c)) return t;
            return null;
        }
    }

    public final Rect panel, inner, manage, locator, title, sub, area, info, badge, card, details;
    /** The two fold lines (x): at a third and two thirds of the card. */
    public final int fold1, fold2;
    /** Picture scale in the locator card (16 map pixels each). */
    public final int scale;

    private DeedMapLayout(Rect panel) {
        this.panel = panel;
        inner = new Rect(panel.x() + BORDER, panel.y() + BORDER, panel.w() - 2 * BORDER, panel.h() - 2 * BORDER - 2);
        fold1 = inner.x() + inner.w() / 3;
        fold2 = inner.x() + 2 * inner.w() / 3;
        manage = new Rect(inner.x(), inner.y(), fold2 - inner.x(), inner.h());
        locator = new Rect(fold2 + 2, inner.y(), inner.right() - fold2 - 2, inner.h());
        title = new Rect(manage.x() + 4, manage.y() + 3, manage.w() - 8, LINE);
        sub = new Rect(title.x(), title.bottom() + 2, title.w(), LINE);
        int buttonsY = manage.bottom() - BUTTON_H - 3;
        info = new Rect(title.x(), buttonsY - LINE - 3, title.w(), LINE);
        area = new Rect(title.x(), sub.bottom() + 5, title.w(), Math.max(16, info.y() - 4 - sub.bottom() - 5));
        badge = new Rect(locator.x() + 2, locator.y() + 2, locator.w() - 4, BADGE_H);
        scale = Math.max(2, Math.min(4, (locator.w() - 16) / 16));
        card = new Rect(locator.x() + 2, badge.bottom() + 4, locator.w() - 4, 16 * scale + 8 + LINE + 4);
        details = new Rect(locator.x() + 4, card.bottom() + 4, locator.w() - 8, Math.max(0, locator.bottom() - card.bottom() - 6));
    }

    /** The card: as large as the screen allows with the side tabs, up to {@value #MAX_W} x {@value #MAX_H}. */
    public static DeedMapLayout of(int screenW, int screenH) {
        int w = Math.max(MIN_W, Math.min(MAX_W, screenW - 2 * TAB_OUT - 8)), h = Math.max(MIN_H, Math.min(MAX_H, screenH - 8));
        return new DeedMapLayout(new Rect((screenW - w) / 2, (screenH - h) / 2, w, h));
    }

    // ------------------------------------------------------------------ tabs, buttons

    /** Left tab {@code i} (0 Map, 1 Buy, 2 Rights): sticks out {@value #TAB_OUT} px, its inner end under the frame. */
    public Rect leftTab(int i) { return new Rect(panel.x() - TAB_OUT, panel.y() + TAB_TOP + i * TAB_STEP, TAB_W, TAB_H); }
    /** Right tab {@code i} (dimensions, the last one may be "next page"). */
    public Rect rightTab(int i) { return new Rect(panel.right() - (TAB_W - TAB_OUT), panel.y() + TAB_TOP + i * TAB_STEP, TAB_W, TAB_H); }
    /** How many right tabs fit along the card. */
    public int rightSlots() { return Math.max(2, (panel.h() - TAB_TOP - 4) / TAB_STEP); }

    /**
     * The dimension tabs shown on page {@code page}: all when they fit in {@code slots}, else {@code slots - 1} per page
     * and the last tab is "next" (wraps to the first page). Returns the indices; {@code -1} marks the "next" tab.
     */
    public static int[] tabPage(int count, int slots, int page) {
        if (count <= slots) { int[] all = new int[count]; for (int i = 0; i < count; i++) all[i] = i; return all; }
        int per = Math.max(1, slots - 1), pages = (count + per - 1) / per, p = Math.floorMod(page, pages);
        int from = p * per, n = Math.min(per, count - from);
        int[] out = new int[n + 1];
        for (int i = 0; i < n; i++) out[i] = from + i;
        out[n] = -1;
        return out;
    }
    public static int tabPages(int count, int slots) { return count <= slots ? 1 : (count + Math.max(1, slots - 1) - 1) / Math.max(1, slots - 1); }

    /** Bottom buttons of the management side: 0 Claim here, 1 Give up. */
    public Rect button(int i) {
        int w = (manage.w() - 8 - GAP) / 2;
        return new Rect(manage.x() + 4 + i * (w + GAP), manage.bottom() - BUTTON_H - 3, w, BUTTON_H);
    }
    /** The confirmation box over the grid (give up a chunk). */
    public Rect confirm() {
        int w = Math.min(area.w() - 8, 200), h = Math.min(area.h(), 70);
        return new Rect(area.x() + (area.w() - w) / 2, area.y() + (area.h() - h) / 2, w, h);
    }
    public Rect confirmButton(int i) {
        Rect c = confirm();
        int w = Math.min(80, (c.w() - 3 * GAP) / 2);
        return new Rect(i == 0 ? c.x() + c.w() / 2 - GAP / 2 - w : c.x() + c.w() / 2 + GAP / 2, c.bottom() - 4 - BUTTON_H, w, BUTTON_H);
    }
    /** The chunk picture inside the locator card. */
    public Rect picture() {
        int s = 16 * scale;
        return new Rect(card.x() + (card.w() - s) / 2, card.y() + 4 + LINE + 2, s, s);
    }
    /** The small "x" that hides the Simple Maps hint (bottom line of the locator). */
    public Rect hintClose() { return new Rect(locator.right() - 11, locator.bottom() - LINE - 1, 9, LINE); }

    // ------------------------------------------------------------------ buy tab (trade screen)

    /** Header of the buy tab: title left, the credits badge right. */
    public Rect tradeTitle() { return new Rect(inner.x() + 4, inner.y() + 4, inner.w() - 8 - tradeBadge().w() - GAP, LINE); }
    public Rect tradeBadge() { int w = Math.min(90, inner.w() / 3); return new Rect(inner.right() - 3 - w, inner.y() + 2, w, BADGE_H); }
    /** The player's inventory (3 rows + hotbar, 18 px slots) at the bottom of the card, centred. */
    public Rect inventory() { return new Rect(inner.x() + (inner.w() - 9 * SLOT) / 2, inner.bottom() - 4 * SLOT - 4 - 3, 9 * SLOT, 4 * SLOT + 4); }
    /** Slot {@code i} of the inventory: 0..8 hotbar (bottom row), 9..35 the three rows above (Vanilla order). */
    public Rect inventorySlot(int i) {
        Rect inv = inventory();
        int col = i % 9, row = i < 9 ? 3 : i / 9 - 1;
        return new Rect(inv.x() + col * SLOT, inv.y() + row * SLOT + (i < 9 ? 4 : 0), SLOT, SLOT);
    }
    /** How many offer rows fit between the header and the inventory. */
    public int offerRows() { return Math.max(1, (inventory().y() - 4 - (tradeBadge().bottom() + 3)) / OFFER_H); }
    /** Offer row {@code row} (left column, like a villager's list). */
    public Rect offer(int row) { return new Rect(inner.x() + 3, tradeBadge().bottom() + 3 + row * OFFER_H, Math.min(100, inner.w() / 2 - 8), OFFER_H - 2); }
    /** Right column: the chosen trade. */
    public Rect tradeDetail() {
        Rect o = offer(0);
        int x = o.right() + 8;
        return new Rect(x, o.y(), inner.right() - 4 - x, inventory().y() - 4 - o.y());
    }
    public Rect priceSlot() { Rect d = tradeDetail(); return new Rect(d.x() + 4, d.y() + LINE + 6, SLOT, SLOT); }
    public Rect creditSlot() { Rect d = tradeDetail(), p = priceSlot(); return new Rect(Math.min(d.right() - SLOT - 4, p.right() + 36), p.y(), SLOT, SLOT); }
    public Rect tradeButton() { Rect d = tradeDetail(), p = priceSlot(); return new Rect(d.x() + 4, p.bottom() + 6, Math.min(100, d.w() - 8), BUTTON_H); }

    // ------------------------------------------------------------------ grid

    /** Groups of 4-touching chunks, the largest first (then north to south, west to east). */
    public static List<Group> groups(List<Chunk> chunks) {
        Set<Chunk> left = new HashSet<>(chunks);
        var out = new ArrayList<Group>();
        for (Chunk start : chunks) {
            if (!left.remove(start)) continue;
            var members = new ArrayList<Chunk>();
            var queue = new ArrayDeque<Chunk>();
            queue.add(start);
            while (!queue.isEmpty()) {
                Chunk c = queue.poll();
                members.add(c);
                for (Chunk n : List.of(new Chunk(c.x() + 1, c.z()), new Chunk(c.x() - 1, c.z()), new Chunk(c.x(), c.z() + 1), new Chunk(c.x(), c.z() - 1)))
                    if (left.remove(n)) queue.add(n);
            }
            int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (Chunk c : members) { minX = Math.min(minX, c.x()); minZ = Math.min(minZ, c.z()); maxX = Math.max(maxX, c.x()); maxZ = Math.max(maxZ, c.z()); }
            members.sort(Comparator.comparingInt(Chunk::z).thenComparingInt(Chunk::x));
            out.add(new Group(List.copyOf(members), minX, minZ, maxX, maxZ));
        }
        out.sort(Comparator.comparingInt((Group g) -> -g.chunks().size()).thenComparingInt(Group::minZ).thenComparingInt(Group::minX));
        return out;
    }

    /**
     * The grid of {@code chunks} in a {@code maxW x maxH} area. {@code zoom} = index into {@link #ZOOMS} chosen by the
     * player, or -1: the largest step that fits (the smallest when none does).
     */
    public static Grid grid(List<Chunk> chunks, int maxW, int maxH, int zoom) {
        List<Group> groups = groups(chunks);
        if (chunks.isEmpty()) return new Grid(groups, List.of(), ZOOMS[0], 0, 0);
        int[] xs = chunks.stream().mapToInt(Chunk::x).distinct().sorted().toArray();
        int[] zs = chunks.stream().mapToInt(Chunk::z).distinct().sorted().toArray();
        int cell = ZOOMS[ZOOMS.length - 1];
        if (zoom >= 0 && zoom < ZOOMS.length) cell = ZOOMS[zoom];
        else for (int c : ZOOMS) if (extent(xs, c) <= maxW && extent(zs, c) <= maxH) { cell = c; break; }
        int[] px = positions(xs, cell), pz = positions(zs, cell);
        Map<Chunk,Integer> groupOf = new HashMap<>();
        for (int g = 0; g < groups.size(); g++) for (Chunk c : groups.get(g).chunks()) groupOf.put(c, g);
        var tiles = new ArrayList<Tile>();
        for (Chunk c : chunks) {
            if (!groupOf.containsKey(c)) continue;
            tiles.add(new Tile(c, groupOf.get(c), px[Arrays.binarySearch(xs, c.x())], pz[Arrays.binarySearch(zs, c.z())]));
        }
        return new Grid(groups, List.copyOf(tiles), cell, extent(xs, cell), extent(zs, cell));
    }

    /** Pixel start of each sorted coordinate: neighbours in the world glue, any jump leaves {@link #GAP} px. */
    static int[] positions(int[] sorted, int cell) {
        int[] out = new int[sorted.length];
        for (int i = 1; i < sorted.length; i++) out[i] = out[i - 1] + cell + ((long) sorted[i] - sorted[i - 1] > 1 ? GAP : 0);
        return out;
    }
    static int extent(int[] sorted, int cell) { return sorted.length == 0 ? 0 : positions(sorted, cell)[sorted.length - 1] + cell; }
}
