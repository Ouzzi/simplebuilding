package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for placing (queue N24/N16). No test logic here: every method delegates to {@link PlaceN24Tests}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class PlaceN24GameTest {

    @GameTest
    public void smithingTemplatesPileUpToFour(GameTestHelper helper) {
        PlaceN24Tests.smithingTemplatesPileUpToFour(helper);
    }

    @GameTest
    public void ingotsLieAsBarsAndStack(GameTestHelper helper) {
        PlaceN24Tests.ingotsLieAsBarsAndStack(helper);
    }

    @GameTest
    public void goatHornHoldsATorchOrARod(GameTestHelper helper) {
        PlaceN24Tests.goatHornHoldsATorchOrARod(helper);
    }

    @GameTest(maxTicks = 60)
    public void spearInADispenserThrustsLikeASpikeTrap(GameTestHelper helper) {
        PlaceN24Tests.spearInADispenserThrustsLikeASpikeTrap(helper);
    }

    @GameTest
    public void stackedStandingRodsJoinWithoutAGap(GameTestHelper helper) {
        PlaceN24Tests.stackedStandingRodsJoinWithoutAGap(helper);
    }

    @GameTest
    public void tiedHammockComesLooseWhenTooFar(GameTestHelper helper) {
        PlaceN24Tests.tiedHammockComesLooseWhenTooFar(helper);
    }
}
