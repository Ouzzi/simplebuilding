package com.simplebuilding.util;

/**
 * Which bar sits in vanilla's contextual bar slot (182x5, where the experience bar is) while an
 * air jump recharges - the pure half of {@code DoubleJumpHudOverlay}, free of client classes so a
 * server gametest can pin it (owner 2026-09-29).
 *
 * <p>Vanilla already shares that slot: in multiplayer it shows the locator bar and switches to the
 * experience bar for {@link #EXPERIENCE_DISPLAY_TICKS} ticks whenever the experience changes
 * ({@code Hud#willPrioritizeExperienceInfo}: {@code experienceDisplayStartTick + 100 > tickCount}),
 * and a jumpable vehicle takes it for its jump bar. The air jump bar slots in between:
 * <ol>
 *   <li>vehicle jump bar - vanilla's choice, untouched;</li>
 *   <li>experience changed recently (the same rule vanilla uses against the locator bar) - the
 *       experience bar;</li>
 *   <li>an air jump cooldown is running - the air jump bar;</li>
 *   <li>otherwise vanilla's default (locator bar in multiplayer, experience bar otherwise).</li>
 * </ol>
 * The mod HUD switch ({@code showModHud}, the HUD key) turns the air jump bar off, and the spawn
 * elytra, which already paints its own boost bar over the experience bar, keeps the slot.
 */
public final class AirJumpBarRule {

    /** Vanilla's {@code Hud.EXPERIENCE_BAR_DISPLAY_TICKS}: how long a changed experience value wins the slot. */
    public static final int EXPERIENCE_DISPLAY_TICKS = 100;

    /** Width of the bar, identical to vanilla's contextual bar. */
    public static final int BAR_WIDTH = 182;

    private AirJumpBarRule() {
    }

    /** Vanilla's {@code willPrioritizeExperienceInfo}, from the two fields it reads. */
    public static boolean experienceChangedRecently(int experienceDisplayStartTick, int playerTickCount) {
        return experienceDisplayStartTick + EXPERIENCE_DISPLAY_TICKS > playerTickCount;
    }

    /**
     * Whether the air jump bar replaces vanilla's bar this frame.
     *
     * @param vehicleBarActive         vanilla picked the jumpable vehicle bar
     * @param canShowExperience        {@code gameMode.hasExperience()} (false in creative and spectator)
     * @param experienceChangedRecently see {@link #experienceChangedRecently}
     * @param cooldownRemaining        ticks until the air jump recharges
     * @param cooldownMax              length of the running cooldown
     * @param hudVisible               the mod HUD switch
     * @param spawnElytraBar           the spawn elytra paints its boost bar into the slot
     */
    public static boolean showsAirJumpBar(boolean vehicleBarActive, boolean canShowExperience, boolean experienceChangedRecently,
                                          int cooldownRemaining, int cooldownMax, boolean hudVisible, boolean spawnElytraBar) {
        if (!hudVisible || spawnElytraBar || vehicleBarActive) {
            return false;
        }
        if (canShowExperience && experienceChangedRecently) {
            return false;
        }
        return cooldownRemaining > 0 && cooldownMax > 0;
    }

    /** Recharged share of the cooldown, 0 (just jumped) to 1 (ready). */
    public static float charged(int cooldownRemaining, int cooldownMax) {
        if (cooldownMax <= 0) {
            return 1.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, (float) (cooldownMax - cooldownRemaining) / (float) cooldownMax));
    }

    /** Width of the progress sprite in pixels (0..182), rounded like the old bar. */
    public static int progressWidth(int cooldownRemaining, int cooldownMax) {
        return Math.round(BAR_WIDTH * charged(cooldownRemaining, cooldownMax));
    }
}
