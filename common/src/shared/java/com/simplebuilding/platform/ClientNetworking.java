package com.simplebuilding.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.function.Consumer;

public final class ClientNetworking {
    private static Consumer<CustomPacketPayload> sender = payload -> {};

    private ClientNetworking() {
    }

    public static void setSender(Consumer<CustomPacketPayload> sender) {
        ClientNetworking.sender = sender != null ? sender : payload -> {};
    }

    public static void send(CustomPacketPayload payload) {
        sender.accept(payload);
    }
}
