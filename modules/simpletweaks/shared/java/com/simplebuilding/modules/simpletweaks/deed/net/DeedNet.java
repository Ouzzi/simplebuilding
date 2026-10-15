package com.simplebuilding.modules.simpletweaks.deed.net;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Loader-neutral hooks of the deed packets (the pattern of Simple Maps): each loader installs how a payload is sent;
 * the shared code only calls these. {@link #toClient} answers whether the payload was sent (the player's client has
 * the channel); without it the deed falls back to the direct behavior.
 */
public final class DeedNet {
    public static volatile BiPredicate<ServerPlayer,CustomPacketPayload> toClient = (player, payload) -> false;
    public static volatile Consumer<CustomPacketPayload> toServer = payload -> { };

    private DeedNet() {}

    public static boolean send(ServerPlayer player, CustomPacketPayload payload) {
        try { return toClient.test(player, payload); }
        catch (RuntimeException e) { return false; }
    }
}
