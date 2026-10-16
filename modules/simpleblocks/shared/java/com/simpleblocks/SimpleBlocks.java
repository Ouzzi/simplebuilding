package com.simpleblocks;

import java.util.List;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Blocks (docs/ai/KONZEPT-MODREF-BLOECKE-2026-10-16.md, owner answers section 6): the missing building blocks.
 * Everything is built on Vanilla blocks only, so the module works on its own. The loaders register through
 * {@link BlocksRegistry}.
 */
public final class SimpleBlocks {
    public static final String MOD_ID = "simpleblocks";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Blocks");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
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
                java.util.stream.Stream.concat(BlocksRegistry.NAMES.stream(), java.util.stream.Stream.of("guide_book")).map(n -> MOD_ID + ":" + n).toList());
    }

    private SimpleBlocks() {}
}
