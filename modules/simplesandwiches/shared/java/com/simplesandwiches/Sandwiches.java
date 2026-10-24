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

    /**
     * Simple End loaded (set by the loader entry before registration): boards of its Ashwood and Duskwood, too
     * (docs/ai/PLAN-ENDWOOD-FULL-2026-10-12.md). Only its public ids are used, never its classes.
     */
    public static boolean simpleEnd;

    /** Super-mod switch "Enable Simple Sandwiches" (framework SubMods): off = registered but inert (no tab entries, no knife/board/bundle/cauldron play). */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID);
    }

    public static void init(java.nio.file.Path configDir) {
        com.simplesandwiches.config.SandwichConfig.load(configDir);
        SandwichesViewer.register(); // JEI24: milk, knife and board ways in the recipe viewer (no JEI class)
    }

    private Sandwiches() {}
}
