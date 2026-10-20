package com.simplemusic.registry;

import com.simplemusic.SimpleMusic;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class MusicTags {
    /** Containers on top of an Orchestrion that act as its playlist (chest, trapped chest, barrel, copper chests). */
    public static final TagKey<Block> PLAYLIST_CONTAINERS = TagKey.create(Registries.BLOCK, SimpleMusic.id("playlist_containers"));

    private MusicTags() {}
}
