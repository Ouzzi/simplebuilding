package com.simplevisuals;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * FR23 (docs/ai/PLAN-A-FRAMES23-2026-10-23.md, replaces the inventory view of TZ22-7): flat blocks lie flat in an item
 * frame, seen from above. Pressure plates, carpets, trapdoors and the daylight detector show their top side to the viewer,
 * panes (glass, stained glass, bars, mod panes) their pane face, each as a 3D block model tilted by {@link #TILT_DEGREES}
 * so the edge shows. This class picks the items and the block state to draw and holds no client classes, so game tests
 * can check the choice; for {@link Kind#FLAT} the renderer also requires a block item model (rails stay sprites).
 */
public final class FrameTilt {
    /** Highest top of a "flat" block in its default state (daylight detector = 6/16). */
    public static final double MAX_HEIGHT = 6.0 / 16.0;
    /** Narrowest footprint in X and Z, so small floor objects keep the Vanilla view. */
    public static final double MIN_WIDTH = 8.0 / 16.0;
    /** Owner choice B (preview frames23): the lower edge comes 15 degrees forward, so the thickness is always visible. */
    public static final float TILT_DEGREES = 15.0F;

    /** How a framed item is drawn. */
    public enum Kind { VANILLA, FLAT, PANE }

    private static final Map<Item, Kind> CACHE = new ConcurrentHashMap<>();

    private FrameTilt() {}

    public static Kind kind(ItemStack stack) {
        return stack.isEmpty() ? Kind.VANILLA : CACHE.computeIfAbsent(stack.getItem(), FrameTilt::compute);
    }

    /** A block item whose default shape lies on the floor, at most 6/16 high and at least 8/16 wide. */
    public static boolean flat(ItemStack stack) {
        return kind(stack) == Kind.FLAT;
    }

    /** The block state drawn for a {@link Kind#FLAT} or {@link Kind#PANE} item: the default state, a pane stretched east-west. */
    public static BlockState viewState(ItemStack stack) {
        BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        if (kind(stack) == Kind.PANE && state.hasProperty(CrossCollisionBlock.EAST) && state.hasProperty(CrossCollisionBlock.WEST)) {
            state = state.setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true);
        }
        return state;
    }

    private static Kind compute(Item item) {
        if (!(item instanceof BlockItem blockItem)) return Kind.VANILLA;
        // Glass panes, stained panes, iron/copper bars and mod panes (wood sheets) all extend IronBarsBlock.
        if (blockItem.getBlock() instanceof IronBarsBlock) return Kind.PANE;
        try {
            var shape = blockItem.getBlock().defaultBlockState().getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            return !shape.isEmpty() && shape.min(Direction.Axis.Y) <= 0.0 && shape.max(Direction.Axis.Y) <= MAX_HEIGHT
                    && shape.max(Direction.Axis.X) - shape.min(Direction.Axis.X) >= MIN_WIDTH
                    && shape.max(Direction.Axis.Z) - shape.min(Direction.Axis.Z) >= MIN_WIDTH ? Kind.FLAT : Kind.VANILLA;
        } catch (RuntimeException e) {
            return Kind.VANILLA; // a shape that needs a real level: keep the Vanilla view
        }
    }
}
