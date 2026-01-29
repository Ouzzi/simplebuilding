package com.simplebuilding.networking;

import com.simplebuilding.util.SurvivalTracerAccessor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.screen.ModHopperScreenHandler;

public class ModMessages {

    public static void registerC2SPackets() {
        // 1. Registrierung der Typen (Muss vor dem Senden/Empfangen passieren)
        // Client -> Server
        PayloadTypeRegistry.playC2S().register(ToggleHopperFilterPayload.ID, ToggleHopperFilterPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SetHopperGhostItemPayload.ID, SetHopperGhostItemPayload.CODEC);

        // Server -> Client (WICHTIG!)
        PayloadTypeRegistry.playS2C().register(SyncHopperGhostItemPayload.ID, SyncHopperGhostItemPayload.CODEC);

        // 2. Receiver registrieren (Nur für C2S auf dem Server)
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


        // Base Data
        PayloadTypeRegistry.playS2C().register(TrimDataPayload.ID, TrimDataPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(TrimDataPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.player() instanceof SurvivalTracerAccessor accessor) {
                    accessor.simplebuilding$setBaseValues(
                            payload.baseDist(), payload.baseTime(),
                            payload.baseHostile(), payload.basePassive(), payload.baseDamage()
                    );
                }
            });
        });

        // Live Data
        PayloadTypeRegistry.playS2C().register(SurvivalSyncPayload.ID, SurvivalSyncPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(SurvivalSyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.player() instanceof SurvivalTracerAccessor accessor) {
                    accessor.simplebuilding$setCurrentValues(
                            payload.currentDist(), payload.currentTime(),
                            payload.currentHostile(), payload.currentPassive(), payload.currentDamage()
                    );
                }
            });
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (handler.player instanceof SurvivalTracerAccessor accessor) {
                accessor.simplebuilding$syncTrimData();
            }
        });
    }
}