package com.simplebuilding.modules.simplemobs;

/**
 * Rules of the Hollow Howler (Tiefenheuler) without world access, so the GameTests can check them directly.
 * Concept: docs/ai/KONZEPT-SCULK-MOB-2026-10-11.md (owner decisions 11.10.).
 */
public final class HowlerLogic {
    public enum Phase { ONE, HUNGER, RAGE }

    public static final double MAX_HEALTH = 240.0, ARMOR = 6.0, ATTACK = 14.0, SPEED = 0.26, KNOCKBACK_RESISTANCE = 0.6;
    public static final int XP = 50;
    /** Manifestation: 5 s from the horn call to the Howler. */
    public static final int MANIFEST_TICKS = 100;
    /** Spawn: 2 s invulnerable and motionless while it emerges. */
    public static final int EMERGE_TICKS = 40;
    public static final int MAX_LIGHT = 7;
    /** The sensor may stand anywhere within this many blocks (each axis) of the body center. */
    public static final int SENSOR_RANGE = 5;
    /** The horn player must be this close to the body center. */
    public static final double PLAYER_RANGE = 16.0;
    public static final int HEARING_RADIUS = 24;
    /** Without a new vibration from its target it forgets the target after this long (blind). */
    public static final int FORGET_TICKS = 200;
    /** Bite: the jaws open this long before they snap; hits in that window are weak-spot hits. */
    public static final int BITE_WINDUP_TICKS = 12, BITE_COOLDOWN_TICKS = 30;
    public static final float WEAK_SPOT_MULTIPLIER = 1.5f;
    public static final int DARKNESS_TICKS = 80;
    public static final float SHRIEK_DAMAGE = 8.0f;
    public static final double SHRIEK_RADIUS = 10.0, SHRIEK_MIN_DISTANCE = 6.0;
    public static final int TRAP_INTERVAL_TICKS = 300, TRAP_LIFETIME_TICKS = 400, TRAP_COUNT = 3;
    public static final int TRAIL_INTERVAL_TICKS = 60, TRAIL_MAX = 25;
    public static final int AFTERMATH_RADIUS = 2;

    private HowlerLogic() {}

    public static Phase phase(float health, float max) {
        float f = max <= 0 ? 0 : health / max;
        if (f <= 0.2f) return Phase.RAGE;
        if (f <= 0.5f) return Phase.HUNGER;
        return Phase.ONE;
    }

    /** Echo shriek cooldown: every 12 s, every 6 s in rage. */
    public static int shriekCooldown(Phase phase) { return phase == Phase.RAGE ? 120 : 240; }

    /** Movement speed multiplier on the base speed: +15 % hungry, +25 % in rage. */
    public static double speedFactor(Phase phase) {
        return switch (phase) { case ONE -> 1.0; case HUNGER -> 1.15; case RAGE -> 1.25; };
    }

    public static double knockbackResistance(Phase phase) { return phase == Phase.RAGE ? 1.0 : KNOCKBACK_RESISTANCE; }

    /** Extra bite reach in blocks once hungry. */
    public static double extraReach(Phase phase) { return phase == Phase.ONE ? 0.0 : 1.0; }

    /** Damage taken: x1.5 while the jaws are opening for a bite (the weak spot). */
    public static float damageTaken(float damage, boolean jawsOpening) { return jawsOpening ? damage * WEAK_SPOT_MULTIPLIER : damage; }

    /** Light rule (owner 11.10.): block + sky light at the skull at most {@code max}. */
    public static boolean darkEnough(int light, int max) { return light <= max; }

    /** Whether a sensor at offset (dx, dy, dz) from the body center counts (cube of {@link #SENSOR_RANGE}). */
    public static boolean sensorInRange(int dx, int dy, int dz) {
        return Math.abs(dx) <= SENSOR_RANGE && Math.abs(dy) <= SENSOR_RANGE && Math.abs(dz) <= SENSOR_RANGE;
    }
}
