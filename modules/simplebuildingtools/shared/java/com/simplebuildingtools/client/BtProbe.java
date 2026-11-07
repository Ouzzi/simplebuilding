package com.simplebuildingtools.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Probe for the tool screens with the REAL operating-system mouse and keyboard (b-mouse10, pattern of Simple Storage's PipeProbe):
 * inert unless the game folder holds {@code simplebuildingtools-probe.json}. Then it creates a flat creative world, sizes the window
 * and follows commands from {@code simplebuildingtools-probe-command.json} (chat command, shot, gui scale, quit). Each tick it writes
 * {@code simplebuildingtools-probe-status.json}: window position, screen class, every widget of the screen with its rectangle in
 * window pixels, the editor code, the slots of a container screen, the crosshair block and the SERVER side of the held stack and
 * of the block entity at the probed position. The driver ({@code modules/simplebuildingtools/tools/mouse_probe.py}) clicks and
 * types through XTest.
 */
public final class BtProbe {
    private static final Gson GSON = new Gson();
    private static final String REQUEST = "simplebuildingtools-probe.json", STATUS = "simplebuildingtools-probe-status.json",
            COMMAND = "simplebuildingtools-probe-command.json";
    private static Boolean active;
    private static boolean worldRequested, ready;
    private static int ticks;
    private static BlockPos watch;

    private BtProbe() {}

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
                    String name = "sb-bt-probe-" + System.nanoTime();
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
                    case "guiScale" -> {
                        mc.options.guiScale().set(c.get("value").getAsInt());
                        mc.resizeGui();
                    }
                    case "cmd" -> mc.player.connection.sendCommand(c.get("value").getAsString());
                    case "watch" -> watch = new BlockPos(c.get("x").getAsInt(), c.get("y").getAsInt(), c.get("z").getAsInt());
                    case "find" -> {
                        var server = mc.getSingleplayerServer();
                        var self = mc.player.blockPosition();
                        watch = server.submit(() -> {
                            var level = server.getPlayerList().getPlayers().getFirst().level();
                            for (BlockPos q : BlockPos.betweenClosed(self.offset(-6, -3, -6), self.offset(6, 3, 6))) {
                                if (BuiltInRegistries.BLOCK.getKey(level.getBlockState(q).getBlock()).getPath().equals("placed_blueprint")) return q.immutable();
                            }
                            return null;
                        }).get(2, TimeUnit.SECONDS);
                    }
                    case "shot" -> Screenshot.grab(mc, false);
                    case "quit" -> mc.stop();
                    default -> org.slf4j.LoggerFactory.getLogger("simplebuildingtools").warn("bt probe: unknown command {}", c);
                }
            }
            writeStatus(mc, dir);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("simplebuildingtools").warn("bt probe: {}", e.toString());
        }
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
        o.addProperty("tick", ticks);
        var screen = mc.gui.screen();
        o.addProperty("screen", screen == null ? "none" : screen.getClass().getName());
        o.addProperty("windowX", w.getX());
        o.addProperty("windowY", w.getY());
        o.addProperty("scale", scale);
        o.addProperty("mouseX", mc.mouseHandler.xpos());
        o.addProperty("mouseY", mc.mouseHandler.ypos());
        JsonArray widgets = new JsonArray();
        if (screen != null) {
            for (var child : screen.children()) {
                if (!(child instanceof AbstractWidget a)) continue;
                JsonObject j = new JsonObject();
                j.addProperty("class", a.getClass().getSimpleName());
                j.addProperty("label", a.getMessage().getString());
                j.addProperty("x", a.getX() * scale);
                j.addProperty("y", a.getY() * scale);
                j.addProperty("w", a.getWidth() * scale);
                j.addProperty("h", a.getHeight() * scale);
                j.addProperty("active", a.active);
                j.addProperty("visible", a.visible);
                if (a instanceof EditBox e) j.addProperty("value", e.getValue());
                widgets.add(j);
            }
            Object code = field(screen, "code");
            if (code instanceof String s) o.addProperty("code", s);
        }
        o.add("widgets", widgets);
        JsonArray slots = new JsonArray();
        if (screen instanceof AbstractContainerScreen<?> cs) {
            Object lx = field(cs, "leftPos"), ty = field(cs, "topPos");
            if (lx instanceof Integer l && ty instanceof Integer t) {
                for (var slot : cs.getMenu().slots) {
                    if (slot.getItem().isEmpty()) continue;
                    JsonObject j = new JsonObject();
                    j.addProperty("item", BuiltInRegistries.ITEM.getKey(slot.getItem().getItem()).toString());
                    j.addProperty("x", (l + slot.x + 8) * scale);
                    j.addProperty("y", (t + slot.y + 8) * scale);
                    slots.add(j);
                }
            }
        }
        o.add("slots", slots);
        if (mc.hitResult instanceof BlockHitResult h && h.getType() == HitResult.Type.BLOCK) {
            o.addProperty("hitPos", h.getBlockPos().toShortString());
            o.addProperty("hitBlock", BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(h.getBlockPos()).getBlock()).toString());
        }
        o.addProperty("pos", mc.player.blockPosition().toShortString());
        var server = mc.getSingleplayerServer();
        if (server != null) {
            try {
                String[] r = server.submit(() -> {
                    var p = server.getPlayerList().getPlayers().getFirst();
                    String held = p.getMainHandItem().isEmpty() ? "empty" : p.getMainHandItem().toString() + " " + p.getMainHandItem().getComponents();
                    String be = "-";
                    if (watch != null) {
                        var entity = p.level().getBlockEntity(watch);
                        be = entity == null ? "no block entity, block " + p.level().getBlockState(watch).getBlock()
                                : entity.saveWithoutMetadata(p.level().registryAccess()).toString();
                    }
                    return new String[] {held, be};
                }).get(2, TimeUnit.SECONDS);
                o.addProperty("serverHeld", r[0]);
                o.addProperty("serverBe", r[1]);
                if (watch != null) o.addProperty("watchPos", watch.toShortString());
            } catch (Exception e) {
                o.addProperty("serverError", e.toString());
            }
        }
        Path tmp = dir.resolve("simplebuildingtools-probe-status.tmp");
        Files.writeString(tmp, GSON.toJson(o));
        Files.move(tmp, dir.resolve(STATUS), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }
}
