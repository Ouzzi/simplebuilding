package com.simplebuilding.modules.simplemobs;

import net.minecraft.util.RandomSource;

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
    /** Client only: after a swing/stomp the raised pose eases back to rest over this many ticks instead of snapping. */
    public static final int RECOVER_TICKS = 6;

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
    /** N66: a player this long uninterrupted in a Watcher core biome may attract one (seconds), with this chance per roll. */
    public static final int FOREST_SECONDS = 120;
    public static final float FOREST_CHANCE = 0.25f;


    // ---- Watcher gaze zones (owner N63) -------------------------------------------------------
    /** Reference FOV of the zone angles; the client reports its FOV (clamped to 30..110), the angles scale with it. */
    public static final int FOV_REF = 70, FOV_MIN = 30, FOV_MAX = 110;
    /** Half angles at the reference FOV: stare (it stands still), slow zone, edge zone (beyond it: free to stalk). */
    public static final float GAZE_STARE_DEG = 10.0f, GAZE_SLOW_DEG = 25.0f, GAZE_EDGE_DEG = 35.0f;
    /** Gaze zones, from the view center outwards. */
    public static final int ZONE_STARE = 0, ZONE_SLOW = 1, ZONE_EDGE = 2, ZONE_FREE = 3;
    /** A direct look into its eyes (owner 15.10., N64): it does not freeze but turns aggressive. Ranks before every other zone. */
    public static final int ZONE_DIRECT = -1;
    /** Half angle (degrees, unscaled) around the eyes that counts as a direct look. */
    public static final float GAZE_DIRECT_DEG = 4.0f;
    /** Ticks it stays aggressive after the last direct look; its speed factor while aggressive. */
    public static final int AGGRO_TICKS = 100;
    public static final double AGGRO_SPEED = 1.35;
    /** Range in blocks: farther away it neither follows nor teleports; interval in seconds of its teleports to a target in range. */
    public static final float RANGE_DEFAULT = 48.0f, TELEPORT_SECONDS_DEFAULT = 6.0f;

    /** Whether the angle to its eyes is a direct look. */
    public static boolean directLook(double eyeAngleDeg, float direct) { return eyeAngleDeg <= direct; }

    public static int clampFov(int fov) { return Math.max(FOV_MIN, Math.min(FOV_MAX, fov)); }

    /** Zone of a point {@code angleDeg} off the view center for a player with this FOV; the three bounds are the config's. */
    public static int gazeZone(double angleDeg, int fov, float stare, float slow, float edge) {
        double s = clampFov(fov) / (double) FOV_REF;
        if (angleDeg <= stare * s) return ZONE_STARE;
        if (angleDeg <= slow * s) return ZONE_SLOW;
        if (angleDeg <= edge * s) return ZONE_EDGE;
        return ZONE_FREE;
    }

    /** Index of the victim among players at these distances: nearer ones are likelier (weight 1 / (1 + d / 16)); -1 if none. */
    public static int pickVictim(double[] dists, float roll) {
        double total = 0;
        for (double d : dists) total += 1.0 / (1.0 + d / 16.0);
        if (total <= 0) return -1;
        double at = roll * total;
        for (int i = 0; i < dists.length; i++) {
            at -= 1.0 / (1.0 + dists[i] / 16.0);
            if (at < 0) return i;
        }
        return dists.length - 1;
    }

    // ---- Focus ------------------------------------------------------------------------------
    public static final int FOCUS_TICKS = 3600, LONG_FOCUS_TICKS = 9600;

    private FearLogic() {}

    /** Slow in / slow out (smoothstep) of a 0..1 ramp, for animation: a soft start and a soft finish instead of a linear ramp. */
    public static float ease(float t) {
        float c = t < 0.0F ? 0.0F : t > 1.0F ? 1.0F : t;
        return c * c * (3.0F - 2.0F * c);
    }

    /** Hollow Watcher freeze poses: {head roll, head pitch, body roll, right arm pitch, left arm pitch, arm spread}. */
    public static final float[][] WATCHER_POSES = {
            {0.12F, 0.0F, 0.0F, 0.0F, 0.0F, 0.06F},
            {-0.35F, 0.15F, -0.08F, 0.0F, -0.45F, 0.1F},
            {0.4F, -0.1F, 0.1F, -0.5F, 0.0F, 0.08F},
            {0.0F, 0.35F, 0.05F, -0.3F, -0.3F, 0.2F},
            {-0.2F, -0.2F, -0.1F, 0.15F, -0.6F, 0.05F},
            {0.55F, 0.1F, 0.14F, -0.7F, 0.1F, 0.14F}};

    /** Next freeze pose: always another one than {@code prev}, deterministic for the entity id and the freeze count. */
    public static int nextWatcherPose(int prev, int entityId, int count) {
        int mix = entityId * 31 + count * 17;
        mix ^= mix >>> 7;
        return (prev + 1 + Math.floorMod(mix, WATCHER_POSES.length - 1)) % WATCHER_POSES.length;
    }

    /** Stop-motion gait: the walk phase snaps to steps of {@code step} instead of running smoothly. */
    public static float stopMotion(float pos, float step) { return (float) Math.floor(pos / step) * step; }

    /** Walk leg pitch with a trailing leg: while the leg swings back it hangs behind the beat (drag). */
    public static float legSwing(float phase, float speed) {
        return ((float) Math.cos(phase) * 0.8F - 0.18F * Math.max(0.0F, (float) Math.sin(phase))) * speed;
    }

    /** Client ramp of a pose value towards its goal: up in {@code up} ticks, down in {@code down} ticks, never overshooting. */
    public static float ramp(float cur, float goal, float up, float down) {
        return goal > cur ? Math.min(goal, cur + 1.0F / up) : Math.max(goal, cur - 1.0F / down);
    }

    /** Damped pendulum for the chain mace after an action ended: t = ticks since then, swings out and settles in about 12 ticks. */
    public static float chainPendulum(float t) {
        return t < 0.0F ? 0.0F : (float) (Math.exp(-0.25 * t) * Math.sin(0.8 * t));
    }

    /** Ticks until the Dread's next idle chain rattle: 8 to 20 seconds (concept: random 8-20 s). */
    public static int rattleWait(RandomSource random) { return 160 + random.nextInt(241); }

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
        return mayWatcherSpawn(enabled, end, distFromOrigin, core, endStone, room, near, roll, chance, false, 0.0f);
    }

    /**
     * As above, plus the forest rule (N66): when the player has been in a core biome for the configured time ({@code dwell})
     * and the spot is in a core biome as well, the chance is at least {@code forestChance}. It adds to the other rules, never lowers them.
     */
    public static boolean mayWatcherSpawn(boolean enabled, boolean end, double distFromOrigin, boolean core, boolean endStone, boolean room, int near,
                                          float roll, float chance, boolean dwell, float forestChance) {
        float c = core ? chance * CORE_CHANCE_FACTOR : chance;
        if (core && dwell) c = Math.max(c, forestChance);
        return enabled && end && (core || distFromOrigin > OUTER_ISLANDS) && endStone && room && near == 0 && roll < c;
    }
}
