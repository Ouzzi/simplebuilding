package com.simplebuilding.modules.simplemusic;

import com.simplemusic.SimpleMusic;
import com.simplemusic.client.BurnScreen;
import com.simplemusic.client.MusicClient;
import com.simplemusic.client.TrackPlayer;
import com.simplemusic.net.TrackDataPayload;
import com.simplemusic.net.TrackPlayPayload;
import com.simplemusic.net.UploadResultPayload;
import net.minecraftforge.network.PacketDistributor;

/** Forge client: screen openers, sender and tick. Loaded only on the client. */
final class MusicForgeClient {
    static void init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context) {
        MusicClient.init();
        net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(event -> event.enqueueWork(() ->
                net.minecraft.client.gui.screens.MenuScreens.register(com.simplemusic.registry.MusicRegistry.DISC_PRESS_MENU, com.simplemusic.client.DiscPressScreen::new)));
        SimpleMusic.toServer = payload -> MusicForge.channel.send(payload, PacketDistributor.SERVER.noArg());
        net.minecraftforge.event.TickEvent.ClientTickEvent.Post.BUS.addListener(event -> MusicClient.tick());
        net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent.BUS.addListener(event ->
                event.register(com.simplemusic.pocket.PocketContents.class, com.simplemusic.client.PocketTooltip::new));
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

    private MusicForgeClient() {}
}
