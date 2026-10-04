package com.simplelib.registry;

import com.simplelib.SimpleLib;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

/** Data-driven parts of the crucible: heat sources, warmable food, extreme-heat recipes, build materials. */
public final class LibTags {
    public static final TagKey<Block> HEAT_MEDIUM = block("heat_source/medium");
    public static final TagKey<Block> HEAT_HIGH = block("heat_source/high");
    public static final TagKey<Block> HEAT_EXTREME = block("heat_source/extreme");
    /** Fluids that heat like soul lava: source extreme, flowing high (SimpleBuilding adds soul lava). */
    public static final TagKey<Fluid> EXTREME_HEAT_FLUIDS = TagKey.create(Registries.FLUID, SimpleLib.id("extreme_heat"));

    /** Food that gets warm in a crucible (owner 37: everything eaten or served warm). */
    public static final TagKey<Item> WARMABLE_FOOD = item("warmable_food");
    /** Inputs whose cooking recipes need extreme heat regardless of the recipe type (owner 52). */
    public static final TagKey<Item> NEEDS_EXTREME_HEAT = item("needs_extreme_heat");
    /** Off-hand materials for the in-world crucible build: walls (strikes 1-4) and handles (5-6). */
    public static final TagKey<Item> CRUCIBLE_WALLS = item("crucible_walls");
    public static final TagKey<Item> CRUCIBLE_HANDLES = item("crucible_handles");
    /** Vanilla upgrade materials for the axe way (owner 50): diamonds, netherite ingot. */
    public static final TagKey<Item> UPGRADE_REINFORCED = item("upgrade_reinforced");
    public static final TagKey<Item> UPGRADE_NETHERITE = item("upgrade_netherite");

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, SimpleLib.id(path));
    }

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, SimpleLib.id(path));
    }

    private LibTags() {}
}
