package com.simplebuilding.integration.clienttest;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
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
 * b-client09 (07.11.): client look at the two new sub-mods Simple Chisels and Simple Building Tools on 26.3 Fabric after
 * the move out of SimpleBuilding. Only vanilla, Fabric and reflection (like Vis24). The log carries
 * {@code [b09] OK|FAIL|INFO} lines; pictures are checked by eye (b-client09 preview page).
 */
public final class B09ToolsClientTest implements FabricClientGameTest {
    private static final String[] NAMESPACES = {"simplechisels", "simplebuildingtools"};
    private final List<String> faults = new ArrayList<>();
    private int shots;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            for (String command : new String[] {"gamemode creative @a", "time set noon", "weather clear",
                    "gamerule advance_time false", "gamerule advance_weather false", "gamerule spawn_mobs false"}) {
                world.getServer().runCommand(command);
            }
            BlockPos base = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().blockPosition());
            world.getServer().runCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:air",
                    base.getX() - 5, base.getY(), base.getZ() - 5, base.getX() + 5, base.getY() + 5, base.getZ() + 8));
            // target: a 3x3 stone-brick wall three blocks ahead (chisel and wand aim at it), a floor stone at the feet row
            BlockPos wall = base.offset(0, 0, 4);
            world.getServer().runCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:stone_bricks",
                    wall.getX() - 1, wall.getY(), wall.getZ(), wall.getX() + 1, wall.getY() + 2, wall.getZ()));
            face(world, base, wall.above());
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();

            guard("registry", () -> registry());
            guard("creative-tabs", () -> tabs(context));
            guard("chisel-hand", () -> chisel(context, world));
            guard("wand", () -> wand(context, world, wall));
            guard("octant", () -> octant(context, world, base, wall));
            guard("blueprint", () -> blueprint(context, world, base, wall));
            guard("jei", () -> jei(context, world));
        }
        faults.forEach(line -> System.out.println("[b09] FAULT " + line));
        System.out.println("[b09] screens shot: " + shots + ", faults: " + faults.size());
    }

    private void face(TestSingleplayerContext world, BlockPos base, BlockPos at) {
        world.getServer().runCommand(String.format(Locale.ROOT, "tp @a %d.5 %d %d.5 facing %d.5 %d.5 %d.5",
                base.getX(), base.getY(), base.getZ(), at.getX(), at.getY(), at.getZ()));
    }

    private void registry() {
        for (String ns : NAMESPACES) {
            List<String> items = new ArrayList<>();
            for (Identifier id : BuiltInRegistries.ITEM.keySet()) if (id.getNamespace().equals(ns)) items.add(id.getPath());
            System.out.println("[b09] INFO items " + ns + " (" + items.size() + "): " + items);
            if (items.isEmpty()) faults.add("no items of " + ns);
        }
        // old ids must be gone (they migrate): a registry hit means a double registration
        for (String old : new String[] {"simplebuilding:iron_chisel", "simplebuilding:octant", "simplebuilding:blueprint",
                "simplebuilding:netherite_building_wand"}) {
            boolean present = BuiltInRegistries.ITEM.keySet().contains(Identifier.tryParse(old));
            System.out.println("[b09] INFO old id " + old + (present ? " STILL REGISTERED" : " absent"));
            if (present) faults.add("old id still registered: " + old);
        }
    }

    /** Every creative tab holding items of the two mods: count, icon, stacks without a model; one picture per such tab. */
    private void tabs(ClientGameTestContext context) {
        List<CreativeModeTab> tabs = context.computeOnClient(c -> {
            var access = c.player.connection.registryAccess();
            CreativeModeTabs.tryRebuildTabContents(c.player.connection.enabledFeatures(), true, access);
            return new ArrayList<>(CreativeModeTabs.tabs());
        });
        List<Identifier> inTab = new ArrayList<>();
        List<CreativeModeTab> mine = new ArrayList<>();
        for (CreativeModeTab tab : tabs) {
            int n = 0;
            for (var stack : tab.getDisplayItems()) {
                Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                for (String ns : NAMESPACES) if (id.getNamespace().equals(ns)) { n++; inTab.add(id); }
            }
            if (n > 0) {
                mine.add(tab);
                System.out.println("[b09] INFO tab '" + tab.getDisplayName().getString() + "' holds " + n + " items of the two mods, icon="
                        + (tab.getIconItem().isEmpty() ? "EMPTY" : BuiltInRegistries.ITEM.getKey(tab.getIconItem().getItem())));
                if (tab.getIconItem().isEmpty()) faults.add("tab without icon: " + tab.getDisplayName().getString());
            }
        }
        for (String ns : NAMESPACES) {
            for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
                if (id.getNamespace().equals(ns) && !inTab.contains(id)) {
                    System.out.println("[b09] INFO not in any creative tab: " + id);
                }
            }
        }
        if (mine.isEmpty()) faults.add("no creative tab holds items of the two mods");
        context.runOnClient(c -> c.setScreenAndShow(new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(
                c.player, c.player.connection.enabledFeatures(), true)));
        int n = 0;
        for (CreativeModeTab tab : mine) {
            context.runOnClient(c -> {
                try {
                    var screen = c.gui.screen();
                    int page = (int) screen.getClass().getMethod("getPage", CreativeModeTab.class).invoke(screen, tab);
                    screen.getClass().getMethod("switchToPage", int.class).invoke(screen, page);
                    var m = net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class
                            .getDeclaredMethod("selectTab", CreativeModeTab.class);
                    m.setAccessible(true);
                    m.invoke(screen, tab);
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
            context.waitTicks(4);
            shot(context, String.format(Locale.ROOT, "tab-%d-%s", n++, tab.getDisplayName().getString().replaceAll("[^A-Za-z0-9]+", "_")));
            context.getInput().setCursorPos(427, 240);
            for (int i = 0; i < 8; i++) context.getInput().scroll(-1);
            context.waitTicks(3);
            shot(context, String.format(Locale.ROOT, "tab-%d-scrolled", n - 1));
            for (int i = 0; i < 40; i++) context.getInput().scroll(-1);
            context.waitTicks(3);
            shot(context, String.format(Locale.ROOT, "tab-%d-end", n - 1));
        }
        context.setScreen(() -> null);
    }

    private void give(ClientGameTestContext context, TestSingleplayerContext world, String command) {
        world.getServer().runCommand("clear @a");
        world.getServer().runCommand(command);
        world.getConnection().waitForClientboundPackets();
        context.runOnClient(c -> c.player.getInventory().setSelectedSlot(0));
        context.waitTicks(5);
    }

    private void chisel(ClientGameTestContext context, TestSingleplayerContext world) {
        give(context, world, "item replace entity @a hotbar.0 with simplechisels:iron_chisel");
        context.runOnClient(c -> c.options.setCameraType(CameraType.FIRST_PERSON));
        context.waitTicks(10);
        shot(context, "chisel-first-person");
        context.runOnClient(c -> c.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        context.waitTicks(10);
        shot(context, "chisel-third-person");
        context.runOnClient(c -> c.options.setCameraType(CameraType.FIRST_PERSON));
        // use the chisel on the wall like a player (right click): stone bricks -> next step
        context.runOnClient(c -> c.gameMode.useItemOn(c.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(c.hitResult instanceof BlockHitResult h ? h.getBlockPos() : BlockPos.ZERO),
                        Direction.NORTH, c.hitResult instanceof BlockHitResult h2 ? h2.getBlockPos() : BlockPos.ZERO, false)));
        context.waitTicks(10);
        shot(context, "chisel-after-use");
        give(context, world, "item replace entity @a hotbar.0 with simplechisels:netherite_chisel");
        context.waitTicks(5);
        shot(context, "chisel-netherite");
    }

    private void wand(ClientGameTestContext context, TestSingleplayerContext world, BlockPos wall) {
        face(world, wall.offset(0, 0, -4), wall.above());
        give(context, world, "item replace entity @a hotbar.0 with simplebuildingtools:netherite_building_wand");
        world.getConnection().waitForClientboundPackets();
        context.waitTicks(15);
        shot(context, "wand-first-person-preview");
        // variants: copper wand (3x3) with the wall block in the inventory, creative and survival
        world.getServer().runCommand("item replace entity @a hotbar.1 with minecraft:stone_bricks 64");
        world.getServer().runCommand("item replace entity @a hotbar.0 with simplebuildingtools:copper_building_wand");
        world.getConnection().waitForClientboundPackets();
        context.waitTicks(20);
        shot(context, "wand-copper-creative-preview");
        world.getServer().runCommand("gamemode survival @a");
        world.getConnection().waitForClientboundPackets();
        context.waitTicks(20);
        shot(context, "wand-copper-survival-preview");
        world.getServer().runCommand("gamemode creative @a");
        world.getServer().runCommand("item replace entity @a hotbar.0 with simplebuildingtools:netherite_building_wand");
        world.getConnection().waitForClientboundPackets();
        context.waitTicks(10);
        context.runOnClient(c -> c.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        context.waitTicks(10);
        shot(context, "wand-third-person");
        context.runOnClient(c -> c.options.setCameraType(CameraType.FIRST_PERSON));
        context.waitTicks(5);
        // settings key K opens the wand screen
        // the wand screen needs Constructor's Touch (by design)
        world.getServer().runCommand("enchant @a simplebuilding:constructors_touch");
        world.getConnection().waitForClientboundPackets();
        context.waitTicks(5);
        context.getInput().pressKey(InputConstants.KEY_K);
        context.waitTicks(10);
        String screen = context.computeOnClient(c -> c.gui.screen() == null ? "-" : c.gui.screen().getClass().getName());
        System.out.println("[b09] INFO wand K screen (Constructor's Touch): " + screen);
        if (screen.equals("-")) faults.add("key K with the wand opened no screen");
        shot(context, "wand-settings-screen");
        context.setScreen(() -> null);
    }

    private void octant(ClientGameTestContext context, TestSingleplayerContext world, BlockPos base, BlockPos wall) {
        face(world, base, wall.above());
        give(context, world, "item replace entity @a hotbar.0 with simplebuildingtools:octant_lime");
        context.waitTicks(10);
        shot(context, "octant-hud-pos-empty");
        BlockPos hit = wall.above();
        context.runOnClient(c -> c.gameMode.useItemOn(c.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(hit), Direction.NORTH, hit, false)));
        context.waitTicks(10);
        shot(context, "octant-hud-pos1");
        BlockPos hit2 = wall.offset(1, 2, 0);
        context.runOnClient(c -> c.gameMode.useItemOn(c.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(hit2), Direction.NORTH, hit2, false)));
        context.waitTicks(10);
        shot(context, "octant-hud-pos2-outline");
        context.getInput().pressKey(InputConstants.KEY_H);
        context.waitTicks(10);
        shot(context, "octant-hud-after-H");
        context.getInput().pressKey(InputConstants.KEY_H);
        context.waitTicks(5);
        context.getInput().pressKey(InputConstants.KEY_K);
        context.waitTicks(10);
        String screen = context.computeOnClient(c -> c.gui.screen() == null ? "-" : c.gui.screen().getClass().getName());
        System.out.println("[b09] INFO octant K screen: " + screen);
        if (screen.equals("-")) faults.add("key K with the octant opened no screen");
        shot(context, "octant-screen");
        context.setScreen(() -> null);
        context.runOnClient(c -> c.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        context.waitTicks(8);
        shot(context, "octant-third-person");
        context.runOnClient(c -> c.options.setCameraType(CameraType.FIRST_PERSON));
    }

    private void blueprint(ClientGameTestContext context, TestSingleplayerContext world, BlockPos base, BlockPos wall) {
        face(world, base, wall.above());
        give(context, world, "item replace entity @a hotbar.0 with simplebuildingtools:blueprint");
        context.waitTicks(5);
        context.runOnClient(c -> c.gameMode.useItem(c.player, InteractionHand.MAIN_HAND));
        context.waitTicks(15);
        String screen = context.computeOnClient(c -> c.gui.screen() == null ? "-" : c.gui.screen().getClass().getName());
        System.out.println("[b09] INFO blueprint use screen: " + screen);
        if (screen.equals("-")) faults.add("blueprint right click opened no screen");
        shot(context, "blueprint-screen");
        context.setScreen(() -> null);
        context.waitTicks(3);
        // N23: sneak + right click on a floor spot places it; empty hand + right click on it opens the editor
        BlockPos floor = base.offset(2, -1, 2);
        world.getServer().runCommand(String.format(Locale.ROOT, "setblock %d %d %d minecraft:stone", floor.getX(), floor.getY(), floor.getZ()));
        world.getConnection().waitForClientboundPackets();
        context.getInput().holdKey(InputConstants.KEY_LSHIFT);
        context.waitTicks(3);
        context.runOnClient(c -> c.gameMode.useItemOn(c.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));
        context.waitTicks(10);
        context.getInput().releaseKey(InputConstants.KEY_LSHIFT);
        world.getConnection().waitForClientboundPackets();
        BlockPos placedAt = floor.above();
        String block = context.computeOnClient(c -> BuiltInRegistries.BLOCK.getKey(c.level.getBlockState(placedAt).getBlock()).toString());
        System.out.println("[b09] INFO block above floor after sneak place: " + block);
        if (!block.endsWith("placed_blueprint")) faults.add("blueprint was not placed (block " + block + ")");
        world.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:air");
        world.getConnection().waitForClientboundPackets();
        context.waitTicks(3);
        shot(context, "blueprint-placed");
        context.runOnClient(c -> c.gameMode.useItemOn(c.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(placedAt), Direction.UP, placedAt, false)));
        context.waitTicks(15);
        screen = context.computeOnClient(c -> c.gui.screen() == null ? "-" : c.gui.screen().getClass().getName());
        System.out.println("[b09] INFO N23 empty hand on placed blueprint screen: " + screen);
        if (screen.equals("-")) faults.add("N23: empty-hand right click on the placed blueprint opened no editor");
        shot(context, "blueprint-placed-editor");
        context.setScreen(() -> null);
    }

    /** JEI (dev mod in the pack): press U (uses) over a chisel in the survival inventory; picture only, no assertion. */
    private void jei(ClientGameTestContext context, TestSingleplayerContext world) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jei")) {
            System.out.println("[b09] INFO jei not loaded");
            return;
        }
        give(context, world, "item replace entity @a hotbar.0 with simplechisels:iron_chisel");
        world.getServer().runCommand("gamemode survival @a");
        world.getConnection().waitForClientboundPackets();
        context.runOnClient(c -> c.setScreenAndShow(new net.minecraft.client.gui.screens.inventory.InventoryScreen(c.player)));
        context.waitTicks(10);
        shot(context, "jei-inventory");
        context.setScreen(() -> null);
        world.getServer().runCommand("gamemode creative @a");
    }

    private void shot(ClientGameTestContext context, String name) {
        context.runOnClient(c -> {
            c.gui.toastManager().clear();
            c.gui.hud.getChat().clearMessages(true);
        });
        context.waitTicks(2);
        context.takeScreenshot("b09-" + name);
        shots++;
        System.out.println("[b09] OK b09-" + name);
    }

    private void guard(String what, ThrowingRunnable work) {
        try {
            work.run();
        } catch (Throwable e) {
            faults.add(what + ": " + e);
            System.out.println("[b09] FAIL " + what + ": " + e);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
