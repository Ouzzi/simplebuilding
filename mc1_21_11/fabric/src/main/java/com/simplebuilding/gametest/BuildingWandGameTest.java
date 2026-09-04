package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the building wand.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link BuildingWandTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class BuildingWandGameTest {

    @GameTest
    public void offHandClickIsPassedOnAndTheWandStopsOutsideBothHands(GameTestHelper helper) {
        BuildingWandTests.offHandClickIsPassedOnAndTheWandStopsOutsideBothHands(helper);
    }

    @GameTest
    public void clickedFaceSetsThePlaneUntilAnAxisModeOverridesIt(GameTestHelper helper) {
        BuildingWandTests.clickedFaceSetsThePlaneUntilAnAxisModeOverridesIt(helper);
    }

    @GameTest
    public void wandTierCapsTheRadiusSettingAndSizesThePlane(GameTestHelper helper) {
        BuildingWandTests.wandTierCapsTheRadiusSettingAndSizesThePlane(helper);
    }

    @GameTest
    public void materialSearchPrefersTheOffHandAndOnlyMasterBuilderReachesTheBackpack(GameTestHelper helper) {
        BuildingWandTests.materialSearchPrefersTheOffHandAndOnlyMasterBuilderReachesTheBackpack(helper);
    }

    @GameTest
    public void wandArmedWithoutMaterialSearchesAgainAndPaletteFallsBackToStone(GameTestHelper helper) {
        BuildingWandTests.wandArmedWithoutMaterialSearchesAgainAndPaletteFallsBackToStone(helper);
    }

    @GameTest
    public void wandEnchantmentsOnlyStickToTheWandsInTheirItemTag(GameTestHelper helper) {
        BuildingWandTests.wandEnchantmentsOnlyStickToTheWandsInTheirItemTag(helper);
    }

    @GameTest
    public void wandRecipesCraftTheLowerTiersAndForgeTheUpperOnes(GameTestHelper helper) {
        BuildingWandTests.wandRecipesCraftTheLowerTiersAndForgeTheUpperOnes(helper);
    }

    @GameTest
    public void ironWandDropsInTheMansionAndTheDiamondWandInTheEndCity(GameTestHelper helper) {
        BuildingWandTests.ironWandDropsInTheMansionAndTheDiamondWandInTheEndCity(helper);
    }
}
