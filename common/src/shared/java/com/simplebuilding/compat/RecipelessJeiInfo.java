package com.simplebuilding.compat;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.items.ModItems;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.ItemLike;

/**
 * JEI-Infoseiten fuer Gegenstaende ohne JEI-sichtbares Rezept (kein Crafting-, Schmiede-,
 * Steinmetz- oder Ofenrezept und keine In-World-Kategorie aus {@link InWorldRecipeCatalog}):
 * Seite -> Gegenstaende. Der JEI-Plugin zeigt fuer jede Seite den Text
 * {@code jei.simplebuilding.info.<seite>} an allen ihren Gegenstaenden. Ohne JEI-Abhaengigkeit, damit
 * ein Server-Gametest ({@code DataIntegrityTests#everyRecipelessModItemHasJeiInfo}) die Liste
 * gegen die Rezepte des laufenden Spiels halten kann.
 *
 * <p>Die rezeptlosen Simple-Tweaks-Gegenstaende (Leih-Elytra, verwitterte Kupferplatten) haben
 * ihre Seite schon ueber {@link com.simplebuilding.tweaks.item.TweaksJeiInfo}; der Kreativ-Platzhalter
 * und die sechs alten Spachtel (seit 2026-09-28, vorher mit eigener Infoseite) sind per
 * {@code c:hidden_from_recipe_viewers} aus JEI, REI und EMI ausgeblendet.
 */
public final class RecipelessJeiInfo {
    public static final String KEY_PREFIX = "jei.simplebuilding.info.";

    private RecipelessJeiInfo() {
    }

    public static Map<String, List<ItemLike>> pages() {
        Map<String, List<ItemLike>> map = new LinkedHashMap<>();
        map.put("astralit_ore", List.of(ModBlocks.ASTRALIT_ORE));
        map.put("nihilith_ore", List.of(ModBlocks.NIHILITH_ORE));
        map.put("astralit_dust", List.of(ModItems.ASTRALIT_DUST));
        map.put("nihilith_shard", List.of(ModItems.NIHILITH_SHARD));
        map.put("enchanted_netherite_apple", List.of(ModItems.ENCHANTED_NETHERITE_APPLE));
        map.put("enchanted_enderite_apple", List.of(ModItems.ENCHANTED_ENDERITE_APPLE));
        if (com.simplebuilding.version.McVersion.SAGE_ORE) {
            map.put("sage_ore", List.of(ModBlocks.SAGE_ORE, ModBlocks.DEEPSLATE_SAGE_ORE));
            map.put("sage_orb", List.of(ModItems.SAGE_ORB));
        }
        if (com.simplebuilding.version.McVersion.DIMENSIONAL_SCRAP) {
            map.put("dimensional_scrap", List.of(ModBlocks.DIMENSIONAL_SCRAP, ModBlocks.NETHER_DIMENSIONAL_SCRAP, ModBlocks.END_DIMENSIONAL_SCRAP));
        }
        return map;
    }

    /** Usage hints for craftable items, separate from the recipeless coverage contract. */
    public static Map<String, List<ItemLike>> supplementalPages() {
        Map<String, List<ItemLike>> map = new LinkedHashMap<>();
        if (com.simplebuilding.version.McVersion.GADGET_REWORK) {
            map.put("velocity_gauge", List.of(ModItems.VELOCITY_GAUGE));
        }
        if (com.simplebuilding.version.McVersion.SILENT_DANDELION) {
            map.put("silent_dandelion", List.of(ModItems.SILENT_DANDELION));
        }
        // Pfeile: seit 2026-10-02 mit Befiederungsrezepten (nur fuers Vanilla-Rezeptbuch), JEI zeigt sie nicht - Hinweis bleibt.
        if (com.simplebuilding.version.McVersion.FLETCHING) {
            map.put("crafted_arrow", List.of(ModItems.CRAFTED_ARROW));
        }
        if (com.simplebuilding.version.McVersion.END_SYSTEMS) {
            map.put("end_signals", List.of(ModItems.NIHIL_REDSTONE, ModItems.ASTRAL_REDSTONE, ModItems.NIHILITH_SWITCH, ModItems.ASTRALIT_SWITCH, ModItems.NIHILITH_LAMP, ModItems.ASTRALIT_LAMP));
            map.put("astral_vault", List.of(ModItems.ASTRAL_VAULT));
        }
        return map;
    }
}
