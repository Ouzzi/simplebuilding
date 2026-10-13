package com.simplebuilding.modules.simplemobs;

/**
 * Rules of the fear mobs (owner 12.10., docs/ai/PLAN-FURCHT-MOBS-2026-10-12.md): the Shivering aura shared by the Dread
 * and the Hollow Watcher, the Dread's light weakness and stomp, the Watcher's spawn rule. Pure functions, tested directly.
 */
public final class FearLogic {
    // ---- aura -------------------------------------------------------------------------------
    public static final double AURA_RADIUS = 16.0;
    /** Closer than this: Shivering II (no sight needed). */
    public static final double AURA_CLOSE = 6.0;
    public static final int AURA_INTERVAL = 20;
    /** Short, so Milk helps only until the next pulse. */
    public static final int AURA_TICKS = 60;
    /** Fading: a level above I steps down in the last 10 ticks and then lasts 30 ticks. */
    public static final int DECAY_WINDOW_TICKS = 10, DECAY_STEP_TICKS = 30;

    // ---- Dread ------------------------------------------------------------------------------
    public static final double DREAD_HEALTH = 260.0, DREAD_ARMOR = 14.0, DREAD_TOUGHNESS = 4.0, DREAD_SPEED = 0.2;
    public static final float DREAD_HIT = 18.0f, DREAD_STOMP = 10.0f;
    public static final int DREAD_XP = 60;
    /** Light (block and sky) at which the Dread is weakened: half damage, aura at most level I. */
    public static final int DREAD_BRIGHT = 12;
    public static final float SHIELD_DISABLE_SECONDS = 10.0f;
    public static final int SWING_WINDUP = 14, SWING_COOLDOWN = 30;
    public static final double SWING_REACH = 2.2;
    public static final int STOMP_WINDUP = 24, STOMP_COOLDOWN = 160;
    public static final double STOMP_RADIUS = 6.0;
    public static final int RISE_TICKS = 60;

    // ---- Hollow Watcher ---------------------------------------------------------------------
    public static final double WATCHER_HEALTH = 200.0, WATCHER_ARMOR = 8.0, WATCHER_SPEED = 0.32;
    public static final float WATCHER_CLAW = 10.0f, GRIP_DAMAGE = 6.0f, GRIP_RELEASE_DAMAGE = 4.0f;
    public static final int WATCHER_XP = 50;
    public static final int FREEZE_TICKS = 60, GRIP_TICKS = 40, GRIP_WINDUP = 10, GRIP_COOLDOWN = 200;
    public static final int CLAW_WINDUP = 6, CLAW_COOLDOWN = 30;
    public static final int TELEPORT_WINDUP = 12, TELEPORT_COOLDOWN = 120;
    /** After a hit the Watcher blinks away 1 s later, 12 to 20 blocks off (N50). */
    public static final int HIT_TELEPORT_TICKS = 20;
    public static final double HIT_TELEPORT_MIN = 12.0, HIT_TELEPORT_MAX = 20.0;
    public static final double TELEPORT_MIN_DIST = 8.0, TELEPORT_BEHIND = 2.5, WATCHER_REACH = 1.8;
    public static final int SCREAM_WINDUP = 20, SCREAM_COOLDOWN = 400, STARE_LIMIT = 100;
    public static final double SCREAM_RADIUS = 12.0;
    public static final int SCREAM_SHIVER_TICKS = 200, SCREAM_BLIND_TICKS = 60;
    /** Outer End islands: farther than this from the origin. */
    public static final double OUTER_ISLANDS = 1000.0;
    public static final float WATCHER_CHANCE = 0.04f;
    public static final int WATCHER_ROLL_INTERVAL = 600;

    // ---- Focus ------------------------------------------------------------------------------
    public static final int FOCUS_TICKS = 3600, LONG_FOCUS_TICKS = 9600;

    private FearLogic() {}

    /** Shivering levels of the aura (N50): level 5 right next to the mob, one level less every ~3 blocks out to 16. */
    public static final int AURA_MAX_LEVEL = 5;
    private static final double[] AURA_STEPS = {4.0, 7.0, 10.0, 13.0, 16.0};

    /**
     * Shivering level the aura gives (0 none, 1..5): the closer, the stronger (under 4 blocks V, under 7 IV, under 10
     * III, under 13 II, under 16 I). It is active only while the mob looks at the target (sight) or is triggered
     * (has a target); {@code capOne}: a weakened Dread gives at most I.
     */
    public static int auraLevel(double dist, boolean active, boolean capOne) {
        if (!active || dist >= AURA_RADIUS) return 0;
        int level = AURA_MAX_LEVEL;
        for (double step : AURA_STEPS) {
            if (dist < step) break;
            level--;
        }
        return capOne ? Math.min(1, level) : level;
    }

    public static boolean bright(int light) { return light >= DREAD_BRIGHT; }

    /** Damage of a Dread attack: halved in bright light. */
    public static float dreadDamage(float base, int light) { return bright(light) ? base * 0.5f : base; }

    /** The stomp hits those on the ground within the ring and about the Dread's height; a jump dodges it. */
    public static boolean stompHits(double horizontalDist, double dy, boolean onGround) {
        return onGround && horizontalDist <= STOMP_RADIUS && Math.abs(dy) < 2.0;
    }

    /** Spawn chance factor in a Watcher core biome (tag simpleend:watcher_core, N50). */
    public static final float CORE_CHANCE_FACTOR = 3.0f;

    /** Natural Hollow Watcher spawn: End, outer islands, on End stone with room, none nearby, the roll. */
    public static boolean mayWatcherSpawn(boolean enabled, boolean end, double distFromOrigin, boolean endStone, boolean room, int near, float roll, float chance) {
        return mayWatcherSpawn(enabled, end, distFromOrigin, false, endStone, room, near, roll, chance);
    }

    /** As above; in a core biome the distance rule is dropped and the chance is {@link #CORE_CHANCE_FACTOR} times higher. */
    public static boolean mayWatcherSpawn(boolean enabled, boolean end, double distFromOrigin, boolean core, boolean endStone, boolean room, int near, float roll, float chance) {
        return enabled && end && (core || distFromOrigin > OUTER_ISLANDS) && endStone && room && near == 0
                && roll < (core ? chance * CORE_CHANCE_FACTOR : chance);
    }
}
