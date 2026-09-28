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
}
