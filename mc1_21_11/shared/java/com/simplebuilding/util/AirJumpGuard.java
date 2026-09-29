package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Server-side check of the air jump packet (audit 2026-09-26 #30, full cooldown owner 2026-09-29).
 *
 * <p>The client decides an air jump on its own ({@code DoubleJumpController}) and only tells the
 * server afterwards, so the server keeps its own record per player and believes a
 * {@code DoubleJumpPayload} only when
 * <ul>
 *   <li>the player is <b>not on the ground</b>, and</li>
 *   <li>the <b>full cooldown</b> since the last accepted air jump has run out
 *       ({@code airJumpCooldownTicks}, half from level II on) - landing does NOT reset it. Until
 *       2026-09-29 landing cleared the record, so the first jump of every fall went through and a
 *       modified client could skip the 20 s / 10 s by touching down briefly.</li>
 * </ul>
 * Packets sent a cooldown apart can arrive a little closer together (lag, bunching), so the server
 * waits {@link #lagToleranceTicks} less than the client: at most {@link #MAX_LAG_TOLERANCE_TICKS}
 * (1 s, owner 2026-09-29) and never more than a quarter of the cooldown. The client's own cooldown
 * (the XP-slot bar) also keeps running across landings, so a normal client never hits the guard.
 */
public final class AirJumpGuard {

    /** Upper bound of the lag tolerance: 1 second (owner 2026-09-29). */
    public static final int MAX_LAG_TOLERANCE_TICKS = 20;

    /**
     * Default {@code airJumpCooldownTicks}: 20 s at level I, so 10 s at level II (owner 2026-09-29;
     * before: 100 ticks = 5 s / 2.5 s).
     */
    public static final int DEFAULT_COOLDOWN_TICKS = 400;
    /** Shortest configurable level I cooldown (1 s): below that the air jump turns into flight. */
    public static final int MIN_COOLDOWN_TICKS = 20;
    /** Longest configurable level I cooldown (5 min). */
    public static final int MAX_COOLDOWN_TICKS = 6000;

    /** The configured level I cooldown kept inside {@link #MIN_COOLDOWN_TICKS}..{@link #MAX_COOLDOWN_TICKS}. */
    public static int clampBase(int configured) {
        return Math.max(MIN_COOLDOWN_TICKS, Math.min(MAX_COOLDOWN_TICKS, configured));
    }

    /** Server tick of the last accepted air jump; absent = none yet (or the player object is new). */
    private static final Map<ServerPlayer, Integer> JUMP_USED_AT = new WeakHashMap<>();

    private AirJumpGuard() {
    }

    /**
     * How much earlier than the full {@code cooldown} the server accepts the next air jump: a quarter
     * of the cooldown, at most {@link #MAX_LAG_TOLERANCE_TICKS}. 400 ticks -> 20, 200 -> 20, 20 -> 5.
     */
    public static int lagToleranceTicks(int cooldown) {
        return Math.max(0, Math.min(MAX_LAG_TOLERANCE_TICKS, cooldown / 4));
    }

    /**
     * Whether this air jump is allowed, and if so, records it. {@code level} is the Double Jump
     * level of the boots (already checked to be at least 1).
     */
    public static boolean tryUse(ServerPlayer player, int level) {
        int now = player.level().getServer() != null ? player.level().getServer().getTickCount() : 0;
        return tryUse(player, level, now);
    }

    /** {@link #tryUse(ServerPlayer, int)} at server tick {@code now} (the gametests drive the clock). */
    public static boolean tryUse(ServerPlayer player, int level, int now) {
        // Serverschalter server.features.airJump (2026-09-28): aus = kein Luftsprung, egal was der Client meint.
        if (player.onGround() || !com.simplebuilding.config.ServerTuning.get().features.airJump) {
            return false;
        }
        Integer usedAt = JUMP_USED_AT.get(player);
        if (usedAt != null) {
            int cooldown = cooldownTicks(level);
            if (now - usedAt < cooldown - lagToleranceTicks(cooldown)) {
                return false;
            }
        }
        JUMP_USED_AT.put(player, now);
        return true;
    }

    /** Whether the server has an accepted air jump on record for this player (landing keeps it). */
    public static boolean isUsed(ServerPlayer player) {
        return JUMP_USED_AT.containsKey(player);
    }

    /** Test hook: drops the record, as if the cooldown had long run out. */
    public static void forget(ServerPlayer player) {
        JUMP_USED_AT.remove(player);
    }

    /** The same cooldown the client waits, see {@code DoubleJumpController#cooldownTicksForLevel}. */
    public static int cooldownTicks(int level) {
        return cooldownTicks(level, clampBase(Simplebuilding.getConfig().airJumpCooldownTicks));
    }

    /** Level 1 = {@code base}, level 2+ = half of it (at least 1 tick); negative bases count as 0. */
    public static int cooldownTicks(int level, int base) {
        int clamped = Math.max(0, base);
        return level >= 2 ? Math.max(1, clamped / 2) : clamped;
    }
}
