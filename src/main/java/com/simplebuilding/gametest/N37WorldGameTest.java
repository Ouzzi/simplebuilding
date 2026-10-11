package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter for Queue N37, world part; the logic lives in {@link N37WorldTests}. */
public final class N37WorldGameTest {

    @GameTest
    public void pilesBreakOnlyTheLookedAtPart(GameTestHelper helper) {
        N37WorldTests.pilesBreakOnlyTheLookedAtPart(helper);
    }

    @GameTest
    public void bucketsDrawNoBarAndTheEnderiteBucketShowsOnlyItsName(GameTestHelper helper) {
        N37WorldTests.bucketsDrawNoBarAndTheEnderiteBucketShowsOnlyItsName(helper);
    }

    @GameTest
    public void sculkJawNeedsAFullBlockTop(GameTestHelper helper) {
        N37WorldTests.sculkJawNeedsAFullBlockTop(helper);
    }
}
