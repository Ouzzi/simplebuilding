package com.simplebuilding.modules.simplemodels.editor;

import com.simplebuilding.modules.simplemodels.editor.EditableModel.Element;
import com.simplebuilding.modules.simplemodels.editor.EditableModel.Rotation;
import com.simplebuilding.modules.simplemodels.editor.EditableModel.Vec;

/**
 * Editor stage 3 (b-models13): gizmo arithmetic for move / rotate / scale (keys G / R / S, like Blockbench and Blender).
 * Every drag is computed from the element at drag start plus the whole mouse way so far, so rounding never drifts and
 * Esc can always return to the start. Pure functions, tested in {@code editor_gizmo}.
 */
public final class Gizmo {
    public enum Mode {
        MOVE("move"), ROTATE("rotate"), SCALE("scale");
        public final String key;
        Mode(String key) { this.key = key; }
    }

    /**
     * Grid presets the snap button cycles through (b-models14): move/scale/UV step in block pixels paired with the rotate
     * step in degrees, index for index. The first is the default (1 px / 22.5 degrees as old vanilla); the last is "off"
     * (0 = free). One button keeps the toolbar on one line at GUI scale 3.
     */
    public static final float[] SNAP_STEPS = {1f, 0.5f, 0.25f, 0f};
    public static final float[] ANGLE_STEPS = {22.5f, 15f, 5f, 0f};
    /** Default rotate step; Shift while rotating = 1 degree, Alt = free. */
    public static final float ANGLE_STEP = ANGLE_STEPS[0], FINE_ANGLE_STEP = 1f;

    /**
     * Model units along an axis for a mouse way {@code dx, dy} (GUI pixels): the way projected onto the axis' screen
     * direction {@code axisX, axisY} (GUI pixels per unit). An axis pointing straight at the viewer gives 0.
     */
    public static float along(float axisX, float axisY, double dx, double dy) {
        float len2 = axisX * axisX + axisY * axisY;
        return len2 < 1e-6f ? 0f : (float) ((dx * axisX + dy * axisY) / len2);
    }

    /**
     * Degrees turned about an axis when the pointer goes from {@code (x0, y0)} to {@code (x1, y1)} around the screen
     * point {@code (cx, cy)}. Counter-clockwise on screen is positive when the axis points at the viewer (right-hand
     * rule, as vanilla's angle), negative when it points away.
     */
    public static float screenAngle(double cx, double cy, double x0, double y0, double x1, double y1, boolean axisTowardsViewer) {
        double a0 = Math.atan2(-(y0 - cy), x0 - cx), a1 = Math.atan2(-(y1 - cy), x1 - cx);
        double delta = Math.toDegrees(a1 - a0);
        delta = ((delta + 540) % 360) - 180;
        return (float) (axisTowardsViewer ? delta : -delta);
    }

    /** {@code value} on the grid {@code step}; step 0 = free. */
    public static float snap(float value, float step) {
        return step <= 0 ? value : Math.round(value / step) * step;
    }

    /** Angle into (-180, 180]. */
    public static float normalise(float degrees) {
        float a = degrees % 360f;
        if (a <= -180f) a += 360f;
        if (a > 180f) a -= 360f;
        return a;
    }

    /**
     * Move along {@code axis} by {@code amount} pixels (snapped to {@code step}), kept inside the vanilla range -16..32.
     * The rotation origin moves with the element.
     */
    public static Element move(Element start, int axis, float amount, float step) {
        float d = snap(amount, step);
        float lo = Math.min(start.from().get(axis), start.to().get(axis)), hi = Math.max(start.from().get(axis), start.to().get(axis));
        d = Math.max(ModelJson.MIN_EXTENT - lo, Math.min(ModelJson.MAX_EXTENT - hi, d));
        Element out = start.withBounds(shift(start.from(), axis, d), shift(start.to(), axis, d));
        if (start.rotation() != null) {
            Rotation r = start.rotation();
            out = out.withRotation(new Rotation(clampVec(shift(r.origin(), axis, d)), r.axis(), r.angle(), r.euler(), r.rescale()));
        }
        return out;
    }

    /**
     * Resize along {@code axis}: {@code positive} drags the {@code to} side, otherwise the {@code from} side; growth
     * {@code amount} (pixels, positive = bigger). {@code symmetric} grows both sides (Ctrl). The dragged face lands on
     * the grid; size never goes below 0 and faces stay inside -16..32.
     */
    public static Element scale(Element start, int axis, float amount, float step, boolean positive, boolean symmetric) {
        float from = start.from().get(axis), to = start.to().get(axis);
        float newTo = to, newFrom = from;
        if (positive || symmetric) newTo = clamp(snap(to + amount, step));
        if (!positive || symmetric) newFrom = clamp(snap(from - amount, step));
        if (newTo < newFrom) {
            if (symmetric) { float mid = (from + to) / 2; newFrom = newTo = mid; }
            else if (positive) newTo = newFrom;
            else newFrom = newTo;
        }
        return start.withBounds(set(start.from(), axis, newFrom), set(start.to(), axis, newTo));
    }

    /**
     * Turn about model {@code axis} by {@code degrees}. No rotation yet: a single-axis rotation about the element centre.
     * Same single axis: angles add. Otherwise the vanilla Euler form ({@code x/y/z}) and the axis component adds.
     */
    public static Element rotate(Element start, int axis, float degrees) {
        Rotation r = start.rotation();
        String name = EditableModel.AXES.get(axis);
        if (r == null) {
            Vec centre = new Vec((start.from().x() + start.to().x()) / 2, (start.from().y() + start.to().y()) / 2, (start.from().z() + start.to().z()) / 2);
            return start.withRotation(Rotation.single(centre, name, normalise(degrees), false));
        }
        if (!r.isEuler() && (r.axis().equals(name) || r.angle() == 0)) {
            return start.withRotation(Rotation.single(r.origin(), name, normalise(r.angle() + degrees), r.rescale()));
        }
        Vec euler = r.isEuler() ? r.euler() : set(Vec.ZERO, EditableModel.AXES.indexOf(r.axis()), r.angle());
        euler = set(euler, axis, normalise(euler.get(axis) + degrees));
        return start.withRotation(Rotation.euler(r.origin(), euler, r.rescale()));
    }

    static Vec shift(Vec v, int axis, float d) {
        return set(v, axis, v.get(axis) + d);
    }

    static Vec set(Vec v, int axis, float value) {
        return new Vec(axis == 0 ? value : v.x(), axis == 1 ? value : v.y(), axis == 2 ? value : v.z());
    }

    private static Vec clampVec(Vec v) {
        return new Vec(clamp(v.x()), clamp(v.y()), clamp(v.z()));
    }

    static float clamp(float v) {
        return Math.max(ModelJson.MIN_EXTENT, Math.min(ModelJson.MAX_EXTENT, v));
    }

    private Gizmo() {}
}
