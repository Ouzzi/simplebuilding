package com.simplemusic.registry;

import com.simplemusic.SimpleMusic;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class MusicTags {
    /** Containers on top of an Orchestrion that act as its playlist (chest, trapped chest, barrel, copper chests). */
    public static final TagKey<Block> PLAYLIST_CONTAINERS = TagKey.create(Registries.BLOCK, SimpleMusic.id("playlist_containers"));
    /**
     * Lines a placed Pocket Jukebox shows a copper contact towards (PJ23-4): redstone dust and, when SimpleBuilding is there,
     * its Astral, Nihil and Bridge dust (optional entries, no import).
     */
    public static final TagKey<Block> POCKET_JUKEBOX_LINKS = TagKey.create(Registries.BLOCK, SimpleMusic.id("pocket_jukebox_links"));

    private MusicTags() {}
}
