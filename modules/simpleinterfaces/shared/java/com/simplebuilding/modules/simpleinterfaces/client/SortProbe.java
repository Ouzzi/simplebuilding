package com.simplebuilding.modules.simpleinterfaces.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simplebuilding.modules.simpleinterfaces.sort.ContainerSort;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Probe for the sort key (WE25-3) with the REAL operating-system mouse (pattern of Simple Storage's PipeProbe): inert unless the game
 * folder holds {@code simpleinterfaces-sort-probe.json}. Creates a flat world, follows {@code simpleinterfaces-sort-command.json}
 * (chat command, expect, shot, quit) and writes {@code simpleinterfaces-sort-status.json} each tick: window, screen class, every slot
 * of an open container screen (menu index, container kind, item, count, centre in window pixels), the crosshair block and, after
 * "expect", the order {@link ContainerSort#sorted} gives for the main inventory. The driver
 * ({@code modules/simpleinterfaces/tools/sort_probe.py}) moves, clicks and types through XTest.
 */
public final class SortProbe {
    private static final Gson GSON = new Gson();
    private static final String REQUEST = "simpleinterfaces-sort-probe.json", STATUS = "simpleinterfaces-sort-status.json",
            COMMAND = "simpleinterfaces-sort-command.json";
    private static Boolean active;
    private static boolean worldRequested, ready;
    private static int ticks;
    private static List<String> expected = List.of();

    private SortProbe() {}

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
                    String name = "sb-sort-probe-" + System.nanoTime();
                    LevelSettings settings = new LevelSettings(name, GameType.CREATIVE,
                            new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, true), true, WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(0L, false, false),
                            provider -> provider.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            mc.gui.screen());
                    return;
                }
                var server = mc.getSingleplayerServer();
                if (mc.level == null || mc.player == null || mc.gameMode == null || server == null || !server.isReady() || mc.gui.screen() != null) return;
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
                    case "cmd" -> mc.player.connection.sendCommand(c.get("value").getAsString());
                    case "expect" -> {
                        List<ItemStack> in = new ArrayList<>();
                        for (int i = 9; i < 36; i++) in.add(mc.player.getInventory().getItem(i).copy());
                        expected = ContainerSort.sorted(in, 99).stream().map(SortProbe::describe).toList();
                    }
                    case "shot" -> Screenshot.grab(mc, false);
                    case "quit" -> mc.stop();
                    default -> org.slf4j.LoggerFactory.getLogger("simpleinterfaces").warn("sort probe: unknown command {}", c);
                }
            }
            writeStatus(mc, dir);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("simpleinterfaces").warn("sort probe: {}", e.toString());
        }
    }

    private static String describe(ItemStack s) {
        return s.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(s.getItem()) + " x" + s.getCount();
    }

    private static Object field(Object o, String name) {
        for (Class<?> k = o.getClass(); k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(o);
            } catch (ReflectiveOperationException ignored) {
                // next superclass
            }
        }
        return null;
    }

    private static void writeStatus(Minecraft mc, Path dir) throws java.io.IOException {
        JsonObject o = new JsonObject();
        var w = mc.getWindow();
        int scale = w.getGuiScale();
        var screen = mc.gui.screen();
        o.addProperty("tick", ticks);
        o.addProperty("screen", screen == null ? "none" : screen.getClass().getName());
        o.addProperty("windowX", w.getX());
        o.addProperty("windowY", w.getY());
        o.addProperty("scale", scale);
        o.addProperty("mouseX", mc.mouseHandler.xpos());
        o.addProperty("mouseY", mc.mouseHandler.ypos());
        o.addProperty("creative", mc.player.hasInfiniteMaterials());
        if (screen instanceof AbstractContainerScreen<?> cs) o.addProperty("carried", describe(cs.getMenu().getCarried()));
        if (screen instanceof CreativeModeInventoryScreen cr && field(cr, "leftPos") instanceof Integer l && field(cr, "topPos") instanceof Integer t) {
            // b-sortprobe27: which creative tab is open and where the Survival Inventory tab sits (centre, window pixels).
            o.addProperty("inventoryTab", cr.isInventoryOpen());
            CreativeModeTab inv = CreativeModeTabs.allTabs().stream().filter(tab -> tab.getType() == CreativeModeTab.Type.INVENTORY).findFirst().orElse(null);
            if (inv != null) {
                try {
                    Method tx = CreativeModeInventoryScreen.class.getDeclaredMethod("getTabX", CreativeModeTab.class);
                    Method ty = CreativeModeInventoryScreen.class.getDeclaredMethod("getTabY", CreativeModeTab.class);
                    tx.setAccessible(true);
                    ty.setAccessible(true);
                    o.addProperty("invTabX", (l + (int) tx.invoke(cr, inv) + 13) * scale);
                    o.addProperty("invTabY", (t + (int) ty.invoke(cr, inv) + 16) * scale);
                } catch (ReflectiveOperationException e) {
                    o.addProperty("invTabError", e.toString());
                }
            }
        }
        JsonArray slots = new JsonArray();
        if (screen instanceof AbstractContainerScreen<?> cs && field(cs, "leftPos") instanceof Integer l && field(cs, "topPos") instanceof Integer t) {
            for (var slot : cs.getMenu().slots) {
                JsonObject j = new JsonObject();
                j.addProperty("index", slot.index);
                j.addProperty("own", slot.container == mc.player.getInventory());
                j.addProperty("cslot", slot.getContainerSlot());
                j.addProperty("stack", describe(slot.getItem()));
                j.addProperty("x", (l + slot.x + 8) * scale);
                j.addProperty("y", (t + slot.y + 8) * scale);
                slots.add(j);
            }
        }
        o.add("slots", slots);
        JsonArray inv = new JsonArray();
        for (int i = 0; i < 36; i++) inv.add(describe(mc.player.getInventory().getItem(i)));
        o.add("inventory", inv);
        JsonArray exp = new JsonArray();
        expected.forEach(exp::add);
        o.add("expected", exp);
        if (mc.hitResult instanceof BlockHitResult h && h.getType() == HitResult.Type.BLOCK) {
            o.addProperty("hitBlock", BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(h.getBlockPos()).getBlock()).toString());
        }
        o.addProperty("pos", mc.player.blockPosition().toShortString());
        Path tmp = dir.resolve("simpleinterfaces-sort-status.tmp");
        Files.writeString(tmp, GSON.toJson(o));
        Files.move(tmp, dir.resolve(STATUS), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }
}
