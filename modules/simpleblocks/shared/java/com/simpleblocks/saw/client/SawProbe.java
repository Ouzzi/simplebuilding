package com.simpleblocks.saw.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simpleblocks.SimpleBlocks;
import com.simpleblocks.saw.Sawing;
import com.simpleblocks.saw.ShapecutterMenu;
import com.simplelib.api.client.ui.UiStyleToggle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * PLAN40 simpleblocks-5: the Shapecutter window with the REAL operating-system mouse and keyboard (pattern of Simple Machines'
 * MachinesProbe / Simple Arrows' FletchingProbe): inert unless the game folder holds {@code simpleblocks-saw-probe.json}. Then, on
 * the title screen, it creates a flat survival world and follows commands from {@code simpleblocks-saw-command.json}: {@code open}
 * (a fresh hotbar - 16 stone bricks, 16 oak planks, 16 cobbled deepslate - and a Shapecutter two blocks east of the player, opened
 * through the block's normal use), {@code style} (UI03: SimpleLib's box look on/off, {@link UiStyleToggle}), {@code close},
 * {@code guiScale}, {@code shot}, {@code quit}. Each tick it writes {@code simpleblocks-saw-status.json}: window position, every slot's
 * centre in window pixels with its item, the cuts of the input with the centre of each visible tile, the chosen cut, the first shown
 * cut (scrolling), the look, whether Simple Octets' stone brick octet exists (by id only, the module stays standalone) and the
 * player's inventory as the SERVER sees it. The driver ({@code modules/simpleblocks/tools/saw_probe.py}) moves and clicks the X11
 * pointer and presses Shift/Escape through XTest. Only this module, SimpleLib's API and Vanilla.
 */
public final class SawProbe {
    private static final Gson GSON = new Gson();
    private static final String REQUEST = "simpleblocks-saw-probe.json", STATUS = "simpleblocks-saw-status.json",
            COMMAND = "simpleblocks-saw-command.json";
    private static final Identifier OCTET = Identifier.fromNamespaceAndPath("simpleoctets", "stone_bricks_octet");
    private static Boolean active;
    private static boolean ready, worldRequested;
    private static int ticks;

    private SawProbe() {}

    public static void tick(Minecraft mc) {
        Path dir = mc.gameDirectory.toPath();
        if (active == null) active = Files.isRegularFile(dir.resolve(REQUEST));
        if (!active) return;
        ticks++;
        try {
            if (!ready) {
                if (!worldRequested) {
                    if (mc.level != null || mc.gui.overlay() != null || mc.gui.screen() == null || ticks < 60) return;
                    worldRequested = true;
                    String name = "sb-saw-probe-" + System.nanoTime();
                    LevelSettings settings = new LevelSettings(name, GameType.SURVIVAL,
                            new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, true), true, WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(0L, false, false),
                            provider -> provider.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            mc.gui.screen());
                    return;
                }
                var server = mc.getSingleplayerServer();
                if (mc.level == null || mc.player == null || mc.gameMode == null || server == null || !server.isReady() || mc.gui.screen() != null
                        || ticks % 20 != 0) return;
                JsonObject req = GSON.fromJson(Files.readString(dir.resolve(REQUEST)), JsonObject.class);
                mc.getWindow().setWindowed(req.get("width").getAsInt(), req.get("height").getAsInt());
                mc.options.guiScale().set(req.get("guiScale").getAsInt());
                mc.resizeGui();
                ready = true;
                return;
            }
            Path cmd = dir.resolve(COMMAND);
            if (Files.isRegularFile(cmd)) {
                JsonObject c = GSON.fromJson(Files.readString(cmd), JsonObject.class);
                Files.delete(cmd);
                switch (c.get("op").getAsString()) {
                    case "open" -> open(mc);
                    case "style" -> UiStyleToggle.setEnabled(c.get("on").getAsBoolean());
                    case "close" -> mc.player.closeContainer();
                    case "guiScale" -> {
                        mc.options.guiScale().set(c.get("value").getAsInt());
                        mc.resizeGui();
                    }
                    case "shot" -> Screenshot.grab(mc, false);
                    case "quit" -> mc.stop();
                    default -> SimpleBlocks.LOG.warn("saw probe: unknown command {}", c);
                }
            }
            writeStatus(mc, dir);
        } catch (Exception e) {
            SimpleBlocks.LOG.warn("saw probe: {}", e.toString());
        }
    }

    /** On the server: a fresh hotbar and a Shapecutter two blocks east, opened by the block's use. */
    private static void open(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        java.util.UUID id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) return;
            player.closeContainer();
            Inventory inv = player.getInventory();
            inv.clearContent();
            inv.setItem(0, new ItemStack(Items.STONE_BRICKS, 16));
            inv.setItem(1, new ItemStack(Items.OAK_PLANKS, 16));
            // the longest list in Vanilla alone (16 cuts > 12 tiles): the mouse wheel has something to scroll
            inv.setItem(2, new ItemStack(Items.COBBLED_DEEPSLATE, 16));
            ServerLevel level = player.level();
            BlockPos at = player.blockPosition().relative(Direction.EAST, 2);
            for (BlockPos q : BlockPos.betweenClosed(at.offset(-1, 0, -1), at.offset(1, 2, 1))) level.setBlock(q, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(at.below(), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(at, Sawing.SAW.defaultBlockState().setValue(StonecutterBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
            level.getBlockState(at).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(at), Direction.WEST, at, false));
        });
    }

    private static void writeStatus(Minecraft mc, Path dir) throws Exception {
        JsonObject o = new JsonObject();
        var w = mc.getWindow();
        int scale = w.getGuiScale();
        var screen = mc.gui.screen();
        o.addProperty("tick", ticks);
        o.addProperty("ready", mc.level != null);
        o.addProperty("screen", screen == null ? "none" : screen.getClass().getSimpleName());
        o.addProperty("windowX", w.getX());
        o.addProperty("windowY", w.getY());
        o.addProperty("scale", scale);
        o.addProperty("style", UiStyleToggle.isEnabled());
        o.addProperty("octetRegistered", BuiltInRegistries.ITEM.containsKey(OCTET));
        JsonArray slots = new JsonArray();
        JsonArray cuts = new JsonArray();
        if (screen instanceof ShapecutterScreen<?> s && s.getMenu() instanceof ShapecutterMenu menu) {
            o.addProperty("menu", menu.getClass().getSimpleName());
            int left = field(s, net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class, "leftPos");
            int top = field(s, net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class, "topPos");
            for (int i = 0; i < menu.slots.size(); i++) {
                Slot slot = menu.slots.get(i);
                JsonObject e = new JsonObject();
                e.addProperty("i", i);
                e.addProperty("inv", slot.container instanceof Inventory);
                e.addProperty("cs", slot.getContainerSlot());
                e.addProperty("x", (left + slot.x + 8) * scale);
                e.addProperty("y", (top + slot.y + 8) * scale);
                describe(e, slot.getItem());
                slots.add(e);
            }
            JsonObject carried = new JsonObject();
            describe(carried, menu.getCarried());
            o.add("carried", carried);
            int start = field(s, ShapecutterScreen.class, "startIndex");
            o.addProperty("selected", menu.selectedIndex());
            o.addProperty("startIndex", start);
            // the grid of 4 x 3 tiles: its centre (for the wheel) and, per shown cut, the centre of its tile (ShapecutterScreen.cutAt)
            o.addProperty("listX", (left + ShapecutterScreen.GRID_X + ShapecutterScreen.COLUMNS * ShapecutterScreen.CELL_W / 2) * scale);
            o.addProperty("listY", (top + ShapecutterScreen.GRID_Y + ShapecutterScreen.ROWS * ShapecutterScreen.CELL_H / 2) * scale);
            List<Sawing.Entry> list = menu.hasInput() ? menu.cuts() : List.of();
            int shown = ShapecutterScreen.COLUMNS * ShapecutterScreen.ROWS;
            for (int i = 0; i < list.size(); i++) {
                JsonObject e = new JsonObject();
                e.addProperty("i", i);
                describe(e, list.get(i).resultStack());
                int pos = i - start;
                boolean visible = pos >= 0 && pos < shown;
                e.addProperty("visible", visible);
                if (visible) {
                    e.addProperty("x", (left + ShapecutterScreen.GRID_X + pos % ShapecutterScreen.COLUMNS * ShapecutterScreen.CELL_W + 8) * scale);
                    e.addProperty("y", (top + ShapecutterScreen.GRID_Y + pos / ShapecutterScreen.COLUMNS * ShapecutterScreen.CELL_H + 9) * scale);
                }
                cuts.add(e);
            }
        }
        o.add("slots", slots);
        o.add("cuts", cuts);
        var server = mc.getSingleplayerServer();
        if (server != null && mc.player != null) {
            java.util.UUID id = mc.player.getUUID();
            try {
                JsonObject inv = server.submit(() -> {
                    JsonObject a = new JsonObject();
                    ServerPlayer player = server.getPlayerList().getPlayer(id);
                    if (player == null) return a;
                    a.addProperty("menu", player.containerMenu.getClass().getSimpleName());
                    JsonObject items = new JsonObject();
                    Inventory pi = player.getInventory();
                    for (int i = 0; i < pi.getContainerSize(); i++) {
                        ItemStack stack = pi.getItem(i);
                        if (stack.isEmpty()) continue;
                        String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                        items.addProperty(key, (items.has(key) ? items.get(key).getAsInt() : 0) + stack.getCount());
                    }
                    a.add("items", items);
                    JsonObject held = new JsonObject();
                    describe(held, player.containerMenu.getCarried());
                    a.add("carried", held);
                    return a;
                }).get(2, TimeUnit.SECONDS);
                o.add("server", inv);
            } catch (Exception e) {
                o.addProperty("serverError", e.toString());
            }
        }
        Path tmp = dir.resolve("simpleblocks-saw-status.tmp");
        Files.writeString(tmp, GSON.toJson(o));
        Files.move(tmp, dir.resolve(STATUS), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }

    private static void describe(JsonObject e, ItemStack stack) {
        e.addProperty("item", stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        e.addProperty("count", stack.getCount());
    }

    private static int field(Object screen, Class<?> owner, String name) throws ReflectiveOperationException {
        java.lang.reflect.Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        return f.getInt(screen);
    }
}
