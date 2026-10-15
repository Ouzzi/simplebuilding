package com.simplebuilding.modules.simplewanderers;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * Client smoke: the module boots, and a Village Wanderer with his leashed horse renders (front, back and the brim from
 * above) with screenshots of the new texture on the Vanilla villager model.
 */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplewanderers")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simplewanderers-title");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            var server = world.getServer();
            server.runCommand("gamemode creative @a");
            server.runCommand("time set noon");
            server.runCommand("gamerule advance_time false");
            server.runCommand("weather clear");
            server.runCommand("execute at @a run fill ~-6 ~-1 ~-6 ~6 ~-1 ~8 minecraft:grass_block");
            server.runCommand("execute at @a run fill ~-6 ~ ~-6 ~6 ~6 ~8 minecraft:air");
            server.runCommand("execute at @a run summon simplewanderers:village_wanderer ~ ~ ~3 {NoAI:1b,Rotation:[180f,0f]}");
            server.runCommand("execute at @a run summon simplewanderers:wanderer_horse ~3 ~ ~4 {NoAI:1b,Rotation:[150f,0f],Variant:258}");
            context.waitTicks(10);
            server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 10");
            context.waitTicks(40);
            context.takeScreenshot("simplewanderers-front");
            server.runCommand("execute as @a at @s run tp @s ~ ~ ~6 180 10");
            context.waitTicks(40);
            context.takeScreenshot("simplewanderers-back");
            server.runCommand("execute as @a at @s run tp @s ~ ~ ~4 150 35");
            context.waitTicks(40);
            context.takeScreenshot("simplewanderers-above");
        }
    }
}
