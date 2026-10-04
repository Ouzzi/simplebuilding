package com.simplesandwiches.sandwich;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;

/**
 * Ingredient to layer picture group used by the sandwich item model
 * ({@code assets/simplesandwiches/items/sandwich.json}, one {@code select} per layer). Unknown
 * ingredients (other mods, datapack additions) use {@code generic}. The same key list drives the
 * texture generator {@code tools/textures/sandwiches.py} and the resource generator.
 */
public final class SandwichVisuals {
    public static final String GENERIC = "generic";
    public static final List<String> KEYS = List.of("meat_raw", "meat_cooked", "fish_raw", "fish_cooked", "potato",
            "carrot", "golden", "apple", "melon", "berries", "beetroot", "kelp", "cookie", "pie", "chorus",
            "spider_eye", "rotten", "cheese", "cake", "netherite", "enderite", GENERIC);

    private static final Map<String, String> BY_ID = Map.ofEntries(
            Map.entry("minecraft:beef", "meat_raw"), Map.entry("minecraft:porkchop", "meat_raw"),
            Map.entry("minecraft:mutton", "meat_raw"), Map.entry("minecraft:rabbit", "meat_raw"),
            Map.entry("minecraft:chicken", "meat_raw"),
            Map.entry("minecraft:cooked_beef", "meat_cooked"), Map.entry("minecraft:cooked_porkchop", "meat_cooked"),
            Map.entry("minecraft:cooked_mutton", "meat_cooked"), Map.entry("minecraft:cooked_rabbit", "meat_cooked"),
            Map.entry("minecraft:cooked_chicken", "meat_cooked"),
            Map.entry("minecraft:cod", "fish_raw"), Map.entry("minecraft:salmon", "fish_raw"),
            Map.entry("minecraft:tropical_fish", "fish_raw"), Map.entry("minecraft:pufferfish", "fish_raw"),
            Map.entry("minecraft:cooked_cod", "fish_cooked"), Map.entry("minecraft:cooked_salmon", "fish_cooked"),
            Map.entry("minecraft:potato", "potato"), Map.entry("minecraft:baked_potato", "potato"),
            Map.entry("minecraft:poisonous_potato", "potato"),
            Map.entry("minecraft:carrot", "carrot"),
            Map.entry("minecraft:golden_carrot", "golden"), Map.entry("minecraft:golden_apple", "golden"),
            Map.entry("minecraft:enchanted_golden_apple", "golden"),
            Map.entry("minecraft:apple", "apple"), Map.entry("minecraft:melon_slice", "melon"),
            Map.entry("minecraft:sweet_berries", "berries"), Map.entry("minecraft:glow_berries", "berries"),
            Map.entry("minecraft:beetroot", "beetroot"), Map.entry("minecraft:dried_kelp", "kelp"),
            Map.entry("minecraft:cookie", "cookie"), Map.entry("minecraft:pumpkin_pie", "pie"),
            Map.entry("minecraft:chorus_fruit", "chorus"), Map.entry("minecraft:spider_eye", "spider_eye"),
            Map.entry("minecraft:rotten_flesh", "rotten"),
            Map.entry("simplesandwiches:cheese_slice", "cheese"), Map.entry("simplesandwiches:cake_slice", "cake"),
            Map.entry("simplebuilding:netherite_apple", "netherite"),
            Map.entry("simplebuilding:enchanted_netherite_apple", "netherite"),
            Map.entry("simplebuilding:netherite_carrot", "netherite"),
            Map.entry("simplebuilding:enderite_apple", "enderite"),
            Map.entry("simplebuilding:enchanted_enderite_apple", "enderite"),
            Map.entry("simplebuilding:enderite_carrot", "enderite"));

    public static String key(Holder<Item> item) {
        String id = item.unwrapKey().map(k -> k.identifier().toString()).orElse("");
        return BY_ID.getOrDefault(id, GENERIC);
    }

    private SandwichVisuals() {}
}
