package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the raw enderite blast, long cooks and the upper furnace tiers' rewards.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link SmeltingTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class SmeltingGameTest {

    @GameTest
    public void rawEnderiteBlastsForAnHourAndPaysTenExperience(GameTestHelper helper) {
        SmeltingTests.rawEnderiteBlastsForAnHourAndPaysTenExperience(helper);
    }

    @GameTest(maxTicks = SmeltingTests.ROUND_TRIP_MAX_TICKS)
    public void longCookTimersSurviveTheSaveAndLoadAsInts(GameTestHelper helper) {
        SmeltingTests.longCookTimersSurviveTheSaveAndLoadAsInts(helper);
    }

    @GameTest
    public void furnaceMenuScalesLongCooksIntoTheShortRange(GameTestHelper helper) {
        SmeltingTests.furnaceMenuScalesLongCooksIntoTheShortRange(helper);
    }

    @GameTest(maxTicks = SmeltingTests.EXPERIENCE_MAX_TICKS)
    public void upperTierFurnacesPayDoubleExperience(GameTestHelper helper) {
        SmeltingTests.upperTierFurnacesPayDoubleExperience(helper);
    }

    @GameTest(maxTicks = SmeltingTests.BONUS_MAX_TICKS)
    public void blastFurnaceBonusPaysRawMetalsEveryFourthOrSecondSmelt(GameTestHelper helper) {
        SmeltingTests.blastFurnaceBonusPaysRawMetalsEveryFourthOrSecondSmelt(helper);
    }
}
