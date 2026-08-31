package com.simplebuilding.world.gen;

import com.simplebuilding.util.ModWorldGen;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.level.levelgen.GenerationStep;

public class ModOreGeneration {
    public static void generateOres() {
        // Füge Astralit zu allen End-Biomen hinzu
        BiomeModifications.addFeature(BiomeSelectors.foundInTheEnd(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                ModWorldGen.ASTRALIT_ORE_PLACED_KEY);

        // Füge Nihilith zu allen End-Biomen hinzu
        BiomeModifications.addFeature(BiomeSelectors.foundInTheEnd(),
                GenerationStep.Decoration.UNDERGROUND_ORES,
                ModWorldGen.NIHILITH_ORE_PLACED_KEY);
    }
}