package com.simplearrows.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Probe for the Fletching Table's potion slot (WE25-9) with the REAL operating-system mouse (pattern of Simple Storage's PipeProbe):
 * inert unless the game folder holds {@code simplearrows-fletching-probe.json}. Then, on the title screen, it creates a flat
 * survival world, gives flint, stick, feather and a potion of swiftness into the hotbar, places a Vanilla fletching table and
 * uses it on the server (Simple Arrows' table menu opens through the normal path), writes {@code simplearrows-fletching-status.json}
 * each tick (window position, every menu slot's centre in window pixels with its item) and follows commands from
 * {@code simplearrows-fletching-command.json} ({@code guiScale}, {@code reset}, {@code shot}, {@code quit}). The driver
 * ({@code modules/simplearrows/tools/fletching_probe.py}) moves and clicks the X11 pointer through XTest. No SimpleBuilding class
 * is imported: the module stays standalone, the probe only reads the open container screen.
 */
public final class FletchingProbe {
    private static final Gson GSON = new Gson();
    private static final String REQUEST = "simplearrows-fletching-probe.json", STATUS = "simplearrows-fletching-status.json",
            COMMAND = "simplearrows-fletching-command.json";
    private static Boolean active;
    private static boolean opened, worldRequested;
    private static int ticks;

    private FletchingProbe() {}

    public static void tick(Minecraft mc) {
        Path dir = mc.gameDirectory.toPath();
        if (active == null) active = Files.isRegularFile(dir.resolve(REQUEST));
        if (!active) return;
        ticks++;
        try {
            if (!opened) {
                if (!worldRequested) {
                    if (mc.level != null || mc.gui.overlay() != null || mc.gui.screen() == null || ticks < 60) return;
                    worldRequested = true;
                    String name = "sb-fletching-probe-" + System.nanoTime();
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
                reset(mc);
                opened = true;
                return;
            }
            Path cmd = dir.resolve(COMMAND);
            if (Files.isRegularFile(cmd)) {
                JsonObject c = GSON.fromJson(Files.readString(cmd), JsonObject.class);
                Files.delete(cmd);
                switch (c.get("op").getAsString()) {
                    case "guiScale" -> {
                        mc.options.guiScale().set(c.get("value").getAsInt());
                        mc.resizeGui();
                    }
                    case "reset" -> reset(mc);
                    case "shot" -> Screenshot.grab(mc, false);
                    case "quit" -> mc.stop();
                    default -> org.slf4j.LoggerFactory.getLogger("simplearrows").warn("fletching probe: unknown command {}", c);
                }
            }
            writeStatus(mc, dir);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("simplearrows").warn("fletching probe: {}", e.toString());
        }
    }

    /** On the server: fresh hotbar (flint, stick, feather, potion of swiftness), a fletching table next to the player, used. */
    private static void reset(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        java.util.UUID id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) return;
            player.closeContainer();
            player.getInventory().clearContent();
            player.getInventory().setItem(0, new ItemStack(Items.FLINT, 8));
            player.getInventory().setItem(1, new ItemStack(Items.STICK, 8));
            player.getInventory().setItem(2, new ItemStack(Items.FEATHER, 8));
            player.getInventory().setItem(3, PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS));
            BlockPos table = player.blockPosition().relative(Direction.EAST, 2);
            player.level().setBlockAndUpdate(table, Blocks.FLETCHING_TABLE.defaultBlockState());
            player.level().getBlockState(table).useWithoutItem(player.level(), player,
                    new BlockHitResult(Vec3.atCenterOf(table), Direction.WEST, table, false));
        });
    }

    private static void writeStatus(Minecraft mc, Path dir) throws Exception {
        JsonObject o = new JsonObject();
        var w = mc.getWindow();
        int scale = w.getGuiScale();
        o.addProperty("tick", ticks);
        o.addProperty("screen", mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getSimpleName());
        o.addProperty("windowX", w.getX());
        o.addProperty("windowY", w.getY());
        o.addProperty("scale", scale);
        o.addProperty("mouseX", mc.mouseHandler.xpos());
        o.addProperty("mouseY", mc.mouseHandler.ypos());
        JsonArray slots = new JsonArray();
        if (mc.gui.screen() instanceof AbstractContainerScreen<?> s) {
            o.addProperty("menu", s.getMenu().getClass().getSimpleName());
            int left = field(s, "leftPos"), top = field(s, "topPos");
            for (int i = 0; i < s.getMenu().slots.size(); i++) {
                Slot slot = s.getMenu().slots.get(i);
                JsonObject e = new JsonObject();
                e.addProperty("i", i);
                e.addProperty("x", (left + slot.x + 8) * scale);
                e.addProperty("y", (top + slot.y + 8) * scale);
                describe(e, slot.getItem());
                slots.add(e);
            }
            JsonObject carried = new JsonObject();
            describe(carried, s.getMenu().getCarried());
            o.add("carried", carried);
        }
        o.add("slots", slots);
        Path tmp = dir.resolve("simplearrows-fletching-status.tmp");
        Files.writeString(tmp, GSON.toJson(o));
        Files.move(tmp, dir.resolve(STATUS), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }

    private static void describe(JsonObject e, ItemStack stack) {
        e.addProperty("item", stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        e.addProperty("count", stack.getCount());
        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        e.addProperty("potion", potion != null && potion.hasEffects());
        Float duration = stack.get(DataComponents.POTION_DURATION_SCALE);
        e.addProperty("durationScale", duration == null ? 1.0F : duration);
    }

    private static int field(AbstractContainerScreen<?> screen, String name) throws ReflectiveOperationException {
        java.lang.reflect.Field f = AbstractContainerScreen.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.getInt(screen);
    }
}
