package com.simplepads.network;

import com.simplepads.PadsConfig;
import com.simplepads.SimplePads;
import com.simplepads.component.PadsComponents;
import com.simplepads.item.PadsItems;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Network of Simple Pads (until 04.11. SimpleBuilding's {@code TweaksNetwork}): the Spawn Elytra boost (client to server) and the
 * server's pad config (server to client, {@link PadsConfigPayload}: boost count for the HUD, stranger break times). The loader sets
 * the senders.
 */
public final class PadsNetwork {
    public static final int BOOST_WINDOW_TICKS = 100;
    public static final int BOOSTS_PER_WINDOW = 3;
    public static final double MAX_BOOST_VELOCITY = 3.0;
    private static final Map<ServerPlayer, long[]> BOOST_WINDOWS = new WeakHashMap<>();

    private static BiConsumer<ServerPlayer, CustomPacketPayload> toPlayer = (player, payload) -> {};
    private static Consumer<CustomPacketPayload> toServer = payload -> {};

    private PadsNetwork() {
    }

    public static void setSenders(BiConsumer<ServerPlayer, CustomPacketPayload> playerSender, Consumer<CustomPacketPayload> serverSender) {
        toPlayer = playerSender;
        toServer = serverSender;
    }

    public static void sendToServer(CustomPacketPayload payload) {
        toServer.accept(payload);
    }

    /** The server's config to one player (join, after {@code /simplepads config}). */
    public static void sendConfig(ServerPlayer player) {
        toPlayer.accept(player, new PadsConfigPayload(SimplePads.localConfigJson()));
    }

    /** The server's config to every player. */
    public static void sendConfigToAll(net.minecraft.server.MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) sendConfig(player);
    }

    /**
     * Boost mit der Spawn-Elytra (Simple Tweaks: SpawnElytraNetworking). Kostet 1/maxBoosts der
     * Leiste, mit Toleranz gegen Rundungsfehler.
     */
    public static void handleBoost(ElytraBoostPayload payload, ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        // Der Client schickt nur im Gleitflug; der Server prueft es jetzt auch (Simple Tweaks nicht).
        if (!chest.is(PadsItems.SPAWN_ELYTRA) || !player.isFallFlying()) {
            return;
        }
        Float boost = chest.get(PadsComponents.BOOST_LEVEL);
        if (boost == null) {
            boost = 0.0f;
        }
        int maxBoosts = SimplePads.config().spawn.boostCount();
        float cost = 1.0f / maxBoosts;
        if (!Float.isFinite(boost) || boost < cost - 0.001f) {
            return;
        }
        long now = player.level().getServer().getTickCount();
        long[] window = BOOST_WINDOWS.computeIfAbsent(player, p -> new long[] {now, 0});
        if (now - window[0] >= BOOST_WINDOW_TICKS || now < window[0]) {
            window[0] = now;
            window[1] = 0;
        }
        if (window[1] >= BOOSTS_PER_WINDOW) return;
        window[1]++;
        double strength = PadsConfig.capped(SimplePads.config().spawn.boostStrength, 0.1, PadsConfig.MAX_BOOST_STRENGTH, 0.6);
        Vec3 look = player.getLookAngle();
        Vec3 velocity = player.getDeltaMovement().add(look.scale(strength));
        if (!Double.isFinite(velocity.x) || !Double.isFinite(velocity.y) || !Double.isFinite(velocity.z)) {
            velocity = Vec3.ZERO;
        } else if (velocity.length() > MAX_BOOST_VELOCITY) {
            velocity = velocity.normalize().scale(MAX_BOOST_VELOCITY);
        }
        player.setDeltaMovement(velocity);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.3f, 1.0f);
        float newLevel = boost - cost;
        chest.set(PadsComponents.BOOST_LEVEL, newLevel < 0.001f ? 0.0f : newLevel);
    }
}
