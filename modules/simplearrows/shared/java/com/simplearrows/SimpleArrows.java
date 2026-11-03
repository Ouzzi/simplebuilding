package com.simplearrows;

import com.simplearrows.config.ArrowsConfig;
import com.simplelib.api.IdMoves;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Arrows (docs/ai/KONZEPT-B-ARROWS01-2026-11-01.md, plan docs/ai/PLAN-B-ARROWS02-2026-11-03.md): a required sub-mod of
 * SimpleBuilding for arrows. Stage 1: the Silver Arrow (from Simple Ores, crafted only with its silver) and the Light Arrow (Vanilla
 * ingredients). Runs alone. The loader entry point calls {@link #init} once with its config directory before it registers anything.
 */
public final class SimpleArrows {
    public static final String MOD_ID = "simplearrows";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Arrows");
    /** Where the Silver Arrow lived until 03.11.2026 (a-ores31). */
    public static final String ORES_NAMESPACE = "simpleores";

    private static boolean movesRegistered;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Super-mod switch "Enable Simple Arrows" (framework SubMods, set by SimpleBuilding): off = no tab entries. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID);
    }

    public static void init(Path configDir) {
        if (configDir != null) {
            // The bonus was Simple Ores' option until 03.11.; a new file takes it over once (the old file stays).
            IdMoves.adoptConfig(configDir, ArrowsConfig.FILE, "simpleores-server.json", List.of("silverArrowUndeadBonus"));
        }
        ArrowsConfig.load(configDir);
        registerIdMoves();
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "arrows", "simplearrows.testcentre.station",
                List.of(MOD_ID + ":silver_arrow", MOD_ID + ":light_arrow"));
    }

    /** {@code simpleores:silver_arrow} (item, entity type, recipe, recipe advancement) resolves to {@code simplearrows:silver_arrow}. */
    public static synchronized void registerIdMoves() {
        if (movesRegistered) return;
        movesRegistered = true;
        move(Registries.ITEM, "silver_arrow");
        move(Registries.ENTITY_TYPE, "silver_arrow");
        move(Registries.RECIPE, "silver_arrow");
        move(Registries.ADVANCEMENT, "recipes/combat/silver_arrow");
    }

    private static void move(net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<?>> registry, String path) {
        IdMoves.move(registry, Identifier.fromNamespaceAndPath(ORES_NAMESPACE, path), id(path));
    }

    private SimpleArrows() {}
}
