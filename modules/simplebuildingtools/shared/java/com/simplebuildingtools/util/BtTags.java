package com.simplebuildingtools.util;

import com.simplebuildingtools.SimpleBuildingTools;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** The mod's tags. */
public final class BtTags {
    /** Blocks no wand and no blueprint places (also takes SimpleBuilding's {@code simplebuilding:building_wand_blacklist}). */
    public static final TagKey<Block> BUILDING_WAND_BLACKLIST = TagKey.create(Registries.BLOCK, SimpleBuildingTools.id("building_wand_blacklist"));
    /** The six crafted wands (enchantable with Cover, Bridge, Linear). */
    public static final TagKey<Item> BUILDING_WANDS = TagKey.create(Registries.ITEM, SimpleBuildingTools.id("building_wands"));
    /** Wand modifiers that exclude each other (Cover, Bridge, Linear). */
    public static final TagKey<net.minecraft.world.item.enchantment.Enchantment> WAND_MODIFIERS =
            TagKey.create(Registries.ENCHANTMENT, SimpleBuildingTools.id("exclusive_set/wand_modifier"));

    private BtTags() {
    }
}
