package com.simplebuilding.modules.simplemobs;

/**
 * Rules of the Hollow Howler (Tiefenheuler) without world access, so the GameTests can check them directly.
 * Concept: docs/ai/KONZEPT-SCULK-MOB-2026-10-11.md (owner decisions 11.10.).
 */
public final class HowlerLogic {
    public enum Phase { ONE, HUNGER, RAGE }

    /** Owner 12.10. (Nachtrag 46): a little stronger than the Warden (500 health, 0 armor, 30 melee, speed 0.3, knockback resistance 1). */
    public static final double MAX_HEALTH = 550.0, ARMOR = 8.0, ATTACK = 20.0, SPEED = 0.3, KNOCKBACK_RESISTANCE = 1.0;
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
    /** After every special attack (shockwave, charge, shriek, tendrils) it pauses this long: room to move and to hit back. */
    public static final int PATTERN_GAP_TICKS = 30;
    /** Ground shockwave: 1 s wind-up (charge sound, ring of particles marks the radius), then every player on the ground within the radius. Jump or step out. */
    public static final int SHOCKWAVE_WINDUP_TICKS = 20, SHOCKWAVE_COOLDOWN_TICKS = 200;
    public static final double SHOCKWAVE_RADIUS = 5.0, SHOCKWAVE_TRIGGER_DISTANCE = 4.0;
    public static final float SHOCKWAVE_DAMAGE = 10.0f;
    /** Charge: crouches 0.8 s facing a locked direction, then runs straight; sidestep it. Hitting a wall staggers it 1.5 s. */
    public static final int CHARGE_WINDUP_TICKS = 16, CHARGE_RUN_TICKS = 20, CHARGE_COOLDOWN_TICKS = 180, STAGGER_TICKS = 30;
    public static final double CHARGE_SPEED = 0.8, CHARGE_MIN_DISTANCE = 6.0, CHARGE_MAX_DISTANCE = 16.0;
    public static final float CHARGE_DAMAGE = 12.0f;
    /** Echo shriek: 1 s wind-up (head thrown back), then hits who is within the radius and in its line of sight - cover blocks it. */
    public static final int SHRIEK_WINDUP_TICKS = 20;
    /** Sculk tendrils: marked spots pulse 1.5 s, then erupt (damage, Slowness II 3 s); step off the mark. */
    public static final int TENDRIL_WINDUP_TICKS = 30, TENDRIL_COOLDOWN_TICKS = 160, TENDRIL_SLOW_TICKS = 60, TENDRIL_COUNT = 3;
    public static final double TENDRIL_RADIUS = 1.2, TENDRIL_RANGE = 16.0;
    public static final float TENDRIL_DAMAGE = 4.0f;
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

    /** Shockwave, charge and tendril cooldowns: a third shorter in rage. */
    public static int cooldown(int base, Phase phase) { return phase == Phase.RAGE ? base * 2 / 3 : base; }

    public enum Attack { NONE, BITE, SHOCKWAVE, SHRIEK, CHARGE, TENDRILS }

    /**
     * Which attack to start. The bite has its own rhythm; every special attack waits for the pause after the last one.
     * Close: shockwave. 6-10 blocks with sight: shriek, then charge (up to 16). Otherwise tendrils under the target.
     */
    public static Attack choose(double dist, double reach, boolean sight, int gap, int bite, int shockwave, int shriek, int charge, int tendrils) {
        if (bite == 0 && dist <= reach) return Attack.BITE;
        if (gap > 0) return Attack.NONE;
        if (shockwave == 0 && dist <= SHOCKWAVE_TRIGGER_DISTANCE) return Attack.SHOCKWAVE;
        if (shriek == 0 && sight && dist > SHRIEK_MIN_DISTANCE && dist <= SHRIEK_RADIUS + 0.5) return Attack.SHRIEK;
        if (charge == 0 && sight && dist > CHARGE_MIN_DISTANCE && dist <= CHARGE_MAX_DISTANCE) return Attack.CHARGE;
        if (tendrils == 0 && dist <= TENDRIL_RANGE) return Attack.TENDRILS;
        return Attack.NONE;
    }

    /** The shockwave runs along the ground: hits within the radius and at most 1.5 blocks above/below, never in the air. */
    public static boolean shockwaveHits(double horizontalDist, double dy, boolean onGround) {
        return onGround && horizontalDist <= SHOCKWAVE_RADIUS && Math.abs(dy) <= 1.5;
    }

    /** Movement speed multiplier on the base speed: +10 % hungry, +20 % in rage (an angry Warden's pace). */
    public static double speedFactor(Phase phase) {
        return switch (phase) { case ONE -> 1.0; case HUNGER -> 1.1; case RAGE -> 1.2; };
    }


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
