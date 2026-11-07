package com.simplebuilding.util;

import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * One chisel as SimpleBuilding's wiki export, recipe viewer and test centre see it (WE25-7): its tier's maps (forward/back,
 * with Constructor's Touch forward/back), its cooldown before Fast Chiseling, whether it only works backwards, and the
 * durability per step forward/back. 26.2: SimpleBuilding's own chisels; 26.3: what Simple Chisels publishes through
 * SimpleLib {@code ChiselActions} ({@link LibServices#chiselTools}).
 */
public record ChiselTool(Item item, Map<Block, Block> forward, Map<Block, Block> backward, Map<Block, Block> touchForward,
                         Map<Block, Block> touchBackward, int cooldownTicks, boolean spatula, int damage, int reverseDamage) {
}
