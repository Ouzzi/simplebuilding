package com.simpleoctets;

import com.simpleoctets.registry.OctetMaterials;
import com.simpleoctets.registry.OctetRegistry;
import com.simplelib.api.IdMoves;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Octets (split stage 6, docs/ai/PLAN-S6-OCTETS-2026-10-17.md): octets (0.5 x 0.5 x 0.5) of every wood, of every
 * block with stairs and slabs and of the melon, the mixed cell of several materials, moved out of SimpleBuilding. The
 * cell logic ({@link com.simpleoctets.block.OctetCellBlock}, {@link com.simpleoctets.block.OctetCells}) is public API:
 * SimpleBuilding's chess octets are cells of it and mix with these. Loader entry points call {@link #init} once in their
 * constructor / initializer, before any registry event, so the id moves are known in time (Forge aliases).
 */
public final class SimpleOctets {
    public static final String MOD_ID = "simpleoctets";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Octets");
    /** The mod the octets came from; old worlds hold its ids. */
    public static final String SB_NAMESPACE = "simplebuilding";

    private static Predicate<String> loaded = id -> false;
    private static boolean movesRegistered;
    private static boolean flammable;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Identifier sb(String path) {
        return Identifier.fromNamespaceAndPath(SB_NAMESPACE, path);
    }

    /** Super-mod switch "Enable Simple Octets" (framework SubMods, set by SimpleBuilding); off = no guide, no melon placing, no Vanilla tab entries. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID);
    }

    /** Whether another mod is loaded (the loader's mod list, set by {@link #init}). */
    public static boolean isLoaded(String modId) {
        return loaded.test(modId);
    }

    public static void init(java.nio.file.Path configDir, Predicate<String> modLoaded) {
        loaded = modLoaded;
        registerIdMoves();
        // Test centre station (framework contract, no import of the test centre): every octet item by id (missing ones skipped).
        List<String> items = new ArrayList<>();
        cellPaths().forEach(path -> items.add(MOD_ID + ":" + path));
        items.add("minecraft:melon_slice");
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "octets", "simpleoctets.testcentre.station", items);
    }

    /**
     * After blocks and items, and again once every mod is loaded (common setup / server start): carving tools
     * (Simple Hammers' sledgehammer) find the cells through SimpleLib's OctetCells. A cell of another mod's block is
     * registered once that block exists (the first call may come before the other mod registered it).
     */
    public static synchronized void registerCells() {
        for (Block cell : OctetRegistry.cells()) {
            if (cell instanceof com.simpleoctets.block.MaterialOctetBlock octet) {
                Block base = octet.source();
                if (base != null && base != net.minecraft.world.level.block.Blocks.AIR) {
                    com.simplelib.api.OctetCells.register(base, cell, octet::piece);
                }
            }
        }
        if (!flammable) {
            flammable = true;
            OctetRegistry.registerFlammability();
        }
    }

    /** Ids of every cell with an item (woods, materials), independent of what is loaded - the full move table. */
    public static List<String> cellPaths() {
        List<String> out = new ArrayList<>();
        OctetRegistry.WOODS.forEach(wood -> out.add(wood + "_octet"));
        OctetMaterials.ALL.forEach(material -> out.add(material.key() + "_octet"));
        return out;
    }

    /** Recipe ids (stonecutter in, crafting back), each with its recipe advancement under recipes/building_blocks/. */
    public static List<String> recipePaths() {
        List<String> out = new ArrayList<>();
        for (String wood : OctetRegistry.WOODS) {
            out.add(wood + "_octet_from_" + wood + "_planks_stonecutting");
            out.add(wood + "_planks_from_octets");
        }
        for (OctetMaterials.Material material : OctetMaterials.ALL) {
            out.add(material.key() + "_octet_from_" + material.base() + "_stonecutting");
            out.add(material.base() + "_from_octets");
        }
        return out;
    }

    /** Old {@code simplebuilding:} ids resolve to the new ones (SimpleLib {@link IdMoves}; the next save writes the new id). */
    public static synchronized void registerIdMoves() {
        if (movesRegistered) return;
        movesRegistered = true;
        for (String path : cellPaths()) {
            IdMoves.move(Registries.BLOCK, sb(path), id(path));
            IdMoves.move(Registries.ITEM, sb(path), id(path));
        }
        IdMoves.move(Registries.BLOCK, sb("melon_octet"), id("melon_octet"));
        IdMoves.move(Registries.BLOCK, sb("mixed_octet"), id("mixed_octet"));
        IdMoves.move(Registries.BLOCK_ENTITY_TYPE, sb("mixed_octet"), id("mixed_octet"));
        for (String path : recipePaths()) {
            IdMoves.move(Registries.RECIPE, sb(path), id(path));
            IdMoves.move(Registries.ADVANCEMENT, sb("recipes/building_blocks/" + path), id("recipes/building_blocks/" + path));
        }
    }

    private SimpleOctets() {}
}
