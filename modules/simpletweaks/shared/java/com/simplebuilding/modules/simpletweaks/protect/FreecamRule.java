package com.simplebuilding.modules.simpletweaks.protect;

import com.simplebuilding.modules.simpletweaks.deed.net.DeedNet;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Freecam rule (default off): Freecam (MIT, client only, no server protocol) may still fly, but not through blocks. The
 * server tells clients with Simple Tweaks to force Freecam's collision on ({@link ProtectRulesPayload}); with
 * {@code requireClient} a client that cannot receive the rule is disconnected. A modified client can ignore the rule –
 * the ores themselves are protected server-side by {@link AntiXray}.
 */
public final class FreecamRule {
    /** Tests: the last kick reason, instead of disconnecting a mock connection. */
    public static volatile java.util.function.BiConsumer<ServerPlayer, Component> kick = (p, reason) -> p.connection.disconnect(reason);

    static void joined(ServerPlayer player) { apply(player); }

    /** Sends the current rule; kicks when required and the client lacks Simple Tweaks. Returns whether the client got it. */
    public static boolean apply(ServerPlayer player) {
        var c = Protect.config().freecam();
        if (!c.enabled()) { if (player.connection != null) DeedNet.send(player, new ProtectRulesPayload(false)); return false; }
        boolean got = player.connection != null && DeedNet.send(player, new ProtectRulesPayload(true));
        if (!got && c.requireClient()) kick.accept(player, kickReason(c));
        return got;
    }
    static Component kickReason(ProtectConfig.Freecam c) {
        var text = c.kickMessage().isEmpty() ? ProtectConfig.DEFAULT.freecam().kickMessage() : c.kickMessage();
        return Component.literal(text); // literal: the kicked client has no Simple Tweaks and thus no translations
    }
    /** After /tweaks protect reload: every online player gets the new rule. */
    public static void broadcast(net.minecraft.server.MinecraftServer server) {
        for (var p : java.util.List.copyOf(server.getPlayerList().getPlayers())) apply(p);
    }
    private FreecamRule() {}
}
