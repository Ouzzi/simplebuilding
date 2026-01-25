package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.rule.BlockMatchRuleTest;
import net.minecraft.structure.rule.RuleTest;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.world.gen.YOffset;
import net.minecraft.world.gen.blockpredicate.BlockPredicate;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.placementmodifier.*;

import java.util.List;

public class ModWorldGen {

    public static final RegistryKey<ConfiguredFeature<?, ?>> ASTRALIT_ORE_KEY = registerConfiguredKey("astralit_ore");
    public static final RegistryKey<ConfiguredFeature<?, ?>> NIHILITH_ORE_KEY = registerConfiguredKey("nihilith_ore");

    public static final RegistryKey<PlacedFeature> ASTRALIT_ORE_PLACED_KEY = registerPlacedKey("astralit_ore_placed");
    public static final RegistryKey<PlacedFeature> NIHILITH_ORE_PLACED_KEY = registerPlacedKey("nihilith_ore_placed");

    public static void bootstrapConfiguredFeatures(Registerable<ConfiguredFeature<?, ?>> context) {
        RuleTest endStoneReplaceables = new BlockMatchRuleTest(Blocks.END_STONE);

        // Astralit: Kleine Ader (3), da es wertvoll sein soll
        register(context, ASTRALIT_ORE_KEY, Feature.ORE, new OreFeatureConfig(endStoneReplaceables, ModBlocks.ASTRALIT_ORE.getDefaultState(), 3));

        // Nihilith: Etwas größere Ader (5), damit man sie besser sieht, wenn sie an der Decke hängt
        register(context, NIHILITH_ORE_KEY, Feature.ORE, new OreFeatureConfig(endStoneReplaceables, ModBlocks.NIHILITH_ORE.getDefaultState(), 5));
    }

    public static void bootstrapPlacedFeatures(Registerable<PlacedFeature> context) {
        var configuredFeatureRegistry = context.getRegistryLookup(RegistryKeys.CONFIGURED_FEATURE);

        // --- ASTRALIT (Oben / Surface) ---
        // STATUS: War zu häufig.
        // FIX: Nur in jedem 8. Chunk EINEN Versuch.
        register(context, ASTRALIT_ORE_PLACED_KEY, configuredFeatureRegistry.getOrThrow(ASTRALIT_ORE_KEY),
                List.of(
                        // Rarity 8: Nur 12.5% Wahrscheinlichkeit pro Chunk (ca. jeder 8. Chunk)
                        RarityFilterPlacementModifier.of(2),
                        // Count 1: Nur 1 Versuch, wenn der Chunk ausgewählt wurde.
                        CountPlacementModifier.of(1),
                        SquarePlacementModifier.of(),
                        PlacedFeatures.MOTION_BLOCKING_HEIGHTMAP,
                        BlockFilterPlacementModifier.of(BlockPredicate.replaceable(Direction.UP.getVector())),
                        BiomePlacementModifier.of()
                ));

        // --- NIHILITH (Unten / Void Exposed) ---
        // STATUS: War zu selten.
        // FIX: Kein RarityFilter mehr, dafür massiv viele Versuche ("Brute Force").
        register(context, NIHILITH_ORE_PLACED_KEY, configuredFeatureRegistry.getOrThrow(NIHILITH_ORE_KEY),
                List.of(
                        // Count 160: Wir brauchen extrem viele Versuche, um die "Kruste" von unten zu treffen.
                        // 98% dieser Versuche schlagen fehl, das ist normal. Übrig bleiben ca. 1-2 Adern pro Chunk.
                        CountPlacementModifier.of(13),
                        SquarePlacementModifier.of(),
                        // Y=0 bis 60: Konzentriert sich auf die untere Hälfte der Inseln.
                        HeightRangePlacementModifier.uniform(YOffset.fixed(0), YOffset.fixed(60)),
                        // Bedingung: Block UNTEN muss Luft sein
                        BlockFilterPlacementModifier.of(BlockPredicate.replaceable(Direction.DOWN.getVector())),
                        BiomePlacementModifier.of()
                ));
    }

    public static RegistryKey<ConfiguredFeature<?, ?>> registerConfiguredKey(String name) {
        return RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(Simplebuilding.MOD_ID, name));
    }

    public static RegistryKey<PlacedFeature> registerPlacedKey(String name) {
        return RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(Simplebuilding.MOD_ID, name));
    }

    private static <FC extends FeatureConfig, F extends Feature<FC>> void register(Registerable<ConfiguredFeature<?, ?>> context,
                                                                                   RegistryKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }

    private static void register(Registerable<PlacedFeature> context, RegistryKey<PlacedFeature> key,
                                 net.minecraft.registry.entry.RegistryEntry<ConfiguredFeature<?, ?>> configuration,
                                 List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }
}