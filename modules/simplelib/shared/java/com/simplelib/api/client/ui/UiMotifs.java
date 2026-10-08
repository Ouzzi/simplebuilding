package com.simplelib.api.client.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Faint marks inside a box (owner image 4; W0-B README point 6): dark = 0.90 x fill, light = halfway to the light
 * colour, scattered by a fixed hash, never over the box's elements. Exactly {@code motif()} of
 * {@code tools/ui/simplecontainers_preview.py} (shapes, count, placement, seed); keep both in step. Client only.
 */
public final class UiMotifs {
    /** Motif kinds of the preview's palette table. */
    public enum Kind { NONE, WOOD, STONE, SMOKE, METAL, NETHER, ENDER, SHULKER, LEATHER, REDSTONE, WOOL, PAPER, GLASS, RUNE }

    /** One shape: dark pixels and light pixels as (dx, dy) pairs. */
    private record Shape(int[] dark, int[] light) {}

    private static Shape s(int[] dark, int[] light) {
        return new Shape(dark, light);
    }

    private static int[] p(int... xy) {
        return xy;
    }

    private static List<Shape> shapes(Kind kind) {
        return switch (kind) {
            case WOOD -> List.of(s(p(0, 0, 1, 0, 2, 0, 3, 0, 4, 0, 4, 1), p()), s(p(0, 1, 0, 0, 1, 0, 2, 0), p()),
                    s(p(0, 0, 1, 0, 2, 0, 3, 0, 4, 0, 5, 0, 6, 0), p()), s(p(1, 0, 2, 0, 3, 0, 0, 1, 4, 1), p()));
            case STONE -> List.of(
                    s(p(0, 1, 1, 1, 2, 2, 3, 2, 4, 3, 5, 3, 6, 2, 7, 1, 4, 4, 4, 5, 3, 6), p(0, 2, 1, 2, 2, 3, 3, 3, 5, 4, 6, 3, 5, 5)),
                    s(p(0, 4, 1, 3, 2, 3, 3, 2, 4, 1, 4, 0, 5, 2, 6, 2, 7, 3, 2, 4, 2, 5), p(1, 4, 3, 3, 5, 3, 6, 3, 3, 5)),
                    s(p(0, 0, 1, 0, 2, 1, 3, 1, 3, 2, 4, 3), p(0, 1, 1, 1, 2, 2, 4, 4)));
            case SMOKE -> List.of(s(p(0, 2, 1, 1, 2, 1, 3, 2, 4, 2, 5, 1), p()), s(p(0, 0, 1, 1, 2, 1, 3, 0), p()));
            case METAL -> List.of(s(p(1, 1), p(0, 0, 1, 0, 0, 1)), s(p(0, 0, 1, 1, 2, 2), p(1, 0, 2, 1)));
            case NETHER -> List.of(s(p(0, 2, 1, 1, 2, 0), p(1, 2, 2, 1)), s(p(0, 0, 1, 0), p(0, 1, 1, 1)));
            case ENDER -> List.of(s(p(), p(1, 0, 0, 1, 1, 1, 2, 1, 1, 2)), s(p(0, 0), p()), s(p(), p(0, 0)));
            case SHULKER -> List.of(s(p(0, 0, 1, 1, 2, 1, 3, 0), p(1, 0, 2, 0)), s(p(0, 1, 1, 0, 2, 0, 3, 1), p()));
            case REDSTONE -> List.of(s(p(0, 0, 1, 0, 2, 1, 3, 1, 4, 1), p()), s(p(0, 0, 1, 1, 1, 2), p()));
            case WOOL -> List.of(s(p(0, 0, 2, 0, 1, 1, 0, 2, 2, 2), p()), s(p(0, 0, 1, 0, 2, 0), p(0, 1, 2, 1)));
            case PAPER -> List.of(s(p(0, 0, 2, 0, 4, 0, 6, 0), p()), s(p(0, 0, 0, 2, 0, 4), p()));
            case GLASS -> List.of(s(p(), p(0, 2, 1, 1, 2, 0)), s(p(), p(0, 3, 1, 2, 2, 1, 3, 0)));
            case RUNE -> List.of(s(p(0, 0, 1, 0, 1, 1, 1, 2, 2, 2), p()), s(p(0, 0, 0, 1, 1, 1, 2, 1, 2, 0), p()),
                    s(p(1, 0, 0, 1, 2, 1, 1, 2), p()));
            default -> List.of();
        };
    }

    private UiMotifs() {}

    private static boolean overlaps(int x0, int y0, int x1, int y1, List<int[]> rects, int pad) {
        for (int[] b : rects) {
            if (x0 < b[2] + pad && x1 > b[0] - pad && y0 < b[3] + pad && y1 > b[1] - pad) return true;
        }
        return false;
    }

    /**
     * Marks of {@code kind} in the box interior {@code x, y, w, h} (the fill area, image coordinates), avoiding the
     * {@code avoid} rects (x0, y0, x1, y1, image coordinates); drawn shifted by {@code ox, oy} (the image's screen
     * position). {@code seed} varies the scatter per screen.
     */
    public static void draw(GuiGraphicsExtractor g, int ox, int oy, Kind kind, int x, int y, int w, int h, UiPalette p,
            List<int[]> avoid, int seed) {
        int dark = UiPalette.scale(p.fill(), 0.90), light = UiPalette.mix(p.fill(), p.light(), 0.5);
        if (kind == Kind.LEATHER) {
            for (int i = x + 2; i < x + w - 2; i++) {
                if ((i - x) % 4 >= 2) continue;
                for (int yy : new int[] {y + 1, y + h - 2}) {
                    if (!overlaps(i, yy, i + 1, yy + 1, avoid, 0)) px(g, ox + i, oy + yy, light);
                }
            }
            for (int j = y + 3; j < y + h - 3; j++) {
                if ((j - y) % 4 >= 2) continue;
                for (int xx : new int[] {x + 1, x + w - 2}) {
                    if (!overlaps(xx, j, xx + 1, j + 1, avoid, 0)) px(g, ox + xx, oy + j, light);
                }
            }
            return;
        }
        if (kind == Kind.METAL) {
            int[][] corners = {{x + 1, y + 1}, {x + w - 3, y + 1}, {x + 1, y + h - 3}, {x + w - 3, y + h - 3}};
            for (int[] c : corners) {
                if (overlaps(c[0], c[1], c[0] + 2, c[1] + 2, avoid, 0)) continue;
                g.fill(ox + c[0], oy + c[1], ox + c[0] + 2, oy + c[1] + 2, UiPalette.scale(p.fill(), 0.78));
                px(g, ox + c[0], oy + c[1], p.light());
            }
        }
        List<Shape> shapes = shapes(kind);
        if (shapes.isEmpty()) return;
        int n = Math.max(2, w * h / 650);
        List<int[]> placed = new ArrayList<>();
        for (int i = 0; i < n * 6 && placed.size() < n; i++) {
            long hsh = Integer.toUnsignedLong(UiFlames.hash(i + seed * 97, w * 31 + h));
            Shape sh = shapes.get((int) (hsh % shapes.size()));
            int sw = 0, shh = 0;
            for (int[] pts : new int[][] {sh.dark(), sh.light()}) {
                for (int k = 0; k < pts.length; k += 2) {
                    sw = Math.max(sw, pts[k] + 1);
                    shh = Math.max(shh, pts[k + 1] + 1);
                }
            }
            int sx = x + 2 + (int) ((hsh >> 4) % Math.max(1, w - sw - 4));
            int sy = y + 2 + (int) ((hsh >> 12) % Math.max(1, h - shh - 4));
            if (overlaps(sx, sy, sx + sw, sy + shh, avoid, 2) || overlaps(sx, sy, sx + sw, sy + shh, placed, 10)) continue;
            placed.add(new int[] {sx, sy, sx + sw, sy + shh});
            for (int k = 0; k < sh.dark().length; k += 2) px(g, ox + sx + sh.dark()[k], oy + sy + sh.dark()[k + 1], dark);
            for (int k = 0; k < sh.light().length; k += 2) px(g, ox + sx + sh.light()[k], oy + sy + sh.light()[k + 1], light);
        }
    }

    private static void px(GuiGraphicsExtractor g, int x, int y, int color) {
        g.fill(x, y, x + 1, y + 1, color);
    }
}
