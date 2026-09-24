package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the pistons that push or break unbreakable blocks.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link PistonBreachTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class PistonBreachGameTest {

    @GameTest(maxTicks = PistonBreachTests.PAID_PUSH_MAX_TICKS)
    public void reinforcedPistonsPushOneUnbreakableOnlyWhenTheRedstoneBlockPays(GameTestHelper helper) {
        PistonBreachTests.reinforcedPistonsPushOneUnbreakableOnlyWhenTheRedstoneBlockPays(helper);
    }

    @GameTest(skyAccess = true, maxTicks = PistonBreachTests.LIMIT_MAX_TICKS)
    public void reinforcedBreachCountsTowardsTheLimitAndOnlyReachesTheFrontBlock(GameTestHelper helper) {
        PistonBreachTests.reinforcedBreachCountsTowardsTheLimitAndOnlyReachesTheFrontBlock(helper);
    }

    @GameTest(maxTicks = PistonBreachTests.STICKY_MAX_TICKS)
    public void stickyReinforcedPistonPushesTheUnbreakableButNeverPullsItBack(GameTestHelper helper) {
        PistonBreachTests.stickyReinforcedPistonPushesTheUnbreakableButNeverPullsItBack(helper);
    }

    @GameTest(maxTicks = PistonBreachTests.SACRIFICE_MAX_TICKS)
    public void netheritePistonSacrificeLeavesNothingBehindAndNeedsTheRedstoneBlock(GameTestHelper helper) {
        PistonBreachTests.netheritePistonSacrificeLeavesNothingBehindAndNeedsTheRedstoneBlock(helper);
    }

    @GameTest(maxTicks = PistonBreachTests.ENDERITE_MAX_TICKS)
    public void enderitePistonBreachesThreeCellsSkippingAirAndStoppingAtImmuneBlocks(GameTestHelper helper) {
        PistonBreachTests.enderitePistonBreachesThreeCellsSkippingAirAndStoppingAtImmuneBlocks(helper);
    }

    @GameTest(maxTicks = PistonBreachTests.HEAD_MAX_TICKS)
    public void breakingTheHeadOfModPistonsBreaksThePistonToo(GameTestHelper helper) {
        PistonBreachTests.breakingTheHeadOfModPistonsBreaksThePistonToo(helper);
    }

    @GameTest(maxTicks = PistonBreachTests.IMMUNE_MAX_TICKS)
    public void immuneBlocksNeverMoveOrBreak(GameTestHelper helper) {
        PistonBreachTests.immuneBlocksNeverMoveOrBreak(helper);
    }

    @GameTest(maxTicks = PistonBreachTests.EXPLOSION_MAX_TICKS)
    public void explosionsCannotDeleteUnbreakableBlocksWhileTheyMove(GameTestHelper helper) {
        PistonBreachTests.explosionsCannotDeleteUnbreakableBlocksWhileTheyMove(helper);
    }

    @GameTest
    public void endPortalFramesBreachOnlyWhileTheirConfigOptionIsOn(GameTestHelper helper) {
        PistonBreachTests.endPortalFramesBreachOnlyWhileTheirConfigOptionIsOn(helper);
    }

    @GameTest
    public void reinforcedStickyPistonCraftsFromSlimeAndDropsItself(GameTestHelper helper) {
        PistonBreachTests.reinforcedStickyPistonCraftsFromSlimeAndDropsItself(helper);
    }
}
