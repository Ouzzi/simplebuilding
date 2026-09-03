package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the protection and range enchantments.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link ProtectionAndRangeTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class ProtectionAndRangeGameTest {

    @GameTest
    public void kineticProtectionScalesWithLevelAndOnlyCoversItsOwnDamageTypes(GameTestHelper helper) {
        ProtectionAndRangeTests.kineticProtectionScalesWithLevelAndOnlyCoversItsOwnDamageTypes(helper);
    }

    @GameTest
    public void kineticProtectionActuallyReducesTheDamageThePlayerTakes(GameTestHelper helper) {
        ProtectionAndRangeTests.kineticProtectionActuallyReducesTheDamageThePlayerTakes(helper);
    }

    @GameTest
    public void rangeAddsBlockInteractionReachInTheMainHandOnly(GameTestHelper helper) {
        ProtectionAndRangeTests.rangeAddsBlockInteractionReachInTheMainHandOnly(helper);
    }

    @GameTest(maxTicks = ProtectionAndRangeTests.VOID_PROTECTION_MAX_TICKS)
    public void voidProtectionLiftsEnderiteBackIntoTheWorldWhileOtherItemsAreLost(GameTestHelper helper) {
        ProtectionAndRangeTests.voidProtectionLiftsEnderiteBackIntoTheWorldWhileOtherItemsAreLost(helper);
    }
}
