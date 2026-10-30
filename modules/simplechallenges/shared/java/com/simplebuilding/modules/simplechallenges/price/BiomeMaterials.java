package com.simplebuilding.modules.simplechallenges.price;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-reachable materials by biome name, used where no chunk exists yet (the probe never generates chunks): what a player
 * gets without any tool from the ground and trees of that biome. Names are matched by substring of the biome path.
 */
public final class BiomeMaterials {
    private BiomeMaterials() {}

    public static List<WeightedItem> itemsFor(String biomePath) {
        String b = biomePath == null ? "" : biomePath;
        List<WeightedItem> out = new ArrayList<>();
        if (b.contains("desert")) {
            add(out, "minecraft:sand", 8);
            add(out, "minecraft:cactus", 2);
        } else if (b.contains("badlands")) {
            add(out, "minecraft:red_sand", 6);
            add(out, "minecraft:dirt", 2);
        } else if (b.contains("beach") || b.contains("ocean") || b.contains("river")) {
            add(out, "minecraft:sand", 6);
            add(out, "minecraft:gravel", 4);
        } else if (b.contains("mushroom")) {
            add(out, "minecraft:dirt", 6);
            add(out, "minecraft:brown_mushroom", 2);
        } else if (b.contains("jungle")) {
            add(out, "minecraft:jungle_log", 4);
            add(out, "minecraft:dirt", 4);
        } else if (b.contains("taiga") || b.contains("snow") || b.contains("frozen") || b.contains("ice") || b.contains("grove")) {
            add(out, "minecraft:spruce_log", 4);
            add(out, "minecraft:dirt", 6);
        } else if (b.contains("dark_forest")) {
            add(out, "minecraft:dark_oak_log", 4);
            add(out, "minecraft:dirt", 4);
        } else if (b.contains("birch")) {
            add(out, "minecraft:birch_log", 4);
            add(out, "minecraft:dirt", 5);
        } else if (b.contains("savanna")) {
            add(out, "minecraft:acacia_log", 3);
            add(out, "minecraft:dirt", 6);
        } else if (b.contains("cherry")) {
            add(out, "minecraft:cherry_log", 3);
            add(out, "minecraft:dirt", 5);
        } else if (b.contains("mangrove")) {
            add(out, "minecraft:mangrove_log", 4);
            add(out, "minecraft:mud", 4);
        } else if (b.contains("swamp")) {
            add(out, "minecraft:oak_log", 3);
            add(out, "minecraft:dirt", 5);
        } else if (b.contains("peaks") || b.contains("stony") || b.contains("windswept")) {
            add(out, "minecraft:gravel", 5);
            add(out, "minecraft:dirt", 4);
        } else {
            add(out, "minecraft:oak_log", 3);
            add(out, "minecraft:dirt", 7);
        }
        return out;
    }

    private static void add(List<WeightedItem> out, String id, int weight) {
        out.add(new WeightedItem(id, weight, 64));
    }
}
