package com.simplebuilding.modules.simpleearth.view;

/**
 * IG31-36 (owner 31.10.): keeps a rolling shell's turn steady when the current tile changes. The shells (V21-V27) turn by a
 * pure function of the spot on the current tile ({@code swing}); after renaming the tile the same world spot has other face
 * coordinates (u +1 becomes -1), so that function gave another turn - the earth jerked round at every border. No function of
 * the spot alone can be steady over both edge directions (a cube corner), so the view carries a correction {@link #fix}:
 * shown turn = fix * swing. At a rename the fix takes over the difference, measured by the shell's own tangent frame at the
 * player on the old and the new tile - the shell around the player stays where it was, only side-face contents change.
 */
final class TurnCarry {
    /** A point of the shell (before turning) at face coordinates u, v of the current tile; u/v beyond +-1 reach the neighbours. */
    interface Shell {
        V3 at(double u, double v);
    }

    private static final double H = 1e-3;
    Quat fix = Quat.IDENTITY;

    void reset() {
        fix = Quat.IDENTITY;
    }

    Quat turn(Quat swing) {
        return fix.mul(swing);
    }

    /**
     * The tile was renamed: the player at (u, v) on the new tile stood at (uOld, vOld) on the old one. Keeps
     * fix_new * swingNew * X_new = fix_old * swingOld * X_old around the player.
     */
    void rename(Shell before, double uOld, double vOld, Quat swingOld, Shell now, double u, double v, Quat swingNew) {
        Quat t = between(frame(now, u, v), frame(before, uOld, vOld));
        fix = fix.mul(swingOld).mul(t).mul(swingNew.conjugate()).normalize();
    }

    /** Lets the tilt part of the fix ebb away (the turn about the vertical stays: no turn caused by the border). */
    void settleTilt(double dt, double rate) {
        if (dt <= 0 || rate <= 0) return;
        Quat twist = twist(fix);
        Quat swing = fix.mul(twist.conjugate());
        fix = Quat.IDENTITY.slerp(swing, Math.exp(-dt * rate)).mul(twist).normalize();
    }

    /** The turn about +Y inside q (swing-twist split). */
    static Quat twist(Quat q) {
        double l = Math.hypot(q.w(), q.y());
        return l < 1e-12 ? Quat.IDENTITY : new Quat(q.w() / l, 0, q.y() / l, 0);
    }

    /** Tangent frame (along u, normal) of the shell at (u, v). */
    static V3[] frame(Shell s, double u, double v) {
        V3 eu = s.at(u + H, v).sub(s.at(u - H, v)), ev = s.at(u, v + H).sub(s.at(u, v - H));
        return new V3[]{eu.normalize(), ev.cross(eu).normalize()};
    }

    /** The rotation that turns frame a onto frame b. */
    static Quat between(V3[] a, V3[] b) {
        Quat first = Quat.fromUnitVectors(a[0], b[0]);
        Quat second = Quat.fromUnitVectors(first.rotate(a[1]).normalize(), b[1]);
        return second.mul(first).normalize();
    }
}
