package com.simplebuilding.modules.simpleearth.view;

/** Immutable 3D vector of the earth view (body space: +X east, +Y up, +Z south, cube half edge 1). No game classes. */
public record V3(double x, double y, double z) {
    public static final V3 ZERO = new V3(0, 0, 0), UP = new V3(0, 1, 0);

    public V3 add(V3 o) { return new V3(x + o.x, y + o.y, z + o.z); }
    public V3 sub(V3 o) { return new V3(x - o.x, y - o.y, z - o.z); }
    public V3 mul(double s) { return new V3(x * s, y * s, z * s); }
    public V3 addScaled(V3 o, double s) { return new V3(x + o.x * s, y + o.y * s, z + o.z * s); }
    public double dot(V3 o) { return x * o.x + y * o.y + z * o.z; }
    public V3 cross(V3 o) { return new V3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
    public double length() { return Math.sqrt(x * x + y * y + z * z); }
    public double distance(V3 o) { return sub(o).length(); }

    public V3 normalize() {
        double l = length();
        return l < 1e-12 ? this : mul(1 / l);
    }

    public boolean finite() {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
    }

    public static V3 lerp(V3 a, V3 b, double t) {
        return new V3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t);
    }
}
