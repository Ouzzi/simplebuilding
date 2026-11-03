package com.simplebuilding.util;

import com.simplelib.api.WearingBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 26.3 twin of the 26.2 class: the breaker pistons are Simple Machines' (MACH32-MOD stage 2), SimpleBuilding imports none of its classes and
 * asks SimpleLib's {@link WearingBlock} instead (QW-15).
 */
public final class PistonWear {
    private PistonWear() {
    }

    /** Full durability of {@code block}; 0 if it does not wear. */
    public static int maxDurability(Block block) {
        return block instanceof WearingBlock wear ? wear.maxDurability() : 0;
    }

    /** Remaining durability in {@code state}; 0 if its block does not wear. */
    public static int durability(BlockState state) {
        return state.getBlock() instanceof WearingBlock wear ? wear.durabilityIn(state) : 0;
    }

    /** The item that repairs {@code block} (only for blocks with {@link #maxDurability} above 0). */
    public static Item repairMaterial(Block block) {
        return ((WearingBlock) block).repairMaterial();
    }
}
