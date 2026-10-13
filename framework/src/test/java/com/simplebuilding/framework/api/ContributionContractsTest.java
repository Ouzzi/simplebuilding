package com.simplebuilding.framework.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The contribution contracts of the module split (stage 3c): stations and guide chapters. */
class ContributionContractsTest {

    @Test
    void stationsKeepOrderReplaceInPlaceAndCopyTheirItems() {
        List<String> items = new ArrayList<>(List.of("simpleovens:netherite_furnace"));
        TestCentreStations.register("simpleovens", "ovens", "station.simpleovens.ovens", items);
        TestCentreStations.register("simplestorage", "chests", "station.simplestorage.chests", List.of("simplestorage:enderite_chest"));
        TestCentreStations.register("simpleovens", "ovens", "station.simpleovens.ovens", List.of("simpleovens:enderite_furnace"));
        items.add("simpleovens:changed_later");
        try {
            List<TestCentreStations.Station> all = TestCentreStations.all();
            assertEquals(List.of("simpleovens:ovens", "simplestorage:chests"), all.stream().map(TestCentreStations.Station::key).toList());
            assertEquals(List.of("simpleovens:enderite_furnace"), all.get(0).items());
            assertEquals(1, TestCentreStations.of("simplestorage").size());
            assertThrows(UnsupportedOperationException.class, () -> all.get(0).items().add("x"));
        } finally {
            assertTrue(TestCentreStations.unregister("simpleovens", "ovens"));
            assertTrue(TestCentreStations.unregister("simplestorage", "chests"));
        }
        assertFalse(TestCentreStations.unregister("simpleovens", "ovens"));
        assertTrue(TestCentreStations.all().isEmpty());
    }

    @Test
    void chaptersAreFoundByBookAndNeedTheirKeys() {
        GuideChapters.register(new GuideChapters.Chapter("simpleovens", "ovens", "machines", "simpleovens:netherite_furnace",
                List.of("simpleovens:netherite_furnace"), List.of(), "guide.simpleovens.ovens.title", "guide.simpleovens.ovens.text"));
        GuideChapters.register(new GuideChapters.Chapter("simplestorage", "chests", "storage", "simplestorage:enderite_chest",
                List.of(), List.of("minecraft:chest"), "guide.simplestorage.chests.title", "guide.simplestorage.chests.text"));
        try {
            assertEquals(1, GuideChapters.forBook("machines").size());
            assertEquals("simpleovens:ovens", GuideChapters.forBook("machines").get(0).key());
            assertEquals(2, GuideChapters.all().size());
            assertTrue(GuideChapters.forBook("end").isEmpty());
        } finally {
            GuideChapters.unregister("simpleovens", "ovens");
            GuideChapters.unregister("simplestorage", "chests");
        }
        assertThrows(NullPointerException.class, () -> new GuideChapters.Chapter("a", "b", "machines", "a:b", List.of(), List.of(), null, "t"));
        assertTrue(GuideChapters.all().isEmpty());
    }
}
