package com.simplebuilding.modules.simplemaps;

import com.simplemaps.MapsComponents;
import com.simplemaps.MapsItems;
import com.simplemaps.SimpleMaps;
import com.simplemaps.net.FrameViewPayload;
import com.simplemaps.net.MapStatePayload;
import com.simplemaps.net.MapsNetwork;
import com.simplemaps.net.TileDataPayload;
import com.simplemaps.net.TileRequestPayload;
import com.simplemaps.net.WaypointEditPayload;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkProtocol;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry; same registration order as Fabric/NeoForge. Loot: forge mixin ReloadableServerRegistriesMixin. */
@Mod(SimpleMaps.MOD_ID)
public final class MapsForge {
    static Channel<CustomPacketPayload> channel;

    public MapsForge(FMLJavaModLoadingContext context) { com.simplebuilding.modules.simplemaps.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleMaps.init(FMLPaths.CONFIGDIR.get());
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            if (event.getRegistryKey().equals(Registries.DATA_COMPONENT_TYPE)) MapsComponents.register();
            if (event.getRegistryKey().equals(Registries.ITEM)) MapsItems.register();
        });
        BuildCreativeModeTabContentsEvent.BUS.addListener(event -> {
            if (!event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) return;
            // After the vanilla map like on Fabric: appending to the tail clashes with other mods' putAfter at the tail.
            var entries = event.getEntries();
            var previous = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MAP);
            for (var stack : MapsItems.tabStacks()) {
                if (entries.contains(previous)) entries.putAfter(previous, stack, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                else event.accept(stack);
                previous = stack;
            }
        });
        channel = ChannelBuilder.named(SimpleMaps.id("main")).payloadChannel().protocol(NetworkProtocol.PLAY)
                .serverbound()
                    .add(TileRequestPayload.TYPE, TileRequestPayload.CODEC.cast(), (payload, ctx) -> onPlayer(ctx, p -> MapsNetwork.handleTiles(payload, p)))
                    .add(WaypointEditPayload.TYPE, WaypointEditPayload.CODEC.cast(), (payload, ctx) -> onPlayer(ctx, p -> MapsNetwork.handleWaypoint(payload, p)))
                    .add(FrameViewPayload.TYPE, FrameViewPayload.CODEC.cast(), (payload, ctx) -> onPlayer(ctx, p -> MapsNetwork.handleFrameView(payload, p)))
                .clientbound()
                    .add(TileDataPayload.TYPE, TileDataPayload.CODEC.cast(), (payload, ctx) -> {
                        ctx.setPacketHandled(true);
                        ctx.enqueueWork(() -> MapsForgeClient.receive(payload));
                    })
                    .add(MapStatePayload.TYPE, MapStatePayload.CODEC.cast(), (payload, ctx) -> {
                        ctx.setPacketHandled(true);
                        ctx.enqueueWork(() -> MapsForgeClient.receive(payload));
                    })
                .build();
        SimpleMaps.toClient = (player, payload) -> {
            if (player.connection != null && channel.isRemotePresent(player.connection.getConnection())) channel.send(payload, PacketDistributor.PLAYER.with(player));
        };
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) MapsForgeClient.init();
    }

    private static void onPlayer(CustomPayloadEvent.Context ctx, Consumer<ServerPlayer> action) {
        ctx.setPacketHandled(true);
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) action.accept(player);
        });
    }
}
