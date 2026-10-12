package com.simplebuilding.modules.simpleinterfaces;

import com.simplebuilding.modules.simpleinterfaces.linked.InteractionGuard;
import com.simplebuilding.modules.simpleinterfaces.linked.LinkedContainers;
import com.simplebuilding.modules.simpleinterfaces.network.ConfigSyncPayload;
import com.simplebuilding.modules.simpleinterfaces.network.LinkedOpenPayload;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Forge entry: the GameTests and the server side of the linked containers / Easy Shulkers (formerly Simple QoL); the
 * style is client mixins (manifest MixinConfigs) plus a lazily loaded config.
 */
@Mod("simpleinterfaces")
public final class ContainersForge {
    private static net.minecraftforge.network.Channel<net.minecraft.network.protocol.common.custom.CustomPacketPayload> channel;

    public ContainersForge(FMLJavaModLoadingContext context) {
        com.simplebuilding.modules.simpleinterfaces.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleInterfaces.configDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
        SimpleInterfaces.load();
        channel = net.minecraftforge.network.ChannelBuilder.named(net.minecraft.resources.Identifier.parse("simpleinterfaces:main")).payloadChannel().protocol(net.minecraftforge.network.NetworkProtocol.PLAY).clientbound()
                .add(ConfigSyncPayload.TYPE, ConfigSyncPayload.STREAM_CODEC.cast(), (p, c) -> {
                    c.setPacketHandled(true);
                    c.enqueueWork(() -> SimpleInterfaces.receiveSync(p.json()));
                })
                .add(LinkedOpenPayload.TYPE, LinkedOpenPayload.CODEC, (p, c) -> {
                    c.setPacketHandled(true);
                    c.enqueueWork(() -> ContainersForgeClient.receive(p));
                }).build();
        LinkedContainers.openSync = (p, payload) -> {
            if (p.connection == null || !channel.isRemotePresent(p.connection.getConnection())) return false;
            channel.send(payload, net.minecraftforge.network.PacketDistributor.PLAYER.with(p));
            return true;
        };
        InteractionGuard.permission = (p, pos) -> {
            var event = new net.minecraftforge.event.level.BlockEvent.BreakEvent(p.level(), pos, p.level().getBlockState(pos), p, net.minecraftforge.common.util.Result.DEFAULT);
            return !net.minecraftforge.event.level.BlockEvent.BreakEvent.BUS.post(event) && !event.getResult().isDenied();
        };
        net.minecraftforge.event.server.ServerStartedEvent.BUS.addListener(e -> SimpleInterfaces.onChange = () -> e.getServer().getPlayerList().getPlayers().forEach(ContainersForge::sync));
        net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) sync(p);
        });
        net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock.BUS.addListener(net.minecraftforge.eventbus.api.listener.Priority.LOWEST, e -> {
            if (e.getUseBlock().isDenied() || e.getUseItem().isDenied()) return false;
            var result = LinkedContainers.onRightClickBlock(e.getEntity(), e.getHand(), e.getPos());
            if (result == net.minecraft.world.InteractionResult.PASS) return false;
            e.setCancellationResult(result);
            return true;
        });
    }

    private static void sync(net.minecraft.server.level.ServerPlayer p) {
        if (p.connection != null && channel.isRemotePresent(p.connection.getConnection()))
            channel.send(new ConfigSyncPayload(SimpleInterfaces.syncJson()), net.minecraftforge.network.PacketDistributor.PLAYER.with(p));
    }
}
