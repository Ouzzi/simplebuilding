package com.simplelib.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;

/**
 * Who is shielded from harm that other players' blocks cause (A29, BS28b-5: the Evil Beacon's negative effects). Modules
 * with a protection - Simple Tweaks' personal PvP switch and newcomer protection - register a predicate under their
 * module id; a harmful block asks {@link #shielded} and skips the player. Without any registered check nobody is
 * shielded. Static and side free.
 */
public final class HarmShields {
    private static final Map<String, Predicate<Player>> CHECKS = new ConcurrentHashMap<>();

    /** Registers (or replaces) the check of {@code moduleId}: true = this player is shielded. */
    public static void register(String moduleId, Predicate<Player> shielded) {
        CHECKS.put(moduleId, shielded);
    }

    public static void unregister(String moduleId) {
        CHECKS.remove(moduleId);
    }

    /** Whether any registered protection shields {@code player}. */
    public static boolean shielded(Player player) {
        for (Predicate<Player> check : CHECKS.values()) {
            if (check.test(player)) return true;
        }
        return false;
    }

    private HarmShields() {}
}
