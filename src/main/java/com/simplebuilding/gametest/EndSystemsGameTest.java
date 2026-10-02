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
}
