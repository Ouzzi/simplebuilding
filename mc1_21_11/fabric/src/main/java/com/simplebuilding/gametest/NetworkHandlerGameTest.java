package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;

/**
 * Fabric adapter for the network handler tests.
 *
 * <p>MC 1.21.11 line. No logic here; every method delegates to the loader-neutral body in
 * {@link NetworkHandlerTests}. Class and method names are load bearing: Fabric derives the test
 * id from them, and those ids have to match the shared catalogue in
 * {@link SimpleBuildingGameTests} so a NeoForge report can be compared line by line.
 */
public final class NetworkHandlerGameTest {

    @GameTest
    public void doubleJumpNeedsEnchantedBootsAndWearsThem(GameTestHelper helper) {
        NetworkHandlerTests.doubleJumpNeedsEnchantedBootsAndWearsThem(helper);
    }

    @GameTest
    public void spaceKeyAndTrimBenefitFlagsReachThePlayer(GameTestHelper helper) {
        NetworkHandlerTests.spaceKeyAndTrimBenefitFlagsReachThePlayer(helper);
    }

    @GameTest
    public void buildingWandConfigureStoresRadiusAndAxis(GameTestHelper helper) {
        NetworkHandlerTests.buildingWandConfigureStoresRadiusAndAxis(helper);
    }

    @GameTest
    public void octantConfigureStoresTheWholeSelectionState(GameTestHelper helper) {
        NetworkHandlerTests.octantConfigureStoresTheWholeSelectionState(helper);
    }

    /** Unrotated: the corner nudging depends on the absolute direction the player looks at. */
    @GameTest(rotation = Rotation.NONE)
    public void octantScrollCyclesShapesAndNudgesCornersByFacing(GameTestHelper helper) {
        NetworkHandlerTests.octantScrollCyclesShapesAndNudgesCornersByFacing(helper);
    }

    @GameTest
    public void masterBuilderPickTakesBlocksOutOfTheEnchantedBundle(GameTestHelper helper) {
        NetworkHandlerTests.masterBuilderPickTakesBlocksOutOfTheEnchantedBundle(helper);
    }
}
