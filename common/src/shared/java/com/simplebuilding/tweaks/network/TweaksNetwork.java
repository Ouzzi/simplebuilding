package com.simplebuilding.tweaks.network;

import com.simplebuilding.platform.PlatformServices;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.component.TweaksComponents;
import com.simplebuilding.tweaks.item.TweaksItems;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
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

    public record LaserDot(float x, float y, float z, long timestamp) {
    }

    private TweaksNetwork() {
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
        int maxBoosts = Math.max(1, SimpleTweaks.config().spawn.maxBoosts);
        float cost = 1.0f / maxBoosts;
        if (boost < cost - 0.001f) {
            return;
        }
        float strength = SimpleTweaks.config().spawn.boostStrength;
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(player.getDeltaMovement().add(look.x * strength, look.y * strength, look.z * strength));
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.3f, 1.0f);
        float newLevel = boost - cost;
        chest.set(TweaksComponents.BOOST_LEVEL, newLevel < 0.001f ? 0.0f : newLevel);
    }

    /** Verteilt den Laserpunkt an alle anderen Spieler derselben Welt. */
    public static void handleLaser(LaserPayload payload, ServerPlayer sender) {
        LaserPayload checked = new LaserPayload(sender.getUUID(), payload.x(), payload.y(), payload.z(), payload.active());
        ServerLevel level = sender.level();
        for (ServerPlayer player : level.players()) {
            if (player != sender && PlatformServices.canSendToPlayer(player, LaserPayload.ID)) {
                PlatformServices.sendToPlayer(player, checked);
            }
        }
    }

    /** Clientseitig: Laserpunkt eines anderen Spielers merken. */
    public static void receiveLaser(LaserPayload payload) {
        ACTIVE_LASERS.put(payload.player(), new LaserDot(payload.x(), payload.y(), payload.z(), System.currentTimeMillis()));
    }

    public static void expireLasers() {
        long now = System.currentTimeMillis();
        ACTIVE_LASERS.entrySet().removeIf(e -> now - e.getValue().timestamp() > 200);
    }
}
