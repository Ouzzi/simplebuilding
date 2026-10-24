package com.simplebuilding.modules.simpleinterfaces;

import com.simplebuilding.modules.simpleinterfaces.linked.InteractionGuard;
import com.simplebuilding.modules.simpleinterfaces.linked.LinkedContainers;
import com.simplebuilding.modules.simpleinterfaces.network.ConfigSyncPayload;
import com.simplebuilding.modules.simpleinterfaces.network.LinkedCancelPayload;
import com.simplebuilding.modules.simpleinterfaces.network.LinkedOpenPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Server side (formerly Simple QoL): linked containers, Easy Shulkers / Ender Chests and their config_sync. */
public final class InterfacesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleInterfaces.configDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        SimpleInterfaces.load();
        PayloadTypeRegistry.clientboundPlay().register(ConfigSyncPayload.TYPE, ConfigSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LinkedOpenPayload.TYPE, LinkedOpenPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(com.simplebuilding.modules.simpleinterfaces.network.EnderChestSnapshotPayload.TYPE,
                com.simplebuilding.modules.simpleinterfaces.network.EnderChestSnapshotPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LinkedCancelPayload.TYPE, LinkedCancelPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(LinkedCancelPayload.TYPE, (payload, c) -> c.server().execute(() -> LinkedContainers.onCancel(c.player())));
        LinkedContainers.openSync = (p, payload) -> {
            if (!ServerPlayNetworking.canSend(p, LinkedOpenPayload.TYPE)) return false;
            ServerPlayNetworking.send(p, payload);
            return true;
        };
        com.simplebuilding.modules.simpleinterfaces.tooltip.ContainerTooltips.sender = (p, payload) -> {
            if (!ServerPlayNetworking.canSend(p, payload.type())) return false;
            ServerPlayNetworking.send(p, payload);
            return true;
        };
        // The probe fires the real BEFORE handlers; area hammers must not take it for a break (HammerActions veto).
        com.simplelib.api.HammerActions.registerAreaVeto(() -> InteractionGuard.probing > 0);
        InteractionGuard.permission = (p, pos) -> {
            InteractionGuard.probing++;
            try {
                return net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.invoker()
                        .beforeBlockBreak(p.level(), p, pos, p.level().getBlockState(pos), p.level().getBlockEntity(pos));
            } finally {
                InteractionGuard.probing--;
            }
        };
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(s -> SimpleInterfaces.onChange = () -> s.getPlayerList().getPlayers().forEach(InterfacesFabric::sync));
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((h, sender, s) -> sync(h.player));
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((p, w, hand, hit) -> LinkedContainers.onRightClickBlock(p, hand, hit.getBlockPos()));
    }

    private static void sync(net.minecraft.server.level.ServerPlayer p) {
        if (ServerPlayNetworking.canSend(p, ConfigSyncPayload.TYPE)) ServerPlayNetworking.send(p, new ConfigSyncPayload(SimpleInterfaces.syncJson()));
        com.simplebuilding.modules.simpleinterfaces.tooltip.ContainerTooltips.send(p);
    }
}
