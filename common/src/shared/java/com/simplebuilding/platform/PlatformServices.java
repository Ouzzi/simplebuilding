package com.simplebuilding.platform;

import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class PlatformServices {
    private static HopperSync hopperSync = HopperSync.NOOP;
    private static PlayerPacketSender playerPacketSender = PlayerPacketSender.NOOP;

    private PlatformServices() {
    }

    public static void setHopperSync(HopperSync hopperSync) {
        PlatformServices.hopperSync = hopperSync != null ? hopperSync : HopperSync.NOOP;
    }

    public static void setPlayerPacketSender(PlayerPacketSender playerPacketSender) {
        PlatformServices.playerPacketSender = playerPacketSender != null ? playerPacketSender : PlayerPacketSender.NOOP;
    }

    public static void broadcastHopperGhostItem(ModHopperBlockEntity blockEntity, int slot, ItemStack stack) {
        hopperSync.broadcastGhostItem(blockEntity, slot, stack);
    }

    public static boolean canSendToPlayer(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return playerPacketSender.canSend(player, type);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        playerPacketSender.send(player, payload);
    }
}
