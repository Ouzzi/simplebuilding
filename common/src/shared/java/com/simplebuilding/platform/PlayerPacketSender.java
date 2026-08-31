package com.simplebuilding.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public interface PlayerPacketSender {
    PlayerPacketSender NOOP = new PlayerPacketSender() {
        @Override
        public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
            return false;
        }

        @Override
        public void send(ServerPlayer player, CustomPacketPayload payload) {
        }
    };

    boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type);

    void send(ServerPlayer player, CustomPacketPayload payload);
}
