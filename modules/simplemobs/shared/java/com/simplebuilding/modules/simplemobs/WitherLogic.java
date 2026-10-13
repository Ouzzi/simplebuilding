package com.simplebuilding.modules.simplemobs;

/**
 * Blighted Wither numbers and pure rules (owner 13.10.: the concept's Wither, just stronger - more health, phases and
 * attacks, every special attack announced). Kept free of game objects so the GameTests can check them directly.
 */
public final class WitherLogic {
    /** Twice the Vanilla 300 health, armor 8 (Vanilla 4). */
    public static final double HEALTH = 600.0;
    public static final double ARMOR = 8.0;
    public static final int XP = 100;
    /** Wither Hearts per kill: always one (Looting does not change it; only the Blighted Wither drops it). */
    public static final int HEARTS = 1;
    /** Phase 2 below half health (the Vanilla armor/shield phase), phase 3 "Blight" below 15 %. */
    public static final float PHASE_2 = 0.5f, PHASE_3 = 0.15f;

    /** Every special attack is announced this long before it hits (sound + particles at the spot). */
    public static final int VOLLEY_WARN = 20, MINION_WARN = 40, NOVA_WARN = 40, WILT_WARN = 40;
    /** Pause between attacks of a kind (ticks, from one warning to the next). */
    public static final int VOLLEY_INTERVAL = 100, MINION_INTERVAL = 400, NOVA_INTERVAL = 240, WILT_INTERVAL = 600;
    /** Twin volley: one skull from each side head at the target at once. */
    public static final int VOLLEY_SKULLS = 2;
    /** Minions: three Wither Skeletons, never more than six near the Wither. */
    public static final int MINIONS = 3, MINION_CAP = 6;
    /** Nova (phase 3): who stays inside 8 blocks gets Wither II for 6 s and is pushed away. */
    public static final double NOVA_RADIUS = 8.0;
    public static final int NOVA_WITHER_TICKS = 120, NOVA_WITHER_AMPLIFIER = 1;
    /** Blight: plants and natural leaves within 12 blocks wilt (never placed blocks of a build). */
    public static final int WILT_RADIUS = 12;

    /** Owner N50: below this share the called Wither Skeletons come armored (iron at stage 1, diamond at stage 2). */
    public static final float ARMORED_SHARE = 0.3f;
    /** Owner N50: drops besides the heart - 3 to 5 nuggets of the helmet's metal and 1 to 2 Nether Stars. */
    public static final int NUGGETS_MIN = 3, NUGGETS_MAX = 5, STARS_MIN = 1, STARS_MAX = 2;

    /**
     * The two summoning stages (owner N50): a Netherite Helmet on the middle skull summons the Blighted Wither, an
     * Enderite Helmet the Abyssal Wither. Warnings stay the same at both stages (fair), only the numbers grow.
     */
    public record Stage(String metal, double health, double armor, int xp, int volleySkulls, int minions, int minionCap,
                        float pause, double novaPush, double novaLift) {
        /** Pause between attacks of a kind at this stage (ticks). */
        public int interval(int base) {
            return Math.round(base * pause);
        }

        /** Minions to call: never past the cap of Wither Skeletons already near. */
        public int minionsToCall(int nearby) {
            return Math.max(0, Math.min(minions, minionCap - nearby));
        }
    }

    public static final Stage NETHERITE = new Stage("netherite", HEALTH, ARMOR, XP, VOLLEY_SKULLS, MINIONS, MINION_CAP, 1.0f, 2.8, 0.6);
    public static final Stage ENDERITE = new Stage("enderite", 1000.0, 14.0, 200, 3, 4, 8, 0.75f, 3.4, 0.7);

    /** Whether minions called at this health share come armored. */
    public static boolean armoredMinions(float health, float max) {
        return max > 0 && health / max <= ARMORED_SHARE;
    }

    private WitherLogic() {}

    /** Phase 1, 2 or 3 for a health share. */
    public static int phase(float health, float max) {
        float share = max <= 0 ? 0 : health / max;
        return share < PHASE_3 ? 3 : share <= PHASE_2 ? 2 : 1;
    }

    /** Minions to call: three, but never past the cap of Wither Skeletons already near. */
    public static int minionsToCall(int nearby) {
        return Math.max(0, Math.min(MINIONS, MINION_CAP - nearby));
    }

}
