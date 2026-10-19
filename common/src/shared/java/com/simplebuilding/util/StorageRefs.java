package com.simplebuilding.util;

import com.simplebuilding.version.McVersion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * The storage blocks that left SimpleBuilding, by id (split stage 8a, docs/ai/PLAN-B-S8-STORAGE-2026-10-19.md): the
 * crates of every wood and the Storage Crafting Table belong to the required, bundled sub-mod Simple Storage
 * ({@code simplestorage:*}) and SimpleBuilding imports none of its classes (principle 3): tabs, the search tab, the test
 * centre and the tests find them in the registry. Both exist on 26.3 only. Call after registration (registry lookups);
 * an unregistered id gives air.
 *
 * <p>Split stage 8b (docs/ai/PLAN-B-S8B-STORAGE-2026-10-19.md): the tiered and trapped chests, the trapped copper chests
 * and the tier shulker boxes followed. They exist on both lines: on 26.2 SimpleBuilding still registers them
 * ({@code McVersion.STORAGE_IN_SB}, {@code simplebuilding:*}), so these lookups work everywhere.
 */
public final class StorageRefs {
    public static final String STORAGE_CRAFTING_TABLE = "storage_crafting_table";

    private StorageRefs() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(McVersion.STORAGE_NAMESPACE, path);
    }

    public static Block block(String path) {
        return BuiltInRegistries.BLOCK.getValue(id(path));
    }

    public static Item item(String path) {
        return BuiltInRegistries.ITEM.getValue(id(path));
    }

    /** Tier names of the chests and shulker boxes, in tier order. */
    public static final java.util.List<String> TIERS = java.util.List.of("reinforced", "netherite", "enderite");

    /** The tiered chest of {@code tier} ({@code "reinforced"}, {@code "netherite"}, {@code "enderite"}). */
    public static Block chestBlock(String tier) {
        return block(tier + "_chest");
    }

    public static Item chest(String tier) {
        return item(tier + "_chest");
    }

    /** The trapped twin of a tiered chest; air where the line has none ({@code McVersion.TRAPPED_TIERED_CHESTS}). */
    public static Block trappedChestBlock(String tier) {
        return block(tier + "_trapped_chest");
    }

    public static Item trappedChest(String tier) {
        return item(tier + "_trapped_chest");
    }

    public static Block shulkerBoxBlock(String tier) {
        return block(tier + "_shulker_box");
    }

    public static Item shulkerBox(String tier) {
        return item(tier + "_shulker_box");
    }

    /** The eight trapped copper chests (oxidation order, then waxed); empty where the line has none. */
    public static java.util.List<Item> trappedCopperChests() {
        if (!McVersion.TRAPPED_TIERED_CHESTS) return java.util.List.of();
        java.util.List<Item> out = new java.util.ArrayList<>();
        for (String waxed : java.util.List.of("", "waxed_")) {
            for (String stage : java.util.List.of("", "exposed_", "weathered_", "oxidized_")) out.add(item(waxed + stage + "trapped_copper_chest"));
        }
        return out;
    }

    /** The crate of a wood ({@code "oak"}, {@code "ashwood"}); air when it is not registered. */
    public static Item crate(String wood) {
        return item(wood + "_crate");
    }
}
