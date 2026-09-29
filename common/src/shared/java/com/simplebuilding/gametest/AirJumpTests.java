package com.simplebuilding.gametest;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.util.AirJumpBarRule;
import com.simplebuilding.util.AirJumpGuard;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * Air jump cooldown and its bar (owner 2026-09-29): 20 s at level I, 10 s at level II, configurable
 * on the server and clamped; the bar in vanilla's contextual bar slot follows vanilla's own
 * experience-over-locator rule. The bar's decision and fill are the pure {@link AirJumpBarRule}
 * both loaders' {@code DoubleJumpHudOverlay} delegate to, so they are pinned here; that the mixin
 * really puts the bar on screen is the client tests' job ({@code AirJumpClientTest}).
 */
public final class AirJumpTests {

    private AirJumpTests() {
    }

    /**
     * The new default is 400 ticks (20 s) at level I and half of it (10 s) at level II, the server
     * sends exactly that to its clients, and a config value outside 20..6000 is pulled back in by
     * loading ({@code validatePostLoad}) as well as by what the server sends and waits.
     *
     * <p>What breaks it: the old default of 100, dropping the clamp in either place, or level II no
     * longer halving.
     */
    public static void theCooldownIsTwentySecondsAtLevelOneAndTenAtLevelTwo(GameTestHelper helper) {
        SimplebuildingConfig fresh = new SimplebuildingConfig();
        helper.assertTrue(fresh.airJumpCooldownTicks == 400,
                "a fresh config's air jump cooldown is " + fresh.airJumpCooldownTicks + " ticks, not 400 (20 s)");
        helper.assertTrue(AirJumpGuard.cooldownTicks(1, 400) == 400 && AirJumpGuard.cooldownTicks(2, 400) == 200,
                "at the default the levels wait " + AirJumpGuard.cooldownTicks(1, 400) + " / "
                        + AirJumpGuard.cooldownTicks(2, 400) + " ticks instead of 400 / 200 (20 s / 10 s)");

        SimplebuildingConfig config = Simplebuilding.getConfig();
        int original = config.airJumpCooldownTicks;
        try {
            config.airJumpCooldownTicks = 400;
            helper.assertTrue(AirJumpGuard.cooldownTicks(1) == 400 && AirJumpGuard.cooldownTicks(2) == 200,
                    "the server guard does not wait 20 s / 10 s at the default");
            helper.assertTrue(SimpleTweaks.localValues().airJumpCooldownTicks() == 400,
                    "the server sends " + SimpleTweaks.localValues().airJumpCooldownTicks() + " instead of 400");

            // --- too short: an air jump every second is the floor, below that it is flight ---
            config.airJumpCooldownTicks = 0;
            helper.assertTrue(SimpleTweaks.localValues().airJumpCooldownTicks() == AirJumpGuard.MIN_COOLDOWN_TICKS,
                    "a cooldown of 0 is sent as " + SimpleTweaks.localValues().airJumpCooldownTicks() + " instead of the floor");
            helper.assertTrue(AirJumpGuard.cooldownTicks(1) == 20 && AirJumpGuard.cooldownTicks(2) == 10,
                    "a cooldown of 0 lets the guard wait " + AirJumpGuard.cooldownTicks(1) + " ticks instead of 20");
            config.validatePostLoad();
            helper.assertTrue(config.airJumpCooldownTicks == 20,
                    "loading keeps a cooldown of 0 as " + config.airJumpCooldownTicks + " instead of 20");

            // --- too long: five minutes is the ceiling ---
            config.airJumpCooldownTicks = 1_000_000;
            helper.assertTrue(SimpleTweaks.localValues().airJumpCooldownTicks() == AirJumpGuard.MAX_COOLDOWN_TICKS,
                    "a huge cooldown is sent as " + SimpleTweaks.localValues().airJumpCooldownTicks() + " instead of 6000");
            config.validatePostLoad();
            helper.assertTrue(config.airJumpCooldownTicks == 6000,
                    "loading keeps a huge cooldown as " + config.airJumpCooldownTicks + " instead of 6000");
        } finally {
            config.airJumpCooldownTicks = original;
        }
        helper.succeed();
    }

    /**
     * Which bar takes vanilla's contextual bar slot, as a decision table over everything the rule
     * reads. The experience bar wins exactly while vanilla itself would put it over the locator bar
     * ({@code experienceDisplayStartTick + 100 > tickCount}), and only where experience is shown at
     * all; then a running cooldown puts the air jump bar there; the vehicle jump bar, the HUD switch
     * and the spawn elytra's own bar keep the slot for vanilla.
     *
     * <p>What breaks it: the air jump bar beating a fresh experience change, an off-by-one in the
     * 100 tick window, a creative player (no experience shown) never seeing the bar, the HUD key no
     * longer hiding it, or the bar showing without a cooldown.
     */
    public static void theBarFollowsVanillasExperienceOverLocatorRule(GameTestHelper helper) {
        // vanilla's window: shown from the change tick through 99 ticks later
        helper.assertTrue(AirJumpBarRule.experienceChangedRecently(1000, 1000), "the tick of the change is not recent");
        helper.assertTrue(AirJumpBarRule.experienceChangedRecently(1000, 1099), "99 ticks after the change is not recent");
        helper.assertTrue(!AirJumpBarRule.experienceChangedRecently(1000, 1100), "100 ticks after the change is still recent");

        // vehicle, canXp, xpRecent, remaining, max, hud, elytra -> expected
        Object[][] table = {
                {false, true, false, 200, 400, true, false, true},   // cooldown, experience quiet: air jump bar
                {false, true, true, 200, 400, true, false, false},   // experience just changed: experience bar
                {false, false, true, 200, 400, true, false, true},   // creative: no experience bar to prefer
                {false, true, false, 0, 400, true, false, false},    // cooldown over: vanilla default
                {false, true, false, 200, 0, true, false, false},    // no cooldown length: nothing to show
                {true, true, false, 200, 400, true, false, false},   // riding a jumpable vehicle
                {false, true, false, 200, 400, false, false, false}, // HUD key off
                {false, true, false, 200, 400, true, true, false},   // spawn elytra paints the slot
        };
        for (Object[] row : table) {
            boolean shows = AirJumpBarRule.showsAirJumpBar((boolean) row[0], (boolean) row[1], (boolean) row[2],
                    (int) row[3], (int) row[4], (boolean) row[5], (boolean) row[6]);
            helper.assertTrue(shows == (boolean) row[7],
                    "vehicle=" + row[0] + " canShowXp=" + row[1] + " xpRecent=" + row[2] + " remaining=" + row[3]
                            + " max=" + row[4] + " hud=" + row[5] + " spawnElytra=" + row[6]
                            + ": the air jump bar " + (shows ? "shows" : "does not show") + ", expected the opposite");
        }
        helper.succeed();
    }

    /**
     * The bar fills up while the air jump recharges: empty right after the jump, half at half the
     * cooldown, full (182 px, the whole contextual bar) once ready, never outside 0..182.
     */
    public static void theBarFillsUpWhileTheAirJumpRecharges(GameTestHelper helper) {
        helper.assertTrue(AirJumpBarRule.progressWidth(400, 400) == 0, "the bar is not empty right after the jump");
        helper.assertTrue(AirJumpBarRule.progressWidth(200, 400) == 91, "at half the cooldown the bar is "
                + AirJumpBarRule.progressWidth(200, 400) + " px wide, not 91");
        helper.assertTrue(AirJumpBarRule.progressWidth(0, 400) == 182, "the recharged bar is not the full 182 px");
        helper.assertTrue(AirJumpBarRule.progressWidth(500, 400) == 0 && AirJumpBarRule.progressWidth(-5, 400) == 182,
                "the fill leaves 0..182 for a remaining time outside the cooldown");
        helper.assertTrue(AirJumpBarRule.BAR_WIDTH == 182, "the bar is no longer as wide as vanilla's experience bar");
        helper.succeed();
    }

    /**
     * The server enforces the full air jump cooldown (owner 2026-09-29): a jump inside the cooldown
     * is refused, landing in between does NOT reset it (until 2026-09-29 the first jump of every fall
     * went through, so a modified client could skip the 20 s / 10 s by touching down briefly), and it
     * is accepted again once the cooldown minus the lag tolerance has passed. The tolerance is never
     * more than 20 ticks (1 s) and never more than a quarter of the cooldown.
     *
     * <p>Driven through {@code AirJumpGuard.tryUse(player, level, now)} with an explicit server tick,
     * so the test does not have to wait 20 real seconds; the packet handler reads the real tick.
     *
     * <p>What breaks it: landing clearing the record again, a tolerance above 1 s, the cooldown
     * counting from the refused packet instead of the accepted one, or level II no longer halving.
     */
    @SuppressWarnings("removal")
    public static void theServerEnforcesTheFullCooldownAcrossLandings(GameTestHelper helper) {
        // --- the tolerance: at most 20 ticks, at most a quarter of the cooldown ---
        for (int cooldown = 0; cooldown <= AirJumpGuard.MAX_COOLDOWN_TICKS; cooldown++) {
            int tolerance = AirJumpGuard.lagToleranceTicks(cooldown);
            helper.assertTrue(tolerance >= 0 && tolerance <= 20 && tolerance <= cooldown / 4,
                    "a cooldown of " + cooldown + " ticks gets a lag tolerance of " + tolerance + " ticks");
        }
        helper.assertTrue(AirJumpGuard.MAX_LAG_TOLERANCE_TICKS == 20
                        && AirJumpGuard.lagToleranceTicks(400) == 20 && AirJumpGuard.lagToleranceTicks(200) == 20
                        && AirJumpGuard.lagToleranceTicks(20) == 5,
                "the lag tolerance is not 20 / 20 / 5 ticks for cooldowns of 400 / 200 / 20");

        SimplebuildingConfig config = Simplebuilding.getConfig();
        int original = config.airJumpCooldownTicks;
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            config.airJumpCooldownTicks = 400;
            AirJumpGuard.forget(player);
            int t = 10_000;

            // --- level I: 400 ticks, accepted again from 380 on ---
            player.setOnGround(false);
            helper.assertTrue(AirJumpGuard.tryUse(player, 1, t), "the first air jump was refused");
            helper.assertTrue(!AirJumpGuard.tryUse(player, 1, t + 1), "a second air jump one tick later was accepted");

            // landing in between does not reset the cooldown - also not through the player tick
            // (PlayerEntityMixin used to clear the record there)
            player.setOnGround(true);
            player.doTick();
            helper.assertTrue(!AirJumpGuard.tryUse(player, 1, t + 2), "an air jump packet on the ground was accepted");
            player.setOnGround(false);
            helper.assertTrue(!AirJumpGuard.tryUse(player, 1, t + 3),
                    "landing reset the air jump cooldown - a client could skip it by touching down");
            helper.assertTrue(AirJumpGuard.isUsed(player), "landing dropped the server's record of the air jump");

            helper.assertTrue(!AirJumpGuard.tryUse(player, 1, t + 379),
                    "an air jump 21 ticks before the end of the cooldown was accepted - more than 1 s tolerance");
            helper.assertTrue(AirJumpGuard.tryUse(player, 1, t + 380),
                    "an air jump 20 ticks (1 s lag tolerance) before the end of the cooldown was refused");

            // --- the next cooldown runs from the accepted jump; level II waits 200, from 180 on ---
            t += 380;
            helper.assertTrue(!AirJumpGuard.tryUse(player, 2, t + 179), "level II accepted an air jump after 179 ticks");
            helper.assertTrue(AirJumpGuard.tryUse(player, 2, t + 180), "level II refused an air jump after 180 ticks");

            // --- the floor: 20 ticks, tolerance 5 ---
            config.airJumpCooldownTicks = 20;
            t += 1_000;
            AirJumpGuard.forget(player);
            helper.assertTrue(AirJumpGuard.tryUse(player, 1, t), "the first air jump at the shortest cooldown was refused");
            helper.assertTrue(!AirJumpGuard.tryUse(player, 1, t + 14), "the shortest cooldown accepted a jump after 14 ticks");
            helper.assertTrue(AirJumpGuard.tryUse(player, 1, t + 15), "the shortest cooldown refused a jump after 15 ticks");
        } finally {
            config.airJumpCooldownTicks = original;
            AirJumpGuard.forget(player);
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }
}
