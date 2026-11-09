package com.simplebuilding.modules.simplemodels.editor;

import com.simplebuilding.modules.simplemodels.editor.EditableModel.Element;
import com.simplebuilding.modules.simplemodels.editor.EditableModel.Vec;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Editor stage 2/3 (b-models13): what the editor screen edits, without any client class. The model and its undo stack,
 * the selected cube, hidden cubes (outliner eye), gizmo mode and grid. A gizmo drag shows a live preview and becomes
 * exactly one undo step when it ends; cancel (Esc / right click) returns to the start. Tested in {@code editor_state}.
 */
public final class ModelEditorState {
    /** A new cube: the middle half of the block. */
    public static final Vec NEW_FROM = new Vec(4, 4, 4), NEW_TO = new Vec(12, 12, 12);

    private final EditHistory<EditableModel> history;
    private final TreeSet<Integer> hidden = new TreeSet<>();
    private int selected = -1;
    private Gizmo.Mode mode = Gizmo.Mode.MOVE;
    private int snapIndex;
    private EditableModel dragStart, live;
    private List<ModelJson.Problem> problems = List.of();
    /** Face of the selected cube the face tab (UV, texture) edits; b-models14. */
    private String face = "north";

    public ModelEditorState(EditableModel model) {
        history = new EditHistory<>(model);
        if (!model.elements().isEmpty()) selected = 0;
    }

    /** The model to show: the live drag preview while dragging, else the current undo state. */
    public EditableModel model() { return live != null ? live : history.current(); }
    public EditableModel committed() { return history.current(); }
    public int selected() { return selected; }
    public Element selectedElement() { return selected >= 0 && selected < model().elements().size() ? model().elements().get(selected) : null; }
    public Set<Integer> hidden() { return java.util.Collections.unmodifiableSet(hidden); }
    public Gizmo.Mode mode() { return mode; }
    public void mode(Gizmo.Mode m) { mode = m; }
    public float snapStep() { return Gizmo.SNAP_STEPS[snapIndex]; }
    /** Rotate step of the current grid preset (0 = free). */
    public float angleStep() { return Gizmo.ANGLE_STEPS[snapIndex]; }
    public void cycleSnap() { snapIndex = (snapIndex + 1) % Gizmo.SNAP_STEPS.length; }
    public boolean canUndo() { return history.canUndo(); }
    public boolean canRedo() { return history.canRedo(); }
    public boolean dragging() { return dragStart != null; }
    /** Problems of the last code text that did not read (empty = the code matches the model). */
    public List<ModelJson.Problem> problems() { return problems; }

    public void select(int index) {
        selected = index >= 0 && index < model().elements().size() ? index : -1;
    }

    public String face() { return face; }
    public void selectFace(String direction) {
        if (EditableModel.DIRECTIONS.contains(direction)) face = direction;
    }

    /** One undo step: the selected cube replaced by {@code change} applied to it (face tab buttons). */
    public void editElement(java.util.function.UnaryOperator<Element> change) {
        Element e = selectedElement();
        if (e != null) apply(history.current().withElement(selected, change.apply(e)));
    }

    /** One undo step: the whole model replaced (texture picker). */
    public void editModel(EditableModel next) {
        apply(next);
        afterJump();
    }

    public void toggleHidden(int index) {
        if (!hidden.remove(index) && index >= 0 && index < model().elements().size()) hidden.add(index);
    }

    // ------------------------------------------------------------ drag (one undo step)

    /** Starts a gizmo drag on the selected cube; false when nothing is selected. */
    public boolean beginDrag() {
        if (selectedElement() == null) return false;
        dragStart = history.current();
        live = dragStart;
        return true;
    }

    /** The selected cube as it stood when the drag began. */
    public Element dragStartElement() {
        return dragStart == null ? null : dragStart.elements().get(selected);
    }

    /** Live preview: the selected cube replaced by {@code element}. */
    public void drag(Element element) {
        if (dragStart != null) live = dragStart.withElement(selected, element);
    }

    /** Ends the drag: one undo step (none when nothing changed). */
    public void endDrag() {
        if (dragStart == null) return;
        EditableModel result = live;
        dragStart = null;
        live = null;
        history.push(result);
    }

    /** Esc / right click while dragging: back to the start, no undo step. */
    public void cancelDrag() {
        dragStart = null;
        live = null;
    }

    // ------------------------------------------------------------ commands

    /** New cube (key N) on the first texture variable of the model; it becomes selected. */
    public void addCube() {
        EditableModel m = history.current();
        String texture = m.textures().keySet().stream().filter(k -> !k.equals("particle")).findFirst().map(k -> "#" + k).orElse("#missing");
        apply(m.addElement(Element.cube(freeName(m, "cube"), NEW_FROM, NEW_TO, texture)));
        selected = history.current().elements().size() - 1;
    }

    /** Duplicate the selected cube (Ctrl+D) right after it, with a free name; the copy becomes selected. */
    public void duplicate() {
        Element e = selectedElement();
        if (e == null) return;
        EditableModel m = history.current();
        var list = new java.util.ArrayList<>(m.elements());
        list.add(selected + 1, e.withName(freeName(m, e.name() == null ? "cube" : e.name())));
        apply(m.withElements(list));
        shiftHidden(selected + 1, 1);
        selected++;
    }

    /** Delete the selected cube (Del); the selection moves to the cube before it. */
    public void delete() {
        if (selectedElement() == null) return;
        int index = selected;
        apply(history.current().removeElement(index));
        hidden.remove(index);
        shiftHidden(index + 1, -1);
        select(Math.min(Math.max(0, index - 1), history.current().elements().size() - 1));
    }

    /** A number field of the properties panel: {@code field} from / to / origin, {@code axis} 0..2. */
    public void setValue(String field, int axis, float value) {
        Element e = selectedElement();
        if (e == null) return;
        value = Gizmo.clamp(value);
        Element next = switch (field) {
            case "from" -> e.withBounds(Gizmo.set(e.from(), axis, value), e.to());
            case "to" -> e.withBounds(e.from(), Gizmo.set(e.to(), axis, value));
            case "origin" -> e.rotation() == null ? e : e.withRotation(new EditableModel.Rotation(Gizmo.set(e.rotation().origin(), axis, value),
                    e.rotation().axis(), e.rotation().angle(), e.rotation().euler(), e.rotation().rescale()));
            default -> throw new IllegalArgumentException(field);
        };
        apply(history.current().withElement(selected, next));
    }

    public void undo() { cancelDrag(); history.undo(); afterJump(); }
    public void redo() { cancelDrag(); history.redo(); afterJump(); }

    /**
     * Code view: text that reads becomes one undo step (and clears the problems); text that does not read keeps the last
     * valid model and lists the problems. Returns whether the text was valid.
     */
    public boolean applyCode(String json) {
        ModelJson.Result result = ModelJson.read(json);
        problems = result.problems();
        if (!result.ok()) return false;
        cancelDrag();
        history.push(result.model());
        afterJump();
        return true;
    }

    /** The code text of the current model (vanilla JSON, compact). */
    public String code() {
        return ModelJson.writeCompact(history.current());
    }

    /** The code text for a code column of {@code width} characters (no wrapped lines at GUI scale 2). */
    public String code(int width) {
        return ModelJson.writeCompact(history.current(), width);
    }

    private void apply(EditableModel next) {
        cancelDrag();
        history.push(next);
        problems = List.of();
    }

    private void afterJump() {
        int size = history.current().elements().size();
        hidden.removeIf(i -> i >= size);
        if (selected >= size) selected = size - 1;
        if (selected < 0 && size > 0) selected = 0;
    }

    private void shiftHidden(int from, int by) {
        var moved = new TreeSet<Integer>();
        for (int i : hidden) moved.add(i >= from ? i + by : i);
        hidden.clear();
        hidden.addAll(moved);
    }

    /** {@code base}, else {@code base_2}, {@code base_3} ... whichever no cube uses yet (max. name length kept). */
    static String freeName(EditableModel m, String base) {
        var used = new java.util.HashSet<String>();
        for (Element e : m.elements()) if (e.name() != null) used.add(e.name());
        String stem = base.replaceAll("_\\d+$", "");
        if (stem.length() > ModelJson.MAX_NAME - 4) stem = stem.substring(0, ModelJson.MAX_NAME - 4);
        if (!used.contains(stem)) return stem;
        for (int n = 2; ; n++) if (!used.contains(stem + "_" + n)) return stem + "_" + n;
    }
}
