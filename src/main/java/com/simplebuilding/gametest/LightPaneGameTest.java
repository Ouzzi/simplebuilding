package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the Light Panes (N57). No test logic here: every method delegates to {@link LightPaneTests}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class LightPaneGameTest {

    @GameTest
    public void panesPlaceOnEverySide(GameTestHelper helper) {
        LightPaneTests.panesPlaceOnEverySide(helper);
    }

    @GameTest
    public void panesNeedFullSupportFace(GameTestHelper helper) {
        LightPaneTests.panesNeedFullSupportFace(helper);
    }

    @GameTest
    public void panesFallOffWithoutSupport(GameTestHelper helper) {
        LightPaneTests.panesFallOffWithoutSupport(helper);
    }

    @GameTest
    public void panesWaterlog(GameTestHelper helper) {
        LightPaneTests.panesWaterlog(helper);
    }

    @GameTest
    public void plainPanesShineAndSwitchWithRedstone(GameTestHelper helper) {
        LightPaneTests.plainPanesShineAndSwitchWithRedstone(helper);
    }

    @GameTest(maxTicks = 60)
    public void paneLightReachesTheWorldAndGoesOut(GameTestHelper helper) {
        LightPaneTests.paneLightReachesTheWorldAndGoesOut(helper);
    }

    @GameTest
    public void redstonePaneColorFollowsSignalStrength(GameTestHelper helper) {
        LightPaneTests.redstonePaneColorFollowsSignalStrength(helper);
    }

    @GameTest
    public void rightClickCyclesTheStartColor(GameTestHelper helper) {
        LightPaneTests.rightClickCyclesTheStartColor(helper);
    }

    @GameTest
    public void panesHaveTheirRecipes(GameTestHelper helper) {
        LightPaneTests.panesHaveTheirRecipes(helper);
    }

    @GameTest
    public void copperPaneFlipsOnEveryRisingEdge(GameTestHelper helper) {
        LightPaneTests.copperPaneFlipsOnEveryRisingEdge(helper);
    }
}
