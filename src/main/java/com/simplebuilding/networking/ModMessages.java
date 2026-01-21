package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.screen.ModHopperScreenHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;

public class ModMessages {

    public static void registerC2SPackets() {
        // --- Client -> Server Pakete ---
        PayloadTypeRegistry.playC2S().register(ToggleHopperFilterPayload.ID, ToggleHopperFilterPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SetHopperGhostItemPayload.ID, SetHopperGhostItemPayload.CODEC);

        // Receiver für Toggle
        ServerPlayNetworking.registerGlobalReceiver(ToggleHopperFilterPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player().currentScreenHandler instanceof ModHopperScreenHandler screenHandler) {
                    if (screenHandler.getBlockEntity() instanceof ModHopperBlockEntity blockEntity) {
                        blockEntity.toggleFilterMode();
                        Simplebuilding.LOGGER.info("SERVER: Filter Mode umgeschaltet.");
                    }
                }
            });
        });

        // Receiver für Ghost Item Setzen (Vom Client ausgelöst)
        ServerPlayNetworking.registerGlobalReceiver(SetHopperGhostItemPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player().currentScreenHandler instanceof ModHopperScreenHandler screenHandler) {
                    if (screenHandler.getBlockEntity() instanceof ModHopperBlockEntity blockEntity) {
                        blockEntity.setGhostItem(payload.slotIndex(), payload.stack());
                        Simplebuilding.LOGGER.info("SERVER: Ghost Item gesetzt in Slot " + payload.slotIndex() + ": " + payload.stack().getName().getString());
                    }
                }
            });
        });
    }

    public static void registerS2CPackets() {
        // --- Server -> Client Pakete ---
        // Hier registrieren wir das neue Sync Paket
        PayloadTypeRegistry.playS2C().register(SyncHopperGhostItemPayload.ID, SyncHopperGhostItemPayload.CODEC);
    }
}