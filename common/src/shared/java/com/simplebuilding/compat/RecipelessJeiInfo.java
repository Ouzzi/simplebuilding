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
 * ist per {@code c:hidden_from_recipe_viewers} aus JEI ausgeblendet.
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
        map.put("legacy_spatula", List.of(ModItems.STONE_SPATULA, ModItems.COPPER_SPATULA, ModItems.IRON_SPATULA,
                ModItems.GOLD_SPATULA, ModItems.DIAMOND_SPATULA, ModItems.NETHERITE_SPATULA));
        return map;
    }
}
