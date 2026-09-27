package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the pressure plate tests ({@link PressurePlateTests}): waxed copper plates,
 * the symmetric release delay, the pressed model and the names. No logic here; class and method
 * names are load bearing, Fabric derives the test id from them. Registered via the
 * {@code fabric-gametest} entrypoint in fabric.mod.json.
 */
public final class PressurePlateGameTest {

    @GameTest
    public void honeycombWaxesEveryStageAndWaxedPlatesStopOxidizing(GameTestHelper helper) {
        PressurePlateTests.honeycombWaxesEveryStageAndWaxedPlatesStopOxidizing(helper);
    }

    @GameTest
    public void theAxeScrapesTheWaxOffBeforeTheOxidation(GameTestHelper helper) {
        PressurePlateTests.theAxeScrapesTheWaxOffBeforeTheOxidation(helper);
    }

    @GameTest
    public void waxedCopperPlatesAreCraftedFromThePlateAndOneHoneycomb(GameTestHelper helper) {
        PressurePlateTests.waxedCopperPlatesAreCraftedFromThePlateAndOneHoneycomb(helper);
    }

    @GameTest(maxTicks = PressurePlateTests.RELEASE_MAX_TICKS)
    public void copperPlatesReleaseAsLateAsTheyPress(GameTestHelper helper) {
        PressurePlateTests.copperPlatesReleaseAsLateAsTheyPress(helper);
    }

    @GameTest
    public void everyModPressurePlateVisiblySinksWhenPressed(GameTestHelper helper) {
        PressurePlateTests.everyModPressurePlateVisiblySinksWhenPressed(helper);
    }

    @GameTest
    public void everyModPressurePlateIsNamedLikeTheVanillaOnes(GameTestHelper helper) {
        PressurePlateTests.everyModPressurePlateIsNamedLikeTheVanillaOnes(helper);
    }
}
