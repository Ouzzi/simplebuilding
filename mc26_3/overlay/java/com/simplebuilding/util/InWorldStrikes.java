package com.simplebuilding.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 26.3: thin shell over the one shared implementation in SimpleLib ({@code com.simplelib.api.InWorldStrikes},
 * owner 2026-10-10: all mods use the same strike logic). Same surface as the 26.2 copy in {@code common/src/mc26_2}
 * (26.2 has no SimpleLib). Rules, cracks, particles, sound, partial results and the growing preview: see the library.
 */
public final class InWorldStrikes {
    public static final int MIN_INTERVAL = com.simplelib.api.InWorldStrikes.MIN_INTERVAL;
    public static final int RESET_TICKS = com.simplelib.api.InWorldStrikes.RESET_TICKS;
    public static final String PENDING_TAG = com.simplelib.api.InWorldStrikes.PENDING_TAG;

    /** Strike {@code done} of {@code total}; at the end {@code finished}. */
    public record Strike(int done, int total, boolean finished) {
    }

    private InWorldStrikes() {
    }

    public static int count(ServerLevel level, BlockPos pos, String kind, BlockState state, int total, long now) {
        return com.simplelib.api.InWorldStrikes.count(level, pos, kind, state, total, now);
    }

    public static @Nullable Strike strikeYield(ServerLevel level, BlockPos pos, String kind, BlockState state, ItemStack result,
                                               int total, long now) {
        var strike = com.simplelib.api.InWorldStrikes.strikeYield(level, pos, kind, state, result, total, now);
        return strike == null ? null : new Strike(strike.done(), strike.total(), strike.finished());
    }

    public static void release(ServerLevel level, BlockPos pos, ItemStack result) {
        com.simplelib.api.InWorldStrikes.release(level, pos, result);
    }

    public static ItemStack part(ItemStack result, int total, int done) {
        return com.simplelib.api.InWorldStrikes.part(result, total, done);
    }

    public static int shown(ServerLevel level, BlockPos pos) {
        return com.simplelib.api.InWorldStrikes.shown(level, pos);
    }

    public static void allowNextStrike(ServerLevel level, BlockPos pos) {
        com.simplelib.api.InWorldStrikes.allowNextStrike(level, pos);
    }

    public static int done(ServerLevel level, BlockPos pos) {
        return com.simplelib.api.InWorldStrikes.done(level, pos);
    }

    public static void clear(ServerLevel level, BlockPos pos) {
        com.simplelib.api.InWorldStrikes.clear(level, pos);
    }

    public static void feedback(ServerLevel level, BlockPos pos, BlockState shown, int done, int total) {
        com.simplelib.api.InWorldStrikes.feedback(level, pos, shown, done, total);
    }

    public static void crack(ServerLevel level, BlockPos pos, int done, int total) {
        com.simplelib.api.InWorldStrikes.crack(level, pos, done, total);
    }

    public static void preview(ServerLevel level, BlockPos pos, BlockState target, int done, int total) {
        com.simplelib.api.InWorldStrikes.preview(level, pos, target, done, total);
    }

    public static int crackStage(int done, int total) {
        return com.simplelib.api.InWorldStrikes.crackStage(done, total);
    }

    public static int crackId(BlockPos pos) {
        return com.simplelib.api.InWorldStrikes.crackId(pos);
    }

    public static void tick(MinecraftServer server) {
        com.simplelib.api.InWorldStrikes.tick(server);
    }
}
