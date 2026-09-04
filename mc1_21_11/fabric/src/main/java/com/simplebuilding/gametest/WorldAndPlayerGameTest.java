package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for ore generation, the air jump, void protection, config and item frames.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link WorldAndPlayerTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class WorldAndPlayerGameTest {

    @GameTest
    public void endOresDropTheirDustAndFollowFortuneWhileSilkTouchKeepsTheOre(GameTestHelper helper) {
        WorldAndPlayerTests.endOresDropTheirDustAndFollowFortuneWhileSilkTouchKeepsTheOre(helper);
    }

    @GameTest
    public void endOreBlocksKeepTheirStrengthLightAndDiamondToolRequirement(GameTestHelper helper) {
        WorldAndPlayerTests.endOreBlocksKeepTheirStrengthLightAndDiamondToolRequirement(helper);
    }

    @GameTest
    public void breakingAnEndOreAwardsThreeToSevenExperience(GameTestHelper helper) {
        WorldAndPlayerTests.breakingAnEndOreAwardsThreeToSevenExperience(helper);
    }

    @GameTest
    public void lockedFramesStillAnswerTheMagnetWhileOtherSneakClicksFallThrough(GameTestHelper helper) {
        WorldAndPlayerTests.lockedFramesStillAnswerTheMagnetWhileOtherSneakClicksFallThrough(helper);
    }

    @GameTest
    public void theDoubleJumpEnchantmentKeepsItsLevelsWeightCostsAndBootSlot(GameTestHelper helper) {
        WorldAndPlayerTests.theDoubleJumpEnchantmentKeepsItsLevelsWeightCostsAndBootSlot(helper);
    }

    @GameTest
    public void theServerOnlyCreditsTheBootsForAnAirJump(GameTestHelper helper) {
        WorldAndPlayerTests.theServerOnlyCreditsTheBootsForAnAirJump(helper);
    }

    @GameTest
    public void enderiteArmourSwallowsVoidDamageExceptOnItsIntervalTick(GameTestHelper helper) {
        WorldAndPlayerTests.enderiteArmourSwallowsVoidDamageExceptOnItsIntervalTick(helper);
    }

    @GameTest
    public void enderiteSlowFallNeedsTwoPiecesFallingSpeedAndTheJumpKey(GameTestHelper helper) {
        WorldAndPlayerTests.enderiteSlowFallNeedsTwoPiecesFallingSpeedAndTheJumpKey(helper);
    }

    @GameTest
    public void modLootPoolsKeepTheirExactCountAndTheAirJumpBookWeights(GameTestHelper helper) {
        WorldAndPlayerTests.modLootPoolsKeepTheirExactCountAndTheAirJumpBookWeights(helper);
    }
}
