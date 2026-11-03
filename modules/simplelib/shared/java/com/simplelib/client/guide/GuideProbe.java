package com.simplelib.client.guide;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simplelib.SimpleLib;
import com.simplelib.api.ModpackGuide;
import com.simplelib.api.client.ModpackGuideBar;
import com.simplelib.api.client.ModpackGuideScreens;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Probe for the Simple Modpack guide with the REAL operating-system mouse (b-guide01, after Simple Earth's MenuProbe):
 * inert unless the game folder holds {@code simplelib-guide-probe.json}. Then, on the title screen, it sizes the window,
 * sets the GUI scale, marks every second chapter as unlocked and opens the guide; each tick it writes
 * {@code simplelib-guide-status.json} (screen, section, chapter, bookmark rectangles in window pixels) and follows
 * {@code simplelib-guide-command.json} (guiScale, open, shot, quit). {@code modules/simplelib/tools/guide_probe.py} moves
 * and clicks the X11 pointer (XTest), so the clicks take the player's path X server -> SDL -> MouseHandler -> screen.
 */
public final class GuideProbe {
    /** What SimpleLib's guide screen reports (GUI pixels). */
    public interface Target {
        @Nullable String probeSection();

        int probeChapter();

        int probeSpread();

        List<int[]> probeTabs();

        List<Integer> probeTabChapters();

        int[] probeNext();
    }

    private static final Gson GSON = new Gson();
    private static Boolean active;
    private static boolean opened;
    private static int ticks;

    private GuideProbe() {}

    public static void tick(Minecraft mc) {
        Path dir = mc.gameDirectory.toPath();
        if (active == null) active = Files.isRegularFile(dir.resolve("simplelib-guide-probe.json"));
        if (!active) return;
        ticks++;
        try {
            if (!opened) {
                if (mc.level != null || mc.gui.overlay() != null || mc.gui.screen() == null || ticks < 60) return;
                JsonObject req = GSON.fromJson(Files.readString(dir.resolve("simplelib-guide-probe.json")), JsonObject.class);
                mc.getWindow().setWindowed(req.get("width").getAsInt(), req.get("height").getAsInt());
                mc.options.guiScale().set(req.get("guiScale").getAsInt());
                mc.resizeGui();
                unlockEverySecond();
                ModpackGuideScreens.open(req.has("section") ? req.get("section").getAsString() : null, null);
                opened = true;
                SimpleLib.LOG.info("Simple Modpack guide probe: book opened");
                return;
            }
            Path cmd = dir.resolve("simplelib-guide-command.json");
            if (Files.isRegularFile(cmd)) {
                JsonObject c = GSON.fromJson(Files.readString(cmd), JsonObject.class);
                Files.delete(cmd);
                switch (c.get("op").getAsString()) {
                    case "guiScale" -> {
                        mc.options.guiScale().set(c.get("value").getAsInt());
                        mc.resizeGui();
                    }
                    case "open" -> ModpackGuideScreens.open(c.has("section") ? c.get("section").getAsString() : null, null);
                    case "shot" -> Screenshot.grab(mc, false);
                    case "quit" -> mc.stop();
                    default -> SimpleLib.LOG.warn("guide probe: unknown command {}", c);
                }
            }
            writeStatus(mc, dir);
        } catch (Exception e) {
            SimpleLib.LOG.warn("Simple Modpack guide probe: {}", e.toString());
        }
    }

    /** Every second chapter of every section opens, so the shots show open and locked bookmarks side by side. */
    private static void unlockEverySecond() {
        Set<Identifier> open = new HashSet<>();
        for (ModpackGuide.Section section : ModpackGuideScreens.sections()) {
            for (int i = 0; i < section.chapters().size(); i++) {
                Identifier advancement = section.chapters().get(i).advancement();
                if (advancement != null && i % 2 == 0) open.add(advancement);
            }
        }
        ModpackGuideScreens.override(open);
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
        JsonArray top = new JsonArray();
        List<String> ids = ModpackGuideBar.ids();
        List<int[]> rects = ModpackGuideBar.rects();
        for (int i = 0; i < ids.size() && i < rects.size(); i++) {
            JsonObject t = new JsonObject();
            t.addProperty("id", ids.get(i));
            t.add("rect", rect(rects.get(i), scale));
            top.add(t);
        }
        o.add("top", top);
        int[] link = ModpackGuideBar.sideLink();
        if (link != null && mc.gui.screen() != null && !(mc.gui.screen() instanceof Target)) o.add("more", rect(link, scale));
        if (mc.gui.screen() instanceof Target t) {
            o.addProperty("section", t.probeSection());
            o.addProperty("chapter", t.probeChapter());
            o.addProperty("spread", t.probeSpread());
            JsonArray tabs = new JsonArray();
            List<int[]> tabRects = t.probeTabs();
            List<Integer> chapters = t.probeTabChapters();
            for (int i = 0; i < tabRects.size(); i++) {
                JsonObject tab = new JsonObject();
                tab.addProperty("chapter", chapters.get(i));
                tab.add("rect", rect(tabRects.get(i), scale));
                tabs.add(tab);
            }
            o.add("tabs", tabs);
            o.add("next", rect(t.probeNext(), scale));
        }
        Path tmp = dir.resolve("simplelib-guide-status.tmp");
        Files.writeString(tmp, GSON.toJson(o));
        Files.move(tmp, dir.resolve("simplelib-guide-status.json"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static JsonArray rect(int[] r, int scale) {
        JsonArray a = new JsonArray();
        for (int v : r) a.add(v * scale);
        return a;
    }
}
