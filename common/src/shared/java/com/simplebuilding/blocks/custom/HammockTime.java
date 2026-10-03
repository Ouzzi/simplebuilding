package com.simplebuilding.blocks.custom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Resting in a hammock speeds the day up (docs/ai/PLAN-HAENGEMATTE-2026-10-02.md), the daytime counterpart of the bed.
 *
 * <ul>
 *   <li>A hammock may be used while {@link Level#isBrightOutside()} in a dimension with a default clock - exactly when
 *       the vanilla bed rule {@code when_dark} says no. When it gets dark, vanilla wakes the resting players itself
 *       ({@code Player#tick} asks the hammock's bed rule every tick).</li>
 *   <li>Like vanilla sleeping, enough players must rest: at least {@code max(1, ceil(active * players_sleeping_percentage
 *       / 100))} of the non-spectators in that level; a percentage above 100 turns it off, as it does for beds.</li>
 *   <li>Then the level's clock gets {@code factor - 1} extra ticks per tick (server option {@code server.hammock.timeFactor},
 *       1 to {@link com.simplebuilding.config.ServerTuning#MAX_HAMMOCK_FACTOR}), once per clock and server tick, and only
 *       with the game rule {@code advance_time}. {@code addTicks} keeps nothing in the save, unlike a clock rate.</li>
 * </ul>
 */
public final class HammockTime {
    /** Resting players never count as deep sleepers: their sleep counter stays below vanilla's 100 (see the player mixin). */
    public static final int DOZE_TICKS = 40;
    private static final Map<Holder<WorldClock>, Integer> LAST_TICK = new HashMap<>();
    private static final Map<ServerLevel, Long> LAST_WAKE = new java.util.WeakHashMap<>();

    private HammockTime() {
    }

    /** Whether a hammock may be (and stay) used in this level right now. */
    public static boolean restAllowed(Level level) {
        return level.dimensionType().defaultClock().isPresent() && level.isBrightOutside();
    }

    /** Whether this player lies in a hammock. */
    public static boolean inHammock(Player player) {
        return player.isSleeping() && player.getSleepingPos()
                .map(pos -> player.level().getBlockState(pos).getBlock() instanceof HammockBlock).orElse(false);
    }

    /** Vanilla's {@code SleepStatus#sleepersNeeded}: at least one, the percentage of the active players rounded up. */
    public static int restersNeeded(int active, int percentage) {
        return Math.max(1, Mth.ceil(active * percentage / 100.0F));
    }

    /**
     * Extra clock ticks per tick for these numbers: {@code factor - 1} when enough of the active players rest and the
     * hammock may be used, otherwise 0.
     */
    public static int extraTicks(int active, int resting, int percentage, boolean allowed, boolean advanceTime, int factor) {
        if (!allowed || !advanceTime || percentage > 100 || resting <= 0 || active <= 0) {
            return 0;
        }
        return resting >= restersNeeded(active, percentage) ? Math.max(0, factor - 1) : 0;
    }

    /** Extra ticks for {@code level} with these players (the level's own players in play, a test's list in a test). */
    public static int extraTicks(ServerLevel level, List<? extends Player> players) {
        int active = 0;
        int resting = 0;
        for (Player player : players) {
            if (player.isSpectator()) {
                continue;
            }
            active++;
            if (inHammock(player)) {
                resting++;
            }
        }
        return extraTicks(active, resting, level.getGameRules().get(GameRules.PLAYERS_SLEEPING_PERCENTAGE), restAllowed(level),
                level.getGameRules().get(GameRules.ADVANCE_TIME), com.simplebuilding.config.ServerTuning.hammockTimeFactor());
    }

    /** End of every level tick (mixin): moves the level's clock on while enough players rest in hammocks. */
    public static void tick(ServerLevel level) {
        if (!com.simplebuilding.version.McVersion.HAMMOCK || level.players().isEmpty()) {
            return;
        }
        Optional<Holder<WorldClock>> clock = level.dimensionType().defaultClock();
        if (clock.isEmpty()) {
            return;
        }
        int extra = extraTicks(level, level.players());
        if (extra <= 0) {
            return;
        }
        int now = level.getServer().getTickCount();
        Integer last = LAST_TICK.put(clock.get(), now);
        if (last != null && last == now) {
            return; // two levels share this clock: speed it up once
        }
        level.clockManager().addTicks(clock.get(), extra);
    }

    /** A player left a hammock this tick (the bed's stop-sleeping hook). */
    public static void markWake(ServerLevel level) {
        LAST_WAKE.put(level, level.getGameTime());
    }

    /**
     * Vanilla's "x/y players sleeping" action bar is on-screen text; hammocks stay silent (owner rule). It is skipped while
     * nobody sleeps in a real bed and either the hammock may be used or a hammock was just left.
     */
    public static boolean silenceSleepAnnouncement(ServerLevel level) {
        if (!com.simplebuilding.version.McVersion.HAMMOCK) {
            return false;
        }
        for (Player player : level.players()) {
            if (player.isSleeping() && !inHammock(player)) {
                return false;
            }
        }
        Long wake = LAST_WAKE.get(level);
        return restAllowed(level) || wake != null && wake == level.getGameTime();
    }
}
