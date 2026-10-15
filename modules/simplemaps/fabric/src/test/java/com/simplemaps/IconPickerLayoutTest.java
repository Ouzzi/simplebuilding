package com.simplemaps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Geometry of the "Configure waypoint" dialog. <b>What breaks this test:</b> a part of the dialog outside it or outside the
 * window, two parts that overlap, or an icon slot that is not found under its own pixels.
 */
class IconPickerLayoutTest {
    private static final int[][] SCREENS = {{320, 240}, {427, 240}, {480, 270}, {640, 360}, {854, 480}, {1920, 1080}};

    @Test
    void dialogStaysOnTheScreenAndHoldsItsParts() {
        for (int[] s : SCREENS) {
            IconPickerLayout l = IconPickerLayout.of(s[0], s[1]);
            String at = s[0] + "x" + s[1];
            assertTrue(l.x() >= 0 && l.x() + l.w() <= s[0] && l.y() >= 0 && l.y() + l.h() <= s[1], "on screen at " + at);
            int right = l.x() + l.w(), bottom = l.y() + l.h();
            assertTrue(l.gridX() + l.gridW() <= right && l.gridY() + l.gridH() <= l.modeY(), "grid above the display row at " + at);
            assertTrue(l.netX() + CubeNet.COLS * IconPickerLayout.NET_CELL <= right, "net inside at " + at);
            assertTrue(l.netY() >= l.modeY() && l.netY() + CubeNet.ROWS * IconPickerLayout.NET_CELL <= l.modeY() + IconPickerLayout.MODE_H, "net in its row at " + at);
            assertTrue(l.previewX() + 32 <= l.modeButtonX() && l.modeButtonX() + l.modeButtonW() <= l.netX(), "preview, button and net side by side at " + at);
            assertTrue(l.modeY() + IconPickerLayout.MODE_H <= l.buttonY() && l.buttonY() + IconPickerLayout.BUTTON_H <= bottom, "buttons below at " + at);
            assertTrue(l.gridRows() >= IconPickerLayout.MIN_ROWS && l.gridRows() <= IconPickerLayout.MAX_ROWS);
            assertTrue(l.searchY() >= l.nameY() + IconPickerLayout.FIELD_H && l.gridY() >= l.searchY() + IconPickerLayout.FIELD_H, "fields stacked at " + at);
        }
    }

    @Test
    void largerWindowsShowMoreRows() {
        assertTrue(IconPickerLayout.of(854, 480).gridRows() > IconPickerLayout.of(320, 200).gridRows());
    }

    @Test
    void everySlotIsFoundUnderItsOwnPixels() {
        IconPickerLayout l = IconPickerLayout.of(854, 480);
        for (int row = 0; row < l.gridRows(); row++) {
            for (int col = 0; col < IconPickerLayout.GRID_COLS; col++) {
                int x = l.gridX() + col * IconPickerLayout.SLOT, y = l.gridY() + row * IconPickerLayout.SLOT;
                assertEquals(row * IconPickerLayout.GRID_COLS + col, l.slotAt(x + 8, y + 8));
            }
        }
        assertEquals(-1, l.slotAt(l.gridX() - 1, l.gridY()));
        assertEquals(-1, l.slotAt(l.gridX(), l.gridY() + l.gridH()));
    }
}
