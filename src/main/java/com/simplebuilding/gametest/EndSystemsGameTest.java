package com.simplebuilding.gametest;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class EndSystemsGameTest {
    @GameTest public void redstoneAliasesResolveItemsAndBlocks(GameTestHelper helper) { EndSystemsTests.redstoneAliasesResolveItemsAndBlocks(helper); }
    @GameTest public void vaultSharesOnlyItsFirstHalfAndPersists(GameTestHelper helper) { EndSystemsTests.vaultSharesOnlyItsFirstHalfAndPersists(helper); }
    @GameTest public void vaultOpensAndConfigPreservesContents(GameTestHelper helper) { EndSystemsTests.vaultOpensAndConfigPreservesContents(helper); }
    @GameTest(maxTicks = 100) public void vaultLidOpensLikeEnderChest(GameTestHelper helper) { EndSystemsTests.vaultLidOpensLikeEnderChest(helper); }
    @GameTest public void nihilVaultSharesBetweenVaultsAndPlayers(GameTestHelper helper) { EndSystemsTests.nihilVaultSharesBetweenVaultsAndPlayers(helper); }
    @GameTest public void nihilVaultNoDupeWithConcurrentMenus(GameTestHelper helper) { EndSystemsTests.nihilVaultNoDupeWithConcurrentMenus(helper); }
    @GameTest public void nihilVaultOnlyInOwnClaims(GameTestHelper helper) { EndSystemsTests.nihilVaultOnlyInOwnClaims(helper); }
    @GameTest public void nihilVaultPersistsAndBreakKeepsContents(GameTestHelper helper) { EndSystemsTests.nihilVaultPersistsAndBreakKeepsContents(helper); }
    @GameTest public void nihilVaultConfigAndBlockedLid(GameTestHelper helper) { EndSystemsTests.nihilVaultConfigAndBlockedLid(helper); }
}
