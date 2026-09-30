package com.simplebuilding.util;

import com.simplebuilding.platform.PlatformServices;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The one question every multi-block tool of the mod asks before it changes a block that the
 * player did not click: may this player break or build <em>here</em>? Vanilla spawn protection and
 * the world border ({@link Level#mayInteract}), then claim and protection mods through the loader's
 * own break and place events ({@link com.simplebuilding.platform.BuildGuard}).
 *
 * <p>Asked on the server only; on the client (and for a non-server player) everything is allowed,
 * the server has the last word anyway. While a check is running, {@link #isProbing()} is true: the
 * mod's own break listeners (sledgehammer, Vein Miner, Strip Miner) then do nothing, so asking
 * about a cell never starts an area break.
 */
public final class BuildPermissions {

    private static int probing;

    private BuildPermissions() {
    }

    /** True while a break or place event fired by this class is being answered. */
    public static boolean isProbing() {
        return probing > 0;
    }

    /** Whether {@code player} may break (or melt, dry, light, prime) the block at {@code pos}. */
    public static boolean mayBreak(Level level, Player player, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }
        if (!level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(player, pos) || !com.simplebuilding.api.WorldPermissions.mayChange(level, player, pos)) {
            return false;
        }
        probing++;
        try {
            return PlatformServices.buildGuard().mayBreak(serverLevel, serverPlayer, pos.immutable(), state);
        } finally {
            probing--;
        }
    }

    /** Whether {@code player} may place {@code state} at {@code pos}. */
    public static boolean mayPlace(Level level, Player player, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }
        if (!level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(player, pos) || !com.simplebuilding.api.WorldPermissions.mayChange(level, player, pos)) {
            return false;
        }
        probing++;
        try {
            return com.simplebuilding.api.WorldPermissions.mayPlace(level, player, pos, state)
                    && PlatformServices.buildGuard().mayPlace(serverLevel, serverPlayer, pos.immutable(), state);
        } finally {
            probing--;
        }
    }
}
