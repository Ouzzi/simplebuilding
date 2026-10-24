package com.simplebuilding.modules.simpleearth.view;

/**
 * One drawn piece of the earth: a regular grid of {@code cols x rows} vertices (positions already in view space, uv in
 * the tile 0..1, u east, v south) showing the terrain tile at offset {@code (tx, tz)} from the current tile. A second tile
 * {@code (mixTx, mixTz)} can be blended in with weight {@code mix} (V4/V5 side faces). {@code mirror} turns the tile by 180
 * degrees (the cube's bottom). {@code kind}: terrain, a plain {@code color} (cell walls) or the corner window.
 */
public final class Patch {
    public static final int TERRAIN = 0, COLOR = 1;

    public final int cols, rows;
    public final V3[] pos;
    public final double[] u, v;
    public int tx, tz;
    public boolean mirror;
    public int mixTx, mixTz;
    public boolean mixMirror;
    public double mix;
    public double alpha = 1;
    public int kind = TERRAIN;
    public int color = 0xFFFFFFFF;
    /** V2: the texture is the cone-folded image around the current tile's corners. */
    public boolean folded;
    /** A window or overlay drawn over other pieces (not used to locate world points). */
    public boolean overlay;

    public Patch(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        this.pos = new V3[cols * rows];
        this.u = new double[cols * rows];
        this.v = new double[cols * rows];
    }

    public Patch tile(int tx, int tz, boolean mirror) {
        this.tx = tx;
        this.tz = tz;
        this.mirror = mirror;
        this.mixTx = tx;
        this.mixTz = tz;
        this.mixMirror = mirror;
        return this;
    }

    public Patch mixWith(int tx, int tz, boolean mirror, double weight) {
        this.mixTx = tx;
        this.mixTz = tz;
        this.mixMirror = mirror;
        this.mix = weight;
        return this;
    }

    public V3 at(int i, int j) {
        return pos[j * cols + i];
    }

    public void set(int i, int j, V3 p, double uu, double vv) {
        int k = j * cols + i;
        pos[k] = p;
        u[k] = uu;
        v[k] = vv;
    }

    /** Applies a rotation and then a translation to every vertex. */
    public Patch transform(Quat q, V3 t) {
        for (int k = 0; k < pos.length; k++) pos[k] = q.rotate(pos[k]).add(t);
        return this;
    }

    /** Uniform scale about the origin. */
    public Patch scale(double s) {
        for (int k = 0; k < pos.length; k++) pos[k] = pos[k].mul(s);
        return this;
    }

    /**
     * Where the tile point {@code (tu, tv)} (0..1 of this patch's main tile, before mirroring) sits in view space, or null when
     * this patch does not cover it. The uv grid is separable (u depends on the column, v on the row), so a search along the
     * first row/column finds the cell and the four corners interpolate.
     */
    public V3 locate(double tu, double tv) {
        double su = mirror ? 1 - tu : tu, sv = mirror ? 1 - tv : tv;
        int i = cell(su, true), j = cell(sv, false);
        if (i < 0 || j < 0) return null;
        double u0 = u[j * cols + i], u1 = u[j * cols + i + 1], v0 = v[j * cols + i], v1 = v[(j + 1) * cols + i];
        double fu = Math.abs(u1 - u0) < 1e-12 ? 0 : (su - u0) / (u1 - u0), fv = Math.abs(v1 - v0) < 1e-12 ? 0 : (sv - v0) / (v1 - v0);
        V3 a = V3.lerp(at(i, j), at(i + 1, j), fu), b = V3.lerp(at(i, j + 1), at(i + 1, j + 1), fu);
        return V3.lerp(a, b, fv);
    }

    private int cell(double s, boolean alongU) {
        int n = alongU ? cols : rows;
        for (int k = 0; k < n - 1; k++) {
            double a = alongU ? u[k] : v[k * cols], b = alongU ? u[k + 1] : v[(k + 1) * cols];
            double lo = Math.min(a, b), hi = Math.max(a, b);
            if (s >= lo - 1e-9 && s <= hi + 1e-9 && hi > lo) return k;
        }
        return -1;
    }
}
