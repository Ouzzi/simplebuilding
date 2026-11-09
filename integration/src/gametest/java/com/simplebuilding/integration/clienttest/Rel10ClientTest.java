package com.simplebuilding.integration.clienttest;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Release acceptance A 09.11. (a-rel10, docs/ai/PLAN-A-REL10-2026-11-09.md): the client look at what came last before the
 * release - Strip Station, Simple Layers (blocks, Cloud Sea and Backrooms by teleport), Dimensional Scrap, the chiseled blocks,
 * pipe window, disc press, trumpet - plus every creative tab of the family (and the IG31-3 tab rule: no mod item in a vanilla
 * tab, no vanilla item in a mod tab), JEI recipe and use pages of the new items and the visible dev entries. Only vanilla,
 * Fabric and reflection (like Vis24 / B09). Log {@code [rel10] OK|FAIL|INFO|SKIP}; pictures are judged by eye.
 */
public final class Rel10ClientTest implements FabricClientGameTest {
    private final List<String> faults = new ArrayList<>();
    private int shots;

    private static final String[] CHISELED = {"simpleblocks:chiseled_polished_andesite", "simpleblocks:chiseled_polished_diorite",
            "simpleblocks:chiseled_polished_granite", "simpleblocks:chiseled_prismarine", "simpleblocks:chiseled_red_nether_bricks",
            "simpleblocks:smooth_stone_wall", "minecraft:chiseled_stone_bricks"};
    private static final String[] SCRAP = {"simpleores:rift_shard_block", "simpleores:nether_rift_shard_block",
            "simpleores:end_rift_shard_block", "simpleores:terranite_block", "simpleores:infernite_block",
            "simpleores:zenithite_block", "minecraft:netherite_block"};
    private static final String[] LAYERS = {"simplelayers:cloud_block", "simplelayers:cloud_stone", "simplelayers:mist_block",
            "simplelayers:glitched_bedrock", "simplelayers:rift", "simplelayers:exit_sign", "simplelayers:neon_light",
            "simplelayers:wallpaper", "simplelayers:striped_wallpaper", "simplelayers:dotted_wallpaper", "simplelayers:damp_carpet",
            "simplelayers:ceiling_tile", "simplelayers:stained_ceiling_tile", "minecraft:white_wool"};
    private static final String[] SCRAP_ITEMS = {"simpleores:rift_shard", "simpleores:nether_rift_shard", "simpleores:end_rift_shard",
            "simpleores:terranite_ingot", "simpleores:infernite_ingot", "simpleores:zenithite_ingot", "simpletools:strip_station",
            "simplemobs:trumpet", "simplelayers:cloud_block"};
    private static final String[] JEI = {"simpletools:strip_station", "simpleores:rift_shard", "simpleores:terranite_ingot",
            "simpleores:infernite_ingot", "simpleores:zenithite_ingot", "simpleores:terranite_block", "simpleblocks:chiseled_prismarine",
            "simpleblocks:smooth_stone_wall", "simplemusic:disc_press", "simplestorage:reinforced_backpack", "simplestorage:pipe",
            "simplelayers:cloud_block", "simplemobs:trumpet", "minecraft:wither_skeleton_skull", "minecraft:oak_log"};
    private static final String[] SCREENS = {"simplestorage:pipe", "simplestorage:reinforced_pipe", "simplemusic:disc_press",
            "minecraft:hopper", "simplebuilding:reinforced_hopper"};

    @Override
    public void runTest(ClientGameTestContext context) {
        guard("title-buttons", () -> {
            buttons(context, "title");
            shot(context, "title");
        });
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            for (String command : new String[] {"gamemode creative @a", "time set noon", "weather clear",
                    "gamerule advance_time false", "gamerule advance_weather false", "gamerule spawn_mobs false"}) {
                world.getServer().runCommand(command);
            }
            BlockPos base = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().blockPosition());
            world.getServer().runCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:air",
                    base.getX() - 8, base.getY(), base.getZ() - 5, base.getX() + 8, base.getY() + 6, base.getZ() + 9));
            world.getServer().runCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:smooth_stone",
                    base.getX() - 8, base.getY() - 1, base.getZ() - 5, base.getX() + 8, base.getY() - 1, base.getZ() + 9));
            BlockPos at = base.offset(0, 0, 3);
            face(world, base, Vec3.atCenterOf(at));
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();

            guard("registry", () -> registry());
            guard("tabs", () -> tabs(context));
            guard("scene-chiseled", () -> scene(context, world, base, "chiseled", CHISELED));
            guard("scene-scrap", () -> scene(context, world, base, "scrap", SCRAP));
            guard("scene-layers", () -> scene(context, world, base, "layers", LAYERS));
            guard("strip-station", () -> strip(context, world, base, at));
            guard("screens", () -> screens(context, world, base, at));
            guard("trumpet", () -> trumpet(context, world, base, at));
            guard("inventory", () -> inventory(context, world));
            guard("jei", () -> jei(context, world));
            guard("pause-buttons", () -> {
                context.runOnClient(c -> c.pauseGame(false));
                context.waitTicks(5);
                buttons(context, "pause");
                shot(context, "pause-menu");
                context.setScreen(() -> null);
            });
            guard("cloud-sea", () -> dimension(context, world, "simplelayers:cloud_sea",
                    new int[][] {{0, 40, 0, 0, 0}, {0, 120, 0, 0, 25}, {0, 260, 0, 0, 40}, {0, 400, 0, 0, 60}}));
            guard("backrooms", () -> dimension(context, world, "simplelayers:backrooms",
                    new int[][] {{4, 2, 4, 0, 0}, {4, 2, 4, 90, 0}, {44, 2, 84, 180, 10}}));
            world.getServer().runCommand(String.format(Locale.ROOT, "execute in minecraft:overworld run tp @a %d.5 %d %d.5",
                    base.getX(), base.getY(), base.getZ()));
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(20);
        }
        faults.forEach(line -> System.out.println("[rel10] FAULT " + line));
        System.out.println("[rel10] screens shot: " + shots + ", faults: " + faults.size());
    }

    private static void face(TestSingleplayerContext world, BlockPos from, Vec3 to) {
        world.getServer().runCommand(String.format(Locale.ROOT, "tp @a %d.5 %d %d.5 facing %.2f %.2f %.2f",
                from.getX(), from.getY(), from.getZ(), to.x, to.y, to.z));
    }

    private void registry() {
        for (String[] group : new String[][] {CHISELED, SCRAP, LAYERS, SCRAP_ITEMS, JEI, SCREENS}) {
            for (String id : group) {
                if (!known(id)) {
                    faults.add("not registered: " + id);
                    System.out.println("[rel10] INFO missing id " + id);
                }
            }
        }
    }

    /** Buttons of the open screen (dev entries must not show up there except the DevVariants panel switch). */
    private static void buttons(ClientGameTestContext context, String where) {
        String list = context.computeOnClient(c -> {
            if (c.gui.screen() == null) return "-";
            List<String> names = new ArrayList<>();
            for (var child : c.gui.screen().children()) {
                if (child instanceof AbstractWidget w) names.add(w.getClass().getSimpleName() + "'" + w.getMessage().getString() + "'");
            }
            return c.gui.screen().getClass().getName() + " " + names;
        });
        System.out.println("[rel10] INFO buttons " + where + ": " + list);
    }

    /** IG31-3: every creative tab of the family photographed (top and end), mod items in vanilla tabs and vanilla items in mod tabs counted. */
    private void tabs(ClientGameTestContext context) {
        List<CreativeModeTab> tabs = context.computeOnClient(c -> {
            CreativeModeTabs.tryRebuildTabContents(c.player.connection.enabledFeatures(), true, c.player.connection.registryAccess());
            return new ArrayList<>(CreativeModeTabs.tabs());
        });
        List<CreativeModeTab> mine = new ArrayList<>();
        List<String> order = new ArrayList<>();
        for (CreativeModeTab tab : tabs) {
            Identifier key = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (key == null || tab.getType() != CreativeModeTab.Type.CATEGORY) continue;
            boolean vanillaTab = key.getNamespace().equals("minecraft");
            int foreign = 0, total = 0;
            List<String> sample = new ArrayList<>();
            for (var stack : tab.getDisplayItems()) {
                total++;
                String ns = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
                boolean modItem = ns.startsWith("simple");
                if (vanillaTab == modItem) {
                    foreign++;
                    if (sample.size() < 8) sample.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                }
            }
            order.add(key + "(" + total + ")");
            if (foreign > 0) {
                System.out.println("[rel10] INFO tab " + key + (vanillaTab ? " holds mod items: " : " holds vanilla items: ") + foreign + " e.g. " + sample);
                faults.add("IG31-3 tab " + key + " has " + foreign + " foreign items, e.g. " + sample);
            }
            if (!vanillaTab && total > 0) mine.add(tab);
            if (!vanillaTab && tab.getIconItem().isEmpty()) faults.add("tab without icon: " + key);
        }
        System.out.println("[rel10] INFO tab order: " + order);
        context.runOnClient(c -> c.setScreenAndShow(new CreativeModeInventoryScreen(c.player, c.player.connection.enabledFeatures(), true)));
        context.waitTicks(5);
        int n = 0;
        for (CreativeModeTab tab : mine) {
            String name = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).toString().replace(':', '-').replace('_', '-');
            context.runOnClient(c -> {
                try {
                    var screen = c.gui.screen();
                    int page = (int) screen.getClass().getMethod("getPage", CreativeModeTab.class).invoke(screen, tab);
                    screen.getClass().getMethod("switchToPage", int.class).invoke(screen, page);
                    var m = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
                    m.setAccessible(true);
                    m.invoke(screen, tab);
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
            context.waitTicks(4);
            shot(context, String.format(Locale.ROOT, "tab-%02d-%s", n, name));
            int items = tab.getDisplayItems().size();
            if (items > 45) {
                context.getInput().setCursorPos(427, 240);
                for (int i = 0; i < 80; i++) context.getInput().scroll(-1);
                context.waitTicks(3);
                shot(context, String.format(Locale.ROOT, "tab-%02d-%s-end", n, name));
            }
            n++;
        }
        context.setScreen(() -> null);
    }

    /** A wall of blocks two high, seven wide, four blocks ahead; the last entry of each list is a vanilla block for comparison. */
    private void scene(ClientGameTestContext context, TestSingleplayerContext world, BlockPos base, String name, String[] ids) {
        BlockPos wall = base.offset(-3, 0, 4);
        world.getServer().runCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:air",
                wall.getX(), wall.getY(), wall.getZ(), wall.getX() + 6, wall.getY() + 2, wall.getZ()));
        for (int i = 0; i < ids.length; i++) {
            if (!known(ids[i])) continue;
            BlockPos p = wall.offset(i % 7, i / 7, 0);
            world.getServer().runCommand(String.format(Locale.ROOT, "setblock %d %d %d %s", p.getX(), p.getY(), p.getZ(), ids[i]));
        }
        face(world, base, new Vec3(base.getX() + 0.5, base.getY() + 1.0, wall.getZ() + 0.5));
        world.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:air");
        world.getConnection().waitForClientboundPackets();
        world.getConnection().waitForChunksRender();
        context.waitTicks(15);
        shot(context, "scene-" + name);
        // a second look from the side (light, side faces)
        world.getServer().runCommand(String.format(Locale.ROOT, "tp @a %d.5 %d %d.5 facing %d.5 %d %d.5",
                base.getX() + 6, base.getY() + 1, base.getZ() + 1, wall.getX() + 3, wall.getY() + 1, wall.getZ()));
        world.getConnection().waitForClientboundPackets();
        context.waitTicks(10);
        shot(context, "scene-" + name + "-side");
        world.getServer().runCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:air",
                wall.getX(), wall.getY(), wall.getZ(), wall.getX() + 6, wall.getY() + 2, wall.getZ()));
        face(world, base, Vec3.atCenterOf(base.offset(0, 0, 3)));
    }

    private void use(ClientGameTestContext context, BlockPos at) {
        context.runOnClient(c -> c.gameMode.useItemOn(c.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(at).add(0, 0.5, 0), Direction.UP, at, false)));
        context.waitTicks(10);
    }

    private void hand(ClientGameTestContext context, TestSingleplayerContext world, String item) {
        world.getServer().runCommand("item replace entity @a weapon.mainhand with " + item);
        world.getConnection().waitForClientboundPackets();
        context.runOnClient(c -> c.player.getInventory().setSelectedSlot(0));
        context.waitTicks(5);
    }

    /** IG31-59: logs into the cage, one axe click strips the whole stack, empty hand takes it back. */
    private void strip(ClientGameTestContext context, TestSingleplayerContext world, BlockPos base, BlockPos at) {
        world.getServer().runCommand("gamemode survival @a");
        world.getServer().runCommand(String.format(Locale.ROOT, "setblock %d %d %d simpletools:strip_station", at.getX(), at.getY(), at.getZ()));
        face(world, base, Vec3.atCenterOf(at));
        hand(context, world, "minecraft:air");
        context.runOnClient(c -> c.player.getInventory().setSelectedSlot(0));
        context.waitTicks(10);
        shot(context, "strip-empty");
        hand(context, world, "minecraft:oak_log 32");
        use(context, at);
        shot(context, "strip-loaded");
        hand(context, world, "minecraft:iron_axe");
        use(context, at);
        context.waitTicks(10);
        shot(context, "strip-worked");
        String block = world.getServer().computeOnServer(server -> server.overworld().getBlockState(at).toString());
        System.out.println("[rel10] INFO strip station after the axe: " + block);
        hand(context, world, "minecraft:air");
        use(context, at);
        String inv = world.getServer().computeOnServer(server -> {
            var p = server.getPlayerList().getPlayers().getFirst();
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < 36; i++) if (!p.getInventory().getItem(i).isEmpty()) b.append(p.getInventory().getItem(i)).append(' ');
            return b.toString();
        });
        System.out.println("[rel10] INFO inventory after taking from the cage: " + inv);
        if (!inv.contains("stripped_oak_log")) faults.add("strip station: no stripped oak logs taken back (" + inv + ")");
        shot(context, "strip-taken");
        world.getServer().runCommand("gamemode creative @a");
        world.getServer().runCommand(String.format(Locale.ROOT, "setblock %d %d %d minecraft:air", at.getX(), at.getY(), at.getZ()));
    }

    private void screens(ClientGameTestContext context, TestSingleplayerContext world, BlockPos base, BlockPos at) {
        for (String id : SCREENS) {
            if (!known(id)) continue;
            guard("screen-" + id, () -> {
                world.getServer().runCommand(String.format(Locale.ROOT, "setblock %d %d %d minecraft:air", at.getX(), at.getY(), at.getZ()));
                world.getServer().runCommand(String.format(Locale.ROOT, "setblock %d %d %d %s", at.getX(), at.getY(), at.getZ(), id));
                hand(context, world, "minecraft:air");
                shot(context, "block-" + path(id) + "-world");
                use(context, at);
                String screen = context.computeOnClient(c -> c.gui.screen() == null ? "-" : c.gui.screen().getClass().getName());
                System.out.println("[rel10] INFO " + id + " screen: " + screen);
                if (screen.equals("-")) {
                    faults.add(id + ": right click opened no screen");
                    return;
                }
                buttons(context, id);
                shot(context, "screen-" + path(id));
                close(context);
            });
        }
        world.getServer().runCommand(String.format(Locale.ROOT, "setblock %d %d %d minecraft:air", at.getX(), at.getY(), at.getZ()));
    }

    private void trumpet(ClientGameTestContext context, TestSingleplayerContext world, BlockPos base, BlockPos at) {
        hand(context, world, "simplemobs:trumpet");
        context.waitTicks(10);
        shot(context, "trumpet-hand");
        context.getInput().holdMouse(InputConstants.MOUSE_BUTTON_RIGHT);
        context.waitTicks(20);
        shot(context, "trumpet-playing");
        context.runOnClient(c -> c.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        context.waitTicks(10);
        shot(context, "trumpet-playing-front");
        context.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_RIGHT);
        context.runOnClient(c -> c.options.setCameraType(CameraType.FIRST_PERSON));
        context.waitTicks(5);
    }

    /** Survival inventory with the new items in the hotbar and the tooltip of each (rarity names, Corrupted / Void). */
    private void inventory(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runCommand("clear @a");
        world.getServer().runCommand("gamemode survival @a");
        for (int slot = 0; slot < SCRAP_ITEMS.length && slot < 9; slot++) {
            if (known(SCRAP_ITEMS[slot])) world.getServer().runCommand("item replace entity @a hotbar." + slot + " with " + SCRAP_ITEMS[slot]);
        }
        world.getConnection().waitForClientboundPackets();
        context.runOnClient(c -> c.setScreenAndShow(new InventoryScreen(c.player)));
        context.waitTicks(8);
        buttons(context, "inventory");
        shot(context, "inventory");
        for (int slot = 0; slot < SCRAP_ITEMS.length && slot < 9; slot++) {
            if (!known(SCRAP_ITEMS[slot])) continue;
            hoverSlot(context, 36 + slot);
            context.waitTicks(4);
            shot(context, "tooltip-" + path(SCRAP_ITEMS[slot]));
        }
        context.setScreen(() -> null);
        world.getServer().runCommand("gamemode creative @a");
    }

    /** JEI (dev mod in the pack): R (recipe) and U (uses) over each item in the survival inventory. */
    private void jei(ClientGameTestContext context, TestSingleplayerContext world) {
        if (!FabricLoader.getInstance().isModLoaded("jei")) {
            System.out.println("[rel10] SKIP jei not loaded");
            return;
        }
        world.getServer().runCommand("gamemode survival @a");
        for (String id : JEI) {
            if (!known(id)) continue;
            for (int key : new int[] {InputConstants.KEY_R, InputConstants.KEY_U}) {
                String kind = key == InputConstants.KEY_R ? "recipe" : "uses";
                guard("jei-" + id + "-" + kind, () -> {
                    world.getServer().runCommand("clear @a");
                    world.getServer().runCommand("item replace entity @a hotbar.0 with " + id);
                    world.getConnection().waitForClientboundPackets();
                    context.runOnClient(c -> c.setScreenAndShow(new InventoryScreen(c.player)));
                    context.waitTicks(6);
                    hoverSlot(context, 36);
                    context.waitTicks(3);
                    context.getInput().pressKey(key);
                    context.waitTicks(10);
                    String screen = context.computeOnClient(c -> c.gui.screen() == null ? "-" : c.gui.screen().getClass().getName());
                    System.out.println("[rel10] INFO jei " + kind + " " + id + " -> " + screen);
                    context.getInput().setCursorPos(1, 1);
                    shotKeepCursor(context, "jei-" + kind + "-" + path(id));
                    context.setScreen(() -> null);
                    context.waitTicks(3);
                });
            }
        }
        world.getServer().runCommand("clear @a");
        world.getServer().runCommand("gamemode creative @a");
    }

    /** Teleport into a dimension and look around: {x, y, z, yaw, pitch} per view. */
    private void dimension(ClientGameTestContext context, TestSingleplayerContext world, String dim, int[][] views) {
        boolean exists = world.getServer().computeOnServer(server -> {
            for (var level : server.getAllLevels()) if (level.dimension().identifier().toString().equals(dim)) return true;
            return false;
        });
        if (!exists) {
            faults.add("dimension not loaded: " + dim);
            return;
        }
        world.getServer().runCommand("gamemode spectator @a");
        int i = 0;
        for (int[] v : views) {
            world.getServer().runCommand(String.format(Locale.ROOT, "execute in %s run tp @a %d.5 %d %d.5 %d %d", dim, v[0], v[1], v[2], v[3], v[4]));
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(40);
            world.getConnection().waitForChunksRender();
            context.waitTicks(20);
            String where = context.computeOnClient(c -> c.level.dimension().identifier() + " y=" + (int) c.player.getY());
            System.out.println("[rel10] INFO in " + where);
            if (!where.startsWith(dim)) faults.add("teleport to " + dim + " ended in " + where);
            shot(context, "dim-" + path(dim) + "-" + i++);
        }
        world.getServer().runCommand("gamemode creative @a");
    }

    // ---- helpers ----------------------------------------------------------------------------------------------------

    private static void close(ClientGameTestContext context) {
        context.runOnClient(c -> {
            if (c.gui.screen() instanceof AbstractContainerScreen<?>) c.player.closeContainer();
            else c.setScreenAndShow(null);
        });
        context.waitTicks(3);
    }

    private static void hoverSlot(ClientGameTestContext context, int slot) {
        double[] point = context.computeOnClient(c -> {
            if (!(c.gui.screen() instanceof AbstractContainerScreen<?> screen)) throw new AssertionError("no container screen");
            var s = screen.getMenu().getSlot(slot);
            int left = (screen.width - field(screen, "imageWidth")) / 2;
            int top = (screen.height - field(screen, "imageHeight")) / 2;
            double scale = (double) c.getWindow().getScreenWidth() / c.getWindow().getGuiScaledWidth();
            return new double[] {(left + s.x + 8) * scale, (top + s.y + 8) * scale};
        });
        context.getInput().setCursorPos(point[0], point[1]);
    }

    private static int field(AbstractContainerScreen<?> screen, String name) {
        try {
            var field = AbstractContainerScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(screen);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }

    private void shot(ClientGameTestContext context, String name) {
        if (!name.startsWith("tooltip-")) context.getInput().setCursorPos(1, 1);
        shotKeepCursor(context, name);
    }

    private void shotKeepCursor(ClientGameTestContext context, String name) {
        context.runOnClient(c -> {
            c.gui.toastManager().clear();
            c.gui.hud.getChat().clearMessages(true);
        });
        context.waitTicks(2);
        String file = String.format(Locale.ROOT, "rel10-%03d-%s", shots, name);
        context.takeScreenshot(file);
        shots++;
        System.out.println("[rel10] OK " + file);
    }

    private void guard(String what, ThrowingRunnable work) {
        try {
            work.run();
        } catch (Throwable e) {
            faults.add(what + ": " + e);
            System.out.println("[rel10] FAIL " + what + ": " + e);
        }
    }

    private static boolean known(String id) {
        Identifier key = Identifier.tryParse(id.split(" ")[0]);
        return key != null && (BuiltInRegistries.BLOCK.containsKey(key) || BuiltInRegistries.ITEM.containsKey(key));
    }

    private static String path(String id) {
        return id.replace(':', '-').replace('_', '-');
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
