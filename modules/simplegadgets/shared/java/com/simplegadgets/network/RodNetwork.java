package com.simplegadgets.network;

import com.simplegadgets.config.GadgetsConfig;
import com.simplegadgets.rod.ResonanceRodItem;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** The Resonance Rod's dot relay (moved from SimpleBuilding's {@code TweaksNetwork}); the loader entry calls it on the server thread. */
public final class RodNetwork {

    /** Client side: the last reported laser dots of other players (expire after 200 ms). */
    public static final Map<UUID, LaserDot> ACTIVE_LASERS = new ConcurrentHashMap<>();

    /** At most this many laser dots per player and second; the client sends 10 (every 2nd tick). */
    public static final int LASER_PACKETS_PER_SECOND = 12;
    /** Only players within this distance of the pointer or of the dot get the dot. */
    public static final double LASER_RELAY_RANGE = 128.0;
    /** Margin above the configured range (eye vs. view point, movement between packets). */
    public static final double LASER_RANGE_SLACK = 8.0;

    /** Server: count window per player, {tick the window began, packets in the window}. */
    private static final Map<ServerPlayer, long[]> LASER_WINDOWS = new WeakHashMap<>();

    public record LaserDot(float x, float y, float z, long timestamp) {
    }

    @FunctionalInterface
    public interface PlayerSender {
        void send(ServerPlayer player, CustomPacketPayload payload);
    }

    private static PlayerSender toPlayer = (player, payload) -> {
    };
    private static java.util.function.Consumer<CustomPacketPayload> toServer = payload -> {
    };

    private RodNetwork() {
    }

    /** How packets leave the client (the Fabric client entry sets ClientPlayNetworking); a single-player client shares the JVM with the server. */
    public static void setServerSender(java.util.function.Consumer<CustomPacketPayload> serverSender) {
        toServer = serverSender;
    }

    /** The current way to a player (game tests swap it for a recording briefly). */
    public static PlayerSender playerSender() {
        return toPlayer;
    }

    public static void setPlayerSender(PlayerSender playerSender) {
        toPlayer = playerSender;
    }

    public static void sendToServer(CustomPacketPayload payload) {
        toServer.accept(payload);
    }

    /** Hands the laser dot on to the players nearby (see {@link #relayLaser}). */
    public static void handleLaser(LaserPayload payload, ServerPlayer sender) {
        relayLaser(payload, sender, toPlayer);
    }

    /**
     * Checks a reported laser dot and hands it on (audit 2026-09-26 #17 - before, every packet went unchecked to the whole
     * dimension): only with the laser switched on (server config), only while the player uses a rod, at most
     * {@link #LASER_PACKETS_PER_SECOND} per second, only within the laser range, and only to players within
     * {@link #LASER_RELAY_RANGE} of the pointer or the dot. The UUID in the packet is always replaced by the sender's.
     *
     * @return how many players got the dot
     */
    public static int relayLaser(LaserPayload payload, ServerPlayer sender, PlayerSender out) {
        if (!GadgetsConfig.rodEnabled || !sender.isUsingItem() || !(sender.getUseItem().getItem() instanceof ResonanceRodItem)) {
            return 0;
        }
        if (!Float.isFinite(payload.x()) || !Float.isFinite(payload.y()) || !Float.isFinite(payload.z())) {
            return 0;
        }
        Vec3 dot = new Vec3(payload.x(), payload.y(), payload.z());
        double maxRange = GadgetsConfig.rodRange + LASER_RANGE_SLACK;
        if (sender.getEyePosition().distanceToSqr(dot) > maxRange * maxRange) {
            return 0;
        }
        if (!withinRate(sender)) {
            return 0;
        }
        LaserPayload checked = new LaserPayload(sender.getUUID(), payload.x(), payload.y(), payload.z(), payload.active());
        ServerLevel level = sender.level();
        double range2 = LASER_RELAY_RANGE * LASER_RELAY_RANGE;
        int sent = 0;
        for (ServerPlayer player : level.players()) {
            if (player != sender && (player.distanceToSqr(sender) <= range2 || player.distanceToSqr(dot) <= range2)) {
                out.send(player, checked);
                sent++;
            }
        }
        return sent;
    }

    private static boolean withinRate(ServerPlayer sender) {
        long now = sender.level().getServer().getTickCount();
        long[] window = LASER_WINDOWS.computeIfAbsent(sender, p -> new long[] {now, 0});
        if (now - window[0] >= 20 || now < window[0]) {
            window[0] = now;
            window[1] = 0;
        }
        if (window[1] >= LASER_PACKETS_PER_SECOND) {
            return false;
        }
        window[1]++;
        return true;
    }

    /** Client side: remember the laser dot of another player. */
    public static void receiveLaser(LaserPayload payload) {
        ACTIVE_LASERS.put(payload.player(), new LaserDot(payload.x(), payload.y(), payload.z(), System.currentTimeMillis()));
    }

    public static void expireLasers() {
        long now = System.currentTimeMillis();
        ACTIVE_LASERS.entrySet().removeIf(e -> now - e.getValue().timestamp() > 200);
    }
}
