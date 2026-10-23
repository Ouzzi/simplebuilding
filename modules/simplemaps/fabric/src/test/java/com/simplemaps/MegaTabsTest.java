package com.simplemaps;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Dimension buttons of the mega map (owner B23-K2). <b>What breaks this test:</b> paging before six dimensions, a page with
 * more than five buttons, or a last page whose free places are not disabled placeholders.
 */
class MegaTabsTest {
    @Test
    void upToFiveInOneRow() {
        assertFalse(MegaTabs.paged(5));
        assertArrayEquals(new int[] {0, 1, 2}, MegaTabs.places(3, 0));
        assertArrayEquals(new int[] {0, 1, 2, 3, 4}, MegaTabs.places(5, 7));
        assertEquals(5 * 16 + 4 * 2, MegaTabs.rowWidth(5));
    }

    @Test
    void sixAndMorePageWithPlaceholders() {
        assertTrue(MegaTabs.paged(6));
        assertEquals(2, MegaTabs.pages(6));
        assertArrayEquals(new int[] {0, 1, 2, 3, 4}, MegaTabs.places(6, 0));
        assertArrayEquals(new int[] {5, -1, -1, -1, -1}, MegaTabs.places(6, 1));
        assertArrayEquals(new int[] {10, 11, -1, -1, -1}, MegaTabs.places(12, 2));
        assertArrayEquals(new int[] {10, 11, -1, -1, -1}, MegaTabs.places(12, 9), "page clamped");
        assertEquals(2, MegaTabs.pageOf(11, 12));
        assertEquals(0, MegaTabs.pageOf(4, 5));
        assertEquals(MegaTabs.rowWidth(5) + 2 * (MegaTabs.CHEVRON_W + MegaTabs.GAP), MegaTabs.rowWidth(6));
    }
}
