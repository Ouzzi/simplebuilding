package com.simplefoundry;

import com.simplefoundry.config.FoundryConfig;
import java.nio.file.Path;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module constants (concept docs/ai/KONZEPT-A-WP23-2026-10-29.md, plan docs/ai/PLAN-B-FOUNDRY32-2026-10-31.md, owner ABN30-13).
 * The Fabric entry point calls {@link #init} once with the config directory, before registering.
 */
public final class SimpleFoundry {
    public static final String MOD_ID = "simplefoundry";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Foundry");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Super-mod switch "Enable Simple Foundry" (framework SubMods, set by Simple Ovens) and the own config. Off = no new melting, creative entries empty. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID) && FoundryConfig.enabled;
    }

    public static void init(Path configDir) {
        FoundryConfig.load(configDir);
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "foundry", "simplefoundry.testcentre.station", FoundryRegistry.itemIds());
    }

    private SimpleFoundry() {}
}
