package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter for the building cores (stack size, recipes, right-click animation). See {@link BuildingCoreTests}. */
public final class BuildingCoreGameTest {

    @GameTest
    public void buildingCoresAreNotStackable(GameTestHelper helper) {
        BuildingCoreTests.buildingCoresAreNotStackable(helper);
    }

    @GameTest
    public void everyCoreRecipeCraftsWithOneCorePerSlot(GameTestHelper helper) {
        BuildingCoreTests.everyCoreRecipeCraftsWithOneCorePerSlot(helper);
    }

    @GameTest
    public void coreAnimationRollFollowsTheSeventyTwentyTenWeights(GameTestHelper helper) {
        BuildingCoreTests.coreAnimationRollFollowsTheSeventyTwentyTenWeights(helper);
    }

    @GameTest
    public void rightClickingTheCorePlaysAnAnimationAndStartsTheCooldown(GameTestHelper helper) {
        BuildingCoreTests.rightClickingTheCorePlaysAnAnimationAndStartsTheCooldown(helper);
    }

    @GameTest
    public void rightClickingStoneWithTheCoreStartsTheOneSecondCooldown(GameTestHelper helper) {
        BuildingCoreTests.rightClickingStoneWithTheCoreStartsTheOneSecondCooldown(helper);
    }

    @GameTest(maxTicks = BuildingCoreTests.ANIMATION_TEST_MAX_TICKS)
    public void coreAnimationsAreServerTimedAndEndWithinTheCooldown(GameTestHelper helper) {
        BuildingCoreTests.coreAnimationsAreServerTimedAndEndWithinTheCooldown(helper);
    }

    @GameTest
    public void coreOreHostsAreTheBlocksOresGenerateIn(GameTestHelper helper) {
        BuildingCoreTests.coreOreHostsAreTheBlocksOresGenerateIn(helper);
    }

    @GameTest
    public void coreOreTablesOnlyHoldTheHostsOresAndFollowTheirWeights(GameTestHelper helper) {
        BuildingCoreTests.coreOreTablesOnlyHoldTheHostsOresAndFollowTheirWeights(helper);
    }

    @GameTest
    public void coreOreChanceClimbsFromCopperToEnderite(GameTestHelper helper) {
        BuildingCoreTests.coreOreChanceClimbsFromCopperToEnderite(helper);
    }

    @GameTest
    public void coreTransmutationTurnsOnlyHostBlocksIntoTheirOres(GameTestHelper helper) {
        BuildingCoreTests.coreTransmutationTurnsOnlyHostBlocksIntoTheirOres(helper);
    }
}
