package com.simplebuilding.modules.simpleinterfaces.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplelib.api.client.keys.KeyConflicts;
import com.simplelib.api.client.keys.KeyboardEdit;
import com.simplelib.api.client.keys.KeyboardLayout;
import com.simplelib.api.client.keys.KeyboardLayout.Cell;
import com.simplelib.api.client.ui.UiKeyboard;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Editor for an own keyboard layout (b-keys34), started from the layout the keyboard picture shows: click a key to select it,
 * drag it or use the arrow buttons to move it (quarter keys), change its size and shape (rectangle / L), caption, remove it.
 * Every physical key pressed here that the layout lacks is added on the first free place; one that is there gets selected.
 * "Add key" takes the next key including Esc. Edits that would overlap or leave the grid are refused with a red hint
 * ({@link KeyboardEdit}); saving needs a valid name. All widgets are Vanilla widgets, so the real mouse works everywhere.
 */
public final class KeysEditorScreen extends net.minecraft.client.gui.screens.Screen {
    private static final String K = "simpleinterfaces.keys.";
    private static final int TOP = 78;

    private final KeysScreen parent;
    private final String origin;
    private List<Cell> cells;
    private int selected = -1;
    private boolean capture;
    private Component message;
    private boolean error;
    private boolean syncing;
    private EditBox name, caption;
    private int scale = 4, ox, oy;
    /** Dragging: key, pointer start and the cap at the start. */
    private int dragCode = -1;
    private double dragX, dragY;
    private Cell dragFrom;
    final Map<String, AbstractWidget> probe = new LinkedHashMap<>();

    public KeysEditorScreen(KeysScreen parent, String origin, List<Cell> start) {
        super(Component.translatable(K + "edit.title"));
        this.parent = parent;
        this.origin = origin;
        this.cells = new ArrayList<>(start);
    }

    private static Component t(String key, Object... args) {
        return Component.translatable(K + key, args);
    }

    private String suggestion() {
        if (KeyboardStore.isCustom(origin)) return origin;
        String base = "My " + KeyboardStore.shortName(origin);
        String out = base;
        for (int i = 2; KeyboardStore.exists(out); i++) out = base + " " + i;
        return out;
    }

    @Override
    protected void init() {
        probe.clear();
        int cx = width / 2;
        String keepName = name == null ? suggestion() : name.getValue();
        name = new EditBox(font, cx - 154, 20, 150, 20, t("edit.name"));
        name.setMaxLength(KeyboardEdit.MAX_NAME);
        name.setHint(t("edit.name"));
        name.setValue(keepName);
        probe.put("name", addRenderableWidget(name));
        caption = new EditBox(font, cx + 4, 20, 70, 20, t("edit.label"));
        caption.setMaxLength(KeyboardEdit.MAX_LABEL);
        caption.setHint(t("edit.label"));
        caption.setResponder(v -> {
            if (syncing) return;
            Cell c = selectedCell();
            if (c != null) set(c.withLabel(v));
        });
        probe.put("label", addRenderableWidget(caption));
        syncCaption();
        int bx = cx - 153, y = 44;
        bx = tool("left", "<", 20, bx, y, () -> edit(c -> c.moved(-1, 0)));
        bx = tool("right", ">", 20, bx, y, () -> edit(c -> c.moved(1, 0)));
        bx = tool("up", "^", 20, bx, y, () -> edit(c -> c.moved(0, -1)));
        bx = tool("down", "v", 20, bx, y, () -> edit(c -> c.moved(0, 1)));
        bx = tool("w_minus", "W-", 24, bx, y, () -> edit(c -> resized(c, -1, 0)));
        bx = tool("w_plus", "W+", 24, bx, y, () -> edit(c -> resized(c, 1, 0)));
        bx = tool("h_minus", "H-", 24, bx, y, () -> edit(c -> resized(c, 0, -1)));
        bx = tool("h_plus", "H+", 24, bx, y, () -> edit(c -> resized(c, 0, 1)));
        bx = tool("shape", "L", 24, bx, y, () -> edit(KeysEditorScreen::reshaped));
        bx = tool("remove", "Del", 30, bx, y, this::removeSelected);
        tool("add", t("edit.add").getString(), 56, bx, y, () -> {
            capture = true;
            say(t("edit.msg.capture"), false);
        });
        int fy = height - 27;
        probe.put("save", addRenderableWidget(Button.builder(t("edit.save"), b -> save()).bounds(cx - 154, fy, 96, 20).build()));
        Button del = addRenderableWidget(Button.builder(t("edit.delete"), b -> {
            KeyboardStore.delete(origin);
            onClose();
        }).bounds(cx - 54, fy, 108, 20).tooltip(Tooltip.create(t("edit.delete.tip"))).build());
        del.active = KeyboardStore.isCustom(origin);
        probe.put("delete", del);
        probe.put("cancel", addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose()).bounds(cx + 58, fy, 96, 20).build()));
        // the picture keeps its size while editing: fitted once for the start layout plus a spare row
        int aw = width - 16, ah = height - 33 - TOP - 8;
        int w0 = Math.max(1, KeyboardLayout.width(cells)), h0 = KeyboardLayout.height(cells) + 2 * KeyboardLayout.UNIT;
        scale = Math.max(2, Math.min(5, Math.min(aw / w0, ah / h0)));
        ox = (width - w0 * scale) / 2;
        oy = TOP + 4;
    }

    private int tool(String id, String text, int w, int x, int y, Runnable action) {
        Button b = Button.builder(Component.literal(text), btn -> action.run()).bounds(x, y, w, 20)
                .tooltip(Tooltip.create(t("edit.tip." + id))).build();
        probe.put(id, addRenderableWidget(b));
        return x + w + 2;
    }

    // --- model -------------------------------------------------------------------------------------------------------

    private Cell selectedCell() {
        for (Cell c : cells) if (c.code() == selected) return c;
        return null;
    }

    private void say(Component text, boolean isError) {
        message = text;
        error = isError;
    }

    private void syncCaption() {
        syncing = true;
        Cell c = selectedCell();
        caption.setValue(c == null ? "" : c.label());
        caption.setEditable(c != null);
        syncing = false;
    }

    private void select(int code) {
        selected = code;
        syncCaption();
    }

    /** Sets the cap of the selected key to {@code next} unless that breaks a rule (then a red hint tells which). */
    private boolean set(Cell next) {
        List<Cell> trial = new ArrayList<>(cells);
        for (int i = 0; i < trial.size(); i++) if (trial.get(i).code() == next.code()) trial.set(i, next);
        Optional<KeyboardEdit.Problem> p = KeyboardEdit.problem(trial);
        if (p.isPresent()) {
            say(t("edit.err." + p.get().kind(), p.get().a(), p.get().b()), true);
            return false;
        }
        cells = trial;
        message = null;
        return true;
    }

    private void edit(UnaryOperator<Cell> change) {
        Cell c = selectedCell();
        if (c == null) {
            say(t("edit.err.none"), true);
            return;
        }
        set(change.apply(c));
    }

    private static Cell resized(Cell c, int dw, int dh) {
        return new Cell(c.code(), c.label(), c.x(), c.y(), Math.max(1, c.w() + dw), Math.max(1, c.h() + dh), c.ex(), c.ey(), c.ew(), c.eh());
    }

    /** Rectangle <-> L: the L gets a second part below, one quarter key narrower on the left (the ISO Enter). */
    private static Cell reshaped(Cell c) {
        if (c.l()) return new Cell(c.code(), c.label(), c.x(), c.y(), c.w(), c.h());
        return new Cell(c.code(), c.label(), c.x(), c.y(), c.w(), c.h(), c.x() + 1, c.y() + c.h(), Math.max(1, c.w() - 1), c.h());
    }

    private void removeSelected() {
        if (selectedCell() == null) {
            say(t("edit.err.none"), true);
            return;
        }
        cells = KeyboardEdit.remove(cells, selected);
        select(-1);
        message = null;
    }

    /** A physical key was pressed (picture code): select it, or add it on a free place when the layout lacks it. */
    public void press(int code) {
        if (code < 0) {
            say(t("edit.err.unsupported"), true);
            return;
        }
        for (Cell c : cells) {
            if (c.code() == code) {
                select(code);
                message = null;
                return;
            }
        }
        List<Cell> grown = KeyboardEdit.add(cells, code, KeyboardLayout.label(code));
        if (grown == cells) {
            say(t("edit.err.full"), true);
            return;
        }
        cells = grown;
        select(code);
        say(t("edit.msg.added", KeyboardLayout.label(code)), false);
    }

    private void save() {
        String n = name.getValue().strip();
        if (!KeyboardEdit.validName(n)) {
            say(t("edit.err.name"), true);
            return;
        }
        Optional<KeyboardEdit.Problem> p = KeyboardEdit.problem(cells);
        if (p.isPresent()) {
            say(t("edit.err." + p.get().kind(), p.get().a(), p.get().b()), true);
            return;
        }
        KeyboardStore.put(n, cells);
        onClose();
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    // --- input -------------------------------------------------------------------------------------------------------

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (caption.isFocused() || name.isFocused()) return super.keyPressed(event);
        if (event.isEscape() && !capture) {
            onClose();
            return true;
        }
        capture = false;
        press(KeyboardLayout.codeOf(InputConstants.getKey(event).getName()));
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
        setFocused(null);
        caption.setFocused(false);
        name.setFocused(false);
        Optional<Cell> hit = KeyboardLayout.at(cells, ox, oy, scale, event.x(), event.y());
        if (hit.isPresent()) {
            select(hit.get().code());
            dragCode = hit.get().code();
            dragFrom = hit.get();
            dragX = event.x();
            dragY = event.y();
            message = null;
        } else {
            select(-1);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragCode >= 0 && dragFrom != null) {
            int mx = (int) Math.round((event.x() - dragX) / scale), my = (int) Math.round((event.y() - dragY) / scale);
            Cell now = selectedCell();
            Cell target = dragFrom.moved(mx, my);
            if (now != null && (now.x() != target.x() || now.y() != target.y())) set(target);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragCode = -1;
        dragFrom = null;
        return super.mouseReleased(event);
    }

    // --- drawing -----------------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractBackground(g, mx, my, delta);
        KeysScreen.pageBox(g, width, height);
        KeysScreen.field(g, 8, TOP, width - 16, height - 33 - TOP);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        KeysScreen.centered(g, font, title, width / 2, 7, 0xFFFFFFFF);
        var hover = KeyboardLayout.at(cells, ox, oy, scale, mx, my);
        UiKeyboard.draw(g, font, cells, ox, oy, scale, c -> KeyConflicts.State.FREE, hover.map(Cell::code).orElse(-1), selected, false);
        Component line = message != null ? message : t("edit.hint");
        int tone = message == null ? 0xFFA0A0A0 : error ? 0xFFFF6060 : 0xFF80FF80;
        if (KeysScreen.boxed() && message != null) tone = error ? com.simplelib.api.client.ui.UiContrast.INK_ERROR : com.simplelib.api.client.ui.UiContrast.INK_OK;
        KeysScreen.centered(g, font, line, width / 2, 68, tone);
    }

    // --- probe / client test ------------------------------------------------------------------------------------------

    /** Rectangles (x, y, w, h, GUI coordinates) of the named widgets and the cap of {@code keyCode} (key "cell"). */
    public Map<String, int[]> probeRects(int keyCode) {
        Map<String, int[]> out = new LinkedHashMap<>();
        probe.forEach((n, w) -> out.put(n, KeysScreen.rect(w)));
        for (Cell c : cells) {
            if (c.code() == keyCode) out.put("cell", new int[]{ox + c.x() * scale, oy + c.y() * scale, c.w() * scale - 1, c.h() * scale - 1});
        }
        return out;
    }

    /** Values the probe and the client test read. */
    public Map<String, Object> probeValues() {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("keys", cells.size());
        v.put("selected", selected);
        v.put("message", message == null ? "" : message.getString());
        v.put("error", error);
        v.put("valid", KeyboardEdit.problem(cells).isEmpty());
        Cell c = selectedCell();
        v.put("cell", c == null ? "" : c.x() + "," + c.y() + "," + c.w() + "," + c.h() + (c.l() ? ",L" : ""));
        return v;
    }
}
