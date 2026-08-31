package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the trade table test (MC 1.21.11 line). No logic here; see
 * {@link TradeRegistryTests}.
 */
public final class TradeRegistryGameTest {

    @GameTest
    public void allModTradesResolveAgainstTheServerRegistries(GameTestHelper helper) {
        TradeRegistryTests.allModTradesResolveAgainstTheServerRegistries(helper);
    }
}
