package com.simplebuilding.modules.simpleearth.view;

/**
 * Geometry of the earth body, ported 1:1 from the previews (earth-konzept.html, wa24-6-earth-v4..v6.html): the six faces of
 * the cube in the frame of the current tile, the 3-step rounding of edges and corners (like the UI frames' stair corners),
 * the superellipsoid of V5 and flat quads for the folding nets. Pure maths, no game classes.
 */
public final class Meshes {
    /** Face index order of the previews: top, east, west, south, north, bottom. */
    public static final int TOP = 0, EAST = 1, WEST = 2, SOUTH = 3, NORTH = 4, BOTTOM = 5;
    /** Tile offset each face shows in the fixed-tile versions V1-V3 (the bottom shows the tile two further east). */
    public static final int[][] FACE_TILES = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {2, 0}};

    private Meshes() {}

    /** Point on face {@code f} of the unit cube for face coordinates a, b in -1..1 (preview FACES[f].p). */
    public static V3 facePoint(int f, double a, double b) {
        return switch (f) {
            case TOP -> new V3(a, 1, b);
            case EAST -> new V3(1, -a, b);
            case WEST -> new V3(-1, a, b);
            case SOUTH -> new V3(a, -b, 1);
            case NORTH -> new V3(a, b, -1);
            default -> new V3(-a, -1, b);
        };
    }

    public static V3 faceNormal(int f) {
        return facePoint(f, 0, 0);
    }

    /** Grid stops along a face: three rounding steps (tan 0, 15, 30, 45 degrees), {@code inner} flat cells, three steps. */
    public static double[] stops(double r, int inner) {
        double[] t = {0, Math.tan(Math.PI / 12), Math.tan(Math.PI / 6), 1};
        double[] s = new double[inner + 7];
        int k = 0;
        for (int i = 3; i >= 1; i--) s[k++] = -(1 - r + r * t[i]);
        for (int i = 0; i <= inner; i++) s[k++] = -(1 - r) + 2 * (1 - r) * i / inner;
        for (int i = 1; i <= 3; i++) s[k++] = 1 - r + r * t[i];
        return s;
    }

    /** Rounds a cube point: everything outside the inner box (half edge 1 - r) is pulled onto a sphere of radius r around it. */
    public static V3 round(V3 p, double r) {
        double lim = 1 - r;
        V3 q = new V3(clamp(p.x(), -lim, lim), clamp(p.y(), -lim, lim), clamp(p.z(), -lim, lim));
        V3 d = p.sub(q);
        double l = d.length();
        return l < 1e-9 ? p : q.addScaled(d, r / l);
    }

    /** A rounded cube face as a grid patch (uv = tile 0..1, u along a, v along b). */
    public static Patch cubeFace(int f, double r, int inner) {
        double[] s = stops(r, inner);
        int n = s.length;
        Patch p = new Patch(n, n);
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) p.set(i, j, round(facePoint(f, s[i], s[j]), r), (s[i] + 1) / 2, (s[j] + 1) / 2);
        }
        return p;
    }

    /** V5: superellipsoid |x|^e + |y|^e + |z|^e = 1, cube point projected radially. */
    public static V3 seProject(V3 c, double e) {
        double s = Math.pow(Math.pow(Math.abs(c.x()), e) + Math.pow(Math.abs(c.y()), e) + Math.pow(Math.abs(c.z()), e), -1 / e);
        return c.mul(s);
    }

    public static V3 seNormal(V3 q, double e) {
        return new V3(Math.signum(q.x()) * Math.pow(Math.abs(q.x()), e - 1), Math.signum(q.y()) * Math.pow(Math.abs(q.y()), e - 1),
                Math.signum(q.z()) * Math.pow(Math.abs(q.z()), e - 1)).normalize();
    }

    /** V5 face: denser towards the edges (sin spacing) where the curvature sits. */
    public static Patch superFace(int f, double e, int cells) {
        Patch p = new Patch(cells + 1, cells + 1);
        for (int j = 0; j <= cells; j++) {
            for (int i = 0; i <= cells; i++) {
                double a = Math.sin((-1 + 2.0 * i / cells) * Math.PI / 2), b = Math.sin((-1 + 2.0 * j / cells) * Math.PI / 2);
                p.set(i, j, seProject(facePoint(f, a, b), e), (a + 1) / 2, (b + 1) / 2);
            }
        }
        return p;
    }

    /** A flat 2 x 2 quad in the y = 0 plane of a piece frame: x0..x1, z0..z1 (preview quadGeo, uv 0..1). */
    public static Patch quad(double x0, double x1, double z0, double z1) {
        Patch p = new Patch(2, 2);
        p.set(0, 0, new V3(x0, 0, z0), 0, 0);
        p.set(1, 0, new V3(x1, 0, z0), 1, 0);
        p.set(0, 1, new V3(x0, 0, z1), 0, 1);
        p.set(1, 1, new V3(x1, 0, z1), 1, 1);
        return p;
    }

    public static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    /** GLSL smoothstep. */
    public static double smooth(double a, double b, double x) {
        double t = clamp((x - a) / (b - a), 0, 1);
        return t * t * (3 - 2 * t);
    }

    /** Sign with 0 counted as +1 (preview sg). */
    public static int sg(double v) {
        return v < 0 ? -1 : 1;
    }
}
