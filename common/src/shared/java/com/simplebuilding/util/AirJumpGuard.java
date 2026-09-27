package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Server-side check of the air jump packet (audit 2026-09-26 #30).
 *
 * <p>The client decides an air jump on its own ({@code DoubleJumpController}) and only tells the
 * server afterwards, so the server used to believe every {@code DoubleJumpPayload}: a modified
 * client could send one on every tick just before landing and never take fall damage. The server
 * now keeps its own record per player:
 * <ul>
 *   <li>an air jump is only accepted while the player is <b>not on the ground</b>;</li>
 *   <li>once one is used, it stays used until the player is back on the ground - a second one in
 *       the same fall is only accepted after the cooldown the client itself waits
 *       ({@code airJumpCooldownTicks}, half from level II on), minus {@link #LATENCY_SLACK_TICKS}
 *       for packets that bunch up on the way.</li>
 * </ul>
 * Landing clears the record ({@link #onPlayerTick}, called from the player tick mixin), so the
 * first air jump of every fall is always accepted - exactly what a normal client can do.
 */
public final class AirJumpGuard {

    /** Packets sent a cooldown apart can arrive a little closer together. */
    public static final int LATENCY_SLACK_TICKS = 3;

    /** Server tick of the last accepted air jump in the current fall; absent = none used since landing. */
    private static final Map<ServerPlayer, Integer> JUMP_USED_AT = new WeakHashMap<>();

    private AirJumpGuard() {
    }

    /**
     * Whether this air jump is allowed, and if so, records it. {@code level} is the Double Jump
     * level of the boots (already checked to be at least 1).
     */
    public static boolean tryUse(ServerPlayer player, int level) {
        if (player.onGround()) {
            return false;
        }
        int now = player.level().getServer() != null ? player.level().getServer().getTickCount() : 0;
        Integer usedAt = JUMP_USED_AT.get(player);
        if (usedAt != null && now - usedAt < cooldownTicks(level) - LATENCY_SLACK_TICKS) {
            return false;
        }
        JUMP_USED_AT.put(player, now);
        return true;
    }

    /** Clears the record once the player stands on the ground again. Cheap when nobody jumped. */
    public static void onPlayerTick(ServerPlayer player) {
        if (!JUMP_USED_AT.isEmpty() && player.onGround()) {
            JUMP_USED_AT.remove(player);
        }
    }

    /** Whether an air jump has been used since the player last stood on the ground. */
    public static boolean isUsed(ServerPlayer player) {
        return JUMP_USED_AT.containsKey(player);
    }

    /** The same cooldown the client waits, see {@code DoubleJumpController#cooldownTicksForLevel}. */
    public static int cooldownTicks(int level) {
        int base = Math.max(0, Simplebuilding.getConfig().airJumpCooldownTicks);
        return level >= 2 ? Math.max(1, base / 2) : base;
    }
}
