package com.simplebuilding.modules.simpleearth.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.simplebuilding.modules.simpleearth.SimpleEarth;
import com.simplebuilding.modules.simpleearth.config.EarthClientConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/**
 * Menu probe for tests with the REAL operating-system mouse (WF25-2/WG26-3): inert unless the game folder holds
 * {@code simpleearth-menu-probe.json}. Then, on the title screen, it sizes the window, sets the GUI scale and opens the Simple
 * Earth menu, writes {@code simpleearth-probe-status.json} each tick (window position, control rectangles in window pixels,
 * current values) and follows commands from {@code simpleearth-probe-command.json} (GUI scale, scroll a row into view,
 * screenshot, quit). An external driver moves and clicks the X11 pointer (XTest) - the input then takes the player's path
 * (X server -> SDL -> MouseHandler -> screen). Fabric's client game tests block real input, so this runs in a normal client.
 */
public final class MenuProbe {
    /** What a menu screen reports to the probe (GUI coordinates, rectangles x, y, w, h). */
    public interface Target {
        int[] probeVersionButton(int version);

        int[] probeButton(boolean reset);

        /** The clickable control of a setting (slider track or toggle) after {@link #probeScrollTo}; null when not shown. */
        int[] probeControl(String key);

        void probeScrollTo(String key);

        int[] probeList();

        double probeScroll();
    }

    private static final Gson GSON = new Gson();
    private static Boolean active;
    private static boolean opened;
    private static int ticks;

    private MenuProbe() {}

    public static void tick(Minecraft mc) {
        Path dir = mc.gameDirectory.toPath();
        if (active == null) active = Files.isRegularFile(dir.resolve("simpleearth-menu-probe.json"));
        if (!active) return;
        ticks++;
        try {
            if (!opened) {
                // the title screen, or whatever the first start shows instead (accessibility onboarding ...), once loading is over
                if (mc.level != null || mc.gui.overlay() != null || mc.gui.screen() == null || ticks < 60) return;
                JsonObject req = GSON.fromJson(Files.readString(dir.resolve("simpleearth-menu-probe.json")), JsonObject.class);
                mc.getWindow().setWindowed(req.get("width").getAsInt(), req.get("height").getAsInt());
                mc.options.guiScale().set(req.get("guiScale").getAsInt());
                mc.resizeGui();
                mc.gui.setScreen(new EarthScreen());
                opened = true;
                SimpleEarth.LOG.info("Simple Earth menu probe: menu opened");
                return;
            }
            Path cmd = dir.resolve("simpleearth-probe-command.json");
            if (Files.isRegularFile(cmd)) {
                JsonObject c = GSON.fromJson(Files.readString(cmd), JsonObject.class);
                Files.delete(cmd);
                String op = c.get("op").getAsString();
                switch (op) {
                    case "guiScale" -> {
                        mc.options.guiScale().set(c.get("value").getAsInt());
                        mc.resizeGui();
                    }
                    case "scrollTo" -> {
                        if (mc.gui.screen() instanceof Target t) t.probeScrollTo(c.get("key").getAsString());
                    }
                    case "set" -> EarthClientConfig.setRender(c.get("key").getAsString(), c.get("value").getAsDouble());
                    case "version" -> {
                        EarthClientConfig.version = c.get("value").getAsInt();
                        if (mc.gui.screen() instanceof EarthScreen) mc.gui.setScreen(new EarthScreen());
                    }
                    case "shot" -> Screenshot.grab(mc, false);
                    case "quit" -> mc.stop();
                    default -> SimpleEarth.LOG.warn("menu probe: unknown command {}", op);
                }
            }
            writeStatus(mc, dir);
        } catch (Exception e) {
            SimpleEarth.LOG.warn("Simple Earth menu probe: {}", e.toString());
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
        o.addProperty("width", w.getScreenWidth());
        o.addProperty("height", w.getScreenHeight());
        o.addProperty("scale", scale);
        o.addProperty("version", EarthClientConfig.version);
        o.addProperty("mouseX", mc.mouseHandler.xpos());
        o.addProperty("mouseY", mc.mouseHandler.ypos());
        JsonObject values = new JsonObject();
        for (String k : new String[]{"detail", "faceEdge", "moonPhases", "paintBudget"}) values.addProperty(k, EarthClientConfig.render(k));
        values.addProperty("tipTime", EarthClientConfig.get(10, "tipTime"));
        o.add("values", values);
        if (mc.gui.screen() instanceof Target t) {
            JsonObject r = new JsonObject();
            r.add("v10", rect(t.probeVersionButton(10), scale));
            r.add("v11", rect(t.probeVersionButton(11), scale));
            r.add("reset", rect(t.probeButton(true), scale));
            r.add("done", rect(t.probeButton(false), scale));
            r.add("list", rect(t.probeList(), scale));
            for (String k : new String[]{"detail", "faceEdge", "moonPhases", "tipTime"}) {
                int[] c = t.probeControl(k);
                if (c != null) r.add(k, rect(c, scale));
            }
            o.add("rects", r);
            o.addProperty("scroll", t.probeScroll());
        }
        Path tmp = dir.resolve("simpleearth-probe-status.tmp");
        Files.writeString(tmp, GSON.toJson(o));
        Files.move(tmp, dir.resolve("simpleearth-probe-status.json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }

    private static com.google.gson.JsonArray rect(int[] r, int scale) {
        com.google.gson.JsonArray a = new com.google.gson.JsonArray();
        if (r != null) for (int v : r) a.add(v * scale);
        return a;
    }
}
