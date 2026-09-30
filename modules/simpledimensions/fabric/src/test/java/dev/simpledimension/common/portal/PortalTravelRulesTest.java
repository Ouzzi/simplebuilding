package dev.simpledimension.common.portal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortalTravelRulesTest {

    @Test
    void identityRatioKeepsCoordinates() {
        BlockPos3i source = new BlockPos3i(128, 75, -42);
        assertEquals(source, PortalTravelRules.toTarget(source, 1.0));
        assertEquals(source, PortalTravelRules.toSource(source, 1.0));
    }

    @Test
    void netherLikeRatioCompressesIntoTargetAndPreservesY() {
        // 8 source blocks per target block (vanilla nether ratio).
        BlockPos3i source = new BlockPos3i(800, 70, -160);
        BlockPos3i target = PortalTravelRules.toTarget(source, 8.0);
        assertEquals(100, target.x());
        assertEquals(-20, target.z());
        assertEquals(70, target.y()); // Y unscaled
    }

    @Test
    void returningToSourceExpandsByTheRatio() {
        BlockPos3i target = new BlockPos3i(100, 70, -20);
        BlockPos3i source = PortalTravelRules.toSource(target, 8.0);
        assertEquals(800, source.x());
        assertEquals(-160, source.z());
        assertEquals(70, source.y());
    }

    @Test
    void nonPositiveRatioIsClampedToMinimum() {
        BlockPos3i source = new BlockPos3i(64, 64, 64);
        assertEquals(new BlockPos3i(128,64,128), PortalTravelRules.toTarget(source, 0.0));
        assertEquals(new BlockPos3i(32,64,32), PortalTravelRules.toSource(source, -3.0));
    }
}
