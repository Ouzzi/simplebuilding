package com.simplemaps;

/**
 * Dimension buttons of the mega map's title bar (owner B23-K2): up to {@link #PER_PAGE} buttons in a row; from six
 * dimensions on, pages of {@link #PER_PAGE} with a chevron left and right, and the free places of the last page are
 * disabled placeholders. Plain maths, no client classes, so tests can check it.
 */
public final class MegaTabs {
    public static final int PER_PAGE = 5, SIZE = 16, GAP = 2, CHEVRON_W = 9;

    private MegaTabs() {}

    public static boolean paged(int count) {
        return count > PER_PAGE;
    }

    public static int pages(int count) {
        return paged(count) ? (count + PER_PAGE - 1) / PER_PAGE : 1;
    }

    public static int clampPage(int page, int count) {
        return Math.max(0, Math.min(pages(count) - 1, page));
    }

    /** The page that shows dimension {@code index}. */
    public static int pageOf(int index, int count) {
        return paged(count) ? clampPage(index / PER_PAGE, count) : 0;
    }

    /** Dimension index of each button place on {@code page}; -1 = disabled placeholder. */
    public static int[] places(int count, int page) {
        if (!paged(count)) {
            int[] out = new int[count];
            for (int i = 0; i < count; i++) out[i] = i;
            return out;
        }
        int[] out = new int[PER_PAGE];
        int first = clampPage(page, count) * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) out[i] = first + i < count ? first + i : -1;
        return out;
    }

    /** Width of the whole row (buttons, gaps and, when paged, both chevrons). */
    public static int rowWidth(int count) {
        int buttons = paged(count) ? PER_PAGE : count;
        int width = buttons * SIZE + Math.max(0, buttons - 1) * GAP;
        return paged(count) ? width + 2 * (CHEVRON_W + GAP) : width;
    }
}
