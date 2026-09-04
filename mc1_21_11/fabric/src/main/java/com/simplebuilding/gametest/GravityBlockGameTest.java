package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the pistons and the gravity blocks.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link GravityBlockTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class GravityBlockGameTest {

    @GameTest(maxTicks = GravityBlockTests.PUSH_LIMIT_MAX_TICKS)
    public void reinforcedPistonMovesEighteenBlocksWhileTheNetheriteOneKeepsVanillasTwelve(GameTestHelper helper) {
        GravityBlockTests.reinforcedPistonMovesEighteenBlocksWhileTheNetheriteOneKeepsVanillasTwelve(helper);
    }

    @GameTest(maxTicks = GravityBlockTests.BREAK_THRESHOLD_MAX_TICKS)
    public void netheritePistonBreaksOnlyWhatTheSignalStrengthCanAfford(GameTestHelper helper) {
        GravityBlockTests.netheritePistonBreaksOnlyWhatTheSignalStrengthCanAfford(helper);
    }

    @GameTest(maxTicks = GravityBlockTests.RETRACTION_MAX_TICKS)
    public void modPistonsAreNotStickyAndUseTheVanillaHead(GameTestHelper helper) {
        GravityBlockTests.modPistonsAreNotStickyAndUseTheVanillaHead(helper);
    }

    @GameTest(maxTicks = GravityBlockTests.PISTON_VERSUS_PISTON_MAX_TICKS)
    public void extendedModPistonsCannotBeShovedByOtherPistons(GameTestHelper helper) {
        GravityBlockTests.extendedModPistonsCannotBeShovedByOtherPistons(helper);
    }

    @GameTest(maxTicks = GravityBlockTests.RISE_CURVE_MAX_TICKS)
    public void levitatingSandLeavesOnVanillasScheduleAndRisesOnItsCurve(GameTestHelper helper) {
        GravityBlockTests.levitatingSandLeavesOnVanillasScheduleAndRisesOnItsCurve(helper);
    }

    @GameTest(maxTicks = GravityBlockTests.CEILING_WAIT_MAX_TICKS)
    public void levitatingSandWaitsUnderTheCeilingUntilTheWayUpIsFree(GameTestHelper helper) {
        GravityBlockTests.levitatingSandWaitsUnderTheCeilingUntilTheWayUpIsFree(helper);
    }

    @GameTest(maxTicks = GravityBlockTests.BLOCKED_LANDING_MAX_TICKS)
    public void blockedLandingSpotsDropTheBlockOrKeepItFlying(GameTestHelper helper) {
        GravityBlockTests.blockedLandingSpotsDropTheBlockOrKeepItFlying(helper);
    }

    @GameTest(maxTicks = GravityBlockTests.COLLISION_MAX_TICKS)
    public void suspendedSandLetsItemsThroughWhileSuspendedGravelHoldsThem(GameTestHelper helper) {
        GravityBlockTests.suspendedSandLetsItemsThroughWhileSuspendedGravelHoldsThem(helper);
    }

    @GameTest
    public void gravityBlocksAndPistonsCarryTheirRegisteredStrengthAndTags(GameTestHelper helper) {
        GravityBlockTests.gravityBlocksAndPistonsCarryTheirRegisteredStrengthAndTags(helper);
    }

    @GameTest
    public void gravityBlockRecipesCraftFromTheirDocumentedPatterns(GameTestHelper helper) {
        GravityBlockTests.gravityBlockRecipesCraftFromTheirDocumentedPatterns(helper);
    }
}
