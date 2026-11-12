package com.simplebuilding.modules.simpledimensions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Nether portal surface in an arch (N33, {@code simpledimensions:shaped_nether_portal}): a Vanilla
 * {@link NetherPortalBlock} - teleport, 1:8 travel, exit search and creation, particles, sound, transition time,
 * zombified piglins - whose frame may be any arch of the owner's rules. Only the shape check differs: Vanilla's
 * {@code updateShape} would test for a rectangle and dissolve the arch, so it is replaced, never called.
 * The texture is Vanilla's portal 40 % darker (built from the client jar, never committed).
 */
public final class ShapedNetherPortalBlock extends NetherPortalBlock {

    public ShapedNetherPortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState,
                                     RandomSource random) {
        Direction.Axis updateAxis = directionToNeighbour.getAxis();
        Direction.Axis axis = state.getValue(AXIS);
        boolean wrongAxis = axis != updateAxis && updateAxis.isHorizontal();
        // Like Vanilla: a changed neighbour in the portal plane re-checks the whole shape; broken -> the cell goes out
        // and its neighbours follow, so the entire portal dissolves.
        if (!wrongAxis && !neighbourState.is(this) && !ShapedNetherPortals.intact(level, pos, axis)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        // Super-mod switch "Enable Simple Dimensions" off: the arch stays, but carries nobody.
        if (DimensionRegistry.on()) {
            super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
        }
    }
}
