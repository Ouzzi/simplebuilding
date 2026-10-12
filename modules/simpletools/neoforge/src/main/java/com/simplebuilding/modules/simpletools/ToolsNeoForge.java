package com.simplebuilding.modules.simpletools;

import net.neoforged.fml.common.Mod;

@Mod("simpletools")
public final class ToolsNeoForge {
    public ToolsNeoForge(net.neoforged.bus.api.IEventBus bus) {
        SimpleTools.configDir = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        SimpleTools.load();
        ModuleNeoTests.register(bus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) -> ToolsCommands.register(e.getDispatcher()));
        var game = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        InteractionGuard.permission = (p, pos) -> !game.post(new net.neoforged.neoforge.event.level.block.BreakBlockEvent(p.level(), pos, p.level().getBlockState(pos), p)).isCanceled();
        game.addListener(net.neoforged.bus.api.EventPriority.LOWEST, (net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) -> {
            if (e.isCanceled()) return;
            var a = FurnaceLavaFillHandler.onRightClickBlock(e.getEntity(), e.getHand(), e.getPos(), e.getFace());
            if (a != net.minecraft.world.InteractionResult.PASS) {
                e.setCancellationResult(a);
                e.setCanceled(true);
            }
        });
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e) -> e.registrar("1").optional().playToClient(com.simplebuilding.modules.simpletools.network.ConfigSyncPayload.TYPE,
                com.simplebuilding.modules.simpletools.network.ConfigSyncPayload.STREAM_CODEC, (p, c) -> c.enqueueWork(() -> SimpleTools.receiveSync(p.json()))));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e) -> SimpleTools.onChange = () -> e.getServer().getPlayerList().getPlayers().forEach(ToolsNeoForge::sync));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) sync(p);
        });
    }
    private static void sync(net.minecraft.server.level.ServerPlayer p) {
        if (p.connection.hasChannel(com.simplebuilding.modules.simpletools.network.ConfigSyncPayload.TYPE))
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new com.simplebuilding.modules.simpletools.network.ConfigSyncPayload(SimpleTools.syncJson()));
    }
}
