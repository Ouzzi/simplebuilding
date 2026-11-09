package com.simplebuilding.modules.simplemodels;

import com.simplebuilding.modules.simplemodels.editor.EditableModel;
import com.simplebuilding.modules.simplemodels.editor.EditableModel.Element;
import com.simplebuilding.modules.simplemodels.editor.EditableModel.Rotation;
import com.simplebuilding.modules.simplemodels.editor.EditableModel.Vec;
import com.simplebuilding.modules.simplemodels.editor.EditorShortcuts;
import com.simplebuilding.modules.simplemodels.editor.UvEdit;
import com.simplebuilding.modules.simplemodels.editor.TextureChoice;
import com.simplebuilding.modules.simplemodels.editor.ModelExport;
import com.simplebuilding.modules.simplemodels.editor.Gizmo;
import com.simplebuilding.modules.simplemodels.editor.ModelEditorLayout;
import com.simplebuilding.modules.simplemodels.editor.ModelEditorState;
import com.simplebuilding.modules.simplemodels.editor.ModelGeometry;
import com.simplebuilding.modules.simplemodels.editor.ModelJson;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestHelper;
import org.joml.Vector3f;

/**
 * Editor stages 2 + 3 (b-models13): layout arithmetic, vanilla geometry (corners, rotation, rescale, UV, picking), gizmo
 * arithmetic and the editor state (one undo step per drag, cancel, commands, code view). No client classes involved, so
 * they run in the server gametests of the module (catalogue {@code ModelTests.ALL}).
 */
public final class EditorTests {
    private static final float EPS = 1e-3f;

    private static boolean near(float a, float b) { return Math.abs(a - b) < EPS; }
    private static boolean near(Vector3f v, float x, float y, float z) { return near(v.x, x) && near(v.y, y) && near(v.z, z); }
    private static Element cube(float x0, float y0, float z0, float x1, float y1, float z1) {
        return Element.cube("c", new Vec(x0, y0, z0), new Vec(x1, y1, z1), "#all");
    }
    private static EditableModel stool() {
        try { return ModelJson.read(ModelExamples.resource("models/item/example/stool.json")).model(); }
        catch (java.io.IOException e) { throw new AssertionError(e); }
    }

    /** Classic layout: panels side by side inside the screen, viewport wide enough; small screens fold panels; code only splits evenly. */
    public static void layout(GameTestHelper h) {
        for (int[] size : new int[][]{{427, 240}, {640, 360}, {960, 540}}) {
            var l = ModelEditorLayout.of(size[0], size[1], false);
            h.assertTrue(l.outliner().right() < l.view().x() && l.view().right() < l.properties().x() && l.properties().right() <= size[0] - ModelEditorLayout.MARGIN,
                    "List | view | properties side by side at " + size[0]);
            h.assertTrue(l.view().w() >= ModelEditorLayout.MIN_VIEW && l.toolbar().bottom() < l.view().y() && l.view().bottom() < l.status().y()
                    && l.status().bottom() <= size[1], "Toolbar above, status below at " + size[0]);
        }
        var small = ModelEditorLayout.of(240, 200, false);
        h.assertTrue(!small.properties().visible() && small.outliner().visible() && small.view().w() >= ModelEditorLayout.MIN_VIEW, "Small screen folds the properties first");
        var tiny = ModelEditorLayout.of(150, 200, false);
        h.assertTrue(!tiny.outliner().visible() && tiny.view().x() == ModelEditorLayout.MARGIN, "Tiny screen keeps only the view");
        var code = ModelEditorLayout.of(427, 240, true);
        h.assertTrue(code.codeOnly() && code.code().right() < code.view().x() && Math.abs(code.code().w() - code.view().w()) <= 1
                && !code.outliner().visible() && !code.properties().visible(), "Code only: code | preview equal width");
        h.assertTrue(code.problems().y() > code.code().bottom() && code.problems().bottom() <= code.view().bottom(), "Problem line under the code");
        h.succeed();
    }

    /** Corners, single-axis / Euler rotation, rescale, auto-UV and UV rotation as vanilla bakes them; ray picking. */
    public static void geometry(GameTestHelper h) {
        var full = cube(0, 0, 0, 16, 16, 16);
        h.assertTrue(near(ModelGeometry.corner(full, 0b111), 16, 16, 16) && near(ModelGeometry.centre(full), 8, 8, 8), "Plain corners and centre");
        var south = ModelGeometry.quad(0, full, "south");
        h.assertTrue(near(south.pos()[0], 0) && near(south.pos()[1], 16) && near(south.pos()[2], 16) && near(south.normal(), 0, 0, 1),
                "South face starts top left (vanilla FaceInfo order)");
        h.assertTrue(near(south.uv()[0], 0) && near(south.uv()[1], 0) && near(south.uv()[4], 1) && near(south.uv()[5], 1), "Full auto-UV");
        var small = cube(2, 3, 4, 6, 9, 12);
        var uv = ModelGeometry.autoUv(small, "north");
        h.assertTrue(near(uv.u1(), 10) && near(uv.v1(), 7) && near(uv.u2(), 14) && near(uv.v2(), 13), "North auto-UV mirrors x and y");
        var turnedFaces = new java.util.LinkedHashMap<>(full.faces());
        turnedFaces.put("south", new EditableModel.Face("#all", null, 90, null, -1));
        var turned = ModelGeometry.quad(0, new Element("c", full.from(), full.to(), null, null, 0, turnedFaces), "south");
        h.assertTrue(near(turned.uv()[0], 0) && near(turned.uv()[1], 1), "Face rotation 90 shifts the UV corners by one");
        var y90 = full.withRotation(Rotation.single(new Vec(8, 8, 8), "y", 90, false));
        h.assertTrue(near(ModelGeometry.corner(y90, 0b100), 0, 0, 0), "y +90 about the centre moves east-north to west-north");
        var x90 = small.withRotation(Rotation.single(new Vec(8, 8, 8), "x", 90, false));
        var ex90 = small.withRotation(Rotation.euler(new Vec(8, 8, 8), new Vec(90, 0, 0), false));
        for (int i = 0; i < 8; i++) {
            var a = ModelGeometry.corner(x90, i);
            h.assertTrue(near(ModelGeometry.corner(ex90, i), a.x, a.y, a.z), "Euler x 90 equals single axis x 90, corner " + i);
        }
        var rescaled = full.withRotation(Rotation.single(new Vec(8, 8, 8), "y", 45, true));
        h.assertTrue(near(ModelGeometry.corner(rescaled, 0b111).x, 24f), "rescale stretches by 1/cos 45 like vanilla: " + ModelGeometry.corner(rescaled, 0b111));
        // Picking: ray from the front (+z) straight back.
        var model = EditableModel.EMPTY.addElement(cube(0, 0, 0, 4, 10, 4)).addElement(cube(8, 0, 0, 12, 10, 16)).addElement(cube(8, 0, 10, 12, 10, 14));
        var back = new Vector3f(0, 0, -1);
        h.assertTrue(ModelGeometry.pick(model, new Vector3f(2, 5, 100), back, Set.of()) == 0, "Ray hits the left cube");
        h.assertTrue(ModelGeometry.pick(model, new Vector3f(6, 5, 100), back, Set.of()) == -1, "Ray through the gap hits nothing");
        h.assertTrue(ModelGeometry.pick(model, new Vector3f(10, 5, 100), back, Set.of()) == 1, "Overlapping cubes: the one whose surface is nearer (z 16) wins");
        h.assertTrue(ModelGeometry.pick(model, new Vector3f(10, 5, 100), back, Set.of(1)) == 2, "Hidden cubes are skipped");
        var bar = EditableModel.EMPTY.addElement(cube(0, 7, 7, 16, 9, 9).withRotation(Rotation.single(new Vec(8, 8, 8), "y", 90, false)));
        h.assertTrue(ModelGeometry.pick(bar, new Vector3f(8, 8, 100), back, Set.of()) == 0 && ModelGeometry.pick(bar, new Vector3f(2, 8, 100), back, Set.of()) == -1,
                "Picking follows the element rotation");
        h.succeed();
    }

    /** Projection onto the axis, snap, clamp, move with pivot, resize per side and symmetric, rotation forms. */
    public static void gizmo(GameTestHelper h) {
        h.assertTrue(near(Gizmo.along(10, 0, 25, 7), 2.5f) && Gizmo.along(0, 0, 5, 5) == 0f, "Mouse way projected onto the axis");
        h.assertTrue(near(Gizmo.snap(2.4f, 1), 2) && near(Gizmo.snap(2.4f, 0.5f), 2.5f) && near(Gizmo.snap(2.4f, 0), 2.4f), "Snap steps");
        h.assertTrue(near(Gizmo.normalise(190), -170) && near(Gizmo.normalise(-180), 180) && near(Gizmo.normalise(720), 0), "Angles into (-180, 180]");
        h.assertTrue(near(Gizmo.screenAngle(0, 0, 10, 0, 0, -10, true), 90) && near(Gizmo.screenAngle(0, 0, 10, 0, 0, -10, false), -90),
                "Counter-clockwise on screen is positive for an axis towards the viewer");
        var c = cube(4, 4, 4, 12, 12, 12);
        var moved = Gizmo.move(c, 0, 2.4f, 1);
        h.assertTrue(near(moved.from().x(), 6) && near(moved.to().x(), 14) && near(moved.from().y(), 4), "Move snaps to the grid");
        var far = Gizmo.move(c, 1, 100, 1);
        h.assertTrue(near(far.to().y(), ModelJson.MAX_EXTENT) && near(far.to().y() - far.from().y(), 8), "Move stops at the vanilla limit, size kept");
        var pivoted = Gizmo.move(c.withRotation(Rotation.single(new Vec(8, 8, 8), "y", 45, false)), 2, -3, 1);
        h.assertTrue(near(pivoted.rotation().origin().z(), 5), "Pivot moves with the cube");
        h.assertTrue(near(Gizmo.scale(c, 0, 3, 1, true, false).to().x(), 15) && near(Gizmo.scale(c, 0, 3, 1, true, false).from().x(), 4), "Resize the + side");
        h.assertTrue(near(Gizmo.scale(c, 0, 3, 1, false, false).from().x(), 1) && near(Gizmo.scale(c, 0, 3, 1, false, false).to().x(), 12), "Resize the - side");
        var both = Gizmo.scale(c, 2, 3, 1, true, true);
        h.assertTrue(near(both.from().z(), 1) && near(both.to().z(), 15), "Ctrl resizes both sides");
        var flat = Gizmo.scale(c, 1, -20, 1, true, false);
        h.assertTrue(near(flat.to().y(), flat.from().y()), "Size never below zero");
        var r1 = Gizmo.rotate(c, 1, 22.5f);
        h.assertTrue(!r1.rotation().isEuler() && r1.rotation().axis().equals("y") && near(r1.rotation().angle(), 22.5f)
                && r1.rotation().origin().equals(new Vec(8, 8, 8)), "First turn: single axis about the centre");
        var r2 = Gizmo.rotate(r1, 1, 22.5f);
        h.assertTrue(!r2.rotation().isEuler() && near(r2.rotation().angle(), 45), "Same axis adds up");
        var r3 = Gizmo.rotate(r2, 0, 10);
        h.assertTrue(r3.rotation().isEuler() && near(r3.rotation().euler().x(), 10) && near(r3.rotation().euler().y(), 45) && near(r3.rotation().euler().z(), 0),
                "Another axis switches to vanilla Euler x/y/z");
        h.assertTrue(ModelJson.read(ModelJson.write(EditableModel.EMPTY.addElement(r3))).ok(), "Gizmo results stay valid vanilla JSON");
        h.succeed();
    }

    /** Drag = one undo step, cancel = none, commands, hidden cubes follow inserts, number fields, code view. */
    public static void state(GameTestHelper h) {
        var start = stool();
        var s = new ModelEditorState(start);
        h.assertTrue(s.selected() == 0 && !s.canUndo(), "First cube selected, nothing to undo");
        h.assertTrue(s.beginDrag(), "Drag starts on the selection");
        s.drag(Gizmo.move(s.dragStartElement(), 1, 1, 1));
        s.drag(Gizmo.move(s.dragStartElement(), 1, 2, 1));
        h.assertTrue(near(s.model().elements().getFirst().from().y(), 12) && s.committed().equals(start), "Live preview, nothing committed yet");
        s.endDrag();
        h.assertTrue(s.canUndo() && near(s.committed().elements().getFirst().from().y(), 12), "One undo step per drag");
        s.undo();
        h.assertTrue(s.model().equals(start) && s.canRedo(), "Undo returns to the start");
        s.redo();
        s.beginDrag();
        s.drag(Gizmo.move(s.dragStartElement(), 0, 3, 1));
        s.cancelDrag();
        h.assertTrue(near(s.model().elements().getFirst().from().x(), 2) && !s.dragging(), "Esc cancels the drag");
        var before = s.committed();
        s.beginDrag();
        s.endDrag();
        h.assertTrue(s.committed() == before, "A drag without change adds no step");
        int n = s.model().elements().size();
        s.addCube();
        h.assertTrue(s.model().elements().size() == n + 1 && s.selected() == n && s.selectedElement().name().equals("cube")
                && s.selectedElement().faces().get("up").texture().equals("#seat"), "New cube on the first texture, selected");
        s.addCube();
        h.assertTrue(s.selectedElement().name().equals("cube_2"), "Free names");
        s.select(1);
        s.toggleHidden(3);
        s.duplicate();
        h.assertTrue(s.selected() == 2 && s.model().elements().get(2).name().equals("leg_nw_2") && s.hidden().equals(Set.of(4)),
                "Duplicate after the original, hidden indices follow");
        s.delete();
        h.assertTrue(s.selected() == 1 && s.hidden().equals(Set.of(3)) && s.model().elements().size() == n + 2, "Delete selects the cube before");
        s.setValue("to", 0, 99);
        h.assertTrue(near(s.selectedElement().to().x(), ModelJson.MAX_EXTENT), "Number fields clamp to the vanilla range");
        var valid = s.committed();
        h.assertTrue(!s.applyCode("{\"elements\": [") && !s.problems().isEmpty() && s.model().equals(valid), "Broken code keeps the last valid model");
        var edited = valid.withElement(0, valid.elements().getFirst().withName("sitz"));
        h.assertTrue(s.applyCode(ModelJson.writeCompact(edited)) && s.problems().isEmpty() && s.model().equals(edited), "Valid code becomes the model");
        for (String name : ModelExamples.NAMES) {
            try {
                var m = ModelJson.read(ModelExamples.resource("models/item/example/" + name + ".json")).model();
                String compact = ModelJson.writeCompact(m);
                h.assertTrue(ModelJson.read(compact).model().equals(m), "Compact code reads back: " + name);
                h.assertTrue(compact.lines().count() < ModelJson.write(m).lines().count() / 2, "Compact code is shorter: " + name);
            } catch (java.io.IOException e) { throw new AssertionError(e); }
        }
        h.succeed();
    }

    private static java.util.Map<String, String> lang(String locale) {
        try (var in = EditorTests.class.getResourceAsStream("/assets/simplemodels/lang/" + locale + ".json")) {
            return ModelCatalogue.GSON.fromJson(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8),
                    new com.google.gson.reflect.TypeToken<java.util.Map<String, String>>(){}.getType());
        } catch (java.io.IOException e) { throw new AssertionError(e); }
    }

    /** b-models14 S1: grid presets, shortcut help texts, problem location in the code, compact JSON for narrow columns. */
    public static void snapHelp(GameTestHelper h) {
        h.assertTrue(Gizmo.SNAP_STEPS.length == Gizmo.ANGLE_STEPS.length && Gizmo.SNAP_STEPS[0] == 1f && Gizmo.ANGLE_STEPS[0] == 22.5f
                && Gizmo.SNAP_STEPS[Gizmo.SNAP_STEPS.length - 1] == 0f && Gizmo.ANGLE_STEPS[Gizmo.ANGLE_STEPS.length - 1] == 0f,
                "Grid presets pair move and rotate steps, default 1 px / 22.5°, last = free");
        var s = new ModelEditorState(stool());
        for (int i = 0; i < Gizmo.SNAP_STEPS.length; i++) s.cycleSnap();
        h.assertTrue(s.snapStep() == 1f && s.angleStep() == 22.5f, "Cycling wraps to the default");
        s.cycleSnap(); s.cycleSnap(); s.cycleSnap();
        h.assertTrue(s.snapStep() == 0f && s.angleStep() == 0f && near(Gizmo.move(s.selectedElement(), 0, 0.3f, s.snapStep()).from().x(), 2.3f), "Free preset moves freely");
        var en = lang("en_us");
        var de = lang("de_de");
        for (String key : EditorShortcuts.langKeys()) h.assertTrue(en.containsKey(key) && de.containsKey(key), "Help text EN/DE: " + key);
        var ids = new java.util.HashSet<String>();
        for (var sc : EditorShortcuts.ALL) h.assertTrue(ids.add(sc.id()) && EditorShortcuts.GROUPS.contains(sc.group()), "Unique entry in a known group: " + sc.id());
        h.assertTrue(EditorShortcuts.keys(EditorShortcuts.ALL.stream().filter(x -> x.id().equals("duplicate")).findFirst().orElseThrow(), de::get).equals("Strg + D"),
                "Modifier names are translated");
        // Problem location: value span for a path, opening brace for a missing field, Gson line/column for syntax.
        String text = "{\n  \"elements\": [\n    {\"from\": [0, 0, 0], \"to\": [1, 40, 1], \"faces\": {\"up\": {\"texture\": \"#a\"}}}\n  ]\n}";
        var bad = ModelJson.read(text);
        h.assertTrue(!bad.ok() && bad.problems().getFirst().path().equals("elements[0].to"), "Out of range problem on elements[0].to");
        int[] at = ModelJson.locate(text, bad.problems().getFirst());
        h.assertTrue(at != null && text.substring(at[0], at[1]).equals("[1, 40, 1]"), "Located the value of elements[0].to: " + (at == null ? null : text.substring(at[0], at[1])));
        int[] uv = ModelJson.locate(text, new ModelJson.Problem("elements[0].faces.up.texture", "bad_texture", java.util.List.of()));
        h.assertTrue(uv != null && text.substring(uv[0], uv[1]).equals("\"#a\""), "Nested object keys");
        int[] idx = ModelJson.locate(text, new ModelJson.Problem("elements[0].to[1]", "not_number", java.util.List.of()));
        h.assertTrue(idx != null && text.substring(idx[0], idx[1]).equals("40"), "Array index");
        int[] missing = ModelJson.locate(text, new ModelJson.Problem("elements[0].rotation", "missing", java.util.List.of()));
        h.assertTrue(missing != null && text.charAt(missing[0]) == '{' && missing[0] > text.indexOf('['), "Missing field marks its object");
        String broken = "{\n  \"elements\": [\n    {\"from\" [0, 0, 0]}\n  ]\n}";
        var syntax = ModelJson.read(broken);
        int[] sx = ModelJson.locate(broken, syntax.problems().getFirst());
        h.assertTrue(!syntax.ok() && syntax.problems().getFirst().key().equals("syntax") && sx != null && broken.lastIndexOf('\n', sx[0]) == broken.indexOf("    {"  ) - 1,
                "Syntax error marked on line 3: " + syntax.problems().getFirst() + " -> " + (sx == null ? null : sx[0]));
        h.assertTrue(ModelJson.locate("{}", new ModelJson.Problem("a.b[3]", "missing", java.util.List.of())) != null, "Locate never throws");
        // Narrow code columns: no line longer than the width unless a single value is longer.
        var model = stool();
        String narrow = ModelJson.writeCompact(model, 34);
        h.assertTrue(ModelJson.read(narrow).model().equals(model), "Narrow compact JSON reads back");
        h.assertTrue(narrow.lines().allMatch(l -> l.length() <= 34 || !l.trim().matches(".*[\\[{].*[\\]}].*")), "Lines fit 34 columns");
        h.assertTrue(narrow.lines().count() > ModelJson.writeCompact(model).lines().count() && ModelJson.writeCompact(model, 1).equals(ModelJson.writeCompact(model, ModelJson.MIN_COMPACT_WIDTH)),
                "Narrower = more lines, never below the minimum width");
        h.succeed();
    }

    /** b-models14 S2: UV rectangle edits, face on/off/cull/tint, face picking, one undo step per face edit or UV drag. */
    public static void uv(GameTestHelper h) {
        var c = cube(2, 0, 4, 10, 6, 12);
        var auto = UvEdit.uv(c, "south");
        h.assertTrue(near(auto.u1(), 2) && near(auto.v1(), 10) && near(auto.u2(), 10) && near(auto.v2(), 16), "Effective UV = auto-UV without an own UV");
        var set = UvEdit.setUv(c, "south", new EditableModel.Uv(-3, 4, 20, 8));
        var u = set.faces().get("south").uv();
        h.assertTrue(near(u.u1(), 0) && near(u.u2(), 16) && near(u.v1(), 4), "Own UV clamped to 0..16");
        h.assertTrue(UvEdit.autoUv(set, "south").faces().get("south").uv() == null, "Auto-UV clears the own UV");
        var r = UvEdit.rotate(UvEdit.rotate(UvEdit.rotate(UvEdit.rotate(c, "up"), "up"), "up"), "up");
        h.assertTrue(UvEdit.rotate(c, "up").faces().get("up").rotation() == 90 && r.faces().get("up").rotation() == 0, "Rotate 90 steps, wraps after 270");
        var m = UvEdit.mirrorU(c, "south").faces().get("south").uv();
        h.assertTrue(near(m.u1(), 10) && near(m.u2(), 2) && near(m.v1(), 10), "Mirror U swaps u1/u2 of the auto-UV");
        var mv = UvEdit.mirrorV(c, "south").faces().get("south").uv();
        h.assertTrue(near(mv.v1(), 16) && near(mv.v2(), 10), "Mirror V swaps v1/v2");
        var off = UvEdit.toggle(c, "down", "#x");
        h.assertTrue(!off.faces().containsKey("down") && off.faces().size() == 5, "Face off removes it");
        h.assertTrue(UvEdit.toggle(off, "down", UvEdit.texture(off, "#missing")).faces().get("down").texture().equals("#all"), "Face on uses the cube's texture");
        var single = new Element("s", c.from(), c.to(), null, null, 0, java.util.Map.of("up", EditableModel.Face.of("#a")));
        h.assertTrue(UvEdit.toggle(single, "up", "#a").faces().size() == 1, "The last face stays");
        var cull = c;
        for (int i = 0; i < 7; i++) cull = UvEdit.cycleCull(cull, "north");
        h.assertTrue("down".equals(UvEdit.cycleCull(c, "north").faces().get("north").cullface()) && cull.faces().get("north").cullface() == null, "Cull cycles through all directions and back");
        h.assertTrue(UvEdit.cycleTint(c, "up").faces().get("up").tintIndex() == 0 && UvEdit.cycleTint(UvEdit.cycleTint(c, "up"), "up").faces().get("up").tintIndex() == -1, "Tint 0 / none");
        h.assertTrue(UvEdit.setTexture(c, "east", "#top").faces().get("east").texture().equals("#top") && UvEdit.cycleCull(off, "down") == off, "Texture per face; edits on an off face do nothing");
        var start = new EditableModel.Uv(4, 4, 8, 8);
        var moved = UvEdit.drag(start, UvEdit.Handle.MOVE, 2.4f, 20, 1);
        h.assertTrue(near(moved.u1(), 6) && near(moved.u2(), 10) && near(moved.v2(), 16) && near(moved.v1(), 12), "Move snaps and stays inside, size kept");
        var corner = UvEdit.drag(start, UvEdit.Handle.U2V2, 3.3f, -1.2f, 0.5f);
        h.assertTrue(near(corner.u2(), 11.5f) && near(corner.v2(), 7) && near(corner.u1(), 4), "Corner drag on the grid");
        h.assertTrue(UvEdit.handleAt(start, 4.4f, 7.8f, 1) == UvEdit.Handle.U1V2 && UvEdit.handleAt(start, 6, 6, 1) == UvEdit.Handle.MOVE
                && UvEdit.handleAt(start, 12, 12, 1) == null, "Handles: corner, inside, outside");
        h.assertTrue(near(UvEdit.toUv(40, 10, 60), 8) && near(UvEdit.toPixel(8, 10, 60), 40) && near(UvEdit.toUv(-50, 10, 60), 0), "Panel <-> UV");
        // Face picking: from the front a ray enters through south, from above through up; rotation counts.
        var model = EditableModel.EMPTY.addElement(cube(4, 0, 4, 12, 8, 12));
        var front = ModelGeometry.pickFace(model, new Vector3f(8, 4, 100), new Vector3f(0, 0, -1), Set.of());
        var top = ModelGeometry.pickFace(model, new Vector3f(8, 100, 8), new Vector3f(0, -1, 0), Set.of());
        h.assertTrue(front != null && front.element() == 0 && front.face().equals("south") && top.face().equals("up"), "Picked faces: south from the front, up from above");
        var turned = EditableModel.EMPTY.addElement(cube(4, 0, 4, 12, 8, 12).withRotation(Rotation.single(new Vec(8, 8, 8), "y", 90, false)));
        h.assertTrue(ModelGeometry.pickFace(turned, new Vector3f(8, 4, 100), new Vector3f(0, 0, -1), Set.of()).face().equals("west")
                && ModelGeometry.pickFace(model, new Vector3f(30, 4, 100), new Vector3f(0, 0, -1), Set.of()) == null, "Rotated: y +90 shows west to the front; miss = null");
        // State: face selection, one step per edit, UV drag through the drag mechanism.
        var st = new ModelEditorState(stool());
        st.selectFace("up");
        st.selectFace("sideways");
        h.assertTrue(st.face().equals("up"), "Only real faces can be selected");
        st.editElement(e -> UvEdit.rotate(e, st.face()));
        h.assertTrue(st.canUndo() && st.selectedElement().faces().get("up").rotation() == 90, "Face edit = one undo step");
        st.beginDrag();
        st.drag(UvEdit.setUv(st.dragStartElement(), "up", UvEdit.drag(UvEdit.uv(st.dragStartElement(), "up"), UvEdit.Handle.MOVE, -1, 0, 1)));
        st.endDrag();
        st.undo();
        h.assertTrue(st.selectedElement().faces().get("up").uv() == null && st.selectedElement().faces().get("up").rotation() == 90, "UV drag = one undo step");
        h.assertTrue(ModelJson.read(ModelJson.write(st.committed())).ok(), "Face edits stay valid vanilla JSON");
        h.succeed();
    }

    /** b-models14 S3: texture variables, atlas search, assigning only valid ids, free variable names. */
    public static void textures(GameTestHelper h) {
        var m = stool();
        var vars = TextureChoice.variables(m);
        h.assertTrue(vars.stream().anyMatch(v -> v.name().equals("seat") && "minecraft:block/oak_planks".equals(v.resolved())), "Variables with their texture: " + vars);
        var chained = m.withTexture("alias", "#seat").withTexture("loop", "#loop");
        h.assertTrue("minecraft:block/oak_planks".equals(TextureChoice.resolve(chained, "#alias")) && TextureChoice.resolve(chained, "#loop") == null
                && TextureChoice.resolve(chained, "#nothing") == null, "References followed, loops and gaps unresolved");
        var ids = java.util.List.of("minecraft:block/oak_planks", "minecraft:block/stripped_oak_log", "minecraft:block/stone", "othermod:block/oak_crate", "minecraft:block/dark_oak_planks");
        h.assertTrue(TextureChoice.search(ids, "oak planks", 10).equals(java.util.List.of("minecraft:block/oak_planks", "minecraft:block/dark_oak_planks")),
                "All words must match, shortest first: " + TextureChoice.search(ids, "oak planks", 10));
        h.assertTrue(TextureChoice.search(ids, "OAK", 2).size() == 2 && TextureChoice.search(ids, "", 100).size() == ids.size()
                && TextureChoice.search(ids, "  ", 100).size() == ids.size(), "Case-insensitive, limit, empty = all");
        var assigned = TextureChoice.assign(m, "seat", "minecraft:block/stone");
        h.assertTrue("minecraft:block/stone".equals(assigned.textures().get("seat")) && assigned.elements().equals(m.elements()), "Assign sets the variable only");
        h.assertTrue(TextureChoice.assign(m, "seat", "../../evil") == m && TextureChoice.assign(m, "seat", "#leg") == m
                && TextureChoice.assign(m, "bad name", "minecraft:block/stone") == m && TextureChoice.assign(m, "seat", "Upper:Case") == m,
                "Invalid ids, references and names are refused");
        h.assertTrue(TextureChoice.freeVariable(m, "seat").equals("seat_2") && TextureChoice.freeVariable(m, "texture").equals("texture"), "Free variable names");
        var added = TextureChoice.assign(m, TextureChoice.freeVariable(m, "texture"), "minecraft:block/stone");
        h.assertTrue(added.textures().size() == m.textures().size() + 1 && ModelJson.read(ModelJson.write(added)).ok(), "New variable stays valid JSON");
        h.succeed();
    }

    /** b-models14 S4: names, path safety (no traversal, no symbolic links), pack files, catalogue entry, zip, workspace. */
    public static void export(GameTestHelper h) {
        h.assertTrue("my_stool".equals(ModelExport.safeName(" My Stool ")) && "a_b".equals(ModelExport.safeName("a-b"))
                && ModelExport.safeName("../evil") == null && ModelExport.safeName("a/b") == null && ModelExport.safeName("a.b") == null
                && ModelExport.safeName("") == null && ModelExport.safeName("x".repeat(21)) == null && ModelExport.safeName("C:x") == null,
                "Names: lower case letters, digits and _ only, max. 20");
        try {
            var root = java.nio.file.Files.createTempDirectory("simplemodels-export-");
            for (String bad : new String[]{"../x.json", "a/../../x.json", "/etc/x.json", "a\\x.json", "", "C:/x.json"}) {
                boolean refused = false;
                try { ModelExport.resolveInside(root, bad); } catch (java.io.IOException e) { refused = true; }
                h.assertTrue(refused, "Path refused: " + bad);
            }
            h.assertTrue(ModelExport.resolveInside(root, "assets/mymodels/items/a.json").startsWith(root.toAbsolutePath().normalize()), "Normal path stays inside");
            var model = stool();
            var files = ModelExport.packFiles(model, "stool_copy", 97);
            h.assertTrue(files.keySet().equals(new java.util.LinkedHashSet<>(java.util.List.of("pack.mcmeta", "assets/mymodels/models/item/stool_copy.json",
                    "assets/mymodels/items/stool_copy.json"))), "Pack files: " + files.keySet());
            var pack = root.resolve("pack");
            var written = ModelExport.writeFiles(pack, files);
            h.assertTrue(written.size() == 3 && written.stream().allMatch(java.nio.file.Files::isRegularFile), "Pack files written");
            var meta = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(pack.resolve("pack.mcmeta"))).getAsJsonObject().getAsJsonObject("pack");
            h.assertTrue(meta.get("min_format").getAsInt() == 97 && meta.get("max_format").getAsInt() == 97, "pack.mcmeta with the pack format");
            h.assertTrue(ModelJson.read(java.nio.file.Files.readString(pack.resolve("assets/mymodels/models/item/stool_copy.json"))).model().equals(model), "Model JSON reads back");
            var item = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(pack.resolve("assets/mymodels/items/stool_copy.json"))).getAsJsonObject().getAsJsonObject("model");
            h.assertTrue(item.get("model").getAsString().equals("mymodels:item/stool_copy") && item.get("type").getAsString().equals("minecraft:model"), "Item model definition");
            ModelExport.writeFiles(pack, files);
            try (var listing = java.nio.file.Files.walk(pack)) {
                h.assertTrue(listing.noneMatch(p -> p.getFileName().toString().endsWith(".tmp")), "Overwrite leaves no temporary files");
            }
            var def = ModelDefinition.parse(com.google.gson.JsonParser.parseString(ModelExport.catalogueEntry("stool_copy", "minecraft:oak_slab", "Steve")).getAsJsonObject());
            h.assertTrue(def.id().equals("mymodels:stool_copy") && def.model().equals("mymodels:stool_copy") && def.base_item().equals("minecraft:oak_slab")
                    && def.match_name().equals("Stool copy"), "Catalogue entry parses like any definition: " + def);
            h.assertTrue((Models.PREFIX + def.id()).length() <= 50, "Anvil name fits");
            // Symbolic links: as root and on the way are refused.
            var outside = java.nio.file.Files.createTempDirectory("simplemodels-outside-");
            boolean linksWork = true;
            try { java.nio.file.Files.createSymbolicLink(root.resolve("linked"), outside); } catch (UnsupportedOperationException | java.io.IOException e) { linksWork = false; }
            if (linksWork) {
                boolean refusedRoot = false, refusedWay = false;
                try { ModelExport.writeFiles(root.resolve("linked"), files); } catch (java.io.IOException e) { refusedRoot = true; }
                var pack2 = root.resolve("pack2");
                java.nio.file.Files.createDirectories(pack2);
                java.nio.file.Files.createSymbolicLink(pack2.resolve("assets"), outside);
                try { ModelExport.writeFiles(pack2, files); } catch (java.io.IOException e) { refusedWay = true; }
                try (var o = java.nio.file.Files.list(outside)) {
                    h.assertTrue(refusedRoot && refusedWay && o.findAny().isEmpty(), "Symbolic links refused, nothing written outside");
                }
            }
            // Zip: the same entries.
            var zip = ModelExport.writeZip(root.resolve("exports"), "stool_copy", files);
            var entries = new java.util.ArrayList<String>();
            try (var in = new java.util.zip.ZipInputStream(java.nio.file.Files.newInputStream(zip))) {
                for (var en = in.getNextEntry(); en != null; en = in.getNextEntry()) entries.add(en.getName());
            }
            h.assertTrue(zip.getFileName().toString().equals("stool_copy.zip") && entries.equals(new java.util.ArrayList<>(files.keySet())), "Zip entries: " + entries);
            // Workspace round trip and listing.
            var ws = root.resolve("workspace");
            h.assertTrue(ModelExport.workspaces(ws).isEmpty(), "No workspace folder = empty list");
            ModelExport.saveWorkspace(ws, "b_model", model);
            ModelExport.saveWorkspace(ws, "a_model", model.withTexture("seat", "minecraft:block/stone"));
            java.nio.file.Files.writeString(ws.resolve("Bad Name.json"), "{}");
            java.nio.file.Files.writeString(ws.resolve("broken.json"), "{\"elements\": 5}");
            h.assertTrue(ModelExport.workspaces(ws).equals(java.util.List.of("a_model", "b_model", "broken")), "Listing: allowed names, sorted: " + ModelExport.workspaces(ws));
            h.assertTrue(ModelExport.loadWorkspace(ws, "b_model").model().equals(model) && !ModelExport.loadWorkspace(ws, "broken").ok(), "Load: round trip, broken = problems");
            boolean refused = false;
            try { ModelExport.loadWorkspace(ws, "../workspace/b_model"); } catch (java.io.IOException e) { refused = true; }
            h.assertTrue(refused, "Load refuses unsafe names");
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
        h.succeed();
    }

    private EditorTests() {}
}
