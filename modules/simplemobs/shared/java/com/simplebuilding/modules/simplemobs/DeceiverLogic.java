package com.simplebuilding.modules.simplemobs;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;

/**
 * Pure rules of the Deceiver: fight phases by health, wave sizes, cycle timing, provocation and
 * natural spawn conditions. No world access, so everything here is unit-testable.
 */
public final class DeceiverLogic {
    public enum Phase { ONE, TWO, THREE }
    public enum Stage { TELEGRAPH, OBSERVE }

    /** Hard upper bound for living summoned mobs per Deceiver (anti-lag / anti-exploit). */
    public static final int HARD_CAP = 24;
    public static final int STARE_TICKS = 60;
    public static final int SHOVES_TO_PROVOKE = 3;
    public static final int TELEGRAPH_TICKS = 40;
    public static final int PULSE_TICKS = 200;
    public static final int HEAL_COOLDOWN_TICKS = 600;
    public static final int FAKE_LIFETIME_TICKS = 1800;

    private static final String[] BASE = {"zombie", "spider", "husk"};
    private static final String[] MID = {"zombie", "husk", "spider", "vindicator"};
    private static final String[] STRONG = {"vindicator", "husk", "zombie", "spider"};
    private static final String[] SPECIAL_MID = {"skeleton", "stray"};
    private static final String[] SPECIAL_STRONG = {"witch", "evoker", "skeleton"};
    private static final String[] PEACEFUL = {"pig", "sheep", "cow", "chicken"};

    private DeceiverLogic() {}

    public static Phase phase(float health, float maxHealth) {
        float f = maxHealth <= 0 ? 0 : health / maxHealth;
        return f > 0.66f ? Phase.ONE : f > 0.33f ? Phase.TWO : Phase.THREE;
    }

    public static int waveMin(Phase p) { return switch (p) { case ONE -> 5; case TWO -> 8; case THREE -> 12; }; }
    public static int waveMax(Phase p) { return switch (p) { case ONE -> 8; case TWO -> 12; case THREE -> 16; }; }
    public static int waveSize(Phase p, RandomSource rnd) { return waveMin(p) + rnd.nextInt(waveMax(p) - waveMin(p) + 1); }
    public static float realChance(Phase p) { return switch (p) { case ONE -> 0.15f; case TWO -> 0.20f; case THREE -> 0.25f; }; }
    public static float specialChance(Phase p) { return switch (p) { case ONE -> 0f; case TWO -> 0.08f; case THREE -> 0.20f; }; }
    public static int maxSpecials() { return 2; }

    /** How many of the wanted mobs may really spawn given the alive ones and the cap. */
    public static int allowed(int wanted, int alive, int cap) {
        return Math.max(0, Math.min(wanted, Math.min(cap, HARD_CAP) - alive));
    }

    /** Length of a full cycle; shortens as the Deceiver gets weaker. */
    public static int cycleTicks(Phase p) { return switch (p) { case ONE -> 240; case TWO -> 200; case THREE -> 160; }; }
    public static int observeTicks(Phase p) { return cycleTicks(p) - TELEGRAPH_TICKS; }

    /** Which perception effects the pulse applies: index 0 = Mirage, 1 = Reverse Mirage (both in phase three). */
    public static boolean pulseMirage(Phase p, int pulseIndex) { return p == Phase.THREE || pulseIndex % 2 == 0; }
    public static boolean pulseReverse(Phase p, int pulseIndex) { return p == Phase.THREE || pulseIndex % 2 == 1; }

    public static int stare(int current, boolean beingLookedAt) {
        return beingLookedAt ? Math.min(STARE_TICKS, current + 1) : Math.max(0, current - 2);
    }

    public static boolean provoked(int stareTicks, int shoves, boolean hit) {
        return hit || stareTicks >= STARE_TICKS || shoves >= SHOVES_TO_PROVOKE;
    }

    /** Whether the Deceiver may drink a healing potion now. */
    public static boolean mayHeal(Phase p, float health, float maxHealth, int cooldown) {
        return cooldown <= 0 && p != Phase.ONE && health < maxHealth * 0.6f;
    }

    /** Mobs that cannot hurt anybody as a fake (melee, vanilla attack damage removed). */
    public static boolean fakeSafe(String kind) {
        return switch (kind) {
            case "zombie", "husk", "spider", "vindicator", "pig", "sheep", "cow", "chicken" -> true;
            default -> false;
        };
    }

    public static int tier(Phase p) { return p.ordinal(); }

    /** 2-3 kinds for one wave. */
    public static List<String> pickKinds(Phase p, boolean peaceful, RandomSource rnd) {
        String[] pool = peaceful ? PEACEFUL : switch (p) { case ONE -> BASE; case TWO -> MID; case THREE -> STRONG; };
        int n = Math.min(pool.length, 2 + rnd.nextInt(2));
        List<String> all = new ArrayList<>(List.of(pool));
        List<String> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(all.remove(rnd.nextInt(all.size())));
        return out;
    }

    /** A special (ranged or caster) kind for this phase, or null. Always real. */
    public static String pickSpecial(Phase p, RandomSource rnd) {
        String[] s = p == Phase.TWO ? SPECIAL_MID : p == Phase.THREE ? SPECIAL_STRONG : new String[0];
        return s.length == 0 ? null : s[rnd.nextInt(s.length)];
    }

    // ---- natural spawning -------------------------------------------------------------

    /** Chance per roll (about every 30 s per player) in a given place; 0 elsewhere. */
    public static float spawnChance(boolean darkForest, boolean outpost, boolean village) {
        float c = 0f;
        if (outpost) c = Math.max(c, 0.02f);
        if (darkForest) c = Math.max(c, 0.01f);
        if (village) c = Math.max(c, 0.005f);
        return c;
    }

    public static boolean mayNaturallySpawn(boolean night, boolean darkForest, boolean outpost, boolean village,
                                            int light, int deceiversNearby, float roll) {
        return night && light <= 7 && deceiversNearby == 0
                && roll < spawnChance(darkForest, outpost, village);
    }
}
