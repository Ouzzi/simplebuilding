package com.simplebuilding.blocks.custom;

import java.util.ArrayList;
import java.util.List;

/**
 * What a hammock looks like (docs/ai/PLAN-HAENGEMATTE-WINKEL-2026-10-02.md): boxes in world coordinates along the line
 * between the two anchor centres, mirror-symmetric about its middle. Drawn by {@code HammockRenderer}; the preview in
 * {@code tools/textures/hammock.py} computes the same boxes. No client classes, so tests can check the geometry.
 *
 * <ul>
 *   <li>cloth: two plates (12 px wide, 1 px thick) from the middle, 3 px above the cloth layer, rising
 *       {@link HammockLayout#SAG_DEGREES} towards each end, with a 3 px hem on both sides;</li>
 *   <li>spreaders: stripped-oak bars (16 px wide) at the cloth ends, 9.5 px above the cloth layer;</li>
 *   <li>ropes: from both spreader ends up to a knot where the line leaves the anchor block, 8 px above the anchor's
 *       floor, and a tie from the knot 7/8 of the way to the anchor centre (reaches a 2 px rod; hidden in full blocks).</li>
 * </ul>
 */
public final class HammockShape {
    private static final double PX = 1.0 / 16.0;
    private static final double SAG = Math.toRadians(HammockLayout.SAG_DEGREES);
    /** Horizontal half length of the sagging cloth (the spreaders sit there). */
    public static final double HALF = Math.cos(SAG);

    private HammockShape() {
    }

    public enum Part {
        CLOTH, HEM, SPREADER, ROPE, KNOT, TIE;

        /** Wool of the hammock's colour, stripped oak, or the rope texture. */
        public boolean wool() {
            return this == CLOTH || this == HEM;
        }
    }

    /**
     * A box: centre, a right-handed frame (a along, b across, c = a x b "up") and half extents along each axis. UVs are
     * a pixel rectangle on the part's texture.
     */
    public record Box(Part part, double[] centre, double[] a, double[] b, double[] c, double ha, double hb, double hc) {
        /** The eight corners (for previews and bounds). */
        public List<double[]> corners() {
            List<double[]> out = new ArrayList<>(8);
            for (int i = -1; i <= 1; i += 2) {
                for (int j = -1; j <= 1; j += 2) {
                    for (int k = -1; k <= 1; k += 2) {
                        out.add(new double[] {
                                centre[0] + i * ha * a[0] + j * hb * b[0] + k * hc * c[0],
                                centre[1] + i * ha * a[1] + j * hb * b[1] + k * hc * c[1],
                                centre[2] + i * ha * a[2] + j * hb * b[2] + k * hc * c[2]});
                    }
                }
            }
            return out;
        }
    }

    /** The boxes of the hammock {@code spot}, in world coordinates (blocks). */
    public static List<Box> boxes(HammockLayout.Spot spot) {
        List<Box> out = new ArrayList<>();
        double ux = spot.ux();
        double uz = spot.uz();
        double length = spot.length();
        double mx = spot.middleX();
        double mz = spot.middleZ();
        double clothY = spot.anchor().getY() - 1;
        double[] up = {0, 1, 0};
        double[] across = {-uz, 0, ux};
        // where the line leaves the anchor block (from its centre), and the knot height
        double exit = 0.5 / Math.max(Math.abs(ux), Math.abs(uz));
        double knotY = spot.anchor().getY() + 8 * PX;
        double spreaderY = clothY + 9.5 * PX;
        for (int s = -1; s <= 1; s += 2) {
            double[] tilt = {s * ux * Math.cos(SAG), Math.sin(SAG), s * uz * Math.cos(SAG)};
            double[][] f = frame(tilt, across);
            double[] origin = {mx, clothY + 3 * PX, mz};
            // cloth plate: bottom edge through the origin, 16 px along the tilt, 1 px thick
            out.add(new Box(Part.CLOTH, add(origin, f[0], 0.5, f[2], 0.5 * PX, f[1], 0), f[0], f[1], f[2], 0.5, 6 * PX, 0.5 * PX));
            for (int side = -1; side <= 1; side += 2) {
                out.add(new Box(Part.HEM, add(origin, f[0], 0.5, f[2], 1.5 * PX, f[1], side * 6.5 * PX), f[0], f[1], f[2],
                        0.5, 0.5 * PX, 1.5 * PX));
            }
            double[] u = {s * ux, 0, s * uz};
            double[][] flat = frame(u, across);
            double[] spreader = {mx + s * HALF * ux, spreaderY, mz + s * HALF * uz};
            out.add(new Box(Part.SPREADER, spreader, flat[0], flat[1], flat[2], PX, 8 * PX, PX));
            double[] knot = {mx + s * (length / 2 - exit) * ux, knotY, mz + s * (length / 2 - exit) * uz};
            for (int side = -1; side <= 1; side += 2) {
                double[] from = add(spreader, across, side * 7 * PX, up, 0, up, 0);
                out.add(strand(from, knot));
            }
            out.add(new Box(Part.KNOT, add(knot, flat[0], -0.25 * PX, up, 0, up, 0), flat[0], flat[1], flat[2], 1.25 * PX, PX, 1.25 * PX));
            double tie = exit * 7.0 / 8.0;
            out.add(new Box(Part.TIE, add(knot, flat[0], tie / 2, up, 0, up, 0), flat[0], flat[1], flat[2], tie / 2, 0.5 * PX, 0.5 * PX));
        }
        return out;
    }

    /** A 1 px rope column from {@code from} to {@code to}. */
    private static Box strand(double[] from, double[] to) {
        double[] d = {to[0] - from[0], to[1] - from[1], to[2] - from[2]};
        double len = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
        double[] dir = {d[0] / len, d[1] / len, d[2] / len};
        // across: horizontal and perpendicular to the strand
        double[] side = {-dir[2], 0, dir[0]};
        if (Math.abs(side[0]) + Math.abs(side[2]) < 1.0E-6) {
            side = new double[] {1, 0, 0};
        }
        double[][] f = frame(dir, side);
        double[] centre = {(from[0] + to[0]) / 2, (from[1] + to[1]) / 2, (from[2] + to[2]) / 2};
        return new Box(Part.ROPE, centre, f[0], f[1], f[2], len / 2, 0.5 * PX, 0.5 * PX);
    }

    /** Right-handed orthonormal frame from {@code a} and a rough {@code b}; c = a x b points up (b flipped if needed). */
    private static double[][] frame(double[] a, double[] b) {
        double[] na = norm(a);
        double dot = na[0] * b[0] + na[1] * b[1] + na[2] * b[2];
        double[] nb = norm(new double[] {b[0] - dot * na[0], b[1] - dot * na[1], b[2] - dot * na[2]});
        double[] c = cross(na, nb);
        if (c[1] < 0) {
            nb = new double[] {-nb[0], -nb[1], -nb[2]};
            c = cross(na, nb);
        }
        return new double[][] {na, nb, c};
    }

    private static double[] add(double[] p, double[] a, double ka, double[] b, double kb, double[] c, double kc) {
        return new double[] {p[0] + a[0] * ka + b[0] * kb + c[0] * kc, p[1] + a[1] * ka + b[1] * kb + c[1] * kc,
                p[2] + a[2] * ka + b[2] * kb + c[2] * kc};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] norm(double[] v) {
        double l = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new double[] {v[0] / l, v[1] / l, v[2] / l};
    }
}
