package com.simplelib.api.client.ui;

/**
 * Faint marks inside a box (owner image 4), one kind per block material - exactly the shapes of {@code MOTIFS} in
 * {@code tools/ui/simplecontainers_preview.py}: each shape is a set of dark pixels (0.90 x fill) and light pixels
 * (halfway to the light colour). {@link #LEATHER} (stitched seam) and {@link #METAL} (corner rivets) add a fixed
 * pattern ({@link UiBoxes#motif}). Plain data, no client classes.
 */
public enum UiMotif {
    /** No marks (inventory box). */
    NONE(),
    /** Grain strokes. */
    WOOD(shape(new int[][] {{0, 0}, {1, 0}, {2, 0}, {3, 0}, {4, 0}, {4, 1}}, new int[0][]), shape(new int[][] {{0, 1}, {0, 0}, {1, 0}, {2, 0}}, new int[0][]), shape(new int[][] {{0, 0}, {1, 0}, {2, 0}, {3, 0}, {4, 0}, {5, 0}, {6, 0}}, new int[0][]), shape(new int[][] {{1, 0}, {2, 0}, {3, 0}, {0, 1}, {4, 1}}, new int[0][])),
    /** Branched cracks with a light lip (image 4). */
    STONE(shape(new int[][] {{0, 1}, {1, 1}, {2, 2}, {3, 2}, {4, 3}, {5, 3}, {6, 2}, {7, 1}, {4, 4}, {4, 5}, {3, 6}}, new int[][] {{0, 2}, {1, 2}, {2, 3}, {3, 3}, {5, 4}, {6, 3}, {5, 5}}), shape(new int[][] {{0, 4}, {1, 3}, {2, 3}, {3, 2}, {4, 1}, {4, 0}, {5, 2}, {6, 2}, {7, 3}, {2, 4}, {2, 5}}, new int[][] {{1, 4}, {3, 3}, {5, 3}, {6, 3}, {3, 5}}), shape(new int[][] {{0, 0}, {1, 0}, {2, 1}, {3, 1}, {3, 2}, {4, 3}}, new int[][] {{0, 1}, {1, 1}, {2, 2}, {4, 4}})),
    /** Rivets in the inner corners plus scratches. */
    METAL(shape(new int[][] {{1, 1}}, new int[][] {{0, 0}, {1, 0}, {0, 1}}), shape(new int[][] {{0, 0}, {1, 1}, {2, 2}}, new int[][] {{1, 0}, {2, 1}})),
    /** Smoke wisps. */
    SMOKE(shape(new int[][] {{0, 2}, {1, 1}, {2, 1}, {3, 2}, {4, 2}, {5, 1}}, new int[0][]), shape(new int[][] {{0, 0}, {1, 1}, {2, 1}, {3, 0}}, new int[0][])),
    /** Netherite flakes. */
    NETHER(shape(new int[][] {{0, 2}, {1, 1}, {2, 0}}, new int[][] {{1, 2}, {2, 1}}), shape(new int[][] {{0, 0}, {1, 0}}, new int[][] {{0, 1}, {1, 1}})),
    /** Sparkle pluses. */
    ENDER(shape(new int[0][], new int[][] {{1, 0}, {0, 1}, {1, 1}, {2, 1}, {1, 2}}), shape(new int[][] {{0, 0}}, new int[0][]), shape(new int[0][], new int[][] {{0, 0}})),
    /** Scale arcs. */
    SHULKER(shape(new int[][] {{0, 0}, {1, 1}, {2, 1}, {3, 0}}, new int[][] {{1, 0}, {2, 0}}), shape(new int[][] {{0, 1}, {1, 0}, {2, 0}, {3, 1}}, new int[0][])),
    /** Stitched seam 2 on / 2 off. */
    LEATHER(),
    /** Redstone traces. */
    REDSTONE(shape(new int[][] {{0, 0}, {1, 0}, {2, 1}, {3, 1}, {4, 1}}, new int[0][]), shape(new int[][] {{0, 0}, {1, 1}, {1, 2}}, new int[0][])),
    /** Woven dots. */
    WOOL(shape(new int[][] {{0, 0}, {2, 0}, {1, 1}, {0, 2}, {2, 2}}, new int[0][]), shape(new int[][] {{0, 0}, {1, 0}, {2, 0}}, new int[][] {{0, 1}, {2, 1}})),
    /** Dotted ruling. */
    PAPER(shape(new int[][] {{0, 0}, {2, 0}, {4, 0}, {6, 0}}, new int[0][]), shape(new int[][] {{0, 0}, {0, 2}, {0, 4}}, new int[0][])),
    /** Glints. */
    GLASS(shape(new int[0][], new int[][] {{0, 2}, {1, 1}, {2, 0}}), shape(new int[0][], new int[][] {{0, 3}, {1, 2}, {2, 1}, {3, 0}})),
    /** Runes. */
    RUNE(shape(new int[][] {{0, 0}, {1, 0}, {1, 1}, {1, 2}, {2, 2}}, new int[0][]), shape(new int[][] {{0, 0}, {0, 1}, {1, 1}, {2, 1}, {2, 0}}, new int[0][]), shape(new int[][] {{1, 0}, {0, 1}, {2, 1}, {1, 2}}, new int[0][]));

    /** One shape: dark pixel offsets and light pixel offsets ({@code {dx, dy}}). */
    public record Shape(int[][] dark, int[][] light) {
        /** Width and height of the shape's bounding box. */
        public int width() {
            return extent(0);
        }

        public int height() {
            return extent(1);
        }

        private int extent(int axis) {
            int m = 0;
            for (int[] p : dark) m = Math.max(m, p[axis] + 1);
            for (int[] p : light) m = Math.max(m, p[axis] + 1);
            return m;
        }
    }

    private final Shape[] shapes;

    UiMotif(Shape... shapes) {
        this.shapes = shapes;
    }

    private static Shape shape(int[][] dark, int[][] light) {
        return new Shape(dark, light);
    }

    /** The scattered shapes (empty for {@link #NONE} and {@link #LEATHER}). */
    public Shape[] shapes() {
        return shapes.clone();
    }

    int count() {
        return shapes.length;
    }

    Shape shape(int i) {
        return shapes[i];
    }
}
