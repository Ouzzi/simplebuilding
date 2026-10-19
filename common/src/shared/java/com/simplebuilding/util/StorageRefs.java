package com.simplebuilding.util;

import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/**
 * The storage blocks that left SimpleBuilding, by id (split stage 8a, docs/ai/PLAN-B-S8-STORAGE-2026-10-19.md): the
 * crates of every wood and the Storage Crafting Table belong to the required, bundled sub-mod Simple Storage
 * ({@code simplestorage:*}) and SimpleBuilding imports none of its classes (principle 3): tabs, the search tab, the test
 * centre and the tests find them in the registry. Both exist on 26.3 only. Call after registration (registry lookups);
 * an unregistered id gives air.
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

    /** The crate of a wood ({@code "oak"}, {@code "ashwood"}); air when it is not registered. */
    public static Item crate(String wood) {
        return item(wood + "_crate");
    }

    /** Every registered crate item (Vanilla woods, with Simple End also its woods). */
    public static List<Item> crates() {
        List<Item> out = new ArrayList<>();
        BuiltInRegistries.ITEM.keySet().stream()
                .filter(key -> key.getNamespace().equals(McVersion.STORAGE_NAMESPACE) && key.getPath().endsWith("_crate"))
                .sorted()
                .forEach(key -> out.add(BuiltInRegistries.ITEM.getValue(key)));
        out.removeIf(item -> item == Items.AIR);
        return out;
    }
}
