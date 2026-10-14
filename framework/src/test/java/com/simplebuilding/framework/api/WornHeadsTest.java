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

    @Test
    void skullLooksSwapTypesMultiplyFactorsAndSurviveFailures() {
        assertEquals("plain", WornHeads.skullType("stack", "plain"));
        assertEquals(1.0F, WornHeads.skullFactor("plain"));
        assertEquals(0.0F, WornHeads.skullEdge("plain"));
        WornHeads.registerLook("look", new WornHeads.SkullLook() {
            @Override
            public Object type(Object stack, Object type) {
                return type + "/" + stack;
            }

            @Override
            public float factor(Object type) {
                return 0.5F;
            }

            @Override
            public float edge(Object type) {
                return 8.0F;
            }
        });
        WornHeads.registerLook("broken", new WornHeads.SkullLook() {
            @Override
            public float factor(Object type) {
                throw new IllegalStateException("broken");
            }
        });
        try {
            assertEquals("cat/siamese", WornHeads.skullType("siamese", "cat"));
            assertEquals(0.5F, WornHeads.skullFactor("cat"));
            assertEquals(8.0F, WornHeads.skullEdge("cat"));
        } finally {
            assertTrue(WornHeads.unregisterLook("look"));
            assertTrue(WornHeads.unregisterLook("broken"));
        }
        assertFalse(WornHeads.unregisterLook("look"));
        assertEquals(1.0F, WornHeads.skullFactor("cat"));
    }
}
