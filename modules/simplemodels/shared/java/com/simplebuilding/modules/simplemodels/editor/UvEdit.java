package com.simplebuilding.modules.simplemodels.editor;

import com.simplebuilding.modules.simplemodels.editor.EditableModel.Element;
import com.simplebuilding.modules.simplemodels.editor.EditableModel.Face;
import com.simplebuilding.modules.simplemodels.editor.EditableModel.Uv;

/**
 * Editor face tab (b-models14, concept §3 "UV-Editor" / stage 5): UV rectangle and face settings of one cube face as pure
 * functions on {@link Element}. UVs are texture pixels 0..16 like vanilla ({@code u1 > u2} mirrors); {@code uv == null}
 * means vanilla auto-UV from the cube bounds. The UV square in the panel maps 0..16 onto its pixels. Tested in
 * {@code editor_uv}.
 */
public final class UvEdit {
    /** What a drag in the UV square moves: the whole rectangle or one corner (named by the UV values it changes). */
    public enum Handle { MOVE, U1V1, U2V1, U2V2, U1V2 }

    /** The UV the face draws with: its own, else auto-UV; {@code null} when the face is off. */
    public static Uv uv(Element e, String direction) {
        Face f = e.faces().get(direction);
        return f == null ? null : f.uv() != null ? f.uv() : ModelGeometry.autoUv(e, direction);
    }

    private static Element face(Element e, String direction, java.util.function.UnaryOperator<Face> change) {
        Face f = e.faces().get(direction);
        return f == null ? e : e.withFace(direction, change.apply(f));
    }

    /** Explicit UV, each value clamped to 0..16. */
    public static Element setUv(Element e, String direction, Uv uv) {
        Uv c = new Uv(clamp(uv.u1()), clamp(uv.v1()), clamp(uv.u2()), clamp(uv.v2()));
        return face(e, direction, f -> new Face(f.texture(), c, f.rotation(), f.cullface(), f.tintIndex()));
    }

    /** Back to vanilla auto-UV (follows the cube size again). */
    public static Element autoUv(Element e, String direction) {
        return face(e, direction, f -> new Face(f.texture(), null, f.rotation(), f.cullface(), f.tintIndex()));
    }

    /** Face rotation 0 -> 90 -> 180 -> 270 -> 0. */
    public static Element rotate(Element e, String direction) {
        return face(e, direction, f -> new Face(f.texture(), f.uv(), (f.rotation() + 90) % 360, f.cullface(), f.tintIndex()));
    }

    /** Mirror left-right: u1 and u2 swap (the auto-UV becomes explicit). */
    public static Element mirrorU(Element e, String direction) {
        Uv u = uv(e, direction);
        return u == null ? e : setUv(e, direction, new Uv(u.u2(), u.v1(), u.u1(), u.v2()));
    }

    /** Mirror up-down: v1 and v2 swap. */
    public static Element mirrorV(Element e, String direction) {
        Uv u = uv(e, direction);
        return u == null ? e : setUv(e, direction, new Uv(u.u1(), u.v2(), u.u2(), u.v1()));
    }

    /**
     * Face on/off. Off removes the face (vanilla draws nothing there); the last face stays (a cube needs one). On adds the
     * face with {@code texture} (the variable the other faces use) and auto-UV.
     */
    public static Element toggle(Element e, String direction, String texture) {
        if (e.faces().containsKey(direction)) return e.faces().size() > 1 ? e.withFace(direction, null) : e;
        return e.withFace(direction, Face.of(texture));
    }

    /** cullface: none -> down -> up -> north -> south -> west -> east -> none. */
    public static Element cycleCull(Element e, String direction) {
        return face(e, direction, f -> {
            int i = f.cullface() == null ? -1 : EditableModel.DIRECTIONS.indexOf(f.cullface());
            String next = i + 1 < EditableModel.DIRECTIONS.size() ? EditableModel.DIRECTIONS.get(i + 1) : null;
            return new Face(f.texture(), f.uv(), f.rotation(), next, f.tintIndex());
        });
    }

    /** tintindex: none (-1) -> 0 -> none; 0 is what grass, leaves and spawn eggs use. */
    public static Element cycleTint(Element e, String direction) {
        return face(e, direction, f -> new Face(f.texture(), f.uv(), f.rotation(), f.cullface(), f.tintIndex() < 0 ? 0 : -1));
    }

    /** The face draws texture {@code texture} ({@code #variable} or a texture id). */
    public static Element setTexture(Element e, String direction, String texture) {
        return face(e, direction, f -> new Face(texture, f.uv(), f.rotation(), f.cullface(), f.tintIndex()));
    }

    /** A texture value for a new face: the first texture another face of the cube uses, else {@code fallback}. */
    public static String texture(Element e, String fallback) {
        return e.faces().values().stream().map(Face::texture).findFirst().orElse(fallback);
    }

    /**
     * The UV after dragging {@code handle} by {@code du, dv} texture pixels from {@code start}. MOVE keeps the size and
     * stays inside 0..16; a corner lands on the grid {@code step} (0 = free).
     */
    public static Uv drag(Uv start, Handle handle, float du, float dv, float step) {
        if (handle == Handle.MOVE) {
            float lu = Math.min(start.u1(), start.u2()), hu = Math.max(start.u1(), start.u2());
            float lv = Math.min(start.v1(), start.v2()), hv = Math.max(start.v1(), start.v2());
            float mu = Math.max(-lu, Math.min(16 - hu, Gizmo.snap(du, step)));
            float mv = Math.max(-lv, Math.min(16 - hv, Gizmo.snap(dv, step)));
            return new Uv(start.u1() + mu, start.v1() + mv, start.u2() + mu, start.v2() + mv);
        }
        boolean u1 = handle == Handle.U1V1 || handle == Handle.U1V2, v1 = handle == Handle.U1V1 || handle == Handle.U2V1;
        float u = clamp(Gizmo.snap((u1 ? start.u1() : start.u2()) + du, step)), v = clamp(Gizmo.snap((v1 ? start.v1() : start.v2()) + dv, step));
        return new Uv(u1 ? u : start.u1(), v1 ? v : start.v1(), u1 ? start.u2() : u, v1 ? start.v2() : v);
    }

    /** The handle under UV point {@code (u, v)}: a corner within {@code tolerance} pixels, else MOVE inside, else null. */
    public static Handle handleAt(Uv uv, float u, float v, float tolerance) {
        Handle best = null;
        float bestD = tolerance;
        for (Handle h : new Handle[]{Handle.U1V1, Handle.U2V1, Handle.U2V2, Handle.U1V2}) {
            float cu = h == Handle.U1V1 || h == Handle.U1V2 ? uv.u1() : uv.u2(), cv = h == Handle.U1V1 || h == Handle.U2V1 ? uv.v1() : uv.v2();
            float d = Math.max(Math.abs(cu - u), Math.abs(cv - v));
            if (d <= bestD) { bestD = d; best = h; }
        }
        if (best != null) return best;
        boolean inside = u >= Math.min(uv.u1(), uv.u2()) && u <= Math.max(uv.u1(), uv.u2()) && v >= Math.min(uv.v1(), uv.v2()) && v <= Math.max(uv.v1(), uv.v2());
        return inside ? Handle.MOVE : null;
    }

    /** Panel pixel to UV (0..16) for a UV square starting at {@code origin} with {@code size} pixels. */
    public static float toUv(double pixel, int origin, int size) {
        return clamp((float) ((pixel - origin) * 16.0 / size));
    }

    /** UV (0..16) to panel pixel. */
    public static float toPixel(float uv, int origin, int size) {
        return origin + uv * size / 16f;
    }

    static float clamp(float v) {
        return Math.max(0, Math.min(16, v));
    }

    private UvEdit() {}
}
