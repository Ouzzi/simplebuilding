package com.simplemaps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Static over unexplored map pixels (owner 19.10.). <b>What breaks this test:</b> a noise that covers the map (too opaque) or stands still. */
class JamNoiseTest {
    @Test
    void faintTintedAndAnimated() {
        int tint = JamNoise.tint(WayfinderMapItem.Kind.END);
        Set<Integer> frames = new HashSet<>();
        for (long tick = 0; tick < 40; tick += 2) {
            int argb = JamNoise.argb(tint, 5, 9, tick);
            int a = argb >>> 24;
            assertTrue(a > 0 && a <= JamNoise.MAX_ALPHA, "alpha " + a);
            assertTrue((argb >> 16 & 0xFF) <= (tint >> 16 & 0xFF) && (argb & 0xFF) <= (tint & 0xFF), "never brighter than the tint");
            frames.add(argb);
        }
        assertTrue(frames.size() > 5, "changes over time");
        assertEquals(JamNoise.argb(tint, 5, 9, 10), JamNoise.argb(tint, 5, 9, 11), "same picture within two ticks");
    }
}
