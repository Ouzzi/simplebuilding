package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the hidden chain above the last pad tiers ({@link TweaksEasterTests}). No logic
 * here; class and method names are load bearing, Fabric derives the test id from them. Registered via
 * the {@code fabric-gametest} entrypoint in fabric.mod.json.
 */
public final class TweaksEasterGameTest {

    @GameTest
    public void theLastTierSmithsBackIntoDontDoItThatWorksLikeTierOne(GameTestHelper helper) {
        TweaksEasterTests.theLastTierSmithsBackIntoDontDoItThatWorksLikeTierOne(helper);
    }

    @GameTest
    public void easterChainNamesEveryStageAndCostsWhatTheNormalTiersCost(GameTestHelper helper) {
        TweaksEasterTests.easterChainNamesEveryStageAndCostsWhatTheNormalTiersCost(helper);
    }

    @GameTest
    public void theFinalEasterPadIsTwiceAsStrongAsTheLastTier(GameTestHelper helper) {
        TweaksEasterTests.theFinalEasterPadIsTwiceAsStrongAsTheLastTier(helper);
    }

    @GameTest
    public void theEasterAdvancementsAreHiddenAndFireAlongTheChain(GameTestHelper helper) {
        TweaksEasterTests.theEasterAdvancementsAreHiddenAndFireAlongTheChain(helper);
    }

    @GameTest
    public void theFunnyStickIsSmithedFromTheFinalPadAndSparklesInTheHand(GameTestHelper helper) {
        TweaksEasterTests.theFunnyStickIsSmithedFromTheFinalPadAndSparklesInTheHand(helper);
    }

    @GameTest
    public void theEasterEggsAreHiddenFromRecipeViewersAndCreativeTabs(GameTestHelper helper) {
        TweaksEasterTests.theEasterEggsAreHiddenFromRecipeViewersAndCreativeTabs(helper);
    }
}
