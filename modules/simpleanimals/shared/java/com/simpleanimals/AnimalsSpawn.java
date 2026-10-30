package com.simpleanimals;

import com.simpleanimals.config.AnimalsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.LevelAccessor;

/**
 * Natural-spawn rules shared by the animals (A12: low weights, thinned further by the config, never at night in caves).
 * Used from each entity's {@code checkSpawnRules}; spawn eggs, spawners and breeding are never affected.
 */
public final class AnimalsSpawn {
    /** True for the two reasons that make a mob appear on its own. */
    public static boolean natural(EntitySpawnReason reason) {
        return reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION;
    }

    /** The master switches and the thinning: false cancels this natural spawn attempt. */
    public static boolean allowed(boolean animalEnabled, RandomSource random) {
        return SimpleAnimals.on() && animalEnabled && (AnimalsConfig.spawnScale >= 1.0 || random.nextDouble() < AnimalsConfig.spawnScale);
    }

    /** Grass, dirt or the like below and daylight above: the Vanilla rule of farm animals. */
    public static boolean onFarmGround(LevelAccessor level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON) && bright(level, pos);
    }

    public static boolean bright(BlockAndLightGetter level, BlockPos pos) {
        return level.getRawBrightness(pos, 0) > 8;
    }

    private AnimalsSpawn() {}
}
