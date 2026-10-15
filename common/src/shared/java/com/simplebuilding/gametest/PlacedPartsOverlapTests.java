package com.simplebuilding.gametest;

import com.simplebuilding.util.ModTags;
import com.simplebuilding.util.PlacedSmallParts;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Queue N65 (owner 2026-10-15): parts on one spot never poke into each other (ingots, bricks, gems, plates, eggs, candles,
 * pickles). Pure geometry on the same hit boxes the renderer draws, for every count 1..{@link PlacedSmallParts#MAX_PARTS},
 * every mix of the part kinds, all four facings, 3D and flat (hammer-switched). Loader-neutral, no world needed.
 */
public final class PlacedPartsOverlapTests {
    private PlacedPartsOverlapTests() {
    }

    private static final double EPS = 1.0E-4;

    /** One representative per kind, a second plate, and both gems (their silhouettes differ). */
    private static List<ItemStack> representatives() {
        return List.of(new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.PAPER), new ItemStack(Items.EGG),
                new ItemStack(Items.CANDLE), new ItemStack(Items.SEA_PICKLE), new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.BRICK), new ItemStack(Items.DIAMOND), new ItemStack(Items.EMERALD));
    }

    /**
     * Every combination of 1..4 parts: all boxes stay inside the block, and no two parts share volume (touching faces are fine).
     * Ingot stacks are covered as well (all-ingot/brick combinations take the stack branch).
     */
    public static void noPartsOverlap(GameTestHelper helper) {
        if (!McVersion.SMALL_PLACEABLES) {
            helper.succeed();
            return;
        }
        List<ItemStack> reps = representatives();
        Map<String, VoxelShape> cache = new HashMap<>();
        int checked = 0;
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (boolean flat : new boolean[]{false, true}) {
                for (int count = 1; count <= PlacedSmallParts.MAX_PARTS; count++) {
                    int[] pick = new int[count];
                    do {
                        List<ItemStack> parts = new ArrayList<>();
                        for (int p : pick) {
                            parts.add(reps.get(p));
                        }
                        checkPile(helper, parts, facing, flat, cache);
                        checked++;
                    } while (next(pick, reps.size()));
                }
            }
        }
        // Every tagged small part as a plate: four of the same, and mixed with a 3D ingot, a gem and an egg.
        List<ItemStack> tagged = new ArrayList<>();
        for (var item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (stack.is(ModTags.Items.PLACEABLE_SMALL)) {
                tagged.add(stack);
            }
        }
        for (ItemStack x : tagged) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                checkPile(helper, List.of(x, x, x, x), facing, false, cache);
                checkPile(helper, List.of(x, new ItemStack(Items.IRON_INGOT), x, new ItemStack(Items.DIAMOND)), facing, false, cache);
                checkPile(helper, List.of(new ItemStack(Items.EGG), x, new ItemStack(Items.GOLD_INGOT), x), facing, false, cache);
                checked += 3;
            }
        }
        helper.assertTrue(checked > 1000, "too few piles checked: " + checked);
        helper.succeed();
    }

    /** Mixed-radix counter over {@code base}; false after the last combination. */
    private static boolean next(int[] pick, int base) {
        for (int i = pick.length - 1; i >= 0; i--) {
            if (++pick[i] < base) {
                return true;
            }
            pick[i] = 0;
        }
        return false;
    }

    private static void checkPile(GameTestHelper helper, List<ItemStack> parts, Direction facing, boolean flat, Map<String, VoxelShape> cache) {
        int count = parts.size();
        boolean stack = PlacedSmallParts.ingotStack(parts, flat);
        List<VoxelShape> shapes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ItemStack part = parts.get(i);
            String key = BuiltInRegistries.ITEM.getKey(part.getItem()) + "/" + facing + "/" + count + "/" + i + "/" + stack + "/" + flat;
            VoxelShape shape = cache.get(key);
            if (shape == null) {
                shape = PlacedSmallParts.partShape(part, facing, count, i, stack, flat);
                cache.put(key, shape);
            }
            shapes.add(shape);
        }
        String what = describe(parts) + " facing " + facing + (flat ? " flat" : "") + (stack ? " stacked" : "");
        for (int i = 0; i < count; i++) {
            VoxelShape a = shapes.get(i);
            helper.assertTrue(!a.isEmpty(), "part " + i + " has no shape: " + what);
            for (int j = i + 1; j < count; j++) {
                helper.assertTrue(!Shapes.joinIsNotEmpty(a, shapes.get(j), BooleanOp.AND), "parts " + i + " and " + j + " overlap: " + what);
            }
        }
    }

    private static String describe(List<ItemStack> parts) {
        StringBuilder sb = new StringBuilder();
        for (ItemStack part : parts) {
            sb.append(BuiltInRegistries.ITEM.getKey(part.getItem()).getPath()).append(' ');
        }
        return sb.toString().trim();
    }

    /**
     * Every slot of every count keeps the footprint of every kind inside its cell, and the cells of one count do not overlap:
     * the guarantee behind {@link #noPartsOverlap}, checked on the (unclamped) rotated rectangles, so a part pushed out of the
     * block cannot hide behind the clamped hit box.
     */
    public static void slotsFitTheirCells(GameTestHelper helper) {
        if (!McVersion.SMALL_PLACEABLES) {
            helper.succeed();
            return;
        }
        for (PlacedSmallParts.Kind kind : PlacedSmallParts.Kind.values()) {
            for (int count = 1; count <= PlacedSmallParts.MAX_PARTS; count++) {
                float[] fp = PlacedSmallParts.footprint(kind, count);
                List<double[]> rects = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    float[] slot = PlacedSmallParts.slot(count, i, kind);
                    double a = Math.toRadians(slot[2]);
                    double hx = fp[0] * Math.abs(Math.cos(a)) + fp[1] * Math.abs(Math.sin(a));
                    double hz = fp[0] * Math.abs(Math.sin(a)) + fp[1] * Math.abs(Math.cos(a));
                    double[] rect = {slot[0] - hx, slot[1] - hz, slot[0] + hx, slot[1] + hz};
                    String what = kind + " count " + count + " index " + i + " rect " + java.util.Arrays.toString(rect);
                    helper.assertTrue(rect[0] >= -EPS && rect[1] >= -EPS && rect[2] <= 16 + EPS && rect[3] <= 16 + EPS, "outside the block: " + what);
                    for (double[] other : rects) {
                        boolean apart = rect[2] <= other[0] + EPS || other[2] <= rect[0] + EPS || rect[3] <= other[1] + EPS || other[3] <= rect[1] + EPS;
                        helper.assertTrue(apart, "overlaps an earlier slot: " + what);
                    }
                    rects.add(rect);
                }
            }
        }
        helper.succeed();
    }
}
