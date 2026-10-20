package com.simplestorage;

import com.simplelib.api.IdMoves;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Storage (split plan stage 8, docs/ai/PLAN-B-S8-STORAGE-2026-10-19.md): part 8a moved the crates of every wood and
 * the Storage Crafting Table out of SimpleBuilding, part 8b (PLAN-B-S8B-STORAGE-2026-10-19.md) the tiered and trapped
 * chests, the trapped copper chests and the tier shulker boxes; bundles and backpacks follow (8c).
 * Loader entry points call {@link #init} once in their constructor / initializer, before any registry event, so the id
 * moves out of SimpleBuilding are known in time (Forge aliases).
 */
public final class SimpleStorage {
    public static final String MOD_ID = "simplestorage";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Storage");
    /** The mod the blocks came from; old worlds hold its ids. */
    public static final String OLD_NAMESPACE = "simplebuilding";
    /** The crafting table that keeps its grid (path unchanged). */
    public static final String STORAGE_CRAFTING_TABLE = "storage_crafting_table";

    private static boolean movesRegistered;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Identifier old(String path) {
        return Identifier.fromNamespaceAndPath(OLD_NAMESPACE, path);
    }

    /** Super-mod switch "Enable Simple Storage" (framework SubMods, set by SimpleBuilding): off = no guide, no Vanilla tab entries. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID);
    }

    public static void init() {
        registerIdMoves();
        // Copper chest -> Reinforced -> Netherite -> Enderite (and the shulker boxes) need SimpleLib's shared materials.
        MATERIALS.forEach(com.simplelib.api.SimpleLibApi::requestMaterial);
        // The shipped upgrade file (data/simplestorage/in_world_upgrades) applies before datapack files of other namespaces.
        com.simplelib.api.InWorldUpgrades.registerDefaults(MOD_ID);
        // An upgrade into a tiered chest keeps the contents of both halves; into a tier shulker box its contents and color.
        com.simplelib.api.InWorldUpgrades.registerHandler(block -> block instanceof com.simplestorage.chest.TieredChestBlock,
                com.simplestorage.chest.TieredChests::upgradeInPlace);
        com.simplelib.api.InWorldUpgrades.registerHandler(block -> block instanceof com.simplestorage.chest.TieredShulkerBoxBlock, (level, pos, to) -> {
            com.simplestorage.chest.TieredShulkerBoxes.upgradeInPlace(level, pos, to);
            return 1;
        });
        // A double chest upgrades as one: the hammer works both halves.
        com.simplelib.api.HammerActions.registerParts(block -> block instanceof com.simplestorage.chest.TieredChestBlock,
                com.simplestorage.chest.TieredChests::halves);
        // Test centre station (framework contract, no import of the test centre): the module's items by id.
        List<String> items = new ArrayList<>();
        movedBlocks().forEach(path -> items.add(MOD_ID + ":" + path));
        items.add(MOD_ID + ":guide_book");
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "storage", "simplestorage.testcentre.station", items);
    }

    /** SimpleLib materials the upgrades and recipes use (cracked diamond, netherite and enderite nuggets). */
    public static final List<String> MATERIALS = List.of("cracked_diamond", "netherite_nugget", "enderite");
    public static final List<String> TIERS = List.of("reinforced", "netherite", "enderite");
    public static final List<String> COPPER_STAGES = List.of("", "exposed_", "weathered_", "oxidized_");
    public static final List<String> DYES = List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");

    /** Every block (and block item) that moved: 8a the crates of all woods (also the End woods) and the table, 8b the chests and boxes. */
    public static List<String> movedBlocks() {
        List<String> out = new ArrayList<>();
        for (com.simplestorage.crate.CrateWood wood : com.simplestorage.crate.CrateWood.values()) out.add(wood.crateId());
        out.add(STORAGE_CRAFTING_TABLE);
        out.addAll(movedChests());
        return out;
    }

    /** Split stage 8b: tiered chests, their trapped twins, the trapped copper chests (four stages, waxed) and the tier shulker boxes. */
    public static List<String> movedChests() {
        List<String> out = new ArrayList<>();
        for (String tier : TIERS) out.add(tier + "_chest");
        for (String tier : TIERS) out.add(tier + "_trapped_chest");
        for (String waxed : List.of("", "waxed_")) for (String stage : COPPER_STAGES) out.add(waxed + stage + "trapped_copper_chest");
        for (String tier : TIERS) out.add(tier + "_shulker_box");
        return out;
    }

    /**
     * The recipes that moved with them (id -> recipe-book folder of its unlock advancement): the Reinforced Chest, the
     * trapped chests, the trapped copper chests and their honeycomb waxing (redstone), the Reinforced Shulker Box from
     * every Vanilla box and the dyeing of every tier box.
     */
    public static java.util.Map<String, String> movedChestRecipes() {
        java.util.Map<String, String> out = new java.util.LinkedHashMap<>();
        out.put("reinforced_chest", "decorations");
        for (String tier : TIERS) out.put(tier + "_trapped_chest", "redstone");
        for (String waxed : List.of("", "waxed_")) for (String stage : COPPER_STAGES) out.put(waxed + stage + "trapped_copper_chest", "redstone");
        for (String stage : COPPER_STAGES) out.put("waxed_" + stage + "trapped_copper_chest_from_honeycomb", "redstone");
        out.put("reinforced_shulker_box_from_shulker_box", "decorations");
        for (String dye : DYES) out.put("reinforced_shulker_box_from_" + dye + "_shulker_box", "decorations");
        for (String dye : DYES) for (String tier : TIERS) out.put(dye + "_" + tier + "_shulker_box", "decorations");
        return out;
    }

    /** Old {@code simplebuilding:} ids resolve to the new ones (SimpleLib {@link IdMoves}; the next save writes the new id). */
    public static synchronized void registerIdMoves() {
        if (movesRegistered) return;
        movesRegistered = true;
        List<String> chests = movedChests();
        for (String path : movedBlocks()) {
            IdMoves.move(Registries.BLOCK, old(path), id(path));
            IdMoves.move(Registries.ITEM, old(path), id(path));
            if (chests.contains(path)) continue;
            // 8a: every crate and the table has a crafting recipe of the same id, unlocked under recipes/decorations.
            IdMoves.move(Registries.RECIPE, old(path), id(path));
            IdMoves.move(Registries.ADVANCEMENT, old("recipes/decorations/" + path), id("recipes/decorations/" + path));
        }
        // 8b: recipes of the chests and boxes (the crates' and the table's are their own block ids, above).
        movedChestRecipes().forEach((path, folder) -> {
            IdMoves.move(Registries.RECIPE, old(path), id(path));
            IdMoves.move(Registries.ADVANCEMENT, old("recipes/" + folder + "/" + path), id("recipes/" + folder + "/" + path));
        });
        for (String type : List.of("tiered_chest", "trapped_copper_chest", "tiered_shulker_box")) {
            IdMoves.move(Registries.BLOCK_ENTITY_TYPE, old(type), id(type));
        }
        IdMoves.move(Registries.DATA_COMPONENT_TYPE, old(com.simplestorage.registry.StorageComponents.CONTAINER_COUNTS_ID),
                id(com.simplestorage.registry.StorageComponents.CONTAINER_COUNTS_ID));
        IdMoves.move(Registries.BLOCK_ENTITY_TYPE, old("crate"), id("crate"));
        IdMoves.move(Registries.BLOCK_ENTITY_TYPE, old(STORAGE_CRAFTING_TABLE), id(STORAGE_CRAFTING_TABLE));
    }

    private SimpleStorage() {}
}
