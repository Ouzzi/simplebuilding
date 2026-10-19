package com.simplestorage;

import com.simplelib.api.IdMoves;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Storage (split plan stage 8, docs/ai/PLAN-B-S8-STORAGE-2026-10-19.md): part 8a moves the crates of every wood and
 * the Storage Crafting Table out of SimpleBuilding; tiered chests, shulker boxes, bundles and backpacks follow (8b/8c).
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
        // Test centre station (framework contract, no import of the test centre): the module's items by id.
        List<String> items = new ArrayList<>();
        movedBlocks().forEach(path -> items.add(MOD_ID + ":" + path));
        items.add(MOD_ID + ":guide_book");
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "storage", "simplestorage.testcentre.station", items);
    }

    /** Every block (and block item) that moved: the crates of all woods (also the End woods) and the table. */
    public static List<String> movedBlocks() {
        List<String> out = new ArrayList<>();
        for (com.simplestorage.crate.CrateWood wood : com.simplestorage.crate.CrateWood.values()) out.add(wood.crateId());
        out.add(STORAGE_CRAFTING_TABLE);
        return out;
    }

    /** Old {@code simplebuilding:} ids resolve to the new ones (SimpleLib {@link IdMoves}; the next save writes the new id). */
    public static synchronized void registerIdMoves() {
        if (movesRegistered) return;
        movesRegistered = true;
        for (String path : movedBlocks()) {
            IdMoves.move(Registries.BLOCK, old(path), id(path));
            IdMoves.move(Registries.ITEM, old(path), id(path));
            // Every moved block has a crafting recipe of the same id, unlocked under recipes/decorations.
            IdMoves.move(Registries.RECIPE, old(path), id(path));
            IdMoves.move(Registries.ADVANCEMENT, old("recipes/decorations/" + path), id("recipes/decorations/" + path));
        }
        IdMoves.move(Registries.BLOCK_ENTITY_TYPE, old("crate"), id("crate"));
        IdMoves.move(Registries.BLOCK_ENTITY_TYPE, old(STORAGE_CRAFTING_TABLE), id(STORAGE_CRAFTING_TABLE));
    }

    private SimpleStorage() {}
}
