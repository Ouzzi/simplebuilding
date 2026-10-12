package com.simplebuilding.modules.simplerespawn;

import com.simplebuilding.modules.simplerespawn.network.DownedListPayload;
import com.simplebuilding.modules.simplerespawn.network.RespawnStatePayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod("simplerespawn")
public final class RespawnNeoForge {
    public RespawnNeoForge(IEventBus bus, Dist dist) {
        RespawnConfig.load(FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e) -> {
            if (e.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) com.simplebuilding.modules.simplerespawn.guide.RespawnGuide.register();
        });
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) com.simplebuilding.modules.simplerespawn.guide.RespawnGuide.installQuests(FMLPaths.CONFIGDIR.get());
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e) -> {
            if (!e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES) || !SimpleRespawn.on()) return;
            var book = new net.minecraft.world.item.ItemStack(com.simplebuilding.modules.simplerespawn.guide.RespawnGuide.book());
            var anchor = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
            if (e.getParentEntries().contains(anchor)) e.insertBefore(anchor, book, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else e.accept(book);
        });
        bus.addListener((RegisterPayloadHandlersEvent e) -> {
            var registrar = e.registrar("1").optional();
            registrar.playToClient(DownedListPayload.TYPE, DownedListPayload.CODEC, (p, c) -> c.enqueueWork(() -> RespawnNeoClient.receive(p)));
            registrar.playToClient(RespawnStatePayload.TYPE, RespawnStatePayload.CODEC, (p, c) -> c.enqueueWork(() -> RespawnNeoClient.receive(p)));
        });
        SimpleRespawn.send = (player, payload) -> {
            if (player.connection != null && player.connection.hasChannel(payload.type())) PacketDistributor.sendToPlayer(player, payload);
        };
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> RespawnServer.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> RespawnServer.reset());
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> RespawnCommands.register(e.getDispatcher()));
        if (dist == Dist.CLIENT) RespawnNeoClient.init();
    }
}
