package com.simplebuilding.recipe;

import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.config.ServerTuningConfig;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksConfig;
import net.minecraft.resources.Identifier;

/**
 * Welche Rezepte der Mod ein abgeschalteter Funktionsschalter mitnimmt (Besitzer 2026-09-28: "samt
 * Rezept", auf allen Loadern gleich). Gefiltert wird beim Aufbau der Rezepttabelle
 * ({@code RecipeMapFilterMixin}), also beim Laden der Datenpakete ({@code /reload}, Weltstart) - fuer
 * Fabric, NeoForge und Forge derselbe Weg, ohne Bedingungen in jeder Rezept-JSON.
 *
 * <p>Erkannt werden die Rezepte an ihrer ID im Namensraum {@code simplebuilding} (auch Faerbe- und
 * Aufwertungsrezepte: {@code netherite_backpack_smithing}, {@code backpack_dyed}); fremde Rezepte, die
 * zufaellig ein Mod-Item ergeben, bleiben.
 *
 * <p>Schalter: {@code server.features.*} (Rucksack, Attractor, Echolot, Blaupause, Erzdetektor,
 * schwebende Bloecke) sowie die Pad-Schalter {@code tweaks.pads.enable*} und
 * {@code tweaks.laserPointer.enable}, die schon die Funktion abschalten.
 */
public final class RecipeFilter {

    private RecipeFilter() {
    }

    /** Ob ueberhaupt ein Schalter aus ist (sonst bleibt die Tabelle unangetastet). */
    public static boolean anyDisabled() {
        ServerTuningConfig.Features f = ServerTuning.get().features;
        TweaksConfig t = SimpleTweaks.config();
        return !f.endSignals || !f.astralVault || !f.backpack || !f.attractor || !f.echoSounder || !f.blueprint || !f.oreDetector || !f.levitatingBlocks
                || !t.pads.enableChunkLoaders || !t.pads.enableElytraPads || !t.pads.enableFlypads
                || !t.pads.enableSpawnTeleporters || !t.pads.enableLaunchpads || !t.pads.enablePotionPads
                || !t.laserPointer.enable;
    }

    /** Ob das Rezept mit dieser ID wegen eines abgeschalteten Schalters fehlt. */
    public static boolean removes(Identifier recipe) {
        if (!"simplebuilding".equals(recipe.getNamespace())) {
            return false;
        }
        String p = recipe.getPath();
        ServerTuningConfig.Features f = ServerTuning.get().features;
        TweaksConfig t = SimpleTweaks.config();
        return (!f.astralVault && p.equals("astral_vault"))
                || (!f.endSignals && java.util.Set.of("nihilith_powder", "astralit_powder", "nihilith_switch", "astralit_switch", "nihilith_lamp", "astralit_lamp").contains(p))
                || (!f.backpack && p.contains("backpack"))
                || (!f.attractor && (p.contains("magnet") || p.contains("attractor")))
                || (!f.echoSounder && (p.contains("echo_compass") || p.contains("echo_sounder")))
                || (!f.blueprint && p.contains("blueprint"))
                || (!f.oreDetector && p.contains("detector"))
                || (!f.levitatingBlocks && (p.startsWith("levitating_") || p.startsWith("suspended_")))
                || (!t.pads.enableChunkLoaders && p.contains("chunk_loader"))
                || (!t.pads.enableElytraPads && p.contains("elytra_pad"))
                || (!t.pads.enableFlypads && p.contains("flypad"))
                || (!t.pads.enableSpawnTeleporters && p.contains("spawn_teleporter"))
                || (!t.pads.enableLaunchpads && p.contains("launchpad"))
                || (!t.pads.enablePotionPads && p.contains("potion_pad"))
                || (!t.laserPointer.enable && (p.equals("amethyst_lens") || p.equals("laser_pointer")));
    }
}
