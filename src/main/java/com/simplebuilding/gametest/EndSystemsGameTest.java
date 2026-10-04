package com.simplebuilding.gametest;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class EndSystemsGameTest {
    @GameTest public void redstoneRecipesYieldTwo(GameTestHelper helper) { EndSystemsTests.redstoneRecipesYieldTwo(helper); }
    @GameTest public void redstoneAliasesResolveItemsAndBlocks(GameTestHelper helper) { EndSystemsTests.redstoneAliasesResolveItemsAndBlocks(helper); }
    @GameTest public void vaultSharesOnlyItsFirstHalfAndPersists(GameTestHelper helper) { EndSystemsTests.vaultSharesOnlyItsFirstHalfAndPersists(helper); }
    @GameTest public void vaultOpensAndConfigPreservesContents(GameTestHelper helper) { EndSystemsTests.vaultOpensAndConfigPreservesContents(helper); }
    @GameTest(maxTicks = 220) public void channelsStayIsolatedAndStopAtFifteen(GameTestHelper helper) { EndSystemsTests.channelsStayIsolatedAndStopAtFifteen(helper); }
    @GameTest public void matchingLampsAndConfigLimits(GameTestHelper helper) { EndSystemsTests.matchingLampsAndConfigLimits(helper); }
    @GameTest(maxTicks = 220) public void powderConnectsLikeRedstoneWire(GameTestHelper helper) { EndSystemsTests.powderConnectsLikeRedstoneWire(helper); }
    @GameTest(maxTicks = 100) public void pistonPushesAllSixAtOnce(GameTestHelper helper) { EndSystemsTests.pistonPushesAllSixAtOnce(helper); }
    @GameTest(maxTicks = 100) public void pistonNeverChains(GameTestHelper helper) { EndSystemsTests.pistonNeverChains(helper); }
    @GameTest(maxTicks = 100) public void nihilPullsAcrossTheGap(GameTestHelper helper) { EndSystemsTests.nihilPullsAcrossTheGap(helper); }
    @GameTest(maxTicks = 100) public void pistonLeavesImmovablesAlone(GameTestHelper helper) { EndSystemsTests.pistonLeavesImmovablesAlone(helper); }
    @GameTest(maxTicks = 100) public void twoPistonsMoveOneBlockOnce(GameTestHelper helper) { EndSystemsTests.twoPistonsMoveOneBlockOnce(helper); }
    @GameTest(maxTicks = 100) public void pistonFiresOnRisingEdgeOnly(GameTestHelper helper) { EndSystemsTests.pistonFiresOnRisingEdgeOnly(helper); }
    @GameTest(maxTicks = 100) public void pistonIgnoresVanillaAndOtherChannel(GameTestHelper helper) { EndSystemsTests.pistonIgnoresVanillaAndOtherChannel(helper); }
    @GameTest public void nihilVaultSharesBetweenVaultsAndPlayers(GameTestHelper helper) { EndSystemsTests.nihilVaultSharesBetweenVaultsAndPlayers(helper); }
    @GameTest public void nihilVaultNoDupeWithConcurrentMenus(GameTestHelper helper) { EndSystemsTests.nihilVaultNoDupeWithConcurrentMenus(helper); }
    @GameTest public void nihilVaultPersistsAndBreakKeepsContents(GameTestHelper helper) { EndSystemsTests.nihilVaultPersistsAndBreakKeepsContents(helper); }
    @GameTest public void nihilVaultConfigAndBlockedLid(GameTestHelper helper) { EndSystemsTests.nihilVaultConfigAndBlockedLid(helper); }
}
