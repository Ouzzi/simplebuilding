package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the dyed backpacks and bundles.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link DyedStorageTests}; the catalogue in {@link SimpleBuildingGameTests} names the same tests for
 * NeoForge.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class DyedStorageGameTest {

    @GameTest
    public void dyeingColoursEveryBackpackAndBundleAndKeepsItsComponents(GameTestHelper helper) {
        DyedStorageTests.dyeingColoursEveryBackpackAndBundleAndKeepsItsComponents(helper);
    }

    @GameTest
    public void waterCauldronWashesOnlyTheDyeOffBackpacksAndBundles(GameTestHelper helper) {
        DyedStorageTests.waterCauldronWashesOnlyTheDyeOffBackpacksAndBundles(helper);
    }

    @GameTest
    public void theDyeColourReachesTheBackpackMenuAndTheBundleTooltip(GameTestHelper helper) {
        DyedStorageTests.theDyeColourReachesTheBackpackMenuAndTheBundleTooltip(helper);
    }
}
