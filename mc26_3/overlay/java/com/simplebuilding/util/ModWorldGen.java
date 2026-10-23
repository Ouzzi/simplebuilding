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

    /**
     * Truebe End-Erze (W23-2, Besitzer 2026-10-23): ein Fuenftel der Erzbloecke einer Ader ist truebes Erz, die echte Menge
     * bleibt im Mittel gleich. Dieselben Platzierungen, nur groessere Adern mit zwei Zielen: zuerst wird End-Stein mit
     * {@code *_DULL_CHANCE} truebes Erz, sonst mit {@code *_REAL_CHANCE} echtes, der Rest bleibt End-Stein. Die Zahlen stammen
     * aus einer Nachbildung von OreFeature 26.3 im jeweiligen Platzierungs-Umfeld (10^6 Adern, docs/ai/PLAN-A-SBTEX23B-2026-10-23.md):
     * Astralit (Oberflaeche) Groesse 4 -> 1,888 Bloecke, Groesse 5 -> 2,913 End-Stein-Treffer; Nihilit (Unterseite) Groesse 5 ->
     * 1,579, Groesse 6 -> 2,030. trueb = alt/4 / neu, echt = alt / (neu - alt/4). Groesse < 8 haelt jeden Kugelradius unter 1,
     * die Adern reichen also weiter nur so tief wie vorher (siehe 26.2-Zwilling).
     */
    public static final int ASTRALIT_VEIN_SIZE = 5;
    public static final int NIHILITH_VEIN_SIZE = 6;
    /** The vein sizes before W23-2 (only real ore), the reference of the expected real amount. */
    public static final int ASTRALIT_VEIN_SIZE_BEFORE = 4;
    public static final int NIHILITH_VEIN_SIZE_BEFORE = 5;
    public static final float ASTRALIT_DULL_CHANCE = 0.162F;
    public static final float ASTRALIT_REAL_CHANCE = 0.773F;
    public static final float NIHILITH_DULL_CHANCE = 0.194F;
    public static final float NIHILITH_REAL_CHANCE = 0.966F;

    /** Weisheitserz (2026-10-01): Groesse, Haeufigkeit und Hoehe wie Vanillas kleines Diamanterz. */
    public static final ResourceKey<Feature> SAGE_ORE_KEY = registerConfiguredKey("sage_ore");
    public static final ResourceKey<PlacedFeature> SAGE_ORE_PLACED_KEY = registerPlacedKey("sage_ore_placed");
    public static final int SAGE_ORE_SIZE = 4;
    public static final float SAGE_ORE_AIR_DISCARD = 0.5F;
    public static final int SAGE_ORE_COUNT = 7;

    /**
     * Dimensions-Schrott (2026-10-01): seltener als Antiker Schrott - eine Ader von hoechstens 2 Bloecken, die nie
     * an Luft liegt, im Schnitt nur in jedem {@link #SCRAP_RARITY}. Chunk (Antiker Schrott: zwei Adern je Chunk).
     */
    public static final int SCRAP_SIZE = 2;
    public static final int SCRAP_RARITY = 3;
    public static final ResourceKey<Feature> SCRAP_OVERWORLD_KEY = registerConfiguredKey("dimensional_scrap");
    public static final ResourceKey<Feature> SCRAP_NETHER_KEY = registerConfiguredKey("nether_dimensional_scrap");
    public static final ResourceKey<Feature> SCRAP_END_KEY = registerConfiguredKey("end_dimensional_scrap");
    public static final ResourceKey<PlacedFeature> SCRAP_OVERWORLD_PLACED_KEY = registerPlacedKey("dimensional_scrap_placed");
    public static final ResourceKey<PlacedFeature> SCRAP_NETHER_PLACED_KEY = registerPlacedKey("nether_dimensional_scrap_placed");
    public static final ResourceKey<PlacedFeature> SCRAP_END_PLACED_KEY = registerPlacedKey("end_dimensional_scrap_placed");

    /** Rissiges Eis (Nachtrag 35): Flecken auf Eisflaechen und vereinzelte 1x1-Stellen, nach freeze_top_layer. */
    public static final ResourceKey<Feature> CRACKED_ICE_PATCH_KEY = registerConfiguredKey("cracked_ice_patch");
    public static final ResourceKey<Feature> CRACKED_ICE_SINGLE_KEY = registerConfiguredKey("cracked_ice_single");
    public static final ResourceKey<PlacedFeature> CRACKED_ICE_PATCH_PLACED_KEY = registerPlacedKey("cracked_ice_patch_placed");
    public static final ResourceKey<PlacedFeature> CRACKED_ICE_SINGLE_PLACED_KEY = registerPlacedKey("cracked_ice_single_placed");
    public static final int CRACKED_ICE_PATCH_SIZE = 14;
    /**
     * Rissiges-Eis-Felder (N53, Besitzer 2026-10-13): sehr grosse Flaechen in Eiszapfen-Biomen und auf gefrorenen Ozeanen
     * ({@code #simplebuilding:has_cracked_ice_fields}) - eine Falle. Je Treffer {@link #CRACKED_ICE_FIELD_DISCS} Scheiben
     * (Radius {@link #CRACKED_ICE_FIELD_MIN_RADIUS}..{@link #CRACKED_ICE_FIELD_MAX_RADIUS}) im selben Chunk, die zusammen
     * ein Feld von gut 20-30 Bloecken bilden; nur in jedem {@link #CRACKED_ICE_FIELD_RARITY}. Chunk dieser Biome. Die
     * Oberflaeche (Eis, Schneeblock, Schnee, Erde, Pulverschnee unter Luft) wird rissiges Eis (Stufe 1), was darunter
     * liegt bis {@link #CRACKED_ICE_FIELD_DEPTH} tief Luft: wer einbricht, faellt in eine Grube und muss sich
     * hochbauen; unter Meereis bleibt das Wasser. Schalter: {@code server.oreGeneration.crackedIceFields}.
     */
    public static final ResourceKey<Feature> CRACKED_ICE_FIELD_KEY = registerConfiguredKey("cracked_ice_field");
    public static final ResourceKey<PlacedFeature> CRACKED_ICE_FIELD_PLACED_KEY = registerPlacedKey("cracked_ice_field_placed");
    public static final int CRACKED_ICE_FIELD_RARITY = 12;
    public static final int CRACKED_ICE_FIELD_DISCS = 4;
    public static final int CRACKED_ICE_FIELD_MIN_RADIUS = 5;
    public static final int CRACKED_ICE_FIELD_MAX_RADIUS = 8;
    public static final int CRACKED_ICE_FIELD_DEPTH = 4;

    /** Sculk Jaws on the floors of the Deep Dark (N50): about 6 attempts per chunk, only where the block can stand. */
    public static final ResourceKey<Feature> SCULK_JAW_KEY = registerConfiguredKey("sculk_jaw");
    public static final ResourceKey<PlacedFeature> SCULK_JAW_PLACED_KEY = registerPlacedKey("sculk_jaw_placed");
    /** Tries per chunk. N55: most random spots fail the sculk rule now, so more tries keep the number of jaws about the same. */
    public static final int SCULK_JAW_COUNT = 24;

    /**
     * N55 (owner): a Sculk Jaw spawns only in the middle of sculk. The spot itself is air where the jaw survives, the block
     * under it is Sculk and so are the four horizontal neighbors of that floor block (a plus of five Sculk blocks).
     * Sculk Veins, Catalysts and other blocks do not count.
     */
    public static BlockPredicate sculkJawSpot() {
        java.util.List<BlockPredicate> all = new java.util.ArrayList<>();
        all.add(BlockPredicate.matchesBlocks(Blocks.AIR));
        all.add(BlockPredicate.wouldSurvive(ModBlocks.SCULK_JAW));
        for (net.minecraft.core.Vec3i offset : java.util.List.of(new net.minecraft.core.Vec3i(0, -1, 0), new net.minecraft.core.Vec3i(1, -1, 0),
                new net.minecraft.core.Vec3i(-1, -1, 0), new net.minecraft.core.Vec3i(0, -1, 1), new net.minecraft.core.Vec3i(0, -1, -1))) {
            all.add(BlockPredicate.matchesBlocks(offset, java.util.List.of(Blocks.SCULK)));
        }
        return BlockPredicate.allOf(all);
    }

    public static void bootstrapConfiguredFeatures(BootstrapContext<Feature> context) {
        context.register(ASTRALIT_ORE_KEY, endOre(ModBlocks.DULL_ASTRALIT_ORE, ASTRALIT_DULL_CHANCE,
                ModBlocks.ASTRALIT_ORE, ASTRALIT_REAL_CHANCE, ASTRALIT_VEIN_SIZE));
        context.register(NIHILITH_ORE_KEY, endOre(ModBlocks.DULL_NIHILITH_ORE, NIHILITH_DULL_CHANCE,
                ModBlocks.NIHILITH_ORE, NIHILITH_REAL_CHANCE, NIHILITH_VEIN_SIZE));
        if (com.simplebuilding.version.McVersion.DIMENSIONAL_SCRAP) {
            context.register(SCRAP_OVERWORLD_KEY, new OreFeature(List.of(net.minecraft.world.level.levelgen.feature.BlockReplacement.replace(
                    new net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest(net.minecraft.tags.BlockTags.DEEPSLATE_ORE_REPLACEABLES),
                    ModBlocks.DIMENSIONAL_SCRAP.defaultBlockState())), SCRAP_SIZE, 1.0F));
            context.register(SCRAP_NETHER_KEY, new OreFeature(List.of(net.minecraft.world.level.levelgen.feature.BlockReplacement.replace(
                    new net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest(net.minecraft.tags.BlockTags.BASE_STONE_NETHER),
                    ModBlocks.NETHER_DIMENSIONAL_SCRAP.defaultBlockState())), SCRAP_SIZE, 1.0F));
            context.register(SCRAP_END_KEY, new OreFeature(List.of(net.minecraft.world.level.levelgen.feature.BlockReplacement.replace(
                    new BlockMatchTest(Blocks.END_STONE), ModBlocks.END_DIMENSIONAL_SCRAP.defaultBlockState())), SCRAP_SIZE, 1.0F));
        }
        if (com.simplebuilding.version.McVersion.NATURE_VARIANTS) {
            // Cracked ice belongs to Simple Blocks since b-blocksmove (by id; datagen loads the required sub-mod).
            context.register(CRACKED_ICE_PATCH_KEY, new OreFeature(new BlockMatchTest(Blocks.ICE),
                    crackedIce().defaultBlockState(), CRACKED_ICE_PATCH_SIZE));
            context.register(CRACKED_ICE_SINGLE_KEY, new net.minecraft.world.level.levelgen.feature.SimpleBlockFeature(
                    new net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider(crackedIce().defaultBlockState())));
            context.register(CRACKED_ICE_FIELD_KEY, crackedIceField());
        }
        if (com.simplebuilding.version.McVersion.NATURE_VARIANTS) {
            context.register(SCULK_JAW_KEY, new net.minecraft.world.level.levelgen.feature.SimpleBlockFeature(
                    new net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider(ModBlocks.SCULK_JAW.defaultBlockState())));
        }
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

    /** An End ore vein: dull ore first (its chance), then the real ore (its chance) - both only in End stone (W23-2). */
    public static OreFeature endOre(net.minecraft.world.level.block.Block dull, float dullChance,
                                    net.minecraft.world.level.block.Block real, float realChance, int size) {
        return new OreFeature(List.of(
                new net.minecraft.world.level.levelgen.feature.BlockReplacement(
                        new net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockMatchTest(Blocks.END_STONE, dullChance), dull.defaultBlockState()),
                new net.minecraft.world.level.levelgen.feature.BlockReplacement(
                        new net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockMatchTest(Blocks.END_STONE, realChance), real.defaultBlockState())),
                size);
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
        if (com.simplebuilding.version.McVersion.NATURE_VARIANTS) {
            register(context, CRACKED_ICE_PATCH_PLACED_KEY, features.getOrThrow(CRACKED_ICE_PATCH_KEY), List.of(
                    net.minecraft.world.level.levelgen.placement.RarityFilter.onAverageOnceEvery(2), InSquarePlacement.spread(),
                    PlacementUtils.HEIGHTMAP, BiomeFilter.biome()));
            register(context, CRACKED_ICE_SINGLE_PLACED_KEY, features.getOrThrow(CRACKED_ICE_SINGLE_KEY), List.of(
                    CountPlacement.of(6), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP,
                    OffsetPlacement.vertical(ConstantInt.of(-1)),
                    BlockPredicateFilter.forPredicate(BlockPredicate.matchesBlocks(Blocks.ICE)), BiomeFilter.biome()));
        }
        if (com.simplebuilding.version.McVersion.NATURE_VARIANTS) {
            // The rarity filter decides per chunk, then several discs inside that chunk make one big field.
            register(context, CRACKED_ICE_FIELD_PLACED_KEY, features.getOrThrow(CRACKED_ICE_FIELD_KEY), List.of(
                    net.minecraft.world.level.levelgen.placement.RarityFilter.onAverageOnceEvery(CRACKED_ICE_FIELD_RARITY),
                    CountPlacement.of(CRACKED_ICE_FIELD_DISCS), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP,
                    OffsetPlacement.vertical(ConstantInt.of(-1)), BiomeFilter.biome()));
        }
        if (com.simplebuilding.version.McVersion.NATURE_VARIANTS) {
            // Pick a random spot, scan down to the floor, stand on it; the block's own survival rule and N55's sculk plus decide.
            register(context, SCULK_JAW_PLACED_KEY, features.getOrThrow(SCULK_JAW_KEY), List.of(
                    CountPlacement.of(SCULK_JAW_COUNT), InSquarePlacement.spread(),
                    HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(2), VerticalAnchor.absolute(-8)),
                    net.minecraft.world.level.levelgen.placement.EnvironmentScanPlacement.scanningFor(Direction.DOWN,
                            BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_PREDICATE, 12),
                    OffsetPlacement.vertical(ConstantInt.of(1)),
                    BlockPredicateFilter.forPredicate(sculkJawSpot()),
                    BiomeFilter.biome()));
        }
        if (com.simplebuilding.version.McVersion.SAGE_ORE) {
            registerSageOre(context, features);
        }
        if (com.simplebuilding.version.McVersion.DIMENSIONAL_SCRAP) {
            registerScrap(context, features);
        }
    }

    /**
     * One disc of a cracked ice field: a Vanilla disk feature whose state provider works top-down through each column -
     * snow layers go, the topmost block under open air becomes cracked ice (stage 1), what lies under it turns into cave
     * air (the pit). Blocks under water are never targets, so frozen ocean water stays where it is.
     */
    /** Simple Blocks' cracked ice (b-blocksmove), by id. */
    private static net.minecraft.world.level.block.Block crackedIce() {
        return com.simplebuilding.util.BlockRefs.block(com.simplebuilding.util.BlockRefs.CRACKED_ICE);
    }

    private static Feature crackedIceField() {
        net.minecraft.core.Vec3i up = Direction.UP.getUnitVec3i();
        // Carved blocks become cave air, so a block under the pit never mistakes it for the open sky; columns still
        // covered by other ground (a slope higher than the disc) match no rule and stay as they are.
        var provider = net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedStateProvider.builder()
                .ifTrueThenProvide(BlockPredicate.matchesBlocks(Blocks.SNOW), Blocks.AIR)
                .ifTrueThenProvide(BlockPredicate.matchesBlocks(up, List.of(Blocks.AIR)),
                        crackedIce().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_3, 1))
                .ifTrueThenProvide(BlockPredicate.matchesBlocks(up, List.of(crackedIce(), Blocks.CAVE_AIR)), Blocks.CAVE_AIR)
                .build();
        BlockPredicate target = BlockPredicate.allOf(
                BlockPredicate.matchesBlocks(Blocks.ICE, Blocks.SNOW_BLOCK, Blocks.SNOW, Blocks.DIRT, Blocks.POWDER_SNOW),
                BlockPredicate.not(BlockPredicate.matchesFluids(up, List.of(net.minecraft.world.level.material.Fluids.WATER))));
        return new net.minecraft.world.level.levelgen.feature.DiskFeature(Holder.direct(provider), target,
                net.minecraft.util.valueproviders.UniformInt.of(CRACKED_ICE_FIELD_MIN_RADIUS, CRACKED_ICE_FIELD_MAX_RADIUS),
                CRACKED_ICE_FIELD_DEPTH);
    }

    /** Placements of the Dimensional Scrap: Overworld deep in deepslate, Nether like ancient debris, End inside the islands. */
    private static void registerScrap(BootstrapContext<PlacedFeature> context, net.minecraft.core.HolderGetter<Feature> features) {
        register(context, SCRAP_OVERWORLD_PLACED_KEY, features.getOrThrow(SCRAP_OVERWORLD_KEY), List.of(
                net.minecraft.world.level.levelgen.placement.RarityFilter.onAverageOnceEvery(SCRAP_RARITY), InSquarePlacement.spread(),
                HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(0), VerticalAnchor.aboveBottom(32)), BiomeFilter.biome()));
        register(context, SCRAP_NETHER_PLACED_KEY, features.getOrThrow(SCRAP_NETHER_KEY), List.of(
                net.minecraft.world.level.levelgen.placement.RarityFilter.onAverageOnceEvery(SCRAP_RARITY), InSquarePlacement.spread(),
                HeightRangePlacement.triangle(VerticalAnchor.absolute(8), VerticalAnchor.absolute(24)), BiomeFilter.biome()));
        register(context, SCRAP_END_PLACED_KEY, features.getOrThrow(SCRAP_END_KEY), List.of(
                net.minecraft.world.level.levelgen.placement.RarityFilter.onAverageOnceEvery(SCRAP_RARITY), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(10), VerticalAnchor.absolute(70)), BiomeFilter.biome()));
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
