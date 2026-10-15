package com.simplemaps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The unfolded cube. <b>What breaks this test:</b> a side without its own cell, a cell outside the 4 x 3 cross, or a click that picks another side. */
class CubeNetTest {
    @Test
    void everySideHasItsOwnCellInsideTheCross() {
        Set<Integer> cells = new HashSet<>();
        for (int f = CubeNet.NORTH; f <= CubeNet.DOWN; f++) {
            assertTrue(CubeNet.column(f) >= 0 && CubeNet.column(f) < CubeNet.COLS && CubeNet.row(f) >= 0 && CubeNet.row(f) < CubeNet.ROWS);
            assertTrue(cells.add(CubeNet.column(f) * 10 + CubeNet.row(f)), "cell of side " + f + " is unique");
            assertEquals(f, CubeNet.faceAtCell(CubeNet.column(f), CubeNet.row(f)));
        }
        assertEquals(CubeNet.FACES, cells.size());
    }

    @Test
    void theFrontSitsInTheMiddleOfTheCross() {
        assertEquals(CubeNet.FRONT, CubeNet.NORTH);
        assertEquals(1, CubeNet.column(CubeNet.FRONT));
        assertEquals(1, CubeNet.row(CubeNet.FRONT));
        assertEquals(CubeNet.column(CubeNet.UP), CubeNet.column(CubeNet.FRONT), "top above the front");
        assertEquals(CubeNet.row(CubeNet.UP) + 1, CubeNet.row(CubeNet.FRONT));
        assertEquals(CubeNet.row(CubeNet.DOWN), CubeNet.row(CubeNet.FRONT) + 1, "bottom below the front");
        assertEquals(CubeNet.column(CubeNet.WEST) + 1, CubeNet.column(CubeNet.FRONT), "left side left of the front");
        assertEquals(CubeNet.column(CubeNet.EAST), CubeNet.column(CubeNet.FRONT) + 1);
        assertEquals(CubeNet.column(CubeNet.SOUTH), CubeNet.column(CubeNet.EAST) + 1, "back next to the right side");
    }

    @Test
    void aClickPicksTheSideUnderIt() {
        int x = 100, y = 50, cell = 12;
        for (int f = CubeNet.NORTH; f <= CubeNet.DOWN; f++) {
            assertEquals(f, CubeNet.faceAt(x, y, cell, x + CubeNet.column(f) * cell + 6, y + CubeNet.row(f) * cell + 6));
        }
        assertEquals(0, CubeNet.faceAt(x, y, cell, x - 1, y + 14), "left of the net");
        assertEquals(0, CubeNet.faceAt(x, y, cell, x + 6, y + 6), "empty corner cell");
        assertEquals(0, CubeNet.faceAt(x, y, cell, x + 4 * cell, y + 14), "right of the net");
    }

    @Test
    void facesAreValidFrom0To6() {
        assertTrue(CubeNet.validFace(CubeNet.MODEL) && CubeNet.validFace(CubeNet.DOWN));
        assertTrue(!CubeNet.validFace(-1) && !CubeNet.validFace(7));
    }
}
