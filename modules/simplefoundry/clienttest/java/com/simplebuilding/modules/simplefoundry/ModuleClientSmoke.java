package com.simplebuilding.modules.simplefoundry;

import com.simplefoundry.block.DeskBlockEntity;
import com.simplefoundry.block.FoundryMenu;
import com.simplefoundry.client.FoundryScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.SimpleContainerData;

/**
 * Client smoke (b-foundry03): the module boots; a glass-fronted tank with melts in it (Variant A: the layers on top of each other),
 * the five fuel gauge steps on the desk front, and the desk window (FOUNDRY32-UI = A) in a known state with screenshots.
 */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplefoundry")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simplefoundry-title");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            var server = world.getServer();
            server.runCommand("gamemode creative @a");
            server.runCommand("time set noon");
            server.runCommand("gamerule advance_time false");
            server.runCommand("weather clear");
            server.runCommand("execute at @a run fill ~-8 ~-1 ~-2 ~8 ~-1 ~10 minecraft:smooth_stone");
            server.runCommand("execute at @a run fill ~-8 ~ ~-2 ~8 ~6 ~10 minecraft:air");
            // tank: three chambers high at ~0 ~ ~6, hull of bricks, glass front (south, towards the player), desk on the west side, grate on the east
            server.runCommand("execute at @a run fill ~-1 ~ ~5 ~1 ~4 ~7 simplefoundry:foundry_brick");
            server.runCommand("execute at @a run fill ~0 ~1 ~6 ~0 ~3 ~6 simplefoundry:melting_chamber");
            server.runCommand("execute at @a run fill ~-1 ~1 ~5 ~1 ~3 ~5 minecraft:glass");
            server.runCommand("execute at @a run setblock ~-1 ~1 ~6 simplefoundry:foundry_desk[facing=south]");
            server.runCommand("execute at @a run setblock ~1 ~1 ~6 simplefoundry:fire_grate");
            server.runCommand("execute at @a run data merge block ~-1 ~1 ~6 {Melt_gold:81,Melt_copper:162,Melt_iron:100,FuelMedium:18000,Tier:2}");
            // five desks with the gauge steps
            for (int level = 0; level <= 4; level++) {
                server.runCommand("execute at @a run setblock ~" + (-6 + 3 * level) + " ~ ~1 simplefoundry:foundry_desk[facing=south,fuel=" + level + "]");
            }
            context.waitTicks(40);
            server.runCommand("execute as @a at @s run tp @s ~ ~ ~-1 0 5");
            context.waitTicks(40);
            context.takeScreenshot("simplefoundry-tank-front");
            server.runCommand("execute as @a at @s run tp @s ~-4 ~1 ~-1 0 15");
            context.waitTicks(40);
            context.takeScreenshot("simplefoundry-desk-gauges");
            window(context, "simplefoundry-window-stone", 0);
            window(context, "simplefoundry-window-netherite", 3);
        }
    }

    /** The desk window with melts in the tank, fuel at about 40 percent and the given tier. */
    private static void window(ClientGameTestContext context, String name, int tier) {
        context.runOnClient(c -> {
            SimpleContainerData data = new SimpleContainerData(DeskBlockEntity.DATA_COUNT);
            data.set(0, tier);
            data.set(1, 1);
            data.set(2, 3);
            data.set(3, 0);
            data.set(4, 1);
            data.set(5, 1000);
            data.set(6, 729);
            data.set(8, 81);
            data.set(10, 162);
            data.set(12, 100);
            data.set(16, 27);
            var menu = FoundryMenu.preview(0, c.player.getInventory(), data);
            c.setScreenAndShow(new FoundryScreen(menu, c.player.getInventory(), Component.translatable("block.simplefoundry.foundry_desk")));
        });
        context.waitTicks(5);
        context.takeScreenshot(name);
        context.runOnClient(c -> c.setScreenAndShow(null));
    }
}
