package com.simplebuilding.modules.simpleenddragonfight;

/**
 * Pure numbers of the fight (no Minecraft types), owner decisions 2026-10-11 in
 * docs/ai/KONZEPT-END-DRAGON-FIGHT-2026-10-11.md: every counted player adds one full health bar.
 */
public final class DragonScaling {
    /** Base health per player with "raise base health" on (Vanilla dragon: 200). */
    public static final double RAISED_BASE = 300.0;
    /** Upper bound of the Vanilla max_health attribute; above it the rest is carried by a damage factor. */
    public static final double ATTRIBUTE_CAP = 1024.0;

    private DragonScaling() {}

    /** Counted players, at least 1 (solo = one bar), optionally capped ({@code cap <= 0}: no cap). */
    public static int players(int counted, int cap) {
        int n = Math.max(1, counted);
        return cap > 0 ? Math.min(n, cap) : n;
    }

    /** Effective dragon health: base x players, base 300 with the switch on, else the attribute base (Vanilla 200). */
    public static double targetHealth(boolean raiseBase, double attributeBase, int players) {
        return (raiseBase ? RAISED_BASE : attributeBase) * Math.max(1, players);
    }

    /** The max_health the dragon really gets (the attribute stops at 1024). */
    public static double appliedMax(double target) {
        return Math.min(target, ATTRIBUTE_CAP);
    }

    /** Damage multiplier so that the applied health lasts as long as the target health (1 up to 1024). */
    public static float damageFactor(double target) {
        return target <= ATTRIBUTE_CAP ? 1.0f : (float) (ATTRIBUTE_CAP / target);
    }

    /** Keeps the health share when the maximum changes; never below 1, so shrinking never kills. */
    public static float rescale(float health, double oldMax, double newMax) {
        if (oldMax <= 0) return (float) newMax;
        return (float) Math.max(1.0, Math.min(newMax, health / oldMax * newMax));
    }

    /** Dragon flight speed: base multiplier plus a small step per further player, capped. */
    public static double speed(double base, double perPlayer, double cap, int players) {
        return Math.max(0.1, Math.min(cap, base + perPlayer * (Math.max(1, players) - 1)));
    }

    /** Dragonmites per wave: base + extra per two further players (1/2 -> 2, 4 -> 3, 8 -> 5 with 2/1). */
    public static int mitesPerWave(int base, int extraPerTwo, int players) {
        return Math.max(0, base + extraPerTwo * ((Math.max(1, players) - 1) / 2));
    }

    /** Living Dragonmites at most: 8 + players, never above the cap (1 -> 9, 4 -> 12, 8 -> 16). */
    public static int maxAlive(int cap, int players) {
        return Math.max(0, Math.min(cap, 8 + Math.max(1, players)));
    }

    /**
     * Lasting difficulty of further summonings (owner 2026-10-11): +step per summoning on health, damage and speed,
     * linear, never above the cap; multiplies with the player scaling.
     */
    public static double summonFactor(double step, double cap, int summons) {
        return Math.max(1.0, Math.min(Math.max(1.0, cap), 1.0 + step * Math.max(0, summons)));
    }

    /** Whether the half-health threshold is reached (the session latches it per fight). */
    public static boolean half(float health, float max) {
        return max > 0 && health <= max * 0.5f;
    }

    /** Growl pitch of the n-th queued entry (0-based), 0.7 rising by 0.05, at most 1.0. */
    public static float growlPitch(int queuePosition) {
        return Math.min(1.0f, 0.7f + 0.05f * Math.max(0, queuePosition));
    }
}
