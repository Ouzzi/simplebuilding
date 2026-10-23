package com.simplebuilding.modules.simpletweaks.deed.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.simplelib.api.client.ui.UiContrast;
import org.junit.jupiter.api.Test;

/** Rule 15: every text of the deed screens (map view, rights editor) reads with at least 4.5:1. */
class DeedStyleContrastTest {
    @Test void everyTextIsReadable() {
        for (int[] pair : DeedStyle.PAIRS) {
            double ratio = UiContrast.ratio(pair[0], pair[1]);
            System.out.printf("deed contrast %08X on %08X = %.2f:1%n", pair[0], pair[1], ratio);
            assertTrue(ratio >= UiContrast.MIN, String.format("%08X on %08X is %.2f:1", pair[0], pair[1], ratio));
        }
    }
}
