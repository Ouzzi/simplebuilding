package com.simplebuilding.modules.simpleearth.view;

/**
 * V21-V24 (owner 26.10., E29-5 c: "like V9 - the unfolded sides solved best - but with rotation"): V9's shell of 5 x 5 joined
 * tiles (corner tiles as parallelograms, so the fourth tile at a corner stays solid) that additionally turns with the orbit:
 * the point under the player is turned towards the eye ({@code rotation} 1 = fully, like V5), so flying to an edge rolls the
 * shell instead of only folding it.
 * <ul>
 * <li>{@link Fold#POSITION} V21: the neighbours fold by the player's position as in V9, plus the rotation.</li>
 * <li>{@link Fold#FIXED} V22: a bowl folded by a fixed angle ({@code foldAngle}), rolling as a whole.</li>
 * <li>{@link Fold#HEIGHT} V23: flat near the render switch, closing to a cube towards the ceiling height.</li>
 * <li>{@link Fold#CUBE} V24: fully folded - the net closed into a cube with its hinges drawn, rolling.</li>
 * </ul>
 */
final class OrbitShellView extends EarthView {
    enum Fold { POSITION, FIXED, HEIGHT, CUBE }

    private final Fold fold;
    /** IG31-36: the turn carried over tile borders. */
    private final TurnCarry carry = new TurnCarry();

    OrbitShellView(Fold fold) {
        this.fold = fold;
    }

    @Override
    protected void reset(double u, double v, ViewParams p) {
        carry.reset();
    }

    /** Node positions (along, drop) at the tile borders -5..5 of one axis for fold angles {@code plus}/{@code minus} (FoldShellView.profile). */
    static double[][] profile(double plus, double minus) {
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
        s.closedBody = fold == Fold.CUBE;
        int tx = tileOf(px), tz = tileOf(pz);
        int dx = tx - curTx, dz = tz - curTz;
        if (tx != curTx || tz != curTz) {
            s.event = tx != curTx && tz != curTz ? "corner" : "edge";
            // a fixed fold re-centres the shell on the new tile: blend the change over
            if (fold != Fold.POSITION) s.fadeStart = true;
            curTx = tx;
            curTz = tz;
        }
        double u = u(px), v = v(pz);
        double[][] nx = profiles(u, p), nz = profiles(v, p);
        // the orbit turn: the point under the player towards the eye, the rest of the way as a sideways shift
        V3 under = new V3(u, 1, v);
        double rotation = Meshes.clamp(p.get("rotation", fold == Fold.POSITION ? 0.6 : 1), 0, 1);
        if (dx != 0 || dz != 0) {
            // IG31-36: the shell round the player stays put over the border (the turn is carried on, not recomputed)
            double uo = u + 2 * dx, vo = v + 2 * dz;
            double[][] ox = profiles(uo, p), oz = profiles(vo, p);
            carry.rename((a, b) -> shell(ox, oz, a, b), uo, vo, FoldWorldView.swing(uo, vo, rotation),
                    (a, b) -> shell(nx, nz, a, b), u, v, FoldWorldView.swing(u, v, rotation));
        }
        carry.settleTilt(dt, p.get("tiltSettle", 1));
        Quat q = carry.turn(FoldWorldView.swing(u, v, rotation));
        V3 below = q.rotate(under);
        V3 offset = new V3(-below.x(), 0, -below.z());
        for (int j = -2; j <= 2; j++) {
            for (int i = -2; i <= 2; i++) {
                Patch patch = new Patch(2, 2).tile(i, j, false);
                for (int b = 0; b <= 1; b++) {
                    for (int a = 0; a <= 1; a++) patch.set(a, b, q.rotate(point(nx[i + 2 + a], nz[j + 2 + b])).add(offset), a, b);
                }
                s.patches.add(patch);
            }
        }
        double lines = p.get("lineAlpha", fold == Fold.CUBE ? 0.5 : 0.2);
        if (lines > 0.01) {
            for (int k = 1; k <= 4; k++) {
                for (int j = 0; j < 5; j++) {
                    s.line(q.rotate(point(nx[k], nz[j])).add(offset), q.rotate(point(nx[k], nz[j + 1])).add(offset), lines, FoldShellView.HINGE);
                    s.line(q.rotate(point(nx[j], nz[k])).add(offset), q.rotate(point(nx[j + 1], nz[k])).add(offset), lines, FoldShellView.HINGE);
                }
            }
        }
        s.orientation = q;
        s.bodyOffset = offset;
        s.contact = new V3(0, below.y(), 0);
        return s;
    }

    /** Node profile of one axis for the player at face coordinate s (V21 folds by the position, the others by angle). */
    private double[][] profiles(double s, ViewParams p) {
        if (fold == Fold.POSITION) {
            double start = p.get("foldStart", 0.1);
            return profile(FoldingNetView.fold(s, start), FoldingNetView.fold(-s, start));
        }
        double angle = switch (fold) {
            case FIXED -> Math.toRadians(p.get("foldAngle", 45));
            case HEIGHT -> Math.toRadians(p.get("foldAngle", 15) + (90 - p.get("foldAngle", 15)) * Meshes.smooth(0, 1, p.get("heightFraction", 0)));
            default -> Math.PI / 2;
        };
        return profile(angle, angle);
    }

    /** The shell at face coordinates u, v (beyond +-1 on the neighbours). */
    private static V3 shell(double[][] nx, double[][] nz, double u, double v) {
        return FoldWorldView.point(nx, nz, 2 + (u + 1) / 2, 2 + (v + 1) / 2);
    }

    private static V3 point(double[] x, double[] z) {
        return new V3(x[0], 1 + x[1] + z[1], z[0]);
    }
}
