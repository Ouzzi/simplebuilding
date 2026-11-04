package com.simplelib.api.aging;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

/**
 * When a block of an {@link AgingChain} may age at all (checked on the server before the chance roll of a random tick).
 * Examples: {@link #biome} for the Pale Garden, a light level, rain, a config switch ({@code (l, p, s) -> MyConfig.on}).
 */
@FunctionalInterface
public interface AgingCondition {
    AgingCondition ALWAYS = (level, pos, state) -> true;

    boolean test(ServerLevel level, BlockPos pos, BlockState state);

    /** Only inside a biome of {@code tag} (the biome at the block itself). */
    static AgingCondition biome(TagKey<Biome> tag) {
        return (level, pos, state) -> level.getBiome(pos).is(tag);
    }

    default AgingCondition and(AgingCondition other) {
        return (level, pos, state) -> test(level, pos, state) && other.test(level, pos, state);
    }
}
