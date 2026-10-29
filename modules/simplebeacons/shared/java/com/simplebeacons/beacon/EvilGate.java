package com.simplebeacons.beacon;

import com.simplebeacons.config.BeaconsConfig;
import com.simplelib.api.HarmShields;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;

/**
 * Who the Evil beacons reach (BS28b-5, balanced against griefing): only survival players, not protected by Simple
 * Tweaks' personal PvP switch or newcomer protection ({@link HarmShields}), and only once
 * {@link BeaconsConfig#evilBeaconGraceMinutes} (default 5 min) have passed since the player's last death - so nobody
 * can be kept down by an Evil Beacon at a spawn point. A player that never died counts from the world's first tick, which the
 * newcomer protection covers anyway. Static and side free.
 */
public final class EvilGate {
    /** Ticks since the last death (Vanilla statistic {@code time_since_death}, reset in {@code ServerPlayer.die}). */
    public static int sinceRespawn(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
    }

    /** Whether an Evil beacon may give {@code player} its effects now. */
    public static boolean affects(Player player) {
        if (!(player instanceof ServerPlayer server) || player.isSpectator() || player.isCreative()) return false;
        if (HarmShields.shielded(player)) return false;
        return sinceRespawn(server) >= BeaconsConfig.evilBeaconGraceMinutes * 1200;
    }

    private EvilGate() {}
}
