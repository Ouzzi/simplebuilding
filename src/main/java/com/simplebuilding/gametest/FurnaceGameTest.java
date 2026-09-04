package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the reinforced and netherite furnaces.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link FurnaceTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class FurnaceGameTest {

    @GameTest(maxTicks = FurnaceTests.BOOST_GUARD_MAX_TICKS)
    public void boostOnlyRunsWhileTheFurnaceBurnsAndCooks(GameTestHelper helper) {
        FurnaceTests.boostOnlyRunsWhileTheFurnaceBurnsAndCooks(helper);
    }

    @GameTest(maxTicks = FurnaceTests.COOL_DOWN_MAX_TICKS)
    public void progressCoolsDownAtTheVanillaRateOnceTheFuelIsSpent(GameTestHelper helper) {
        FurnaceTests.progressCoolsDownAtTheVanillaRateOnceTheFuelIsSpent(helper);
    }

    @GameTest(maxTicks = FurnaceTests.COOK_CAP_MAX_TICKS)
    public void boostNeverPushesCookingProgressToTheFullCookTime(GameTestHelper helper) {
        FurnaceTests.boostNeverPushesCookingProgressToTheFullCookTime(helper);
    }

    @GameTest
    public void everyTierOpensTheMenuOfItsVanillaCounterpart(GameTestHelper helper) {
        FurnaceTests.everyTierOpensTheMenuOfItsVanillaCounterpart(helper);
    }

    @GameTest
    public void furnaceBlocksCarryTheirRegisteredHardnessResistanceAndTags(GameTestHelper helper) {
        FurnaceTests.furnaceBlocksCarryTheirRegisteredHardnessResistanceAndTags(helper);
    }

    @GameTest
    public void onlyNetheriteFurnaceItemsSurviveLava(GameTestHelper helper) {
        FurnaceTests.onlyNetheriteFurnaceItemsSurviveLava(helper);
    }

    @GameTest(maxTicks = FurnaceTests.DROP_MAX_TICKS)
    public void allSixFurnacesDropThemselvesWhenBroken(GameTestHelper helper) {
        FurnaceTests.allSixFurnacesDropThemselvesWhenBroken(helper);
    }

    @GameTest
    public void furnaceRecipesKeepTheirBookCategoryAndRejectNearMissGrids(GameTestHelper helper) {
        FurnaceTests.furnaceRecipesKeepTheirBookCategoryAndRejectNearMissGrids(helper);
    }

    @GameTest(maxTicks = FurnaceTests.FUEL_PARITY_MAX_TICKS)
    public void oneCoalFeedsSeveralNetheriteSmeltsWhereVanillaManagesOne(GameTestHelper helper) {
        FurnaceTests.oneCoalFeedsSeveralNetheriteSmeltsWhereVanillaManagesOne(helper);
    }
}
