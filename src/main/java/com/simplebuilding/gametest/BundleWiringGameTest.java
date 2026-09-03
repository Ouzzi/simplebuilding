package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for how the bundles connect to the rest of the game.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link BundleWiringTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class BundleWiringGameTest {

    @GameTest
    public void funnelBundleSweepsUpDropsOnTouchUnlessThePlayerSneaks(GameTestHelper helper) {
        BundleWiringTests.funnelBundleSweepsUpDropsOnTouchUnlessThePlayerSneaks(helper);
    }

    @GameTest
    public void netheriteBundleOnTheGroundSurvivesFireAndExplosions(GameTestHelper helper) {
        BundleWiringTests.netheriteBundleOnTheGroundSurvivesFireAndExplosions(helper);
    }

    @GameTest
    public void bundlePacketsOnlyTouchTheSlotsTheyOwn(GameTestHelper helper) {
        BundleWiringTests.bundlePacketsOnlyTouchTheSlotsTheyOwn(helper);
    }

    @GameTest
    public void anvilBlanksTheResultForColourPaletteWithoutMasterBuilder(GameTestHelper helper) {
        BundleWiringTests.anvilBlanksTheResultForColourPaletteWithoutMasterBuilder(helper);
    }

    @GameTest
    public void buildingWandBuildsFromTheBundleAndPaysOnePiecePerBlock(GameTestHelper helper) {
        BundleWiringTests.buildingWandBuildsFromTheBundleAndPaysOnePiecePerBlock(helper);
    }

    @GameTest
    public void bundleRecipesCraftTheBaseAndUpgradeItTierByTier(GameTestHelper helper) {
        BundleWiringTests.bundleRecipesCraftTheBaseAndUpgradeItTierByTier(helper);
    }

    @GameTest
    public void wanderingTraderSellsAndBuysTheReinforcedBundle(GameTestHelper helper) {
        BundleWiringTests.wanderingTraderSellsAndBuysTheReinforcedBundle(helper);
    }

    @GameTest
    public void reinforcedBundleSitsInDungeonShipwreckAndMineshaftLoot(GameTestHelper helper) {
        BundleWiringTests.reinforcedBundleSitsInDungeonShipwreckAndMineshaftLoot(helper);
    }

    @GameTest
    public void containerEnchantmentsAcceptTheBundlesTheyAreMeantFor(GameTestHelper helper) {
        BundleWiringTests.containerEnchantmentsAcceptTheBundlesTheyAreMeantFor(helper);
    }
}
