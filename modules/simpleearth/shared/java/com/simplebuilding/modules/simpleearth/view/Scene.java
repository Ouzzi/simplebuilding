package com.simplebuilding.modules.simpleearth.view;

import java.util.ArrayList;
import java.util.List;

/**
 * What one frame of an earth version shows, in view space (cube half edge 1, +Y up towards the player): the pieces, extra
 * lines (tesseract edges, hinges, window frames), the surface point under the player ({@code contact}) and the current tile
 * (relative to the activation point). {@code fadeStart} asks the renderer for a 0.35 s cross-fade from the previous frame
 * (the previews' "Bild-Überblendung"); {@code event} names what just happened (edge, corner, jump) for tests and sounds.
 */
public final class Scene {
    public final List<Patch> patches = new ArrayList<>();
    /** Line segments: x0 y0 z0 x1 y1 z1 alpha, ARGB colour in {@link #lineColors}. */
    public final List<double[]> lines = new ArrayList<>();
    public final List<Integer> lineColors = new ArrayList<>();
    public V3 contact = V3.UP;
    public int curTx, curTz;
    /** Body orientation for lighting and the atmosphere shell (identity for the nets). */
    public Quat orientation = Quat.IDENTITY;
    public V3 bodyOffset = V3.ZERO;
    public double bodyScale = 1;
    /** Whether the body is a closed cube/ball the atmosphere shell may wrap (false for the nets). */
    public boolean closedBody = true;
    public boolean fadeStart;
    /** E29-2: the faces use the equiangular cube mapping (world position runs with the angle seen from the centre). */
    public boolean equiangular;
    /**
     * E29-2: how much higher the ground under the player lies than the face centres (cube corners stick out); the renderer
     * lowers the eye by it so the eye keeps the same distance from the earth's centre - an orbit.
     */
    public double eyeDrop;
    /** E31-4: the body is a rounded cube |x|^e + |y|^e + |z|^e = 1 with this exponent (0 = a plain cube): the atmosphere follows it. */
    public double bodyExponent;
    public String event = "";

    public void line(V3 a, V3 b, double alpha, int argb) {
        lines.add(new double[]{a.x(), a.y(), a.z(), b.x(), b.y(), b.z(), alpha});
        lineColors.add(argb);
    }

    public boolean finite() {
        if (!contact.finite() || !orientation.finite()) return false;
        for (Patch p : patches) for (V3 v : p.pos) if (v == null || !v.finite()) return false;
        for (double[] l : lines) for (double d : l) if (!Double.isFinite(d)) return false;
        return true;
    }

    /** View-space position of the world point at tile offset {@code (tx, tz)} and tile uv, from the first patch showing that tile unmixed. */
    public V3 locate(int tx, int tz, double tu, double tv) {
        if (equiangular) {
            // world fraction -> face coordinate of the equiangular cube
            tu = (Math.tan(Math.PI / 4 * (2 * tu - 1)) + 1) / 2;
            tv = (Math.tan(Math.PI / 4 * (2 * tv - 1)) + 1) / 2;
        }
        for (Patch p : patches) {
            if (p.kind != Patch.TERRAIN || p.overlay || p.tx != tx || p.tz != tz || p.alpha < 0.5 || p.mix > 0.5) continue;
            V3 at = p.locate(tu, tv);
            if (at != null) return at;
        }
        return null;
    }
}
