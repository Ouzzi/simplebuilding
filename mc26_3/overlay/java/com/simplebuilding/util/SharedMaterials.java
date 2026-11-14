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
            "dimensional_scrap", "nether_dimensional_scrap", "end_dimensional_scrap",
            // N46/N47-M2 (b-circ46, 14.11.): the Astralit/Nihilit line with its ores, dust and devices.
            "astralit_ore", "nihilith_ore", "dull_astralit_ore", "dull_nihilith_ore", "astralit_dust", "dull_astralit_dust", "nihilith_shard",
            "dull_nihilith_shard", "astral_redstone", "nihil_redstone", "bridge_redstone", "astral_redstone_block", "nihil_redstone_block",
            "bridge_redstone_block", "astral_redstone_torch", "nihil_redstone_torch", "astralit_lamp", "nihilith_lamp", "astralit_end_rod",
            "nihilith_end_rod", "astral_piston", "nihil_piston", "astral_rail", "nihil_rail");
    /** IG31-13: block ids that moved to Simple Ores. */
    public static final List<String> ORES_BLOCKS = List.of("enderite_block", "sage_ore", "deepslate_sage_ore", "dimensional_scrap",
            "nether_dimensional_scrap", "end_dimensional_scrap",
            // N46/N47-M2 (b-circ46): the line's blocks (the wall torches have no item).
            "astralit_ore", "nihilith_ore", "dull_astralit_ore", "dull_nihilith_ore", "astral_redstone", "nihil_redstone", "bridge_redstone",
            "astral_redstone_block", "nihil_redstone_block", "bridge_redstone_block", "astral_redstone_torch", "astral_redstone_wall_torch",
            "nihil_redstone_torch", "nihil_redstone_wall_torch", "astralit_lamp", "nihilith_lamp", "astralit_end_rod", "nihilith_end_rod",
            "astral_piston", "nihil_piston", "astral_rail", "nihil_rail");
    /** IG31-13: recipe ids (stage 3d: SimpleBuilding's, then SimpleLib's) that moved to Simple Ores -> advancement folder; N46/M2: the line's recipes. */
    public static final java.util.Map<String, String> ORES_RECIPES = java.util.Map.ofEntries(
            java.util.Map.entry("enderite_nugget_from_ingot", "misc"),
            java.util.Map.entry("enderite_ingot_from_nugget", "misc"),
            java.util.Map.entry("layered_raw_enderite", "misc"),
            java.util.Map.entry("enderite_scrap_from_blasting_layered_raw_enderite", "misc"),
            java.util.Map.entry("enderite_ingot_from_scrap", "misc"),
            java.util.Map.entry("enderite_block", "decorations"),
            java.util.Map.entry("enderite_ingot", "building_blocks"),
            java.util.Map.entry("enderite_upgrade_template", "misc"),
            java.util.Map.entry("astral_piston", "redstone"),
            java.util.Map.entry("astral_rail", "transportation"),
            java.util.Map.entry("astral_redstone", "redstone"),
            java.util.Map.entry("astral_redstone_block", "redstone"),
            java.util.Map.entry("astral_redstone_from_astral_redstone_block", "redstone"),
            java.util.Map.entry("astral_redstone_torch", "redstone"),
            java.util.Map.entry("astralit_dust_from_dull_astralit_dust", "misc"),
            java.util.Map.entry("astralit_end_rod", "decorations"),
            java.util.Map.entry("astralit_lamp", "redstone"),
            java.util.Map.entry("bridge_redstone", "redstone"),
            java.util.Map.entry("bridge_redstone_block", "redstone"),
            java.util.Map.entry("bridge_redstone_from_bridge_redstone_block", "redstone"),
            java.util.Map.entry("nihil_piston", "redstone"),
            java.util.Map.entry("nihil_rail", "transportation"),
            java.util.Map.entry("nihil_redstone", "redstone"),
            java.util.Map.entry("nihil_redstone_block", "redstone"),
            java.util.Map.entry("nihil_redstone_from_nihil_redstone_block", "redstone"),
            java.util.Map.entry("nihil_redstone_torch", "redstone"),
            java.util.Map.entry("nihilith_end_rod", "decorations"),
            java.util.Map.entry("nihilith_lamp", "redstone"),
            java.util.Map.entry("nihilith_shard_from_dull_nihilith_shard", "misc"));
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

    /** N46/M2: a Simple Ores block that only 26.3 has (the line); the 26.2 twin never registers one (McVersion.END_SYSTEMS off). */
    public static Block oresBlock(String id) {
        return ores(net.minecraft.core.registries.BuiltInRegistries.BLOCK, id);
    }

    /** N46/M2: a Simple Ores item that only 26.3 has (the line). */
    public static Item oresItem(String id) {
        return ores(net.minecraft.core.registries.BuiltInRegistries.ITEM, id);
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
