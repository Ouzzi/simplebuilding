package com.simplelib;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SimpleLib: content shared by several Simple mods (warm food, shared materials, id moves, in-world upgrades, UI; principle 6a, bundled per Jar-in-Jar). It knows
 * none of its users; partners talk to it only through {@code com.simplelib.api}. Loader entry
 * points call {@link #init} once with their config directory.
 */
public final class SimpleLib {
    public static final String MOD_ID = "simplelib";
    public static final Logger LOG = LoggerFactory.getLogger("SimpleLib");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void init(java.nio.file.Path configDir) {
        com.simplelib.config.LibConfig.load(configDir);
        com.simplelib.api.client.ui.UiBrightness.load(configDir); // N49: local look, plain file (no client classes)
        // CMD24: /simplelib config for the own keys of simplelib-server.json (the file keeps keys of other owners).
        com.simplelib.api.config.ConfigCommands.server(MOD_ID).json(com.simplelib.config.LibConfig::toJson, com.simplelib.config.LibConfig::apply)
                .keys(com.simplelib.config.LibConfig.KEYS).save(com.simplelib.config.LibConfig::save).register();
        com.simplelib.test.IdMovesTests.registerTestMoves();
    }

    private SimpleLib() {}
}
