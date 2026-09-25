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
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;

import java.util.List;

/**
 * The version dependent half of {@link OreGenAndItemFrameTests}, MC 26.2 side (twin in
 * mc26_3/overlay/java). 26.3 rebuilt worldgen features: no ConfiguredFeature/OreConfiguration any
 * more, OffsetPlacement instead of RandomOffsetPlacement, and placement modifiers push positions
 * into a consumer instead of returning a stream. Everything the test asks stays the same.
 */
final class OreGenChecks {

    private OreGenChecks() {
    }

    /** "Replaceable block at this offset" as the mod's placement writes it. */
    static BlockPredicate replaceableAt(Direction direction) {
        return BlockPredicate.replaceable(direction.getUnitVec3i());
    }

    /** A fixed vertical offset of the placement origin. */
    static PlacementModifier verticalOffset(int dy) {
        return RandomOffsetPlacement.vertical(ConstantInt.of(dy));
    }

    static boolean isOffset(PlacementModifier modifier) {
        return modifier instanceof RandomOffsetPlacement;
    }

    static List<BlockPos> positions(PlacementModifier modifier, PlacementContext context, RandomSource random,
                                    BlockPos origin) {
        return modifier.getPositions(context, random, origin).toList();
    }

    static void assertOreFeature(GameTestHelper helper, ResourceKey<ConfiguredFeature<?, ?>> key,
                                 Block ore, int veinSize) {
        Registry<ConfiguredFeature<?, ?>> registry =
                helper.getLevel().registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
        ConfiguredFeature<?, ?> configured = registry.getValue(key);
        helper.assertTrue(configured != null,
                key.identifier() + " is not in the configured feature registry; the generated worldgen JSON is "
                        + "missing from the jar or was never regenerated");

        // Ueber Feature<?> statt direkt, sonst vergleicht javac zwei Capture-Typen miteinander.
        Feature<?> feature = configured.feature();
        helper.assertTrue(feature == Feature.ORE, key.identifier() + " is no longer an ore feature");
        helper.assertTrue(configured.config() instanceof OreConfiguration,
                key.identifier() + " no longer carries an OreConfiguration");
        OreConfiguration config = (OreConfiguration) configured.config();

        helper.assertValueEqual(config.size, veinSize, key.identifier() + " vein size");
        helper.assertValueEqual(config.targetStates.size(), 1, key.identifier() + " target count");
        helper.assertTrue(config.discardChanceOnAirExposure == 0.0F,
                key.identifier() + " now discards ore on air exposure, which changes how much of it is reachable");

        OreConfiguration.TargetBlockState target = config.targetStates.get(0);
        helper.assertTrue(target.state.is(ore),
                key.identifier() + " places " + target.state + " instead of the mod's ore block");

        // Das Ersetzungs-Muster wird gefahren, nicht nur verglichen: End-Stein ja, alles andere nein.
        RandomSource random = RandomSource.create();
        helper.assertTrue(target.target.test(Blocks.END_STONE.defaultBlockState(), random),
                key.identifier() + " no longer replaces end stone, so it cannot generate in the End");
        helper.assertFalse(target.target.test(Blocks.STONE.defaultBlockState(), random),
                key.identifier() + " also replaces overworld stone, so the ore leaks out of the End");
    }
}
