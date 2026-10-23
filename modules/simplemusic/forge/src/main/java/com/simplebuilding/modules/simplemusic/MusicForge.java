package com.simplebuilding.modules.simplemusic;

import com.simplemusic.SimpleMusic;
import com.simplemusic.net.SheetEditPayload;
import com.simplemusic.net.TrackDataPayload;
import com.simplemusic.net.TrackPlayPayload;
import com.simplemusic.net.TrackRequestPayload;
import com.simplemusic.net.UploadBeginPayload;
import com.simplemusic.net.UploadChunkPayload;
import com.simplemusic.net.UploadResultPayload;
import com.simplemusic.registry.MusicComponents;
import com.simplemusic.registry.MusicRegistry;
import com.simplemusic.upload.MusicCommands;
import com.simplemusic.upload.MusicServer;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkProtocol;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry; same registration order as Fabric/NeoForge. */
@Mod(SimpleMusic.MOD_ID)
public final class MusicForge {
    static Channel<CustomPacketPayload> channel;

    public MusicForge(FMLJavaModLoadingContext context) {
        com.simplebuilding.modules.simplemusic.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleMusic.init(FMLPaths.CONFIGDIR.get());
        SimpleMusic.modLoaded = net.minecraftforge.fml.ModList::isLoaded;
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.DATA_COMPONENT_TYPE)) MusicComponents.register();
            if (key.equals(Registries.BLOCK)) MusicRegistry.registerBlocks();
            if (key.equals(Registries.ITEM)) MusicRegistry.registerItems();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) MusicRegistry.registerTab();
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                MusicRegistry.registerBlockEntities(new BlockEntityType<>(com.simplemusic.press.DiscPressBlockEntity::new, Set.of(MusicRegistry.DISC_PRESS)),
                        new BlockEntityType<>(com.simplemusic.pocket.PocketJukeboxBlockEntity::new, Set.copyOf(MusicRegistry.POCKET_JUKEBOX_BLOCKS)));
            }
            if (key.equals(Registries.MENU)) MusicRegistry.registerMenus();
        });
        net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(event -> {
            boolean vanillaTabs = com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleMusic.MOD_ID);
            if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS) && vanillaTabs) {
                MusicRegistry.tabStacks().forEach(s -> event.getEntries().put(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES) && SimpleMusic.on()) {
                event.getEntries().put(com.simplemusic.guide.MusicGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                MusicRegistry.toolStacks().forEach(s -> event.getEntries().put(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
        });
        channel = ChannelBuilder.named(SimpleMusic.id("main")).payloadChannel().protocol(NetworkProtocol.PLAY)
                .serverbound()
                    .add(SheetEditPayload.TYPE, SheetEditPayload.CODEC.cast(), (p, ctx) -> onPlayer(ctx, pl -> MusicServer.handleSheetEdit(p, pl)))
                    .add(UploadBeginPayload.TYPE, UploadBeginPayload.CODEC.cast(), (p, ctx) -> onPlayer(ctx, pl -> MusicServer.handleUploadBegin(p, pl)))
                    .add(UploadChunkPayload.TYPE, UploadChunkPayload.CODEC.cast(), (p, ctx) -> onPlayer(ctx, pl -> MusicServer.handleUploadChunk(p, pl)))
                    .add(TrackRequestPayload.TYPE, TrackRequestPayload.CODEC.cast(), (p, ctx) -> onPlayer(ctx, pl -> MusicServer.handleTrackRequest(p, pl)))
                .clientbound()
                    .add(UploadResultPayload.TYPE, UploadResultPayload.CODEC.cast(), (p, ctx) -> {
                        ctx.setPacketHandled(true);
                        ctx.enqueueWork(() -> MusicForgeClient.receive(p));
                    })
                    .add(TrackPlayPayload.TYPE, TrackPlayPayload.CODEC.cast(), (p, ctx) -> {
                        ctx.setPacketHandled(true);
                        ctx.enqueueWork(() -> MusicForgeClient.receive(p));
                    })
                    .add(TrackDataPayload.TYPE, TrackDataPayload.CODEC.cast(), (p, ctx) -> {
                        ctx.setPacketHandled(true);
                        ctx.enqueueWork(() -> MusicForgeClient.receive(p));
                    })
                .build();
        SimpleMusic.toClient = (player, payload) -> {
            if (player.connection != null && channel.isRemotePresent(player.connection.getConnection())) channel.send(payload, PacketDistributor.PLAYER.with(player));
        };
        net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(e -> MusicCommands.register(e.getDispatcher()));
        net.minecraftforge.event.TickEvent.ServerTickEvent.Post.BUS.addListener(e -> MusicServer.tick(e.server()));
        net.minecraftforge.event.server.ServerStoppedEvent.BUS.addListener(e -> MusicServer.stopped());
        if (net.minecraftforge.fml.ModList.isLoaded("ftbquests")) com.simplemusic.guide.MusicGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) MusicForgeClient.init(context);
    }

    private static void onPlayer(CustomPayloadEvent.Context ctx, Consumer<ServerPlayer> action) {
        ctx.setPacketHandled(true);
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) action.accept(player);
        });
    }
}
