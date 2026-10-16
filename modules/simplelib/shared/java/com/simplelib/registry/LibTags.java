package com.simplelib.registry;

import com.simplelib.SimpleLib;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Data-driven parts of SimpleLib: warmable food (the crucible tags live in Simple Crucibles). */
public final class LibTags {
    /** Food that gets warm in a crucible (owner 37: everything eaten or served warm). */
    public static final TagKey<Item> WARMABLE_FOOD = item("warmable_food");

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, SimpleLib.id(path));
    }

    private LibTags() {}
}
