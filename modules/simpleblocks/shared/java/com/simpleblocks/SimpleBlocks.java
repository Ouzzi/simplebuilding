package com.simpleblocks;

import com.simpleblocks.nature.NatureBlocks;
import com.simplelib.api.IdMoves;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Blocks (docs/ai/KONZEPT-MODREF-BLOECKE-2026-10-16.md, owner answers section 6): the missing building blocks.
 * Everything is built on Vanilla blocks only, so the module works on its own. The loaders register through
 * {@link BlocksRegistry}. Since 1.0.0 (docs/ai/PLAN-B-BLOCKSMOVE-2026-10-18.md) it also holds SimpleBuilding's building
 * blocks without a home of their own ({@link NatureBlocks}) and is a required sub-mod of SimpleBuilding; loaders call
 * {@link #registerIdMoves} in their constructor / initializer, before any registry event (Forge aliases).
 */
public final class SimpleBlocks {
    public static final String MOD_ID = "simpleblocks";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Blocks");

    /** The mod the moved building blocks came from; old worlds hold its ids. */
    public static final String OLD_NAMESPACE = "simplebuilding";
    /** Set by the loader entry: SimpleBuilding is loaded (its search-tab chains then place the moved blocks in the Vanilla tabs). */
    public static boolean simplebuildingLoaded;

    private static boolean movesRegistered;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Identifier old(String path) {
        return Identifier.fromNamespaceAndPath(OLD_NAMESPACE, path);
    }

    /**
     * Old {@code simplebuilding:} ids of the moved blocks resolve to the ones here (SimpleLib {@link IdMoves}: blocks in
     * chunks, items, the rising block entity, recipe book, recipe advancements; the next save writes the new id).
     */
    public static synchronized void registerIdMoves() {
        if (movesRegistered) return;
        movesRegistered = true;
        for (String path : NatureBlocks.NAMES) {
            IdMoves.move(Registries.BLOCK, old(path), id(path));
            IdMoves.move(Registries.ITEM, old(path), id(path));
        }
        IdMoves.move(Registries.ENTITY_TYPE, old(NatureBlocks.LEVITATING_ENTITY), id(NatureBlocks.LEVITATING_ENTITY));
        NatureBlocks.MOVED_RECIPES.forEach((path, folder) -> {
            IdMoves.move(Registries.RECIPE, old(path), id(path));
            IdMoves.move(Registries.ADVANCEMENT, old("recipes/" + folder + "/" + path), id("recipes/" + folder + "/" + path));
        });
    }

    /**
     * Super-mod switch "Enable Simple Blocks" (framework SubMods, mirrored by SimpleBuilding). Off hides the module's
     * creative tab and its entries in the Vanilla tabs; placed blocks stay, recipes and loot go on (plain data).
     */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID);
    }

    /** Test centre station (framework contract, no import of the test centre): the module's items by id. */
    static void registerStation() {
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "blocks", "simpleblocks.testcentre.station",
                java.util.stream.Stream.of(BlocksRegistry.NAMES.stream(), NatureBlocks.NAMES.stream(), java.util.stream.Stream.of(com.simpleblocks.saw.Sawing.NAME, "guide_book")).flatMap(s -> s).map(n -> MOD_ID + ":" + n).toList());
    }

    private SimpleBlocks() {}
}
