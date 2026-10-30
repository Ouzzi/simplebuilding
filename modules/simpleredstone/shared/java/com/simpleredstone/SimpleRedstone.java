package com.simpleredstone;

import com.simpleredstone.config.RedstoneConfig;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Redstone (WP-17, docs/ai/KONZEPT-A-WP17-2026-10-29.md, owner choice ABN29-3): the Double Piston (1x2, signal 1-7 reaches
 * one block, 8-15 two) and the Signal Dampener (four stages, takes 0-3 off the signal). Vanilla ingredients only, runs alone.
 * The loader entry point calls {@link #init} once with its config directory before it registers anything.
 */
public final class SimpleRedstone {
    public static final String MOD_ID = "simpleredstone";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Redstone");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Super-mod switch "Enable Simple Redstone" (framework SubMods, set by SimpleBuilding): off = no tab entries, no guide. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID);
    }

    /** Feature switch {@code features.doublePiston}: off = the Double Piston stays put (placed blocks keep standing). */
    public static boolean doublePistonOn() {
        return on() && RedstoneConfig.doublePiston;
    }

    /** Feature switch {@code features.signalDampener}: off = the Dampener outputs nothing. */
    public static boolean signalDampenerOn() {
        return on() && RedstoneConfig.signalDampener;
    }

    public static void init(Path configDir) {
        RedstoneConfig.load(configDir);
        // Test centre station (framework contract, no import of the test centre): the module's items by id.
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "redstone", "simpleredstone.testcentre.station",
                List.of(MOD_ID + ":double_piston", MOD_ID + ":signal_dampener", MOD_ID + ":guide_book"));
    }

    private SimpleRedstone() {}
}
