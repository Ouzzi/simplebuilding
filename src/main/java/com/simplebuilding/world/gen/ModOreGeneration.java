package com.simplebuilding.world.gen;

import com.simplebuilding.util.ModWorldGen;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;

import java.util.function.Predicate;

public class ModOreGeneration {
    /**
     * Every biome the End can generate, plus everything tagged minecraft:is_end (what the NeoForge
     * biome modifiers select). In a normal world the tag adds nothing; it matters where the End is a
     * flat single-biome dimension - the MC 26.3 gametest server builds its world from
     * FLAT_ALL_DIMENSIONS, where foundInTheEnd() alone matches only minecraft:the_end.
     */
    private static final Predicate<BiomeSelectionContext> END_BIOMES =
            BiomeSelectors.foundInTheEnd().or(BiomeSelectors.tag(BiomeTags.IS_END));

    public static void generateOres() {
        // Füge Astralit zu allen End-Biomen hinzu
        BiomeModifications.addFeature(END_BIOMES,
                GenerationStep.Decoration.UNDERGROUND_ORES,
                ModWorldGen.ASTRALIT_ORE_PLACED_KEY);

        // Füge Nihilith zu allen End-Biomen hinzu
        BiomeModifications.addFeature(END_BIOMES,
                GenerationStep.Decoration.UNDERGROUND_ORES,
                ModWorldGen.NIHILITH_ORE_PLACED_KEY);
    }
}