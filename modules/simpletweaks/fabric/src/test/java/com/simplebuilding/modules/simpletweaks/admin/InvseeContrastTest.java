package com.simplebuilding.modules.simpletweaks.admin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.simplebuilding.modules.simpletweaks.admin.invsee.client.InvseeColors;
import com.simplelib.api.client.ui.UiContrast;
import org.junit.jupiter.api.Test;

/** Rule 15: every text of the admin inventory screen is readable (contrast at least 4.5:1). */
class InvseeContrastTest {
    @Test void everyLabelIsReadable() {
        for (int[] pair : InvseeColors.PAIRS) {
            double ratio = UiContrast.ratio(pair[0], pair[1]);
            System.out.printf("invsee contrast %08X on %08X = %.2f:1%n", pair[0], pair[1], ratio);
            assertTrue(ratio >= UiContrast.MIN, String.format("%08X on %08X is %.2f:1", pair[0], pair[1], ratio));
        }
    }
}
