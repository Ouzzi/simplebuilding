package com.simplelib.api.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Engraved symbols of the N12 container style instead of text (owner image 3; README of the W0-B previews, point 5):
 * a stroke in the slot colour with a 1 px light edge under it, sunk like the slots. Progress fills the symbol white
 * (image 4) from the left or from the top. Pixel art and numbers exactly as {@code tools/ui/simpleinterfaces_preview.py}
 * (ARROW, HEAT, SMOKE, ARROW_DOWN, CHECK, CROSS, PYRAMID, STAR); keep both in step. Client only.
 */
public final class UiSymbols {
    /** Progress colour (image 4: white arrow). */
    public static final int PROGRESS = 0xFFFFFFFF;

    /** A one-colour bitmap; {@code '#'} = stroke. */
    public record Bitmap(int width, int height, int[] xs, int[] ys, boolean[] grid) {
        public static Bitmap of(String... rows) {
            int n = 0, w = 0;
            for (String row : rows) {
                w = Math.max(w, row.length());
                for (char c : row.toCharArray()) if (c == '#') n++;
            }
            int[] xs = new int[n], ys = new int[n];
            int i = 0;
            for (int y = 0; y < rows.length; y++) {
                for (int x = 0; x < rows[y].length(); x++) {
                    if (rows[y].charAt(x) == '#') {
                        xs[i] = x;
                        ys[i++] = y;
                    }
                }
            }
            int maxX = 0, maxY = 0;
            for (int k = 0; k < n; k++) {
                maxX = Math.max(maxX, xs[k]);
                maxY = Math.max(maxY, ys[k]);
            }
            boolean[] grid = new boolean[(maxX + 1) * (maxY + 1)];
            for (int k = 0; k < n; k++) grid[ys[k] * (maxX + 1) + xs[k]] = true;
            return new Bitmap(maxX + 1, maxY + 1, xs, ys, grid);
        }

        /** Whether ({@code x}, {@code y}) is a stroke pixel. */
        public boolean has(int x, int y) {
            return x >= 0 && y >= 0 && x < width && y < height && grid[y * width + x];
        }
    }

    /** 22x15, image 3's chunky arrow (furnace, crafting table). */
    public static final Bitmap ARROW = Bitmap.of(
            "..............#.......",
            "..............##......",
            "..............###.....",
            "..............####....",
            "..............#####...",
            "###################...",
            "####################..",
            "#####################.",
            "####################..",
            "###################...",
            "..............#####...",
            "..............####....",
            "..............###.....",
            "..............##......",
            "..............#.......");
    /** 9x26, brewing progress (Vanilla's 9x28 spot). */
    public static final Bitmap ARROW_DOWN = Bitmap.of(
            "...###...", "...###...", "...###...", "...###...", "...###...", "...###...", "...###...", "...###...",
            "...###...", "...###...", "...###...", "...###...", "...###...", "...###...", "...###...", "...###...",
            "...###...", "...###...", "#########", ".#######.", "..#####..", "..#####..", "...###...", "...###...",
            "....#....", "....#....");
    /** 14x13: three rising heat waves (furnace, blast furnace). */
    public static final Bitmap HEAT = Bitmap.of(
            ".#....#....#..", "#....#....#...", "#....#....#...", ".#....#....#..", "..#....#....#.", "..#....#....#.",
            ".#....#....#..", "#....#....#...", "#....#....#...", ".#....#....#..", "..#....#....#.", "..#....#....#.",
            ".#....#....#..");
    /** 14x13: smoker - soft curls instead of sharp waves. */
    public static final Bitmap SMOKE = Bitmap.of(
            "..##....##....", ".#..#..#..#...", "....#.....#...", "...#.....#....", "..#.....#.....", "..#.....#.##..",
            "...#.....#..#.", "....#......#..", "....#.....#...", "...#.....#....", "..#.....#.....", "..#.....#.....",
            "...#.....#....");
    public static final Bitmap CROSS = Bitmap.of("#.....#", "##...##", ".##.##.", "..###..", ".##.##.", "##...##", "#.....#");
    public static final Bitmap CHECK = Bitmap.of("......#", ".....##", "#...##.", "##.##..", ".###...", "..#....");
    /** Beacon: primary powers (the pyramid). */
    public static final Bitmap PYRAMID = Bitmap.of("....#....", "...###...", "..#####..", ".#######.", "#########");
    /** Beacon: secondary power (the star). */
    public static final Bitmap STAR = Bitmap.of("...#...", "..###..", "#######", ".#####.", ".##.##.", "#.....#");

    /** 16x11, the small arrow of a 2x2 crafting grid (backpack, inventory, cartography table). */
    public static final Bitmap ARROW_SMALL = Bitmap.of(
            "..........#.....", "..........##....", "..........###...", "#############...", "##############..",
            "###############.", "##############..", "#############...", "..........###...", "..........##....",
            "..........#.....");
    /** 12x12 filter funnel (mod hopper filter key). */
    public static final Bitmap FUNNEL = Bitmap.of(
            "############", "#..........#", ".#........#.", "..#......#..", "...#....#...", "....#..#....",
            "....#..#....", "....#..#....", "....#..#....", "....#..#....", ".....##.....", "............");
    /** 7x7 two stacked boxes (stack multiplier of the mod chests). */
    public static final Bitmap STACK = Bitmap.of("..#####", "..#...#", "#####.#", "#...#.#", "#...###", "#...#..", "#####..");
    /** 12x12 redstone dust blob (powered sign: crafter, auto smither). */
    public static final Bitmap REDSTONE = Bitmap.of(
            "....#..#....", ".#.###.##...", "..#####.#.#.", ".########...", "#.#######.#.", ".#########..",
            "..########.#", ".#.######...", "...##.###.#.", "..#..#.#....", ".....#......", "............");
    /** 9x8 horseshoe, opening up (hoof panel). */
    public static final Bitmap HORSESHOE = Bitmap.of(
            "...###...", "..#####..", ".##...##.", ".#.....#.", "#.......#", "#.......#", "#.......#", "#.......#");
    // G3 (anvil, grindstone, cartography table, trading, inventory): same pixels as the preview tool.
    /** 11x11 plus (anvil, cartography table). */
    public static final Bitmap PLUS = Bitmap.of("....###....", "....###....", "....###....", "....###....", "###########",
            "###########", "###########", "....###....", "....###....", "....###....", "....###....");
    /** 19x19 grindstone wheel. */
    public static final Bitmap WHEEL = Bitmap.of(
            "......#######......", "....##.......##....", "...#...........#...", "..#.....###.....#..", ".#....#######....#.",
            ".#...###...###...#.", "#...##.......##...#", "#...##..###..##...#", "#..##..#####..##..#", "#..##..#####..##..#",
            "#..##..#####..##..#", "#...##..###..##...#", "#...##.......##...#", ".#...###...###...#.", ".#....#######....#.",
            "..#.....###.....#..", "...#...........#...", "....##.......##....", "......#######......");
    /** 6x8 hammer in front of the anvil's name field. */
    public static final Bitmap ANVIL_HAMMER = Bitmap.of(".#####....", ".#####....", ".#####....", "...#......", "...#......",
            "...#......", "...#......", "...#......");
    /** 7x7 experience orb (anvil cost, grindstone). */
    public static final Bitmap XP = Bitmap.of("..###..", ".#####.", "###.###", "##...##", "###.###", ".#####.", "..###..");
    /** 8x7 trade arrow of a villager offer. */
    public static final Bitmap TRADE_ARROW = Bitmap.of("....#...", "....##..", "#######.", "########", "#######.", "....##..",
            "....#...");

    private UiSymbols() {}

    /** Engraved like image 3: stroke in the slot colour, a 1 px light edge under it. */
    public static void engrave(GuiGraphicsExtractor g, Bitmap bm, int x, int y, UiPalette p) {
        engrave(g, bm, x, y, p, p.slot(), true);
    }

    /** {@link #engrave} in {@code color}; {@code light} = with the light edge under the stroke. */
    public static void engrave(GuiGraphicsExtractor g, Bitmap bm, int x, int y, UiPalette p, int color, boolean light) {
        int[] xs = bm.xs(), ys = bm.ys();
        if (light) {
            for (int k = 0; k < xs.length; k++) {
                if (!bm.has(xs[k], ys[k] + 1)) g.fill(x + xs[k], y + ys[k] + 1, x + xs[k] + 1, y + ys[k] + 2, p.light());
            }
        }
        for (int k = 0; k < xs.length; k++) g.fill(x + xs[k], y + ys[k], x + xs[k] + 1, y + ys[k] + 1, color);
    }

    /** Engraved symbol filled white up to {@code frac} (0..1) of its width, or of its height when {@code vertical}. */
    public static void progress(GuiGraphicsExtractor g, Bitmap bm, int x, int y, UiPalette p, float frac, boolean vertical) {
        engrave(g, bm, x, y, p);
        if (frac <= 0) return;
        double limit = frac * (vertical ? bm.height() : bm.width());
        int[] xs = bm.xs(), ys = bm.ys();
        for (int k = 0; k < xs.length; k++) {
            if ((vertical ? ys[k] : xs[k]) < limit) g.fill(x + xs[k], y + ys[k], x + xs[k] + 1, y + ys[k] + 1, PROGRESS);
        }
    }
}
