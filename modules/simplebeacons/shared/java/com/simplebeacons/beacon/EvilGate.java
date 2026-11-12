package com.simplebeacons.beacon;

import com.simplebeacons.config.BeaconsConfig;
import com.simplebuilding.framework.api.Protection;
import com.simplelib.api.HarmShields;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Who the Evil beacons reach (BS28b-5, balanced against griefing): no spectators, not protected by Simple
 * Tweaks' personal PvP switch or newcomer protection ({@link HarmShields}), and only once
 * {@link BeaconsConfig#evilBeaconGraceMinutes} (default 5 min) have passed since the player's last death - so nobody
 * can be kept down by an Evil Beacon at a spawn point. A player that never died counts from the world's first tick, which the
 * newcomer protection covers anyway. F7 (12.11.): and not in a claim the beacon's owner has no right in ({@link
 * BeaconsConfig#evilSparesForeignClaims}). Static and side free.
 */
public final class EvilGate {
    /** Ticks since the last death (Vanilla statistic {@code time_since_death}, reset in {@code ServerPlayer.die}). */
    public static int sinceRespawn(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
    }

    /** Whether an Evil beacon may give {@code player} its effects now (spectator, protection and grace rules; not the claims). */
    public static boolean affects(Player player) {
        if (!(player instanceof ServerPlayer server) || player.isSpectator()) return false;
        if (HarmShields.shielded(player)) return false;
        return sinceRespawn(server) >= BeaconsConfig.evilBeaconGraceMinutes * 1200;
    }

    /**
     * {@link #affects(Player)} and, while {@link BeaconsConfig#evilSparesForeignClaims} is on and a claims provider
     * is registered with the framework {@link Protection}, only where the beacon's {@code owner} (placer or upgrader) may act at
     * the player's position: free land and the claims of the owner and the owner's team yes, everybody else's claim no.
     * An unknown owner (blocks of older worlds, null) counts as nobody, so every claim is foreign to it.
     */
    public static boolean affects(Player player, @Nullable UUID owner) {
        if (!affects(player)) return false;
        if (!BeaconsConfig.evilSparesForeignClaims || !(player instanceof ServerPlayer server)) return true;
        var minecraft = server.level().getServer();
        if (!Protection.active(minecraft)) return true;
        BlockPos at = server.blockPosition();
        var target = new Protection.Target(owner, server.level().dimension().identifier().toString(), at.getX(), at.getY(), at.getZ(), false);
        return Protection.allows(minecraft, target, Protection.Action.ANY);
    }

    private EvilGate() {}
}
