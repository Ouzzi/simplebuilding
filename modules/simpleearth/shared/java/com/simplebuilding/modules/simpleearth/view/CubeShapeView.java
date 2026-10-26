package com.simplebuilding.modules.simpleearth.view;

/**
 * V10-V12 (owner 25.10., WD25-17: "die Würfelform stärker betonen"): a sharp cube whose top face always lies level under
 * the player - nothing tilts while you glide over a face. At an edge the cube tips over it like a rolled die: the faces are
 * renamed (as in V4, {@link RollingCubeView#edgeTurn}) and the display turns back level within {@code tipTime} seconds about
 * the point under the player, so the edge you cross is the pivot. Side faces blend the old and new neighbour like V4, the
 * bottom shows the tile two further towards the nearer edge.
 * <ul>
 * <li>{@link Style#TIP} V10 "Kippwürfel": almost sharp edges ({@code rounding}), all twelve edges as bright lines.</li>
 * <li>{@link Style#CELLS} V11 "Zauberwürfel": every face split into {@code cells} x {@code cells} pieces with fugues
 * ({@code seam}) over a dark core - a cube built from cubes.</li>
 * <li>{@link Style#CORNER} V12 "Eckansicht": the higher you fly ({@code tiltStart} .. ceiling), the more the corner of your
 * quarter turns up to you ({@code cornerTilt} 1 = fully), until three faces show like the block icon in the inventory.</li>
 * </ul>
 */
final class CubeShapeView extends EarthView {
    enum Style { TIP, CELLS, CORNER }

    static final int EDGE = 0xFFEAF2FF, CORE = 0xFF15131F;
    private final Style style;
    private Quat shown = Quat.IDENTITY, from, tiltNow = Quat.IDENTITY;
    private double animT;
    private String lastBottom = "";

    CubeShapeView(Style style) {
        this.style = style;
    }

    @Override
    protected void reset(double u, double v, ViewParams p) {
        shown = Quat.IDENTITY;
        from = null;
        tiltNow = Quat.IDENTITY;
        lastBottom = "";
    }

    @Override
    protected Scene frame(double px, double pz, double dt, ViewParams p) {
        Scene s = new Scene();
        int tx = tileOf(px), tz = tileOf(pz);
        boolean crossed = false, diagonal = false;
        while (tx != curTx || tz != curTz) {
            int dx = tx != curTx ? Integer.signum(tx - curTx) : 0, dz = dx == 0 ? Integer.signum(tz - curTz) : 0;
            if (crossed) diagonal = true;
            shown = shown.mul(RollingCubeView.edgeTurn(dx, dz));
            curTx += dx;
            curTz += dz;
            crossed = true;
        }
        if (crossed) {
            // tip over the edge: start from the renamed display (unchanged picture) and turn back level
            from = shown;
            animT = 0;
            s.event = diagonal ? "corner" : "edge";
        }
        if (from != null) {
            animT += dt / Math.max(0.05, p.get("tipTime", 0.6));
            double e = Math.min(1, animT);
            shown = from.slerp(Quat.IDENTITY, e * e * (3 - 2 * e));
            if (e >= 1) {
                shown = Quat.IDENTITY;
                from = null;
            }
        }
        double u = u(px), v = v(pz), au = Math.abs(u), av = Math.abs(v);
        double sideV = RollingCubeView.side(av, au), sideU = RollingCubeView.side(au, av);
        int su = Meshes.sg(u), sv = Meshes.sg(v);
        int[] bottom = au >= av ? new int[]{2 * su, 0, 0} : new int[]{0, 2 * sv, 1};
        int[][] a = {{0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, bottom};
        int[][] b = {{0, 0, 0}, {1, sv, 0}, {-1, sv, 0}, {su, 1, 0}, {su, -1, 0}, bottom};
        double[] w = {0, sideV, sideV, sideU, sideU, 0};

        // V12: turn the corner of the player's quarter towards the eye with the height
        Quat q = shown;
        double tilt = 0;
        if (style == Style.CORNER) {
            tilt = Meshes.clamp(p.get("cornerTilt", 1), 0, 1) * Meshes.smooth(p.get("tiltStart", 0.35), 1, p.get("heightFraction", 0));
            V3 corner = shown.rotate(new V3(su, 1, sv)).normalize();
            Quat target = tilt > 1e-4 ? Quat.IDENTITY.slerp(Quat.fromUnitVectors(corner, V3.UP), tilt) : Quat.IDENTITY;
            // E29-3: the corner turn follows smoothly (it used to jump when the player crossed the middle or an edge of a tile)
            double tau = Math.max(0.05, p.get("tipTime", 1.0)) * 0.6;
            tiltNow = tiltNow.slerp(target, dt <= 0 ? 1 : 1 - Math.exp(-dt / tau));
            q = tiltNow.mul(shown);
        }
        double r = style == Style.TIP ? Meshes.clamp(p.get("rounding", 0.02), 0.005, 0.3) : 0.005;
        V3 contactBody = Meshes.round(new V3(u, 1, v), r);
        // the point under the player stays put (the die pivots about it); V12 slides its top corner under the eye instead
        V3 offset = V3.UP.sub(q.rotate(contactBody));
        if (tilt > 1e-4) {
            V3 centred = new V3(0, 1 - top(q), 0);
            offset = V3.lerp(offset, centred, tilt);
        }
        if (style == Style.CELLS) cells(s, q, offset, a, b, w, (int) Math.round(Meshes.clamp(p.get("cells", 3), 2, 8)), Meshes.clamp(p.get("seam", 0.05), 0, 0.2));
        else {
            for (int f = 0; f < 6; f++) {
                Patch face = Meshes.cubeFace(f, r, 6).tile(a[f][0], a[f][1], a[f][2] != 0).mixWith(b[f][0], b[f][1], b[f][2] != 0, w[f]);
                face.transform(q, offset);
                s.patches.add(face);
            }
            double lines = p.get("edgeLines", 0.8);
            if (lines > 0.01) edges(s, q, offset, r, lines);
        }
        String bottomKey = bottom[0] + "," + bottom[1];
        if (!crossed && !lastBottom.isEmpty() && !bottomKey.equals(lastBottom)) s.fadeStart = true;
        if (crossed && (diagonal || Math.min(au, av) > 0.85)) s.fadeStart = true;
        lastBottom = bottomKey;
        s.orientation = q;
        s.bodyOffset = offset;
        s.contact = V3.UP;
        return s;
    }

    /** Highest point of the turned unit cube (its top corner height). */
    static double top(Quat q) {
        double best = -9;
        for (int x = -1; x <= 1; x += 2) for (int y = -1; y <= 1; y += 2) for (int z = -1; z <= 1; z += 2) best = Math.max(best, q.rotate(new V3(x, y, z)).y());
        return best;
    }

    /** The twelve cube edges along the middle of their rounding, a hair outside the faces so they are not hidden by them. */
    static void edges(Scene s, Quat q, V3 offset, double r, double alpha) {
        double in = 1 - r, out = (in + r / Math.sqrt(2)) * 1.004;
        for (int axis = 0; axis < 3; axis++) {
            for (int s1 = -1; s1 <= 1; s1 += 2) {
                for (int s2 = -1; s2 <= 1; s2 += 2) {
                    double[] a = new double[3], b = new double[3];
                    a[axis] = -in;
                    b[axis] = in;
                    a[(axis + 1) % 3] = b[(axis + 1) % 3] = s1 * out;
                    a[(axis + 2) % 3] = b[(axis + 2) % 3] = s2 * out;
                    s.line(q.rotate(new V3(a[0], a[1], a[2])).add(offset), q.rotate(new V3(b[0], b[1], b[2])).add(offset), alpha, EDGE);
                }
            }
        }
    }

    /** V11: n x n flat pieces per face with gaps, a dark core cube shows through the fugues. */
    private static void cells(Scene s, Quat q, V3 offset, int[][] a, int[][] b, double[] w, int n, double seam) {
        for (int f = 0; f < 6; f++) {
            Patch core = Meshes.cubeFace(f, 0.005, 1).scale(0.985);
            core.kind = Patch.COLOR;
            core.color = CORE;
            core.overlay = true;
            core.transform(q, offset);
            s.patches.add(core);
            for (int cj = 0; cj < n; cj++) {
                for (int ci = 0; ci < n; ci++) {
                    double a0 = -1 + 2.0 * ci / n + seam / 2, a1 = -1 + 2.0 * (ci + 1) / n - seam / 2;
                    double b0 = -1 + 2.0 * cj / n + seam / 2, b1 = -1 + 2.0 * (cj + 1) / n - seam / 2;
                    Patch cell = new Patch(2, 2).tile(a[f][0], a[f][1], a[f][2] != 0).mixWith(b[f][0], b[f][1], b[f][2] != 0, w[f]);
                    cell.set(0, 0, Meshes.facePoint(f, a0, b0), (a0 + 1) / 2, (b0 + 1) / 2);
                    cell.set(1, 0, Meshes.facePoint(f, a1, b0), (a1 + 1) / 2, (b0 + 1) / 2);
                    cell.set(0, 1, Meshes.facePoint(f, a0, b1), (a0 + 1) / 2, (b1 + 1) / 2);
                    cell.set(1, 1, Meshes.facePoint(f, a1, b1), (a1 + 1) / 2, (b1 + 1) / 2);
                    cell.transform(q, offset);
                    s.patches.add(cell);
                }
            }
        }
    }
}
