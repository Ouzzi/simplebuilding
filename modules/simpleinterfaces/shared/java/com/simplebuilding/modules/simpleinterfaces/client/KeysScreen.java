package com.simplebuilding.modules.simpleinterfaces.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplelib.api.client.keys.KeyConflicts;
import com.simplelib.api.client.keys.KeyConflicts.Binding;
import com.simplelib.api.client.keys.KeyboardLayout;
import com.simplelib.api.client.ui.UiKeyboard;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

/**
 * The key binding page (UI28-1 / WP-25, rebuilt IG31-31 on 31.10.): every key mapping of Vanilla and all mods in three views -
 * "by mod" (one group per mod), "by function" (Vanilla's categories in Vanilla's order, like the Vanilla controls page) and a
 * keyboard picture (60 / 75 / 100 %). Everything is a real Vanilla widget (buttons, edit box, the Vanilla scroll list, a
 * widget for the picture), so clicks take the same path as on any Vanilla screen - the self-drawn buttons with their own hit
 * test before did not react to the real mouse everywhere (IG31-31). The look is Vanilla's: menu background, list, buttons.
 * Saving is Vanilla's own ({@code KeyMapping.setKey}, {@code Options.save}), so {@code options.txt} stays the one place; the
 * Vanilla controls page gets a button to this page ({@code KeyBindsScreenMixin}). Conflicts are only shown and offered a
 * free key nearby, never changed on their own (owner's K5 default). The logic lives in SimpleLib ({@link KeyConflicts},
 * {@link KeyboardLayout}).
 */
public final class KeysScreen extends Screen {
    /** The three views. */
    public enum View { MOD, FUNCTION, KEYBOARD }

    private static final String K = "simpleinterfaces.keys.";
    private static final int ROW = 20, TOP = 68;
    /** Remembered between openings. */
    private static View view = View.MOD;
    private static boolean german;
    private static KeyboardLayout.Size size = KeyboardLayout.Size.FULL;
    private static final Map<Integer, String> NAMES = new HashMap<>();

    private record Item(KeyMapping mapping, Binding binding) {}

    private final Screen parent;
    private String query = "";
    private boolean onlyConflicts, onlyUnbound, armedReset;
    private final Set<String> collapsed = new HashSet<>();
    KeyMapping listening;
    /** Key code a list click is going to assign to (chosen on the keyboard picture), or -1. */
    private int pickCode = -1;
    private List<Item> entries = List.of();
    private Map<String, List<Binding>> byKey = Map.of();
    private final Map<String, Item> byId = new HashMap<>();
    private KeysList list;
    private EditBox search;
    /** Named widgets for the real-mouse probe and the client test (GUI coordinates). */
    final Map<String, AbstractWidget> probe = new LinkedHashMap<>();

    public KeysScreen(Screen parent) {
        super(Component.translatable(K + "title"));
        this.parent = parent;
    }

    /** Picks the view and picture size the next opening starts with (client smoke test). */
    public static void preset(View start, KeyboardLayout.Size keyboardSize) {
        org.slf4j.LoggerFactory.getLogger("simpleinterfaces").info("DEBUG keys preset {}", start, new Throwable());
        view = start;
        size = keyboardSize;
    }

    /** Old form: {@code list} = the by-mod view, else the keyboard. */
    public static void preset(boolean list, KeyboardLayout.Size keyboardSize) {
        preset(list ? View.MOD : View.KEYBOARD, keyboardSize);
    }

    public static View view() {
        return view;
    }

    private static MutableComponent t(String key, Object... args) {
        return Component.translatable(K + key, args);
    }

    // --- data --------------------------------------------------------------------------------------------------------

    private void refresh() {
        List<Item> out = new ArrayList<>();
        byId.clear();
        KeyMapping[] all = minecraft.options.keyMappings.clone();
        Arrays.sort(all); // Vanilla's order: category order, then name
        for (KeyMapping m : all) {
            KeyMapping.Category category = m.getCategory();
            Binding b = new Binding(m.getName(), Component.translatable(m.getName()).getString(), category.label().getString(),
                    category.id().getNamespace(), m.saveString(), m.getDefaultKey().getName());
            Item e = new Item(m, b);
            out.add(e);
            byId.put(b.id(), e);
        }
        entries = out;
        byKey = KeyConflicts.byKey(out.stream().map(Item::binding).toList());
    }

    private String shown(Binding b) {
        Item e = byId.get(b.id());
        return e == null ? "" : e.mapping.getTranslatedKeyMessage().getString();
    }

    static String nameOf(int code) {
        return NAMES.computeIfAbsent(code, c -> InputConstants.Type.KEYBOARD.getOrCreate(c).getName());
    }

    /** Group title in the by-mod view: "Minecraft" or the label of the mod's first category (normally the mod's name). */
    private String modTitle(String owner) {
        if ("minecraft".equals(owner)) return "Minecraft";
        for (Item e : entries) if (e.binding.owner().equals(owner)) return e.binding.group();
        return owner;
    }

    /** The shown groups of the current list view, in order: group id -> entries. */
    private Map<String, List<Item>> groups() {
        List<Binding> shown = KeyConflicts.filter(entries.stream().map(Item::binding).toList(), query, onlyConflicts, onlyUnbound, this::shown);
        Map<String, List<Item>> out = new LinkedHashMap<>();
        if (view == View.MOD) {
            // Vanilla first, then the mods by name; inside a mod Vanilla's category order (stable sort)
            List<Binding> sorted = new ArrayList<>(shown);
            sorted.sort((a, b) -> {
                int vanilla = Boolean.compare(!"minecraft".equals(a.owner()), !"minecraft".equals(b.owner()));
                return vanilla != 0 ? vanilla : modTitle(a.owner()).compareToIgnoreCase(modTitle(b.owner()));
            });
            for (Binding b : sorted) out.computeIfAbsent("mod/" + b.owner(), k -> new ArrayList<>()).add(byId.get(b.id()));
        } else {
            for (Binding b : shown) out.computeIfAbsent("fn/" + b.owner() + "/" + b.group(), k -> new ArrayList<>()).add(byId.get(b.id()));
        }
        return out;
    }

    // --- actions -----------------------------------------------------------------------------------------------------

    void assign(KeyMapping m, InputConstants.Key key) {
        m.setKey(key);
        KeyMapping.resetMapping();
        minecraft.options.save();
        refresh();
        if (list != null) list.refill();
    }

    private void resetAll(List<Item> which) {
        for (Item e : which) e.mapping.setKey(e.mapping.getDefaultKey());
        KeyMapping.resetMapping();
        minecraft.options.save();
        refresh();
        if (list != null) list.refill();
    }

    private void switchView(View next) {
        org.slf4j.LoggerFactory.getLogger("simpleinterfaces").info("DEBUG keys switchView {} -> {}", view, next, new Throwable());
        view = next;
        listening = null;
        if (next == View.KEYBOARD) pickCode = -1;
        rebuildWidgets();
    }

    /** Keyboard picture: a key was clicked - its own page lists the actions on it (remove, move, assign). */
    private void openKey(int code) {
        minecraft.gui.setScreen(new KeyCellScreen(this, code));
    }

    /** From {@link KeyCellScreen}: pick the action for {@code code} in the list. */
    void startPick(int code) {
        pickCode = code;
        view = View.FUNCTION;
        minecraft.gui.setScreen(this);
    }

    Map<String, List<Binding>> byKey() {
        refresh();
        return byKey;
    }

    KeyMapping mapping(Binding b) {
        Item e = byId.get(b.id());
        return e == null ? null : e.mapping;
    }

    static boolean german() {
        return german;
    }

    /** The key codes already bound (for "move to a free key"). */
    Set<Integer> usedCodes() {
        Set<Integer> used = new HashSet<>();
        for (KeyboardLayout.Cell c : KeyboardLayout.cells(KeyboardLayout.Size.FULL, false)) {
            if (byKey.containsKey(nameOf(c.code()))) used.add(c.code());
        }
        return used;
    }

    // --- screen ------------------------------------------------------------------------------------------------------

    @Override
    protected void init() {
        org.slf4j.LoggerFactory.getLogger("simpleinterfaces").info("DEBUG keys init view {}", view);
        refresh();
        probe.clear();
        list = null;
        search = null;
        int cx = width / 2;
        // row 1: the three views
        int tabW = Math.min(110, (width - 24) / 3);
        View[] views = View.values();
        for (int i = 0; i < views.length; i++) {
            View v = views[i];
            String id = v.name().toLowerCase(java.util.Locale.ROOT);
            Button tab = addRenderableWidget(Button.builder(t("tab." + id), b -> {
                org.slf4j.LoggerFactory.getLogger("simpleinterfaces").info("DEBUG keys tab pressed {}", v);
                switchView(v);
            })
                    .bounds(cx - (3 * tabW + 8) / 2 + i * (tabW + 4), 20, tabW, 20).build());
            tab.active = view != v;
            probe.put("tab_" + id, tab);
        }
        // row 2: search and filters, or the picture's size and layout
        int rowY = 44;
        if (view == View.KEYBOARD) {
            int bx = cx - (3 * 52 + 102) / 2;
            for (KeyboardLayout.Size s : KeyboardLayout.Size.values()) {
                Button b = addRenderableWidget(Button.builder(Component.literal(s.percent), x -> {
                    size = s;
                    rebuildWidgets();
                }).bounds(bx, rowY, 50, 20).build());
                b.active = size != s;
                probe.put("size_" + s.percent.replace("%", ""), b);
                bx += 52;
            }
            probe.put("german", addRenderableWidget(Button.builder(filterLabel("layout.german", german), b -> {
                german = !german;
                rebuildWidgets();
            }).bounds(bx, rowY, 102, 20).build()));
            int top = TOP, bottom = height - 33;
            probe.put("keyboard", addRenderableWidget(new KeyboardWidget(8, top, width - 16, bottom - top)));
        } else {
            if (pickCode >= 0) {
                probe.put("cancel_pick", addRenderableWidget(Button.builder(t("cancel"), b -> {
                    pickCode = -1;
                    rebuildWidgets();
                }).bounds(cx + 104, rowY, 100, 20).build()));
            } else {
                int sw = Math.max(60, Math.min(200, width - 24 - 2 * 104));
                int sx = cx - (sw + 2 * 104) / 2;
                search = new EditBox(font, sx, rowY, sw, 20, t("search")) {
                    @Override
                    public void setFocused(boolean focused) {
                        super.setFocused(focused);
                        Minecraft.getInstance().onTextInputFocusChange(this, focused);
                    }
                };
                search.setMaxLength(64);
                search.setHint(t("search"));
                search.setValue(query);
                search.setResponder(q -> {
                    query = q;
                    if (list != null) {
                        list.refill();
                        list.setScrollAmount(0);
                    }
                });
                probe.put("search", addRenderableWidget(search));
                probe.put("only_conflicts", addRenderableWidget(Button.builder(filterLabel("only_conflicts", onlyConflicts), b -> {
                    onlyConflicts = !onlyConflicts;
                    b.setMessage(filterLabel("only_conflicts", onlyConflicts));
                    list.refill();
                }).bounds(sx + sw + 4, rowY, 100, 20).build()));
                probe.put("only_unbound", addRenderableWidget(Button.builder(filterLabel("only_unbound", onlyUnbound), b -> {
                    onlyUnbound = !onlyUnbound;
                    b.setMessage(filterLabel("only_unbound", onlyUnbound));
                    list.refill();
                }).bounds(sx + sw + 108, rowY, 100, 20).build()));
            }
            list = addRenderableWidget(new KeysList(minecraft, width, height - 33 - TOP, TOP));
            list.refill();
        }
        // footer
        int fy = height - 27;
        probe.put("reset_all", addRenderableWidget(Button.builder(resetLabel(), b -> {
            if (armedReset) resetAll(entries);
            armedReset = !armedReset;
            b.setMessage(resetLabel());
        }).bounds(cx - 154, fy, 150, 20).build()));
        probe.put("done", addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(cx + 4, fy, 150, 20).build()));
    }

    private Component resetLabel() {
        return armedReset ? t("reset_all_confirm").withStyle(ChatFormatting.YELLOW) : t("reset_all");
    }

    private static Component filterLabel(String key, boolean on) {
        return CommonComponents.optionNameValue(t(key), CommonComponents.optionStatus(on));
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        minecraft.options.save();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        g.centeredText(font, title, width / 2, 7, 0xFFFFFFFF);
        if (view != View.KEYBOARD && pickCode >= 0) {
            g.centeredText(font, t("pick", KeyboardLayout.label(pickCode, german)), width / 2 - 52, 50, 0xFFFFFF55);
        }
        Component info = t("footer", KeyConflicts.sharedKeys(byKey), size.percent);
        if (font.width(info) < width / 2 - 170) g.text(font, info, 8, height - 21, 0xFFA0A0A0);
    }

    // --- input: assigning a key, like Vanilla's controls page --------------------------------------------------------

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listening != null) {
            KeyMapping m = listening;
            listening = null;
            assign(m, event.isEscape() ? InputConstants.UNKNOWN : InputConstants.getKey(event));
            return true;
        }
        if (event.isEscape() && pickCode >= 0) {
            pickCode = -1;
            rebuildWidgets();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (listening != null) {
            KeyMapping m = listening;
            listening = null;
            assign(m, InputConstants.Type.MOUSE.getOrCreate(event.button()));
            return true;
        }
        AbstractWidget reset = probe.get("reset_all");
        if (armedReset && reset != null && !reset.isMouseOver(event.x(), event.y())) {
            armedReset = false; // any other click cancels the question
            if (reset instanceof Button b) b.setMessage(resetLabel());
        }
        return super.mouseClicked(event, doubleClick);
    }

    // --- the list (Vanilla scroll list, Vanilla rows) ---------------------------------------------------------------

    final class KeysList extends ContainerObjectSelectionList<Row> {
        KeysList(Minecraft mc, int width, int height, int y) {
            super(mc, width, height, y, ROW);
        }

        @Override
        public int getRowWidth() {
            return Math.min(360, width - 20);
        }

        void refill() {
            double keep = scrollAmount();
            List<Row> rows = new ArrayList<>();
            Map<String, List<Item>> groups = groups();
            for (Map.Entry<String, List<Item>> gr : groups.entrySet()) {
                List<Item> items = gr.getValue();
                Binding first = items.get(0).binding;
                String title = view == View.MOD ? modTitle(first.owner()) : first.group();
                rows.add(new HeaderRow(gr.getKey(), Component.literal(title), items));
                if (collapsed.contains(gr.getKey())) continue;
                for (Item e : items) rows.add(new KeyRow(e));
            }
            if (groups.isEmpty()) rows.add(new HeaderRow(null, t("empty"), List.of()));
            replaceEntries(rows);
            setScrollAmount(keep);
        }

        /** Scrolls the row of {@code id} into view (probe/test); false when it is not shown. */
        boolean show(String id) {
            KeyRow r = row(id);
            if (r == null) return false;
            scrollToEntry(r);
            return true;
        }

        KeyRow row(String id) {
            for (Row r : children()) if (r instanceof KeyRow k && k.entry.binding.id().equals(id)) return k;
            return null;
        }

        HeaderRow header(int index) {
            int i = 0;
            for (Row r : children()) if (r instanceof HeaderRow h && h.id != null && i++ == index) return h;
            return null;
        }
    }

    abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {}

    /** A group heading like Vanilla's category rows (bold, underlined, centred), with fold and group reset buttons. */
    final class HeaderRow extends Row {
        final String id;
        private final Component title;
        final Button fold, reset;

        HeaderRow(String id, Component title, List<Item> items) {
            this.id = id;
            this.title = id == null ? title : title.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE)
                    .append(Component.literal(" (" + items.size() + ")").withStyle(ChatFormatting.GRAY));
            boolean folded = id != null && collapsed.contains(id);
            fold = id == null ? null : Button.builder(Component.literal(folded ? "+" : "-"), b -> {
                if (!collapsed.remove(id)) collapsed.add(id);
                list.refill();
            }).bounds(0, 0, 20, 20).tooltip(Tooltip.create(t(folded ? "unfold" : "fold"))).build();
            reset = id == null ? null : Button.builder(Component.translatable("controls.reset"), b -> resetAll(items)).bounds(0, 0, 50, 20)
                    .tooltip(Tooltip.create(t("reset_group"))).build();
            if (reset != null) reset.active = items.stream().anyMatch(e -> !e.binding.isDefault());
        }

        @Override
        public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovered, float delta) {
            var font = Minecraft.getInstance().font;
            g.centeredText(font, title, getContentX() + getContentWidth() / 2, getContentYMiddle() - 4, 0xFFFFFFFF);
            if (fold != null) {
                fold.setPosition(getContentX(), getContentY() - 2);
                fold.extractRenderState(g, mouseX, mouseY, delta);
                reset.setPosition(getContentX() + getContentWidth() - reset.getWidth(), getContentY() - 2);
                reset.extractRenderState(g, mouseX, mouseY, delta);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return fold == null ? List.of() : List.of(fold, reset);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return fold == null ? List.of() : List.of(fold, reset);
        }
    }

    /** One action like Vanilla's key rows: name, the key button (click, then press a key or mouse button), reset. */
    final class KeyRow extends Row {
        final Item entry;
        final Button change, reset;

        KeyRow(Item entry) {
            this.entry = entry;
            change = Button.builder(entry.mapping.getTranslatedKeyMessage(), b -> {
                if (pickCode >= 0) {
                    assign(entry.mapping, InputConstants.Type.KEYBOARD.getOrCreate(pickCode));
                    pickCode = -1;
                    switchView(View.KEYBOARD);
                } else {
                    listening = listening == entry.mapping ? null : entry.mapping;
                }
            }).bounds(0, 0, 75, 20).build();
            reset = Button.builder(Component.translatable("controls.reset"), b -> assign(entry.mapping, entry.mapping.getDefaultKey()))
                    .bounds(0, 0, 50, 20).build();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovered, float delta) {
            Item now = byId.get(entry.binding.id()); // current state after an assignment
            Binding b = now == null ? entry.binding : now.binding;
            List<Binding> others = KeyConflicts.sharedWith(byKey, b);
            Component key = entry.mapping.getTranslatedKeyMessage();
            if (!others.isEmpty()) key = Component.literal("[ ").append(key.copy().withStyle(ChatFormatting.WHITE)).append(" ]").withStyle(ChatFormatting.YELLOW);
            if (listening == entry.mapping) {
                key = Component.literal("> ").append(key.copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE)).append(" <")
                        .withStyle(ChatFormatting.YELLOW);
            } else if (pickCode >= 0) {
                key = Component.literal("-> " + KeyboardLayout.label(pickCode, german)).withStyle(ChatFormatting.YELLOW);
            }
            change.setMessage(key);
            if (others.isEmpty()) {
                change.setTooltip(null);
            } else {
                MutableComponent names = Component.empty();
                for (int i = 0; i < others.size(); i++) names.append(i == 0 ? "" : ", ").append(others.get(i).action());
                change.setTooltip(Tooltip.create(Component.translatable("controls.keybinds.duplicateKeybinds", names)));
            }
            reset.active = !b.isDefault();
            int right = getContentX() + getContentWidth();
            reset.setPosition(right - reset.getWidth(), getContentY() - 2);
            change.setPosition(reset.getX() - 5 - change.getWidth(), getContentY() - 2);
            reset.extractRenderState(g, mouseX, mouseY, delta);
            change.extractRenderState(g, mouseX, mouseY, delta);
            var font = Minecraft.getInstance().font;
            g.text(font, fit(font, b.action(), change.getX() - getContentX() - 8), getContentX(), getContentYMiddle() - 4, 0xFFFFFFFF);
            if (!others.isEmpty()) g.fill(change.getX() - 6, getContentY() - 1, change.getX() - 3, getContentBottom(), 0xFFFFFF00);
            if (view == View.MOD && hovered && mouseX < change.getX()) {
                g.setTooltipForNextFrame(font, List.of(Component.literal(b.group()).getVisualOrderText()), mouseX, mouseY);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(change, reset);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(change, reset);
        }
    }

    static String fit(net.minecraft.client.gui.Font font, String text, int max) {
        if (font.width(text) <= max) return text;
        String s = text;
        while (!s.isEmpty() && font.width(s + "...") > max) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    // --- the keyboard picture (one widget; a click opens the key's page) ---------------------------------------------

    final class KeyboardWidget extends AbstractWidget {
        KeyboardWidget(int x, int y, int w, int h) {
            super(x, y, w, h, t("tab.keyboard"));
        }

        private List<KeyboardLayout.Cell> cells() {
            return KeyboardLayout.cells(size, german);
        }

        int scale() {
            return KeyboardLayout.scale(cells(), getWidth() - 8, getHeight() - 8);
        }

        int originX() {
            return getX() + (getWidth() - KeyboardLayout.width(cells()) * scale()) / 2;
        }

        int originY() {
            return getY() + (getHeight() - KeyboardLayout.height(cells()) * scale()) / 2;
        }

        /** The cell of {@code code} as x, y, w, h in GUI coordinates (probe), or null. */
        int[] cellRect(int code) {
            int s = scale();
            for (KeyboardLayout.Cell c : cells()) {
                if (c.code() == code) return new int[]{originX() + c.x() * s, originY() + c.y() * s, c.w() * s - 1, c.h() * s - 1};
            }
            return null;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            List<KeyboardLayout.Cell> cells = cells();
            int scale = scale(), ox = originX(), oy = originY();
            var hover = isHovered() ? KeyboardLayout.at(cells, ox, oy, scale, mx, my) : java.util.Optional.<KeyboardLayout.Cell>empty();
            UiKeyboard.draw(g, font, cells, ox, oy, scale, c -> KeyConflicts.state(byKey, nameOf(c.code())), hover.map(KeyboardLayout.Cell::code).orElse(-1), -1);
            hover.ifPresent(c -> {
                List<FormattedCharSequence> lines = new ArrayList<>();
                List<Binding> on = byKey.getOrDefault(nameOf(c.code()), List.of());
                lines.add(Component.literal(c.label()).getVisualOrderText());
                if (on.isEmpty()) lines.add(t("tip.free").getVisualOrderText());
                if (on.size() > 1) lines.add(t("tip.shared").getVisualOrderText());
                for (int i = 0; i < Math.min(on.size(), 4); i++) lines.add(Component.literal(on.get(i).action()).getVisualOrderText());
                if (on.size() > 4) lines.add(Component.literal("+" + (on.size() - 4)).getVisualOrderText());
                lines.add(t("tip.click").getVisualOrderText());
                g.setTooltipForNextFrame(font, lines, mx, my);
            });
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            KeyboardLayout.at(cells(), originX(), originY(), scale(), event.x(), event.y()).ifPresent(c -> openKey(c.code()));
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    // --- probe / client test ------------------------------------------------------------------------------------------

    /** Scrolls the action {@code id} into view (list views); false when it is not in the list. */
    public boolean probeShow(String id) {
        return list != null && list.show(id);
    }

    /**
     * Rectangles (x, y, w, h, GUI coordinates) of the named widgets, the key and reset button of {@code rowId} when its row
     * is in view, the first group's fold/reset buttons and the keyboard cell of {@code keyCode}.
     */
    public Map<String, int[]> probeRects(String rowId, int keyCode) {
        Map<String, int[]> out = new LinkedHashMap<>();
        probe.forEach((name, w) -> out.put(name, rect(w)));
        if (list != null) {
            out.put("list", rect(list));
            KeyRow row = rowId == null ? null : list.row(rowId);
            if (row != null && row.change.getY() >= list.getY() && row.change.getBottom() <= list.getBottom()) {
                out.put("key_button", rect(row.change));
                out.put("reset_button", rect(row.reset));
            }
            HeaderRow head = list.header(0);
            if (head != null && head.fold.getY() >= list.getY() && head.fold.getBottom() <= list.getBottom()) {
                out.put("fold_first", rect(head.fold));
                out.put("reset_first_group", rect(head.reset));
            }
        }
        if (probe.get("keyboard") instanceof KeyboardWidget kw && keyCode >= 0) {
            int[] r = kw.cellRect(keyCode);
            if (r != null) out.put("cell", r);
        }
        return out;
    }

    /** Values the probe and the client test read. */
    public Map<String, Object> probeValues() {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("view", view.name());
        v.put("onlyConflicts", onlyConflicts);
        v.put("onlyUnbound", onlyUnbound);
        v.put("armedReset", armedReset);
        v.put("listening", listening == null ? "" : listening.getName());
        v.put("pickCode", pickCode);
        v.put("size", size.percent);
        v.put("german", german);
        v.put("collapsed", collapsed.size());
        v.put("rows", list == null ? 0 : list.children().size());
        v.put("query", query);
        return v;
    }

    static int[] rect(AbstractWidget w) {
        return new int[]{w.getX(), w.getY(), w.getWidth(), w.getHeight()};
    }
}
