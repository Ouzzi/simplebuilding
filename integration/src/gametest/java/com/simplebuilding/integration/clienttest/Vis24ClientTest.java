package com.simplebuilding.integration.clienttest;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
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
 * Client-Sicht 24.10. (VIS24, docs/ai/PLAN-B-CLIENTVIS24-2026-10-24.md): every screen the integration pack can open
 * that no module smoke already photographs - vanilla stations in the mod frame, mod blocks, item screens (every guide
 * book, sheet music), pause menu, key binds, every ModMenu config screen - opened the way a player opens it (a real
 * right click through {@code MultiPlayerGameMode}) and photographed, plus a few interactions (slot click and drag,
 * mouse wheel, button click, tooltip hover).
 *
 * <p>Only vanilla, Fabric and reflection: no module class is compiled in, so a module that is missing or renamed an id
 * is skipped with a log line instead of breaking the build. Each entry is guarded on its own; the run never stops at the
 * first fault. The pictures are checked by eye (vis24 preview page); the log carries {@code [vis24] OK|SKIP|FAIL|RAWKEY}
 * lines and the summary {@code [vis24] screens shot: N} the test runner looks for.
 */
public final class Vis24ClientTest implements FabricClientGameTest {
    /** A raw translation key shown as text: lowercase dotted words, at least two dots (block.mod.thing). */
    private static final Pattern RAW_KEY = Pattern.compile("[a-z0-9_]+(\\.[a-z0-9_]+){2,}");

    private final List<String> faults = new ArrayList<>();
    private int shots;
    private int index;
    /** "" for the English pass, "de-" for the German one (the owner plays in German: longer labels). */
    private String lang = "";

    @Override
    public void runTest(ClientGameTestContext context) {
        guard("title", () -> shot(context, "title"));
        guard("modmenu-list", () -> {
            context.clickScreenButton("modmenu.title");
            context.waitTicks(10);
            shot(context, "modmenu-list");
            context.setScreen(() -> null);
        });
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            for (String command : new String[] {"gamemode creative @a", "time set noon", "weather clear",
                    "gamerule advance_time false", "gamerule advance_weather false", "gamerule spawn_mobs false"}) {
                world.getServer().runCommand(command);
            }
            BlockPos base = world.getServer().computeOnServer(server ->
                    server.getPlayerList().getPlayers().getFirst().blockPosition());
            BlockPos at = base.offset(0, 0, 3);
            world.getServer().runCommand(String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:air",
                    base.getX() - 4, base.getY(), base.getZ() - 4, base.getX() + 4, base.getY() + 4, base.getZ() + 8));
            world.getServer().runCommand(String.format(Locale.ROOT, "tp @a %d.5 %d %d.5 facing %d.5 %d.5 %d.5",
                    base.getX(), base.getY(), base.getZ(), at.getX(), at.getY(), at.getZ()));
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();

            hud(context, world);
            menus(context);
            blocks(context, world, at);
            entities(context, world, at);
            items(context, world);
            configs(context);
            // Second pass in German: the same screens, where longer words overflow first.
            guard("language-de", () -> {
                language(context, "de_de");
                lang = "de-";
                hud(context, world);
                menus(context);
                blocks(context, world, at);
                entities(context, world, at);
                items(context, world);
                configs(context);
                // (no switch back to English: a resource reload right before leaving the world hung the shutdown)
            });
        }
        faults.forEach(line -> System.out.println("[vis24] FAULT " + line));
        System.out.println("[vis24] screens shot: " + shots + ", faults: " + faults.size());
    }

    // ---- HUD, inventory, tooltip ----------------------------------------------------------------------------------

    private void hud(ClientGameTestContext context, TestSingleplayerContext world) {
        String[] hotbar = {"simplebuilding:octant", "simplebuilding:netherite_building_wand", "simplebuilding:sledgehammer",
                "simplebuilding:reinforced_backpack", "simplemaps:wayfinder_map", "simplemusic:sheet_music",
                "simplebuilding:guide_book", "minecraft:diamond_pickaxe", "simplebuilding:paint_box"};
        world.getServer().runCommand("clear @a");
        for (int slot = 0; slot < hotbar.length; slot++) {
            if (known(hotbar[slot])) world.getServer().runCommand("item replace entity @a hotbar." + slot + " with " + hotbar[slot]);
        }
        world.getConnection().waitForClientboundPackets();
        context.runOnClient(c -> c.player.getInventory().setSelectedSlot(0));
        context.waitTicks(10);
        guard("hud-hotbar-octant", () -> shot(context, "hud-hotbar-octant"));
        context.runOnClient(c -> c.player.getInventory().setSelectedSlot(1));
        context.waitTicks(10);
        guard("hud-hotbar-wand", () -> shot(context, "hud-hotbar-wand"));
        // Survival inventory (creative would open the creative screen) with tooltip hover over every hotbar item.
        world.getServer().runCommand("gamemode survival @a");
        world.getConnection().waitForClientboundPackets();
        guard("inventory", () -> {
            context.runOnClient(c -> c.setScreenAndShow(new InventoryScreen(c.player)));
            context.waitTicks(5);
            shot(context, "inventory");
            for (int slot = 0; slot < hotbar.length; slot++) {
                final int s = slot;
                if (!known(hotbar[s])) continue;
                hoverSlot(context, 36 + s);
                context.waitTicks(3);
                shot(context, "tooltip-" + path(hotbar[s]));
            }
            context.setScreen(() -> null);
        });
        world.getServer().runCommand("gamemode creative @a");
        world.getConnection().waitForClientboundPackets();
    }

    // ---- menus ----------------------------------------------------------------------------------------------------

    private void menus(ClientGameTestContext context) {
        guard("pause-menu", () -> {
            context.runOnClient(c -> c.pauseGame(false));
            context.waitTicks(5);
            shot(context, "pause-menu");
            // Interaction: a real button click (Options), then back.
            context.clickScreenButton("menu.options");
            context.waitTicks(5);
            shot(context, "options");
            context.clickScreenButton("options.controls");
            context.waitTicks(5);
            shot(context, "options-controls");
            context.runOnClient(c -> c.setScreenAndShow(new net.minecraft.client.gui.screens.options.controls.KeyBindsScreen(
                    c.gui.screen(), c.options)));
            context.waitTicks(5);
            shot(context, "key-binds");
            // Interaction: the mouse wheel through the key list, down to the mod categories.
            context.getInput().setCursorPos(427, 240);
            for (int i = 0; i < 6; i++) context.getInput().scroll(-10);
            context.waitTicks(5);
            shot(context, "key-binds-scrolled");
            for (int i = 0; i < 30; i++) context.getInput().scroll(-10);
            context.waitTicks(5);
            shot(context, "key-binds-end");
            context.setScreen(() -> null);
        });
        guard("creative", () -> {
            context.runOnClient(c -> c.setScreenAndShow(new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(
                    c.player, c.player.connection.enabledFeatures(), true)));
            context.waitTicks(5);
            shot(context, "creative");
            context.getInput().setCursorPos(427, 240);
            for (int i = 0; i < 5; i++) context.getInput().scroll(-1);
            context.waitTicks(3);
            shot(context, "creative-scrolled");
            context.setScreen(() -> null);
        });
    }

    // ---- blocks with a screen -------------------------------------------------------------------------------------

    private void blocks(ClientGameTestContext context, TestSingleplayerContext world, BlockPos at) {
        String[] blocks = {
                // vanilla stations (mod frame / station mixins)
                "minecraft:crafting_table", "minecraft:furnace", "minecraft:blast_furnace", "minecraft:smoker",
                "minecraft:anvil", "minecraft:grindstone", "minecraft:loom", "minecraft:stonecutter",
                "minecraft:smithing_table", "minecraft:cartography_table", "minecraft:brewing_stand",
                "minecraft:enchanting_table", "minecraft:crafter", "minecraft:beacon", "minecraft:chest",
                "minecraft:barrel", "minecraft:hopper", "minecraft:dispenser", "minecraft:shulker_box",
                "minecraft:fletching_table", "minecraft:ender_chest",
                // mod blocks
                "simplestorage:reinforced_chest", "simplestorage:netherite_chest", "simplestorage:enderite_chest",
                "simplestorage:oak_crate", "simplestorage:storage_crafting_table",
                "simplebuilding:reinforced_hopper", "simplebuilding:netherite_hopper", "simplebuilding:enderite_hopper",
                "simpleovens:reinforced_furnace", "simpleovens:netherite_blast_furnace", "simpleovens:enderite_furnace",
                "simplebuilding:auto_smither", "simplebuilding:autonomous_crafter", "simplebuilding:astral_enchanting_table",
                "simplebuilding:astral_vault", "simplebuilding:nihil_vault",
                "simplebeacons:strong_beacon", "simplecrucibles:iron_crucible", "simplecrucibles:netherite_crucible",
                "simplemusic:disc_press", "simpleblocks:shapecutter", "simplequestionary:questionary_table",
                "simpleshops:sales_block", "simpleshops:sales_storage", "simpleshops:display_case",
                "simpletools:death_vault"};
        for (String id : blocks) {
            if (!known(id)) {
                skip(id, "no such block in this pack");
                continue;
            }
            guard(id, () -> {
                world.getServer().runCommand(cmd("setblock %d %d %d minecraft:air", at));
                world.getServer().runCommand(cmd("setblock %d %d %d " + id, at));
                world.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:air");
                world.getConnection().waitForClientboundPackets();
                context.waitTicks(3);
                context.runOnClient(c -> c.gameMode.useItemOn(c.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(at).add(0, 0.5, 0), Direction.UP, at, false)));
                if (!awaitScreen(context, id)) {
                    shot(context, "block-" + path(id) + "-noscreen");
                    return;
                }
                shot(context, "block-" + path(id));
                if (id.equals("minecraft:chest")) chestInteraction(context, world);
                close(context);
            });
        }
        world.getServer().runCommand(cmd("setblock %d %d %d minecraft:air", at));
    }

    /** Interaction in a vanilla chest: click a stack up, drag it over three slots, put the rest back. */
    private void chestInteraction(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runCommand("item replace entity @a hotbar.0 with minecraft:iron_ingot 60");
        world.getConnection().waitForClientboundPackets();
        context.waitTicks(3);
        int hotbarSlot = 27 + 27; // chest 27 + main inventory 27, then the hotbar
        hoverSlot(context, hotbarSlot);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(2);
        hoverSlot(context, 0);
        context.waitTicks(2);
        shot(context, "interaction-chest-carried");
        context.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
        for (int slot = 0; slot < 3; slot++) {
            hoverSlot(context, slot);
            context.waitTicks(2);
        }
        context.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(3);
        shot(context, "interaction-chest-dragged");
        hoverSlot(context, hotbarSlot);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(2);
    }

    // ---- entities with a screen -----------------------------------------------------------------------------------

    private void entities(ClientGameTestContext context, TestSingleplayerContext world, BlockPos at) {
        guard("villager-trades", () -> {
            world.getServer().runCommand(cmd("summon minecraft:villager %d %d %d {NoAI:1b,VillagerData:{profession:\"minecraft:toolsmith\",level:3,type:\"minecraft:plains\"},Offers:{Recipes:[{buy:{id:\"minecraft:emerald\",count:4},sell:{id:\"minecraft:iron_pickaxe\",count:1},maxUses:12},{buy:{id:\"minecraft:coal\",count:15},sell:{id:\"minecraft:emerald\",count:1},maxUses:16}]}}", at));
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(5);
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var villager = player.level().getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class,
                        new net.minecraft.world.phys.AABB(at).inflate(2)).getFirst();
                interact(villager, player);
            });
            if (awaitScreen(context, "villager")) shot(context, "entity-villager-trades");
            close(context);
            world.getServer().runCommand("kill @e[type=minecraft:villager]");
        });
        guard("horse-inventory", () -> {
            world.getServer().runCommand(cmd("summon minecraft:horse %d %d %d {NoAI:1b,Tame:1b,Variant:0}", at));
            world.getServer().runCommand("item replace entity @e[type=minecraft:horse,limit=1] saddle with minecraft:saddle");
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(5);
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var horse = player.level().getEntitiesOfClass(net.minecraft.world.entity.animal.equine.AbstractHorse.class,
                        new net.minecraft.world.phys.AABB(at).inflate(2)).getFirst();
                horse.openCustomInventoryScreen(player);
            });
            if (awaitScreen(context, "horse")) shot(context, "entity-horse-inventory");
            close(context);
            world.getServer().runCommand("kill @e[type=minecraft:horse]");
        });
    }

    /** {@code Entity.interact(player, hand[, location])}, whichever shape this line has. */
    private static void interact(net.minecraft.world.entity.Entity entity, net.minecraft.world.entity.player.Player player) {
        for (var method : entity.getClass().getMethods()) {
            var types = method.getParameterTypes();
            if (!method.getName().equals("interact") || types.length < 2
                    || !types[0].isAssignableFrom(player.getClass()) || types[1] != InteractionHand.class) continue;
            Object[] args = new Object[types.length];
            args[0] = player;
            args[1] = InteractionHand.MAIN_HAND;
            for (int i = 2; i < types.length; i++) args[i] = types[i] == Vec3.class ? entity.position() : null;
            try {
                method.invoke(entity, args);
                return;
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("interact", e);
            }
        }
        throw new AssertionError("no Entity.interact(Player, InteractionHand, ...) on " + entity.getClass());
    }

    // ---- items with a screen --------------------------------------------------------------------------------------

    private void items(ClientGameTestContext context, TestSingleplayerContext world) {
        // (octant and wand screens open by key, the core client suite photographs them)
        List<String> ids = new ArrayList<>(List.of("simplemusic:sheet_music", "simplemaps:wayfinder_map",
                "simpletweaks:claim_deed"));
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            // German pass: the mega guide only (the module books are Vanilla books, they wrap by themselves).
            if (id.getPath().startsWith("guide_book") && (lang.isEmpty() || id.toString().equals("simplebuilding:guide_book"))) {
                ids.add(id.toString());
            }
        }
        ids.sort(null);
        // Select slot 0 first and let the server hear it: "weapon.mainhand" is the server's selected slot, and with the
        // HUD pass's slot 1 still selected there the first item went into the wrong slot (the first book "opened nothing").
        context.runOnClient(c -> c.player.getInventory().setSelectedSlot(0));
        context.waitTicks(20);
        for (String id : ids) {
            if (!known(id)) {
                skip(id, "no such item in this pack");
                continue;
            }
            guard(id, () -> {
                world.getServer().runCommand("item replace entity @a weapon.mainhand with " + id);
                world.getConnection().waitForClientboundPackets();
                context.runOnClient(c -> c.player.getInventory().setSelectedSlot(0));
                context.waitTicks(3);
                context.runOnClient(c -> c.gameMode.useItem(c.player, InteractionHand.MAIN_HAND));
                if (!quietScreen(context)) {
                    context.waitTicks(10);
                    context.runOnClient(c -> c.gameMode.useItem(c.player, InteractionHand.MAIN_HAND));
                }
                if (!awaitScreen(context, id)) {
                    skip(id, "use opens no screen");
                    return;
                }
                shot(context, "item-" + path(id));
                close(context);
            });
        }
        world.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:air");
    }

    // ---- config screens (ModMenu entry points) ----------------------------------------------------------------------

    private void configs(ClientGameTestContext context) {
        List<String> mods = new ArrayList<>();
        for (var container : FabricLoader.getInstance().getEntrypointContainers("modmenu", Object.class)) {
            String mod = container.getProvider().getMetadata().getId();
            if (mod.startsWith("simple") || mod.equals("wiringexample")) mods.add(mod);
        }
        mods.sort(null);
        for (String mod : mods) {
            guard("config-" + mod, () -> {
                boolean open = context.computeOnClient(c -> {
                    Screen screen = configScreen(mod);
                    if (screen == null) return false;
                    c.setScreenAndShow(screen);
                    return true;
                });
                if (!open) {
                    skip("config-" + mod, "no config screen factory");
                    return;
                }
                context.waitTicks(5);
                shot(context, "config-" + mod);
                // A tab labelled with a raw key: open it to see what it holds.
                double[] tab = context.computeOnClient(c -> {
                    for (var child : c.gui.screen().children()) {
                        if (child instanceof AbstractWidget w && RAW_KEY.matcher(w.getMessage().getString().strip()).matches()) {
                            double scale = (double) c.getWindow().getScreenWidth() / c.getWindow().getGuiScaledWidth();
                            return new double[] {(w.getX() + 4) * scale, (w.getY() + w.getHeight() / 2.0) * scale};
                        }
                    }
                    return null;
                });
                if (tab != null) {
                    context.getInput().setCursorPos(tab[0], tab[1]);
                    context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
                    context.waitTicks(5);
                    shot(context, "config-" + mod + "-rawtab");
                }
                context.setScreen(() -> null);
            });
        }
    }

    private static Screen configScreen(String mod) {
        for (var container : FabricLoader.getInstance().getEntrypointContainers("modmenu", Object.class)) {
            if (!container.getProvider().getMetadata().getId().equals(mod)) continue;
            try {
                Object api = container.getEntrypoint();
                Object factory = api.getClass().getMethod("getModConfigScreenFactory").invoke(api);
                if (factory == null) return null;
                for (Class<?> type : factory.getClass().getInterfaces()) {
                    try {
                        return (Screen) type.getMethod("create", Screen.class).invoke(factory, (Screen) null);
                    } catch (NoSuchMethodException ignored) {
                        // not the factory interface
                    }
                }
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("config screen of " + mod, e);
            }
        }
        return null;
    }

    private static void language(ClientGameTestContext context, String language) {
        var reload = context.computeOnClient(c -> {
            c.setScreenAndShow(null);
            c.getLanguageManager().setSelected(language);
            c.options.languageCode = language;
            return c.reloadResourcePacks();
        });
        context.waitFor(c -> reload.isDone(), 2400);
        reload.join();
        context.waitFor(c -> c.gui.overlay() == null, 600);
    }

    // ---- helpers --------------------------------------------------------------------------------------------------

    /** True when a screen opened within 20 ticks; no fault recorded (the caller retries). */
    private static boolean quietScreen(ClientGameTestContext context) {
        try {
            context.waitFor(c -> c.gui.screen() != null, 20);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private boolean awaitScreen(ClientGameTestContext context, String what) {
        try {
            context.waitFor(c -> c.gui.screen() != null, 40);
        } catch (Throwable e) {
            faults.add(what + ": no screen opened within 40 ticks");
            return false;
        }
        context.waitTicks(5);
        return true;
    }

    private static void close(ClientGameTestContext context) {
        context.runOnClient(c -> {
            if (c.gui.screen() instanceof AbstractContainerScreen<?>) c.player.closeContainer();
            else c.setScreenAndShow(null);
        });
        context.waitTicks(3);
    }

    /** Puts the cursor over menu slot {@code slot} of the open container screen (window pixels). */
    private static void hoverSlot(ClientGameTestContext context, int slot) {
        double[] point = context.computeOnClient(c -> {
            if (!(c.gui.screen() instanceof AbstractContainerScreen<?> screen)) throw new AssertionError("no container screen");
            var s = screen.getMenu().getSlot(slot);
            int left = (screen.width - field(screen, "imageWidth")) / 2;
            int top = (screen.height - field(screen, "imageHeight")) / 2;
            var window = c.getWindow();
            double scale = (double) window.getScreenWidth() / window.getGuiScaledWidth();
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
        context.runOnClient(c -> c.gui.toastManager().clear());
        if (!name.startsWith("tooltip-") && !name.startsWith("interaction-")) context.getInput().setCursorPos(1, 1);
        context.waitTicks(1);
        String rawKeys = context.computeOnClient(Vis24ClientTest::rawKeys);
        String screen = context.computeOnClient(c -> c.gui.screen() == null ? "-" : c.gui.screen().getClass().getName());
        String file = String.format(Locale.ROOT, "vis24-%03d-%s%s", index++, lang, name);
        context.takeScreenshot(file);
        shots++;
        System.out.println("[vis24] OK " + file + " -> " + screen);
        if (!rawKeys.isEmpty()) {
            System.out.println("[vis24] RAWKEY " + file + ": " + rawKeys);
            faults.add(file + ": raw translation keys " + rawKeys);
        }
    }

    /** Title and widget texts of the open screen that look like untranslated keys. */
    private static String rawKeys(Minecraft c) {
        Screen screen = c.gui.screen();
        if (screen == null) return "";
        List<String> found = new ArrayList<>();
        Function<String, Boolean> raw = text -> RAW_KEY.matcher(text.strip()).matches();
        if (raw.apply(screen.getTitle().getString())) found.add(screen.getTitle().getString());
        for (var child : screen.children()) {
            if (child instanceof AbstractWidget widget && raw.apply(widget.getMessage().getString())) {
                found.add(widget.getMessage().getString());
            }
        }
        return String.join(", ", found);
    }

    private void guard(String what, ThrowingRunnable work) {
        try {
            work.run();
        } catch (Throwable e) {
            faults.add(what + ": " + e);
            System.out.println("[vis24] FAIL " + what + ": " + e);
        }
    }

    private static void skip(String what, String why) {
        System.out.println("[vis24] SKIP " + what + ": " + why);
    }

    private static boolean known(String id) {
        Identifier key = Identifier.tryParse(id);
        return key != null && (BuiltInRegistries.BLOCK.containsKey(key) || BuiltInRegistries.ITEM.containsKey(key));
    }

    private static String path(String id) {
        return id.replace(':', '-').replace('_', '-');
    }

    private static String cmd(String format, BlockPos at) {
        return String.format(Locale.ROOT, format, at.getX(), at.getY(), at.getZ());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
