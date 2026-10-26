package com.simplebuilding.modules.simpleearth.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.modules.simpleearth.config.EarthClientConfig;
import com.simplebuilding.modules.simpleearth.config.EarthSettings;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * The Simple Earth menu (key O or the dev button): a box at the right edge that leaves the world visible and does not pause -
 * the movement keys keep working while the mouse works the menu (the pattern of the Octant screen). Pick a version V1-V12
 * (two rows); the scroll list shows the display settings of all versions first, then that version's values as sliders (drag,
 * click, Shift + wheel steps; on/off values toggle on a click), every change applies live.
 * Vanilla inventory palette: #404040 on #C6C6C6 = 5.7:1, on the list well #D6D6D6 = 7.1:1; secondary #4A4A4A = 5.2:1 / 6.1:1.
 */
public final class EarthScreen extends Screen {
    static final int LABEL = 0xFF404040, SECONDARY_TEXT = 0xFF4A4A4A, TRACK = 0xFF8B8B8B, TRACK_DARK = 0xFF373737, BAR = 0xFF4E7A3A;
    /** Panel (UiPalette.INVENTORY fill) and the lighter list well the rows sit on. */
    static final int PANEL_BG = 0xFFC6C6C6, LIST_BG = 0xFFD6D6D6;
    static final int W = 214, ROW = 24, PAD = 8;
    private int panelX, panelY, panelH, listTop, listBottom;
    private double scroll;
    private int dragging = -1;
    private final List<Row> rows = new ArrayList<>();
    private final List<Button> versionButtons = new ArrayList<>();
    private Button resetButton, doneButton;
    private boolean missLogged, polledDown, pollHandled, polledLogged, scrollDragging;
    private long lastEventPress, pollPressAt;
    private int polled;
    private long lastSend;
    private boolean serverDirty;

    /** A list row: a setting (of all versions when {@code global}), or a heading (setting == null). */
    private record Row(EarthSettings.Setting setting, Component heading, boolean global) {}

    public EarthScreen() {
        super(Component.translatable("simpleearth.screen.title"));
    }

    @Override
    protected void init() {
        panelH = Math.min(height - 12, 320);
        panelX = width - W - 6;
        panelY = Math.max(6, (height - panelH) / 2);
        versionButtons.clear();
        int bx = panelX + PAD + 1, perRow = (EarthSettings.VERSIONS + 1) / 2, bw = (W - 2 * PAD) / perRow;
        for (int v = 1; v <= EarthSettings.VERSIONS; v++) {
            int ver = v, col = (v - 1) % perRow, line = (v - 1) / perRow;
            Button b = Button.builder(Component.literal("V" + v), btn -> select(ver)).bounds(bx + col * bw, panelY + 20 + line * 21, bw - 1, 20)
                    .tooltip(Tooltip.create(Component.translatable("simpleearth.version." + v + ".name")
                            .append("\n").append(Component.translatable("simpleearth.version." + v + ".desc")))).build();
            versionButtons.add(addRenderableWidget(b));
        }
        int by = panelY + panelH - 26;
        resetButton = addRenderableWidget(Button.builder(Component.translatable("simpleearth.screen.reset"), b -> {
            EarthClientConfig.resetVersion(EarthClientConfig.version);
            EarthClientConfig.resetRender();
            changed(null);
        }).bounds(panelX + PAD, by, 96, 20).tooltip(Tooltip.create(Component.translatable("simpleearth.screen.reset.tip"))).build());
        doneButton = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(panelX + W - PAD - 96, by, 96, 20).build());
        listTop = panelY + 104;
        listBottom = by - 6;
        rebuild();
    }

    private void select(int version) {
        EarthClientConfig.version = version;
        EarthClient.resetView();
        scroll = 0;
        rebuild();
        changed(EarthSettings.find(version, "edge"));
    }

    private void rebuild() {
        rows.clear();
        int v = EarthClientConfig.version;
        rows.add(new Row(null, Component.translatable("simpleearth.screen.render"), true));
        for (EarthSettings.Setting s : EarthSettings.RENDER) rows.add(new Row(s, null, true));
        rows.add(new Row(null, Component.translatable("simpleearth.screen.shared"), false));
        int shared = EarthSettings.SHARED.size();
        List<EarthSettings.Setting> all = EarthSettings.forVersion(v);
        for (int i = 0; i < all.size(); i++) {
            if (i == shared) rows.add(new Row(null, Component.translatable("simpleearth.screen.own", "V" + v), false));
            rows.add(new Row(all.get(i), null, false));
        }
        for (int i = 0; i < versionButtons.size(); i++) versionButtons.get(i).active = i + 1 != v;
    }

    private void changed(EarthSettings.Setting s) {
        if (s == null || s.key().equals("activationY") || s.key().equals("switchY") || s.key().equals("edge")) {
            serverDirty = true;
        }
        if (s == null || s.key().equals("edge")) EarthClient.resetView();
    }

    private double value(Row row) {
        return row.global() ? EarthClientConfig.render(row.setting().key()) : EarthClientConfig.get(EarthClientConfig.version, row.setting().key());
    }

    private void store(Row row, double value) {
        if (row.global()) EarthClientConfig.setRender(row.setting().key(), value);
        else EarthClientConfig.set(EarthClientConfig.version, row.setting().key(), value);
        changed(row.setting());
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

    // --- input: movement keys pass through (OctantScreen pattern) ---

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (movement(input, true)) return false;
        // the list also scrolls with the arrow and page keys (WG26-3: works whatever happens to the mouse wheel)
        int k = input.key();
        if (k == InputConstants.KEY_UP || k == InputConstants.KEY_DOWN || k == InputConstants.KEY_PAGEUP || k == InputConstants.KEY_PAGEDOWN) {
            double d = (k == InputConstants.KEY_UP ? -1 : k == InputConstants.KEY_DOWN ? 1 : k == InputConstants.KEY_PAGEUP ? -6 : 6) * ROW;
            scroll = Math.max(0, Math.min(maxScroll(), scroll + d));
            return true;
        }
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

    // --- test hooks (client game test WD25-23: real mouse events at these GUI points) ---

    /** Centre of the version button {@code v}. */
    public int[] testVersionButton(int v) {
        Button b = versionButtons.get(v - 1);
        return new int[]{b.getX() + b.getWidth() / 2, b.getY() + b.getHeight() / 2};
    }

    /** A point on the slider track of {@code key} at {@code fraction} (0..1); scrolls the row into view first. */
    public int[] testSlider(String key, double fraction) {
        int i = testRow(key);
        int y = rowY(i) + 13 + 3, tx = trackX(), tw = trackW();
        return new int[]{tx + (int) Math.round(fraction * tw), y};
    }

    /** The label point of the row {@code key} (toggles flip on a click anywhere on the row); scrolls it into view. */
    public int[] testRowPoint(String key) {
        int i = testRow(key);
        return new int[]{panelX + W / 2, rowY(i) + 6};
    }

    /** Middle of the scroll list. */
    public int[] testListCentre() {
        return new int[]{panelX + W / 2, (listTop + listBottom) / 2};
    }

    /** Centre of the reset (true) or done (false) button. */
    public int[] testButton(boolean reset) {
        Button b = reset ? resetButton : doneButton;
        return new int[]{b.getX() + b.getWidth() / 2, b.getY() + b.getHeight() / 2};
    }

    /** Diagnosis for the client test: what the Vanilla dispatch finds at a point. */
    public String testChildAt(double x, double y) {
        return getChildAt(x, y).map(c -> c.getClass().getSimpleName() + (c instanceof Button b ? " " + b.getMessage().getString() + " active=" + b.active : "")).orElse("none")
                + ", children " + children().size();
    }

    public double testScroll() {
        return scroll;
    }

    private int testRow(String key) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).setting() != null && rows.get(i).setting().key().equals(key)) {
                scroll = Math.max(0, Math.min(maxScroll(), i * ROW - ROW));
                return i;
            }
        }
        throw new IllegalArgumentException(key);
    }

    private int rowY(int index) {
        return listTop + index * ROW - (int) scroll;
    }

    private int maxScroll() {
        return Math.max(0, rows.size() * ROW - (listBottom - listTop));
    }

    private int trackX() {
        return panelX + PAD + 14;
    }

    private int trackW() {
        return W - 2 * PAD - 28;
    }

    /** Index of the slider row under the mouse, or -1. */
    private int rowAt(double mx, double my) {
        if (mx < panelX + PAD || mx > panelX + W - PAD || my < listTop || my >= listBottom) return -1;
        int i = (int) ((my - listTop + scroll) / ROW);
        return i >= 0 && i < rows.size() && rows.get(i).setting() != null ? i : -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && pressAt(event.x(), event.y(), event)) {
            lastEventPress = System.currentTimeMillis();
            return true;
        }
        if (!missLogged && event.x() >= panelX && event.x() <= panelX + W && event.y() >= panelY && event.y() <= panelY + panelH) {
            // WF25-2 diagnosis for players whose clicks miss (other mods moving input or drawing): one line in latest.log per menu
            missLogged = true;
            com.simplebuilding.modules.simpleearth.SimpleEarth.LOG.info("Simple Earth menu: click at {}/{} (GUI {}x{}, scale {}) hit no control; panel x {}..{}, list y {}..{}, scroll {}",
                    (int) event.x(), (int) event.y(), width, height, minecraft.getWindow().getGuiScale(), panelX, panelX + W, listTop, listBottom, (int) scroll);
        }
        return super.mouseClicked(event, doubleClick);
    }

    /**
     * A left press at a GUI point: the panel's buttons, toggles, slider steps and tracks, the scroll bar. Called by
     * {@link #mouseClicked} and by {@link #pollMouse} (WG26-3: in the owner's full pack the screen never got the press for
     * anything but the version buttons - the menu now also reads the real mouse button itself each frame).
     */
    private boolean pressAt(double x, double y, net.minecraft.client.input.InputWithModifiers input) {
        List<Button> own = new ArrayList<>(versionButtons);
        own.add(resetButton);
        own.add(doneButton);
        for (Button b : own) {
            if (b != null && b.active && b.visible && x >= b.getX() && x < b.getX() + b.getWidth() && y >= b.getY() && y < b.getY() + b.getHeight()) {
                b.playDownSound(minecraft.getSoundManager());
                if (input != null) b.onPress(input);
                else b.onPress(new MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(0, 0)));
                return true;
            }
        }
        if (maxScroll() > 0 && x >= panelX + W - PAD - 6 && x <= panelX + W - PAD + 2 && y >= listTop && y < listBottom) {
            scrollDragging = true;
            scrollTo(y);
            return true;
        }
        int i = rowAt(x, y);
        if (i < 0) return false;
        Row row = rows.get(i);
        if (row.setting().kind() == EarthSettings.Kind.TOGGLE) {
            store(row, value(row) >= 0.5 ? 0 : 1);
            return true;
        }
        int tx = trackX(), tw = trackW();
        // WF25-2: the whole row height counts (it was only the 12 px band round the track)
        if (x >= panelX + PAD && x < tx - 2) {
            step(row, -1);
            return true;
        }
        if (x > tx + tw + 2) {
            step(row, 1);
            return true;
        }
        if (x >= tx && x <= tx + tw) {
            dragging = i;
            dragTo(row, x);
            return true;
        }
        return false;
    }

    private void scrollTo(double y) {
        double f = Math.max(0, Math.min(1, (y - listTop) / Math.max(1, listBottom - listTop)));
        scroll = f * maxScroll();
    }

    /**
     * WG26-3: the real mouse (GLFW, independent of every mod between the window and this screen), once a frame. A press the
     * screen did not get as an event within 150 ms is handled here; drag and release likewise.
     */
    private void pollMouse() {
        if (minecraft == null) return;
        long handle = minecraft.getWindow().handle();
        boolean down = org.lwjgl.glfw.GLFW.glfwGetMouseButton(handle, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        double[] cx = new double[1], cy = new double[1];
        org.lwjgl.glfw.GLFW.glfwGetCursorPos(handle, cx, cy);
        var w = minecraft.getWindow();
        double x = cx[0] * w.getGuiScaledWidth() / Math.max(1, w.getScreenWidth()), y = cy[0] * w.getGuiScaledHeight() / Math.max(1, w.getScreenHeight());
        long now = System.currentTimeMillis();
        if (down && !polledDown) {
            pollPressAt = now;
            pollHandled = false;
        }
        if (down && !pollHandled && now - pollPressAt >= 50 && now - lastEventPress > 150) {
            pollHandled = true;
            if (pressAt(x, y, null)) {
                polled++;
                if (!polledLogged) {
                    polledLogged = true;
                    com.simplebuilding.modules.simpleearth.SimpleEarth.LOG.info("Simple Earth menu: a click reached the menu only through the mouse poll (another mod took the event)");
                }
            }
        }
        if (down && pollHandled && now - lastEventPress > 150) {
            if (dragging >= 0 && dragging < rows.size()) dragTo(rows.get(dragging), x);
            if (scrollDragging) scrollTo(y);
        }
        if (!down && polledDown && now - lastEventPress > 150) {
            dragging = -1;
            scrollDragging = false;
        }
        polledDown = down;
    }

    /** Clicks handled by the poll (client test). */
    public int testPolled() {
        return polled;
    }

    /** Test: the poll path for a press at a GUI point (the event path skipped). */
    public boolean testPressByPoll(double x, double y) {
        return pressAt(x, y, null);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (scrollDragging) {
            scrollTo(event.y());
            return true;
        }
        if (dragging >= 0 && dragging < rows.size()) {
            dragTo(rows.get(dragging), event.x());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = -1;
        scrollDragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int i = rowAt(mx, my);
        if (i >= 0 && (InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT))) {
            step(rows.get(i), sy > 0 ? 1 : -1);
            return true;
        }
        if (mx >= panelX && mx <= panelX + W && my >= listTop && my < listBottom) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - sy * ROW));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    private void step(Row row, int dir) {
        store(row, value(row) + dir * row.setting().step());
    }

    private void dragTo(Row row, double mx) {
        EarthSettings.Setting s = row.setting();
        double f = Math.max(0, Math.min(1, (mx - trackX()) / trackW()));
        double raw = s.min() + f * (s.max() - s.min());
        store(row, s.min() + Math.round((raw - s.min()) / s.step()) * s.step());
    }

    // --- drawing ---

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // the world stays visible (no blur), one keeps flying while the menu is open
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        pollMouse();
        UiBoxes.box(g, panelX, panelY, W, panelH, UiPalette.INVENTORY);
        g.text(font, title, panelX + PAD, panelY + 8, LABEL, false);
        int v = EarthClientConfig.version;
        Component name = Component.translatable("simpleearth.version." + v + ".name");
        g.text(font, name, panelX + W - PAD - font.width(name), panelY + 8, SECONDARY_TEXT, false);
        g.textWithWordWrap(font, Component.translatable("simpleearth.version." + v + ".desc"), panelX + PAD, panelY + 64, W - 2 * PAD, SECONDARY_TEXT, false);
        UiBoxes.insetColored(g, panelX + PAD - 1, listTop - 2, W - 2 * PAD + 2, listBottom - listTop + 4, UiPalette.INVENTORY, LIST_BG);
        g.enableScissor(panelX + PAD, listTop, panelX + W - PAD, listBottom);
        Row hovered = null;
        for (int i = 0; i < rows.size(); i++) {
            int y = rowY(i);
            if (y + ROW < listTop || y > listBottom) continue;
            Row row = rows.get(i);
            if (row.setting() == null) {
                g.text(font, row.heading(), panelX + PAD + 3, y + 9, LABEL, false);
                g.fill(panelX + PAD + 3, y + 19, panelX + W - PAD - 3, y + 20, TRACK);
                continue;
            }
            EarthSettings.Setting s = row.setting();
            double value = value(row);
            Component label = Component.translatable("text.autoconfig.simpleearth.option." + s.key());
            g.text(font, label, panelX + PAD + 3, y + 2, LABEL, false);
            String shown = format(s, value);
            g.text(font, shown, panelX + W - PAD - 3 - font.width(shown), y + 2, LABEL, false);
            if (mouseX >= panelX + PAD && mouseX <= panelX + W - PAD && mouseY >= Math.max(y, listTop) && mouseY < Math.min(y + ROW, listBottom)) hovered = row;
            if (s.kind() == EarthSettings.Kind.TOGGLE) {
                // on/off: a Vanilla-style check box under the label, a click anywhere on the row toggles
                int bx = trackX(), by = y + 11;
                UiBoxes.inset(g, bx, by, 10, 10, UiPalette.INVENTORY);
                if (value >= 0.5) g.fill(bx + 2, by + 2, bx + 8, by + 8, BAR);
                continue;
            }
            int ty = y + 13, tx = trackX(), tw = trackW();
            g.text(font, "-", panelX + PAD + 4, ty - 1, LABEL, false);
            g.text(font, "+", tx + tw + 5, ty - 1, LABEL, false);
            g.fill(tx, ty, tx + tw, ty + 6, TRACK);
            g.fill(tx, ty, tx + tw, ty + 1, TRACK_DARK);
            g.fill(tx, ty, tx + 1, ty + 6, TRACK_DARK);
            g.fill(tx, ty + 5, tx + tw, ty + 6, 0xFFFFFFFF);
            double f = (value - s.min()) / Math.max(1e-9, s.max() - s.min());
            int fx = tx + 1 + (int) Math.round(f * (tw - 2));
            g.fill(tx + 1, ty + 1, fx, ty + 5, BAR);
            UiBoxes.raised(g, Math.max(tx, fx - 2), ty - 2, 5, 10, 0xFFC6C6C6);
        }
        g.disableScissor();
        if (maxScroll() > 0) {
            int barH = Math.max(12, (listBottom - listTop) * (listBottom - listTop) / (rows.size() * ROW));
            int barY = listTop + (int) ((listBottom - listTop - barH) * scroll / maxScroll());
            g.fill(panelX + W - PAD - 2, barY, panelX + W - PAD, barY + barH, SECONDARY_TEXT);
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
        if (hovered != null && dragging < 0) {
            g.setTooltipForNextFrame(font, Component.translatable("text.autoconfig.simpleearth.option." + hovered.setting().key() + ".@Tooltip"), mouseX, mouseY);
        }
    }

    static String format(EarthSettings.Setting s, double value) {
        if (s.kind() == EarthSettings.Kind.TOGGLE) return Component.translatable(value >= 0.5 ? "options.on" : "options.off").getString();
        if (s.kind() == EarthSettings.Kind.INT) return String.format(Locale.ROOT, "%,d", Math.round(value)).replace(',', ' ');
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
