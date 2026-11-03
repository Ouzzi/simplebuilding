package com.simplebuilding.util;

import com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The wear of breaker pistons for Jade ({@code BlockInfo}) and the JEI export ({@code InWorldTransformations}). 26.2: the core's own
 * {@link NetheriteBreakerPistonBlock}; the 26.3 twin asks SimpleLib's {@code WearingBlock} (Simple Machines' pistons, QW-15).
 */
public final class PistonWear {
    private PistonWear() {
    }

    /** Full durability of {@code block}; 0 if it does not wear. */
    public static int maxDurability(Block block) {
        return block instanceof NetheriteBreakerPistonBlock breaker ? breaker.maxDurability() : 0;
    }

    /** Remaining durability in {@code state}; 0 if its block does not wear. */
    public static int durability(BlockState state) {
        return maxDurability(state.getBlock()) > 0 ? NetheriteBreakerPistonBlock.durabilityOf(state) : 0;
    }

    /** The item that repairs {@code block} (only for blocks with {@link #maxDurability} above 0). */
    public static Item repairMaterial(Block block) {
        return ((NetheriteBreakerPistonBlock) block).repairMaterial();
    }
}
