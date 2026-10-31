package com.simplebuilding.modules.simpleinterfaces.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;

/**
 * Probe for tests of the key binding page with the REAL operating-system mouse and keyboard (IG31-31, pattern of Simple Earth's
 * MenuProbe): inert unless the game folder holds {@code simpleinterfaces-keys-probe.json}. Then, on the title screen, it sizes
 * the window, sets the GUI scale and opens the page, writes {@code simpleinterfaces-keys-status.json} each tick (window position,
 * widget rectangles in window pixels, the page's state, the key of the probed action) and follows commands from
 * {@code simpleinterfaces-keys-command.json}. The driver ({@code modules/simpleinterfaces/tools/keys_probe.py}) moves and clicks
 * the X11 pointer and presses keys through XTest - the input takes the player's path (X server -> SDL -> game -> screen).
 */
public final class KeysProbe {
    private static final Gson GSON = new Gson();
    private static final String REQUEST = "simpleinterfaces-keys-probe.json", STATUS = "simpleinterfaces-keys-status.json",
            COMMAND = "simpleinterfaces-keys-command.json";
    private static Boolean active;
    private static boolean opened;
    private static int ticks;
    /** The action whose key and reset buttons the status reports, and the keyboard cell it reports. */
    private static String row = "key.simpleinterfaces.open_keys";
    private static int cell = 71;

    private KeysProbe() {}

    public static void tick(Minecraft mc) {
        Path dir = mc.gameDirectory.toPath();
        if (active == null) active = Files.isRegularFile(dir.resolve(REQUEST));
        if (!active) return;
        ticks++;
        try {
            if (!opened) {
                if (mc.level != null || mc.gui.overlay() != null || mc.gui.screen() == null || ticks < 60) return;
                JsonObject req = GSON.fromJson(Files.readString(dir.resolve(REQUEST)), JsonObject.class);
                mc.getWindow().setWindowed(req.get("width").getAsInt(), req.get("height").getAsInt());
                mc.options.guiScale().set(req.get("guiScale").getAsInt());
                mc.resizeGui();
                KeysScreen.preset(KeysScreen.View.MOD, com.simplelib.api.client.keys.KeyboardLayout.Size.FULL);
                mc.gui.setScreen(new KeysScreen(mc.gui.screen()));
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
                    case "row" -> row = c.get("value").getAsString();
                    case "cell" -> cell = c.get("value").getAsInt();
                    case "show" -> {
                        if (mc.gui.screen() instanceof KeysScreen k) k.probeShow(row);
                    }
                    case "open" -> mc.gui.setScreen(new KeysScreen(null));
                    case "controls" -> mc.gui.setScreen(new KeyBindsScreen(null, mc.options));
                    case "shot" -> Screenshot.grab(mc, false);
                    case "quit" -> mc.stop();
                    default -> org.slf4j.LoggerFactory.getLogger("simpleinterfaces").warn("keys probe: unknown command {}", c);
                }
            }
            writeStatus(mc, dir);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("simpleinterfaces").warn("keys probe: {}", e.toString());
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
        KeyMapping probed = KeyMapping.get(row);
        o.addProperty("row", row);
        o.addProperty("rowKey", probed == null ? "" : probed.saveString());
        JsonObject rects = new JsonObject();
        JsonObject values = new JsonObject();
        if (mc.gui.screen() instanceof KeysScreen k) {
            k.probeRects(row, cell).forEach((name, r) -> rects.add(name, rect(r, scale)));
            k.probeValues().forEach((name, v) -> values.addProperty(name, String.valueOf(v)));
        } else if (mc.gui.screen() instanceof KeyCellScreen s) {
            s.probeRects().forEach((name, r) -> rects.add(name, rect(r, scale)));
            values.addProperty("code", String.valueOf(s.code()));
        } else if (mc.gui.screen() instanceof KeyBindsScreen s) {
            // the button the mixin adds to Vanilla's controls page
            for (GuiEventListener child : s.children()) {
                if (child instanceof AbstractWidget wd && wd.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("simpleinterfaces.keys.open").getString())) {
                    rects.add("simple_keys", rect(KeysScreen.rect(wd), scale));
                }
            }
        }
        o.add("rects", rects);
        o.add("values", values);
        Path tmp = dir.resolve("simpleinterfaces-keys-status.tmp");
        Files.writeString(tmp, GSON.toJson(o));
        Files.move(tmp, dir.resolve(STATUS), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }

    private static JsonArray rect(int[] r, int scale) {
        JsonArray a = new JsonArray();
        if (r != null) for (int v : r) a.add(v * scale);
        return a;
    }
}
