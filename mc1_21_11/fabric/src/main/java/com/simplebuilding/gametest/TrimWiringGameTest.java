package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for how the armour trim bonuses reach the game.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link TrimWiringTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class TrimWiringGameTest {

    @GameTest
    public void theSurvivalFactorTracksDistanceAndTimeSinceTheLastDeath(GameTestHelper helper) {
        TrimWiringTests.theSurvivalFactorTracksDistanceAndTimeSinceTheLastDeath(helper);
    }

    @GameTest
    public void theCombatFactorWeighsKillsAndDamageByMobCategory(GameTestHelper helper) {
        TrimWiringTests.theCombatFactorWeighsKillsAndDamageByMobCategory(helper);
    }

    @GameTest
    public void theTrackerSurvivesTheSaveAndRebasesOnlyOnDeath(GameTestHelper helper) {
        TrimWiringTests.theTrackerSurvivesTheSaveAndRebasesOnlyOnDeath(helper);
    }

    @GameTest
    public void thePlayerMixinDeliversSpeedHungerAndExperienceBehindItsGuards(GameTestHelper helper) {
        TrimWiringTests.thePlayerMixinDeliversSpeedHungerAndExperienceBehindItsGuards(helper);
    }

    @GameTest
    public void everyServerSideHitRunsThroughTheTrimDamageModifier(GameTestHelper helper) {
        TrimWiringTests.everyServerSideHitRunsThroughTheTrimDamageModifier(helper);
    }

    @GameTest
    public void coastHoldsTheAirSupplyAndSilenceLowersTheVisibility(GameTestHelper helper) {
        TrimWiringTests.coastHoldsTheAirSupplyAndSilenceLowersTheVisibility(helper);
    }

    @GameTest
    public void theTickDrivenTrimEffectsFireOnTheirOwnCadence(GameTestHelper helper) {
        TrimWiringTests.theTickDrivenTrimEffectsFireOnTheirOwnCadence(helper);
    }

    @GameTest
    public void theThreeTrimMaterialsKeepTheirColoursAndTheirTags(GameTestHelper helper) {
        TrimWiringTests.theThreeTrimMaterialsKeepTheirColoursAndTheirTags(helper);
    }

    @GameTest
    public void theTrimMultiplierCommandGuardsItsRangeAndItsPermission(GameTestHelper helper) {
        TrimWiringTests.theTrimMultiplierCommandGuardsItsRangeAndItsPermission(helper);
    }
}
