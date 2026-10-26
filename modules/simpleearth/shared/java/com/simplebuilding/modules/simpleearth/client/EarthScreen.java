package com.simplebuilding.modules.simpleearth.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.modules.simpleearth.config.EarthClientConfig;
import com.simplebuilding.modules.simpleearth.config.EarthSettings;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * The Simple Earth menu (key O or the dev button): a box at the right edge that leaves the world visible and does not pause -
 * the movement keys keep working while the mouse works the menu (the pattern of the Octant screen). The favourite versions
 * (V5, V13-V24) are direct buttons in one row, V1-V12 sit in a drop-down (E29-6); the scroll list shows the display settings of all versions first, then that version's values.
 *
 * <p>WF25-2/WG26-3 (owner 26.10.: only the real Vanilla buttons reacted, never the self-drawn list): every control is a real
 * Vanilla widget - {@link AbstractSliderButton} for numbers, {@link CycleButton} for on/off - in a real Vanilla scroll list
 * ({@link ContainerObjectSelectionList}); clicks, drags, keys and the wheel take the standard Vanilla way, nothing is
 * hit-tested by hand. Vanilla inventory palette: #404040 on #C6C6C6 = 5.7:1, on the list well #D6D6D6 = 7.1:1; secondary
 * #4A4A4A = 5.2:1 / 6.1:1.
 */
public final class EarthScreen extends Screen implements MenuProbe.Target {
    static final int LABEL = 0xFF404040, SECONDARY_TEXT = 0xFF4A4A4A, TRACK = 0xFF8B8B8B;
    /** Panel (UiPalette.INVENTORY fill) and the lighter list well the rows sit on. */
    static final int PANEL_BG = 0xFFC6C6C6, LIST_BG = 0xFFD6D6D6;
    static final int ROW = 24, PAD = 8;
    /** Panel width: 300 (E29-6, room for the favourites row), less in very narrow windows. */
    private int W = 300;
    private int panelX, panelY, panelH, listTop, listBottom, descY;
    private SettingsList list;
    private final List<Button> versionButtons = new ArrayList<>();
    private Button resetButton, doneButton;
    private CycleButton<Integer> olderVersions;
    private long lastSend;
    private boolean serverDirty;

    public EarthScreen() {
        super(Component.translatable("simpleearth.screen.title"));
    }

    @Override
    protected void init() {
        W = Math.min(300, width - 12);
        panelH = Math.min(height - 12, 320);
        panelX = width - W - 6;
        panelY = Math.max(6, (height - panelH) / 2);
        // E31-5: every setting in ONE list right under the title; the version choice and its description below it
        int by = panelY + panelH - 26;
        descY = by - 34;
        int dropY = descY - 23, favY = dropY - 21;
        versionButtons.clear();
        // E29-6: the favourites (V5 and the new V13-V24) as direct buttons in one row, V1-V12 in a drop-down below
        int[] fav = EarthSettings.FAVOURITES;
        int bx = panelX + PAD, bw = (W - 2 * PAD) / fav.length;
        for (int k = 0; k < fav.length; k++) {
            int ver = fav[k];
            Button b = Button.builder(Component.literal("V" + ver), btn -> select(ver)).bounds(bx + k * bw, favY, bw - 1, 20)
                    .tooltip(Tooltip.create(Component.translatable("simpleearth.version." + ver + ".name")
                            .append("\n").append(Component.translatable("simpleearth.version." + ver + ".desc")))).build();
            versionButtons.add(addRenderableWidget(b));
        }
        List<Integer> older = new ArrayList<>();
        older.add(0);
        // E30-4: every version that is not a favourite (V1-V24) sits in the drop-down
        for (int v = 1; v <= EarthSettings.VERSIONS; v++) if (!favourite(v)) older.add(v);
        int now = favourite(EarthClientConfig.version) ? 0 : EarthClientConfig.version;
        olderVersions = addRenderableWidget(CycleButton.builder((Integer v) -> Component.literal(v == 0 ? "-" : "V" + v), now)
                .withValues(older)
                .create(panelX + PAD, dropY, W - 2 * PAD, 20, Component.translatable("simpleearth.screen.older"), (button, v) -> {
                    if (v != 0) select(v);
                }));
        olderVersions.setTooltip(Tooltip.create(Component.translatable("simpleearth.screen.older.tip")));
        resetButton = addRenderableWidget(Button.builder(Component.translatable("simpleearth.screen.reset"), b -> {
            EarthClientConfig.resetVersion(EarthClientConfig.version);
            EarthClientConfig.resetRender();
            changed(null);
            rebuild();
        }).bounds(panelX + PAD, by, 96, 20).tooltip(Tooltip.create(Component.translatable("simpleearth.screen.reset.tip"))).build());
        doneButton = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(panelX + W - PAD - 96, by, 96, 20).build());
        listTop = panelY + 22;
        listBottom = favY - 6;
        list = addRenderableWidget(new SettingsList(minecraft, W - 2 * PAD, Math.max(ROW, listBottom - listTop), listTop));
        list.setX(panelX + PAD);
        rebuild();
    }

    private static boolean favourite(int v) {
        for (int f : EarthSettings.FAVOURITES) if (f == v) return true;
        return false;
    }

    private void select(int version) {
        EarthClientConfig.version = version;
        EarthClient.resetView();
        rebuild();
        list.setScrollAmount(0);
        changed(EarthSettings.find(version, "edge"));
    }

    /** The list for the chosen version: display settings of all versions, the shared values, the version's own values. */
    private void rebuild() {
        double keep = list.scrollAmount();
        List<Row> entries = new ArrayList<>();
        int v = EarthClientConfig.version;
        // one block: the important shared values first (edge, render height, blend band, detail ...), the version's own values,
        // then the display values
        for (EarthSettings.Setting s : EarthSettings.forVersion(v)) entries.add(control(s, false));
        for (EarthSettings.Setting s : EarthSettings.RENDER) entries.add(control(s, true));
        list.replaceEntries(entries);
        list.setScrollAmount(keep);
        for (int i = 0; i < versionButtons.size(); i++) versionButtons.get(i).active = EarthSettings.FAVOURITES[i] != v;
        if (olderVersions != null && olderVersions.getValue() != (favourite(v) ? 0 : v)) olderVersions.setValue(favourite(v) ? 0 : v);
    }

    /** A Vanilla widget for one setting: on/off button for toggles, slider for numbers; changes apply at once. */
    private Row control(EarthSettings.Setting s, boolean global) {
        Component label = Component.translatable("text.autoconfig.simpleearth.option." + s.key());
        Tooltip tip = Tooltip.create(Component.translatable("text.autoconfig.simpleearth.option." + s.key() + ".@Tooltip"));
        AbstractWidget widget;
        if (s.kind() == EarthSettings.Kind.TOGGLE) {
            widget = CycleButton.onOffBuilder(value(s, global) >= 0.5).create(0, 0, 100, 20, label,
                    (button, on) -> store(s, global, on ? 1 : 0));
        } else {
            widget = new SettingSlider(s, global, label);
        }
        widget.setTooltip(tip);
        return new Row(null, widget, s);
    }

    /** A number setting as a Vanilla slider: position = share of the range, snapped to the setting's step. */
    private final class SettingSlider extends AbstractSliderButton {
        private final EarthSettings.Setting setting;
        private final boolean global;
        private final Component label;

        SettingSlider(EarthSettings.Setting setting, boolean global, Component label) {
            super(0, 0, 100, 20, Component.empty(), fraction(setting, value(setting, global)));
            this.setting = setting;
            this.global = global;
            this.label = label;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (setting == null) return; // called by the super constructor before the fields are set
            setMessage(Component.empty().append(label).append(": " + format(setting, value(setting, global))));
        }

        @Override
        protected void applyValue() {
            store(setting, global, fromFraction(setting, value));
        }
    }

    private static double fraction(EarthSettings.Setting s, double v) {
        if (s.kind() == EarthSettings.Kind.LOG) return Math.max(0, Math.min(1, Math.log(v / s.min()) / Math.log(s.max() / s.min())));
        return Math.max(0, Math.min(1, (v - s.min()) / Math.max(1e-9, s.max() - s.min())));
    }

    /** Slider position (0..1) to a value of the setting, snapped to its step (LOG: logarithmic scale). */
    static double fromFraction(EarthSettings.Setting s, double f) {
        double raw = s.kind() == EarthSettings.Kind.LOG ? s.min() * Math.pow(s.max() / s.min(), f) : s.min() + f * (s.max() - s.min());
        return s.min() + Math.round((raw - s.min()) / s.step()) * s.step();
    }

    private void changed(EarthSettings.Setting s) {
        if (s == null || s.key().equals("switchY") || s.key().equals("edge")) serverDirty = true;
        if (s == null || s.key().equals("edge")) EarthClient.resetView();
    }

    private static double value(EarthSettings.Setting s, boolean global) {
        return global ? EarthClientConfig.render(s.key()) : EarthClientConfig.get(EarthClientConfig.version, s.key());
    }

    private void store(EarthSettings.Setting s, boolean global, double value) {
        if (global) EarthClientConfig.setRender(s.key(), value);
        else EarthClientConfig.set(EarthClientConfig.version, s.key(), value);
        changed(s);
    }

    @Override
    public void tick() {
        long now = System.currentTimeMillis();
        if (serverDirty && now - lastSend > 250) {
            EarthClient.sendConfig();
            serverDirty = false;
            lastSend = now;
        }
    }

    @Override
    public void removed() {
        if (serverDirty) EarthClient.sendConfig();
        EarthClientConfig.save();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --- input: movement keys pass through (OctantScreen pattern), everything else is Vanilla ---

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (movement(input, true)) return false;
        if (EarthClient.KEY.matches(input) || minecraft.options.keyInventory.matches(input) || input.key() == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean keyReleased(KeyEvent input) {
        if (movement(input, false)) return false;
        return super.keyReleased(input);
    }

    private boolean movement(KeyEvent input, boolean down) {
        if (minecraft == null || minecraft.options == null) return false;
        var o = minecraft.options;
        for (var key : new net.minecraft.client.KeyMapping[]{o.keyUp, o.keyDown, o.keyLeft, o.keyRight, o.keyJump, o.keyShift, o.keySprint}) {
            if (key.matches(input)) {
                key.setDown(down);
                return true;
            }
        }
        return false;
    }

    // --- drawing ---

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // the world stays visible (no blur), one keeps flying while the menu is open
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        UiBoxes.box(g, panelX, panelY, W, panelH, UiPalette.INVENTORY);
        g.text(font, title, panelX + PAD, panelY + 8, LABEL, false);
        int v = EarthClientConfig.version;
        Component name = Component.translatable("simpleearth.version." + v + ".name");
        g.text(font, name, panelX + W - PAD - font.width(name), panelY + 8, SECONDARY_TEXT, false);
        g.textWithWordWrap(font, Component.translatable("simpleearth.version." + v + ".desc"), panelX + PAD, descY, W - 2 * PAD, SECONDARY_TEXT, false);
        UiBoxes.insetColored(g, panelX + PAD - 1, listTop - 2, W - 2 * PAD + 2, listBottom - listTop + 4, UiPalette.INVENTORY, LIST_BG);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    static String format(EarthSettings.Setting s, double value) {
        if (s.kind() == EarthSettings.Kind.TOGGLE) return Component.translatable(value >= 0.5 ? "options.on" : "options.off").getString();
        if (s.kind() == EarthSettings.Kind.INT || s.kind() == EarthSettings.Kind.LOG) return String.format(Locale.ROOT, "%,d", Math.round(value)).replace(',', ' ');
        return String.format(Locale.ROOT, "%.2f", value);
    }

    // --- the Vanilla scroll list ---

    /** Vanilla scroll list on the panel's list well (own background, no in-world dark band). */
    static final class SettingsList extends ContainerObjectSelectionList<Row> {
        SettingsList(Minecraft mc, int width, int height, int y) {
            super(mc, width, height, y, ROW);
        }

        @Override
        public int getRowWidth() {
            return getWidth() - 12;
        }

        @Override
        public int getRowLeft() {
            return getX() + 2;
        }

        @Override
        protected int scrollBarX() {
            return getX() + getWidth() - 6;
        }

        @Override
        protected void extractListBackground(GuiGraphicsExtractor g) {
        }

        @Override
        protected void extractListSeparators(GuiGraphicsExtractor g) {
        }

        void show(Row e) {
            scrollToEntry(e);
        }
    }

    /** One row: a heading ({@code widget == null}) or one Vanilla widget spanning the row. */
    static final class Row extends ContainerObjectSelectionList.Entry<Row> {
        final Component heading;
        final AbstractWidget widget;
        final EarthSettings.Setting setting;

        Row(Component heading, AbstractWidget widget, EarthSettings.Setting setting) {
            this.heading = heading;
            this.widget = widget;
            this.setting = setting;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovered, float delta) {
            if (widget == null) {
                var font = Minecraft.getInstance().font;
                g.text(font, heading, getContentX() + 1, getContentY() + 8, LABEL, false);
                g.fill(getContentX() + 1, getContentY() + 18, getContentX() + getContentWidth(), getContentY() + 19, TRACK);
                return;
            }
            widget.setX(getContentX());
            widget.setY(getContentY() + Math.max(0, (getContentHeight() - widget.getHeight()) / 2));
            widget.setWidth(getContentWidth());
            widget.extractRenderState(g, mouseX, mouseY, delta);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return widget == null ? List.of() : List.of(widget);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return widget == null ? List.of() : List.of(widget);
        }
    }

    // --- menu probe and client tests (rectangles in GUI coordinates) ---

    @Override
    public int[] probeVersionButton(int v) {
        Button b = null;
        for (int i = 0; i < EarthSettings.FAVOURITES.length; i++) if (EarthSettings.FAVOURITES[i] == v) b = versionButtons.get(i);
        if (b == null) {
            // older versions: the drop-down
            return new int[]{olderVersions.getX(), olderVersions.getY(), olderVersions.getWidth(), olderVersions.getHeight()};
        }
        return new int[]{b.getX(), b.getY(), b.getWidth(), b.getHeight()};
    }

    @Override
    public int[] probeButton(boolean reset) {
        Button b = reset ? resetButton : doneButton;
        return new int[]{b.getX(), b.getY(), b.getWidth(), b.getHeight()};
    }

    private Row entry(String key) {
        for (Row e : list.children()) if (e.setting != null && e.setting.key().equals(key)) return e;
        return null;
    }

    @Override
    public int[] probeControl(String key) {
        Row e = entry(key);
        if (e == null || e.getY() < list.getY() || e.getY() + e.getHeight() > list.getY() + list.getHeight()) return null;
        AbstractWidget w = e.widget;
        return new int[]{w.getX(), w.getY(), w.getWidth(), w.getHeight()};
    }

    @Override
    public void probeScrollTo(String key) {
        Row e = entry(key);
        if (e != null) list.show(e);
    }

    @Override
    public int[] probeList() {
        return new int[]{list.getX(), list.getY(), list.getWidth(), list.getHeight()};
    }

    @Override
    public double probeScroll() {
        return list.scrollAmount();
    }
}
