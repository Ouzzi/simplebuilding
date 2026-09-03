package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;

/**
 * Fabric adapter for the vein and strip miner enchantments.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link VeinAndStripMinerTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class VeinAndStripMinerGameTest {

    @GameTest(maxTicks = VeinAndStripMinerTests.DROP_MAX_TICKS, rotation = Rotation.NONE)
    public void veinMinerBreaksTheWholeVeinThroughTheBlockBreakEvent(GameTestHelper helper) {
        VeinAndStripMinerTests.veinMinerBreaksTheWholeVeinThroughTheBlockBreakEvent(helper);
    }

    @GameTest(maxTicks = VeinAndStripMinerTests.DROP_MAX_TICKS, rotation = Rotation.NONE)
    public void veinMinerFollowsLogsWithAnAxeThroughTheBlockBreakEvent(GameTestHelper helper) {
        VeinAndStripMinerTests.veinMinerFollowsLogsWithAnAxeThroughTheBlockBreakEvent(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void veinMinerRefusesNonOresAndTooWeakPickaxesAndDivergesFromTheHighlightOnQuartz(GameTestHelper helper) {
        VeinAndStripMinerTests.veinMinerRefusesNonOresAndTooWeakPickaxesAndDivergesFromTheHighlightOnQuartz(helper);
    }

    @GameTest(maxTicks = VeinAndStripMinerTests.DROP_MAX_TICKS, rotation = Rotation.NONE)
    public void stripMinerTunnelsAlongTheFacingAndRefundsDurabilityThroughTheBlockBreakEvent(GameTestHelper helper) {
        VeinAndStripMinerTests.stripMinerTunnelsAlongTheFacingAndRefundsDurabilityThroughTheBlockBreakEvent(helper);
    }
}
