package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import java.util.List;

public class ModWorldGen {

    public static final ResourceKey<ConfiguredFeature<?, ?>> ASTRALIT_ORE_KEY = registerConfiguredKey("astralit_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> NIHILITH_ORE_KEY = registerConfiguredKey("nihilith_ore");

    public static final ResourceKey<PlacedFeature> ASTRALIT_ORE_PLACED_KEY = registerPlacedKey("astralit_ore_placed");
    public static final ResourceKey<PlacedFeature> NIHILITH_ORE_PLACED_KEY = registerPlacedKey("nihilith_ore_placed");

    public static void bootstrapConfiguredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest endStoneReplaceables = new BlockMatchTest(Blocks.END_STONE);

        // Astralit: Kleine Ader (3), da es wertvoll sein soll
        register(context, ASTRALIT_ORE_KEY, Feature.ORE, new OreConfiguration(endStoneReplaceables, ModBlocks.ASTRALIT_ORE.defaultBlockState(), 3));

        // Nihilith: Etwas größere Ader (5), damit man sie besser sieht, wenn sie an der Decke hängt
        register(context, NIHILITH_ORE_KEY, Feature.ORE, new OreConfiguration(endStoneReplaceables, ModBlocks.NIHILITH_ORE.defaultBlockState(), 5));
    }

    public static void bootstrapPlacedFeatures(BootstrapContext<PlacedFeature> context) {
        var configuredFeatureRegistry = context.lookup(Registries.CONFIGURED_FEATURE);

        // --- ASTRALIT (Oben / Surface) ---
        // STATUS: War zu häufig.
        // FIX: Nur in jedem 8. Chunk EINEN Versuch.
        register(context, ASTRALIT_ORE_PLACED_KEY, configuredFeatureRegistry.getOrThrow(ASTRALIT_ORE_KEY),
                List.of(
                        // Rarity 8: Nur 12.5% Wahrscheinlichkeit pro Chunk (ca. jeder 8. Chunk)
                        RarityFilter.onAverageOnceEvery(2),
                        // Count 1: Nur 1 Versuch, wenn der Chunk ausgewählt wurde.
                        CountPlacement.of(1),
                        InSquarePlacement.spread(),
                        PlacementUtils.HEIGHTMAP,
                        BlockPredicateFilter.forPredicate(BlockPredicate.replaceable(Direction.UP.getUnitVec3i())),
                        BiomeFilter.biome()
                ));

        // --- NIHILITH (Unten / Void Exposed) ---
        // STATUS: War zu selten.
        // FIX: Kein RarityFilter mehr, dafür massiv viele Versuche ("Brute Force").
        register(context, NIHILITH_ORE_PLACED_KEY, configuredFeatureRegistry.getOrThrow(NIHILITH_ORE_KEY),
                List.of(
                        // Count 160: Wir brauchen extrem viele Versuche, um die "Kruste" von unten zu treffen.
                        // 98% dieser Versuche schlagen fehl, das ist normal. Übrig bleiben ca. 1-2 Adern pro Chunk.
                        CountPlacement.of(13),
                        InSquarePlacement.spread(),
                        // Y=0 bis 60: Konzentriert sich auf die untere Hälfte der Inseln.
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(60)),
                        // Bedingung: Block UNTEN muss Luft sein
                        BlockPredicateFilter.forPredicate(BlockPredicate.replaceable(Direction.DOWN.getUnitVec3i())),
                        BiomeFilter.biome()
                ));
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerConfiguredKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name));
    }

    public static ResourceKey<PlacedFeature> registerPlacedKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                                                                   ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }

    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key,
                                 net.minecraft.core.Holder<ConfiguredFeature<?, ?>> configuration,
                                 List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }
}