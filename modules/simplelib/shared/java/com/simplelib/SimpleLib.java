package com.simplelib;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SimpleLib: content shared by several Simple mods (principle 6a, bundled per Jar-in-Jar). It knows
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
    }

    private SimpleLib() {}
}
