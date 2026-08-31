package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;

/**
 * Fabric adapter for the tool behaviour tests.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link ToolBehaviourTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
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
