package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

/**
 * The mod ships 20 data-driven villager trades. This asserts they actually reach the
 * villager_trade registry of a running server -- which is a different question from
 * whether the vanilla trade tags pick them up (see TradeAndMigrationGameTest).
 */
public final class TradeRegistryGameTest {

    private static final int EXPECTED_TRADES = 20;

    @GameTest
    public void allModTradesReachTheRegistry(GameTestHelper helper) {
        Registry<?> registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
        long ours = registry.keySet().stream()
                .filter(key -> "simplebuilding".equals(key.getNamespace()))
                .count();

        if (ours != EXPECTED_TRADES) {
            throw new GameTestAssertException(Component.literal(
                    "expected " + EXPECTED_TRADES + " simplebuilding trades in the villager_trade registry, found " + ours), 0);
        }
        helper.succeed();
    }
}
