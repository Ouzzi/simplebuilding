package com.simplelib.api;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that wears out in steps stored in its block state (breaker pistons). Jade line and JEI repair entry ask this
 * interface, so they work for the blocks of either owner (SimpleBuilding on 26.2, Simple Machines on 26.3) without
 * importing their classes.
 */
public interface WearingBlock {
    /** Full durability of this block; 0 for none. */
    int maxDurability();

    /** Remaining durability in {@code state}. */
    int durabilityIn(BlockState state);

    /** The item that restores full durability. */
    Item repairMaterial();
}
