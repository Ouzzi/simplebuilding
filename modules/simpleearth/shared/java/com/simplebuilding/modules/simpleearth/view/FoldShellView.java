package com.simplebuilding.modules.simpleearth.view;

/**
 * V9 "Faltschale" (new, 24.10.): V6's folding taken one step further - the fold profile along x (east/west neighbours) and
 * along z (north/south) are separate curves, and every one of the 5 x 5 tiles round the player is the product of both
 * (x and the x-drop from the first, z and the z-drop from the second). So the tiles stay glued together everywhere: the
 * corner tiles are parallelograms that open like a window as both sides flatten, the second ring closes the shell into a
 * cube below, and no part ever needs a cross-fade. The tile under the player stays level (as V6).
 */
final class FoldShellView extends EarthView {
    static final int HINGE = 0xFFB89CFF;

    @Override
    protected void reset(double u, double v, ViewParams p) {
    }

    /**
     * Node positions (along, drop) at the tile borders -5, -3, -1, 1, 3, 5 (cube units, tile width 2) of one axis for the
     * player at face coordinate {@code s}: the centre tile flat, the first ring folded by {@link FoldingNetView#fold}, the
     * second ring a further 90 degrees.
     */
    static double[][] profile(double s, double start) {
        double plus = FoldingNetView.fold(s, start), minus = FoldingNetView.fold(-s, start);
        double[][] n = new double[6][];
        n[2] = new double[]{-1, 0};
        n[3] = new double[]{1, 0};
        n[4] = new double[]{n[3][0] + 2 * Math.cos(plus), n[3][1] - 2 * Math.sin(plus)};
        n[5] = new double[]{n[4][0] + 2 * Math.cos(plus + Math.PI / 2), n[4][1] - 2 * Math.sin(plus + Math.PI / 2)};
        n[1] = new double[]{n[2][0] - 2 * Math.cos(minus), n[2][1] - 2 * Math.sin(minus)};
        n[0] = new double[]{n[1][0] - 2 * Math.cos(minus + Math.PI / 2), n[1][1] - 2 * Math.sin(minus + Math.PI / 2)};
        return n;
    }

    @Override
    protected Scene frame(double px, double pz, double dt, ViewParams p) {
        Scene s = new Scene();
        s.closedBody = false;
        int tx = tileOf(px), tz = tileOf(pz);
        if (tx != curTx || tz != curTz) {
            s.event = tx != curTx && tz != curTz ? "corner" : "edge";
            curTx = tx;
            curTz = tz;
        }
        double u = u(px), v = v(pz), start = p.get("foldStart", 0.1), lines = p.get("lineAlpha", 0.35);
        double[][] nx = profile(u, start), nz = profile(v, start);
        for (int j = -2; j <= 2; j++) {
            for (int i = -2; i <= 2; i++) {
                Patch q = new Patch(2, 2).tile(i, j, false);
                for (int b = 0; b <= 1; b++) {
                    for (int a = 0; a <= 1; a++) q.set(a, b, point(nx[i + 2 + a], nz[j + 2 + b], u, v), a, b);
                }
                s.patches.add(q);
            }
        }
        if (lines > 0.01) {
            for (int k = 1; k <= 4; k++) {
                for (int j = 0; j < 5; j++) {
                    s.line(point(nx[k], nz[j], u, v), point(nx[k], nz[j + 1], u, v), lines, HINGE);
                    s.line(point(nx[j], nz[k], u, v), point(nx[j + 1], nz[k], u, v), lines, HINGE);
                }
            }
        }
        s.contact = V3.UP;
        return s;
    }

    private static V3 point(double[] x, double[] z, double u, double v) {
        return new V3(x[0] - u, 1 + x[1] + z[1], z[0] - v);
    }
}
