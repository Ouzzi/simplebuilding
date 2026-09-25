package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;

/**
 * Fabric adapter for the building wand's modes (Linear line, Bridge, Cover, player-like
 * orientation, undo, octant fill and roof). No logic here; see {@link WandModeTests}. Pinned to an
 * unrotated structure because every mode depends on the direction the player faces.
 */
public final class WandModeGameTest {

    @GameTest(rotation = Rotation.NONE)
    public void linearWhileSneakingBuildsTheLineAwayFromTheClickedFace(GameTestHelper helper) {
        WandModeTests.linearWhileSneakingBuildsTheLineAwayFromTheClickedFace(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void bridgeRunsFromTheBlockUnderfootInTheFacingDirection(GameTestHelper helper) {
        WandModeTests.bridgeRunsFromTheBlockUnderfootInTheFacingDirection(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void coverOnlyGrowsTheSurfaceOfTheClickedKind(GameTestHelper helper) {
        WandModeTests.coverOnlyGrowsTheSurfaceOfTheClickedKind(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void wandSetsStairsAndLogsLikeThePlayerWould(GameTestHelper helper) {
        WandModeTests.wandSetsStairsAndLogsLikeThePlayerWould(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void undoTakesBackOnlyTheLastActionAndOnlyUnchangedBlocks(GameTestHelper helper) {
        WandModeTests.undoTakesBackOnlyTheLastActionAndOnlyUnchangedBlocks(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void octantInTheOffHandFillsItsShapeWithTheWand(GameTestHelper helper) {
        WandModeTests.octantInTheOffHandFillsItsShapeWithTheWand(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void roofModeLaysStairsTowardsTheRidgeWithSlabsOnTop(GameTestHelper helper) {
        WandModeTests.roofModeLaysStairsTowardsTheRidgeWithSlabsOnTop(helper);
    }
}
