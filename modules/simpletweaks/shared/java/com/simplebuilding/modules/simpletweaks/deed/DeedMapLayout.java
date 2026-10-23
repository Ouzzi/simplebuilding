package com.simplebuilding.modules.simpletweaks.deed;

import com.simplebuilding.modules.simpletweaks.deed.DeedLayout.Rect;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Geometry of the deed's map view (SP22-8 §4), free of Minecraft classes so a JUnit test ({@code DeedMapLayoutTest})
 * checks "nothing overlaps, everything inside the sheet" without booting the game.
 *
 * <pre>
 * title                                           (chunks, credits)
 * info line (next purchase)
 * [Overworld][Nether][End][more...]           [>]
 * +-- sheet -------------------------------------+
 * | overview: tiles 2x2 slots, groups, gaps       |
 * | or the detail map of one chunk                |
 * +-----------------------------------------------+
 * hint "Simple Maps recommended"              [x]   (only without Simple Maps)
 * [Rights] [Buy]              [Claim here] [Done]
 * </pre>
 *
 * Overview: chunks that touch (4-neighbourhood) form a group and lie side by side at scale; groups are placed in their
 * real compass direction from each other, but the gap is squeezed to one tile (or the label width) with a dotted line
 * and a label "312 blocks NE" (edge-to-edge distance in blocks). Links follow a minimum spanning tree of the groups, so
 * every group hangs on its nearest neighbour and the lines do not cross each other's groups.
 */
public final class DeedMapLayout {
    /** One tile = 2x2 slots (36 px pitch), the picture is 32x32 (16x16 map pixels x 2). */
    public static final int TILE = 36, PICTURE = 32, PAD = 6;
    public static final int MAX_W = 380, MIN_W = 240, MAX_H = 250, MIN_H = 150, EDGE = 8, GAP = 4;
    public static final int TITLE_H = 10, TAB_H = 16, BUTTON_H = 20, HINT_H = 10, ARROW_W = 14;
    /** Detail map scales tried from large to small (16 map pixels each). */
    static final int[] SCALES = {8, 6, 4, 3, 2};

    public record Chunk(int x, int z) {}
    /** A group of touching chunks; bounds in chunk coordinates (inclusive). */
    public record Group(List<Chunk> chunks, int minX, int minZ, int maxX, int maxZ) {
        public int w() { return maxX - minX + 1; }
        public int h() { return maxZ - minZ + 1; }
        boolean contains(Chunk c) { return chunks.contains(c); }
    }
    /** A tile at {@code x, y} (content pixels, top left of the 36 px cell). */
    public record Tile(Chunk chunk, int group, int x, int y) {
        public Rect rect() { return new Rect(x, y, TILE - 1, TILE - 1); }
    }
    /** A squeezed gap: a dotted line {@code x1,y1 -> x2,y2}, its label (edge-to-edge {@code blocks}, compass {@code direction}). */
    public record Link(int from, int to, int x1, int y1, int x2, int y2, int blocks, String direction, Rect label) {}
    public record Overview(List<Group> groups, List<Tile> tiles, List<Link> links, Rect[] groupRects, int width, int height) {
        /** Content point to centre the view on at the start: the group of {@code here}, else the largest group. */
        public int[] focus(Chunk here) {
            int g = 0;
            for (int i = 0; i < groups.size(); i++) if (here != null && groups.get(i).contains(here)) { g = i; break; }
            Rect r = groupRects[g];
            return new int[]{r.x() + r.w() / 2, r.y() + r.h() / 2};
        }
        /** The tile under a content point, or null. */
        public Tile at(double x, double y) {
            for (Tile t : tiles) if (x >= t.x() && x < t.x() + TILE - 1 && y >= t.y() && y < t.y() + TILE - 1) return t;
            return null;
        }
    }

    public final Rect panel, title, info, tabs, sheet, hint, buttons;
    public final boolean showHint;

    private DeedMapLayout(Rect panel, boolean showHint) {
        this.panel = panel; this.showHint = showHint;
        int x = panel.x() + EDGE, w = panel.w() - 2 * EDGE;
        title = new Rect(x, panel.y() + 7, w, TITLE_H);
        info = new Rect(x, title.bottom() + 2, w, TITLE_H);
        tabs = new Rect(x, info.bottom() + 2, w, TAB_H);
        buttons = new Rect(x, panel.bottom() - 10 - BUTTON_H, w, BUTTON_H);
        hint = showHint ? new Rect(x, buttons.y() - GAP - HINT_H, w, HINT_H) : new Rect(x, buttons.y(), w, 0);
        int sheetBottom = (showHint ? hint.y() : buttons.y()) - GAP;
        sheet = new Rect(x, tabs.bottom(), w, Math.max(24, sheetBottom - tabs.bottom()));
    }

    /** The map view: as large as the screen allows up to {@value #MAX_W} x {@value #MAX_H}. */
    public static DeedMapLayout of(int screenW, int screenH, boolean showHint) {
        int w = Math.max(MIN_W, Math.min(MAX_W, screenW - 16)), h = Math.max(MIN_H, Math.min(MAX_H, screenH - 16));
        return new DeedMapLayout(new Rect((screenW - w) / 2, (screenH - h) / 2, w, h), showHint);
    }

    // ------------------------------------------------------------------ footer, tabs, hint, detail

    /** Footer: 0 Rights, 1 Buy (left), 2 Claim here, 3 Done (right). The claim button takes the spare width up to 130 px. */
    public Rect button(int index) {
        int small = Math.min(56, (buttons.w() - 3 * GAP) / 5);
        int claim = Math.min(130, buttons.w() - 3 * small - 3 * GAP);
        return switch (index) {
            case 0 -> new Rect(buttons.x(), buttons.y(), small, BUTTON_H);
            case 1 -> new Rect(buttons.x() + small + GAP, buttons.y(), small, BUTTON_H);
            case 2 -> new Rect(buttons.right() - small - GAP - claim, buttons.y(), claim, BUTTON_H);
            default -> new Rect(buttons.right() - small, buttons.y(), small, BUTTON_H);
        };
    }

    /** The small "x" that hides the hint for good (this client). */
    public Rect hintClose() { return new Rect(hint.right() - 9, hint.y(), 9, HINT_H); }

    /**
     * Tabs for the labels starting at {@code offset}, {@code widths} = label text widths. As many as fit; when not all
     * fit, the arrow button {@link #nextTabs} sits at the right end.
     */
    public List<Rect> tabs(int[] widths, int offset) {
        var out = new ArrayList<Rect>();
        int total = 0;
        for (int w : widths) total += Math.max(30, w + 10) + 1;
        int limit = total > tabs.w() || offset > 0 ? nextTabs().x() - 1 : tabs.right();
        int x = tabs.x();
        for (int i = Math.max(0, offset); i < widths.length; i++) {
            int w = Math.max(30, widths[i] + 10);
            if (x + w > limit) break;
            out.add(new Rect(x, tabs.y(), w, TAB_H));
            x += w + 1;
        }
        return out;
    }
    public Rect nextTabs() { return new Rect(tabs.right() - ARROW_W, tabs.y(), ARROW_W, TAB_H); }

    /** Largest detail scale whose 16x16 map fits beside the info column; at least 2. */
    public int detailScale() {
        for (int s : SCALES) if (16 * s + 8 <= sheet.h() && 16 * s <= sheet.w() / 2) return s;
        return 2;
    }
    /** The detail map (picture with a 3 px paper frame inside the sheet's left half). */
    public Rect detailMap() {
        int s = 16 * detailScale();
        return new Rect(sheet.x() + 7, sheet.y() + Math.max(4, (sheet.h() - s) / 2), s, s);
    }
    /** Text column right of the detail map. */
    public Rect detailInfo() {
        Rect m = detailMap();
        return new Rect(m.right() + 10, sheet.y() + 6, sheet.right() - m.right() - 16, sheet.h() - 12);
    }
    /** "Give up chunk ..." and "< Overview" under the detail text. */
    public Rect detailButton(int row) {
        Rect i = detailInfo();
        int w = Math.min(120, i.w());
        return new Rect(i.x(), sheet.bottom() - 6 - (2 - row) * (BUTTON_H + GAP) + GAP, w, BUTTON_H);
    }
    /** The confirmation box over the sheet (give up a chunk). */
    public Rect confirm() {
        int w = Math.min(sheet.w() - 16, 240), h = Math.min(sheet.h() - 8, 74);
        return new Rect(sheet.x() + (sheet.w() - w) / 2, sheet.y() + (sheet.h() - h) / 2, w, h);
    }
    public Rect confirmButton(int index) {
        Rect c = confirm();
        int w = Math.min(90, (c.w() - 3 * GAP) / 2);
        return new Rect(index == 0 ? c.x() + c.w() / 2 - GAP / 2 - w : c.x() + c.w() / 2 + GAP / 2, c.bottom() - 6 - BUTTON_H, w, BUTTON_H);
    }

    // ------------------------------------------------------------------ overview

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

    /** Edge-to-edge distance of two groups in blocks (0 = touching corners). */
    public static int blocks(Group a, Group b) {
        long gx = Math.max(0, (long) Math.max(a.minX(), b.minX()) - Math.min(a.maxX(), b.maxX()) - 1);
        long gz = Math.max(0, (long) Math.max(a.minZ(), b.minZ()) - Math.min(a.maxZ(), b.maxZ()) - 1);
        return (int) Math.min(Integer.MAX_VALUE, Math.round(Math.sqrt((double) gx * gx + (double) gz * gz) * 16));
    }

    /** Compass direction from {@code a} to {@code b}: {sx, sz} in -1..1 (north = -z), from the group centres. */
    static int[] direction(Group a, Group b) {
        double dx = (double) b.minX() + b.maxX() - a.minX() - a.maxX(), dz = (double) b.minZ() + b.maxZ() - a.minZ() - a.maxZ();
        if (dx == 0 && dz == 0) return new int[]{0, 1};
        double t = Math.tan(Math.toRadians(67.5));
        int sx = Math.abs(dx) > Math.abs(dz) * t ? (int) Math.signum(dx) : Math.abs(dz) > Math.abs(dx) * t ? 0 : (int) Math.signum(dx);
        int sz = Math.abs(dz) > Math.abs(dx) * t ? (int) Math.signum(dz) : Math.abs(dx) > Math.abs(dz) * t ? 0 : (int) Math.signum(dz);
        return new int[]{sx, sz};
    }
    /** "n", "ne", "e", ... (lang key suffix). */
    public static String compass(int sx, int sz) {
        return (sz < 0 ? "n" : sz > 0 ? "s" : "") + (sx > 0 ? "e" : sx < 0 ? "w" : "");
    }

    /**
     * The overview of {@code chunks}. {@code labelWidth.apply(blocks, direction)} = pixel width of a gap label; a
     * horizontal or diagonal gap is made wide enough for it (at least one tile), a vertical one is one tile high
     * (the label sits beside the line).
     */
    public static Overview overview(List<Chunk> chunks, BiFunction<Integer,String,Integer> labelWidth) {
        List<Group> groups = groups(chunks);
        int n = groups.size();
        Rect[] rects = new Rect[n];
        var links = new ArrayList<Link>();
        if (n == 0) return new Overview(groups, List.of(), List.of(), rects, 0, 0);
        // Prim's minimum spanning tree from the largest group.
        int[] parent = new int[n]; long[] best = new long[n]; boolean[] done = new boolean[n];
        java.util.Arrays.fill(best, Long.MAX_VALUE); java.util.Arrays.fill(parent, -1);
        best[0] = 0;
        var order = new ArrayList<Integer>();
        for (int k = 0; k < n; k++) {
            int u = -1;
            for (int i = 0; i < n; i++) if (!done[i] && (u < 0 || best[i] < best[u])) u = i;
            done[u] = true; order.add(u);
            for (int v = 0; v < n; v++) {
                if (done[v]) continue;
                long d = blocks(groups.get(u), groups.get(v));
                if (d < best[v]) { best[v] = d; parent[v] = u; }
            }
        }
        var placed = new ArrayList<Rect>();
        Map<Integer,int[]> dirs = new HashMap<>();
        for (int g : order) {
            Group group = groups.get(g);
            int w = group.w() * TILE, h = group.h() * TILE;
            if (parent[g] < 0) { rects[g] = new Rect(0, 0, w, h); placed.add(rects[g]); continue; }
            Rect p = rects[parent[g]];
            int[] d = direction(groups.get(parent[g]), group);
            dirs.put(g, d);
            int blocks = blocks(groups.get(parent[g]), group);
            int label = Math.max(0, labelWidth.apply(blocks, compass(d[0], d[1])));
            int gapX = Math.max(TILE, label + 12), gapY = TILE;
            int x = d[0] > 0 ? p.right() + gapX : d[0] < 0 ? p.x() - gapX - w : p.x() + (p.w() - w) / 2;
            int y = d[1] > 0 ? p.bottom() + gapY : d[1] < 0 ? p.y() - gapY - h : p.y() + (p.h() - h) / 2;
            Rect r = new Rect(x, y, w, h);
            // Push further out along the same direction until the group (with a margin) is free.
            for (int guard = 0; guard < 4 * n + 8 && collides(r, placed); guard++)
                r = new Rect(r.x() + d[0] * TILE, r.y() + d[1] * TILE, w, h);
            rects[g] = r; placed.add(r);
            links.add(link(parent[g], g, p, r, d, blocks, label));
        }
        // Normalise: everything (tiles and labels) starts at PAD.
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (Rect r : rects) { minX = Math.min(minX, r.x()); minY = Math.min(minY, r.y()); maxX = Math.max(maxX, r.right()); maxY = Math.max(maxY, r.bottom()); }
        for (Link l : links) { minX = Math.min(minX, l.label().x()); minY = Math.min(minY, l.label().y()); maxX = Math.max(maxX, l.label().right()); maxY = Math.max(maxY, l.label().bottom()); }
        int ox = PAD - minX, oy = PAD - minY;
        var tiles = new ArrayList<Tile>();
        for (int g = 0; g < n; g++) {
            Rect r = rects[g] = new Rect(rects[g].x() + ox, rects[g].y() + oy, rects[g].w(), rects[g].h());
            Group group = groups.get(g);
            for (Chunk c : group.chunks()) tiles.add(new Tile(c, g, r.x() + (c.x() - group.minX()) * TILE, r.y() + (c.z() - group.minZ()) * TILE));
        }
        var shifted = new ArrayList<Link>();
        for (Link l : links) shifted.add(new Link(l.from(), l.to(), l.x1() + ox, l.y1() + oy, l.x2() + ox, l.y2() + oy, l.blocks(), l.direction(),
                new Rect(l.label().x() + ox, l.label().y() + oy, l.label().w(), l.label().h())));
        return new Overview(groups, List.copyOf(tiles), List.copyOf(shifted), rects, maxX - minX + 2 * PAD, maxY - minY + 2 * PAD);
    }

    private static boolean collides(Rect r, List<Rect> placed) {
        Rect grown = new Rect(r.x() - 8, r.y() - 8, r.w() + 16, r.h() + 16);
        for (Rect p : placed) if (grown.overlaps(p)) return true;
        return false;
    }

    /** The line between the facing edges of two group rectangles and the place of its label. */
    private static Link link(int from, int to, Rect a, Rect b, int[] d, int blocks, int labelW) {
        // Endpoints: on the side facing the other group, at the middle of the overlap (or the near corner).
        int x1, y1, x2, y2;
        if (d[0] != 0) { x1 = d[0] > 0 ? a.right() - 1 : a.x(); x2 = d[0] > 0 ? b.x() : b.right() - 1; }
        else { int mid = mid(a.x(), a.right(), b.x(), b.right()); x1 = mid; x2 = mid; }
        if (d[1] != 0) { y1 = d[1] > 0 ? a.bottom() - 1 : a.y(); y2 = d[1] > 0 ? b.y() : b.bottom() - 1; }
        else { int mid = mid(a.y(), a.bottom(), b.y(), b.bottom()); y1 = mid; y2 = mid; }
        if (d[0] != 0 && d[1] == 0) { y1 = mid(a.y(), a.bottom(), b.y(), b.bottom()); y2 = y1; }
        int mx = (x1 + x2) / 2, my = (y1 + y2) / 2;
        // Horizontal and diagonal gaps: label centred above the line's middle; vertical gaps: beside the line.
        Rect label = d[0] != 0 ? new Rect(mx - labelW / 2, my - 11, labelW, 9) : new Rect(mx + 4, my - 4, labelW, 9);
        return new Link(from, to, x1, y1, x2, y2, blocks, compass(d[0], d[1]), label);
    }
    /** Middle of the overlap of [a0,a1) and [b0,b1), or the middle between them when they do not overlap. */
    private static int mid(int a0, int a1, int b0, int b1) {
        int lo = Math.max(a0, b0), hi = Math.min(a1, b1);
        return lo < hi ? (lo + hi) / 2 : (Math.min(a1, b1) + Math.max(a0, b0)) / 2;
    }
}
