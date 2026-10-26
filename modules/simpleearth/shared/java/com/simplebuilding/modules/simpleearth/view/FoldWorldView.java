package com.simplebuilding.modules.simpleearth.view;

/**
 * V25-V27 "Faltwelt" (owner 26.10., E30-3/E30-4: a mix of V23, V24 and V5). Climbing past the render height:
 * <ol>
 * <li>phase A "blend" (blend-in band): the flat map of the 5 x 5 tiles round the player fades in (the renderer's fade,
 * eased in and out);</li>
 * <li>phase B "fold" ({@code foldBand} blocks above that): the flat map folds into the earth cube - every hinge turns by the
 * same eased angle (V23/V24's shell), and the shell turns the point under the player towards the eye (the orbit of V13-V24).
 * Over a face middle (0 ~ 0) you end above the middle of a face, over an edge (half an edge from 0) above the middle of an
 * edge, over a corner above the corner - a corner has its own band and curve ({@code cornerFoldBand}, {@code cornerEasing});</li>
 * <li>V26/V27 then round the folded cube towards V5's body ({@code roundness}, {@code exponent}).</li>
 * </ol>
 * The eased curve: {@link #ease} (t^p / (t^p + (1 - t)^p), p = {@code easing}; 1 = linear, 2 = soft, larger = steeper middle).
 */
final class FoldWorldView extends EarthView {
    static final int HINGE = 0xFFB89CFF;
    /** Vertices per tile side (the tiles must bend when the cube rounds). */
    static final int GRID = 8;
    private final double defaultRoundness, defaultExponent;
    /** The last phase values (client test: the measured curve). */
    double lastBlend, lastFold;

    FoldWorldView(double roundness, double exponent) {
        this.defaultRoundness = roundness;
        this.defaultExponent = exponent;
    }

    /** Ease in and out with power p (p 1 = linear). */
    public static double ease(double t, double p) {
        double x = Meshes.clamp(t, 0, 1), a = Math.pow(x, p), b = Math.pow(1 - x, p);
        return a + b <= 0 ? x : a / (a + b);
    }

    @Override
    protected void reset(double u, double v, ViewParams p) {
    }

    @Override
    protected Scene frame(double px, double pz, double dt, ViewParams p) {
        Scene s = new Scene();
        int tx = tileOf(px), tz = tileOf(pz);
        double u = u(px), v = v(pz);
        // phase values from the height above the render height (blocks)
        double above = p.get("aboveSwitch", 1e9), band = Math.max(1, p.get("fadeBand", 128));
        double corner = Meshes.smooth(0.5, 0.95, Math.min(Math.abs(u), Math.abs(v)));
        double foldBand = Math.max(1, p.get("foldBand", 1024) * (1 - corner) + p.get("cornerFoldBand", 2048) * corner);
        double easing = p.get("easing", 2) * (1 - corner) + p.get("cornerEasing", 3) * corner;
        double blend = ease(above / band, 2);
        double fold = ease((above - band) / foldBand, Math.max(1, easing));
        lastBlend = blend;
        lastFold = fold;
        if (tx != curTx || tz != curTz) {
            s.event = tx != curTx && tz != curTz ? "corner" : "edge";
            if (fold > 0.01) s.fadeStart = true;
            curTx = tx;
            curTz = tz;
        }
        double angle = fold * Math.PI / 2;
        double[][] nx = profile(angle), nz = profile(angle);
        double rotation = Meshes.clamp(p.get("rotation", 1), 0, 1) * fold;
        V3 under = new V3(u, 1, v);
        Quat q = Quat.IDENTITY.slerp(Quat.fromUnitVectors(under.normalize(), V3.UP), rotation);
        double round = Meshes.clamp(p.get("roundness", defaultRoundness), 0, 1) * Meshes.smooth(0.6, 1, fold);
        double exponent = Math.max(2, p.get("exponent", defaultExponent));
        V3 below = q.rotate(shape(point(nx, nz, 2 + (u + 1) / 2, 2 + (v + 1) / 2), round, exponent));
        V3 offset = new V3(-below.x(), 0, -below.z());
        for (int j = -2; j <= 2; j++) {
            for (int i = -2; i <= 2; i++) {
                Patch patch = new Patch(GRID + 1, GRID + 1).tile(i, j, false);
                for (int b = 0; b <= GRID; b++) {
                    for (int a = 0; a <= GRID; a++) {
                        double fa = a / (double) GRID, fb = b / (double) GRID;
                        V3 at = shape(point(nx, nz, i + 2 + fa, j + 2 + fb), round, exponent);
                        patch.set(a, b, q.rotate(at).add(offset), fa, fb);
                    }
                }
                s.patches.add(patch);
            }
        }
        double lines = p.get("lineAlpha", 0.3) * fold;
        if (lines > 0.01) {
            for (int k = 1; k <= 4; k++) {
                for (int j = 0; j < 5; j++) {
                    for (int m = 0; m < GRID; m++) {
                        double f0 = j + m / (double) GRID, f1 = j + (m + 1) / (double) GRID;
                        s.line(q.rotate(shape(point(nx, nz, k, f0), round, exponent)).add(offset), q.rotate(shape(point(nx, nz, k, f1), round, exponent)).add(offset), lines, HINGE);
                        s.line(q.rotate(shape(point(nx, nz, f0, k), round, exponent)).add(offset), q.rotate(shape(point(nx, nz, f1, k), round, exponent)).add(offset), lines, HINGE);
                    }
                }
            }
        }
        s.closedBody = fold > 0.99;
        s.orientation = q;
        s.bodyOffset = offset;
        s.contact = new V3(0, below.y(), 0);
        return s;
    }

    /**
     * Node positions (along, drop) at the tile borders -5..5 of one axis: both hinge rings turn by the same angle (the outer
     * ring twice as far in total), so the map is flat at 0 and a closed cube at 90 degrees.
     */
    static double[][] profile(double a) {
        double[][] n = new double[6][];
        n[2] = new double[]{-1, 0};
        n[3] = new double[]{1, 0};
        n[4] = new double[]{n[3][0] + 2 * Math.cos(a), n[3][1] - 2 * Math.sin(a)};
        n[5] = new double[]{n[4][0] + 2 * Math.cos(2 * a), n[4][1] - 2 * Math.sin(2 * a)};
        n[1] = new double[]{n[2][0] - 2 * Math.cos(a), n[2][1] - 2 * Math.sin(a)};
        n[0] = new double[]{n[1][0] - 2 * Math.cos(2 * a), n[1][1] - 2 * Math.sin(2 * a)};
        return n;
    }

    /** A point of the shell at node coordinates (0..5 along each axis, fractions inside a tile; centre tile 2..3). */
    static V3 point(double[][] nx, double[][] nz, double ix, double iz) {
        double[] x = lerpNode(nx, ix), z = lerpNode(nz, iz);
        return new V3(x[0], 1 + x[1] + z[1], z[0]);
    }

    private static double[] lerpNode(double[][] n, double i) {
        int k = (int) Math.max(0, Math.min(4, Math.floor(i)));
        double f = i - k;
        return new double[]{n[k][0] + (n[k + 1][0] - n[k][0]) * f, n[k][1] + (n[k + 1][1] - n[k][1]) * f};
    }

    /** V26/V27: the folded cube pulled towards V5's rounded body (superellipsoid). */
    static V3 shape(V3 p, double round, double exponent) {
        if (round <= 1e-4) return p;
        return V3.lerp(p, Meshes.seProject(p, exponent), round);
    }

    /** Phase values of the last frame: blend, fold (client test). */
    public double[] phases() {
        return new double[]{lastBlend, lastFold};
    }
}
