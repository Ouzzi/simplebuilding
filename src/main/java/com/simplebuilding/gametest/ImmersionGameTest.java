package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for immersion: pad states, flypad warning, info tooltips and the HUD layout.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link ImmersionTests}. Registered through the {@code fabric-gametest} entrypoint in
 * {@code fabric.mod.json}. Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class ImmersionGameTest {

    @GameTest
    public void launchpadShowsItsFillLevelInItsBlockState(GameTestHelper helper) {
        ImmersionTests.launchpadShowsItsFillLevelInItsBlockState(helper);
    }

    @GameTest
    public void chunkLoaderShowsWhetherItKeepsChunksLoaded(GameTestHelper helper) {
        ImmersionTests.chunkLoaderShowsWhetherItKeepsChunksLoaded(helper);
    }

    @GameTest
    public void flypadShowsActiveWhileSomeoneIsInItsField(GameTestHelper helper) {
        ImmersionTests.flypadShowsActiveWhileSomeoneIsInItsField(helper);
    }

    @GameTest
    public void flypadWarningRisesTowardsTheEdgeOfItsField(GameTestHelper helper) {
        ImmersionTests.flypadWarningRisesTowardsTheEdgeOfItsField(helper);
    }

    @GameTest
    public void padAndMachineTooltipsNameTheirNumbers(GameTestHelper helper) {
        ImmersionTests.padAndMachineTooltipsNameTheirNumbers(helper);
    }

    @GameTest
    public void enderiteArmorDampsTheFallWhileSneaking(GameTestHelper helper) {
        ImmersionTests.enderiteArmorDampsTheFallWhileSneaking(helper);
    }

    @GameTest
    public void armorAndFoodTooltipsExplainWhatTheyDo(GameTestHelper helper) {
        ImmersionTests.armorAndFoodTooltipsExplainWhatTheyDo(helper);
    }

    @GameTest
    public void coreTooltipsListExactlyTheRecipesThatTakeTheCore(GameTestHelper helper) {
        ImmersionTests.coreTooltipsListExactlyTheRecipesThatTakeTheCore(helper);
    }

    @GameTest
    public void hudBoxesFollowTheConfiguredPositionAndScale(GameTestHelper helper) {
        ImmersionTests.hudBoxesFollowTheConfiguredPositionAndScale(helper);
    }
}
