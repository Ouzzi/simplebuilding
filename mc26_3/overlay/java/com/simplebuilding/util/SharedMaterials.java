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
 */
public final class SharedMaterials {
    /** Every material group SimpleBuilding uses (all of them): their recipes and the anvil crushing stay on. */
    public static final List<String> GROUPS = List.of("cracked_diamond", "netherite_nugget", "enderite", "upgrade_templates");

    /** Item ids that moved from {@code simplebuilding:} to {@code simplelib:} (same paths). */
    public static final List<String> ITEMS = List.of("diamond_pebble", "cracked_diamond", "cracked_diamond_block", "netherite_nugget",
            "enderite_nugget", "raw_enderite", "layered_raw_enderite", "enderite_scrap", "enderite_ingot", "enderite_block",
            "basic_upgrade_template", "enderite_upgrade_template");
    /** Block ids that moved. */
    public static final List<String> BLOCKS = List.of("cracked_diamond_block", "enderite_block");
    /** Recipe ids that moved -> folder of their recipe advancement ({@code recipes/<folder>/<id>}, same path in SimpleLib). */
    public static final java.util.Map<String, String> RECIPES = java.util.Map.ofEntries(
            java.util.Map.entry("cracked_diamond", "misc"), java.util.Map.entry("diamond_from_blasting_cracked_diamond", "misc"),
            java.util.Map.entry("cracked_diamond_block", "building_blocks"), java.util.Map.entry("cracked_diamond_from_cracked_diamond_block", "misc"),
            java.util.Map.entry("basic_upgrade_template", "misc"), java.util.Map.entry("netherite_nugget", "misc"),
            java.util.Map.entry("netherite_ingot", "misc"), java.util.Map.entry("enderite_nugget_from_ingot", "misc"),
            java.util.Map.entry("enderite_ingot_from_nugget", "misc"), java.util.Map.entry("layered_raw_enderite", "misc"),
            java.util.Map.entry("enderite_scrap_from_blasting_layered_raw_enderite", "misc"), java.util.Map.entry("enderite_ingot_from_scrap", "misc"),
            java.util.Map.entry("enderite_block", "decorations"), java.util.Map.entry("enderite_ingot", "building_blocks"),
            java.util.Map.entry("enderite_upgrade_template", "misc"));

    private SharedMaterials() {
    }

    public static Item item(String id, Supplier<Item> own) {
        return SimpleLibApi.materialItem(id);
    }

    public static Block block(String id, Supplier<Block> own) {
        return SimpleLibApi.materialBlock(id);
    }

    /** In the mod constructor / initializer (before data packs load): request the groups, plug in the claims check. */
    public static void request() {
        GROUPS.forEach(SimpleLibApi::requestMaterial);
        SimpleLibApi.setAutomationCheck(com.simplebuilding.api.WorldPermissions::mayAutomate);
    }
}
