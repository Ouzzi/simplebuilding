package com.simplebuilding.gametest;

import com.simplebuilding.discover.SimpleBuildingDiscover;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the discoverability tests (docs/ai/PRINZIPIEN-ENTDECKBARKEIT.md). The bodies live in the
 * generated {@link SimpleBuildingDiscover}; class and method names are load bearing (Fabric derives the test id).
 */
public final class DiscoverGameTest {

    @GameTest
    public void everyBookRecipeHasAnUnlock(GameTestHelper helper) {
        SimpleBuildingDiscover.recipeUnlocks(helper);
    }

    @GameTest
    public void behaviourItemsHaveTooltipOrWikiEntry(GameTestHelper helper) {
        SimpleBuildingDiscover.behaviourHints(helper);
    }

    @GameTest
    public void firstPickupTipsShowOnce(GameTestHelper helper) {
        SimpleBuildingDiscover.firstPickupTips(helper);
    }
}
