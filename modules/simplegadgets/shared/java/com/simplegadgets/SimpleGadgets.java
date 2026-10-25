package com.simplegadgets;

import com.simplelib.api.IdMoves;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Gadgets (OW24-2, docs/ai/PLAN-B-GADGETS24-2026-10-24.md; family plan 2026-10-16): the gadgets you hold and use -
 * measure, turn, pull, locate - moved out of SimpleBuilding stage by stage. Stage G1: the Gauge ({@code velocity_gauge}); stage G2: the Transposer ({@code rotator}) and the Resonance Rod ({@code amethyst_lens}).
 * The loader entry point calls {@link #init} once before it registers anything, so the id moves out of SimpleBuilding are
 * known in time.
 */
public final class SimpleGadgets {
    public static final String MOD_ID = "simplegadgets";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Gadgets");
    /** The mod the gadgets came from; old worlds hold its ids. */
    public static final String OLD_NAMESPACE = "simplebuilding";
    /** The moved items with their recipe unlock folder ({@code recipes/<folder>/<path>}), in tab order. */
    public static final List<String> MOVED_ITEMS = List.of("velocity_gauge", "rotator");

    private static boolean movesRegistered;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Identifier old(String path) {
        return Identifier.fromNamespaceAndPath(OLD_NAMESPACE, path);
    }

    /** Super-mod switch "Enable Simple Gadgets" (framework SubMods, set by SimpleBuilding): off = no tab entries, no guide. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID);
    }

    public static void init(java.nio.file.Path configDir) {
        com.simplegadgets.config.GadgetsConfig.load(configDir);
        registerIdMoves();
        com.simplegadgets.rotator.RotatorItem.publishFacts();
        // Test centre station (framework contract, no import of the test centre): the module's items by id.
        List<String> items = new ArrayList<>();
        MOVED_ITEMS.forEach(path -> items.add(MOD_ID + ":" + path));
        items.add(MOD_ID + ":guide_book");
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "gadgets", "simplegadgets.testcentre.station", items);
    }

    /** Old {@code simplebuilding:} ids resolve to the new ones (SimpleLib {@link IdMoves}; the next save writes the new id). */
    public static synchronized void registerIdMoves() {
        if (movesRegistered) return;
        movesRegistered = true;
        for (String path : MOVED_ITEMS) {
            IdMoves.move(Registries.ITEM, old(path), id(path));
            IdMoves.move(Registries.RECIPE, old(path), id(path));
            IdMoves.move(Registries.ADVANCEMENT, old("recipes/tools/" + path), id("recipes/tools/" + path));
        }
    }

    private SimpleGadgets() {}
}
