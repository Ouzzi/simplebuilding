package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter for Queue N37, world part; the logic lives in {@link WorldPolishTests}. */
public final class WorldPolishGameTest {

    @GameTest
    public void pilesBreakOnlyTheLookedAtPart(GameTestHelper helper) {
        WorldPolishTests.pilesBreakOnlyTheLookedAtPart(helper);
    }

    @GameTest
    public void bucketsDrawNoBarAndTheEnderiteBucketShowsOnlyItsName(GameTestHelper helper) {
        WorldPolishTests.bucketsDrawNoBarAndTheEnderiteBucketShowsOnlyItsName(helper);
    }

    @GameTest
    public void sculkJawNeedsFullBlockTop(GameTestHelper helper) {
        WorldPolishTests.sculkJawNeedsFullBlockTop(helper);
    }

    @GameTest
    public void standersStayOnTheChestBoatDeck(GameTestHelper helper) {
        WorldPolishTests.standersStayOnTheChestBoatDeck(helper);
    }
}
