package com.simplelib.test;

import com.simplelib.api.TrackingFilters;
import com.simplelib.api.portal.PortalRule;
import com.simplelib.api.portal.PortalState;
import com.simplelib.api.portal.PortalTransit;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;

/**
 * BACK22 core (docs/ai/PLAN-B-BACK22-KERN-2026-11-10.md K3/K4): the portal door rules, the turn between two doors and the fog
 * pulse fade - the world tests with real doors live in Simple Layers (its door, frame, double door, transit, preload).
 */
final class PortalLibTests {
    private PortalLibTests() {}

    static void register() {
        LibTests.ALL.put("portal_rule_states", PortalLibTests::ruleStates);
        LibTests.ALL.put("portal_turns", PortalLibTests::turns);
        LibTests.ALL.put("portal_pulse_fades", PortalLibTests::pulseFades);
    }

    /** two_way never changes; activate_once starts dormant and opens for good; break_on_transit breaks after a passage and mends. */
    static void ruleStates(GameTestHelper h) {
        PortalRule two = PortalRule.TWO_WAY, once = PortalRule.ACTIVATE_ONCE, brk = PortalRule.BREAK_ON_TRANSIT;
        h.assertTrue(two.initial() == PortalState.OPEN && two.afterTransit(PortalState.OPEN) == PortalState.OPEN && two.mend(PortalState.OPEN) == null, "two_way");
        h.assertTrue(once.initial() == PortalState.DORMANT && !once.canTransit(PortalState.DORMANT), "activate_once starts dormant");
        h.assertTrue(once.mend(PortalState.DORMANT) == PortalState.OPEN && once.afterTransit(PortalState.OPEN) == PortalState.OPEN && once.mend(PortalState.OPEN) == null, "activate_once stays open");
        h.assertTrue(brk.initial() == PortalState.OPEN && brk.afterTransit(PortalState.OPEN) == PortalState.BROKEN, "break_on_transit breaks");
        h.assertTrue(!brk.canTransit(PortalState.BROKEN) && brk.mend(PortalState.BROKEN) == PortalState.OPEN, "break_on_transit mends");
        h.assertTrue(PortalRule.get(brk.id()) == brk && PortalRule.get(once.id()) == once && PortalRule.get(two.id()) == two, "registry");
        h.assertTrue(TrackingFilters.size() >= 0, "tracking filters reachable");
        h.succeed();
    }

    /** The turn between the walking directions of two doors maps speed and look onto each other, for all 16 pairs. */
    static void turns(GameTestHelper h) {
        for (Direction from : Direction.Plane.HORIZONTAL) {
            for (Direction to : Direction.Plane.HORIZONTAL) {
                int q = PortalTransit.quarters(from, to);
                Vec3 v = PortalTransit.rotate(new Vec3(from.getStepX(), 0.3, from.getStepZ()), q);
                h.assertTrue(Math.abs(v.x - to.getStepX()) < 1e-9 && Math.abs(v.z - to.getStepZ()) < 1e-9 && Math.abs(v.y - 0.3) < 1e-9,
                        from + " -> " + to + " (" + q + " quarters) gives " + v);
                h.assertTrue(Math.floorMod((int) (from.toYRot() + 90 * q - to.toYRot()), 360) == 0, "yaw " + from + " -> " + to);
            }
        }
        h.succeed();
    }

    static void pulseFades(GameTestHelper h) {
        long len = 200_000_000L;
        h.assertTrue(com.simplelib.net.PortalPulsePayload.alpha(0, len) == 255 && com.simplelib.net.PortalPulsePayload.alpha(len / 2, len) == 128 && com.simplelib.net.PortalPulsePayload.alpha(len, len) == 0
                && com.simplelib.net.PortalPulsePayload.alpha(-1, len) == 0, "fade 255 -> 0");
        h.succeed();
    }
}
