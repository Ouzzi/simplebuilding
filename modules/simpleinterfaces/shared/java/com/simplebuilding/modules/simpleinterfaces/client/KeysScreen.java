package com.simplebuilding.modules.simpleinterfaces.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplelib.api.client.keys.KeyConflicts;
import com.simplelib.api.client.keys.KeyConflicts.Binding;
import com.simplelib.api.client.keys.KeyboardLayout;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiContrast;
import com.simplelib.api.client.ui.UiKeyboard;
import com.simplelib.api.client.ui.UiPalette;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The key binding page (UI28-1 / WP-25): every key mapping of Vanilla and all mods in the SimpleLib box style - a list with search,
 * categories, conflict marks and reset, and a keyboard picture (60 / 75 / 100 %). The Vanilla controls page stays; it gets a button
 * to this page ({@code KeyBindsScreenMixin}). Saving is Vanilla's own ({@code KeyMapping.setKey}, {@code Options.save}), so
 * {@code options.txt} stays the one place. The logic lives in SimpleLib ({@link KeyConflicts}, {@link KeyboardLayout}).
 * Conflicts are only shown and offered a free key nearby, never changed on their own (owner's K5 default).
 */
public final class KeysScreen extends Screen {
    static final UiPalette PALETTE = UiPalette.derived(0xFF55606E);
    private static final int ROW = 14, KEY_W = 74, SMALL = 14;
    private static final String K = "simpleinterfaces.keys.";
    /** Remembered between openings. */
    private static boolean listTab = true, german;
    private static KeyboardLayout.Size size = KeyboardLayout.Size.FULL;
    private static final Map<Integer, String> NAMES = new HashMap<>();

    private record Entry(KeyMapping mapping, Binding binding) {}

    private record Group(String name, String owner, List<Entry> items, boolean collapsed) {}

    private record Hit(int x, int y, int w, int h, Runnable run) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final Screen parent;
    private EditBox search;
    private String query = "";
    private boolean onlyConflicts, onlyUnbound, armedReset;
    private final Set<String> collapsed = new HashSet<>();
    private KeyMapping listening;
    /** Key code a list click is going to assign to (set from the keyboard popup), or -1. */
    private int pickCode = -1;
    private boolean inPopup;
    private int popupCode = -1;
    private int scroll;
    private int x, y, w, h;
    private List<Entry> entries = List.of();
    private Map<String, List<Binding>> byKey = Map.of();
    private final Map<String, Entry> byId = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>(), popupHits = new ArrayList<>();

    public KeysScreen(Screen parent) {
        super(Component.translatable(K + "title"));
        this.parent = parent;
    }

    /** Picks the tab and picture size the next opening starts with (client smoke test). */
    public static void preset(boolean list, KeyboardLayout.Size keyboardSize) {
        listTab = list;
        size = keyboardSize;
    }

    private static String t(String key, Object... args) {
        return Component.translatable(K + key, args).getString();
    }

    // --- data --------------------------------------------------------------------------------------------------------

    private void refresh() {
        List<Entry> list = new ArrayList<>();
        byId.clear();
        for (KeyMapping m : minecraft.options.keyMappings) {
            KeyMapping.Category category = m.getCategory();
            Binding b = new Binding(m.getName(), Component.translatable(m.getName()).getString(), category.label().getString(),
                    category.id().getNamespace(), m.saveString(), m.getDefaultKey().getName());
            Entry e = new Entry(m, b);
            list.add(e);
            byId.put(b.id(), e);
        }
        list.sort((a, b) -> {
            int vanilla = Boolean.compare(!"minecraft".equals(a.binding.owner()), !"minecraft".equals(b.binding.owner()));
            if (vanilla != 0) return vanilla;
            int g = a.binding.group().compareToIgnoreCase(b.binding.group());
            return g != 0 ? g : a.binding.action().compareToIgnoreCase(b.binding.action());
        });
        entries = list;
        byKey = KeyConflicts.byKey(list.stream().map(Entry::binding).toList());
    }

    private String shown(Binding b) {
        Entry e = byId.get(b.id());
        return e == null ? "" : e.mapping.getTranslatedKeyMessage().getString();
    }

    private static String nameOf(int code) {
        return NAMES.computeIfAbsent(code, c -> InputConstants.Type.KEYBOARD.getOrCreate(c).getName());
    }

    private List<Group> groups() {
        List<Binding> shown = KeyConflicts.filter(entries.stream().map(Entry::binding).toList(), query, onlyConflicts, onlyUnbound, this::shown);
        Map<String, List<Entry>> byGroup = new LinkedHashMap<>();
        for (Binding b : shown) byGroup.computeIfAbsent(b.owner() + "/" + b.group(), k -> new ArrayList<>()).add(byId.get(b.id()));
        List<Group> out = new ArrayList<>();
        for (List<Entry> items : byGroup.values()) {
            Binding first = items.get(0).binding;
            out.add(new Group(first.group(), first.owner(), items, collapsed.contains(first.owner() + "/" + first.group())));
        }
        return out;
    }

    // --- actions -----------------------------------------------------------------------------------------------------

    private void assign(KeyMapping m, InputConstants.Key key) {
        m.setKey(key);
        KeyMapping.resetMapping();
        minecraft.options.save();
        refresh();
    }

    private void resetAll(Iterable<Entry> which) {
        for (Entry e : which) e.mapping.setKey(e.mapping.getDefaultKey());
        KeyMapping.resetMapping();
        minecraft.options.save();
        refresh();
    }

    // --- screen ------------------------------------------------------------------------------------------------------

    @Override
    protected void init() {
        w = Math.min(width - 12, 430);
        h = Math.min(height - 12, 292);
        x = (width - w) / 2;
        y = (height - h) / 2;
        refresh();
        search = new EditBox(font, x + 8, y + 42, w - 16 - 150, 16, Component.translatable(K + "search")) {
            @Override
            public void setFocused(boolean focused) {
                super.setFocused(focused);
                Minecraft.getInstance().onTextInputFocusChange(this, focused);
            }
        };
        search.setMaxLength(64);
        search.setHint(Component.translatable(K + "search"));
        search.setValue(query);
        search.setResponder(q -> {
            query = q;
            scroll = 0;
        });
        addRenderableWidget(search);
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractBackground(g, mx, my, delta);
        UiBoxes.box(g, x, y, w, h, PALETTE);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        hits.clear();
        popupHits.clear();
        refresh();
        g.text(font, title, x + 8, y + 7, PALETTE.label(), false);
        // tabs
        button(g, mx, my, x + 8, y + 20, 64, 16, t("tab.list"), listTab, true, () -> switchTab(true));
        button(g, mx, my, x + 74, y + 20, 74, 16, t("tab.keyboard"), !listTab, true, () -> switchTab(false));
        search.visible = listTab;
        search.active = listTab;
        int contentY = y + 64, contentH = h - 64 - 28;
        if (listTab) {
            int fx = x + w - 8;
            fx -= 70;
            button(g, mx, my, fx, y + 42, 70, 16, t("only_unbound"), onlyUnbound, true, () -> onlyUnbound = !onlyUnbound);
            fx -= 76;
            button(g, mx, my, fx, y + 42, 74, 16, t("only_conflicts"), onlyConflicts, true, () -> onlyConflicts = !onlyConflicts);
            drawList(g, mx, my, x + 8, contentY, w - 16, contentH);
        } else {
            int bx = x + 8;
            for (KeyboardLayout.Size s : KeyboardLayout.Size.values()) {
                button(g, mx, my, bx, y + 42, 44, 16, s.percent, size == s, true, () -> size = s);
                bx += 46;
            }
            button(g, mx, my, bx + 6, y + 42, 56, 16, t("layout.german"), german, true, () -> german = !german);
            drawKeyboard(g, mx, my, x + 8, contentY, w - 16, contentH);
        }
        // footer
        int fy = y + h - 22;
        g.text(font, t("footer", KeyConflicts.sharedKeys(byKey), size.percent), x + 8, fy + 4, PALETTE.label(), false);
        int rx = x + w - 8 - 60;
        button(g, mx, my, rx, fy, 60, 16, t("done"), false, true, this::onClose);
        rx -= 100;
        String reset = armedReset ? t("reset_all_confirm") : t("reset_all");
        button(g, mx, my, rx, fy, 96, 16, reset, armedReset, true, () -> {
            if (armedReset) resetAll(entries);
            armedReset = !armedReset;
        });
        super.extractRenderState(g, mx, my, delta);
        if (popupCode >= 0 && !listTab) drawPopup(g, mx, my);
    }

    private void switchTab(boolean list) {
        listTab = list;
        popupCode = -1;
        pickCode = -1;
        listening = null;
    }

    private int ink(int face) {
        return UiContrast.ensure(PALETTE.label(), face);
    }

    private void button(GuiGraphicsExtractor g, int mx, int my, int bx, int by, int bw, int bh, String text, boolean on, boolean enabled, Runnable run) {
        boolean hover = enabled && new Hit(bx, by, bw, bh, run).contains(mx, my);
        int face = on ? PALETTE.slot() : UiPalette.mix(PALETTE.fill(), 0xFFFFFFFF, hover ? 0.34 : 0.2);
        if (on) UiBoxes.sunkRect(g, bx, by, bw, bh, PALETTE);
        else UiBoxes.raised(g, bx, by, bw, bh, face);
        int color = enabled ? ink(face) : UiContrast.ensure(0xFF9A9A9A, face);
        g.text(font, fit(text, bw - 4), bx + (bw - Math.min(font.width(text), bw - 4)) / 2, by + (bh - 8) / 2 + 1, color, false);
        if (enabled) (inPopup ? popupHits : hits).add(new Hit(bx, by, bw, bh, run));
    }

    private String fit(String text, int max) {
        if (font.width(text) <= max) return text;
        String s = text;
        while (!s.isEmpty() && font.width(s + "...") > max) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    // --- list --------------------------------------------------------------------------------------------------------

    private void drawList(GuiGraphicsExtractor g, int mx, int my, int lx, int ly, int lw, int lh) {
        UiBoxes.inset(g, lx, ly, lw, lh, PALETTE);
        int top = ly;
        if (pickCode >= 0) {
            g.text(font, fit(t("pick", KeyboardLayout.label(pickCode, german)), lw - 80), lx + 6, ly + 3, ink(PALETTE.slot()), false);
            button(g, mx, my, lx + lw - 70, ly + 1, 66, 12, t("cancel"), false, true, () -> pickCode = -1);
            top += ROW;
        }
        List<Group> groups = groups();
        int total = 0;
        for (Group gr : groups) total += ROW + (gr.collapsed ? 0 : gr.items.size() * ROW);
        int view = lh - (top - ly) - 2;
        scroll = Math.max(0, Math.min(scroll, total - view));
        if (groups.isEmpty()) g.text(font, t("empty"), lx + 8, top + 6, ink(PALETTE.slot()), false);
        g.enableScissor(lx + 1, top, lx + lw - 1, top + view);
        int ry = top - scroll;
        List<Hit> list = new ArrayList<>();
        for (Group gr : groups) {
            drawHeader(g, mx, my, gr, lx, ry, lw, top, top + view, list);
            ry += ROW;
            if (gr.collapsed) continue;
            for (Entry e : gr.items) {
                if (ry + ROW > top && ry < top + view) drawRow(g, mx, my, e, lx, ry, lw, top, top + view, list);
                ry += ROW;
            }
        }
        g.disableScissor();
        // scroll bar
        if (total > view) {
            int bh = Math.max(12, view * view / total), by = top + (view - bh) * scroll / Math.max(1, total - view);
            g.fill(lx + lw - 4, top, lx + lw - 2, top + view, UiPalette.scale(PALETTE.fill(), 0.5));
            g.fill(lx + lw - 4, by, lx + lw - 2, by + bh, PALETTE.light());
        }
        // list hits come after the scissor; clip them to the visible band
        int clipTop = top, clipBottom = top + view;
        for (Hit hit : list) {
            if (hit.y + hit.h > clipTop && hit.y < clipBottom) hits.add(hit);
        }
    }

    private void drawHeader(GuiGraphicsExtractor g, int mx, int my, Group gr, int lx, int ry, int lw, int top, int bottom, List<Hit> list) {
        if (ry + ROW <= top || ry >= bottom) return;
        int face = UiPalette.mix(PALETTE.fill(), 0xFF000000, 0.12);
        g.fill(lx + 2, ry, lx + lw - 6, ry + ROW - 1, face);
        String id = gr.owner + "/" + gr.name;
        g.text(font, (gr.collapsed ? "+ " : "- ") + fit(gr.name + " (" + gr.items.size() + ")", lw - 130)
                + ("minecraft".equals(gr.owner) ? "" : "  " + gr.owner), lx + 6, ry + 3, ink(face), false);
        list.add(new Hit(lx + 2, ry, lw - 28, ROW - 1, () -> {
            if (!collapsed.remove(id)) collapsed.add(id);
        }));
        boolean any = gr.items.stream().anyMatch(e -> !e.binding.isDefault());
        small(g, mx, my, lx + lw - 6 - SMALL - 1, ry, "↺", any, () -> resetAll(gr.items), list);
    }

    private void small(GuiGraphicsExtractor g, int mx, int my, int sx, int sy, String text, boolean enabled, Runnable run, List<Hit> list) {
        boolean hover = enabled && mx >= sx && mx < sx + SMALL && my >= sy && my < sy + ROW - 1;
        int face = UiPalette.mix(PALETTE.fill(), 0xFFFFFFFF, hover ? 0.34 : 0.2);
        UiBoxes.raised(g, sx, sy, SMALL, ROW - 1, face);
        g.text(font, text, sx + (SMALL - font.width(text)) / 2, sy + 3, enabled ? ink(face) : UiContrast.ensure(0xFF8A8A8A, face), false);
        if (enabled) list.add(new Hit(sx, sy, SMALL, ROW - 1, run));
    }

    private void drawRow(GuiGraphicsExtractor g, int mx, int my, Entry e, int lx, int ry, int lw, int top, int bottom, List<Hit> list) {
        Binding b = e.binding;
        boolean conflict = KeyConflicts.conflicts(byKey, b);
        boolean rowHover = mx >= lx + 2 && mx < lx + lw - 6 && my >= Math.max(ry, top) && my < Math.min(ry + ROW, bottom);
        int rowFace = UiPalette.mix(UiPalette.mix(PALETTE.slot(), PALETTE.fill(), 0.35), 0xFFFFFFFF, rowHover ? 0.1 : 0);
        g.fill(lx + 2, ry, lx + lw - 6, ry + ROW - 1, rowFace);
        if (conflict) {
            g.fill(lx + 4, ry + 2, lx + 8, ry + ROW - 3, UiKeyboard.SHARED);
        }
        int right = lx + lw - 6;
        int resetX = right - SMALL - 1, keyX = resetX - KEY_W - 2;
        g.text(font, fit(b.action(), keyX - (lx + 12) - 4), lx + 12, ry + 3, ink(rowFace), false);
        boolean waiting = listening == e.mapping;
        String keyText = waiting ? t("press") : b.unbound() ? t("none") : shown(b);
        int keyFace = waiting ? PALETTE.fill() : conflict ? UiPalette.mix(UiKeyboard.SHARED, 0xFF000000, 0.1) : UiPalette.mix(PALETTE.fill(), 0xFFFFFFFF, 0.2);
        UiBoxes.raised(g, keyX, ry, KEY_W, ROW - 1, keyFace);
        g.text(font, fit(keyText, KEY_W - 4), keyX + (KEY_W - Math.min(font.width(keyText), KEY_W - 4)) / 2, ry + 3,
                UiContrast.ensure(0xFFFFFFFF, keyFace), false);
        list.add(new Hit(keyX, ry, KEY_W, ROW - 1, () -> {
            if (pickCode >= 0) {
                assign(e.mapping, InputConstants.Type.KEYBOARD.getOrCreate(pickCode));
                pickCode = -1;
                listTab = false;
            } else {
                listening = waiting ? null : e.mapping;
            }
        }));
        small(g, mx, my, resetX, ry, "↺", !b.isDefault(), () -> assign(e.mapping, e.mapping.getDefaultKey()), list);
        if (rowHover && !(mx >= keyX)) {
            List<Binding> others = KeyConflicts.sharedWith(byKey, b);
            if (!others.isEmpty()) {
                List<FormattedCharSequence> lines = new ArrayList<>();
                lines.addAll(font.split(Component.literal(t("tip.shares")), 220));
                for (Binding o : others) lines.addAll(font.split(Component.literal(o.action() + " (" + o.group() + ")"), 220));
                g.setTooltipForNextFrame(font, lines, mx, my);
            }
        }
    }

    // --- keyboard ----------------------------------------------------------------------------------------------------

    private List<KeyboardLayout.Cell> cells() {
        return KeyboardLayout.cells(size, german);
    }

    private void drawKeyboard(GuiGraphicsExtractor g, int mx, int my, int kx, int ky, int kw, int kh) {
        List<KeyboardLayout.Cell> cells = cells();
        int scale = KeyboardLayout.scale(cells, kw - 8, kh - 8);
        int pw = KeyboardLayout.width(cells) * scale, ph = KeyboardLayout.height(cells) * scale;
        int ox = kx + (kw - pw) / 2, oy = ky + (kh - ph) / 2;
        UiBoxes.inset(g, kx, ky, kw, kh, PALETTE);
        var hover = popupCode >= 0 ? java.util.Optional.<KeyboardLayout.Cell>empty() : KeyboardLayout.at(cells, ox, oy, scale, mx, my);
        UiKeyboard.draw(g, font, cells, ox, oy, scale, c -> KeyConflicts.state(byKey, nameOf(c.code())), hover.map(KeyboardLayout.Cell::code).orElse(-1), popupCode);
        hover.ifPresent(c -> {
            List<FormattedCharSequence> lines = new ArrayList<>();
            List<Binding> on = byKey.getOrDefault(nameOf(c.code()), List.of());
            lines.addAll(font.split(Component.literal(c.label()), 220));
            if (on.isEmpty()) lines.addAll(font.split(Component.literal(t("tip.free")), 220));
            if (on.size() > 1) lines.addAll(font.split(Component.literal(t("tip.shared")), 220));
            for (int i = 0; i < Math.min(on.size(), 4); i++) lines.addAll(font.split(Component.literal(on.get(i).action()), 220));
            if (on.size() > 4) lines.addAll(font.split(Component.literal("+" + (on.size() - 4)), 220));
            lines.addAll(font.split(Component.literal(t("tip.click")), 220));
            g.setTooltipForNextFrame(font, lines, mx, my);
        });
        hits.add(new Hit(ox, oy, pw, ph, () -> KeyboardLayout.at(cells, ox, oy, scale, lastX, lastY).ifPresent(c -> popupCode = c.code())));
    }

    private double lastX, lastY;

    private void drawPopup(GuiGraphicsExtractor g, int mx, int my) {
        String name = nameOf(popupCode);
        List<Binding> on = byKey.getOrDefault(name, List.of());
        inPopup = true;
        int pw = 230, ph = 38 + on.size() * ROW + 22;
        int px = x + (w - pw) / 2, py = y + (h - ph) / 2;
        g.fill(0, 0, width, height, 0x88000000);
        UiBoxes.box(g, px, py, pw, ph, PALETTE);
        KeyboardLayout.Cell cell = cells().stream().filter(c -> c.code() == popupCode).findFirst().orElse(null);
        String label = cell == null ? name : cell.label();
        g.text(font, label, px + 8, py + 7, PALETTE.label(), false);
        int ry = py + 22;
        for (int i = 0; i < on.size(); i++) {
            Binding b = on.get(i);
            Entry e = byId.get(b.id());
            g.text(font, fit(b.action(), pw - 16 - 90), px + 8, ry + 3, PALETTE.label(), false);
            int bx = px + pw - 8 - SMALL;
            button(g, mx, my, bx, ry, SMALL, ROW - 1, "x", false, true, () -> assign(e.mapping, InputConstants.UNKNOWN));
            if (on.size() > 1 && i > 0) {
                var free = KeyboardLayout.nearestFree(KeyboardLayout.cells(KeyboardLayout.Size.FULL, german), usedCodes(), popupCode);
                if (free.isPresent()) {
                    KeyboardLayout.Cell target = free.get();
                    button(g, mx, my, bx - 62, ry, 60, ROW - 1, t("move", target.label()), false, true,
                            () -> assign(e.mapping, InputConstants.Type.KEYBOARD.getOrCreate(target.code())));
                }
            }
            ry += ROW;
        }
        button(g, mx, my, px + 8, ry + 4, pw - 16, 16, t("popup.assign"), false, true, () -> {
            pickCode = popupCode;
            popupCode = -1;
            listTab = true;
        });
        inPopup = false;
    }

    private Set<Integer> usedCodes() {
        Set<Integer> used = new HashSet<>();
        for (KeyboardLayout.Cell c : KeyboardLayout.cells(KeyboardLayout.Size.FULL, false)) {
            if (byKey.containsKey(nameOf(c.code()))) used.add(c.code());
        }
        return used;
    }

    // --- input -------------------------------------------------------------------------------------------------------

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listening != null) {
            KeyMapping m = listening;
            listening = null;
            if (event.key() != InputConstants.KEY_ESCAPE) assign(m, InputConstants.Type.KEYBOARD.getOrCreate(event.key()));
            return true;
        }
        if (event.key() == InputConstants.KEY_ESCAPE && (popupCode >= 0 || pickCode >= 0)) {
            popupCode = -1;
            pickCode = -1;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        lastX = mx;
        lastY = my;
        if (listening != null) {
            KeyMapping m = listening;
            listening = null;
            assign(m, InputConstants.Type.MOUSE.getOrCreate(event.button()));
            return true;
        }
        if (event.button() == 0) {
            if (popupCode >= 0 && !listTab) {
                for (Hit hit : new ArrayList<>(popupHits)) {
                    if (hit.contains(mx, my)) {
                        hit.run.run();
                        return true;
                    }
                }
                popupCode = -1; // a click outside closes it
                return true;
            }
            boolean wasArmed = armedReset;
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit hit = hits.get(i);
                if (hit.contains(mx, my)) {
                    hit.run.run();
                    if (wasArmed && armedReset) armedReset = false; // any other button cancels the question
                    return true;
                }
            }
            armedReset = false;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (listTab) {
            scroll = Math.max(0, scroll - (int) (scrollY * ROW * 2));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
