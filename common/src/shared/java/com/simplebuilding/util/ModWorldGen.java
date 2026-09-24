package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
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
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
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

        // Astralit: Adergroesse 4. Groesse 3 ist fuer Feature.ORE fast leer: selbst ganz in End-Stein
        // setzt nur gut jede dritte Ader ueberhaupt einen Block (Mittel 0,46). Groesse 4 trifft in
        // rund drei von vier Faellen (Mittel 2,0 Bloecke); gemessen mit einer Nachbildung von
        // OreFeature, keine Vanilla-Zusage.
        register(context, ASTRALIT_ORE_KEY, Feature.ORE, new OreConfiguration(endStoneReplaceables, ModBlocks.ASTRALIT_ORE.defaultBlockState(), 4));

        // Nihilith: Adergroesse 5 (jeder Kugelradius bleibt unter 0,8125). Damit reicht die Ader
        // hoechstens drei Bloecke unter ihren Ursprung und nicht darueber; siehe die Platzierung unten.
        register(context, NIHILITH_ORE_KEY, Feature.ORE, new OreConfiguration(endStoneReplaceables, ModBlocks.NIHILITH_ORE.defaultBlockState(), 5));
    }

    public static void bootstrapPlacedFeatures(BootstrapContext<PlacedFeature> context) {
        var configuredFeatureRegistry = context.lookup(Registries.CONFIGURED_FEATURE);

        // --- ASTRALIT (Oberflaeche) ---
        // Ein Versuch je Chunk, ohne RarityFilter. Der fruehere RarityFilter(2) liess nur einen halben
        // Versuch je Chunk uebrig; zusammen mit Adergroesse 3 kamen so rund 0,18 Erzbloecke auf einen
        // Chunk mit End-Stein. Jetzt sind es rund 1,4 bis 1,6 (Simulation des End-Gelaendes, keine
        // Zusage).
        register(context, ASTRALIT_ORE_PLACED_KEY, configuredFeatureRegistry.getOrThrow(ASTRALIT_ORE_KEY),
                List.of(
                        CountPlacement.of(1),
                        InSquarePlacement.spread(),
                        // Anker H = oberster bewegungshemmender Block + 1, also ein Block ueber der
                        // Oberflaeche. OreFeature legt die Aderzentren zwischen H-2 und H; bei Adergroesse 4
                        // erreicht die Ader nur Y H-3 bis H, und H selbst ist kein End-Stein. Ersetzt wird
                        // also nur End-Stein in den drei Schichten unter dem Anker.
                        PlacementUtils.HEIGHTMAP,
                        BlockPredicateFilter.forPredicate(BlockPredicate.replaceable(Direction.UP.getUnitVec3i())),
                        BiomeFilter.biome()
                ));

        // --- NIHILITH (Inselunterseite) ---
        // Der fruehere Filter pruefte nur "unten Luft" und liess damit fast jeden Punkt im Leeren
        // durch; in der Simulation landete rund die Haelfte der Erzbloecke auf Inseloberseiten. Jetzt
        // zaehlt ein Versuch nur auf einem Unterseitenblock U (End-Stein mit ersetzbarem Block
        // darunter). Der Versatz um +1 setzt den Ursprung auf U+1; OreFeature legt die Aderzentren
        // dann zwischen U-1 und U+1, und bei Adergroesse 5 erreicht die Ader nur End-Stein von U-2 bis
        // U+1. U-1 ist ersetzbar (meist Luft), also sitzt das Erz praktisch in der Unterseitenschicht
        // und der Schicht darueber.
        register(context, NIHILITH_ORE_PLACED_KEY, configuredFeatureRegistry.getOrThrow(NIHILITH_ORE_KEY),
                List.of(
                        // 64 Versuche je Chunk; die meisten treffen keinen Unterseitenblock und fallen am
                        // Filter heraus. Ergibt rund 1,4 bis 1,5 Erzbloecke je Chunk mit End-Stein
                        // (Simulation, keine Zusage).
                        CountPlacement.of(64),
                        InSquarePlacement.spread(),
                        // Y 0 bis 60: in der Simulation des Vanilla-Ends lag keine Inselunterseite hoeher.
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(60)),
                        BlockPredicateFilter.forPredicate(BlockPredicate.allOf(
                                BlockPredicate.matchesBlocks(Blocks.END_STONE),
                                BlockPredicate.replaceable(Direction.DOWN.getUnitVec3i()))),
                        RandomOffsetPlacement.vertical(ConstantInt.of(1)),
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