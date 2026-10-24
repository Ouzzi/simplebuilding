package com.simplebuilding.modules.simpleearth.view;

/**
 * V6 "Faltnetz" (concept §3.7, wa24-6-earth-v6.html) and its tesseract descendants V7/V8: nothing turns, the tile under the
 * player stays level and the neighbours fold in from vertical to flat from the tile's middle on (17 hinged pieces, corner
 * tiles twice - on the east/west and on the north/south carrier; whichever carrier lies flat shows its copy).
 * <ul>
 * <li>{@link Style#PLAIN} V6: as the preview, far parts of the corner area cross-fade.</li>
 * <li>{@link Style#CELLS} V7 "Zellen-Netz": each piece is a flat cube cell (walls + wire edges) - the unfolding 3D net of a
 * 4D cube (Dalí cross).</li>
 * <li>{@link Style#INVERSION} V8 "Netz + Umstülpen": instead of the cross-fade at a corner the V3 tesseract inversion runs
 * (inner wire cube, pulse), and triangular corner windows fill the gaps between two folded sides.</li>
 * </ul>
 */
final class FoldingNetView extends EarthView {
    enum Style { PLAIN, CELLS, INVERSION }

    static final int WALL = 0xFF3A3352, EDGE = 0xFFB89CFF;
    /** parent, pivot xyz, out xyz (0 = root), tile offset, quad x0 x1 z0 z1, fold (0 none, 1 E, 2 W, 3 N, 4 S, 5 F), alpha key. */
    private static final double[][] PIECES = {
            {-1, 0, 1, 0, 0, 0, 0, 0, 0, -1, 1, -1, 1, 0, 0},
            {0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 2, -1, 1, 1, 0},
            {1, 2, 0, 0, 1, 0, 0, 2, 0, 0, 2, -1, 1, 5, 5},
            {1, 1, 0, -1, 0, 0, -1, 1, -1, -1, 1, -2, 0, 3, 1},
            {1, 1, 0, 1, 0, 0, 1, 1, 1, -1, 1, 0, 2, 4, 1},
            {0, -1, 0, 0, -1, 0, 0, -1, 0, -2, 0, -1, 1, 2, 0},
            {5, -2, 0, 0, -1, 0, 0, -2, 0, -2, 0, -1, 1, 5, 5},
            {5, -1, 0, -1, 0, 0, -1, -1, -1, -1, 1, -2, 0, 3, 2},
            {5, -1, 0, 1, 0, 0, 1, -1, 1, -1, 1, 0, 2, 4, 2},
            {0, 0, 0, -1, 0, 0, -1, 0, -1, -1, 1, -2, 0, 3, 0},
            {9, 0, 0, -2, 0, 0, -1, 0, -2, -1, 1, -2, 0, 5, 6},
            {9, 1, 0, -1, 1, 0, 0, 1, -1, 0, 2, -1, 1, 1, 3},
            {9, -1, 0, -1, -1, 0, 0, -1, -1, -2, 0, -1, 1, 2, 3},
            {0, 0, 0, 1, 0, 0, 1, 0, 1, -1, 1, 0, 2, 4, 0},
            {13, 0, 0, 2, 0, 0, 1, 0, 2, -1, 1, 0, 2, 5, 6},
            {13, 1, 0, 1, 1, 0, 0, 1, 1, 0, 2, -1, 1, 1, 4},
            {13, -1, 0, 1, -1, 0, 0, -1, 1, -2, 0, -1, 1, 2, 4},
    };

    private final Style style;
    private double animT = -1;

    FoldingNetView(Style style) {
        this.style = style;
    }

    @Override
    protected void reset(double u, double v, ViewParams p) {
        animT = -1;
    }

    /** Fold angle of a neighbour: vertical (90 degrees) up to the fold start, flat at the edge. */
    static double fold(double s, double start) {
        return Math.PI / 2 * (1 - Meshes.smooth(start, 1, s));
    }

    @Override
    protected Scene frame(double px, double pz, double dt, ViewParams p) {
        Scene s = new Scene();
        s.closedBody = false;
        int tx = tileOf(px), tz = tileOf(pz);
        boolean crossed = false, diagonal = false;
        while (tx != curTx || tz != curTz) {
            if (tx != curTx) curTx += Integer.signum(tx - curTx);
            else curTz += Integer.signum(tz - curTz);
            if (crossed) diagonal = true;
            crossed = true;
        }
        double u = u(px), v = v(pz);
        double other = crossed ? (Math.abs(u) > Math.abs(v) ? Math.abs(v) : Math.abs(u)) : 0;
        if (crossed) s.event = diagonal ? "corner" : "edge";
        boolean cornerEvent = crossed && (other > 0.7 || diagonal);
        if (cornerEvent) {
            if (style == Style.INVERSION) animT = 0;
            else s.fadeStart = true;
        }
        double start = p.get("foldStart", 0.1);
        double[] f = {0, fold(u, start), fold(-u, start), fold(-v, start), fold(v, start), Math.PI / 2};
        double[] alpha = {1, 1 - Meshes.smooth(0.06, 0.35, f[1]), 1 - Meshes.smooth(0.06, 0.35, f[2]),
                1 - Meshes.smooth(0.06, 0.35, f[3]), 1 - Meshes.smooth(0.06, 0.35, f[4]),
                1 - Meshes.smooth(0.75, 0.97, Math.abs(v)), 1 - Meshes.smooth(0.75, 0.97, Math.abs(u))};
        double pulse = 1, tess = 0;
        if (animT >= 0) {
            animT += dt / Math.max(0.05, p.get("inversion", 0.9));
            double e = Math.min(1, animT);
            pulse = 1 - 0.12 * Math.sin(Math.PI * e);
            tess = Math.sin(Math.PI * e) * 0.9;
            if (e >= 1) animT = -1;
        }
        V3 root = new V3(-u, 0, -v);
        Quat[] rot = new Quat[PIECES.length];
        V3[] at = new V3[PIECES.length];
        double depth = p.get("cellDepth", 0.18), lineAlpha = p.get("lineAlpha", 0.6);
        for (int i = 0; i < PIECES.length; i++) {
            double[] d = PIECES[i];
            int parent = (int) d[0];
            V3 pivot = new V3(d[1], d[2], d[3]);
            Quat local = Quat.IDENTITY;
            if (parent >= 0) {
                V3 axis = new V3(d[4], d[5], d[6]).cross(new V3(0, -1, 0)).normalize();
                local = Quat.axisAngle(axis, f[(int) d[13]]);
                rot[i] = rot[parent].mul(local);
                at[i] = at[parent].add(rot[parent].rotate(pivot));
            } else {
                rot[i] = Quat.IDENTITY;
                at[i] = root.add(pivot);
            }
            double a = alpha[(int) d[14]];
            if (a <= 0.01) continue;
            Patch q = Meshes.quad(d[9], d[10], d[11], d[12]).tile((int) d[7], (int) d[8], false);
            q.alpha = a;
            q.transform(rot[i], at[i]).scale(pulse);
            s.patches.add(q);
            if (style == Style.CELLS) cell(s, rot[i], at[i], d, a, depth, lineAlpha, pulse);
        }
        if (style == Style.INVERSION) {
            windows(s, root, rot, at, u, v, alpha, p, pulse);
            if (tess > 0.01) {
                Scene t = new Scene();
                CubeJumpView.tesseract(t, Quat.IDENTITY, 1, tess);
                for (int k = 0; k < t.lines.size(); k++) {
                    double[] l = t.lines.get(k);
                    V3 a0 = new V3(l[0], l[1], l[2]).add(root).mul(pulse), b0 = new V3(l[3], l[4], l[5]).add(root).mul(pulse);
                    s.line(a0, b0, l[6], CubeJumpView.TESS);
                }
            }
        }
        s.bodyScale = pulse;
        s.contact = V3.UP.mul(pulse);
        return s;
    }

    /** V7: walls below a piece (cell depth) and its wire edges. */
    private static void cell(Scene s, Quat r, V3 t, double[] d, double alpha, double depth, double lineAlpha, double scale) {
        double x0 = d[9], x1 = d[10], z0 = d[11], z1 = d[12];
        double[][] edges = {{x0, z0, x1, z0}, {x1, z0, x1, z1}, {x1, z1, x0, z1}, {x0, z1, x0, z0}};
        for (double[] e : edges) {
            Patch w = new Patch(2, 2);
            w.set(0, 0, new V3(e[0], 0, e[1]), 0, 0);
            w.set(1, 0, new V3(e[2], 0, e[3]), 1, 0);
            w.set(0, 1, new V3(e[0], -depth, e[1]), 0, 1);
            w.set(1, 1, new V3(e[2], -depth, e[3]), 1, 1);
            w.kind = Patch.COLOR;
            w.color = WALL;
            w.alpha = alpha;
            w.overlay = true;
            w.transform(r, t).scale(scale);
            s.patches.add(w);
            V3 a = r.rotate(new V3(e[0], 0, e[1])).add(t).mul(scale), b = r.rotate(new V3(e[2], 0, e[3])).add(t).mul(scale);
            V3 a2 = r.rotate(new V3(e[0], -depth, e[1])).add(t).mul(scale), b2 = r.rotate(new V3(e[2], -depth, e[3])).add(t).mul(scale);
            s.line(a, b, lineAlpha * alpha, EDGE);
            s.line(a2, b2, lineAlpha * alpha * 0.6, EDGE);
            s.line(a, a2, lineAlpha * alpha * 0.6, EDGE);
        }
    }

    /**
     * V8 corner windows: between the folded east/west and north/south side a triangle of the diagonal tile spans from the top
     * corner to both sides' far corners, so all four tiles round a grid point are always visible; it fades out while the real
     * corner piece fades in.
     */
    private static void windows(Scene s, V3 root, Quat[] rot, V3[] at, double u, double v, double[] alpha, ViewParams p, double scale) {
        double start = p.get("windowStart", 0.5);
        for (int su = -1; su <= 1; su += 2) {
            for (int sv = -1; sv <= 1; sv += 2) {
                double near = Math.min(su * u, sv * v);
                double ws = Meshes.clamp((near - start) / Math.max(0.01, 1 - start), 0, 1);
                int side = su > 0 ? 1 : 5, front = sv > 0 ? 13 : 9;
                double real = Math.max(alpha[su > 0 ? 1 : 2], alpha[sv > 0 ? 4 : 3]);
                double a = ws * (1 - real);
                if (a <= 0.01) continue;
                V3 c = root.add(new V3(su, 1, sv));
                V3 e = rot[side].rotate(new V3(su * 2, 0, sv)).add(at[side]);
                V3 n = rot[front].rotate(new V3(su, 0, sv * 2)).add(at[front]);
                double cu = su > 0 ? 0 : 1, cv = sv > 0 ? 0 : 1;
                Patch w = new Patch(2, 2).tile(su, sv, false);
                w.set(0, 0, c, cu, cv);
                w.set(1, 0, e, 1 - cu, cv);
                w.set(0, 1, n, cu, 1 - cv);
                w.set(1, 1, n, cu, 1 - cv);
                w.overlay = true;
                w.alpha = a;
                w.scale(scale);
                s.patches.add(w);
                s.line(c.mul(scale), e.mul(scale), a, CubeJumpView.WINDOW_FRAME);
                s.line(e.mul(scale), n.mul(scale), a, CubeJumpView.WINDOW_FRAME);
                s.line(n.mul(scale), c.mul(scale), a, CubeJumpView.WINDOW_FRAME);
            }
        }
    }
}
