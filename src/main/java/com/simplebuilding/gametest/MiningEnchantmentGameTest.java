package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the mining enchantments.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link MiningEnchantmentTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class MiningEnchantmentGameTest {

    @GameTest
    public void veinMinerSpendsItsPerLevelBudgetAndStopsWhenTheToolBreaks(GameTestHelper helper) {
        MiningEnchantmentTests.veinMinerSpendsItsPerLevelBudgetAndStopsWhenTheToolBreaks(helper);
    }

    @GameTest
    public void veinMinerAndStripMinerIgnoreToolsAndBlocksOutsideTheirGates(GameTestHelper helper) {
        MiningEnchantmentTests.veinMinerAndStripMinerIgnoreToolsAndBlocksOutsideTheirGates(helper);
    }

    @GameTest
    public void stripMinerDigsUpwardsOnlyPastTheSteepPitchThreshold(GameTestHelper helper) {
        MiningEnchantmentTests.stripMinerDigsUpwardsOnlyPastTheSteepPitchThreshold(helper);
    }

    @GameTest
    public void stripMinerDividesThePlayerDestroySpeedPerLevel(GameTestHelper helper) {
        MiningEnchantmentTests.stripMinerDividesThePlayerDestroySpeedPerLevel(helper);
    }

    @GameTest
    public void versatilityRefusesCandidatesThatCannotHarvestTheBlock(GameTestHelper helper) {
        MiningEnchantmentTests.versatilityRefusesCandidatesThatCannotHarvestTheBlock(helper);
    }

    @GameTest
    public void versatilityPrefersTheHammerAndRanksTheChiselLast(GameTestHelper helper) {
        MiningEnchantmentTests.versatilityPrefersTheHammerAndRanksTheChiselLast(helper);
    }

    @GameTest
    public void miningEnchantmentTagsHoldExactlyWhatTheyDeclare(GameTestHelper helper) {
        MiningEnchantmentTests.miningEnchantmentTagsHoldExactlyWhatTheyDeclare(helper);
    }

    @GameTest
    public void miningEnchantmentBooksAndTheDiamondHammerSitInTheirLootPools(GameTestHelper helper) {
        MiningEnchantmentTests.miningEnchantmentBooksAndTheDiamondHammerSitInTheirLootPools(helper);
    }

    @GameTest
    public void miningPickaxeTradeAlwaysCarriesAnEnchantmentFromItsPool(GameTestHelper helper) {
        MiningEnchantmentTests.miningPickaxeTradeAlwaysCarriesAnEnchantmentFromItsPool(helper);
    }

    @GameTest
    public void creativeTabOffersEveryMiningEnchantmentBookAtMaxLevel(GameTestHelper helper) {
        MiningEnchantmentTests.creativeTabOffersEveryMiningEnchantmentBookAtMaxLevel(helper);
    }
}
