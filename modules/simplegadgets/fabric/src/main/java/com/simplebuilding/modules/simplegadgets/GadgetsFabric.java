package com.simplebuilding.modules.simplegadgets;

import com.simplegadgets.GadgetProtection;
import com.simplegadgets.GadgetsRegistry;
import com.simplegadgets.network.LaserPayload;
import com.simplegadgets.network.RodNetwork;
import com.simplegadgets.SimpleGadgets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class GadgetsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleGadgets.init(FabricLoader.getInstance().getConfigDir());
        GadgetsRegistry.registerItems();
        GadgetsRegistry.registerTab();
        // IG31-3 / QW-5: gadgets and guide stand in the Gadgets tab; CreativeTabPolicy keeps mod items out of Vanilla tabs.
        // The Resonance Rod's dot: both ways, relayed by the server (SimpleBuilding's payload moved here as simplegadgets:laser_pos).
        PayloadTypeRegistry.serverboundPlay().register(LaserPayload.ID, LaserPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LaserPayload.ID, LaserPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(LaserPayload.ID,
                (payload, context) -> context.server().execute(() -> RodNetwork.handleLaser(payload, context.player())));
        RodNetwork.setPlayerSender((player, payload) -> {
            if (ServerPlayNetworking.canSend(player, payload.type())) ServerPlayNetworking.send(player, payload);
        });
        // Claim and protection mods guard blocks through the loader's break event: fire it with the real player (Fabric has no place event).
        GadgetProtection.setBuildGuard(new GadgetProtection.BuildGuard() {
            @Override
            public boolean mayBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                boolean allowed = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, blockEntity);
                if (!allowed) PlayerBlockBreakEvents.CANCELED.invoker().onBlockBreakCanceled(level, player, pos, state, blockEntity);
                return allowed;
            }

            @Override
            public boolean mayPlace(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
                BlockState current = level.getBlockState(pos);
                return PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, current, level.getBlockEntity(pos));
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> com.simplegadgets.rotator.RotatorItem.onDisconnect(handler.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> com.simplegadgets.rotator.RotatorItem.clearAll());
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simplegadgets.guide.GadgetsGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
