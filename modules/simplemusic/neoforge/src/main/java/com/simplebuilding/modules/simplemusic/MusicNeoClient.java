package com.simplebuilding.modules.simplemusic;

import com.simplemusic.SimpleMusic;
import com.simplemusic.client.BurnScreen;
import com.simplemusic.client.MusicClient;
import com.simplemusic.client.TrackPlayer;
import com.simplemusic.net.TrackDataPayload;
import com.simplemusic.net.TrackPlayPayload;
import com.simplemusic.net.UploadResultPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only; referenced only on the client dist. */
final class MusicNeoClient {
    static void init(net.neoforged.bus.api.IEventBus bus) {
        MusicClient.init();
        bus.addListener((net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) ->
                event.register(com.simplemusic.registry.MusicRegistry.DISC_PRESS_MENU, com.simplemusic.client.DiscPressScreen::new));
        bus.addListener((net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent event) ->
                event.register(com.simplemusic.pocket.PocketContents.class, com.simplemusic.client.PocketTooltip::new));
        SimpleMusic.toServer = ClientPacketDistributor::sendToServer;
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post e) -> MusicClient.tick());
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) -> TrackPlayer.clear());
    }

    static void receive(UploadResultPayload p) {
        BurnScreen.receive(p);
    }

    static void receive(TrackPlayPayload p) {
        TrackPlayer.receive(p);
    }

    static void receive(TrackDataPayload p) {
        TrackPlayer.receive(p);
    }

    private MusicNeoClient() {}
}
