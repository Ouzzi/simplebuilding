package com.simplebuilding.modules.simpleblocks;

import com.mojang.blaze3d.platform.InputConstants;
import com.simpleblocks.BlocksRegistry;
import com.simpleblocks.compact.CollapsedEffect;
import com.simpleblocks.pale.PaleBlocks;
import com.simpleblocks.saw.ShapecutterMenu;
import com.simpleblocks.saw.client.ShapecutterScreen;
import com.simpleblocks.snow.FrostGrassBlock;
import com.simpleblocks.snow.SnowBlocks;
import com.simpleblocks.snow.SnowCapBlock;
import com.simpleblocks.snow.SnowCaps;
import com.simpleblocks.snow.SnowConfig;
import com.simpleblocks.snow.SnowloggedBlock;
import com.simpleblocks.snow.Snowlogging;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Client smoke: the module boots, and a build scene shows every block: a front row of the stairs, slabs and walls, a back
 * row of the full blocks, with screenshots from the front and from above; then the Shapecutter in the world and its
 * screen (planks, stone bricks), which also proves the saw's recipe list reached the client.
 *
 * <p>PLAN40 simpleblocks-5 (in the game what was only seen at the tab's ends): snow (snowlogged fences, flowers and grass, snow
 * caps on fence, wall, stairs and slab), frost grass frozen by the real code path next to grass, snowy grass and the grass slab
 * in two biomes, all pale stages with the waxed ones, the Collapsed alarm in hotbar and inventory, the sandstone pillars, the
 * froglights by day and night, every cracked/mossy brick (built from Vanilla's textures) over its base, the blocks new in
 * simpleblocks-3, the config screen (as Mod Menu opens it) and, only with JEI, the saw tab. Every block is set on the server by
 * code; the server checks are collected and fail the test only after the last screenshot.
 */
public final class ModuleClientSmoke implements FabricClientGameTest {
    /** Checks of the PLAN40 scenes; the test fails at the end if any holds an entry. */
    private final List<String> faults = Collections.synchronizedList(new ArrayList<>());
    /** Ground cell (first air above the floor) of the first PLAN40 plot; each further plot lies 48 blocks east. */
    private BlockPos ground;

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
            // Shapecutter: in the world next to a stonecutter, then its screen with planks and with stone.
            server.runCommand("execute as @a at @s run tp @s ~ ~ ~-4 0 12");
            server.runCommand("execute at @a run fill ~-12 ~ ~-2 ~12 ~6 ~12 minecraft:air");
            server.runCommand("execute at @a run setblock ~-1 ~ ~3 minecraft:stonecutter[facing=north]");
            server.runCommand("execute at @a run setblock ~1 ~ ~3 simpleblocks:shapecutter[facing=north]");
            server.runCommand("execute at @a run setblock ~3 ~ ~3 simpleblocks:shapecutter[facing=west]");
            server.runCommand("execute as @a at @s run tp @s ~1 ~0.6 ~0.4 0 38");
            context.waitTicks(40);
            context.takeScreenshot("simpleblocks-saw-world");
            // UI03: the same screen in Vanilla's stonecutter look (style off), for the before / after pair
            com.simplelib.api.client.ui.UiStyleToggle.setEnabled(false);
            sawScreen(context, "ui03-saw-old", new ItemStack(Items.STONE_BRICKS, 40), Items.STONE_BRICK_STAIRS);
            com.simplelib.api.client.ui.UiStyleToggle.setEnabled(true);
            sawScreen(context, "ui03-saw-box", new ItemStack(Items.STONE_BRICKS, 40), Items.STONE_BRICK_STAIRS);
            sawScreen(context, "simpleblocks-saw-planks", new ItemStack(Items.OAK_PLANKS, 16), Items.OAK_STAIRS);
            sawScreen(context, "simpleblocks-saw-stone", new ItemStack(Items.STONE_BRICKS, 16), Items.STONE_BRICK_STAIRS);
            paleGarden(context, server);
            plan40Scenes(context, world);
            if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplebuilding")) testCenter(context, world);
        }
        if (!faults.isEmpty()) throw new AssertionError("simpleblocks client checks: " + String.join("; ", faults));
    }

    /**
     * DW04D-1, with SimpleBuilding loaded: {@code /testcenter mod <id> build here} lays the block families of a mod out in
     * front of the player; photographed from above (spectator, looking straight down) and at an angle.
     */
    private static void testCenter(ClientGameTestContext context, net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext world) {
        var server = world.getServer();
        server.runCommand("gamemode spectator @a");
        String[] mods = {"simpleblocks", "simpleoctets"};
        for (int i = 0; i < mods.length; i++) {
            String mod = mods[i];
            server.runCommand("execute as @a at @s run tp @s ~ ~ ~" + (i == 0 ? 40 : 200) + " 0 0");
            server.runCommand("execute as @a at @s run testcenter mod " + mod + " build here");
            context.waitTicks(20);
            server.runCommand("execute as @a at @s run tp @s ~30 ~40 ~32 0 90");
            world.getConnection().waitForChunksRender();
            context.waitTicks(60);
            context.takeScreenshot(String.join("-", "tc01", mod, "above"));
            server.runCommand("execute as @a at @s run tp @s ~ ~30 ~ 0 90");
            world.getConnection().waitForChunksRender();
            context.waitTicks(60);
            context.takeScreenshot(String.join("-", "tc01", mod, "high"));
            server.runCommand("execute as @a at @s run tp @s ~-12 ~-58 ~-30 0 35");
            world.getConnection().waitForChunksRender();
            context.waitTicks(60);
            context.takeScreenshot(String.join("-", "tc01", mod, "angle"));
            server.runCommand("execute as @a at @s run tp @s ~-18 ~-12 ~-2 0 0");
        }
    }

    private static void sawScreen(ClientGameTestContext context, String name, ItemStack input, net.minecraft.world.item.Item pick) {
        context.runOnClient(c -> {
            var menu = new ShapecutterMenu(0, c.player.getInventory());
            menu.container.setItem(0, input);
            if (menu.cuts().isEmpty()) throw new AssertionError(name + ": no cuts on the client (saw recipe list not synced)");
            for (int i = 0; i < menu.cuts().size(); i++) if (menu.cuts().get(i).result().item().value() == pick) menu.clickMenuButton(c.player, i);
            c.setScreenAndShow(new ShapecutterScreen<>(menu, c.player.getInventory(), Component.translatable("container.simpleblocks.shapecutter")));
        });
        context.waitTicks(5);
        context.takeScreenshot(name);
        context.runOnClient(c -> c.setScreenAndShow(null));
    }

    /**
     * DW04B (b-pale01): the five pale chains in the Pale Garden, one row per block (pumpkin, carved pumpkin, jack o'lantern,
     * melon, hay bale), seen from the front left to right Vanilla | faded | bleached | pale; pale moss floor, pale oak behind.
     */
    private static void paleGarden(ClientGameTestContext context, net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server) {
        server.runCommand("execute at @a run fill ~-12 ~ ~-2 ~12 ~8 ~16 minecraft:air");
        server.runCommand("execute at @a run fill ~-12 ~-1 ~-2 ~12 ~-1 ~16 minecraft:pale_moss_block");
        server.runCommand("execute at @a run fillbiome ~-16 ~-4 ~-8 ~16 ~16 ~24 minecraft:pale_garden");
        String[] rows = {"pumpkin", "carved_pumpkin[facing=north]", "jack_o_lantern[facing=north]", "melon", "hay_block"};
        String[] stages = {"minecraft:", "simpleblocks:faded_", "simpleblocks:bleached_", "simpleblocks:pale_"};
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < stages.length; c++) {
                server.runCommand("execute at @a run setblock ~" + (3 - 2 * c) + " ~ ~" + (3 + 2 * r) + " " + stages[c] + rows[r]);
            }
        }
        for (int x : new int[]{-7, 7}) {
            server.runCommand("execute at @a run fill ~" + x + " ~ ~12 ~" + x + " ~5 ~12 minecraft:pale_oak_log");
            server.runCommand("execute at @a run fill ~" + (x - 2) + " ~5 ~10 ~" + (x + 2) + " ~7 ~14 minecraft:pale_oak_leaves[persistent=true] keep");
        }
        server.runCommand("execute at @a run fill ~-4 ~ ~14 ~4 ~ ~14 minecraft:pale_moss_carpet");
        context.waitTicks(20);
        server.runCommand("execute as @a at @s run tp @s ~ ~3 ~-1 0 32");
        context.waitTicks(60);
        context.takeScreenshot("pale01-garden");
        server.runCommand("execute as @a at @s run tp @s ~ ~ ~2 0 20");
        context.waitTicks(40);
        context.takeScreenshot("pale01-close");
    }

    // ================================================================================== PLAN40 simpleblocks-5

    /**
     * The scenes of PLAN40 simpleblocks-5, each on its own cleared plot 48 blocks east of the last (spectator camera, random ticks
     * off so nothing melts, thaws, ages or spreads while it is photographed); afterwards the player is back where it was.
     */
    private void plan40Scenes(ClientGameTestContext context, TestSingleplayerContext world) {
        var server = world.getServer();
        double[] start = server.computeOnServer(s -> {
            var p = s.getPlayerList().getPlayers().getFirst();
            return new double[] {p.getX(), p.getY(), p.getZ()};
        });
        server.runCommand("gamemode spectator @a");
        server.runCommand("gamerule random_tick_speed 0");
        server.runCommand("time set noon");
        server.runCommand("weather clear");
        // the first plot lies 160 blocks east of the earlier scenes: the camera loads its chunks, then the ground is read there
        tp(server, start[0] + 160, start[1] + 4, start[2], 0, 20);
        world.getConnection().waitForChunksRender();
        context.waitTicks(20);
        ground = server.computeOnServer(s -> s.overworld().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                BlockPos.containing(start[0] + 160, 0, start[2])));
        snow(context, world, 0);
        frostGrass(context, world, 1);
        paleWaxed(context, world, 2);
        collapsed(context, world, 3);
        pillarsAndFroglights(context, world, 4);
        agedBricks(context, world, 5);
        newBlocks(context, world, 7);
        configScreen(context);
        jeiSaw(context, world);
        server.runCommand("gamemode creative @a");
        server.runCommand("clear @a");
        server.runCommand("time set noon");
        server.runCommand("gamerule random_tick_speed 3");
        tp(server, start[0], start[1], start[2], 0, 0);
        world.getConnection().waitForChunksRender();
        context.waitTicks(20);
    }

    /** SN30 / SNOW32: snowlogged fences (1, 3, 5 layers), flowers, grass and fern; snow caps on wall, stairs, slab and fence. */
    private void snow(ClientGameTestContext context, TestSingleplayerContext world, int plot) {
        BlockPos o = plot(context, world, plot, "minecraft:grass_block", "minecraft:snowy_plains");
        world.getServer().runOnServer(s -> {
            ServerLevel level = s.overworld();
            // three connected oak fences, each cell then shared with snow (Snowlogging.snowlog, as a snow layer used on it does)
            int[] layers = {1, 3, 5};
            for (int i = 0; i < 3; i++) put(level, o.offset(-7 + i, 0, 3), "minecraft:oak_fence");
            for (int i = 0; i < 3; i++) {
                BlockPos p = o.offset(-7 + i, 0, 3);
                Snowlogging.snowlog(level, p, level.getBlockState(p));
                level.setBlock(p, level.getBlockState(p).trySetValue(SnowloggedBlock.LAYERS, layers[i]), Block.UPDATE_ALL);
                expectHeld(level, p, "minecraft:oak_fence", layers[i]);
            }
            // snow caps (SNOW32-FALL, SnowCaps.capAt as falling snow calls it): on blocks without a full top face; on the slab half a block lower
            String[] capped = {"minecraft:cobblestone_wall", "minecraft:stone_brick_stairs[facing=south]", "minecraft:stone_slab[type=bottom]", "minecraft:oak_fence"};
            for (int i = 0; i < capped.length; i++) {
                BlockPos p = o.offset(-2 + 2 * i, 0, 3);
                put(level, p, capped[i]);
                if (!SnowCaps.capAt(level, p.above())) faults.add("no snow cap on " + capped[i]);
                BlockState cap = level.getBlockState(p.above());
                boolean lowered = capped[i].contains("slab");
                if (!cap.is(SnowBlocks.SNOW_CAP) || cap.getValue(SnowCapBlock.LOWERED) != lowered) faults.add("snow cap on " + capped[i] + ": " + cap);
            }
            // flowers, grass, fern and bush with one layer (SnowCaps.snowlogPlant, as falling snow leaves it), a poppy with three
            String[] plants = {"minecraft:poppy", "minecraft:dandelion", "minecraft:short_grass", "minecraft:fern", "minecraft:cornflower", "minecraft:bush"};
            for (int i = 0; i < plants.length; i++) {
                BlockPos p = o.offset(-7 + 2 * i, 0, 6);
                put(level, p, plants[i]);
                if (!SnowCaps.snowlogPlant(level, p, level.getBlockState(p))) faults.add("could not snowlog " + plants[i]);
                expectHeld(level, p, plants[i], 1);
            }
            BlockPos poppy = o.offset(6, 0, 6);
            put(level, poppy, "minecraft:poppy");
            Snowlogging.snowlog(level, poppy, level.getBlockState(poppy));
            level.setBlock(poppy, level.getBlockState(poppy).trySetValue(SnowloggedBlock.LAYERS, 3), Block.UPDATE_ALL);
            expectHeld(level, poppy, "minecraft:poppy", 3);
            // Vanilla for comparison: a snow layer on the grass and a plain fence
            put(level, o.offset(-7, 0, 9), "minecraft:snow[layers=1]");
            put(level, o.offset(-5, 0, 9), "minecraft:oak_fence");
        });
        look(context, world, o, 0.5, 1.4, -4.5, 0, 22);
        context.takeScreenshot("simpleblocks-snow-front");
        look(context, world, o, 0.5, 4.5, -1.5, 0, 55);
        context.takeScreenshot("simpleblocks-snow-above");
    }

    /**
     * SN30-2: frost grass made by the real freezing code ({@link FrostGrassBlock#freezes}; switch on and chance 1 only for this
     * call, then the config is as before) next to grass, snowy grass and the grass slab (with a snow cap) in a snowy biome;
     * behind them grass, grass slab, frost grass and snowy grass in a jungle, so the biome colour of grass and grass slab shows.
     */
    private void frostGrass(ClientGameTestContext context, TestSingleplayerContext world, int plot) {
        BlockPos o = plot(context, world, plot, "minecraft:grass_block", "minecraft:snowy_plains");
        biome(world.getServer(), o.offset(-16, -4, 9), o.offset(16, 12, 20), "minecraft:jungle");
        world.getServer().runOnServer(s -> {
            ServerLevel level = s.overworld();
            // front row (snowy): grass | grass + snow | frozen + snow | frozen, snow taken away | grass slab | grass slab + cap
            String[] row = {"minecraft:grass_block", "minecraft:grass_block", "minecraft:grass_block", "minecraft:grass_block",
                    "simpleblocks:grass_slab", "simpleblocks:grass_slab"};
            for (int i = 0; i < row.length; i++) put(level, o.offset(-5 + 2 * i, 0, 3), row[i]);
            for (int i : new int[] {1, 2, 3}) put(level, o.offset(-5 + 2 * i, 1, 3), "minecraft:snow[layers=1]");
            if (!SnowCaps.capAt(level, o.offset(5, 1, 3))) faults.add("no snow cap on the grass slab");
            boolean was = SnowConfig.frostGrass;
            int chance = SnowConfig.freezeOneIn;
            BlockPos a = o.offset(-1, 0, 3), b = o.offset(1, 0, 3);
            System.out.println("[simpleblocks] frost grass switch in this run: " + was + " (default off since PLAN40 simpleblocks-2)");
            if (!was && FrostGrassBlock.canFreeze(level, a)) faults.add("frost grass would freeze while its switch is off");
            SnowConfig.frostGrass = true;
            SnowConfig.freezeOneIn = 1;
            try {
                for (BlockPos p : new BlockPos[] {a, b}) {
                    if (!FrostGrassBlock.freezes(level.getBlockState(p), level, p, level.getRandom()) || !level.getBlockState(p).is(SnowBlocks.FROST_GRASS)) {
                        faults.add("snowy grass in the snowy biome did not freeze at " + p.toShortString() + ": " + level.getBlockState(p));
                    }
                }
            } finally {
                SnowConfig.frostGrass = was;
                SnowConfig.freezeOneIn = chance;
            }
            level.setBlock(b.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            // back row (jungle): grass | grass slab | frost grass | snowy grass
            String[] back = {"minecraft:grass_block", "simpleblocks:grass_slab", "simpleblocks:frost_grass", "minecraft:grass_block"};
            for (int i = 0; i < back.length; i++) put(level, o.offset(-3 + 2 * i, 0, 13), back[i]);
            put(level, o.offset(3, 1, 13), "minecraft:snow[layers=1]");
        });
        look(context, world, o, 0.5, 3.5, -3.5, 0, 30);
        context.takeScreenshot("simpleblocks-frost-grass");
        look(context, world, o, 0.5, 0.8, -1.0, 0, 18);
        context.takeScreenshot("simpleblocks-frost-close");
    }

    /** DW04B / PALE01: per chain Vanilla | faded | bleached | pale, then the waxed ones (waxed, waxed faded, waxed bleached). */
    private void paleWaxed(ClientGameTestContext context, TestSingleplayerContext world, int plot) {
        BlockPos o = plot(context, world, plot, "minecraft:smooth_stone", null);
        world.getServer().runOnServer(s -> {
            ServerLevel level = s.overworld();
            int[] x = {-7, -5, -3, -1, 3, 5, 7};
            for (int r = 0; r < PaleBlocks.CHAINS.size(); r++) {
                String base = PaleBlocks.CHAINS.get(r).id().getPath();
                String[] row = {"minecraft:" + base, "simpleblocks:faded_" + base, "simpleblocks:bleached_" + base, "simpleblocks:pale_" + base,
                        "simpleblocks:waxed_" + base, "simpleblocks:waxed_faded_" + base, "simpleblocks:waxed_bleached_" + base};
                for (int c = 0; c < row.length; c++) put(level, o.offset(x[c], 0, 2 + 2 * r), row[c]);
            }
        });
        look(context, world, o, 0.5, 4.5, -4.5, 0, 35);
        context.takeScreenshot("simpleblocks-pale-waxed");
    }

    /**
     * WH26-10: two Collapsed stacks in the hotbar slow the player (Slowness II on the server) and their slots glow red
     * (SimpleLib SlotAlert, Simple Blocks' rule); the dense and compressed stacks beside them do not. Hotbar and inventory.
     */
    private void collapsed(ClientGameTestContext context, TestSingleplayerContext world, int plot) {
        BlockPos o = plot(context, world, plot, "minecraft:smooth_stone", null);
        var server = world.getServer();
        // the three stages as blocks in front of the player
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            String[] stages = {"minecraft:cobblestone", "simpleblocks:compressed_cobblestone", "simpleblocks:dense_cobblestone", "simpleblocks:collapsed_cobblestone"};
            for (int i = 0; i < stages.length; i++) put(level, o.offset(-3 + 2 * i, 0, 4), stages[i]);
        });
        server.runCommand("gamemode survival @a");
        server.runCommand("clear @a");
        String[] hotbar = {"simpleblocks:collapsed_cobblestone", "simpleblocks:collapsed_dirt", "simpleblocks:dense_dirt", "simpleblocks:compressed_stone", "minecraft:cobblestone"};
        for (int k = 0; k < hotbar.length; k++) server.runCommand("item replace entity @a hotbar." + k + " with " + hotbar[k]);
        tp(server, o.getX() + 0.5, o.getY(), o.getZ() + 0.5, 0, 25);
        world.getConnection().waitForClientboundPackets();
        context.runOnClient(c -> c.player.getInventory().setSelectedSlot(4));
        world.getConnection().waitForChunksRender();
        context.waitTicks(3 * CollapsedEffect.INTERVAL);
        int amp = server.computeOnServer(s -> {
            var effect = s.getPlayerList().getPlayers().getFirst().getEffect(MobEffects.SLOWNESS);
            return effect == null ? -1 : effect.getAmplifier();
        });
        if (amp != 1) faults.add("two Collapsed stacks should give Slowness II, amplifier " + amp);
        boolean[] glow = context.computeOnClient(c -> {
            boolean[] out = new boolean[hotbar.length];
            for (int k = 0; k < out.length; k++) out[k] = com.simplelib.api.client.SlotAlert.active(c.player.getInventory().getItem(k));
            return out;
        });
        if (!glow[0] || !glow[1] || glow[2] || glow[3] || glow[4]) faults.add("slot alarm should glow on the two Collapsed slots only: " + java.util.Arrays.toString(glow));
        context.takeScreenshot("simpleblocks-collapsed-hotbar");
        context.runOnClient(c -> c.setScreenAndShow(new InventoryScreen(c.player)));
        context.getInput().setCursorPos(1, 1);
        context.waitTicks(10);
        context.takeScreenshot("simpleblocks-collapsed-inventory");
        context.runOnClient(c -> c.player.closeContainer());
        context.waitTicks(3);
        server.runCommand("clear @a");
        server.runCommand("effect clear @a");
        server.runCommand("gamemode spectator @a");
    }

    /** PAL14: the sandstone pillars standing and lying next to the sandstone families and the quartz pillar; froglights 3 + 3. */
    private void pillarsAndFroglights(ClientGameTestContext context, TestSingleplayerContext world, int plot) {
        BlockPos o = plot(context, world, plot, "minecraft:smooth_stone", null);
        world.getServer().runOnServer(s -> {
            ServerLevel level = s.overworld();
            String[][] row = {{"-8", "minecraft:sandstone"}, {"-7", "minecraft:cut_sandstone"}, {"-6", "minecraft:chiseled_sandstone"},
                    {"-4", "simpleblocks:sandstone_pillar[axis=y]"}, {"-2", "simpleblocks:sandstone_pillar[axis=x]"},
                    {"0", "minecraft:quartz_pillar[axis=y]"},
                    {"2", "minecraft:red_sandstone"}, {"3", "minecraft:cut_red_sandstone"}, {"4", "minecraft:chiseled_red_sandstone"},
                    {"6", "simpleblocks:red_sandstone_pillar[axis=y]"}, {"8", "simpleblocks:red_sandstone_pillar[axis=x]"}};
            for (String[] e : row) {
                BlockPos p = o.offset(Integer.parseInt(e[0]), 0, 3);
                put(level, p, e[1]);
                if (e[1].contains("axis=y")) put(level, p.above(), e[1]);
            }
            String[] lights = {"minecraft:ochre_froglight", "minecraft:verdant_froglight", "minecraft:pearlescent_froglight",
                    "simpleblocks:scarlet_froglight", "simpleblocks:aqua_froglight", "simpleblocks:azure_froglight"};
            for (int i = 0; i < lights.length; i++) put(level, o.offset(-5 + 2 * i, 0, 8), lights[i]);
        });
        look(context, world, o, 0.5, 0.4, -4.5, 0, 8);
        context.takeScreenshot("simpleblocks-pillars");
        look(context, world, o, 0.5, 2.2, 1.5, 0, 25);
        context.takeScreenshot("simpleblocks-froglights-day");
        world.getServer().runCommand("time set midnight");
        context.waitTicks(40);
        context.takeScreenshot("simpleblocks-froglights-night");
        world.getServer().runCommand("time set noon");
        context.waitTicks(10);
    }

    /**
     * The cracked and mossy bricks ({@link BlocksRegistry#AGED}): their textures are built from Vanilla's at build time and never
     * committed, so only the game shows them. Each stands under its base brick, nine per picture; no placeholder marker may be loaded.
     */
    private void agedBricks(ClientGameTestContext context, TestSingleplayerContext world, int plot) {
        boolean placeholders = context.computeOnClient(c -> c.getResourceManager()
                .getResource(Identifier.fromNamespaceAndPath("simpleblocks", "vanilla_derived_placeholders.txt")).isPresent());
        if (placeholders) faults.add("the Vanilla-derived brick textures are grey placeholders (marker vanilla_derived_placeholders.txt loaded)");
        int half = (BlocksRegistry.AGED.size() + 1) / 2;
        for (int part = 0; part < 2; part++) {
            BlockPos o = plot(context, world, plot + part, "minecraft:smooth_stone", null);
            List<String[]> pairs = BlocksRegistry.AGED.subList(part * half, Math.min(BlocksRegistry.AGED.size(), (part + 1) * half));
            world.getServer().runOnServer(s -> {
                ServerLevel level = s.overworld();
                for (int i = 0; i < pairs.size(); i++) {
                    String[] pair = pairs.get(i);
                    BlockPos p = o.offset(-8 + 2 * i, 0, 3);
                    put(level, p, "simpleblocks:" + pair[0]);
                    put(level, p.above(), pair[1].contains(":") ? pair[1] : "minecraft:" + pair[1]);
                }
            });
            look(context, world, o, 0.5, 0.0, -6.0, 0, 6);
            context.takeScreenshot(part == 0 ? "simpleblocks-aged-bricks-a" : "simpleblocks-aged-bricks-b");
        }
    }

    /**
     * simpleblocks-3: the red sand slab next to sand, red sand and the sand slab; suspended red sand hanging in the air; levitating
     * red sand rising up to a ceiling (server check); the sculk tile stairs, slab and wall next to the tiles; the chiseled blue nether
     * bricks next to the blue and cracked blue ones, the red family behind.
     */
    private void newBlocks(ClientGameTestContext context, TestSingleplayerContext world, int plot) {
        BlockPos o = plot(context, world, plot, "minecraft:smooth_stone", null);
        BlockPos rising = o.offset(-1, 0, 3);
        world.getServer().runOnServer(s -> {
            ServerLevel level = s.overworld();
            put(level, o.offset(-8, 0, 3), "minecraft:sand");
            put(level, o.offset(-7, 0, 3), "simpleblocks:sand_slab");
            put(level, o.offset(-6, 0, 3), "minecraft:red_sand");
            put(level, o.offset(-5, 0, 3), "simpleblocks:red_sand_slab");
            put(level, o.offset(-3, 1, 3), "simpleblocks:suspended_red_sand");
            put(level, rising.above(3), "minecraft:smooth_stone");
            put(level, rising, "simpleblocks:levitating_red_sand");
            put(level, o.offset(1, 0, 3), "simpleblocks:sculk_tiles");
            put(level, o.offset(2, 0, 3), "simpleblocks:sculk_tile_stairs[facing=south]");
            put(level, o.offset(3, 0, 3), "simpleblocks:sculk_tile_slab");
            put(level, o.offset(4, 0, 3), "simpleblocks:sculk_tile_wall");
            put(level, o.offset(4, 1, 3), "simpleblocks:sculk_tile_wall");
            String[] blue = {"simpleblocks:blue_nether_bricks", "simpleblocks:cracked_blue_nether_bricks", "simpleblocks:chiseled_blue_nether_bricks"};
            String[] red = {"minecraft:red_nether_bricks", "simpleblocks:cracked_red_nether_bricks", "simpleblocks:chiseled_red_nether_bricks"};
            for (int i = 0; i < 3; i++) {
                put(level, o.offset(6 + i, 0, 3), blue[i]);
                put(level, o.offset(6 + i, 0, 5), red[i]);
            }
        });
        context.waitTicks(60);
        String risen = world.getServer().computeOnServer(s -> {
            ServerLevel level = s.overworld();
            return BuiltInRegistries.BLOCK.getKey(level.getBlockState(rising).getBlock()) + " / "
                    + BuiltInRegistries.BLOCK.getKey(level.getBlockState(rising.above(2)).getBlock());
        });
        if (!risen.equals("minecraft:air / simpleblocks:levitating_red_sand")) faults.add("levitating red sand should rise to the ceiling: " + risen);
        look(context, world, o, 0.5, 0.6, -5.0, 0, 10);
        context.takeScreenshot("simpleblocks-new-blocks");
        look(context, world, o, 5.5, 0.4, -0.5, 0, 15);
        context.takeScreenshot("simpleblocks-new-blocks-close");
    }

    /**
     * The config screen of Simple Blocks as a player opens it: with Mod Menu through the factory SimpleLib offers it
     * ({@code SimpleLibModMenu.getProvidedConfigScreenFactories}), without Mod Menu straight from SimpleLib's generic screen.
     * The other tabs (pale, collapsed) are clicked when the screen offers them as buttons (not promised as screenshots).
     */
    private void configScreen(ClientGameTestContext context) {
        context.setScreen(() -> {
            try {
                return openConfig();
            } catch (ReflectiveOperationException | RuntimeException e) {
                faults.add("config screen: " + e);
                return null;
            }
        });
        context.waitTicks(10);
        String shown = context.computeOnClient(c -> c.gui.screen() == null ? "-" : c.gui.screen().getClass().getName());
        System.out.println("[simpleblocks] config screen -> " + shown);
        if (shown.equals("-")) faults.add("the config screen of Simple Blocks did not open");
        context.getInput().setCursorPos(1, 1);
        context.waitTicks(2);
        context.takeScreenshot("simpleblocks-config-screen");
        for (String group : new String[] {"pale", "collapsed"}) {
            if (context.tryClickScreenButton("text.autoconfig.simpleblocks.category." + group)) {
                context.getInput().setCursorPos(1, 1);
                context.waitTicks(5);
                context.takeScreenshot(String.join("-", "simpleblocks", "config", group));
            } else {
                System.out.println("[simpleblocks] INFO config tab " + group + " is no button on this screen");
            }
        }
        context.setScreen(() -> null);
        context.waitTicks(2);
    }

    private Screen openConfig() throws ReflectiveOperationException {
        if (FabricLoader.getInstance().isModLoaded("modmenu")) {
            for (var container : FabricLoader.getInstance().getEntrypointContainers("modmenu", Object.class)) {
                if (!container.getProvider().getMetadata().getId().equals("simplelib")) continue;
                Object api = container.getEntrypoint();
                Object factory = ((java.util.Map<?, ?>) api.getClass().getMethod("getProvidedConfigScreenFactories").invoke(api)).get("simpleblocks");
                if (factory == null) {
                    faults.add("Mod Menu: SimpleLib offers no config screen for simpleblocks");
                    break;
                }
                var create = Class.forName("com.terraformersmc.modmenu.api.ConfigScreenFactory").getMethod("create", Screen.class);
                return (Screen) create.invoke(factory, (Screen) null);
            }
        }
        return com.simplelib.config.screen.GenericConfigScreen.create("simpleblocks", null);
    }

    /** JEI (only when loaded): U over the Shapecutter in the survival inventory opens its uses, the saw tab among them. */
    private void jeiSaw(ClientGameTestContext context, TestSingleplayerContext world) {
        if (!FabricLoader.getInstance().isModLoaded("jei")) {
            System.out.println("[simpleblocks] SKIP JEI saw tab: JEI not loaded in this client");
            return;
        }
        var server = world.getServer();
        server.runCommand("gamemode survival @a");
        server.runCommand("clear @a");
        server.runCommand("item replace entity @a hotbar.0 with simpleblocks:shapecutter");
        world.getConnection().waitForClientboundPackets();
        context.runOnClient(c -> c.setScreenAndShow(new InventoryScreen(c.player)));
        context.waitTicks(6);
        double[] point = context.computeOnClient(c -> {
            var screen = (AbstractContainerScreen<?>) c.gui.screen();
            var slot = screen.getMenu().getSlot(36);
            double scale = (double) c.getWindow().getScreenWidth() / c.getWindow().getGuiScaledWidth();
            return new double[] {(field(screen, "leftPos") + slot.x + 8) * scale, (field(screen, "topPos") + slot.y + 8) * scale};
        });
        context.getInput().setCursorPos(point[0], point[1]);
        context.waitTicks(3);
        context.getInput().pressKey(InputConstants.KEY_U);
        context.waitTicks(10);
        String shown = context.computeOnClient(c -> c.gui.screen() == null ? "-" : c.gui.screen().getClass().getName());
        System.out.println("[simpleblocks] JEI uses of the Shapecutter -> " + shown);
        if (!shown.contains("mezz.jei")) faults.add("JEI did not open the uses of the Shapecutter (" + shown + ")");
        context.getInput().setCursorPos(1, 1);
        context.waitTicks(3);
        context.takeScreenshot(String.join("-", "simpleblocks", "jei", "saw"));
        context.setScreen(() -> null);
        context.waitTicks(3);
        server.runCommand("clear @a");
        server.runCommand("gamemode spectator @a");
    }

    // ---------------------------------------------------------------------------------- helpers of the PLAN40 scenes

    /** Plot {@code n}: camera there first (its chunks load), then a cleared 29 x 11 x 25 box, a floor and optionally a biome. */
    private BlockPos plot(ClientGameTestContext context, TestSingleplayerContext world, int n, String floor, String biomeId) {
        BlockPos o = ground.offset(48 * n, 0, 0);
        var server = world.getServer();
        tp(server, o.getX() + 0.5, o.getY() + 3, o.getZ() - 5.5, 0, 20);
        world.getConnection().waitForChunksRender();
        context.waitTicks(10);
        fill(server, o.offset(-14, 0, -8), o.offset(14, 10, 16), "minecraft:air");
        fill(server, o.offset(-14, -1, -8), o.offset(14, -1, 16), floor);
        if (biomeId != null) biome(server, o.offset(-16, -4, -12), o.offset(16, 12, 20), biomeId);
        return o;
    }

    private static void fill(TestServerContext server, BlockPos a, BlockPos b, String block) {
        server.runCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d %s", a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ(), block));
    }

    private static void biome(TestServerContext server, BlockPos a, BlockPos b, String biomeId) {
        server.runCommand(String.format(Locale.ROOT, "fillbiome %d %d %d %d %d %d %s", a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ(), biomeId));
    }

    private static void tp(TestServerContext server, double x, double y, double z, float yaw, float pitch) {
        server.runCommand(String.format(Locale.ROOT, "tp @a %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
    }

    /** Camera (feet) at the plot origin plus (dx, dy, dz), waiting until the new view is drawn. */
    private static void look(ClientGameTestContext context, TestSingleplayerContext world, BlockPos o, double dx, double dy, double dz, float yaw, float pitch) {
        tp(world.getServer(), o.getX() + dx, o.getY() + dy, o.getZ() + dz, yaw, pitch);
        world.getConnection().waitForClientboundPackets();
        context.waitTicks(20);
        world.getConnection().waitForChunksRender();
        context.waitTicks(20);
    }

    /** Sets a block from its command spelling ({@code ns:path[props]}); unknown is a fault. Without a facing it faces the camera (north). */
    private void put(ServerLevel level, BlockPos pos, String spec) {
        try {
            BlockState state = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, spec, false).blockState();
            if (!spec.contains("facing=")) state = state.trySetValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
            level.setBlock(pos, state, Block.UPDATE_ALL);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            faults.add("unknown block " + spec + ": " + e.getMessage());
        }
    }

    /** The cell at {@code pos} is snowlogged with {@code layers} layers and holds {@code held}. */
    private void expectHeld(ServerLevel level, BlockPos pos, String held, int layers) {
        BlockState cell = level.getBlockState(pos);
        BlockState inside = SnowloggedBlock.held(level, pos);
        String id = inside == null ? "nothing" : BuiltInRegistries.BLOCK.getKey(inside.getBlock()).toString();
        if (!cell.is(SnowBlocks.SNOWLOGGED) || cell.getValue(SnowloggedBlock.LAYERS) != layers || !id.equals(held)) {
            faults.add("snowlogged " + held + " x" + layers + " expected at " + pos.toShortString() + ", found " + cell + " holding " + id);
        }
    }

    private static int field(AbstractContainerScreen<?> screen, String name) {
        try {
            var f = AbstractContainerScreen.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.getInt(screen);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }
}
