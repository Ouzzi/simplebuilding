package com.simplebuilding.modules.simpleearth.view;

/**
 * V13-V20 (owner 26.10., E29-2/E29-5): a closed earth you ORBIT. The eye stays at the same distance from the earth's centre
 * (one edge-half above the face centre plus the real height); flying forward or sideways turns the earth under you, the point
 * straight below (the sub-satellite point) is always the surface under the player - never the edge. The faces use the
 * equiangular cube (as climate models do): the position on a face runs with the angle seen from the centre, so a steady
 * flight is a steady turn and the map still matches the world (half an edge from the centre lies the face edge).
 * The orientation is carried along like V4 (renaming at an edge, soft north lead) and follows almost at once.
 * <ul>
 * <li>{@link Shape#SUPER} (V13-V16, "like V5"): a rounded cube-ball |x|^e + |y|^e + |z|^e = 1 (e 2.4 = nearly a sphere,
 * the quadrilateralized spherical cube of COBE; e 8 = V5).</li>
 * <li>{@link Shape#CUBE} (V17-V20, "like V12, done right"): a sharp cube with edge lines; V18 turns a corner up with the
 * height (smoothly).</li>
 * <li>{@link Corner}: towards a corner the diagonal tile spreads over the corner as a fourth face lying on the body
 * (WINDOW: a framed window that grows to half the corner; FOUR: covers the whole corner, no frame - four faces meet).</li>
 * </ul>
 */
final class OrbitBodyView extends EarthView {
    enum Shape { SUPER, CUBE }

    enum Corner { NONE, WINDOW, FOUR }

    private final Shape shape;
    private final Corner corner;
    private final boolean tiltWithHeight;
    private Quat target = Quat.IDENTITY, shown = Quat.IDENTITY, tiltNow = Quat.IDENTITY;
    private String lastBottom = "";

    OrbitBodyView(Shape shape, Corner corner, boolean tiltWithHeight) {
        this.shape = shape;
        this.corner = corner;
        this.tiltWithHeight = tiltWithHeight;
    }

    /** Face coordinate (tan of the angle) of a linear position -1..1 on the tile: the equiangular cube. */
    static double angular(double s) {
        return Math.tan(Math.PI / 4 * s);
    }

    private double exponent(ViewParams p) {
        return Math.max(2, p.get("exponent", 8));
    }

    private double rounding(ViewParams p) {
        return Meshes.clamp(p.get("rounding", 0.03), 0.005, 0.3);
    }

    /** The body surface point in a direction (radial projection, a hair outside). */
    private V3 project(V3 dir, ViewParams p) {
        if (shape == Shape.SUPER) return Meshes.seProject(dir, exponent(p)).mul(1.004);
        return CubeJumpView.onBody(dir, rounding(p));
    }

    private void transport(V3 local, double dt, boolean full, ViewParams p) {
        V3 w = target.rotate(local).normalize();
        target = Quat.fromUnitVectors(w, V3.UP).mul(target);
        V3 north = target.rotate(new V3(0, 0, -1));
        north = new V3(north.x(), 0, north.z());
        if (north.dot(north) > 1e-8) {
            double angle = Math.atan2(north.x(), -north.z()), k = full ? 1 : 1 - Math.exp(-dt * p.get("northRate", 2.5));
            target = Quat.axisAngle(V3.UP, angle * k).mul(target);
        }
    }

    @Override
    protected void reset(double u, double v, ViewParams p) {
        target = Quat.IDENTITY;
        transport(new V3(angular(u), 1, angular(v)).normalize(), 0, true, p);
        shown = target;
        tiltNow = Quat.IDENTITY;
        lastBottom = "";
    }

    @Override
    protected Scene frame(double px, double pz, double dt, ViewParams p) {
        Scene s = new Scene();
        s.equiangular = true;
        int tx = tileOf(px), tz = tileOf(pz);
        boolean crossed = false, diagonal = false;
        while (tx != curTx || tz != curTz) {
            int dx = tx != curTx ? Integer.signum(tx - curTx) : 0, dz = dx == 0 ? Integer.signum(tz - curTz) : 0;
            if (crossed) diagonal = true;
            Quat m = RollingCubeView.edgeTurn(dx, dz);
            target = target.mul(m);
            shown = shown.mul(m);
            curTx += dx;
            curTz += dz;
            crossed = true;
        }
        if (crossed) s.event = diagonal ? "corner" : "edge";
        double u = u(px), v = v(pz), au = Math.abs(u), av = Math.abs(v);
        double cu = angular(u), cv = angular(v);
        transport(new V3(cu, 1, cv).normalize(), dt, false, p);
        double follow = p.get("follow", 30);
        shown = dt <= 0 ? target : shown.slerp(target, 1 - Math.exp(-dt * follow));
        Quat q = shown;
        double tilt = 0;
        if (tiltWithHeight) {
            tilt = Meshes.clamp(p.get("cornerTilt", 1), 0, 1) * Meshes.smooth(p.get("tiltStart", 0.35), 1, p.get("heightFraction", 0));
            V3 c = shown.rotate(new V3(Meshes.sg(u), 1, Meshes.sg(v))).normalize();
            Quat goal = tilt > 1e-4 ? Quat.IDENTITY.slerp(Quat.fromUnitVectors(c, V3.UP), tilt) : Quat.IDENTITY;
            tiltNow = tiltNow.slerp(goal, dt <= 0 ? 1 : 1 - Math.exp(-dt / Math.max(0.05, p.get("tipTime", 1.2) * 0.6)));
            q = tiltNow.mul(shown);
        }
        // face contents as V4: side faces blend old and new neighbour towards an edge; the bottom shows two tiles on
        double sideV = RollingCubeView.side(av, au), sideU = RollingCubeView.side(au, av);
        int su = Meshes.sg(u), sv = Meshes.sg(v);
        int[] bottom = au >= av ? new int[]{2 * su, 0, 0} : new int[]{0, 2 * sv, 1};
        int[][] a = {{0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, bottom};
        int[][] b = {{0, 0, 0}, {1, sv, 0}, {-1, sv, 0}, {su, 1, 0}, {su, -1, 0}, bottom};
        double[] w = {0, sideV, sideV, sideU, sideU, 0};
        for (int f = 0; f < 6; f++) {
            Patch face = (shape == Shape.SUPER ? Meshes.superFace(f, exponent(p), 40) : Meshes.cubeFace(f, rounding(p), 10))
                    .tile(a[f][0], a[f][1], a[f][2] != 0).mixWith(b[f][0], b[f][1], b[f][2] != 0, w[f]);
            face.transform(q, V3.ZERO);
            s.patches.add(face);
        }
        if (shape == Shape.CUBE) {
            double lines = p.get("edgeLines", 0.7);
            if (lines > 0.01) CubeShapeView.edges(s, q, V3.ZERO, rounding(p), lines);
        }
        if (corner != Corner.NONE) cornerFace(s, q, u, v, p);
        String bottomKey = bottom[0] + "," + bottom[1];
        if (!crossed && !lastBottom.isEmpty() && !bottomKey.equals(lastBottom)) s.fadeStart = true;
        if (crossed && (diagonal || Math.min(au, av) > 0.85)) s.fadeStart = true;
        lastBottom = bottomKey;
        s.orientation = q;
        s.bodyOffset = V3.ZERO;
        // the ground straight below the eye; the eye stays 1 + h from the centre (constant distance - an orbit)
        V3 ground = q.rotate(project(new V3(cu, 1, cv).normalize(), p).mul(1 / 1.004));
        s.contact = new V3(0, ground.length(), 0);
        s.eyeDrop = ground.length() - 1;
        return s;
    }

    /**
     * The fourth face at a corner (E29-5, after V3): the diagonal tile spreads from the corner over the three faces as a
     * curved triangle lying on the body; WINDOW grows to about half the corner with a frame, FOUR covers the corner fully.
     */
    private void cornerFace(Scene s, Quat q, double u, double v, ViewParams p) {
        int su = Meshes.sg(u), sv = Meshes.sg(v);
        double near = Math.min(Math.abs(u), Math.abs(v)), start = p.get("cornerStart", 0.4);
        double ws = Meshes.clamp((near - start) / Math.max(0.01, 1 - start), 0, 1);
        if (ws <= 0.01) return;
        double size = corner == Corner.FOUR ? 0.15 + 0.85 * ws : 0.08 + 0.5 * ws;
        V3 c = new V3(su, 1, sv);
        V3 pa = c.add(new V3(-su * size, 0, 0)), pb = c.add(new V3(0, -size, 0)), pc = c.add(new V3(0, 0, -sv * size));
        int n = 10;
        Patch w = new Patch(n + 1, n + 1).tile(su, sv, false);
        for (int j = 0; j <= n; j++) {
            for (int i = 0; i <= n; i++) {
                double fu = i / (double) n, fv = j / (double) n;
                V3 flat = V3.lerp(V3.lerp(pa, pc, fu), pb, fv);
                // the diagonal tile's corner quarter, stretched over the corner (the "distorted" fourth face)
                w.set(i, j, project(flat, p), su > 0 ? (1 - fv) * fu * 0.5 : 1 - (1 - fv) * fu * 0.5, sv > 0 ? fv * 0.5 : 1 - fv * 0.5);
            }
        }
        w.transform(q, V3.ZERO);
        w.overlay = true;
        w.alpha = corner == Corner.FOUR ? Math.min(1, ws * 3) : Math.min(1, ws * 2);
        s.patches.add(w);
        if (corner == Corner.WINDOW) {
            for (int k = 0; k < n; k++) {
                double f0 = k / (double) n, f1 = (k + 1) / (double) n;
                s.line(q.rotate(project(V3.lerp(pa, pc, f0), p)), q.rotate(project(V3.lerp(pa, pc, f1), p)), w.alpha, CubeJumpView.WINDOW_FRAME);
                s.line(q.rotate(project(V3.lerp(pc, pb, f0), p)), q.rotate(project(V3.lerp(pc, pb, f1), p)), w.alpha, CubeJumpView.WINDOW_FRAME);
                s.line(q.rotate(project(V3.lerp(pb, pa, f0), p)), q.rotate(project(V3.lerp(pb, pa, f1), p)), w.alpha, CubeJumpView.WINDOW_FRAME);
            }
        }
    }
}
