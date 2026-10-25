package com.simplebuilding.modules.simpleearth.view;

/**
 * V4 "Rollen + Slerp" and V5 "Glatte Normale" (concept §3.7, wa24-6-earth-v4/v5.html): the orientation is carried forward
 * (parallel transport, at an edge the faces are only renamed = orientation x 90 degrees) with a soft north lead and a slerped
 * display; side faces blend the old and new neighbour tile so they look the same before and after an edge; the bottom shows
 * the tile two further along the main flight axis. V5 uses a superellipsoid whose ground normal under the player always points up.
 */
final class RollingCubeView extends EarthView {
    private final boolean smooth;
    private Quat target = Quat.IDENTITY, shown = Quat.IDENTITY;
    private String lastBottom = "";

    RollingCubeView(boolean smooth) {
        this.smooth = smooth;
    }

    /** Renaming at an edge: maps new tile coordinates onto the old (the new top = the old side in flight direction). */
    static Quat edgeTurn(int dx, int dz) {
        if (dx > 0) return Quat.axisAngle(new V3(0, 0, 1), -Math.PI / 2);
        if (dx < 0) return Quat.axisAngle(new V3(0, 0, 1), Math.PI / 2);
        if (dz > 0) return Quat.axisAngle(new V3(1, 0, 0), Math.PI / 2);
        return Quat.axisAngle(new V3(1, 0, 0), -Math.PI / 2);
    }

    private V3 contactDir(double u, double v, ViewParams p) {
        return smooth ? Meshes.seNormal(Meshes.seProject(new V3(u, 1, v), exponent(p)), exponent(p)) : new V3(u, 1, v).normalize();
    }

    private static double exponent(ViewParams p) {
        return Math.max(2.5, p.get("exponent", 8));
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
        transport(contactDir(u, v, p), 0, true, p);
        shown = target;
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
            Quat m = edgeTurn(dx, dz);
            target = target.mul(m);
            shown = shown.mul(m);
            curTx += dx;
            curTz += dz;
            crossed = true;
        }
        double u = u(px), v = v(pz), au = Math.abs(u), av = Math.abs(v);
        if (crossed) s.event = diagonal ? "corner" : "edge";
        // face content: tile a, tile b, mix weight (side faces 50/50 towards the edge -> same picture before and after)
        double sideV = side(av, au), sideU = side(au, av);
        int su = Meshes.sg(u), sv = Meshes.sg(v);
        int[] bottom = au >= av ? new int[]{2 * su, 0, 0} : new int[]{0, 2 * sv, 1};
        int[][] a = {{0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, bottom};
        int[][] b = {{0, 0, 0}, {1, sv, 0}, {-1, sv, 0}, {su, 1, 0}, {su, -1, 0}, bottom};
        double[] w = {0, sideV, sideV, sideU, sideU, 0};
        transport(contactDir(u, v, p), dt, false, p);
        shown = shown.slerp(target, 1 - Math.exp(-dt * p.get("followRate", smooth ? 10 : 6)));
        double r = p.get("rounding", 0.07), e = exponent(p);
        V3 pc = shown.rotate(smooth ? Meshes.seProject(new V3(u, 1, v), e) : Meshes.round(new V3(u, 1, v), r));
        V3 offset = smooth ? new V3(-pc.x(), 1 - pc.y(), -pc.z()) : V3.ZERO;
        for (int f = 0; f < 6; f++) {
            Patch face = (smooth ? Meshes.superFace(f, e, 40) : Meshes.cubeFace(f, r, 14))
                    .tile(a[f][0], a[f][1], a[f][2] != 0).mixWith(b[f][0], b[f][1], b[f][2] != 0, w[f]);
            face.transform(shown, offset);
            s.patches.add(face);
        }
        String bottomKey = bottom[0] + "," + bottom[1];
        if (!crossed && !lastBottom.isEmpty() && !bottomKey.equals(lastBottom)) s.fadeStart = true;
        if (crossed && (diagonal || Math.min(au, av) > 0.85)) s.fadeStart = true;
        lastBottom = bottomKey;
        s.orientation = shown;
        s.bodyOffset = offset;
        s.contact = smooth ? V3.UP : pc;
        return s;
    }

    /** Side face mix: half towards the edge along the flight axis, fading out where the face itself is about to become top/bottom. */
    static double side(double along, double cross) {
        return 0.5 * Meshes.smooth(0.3, 1, along) * (1 - Meshes.smooth(0.85, 1, cross));
    }
}
