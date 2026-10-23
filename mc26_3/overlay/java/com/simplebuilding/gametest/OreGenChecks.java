package com.simplebuilding.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.ReplaceablePredicate;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockMatchTest;
import net.minecraft.server.level.ServerLevel;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.util.ModWorldGen;

import java.util.ArrayList;
import java.util.List;

/** MC 26.3 side of {@code OreGenChecks} (see the 26.2 twin for the contract). */
final class OreGenChecks {

    private OreGenChecks() {
    }

    static BlockPredicate replaceableAt(Direction direction) {
        return new ReplaceablePredicate(direction.getUnitVec3i());
    }

    static PlacementModifier verticalOffset(int dy) {
        return OffsetPlacement.vertical(ConstantInt.of(dy));
    }

    static boolean isOffset(PlacementModifier modifier) {
        return modifier instanceof OffsetPlacement;
    }

    static List<BlockPos> positions(PlacementModifier modifier, PlacementContext context, RandomSource random,
                                    BlockPos origin) {
        List<BlockPos> out = new ArrayList<>();
        modifier.modify(context, random, origin, pos -> out.add(pos.immutable()));
        return out;
    }

    static void assertOreFeature(GameTestHelper helper, ResourceKey<Feature> key, Block ore, int veinSize) {
        Registry<Feature> registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.FEATURE);
        Feature feature = registry.getValue(key);
        helper.assertTrue(feature != null,
                key.identifier() + " is not in the feature registry; the generated worldgen JSON is "
                        + "missing from the jar or was never regenerated");
        helper.assertTrue(feature instanceof OreFeature, key.identifier() + " is no longer an ore feature");
        OreFeature config = (OreFeature) feature;

        helper.assertValueEqual(config.size(), veinSize, key.identifier() + " vein size");
        helper.assertValueEqual(config.targetStates().size(), 1, key.identifier() + " target count");
        helper.assertTrue(config.discardChanceOnAirExposure() == 0.0F,
                key.identifier() + " now discards ore on air exposure, which changes how much of it is reachable");

        BlockReplacement target = config.targetStates().get(0);
        helper.assertTrue(target.state().is(ore),
                key.identifier() + " places " + target.state() + " instead of the mod's ore block");

        RandomSource random = RandomSource.create();
        helper.assertTrue(target.target().test(Blocks.END_STONE.defaultBlockState(), BlockPos.ZERO, random),
                key.identifier() + " no longer replaces end stone, so it cannot generate in the End");
        helper.assertFalse(target.target().test(Blocks.STONE.defaultBlockState(), BlockPos.ZERO, random),
                key.identifier() + " also replaces overworld stone, so the ore leaks out of the End");
    }

    /**
     * The two End ore features since W23-2: one vein, two targets - the dull ore first (random End stone match with the dull
     * chance), then the real ore (random End stone match with the real chance) - and the bigger vein sizes of ModWorldGen.
     */
    static void assertEndOreFeatures(GameTestHelper helper) {
        assertEndOre(helper, ModWorldGen.ASTRALIT_ORE_KEY, ModBlocks.DULL_ASTRALIT_ORE, ModWorldGen.ASTRALIT_DULL_CHANCE,
                ModBlocks.ASTRALIT_ORE, ModWorldGen.ASTRALIT_REAL_CHANCE, ModWorldGen.ASTRALIT_VEIN_SIZE);
        assertEndOre(helper, ModWorldGen.NIHILITH_ORE_KEY, ModBlocks.DULL_NIHILITH_ORE, ModWorldGen.NIHILITH_DULL_CHANCE,
                ModBlocks.NIHILITH_ORE, ModWorldGen.NIHILITH_REAL_CHANCE, ModWorldGen.NIHILITH_VEIN_SIZE);
    }

    private static void assertEndOre(GameTestHelper helper, ResourceKey<Feature> key, Block dull, float dullChance, Block real,
                                     float realChance, int veinSize) {
        Feature feature = helper.getLevel().registryAccess().lookupOrThrow(Registries.FEATURE).getValue(key);
        helper.assertTrue(feature instanceof OreFeature, key.identifier() + " is missing or no longer an ore feature: " + feature);
        OreFeature ore = (OreFeature) feature;
        helper.assertValueEqual(ore.size(), veinSize, key.identifier() + " vein size");
        helper.assertValueEqual(ore.targetStates().size(), 2, key.identifier() + " target count (dull, then real)");
        helper.assertTrue(ore.discardChanceOnAirExposure() == 0.0F,
                key.identifier() + " now discards ore on air exposure, which changes how much of it is reachable");
        assertRandomEndStoneTarget(helper, key, ore.targetStates().get(0), dull, dullChance);
        assertRandomEndStoneTarget(helper, key, ore.targetStates().get(1), real, realChance);
    }

    private static void assertRandomEndStoneTarget(GameTestHelper helper, ResourceKey<Feature> key, BlockReplacement target,
                                                   Block ore, float chance) {
        helper.assertTrue(target.state().is(ore), key.identifier() + " places " + target.state() + " where " + ore + " belongs");
        helper.assertTrue(target.target() instanceof RandomBlockMatchTest, key.identifier() + " target of " + ore
                + " is no random End stone match: " + target.target());
        RandomSource random = RandomSource.create(1L);
        int hits = 0;
        for (int i = 0; i < 4000; i++) {
            helper.assertFalse(target.target().test(Blocks.STONE.defaultBlockState(), BlockPos.ZERO, random),
                    key.identifier() + " also replaces overworld stone with " + ore);
            if (target.target().test(Blocks.END_STONE.defaultBlockState(), BlockPos.ZERO, random)) {
                hits++;
            }
        }
        helper.assertTrue(Math.abs(hits / 4000.0 - chance) < 0.03, key.identifier() + " turns " + hits + "/4000 End stone into "
                + ore + ", expected a chance of " + chance);
    }

    /**
     * W23-2 in numbers: the registered End ore features are placed many times (fixed seed) into End stone shaped like their
     * placement - Astralit under the surface anchor (End stone only below the origin), Nihilit at an island underside (End stone
     * from one block under the origin up) - and compared with the feature before W23-2 (old vein size, real ore only).
     * The dull ore must be a fifth of all ore blocks (0.20 +- 0.04) and the real ore as much as before (+- 10 %).
     */
    static void assertDullShareAndRealAmount(GameTestHelper helper) {
        Registry<Feature> registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.FEATURE);
        measure(helper, "astralit", registry.getValue(ModWorldGen.ASTRALIT_ORE_KEY),
                new OreFeature(new BlockMatchTest(Blocks.END_STONE), ModBlocks.ASTRALIT_ORE.defaultBlockState(), ModWorldGen.ASTRALIT_VEIN_SIZE_BEFORE),
                ModBlocks.DULL_ASTRALIT_ORE, ModBlocks.ASTRALIT_ORE, -1);
        measure(helper, "nihilith", registry.getValue(ModWorldGen.NIHILITH_ORE_KEY),
                new OreFeature(new BlockMatchTest(Blocks.END_STONE), ModBlocks.NIHILITH_ORE.defaultBlockState(), ModWorldGen.NIHILITH_VEIN_SIZE_BEFORE),
                ModBlocks.DULL_NIHILITH_ORE, ModBlocks.NIHILITH_ORE, 0);
    }

    /** Veins per feature; enough for a few percent of statistical spread, small enough for one test tick. */
    static final int VEINS = 600;
    /** The test box around the vein origin: |dx|,|dz| <= 3 (vein reach at size 6: 1 + 1 = 2, plus margin), dy -4..+1. */
    private static final int R = 3;

    /** stoneTop: End stone at origin dy <= stoneTop (Astralit: -1, below the surface anchor), else from dy >= -1 up (Nihilit). */
    private static void measure(GameTestHelper helper, String name, Feature now, Feature before, Block dull, Block real, int stoneTop) {
        helper.assertTrue(now != null, name + " ore feature missing from the registry");
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(4, 6, 4));
        boolean surface = stoneTop < 0;
        long[] nowCount = run(level, now, origin, surface, dull, real, 23L);
        long[] beforeCount = run(level, before, origin, surface, dull, real, 23L);
        double share = nowCount[0] / (double) Math.max(1, nowCount[0] + nowCount[1]);
        double ratio = nowCount[1] / (double) Math.max(1, beforeCount[1]);
        helper.assertTrue(Math.abs(share - 0.20) <= 0.04, name + ": dull ore is " + Math.round(share * 1000) / 10.0
                + " % of the ore blocks (" + nowCount[0] + " dull, " + nowCount[1] + " real in " + VEINS + " veins), expected 20 %");
        helper.assertTrue(Math.abs(ratio - 1.0) <= 0.10, name + ": " + nowCount[1] + " real ore blocks in " + VEINS
                + " veins, before W23-2 " + beforeCount[1] + " - the real amount has to stay the same");
        helper.assertValueEqual(beforeCount[0], 0L, name + " dull ore in the old feature");
    }

    /**
     * The feature's own vein (its targets, size and doPlace) without OreFeature.place's world-generation shortcut, which
     * skips a vein whose box lies above the OCEAN_FLOOR_WG heightmap - a heightmap a live chunk only primes once and never
     * updates, so the hand-built End stone of this test may not count. The lines before doPlace are place()'s own.
     */
    private static final class VeinPlacer extends OreFeature {
        VeinPlacer(OreFeature of) {
            super(of.targetStates(), of.size(), of.discardChanceOnAirExposure());
        }

        void placeHere(ServerLevel level, RandomSource random, BlockPos origin) {
            float dir = random.nextFloat() * (float) Math.PI;
            float spreadXY = size() / 8.0F;
            int maxRadius = net.minecraft.util.Mth.ceil((size() / 16.0F * 2.0F + 1.0F) / 2.0F);
            double x0 = origin.getX() + Math.sin(dir) * spreadXY;
            double x1 = origin.getX() - Math.sin(dir) * spreadXY;
            double z0 = origin.getZ() + Math.cos(dir) * spreadXY;
            double z1 = origin.getZ() - Math.cos(dir) * spreadXY;
            double y0 = origin.getY() + random.nextInt(3) - 2;
            double y1 = origin.getY() + random.nextInt(3) - 2;
            int xStart = origin.getX() - net.minecraft.util.Mth.ceil(spreadXY) - maxRadius;
            int yStart = origin.getY() - 2 - maxRadius;
            int zStart = origin.getZ() - net.minecraft.util.Mth.ceil(spreadXY) - maxRadius;
            int sizeXZ = 2 * (net.minecraft.util.Mth.ceil(spreadXY) + maxRadius);
            int sizeY = 2 * (2 + maxRadius);
            doPlace(level, random, x0, x1, z0, z1, y0, y1, xStart, yStart, zStart, sizeXZ, sizeY);
        }
    }

    private static long[] run(ServerLevel level, Feature feature, BlockPos origin, boolean surface, Block dull, Block real, long seed) {
        RandomSource random = RandomSource.create(seed);
        List<BlockPos> box = new ArrayList<>();
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                for (int dy = -4; dy <= 1; dy++) {
                    box.add(origin.offset(dx, dy, dz));
                }
            }
        }
        long dullCount = 0;
        long realCount = 0;
        for (int vein = 0; vein < VEINS; vein++) {
            for (BlockPos pos : box) {
                int dy = pos.getY() - origin.getY();
                boolean stone = surface ? dy <= -1 : dy >= -1;
                net.minecraft.world.level.block.state.BlockState want = (stone ? Blocks.END_STONE : Blocks.AIR).defaultBlockState();
                if (level.getBlockState(pos) != want) {
                    level.setBlock(pos, want, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                }
            }
            new VeinPlacer((OreFeature) feature).placeHere(level, random, origin);
            for (BlockPos pos : box) {
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                if (state.is(dull)) {
                    dullCount++;
                } else if (state.is(real)) {
                    realCount++;
                }
            }
        }
        for (BlockPos pos : box) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
        return new long[]{dullCount, realCount};
    }
}
