package com.simplemusic.registry;

import com.simplemusic.SimpleMusic;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class MusicTags {
    /**
     * Lines a placed Pocket Jukebox shows a copper contact towards (PJ23-4): redstone dust and, when SimpleBuilding is there,
     * its Astral, Nihil and Bridge dust (optional entries, no import).
     */
    public static final TagKey<Block> POCKET_JUKEBOX_LINKS = TagKey.create(Registries.BLOCK, SimpleMusic.id("pocket_jukebox_links"));

    private MusicTags() {}
}
