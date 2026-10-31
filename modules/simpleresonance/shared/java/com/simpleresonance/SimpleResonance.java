package com.simpleresonance;

import com.simpleresonance.config.ResonanceConfig;
import java.nio.file.Path;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module constants (WP-22, docs/ai/PLAN-A-ZONES31-2026-10-30.md). Loader entry points call {@link #init} once with their
 * config directory; {@link ResonanceService#install} registers the contract for other mods.
 */
public final class SimpleResonance {
    public static final String MOD_ID = "simpleresonance";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Resonance");

    private SimpleResonance() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Super-mod switch "Enable Simple Resonance" (framework SubMods, mirrored by SimpleBuilding) and the own switch. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID) && ResonanceConfig.enabled;
    }

    public static void init(Path configDir) {
        ResonanceConfig.load(configDir);
        ResonanceService.install();
        // Test centre station (framework contract, no import of the test centre): the module's items by id.
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "resonance", "simpleresonance.testcentre.station",
                java.util.List.of("simpleresonance:guide_book"));
    }
}
