package com.simplebuilding.modules.simplemovement;

import net.neoforged.fml.common.Mod;

@Mod("simplemovement")
public final class MovementNeoForge {
    public MovementNeoForge(net.neoforged.bus.api.IEventBus bus) {
        SimpleMovement.configDir = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        SimpleMovement.load();
        ModuleNeoTests.register(bus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) -> MovementCommands.register(e.getDispatcher()));
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e) -> e.registrar("1").optional().playToClient(com.simplebuilding.modules.simplemovement.network.CrawlStatePayload.TYPE,
                com.simplebuilding.modules.simplemovement.network.CrawlStatePayload.CODEC, (p, c) -> c.enqueueWork(() -> com.simplebuilding.modules.simplemovement.client.MovementClient.receive(p))));
        SimpleMovement.crawlSync = (player, state) -> {
            if (player instanceof net.minecraft.server.level.ServerPlayer p)
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new com.simplebuilding.modules.simplemovement.network.CrawlStatePayload(p.getUUID(), state));
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking e) -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer viewer && e.getTarget() instanceof net.minecraft.world.entity.player.Player p
                    && p instanceof CrawlAccessor c && c.simpleQualityOfLife$isCrawling() && viewer.connection.hasChannel(com.simplebuilding.modules.simplemovement.network.CrawlStatePayload.TYPE))
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(viewer, new com.simplebuilding.modules.simplemovement.network.CrawlStatePayload(p.getUUID(), true));
        });
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e) -> e.registrar("1").optional().playToClient(com.simplebuilding.modules.simplemovement.network.ConfigSyncPayload.TYPE,
                com.simplebuilding.modules.simplemovement.network.ConfigSyncPayload.STREAM_CODEC, (p, c) -> c.enqueueWork(() -> SimpleMovement.receiveSync(p.json()))));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e) -> SimpleMovement.onChange = () -> e.getServer().getPlayerList().getPlayers().forEach(MovementNeoForge::sync));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) sync(p);
        });
    }
    private static void sync(net.minecraft.server.level.ServerPlayer p) {
        if (p.connection.hasChannel(com.simplebuilding.modules.simplemovement.network.ConfigSyncPayload.TYPE))
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new com.simplebuilding.modules.simplemovement.network.ConfigSyncPayload(SimpleMovement.syncJson()));
    }
}
