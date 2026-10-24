package com.simplebuilding.modules.simpletools;
import com.simplelib.api.InteractionGuard;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("simpletools")
public final class ToolsForge {
    private static net.minecraftforge.network.Channel<net.minecraft.network.protocol.common.custom.CustomPacketPayload> channel;

    public ToolsForge(FMLJavaModLoadingContext context) { com.simplebuilding.modules.simpletools.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleTools.registerMoves();
        SimpleTools.configDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
        SimpleTools.load();
        net.minecraftforge.registries.RegisterEvent.getBus(context.getModBusGroup()).addListener(e -> {
            if (e.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) com.simplebuilding.modules.simpletools.guide.ToolsGuide.register();
        });
        net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(e -> {
            if (e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES)) e.getEntries().put(com.simplebuilding.modules.simpletools.guide.ToolsGuide.stack(), net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        if (net.minecraftforge.fml.ModList.isLoaded("ftbquests")) com.simplebuilding.modules.simpletools.guide.ToolsGuide.installQuests(SimpleTools.configDir);
        net.minecraftforge.registries.RegisterEvent.getBus(context.getModBusGroup()).addListener(e -> {
            var key = e.getRegistryKey();
            if (key.equals(net.minecraft.core.registries.Registries.BLOCK)) ToolsRegistry.registerBlocks();
            if (key.equals(net.minecraft.core.registries.Registries.ITEM)) ToolsRegistry.registerItems();
            if (key.equals(net.minecraft.core.registries.Registries.MENU)) ToolsRegistry.registerMenus();
        });
        net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(e -> {
            if (e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS))
                ToolsRegistry.tabStacks().forEach(st -> e.getEntries().put(st, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            if (e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES))
                ToolsRegistry.brushStacks().forEach(st -> {
                    var brush = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BRUSH);
                    if (e.getEntries().contains(brush)) e.getEntries().putAfter(brush, st, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                    else e.getEntries().put(st, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                });
        });
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) ToolsForgeClient.init(context);
        net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(e -> ToolsCommands.register(e.getDispatcher()));
        InteractionGuard.permission = (p, pos) -> {
            var event = new net.minecraftforge.event.level.BlockEvent.BreakEvent(p.level(), pos, p.level().getBlockState(pos), p, net.minecraftforge.common.util.Result.DEFAULT);
            return !net.minecraftforge.event.level.BlockEvent.BreakEvent.BUS.post(event) && !event.getResult().isDenied();
        };
        net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock.BUS.addListener(net.minecraftforge.eventbus.api.listener.Priority.LOWEST, e -> {
            if (e.getUseBlock().isDenied() || e.getUseItem().isDenied()) return false;
            var result = FurnaceLavaFillHandler.onRightClickBlock(e.getEntity(), e.getHand(), e.getPos(), e.getFace());
            if (result == net.minecraft.world.InteractionResult.PASS) return false;
            e.setCancellationResult(result);
            return true;
        });
        channel = net.minecraftforge.network.ChannelBuilder.named(net.minecraft.resources.Identifier.parse("simpletools:main")).payloadChannel().protocol(net.minecraftforge.network.NetworkProtocol.PLAY).clientbound()
                .add(com.simplebuilding.modules.simpletools.network.ConfigSyncPayload.TYPE, com.simplebuilding.modules.simpletools.network.ConfigSyncPayload.STREAM_CODEC.cast(), (p, c) -> {
                    c.setPacketHandled(true);
                    c.enqueueWork(() -> SimpleTools.receiveSync(p.json()));
                }).build();
        net.minecraftforge.event.server.ServerStartedEvent.BUS.addListener(e -> SimpleTools.onChange = () -> e.getServer().getPlayerList().getPlayers().forEach(ToolsForge::sync));
        net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) sync(p);
        });
    }
    private static void sync(net.minecraft.server.level.ServerPlayer p) {
        if (p.connection != null && channel.isRemotePresent(p.connection.getConnection()))
            channel.send(new com.simplebuilding.modules.simpletools.network.ConfigSyncPayload(SimpleTools.syncJson()), net.minecraftforge.network.PacketDistributor.PLAYER.with(p));
    }
}
