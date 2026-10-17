package com.simplebuilding.modules.simpleblocks;

import com.simpleblocks.BlocksRegistry;
import com.simpleblocks.saw.ShapingSawMenu;
import com.simpleblocks.saw.client.ShapingSawScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * Client smoke: the module boots, and a build scene shows every block: a front row of the stairs, slabs and walls, a back
 * row of the full blocks, with screenshots from the front and from above; then the Shaping Saw in the world and its
 * screen (planks, stone bricks), which also proves the saw's recipe list reached the client.
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
            // Shaping Saw: in the world next to a stonecutter, then its screen with planks and with stone.
            server.runCommand("execute as @a at @s run tp @s ~ ~ ~-4 0 12");
            server.runCommand("execute at @a run fill ~-12 ~ ~-2 ~12 ~6 ~12 minecraft:air");
            server.runCommand("execute at @a run setblock ~-1 ~ ~3 minecraft:stonecutter[facing=north]");
            server.runCommand("execute at @a run setblock ~1 ~ ~3 simpleblocks:shaping_saw[facing=north]");
            server.runCommand("execute at @a run setblock ~3 ~ ~3 simpleblocks:shaping_saw[facing=west]");
            server.runCommand("execute as @a at @s run tp @s ~1 ~0.6 ~0.4 0 38");
            context.waitTicks(40);
            context.takeScreenshot("simpleblocks-saw-world");
            sawScreen(context, "simpleblocks-saw-planks", new ItemStack(Items.OAK_PLANKS, 16), Items.OAK_STAIRS);
            sawScreen(context, "simpleblocks-saw-stone", new ItemStack(Items.STONE_BRICKS, 16), Items.STONE_BRICK_STAIRS);
        }
    }

    private static void sawScreen(ClientGameTestContext context, String name, ItemStack input, net.minecraft.world.item.Item pick) {
        context.runOnClient(c -> {
            var menu = new ShapingSawMenu(0, c.player.getInventory());
            menu.container.setItem(0, input);
            if (menu.cuts().isEmpty()) throw new AssertionError(name + ": no cuts on the client (saw recipe list not synced)");
            for (int i = 0; i < menu.cuts().size(); i++) if (menu.cuts().get(i).result().item().value() == pick) menu.clickMenuButton(c.player, i);
            c.setScreenAndShow(new ShapingSawScreen(menu, c.player.getInventory(), Component.translatable("container.simpleblocks.shaping_saw")));
        });
        context.waitTicks(5);
        context.takeScreenshot(name);
        context.runOnClient(c -> c.setScreenAndShow(null));
    }
}
