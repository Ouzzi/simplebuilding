package com.simplebuilding.modules.simpleearth.view;

/**
 * V1 "Kachel-Würfel", V2 "Kegel-Faltung", V3 "Eckfenster + Umstülpen" (concept §3.3-3.6, earth-konzept.html): fixed tiles,
 * the cube is turned each frame by the shortest rotation that puts the point under the player on top; at an edge the faces
 * are relabelled; over a grid point the cube turns 180 degrees about the horizontal diagonal (V1/V2 0.35 s, V3 0.9 s with
 * tesseract lines, a scale pulse and the corner window before it).
 */
public final class CubeJumpView extends EarthView {
    static final int TESS = 0xFFC08CFF, WINDOW_FRAME = 0xFFD7B3FF;
    private final int mode;
    private Quat lastQ = Quat.IDENTITY;
    private Quat animFrom;
    private double animT, animDur;

    CubeJumpView(int mode) {
        this.mode = mode;
    }

    static Quat orient(double u, double v) {
        return Quat.fromUnitVectors(new V3(u, 1, v).normalize(), V3.UP);
    }

    @Override
    protected void reset(double u, double v, ViewParams p) {
        lastQ = orient(u, v);
        animFrom = null;
    }

    @Override
    protected Scene frame(double px, double pz, double dt, ViewParams p) {
        Scene s = new Scene();
        int tx = tileOf(px), tz = tileOf(pz), dtx = tx - curTx, dtz = tz - curTz;
        double u = (px - tx * edge) / half, v = (pz - tz * edge) / half;
        Quat q = orient(u, v);
        if (dtx != 0 || dtz != 0) {
            if (dtx != 0 && dtz != 0) {
                // corner of the world: 180 degrees about the horizontal diagonal, the display starts at q_old * R
                V3 axis = new V3(Integer.signum(dtx), 0, -Integer.signum(dtz)).normalize();
                Quat r = Quat.axisAngle(axis, Math.PI);
                animFrom = q.conjugate().mul(lastQ).mul(r);
                animT = 0;
                animDur = mode == 3 ? p.get("inversion", 0.9) : p.get("cornerTurn", 0.35);
                s.event = "corner";
            } else {
                s.event = "edge";
            }
            curTx = tx;
            curTz = tz;
        }
        Quat qd = q;
        double pulse = 1, tessAlpha = 0;
        if (animFrom != null) {
            animT += dt / Math.max(0.05, animDur);
            double e = Math.min(1, animT), k = e * e * (3 - 2 * e);
            qd = q.mul(animFrom.slerp(Quat.IDENTITY, k));
            if (mode == 3) {
                tessAlpha = Math.sin(Math.PI * e) * 0.9;
                pulse = 1 - 0.12 * Math.sin(Math.PI * e);
            }
            if (e >= 1) animFrom = null;
        }
        lastQ = q;
        double r = p.get("rounding", 0.07);
        for (int f = 0; f < 6; f++) {
            Patch face = Meshes.cubeFace(f, r, 14).tile(Meshes.FACE_TILES[f][0], Meshes.FACE_TILES[f][1], false);
            face.folded = mode == 2;
            face.transform(qd, V3.ZERO).scale(pulse);
            s.patches.add(face);
        }
        if (tessAlpha > 0.01) tesseract(s, qd, pulse, tessAlpha);
        if (mode == 3 && animFrom == null) window(s, qd, u, v, p);
        s.orientation = qd;
        s.bodyScale = pulse;
        s.contact = qd.rotate(Meshes.round(new V3(u, 1, v), r)).mul(pulse);
        return s;
    }

    /** Inner wire cube (0.42) and its connectors to the outer corners, the classic tesseract projection. */
    static void tesseract(Scene s, Quat q, double scale, double alpha) {
        int[][] c = {{-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1}, {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}};
        int[][] e = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        double in = 0.42;
        for (int[] ab : e) {
            s.line(q.rotate(corner(c[ab[0]], in)).mul(scale), q.rotate(corner(c[ab[1]], in)).mul(scale), alpha, TESS);
        }
        for (int[] k : c) s.line(q.rotate(corner(k, in)).mul(scale), q.rotate(corner(k, 1.0)).mul(scale), alpha, TESS);
    }

    private static V3 corner(int[] c, double s) {
        return new V3(c[0] * s, c[1] * s, c[2] * s);
    }

    /**
     * V3 corner window: the diagonal tile grows out of the corner as a small fourth face. WE25-2: it used to be a flat triangle
     * across the sharp corner, which stood off the rounded body like a loose plate; now the triangle is projected onto the
     * rounded corner (a hair above it), so the window lies on the body and only its frame marks it.
     */
    private static void window(Scene s, Quat q, double u, double v, ViewParams p) {
        int su = Meshes.sg(u), sv = Meshes.sg(v);
        double near = Math.min(Math.abs(u), Math.abs(v)), start = p.get("windowStart", 0.5);
        double ws = Meshes.clamp((near - start) / Math.max(0.01, 1 - start), 0, 1);
        if (ws <= 0.01) return;
        double size = 0.08 + p.get("windowSize", 0.62) * ws, r = p.get("rounding", 0.07);
        V3 corner = new V3(su, 1, sv);
        V3 a = corner.add(new V3(-su * size, 0, 0)), b = corner.add(new V3(0, -size, 0)), c = corner.add(new V3(0, 0, -sv * size));
        int n = 8;
        Patch w = new Patch(n + 1, n + 1).tile(su, sv, false);
        for (int j = 0; j <= n; j++) {
            for (int i = 0; i <= n; i++) {
                double fu = i / (double) n, fv = j / (double) n;
                V3 flat = V3.lerp(V3.lerp(a, c, fu), b, fv);
                w.set(i, j, onBody(flat, r), fu * (1 - fv) + 0.5 * fv, fv);
            }
        }
        w.transform(q, V3.ZERO);
        w.overlay = true;
        w.alpha = Math.min(1, ws * 2);
        s.patches.add(w);
        for (int k = 0; k < n; k++) {
            double f0 = k / (double) n, f1 = (k + 1) / (double) n;
            s.line(q.rotate(onBody(V3.lerp(a, c, f0), r)), q.rotate(onBody(V3.lerp(a, c, f1), r)), w.alpha, WINDOW_FRAME);
            s.line(q.rotate(onBody(V3.lerp(c, b, f0), r)), q.rotate(onBody(V3.lerp(c, b, f1), r)), w.alpha, WINDOW_FRAME);
            s.line(q.rotate(onBody(V3.lerp(b, a, f0), r)), q.rotate(onBody(V3.lerp(b, a, f1), r)), w.alpha, WINDOW_FRAME);
        }
    }

    /** A point pushed radially onto the rounded cube (edge length 2, rounding r), 0.4 % above it. */
    public static V3 onBody(V3 pt, double r) {
        double m = Math.max(Math.abs(pt.x()), Math.max(Math.abs(pt.y()), Math.abs(pt.z())));
        V3 onCube = pt.mul(1 / Math.max(1e-9, m));
        return Meshes.round(onCube, r).mul(1.004);
    }
}
