package com.simplestorage.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simplestorage.pipe.ItemPipeBlockEntity;
import com.simplestorage.pipe.PipeMenu;
import com.simplestorage.pipe.PipeTier;
import com.simplestorage.pipe.Pipes;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Probe for the pipe window with the REAL operating-system mouse (ROHR33, pattern of Simple Keys' KeysProbe): inert unless the game
 * folder holds {@code simplestorage-pipe-probe.json}. Then, on the title screen, it creates a world, then sizes the window, sets the GUI scale and opens
 * the pipe window (the container screen needs a player: the probe first opens a flat singleplayer world) on a menu without a server block (the side keys answer locally with the same cycle the server uses), writes
 * {@code simplestorage-pipe-status.json} each tick (window position, key rectangles in window pixels, the six states, the tooltip lines
 * of the side under the pointer) and follows commands from {@code simplestorage-pipe-command.json}. The driver
 * ({@code modules/simplestorage/tools/pipe_probe.py}) moves and clicks the X11 pointer through XTest.
 */
public final class PipeProbe {
    private static final Gson GSON = new Gson();
    private static final String REQUEST = "simplestorage-pipe-probe.json", STATUS = "simplestorage-pipe-status.json", COMMAND = "simplestorage-pipe-command.json";
    private static Boolean active;
    private static boolean opened, worldRequested;
    private static int ticks;
    private static PipeTier tier = PipeTier.REINFORCED;

    private PipeProbe() {}

    /** The window on a menu without a server: a click on a side key cycles locally like {@link ItemPipeBlockEntity#cycleSide}. */
    private static final class ProbeScreen extends PipeScreen {
        private final PipeMenu probeMenu;

        ProbeScreen(PipeMenu menu, Inventory inventory) {
            super(menu, inventory, Component.translatable("block.simplestorage.reinforced_pipe"));
            this.probeMenu = menu;
        }

        @Override
        protected void press(int id) {
            if (id < PipeMenu.BUTTON_SIDE || id >= PipeMenu.BUTTON_SIDE + 6) return;
            Direction side = Direction.values()[id - PipeMenu.BUTTON_SIDE];
            int[] modes = Pipes.unpack(this.probeMenu.sideModesPacked());
            modes[side.ordinal()] = Pipes.nextMode(this.probeMenu.tier(), side, modes[side.ordinal()]);
            this.probeMenu.probeData(ItemPipeBlockEntity.SIDES, Pipes.pack(modes));
        }
    }

    private static ProbeScreen open(Minecraft mc, int sides) {
        Inventory inventory = mc.player.getInventory();
        PipeMenu menu = new PipeMenu(0, inventory);
        menu.probeData(ItemPipeBlockEntity.TIER, tier.ordinal());
        menu.probeData(ItemPipeBlockEntity.SIDES, sides);
        ProbeScreen screen = new ProbeScreen(menu, inventory);
        mc.gui.setScreen(screen);
        return screen;
    }

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
                    String name = "sb-pipe-probe-" + System.nanoTime();
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
                open(mc, Pipes.pack(Pipes.fromSingleOutput(Direction.DOWN)));
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
                    case "tier" -> tier = PipeTier.valueOf(c.get("value").getAsString());
                    case "open" -> open(mc, c.has("sides") ? c.get("sides").getAsInt() : Pipes.pack(Pipes.fromSingleOutput(Direction.DOWN)));
                    case "shot" -> Screenshot.grab(mc, false);
                    case "quit" -> mc.stop();
                    default -> org.slf4j.LoggerFactory.getLogger("simplestorage").warn("pipe probe: unknown command {}", c);
                }
            }
            writeStatus(mc, dir);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("simplestorage").warn("pipe probe: {}", e.toString());
        }
    }

    private static void writeStatus(Minecraft mc, Path dir) throws java.io.IOException {
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
        JsonObject rects = new JsonObject();
        JsonObject values = new JsonObject();
        if (mc.gui.screen() instanceof PipeScreen s) {
            for (var child : s.children()) {
                if (child instanceof PipeScreen.SideButton b) {
                    JsonArray a = new JsonArray();
                    for (int v : new int[] {b.getX(), b.getY(), b.getWidth(), b.getHeight()}) a.add(v * scale);
                    rects.add(b.side().getName(), a);
                    values.addProperty(b.side().getName(), s.probeMode(b.side()));
                }
            }
            JsonArray box = new JsonArray();
            for (int v : PipeScreen.boxInside()) box.add(v * scale);
            rects.add("box", box);   // relative to the window like the keys (x, y, w, h)
            JsonArray tip = new JsonArray();
            for (Component line : s.probeTooltip()) tip.add(line.getString());
            values.add("tooltip", tip);
        }
        o.add("rects", rects);
        o.add("values", values);
        Path tmp = dir.resolve("simplestorage-pipe-status.tmp");
        Files.writeString(tmp, GSON.toJson(o));
        Files.move(tmp, dir.resolve(STATUS), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }
}
