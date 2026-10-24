package com.simpletrims;

import com.simplelib.api.IdMoves;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module constants. Loader entry points call {@link #init} once in their constructor / initializer with their config
 * directory - before any registry event, so the id moves out of SimpleBuilding are known in time (Forge aliases).
 */
public final class SimpleTrims {
    public static final String MOD_ID = "simpletrims";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Trims");
    /** The mod the trim templates came from (N18 F1: ids move to {@code simpletrims:} with world migration). */
    public static final String OLD_NAMESPACE = "simplebuilding";

    /** Items that moved (paths are unchanged). */
    public static final List<String> MOVED_ITEMS = List.of("glowing_trim_template", "emitting_trim_template", "pulsating_trim_template");
    /** Item components that moved. */
    public static final List<String> MOVED_COMPONENTS = List.of("glow_level", "visual_glow", "light_source", "pulsating");
    /** Recipes that moved (recipe book). */
    public static final List<String> MOVED_RECIPES = List.of("pulsating_trim_template", "glowing_trim_template_duplication",
            "emitting_trim_template_duplication", "pulsating_trim_template_duplication");
    /** Advancements: old path in SimpleBuilding's hammer tab -> new path here. */
    public static final List<String[]> MOVED_ADVANCEMENTS = List.of(new String[] {"hammer/glow_up", "glow_up"},
            new String[] {"hammer/bright_idea", "bright_idea"}, new String[] {"hammer/full_radiance", "full_radiance"});

    private static boolean movesRegistered;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Super-mod switch "Enable Simple Trims" (framework SubMods, mirrored by SimpleBuilding) and the own config: off = inert. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID) && com.simpletrims.config.TrimsConfig.enabled;
    }

    public static void init(java.nio.file.Path configDir) {
        com.simpletrims.config.TrimsConfig.load(configDir);
        // CMD24: /simpletrims config for every option.
        com.simplelib.api.config.ConfigCommands.server(MOD_ID).json(com.simpletrims.config.TrimsConfig::toJson, com.simpletrims.config.TrimsConfig::apply)
                .file(configDir, com.simpletrims.config.TrimsConfig.FILE).register();
        registerIdMoves();
        com.simpletrims.placed.PlateHint.register();
    }

    /** Old {@code simplebuilding:} ids resolve to the new ones (SimpleLib {@link IdMoves}; the next save writes the new id). */
    public static synchronized void registerIdMoves() {
        if (movesRegistered) return;
        movesRegistered = true;
        for (String path : MOVED_ITEMS) IdMoves.move(Registries.ITEM, old(path), id(path));
        for (String path : MOVED_COMPONENTS) IdMoves.move(Registries.DATA_COMPONENT_TYPE, old(path), id(path));
        for (String path : MOVED_RECIPES) IdMoves.move(Registries.RECIPE, old(path), id(path));
        for (String[] advancement : MOVED_ADVANCEMENTS) IdMoves.move(Registries.ADVANCEMENT, old(advancement[0]), id(advancement[1]));
    }

    public static Identifier old(String path) {
        return Identifier.fromNamespaceAndPath(OLD_NAMESPACE, path);
    }

    private SimpleTrims() {}
}
