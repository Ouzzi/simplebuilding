package com.simplebuilding.modules.simpleblocks;

import com.simpleblocks.BlocksRegistry;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * Client smoke: the module boots, and a build scene shows every block: a front row of the stairs, slabs and walls, a back
 * row of the full blocks, with screenshots from the front and from above.
 */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simpleblocks")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simpleblocks-title");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            var server = world.getServer();
            server.runCommand("gamemode creative @a");
            server.runCommand("time set noon");
            server.runCommand("gamerule advance_time false");
            server.runCommand("weather clear");
            server.runCommand("execute at @a run fill ~-12 ~-1 ~-2 ~12 ~-1 ~12 minecraft:smooth_stone");
            server.runCommand("execute at @a run fill ~-12 ~ ~-2 ~12 ~6 ~12 minecraft:air");
            int full = 0;
            int shaped = 0;
            for (String name : BlocksRegistry.NAMES) {
                boolean shape = name.endsWith("_stairs") || name.endsWith("_slab") || name.endsWith("_wall");
                int x = shape ? -11 + 2 * shaped++ : -11 + 2 * full++;
                int z = shape ? 3 : 8;
                // Stairs face the player; the walls get a post of the same block on top.
                String state = name.endsWith("_stairs") ? "[facing=south]" : "";
                server.runCommand("execute at @a run setblock ~" + x + " ~ ~" + z + " simpleblocks:" + name + state);
                if (name.endsWith("_wall")) server.runCommand("execute at @a run setblock ~" + x + " ~1 ~" + z + " simpleblocks:" + name);
            }
            context.waitTicks(10);
            server.runCommand("execute as @a at @s run tp @s ~ ~ ~-1 0 12");
            context.waitTicks(40);
            context.takeScreenshot("simpleblocks-front");
            server.runCommand("execute as @a at @s run tp @s ~ ~4 ~5 0 50");
            context.waitTicks(40);
            context.takeScreenshot("simpleblocks-above");
        }
    }
}
