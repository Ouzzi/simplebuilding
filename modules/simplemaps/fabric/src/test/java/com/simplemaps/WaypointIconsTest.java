package com.simplemaps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

/** Waypoint icons. <b>What breaks this test:</b> an old waypoint colour that no longer maps to its wool, or a random icon pool with doubles or gaps. */
class WaypointIconsTest {
    @Test
    void oldColoursBecomeTheWoolOfTheNearestDye() {
        assertEquals(Identifier.withDefaultNamespace("orange_wool"), WaypointIcons.forColor(0xFFF9801D));
        assertEquals(Identifier.withDefaultNamespace("white_wool"), WaypointIcons.forColor(0xFFFFFFFF));
        assertEquals(Identifier.withDefaultNamespace("black_wool"), WaypointIcons.forColor(0xFF000000));
        assertEquals(Identifier.withDefaultNamespace("red_wool"), WaypointIcons.forColor(0xFFC03020));
        for (int color : Waypoint.DEFAULT_COLORS) assertTrue(WaypointIcons.forColor(color).getPath().endsWith("_wool"));
    }

    @Test
    void theRandomPoolIsUniqueAndDeterministicPerSeed() {
        assertEquals(WaypointIcons.POOL.size(), new HashSet<>(WaypointIcons.POOL).size());
        assertTrue(WaypointIcons.POOL.size() >= 20);
        assertEquals(WaypointIcons.random(new Random(7)), WaypointIcons.random(new Random(7)));
        HashSet<Identifier> seen = new HashSet<>();
        Random random = new Random(1);
        for (int i = 0; i < 200; i++) seen.add(WaypointIcons.random(random));
        assertTrue(seen.size() > 10, "varied: " + seen.size());
        assertNotEquals(1, seen.size());
    }

    @Test
    void aWaypointWithoutIconTakesTheColourWool() {
        Waypoint w = new Waypoint(0, 1, 2, "x", 0xFFF9801D, null, 99);
        assertEquals(Identifier.withDefaultNamespace("orange_wool"), w.icon());
        assertEquals(CubeNet.FRONT, w.face(), "an invalid side becomes the front");
    }
}
