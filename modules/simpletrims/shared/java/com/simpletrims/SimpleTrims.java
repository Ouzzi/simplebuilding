package com.simpletrims;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Module constants. Loader entry points call {@link #init} once with their config directory. */
public final class SimpleTrims {
    public static final String MOD_ID = "simpletrims";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Trims");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Super-mod switch "Enable Simple Trims" (framework SubMods): off = inert (no template-tool recognition). */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID);
    }

    public static void init(java.nio.file.Path configDir) {
        com.simpletrims.config.TrimsConfig.load(configDir);
    }

    private SimpleTrims() {}
}
