package com.simplebuilding.modules.simplemusic;

import com.simplemusic.SimpleMusic;
import com.simplemusic.client.BurnScreen;
import com.simplemusic.client.MusicClient;
import com.simplemusic.client.TrackPlayer;
import com.simplemusic.net.TrackDataPayload;
import com.simplemusic.net.TrackPlayPayload;
import com.simplemusic.net.UploadResultPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class MusicFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MusicClient.init();
        net.minecraft.client.gui.screens.MenuScreens.register(com.simplemusic.registry.MusicRegistry.DISC_PRESS_MENU, com.simplemusic.client.DiscPressScreen::new);
        SimpleMusic.toServer = payload -> {
            if (ClientPlayNetworking.canSend(payload.type())) ClientPlayNetworking.send(payload);
        };
        ClientPlayNetworking.registerGlobalReceiver(UploadResultPayload.TYPE, (p, c) -> c.client().execute(() -> BurnScreen.receive(p)));
        ClientPlayNetworking.registerGlobalReceiver(TrackPlayPayload.TYPE, (p, c) -> c.client().execute(() -> TrackPlayer.receive(p)));
        ClientPlayNetworking.registerGlobalReceiver(TrackDataPayload.TYPE, (p, c) -> c.client().execute(() -> TrackPlayer.receive(p)));
        ClientTickEvents.END_CLIENT_TICK.register(client -> MusicClient.tick());
        // W24-2: the line contacts in the line's colour (tintindex 1..4 = north, east, south, west).
        net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register(com.simplemusic.client.PocketLinkTint.SOURCES,
                com.simplemusic.registry.MusicRegistry.POCKET_JUKEBOX_BLOCKS.toArray(new net.minecraft.world.level.block.Block[0]));
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(com.simplemusic.registry.MusicRegistry.POCKET_JUKEBOX_BE,
                com.simplemusic.client.PocketJukeboxRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback.EVENT.register(data ->
                data instanceof com.simplemusic.pocket.PocketContents pocket ? new com.simplemusic.client.PocketTooltip(pocket) : null);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(TrackPlayer::clear));
    }
}
