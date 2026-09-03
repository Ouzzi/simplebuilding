package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;

/**
 * Fabric adapter for the durability and cooldown costs.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link ConsumptionAndDurabilityTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class ConsumptionAndDurabilityGameTest {

    @GameTest(rotation = Rotation.NONE)
    public void chiselChargesDurabilityAndCooldownOnlyOutsideCreative(GameTestHelper helper) {
        ConsumptionAndDurabilityTests.chiselChargesDurabilityAndCooldownOnlyOutsideCreative(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void octantAndRotatorSpendOnePointOfWearPerAcceptedClick(GameTestHelper helper) {
        ConsumptionAndDurabilityTests.octantAndRotatorSpendOnePointOfWearPerAcceptedClick(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void buildingWandBillsOneBlockAndOnePointOfWearPerPlacement(GameTestHelper helper) {
        ConsumptionAndDurabilityTests.buildingWandBillsOneBlockAndOnePointOfWearPerPlacement(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void sledgehammerSecondaryUseWearsDownOnlyTheSurvivalPlayer(GameTestHelper helper) {
        ConsumptionAndDurabilityTests.sledgehammerSecondaryUseWearsDownOnlyTheSurvivalPlayer(helper);
    }

    @GameTest
    public void doubleJumpBootsWearDownForTheSurvivalPlayer(GameTestHelper helper) {
        ConsumptionAndDurabilityTests.doubleJumpBootsWearDownForTheSurvivalPlayer(helper);
    }
}
