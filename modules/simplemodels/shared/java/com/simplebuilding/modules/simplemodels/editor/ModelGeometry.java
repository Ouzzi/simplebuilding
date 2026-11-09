package com.simplebuilding.modules.simplemodels.editor;

import com.simplebuilding.modules.simplemodels.editor.EditableModel.Element;
import com.simplebuilding.modules.simplemodels.editor.EditableModel.Face;
import com.simplebuilding.modules.simplemodels.editor.EditableModel.Vec;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/**
 * Editor stage 2 (b-models13): the geometry of an {@link EditableModel} as vanilla 26.3 bakes it, in block pixels
 * (0..16 = one block). Same corner order per face as {@code FaceInfo}, same auto-UV as {@code FaceBakery.defaultFaceUV},
 * UV rotation as the vertex-index shift of {@code Quadrant}, element rotation as {@code CuboidRotation} (single axis:
 * {@code rotation(angle, axis)}; Euler: {@code rotationZYX(z, y, x)}; {@code rescale}: 1 / largest component of each
 * rotated unit axis). Only JOML, no client classes: the viewport draws from it and server gametests check it.
 */
public final class ModelGeometry {
    /** Centre of the editor view and the radius that fits one block (half its space diagonal, rounded up). */
    public static final Vector3f CENTRE = new Vector3f(8, 8, 8);
    public static final float VIEW_RADIUS = 14f;

    /** Corner extents per face and vertex (bit 4 = max x, 2 = max y, 1 = max z), vanilla {@code FaceInfo} order. */
    private static final int[][] FACE_CORNERS = {
            {0b001, 0b000, 0b100, 0b101}, // down
            {0b010, 0b011, 0b111, 0b110}, // up
            {0b110, 0b100, 0b000, 0b010}, // north
            {0b011, 0b001, 0b101, 0b111}, // south
            {0b010, 0b000, 0b001, 0b011}, // west
            {0b111, 0b101, 0b100, 0b110}, // east
    };
    /** Outward normals in {@link EditableModel#DIRECTIONS} order. */
    public static final float[][] NORMALS = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

    /** One face ready to draw: 4 corners (block pixels), 4 UV pairs in 0..1 of the sprite, the outward normal. */
    public record Quad(int element, String direction, Face face, float[] pos, float[] uv, Vector3f normal) {}

    /** The rotation part of an element (with rescale) as a 3x3 matrix; identity without rotation. */
    public static Matrix3f matrix(EditableModel.Rotation r) {
        Matrix3f m = new Matrix3f();
        if (r == null) return m;
        if (r.isEuler()) {
            m.rotationZYX(rad(r.euler().z()), rad(r.euler().y()), rad(r.euler().x()));
        } else if (r.angle() != 0) {
            int a = EditableModel.AXES.indexOf(r.axis());
            m.rotation(rad(r.angle()), a == 0 ? 1 : 0, a == 1 ? 1 : 0, a == 2 ? 1 : 0);
        }
        if (r.rescale() && !m.equals(new Matrix3f())) {
            Vector3f s = new Vector3f();
            for (int axis = 0; axis < 3; axis++) {
                Vector3f t = m.transform(new Vector3f(axis == 0 ? 1 : 0, axis == 1 ? 1 : 0, axis == 2 ? 1 : 0));
                s.setComponent(axis, 1f / Math.max(Math.abs(t.x), Math.max(Math.abs(t.y), Math.abs(t.z))));
            }
            m.scale(s);
        }
        return m;
    }

    /** {@code p} (block pixels) moved by the element rotation about its origin. */
    public static Vector3f transform(Element e, Vector3f p) {
        if (e.rotation() == null) return p;
        Vec o = e.rotation().origin();
        return matrix(e.rotation()).transform(p.sub(o.x(), o.y(), o.z())).add(o.x(), o.y(), o.z());
    }

    /** Corner {@code bits} (see {@link #FACE_CORNERS}) of the element, rotated. */
    public static Vector3f corner(Element e, int bits) {
        return transform(e, new Vector3f((bits & 4) != 0 ? e.to().x() : e.from().x(),
                (bits & 2) != 0 ? e.to().y() : e.from().y(), (bits & 1) != 0 ? e.to().z() : e.from().z()));
    }

    /** All eight corners, index = bits (x 4, y 2, z 1). */
    public static Vector3f[] corners(Element e) {
        Vector3f[] out = new Vector3f[8];
        for (int i = 0; i < 8; i++) out[i] = corner(e, i);
        return out;
    }

    /** Element centre (rotated). */
    public static Vector3f centre(Element e) {
        return transform(e, new Vector3f((e.from().x() + e.to().x()) / 2, (e.from().y() + e.to().y()) / 2, (e.from().z() + e.to().z()) / 2));
    }

    /** Vanilla auto-UV of a face (0..16), {@code FaceBakery.defaultFaceUV}. */
    public static EditableModel.Uv autoUv(Element e, String direction) {
        Vec f = e.from(), t = e.to();
        return switch (direction) {
            case "down" -> new EditableModel.Uv(f.x(), 16 - t.z(), t.x(), 16 - f.z());
            case "up" -> new EditableModel.Uv(f.x(), f.z(), t.x(), t.z());
            case "north" -> new EditableModel.Uv(16 - t.x(), 16 - t.y(), 16 - f.x(), 16 - f.y());
            case "south" -> new EditableModel.Uv(f.x(), 16 - t.y(), t.x(), 16 - f.y());
            case "west" -> new EditableModel.Uv(f.z(), 16 - t.y(), t.z(), 16 - f.y());
            case "east" -> new EditableModel.Uv(16 - t.z(), 16 - t.y(), 16 - f.z(), 16 - f.y());
            default -> throw new IllegalArgumentException(direction);
        };
    }

    /** One face as vanilla bakes it (corners rotated, UVs in 0..1 with face rotation applied). */
    public static Quad quad(int index, Element e, String direction) {
        int d = EditableModel.DIRECTIONS.indexOf(direction);
        Face face = e.faces().get(direction);
        EditableModel.Uv uvs = face.uv() != null ? face.uv() : autoUv(e, direction);
        int shift = Math.floorMod(face.rotation() / 90, 4);
        float[] pos = new float[12], uv = new float[8];
        for (int i = 0; i < 4; i++) {
            Vector3f p = corner(e, FACE_CORNERS[d][i]);
            pos[i * 3] = p.x; pos[i * 3 + 1] = p.y; pos[i * 3 + 2] = p.z;
            int k = (i + shift) % 4;
            uv[i * 2] = (k == 0 || k == 1 ? uvs.u1() : uvs.u2()) / 16f;
            uv[i * 2 + 1] = (k == 0 || k == 3 ? uvs.v1() : uvs.v2()) / 16f;
        }
        Vector3f normal = new Vector3f(NORMALS[d][0], NORMALS[d][1], NORMALS[d][2]);
        if (e.rotation() != null) matrix(e.rotation()).transform(normal).normalize();
        return new Quad(index, direction, face, pos, uv, normal);
    }

    /**
     * Index of the nearest element hit by the ray {@code origin + t * direction} (block pixels), or -1. Elements whose
     * index is in {@code hidden} are skipped. The ray goes into each element's own space (inverse rotation about its
     * origin) and meets the box {@code from..to} there; the ray parameter stays the same, so hits compare directly.
     */
    public static int pick(EditableModel model, Vector3f origin, Vector3f direction, java.util.Set<Integer> hidden) {
        int best = -1;
        float bestT = Float.POSITIVE_INFINITY;
        for (int i = 0; i < model.elements().size(); i++) {
            if (hidden.contains(i)) continue;
            float t = hit(model.elements().get(i), origin, direction);
            if (t < bestT) { bestT = t; best = i; }
        }
        return best;
    }

    /** Ray parameter of the first hit on the element, or +infinity. */
    public static float hit(Element e, Vector3f origin, Vector3f direction) {
        Vector3f o = new Vector3f(origin), d = new Vector3f(direction);
        if (e.rotation() != null) {
            Matrix3f inverse = matrix(e.rotation()).invert();
            Vec c = e.rotation().origin();
            inverse.transform(o.sub(c.x(), c.y(), c.z())).add(c.x(), c.y(), c.z());
            inverse.transform(d);
        }
        float near = Float.NEGATIVE_INFINITY, far = Float.POSITIVE_INFINITY;
        for (int a = 0; a < 3; a++) {
            float lo = Math.min(e.from().get(a), e.to().get(a)), hi = Math.max(e.from().get(a), e.to().get(a));
            float oa = o.get(a), da = d.get(a);
            if (Math.abs(da) < 1e-7f) {
                if (oa < lo - 1e-4f || oa > hi + 1e-4f) return Float.POSITIVE_INFINITY;
                continue;
            }
            float t1 = (lo - oa) / da, t2 = (hi - oa) / da;
            near = Math.max(near, Math.min(t1, t2));
            far = Math.min(far, Math.max(t1, t2));
        }
        return near <= far + 1e-5f && far >= 0 ? Math.max(near, 0) : Float.POSITIVE_INFINITY;
    }

    private static float rad(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    private ModelGeometry() {}
}
