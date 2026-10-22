package com.simplevisuals;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;

/**
 * TZ22-7 (docs/ai/PLAN-A-FRAMES22-2026-10-22.md): flat block items (pressure plates, carpets, trapdoors, daylight
 * detector) show in an item frame like in the inventory, tilted from above, instead of as a thin strip. This class picks
 * the items and holds no client classes, so game tests can check the choice; the renderer also requires a block model.
 */
public final class FrameTilt {
    /** Highest top of a "flat" block in its default state (daylight detector = 6/16). */
    public static final double MAX_HEIGHT = 6.0 / 16.0;
    /** Narrowest footprint in X and Z, so small floor objects keep the Vanilla view. */
    public static final double MIN_WIDTH = 8.0 / 16.0;
    private static final Map<Item, Boolean> CACHE = new ConcurrentHashMap<>();

    private FrameTilt() {}

    /** A block item whose default shape lies on the floor, at most 6/16 high and at least 8/16 wide. */
    public static boolean flat(ItemStack stack) {
        return !stack.isEmpty() && CACHE.computeIfAbsent(stack.getItem(), FrameTilt::compute);
    }

    private static boolean compute(Item item) {
        if (!(item instanceof BlockItem blockItem)) return false;
        try {
            var shape = blockItem.getBlock().defaultBlockState().getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            return !shape.isEmpty() && shape.min(Direction.Axis.Y) <= 0.0 && shape.max(Direction.Axis.Y) <= MAX_HEIGHT
                    && shape.max(Direction.Axis.X) - shape.min(Direction.Axis.X) >= MIN_WIDTH
                    && shape.max(Direction.Axis.Z) - shape.min(Direction.Axis.Z) >= MIN_WIDTH;
        } catch (RuntimeException e) {
            return false; // a shape that needs a real level: keep the Vanilla view
        }
    }
}
