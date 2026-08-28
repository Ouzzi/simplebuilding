package com.simplebuilding.gametest;

import com.simplebuilding.items.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/**
 * Smallest possible in-game test: proves the harness runs and that the mod's
 * registrations survived into a live server. Everything else builds on this.
 */
public final class SmokeGameTest {

    @GameTest
    public void modItemsAreRegistered(GameTestHelper helper) {
        Identifier id = BuiltInRegistries.ITEM.getKey(ModItems.STONE_CHISEL);
        if (id == null || !"simplebuilding".equals(id.getNamespace())) {
            throw new GameTestAssertException(
                    net.minecraft.network.chat.Component.literal("stone_chisel is not registered: " + id), 0);
        }
        helper.succeed();
    }
}
