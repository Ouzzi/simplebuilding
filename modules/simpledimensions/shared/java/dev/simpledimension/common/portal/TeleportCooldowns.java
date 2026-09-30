package dev.simpledimension.common.portal;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportCooldowns {
    private static final Map<UUID, Long> LAST_TELEPORT_MS = new ConcurrentHashMap<>();

    private TeleportCooldowns() {
    }

    public static boolean tryConsume(UUID playerId, long nowMs, long cooldownMs) {
        if (isOnCooldown(playerId, nowMs, cooldownMs)) {
            return false;
        }
        LAST_TELEPORT_MS.put(playerId, nowMs);
        return true;
    }

    public static boolean isOnCooldown(UUID playerId, long nowMs, long cooldownMs) {
        Long last = LAST_TELEPORT_MS.get(playerId);
        return last != null && (nowMs - last) < cooldownMs;
    }
}
