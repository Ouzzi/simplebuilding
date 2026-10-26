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
    private String lastBottom = "";

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
        // E31-1: the flat map reaches renderDistance rings; folded, only the cube's six faces stay (see alpha)
        int reach = Math.max(2, Math.min(5, (int) Math.round(p.get("renderDistance", 2))));
        double[][] nx = profile(angle, reach), nz = profile(angle, reach);
        double rotation = Meshes.clamp(p.get("rotation", 1), 0, 1) * fold;
        V3 under = new V3(u, 1, v);
        Quat q = Quat.IDENTITY.slerp(Quat.fromUnitVectors(under.normalize(), V3.UP), rotation);
        double round = Meshes.clamp(p.get("roundness", defaultRoundness), 0, 1) * Meshes.smooth(0.6, 1, fold);
        double exponent = Math.max(2, p.get("exponent", defaultExponent));
        V3 below = q.rotate(shape(point(nx, nz, reach + (u + 1) / 2, reach + (v + 1) / 2), round, exponent));
        V3 offset = new V3(-below.x(), 0, -below.z());
        int su = Meshes.sg(u), sv = Meshes.sg(v);
        int[] bottom = Math.abs(u) >= Math.abs(v) ? new int[]{2 * su, 0} : new int[]{0, 2 * sv};
        String bottomKey = bottom[0] + "," + bottom[1];
        if (fold > 0.5 && !lastBottom.isEmpty() && !bottomKey.equals(lastBottom)) s.fadeStart = true;
        lastBottom = bottomKey;
        double lines = p.get("lineAlpha", 0.3) * fold;
        for (int j = -reach; j <= reach; j++) {
            for (int i = -reach; i <= reach; i++) {
                double alpha = tileAlpha(i, j, bottom, fold);
                if (alpha <= 0.005) continue;
                Patch patch = new Patch(GRID + 1, GRID + 1).tile(i, j, false);
                for (int b = 0; b <= GRID; b++) {
                    for (int a = 0; a <= GRID; a++) {
                        double fa = a / (double) GRID, fb = b / (double) GRID;
                        V3 at = shape(point(nx, nz, i + reach + fa, j + reach + fb), round, exponent);
                        patch.set(a, b, q.rotate(at).add(offset), fa, fb);
                    }
                }
                patch.alpha = alpha;
                s.patches.add(patch);
                if (lines * alpha > 0.01) {
                    // the tile's own border as hinge lines (only tiles that stay part of the cube)
                    for (int m = 0; m < GRID; m++) {
                        for (int side = 0; side < 4; side++) {
                            int a0 = side == 0 ? m : side == 1 ? GRID : side == 2 ? m : 0, b0 = side == 0 ? 0 : side == 1 ? m : side == 2 ? GRID : m;
                            int a1 = side == 0 ? m + 1 : side == 1 ? GRID : side == 2 ? m + 1 : 0, b1 = side == 0 ? 0 : side == 1 ? m + 1 : side == 2 ? GRID : m + 1;
                            s.line(patch.at(a0, b0), patch.at(a1, b1), lines * alpha, HINGE);
                        }
                    }
                }
            }
        }
        s.closedBody = fold > 0.99;
        s.orientation = q;
        if (round > 1e-3) s.bodyExponent = 30 + (exponent - 30) * round;
        s.bodyOffset = offset;
        s.contact = new V3(0, below.y(), 0);
        return s;
    }

    /**
     * Node positions (along, drop) at the tile borders of one axis for {@code reach} rings (index reach = -1, reach + 1 = 1):
     * every hinge turns by the same angle, ring k k times as far in total - flat at 0, a closed cube at 90 degrees.
     */
    static double[][] profile(double a, int reach) {
        double[][] n = new double[2 * reach + 2][];
        n[reach] = new double[]{-1, 0};
        n[reach + 1] = new double[]{1, 0};
        for (int k = 1; k <= reach; k++) {
            double c = Math.cos(k * a), sn = Math.sin(k * a);
            n[reach + 1 + k] = new double[]{n[reach + k][0] + 2 * c, n[reach + k][1] - 2 * sn};
            n[reach - k] = new double[]{n[reach - k + 1][0] - 2 * c, n[reach - k + 1][1] - 2 * sn};
        }
        return n;
    }

    /**
     * E31-3: which tiles the folded cube keeps. The top, its four neighbours and one bottom (two tiles on towards the nearer
     * edge, as V4) are the cube's six faces; the diagonal and other outer tiles have no place on it (folded they turned into
     * sticks and sheets beyond the cube - "the earth looked broken after the fold") and fade out while it folds.
     */
    static double tileAlpha(int i, int j, int[] bottom, double fold) {
        int ring = Math.max(Math.abs(i), Math.abs(j));
        if (ring == 0 || ring == 1 && (i == 0 || j == 0) || i == bottom[0] && j == bottom[1]) return 1;
        if (ring <= 2) return 1 - Meshes.smooth(0.4, 0.95, fold);
        return 1 - Meshes.smooth(0, 0.6, fold);
    }

    /** A point of the shell at node coordinates (0..5 along each axis, fractions inside a tile; centre tile 2..3). */
    static V3 point(double[][] nx, double[][] nz, double ix, double iz) {
        // ix/iz: node coordinates 0 .. 2 reach + 1
        double[] x = lerpNode(nx, ix), z = lerpNode(nz, iz);
        return new V3(x[0], 1 + x[1] + z[1], z[0]);
    }

    private static double[] lerpNode(double[][] n, double i) {
        int k = (int) Math.max(0, Math.min(n.length - 2, Math.floor(i)));
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
