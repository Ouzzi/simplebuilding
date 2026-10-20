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
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class MusicFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleMusic.init(FabricLoader.getInstance().getConfigDir());
        SimpleMusic.modLoaded = FabricLoader.getInstance()::isModLoaded;
        MusicComponents.register();
        MusicRegistry.registerBlocks();
        MusicRegistry.registerItems();
        MusicRegistry.registerBlockEntities(new BlockEntityType<>(OrchestrionBlockEntity::new, Set.of(MusicRegistry.ORCHESTRION)));
        MusicRegistry.registerTab();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out -> {
            if (!com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FabricLoader.getInstance().getConfigDir(), SimpleMusic.MOD_ID)) return;
            var stacks = MusicRegistry.tabStacks();
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.JUKEBOX))) out.insertAfter(Items.JUKEBOX, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else stacks.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleMusic.on()) return;
            var book = java.util.List.of(com.simplemusic.guide.MusicGuide.stack());
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        PayloadTypeRegistry.serverboundPlay().register(SheetEditPayload.TYPE, SheetEditPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UploadBeginPayload.TYPE, UploadBeginPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UploadChunkPayload.TYPE, UploadChunkPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TrackRequestPayload.TYPE, TrackRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(UploadResultPayload.TYPE, UploadResultPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TrackPlayPayload.TYPE, TrackPlayPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TrackDataPayload.TYPE, TrackDataPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SheetEditPayload.TYPE, (p, c) -> c.server().execute(() -> MusicServer.handleSheetEdit(p, c.player())));
        ServerPlayNetworking.registerGlobalReceiver(UploadBeginPayload.TYPE, (p, c) -> c.server().execute(() -> MusicServer.handleUploadBegin(p, c.player())));
        ServerPlayNetworking.registerGlobalReceiver(UploadChunkPayload.TYPE, (p, c) -> c.server().execute(() -> MusicServer.handleUploadChunk(p, c.player())));
        ServerPlayNetworking.registerGlobalReceiver(TrackRequestPayload.TYPE, (p, c) -> c.server().execute(() -> MusicServer.handleTrackRequest(p, c.player())));
        SimpleMusic.toClient = (player, payload) -> {
            if (ServerPlayNetworking.canSend(player, payload.type())) ServerPlayNetworking.send(player, payload);
        };
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> MusicCommands.register(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(MusicServer::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> MusicServer.stopped());
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simplemusic.guide.MusicGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
