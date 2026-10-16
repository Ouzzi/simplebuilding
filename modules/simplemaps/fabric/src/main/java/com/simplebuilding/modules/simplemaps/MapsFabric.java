package com.simplebuilding.modules.simplemaps;

import com.simplemaps.MapsComponents;
import com.simplemaps.MapsItems;
import com.simplemaps.MapsLoot;
import com.simplemaps.SimpleMaps;
import com.simplemaps.net.DisplayPayload;
import com.simplemaps.net.FrameViewPayload;
import com.simplemaps.net.LayerDataPayload;
import com.simplemaps.net.LayerRequestPayload;
import com.simplemaps.net.LayerService;
import com.simplemaps.net.MapStatePayload;
import com.simplemaps.net.MapsNetwork;
import com.simplemaps.net.TileDataPayload;
import com.simplemaps.net.TileRequestPayload;
import com.simplemaps.net.WaypointEditPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class MapsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleMaps.init(FabricLoader.getInstance().getConfigDir());
        MapsComponents.register();
        MapsItems.register();
        MapsItems.registerTab();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> out.insertAfter(Items.MAP, MapsItems.tabStacks(), net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        LootTableEvents.MODIFY.register((key, table, source, registries) -> MapsLoot.apply(key, table::withPool, registries));
        PayloadTypeRegistry.serverboundPlay().register(TileRequestPayload.TYPE, TileRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(WaypointEditPayload.TYPE, WaypointEditPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FrameViewPayload.TYPE, FrameViewPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DisplayPayload.TYPE, DisplayPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TileDataPayload.TYPE, TileDataPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MapStatePayload.TYPE, MapStatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LayerRequestPayload.TYPE, LayerRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LayerDataPayload.TYPE, LayerDataPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(LayerRequestPayload.TYPE,
                (payload, context) -> context.server().execute(() -> LayerService.handle(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(TileRequestPayload.TYPE,
                (payload, context) -> context.server().execute(() -> MapsNetwork.handleTiles(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(WaypointEditPayload.TYPE,
                (payload, context) -> context.server().execute(() -> MapsNetwork.handleWaypoint(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(FrameViewPayload.TYPE,
                (payload, context) -> context.server().execute(() -> MapsNetwork.handleFrameView(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(DisplayPayload.TYPE,
                (payload, context) -> context.server().execute(() -> MapsNetwork.handleDisplay(payload, context.player())));
        SimpleMaps.toClient = (player, payload) -> {
            if (ServerPlayNetworking.canSend(player, payload.type())) ServerPlayNetworking.send(player, payload);
        };
    }
}
