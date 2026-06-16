package com.simplebuilding.neoforge.networking;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.networking.*;
import com.simplebuilding.platform.PlatformServices;
import com.simplebuilding.platform.PlayerPacketSender;
import com.simplebuilding.util.SurvivalTracerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Simplebuilding.MOD_ID)
public final class NeoForgeNetworkRegistration {
    private NeoForgeNetworkRegistration() {
    }

    public static void registerPlatformServices() {
        PlatformServices.setPlayerPacketSender(new PlayerPacketSender() {
            @Override
            public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
                return player.connection != null;
            }

            @Override
            public void send(ServerPlayer player, CustomPacketPayload payload) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        });
        PlatformServices.setHopperSync((blockEntity, slot, stack) -> {
            if (blockEntity.getLevel() instanceof ServerLevel serverLevel) {
                PacketDistributor.sendToPlayersTrackingChunk(
                        serverLevel,
                        new ChunkPos(blockEntity.getBlockPos().getX() >> 4, blockEntity.getBlockPos().getZ() >> 4),
                        new SyncHopperGhostItemPayload(blockEntity.getBlockPos(), slot, stack)
                );
            }
        });
    }

    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(ToggleHopperFilterPayload.ID, ToggleHopperFilterPayload.CODEC,
                (payload, context) -> runOnPlayer(context, player -> ModMessageHandlers.handleToggleHopperFilter(payload, player)));
        registrar.playToServer(SetHopperGhostItemPayload.ID, SetHopperGhostItemPayload.CODEC,
                (payload, context) -> runOnPlayer(context, player -> ModMessageHandlers.handleSetHopperGhostItem(payload, player)));
        registrar.playToServer(SpaceKeyPayload.ID, SpaceKeyPayload.CODEC,
                (payload, context) -> runOnPlayer(context, player -> ModMessageHandlers.handleSpaceKey(payload, player)));
        registrar.playToServer(DoubleJumpPayload.ID, DoubleJumpPayload.CODEC,
                (payload, context) -> runOnPlayer(context, player -> ModMessageHandlers.handleDoubleJump(payload, player)));
        registrar.playToServer(TrimBenefitPayload.ID, TrimBenefitPayload.CODEC,
                (payload, context) -> runOnPlayer(context, player -> ModMessageHandlers.handleTrimBenefit(payload, player)));
        registrar.playToServer(ReinforcedBundleSelectionPayload.ID, ReinforcedBundleSelectionPayload.CODEC,
                (payload, context) -> runOnPlayer(context, player -> ModMessageHandlers.handleReinforcedBundleSelection(payload, player)));
        registrar.playToServer(OctantConfigurePayload.ID, OctantConfigurePayload.CODEC,
                (payload, context) -> runOnPlayer(context, player -> ModMessageHandlers.handleOctantConfigure(payload, player)));
        registrar.playToServer(OctantScrollPayload.ID, OctantScrollPayload.CODEC,
                (payload, context) -> runOnPlayer(context, player -> ModMessageHandlers.handleOctantScroll(payload, player)));
        registrar.playToServer(BuildingWandConfigurePayload.ID, BuildingWandConfigurePayload.CODEC,
                (payload, context) -> runOnPlayer(context, player -> ModMessageHandlers.handleBuildingWandConfigure(payload, player)));
        registrar.playToServer(MasterBuilderPickPayload.ID, MasterBuilderPickPayload.CODEC,
                (payload, context) -> runOnPlayer(context, player -> ModMessageHandlers.handleMasterBuilderPick(payload, player)));

        registrar.playToClient(SyncHopperGhostItemPayload.ID, SyncHopperGhostItemPayload.CODEC, (payload, context) -> context.enqueueWork(() -> {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null && client.level.getBlockEntity(payload.pos()) instanceof ModHopperBlockEntity blockEntity) {
                blockEntity.setGhostItemClient(payload.slot(), payload.stack());
            }
        }));
        registrar.playToClient(TrimDataPayload.ID, TrimDataPayload.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof SurvivalTracerAccessor accessor) {
                accessor.simplebuilding$setBaseValues(payload.baseDist(), payload.baseTime(), payload.baseHostile(), payload.basePassive(), payload.baseDamage());
            }
        }));
        registrar.playToClient(SurvivalSyncPayload.ID, SurvivalSyncPayload.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof SurvivalTracerAccessor accessor) {
                accessor.simplebuilding$setCurrentValues(payload.currentDist(), payload.currentTime(), payload.currentHostile(), payload.currentPassive(), payload.currentDamage());
            }
        }));
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof SurvivalTracerAccessor accessor) {
            accessor.simplebuilding$syncTrimData();
        }
    }

    private static void runOnPlayer(IPayloadContext context, java.util.function.Consumer<ServerPlayer> action) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                action.accept(serverPlayer);
            }
        });
    }
}
