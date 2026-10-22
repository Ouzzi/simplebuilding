package com.simplebuilding.modules.simplemaps;

import com.simplemaps.MapsComponents;
import com.simplemaps.MapsConfig;
import com.simplemaps.MapsItems;
import com.simplemaps.MapsLoot;
import com.simplemaps.SimpleMaps;
import com.simplemaps.net.DisplayPayload;
import com.simplemaps.net.FrameSavedPayload;
import com.simplemaps.net.FrameViewPayload;
import com.simplemaps.net.LayerDataPayload;
import com.simplemaps.net.LayerRequestPayload;
import com.simplemaps.net.LayerService;
import com.simplemaps.net.MapStatePayload;
import com.simplemaps.net.MapsNetwork;
import com.simplemaps.net.TileDataPayload;
import com.simplemaps.net.TileRequestPayload;
import com.simplemaps.net.WaypointEditPayload;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry: Vanilla Registry.register works inside the matching RegisterEvent. */
@Mod(SimpleMaps.MOD_ID)
public final class MapsNeoForge {
    public MapsNeoForge(IEventBus bus, Dist dist) {
        SimpleMaps.init(FMLPaths.CONFIGDIR.get());
        SimpleMaps.modLoaded = id -> net.neoforged.fml.ModList.get().isLoaded(id);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) -> com.simplemaps.web.WebCommands.register(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) -> com.simplemaps.web.WebMap.tick(e.getServer()));
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e) -> {
            if (e.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) com.simplebuilding.modules.simplemaps.guide.MapsGuide.register();
        });
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES)) e.accept(com.simplebuilding.modules.simplemaps.guide.MapsGuide.stack(), net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) com.simplebuilding.modules.simplemaps.guide.MapsGuide.installQuests(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            if (event.getRegistryKey().equals(Registries.DATA_COMPONENT_TYPE)) MapsComponents.register();
            if (event.getRegistryKey().equals(Registries.ITEM)) MapsItems.register();
            if (event.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB)) MapsItems.registerTab();
        });
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES) && MapsConfig.vanillaTabs()) MapsItems.tabStacks().forEach(event::accept);
        });
        bus.addListener((RegisterPayloadHandlersEvent event) -> {
            var registrar = event.registrar("1").optional();
            registrar.playToServer(TileRequestPayload.TYPE, TileRequestPayload.CODEC,
                    (payload, context) -> context.enqueueWork(() -> MapsNetwork.handleTiles(payload, (ServerPlayer) context.player())));
            registrar.playToServer(WaypointEditPayload.TYPE, WaypointEditPayload.CODEC,
                    (payload, context) -> context.enqueueWork(() -> MapsNetwork.handleWaypoint(payload, (ServerPlayer) context.player())));
            registrar.playToServer(FrameViewPayload.TYPE, FrameViewPayload.CODEC,
                    (payload, context) -> context.enqueueWork(() -> MapsNetwork.handleFrameView(payload, (ServerPlayer) context.player())));
            registrar.playToServer(DisplayPayload.TYPE, DisplayPayload.CODEC,
                    (payload, context) -> context.enqueueWork(() -> MapsNetwork.handleDisplay(payload, (ServerPlayer) context.player())));
            registrar.playToClient(TileDataPayload.TYPE, TileDataPayload.CODEC,
                    (payload, context) -> context.enqueueWork(() -> MapsNeoClient.receive(payload)));
            registrar.playToServer(LayerRequestPayload.TYPE, LayerRequestPayload.CODEC,
                    (payload, context) -> context.enqueueWork(() -> LayerService.handle(payload, (ServerPlayer) context.player())));
            registrar.playToClient(LayerDataPayload.TYPE, LayerDataPayload.CODEC,
                    (payload, context) -> context.enqueueWork(() -> MapsNeoClient.receive(payload)));
            registrar.playToClient(MapStatePayload.TYPE, MapStatePayload.CODEC,
                    (payload, context) -> context.enqueueWork(() -> MapsNeoClient.receive(payload)));
            registrar.playToClient(FrameSavedPayload.TYPE, FrameSavedPayload.CODEC,
                    (payload, context) -> context.enqueueWork(() -> MapsNeoClient.receive(payload)));
        });
        NeoForge.EVENT_BUS.addListener((LootTableLoadEvent event) -> MapsLoot.apply(
                ResourceKey.create(Registries.LOOT_TABLE, event.getName()), pool -> event.getTable().addPool(pool.build()), event.getRegistries()));
        SimpleMaps.toClient = (player, payload) -> {
            if (player.connection != null && player.connection.hasChannel(payload.type())) PacketDistributor.sendToPlayer(player, payload);
        };
        if (dist == Dist.CLIENT) MapsNeoClient.init();
    }
}
