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
 * ingredients). Stage 2 (b-arrows03, 04.11., docs/ai/PLAN-B-ARROWS03-2026-11-04.md): the Fletching Table and its crafted arrows
 * from the SimpleBuilding core ({@code com.simplearrows.fletching}, ids moved). Runs alone. The loader entry point calls {@link #init} once with its config directory before it registers anything.
 */
public final class SimpleArrows {
    public static final String MOD_ID = "simplearrows";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Arrows");
    /** Where the Silver Arrow lived until 03.11.2026 (a-ores31). */
    public static final String ORES_NAMESPACE = "simpleores";
    /** Where the Fletching Table and its arrows lived until 04.11.2026. */
    public static final String SB_NAMESPACE = "simplebuilding";

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
        // Diamond and Netherite tips: SimpleLib's shared pebble and nugget (recipes and anvil crushing only once requested).
        com.simplelib.api.SimpleLibApi.requestMaterial("cracked_diamond");
        com.simplelib.api.SimpleLibApi.requestMaterial("netherite_nugget");
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "arrows", "simplearrows.testcentre.station",
                List.of(MOD_ID + ":silver_arrow", MOD_ID + ":light_arrow", MOD_ID + ":crafted_arrow", MOD_ID + ":guide_book"));
    }

    /**
     * {@code simpleores:silver_arrow} (item, entity type, recipe, recipe advancement) resolves to {@code simplearrows:silver_arrow};
     * the Fletching Table's {@code simplebuilding:} ids (crafted arrow item and entity, arrow parts component, menu, recipe type and
     * serializer, three recipe book categories, the part recipes and their advancement) resolve to {@code simplearrows:}.
     */
    public static synchronized void registerIdMoves() {
        if (movesRegistered) return;
        movesRegistered = true;
        move(ORES_NAMESPACE, Registries.ITEM, "silver_arrow");
        move(ORES_NAMESPACE, Registries.ENTITY_TYPE, "silver_arrow");
        move(ORES_NAMESPACE, Registries.RECIPE, "silver_arrow");
        move(ORES_NAMESPACE, Registries.ADVANCEMENT, "recipes/combat/silver_arrow");
        move(SB_NAMESPACE, Registries.ITEM, com.simplearrows.fletching.Fletchings.CRAFTED_ARROW_ID);
        move(SB_NAMESPACE, Registries.ENTITY_TYPE, com.simplearrows.fletching.Fletchings.CRAFTED_ARROW_ID);
        move(SB_NAMESPACE, Registries.DATA_COMPONENT_TYPE, "arrow_parts");
        move(SB_NAMESPACE, Registries.MENU, com.simplearrows.fletching.FletchingRecipes.ID);
        move(SB_NAMESPACE, Registries.RECIPE_SERIALIZER, com.simplearrows.fletching.FletchingRecipes.ID);
        move(SB_NAMESPACE, Registries.RECIPE_TYPE, com.simplearrows.fletching.FletchingRecipes.ID);
        for (com.simplearrows.fletching.FletchingRecipe.Kind kind : com.simplearrows.fletching.FletchingRecipe.Kind.values()) {
            move(SB_NAMESPACE, Registries.RECIPE_BOOK_CATEGORY, com.simplearrows.fletching.FletchingRecipes.categoryId(kind));
        }
        for (String path : com.simplearrows.fletching.Fletchings.recipePaths()) {
            move(SB_NAMESPACE, Registries.RECIPE, path);
        }
        move(SB_NAMESPACE, Registries.ADVANCEMENT, "recipes/combat/fletching_table_parts");
    }

    private static void move(String from, net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<?>> registry, String path) {
        IdMoves.move(registry, Identifier.fromNamespaceAndPath(from, path), id(path));
    }

    private SimpleArrows() {}
}
