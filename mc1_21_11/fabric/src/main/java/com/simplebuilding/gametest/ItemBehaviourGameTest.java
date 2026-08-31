package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;

/**
 * Fabric adapter for the item behaviour tests.
 *
 * <p>MC 1.21.11 line. No logic here; every method delegates to the loader-neutral body in
 * {@link ItemBehaviourTests}. Everything that touches world positions or block facings is pinned
 * to an unrotated structure, because the assertions name absolute directions.
 */
public final class ItemBehaviourGameTest {

    @GameTest(rotation = Rotation.NONE)
    public void rotatorTurnsLogsByClickedFaceAndRim(GameTestHelper helper) {
        ItemBehaviourTests.rotatorTurnsLogsByClickedFaceAndRim(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void rotatorCyclesFacingBlocksAndLeavesPlainBlocksAlone(GameTestHelper helper) {
        ItemBehaviourTests.rotatorCyclesFacingBlocksAndLeavesPlainBlocksAlone(helper);
    }

    @GameTest
    public void quiverTakesArrowsAndRefusesEverythingElse(GameTestHelper helper) {
        ItemBehaviourTests.quiverTakesArrowsAndRefusesEverythingElse(helper);
    }

    @GameTest
    public void bundleCapacityGrowsWithTierAndEnchantments(GameTestHelper helper) {
        ItemBehaviourTests.bundleCapacityGrowsWithTierAndEnchantments(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void oreDetectorCyclesModesAndLearnsACustomBlock(GameTestHelper helper) {
        ItemBehaviourTests.oreDetectorCyclesModesAndLearnsACustomBlock(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void octantStoresBothCornersAndRespectsTheLock(GameTestHelper helper) {
        ItemBehaviourTests.octantStoresBothCornersAndRespectsTheLock(helper);
    }

    @GameTest(maxTicks = ItemBehaviourTests.WAND_MAX_TICKS, rotation = Rotation.NONE)
    public void buildingWandFillsThePlaneItIsPointedAt(GameTestHelper helper) {
        ItemBehaviourTests.buildingWandFillsThePlaneItIsPointedAt(helper);
    }
}
