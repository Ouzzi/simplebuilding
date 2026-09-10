package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.util.SurvivalTracerAccessor;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import java.util.function.BiConsumer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class ModMessages {

    private static boolean registered = false;

    public static void registerC2SPackets() {
        if (registered) {
            Simplebuilding.LOGGER.info("ModMessages already registered, skipping.");
            return;
        }
        registered = true;

        // --- 1. REGISTRIERUNG DER PAYLOAD-TYPEN (Beide Seiten müssen diese kennen) ---

        // Client -> Server (C2S)
        PayloadTypeRegistry.serverboundPlay().register(ToggleHopperFilterPayload.ID, ToggleHopperFilterPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SetHopperGhostItemPayload.ID, SetHopperGhostItemPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SpaceKeyPayload.ID, SpaceKeyPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DoubleJumpPayload.ID, DoubleJumpPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TrimBenefitPayload.ID, TrimBenefitPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ReinforcedBundleSelectionPayload.ID, ReinforcedBundleSelectionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OctantConfigurePayload.ID, OctantConfigurePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OctantScrollPayload.ID, OctantScrollPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BuildingWandConfigurePayload.ID, BuildingWandConfigurePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MasterBuilderPickPayload.ID, MasterBuilderPickPayload.CODEC);


        // Server -> Client (S2C)
        PayloadTypeRegistry.clientboundPlay().register(SyncHopperGhostItemPayload.ID, SyncHopperGhostItemPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TrimDataPayload.ID, TrimDataPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SurvivalSyncPayload.ID, SurvivalSyncPayload.CODEC);

        // --- 2. SERVER-RECEIVER ---
        // Every receiver hands its payload to the SHARED handler in ModMessageHandlers, the
        // same class the NeoForge module delegates to and the server gametests call directly.
        // This file used to carry a second, hand copied body for every one of them; the two
        // copies were identical when they were compared on 2026-09-10, and identical copies are
        // exactly the state that stops being true silently. It also meant the server tests on
        // ModMessageHandlers said nothing about what a Fabric server actually ran. Now they do.
        receive(DoubleJumpPayload.ID, ModMessageHandlers::handleDoubleJump);
        receive(ToggleHopperFilterPayload.ID, ModMessageHandlers::handleToggleHopperFilter);
        receive(SetHopperGhostItemPayload.ID, ModMessageHandlers::handleSetHopperGhostItem);
        receive(SpaceKeyPayload.ID, ModMessageHandlers::handleSpaceKey);
        receive(TrimBenefitPayload.ID, ModMessageHandlers::handleTrimBenefit);
        receive(ReinforcedBundleSelectionPayload.ID, ModMessageHandlers::handleReinforcedBundleSelection);
        receive(OctantConfigurePayload.ID, ModMessageHandlers::handleOctantConfigure);
        receive(OctantScrollPayload.ID, ModMessageHandlers::handleOctantScroll);
        receive(BuildingWandConfigurePayload.ID, ModMessageHandlers::handleBuildingWandConfigure);
        receive(MasterBuilderPickPayload.ID, ModMessageHandlers::handleMasterBuilderPick);

        // Events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (handler.player instanceof SurvivalTracerAccessor accessor) {
                accessor.simplebuilding$syncTrimData();
            }
        });
    }

    /**
     * Registers one serverbound payload and runs its shared handler on the server thread.
     *
     * <p>{@code context.server().execute} is the part every copy had in common and the part
     * that matters: the receiver is called on the network thread, the handlers touch the
     * player's inventory and items, and those belong to the server thread.
     */
    private static <T extends CustomPacketPayload> void receive(CustomPacketPayload.Type<T> type,
                                                                 BiConsumer<T, ServerPlayer> handler) {
        ServerPlayNetworking.registerGlobalReceiver(type,
                (payload, context) -> context.server().execute(() -> handler.accept(payload, context.player())));
    }
}
