package com.simplebuilding.modules.simplemodels.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.modules.simplemodels.ModelExamples;
import com.simplebuilding.modules.simplemodels.editor.EditableModel;
import com.simplebuilding.modules.simplemodels.editor.EditableModel.Element;
import com.simplebuilding.modules.simplemodels.editor.Gizmo;
import com.simplebuilding.modules.simplemodels.editor.ModelEditorLayout;
import com.simplebuilding.modules.simplemodels.editor.ModelEditorLayout.Rect;
import com.simplebuilding.modules.simplemodels.editor.ModelEditorState;
import com.simplebuilding.modules.simplemodels.editor.ModelGeometry;
import com.simplebuilding.modules.simplemodels.editor.ModelJson;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiCodeArea;
import com.simplelib.api.client.ui.UiOrbitCamera;
import com.simplelib.api.client.ui.UiPalette;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * Simple Models editor (b-models13, stages 2 + 3; concept docs/ai/KONZEPT-A-MODELS11-2026-11-09.md §3). Owner choice
 * MODELS11 = A, Blockbench classic: toolbar, cube list, viewport with orbit camera and gizmo (G move / R rotate /
 * S scale), properties panel, status line; the "code only" switch (F4) shows the vanilla JSON in SimpleLib's
 * {@link UiCodeArea} next to a live preview (MODELS12 variant A). The DSL of stage 4 replaces the JSON text later.
 *
 * <p>All editing logic sits in {@link ModelEditorState} / {@link Gizmo} / {@link ModelGeometry} (server-testable);
 * this screen only maps input and draws. Shortcuts act only while no text field has the focus.
 */
public final class ModelEditorScreen extends Screen {
    static final UiPalette PALETTE = UiPalette.INVENTORY;
    /** Text on the grey panels: #404040 on #C6C6C6 = 6.1:1; on the lighter list face #D6D6D6 = 7.1:1. */
    static final int LABEL = 0xFF404040, LIST_FACE = 0xFFD6D6D6;
    /** Selected cube row: white text and eye only on the dark row fill #5A5A5A = 6.9:1 (never on LIST_FACE). */
    static final int ROW_SELECTED = 0xFF5A5A5A, ROW_SELECTED_TEXT = 0xFFFFFFFF; // contrast-ok: drawn on ROW_SELECTED only
    /** Light text on the dark viewport (#F2EEE8 on #1E1F23 = 15:1). */
    static final int VIEW_TEXT = UiPalette.LIGHT_LABEL;
    static final int ROW = 12, HANDLE = 5, GIZMO_PX = 30;
    /** Code view colours on UiCodeArea's paper (#F3EBD2): plain, key, string, number, punctuation - each >= 4.5:1. */
    static final int[] CODE_COLOURS = {0xFF3B2F20, 0xFF7A3E9D, 0xFF2E6B31, 0xFF9A3F1B, 0xFF6B5B45};
    private static final long CODE_DEBOUNCE_MS = 150;

    private enum Drag { NONE, ORBIT, PAN, GIZMO }

    private final Screen parent;
    private final ModelEditorState state;
    private final UiOrbitCamera camera = new UiOrbitCamera();
    private ModelEditorLayout layout;
    private boolean codeOnly;
    private UiCodeArea codeArea;
    private String codeText;
    private long codeChangedAt = -1;
    private final EditBox[] fields = new EditBox[9];
    /** Which number fields take input (EditBox keeps this private): origin only with a rotation. */
    private final boolean[] editable = new boolean[9];
    private final List<Button> toolbar = new ArrayList<>();
    private Button undoButton, redoButton, snapButton;
    private Drag drag = Drag.NONE;
    private int gizmoAxis;
    private boolean gizmoPositive;
    private double startX, startY;
    private String dragLabel;
    private int listScroll;

    public ModelEditorScreen(Screen parent, EditableModel model) {
        super(Component.translatable("simplemodels.editor.title"));
        this.parent = parent;
        this.state = new ModelEditorState(model);
    }

    public ModelEditorState state() { return state; }
    public UiOrbitCamera camera() { return camera; }
    public boolean codeOnly() { return codeOnly; }
    public ModelEditorLayout layout() { return layout; }

    /** Switches between classic and code only (F4 / toolbar). */
    public void setCodeOnly(boolean on) {
        if (state.dragging()) state.cancelDrag();
        codeOnly = on;
        codeText = null;
        rebuildWidgets();
    }

    // ------------------------------------------------------------ loading

    /**
     * The editable model behind a catalogue item model id ({@code ns:path}): the item definition
     * {@code assets/ns/items/path.json} names the model, whose JSON is read from the loaded resources. Falls back to the
     * built-in stool when nothing readable with cubes is found.
     */
    public static EditableModel load(String itemModelId) {
        var resources = net.minecraft.client.Minecraft.getInstance().getResourceManager();
        Identifier id = itemModelId == null ? null : Identifier.tryParse(itemModelId);
        if (id != null) {
            List<Identifier> candidates = new ArrayList<>();
            try (var reader = resources.openAsReader(id.withPath(p -> "items/" + p + ".json"))) {
                var json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("model");
                if (json != null && json.has("model")) {
                    Identifier model = Identifier.tryParse(json.get("model").getAsString());
                    if (model != null) candidates.add(model);
                }
            } catch (Exception ignored) {
                // no item definition: try the id as a model path below
            }
            candidates.add(id.withPath(p -> "item/" + p));
            candidates.add(id);
            for (Identifier model : candidates) {
                try (var reader = resources.openAsReader(model.withPath(p -> "models/" + p + ".json"))) {
                    var result = ModelJson.read(reader.lines().collect(java.util.stream.Collectors.joining("\n")));
                    // Flat item models (item/generated) have no cubes to edit: try the next candidate / the stool.
                    if (result.ok() && !result.model().elements().isEmpty()) return result.model();
                } catch (Exception ignored) {
                    // next candidate
                }
            }
        }
        try {
            var result = ModelJson.read(ModelExamples.resource("models/item/example/stool.json"));
            if (result.ok()) return result.model();
        } catch (java.io.IOException ignored) {
            // built-in example missing: start empty
        }
        return EditableModel.EMPTY;
    }

    // ------------------------------------------------------------ widgets

    private Button tool(Component label, Component tip, int x, int w, Runnable action) {
        Button b = Button.builder(label, btn -> action.run()).bounds(x, layout.toolbar().y(), w, 20).build();
        if (tip != null) b.setTooltip(Tooltip.create(tip));
        toolbar.add(b);
        return addRenderableWidget(b);
    }

    @Override
    protected void init() {
        layout = ModelEditorLayout.of(width, height, codeOnly);
        toolbar.clear();
        int x = layout.toolbar().x();
        if (!codeOnly) {
            for (Gizmo.Mode m : Gizmo.Mode.values()) {
                String letter = m == Gizmo.Mode.MOVE ? "G" : m == Gizmo.Mode.ROTATE ? "R" : "S";
                Button b = tool(Component.literal(letter), Component.translatable("simplemodels.editor.mode." + m.key), x, 20, () -> { state.mode(m); refresh(); });
                b.active = state.mode() != m;
                x += 22;
            }
            x += 4;
            snapButton = tool(snapLabel(), Component.translatable("simplemodels.editor.snap.hint"), x, 56, () -> { state.cycleSnap(); refresh(); });
            x += 60;
        }
        undoButton = tool(Component.literal("↶"), Component.translatable("simplemodels.editor.undo"), x, 20, () -> { state.undo(); afterChange(); });
        redoButton = tool(Component.literal("↷"), Component.translatable("simplemodels.editor.redo"), x + 22, 20, () -> { state.redo(); afterChange(); });
        x += 48;
        if (!codeOnly) {
            tool(Component.literal("+"), Component.translatable("simplemodels.editor.add"), x, 20, () -> { state.addCube(); afterChange(); });
            tool(Component.literal("⧉"), Component.translatable("simplemodels.editor.duplicate"), x + 22, 20, () -> { state.duplicate(); afterChange(); });
            tool(Component.literal("✕"), Component.translatable("simplemodels.editor.delete"), x + 44, 20, () -> { state.delete(); afterChange(); });
        }
        int right = layout.toolbar().right();
        tool(Component.translatable("simplemodels.editor.back"), null, right - 50, 50, this::onClose);
        tool(Component.translatable(codeOnly ? "simplemodels.editor.classic" : "simplemodels.editor.code_only"),
                Component.translatable("simplemodels.editor.code_only.hint"), right - 54 - 84, 84, () -> setCodeOnly(!codeOnly));

        if (codeOnly) {
            Rect c = layout.code();
            if (codeText == null) codeText = state.code();
            int guiScale = (int) Math.round(minecraft.getWindow().getGuiScale());
            float scale = guiScale >= 3 ? (guiScale - 1f) / guiScale : 1f;
            codeArea = new UiCodeArea(font, c.x(), c.y(), c.w(), c.h(), scale, false, codeText, this::onCode, ModelJson.MAX_CHARS,
                    CODE_COLOURS, Component.translatable("simplemodels.editor.code"));
            highlight();
            addRenderableWidget(codeArea);
        } else {
            codeArea = null;
            Rect p = layout.properties();
            if (p.visible()) {
                int boxW = (p.w() - 12) / 3;
                for (int row = 0; row < 3; row++) {
                    for (int axis = 0; axis < 3; axis++) {
                        EditBox box = new EditBox(font, p.x() + 5 + axis * (boxW + 1), p.y() + 26 + row * 26, boxW, 13,
                                Component.translatable("simplemodels.editor." + FIELD_NAMES[row]));
                        box.setMaxLength(8);
                        fields[row * 3 + axis] = addRenderableWidget(box);
                    }
                }
            } else {
                java.util.Arrays.fill(fields, null);
            }
        }
        refresh();
    }

    private static final String[] FIELD_NAMES = {"from", "to", "origin"};

    private Component snapLabel() {
        return Component.translatable("simplemodels.editor.snap", number(state.snapStep()));
    }

    /** Button states and number fields follow the state (fields only while not being typed in). */
    private void refresh() {
        if (undoButton != null) undoButton.active = state.canUndo();
        if (redoButton != null) redoButton.active = state.canRedo();
        if (snapButton != null) snapButton.setMessage(snapLabel());
        if (!codeOnly) {
            for (int i = 0; i < 3 && i < toolbar.size(); i++) toolbar.get(i).active = state.mode() != Gizmo.Mode.values()[i];
        }
        Element e = state.selectedElement();
        for (int i = 0; i < fields.length; i++) {
            EditBox box = fields[i];
            if (box == null) continue;
            int row = i / 3, axis = i % 3;
            boolean enabled = e != null && (row < 2 || e.rotation() != null);
            box.setEditable(enabled);
            editable[i] = enabled;
            box.visible = e != null;
            if (!box.isFocused()) box.setValue(enabled ? number(row == 0 ? e.from().get(axis) : row == 1 ? e.to().get(axis) : e.rotation().origin().get(axis)) : "");
        }
    }

    /** Whole numbers without ".0", else up to 3 decimals. */
    static String number(float f) {
        if (f == Math.rint(f)) return Integer.toString((int) f);
        String s = String.format(java.util.Locale.ROOT, "%.3f", f);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private void afterChange() {
        if (codeOnly && codeArea != null) {
            String code = state.code();
            if (!code.equals(codeArea.getValue())) {
                codeText = code;
                codeArea.replaceAll(code);
                codeChangedAt = -1;
            }
            highlight();
        }
        refresh();
    }

    // ------------------------------------------------------------ code view

    private void onCode(String text) {
        codeText = text;
        codeChangedAt = net.minecraft.util.Util.getMillis();
    }

    @Override
    public void tick() {
        super.tick();
        if (codeChangedAt >= 0 && net.minecraft.util.Util.getMillis() - codeChangedAt >= CODE_DEBOUNCE_MS) {
            codeChangedAt = -1;
            state.applyCode(codeText);
            highlight();
        }
        // Buttons and number fields follow every state change, also ones not made through this screen (cheap: nine fields).
        refresh();
    }

    /** JSON colouring: keys, strings, numbers, punctuation. */
    private void highlight() {
        if (codeArea == null) return;
        String s = codeArea.getValue();
        byte[] styles = new byte[s.length()];
        for (int i = 0; i < s.length(); ) {
            char c = s.charAt(i);
            if (c == '"') {
                int j = i + 1;
                while (j < s.length() && s.charAt(j) != '"' && s.charAt(j) != '\n') j += s.charAt(j) == '\\' ? 2 : 1;
                j = Math.min(s.length(), j + 1);
                int k = j;
                while (k < s.length() && s.charAt(k) == ' ') k++;
                byte style = (byte) (k < s.length() && s.charAt(k) == ':' ? 1 : 2);
                for (int n = i; n < j; n++) styles[n] = style;
                i = j;
            } else if (c == '-' || Character.isDigit(c)) {
                int j = i + 1;
                while (j < s.length() && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.' || s.charAt(j) == 'e' || s.charAt(j) == 'E')) j++;
                for (int n = i; n < j; n++) styles[n] = 3;
                i = j;
            } else {
                if ("{}[],:".indexOf(c) >= 0) styles[i] = 4;
                i++;
            }
        }
        codeArea.setProblems(styles, List.of());
    }

    // ------------------------------------------------------------ input

    private boolean typing() {
        return getFocused() instanceof EditBox box && box.isFocused() || codeArea != null && codeArea.isFocused();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape() && state.dragging()) {
            state.cancelDrag();
            drag = Drag.NONE;
            refresh();
            return true;
        }
        if (getFocused() instanceof EditBox box && box.isFocused() && event.isConfirmation()) {
            applyField(box);
            box.setFocused(false);
            setFocused(null);
            return true;
        }
        if (event.key() == InputConstants.KEY_F4) {
            setCodeOnly(!codeOnly);
            return true;
        }
        if (typing()) return super.keyPressed(event);
        boolean ctrl = event.hasControlDownWithQuirk();
        int key = event.shortcutKey();
        if (ctrl && (key == 'z' && !event.hasShiftDown())) { state.undo(); afterChange(); return true; }
        if (ctrl && (key == 'y' || key == 'z')) { state.redo(); afterChange(); return true; }
        if (!codeOnly) {
            if (ctrl && key == 'd') { state.duplicate(); afterChange(); return true; }
            if (!ctrl && key == 'g') { state.mode(Gizmo.Mode.MOVE); refresh(); return true; }
            if (!ctrl && key == 'r') { state.mode(Gizmo.Mode.ROTATE); refresh(); return true; }
            if (!ctrl && key == 's') { state.mode(Gizmo.Mode.SCALE); refresh(); return true; }
            if (!ctrl && key == 'n') { state.addCube(); afterChange(); return true; }
            if (!ctrl && key == 'h' && state.selected() >= 0) { state.toggleHidden(state.selected()); return true; }
            if (event.key() == InputConstants.KEY_DELETE) { state.delete(); afterChange(); return true; }
        }
        if (event.key() == InputConstants.KEY_NUMPAD1) { camera.view(UiOrbitCamera.View.FRONT); return true; }
        if (event.key() == InputConstants.KEY_NUMPAD3) { camera.view(UiOrbitCamera.View.RIGHT); return true; }
        if (event.key() == InputConstants.KEY_NUMPAD7) { camera.view(UiOrbitCamera.View.TOP); return true; }
        if (event.key() == InputConstants.KEY_NUMPAD0 || event.key() == InputConstants.KEY_HOME) { camera.reset(); return true; }
        return super.keyPressed(event);
    }

    private void applyField(EditBox box) {
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] != box) continue;
            try {
                state.setValue(FIELD_NAMES[i / 3], i % 3, Float.parseFloat(box.getValue().trim().replace(',', '.')));
            } catch (NumberFormatException ignored) {
                // keep the model; refresh shows the old value again
            }
            box.setFocused(false);
            afterChange();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        Rect view = layout.view();
        if (drag == Drag.GIZMO && event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            state.cancelDrag();
            drag = Drag.NONE;
            refresh();
            return true;
        }
        if (view.contains(mx, my) && getChildAt(mx, my).isEmpty()) {
            if (getFocused() != null) { getFocused().setFocused(false); setFocused(null); }
            if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                if (!codeOnly && hitGizmo(mx, my) && state.beginDrag()) {
                    drag = Drag.GIZMO;
                    startX = mx;
                    startY = my;
                    dragLabel = null;
                    return true;
                }
                if (!codeOnly) {
                    var ray = camera.ray(mx, my, view.x(), view.y(), view.w(), view.h(), ModelGeometry.VIEW_RADIUS);
                    state.select(ModelGeometry.pick(state.model(), ray.origin().add(ModelGeometry.CENTRE), ray.direction(), state.hidden()));
                    refresh();
                }
                if (doubleClick) camera.reset();
                drag = Drag.ORBIT; // left drag on empty space turns as well (like the blueprint view)
                return true;
            }
            drag = event.button() == InputConstants.MOUSE_BUTTON_MIDDLE || event.hasShiftDown() ? Drag.PAN : Drag.ORBIT;
            return true;
        }
        Rect list = layout.outliner();
        if (list.contains(mx, my) && my >= list.y() + 14) {
            int index = (int) ((my - list.y() - 14) / ROW) + listScroll;
            if (index < state.model().elements().size()) {
                if (mx < list.x() + 14) state.toggleHidden(index);
                else state.select(index);
                refresh();
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        switch (drag) {
            case ORBIT -> { camera.drag(dx, dy); return true; }
            case PAN -> { camera.pan(dx, dy); return true; }
            case GIZMO -> { gizmoDrag(event.x(), event.y()); return true; }
            default -> { return super.mouseDragged(event, dx, dy); }
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (drag == Drag.GIZMO) {
            state.endDrag();
            afterChange();
        }
        boolean was = drag != Drag.NONE;
        drag = Drag.NONE;
        dragLabel = null;
        return super.mouseReleased(event) || was;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (layout.view().contains(mx, my)) {
            camera.zoom(scrollY);
            return true;
        }
        for (int i = 0; i < fields.length; i++) {
            EditBox box = fields[i];
            if (box != null && box.visible && box.isMouseOver(mx, my) && editable[i]) {
                // Wheel on a number: +-1, Shift +-0.25, Ctrl +-4 (concept §3).
                float step = minecraft.hasShiftDown() ? 0.25f : minecraft.hasControlDown() ? 4f : 1f;
                try {
                    box.setValue(number(Float.parseFloat(box.getValue().replace(',', '.')) + (float) Math.signum(scrollY) * step));
                    applyField(box);
                } catch (NumberFormatException ignored) {
                    // not a number: leave it
                }
                return true;
            }
        }
        if (layout.outliner().contains(mx, my)) {
            int rows = Math.max(1, (layout.outliner().h() - 16) / ROW);
            listScroll = Math.clamp(listScroll - (int) Math.signum(scrollY), 0, Math.max(0, state.model().elements().size() - rows));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    // ------------------------------------------------------------ gizmo

    private static Vector3f unit(int axis) {
        return new Vector3f(axis == 0 ? 1 : 0, axis == 1 ? 1 : 0, axis == 2 ? 1 : 0);
    }

    /** Model length that appears as {@link #GIZMO_PX} GUI pixels. */
    private float gizmoLength() {
        Rect v = layout.view();
        return GIZMO_PX / camera.scale(v.w(), v.h(), ModelGeometry.VIEW_RADIUS);
    }

    /** Handle end of an axis (sign -1 = the negative end of the scale gizmo). */
    private Vector2f handle(Element e, int axis, int sign) {
        Vector3f c = ModelGeometry.centre(e);
        return ModelViewport.project(camera, layout.view(), c.add(unit(axis).mul(sign * gizmoLength())));
    }

    /** Ring of the rotate gizmo about an axis, 32 points. */
    private List<Vector2f> ring(Element e, int axis) {
        Vector3f c = ModelGeometry.centre(e);
        Vector3f u = unit((axis + 1) % 3), w = unit((axis + 2) % 3);
        float r = gizmoLength();
        List<Vector2f> out = new ArrayList<>(33);
        for (int i = 0; i <= 32; i++) {
            double t = i * Math.PI * 2 / 32;
            Vector3f p = new Vector3f(c).add(new Vector3f(u).mul((float) (Math.cos(t) * r))).add(new Vector3f(w).mul((float) (Math.sin(t) * r)));
            out.add(ModelViewport.project(camera, layout.view(), p));
        }
        return out;
    }

    /** Whether the pointer is on a gizmo handle of the selected cube; sets the axis/side to drag. */
    private boolean hitGizmo(double mx, double my) {
        Element e = state.selectedElement();
        if (e == null) return false;
        double best = 5;
        boolean hit = false;
        for (int axis = 0; axis < 3; axis++) {
            if (state.mode() == Gizmo.Mode.ROTATE) {
                List<Vector2f> ring = ring(e, axis);
                for (int i = 0; i + 1 < ring.size(); i++) {
                    double d = segmentDistance(mx, my, ring.get(i), ring.get(i + 1));
                    if (d < best) { best = d; gizmoAxis = axis; gizmoPositive = true; hit = true; }
                }
            } else {
                for (int sign : state.mode() == Gizmo.Mode.SCALE ? new int[]{1, -1} : new int[]{1}) {
                    Vector2f h = handle(e, axis, sign);
                    double d = Math.max(Math.abs(mx - h.x), Math.abs(my - h.y));
                    if (d <= HANDLE && d < best) { best = d; gizmoAxis = axis; gizmoPositive = sign > 0; hit = true; }
                }
            }
        }
        return hit;
    }

    private static double segmentDistance(double px, double py, Vector2f a, Vector2f b) {
        double dx = b.x - a.x, dy = b.y - a.y, len2 = dx * dx + dy * dy;
        double t = len2 < 1e-9 ? 0 : Math.max(0, Math.min(1, ((px - a.x) * dx + (py - a.y) * dy) / len2));
        double cx = a.x + t * dx - px, cy = a.y + t * dy - py;
        return Math.sqrt(cx * cx + cy * cy);
    }

    private void gizmoDrag(double mx, double my) {
        Element start = state.dragStartElement();
        if (start == null) return;
        Rect v = layout.view();
        boolean free = minecraft.hasAltDown();
        float step = free ? 0 : state.snapStep();
        Vector2f axis = camera.screenAxis(unit(gizmoAxis), v.w(), v.h(), ModelGeometry.VIEW_RADIUS);
        double dx = mx - startX, dy = my - startY;
        Element next;
        switch (state.mode()) {
            case MOVE -> {
                next = Gizmo.move(start, gizmoAxis, Gizmo.along(axis.x, axis.y, dx, dy), step);
                dragLabel = signed(next.from().get(gizmoAxis) - start.from().get(gizmoAxis)) + " px";
            }
            case SCALE -> {
                float sign = gizmoPositive ? 1 : -1;
                next = Gizmo.scale(start, gizmoAxis, Gizmo.along(axis.x * sign, axis.y * sign, dx, dy), step, gizmoPositive, minecraft.hasControlDown());
                float size = next.to().get(gizmoAxis) - next.from().get(gizmoAxis);
                dragLabel = number(size) + " px";
            }
            default -> {
                Vector2f c = ModelViewport.project(camera, v, ModelGeometry.centre(start));
                float degrees = Gizmo.screenAngle(c.x, c.y, startX, startY, mx, my, camera.depth(unit(gizmoAxis)) > 0);
                degrees = Gizmo.snap(degrees, free ? 0 : minecraft.hasShiftDown() ? Gizmo.FINE_ANGLE_STEP : Gizmo.ANGLE_STEP);
                next = degrees == 0 ? start : Gizmo.rotate(start, gizmoAxis, degrees);
                dragLabel = signed(degrees) + "°";
            }
        }
        state.drag(next);
    }

    private static String signed(float f) {
        return (f > 0 ? "+" : "") + number(f);
    }

    // ------------------------------------------------------------ drawing

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractBackground(g, mx, my, delta);
        UiBoxes.box(g, 0, 0, width, height, PALETTE, false);
        if (layout.outliner().visible()) outliner(g, layout.outliner(), mx, my);
        if (layout.properties().visible()) properties(g, layout.properties());
        Rect v = layout.view();
        g.fill(v.x() - 1, v.y() - 1, v.right() + 1, v.bottom() + 1, UiBoxes.RIM);
        ModelViewport.render(g, v, camera, state.model(), state.hidden(), codeOnly ? List.of() : overlay());
    }

    /** Selection outline and gizmo lines of the selected cube. */
    private List<ModelViewport.Line> overlay() {
        Element e = state.selectedElement();
        if (e == null || state.hidden().contains(state.selected())) return List.of();
        Rect v = layout.view();
        List<ModelViewport.Line> lines = new ArrayList<>(ModelViewport.box(camera, v, ModelGeometry.corners(e), 1.5f, ModelViewport.SELECTION));
        Vector2f c = ModelViewport.project(camera, v, ModelGeometry.centre(e));
        for (int axis = 0; axis < 3; axis++) {
            int colour = ModelViewport.AXIS[axis];
            if (state.mode() == Gizmo.Mode.ROTATE) {
                List<Vector2f> ring = ring(e, axis);
                for (int i = 0; i + 1 < ring.size(); i++)
                    lines.add(new ModelViewport.Line(ring.get(i).x, ring.get(i).y, ring.get(i + 1).x, ring.get(i + 1).y, 2f, colour));
            } else {
                Vector2f end = handle(e, axis, 1);
                Vector2f from = state.mode() == Gizmo.Mode.SCALE ? handle(e, axis, -1) : c;
                lines.add(new ModelViewport.Line(from.x, from.y, end.x, end.y, 2f, colour));
            }
        }
        return lines;
    }

    private void outliner(GuiGraphicsExtractor g, Rect r, int mx, int my) {
        UiBoxes.insetColored(g, r.x(), r.y(), r.w(), r.h(), PALETTE, LIST_FACE);
        g.text(font, Component.translatable("simplemodels.editor.cubes"), r.x() + 4, r.y() + 3, LABEL, false);
        var elements = state.model().elements();
        int rows = Math.max(1, (r.h() - 16) / ROW);
        for (int row = 0; row < rows && row + listScroll < elements.size(); row++) {
            int i = row + listScroll, y = r.y() + 14 + row * ROW;
            boolean selected = i == state.selected();
            if (selected) g.fill(r.x() + 1, y, r.right() - 1, y + ROW, ROW_SELECTED);
            boolean hidden = state.hidden().contains(i);
            // Eye: filled square = shown, hollow = hidden.
            int ex = r.x() + 4, ey = y + 2;
            int eyeColour = selected ? ROW_SELECTED_TEXT : LABEL;
            g.fill(ex, ey, ex + 8, ey + 8, eyeColour);
            if (hidden) g.fill(ex + 1, ey + 1, ex + 7, ey + 7, selected ? ROW_SELECTED : LIST_FACE);
            else g.fill(ex + 3, ey + 3, ex + 5, ey + 5, selected ? ROW_SELECTED : LIST_FACE);
            String name = elements.get(i).name() == null ? "#" + i : elements.get(i).name();
            name = font.plainSubstrByWidth(name, r.w() - 20);
            g.text(font, name, r.x() + 15, y + 2, selected ? ROW_SELECTED_TEXT : LABEL, false);
        }
    }

    private void properties(GuiGraphicsExtractor g, Rect r) {
        UiBoxes.insetColored(g, r.x(), r.y(), r.w(), r.h(), PALETTE, LIST_FACE);
        Element e = state.selectedElement();
        g.text(font, Component.translatable("simplemodels.editor.properties"), r.x() + 4, r.y() + 3, LABEL, false);
        if (e == null) {
            g.textWithWordWrap(font, Component.translatable("simplemodels.editor.nothing_selected"), r.x() + 4, r.y() + 16, r.w() - 8, LABEL, false);
            return;
        }
        for (int row = 0; row < 3; row++)
            g.text(font, Component.translatable("simplemodels.editor." + FIELD_NAMES[row]), r.x() + 5, r.y() + 16 + row * 26, LABEL, false);
        var rot = e.rotation();
        Component rotation = rot == null || !rot.isEuler() && rot.angle() == 0 ? Component.translatable("simplemodels.editor.rotation.none")
                : Component.literal(rot.isEuler() ? "x " + number(rot.euler().x()) + "°, y " + number(rot.euler().y()) + "°, z " + number(rot.euler().z()) + "°"
                : rot.axis() + " " + number(rot.angle()) + "°");
        g.textWithWordWrap(font, Component.translatable("simplemodels.editor.rotation", rotation), r.x() + 5, r.y() + 96, r.w() - 10, LABEL, false);
        g.textWithWordWrap(font, Component.translatable("simplemodels.editor.mode." + state.mode().key), r.x() + 5, r.y() + 120, r.w() - 10, LABEL, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        Rect v = layout.view();
        // Gizmo handles (squares with a dark rim) above the lines.
        Element e = state.selectedElement();
        if (!codeOnly && e != null && state.mode() != Gizmo.Mode.ROTATE && !state.hidden().contains(state.selected())) {
            for (int axis = 0; axis < 3; axis++) {
                for (int sign : state.mode() == Gizmo.Mode.SCALE ? new int[]{1, -1} : new int[]{1}) {
                    Vector2f h = handle(e, axis, sign);
                    int hx = Math.round(h.x), hy = Math.round(h.y);
                    if (!v.contains(hx, hy)) continue;
                    g.fill(hx - 3, hy - 3, hx + 4, hy + 4, UiBoxes.RIM);
                    g.fill(hx - 2, hy - 2, hx + 3, hy + 3, ModelViewport.AXIS[axis]);
                }
            }
        }
        String hint = font.plainSubstrByWidth(Component.translatable("simplemodels.editor.view_hint").getString(), v.w() - 8);
        g.text(font, hint, v.x() + 4, v.bottom() - 11, VIEW_TEXT, false);
        if (dragLabel != null) g.text(font, dragLabel, mx + 10, my - 4, VIEW_TEXT, true);
        Rect s = layout.status();
        Component mode = Component.translatable("simplemodels.editor.mode." + state.mode().key);
        g.text(font, Component.translatable("simplemodels.editor.status", state.model().elements().size(), number(state.snapStep()), mode),
                s.x() + 2, s.y() + 2, LABEL, false);
        if (codeOnly) {
            Rect p = layout.problems();
            var problems = state.problems();
            Component line = problems.isEmpty() ? Component.translatable("simplemodels.editor.code_ok")
                    : Component.translatable("simplemodels.editor.code_problem", problems.getFirst().path().isEmpty() ? "JSON" : problems.getFirst().path(),
                    Component.translatable("simplemodels.editor.problem." + problems.getFirst().key(), problems.getFirst().args().toArray()));
            g.textWithWordWrap(font, line, p.x() + 2, p.y() + 2, p.w() - 4, problems.isEmpty() ? LABEL : 0xFF8E1B1B, false);
        }
    }
}
