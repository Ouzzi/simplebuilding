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

    // ---- horror: heartbeat, silence, rage pulse, camera shake (concept 2026-10-15, owner answers section 6) ----

    /** Heartbeat period in ticks per phase (the Warden's runs 40 to 10). */
    public static int heartbeatDelay(Phase phase) {
        return switch (phase) { case ONE -> 40; case HUNGER -> 26; case RAGE -> 14; };
    }

    /** The second beat of the lub-dub follows this many ticks after the first. */
    public static final int HEART_SECOND_BEAT_TICKS = 6;
    /** Before a phase change it holds its breath this long (no ambient, no heartbeat), then roars. */
    public static final int PHASE_HUSH_TICKS = 20;
    /** Death takes 2 s (Vanilla 1 s): knees, head drops, jaw falls, glow fades. */
    public static final int DEATH_TICKS = 40;
    /** Darkness pulse defaults (like the Warden's): every 6 s, 13 s of Darkness, within 20 blocks. */
    public static final int PULSE_INTERVAL_TICKS = 120, PULSE_DARKNESS_TICKS = 260, PULSE_RADIUS = 20;
    public static final double SHAKE_RANGE = 16.0;
    public static final float SHAKE_MAX_DEGREES = 0.8f;

    /** Tick of the first beat's cycle: the heartbeat is due when this is 0, the second beat when it equals {@link #HEART_SECOND_BEAT_TICKS}. */
    public static int heartbeatCycle(int tickCount, int id, Phase phase) { return Math.floorMod(tickCount + id, heartbeatDelay(phase)); }

    /** Silence rule: no ambient and no heartbeat while it emerges, attacks, staggers or holds its breath before a phase change. */
    public static boolean quiet(boolean emerging, Attack attack, int hush, int stagger) {
        return emerging || attack != Attack.NONE || hush > 0 || stagger > 0;
    }

    /** Darkness pulse period: the configured one snapped to a whole number of heartbeats, so the pulse lands on a beat. */
    public static int pulseInterval(int configured, Phase phase) {
        int d = heartbeatDelay(phase);
        return d * Math.max(1, Math.round(configured / (float) d));
    }

    /** The pulse only runs in rage, on the first beat of every pulse period. */
    public static boolean pulseDue(int tickCount, int id, Phase phase, int configured) {
        return phase == Phase.RAGE && Math.floorMod(tickCount + id, pulseInterval(configured, phase)) == 0;
    }

    /** Glow brightness over a heartbeat: 0.45 at rest (never off, so it does not flash), peaks at the two beats. */
    public static float heartGlow(float ticksSinceBeat) {
        float a = Math.max(0.0f, 1.0f - Math.abs(ticksSinceBeat) / 3.0f);
        float b = 0.75f * Math.max(0.0f, 1.0f - Math.abs(ticksSinceBeat - HEART_SECOND_BEAT_TICKS) / 3.0f);
        return Math.min(1.0f, 0.45f + 0.55f * Math.max(a, b));
    }

    /** Jaw opening of the bite {@code elapsed} ticks after it started: closes a little (anticipation), then opens with cosine easing to 1. */
    public static float biteJaw(float elapsed) {
        if (elapsed <= 0.0f) return 0.0f;
        if (elapsed < 3.0f) return -0.1f * (float) Math.sin(Math.PI * elapsed / 3.0);
        float x = Math.min(1.0f, (elapsed - 3.0f) / (BITE_WINDUP_TICKS - 3.0f));
        return 0.5f - 0.5f * (float) Math.cos(Math.PI * x);
    }

    /** Overshoot after the snap: the jaw swings past closed and settles over 4 ticks. */
    public static float biteRecoil(float ticksLeft) { return -0.15f * Math.max(0.0f, Math.min(1.0f, ticksLeft / 4.0f)); }

    /** Emerge height 0 (under ground) .. 1 (out) for linear progress 0..1: slow start, a hold with twitching, then out with a small overshoot. */
    public static float emergeHeight(float p) {
        if (p <= 0.0f) return 0.0f;
        if (p >= 1.0f) return 1.0f;
        if (p < 0.3f) return 0.25f * (p / 0.3f);
        if (p < 0.55f) return 0.25f;
        float x = (p - 0.55f) / 0.45f - 1.0f;
        return 0.25f + 0.75f * (1.0f + 1.8f * x * x * x + 0.8f * x * x);
    }

    /** Camera shake in degrees for a player {@code dist} blocks away: only while it winds up the shockwave or the shriek. */
    public static float shakeAmplitude(Attack attack, double dist) {
        if ((attack != Attack.SHOCKWAVE && attack != Attack.SHRIEK) || dist >= SHAKE_RANGE) return 0.0f;
        return SHAKE_MAX_DEGREES * (float) (1.0 - dist / SHAKE_RANGE);
    }
}
