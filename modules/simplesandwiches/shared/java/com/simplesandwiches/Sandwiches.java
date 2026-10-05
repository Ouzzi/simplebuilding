package com.simplesandwiches;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Module constants. Loader entry points call {@link #init} once with their config directory. */
public final class Sandwiches {
    public static final String MOD_ID = "simplesandwiches";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Sandwiches");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void init(java.nio.file.Path configDir) {
        com.simplesandwiches.config.SandwichConfig.load(configDir);
    }

    private Sandwiches() {}
}
