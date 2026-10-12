package com.simplebuilding.modules.simplemovement;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("simplemovement")
public final class MovementForge {
    private static net.minecraftforge.network.Channel<net.minecraft.network.protocol.common.custom.CustomPacketPayload> channel;

    public MovementForge(FMLJavaModLoadingContext context) { com.simplebuilding.modules.simplemovement.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleMovement.configDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
        SimpleMovement.load();
        net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(e -> MovementCommands.register(e.getDispatcher()));
        channel = net.minecraftforge.network.ChannelBuilder.named(net.minecraft.resources.Identifier.parse("simplemovement:main")).payloadChannel().protocol(net.minecraftforge.network.NetworkProtocol.PLAY).clientbound()
                .add(com.simplebuilding.modules.simplemovement.network.ConfigSyncPayload.TYPE, com.simplebuilding.modules.simplemovement.network.ConfigSyncPayload.STREAM_CODEC.cast(), (p, c) -> {
                    c.setPacketHandled(true);
                    c.enqueueWork(() -> SimpleMovement.receiveSync(p.json()));
                })
                .add(com.simplebuilding.modules.simplemovement.network.CrawlStatePayload.TYPE, com.simplebuilding.modules.simplemovement.network.CrawlStatePayload.CODEC.cast(), (p, c) -> {
                    c.setPacketHandled(true);
                    c.enqueueWork(() -> MovementForgeClient.receive(p));
                }).build();
        SimpleMovement.crawlSync = (p, state) -> {
            if (p instanceof net.minecraft.server.level.ServerPlayer player && player.connection != null)
                channel.send(new com.simplebuilding.modules.simplemovement.network.CrawlStatePayload(p.getUUID(), state), net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(player));
        };
        net.minecraftforge.event.entity.player.PlayerEvent.StartTracking.BUS.addListener(e -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer viewer && e.getTarget() instanceof net.minecraft.world.entity.player.Player p
                    && p instanceof CrawlAccessor c && c.simpleQualityOfLife$isCrawling())
                channel.send(new com.simplebuilding.modules.simplemovement.network.CrawlStatePayload(p.getUUID(), true), net.minecraftforge.network.PacketDistributor.PLAYER.with(viewer));
        });
        net.minecraftforge.event.server.ServerStartedEvent.BUS.addListener(e -> SimpleMovement.onChange = () -> e.getServer().getPlayerList().getPlayers().forEach(MovementForge::sync));
        net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) sync(p);
        });
    }
    private static void sync(net.minecraft.server.level.ServerPlayer p) {
        if (p.connection != null && channel.isRemotePresent(p.connection.getConnection()))
            channel.send(new com.simplebuilding.modules.simplemovement.network.ConfigSyncPayload(SimpleMovement.syncJson()), net.minecraftforge.network.PacketDistributor.PLAYER.with(p));
    }
}
