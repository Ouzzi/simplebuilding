package com.simplemusic.client;

import com.simplemusic.SimpleMusic;

/** Client setup shared by the loaders: screen hooks and the per-tick work. */
public final class MusicClient {
    private MusicClient() {}

    public static void init() {
        SimpleMusic.openSheet = SheetMusicScreen::open;
    }

    public static void tick() {
        SheetPreview.tick();
        TrackPlayer.tick();
        PocketPlayer.tick();
    }
}
