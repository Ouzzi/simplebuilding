package com.simplepads.advancement;

import com.simplepads.SimplePads;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The advancement trigger {@code simplepads:pad_used} (until 04.11. SimpleBuilding's {@code simplebuilding:feature_used} with the
 * same feature names): the advancements {@code simplepads:pads/*} wait for a pad to act on the player.
 */
public final class PadTriggers {
    public static final PadTrigger PAD_USED = new PadTrigger();
    public static final String LAUNCHPAD = "launchpad";
    public static final String FLYPAD = "flypad";
    public static final String ELYTRA_PAD = "elytra_pad";
    public static final String SPAWN_TELEPORT = "spawn_teleport";
    public static final String POTION_PAD = "potion_pad";

    private static boolean registered;

    private PadTriggers() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, SimplePads.id("pad_used"), PAD_USED);
    }

    /** Reports that {@code player} just used {@code feature}; a no-op for client players. */
    public static void feature(Player player, String feature) {
        if (player instanceof ServerPlayer serverPlayer) {
            PAD_USED.trigger(serverPlayer, feature);
        }
    }
}
