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
                    // O8 audit 1 (two clients on one dedicated server): "server": "host:port" joins that server instead of a new world
                    JsonObject join = GSON.fromJson(Files.readString(dir.resolve(REQUEST)), JsonObject.class);
                    if (join.has("server")) {
                        String address = join.get("server").getAsString();
                        net.minecraft.client.gui.screens.ConnectScreen.startConnecting(mc.gui.screen(), mc,
                                net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(address),
                                new net.minecraft.client.multiplayer.ServerData("sb-probe", address, net.minecraft.client.multiplayer.ServerData.Type.OTHER),
                                false, null);
                        return;
                    }
                    String name = "sb-sort-probe-" + System.nanoTime();
                    LevelSettings settings = new LevelSettings(name, GameType.CREATIVE,
                            new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, true), true, WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(0L, false, false),
                            provider -> provider.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            mc.gui.screen());
                    return;
                }
                var server = mc.getSingleplayerServer();
                JsonObject req = GSON.fromJson(Files.readString(dir.resolve(REQUEST)), JsonObject.class);
                boolean remote = req.has("server");
                if (mc.level == null || mc.player == null || mc.gameMode == null || !remote && (server == null || !server.isReady())
                        || mc.gui.screen() != null) return;
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
                    // N79: pick a dev variant (e.g. the spyglass frame width) without the variants panel
                    case "variant" -> com.simplelib.api.DevVariants.select(c.get("id").getAsString(), c.get("value").getAsString());
                    // a-w4c: opens SimpleLib's generic config screen of a mod directly (by reflection, the class lives in the Fabric part)
                    case "config" -> {
                        var create = Class.forName("com.simplelib.config.screen.GenericConfigScreen").getMethod("create", String.class, net.minecraft.client.gui.screens.Screen.class);
                        mc.gui.setScreen((net.minecraft.client.gui.screens.Screen) create.invoke(null, c.get("value").getAsString(), mc.gui.screen()));
                    }
                    // b-earth82: opens a mod screen by class name (public no-argument constructor) when its key is out of the
                    // driver's reach (Simple Earth's "#" = SDL NONUSHASH has no Xvfb key)
                    case "screen" -> mc.gui.setScreen((net.minecraft.client.gui.screens.Screen) Class.forName(c.get("value").getAsString())
                            .getConstructor().newInstance());
                    // N118: switch the game language (config screen texts EN/DE) and reload the resources; the driver waits for the reload
                    case "lang" -> {
                        String code = c.get("value").getAsString();
                        mc.options.languageCode = code;
                        mc.getLanguageManager().setSelected(code);
                        mc.options.save();
                        mc.reloadResourcePacks();
                    }
                    // N118: a fixed free-look camera offset (degrees) while the driver holds the real free-look key, so the crosshair
                    // mark has a known place without relying on the grabbed pointer reaching the game
                    case "freelook" -> com.simplelib.api.client.FreeLook.set(c.get("yaw").getAsFloat(), c.get("pitch").getAsFloat(),
                            c.get("held").getAsBoolean());
                    // N124: GUI scale during a run (key page at GUI 3), the same steps as the start request
                    case "guiscale" -> {
                        mc.options.guiScale().set(c.get("value").getAsInt());
                        mc.resizeGui();
                    }
                    // N124: one local option of Simple Interfaces in memory only (e.g. zoomHotkey), not saved to the file
                    case "siclient" -> {
                        var cfg = ContainersClient.config();
                        Field f = cfg.getClass().getField(c.get("field").getAsString());
                        if (f.getType() == boolean.class) f.setBoolean(cfg, c.get("value").getAsBoolean());
                        else if (f.getType() == int.class) f.setInt(cfg, c.get("value").getAsInt());
                    }
                    case "quit" -> mc.stop();
                    default -> org.slf4j.LoggerFactory.getLogger("simpleinterfaces").warn("sort probe: unknown command {}", c);
                }
            }
            writeStatus(mc, dir);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("simpleinterfaces").warn("sort probe: {}", e.toString());
        }
    }

    /**
     * b-probe5: every displayed creative tab (registry id, centre in window pixels, page when the loader pages the tabs), the open
     * page and the selected tab, so a driver can click a tab with the real mouse. Fabric's page methods are read by name; without
     * them the page fields are missing.
     */
    private static void creativeTabs(CreativeModeInventoryScreen cr, int l, int t, int scale, JsonObject o) {
        JsonArray tabs = new JsonArray();
        try {
            Method tx = CreativeModeInventoryScreen.class.getDeclaredMethod("getTabX", CreativeModeTab.class);
            Method ty = CreativeModeInventoryScreen.class.getDeclaredMethod("getTabY", CreativeModeTab.class);
            tx.setAccessible(true);
            ty.setAccessible(true);
            Method page = null;
            try {
                page = cr.getClass().getMethod("getPage", CreativeModeTab.class);
            } catch (NoSuchMethodException ignored) {
                // no paging loader
            }
            for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
                if (!tab.shouldDisplay()) continue;
                JsonObject j = new JsonObject();
                j.addProperty("id", String.valueOf(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab)));
                j.addProperty("x", (l + (int) tx.invoke(cr, tab) + 13) * scale);
                j.addProperty("y", (t + (int) ty.invoke(cr, tab) + 16) * scale);
                if (page != null && page.invoke(cr, tab) instanceof Integer p) j.addProperty("page", p);
                tabs.add(j);
            }
            if (field(cr, "selectedTab") instanceof CreativeModeTab sel) o.addProperty("selectedTab", String.valueOf(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(sel)));
            try {
                if (cr.getClass().getMethod("getCurrentPage").invoke(cr) instanceof Integer p) o.addProperty("tabPage", p);
            } catch (NoSuchMethodException ignored) {
                // no paging loader
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            o.addProperty("tabsError", e.toString());
        }
        o.add("tabs", tabs);
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

    /** Simple class name of a widget; anonymous subclasses and Vanilla's {@code Button.Plain} report the first named class above them ("EditBox", "Button"). */
    private static String widgetClass(Class<?> c) {
        for (; c != null; c = c.getSuperclass()) {
            String n = c.getSimpleName();
            if (!n.isEmpty() && !n.equals("Plain")) return n;
        }
        return "AbstractWidget";
    }

    private static void collectWidgets(List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children, int scale, JsonArray out,
                                       JsonArray entries, int depth) {
        if (depth > 4) return;
        for (var child : children) {
            // b-probe5: list entries that carry an option name (Cloth Config's AbstractConfigEntry#getFieldName), by reflection, so a
            // driver can check that a config screen shows lang texts and no raw option paths.
            if (!(child instanceof net.minecraft.client.gui.components.AbstractWidget)) {
                try {
                    // the drawn name (AbstractConfigListEntry#getDisplayedFieldName); getFieldName can be an internal id (UUID)
                    Object shown;
                    try {
                        shown = child.getClass().getMethod("getDisplayedFieldName").invoke(child);
                    } catch (NoSuchMethodException e) {
                        shown = child.getClass().getMethod("getFieldName").invoke(child);
                    }
                    if (shown instanceof net.minecraft.network.chat.Component name) {
                        JsonObject e = new JsonObject();
                        e.addProperty("class", widgetClass(child.getClass()));
                        e.addProperty("label", name.getString());
                        // N118: the entry's tooltip lines (Cloth TooltipListEntry#getTooltip: Optional<Component[]>), for EN/DE checks
                        try {
                            if (child.getClass().getMethod("getTooltip").invoke(child) instanceof java.util.Optional<?> tip && tip.isPresent()
                                    && tip.get() instanceof net.minecraft.network.chat.Component[] lines) {
                                JsonArray t = new JsonArray();
                                for (var line : lines) t.add(line.getString());
                                e.add("tooltip", t);
                            }
                        } catch (ReflectiveOperationException | RuntimeException ignored) {
                            // no tooltip method
                        }
                        entries.add(e);
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // not an option entry
                }
            }
            if (child instanceof net.minecraft.client.gui.components.AbstractWidget w) {
                JsonObject j = new JsonObject();
                j.addProperty("class", widgetClass(w.getClass()));
                j.addProperty("label", w.getMessage().getString());
                j.addProperty("active", w.active);
                j.addProperty("visible", w.visible);
                j.addProperty("x", (w.getX() + w.getWidth() / 2.0) * scale);
                j.addProperty("y", (w.getY() + w.getHeight() / 2.0) * scale);
                j.addProperty("w", w.getWidth() * scale);
                j.addProperty("h", w.getHeight() * scale);
                if (w instanceof net.minecraft.client.gui.components.EditBox eb) j.addProperty("value", eb.getValue());
                out.add(j);
            }
            if (child instanceof net.minecraft.client.gui.components.events.ContainerEventHandler c) collectWidgets(c.children(), scale, out, entries, depth + 1);
        }
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
        // b-exp61: also without a player (title / world list screens after "Save and Quit"), so a driver sees those screens
        if (mc.player != null) o.addProperty("creative", mc.player.hasInfiniteMaterials());
        if (mc.player != null && screen instanceof AbstractContainerScreen<?> cs) o.addProperty("carried", describe(cs.getMenu().getCarried()));
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
            creativeTabs(cr, l, t, scale, o);
        }
        JsonArray slots = new JsonArray();
        if (mc.player != null && screen instanceof AbstractContainerScreen<?> cs && field(cs, "leftPos") instanceof Integer l && field(cs, "topPos") instanceof Integer t) {
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
        // a-w4c: every real widget of any open screen (Vanilla buttons, edit boxes ...) in window pixels, so a driver can click
        // it with the real OS mouse by its label without screen-specific layout maths.
        JsonArray widgets = new JsonArray(), entries = new JsonArray();
        if (screen != null) collectWidgets(screen.children(), scale, widgets, entries, 0);
        o.add("widgets", widgets);
        o.add("entries", entries);
        if (screen != null) o.addProperty("title", screen.getTitle().getString());
        // b-probe5: the open menu's synced data slots in the order the menu added them (e.g. an enchanter's tier and offers).
        if (screen instanceof AbstractContainerScreen<?> cs && field(cs.getMenu(), "dataSlots") instanceof List<?> data) {
            JsonArray values = new JsonArray();
            for (Object d : data) if (d instanceof net.minecraft.world.inventory.DataSlot ds) values.add(ds.get());
            o.add("menuData", values);
        }
        // a-w4c: a few well-known state fields of the open screen (page, scroll ... when the screen has them), read by reflection.
        JsonObject fields = new JsonObject();
        if (screen != null) {
            for (String n : new String[] {"page", "scroll", "selected", "tab", "mode", "index", "spread", "chapter"}) {
                Object v = field(screen, n);
                if (v instanceof Number num) fields.addProperty(n, num);
                else if (v instanceof Boolean b) fields.addProperty(n, b);
                else if (v instanceof String str) fields.addProperty(n, str);
            }
        }
        o.add("fields", fields);
        // N118: guide screens (SimpleLib's guide screen, SimpleBuilding's book): bookmarks, chapters, open/locked, pages (SimpleLib's GuideProbe facts)
        if (screen != null && (screen instanceof com.simplelib.client.guide.GuideProbe.Target || screen.getClass().getSimpleName().contains("Guide"))) {
            JsonObject guide = new JsonObject();
            com.simplelib.client.guide.GuideProbe.describe(mc, guide, scale);
            o.add("guide", guide);
        }
        JsonArray inv = new JsonArray();
        if (mc.player != null) for (int i = 0; i < 36; i++) inv.add(describe(mc.player.getInventory().getItem(i)));
        o.add("inventory", inv);
        JsonArray exp = new JsonArray();
        expected.forEach(exp::add);
        o.add("expected", exp);
        if (mc.level != null && mc.hitResult instanceof BlockHitResult h && h.getType() == HitResult.Type.BLOCK) {
            o.addProperty("hitBlock", BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(h.getBlockPos()).getBlock()).toString());
        }
        if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult eh) {
            o.addProperty("hitEntity", BuiltInRegistries.ENTITY_TYPE.getKey(eh.getEntity().getType()).toString());
        }
        if (mc.player != null) {
            o.addProperty("pos", mc.player.blockPosition().toShortString());
            // b-probe5: the body's turn, for probes of camera-only turns (SimpleLib's Free Look)
            o.addProperty("yaw", mc.player.getYRot());
            o.addProperty("pitch", mc.player.getXRot());
            // N60P r3: the client's sneak state (LocalPlayer reads the sneak key from its input) for probes of sneak + click actions
            o.addProperty("sneaking", mc.player.isShiftKeyDown());
            // N60P r4: the client's view of the held stack's component changes (e.g. bundle contents) for probes that cannot read chat
            o.addProperty("mainhandComponents", String.valueOf(mc.player.getMainHandItem().getComponentsPatch()));
            // N118: SimpleLib free look - on/off, camera offset and where the grey body-direction crosshair is drawn (window pixels)
            JsonObject look = new JsonObject();
            look.addProperty("active", com.simplelib.api.client.FreeLook.active());
            look.addProperty("yaw", com.simplelib.api.client.FreeLook.yawOffset());
            look.addProperty("pitch", com.simplelib.api.client.FreeLook.pitchOffset());
            look.addProperty("crosshairOption", com.simplelib.api.FreeLookRules.crosshair());
            int[] mark = com.simplelib.api.client.FreeLook.crosshairPoint(mc.player.getXRot(), w.getGuiScaledWidth(), w.getGuiScaledHeight(),
                    mc.options.fov().get());
            if (mark != null) {
                JsonArray m = new JsonArray();
                m.add(mark[0] * scale);
                m.add(mark[1] * scale);
                look.add("crosshair", m);
            }
            o.add("freeLook", look);
        }
        // N124: zoom key / spyglass state of Simple Interfaces (factor = field-of-view factor, smaller = closer)
        if (mc.player != null) {
            JsonObject zoom = new JsonObject();
            zoom.addProperty("scoping", mc.player.isScoping());
            zoom.addProperty("key", ZoomState.keyZoom());
            zoom.addProperty("zooming", ZoomState.zooming(mc.player));
            zoom.addProperty("overridesFov", ZoomState.overridesFov(mc.player));
            zoom.addProperty("factor", ZoomState.fov());
            zoom.addProperty("hideHud", ZoomState.hideHud());
            o.add("zoom", zoom);
        }
        // N124: the slot under the pointer of a container screen and the container preview its tooltip draws (shulker/ender grid or note)
        if (screen instanceof AbstractContainerScreen<?> cs && field(cs, "hoveredSlot") instanceof net.minecraft.world.inventory.Slot hs) {
            JsonObject hover = new JsonObject();
            hover.addProperty("index", hs.index);
            hover.addProperty("stack", describe(hs.getItem()));
            hover.addProperty("shift", com.simplelib.api.client.ui.UiShiftTooltip.held());
            var preview = ContainerPreviewTooltip.of(hs.getItem());
            if (preview != null) {
                hover.addProperty("grid", preview.grid());
                hover.addProperty("rows", preview.rows());
                hover.addProperty("filled", preview.slots().stream().filter(x -> !x.isEmpty()).count());
                hover.addProperty("more", preview.more());
                if (preview.note() != null) hover.addProperty("note", preview.note().getString());
            }
            o.add("hover", hover);
        }
        Path tmp = dir.resolve("simpleinterfaces-sort-status.tmp");
        Files.writeString(tmp, GSON.toJson(o));
        Files.move(tmp, dir.resolve(STATUS), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }
}
