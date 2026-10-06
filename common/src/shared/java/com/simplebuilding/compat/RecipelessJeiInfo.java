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
        if (com.simplebuilding.version.McVersion.CRUCIBLE) {
            // Crucible P5: gefuellte Eimer entstehen nur durch Schoepfen (kein Rezept).
            map.put("soul_lava_bucket", List.of(com.simplebuilding.fluid.ModFluids.SOUL_LAVA_BUCKET, com.simplebuilding.fluid.ModFluids.ENDERITE_SOUL_LAVA_BUCKET));
            map.put("copper_bucket_filled", List.of(com.simplebuilding.fluid.ModFluids.COPPER_WATER_BUCKET, com.simplebuilding.fluid.ModFluids.COPPER_LAVA_BUCKET));
            map.put("enderite_crucible", List.of(com.simplebuilding.crucible.CrucibleCompat.enderiteCrucible(), com.simplebuilding.crucible.CrucibleCompat.enderiteBarrel()));
            map.put("enderite_bucket_filled", List.of(com.simplebuilding.fluid.ModFluids.ENDERITE_WATER_BUCKET, com.simplebuilding.fluid.ModFluids.ENDERITE_LAVA_BUCKET));
        }
        if (com.simplebuilding.version.McVersion.DIMENSIONAL_SCRAP) {
            map.put("dimensional_scrap", List.of(ModBlocks.DIMENSIONAL_SCRAP, ModBlocks.NETHER_DIMENSIONAL_SCRAP, ModBlocks.END_DIMENSIONAL_SCRAP));
        }
        return map;
    }

    /** Usage hints for craftable items, separate from the recipeless coverage contract. */
    public static Map<String, List<ItemLike>> supplementalPages() {
        Map<String, List<ItemLike>> map = new LinkedHashMap<>();
        if (com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            map.put("hammer_fire_chips", List.of(ModItems.FIRE_CHIP, net.minecraft.world.item.Items.FIRE_CHARGE));
            map.put("hammer_ice_chips", List.of(ModItems.ICE_CHIP, net.minecraft.world.item.Items.ICE, net.minecraft.world.item.Items.PACKED_ICE));
            map.put("hammer_obsidian_chips", List.of(ModItems.OBSIDIAN_CHIP, net.minecraft.world.item.Items.OBSIDIAN));
        }
        map.put("furnace_tiers", List.of(ModBlocks.REINFORCED_FURNACE, ModBlocks.NETHERITE_FURNACE, ModBlocks.ENDERITE_FURNACE,
                ModBlocks.REINFORCED_SMOKER, ModBlocks.NETHERITE_SMOKER, ModBlocks.ENDERITE_SMOKER,
                ModBlocks.REINFORCED_BLAST_FURNACE, ModBlocks.NETHERITE_BLAST_FURNACE, ModBlocks.ENDERITE_BLAST_FURNACE));
        if (com.simplebuilding.version.McVersion.GADGET_REWORK) {
            map.put("velocity_gauge", List.of(ModItems.VELOCITY_GAUGE));
        }
        if (com.simplebuilding.version.McVersion.CRUCIBLE) {
            map.put("copper_bucket", List.of(com.simplebuilding.fluid.ModFluids.COPPER_BUCKET));
            map.put("enderite_bucket", List.of(com.simplebuilding.fluid.ModFluids.ENDERITE_BUCKET));
        }
        if (com.simplebuilding.version.McVersion.SILENT_DANDELION) {
            map.put("silent_dandelion", List.of(ModItems.SILENT_DANDELION));
        }
        if (com.simplebuilding.version.McVersion.CHESS) {
            // Schach: wie Achtel und Figuren gesetzt, getauscht und aufgenommen werden.
            List<ItemLike> chess = new java.util.ArrayList<>(com.simplebuilding.chess.ChessItems.octets());
            chess.addAll(com.simplebuilding.chess.ChessItems.pieces().values());
            map.put("chess", chess);
        }
        // Pfeile: seit 2026-10-02 mit Befiederungsrezepten (nur fuers Vanilla-Rezeptbuch), JEI zeigt sie nicht - Hinweis bleibt.
        if (com.simplebuilding.version.McVersion.FLETCHING) {
            map.put("crafted_arrow", List.of(ModItems.CRAFTED_ARROW));
        }
        if (com.simplebuilding.version.McVersion.END_SYSTEMS) {
            map.put("end_signals", List.of(ModItems.NIHIL_REDSTONE, ModItems.ASTRAL_REDSTONE, ModItems.NIHILITH_SWITCH, ModItems.ASTRALIT_SWITCH, ModItems.NIHILITH_LAMP, ModItems.ASTRALIT_LAMP));
            map.put("astral_vault", List.of(ModItems.ASTRAL_VAULT));
            map.put("nihil_vault", List.of(ModItems.NIHIL_VAULT));
            map.put("end_pistons", List.of(ModItems.ASTRAL_PISTON, ModItems.NIHIL_PISTON));
        }
        if (com.simplebuilding.version.McVersion.END_RAILS) {
            map.put("end_rails", List.of(ModItems.ASTRAL_RAIL, ModItems.NIHIL_RAIL));
        }
        if (com.simplebuilding.version.McVersion.STANDING_RODS) {
            // Aufgestellte Staebe (2026-10-04): Schleichen + Rechtsklick auf eine Oberseite stellt sie senkrecht auf.
            map.put("standing_rods", List.of(net.minecraft.world.item.Items.STICK, net.minecraft.world.item.Items.BONE,
                    net.minecraft.world.item.Items.BLAZE_ROD, net.minecraft.world.item.Items.BREEZE_ROD, ModItems.DIAMOND_ROD));
        }
        if (com.simplebuilding.version.McVersion.MUSIC_DISCS) {
            // Fundorte und B-Seite der Platten (2026-10-03), Wirkung der Lautsprecher.
            map.put("music_discs", List.<ItemLike>copyOf(com.simplebuilding.util.MusicDiscs.items()));
            map.put("amplifiers", List.of(ModItems.JUKEBOX_AMPLIFIER, ModItems.NOTE_AMPLIFIER));
        }
        return map;
    }
}
