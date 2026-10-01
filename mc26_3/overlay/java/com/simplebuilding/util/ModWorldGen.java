package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.ReplaceablePredicate;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import java.util.List;

/**
 * End ore generation, MC 26.3 side (twin: common/src/mc26_2/java/.../ModWorldGen.java). Same keys,
 * same vein sizes, same placements - only the 26.3 types: an OreFeature record in the
 * worldgen/feature registry instead of a ConfiguredFeature, OffsetPlacement instead of
 * RandomOffsetPlacement, ReplaceablePredicate built directly. The reasoning behind every number is
 * documented in the 26.2 twin.
 */
public class ModWorldGen {

    /** The registry the ore features live in; datagen and NeoForge register through this. */
    public static final ResourceKey<Registry<Feature>> FEATURE_REGISTRY = Registries.FEATURE;

    public static final ResourceKey<Feature> ASTRALIT_ORE_KEY = registerConfiguredKey("astralit_ore");
    public static final ResourceKey<Feature> NIHILITH_ORE_KEY = registerConfiguredKey("nihilith_ore");

    public static final ResourceKey<PlacedFeature> ASTRALIT_ORE_PLACED_KEY = registerPlacedKey("astralit_ore_placed");
    public static final ResourceKey<PlacedFeature> NIHILITH_ORE_PLACED_KEY = registerPlacedKey("nihilith_ore_placed");

    /** Weisheitserz (2026-10-01): Groesse, Haeufigkeit und Hoehe wie Vanillas kleines Diamanterz. */
    public static final ResourceKey<Feature> SAGE_ORE_KEY = registerConfiguredKey("sage_ore");
    public static final ResourceKey<PlacedFeature> SAGE_ORE_PLACED_KEY = registerPlacedKey("sage_ore_placed");
    public static final int SAGE_ORE_SIZE = 4;
    public static final float SAGE_ORE_AIR_DISCARD = 0.5F;
    public static final int SAGE_ORE_COUNT = 7;

    public static void bootstrapConfiguredFeatures(BootstrapContext<Feature> context) {
        RuleTest endStoneReplaceables = new BlockMatchTest(Blocks.END_STONE);
        context.register(ASTRALIT_ORE_KEY, new OreFeature(endStoneReplaceables, ModBlocks.ASTRALIT_ORE.defaultBlockState(), 4));
        context.register(NIHILITH_ORE_KEY, new OreFeature(endStoneReplaceables, ModBlocks.NIHILITH_ORE.defaultBlockState(), 5));
        if (com.simplebuilding.version.McVersion.SAGE_ORE) {
            context.register(SAGE_ORE_KEY, new OreFeature(List.of(
                    net.minecraft.world.level.levelgen.feature.BlockReplacement.replace(
                            new net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest(net.minecraft.tags.BlockTags.STONE_ORE_REPLACEABLES),
                            ModBlocks.SAGE_ORE.defaultBlockState()),
                    net.minecraft.world.level.levelgen.feature.BlockReplacement.replace(
                            new net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest(net.minecraft.tags.BlockTags.DEEPSLATE_ORE_REPLACEABLES),
                            ModBlocks.DEEPSLATE_SAGE_ORE.defaultBlockState())),
                    SAGE_ORE_SIZE, SAGE_ORE_AIR_DISCARD));
        }
    }

    public static void bootstrapPlacedFeatures(BootstrapContext<PlacedFeature> context) {
        var features = context.lookup(Registries.FEATURE);

        register(context, ASTRALIT_ORE_PLACED_KEY, features.getOrThrow(ASTRALIT_ORE_KEY),
                List.of(
                        CountPlacement.of(1),
                        InSquarePlacement.spread(),
                        PlacementUtils.HEIGHTMAP,
                        BlockPredicateFilter.forPredicate(new ReplaceablePredicate(Direction.UP.getUnitVec3i())),
                        BiomeFilter.biome()
                ));

        register(context, NIHILITH_ORE_PLACED_KEY, features.getOrThrow(NIHILITH_ORE_KEY),
                List.of(
                        CountPlacement.of(64),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(60)),
                        BlockPredicateFilter.forPredicate(BlockPredicate.allOf(
                                BlockPredicate.matchesBlocks(Blocks.END_STONE),
                                new ReplaceablePredicate(Direction.DOWN.getUnitVec3i()))),
                        OffsetPlacement.vertical(ConstantInt.of(1)),
                        BiomeFilter.biome()
                ));
        if (com.simplebuilding.version.McVersion.SAGE_ORE) {
            registerSageOre(context, features);
        }
    }

    /** Placement of the Sage Ore; registered from {@link #bootstrapPlacedFeatures}. */
    private static void registerSageOre(BootstrapContext<PlacedFeature> context, net.minecraft.core.HolderGetter<Feature> features) {
        register(context, SAGE_ORE_PLACED_KEY, features.getOrThrow(SAGE_ORE_KEY),
                List.of(
                        CountPlacement.of(SAGE_ORE_COUNT),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)),
                        BiomeFilter.biome()
                ));
    }

    public static ResourceKey<Feature> registerConfiguredKey(String name) {
        return ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name));
    }

    public static ResourceKey<PlacedFeature> registerPlacedKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name));
    }

    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key,
                                 Holder<Feature> feature, List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(feature, List.copyOf(modifiers)));
    }
}
