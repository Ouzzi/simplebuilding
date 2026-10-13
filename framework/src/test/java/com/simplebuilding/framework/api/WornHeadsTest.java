package com.simplebuilding.framework.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The extra head slot contract (Simple Mobheads): providers add heads, null entries and failing providers count as none. */
class WornHeadsTest {

    @Test
    void providersAddTheirHeadsAndFailuresAreSkipped() {
        assertTrue(WornHeads.extra("player").isEmpty());
        WornHeads.register("slot", entity -> entity.equals("player") ? Arrays.asList("cat_head", null) : List.of());
        WornHeads.register("broken", entity -> {
            throw new IllegalStateException("boom");
        });
        try {
            assertEquals(List.of("cat_head"), WornHeads.extra("player"));
            assertTrue(WornHeads.extra("zombie").isEmpty());
        } finally {
            assertTrue(WornHeads.unregister("slot"));
            assertTrue(WornHeads.unregister("broken"));
        }
        assertFalse(WornHeads.unregister("slot"));
        assertTrue(WornHeads.extra("player").isEmpty());
    }
}
