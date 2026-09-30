package dev.simpledimension.common.portal;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeleportCooldownsTest {
    @Test
    void secondTeleportIsBlockedInsideCooldownWindow() {
        UUID playerId = UUID.randomUUID();
        var clock = new TeleportCooldowns();
        long now = 1_000L;

        assertTrue(clock.tryConsume(playerId, now, 1_500L));
        assertFalse(clock.tryConsume(playerId, now + 700L, 1_500L));
    }

    @Test
    void teleportIsAllowedAgainAfterCooldownWindow() {
        UUID playerId = UUID.randomUUID();
        var clock = new TeleportCooldowns();
        long now = 2_000L;

        assertTrue(clock.tryConsume(playerId, now, 1_500L));
        assertTrue(clock.tryConsume(playerId, now + 1_600L, 1_500L));
    }
}
