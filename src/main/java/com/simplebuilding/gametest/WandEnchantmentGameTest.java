package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the building enchantments.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link WandEnchantmentTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class WandEnchantmentGameTest {

    @GameTest
    public void masterBuilderOpensTheBackpackAndTheBundlesInsideItToTheWand(GameTestHelper helper) {
        WandEnchantmentTests.masterBuilderOpensTheBackpackAndTheBundlesInsideItToTheWand(helper);
    }

    @GameTest
    public void masterBuilderMovesThePreviewSourcesTheSameWayItMovesThePlacement(GameTestHelper helper) {
        WandEnchantmentTests.masterBuilderMovesThePreviewSourcesTheSameWayItMovesThePlacement(helper);
    }

    @GameTest
    public void fastChiselingNeverPushesTheChiselCooldownBelowOneTick(GameTestHelper helper) {
        WandEnchantmentTests.fastChiselingNeverPushesTheChiselCooldownBelowOneTick(helper);
    }

    @GameTest
    public void theBuildingEnchantmentsReachEveryToolWhoseCodeReadsThem(GameTestHelper helper) {
        WandEnchantmentTests.theBuildingEnchantmentsReachEveryToolWhoseCodeReadsThem(helper);
    }

    @GameTest
    public void buildingEnchantmentBooksSitInTheStructureChestsTheyBelongTo(GameTestHelper helper) {
        WandEnchantmentTests.buildingEnchantmentBooksSitInTheStructureChestsTheyBelongTo(helper);
    }

    @GameTest
    public void librarianBookTradesHandOutOnlyTheEnchantmentsTheyDeclare(GameTestHelper helper) {
        WandEnchantmentTests.librarianBookTradesHandOutOnlyTheEnchantmentsTheyDeclare(helper);
    }
}
