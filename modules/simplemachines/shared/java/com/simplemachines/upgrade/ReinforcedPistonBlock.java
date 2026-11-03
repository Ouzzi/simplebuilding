package com.simplemachines.upgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Reinforced Piston, normal ({@code sticky = false}) and sticky ({@code sticky = true}).
 *
 * <p>Most of what sets it apart from Vanilla's piston sits in the mixins: {@code PistonHandlerMixin} raises the push limit from
 * 12 to 18 and lets the one breachable block right in front ({@link PistonBreach}) move along when a redstone block next to it
 * pays; {@code PistonBlockMixin} makes it immovable while extended; {@code PistonHeadBlockMixin} lets Vanilla's head accept it.
 *
 * <p>Here only the payment happens: the redstone block goes only after Vanilla's extension really happened (with a breachable
 * block in front, extending can only succeed through the mixin's exception). Retracting never takes the block back.
 */
public class ReinforcedPistonBlock extends PistonBaseBlock {
    private final boolean sticky;

    public ReinforcedPistonBlock(boolean sticky, Properties settings) {
        super(sticky, settings);
        this.sticky = sticky;
    }

    /** Whether this piston is sticky; Vanilla's own field {@code isSticky} is private. */
    public boolean isStickyPiston() {
        return this.sticky;
    }

    @Override
    public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int type, int data) {
        BlockPos fuel = null;
        if (type == TRIGGER_EXTEND && !world.isClientSide()) {
            Direction facing = state.getValue(FACING);
            if (PistonBreach.isBreachable(world, pos.relative(facing))) {
                fuel = PistonBreach.findFuel(world, pos, facing);
                if (fuel != null && !PistonGuard.mayAutomate(world, pos, fuel)) return false;
            }
        }
        boolean fired = super.triggerEvent(state, world, pos, type, data);
        if (fired && fuel != null) {
            // Only after the push worked, and without a drop: the redstone block is spent.
            world.destroyBlock(fuel, false);
        }
        return fired;
    }
}
