package com.simplebuilding.modules.simpleinterfaces;

import com.simplebuilding.modules.simpleinterfaces.linked.InteractionGuard;
import com.simplebuilding.modules.simpleinterfaces.linked.LinkedContainers;
import com.simplebuilding.modules.simpleinterfaces.network.ConfigSyncPayload;
import com.simplebuilding.modules.simpleinterfaces.network.LinkedOpenPayload;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge entry: the GameTests and the server side of the linked containers / Easy Shulkers (formerly Simple QoL);
 * the style itself is client mixins plus a lazily loaded config.
 */
@Mod("simpleinterfaces")
public final class ContainersNeoForge {
    public ContainersNeoForge(net.neoforged.bus.api.IEventBus bus) {
        ModuleNeoTests.register(bus);
        SimpleInterfaces.configDir = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        SimpleInterfaces.load();
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e) -> e.registrar("1").optional().playToClient(ConfigSyncPayload.TYPE,
                ConfigSyncPayload.STREAM_CODEC, (p, c) -> c.enqueueWork(() -> SimpleInterfaces.receiveSync(p.json()))));
        // Main-thread handler (NeoForge default) in packet order: the slots must exist before the menu's first content packet.
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e) -> e.registrar("1").optional().playToClient(LinkedOpenPayload.TYPE,
                LinkedOpenPayload.CODEC, (p, c) -> com.simplebuilding.modules.simpleinterfaces.client.LinkedPanel.receive(p)));
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e) -> e.registrar("1").optional().playToServer(
                com.simplebuilding.modules.simpleinterfaces.network.LinkedCancelPayload.TYPE, com.simplebuilding.modules.simpleinterfaces.network.LinkedCancelPayload.CODEC,
                (p, c) -> c.enqueueWork(() -> LinkedContainers.onCancel((net.minecraft.server.level.ServerPlayer) c.player()))));
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e) -> e.registrar("1").optional().playToClient(
                com.simplebuilding.modules.simpleinterfaces.network.EnderChestSnapshotPayload.TYPE, com.simplebuilding.modules.simpleinterfaces.network.EnderChestSnapshotPayload.CODEC,
                (p, c) -> c.enqueueWork(() -> com.simplebuilding.modules.simpleinterfaces.tooltip.ContainerTooltips.receive(p))));
        com.simplebuilding.modules.simpleinterfaces.tooltip.ContainerTooltips.sender = (p, payload) -> {
            if (!p.connection.hasChannel(payload.type())) return false;
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, payload);
            return true;
        };
        // Lambda: the client class only loads when Escape is pressed (client side).
        LinkedContainers.cancelToServer = payload -> net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);
        LinkedContainers.openSync = (p, payload) -> {
            if (!p.connection.hasChannel(LinkedOpenPayload.TYPE)) return false;
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, payload);
            return true;
        };
        var game = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        InteractionGuard.permission = (p, pos) -> !game.post(new net.neoforged.neoforge.event.level.block.BreakBlockEvent(p.level(), pos, p.level().getBlockState(pos), p)).isCanceled();
        game.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e) -> SimpleInterfaces.onChange = () -> e.getServer().getPlayerList().getPlayers().forEach(ContainersNeoForge::sync));
        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) sync(p);
        });
        game.addListener(net.neoforged.bus.api.EventPriority.LOWEST, (net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) -> {
            if (e.isCanceled()) return;
            var a = LinkedContainers.onRightClickBlock(e.getEntity(), e.getHand(), e.getPos());
            if (a != net.minecraft.world.InteractionResult.PASS) {
                e.setCancellationResult(a);
                e.setCanceled(true);
            }
        });
    }

    private static void sync(net.minecraft.server.level.ServerPlayer p) {
        if (p.connection.hasChannel(ConfigSyncPayload.TYPE)) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new ConfigSyncPayload(SimpleInterfaces.syncJson()));
        com.simplebuilding.modules.simpleinterfaces.tooltip.ContainerTooltips.send(p);
    }
}
