package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;

/**
 * Fabric adapter for the tool behaviour tests (MC 1.21.11 line). No logic here; see
 * {@link ToolBehaviourTests}. Every test is pinned to an unrotated structure because several of
 * them depend on absolute directions (player facing, mining direction).
 */
public final class ToolBehaviourGameTest {

    @GameTest(rotation = Rotation.NONE)
    public void sledgehammerBreaksThreeByThreeAroundOrigin(GameTestHelper helper) {
        ToolBehaviourTests.sledgehammerBreaksThreeByThreeAroundOrigin(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void sledgehammerOverrideLevelsWidenBlockSelection(GameTestHelper helper) {
        ToolBehaviourTests.sledgehammerOverrideLevelsWidenBlockSelection(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void chiselAndSpatulaTransformBlockInBothDirections(GameTestHelper helper) {
        ToolBehaviourTests.chiselAndSpatulaTransformBlockInBothDirections(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void chiselTierGatesTransformations(GameTestHelper helper) {
        ToolBehaviourTests.chiselTierGatesTransformations(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void veinMinerCollectsConnectedOreCluster(GameTestHelper helper) {
        ToolBehaviourTests.veinMinerCollectsConnectedOreCluster(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void stripMinerFollowsPlayerFacingAndStopsAtGaps(GameTestHelper helper) {
        ToolBehaviourTests.stripMinerFollowsPlayerFacingAndStopsAtGaps(helper);
    }

    @GameTest(rotation = Rotation.NONE, maxTicks = ToolBehaviourTests.MAGNET_MAX_TICKS)
    public void magnetPullsNearbyItemsAndIgnoresDistantOnes(GameTestHelper helper) {
        ToolBehaviourTests.magnetPullsNearbyItemsAndIgnoresDistantOnes(helper);
    }
}
