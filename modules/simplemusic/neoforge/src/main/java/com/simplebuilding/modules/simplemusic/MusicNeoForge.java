package com.simplebuilding.modules.simplemusic;

import com.simplemusic.SimpleMusic;
import com.simplemusic.net.SheetEditPayload;
import com.simplemusic.net.TrackDataPayload;
import com.simplemusic.net.TrackPlayPayload;
import com.simplemusic.net.TrackRequestPayload;
import com.simplemusic.net.UploadBeginPayload;
import com.simplemusic.net.UploadChunkPayload;
import com.simplemusic.net.UploadResultPayload;
import com.simplemusic.orchestrion.OrchestrionBlockEntity;
import com.simplemusic.registry.MusicComponents;
import com.simplemusic.registry.MusicRegistry;
import com.simplemusic.upload.MusicCommands;
import com.simplemusic.upload.MusicServer;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry: Vanilla Registry.register works inside the matching RegisterEvent. */
@Mod(SimpleMusic.MOD_ID)
public final class MusicNeoForge {
    public MusicNeoForge(IEventBus bus, Dist dist) {
        SimpleMusic.init(FMLPaths.CONFIGDIR.get());
        SimpleMusic.modLoaded = net.neoforged.fml.ModList.get()::isLoaded;
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.DATA_COMPONENT_TYPE)) MusicComponents.register();
            if (key.equals(Registries.BLOCK)) MusicRegistry.registerBlocks();
            if (key.equals(Registries.ITEM)) MusicRegistry.registerItems();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) MusicRegistry.registerTab();
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                MusicRegistry.registerBlockEntities(new BlockEntityType<>(OrchestrionBlockEntity::new, Set.of(MusicRegistry.ORCHESTRION), false),
                        new BlockEntityType<>(com.simplemusic.press.DiscPressBlockEntity::new, Set.of(MusicRegistry.DISC_PRESS), false));
            }
            if (key.equals(Registries.MENU)) MusicRegistry.registerMenus();
        });
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            boolean vanillaTabs = com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleMusic.MOD_ID);
            if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS) && vanillaTabs) {
                MusicRegistry.tabStacks().forEach(s -> event.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES) && SimpleMusic.on()) {
                event.accept(com.simplemusic.guide.MusicGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                MusicRegistry.toolStacks().forEach(s -> event.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
        });
        bus.addListener((RegisterPayloadHandlersEvent event) -> {
            var registrar = event.registrar("1").optional();
            registrar.playToServer(SheetEditPayload.TYPE, SheetEditPayload.CODEC,
                    (p, c) -> c.enqueueWork(() -> MusicServer.handleSheetEdit(p, (ServerPlayer) c.player())));
            registrar.playToServer(UploadBeginPayload.TYPE, UploadBeginPayload.CODEC,
                    (p, c) -> c.enqueueWork(() -> MusicServer.handleUploadBegin(p, (ServerPlayer) c.player())));
            registrar.playToServer(UploadChunkPayload.TYPE, UploadChunkPayload.CODEC,
                    (p, c) -> c.enqueueWork(() -> MusicServer.handleUploadChunk(p, (ServerPlayer) c.player())));
            registrar.playToServer(TrackRequestPayload.TYPE, TrackRequestPayload.CODEC,
                    (p, c) -> c.enqueueWork(() -> MusicServer.handleTrackRequest(p, (ServerPlayer) c.player())));
            registrar.playToClient(UploadResultPayload.TYPE, UploadResultPayload.CODEC,
                    (p, c) -> c.enqueueWork(() -> MusicNeoClient.receive(p)));
            registrar.playToClient(TrackPlayPayload.TYPE, TrackPlayPayload.CODEC,
                    (p, c) -> c.enqueueWork(() -> MusicNeoClient.receive(p)));
            registrar.playToClient(TrackDataPayload.TYPE, TrackDataPayload.CODEC,
                    (p, c) -> c.enqueueWork(() -> MusicNeoClient.receive(p)));
        });
        SimpleMusic.toClient = (player, payload) -> {
            if (player.connection != null && player.connection.hasChannel(payload.type())) PacketDistributor.sendToPlayer(player, payload);
        };
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) -> MusicCommands.register(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) -> MusicServer.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> MusicServer.stopped());
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) com.simplemusic.guide.MusicGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (dist == Dist.CLIENT) MusicNeoClient.init(bus);
    }
}
