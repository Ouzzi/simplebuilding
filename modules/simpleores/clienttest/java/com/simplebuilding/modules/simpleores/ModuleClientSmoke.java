package com.simplebuilding.modules.simpleores;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Client smoke of Simple Ores (PLAN40 simpleores-6): the module boots; then, HUD hidden, spectator camera, scenes built fresh in front of
 * the player: an ore showcase (every ore and its deepslate / Nether / End form, Nether rose gold ore, sage ore, dimensional scrap,
 * tin-bearing sand and gravel, geode crust), the storage and scrap blocks, lead plates and silver sheets stacked 1..8 high (and on a
 * wall), the metal doors and trapdoors closed and open, the silver mirror on wall, floor and ceiling, item frames with every ingot,
 * nugget, raw metal, pressling, apple and carrot, the presslings and the food in the survival inventory; then world generation: each of
 * nine features ({@code place feature simpleores:<id>}, ids of {@code data/simpleores/worldgen/feature}) placed in the middle of a block
 * of its host rock, photographed cut open (front half of the host gone) and as an x-ray (all host rock gone). The block and item lists
 * come from the registry by name pattern, so a renamed or added metal shows up without touching this test. A feature that places
 * nothing in six tries is reported after the last picture. Sounds (lead damping, metal sounds) need an ear: owner check.
 */
public final class ModuleClientSmoke implements FabricClientGameTest {
    /** Feature id, host rock ("split" = stone above, deepslate below; "bed" = sand and gravel), cut and x-ray screenshot. */
    private static final String[][] FEATURES = {
            {"zinc_geode", "minecraft:stone", "simpleores-worldgen-zinc-geode-cut", "simpleores-worldgen-zinc-geode-xray"},
            {"silver_ore", "split", "simpleores-worldgen-silver-cut", "simpleores-worldgen-silver-xray"},
            {"nickel_ore", "minecraft:deepslate", "simpleores-worldgen-nickel-cut", "simpleores-worldgen-nickel-xray"},
            {"tin_ore", "minecraft:stone", "simpleores-worldgen-tin-cut", "simpleores-worldgen-tin-xray"},
            {"tin_placer", "bed", "simpleores-worldgen-tin-placer-cut", "simpleores-worldgen-tin-placer-xray"},
            {"lead_ore", "minecraft:stone", "simpleores-worldgen-lead-cut", "simpleores-worldgen-lead-xray"},
            {"sage_ore", "split", "simpleores-worldgen-sage-cut", "simpleores-worldgen-sage-xray"},
            {"dimensional_scrap", "minecraft:deepslate", "simpleores-worldgen-scrap-cut", "simpleores-worldgen-scrap-xray"},
            {"nether_gold_rose", "minecraft:netherrack", "simpleores-worldgen-nether-rose-gold-cut", "simpleores-worldgen-nether-rose-gold-xray"},
    };
    private static final List<String> PROBLEMS = new ArrayList<>();
    private static BlockPos base;

    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simpleores")) throw new AssertionError("Module did not boot");
        PROBLEMS.clear();
        context.takeScreenshot("simpleores-title");
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server -> {
                base = server.getPlayerList().getPlayers().getFirst().blockPosition();
                run(server, "gamemode spectator @a");
                run(server, "time set noon");
                run(server, "gamerule advance_time false");
                run(server, "weather clear");
            });
            hud(context, false);
            showcase(context, world);
            plates(context, world);
            doors(context, world);
            mirror(context, world);
            items(context, world);
            for (String[] feature : FEATURES) worldgen(context, world, feature);
            hud(context, true);
        }
        if (!PROBLEMS.isEmpty()) throw new AssertionError(PROBLEMS.size() + " Simple Ores problem(s): " + String.join("; ", PROBLEMS));
    }

    /** Every ore form in rows, then every storage and scrap block. */
    private static void showcase(ClientGameTestContext context, TestSingleplayerContext world) {
        List<String> ores = blocks(p -> p.endsWith("_ore") || p.contains("dimensional_scrap") || p.startsWith("tin_bearing") || p.equals("geode_crust"));
        List<String> storage = blocks(p -> p.endsWith("_block"));
        if (ores.size() < 15) PROBLEMS.add("only " + ores.size() + " ore blocks: " + ores);
        world.getServer().runOnServer(server -> {
            clear(server);
            grid(server, ores, 10);
        });
        settle(context, world);
        look(context, world, 0.0, 4.0, 0.0, 0.0, 0.5, 8.0);
        context.takeScreenshot("simpleores-ores-front");
        look(context, world, 0.0, 12.0, 4.0, 0.0, 0.0, 9.0);
        context.takeScreenshot("simpleores-ores-above");
        world.getServer().runOnServer(server -> {
            clear(server);
            grid(server, storage, 12);
        });
        settle(context, world);
        look(context, world, 0.0, 6.0, -1.0, 0.0, 0.5, 9.0);
        context.takeScreenshot("simpleores-storage-blocks");
        System.out.println("[ores] showcase: " + ores.size() + " ores, " + storage.size() + " storage blocks");
    }

    /** Blocks in rows of {@code perRow}, one block apart, the first row at z 6, read left to right from the front. */
    private static void grid(MinecraftServer server, List<String> ids, int perRow) {
        for (int i = 0; i < ids.size(); i++) {
            int x = 2 * (perRow / 2) - 1 - 2 * (i % perRow);
            set(server, x, 0, 6 + 2 * (i / perRow), ids.get(i));
        }
    }

    /** Lead plates and silver sheets with 1..8 layers on the floor (left to right), and two of each on a wall face. */
    private static void plates(ClientGameTestContext context, TestSingleplayerContext world) {
        List<String> plates = blocks(p -> p.equals("lead_plate") || p.equals("silver_sheet"));
        if (plates.size() != 2) PROBLEMS.add("plates: " + plates);
        world.getServer().runOnServer(server -> {
            clear(server);
            fill(server, -9, 0, 12, 9, 2, 12, "minecraft:stone");
            for (int r = 0; r < plates.size(); r++) {
                for (int layers = 1; layers <= 8; layers++) {
                    set(server, 9 - 2 * layers, 0, 6 + 3 * r, plates.get(r) + "[facing=up,layers=" + layers + "]");
                }
                set(server, -1 + 6 * r, 1, 11, plates.get(r) + "[facing=north,layers=8]");
                set(server, -3 + 6 * r, 1, 11, plates.get(r) + "[facing=north,layers=3]");
            }
        });
        settle(context, world);
        look(context, world, 0.0, 4.5, 1.0, 0.0, 0.3, 9.0);
        context.takeScreenshot("simpleores-plates");
    }

    /** Each metal door closed and open, each trapdoor closed and open, side by side. */
    private static void doors(ClientGameTestContext context, TestSingleplayerContext world) {
        List<String> doors = blocks(p -> p.endsWith("_door"));
        if (doors.size() < 6) PROBLEMS.add("only " + doors.size() + " doors: " + doors);
        world.getServer().runOnServer(server -> {
            clear(server);
            for (int i = 0; i < doors.size(); i++) {
                int x = 3 * (doors.size() / 2) - 2 - 3 * i;
                String door = doors.get(i);
                for (int k = 0; k < 2; k++) {
                    String open = k == 0 ? "false" : "true";
                    set(server, x + k, 0, 8, door + "[half=lower,facing=north,hinge=left,open=" + open + "]");
                    set(server, x + k, 1, 8, door + "[half=upper,facing=north,hinge=left,open=" + open + "]");
                }
                String trapdoor = door.substring(0, door.length() - "_door".length()) + "_trapdoor";
                if (BuiltInRegistries.BLOCK.containsKey(Identifier.parse(trapdoor))) {
                    set(server, x, 0, 5, trapdoor + "[half=bottom,facing=north,open=false]");
                    set(server, x + 1, 0, 5, trapdoor + "[half=bottom,facing=north,open=true]");
                } else {
                    PROBLEMS.add("no trapdoor for " + door);
                }
            }
        });
        settle(context, world);
        look(context, world, 0.0, 3.5, -3.0, 0.0, 1.0, 7.0);
        context.takeScreenshot("simpleores-doors");
    }

    /** The silver mirror on a wall, on the floor and under a ceiling block, a gold block in front. */
    private static void mirror(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            clear(server);
            fill(server, -4, 0, 9, 4, 3, 9, "minecraft:polished_andesite");
            set(server, -2, 1, 8, "simpleores:silver_mirror[face=wall,facing=north]");
            set(server, 0, 0, 7, "simpleores:silver_mirror[face=floor,facing=north]");
            set(server, 2, 3, 8, "minecraft:polished_andesite");
            set(server, 2, 2, 8, "simpleores:silver_mirror[face=ceiling,facing=north]");
            set(server, 0, 0, 5, "minecraft:gold_block");
        });
        settle(context, world);
        look(context, world, 0.5, 2.8, 3.0, 0.0, 1.2, 8.5);
        context.takeScreenshot("simpleores-mirror");
    }

    /** Item frames with every ingot, nugget, raw metal, pressling, apple and carrot; presslings and food in the inventory. */
    private static void items(ClientGameTestContext context, TestSingleplayerContext world) {
        List<String> items = itemIds(p -> p.endsWith("_ingot") || p.endsWith("_nugget") || p.startsWith("raw_") || p.endsWith("_pressling")
                || p.endsWith("_apple") || p.endsWith("_carrot"));
        List<String> food = itemIds(p -> p.endsWith("_pressling") || p.endsWith("_apple") || p.endsWith("_carrot"));
        if (food.size() < 6) PROBLEMS.add("only " + food.size() + " presslings/apples/carrots: " + food);
        world.getServer().runOnServer(server -> {
            clear(server);
            fill(server, -8, 0, 9, 7, 5, 9, "minecraft:spruce_planks");
            ServerLevel level = server.overworld();
            for (int i = 0; i < items.size() && i < 16 * 5; i++) {
                ItemFrame frame = new ItemFrame(level, base.offset(7 - i % 16, 4 - i / 16, 8), Direction.NORTH);
                frame.setItem(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(items.get(i)))), false);
                level.addFreshEntity(frame);
            }
        });
        settle(context, world);
        look(context, world, -0.5, 2.5, 0.0, -0.5, 2.5, 8.5);
        context.takeScreenshot("simpleores-items-frames");
        world.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            run(server, "gamemode survival @a");
            player.getInventory().clearContent();
            for (int i = 0; i < food.size() && i < 36; i++) {
                player.getInventory().setItem(i, new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(food.get(i))), 1 + i % 3));
            }
        });
        world.getConnection().waitForClientboundPackets();
        context.waitTicks(5);
        context.runOnClient(c -> c.setScreenAndShow(new InventoryScreen(c.player)));
        context.waitTicks(5);
        context.takeScreenshot("simpleores-items-inventory");
        context.runOnClient(c -> c.setScreenAndShow(null));
        world.getServer().runOnServer(server -> {
            server.getPlayerList().getPlayers().getFirst().getInventory().clearContent();
            run(server, "gamemode spectator @a");
        });
        world.getConnection().waitForClientboundPackets();
    }

    /** One feature in a 9 x 9 x 9 block of its host rock: placed, cut open (front half), then the host taken away (x-ray). */
    private static void worldgen(ClientGameTestContext context, TestSingleplayerContext world, String[] f) {
        String id = "simpleores:" + f[0];
        boolean bed = f[1].equals("bed");
        List<String> hosts = bed ? List.of("minecraft:sand", "minecraft:gravel")
                : f[1].equals("split") ? List.of("minecraft:stone", "minecraft:deepslate") : List.of(f[1]);
        int top = bed ? 2 : 8;
        int[] placed = new int[1];
        world.getServer().runOnServer(server -> {
            clear(server);
            if (bed) {
                fill(server, -4, 0, 10, 0, top, 18, hosts.get(0));
                fill(server, 1, 0, 10, 4, top, 18, hosts.get(1));
            } else {
                fill(server, -4, 0, 10, 4, top, 18, hosts.get(0));
                if (hosts.size() > 1) fill(server, -4, 0, 10, 4, 4, 18, hosts.get(1));
            }
            BlockPos centre = base.offset(0, bed ? 1 : 4, 14);
            for (int attempt = 0; attempt < 6 && placed[0] == 0; attempt++) {
                run(server, "place feature " + id + " " + centre.getX() + " " + centre.getY() + " " + centre.getZ());
                placed[0] = countForeign(server.overworld(), hosts, top);
            }
            System.out.println("[ores] feature " + id + ": " + placed[0] + " blocks");
        });
        if (placed[0] == 0) PROBLEMS.add("feature " + id + " placed nothing in " + hosts);
        settle(context, world);
        world.getServer().runOnServer(server -> {
            for (String host : hosts) fill(server, -4, 0, 10, 4, top, 13, "minecraft:air replace " + host);
        });
        settle(context, world);
        look(context, world, -6.0, top + 4.0, 3.0, 0.0, top / 2.0, 14.0);
        context.takeScreenshot(f[2]);
        world.getServer().runOnServer(server -> {
            for (String host : hosts) fill(server, -4, 0, 10, 4, top, 18, "minecraft:air replace " + host);
        });
        settle(context, world);
        context.takeScreenshot(f[3]);
    }

    /** Blocks in the host block that are neither air nor host rock: what the feature placed. */
    private static int countForeign(ServerLevel level, List<String> hosts, int top) {
        int n = 0;
        for (BlockPos q : BlockPos.betweenClosed(base.offset(-4, 0, 10), base.offset(4, top, 18))) {
            Block block = level.getBlockState(q).getBlock();
            if (block != Blocks.AIR && !hosts.contains(BuiltInRegistries.BLOCK.getKey(block).toString())) n++;
        }
        return n;
    }

    /** Simple Ores block ids whose path matches, sorted (with namespace). */
    private static List<String> blocks(Predicate<String> path) {
        List<String> out = new ArrayList<>();
        for (Identifier key : BuiltInRegistries.BLOCK.keySet()) {
            if (key.getNamespace().equals("simpleores") && path.test(key.getPath())) out.add(key.toString());
        }
        out.sort(null);
        return out;
    }

    /** Simple Ores item ids whose path matches, sorted (with namespace). */
    private static List<String> itemIds(Predicate<String> path) {
        List<String> out = new ArrayList<>();
        for (Identifier key : BuiltInRegistries.ITEM.keySet()) {
            if (key.getNamespace().equals("simpleores") && path.test(key.getPath())) out.add(key.toString());
        }
        out.sort(null);
        return out;
    }

    private static void hud(ClientGameTestContext context, boolean shown) {
        context.runOnClient(c -> {
            if (c.gui.hud.isHidden() == shown) c.gui.hud.toggle();
        });
    }

    private static void settle(ClientGameTestContext context, TestSingleplayerContext world) {
        context.waitTicks(10);
        world.getConnection().waitForClientboundPackets();
        world.getConnection().waitForChunksRender();
        context.waitTicks(10);
    }

    /** A smooth stone floor and air above, around the scene; the item frames of an earlier scene go. */
    private static void clear(MinecraftServer server) {
        fill(server, -16, -1, -6, 16, -1, 24, "minecraft:smooth_stone");
        fill(server, -16, 0, -6, 16, 12, 24, "minecraft:air");
        run(server, "kill @e[type=minecraft:item_frame]");
    }

    private static void set(MinecraftServer server, int x, int y, int z, String block) {
        run(server, "setblock " + at(x, y, z) + " " + block);
    }

    private static void fill(MinecraftServer server, int x0, int y0, int z0, int x1, int y1, int z1, String block) {
        run(server, "fill " + at(x0, y0, z0) + " " + at(x1, y1, z1) + " " + block);
    }

    private static String at(int x, int y, int z) {
        return (base.getX() + x) + " " + (base.getY() + y) + " " + (base.getZ() + z);
    }

    private static void run(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }

    /** Eye at base + (ex, ey, ez) looking at base + (tx, ty, tz) ({@code tp ... facing} turns from the feet, so the target is lowered alike). */
    private static void look(ClientGameTestContext context, TestSingleplayerContext world, double ex, double ey, double ez, double tx, double ty, double tz) {
        world.getServer().runOnServer(server -> run(server, String.format(Locale.ROOT, "tp @a %.2f %.2f %.2f facing %.2f %.2f %.2f",
                base.getX() + ex, base.getY() + ey - 1.62, base.getZ() + ez, base.getX() + tx, base.getY() + ty - 1.62, base.getZ() + tz)));
        world.getConnection().waitForClientboundPackets();
        world.getConnection().waitForChunksRender();
        context.waitTicks(15);
    }
}
