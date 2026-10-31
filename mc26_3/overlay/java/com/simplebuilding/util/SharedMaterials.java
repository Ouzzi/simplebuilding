package com.simplebuilding.util;

import com.simplelib.api.SimpleLibApi;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * 26.3: the shared materials (split stage 3d) belong to SimpleLib ({@code simplelib:*}). {@code ModItems}/{@code ModBlocks}
 * keep their fields, which point at SimpleLib's objects, so every use in SimpleBuilding stays as it was; SimpleBuilding's
 * own registration ({@code own}) is not run. The 26.2 twin (common/src/mc26_2/java) registers them as before.
 *
 * <p>IG31-13 (31.10.): the Enderite materials ({@link #ORES_ITEMS}), Dimensional Scrap and Sage Ore with the sage orb belong to
 * the required sub-mod Simple Ores ({@code simpleores:*}), found by id (SimpleBuilding runs Simple Ores' initializer before its
 * own registration, {@code Simplebuilding#onInitialize}); the six building cores moved to SimpleLib ({@code simplelib:*_core}).
 */
public final class SharedMaterials {
    /** Every material group SimpleBuilding uses (all of them): their recipes and the anvil crushing stay on. */
    public static final List<String> GROUPS = List.of("cracked_diamond", "netherite_nugget", "upgrade_templates",
            "polished_end_stone", "cores");

    /** Item ids that moved from {@code simplebuilding:} to {@code simplelib:} (same paths). */
    public static final List<String> ITEMS = List.of("diamond_pebble", "cracked_diamond", "cracked_diamond_block", "netherite_nugget",
            "basic_upgrade_template",
            // Module family stage 3 (AGENTS rule 16): the shared End building block.
            "polished_end_stone",
            // IG31-13: the building cores.
            "copper_core", "iron_core", "gold_core", "diamond_core", "netherite_core", "enderite_core");
    /** Block ids that moved. */
    public static final List<String> BLOCKS = List.of("cracked_diamond_block", "polished_end_stone");
    /** IG31-13: item ids that moved from {@code simplebuilding:} to {@code simpleores:} (same paths; the blocks have block items). */
    public static final List<String> ORES_ITEMS = List.of("enderite_nugget", "raw_enderite", "layered_raw_enderite", "enderite_scrap",
            "enderite_ingot", "enderite_block", "enderite_upgrade_template", "sage_ore", "deepslate_sage_ore", "sage_orb",
            "dimensional_scrap", "nether_dimensional_scrap", "end_dimensional_scrap");
    /** IG31-13: block ids that moved to Simple Ores. */
    public static final List<String> ORES_BLOCKS = List.of("enderite_block", "sage_ore", "deepslate_sage_ore", "dimensional_scrap",
            "nether_dimensional_scrap", "end_dimensional_scrap");
    /** IG31-13: recipe ids (stage 3d: SimpleBuilding's, then SimpleLib's) that moved to Simple Ores -> advancement folder. */
    public static final java.util.Map<String, String> ORES_RECIPES = java.util.Map.of("enderite_nugget_from_ingot", "misc",
            "enderite_ingot_from_nugget", "misc", "layered_raw_enderite", "misc", "enderite_scrap_from_blasting_layered_raw_enderite", "misc",
            "enderite_ingot_from_scrap", "misc", "enderite_block", "decorations", "enderite_ingot", "building_blocks",
            "enderite_upgrade_template", "misc");
    /** Recipe ids that moved -> folder of their recipe advancement ({@code recipes/<folder>/<id>}, same path in SimpleLib). */
    public static final java.util.Map<String, String> RECIPES = java.util.Map.ofEntries(
            java.util.Map.entry("cracked_diamond", "misc"), java.util.Map.entry("diamond_from_blasting_cracked_diamond", "misc"),
            java.util.Map.entry("cracked_diamond_block", "building_blocks"), java.util.Map.entry("cracked_diamond_from_cracked_diamond_block", "misc"),
            java.util.Map.entry("basic_upgrade_template", "misc"), java.util.Map.entry("netherite_nugget", "misc"),
            java.util.Map.entry("netherite_ingot", "misc"),
            java.util.Map.entry("polished_end_stone", "building_blocks"),
            java.util.Map.entry("polished_end_stone_from_end_stone_stonecutting", "building_blocks"),
            java.util.Map.entry("polished_end_stone_from_end_stone_bricks_stonecutting", "building_blocks"));

    private SharedMaterials() {
    }

    /** Whether {@code item} is one of the shared materials (SimpleLib's or Simple Ores', not SimpleBuilding's own registration). */
    public static boolean isShared(Item item) {
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        return id != null && ("simplelib".equals(id.getNamespace()) && ITEMS.contains(id.getPath())
                || ORES.equals(id.getNamespace()) && ORES_ITEMS.contains(id.getPath()));
    }

    public static boolean isShared(Block block) {
        var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        return id != null && ("simplelib".equals(id.getNamespace()) && BLOCKS.contains(id.getPath())
                || ORES.equals(id.getNamespace()) && ORES_BLOCKS.contains(id.getPath()));
    }

    /** Namespace of Simple Ores. */
    public static final String ORES = "simpleores";

    public static Item item(String id, Supplier<Item> own) {
        if (ORES_ITEMS.contains(id)) return ores(net.minecraft.core.registries.BuiltInRegistries.ITEM, id);
        return SimpleLibApi.materialItem(id);
    }

    public static Block block(String id, Supplier<Block> own) {
        if (ORES_BLOCKS.contains(id)) return ores(net.minecraft.core.registries.BuiltInRegistries.BLOCK, id);
        return SimpleLibApi.materialBlock(id);
    }

    /** A Simple Ores object by id; it must be registered already (Simple Ores' initializer runs first), else this is a load-order bug. */
    private static <T> T ores(net.minecraft.core.Registry<T> registry, String path) {
        var id = net.minecraft.resources.Identifier.fromNamespaceAndPath(ORES, path);
        if (!registry.containsKey(id)) {
            throw new IllegalStateException("Simple Ores has not registered " + id + " yet: it must initialize before SimpleBuilding (IG31-13)");
        }
        return registry.getValue(id);
    }

    /** In the mod constructor / initializer (before data packs load): request the groups, plug in the claims check. */
    public static void request() {
        GROUPS.forEach(SimpleLibApi::requestMaterial);
        SimpleLibApi.setAutomationCheck(com.simplebuilding.api.WorldPermissions::mayAutomate);
    }
}
