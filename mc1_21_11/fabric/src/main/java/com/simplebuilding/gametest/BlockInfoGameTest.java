package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for what the optional Jade plugin shows (compat.BlockInfo). No test logic: every
 * method delegates to {@link BlockInfoTests}; class and method names are load bearing (Fabric
 * derives the test id).
 */
public final class BlockInfoGameTest {

    @GameTest
    public void launchpadShowsItsChargesAgainstItsCapacity(GameTestHelper helper) {
        BlockInfoTests.launchpadShowsItsChargesAgainstItsCapacity(helper);
    }

    @GameTest
    public void padOwnerIsNamedByTheServer(GameTestHelper helper) {
        BlockInfoTests.padOwnerIsNamedByTheServer(helper);
    }

    @GameTest
    public void potionPadShowsItsPotionAndCooldown(GameTestHelper helper) {
        BlockInfoTests.potionPadShowsItsPotionAndCooldown(helper);
    }

    @GameTest
    public void chunkLoaderShowsHowManyChunksItHolds(GameTestHelper helper) {
        BlockInfoTests.chunkLoaderShowsHowManyChunksItHolds(helper);
    }

    @GameTest
    public void pistonDurabilityFollowsTheBlockState(GameTestHelper helper) {
        BlockInfoTests.pistonDurabilityFollowsTheBlockState(helper);
    }

    @GameTest
    public void chestSlotsFollowTheTierAndDoubleChests(GameTestHelper helper) {
        BlockInfoTests.chestSlotsFollowTheTierAndDoubleChests(helper);
    }

    @GameTest
    public void hopperFilterNamesTheModeAndItems(GameTestHelper helper) {
        BlockInfoTests.hopperFilterNamesTheModeAndItems(helper);
    }

    @GameTest
    public void furnaceSpeedFollowsTheTier(GameTestHelper helper) {
        BlockInfoTests.furnaceSpeedFollowsTheTier(helper);
    }

    @GameTest
    public void linesSurviveTheServerDataTag(GameTestHelper helper) {
        BlockInfoTests.linesSurviveTheServerDataTag(helper);
    }
}
