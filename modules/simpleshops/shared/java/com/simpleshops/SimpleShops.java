package com.simpleshops;

import com.simpleshops.config.ShopsConfig;
import java.nio.file.Path;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module constants (concept docs/ai/KONZEPT-SHOP-VITRINE-2026-10-14.md, owner answers section 7).
 * Loader entry points call {@link #init} once with their config directory, before any registry event.
 */
public final class SimpleShops {
    public static final String MOD_ID = "simpleshops";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Shops");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /**
     * Super-mod switch "Enable Simple Shops" (framework SubMods, mirrored by SimpleBuilding) and the own config. Off = trading,
     * menus and new placements are inert; the protection of placed shops (break, explosions, pistons) stays.
     */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID) && ShopsConfig.enabled;
    }

    public static void init(Path configDir) {
        ShopsConfig.load(configDir);
        registerMapLayer();
        // Test centre station (framework contract, no import of the test centre): the module's items by id.
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "shops", "simpleshops.testcentre.station",
                java.util.List.of("simpleshops:sales_block", "simpleshops:display_case", "simpleshops:glow_display_case",
                        "simpleshops:guide_book"));
    }

    /** Wayfinder map layer (framework contract, no import of the map module): active shops of loaded chunks with their offer and price. */
    private static void registerMapLayer() {
        com.simplebuilding.framework.api.MapLayerSources.register(MOD_ID, "shops", "layer.simpleshops.shops", ShopMapLayer::collect);
    }

    private SimpleShops() {}
}
