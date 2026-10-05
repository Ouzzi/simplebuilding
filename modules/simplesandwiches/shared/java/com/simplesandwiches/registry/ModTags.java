package com.simplesandwiches.registry;

import com.simplesandwiches.Sandwiches;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    /** Everything that may go between the bread (generated: Vanilla food without bowl/bottle + optional SimpleBuilding). */
    public static final TagKey<Item> SANDWICH_INGREDIENTS = TagKey.create(Registries.ITEM, Sandwiches.id("sandwich_ingredients"));
    /** Anvil repair material of the knife (iron nuggets, owner decision F8). */
    public static final TagKey<Item> KNIFE_REPAIR_MATERIALS = TagKey.create(Registries.ITEM, Sandwiches.id("knife_repair_materials"));
    public static final TagKey<Item> CUTTING_BOARD_ITEMS = TagKey.create(Registries.ITEM, Sandwiches.id("cutting_boards"));
    public static final TagKey<Block> CUTTING_BOARDS = TagKey.create(Registries.BLOCK, Sandwiches.id("cutting_boards"));

    private ModTags() {}
}
