package com.simplebuilding.modules.simplemobs;

/**
 * Rules of the two End bosses (WP-19, owner 01.11.: Void Runner A1, Brimguard hat M with venting) as pure functions,
 * tested directly. Plan docs/ai/PLAN-B-BOSS01-2026-11-02.md, concept docs/ai/KONZEPT-A-WP19-2026-10-29.md.
 */
public final class BossLogic {
    private BossLogic() {}

    // ---- Void Runner (Leerenläufer) --------------------------------------------------------
    public static final double RUNNER_HEALTH = 900.0, RUNNER_ARMOR = 10.0, RUNNER_TOUGHNESS = 4.0, RUNNER_HIT = 14.0;
    /** Concept 0.62 would outrun a fast horse; 0.45 is about twice a zombie and 1.3 x the Deceiver (plan, decision). */
    public static final double RUNNER_SPEED = 0.45;
    public static final float RUNNER_WIDTH = 0.5f, RUNNER_HEIGHT = 1.9f;
    public static final int RUNNER_XP = 500;
    /** Ticks between two melee hits; circling radius; sprint-in when the circle has run this long. */
    public static final int RUNNER_HIT_COOLDOWN = 12;
    public static final double RUNNER_CIRCLE = 5.0;
    public static final int RUNNER_CIRCLE_TICKS = 50;
    /** Dodge step after a hit: sideways dash (blocks), cooldown in ticks (halved in rage). */
    public static final double DODGE_DISTANCE = 2.5;
    public static final int DODGE_COOLDOWN = 40;
    /** Chance to sidestep an arrow or trident. */
    public static final float PROJECTILE_DODGE = 0.6f;
    /** Lunge (phase 2+): crouch telegraph, max distance, damage, cooldown; a shield that blocks it staggers the runner. */
    public static final int LUNGE_WINDUP = 8, LUNGE_COOLDOWN = 120, STAGGER_TICKS = 30;
    public static final double LUNGE_RANGE = 6.0;
    public static final float LUNGE_DAMAGE = 18.0f;
    public static final float RUNNER_PHASE2 = 0.66f, RUNNER_PHASE3 = 0.33f;
    public static final double PHASE2_SPEED_BONUS = 0.15;
    public static final float RAGE_DAMAGE = 1.25f;

    /** Phase 1..3 by remaining health share. */
    public static int runnerPhase(float share) {
        return share < RUNNER_PHASE3 ? 3 : share < RUNNER_PHASE2 ? 2 : 1;
    }

    /** Dodge cooldown in the given phase: halved in rage (phase 3). */
    public static int dodgeCooldown(int phase) {
        return phase >= 3 ? DODGE_COOLDOWN / 2 : DODGE_COOLDOWN;
    }

    /** Melee damage in the given phase: x1.25 in rage. */
    public static float runnerDamage(float base, int phase) {
        return phase >= 3 ? base * RAGE_DAMAGE : base;
    }

    /** Whether the runner may start a lunge: phase 2+, off cooldown, on the ground, target between 3 and 6 blocks. */
    public static boolean lungeReady(int phase, int cooldown, boolean onGround, double distance) {
        return phase >= 2 && cooldown <= 0 && onGround && distance >= 3.0 && distance <= LUNGE_RANGE;
    }

    // ---- Brimguard (Schirmling) ------------------------------------------------------------
    public static final double BRIM_HEALTH = 700.0, BRIM_ARMOR = 14.0, BRIM_TOUGHNESS = 6.0, BRIM_HIT = 18.0, BRIM_SPEED = 0.20;
    public static final int BRIM_XP = 400;
    /** Hat preset M (owner 01.11.): brim edge height h and hat radius R (blocks), body radius r, standing height. */
    public static final double HAT_H = 0.8, HAT_R = 1.1, BODY_R = 0.3, STAND_HEIGHT = 1.5;
    public static final float BRIM_WIDTH = 0.6f, BRIM_HEIGHT = 1.5f, CROUCH_SCALE = 2.0f / 3.0f;
    /** Venting (Luef=ja): after every 2nd hat slam the brim rises by this much for VENT_TICKS; damage taken x1.5 then. */
    public static final double VENT_LIFT = 0.5;
    public static final int VENT_TICKS = 50, VENT_EVERY = 2;
    public static final float VENT_DAMAGE = 1.5f;
    /** Hat slam: telegraph, radius, damage, cooldown. Charge (phase 2+): telegraph, distance, damage, cooldown. */
    public static final int SLAM_WINDUP = 12, SLAM_COOLDOWN = 160, CHARGE_WINDUP = 10, CHARGE_COOLDOWN = 140, CHARGE_TICKS = 16;
    public static final double SLAM_RADIUS = 3.0, CHARGE_DISTANCE = 6.0;
    public static final float SLAM_DAMAGE = 16.0f, CHARGE_DAMAGE = 20.0f;
    /** Ducking: when shot from steeper than this or the target is farther than DUCK_FAR; ticks per phase. */
    public static final double DUCK_SHOT_DEG = 25.0, DUCK_FAR = 6.0;
    public static final int DUCK_TICKS = 80, DUCK_TICKS_P3 = 50, DUCK_COOLDOWN = 100;
    public static final float BRIM_PHASE2 = 0.65f, BRIM_PHASE3 = 0.30f;

    public static int brimPhase(float share) {
        return share < BRIM_PHASE3 ? 3 : share < BRIM_PHASE2 ? 2 : 1;
    }

    /** Phase 3 decay: the brim edge rises 0.1 for every full 10 % of health below 30 %. */
    public static double decayLift(float share) {
        if (share >= BRIM_PHASE3) return 0.0;
        return 0.1 * Math.floor((BRIM_PHASE3 - share) * 10.0 + 1.0e-6);
    }

    /** The brim edge height (blocks above the feet): preset h, scaled 2/3 while ducking, plus venting and decay. */
    public static double brimHeight(boolean crouching, double lift) {
        return HAT_H * (crouching ? CROUCH_SCALE : 1.0) + lift;
    }

    /** Body height: 1.5 standing, 1.0 ducking. */
    public static double bodyHeight(boolean crouching) {
        return STAND_HEIGHT * (crouching ? CROUCH_SCALE : 1.0);
    }

    /**
     * The hat rule (concept 8.2): a shot that reaches the body at height {@code y} (blocks above the feet) with a downward
     * slope of {@code thetaRad} (0 = level, positive = from above) is caught by the hat when {@code y > h - D tan(theta)}
     * with D = R - r, or when it lands on the hat itself ({@code y > h}). From below or level it is never caught below h.
     */
    public static boolean hatBlocks(double y, double thetaRad, boolean crouching, double lift) {
        double h = brimHeight(crouching, lift);
        if (y > h) return true;
        if (thetaRad <= 0.0) return false;
        double d = HAT_R - BODY_R;
        return y > h - d * Math.tan(Math.min(thetaRad, Math.toRadians(89.0)));
    }

    /** Highest body point a shot of this slope can still reach (negative = none). */
    public static double reachableHeight(double thetaRad, boolean crouching, double lift) {
        double h = brimHeight(crouching, lift);
        return thetaRad <= 0.0 ? h : h - (HAT_R - BODY_R) * Math.tan(thetaRad);
    }

    /** Should it duck: off cooldown and either shot at from steeper than 25 degrees or the target is far away. */
    public static boolean shouldDuck(int cooldown, double shotDeg, double targetDistance) {
        return cooldown <= 0 && (shotDeg > DUCK_SHOT_DEG || targetDistance > DUCK_FAR);
    }

    /** Venting starts after every 2nd hat slam. */
    public static boolean ventsAfter(int slams) {
        return slams > 0 && slams % VENT_EVERY == 0;
    }

    /** Speed bonus: +15 % in phase 3 (Brimguard) or from phase 2 on (Void Runner), as a multiplier. */
    public static double speedFactor(int phase, int fromPhase) {
        return phase >= fromPhase ? 1.0 + PHASE2_SPEED_BONUS : 1.0;
    }
}
