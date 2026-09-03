package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the glowing armour and the wearable light source.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link DynamicLightTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class DynamicLightGameTest {

    @GameTest
    public void theTwoLevelCountersKeepTheirOwnStorageAndCaps(GameTestHelper helper) {
        DynamicLightTests.theTwoLevelCountersKeepTheirOwnStorageAndCaps(helper);
    }

    @GameTest
    public void bothSmithingUpgradesAddOneLevelPerStepAndStopAtTheirCap(GameTestHelper helper) {
        DynamicLightTests.bothSmithingUpgradesAddOneLevelPerStepAndStopAtTheirCap(helper);
    }

    @GameTest
    public void theSmithingUpgradeOnlyFiresForArmourAndTheMatchingMaterial(GameTestHelper helper) {
        DynamicLightTests.theSmithingUpgradeOnlyFiresForArmourAndTheMatchingMaterial(helper);
    }

    @GameTest
    public void theSmithingTableTakesTheModTemplatesAndKeepsTheVanillaOnes(GameTestHelper helper) {
        DynamicLightTests.theSmithingTableTakesTheModTemplatesAndKeepsTheVanillaOnes(helper);
    }

    @GameTest
    public void wornEmissionLevelsAddUpIntoTheLightBlockOverThePlayersHead(GameTestHelper helper) {
        DynamicLightTests.wornEmissionLevelsAddUpIntoTheLightBlockOverThePlayersHead(helper);
    }

    @GameTest
    public void theLightBlockOnlyReplacesAirOrWaterSourcesAndPutsTheWaterBack(GameTestHelper helper) {
        DynamicLightTests.theLightBlockOnlyReplacesAirOrWaterSourcesAndPutsTheWaterBack(helper);
    }

    @GameTest
    public void theLightFollowsThePlayerAndGoesOutWithTheArmour(GameTestHelper helper) {
        DynamicLightTests.theLightFollowsThePlayerAndGoesOutWithTheArmour(helper);
    }

    @GameTest(maxTicks = DynamicLightTests.TICK_WIRING_MAX_TICKS)
    public void theServerTickWiringLightsTheWearerOnItsOwn(GameTestHelper helper) {
        DynamicLightTests.theServerTickWiringLightsTheWearerOnItsOwn(helper);
    }
}
