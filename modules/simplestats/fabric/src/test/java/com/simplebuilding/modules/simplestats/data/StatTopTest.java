package com.simplebuilding.modules.simplestats.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** N78 tooltip top list. <b>What breaks this test:</b> a wrong order, ties that depend on the map order, zero entries shown, or more than n. */
class StatTopTest {
    @Test
    void largestFirstTiesByKeyAtMostN() {
        StatData.Detail d = new StatData.Detail(Map.of("minecraft:stone", 5L, "minecraft:dirt", 9L, "minecraft:andesite", 5L, "minecraft:sand", 0L, "minecraft:clay", 1L), 3L);
        List<String> keys = StatTop.of(d, 3).stream().map(Map.Entry::getKey).toList();
        assertEquals(List.of("minecraft:dirt", "minecraft:andesite", "minecraft:stone"), keys);
        assertEquals(4, StatTop.of(d, 10).size(), "entries with 0 are left out");
        assertTrue(StatTop.of(null, 3).isEmpty());
        assertTrue(StatTop.of(d, 0).isEmpty());
    }
}
