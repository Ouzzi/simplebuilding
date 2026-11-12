package com.simpledinos.entity.part;

import java.util.List;
import net.minecraft.world.phys.AABB;

/**
 * The bones of a dinosaur model as plain numbers (a-dino36b), so the server knows where the client draws the head and the tail.
 * Same math as the client: a bone moves to its pivot and turns Z * Y * X (ModelPart, children inherit), the renderer scales the model,
 * turns it by the body yaw, model y = 24 is the ground and model -z is the front (LivingEntityRenderer). Units are model pixels
 * (1/16 block). The bone data is generated with the client layers ({@code tools/gen_models.py}), so both always match.
 */
public final class Skeleton {
    /** A bone: name, parent index (-1 = root; parents come first), pivot, rest rotation (radians), boxes as x, y, z, w, h, d groups. */
    public record Bone(String name, int parent, float px, float py, float pz, float rx, float ry, float rz, float[] boxes) {}

    private static final float[] NONE = {0.0F, 0.0F, 0.0F};
    private final Bone[] bones;

    public Skeleton(Bone... bones) {
        this.bones = bones;
        for (int i = 0; i < bones.length; i++) {
            if (bones[i].parent() >= i) throw new IllegalArgumentException("parent after child: " + bones[i].name());
        }
    }

    public int size() {
        return bones.length;
    }

    public int index(String name) {
        for (int i = 0; i < bones.length; i++) if (bones[i].name().equals(name)) return i;
        throw new IllegalArgumentException("no bone " + name);
    }

    /**
     * Adds the world boxes of the {@code which} bones to {@code out}: each turned cube becomes the axis-aligned box around its eight
     * corners. {@code extra[i]} is an extra rotation {x, y, z} (radians) of bone i on top of its rest pose, or null; {@code scale} is the
     * drawn model scale (model scale x growth), {@code yawDeg} the body yaw, (ox, oy, oz) the entity position (feet).
     */
    public void boxes(float[][] extra, int[] which, double scale, float yawDeg, double ox, double oy, double oz, List<AABB> out) {
        int n = bones.length;
        double[][] rot = new double[n][];
        double[][] pos = new double[n][];
        for (int i = 0; i < n; i++) {
            Bone b = bones[i];
            float[] e = extra != null && extra[i] != null ? extra[i] : NONE;
            double[] r = rotationZyx(b.rx() + e[0], b.ry() + e[1], b.rz() + e[2]);
            double[] t = {b.px(), b.py(), b.pz()};
            if (b.parent() >= 0) {
                double[] pr = rot[b.parent()];
                double[] moved = apply(pr, t);
                double[] pp = pos[b.parent()];
                t = new double[] {pp[0] + moved[0], pp[1] + moved[1], pp[2] + moved[2]};
                r = mul(pr, r);
            }
            rot[i] = r;
            pos[i] = t;
        }
        double yaw = Math.toRadians(yawDeg), cos = Math.cos(yaw), sin = Math.sin(yaw), s = scale / 16.0;
        for (int i : which) {
            float[] c = bones[i].boxes();
            for (int k = 0; k + 5 < c.length; k += 6) {
                double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
                double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
                for (int corner = 0; corner < 8; corner++) {
                    double[] p = {c[k] + ((corner & 1) != 0 ? c[k + 3] : 0), c[k + 1] + ((corner & 2) != 0 ? c[k + 4] : 0),
                            c[k + 2] + ((corner & 4) != 0 ? c[k + 5] : 0)};
                    double[] m = apply(rot[i], p);
                    double lx = (pos[i][0] + m[0]) * s, ly = (24.0 - pos[i][1] - m[1]) * s, lz = -(pos[i][2] + m[2]) * s;
                    double wx = ox + lx * cos - lz * sin, wz = oz + lx * sin + lz * cos, wy = oy + ly;
                    minX = Math.min(minX, wx); maxX = Math.max(maxX, wx);
                    minY = Math.min(minY, wy); maxY = Math.max(maxY, wy);
                    minZ = Math.min(minZ, wz); maxZ = Math.max(maxZ, wz);
                }
                out.add(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
            }
        }
    }

    /** Row-major 3 x 3 of Rz * Ry * Rx (JOML {@code rotationZYX}, as ModelPart uses it). */
    static double[] rotationZyx(double x, double y, double z) {
        double cx = Math.cos(x), sx = Math.sin(x), cy = Math.cos(y), sy = Math.sin(y), cz = Math.cos(z), sz = Math.sin(z);
        return new double[] {
                cz * cy, cz * sy * sx - sz * cx, cz * sy * cx + sz * sx,
                sz * cy, sz * sy * sx + cz * cx, sz * sy * cx - cz * sx,
                -sy, cy * sx, cy * cx};
    }

    static double[] mul(double[] a, double[] b) {
        double[] r = new double[9];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) r[i * 3 + j] = a[i * 3] * b[j] + a[i * 3 + 1] * b[3 + j] + a[i * 3 + 2] * b[6 + j];
        }
        return r;
    }

    static double[] apply(double[] m, double[] v) {
        return new double[] {m[0] * v[0] + m[1] * v[1] + m[2] * v[2], m[3] * v[0] + m[4] * v[1] + m[5] * v[2], m[6] * v[0] + m[7] * v[1] + m[8] * v[2]};
    }
}
