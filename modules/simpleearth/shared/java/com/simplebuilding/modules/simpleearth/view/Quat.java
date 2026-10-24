package com.simplebuilding.modules.simpleearth.view;

/**
 * Unit quaternion (w, x, y, z), the same conventions as three.js in the previews: {@code a.mul(b)} applies b first,
 * {@link #rotate} = q v q^-1, {@link #fromUnitVectors} = shortest rotation, {@link #slerp} towards a target.
 */
public record Quat(double w, double x, double y, double z) {
    public static final Quat IDENTITY = new Quat(1, 0, 0, 0);

    public static Quat axisAngle(V3 axis, double angle) {
        V3 a = axis.normalize();
        double s = Math.sin(angle / 2);
        return new Quat(Math.cos(angle / 2), a.x() * s, a.y() * s, a.z() * s);
    }

    /** Shortest rotation that turns unit vector {@code from} onto unit vector {@code to} (three.js setFromUnitVectors). */
    public static Quat fromUnitVectors(V3 from, V3 to) {
        double r = from.dot(to) + 1;
        double qx, qy, qz, qw;
        if (r < 1e-9) {
            r = 0;
            if (Math.abs(from.x()) > Math.abs(from.z())) {
                qx = -from.y(); qy = from.x(); qz = 0; qw = r;
            } else {
                qx = 0; qy = -from.z(); qz = from.y(); qw = r;
            }
        } else {
            V3 c = from.cross(to);
            qx = c.x(); qy = c.y(); qz = c.z(); qw = r;
        }
        return new Quat(qw, qx, qy, qz).normalize();
    }

    public Quat mul(Quat b) {
        return new Quat(w * b.w - x * b.x - y * b.y - z * b.z,
                w * b.x + x * b.w + y * b.z - z * b.y,
                w * b.y - x * b.z + y * b.w + z * b.x,
                w * b.z + x * b.y - y * b.x + z * b.w);
    }

    public Quat conjugate() {
        return new Quat(w, -x, -y, -z);
    }

    public Quat normalize() {
        double l = Math.sqrt(w * w + x * x + y * y + z * z);
        return l < 1e-12 ? IDENTITY : new Quat(w / l, x / l, y / l, z / l);
    }

    public double dot(Quat o) {
        return w * o.w + x * o.x + y * o.y + z * o.z;
    }

    public V3 rotate(V3 v) {
        // t = 2 * cross(q.xyz, v); v' = v + w * t + cross(q.xyz, t)
        double tx = 2 * (y * v.z() - z * v.y()), ty = 2 * (z * v.x() - x * v.z()), tz = 2 * (x * v.y() - y * v.x());
        return new V3(v.x() + w * tx + (y * tz - z * ty), v.y() + w * ty + (z * tx - x * tz), v.z() + w * tz + (x * ty - y * tx));
    }

    /** Angle between two orientations in degrees (0 = same). */
    public double angleTo(Quat o) {
        return Math.toDegrees(2 * Math.acos(Math.min(1, Math.abs(dot(o)))));
    }

    /** three.js Quaternion.slerp: from this towards {@code b} by {@code t} (0 = this, 1 = b). */
    public Quat slerp(Quat b, double t) {
        if (t <= 0) return this;
        if (t >= 1) return b;
        double cos = dot(b);
        Quat end = b;
        if (cos < 0) {
            cos = -cos;
            end = new Quat(-b.w, -b.x, -b.y, -b.z);
        }
        if (cos >= 1 - 1e-9) return this;
        double sinSq = 1 - cos * cos;
        if (sinSq <= 1e-12) {
            double s = 1 - t;
            return new Quat(s * w + t * end.w, s * x + t * end.x, s * y + t * end.y, s * z + t * end.z).normalize();
        }
        double sin = Math.sqrt(sinSq), half = Math.atan2(sin, cos);
        double ra = Math.sin((1 - t) * half) / sin, rb = Math.sin(t * half) / sin;
        return new Quat(w * ra + end.w * rb, x * ra + end.x * rb, y * ra + end.y * rb, z * ra + end.z * rb);
    }

    public boolean finite() {
        return Double.isFinite(w) && Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
    }
}
