package com.simpleslabs;

import com.simpleslabs.registry.HalfSlabMaterials;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Slabs (N86, owner 18.11.; docs/ai/PLAN-N86-BAU-2026-11-19.md): a 2-px half slab of every block with a slab, freely
 * stacked in the eight layers of a cell, plus the side half slab / side slab. Shares SimpleLib's shape core with Simple Octets
 * (com.simplelib.api.shape.ShapeMaterials: property templates of other mods' blocks, fire table; CopperAging). Loader entry
 * points call {@link #init} once, before registering.
 */
public final class SimpleSlabs {
    public static final String MOD_ID = "simpleslabs";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Slabs");

    private static Predicate<String> loaded = id -> false;

    private SimpleSlabs() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Super-mod switch "Enable Simple Slabs" (framework SubMods, set by SimpleBuilding); off = empty creative tab. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID);
    }

    /** Whether another mod is loaded (the loader's mod list, set by {@link #init}). */
    public static boolean isLoaded(String modId) {
        return loaded.test(modId);
    }

    public static void init(Predicate<String> modLoaded) {
        loaded = modLoaded;
        // Test centre station (framework contract): every half slab item by id (missing ones are skipped there).
        List<String> items = new ArrayList<>();
        HalfSlabMaterials.ALL.forEach(m -> items.add(MOD_ID + ":" + m.id()));
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "half_slabs", "simpleslabs.testcentre.station", items);
    }
}
