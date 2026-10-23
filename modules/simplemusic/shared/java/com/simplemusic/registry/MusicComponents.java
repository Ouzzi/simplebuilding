package com.simplemusic.registry;

import com.simplemusic.SimpleMusic;
import com.simplemusic.disc.TrackRef;
import com.simplemusic.pocket.PocketState;
import com.simplemusic.sheet.SheetContent;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

/** Data components of Simple Music (registered in the loaders' data component event, before the items). */
public final class MusicComponents {
    public static final DataComponentType<SheetContent> SHEET = DataComponentType.<SheetContent>builder()
            .persistent(SheetContent.CODEC).networkSynchronized(SheetContent.STREAM_CODEC).cacheEncoding().build();
    public static final DataComponentType<TrackRef> TRACK = DataComponentType.<TrackRef>builder()
            .persistent(TrackRef.CODEC).networkSynchronized(TrackRef.STREAM_CODEC).build();
    public static final DataComponentType<PocketState> POCKET = DataComponentType.<PocketState>builder()
            .persistent(PocketState.CODEC).networkSynchronized(PocketState.STREAM_CODEC).build();
    /** Marks a Pocket Jukebox that plays right now (item model: the turning record, PJ23-3); never saved. */
    public static final DataComponentType<net.minecraft.util.Unit> PLAYING = DataComponentType.<net.minecraft.util.Unit>builder()
            .networkSynchronized(net.minecraft.util.Unit.STREAM_CODEC).build();

    public static void register() {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleMusic.id("sheet"), SHEET);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleMusic.id("track"), TRACK);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleMusic.id("pocket"), POCKET);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleMusic.id("playing"), PLAYING);
    }

    private MusicComponents() {}
}
