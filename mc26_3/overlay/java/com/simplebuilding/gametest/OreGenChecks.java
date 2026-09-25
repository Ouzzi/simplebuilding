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
}
