package com.simplechess;

import com.simplelib.api.IdMoves;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Chess (N47 M7, docs/ai/PLAN-B-CHESS47-2026-11-14.md): chess pieces (13 colours, six kinds, 3D and flat) standing up to
 * four on a block, the twelve quartz checkers with stairs and slabs and - with Simple Octets - the chess octets, moved out of
 * the SimpleBuilding core with the same paths. Optional sub-mod of SimpleBuilding; needs only SimpleLib. Loader entry points
 * call {@link #init} once, before any registry event, so the id moves are known in time.
 */
public final class SimpleChess {
    public static final String MOD_ID = "simplechess";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Chess");
    /** The mod chess came from; old worlds hold its ids. */
    public static final String OLD_NAMESPACE = "simplebuilding";

    private static Predicate<String> loaded = id -> false;
    private static boolean movesRegistered;

    private SimpleChess() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Identifier old(String path) {
        return Identifier.fromNamespaceAndPath(OLD_NAMESPACE, path);
    }

    /** Super-mod switch "Enable Simple Chess" (framework SubMods, set by SimpleBuilding): off hides the tab entries. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID);
    }

    /** Whether another mod is loaded (the loader's mod list, set by {@link #init}). */
    public static boolean isLoaded(String modId) {
        return loaded.test(modId);
    }

    public static void init(Path configDir, Predicate<String> modLoaded) {
        loaded = modLoaded;
        registerIdMoves();
        // Recipe viewer page (SimpleLib, free of JEI classes): how octets and pieces are placed, swapped and picked up.
        com.simplelib.api.RecipeViewer.info(MOD_ID, SimpleChess::viewerPages);
    }

    private static List<com.simplelib.api.RecipeViewer.Info> viewerPages() {
        List<net.minecraft.world.item.ItemStack> items = new ArrayList<>();
        com.simplechess.registry.ChessItems.octets().forEach(i -> items.add(new net.minecraft.world.item.ItemStack(i)));
        com.simplechess.registry.ChessItems.pieces().values().forEach(i -> items.add(new net.minecraft.world.item.ItemStack(i)));
        if (items.isEmpty() || !on()) return List.of();
        String key = isLoaded("simpleoctets") ? "jei.simplechess.info.chess" : "jei.simplechess.info.chess_pieces";
        return List.of(new com.simplelib.api.RecipeViewer.Info(items, List.of(net.minecraft.network.chat.Component.translatable(key))));
    }

    /** Test centre station (framework contract, no import of the test centre): after the items are registered. */
    public static void registerStation() {
        List<String> items = new ArrayList<>();
        for (net.minecraft.world.item.Item item : com.simplechess.registry.ChessItems.all()) {
            items.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString());
        }
        items.add(MOD_ID + ":guide_book");
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "chess", "simplechess.testcentre.station", items);
    }

    /** Every moved block path (checkers, their stairs and slabs, the octet cell, the pieces cell). */
    public static List<String> movedBlocks() {
        List<String> out = new ArrayList<>(checkerPaths());
        out.add("checker_octet");
        out.add("chess_pieces");
        return out;
    }

    /** Every moved item path (checkers, stairs, slabs, octets, pieces). */
    public static List<String> movedItems() {
        List<String> out = new ArrayList<>(checkerPaths());
        for (ChessColor color : ChessColor.values()) {
            out.add(color.id() + "_octet");
            for (boolean flat : new boolean[]{false, true}) {
                for (ChessPiece piece : ChessPiece.values()) out.add(color.id() + "_chess_" + piece.id() + (flat ? "_flat" : ""));
            }
        }
        return out;
    }

    private static List<String> checkerPaths() {
        List<String> out = new ArrayList<>();
        for (ChessColor color : ChessColor.values()) {
            if (color.checkerId() == null) continue;
            out.add(color.checkerId());
            out.add(color.checkerId() + "_stairs");
            out.add(color.checkerId() + "_slab");
        }
        return out;
    }

    /** Moved recipes (the recipe book) -> folder of their unlock advancement {@code recipes/<folder>/<path>} (null: none). */
    public static Map<String, String> movedRecipes() {
        Map<String, String> out = new LinkedHashMap<>();
        for (ChessColor color : ChessColor.values()) {
            String source = color.checkerId() != null ? color.checkerId() : "quartz_block";
            out.put(color.id() + "_octet_from_" + source + "_stonecutting", "building_blocks");
            out.put(color.id() + "_octet_from_" + source + "_sawing", null);
            for (boolean flat : new boolean[]{false, true}) {
                for (ChessPiece piece : ChessPiece.values()) {
                    out.put(color.id() + "_chess_" + piece.id() + (flat ? "_flat" : "") + "_from_" + color.id() + "_octet_stonecutting", "decorations");
                }
            }
            String c = color.checkerId();
            if (c == null) continue;
            for (String path : List.of(c, c + "_stairs", c + "_slab", c + "_stairs_from_" + c + "_stonecutting", c + "_slab_from_" + c + "_stonecutting")) {
                out.put(path, "building_blocks");
            }
        }
        return out;
    }

    /** Old {@code simplebuilding:} ids resolve to the new ones (SimpleLib {@link IdMoves}; the next save writes the new id). */
    public static synchronized void registerIdMoves() {
        if (movesRegistered) return;
        movesRegistered = true;
        for (String path : movedBlocks()) IdMoves.move(Registries.BLOCK, old(path), id(path));
        for (String path : movedItems()) IdMoves.move(Registries.ITEM, old(path), id(path));
        IdMoves.move(Registries.BLOCK_ENTITY_TYPE, old("chess_pieces"), id("chess_pieces"));
        movedRecipes().forEach((path, folder) -> {
            IdMoves.move(Registries.RECIPE, old(path), id(path));
            if (folder != null) IdMoves.move(Registries.ADVANCEMENT, old("recipes/" + folder + "/" + path), id("recipes/" + folder + "/" + path));
        });
        IdMoves.move(Registries.ADVANCEMENT, old("building/checkmate"), id("building/checkmate"));
    }
}
