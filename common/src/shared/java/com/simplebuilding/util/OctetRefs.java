package com.simplebuilding.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

/**
 * The material, wood and melon octets and the mixed cell by registry id (split stage 6, docs/ai/PLAN-S6-OCTETS-2026-10-17.md):
 * they moved to the required sub-mod Simple Octets ({@code simpleoctets:}); SimpleBuilding names them only by id, like
 * {@link OvenRefs}. The cell logic itself SimpleBuilding reaches through {@code blocks.custom.OctetCellBlock}/{@code OctetCells}
 * (26.3: Simple Octets' API), the full block of a cell through SimpleLib's {@code OctetCells} ({@link LibServices}).
 * On 26.2 there are no octets: every lookup is empty.
 */
public final class OctetRefs {
    public static final String NAMESPACE = "simpleoctets";
    /** Woods with an octet (Simple Octets' order); Ash-/Duskwood only with Simple End. */
    public static final List<String> WOODS = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "pale_oak", "poplar", "bamboo", "crimson", "warped", "ashwood", "duskwood");

    private OctetRefs() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, path);
    }

    /** The block {@code simpleoctets:<path>}, or null when it is not registered. */
    public static @Nullable Block block(String path) {
        return BuiltInRegistries.BLOCK.getOptional(id(path)).orElse(null);
    }

    /** The item {@code simpleoctets:<path>}, or null when it is not registered. */
    public static @Nullable Item item(String path) {
        Item item = BuiltInRegistries.ITEM.getOptional(id(path)).orElse(null);
        return item == Items.AIR ? null : item;
    }

    /** The mixed cell (octets of several materials), null without Simple Octets. */
    public static @Nullable Block mixed() {
        return block("mixed_octet");
    }

    /** The melon octet cell, null without Simple Octets. */
    public static @Nullable Block melon() {
        return block("melon_octet");
    }

    /** Namespace of a wood's planks: Ash-/Duskwood belong to Simple End. */
    public static String woodNamespace(String wood) {
        return "ashwood".equals(wood) || "duskwood".equals(wood) ? "simpleend" : "minecraft";
    }

    /** The wood octet cells of this game, in Simple Octets' order. */
    public static List<Block> woodCells() {
        List<Block> out = new ArrayList<>();
        for (String wood : WOODS) {
            Block cell = block(wood + "_octet");
            if (cell != null) out.add(cell);
        }
        return out;
    }

    /** The material octet cells (every Simple Octets cell with an item except the woods), registry order. */
    public static List<Block> materialCells() {
        List<Block> woods = woodCells();
        List<Block> out = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (NAMESPACE.equals(id.getNamespace()) && id.getPath().endsWith("_octet") && !woods.contains(block)
                    && BuiltInRegistries.ITEM.containsKey(id)) out.add(block);
        }
        return out;
    }

    /** The octet item of a cell (same id), null for the melon and the mixed cell. */
    public static @Nullable Item itemOf(@Nullable Block cell) {
        if (cell == null) return null;
        Identifier id = BuiltInRegistries.BLOCK.getKey(cell);
        return NAMESPACE.equals(id.getNamespace()) ? item(id.getPath()) : null;
    }

    /** The full block whose octets {@code cell} holds (SimpleLib OctetCells), or null. */
    public static @Nullable Block source(@Nullable Block cell) {
        Block base = LibServices.octetBaseOf(cell);
        return base == Blocks.AIR ? null : base;
    }

    /** Whether {@code item} is one of Simple Octets' octet items. */
    public static boolean isOctetItem(@Nullable Item item) {
        return item != null && NAMESPACE.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())
                && BuiltInRegistries.ITEM.getKey(item).getPath().endsWith("_octet");
    }
}
