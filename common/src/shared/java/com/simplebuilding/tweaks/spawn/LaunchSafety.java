package com.simplebuilding.tweaks.spawn;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fallschutz nach einem Start vom Enderit-Launchpad: gilt bis zur naechsten Landung (frueheste
 * Pruefung eine halbe Sekunde nach dem Start, damit der Start selbst nicht als Landung zaehlt).
 * Bewusst nicht gespeichert - ein Neustart mitten im Flug beendet den Schutz.
 */
public final class LaunchSafety {
    private static final int MIN_AIR_TICKS = 10;
    private static final Map<UUID, Long> LAUNCHED_AT = new ConcurrentHashMap<>();

    private LaunchSafety() {
    }

    public static void protect(ServerPlayer player) {
        LAUNCHED_AT.put(player.getUUID(), player.level().getGameTime());
    }

    public static boolean isProtected(LivingEntity entity) {
        return LAUNCHED_AT.containsKey(entity.getUUID());
    }

    /** Vom Fallschaden-Mixin: Schutz verbrauchen, Schaden verhindern. */
    public static boolean consumeOnFall(LivingEntity entity) {
        return LAUNCHED_AT.remove(entity.getUUID()) != null;
    }

    /** Jeden Server-Tick: wer gelandet ist (ohne Fallschaden, z. B. im Wasser), verliert den Schutz. */
    public static void serverTick(MinecraftServer server) {
        if (LAUNCHED_AT.isEmpty()) {
            return;
        }
        LAUNCHED_AT.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                return true;
            }
            long airTime = player.level().getGameTime() - entry.getValue();
            return airTime > MIN_AIR_TICKS && (player.onGround() || player.isInWater());
        });
    }
}
