package com.simplebuilding.tweaks.network;

import com.simplebuilding.platform.ClientNetworking;
import com.simplebuilding.platform.PlatformServices;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.component.TweaksComponents;
import com.simplebuilding.tweaks.item.LaserPointerItem;
import com.simplebuilding.tweaks.item.TweaksItems;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Gemeinsame Handler der Simple-Tweaks-Pakete; die Loader rufen sie auf dem Server-Thread auf. */
public final class TweaksNetwork {

    /** Clientseitig: die zuletzt gemeldeten Laserpunkte anderer Spieler (verfallen nach 200 ms). */
    public static final Map<UUID, LaserDot> ACTIVE_LASERS = new ConcurrentHashMap<>();

    /** Hoechstens so viele Laserpunkte je Spieler und Sekunde; der Client schickt 10 (jeden 2. Tick). */
    public static final int LASER_PACKETS_PER_SECOND = 12;
    /** Nur Spieler in diesem Abstand zum Zeigenden oder zum Punkt bekommen den Laserpunkt. */
    public static final double LASER_RELAY_RANGE = 128.0;
    /** Spielraum ueber der Config-Reichweite (Blickpunkt vs. Augenhoehe, Bewegung zwischen Paketen). */
    public static final double LASER_RANGE_SLACK = 8.0;

    /** Server: Zaehlfenster je Spieler, {tick des Fensterbeginns, Pakete im Fenster}. */
    private static final Map<ServerPlayer, long[]> LASER_WINDOWS = new WeakHashMap<>();
    public static final int BOOST_WINDOW_TICKS = 100;
    public static final int BOOSTS_PER_WINDOW = 3;
    public static final double MAX_BOOST_VELOCITY = 3.0;
    private static final Map<ServerPlayer, long[]> BOOST_WINDOWS = new WeakHashMap<>();

    public record LaserDot(float x, float y, float z, long timestamp) {
    }

    /**
     * Wie Pakete dieses Teils verschickt werden. Standard: die gemeinsamen Wege der Mod
     * (PlatformServices/ClientNetworking); Forge setzt eigene, weil sein Kanal "simplebuilding:main"
     * nur die aelteren Pakete kennt.
     */
    private static PlayerSender toPlayer = (player, payload) -> {
        if (PlatformServices.canSendToPlayer(player, payload.type())) {
            PlatformServices.sendToPlayer(player, payload);
        }
    };
    private static java.util.function.Consumer<CustomPacketPayload> toServer = ClientNetworking::send;

    @FunctionalInterface
    public interface PlayerSender {
        void send(ServerPlayer player, CustomPacketPayload payload);
    }

    private TweaksNetwork() {
    }

    public static void setSenders(PlayerSender playerSender, java.util.function.Consumer<CustomPacketPayload> serverSender) {
        toPlayer = playerSender;
        toServer = serverSender;
    }

    /** Der aktuelle Weg zum Spieler (Spieltests tauschen ihn kurz gegen einen Mitschnitt). */
    public static PlayerSender playerSender() {
        return toPlayer;
    }

    public static void setPlayerSender(PlayerSender playerSender) {
        toPlayer = playerSender;
    }

    public static void sendToServer(CustomPacketPayload payload) {
        toServer.accept(payload);
    }

    /**
     * Boost mit der Spawn-Elytra (Simple Tweaks: SpawnElytraNetworking). Kostet 1/maxBoosts der
     * Leiste, mit Toleranz gegen Rundungsfehler.
     */
    public static void handleBoost(ElytraBoostPayload payload, ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        // Der Client schickt nur im Gleitflug; der Server prueft es jetzt auch (Simple Tweaks nicht).
        if (!chest.is(TweaksItems.SPAWN_ELYTRA) || !player.isFallFlying()) {
            return;
        }
        Float boost = chest.get(TweaksComponents.BOOST_LEVEL);
        if (boost == null) {
            boost = 0.0f;
        }
        int maxBoosts = SimpleTweaks.config().spawn.boostCount();
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
        double strength = com.simplebuilding.tweaks.TweaksConfig.capped(
                SimpleTweaks.config().spawn.boostStrength, 0.1,
                com.simplebuilding.tweaks.TweaksConfig.MAX_BOOST_STRENGTH, 0.6);
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
        chest.set(TweaksComponents.BOOST_LEVEL, newLevel < 0.001f ? 0.0f : newLevel);
    }

    /** Verteilt den Laserpunkt an die Spieler in der Naehe (siehe {@link #relayLaser}). */
    public static void handleLaser(LaserPayload payload, ServerPlayer sender) {
        relayLaser(payload, sender, toPlayer);
    }

    /**
     * Prueft einen gemeldeten Laserpunkt und verteilt ihn (Audit 2026-09-26 #17 - vorher ging jedes
     * Paket ungeprueft an die ganze Dimension): nur mit eingeschaltetem Laser (Server-Config), nur
     * waehrend der Spieler einen Laserpointer benutzt, hoechstens {@link #LASER_PACKETS_PER_SECOND}
     * je Sekunde, nur innerhalb der Laser-Reichweite, und nur an Spieler in
     * {@link #LASER_RELAY_RANGE} um den Zeigenden oder den Punkt. Die UUID im Paket wird immer durch
     * die des Absenders ersetzt.
     *
     * @return wie viele Spieler den Punkt bekamen
     */
    public static int relayLaser(LaserPayload payload, ServerPlayer sender, PlayerSender out) {
        if (!SimpleTweaks.config().laserPointer.enable
                || !sender.isUsingItem() || !(sender.getUseItem().getItem() instanceof LaserPointerItem)) {
            return 0;
        }
        if (!Float.isFinite(payload.x()) || !Float.isFinite(payload.y()) || !Float.isFinite(payload.z())) {
            return 0;
        }
        Vec3 dot = new Vec3(payload.x(), payload.y(), payload.z());
        double maxRange = com.simplebuilding.tweaks.TweaksConfig.capped(SimpleTweaks.config().laserPointer.range,
                1, com.simplebuilding.tweaks.TweaksConfig.MAX_LASER_RANGE, 512) + LASER_RANGE_SLACK;
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

    /** Clientseitig: Laserpunkt eines anderen Spielers merken. */
    public static void receiveLaser(LaserPayload payload) {
        ACTIVE_LASERS.put(payload.player(), new LaserDot(payload.x(), payload.y(), payload.z(), System.currentTimeMillis()));
    }

    public static void expireLasers() {
        long now = System.currentTimeMillis();
        ACTIVE_LASERS.entrySet().removeIf(e -> now - e.getValue().timestamp() > 200);
    }

    // ---- Config-Abgleich (Audit #16) ----

    /** Schickt einem Spieler die clientrelevanten Config-Werte (beim Einloggen). */
    public static void sendConfig(ServerPlayer player) {
        toPlayer.send(player, TweaksConfigPayload.of(SimpleTweaks.localValues()));
    }

    /** Schickt allen Spielern die clientrelevanten Config-Werte (nach einem Tweaks-Befehl). */
    public static void broadcastConfig(MinecraftServer server) {
        TweaksConfigPayload payload = TweaksConfigPayload.of(SimpleTweaks.localValues());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            toPlayer.send(player, payload);
        }
    }

    /** Clientseitig: Werte des Servers uebernehmen. */
    public static void receiveConfig(TweaksConfigPayload payload) {
        SimpleTweaks.setServerValues(payload.values());
        com.simplebuilding.config.ServerTuning.checkStartupValues(payload.serverTuning());
    }
}
