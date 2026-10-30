package com.simplebuilding.modules.simplemobs.trials;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Duck interface of the Brewing Stand block entity (mixin {@code BrewingStandSoulMixin}): the soul charges and the Evil / Void brewing step. */
public interface SoulBrewing {
    /** Soul charges left (1 Soulfire Powder = {@link EvilBrewing#CHARGES}; every Evil / Void brew takes one). */
    int simplemobs$soul();

    void simplemobs$setSoul(int charges);

    /** One server tick of the Evil / Void recipe; {@code true} = it handled this tick (Vanilla's brewing step is skipped). */
    boolean simplemobs$tick(Level level, BlockPos pos, BlockState state);
}
