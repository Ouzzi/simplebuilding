package com.simplebuilding.blueprint;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Beispiel-Bauwerke fuer den Knopf "Beispiel einfuegen" im Editor (nur bei leerem Code): je Biom
 * ein vanilla-nahes Dorfhaus und weitere typische Bauwerke, alle hoechstens 16 x 16 x 16,
 * handgeschrieben und kommentiert als Ressourcen {@code data/simplebuilding/blueprint_examples/<name>.sbp},
 * damit sie zugleich zeigen, wie der Code aufgebaut ist. Das Biom ist die aktuelle Position des
 * Spielers; welches Bauwerk kommt, entscheidet der Zufall.
 */
public final class BlueprintExamples {
    /** Biom-Gruppe -> ihre Vorlagen (Ressourcennamen); das erste ist immer das Dorfhaus. */
    public static final Map<String, List<String>> TEMPLATES = Map.of(
            "plains", List.of("plains_house", "plains_well", "plains_ruined_portal"),
            "desert", List.of("desert_house", "desert_well", "desert_temple_ruin"),
            "savanna", List.of("savanna_house", "savanna_market"),
            "taiga", List.of("taiga_house", "taiga_camp"),
            "snow", List.of("snow_house", "snow_igloo"),
            "cherry", List.of("cherry_house", "cherry_pavilion"),
            "swamp", List.of("swamp_house", "swamp_witch_hut"));
    public static final String DEFAULT = "plains";
    private static final Map<String, String> CODE = new HashMap<>();

    private BlueprintExamples() {
    }

    /**
     * Biom-Gruppe fuer ein Biom (Pfad der Biom-Id): die Vanilla-Dorftypen nach Vanillas eigener
     * Zuordnung (VillagerType), Kirschhain und Sumpf/Mangrovensumpf eigens; Dschungel geht auf die
     * Sumpf-Gruppe (Mangrovenholz, feucht-tropisch), alles andere auf die Ebene.
     */
    public static String groupFor(String biomePath) {
        return switch (biomePath) {
            case "cherry_grove" -> "cherry";
            case "swamp", "mangrove_swamp", "jungle", "sparse_jungle", "bamboo_jungle" -> "swamp";
            case "desert", "badlands", "eroded_badlands", "wooded_badlands" -> "desert";
            case "savanna", "savanna_plateau", "windswept_savanna" -> "savanna";
            case "snowy_plains", "snowy_taiga", "snowy_beach", "snowy_slopes", "ice_spikes", "frozen_river", "frozen_ocean",
                 "deep_frozen_ocean", "grove", "frozen_peaks", "jagged_peaks" -> "snow";
            case "taiga", "old_growth_pine_taiga", "old_growth_spruce_taiga", "windswept_hills", "windswept_gravelly_hills",
                 "windswept_forest" -> "taiga";
            default -> DEFAULT;
        };
    }

    public static String groupAt(Level level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey().map(k -> groupFor(k.identifier().getPath())).orElse(DEFAULT);
    }

    /** Eine Vorlage der Gruppe, gewaehlt mit {@code random} (bekommt die Anzahl, liefert einen Index). */
    public static String pick(String group, IntUnaryOperator random) {
        List<String> names = TEMPLATES.getOrDefault(group, TEMPLATES.get(DEFAULT));
        return names.get(Math.floorMod(random.applyAsInt(names.size()), names.size()));
    }

    /** Der Code einer Vorlage (aus der Ressource, zwischengespeichert); leer, wenn sie fehlt. */
    public static String code(String name) {
        synchronized (CODE) {
            return CODE.computeIfAbsent(name, BlueprintExamples::load);
        }
    }

    private static String load(String name) {
        String path = "/data/simplebuilding/blueprint_examples/" + name + ".sbp";
        try (InputStream in = BlueprintExamples.class.getResourceAsStream(path)) {
            if (in == null) {
                return "";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r", "").strip() + "\n";
        } catch (IOException e) {
            return "";
        }
    }
}
