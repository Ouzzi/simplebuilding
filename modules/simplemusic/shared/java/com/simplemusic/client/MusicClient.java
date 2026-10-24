package com.simplemusic.client;

import com.simplemusic.SimpleMusic;
import com.simplemusic.config.MusicClientConfig;

/** Client setup shared by the loaders: screen hooks and the per-tick work. */
public final class MusicClient {
    private MusicClient() {}

    public static void init() {
        SimpleMusic.openSheet = SheetMusicScreen::open;
        MusicClientConfig.load(SimpleMusic.configDir);
        // CMD24: /simpleclient simplemusic (load foreign music).
        com.simplelib.api.config.ConfigCommands.client(SimpleMusic.MOD_ID).json(MusicClientConfig::toJson, MusicClientConfig::apply)
                .file(SimpleMusic.configDir, MusicClientConfig.FILE).register();
    }

    /** True (and one action-bar hint per track) when "Load foreign music" is off and the track is not ours. */
    static boolean refuseForeign(String hash) {
        if (MusicClientConfig.allowed(hash)) return false;
        if (MusicClientConfig.firstRefusal(hash)) {
            net.minecraft.client.Minecraft.getInstance().gui.hud.setOverlayMessage(
                    net.minecraft.network.chat.Component.translatable("simplemusic.foreign_off"), false);
        }
        return true;
    }

    public static void tick() {
        SheetPreview.tick();
        TrackPlayer.tick();
        PocketPlayer.tick();
    }
}
