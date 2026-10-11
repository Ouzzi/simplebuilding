package com.simplebuilding.modules.simplefarming;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("simplefarming")
public final class FarmingForge {
    private static net.minecraftforge.network.Channel<net.minecraft.network.protocol.common.custom.CustomPacketPayload> channel;

    public FarmingForge(FMLJavaModLoadingContext context) { com.simplebuilding.modules.simplefarming.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleFarming.configDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
        SimpleFarming.load();
        net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(e -> FarmingCommands.register(e.getDispatcher()));
        HoeHarvestHint.register();
        if (com.simplebuilding.modules.simplefarming.forge.ModuleForgeTests.enabled()) com.simplebuilding.modules.simplefarming.forge.FarmingForgeChecks.register();
        InteractionGuard.permission = (p, pos) -> {
            var event = new net.minecraftforge.event.level.BlockEvent.BreakEvent(p.level(), pos, p.level().getBlockState(pos), p, net.minecraftforge.common.util.Result.DEFAULT);
            return !net.minecraftforge.event.level.BlockEvent.BreakEvent.BUS.post(event) && !event.getResult().isDenied();
        };
        net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock.BUS.addListener(net.minecraftforge.eventbus.api.listener.Priority.LOWEST, e -> {
            if (e.getUseBlock().isDenied() || e.getUseItem().isDenied()) return false;
            var result = HoeHarvestHandler.onRightClickBlock(e.getEntity(), e.getHand(), e.getPos(), e.getFace());
            if (result == net.minecraft.world.InteractionResult.PASS) return false;
            e.setCancellationResult(result);
            return true;
        });
        net.minecraftforge.event.level.BlockEvent.FarmlandTrampleEvent.BUS.addListener(e -> {
            return Protection.farmland(e.getEntity());
        });
        channel = net.minecraftforge.network.ChannelBuilder.named(net.minecraft.resources.Identifier.parse("simplefarming:main")).payloadChannel().protocol(net.minecraftforge.network.NetworkProtocol.PLAY).clientbound()
                .add(com.simplebuilding.modules.simplefarming.network.ConfigSyncPayload.TYPE, com.simplebuilding.modules.simplefarming.network.ConfigSyncPayload.STREAM_CODEC.cast(), (p, c) -> {
                    c.setPacketHandled(true);
                    c.enqueueWork(() -> SimpleFarming.receiveSync(p.json()));
                }).build();
        net.minecraftforge.event.server.ServerStartedEvent.BUS.addListener(e -> SimpleFarming.onChange = () -> e.getServer().getPlayerList().getPlayers().forEach(FarmingForge::sync));
        net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) sync(p);
        });
    }
    private static void sync(net.minecraft.server.level.ServerPlayer p) {
        if (p.connection != null && channel.isRemotePresent(p.connection.getConnection()))
            channel.send(new com.simplebuilding.modules.simplefarming.network.ConfigSyncPayload(SimpleFarming.syncJson()), net.minecraftforge.network.PacketDistributor.PLAYER.with(p));
    }
}
