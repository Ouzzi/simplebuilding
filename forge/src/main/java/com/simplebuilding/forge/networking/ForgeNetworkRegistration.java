package com.simplebuilding.forge.networking;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.networking.*;
import com.simplebuilding.platform.PlatformServices;
import com.simplebuilding.platform.PlayerPacketSender;
import com.simplebuilding.util.SurvivalTracerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkProtocol;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Consumer;

/**
 * Forge port of the networking layer. Builds one PayloadChannel carrying every
 * shared payload and wires the loader-agnostic PlatformServices / ClientNetworking.
 */
public final class ForgeNetworkRegistration {
    private static Channel<CustomPacketPayload> channel;

    private ForgeNetworkRegistration() {
    }

    public static void register() {
        channel = ChannelBuilder.named(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "main"))
                .payloadChannel()
                .protocol(NetworkProtocol.PLAY)
                .serverbound()
                    .add(ToggleHopperFilterPayload.ID, ToggleHopperFilterPayload.CODEC,
                            (payload, ctx) -> runOnPlayer(ctx, player -> ModMessageHandlers.handleToggleHopperFilter(payload, player)))
                    .add(SetHopperGhostItemPayload.ID, SetHopperGhostItemPayload.CODEC,
                            (payload, ctx) -> runOnPlayer(ctx, player -> ModMessageHandlers.handleSetHopperGhostItem(payload, player)))
                    .add(SpaceKeyPayload.ID, SpaceKeyPayload.CODEC,
                            (payload, ctx) -> runOnPlayer(ctx, player -> ModMessageHandlers.handleSpaceKey(payload, player)))
                    .add(DoubleJumpPayload.ID, DoubleJumpPayload.CODEC,
                            (payload, ctx) -> runOnPlayer(ctx, player -> ModMessageHandlers.handleDoubleJump(payload, player)))
                    .add(TrimBenefitPayload.ID, TrimBenefitPayload.CODEC,
                            (payload, ctx) -> runOnPlayer(ctx, player -> ModMessageHandlers.handleTrimBenefit(payload, player)))
                    .add(ReinforcedBundleSelectionPayload.ID, ReinforcedBundleSelectionPayload.CODEC,
                            (payload, ctx) -> runOnPlayer(ctx, player -> ModMessageHandlers.handleReinforcedBundleSelection(payload, player)))
                    .add(OctantConfigurePayload.ID, OctantConfigurePayload.CODEC,
                            (payload, ctx) -> runOnPlayer(ctx, player -> ModMessageHandlers.handleOctantConfigure(payload, player)))
                    .add(OctantScrollPayload.ID, OctantScrollPayload.CODEC,
                            (payload, ctx) -> runOnPlayer(ctx, player -> ModMessageHandlers.handleOctantScroll(payload, player)))
                    .add(BuildingWandConfigurePayload.ID, BuildingWandConfigurePayload.CODEC,
                            (payload, ctx) -> runOnPlayer(ctx, player -> ModMessageHandlers.handleBuildingWandConfigure(payload, player)))
                    .add(MasterBuilderPickPayload.ID, MasterBuilderPickPayload.CODEC,
                            (payload, ctx) -> runOnPlayer(ctx, player -> ModMessageHandlers.handleMasterBuilderPick(payload, player)))
                .clientbound()
                    .add(SyncHopperGhostItemPayload.ID, SyncHopperGhostItemPayload.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
                        Minecraft client = Minecraft.getInstance();
                        if (client.level != null && client.level.getBlockEntity(payload.pos()) instanceof ModHopperBlockEntity blockEntity) {
                            blockEntity.setGhostItemClient(payload.slot(), payload.stack());
                        }
                    }))
                    .add(TrimDataPayload.ID, TrimDataPayload.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
                        if (Minecraft.getInstance().player instanceof SurvivalTracerAccessor accessor) {
                            accessor.simplebuilding$setBaseValues(payload.baseDist(), payload.baseTime(), payload.baseHostile(), payload.basePassive(), payload.baseDamage());
                        }
                    }))
                    .add(SurvivalSyncPayload.ID, SurvivalSyncPayload.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
                        if (Minecraft.getInstance().player instanceof SurvivalTracerAccessor accessor) {
                            accessor.simplebuilding$setCurrentValues(payload.currentDist(), payload.currentTime(), payload.currentHostile(), payload.currentPassive(), payload.currentDamage());
                        }
                    }))
                .build();

        registerPlatformServices();
    }

    public static void sendToServer(CustomPacketPayload payload) {
        channel.send(payload, PacketDistributor.SERVER.noArg());
    }

    private static void registerPlatformServices() {
        PlatformServices.setPlayerPacketSender(new PlayerPacketSender() {
            @Override
            public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
                return player.connection != null;
            }

            @Override
            public void send(ServerPlayer player, CustomPacketPayload payload) {
                channel.send(payload, PacketDistributor.PLAYER.with(player));
            }
        });
        PlatformServices.setHopperSync((blockEntity, slot, stack) -> {
            if (blockEntity.getLevel() instanceof ServerLevel serverLevel) {
                channel.send(
                        new SyncHopperGhostItemPayload(blockEntity.getBlockPos(), slot, stack),
                        PacketDistributor.TRACKING_CHUNK.with(serverLevel.getChunkAt(blockEntity.getBlockPos()))
                );
            }
        });
    }

    private static void runOnPlayer(CustomPayloadEvent.Context context, Consumer<ServerPlayer> action) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                action.accept(player);
            }
        });
    }
}
