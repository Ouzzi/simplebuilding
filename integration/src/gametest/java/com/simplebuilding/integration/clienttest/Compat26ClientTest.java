package com.simplebuilding.integration.clienttest;

import com.mojang.blaze3d.platform.InputConstants;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Compatibility run b-compat26 (docs/ai/PLAN-B-COMPAT26-2026-11-10.md, tools/testrunner target {@code compat26-client-263}):
 * every Simple mod next to Lithium, JEI and Mod Menu. World, inventory, JEI recipe/use page, Mod Menu list and the config
 * screens it offers for the Simple mods, a block placed by a player click. Only vanilla, Fabric and reflection (Mod Menu and JEI
 * are no compile dependencies). Log {@code [compat26] OK|FAIL|INFO}; the runner also scans the whole log for mixin conflicts.
 */
public final class Compat26ClientTest implements FabricClientGameTest {
    private final List<String> faults = new ArrayList<>();
    private int shots;

    private static final String[] REQUIRED_MODS = {"simplebuilding", "lithium", "jei", "modmenu"};
    private static final String[] JEI_ITEMS = {"minecraft:oak_log", "simpletools:strip_station", "simplebuilding:reinforced_hopper"};
    private static final String[] PLACE = {"minecraft:stone", "simpleblocks:chiseled_prismarine", "simplestorage:pipe"};

    @Override
    public void runTest(ClientGameTestContext context) {
        for (String id : REQUIRED_MODS) {
            boolean loaded = FabricLoader.getInstance().isModLoaded(id);
            System.out.println("[compat26] INFO mod " + id + " loaded=" + loaded);
            if (!loaded) faults.add("mod not loaded: " + id);
        }
        long simple = FabricLoader.getInstance().getAllMods().stream().filter(m -> m.getMetadata().getId().startsWith("simple")).count();
        System.out.println("[compat26] INFO simple mods loaded: " + simple);
        if (simple < 20) faults.add("only " + simple + " Simple mods loaded");

        guard("modmenu-list", () -> modMenuList(context));
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            for (String command : new String[] {"gamemode creative @a", "time set noon", "weather clear",
                    "gamerule advance_time false", "gamerule advance_weather false", "gamerule spawn_mobs false"}) {
                world.getServer().runCommand(command);
            }
            BlockPos base = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().blockPosition());
            world.getServer().runCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:air",
                    base.getX() - 6, base.getY(), base.getZ() - 4, base.getX() + 6, base.getY() + 5, base.getZ() + 8));
            world.getServer().runCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:smooth_stone",
                    base.getX() - 6, base.getY() - 1, base.getZ() - 4, base.getX() + 6, base.getY() - 1, base.getZ() + 8));
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            context.waitTicks(40); // Lithium's ticking/lighting/entity changes get a few real ticks
            shot(context, "world");

            guard("place", () -> place(context, world, base));
            guard("inventory", () -> inventory(context, world));
            guard("jei", () -> jei(context, world));
            guard("modmenu-config", () -> modMenuConfigs(context));
            context.waitTicks(40);
        }
        faults.forEach(line -> System.out.println("[compat26] FAULT " + line));
        System.out.println("[compat26] done: shots " + shots + ", faults " + faults.size());
    }

    /** Mod Menu's own list screen (class by reflection), opened like the title screen button does. */
    private void modMenuList(ClientGameTestContext context) throws Throwable {
        var ctor = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen").getConstructor(Screen.class);
        // Screen.<init> calls Minecraft.getInstance(), which Fabric's client gametest forbids on the test thread: build on the client.
        context.setScreen(() -> {
            try {
                return (Screen) ctor.newInstance((Screen) null);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
        context.waitTicks(10);
        shot(context, "modmenu-list");
        context.setScreen(() -> null);
    }

    /**
     * The config screens Mod Menu offers (entrypoint "modmenu"): the mods' own, then (L13) the generic SimpleLib screens it
     * provides for the other Simple mods; open, photograph the first ones, close; an exception is a fault.
     */
    private void modMenuConfigs(ClientGameTestContext context) {
        int opened = 0;
        java.util.Set<String> seen = new java.util.HashSet<>();
        java.util.Map<String, Object> provided = new java.util.LinkedHashMap<>();
        for (var container : FabricLoader.getInstance().getEntrypointContainers("modmenu", Object.class)) {
            String id = container.getProvider().getMetadata().getId();
            if (!id.startsWith("simple")) continue;
            final int index = opened;
            guard("modmenu-config-" + id, () -> {
                Object api = container.getEntrypoint();
                Object factory = api.getClass().getMethod("getModConfigScreenFactory").invoke(api);
                if (!factory.getClass().getName().endsWith("NullScreenFactory")) open(context, id, factory, index < 4);
                Object more = api.getClass().getMethod("getProvidedConfigScreenFactories").invoke(api);
                ((java.util.Map<?, ?>) more).forEach((k, v) -> provided.putIfAbsent((String) k, v));
            });
            seen.add(id);
            opened++;
        }
        int generic = 0;
        for (var e : provided.entrySet()) {
            String id = e.getKey();
            if (!id.startsWith("simple") || seen.contains(id)) continue;
            final boolean photo = generic < 2;
            guard("modmenu-config-" + id, () -> open(context, id, e.getValue(), photo));
            generic++;
        }
        opened += generic;
        System.out.println("[compat26] INFO generic config screens (SimpleLib, L13): " + generic);
        System.out.println("[compat26] INFO config screens tried: " + opened);
        if (opened == 0) faults.add("Mod Menu offers no config screen of a Simple mod");
    }

    /** Opens a Mod Menu config screen factory on the client thread, logs the screen class, photographs it if asked, closes. */
    private void open(ClientGameTestContext context, String id, Object factory, boolean photo) throws Exception {
        Method create = Class.forName("com.terraformersmc.modmenu.api.ConfigScreenFactory").getMethod("create", Screen.class);
        context.setScreen(() -> {
            try {
                return (Screen) create.invoke(factory, (Screen) null);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
        context.waitTicks(8);
        String shown = context.computeOnClient(c -> c.gui.screen() == null ? "-" : c.gui.screen().getClass().getName());
        System.out.println("[compat26] INFO config " + id + " -> " + shown);
        if (shown.equals("-")) faults.add("config screen of " + id + " did not open");
        if (photo) shot(context, "config-" + id);
        context.setScreen(() -> null);
        context.waitTicks(2);
    }

    /** A block placed by a player click (useItemOn), proven on the server. */
    private void place(ClientGameTestContext context, TestSingleplayerContext world, BlockPos base) {
        for (int i = 0; i < PLACE.length; i++) {
            String id = PLACE[i];
            if (!known(id)) {
                System.out.println("[compat26] INFO place skip unknown " + id);
                continue;
            }
            BlockPos target = base.offset(-4 + 2 * i, 0, 3);
            world.getServer().runCommand("item replace entity @a weapon.mainhand with " + id);
            world.getConnection().waitForClientboundPackets();
            context.runOnClient(c -> c.player.getInventory().setSelectedSlot(0));
            context.waitTicks(3);
            BlockPos ground = target.below();
            context.runOnClient(c -> c.gameMode.useItemOn(c.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false)));
            context.waitTicks(6);
            boolean placed = world.getServer().computeOnServer(s -> !s.overworld().getBlockState(target).isAir());
            System.out.println("[compat26] " + (placed ? "OK" : "FAIL") + " place " + id + " at " + target);
            if (!placed) faults.add("block not placed: " + id);
        }
        shot(context, "placed");
    }

    private void inventory(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runCommand("gamemode survival @a");
        world.getServer().runCommand("clear @a");
        for (int slot = 0; slot < JEI_ITEMS.length; slot++) {
            if (known(JEI_ITEMS[slot])) world.getServer().runCommand("item replace entity @a hotbar." + slot + " with " + JEI_ITEMS[slot]);
        }
        world.getConnection().waitForClientboundPackets();
        context.runOnClient(c -> c.setScreenAndShow(new InventoryScreen(c.player)));
        context.waitTicks(10);
        shot(context, "inventory");
        context.setScreen(() -> null);
    }

    /** JEI: R (recipe) and U (uses) over an item in the inventory must open a JEI screen. */
    private void jei(ClientGameTestContext context, TestSingleplayerContext world) {
        for (String id : JEI_ITEMS) {
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
                    System.out.println("[compat26] INFO jei " + kind + " " + id + " -> " + screen);
                    if (!screen.contains("mezz.jei")) faults.add("JEI " + kind + " page did not open for " + id + " (" + screen + ")");
                    context.getInput().setCursorPos(1, 1);
                    shot(context, "jei-" + kind + "-" + id.replace(':', '-'));
                    context.setScreen(() -> null);
                    context.waitTicks(3);
                });
            }
        }
        world.getServer().runCommand("clear @a");
        world.getServer().runCommand("gamemode creative @a");
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
        context.runOnClient(c -> {
            c.gui.toastManager().clear();
            c.gui.hud.getChat().clearMessages(true);
        });
        context.waitTicks(2);
        String file = String.format(Locale.ROOT, "compat26-%03d-%s", shots, name);
        context.takeScreenshot(file);
        shots++;
        System.out.println("[compat26] OK " + file);
    }

    private void guard(String what, ThrowingRunnable work) {
        try {
            work.run();
        } catch (Throwable e) {
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) root = root.getCause();
            String text = root == e ? e.toString() : e.getClass().getSimpleName() + " <- " + root;
            faults.add(what + ": " + text);
            System.out.println("[compat26] FAIL " + what + ": " + text);
            e.printStackTrace(System.out);
        }
    }

    private static boolean known(String id) {
        Identifier key = Identifier.tryParse(id);
        return key != null && (BuiltInRegistries.BLOCK.containsKey(key) || BuiltInRegistries.ITEM.containsKey(key));
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
