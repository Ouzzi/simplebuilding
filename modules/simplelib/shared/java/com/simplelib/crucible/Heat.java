package com.simplelib.crucible;

import com.simplelib.api.SimpleLibApi;
import com.simplelib.config.LibConfig;
import com.simplelib.registry.LibTags;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;

/**
 * Where a crucible gets its heat (owner F26/F30/F35, plan section 6): only from the block directly
 * below (full speed) or two blocks below (10 % slower, room for a hopper in between). Flowing
 * (soul) lava is one level lower than the source; cauldrons count like the source. In the Nether
 * every source is one level hotter, at most up to high.
 */
public final class Heat {
    /** Heat at a crucible position plus its position multiplier. */
    public record Reading(HeatLevel level, double multiplier) {
        public static final Reading NONE = new Reading(HeatLevel.NONE, 1.0);
    }

    public static Reading at(Level level, BlockPos crucible) {
        BlockPos below = crucible.below();
        BlockState first = level.getBlockState(below);
        HeatLevel direct = of(level, below, first);
        if (direct != HeatLevel.NONE) return new Reading(nether(level, direct), 1.0);
        if (!passesHeat(level, below, first)) return Reading.NONE;
        BlockPos two = below.below();
        HeatLevel second = of(level, two, level.getBlockState(two));
        if (second == HeatLevel.NONE) return Reading.NONE;
        return new Reading(nether(level, second), 1.0 - LibConfig.twoBelowPenalty);
    }

    /** Heat of one block, ignoring the Nether bonus. */
    public static HeatLevel of(Level level, BlockPos pos, BlockState state) {
        if (state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT)) return HeatLevel.NONE;
        if (state.getBlock() instanceof com.simplelib.cauldron.ReinforcedCauldronBlock) {
            // Owner F30: a cauldron counts like the source block of its content.
            return switch (state.getValue(com.simplelib.cauldron.ReinforcedCauldronBlock.CONTENT)) {
                case LAVA -> HeatLevel.HIGH;
                case EXTREME -> HeatLevel.EXTREME;
                default -> HeatLevel.NONE;
            };
        }
        HeatLevel hooked = SimpleLibApi.heatOf(level, pos, state);
        if (hooked != HeatLevel.NONE) return hooked;
        if (state.is(LibTags.HEAT_EXTREME)) return HeatLevel.EXTREME;
        if (state.is(LibTags.HEAT_HIGH)) return HeatLevel.HIGH;
        if (state.is(LibTags.HEAT_MEDIUM)) return HeatLevel.MEDIUM;
        FluidState fluid = state.getFluidState();
        if (fluid.isEmpty()) return HeatLevel.NONE;
        HeatLevel source = fluid.is(LibTags.EXTREME_HEAT_FLUIDS) ? HeatLevel.EXTREME
                : fluid.is(FluidTags.LAVA) ? HeatLevel.HIGH : HeatLevel.NONE;
        return fluid.isSource() ? source : source.down();
    }

    /** Between crucible and a source two blocks down: air, a hopper or any block that is not a full cube (owner 16). */
    static boolean passesHeat(Level level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.getBlock() instanceof HopperBlock) return true;
        if (state.is(Blocks.MOVING_PISTON)) return false;
        return !state.isCollisionShapeFullBlock(level, pos);
    }

    private static HeatLevel nether(Level level, HeatLevel heat) {
        if (!LibConfig.netherBonus || level.dimension() != Level.NETHER || heat.atLeast(HeatLevel.HIGH)) return heat;
        return heat.up();
    }

    private Heat() {}
}
