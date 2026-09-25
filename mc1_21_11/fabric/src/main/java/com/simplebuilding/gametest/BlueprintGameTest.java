package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the blueprint tests (MC 1.21.11 line).
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link BlueprintTests}; the annotation only restates the runner parameters.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class BlueprintGameTest {

    @GameTest
    public void codeRoundTripsAndParsesTheSpecExamples(GameTestHelper helper) {
        BlueprintTests.codeRoundTripsAndParsesTheSpecExamples(helper);
    }

    @GameTest
    public void sizeLimitsCapTheCodeAndMapWandTiers(GameTestHelper helper) {
        BlueprintTests.sizeLimitsCapTheCodeAndMapWandTiers(helper);
    }

    @GameTest
    public void materialListCountsItemsSortedByAmount(GameTestHelper helper) {
        BlueprintTests.materialListCountsItemsSortedByAmount(helper);
    }

    @GameTest
    public void cartographyTableScansTheOctantSelection(GameTestHelper helper) {
        BlueprintTests.cartographyTableScansTheOctantSelection(helper);
    }

    @GameTest
    public void buildPlacesOnlyAvailableBlocksAndSkipsExistingOnes(GameTestHelper helper) {
        BlueprintTests.buildPlacesOnlyAvailableBlocksAndSkipsExistingOnes(helper);
    }

    @GameTest
    public void rotationTurnsTheBuildAndTheScrollPacketStepsIt(GameTestHelper helper) {
        BlueprintTests.rotationTurnsTheBuildAndTheScrollPacketStepsIt(helper);
    }

    @GameTest
    public void wandTierRefusesBlueprintsLargerThanItsCube(GameTestHelper helper) {
        BlueprintTests.wandTierRefusesBlueprintsLargerThanItsCube(helper);
    }

    @GameTest
    public void editPacketSavesSignsAndLocksTheBlueprint(GameTestHelper helper) {
        BlueprintTests.editPacketSavesSignsAndLocksTheBlueprint(helper);
    }

    @GameTest
    public void recipeCraftsOneBlankBlueprint(GameTestHelper helper) {
        BlueprintTests.recipeCraftsOneBlankBlueprint(helper);
    }

    @GameTest
    public void scanFollowsTheOctantShape(GameTestHelper helper) {
        BlueprintTests.scanFollowsTheOctantShape(helper);
    }

    @GameTest
    public void largeScanRunsOverSeveralTicks(GameTestHelper helper) {
        BlueprintTests.largeScanRunsOverSeveralTicks(helper);
    }

    @GameTest
    public void largeBuildRunsOverSeveralTicks(GameTestHelper helper) {
        BlueprintTests.largeBuildRunsOverSeveralTicks(helper);
    }
}
