package com.simplebuilding.modules.simplevault;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Vault - "Grundgewölbe" (UNDER21, owner 10.11.: own mod, plan docs/ai/PLAN-B-UNDER21-2026-11-10.md): the Bedrock Vault, a
 * dimension of solid bedrock with one quiet complex of halls per 128 x 128 region, reached through a rare gap in the Overworld
 * bedrock, left only through its riddles and the Ascent Altar. Stand-alone (requires SimpleLib only); the loader entry calls
 * {@link #init} once.
 */
public final class SimpleVault {
    public static final String MOD_ID = "simplevault";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Vault");

    private SimpleVault() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Super-mod switch "Enable Simple Vault" (framework SubMods, set by SimpleBuilding): off = no tab entries, no travel. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID);
    }

    /** Config, config command and test centre station; before the registrations. */
    public static void init(Path configDir) {
        VaultConfig.load(configDir);
        com.simplelib.api.config.ConfigCommands.server(MOD_ID).json(VaultConfig::toRootJson, VaultConfig::applyRoot)
                .file(configDir, VaultConfig.FILE).register();
        List<String> items = new ArrayList<>();
        for (String path : VaultRegistry.ITEM_PATHS) items.add(MOD_ID + ":" + path);
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "main", "simplevault.testcentre.station", items);
    }
}
