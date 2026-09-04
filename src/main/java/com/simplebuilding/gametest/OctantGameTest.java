package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the octant.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link OctantTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class OctantGameTest {

    @GameTest
    public void allOctantColoursShareDurabilityAndTheirPaint(GameTestHelper helper) {
        OctantTests.allOctantColoursShareDurabilityAndTheirPaint(helper);
    }

    @GameTest
    public void airClicksOnlyResetAnUnlockedOctantWhileSneaking(GameTestHelper helper) {
        OctantTests.airClicksOnlyResetAnUnlockedOctantWhileSneaking(helper);
    }

    @GameTest
    public void shapeCatalogueIsFixedAndTheChosenShapeShowsInTheName(GameTestHelper helper) {
        OctantTests.shapeCatalogueIsFixedAndTheChosenShapeShowsInTheName(helper);
    }

    @GameTest
    public void octantTooltipListsTheLockAndBothCorners(GameTestHelper helper) {
        OctantTests.octantTooltipListsTheLockAndBothCorners(helper);
    }

    @GameTest
    public void octantScrollPacketsOnlyEverTouchTheMainHand(GameTestHelper helper) {
        OctantTests.octantScrollPacketsOnlyEverTouchTheMainHand(helper);
    }

    @GameTest
    public void dyeingRecipesProduceEveryColouredOctant(GameTestHelper helper) {
        OctantTests.dyeingRecipesProduceEveryColouredOctant(helper);
    }

    @GameTest
    public void waterCauldronWashesTheColourOffAnOctant(GameTestHelper helper) {
        OctantTests.waterCauldronWashesTheColourOffAnOctant(helper);
    }

    @GameTest
    public void chestOctantsAreEnchantedInTheTwoDangerousChestsOnly(GameTestHelper helper) {
        OctantTests.chestOctantsAreEnchantedInTheTwoDangerousChestsOnly(helper);
    }

    @GameTest
    public void theOctantOnlyMeasuresAndPlacesNothing(GameTestHelper helper) {
        OctantTests.theOctantOnlyMeasuresAndPlacesNothing(helper);
    }
}
