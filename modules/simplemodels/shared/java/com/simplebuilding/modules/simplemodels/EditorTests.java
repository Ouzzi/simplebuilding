package com.simplebuilding.modules.simplemodels;

import com.simplebuilding.modules.simplemodels.editor.EditableModel;
import com.simplebuilding.modules.simplemodels.editor.EditableModel.Element;
import com.simplebuilding.modules.simplemodels.editor.EditableModel.Rotation;
import com.simplebuilding.modules.simplemodels.editor.EditableModel.Vec;
import com.simplebuilding.modules.simplemodels.editor.ModelEditorLayout;
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

    private EditorTests() {}
}
