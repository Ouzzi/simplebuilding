package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.HammockBlock;
import com.simplebuilding.blocks.custom.HammockLayout;
import com.simplebuilding.blocks.custom.HammockRopeBlock;
import com.simplebuilding.blocks.custom.HammockShape;
import com.simplebuilding.blocks.custom.HammockTime;
import com.simplebuilding.blocks.custom.StandingRodBlock;
import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.HammockItem;
import com.simplebuilding.version.McVersion;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Hammock (docs/ai/PLAN-HAENGEMATTE-2026-10-02.md, v2; any angle: PLAN-HAENGEMATTE-WINKEL-2026-10-02.md): placement
 * between two anchors 2 to 4 apart, straight in one click or at any angle in two, the cloth in the middle, falling and dropping once, resting day and night, the clock speed-up and
 * its share of players, the recipe, the capped server option. Loader-neutral; on lines without {@link McVersion#HAMMOCK}
 * the tests pass at once.
 */
public final class HammockTests {
    private HammockTests() {
    }

    /** Anchors on rope height y=3, the cloth hangs at y=2; straight hammocks run south from z=1. */
    private static final int ROPE_Y = 3;

    private static ServerPlayer player(GameTestHelper helper, GameType mode) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(mode);
        Vec3 corner = helper.absoluteVec(new Vec3(7.5, 1.0, 7.5));
        player.setPos(corner.x, corner.y, corner.z);
        player.setYRot(0.0F); // facing south
        return player;
    }

    private static ItemStack hold(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!held.is(ModItems.WHITE_HAMMOCK)) {
            held = new ItemStack(ModItems.WHITE_HAMMOCK);
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
        }
        return held;
    }

    /** Clicks the south face of the block at {@code anchor} (relative) with a white hammock (the one in hand, if any). */
    private static InteractionResult click(GameTestHelper helper, ServerPlayer player, BlockPos anchor) {
        ItemStack stack = hold(player);
        BlockPos abs = helper.absolutePos(anchor);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).add(0.0, 0.0, 0.5), Direction.SOUTH, abs, false);
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static BlockState at(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos);
    }

    private static boolean isHammock(BlockState state) {
        return state.getBlock() instanceof HammockBlock || state.getBlock() instanceof HammockRopeBlock;
    }

    /**
     * Asserts the white hammock {@code spot} (relative): a rope block in every rope cell, cloth only in the cloth cells
     * (the head where it belongs, air under the other rope cells), every block entity linked to the hammock, intact.
     */
    private static void assertHangs(GameTestHelper helper, HammockLayout.Spot spot, String what) {
        HammockLayout.Spot abs = spot.moved(helper.absolutePos(BlockPos.ZERO));
        List<BlockPos> cloth = spot.clothCells();
        for (BlockPos cell : spot.ropeCells()) {
            BlockState rope = at(helper, cell);
            helper.assertTrue(rope.is(ModBlocks.HAMMOCK_ROPE), what + ": rope cell " + cell.toShortString() + " is " + rope);
            helper.assertValueEqual(HammockLayout.spotAt(helper.getLevel(), helper.absolutePos(cell)), abs,
                    what + ": hammock of rope cell " + cell.toShortString());
            if (!cloth.contains(cell.below())) {
                helper.assertTrue(at(helper, cell.below()).isAir(), what + ": cell under the rope " + cell.toShortString() + " is not free");
            }
        }
        for (BlockPos cell : cloth) {
            BlockState below = at(helper, cell);
            boolean head = cell.equals(spot.clothHead());
            helper.assertTrue(below.is(ModBlocks.WHITE_HAMMOCK) && (below.getValue(HammockBlock.PART) == BedPart.HEAD) == head
                    && below.getValue(HammockBlock.FACING) == spot.facing() && below.getValue(HammockLayout.STRAIGHT) == spot.straight(),
                    what + ": cloth cell " + cell.toShortString() + " is " + below);
            helper.assertValueEqual(HammockLayout.spotAt(helper.getLevel(), helper.absolutePos(cell)), abs,
                    what + ": hammock of cloth cell " + cell.toShortString());
        }
        helper.assertTrue(HammockLayout.intact(helper.getLevel(), helper.absolutePos(spot.clothHead())), what + ": not intact");
    }

    /** Asserts that none of the cells of {@code spot} (relative) holds a hammock block any more. */
    private static void assertGone(GameTestHelper helper, HammockLayout.Spot spot, String what) {
        for (BlockPos cell : spot.cells()) {
            helper.assertFalse(isHammock(at(helper, cell)), what + ": hammock block left at " + cell.toShortString());
        }
    }

    private static void assertNoHammockIn(GameTestHelper helper, int x, String what) {
        for (int y = 1; y <= ROPE_Y + 1; y++) {
            for (int z = 0; z < 8; z++) {
                helper.assertFalse(isHammock(at(helper, new BlockPos(x, y, z))), what + ": hammock block left at " + x + "," + y + "," + z);
            }
        }
    }

    private static int dropped(GameTestHelper helper, Item item) {
        AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8.0, 6.0, 8.0);
        int count = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, box, e -> e.getItem().is(item))) {
            count += entity.getItem().getCount();
        }
        return count;
    }

    private static HammockLayout.Spot straight(int x, int gap) {
        return new HammockLayout.Spot(new BlockPos(x, ROPE_Y, 1), 0, gap + 1);
    }

    /** Builds a straight hammock between stone anchors at (x, ROPE_Y, 1) and (x, ROPE_Y, 2 + gap) with one click. */
    private static HammockLayout.Spot hang(GameTestHelper helper, ServerPlayer player, int x, int gap) {
        HammockLayout.Spot spot = straight(x, gap);
        helper.setBlock(spot.anchor(), Blocks.STONE);
        helper.setBlock(spot.otherAnchor(), Blocks.STONE);
        InteractionResult result = click(helper, player, spot.anchor());
        helper.assertTrue(result.consumesAction(), "gap " + gap + ": the hammock was refused (" + result + ")");
        return spot;
    }

    /**
     * Two clicks (sneaking, so the first one always remembers its anchor) between stone anchors at {@code spot}
     * (relative); returns the second click's result. The remembered anchor is forgotten afterwards either way.
     */
    private static InteractionResult twoClicks(GameTestHelper helper, ServerPlayer player, HammockLayout.Spot spot) {
        helper.setBlock(spot.anchor(), Blocks.STONE);
        helper.setBlock(spot.otherAnchor(), Blocks.STONE);
        player.setShiftKeyDown(true);
        helper.assertTrue(click(helper, player, spot.anchor()).consumesAction(), "the first click on an anchor was refused");
        helper.assertValueEqual(HammockItem.storedAnchor(player.getMainHandItem(), helper.getLevel()),
                Optional.of(helper.absolutePos(spot.anchor())), "remembered first anchor");
        InteractionResult result = click(helper, player, spot.otherAnchor());
        player.setShiftKeyDown(false);
        HammockItem.clearAnchor(player.getMainHandItem());
        return result;
    }

    private static List<BlockPos> cells(BlockPos origin, int y, int[][] xz) {
        List<BlockPos> out = new java.util.ArrayList<>();
        for (int[] c : xz) {
            out.add(origin.offset(c[0], y, c[1]));
        }
        return out;
    }

    /** One click: gaps 1 and 5 fail, 2, 3 and 4 hang with the cloth in the middle; a fence holds like a block. */
    public static void hangsOnlyBetweenTwoAnchorsTwoToFourApart(GameTestHelper helper) {
        if (!McVersion.HAMMOCK) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        for (int gap = 1; gap <= 5; gap++) {
            HammockLayout.Spot spot = straight(gap, gap);
            helper.setBlock(spot.anchor(), Blocks.STONE);
            helper.setBlock(spot.otherAnchor(), Blocks.STONE);
            InteractionResult result = click(helper, player, spot.anchor());
            if (gap >= 2 && gap <= 4) {
                helper.assertTrue(result.consumesAction(), "gap " + gap + " was refused: " + result);
                assertHangs(helper, spot, "gap " + gap);
            } else {
                assertNoHammockIn(helper, gap, "gap " + gap);
                // no hammock, but the clicked anchor waits for a second click (two-click placing)
                helper.assertValueEqual(HammockItem.storedAnchor(player.getMainHandItem(), helper.getLevel()),
                        Optional.of(helper.absolutePos(spot.anchor())), "gap " + gap + ": remembered anchor");
                HammockItem.clearAnchor(player.getMainHandItem());
            }
        }
        helper.setBlock(new BlockPos(6, ROPE_Y, 1), Blocks.OAK_FENCE);
        helper.setBlock(new BlockPos(6, ROPE_Y, 4), Blocks.OAK_FENCE);
        helper.assertTrue(click(helper, player, new BlockPos(6, ROPE_Y, 1)).consumesAction(), "fences did not hold the hammock");
        assertHangs(helper, straight(6, 2), "between fences");
        helper.assertFalse(HammockLayout.isAnchor(helper.getLevel(), helper.absolutePos(new BlockPos(7, ROPE_Y, 1))), "air is an anchor");
        helper.succeed();
    }

    /**
     * Straight and 45-degree hammocks occupy exactly the cells of v2: rope cells 1..gap along the line, cloth cells
     * 0,1 / 0,1,2 / 1,2 (by index), the cloth head at index 1 / 1 / 2 - in every direction.
     */
    public static void clothHangsInTheMiddleAtEveryGap(GameTestHelper helper) {
        int[][] clothIndex = {{0, 1}, {0, 1, 2}, {1, 2}};
        int[] headIndex = {1, 1, 2};
        BlockPos a = new BlockPos(10, 64, -7);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (boolean diagonal : new boolean[] {false, true}) {
                int sx = facing.getStepX() + (diagonal ? facing.getClockWise().getStepX() : 0);
                int sz = facing.getStepZ() + (diagonal ? facing.getClockWise().getStepZ() : 0);
                for (int gap = HammockLayout.MIN_GAP; gap <= HammockLayout.MAX_GAP; gap++) {
                    HammockLayout.Spot spot = new HammockLayout.Spot(a, sx * (gap + 1), sz * (gap + 1));
                    String what = facing + (diagonal ? " diagonal " : " straight ") + gap;
                    List<BlockPos> rope = new java.util.ArrayList<>();
                    for (int i = 0; i < gap; i++) {
                        rope.add(a.offset(sx * (i + 1), 0, sz * (i + 1)));
                    }
                    List<BlockPos> cloth = new java.util.ArrayList<>();
                    for (int i : clothIndex[gap - 2]) {
                        cloth.add(rope.get(i).below());
                    }
                    helper.assertValueEqual(spot.ropeCells(), rope, what + ": rope cells");
                    helper.assertValueEqual(spot.clothCells(), cloth, what + ": cloth cells");
                    helper.assertValueEqual(spot.clothHead(), rope.get(headIndex[gap - 2]).below(), what + ": cloth head");
                    helper.assertValueEqual(spot.facing(), facing, what + ": facing");
                    helper.assertValueEqual(spot.straight(), !diagonal, what + ": straight");
                }
            }
        }
        helper.succeed();
    }

    /**
     * Centred at every angle: for every allowed offset (2 to 4 free cells along the main axis, any side offset) the
     * rope and cloth cells mirror about the middle between the anchor centres, cloth cells hang under rope cells, the
     * rope cells form a chain from anchor to anchor, the head point lies in the cloth head, and the drawn boxes
     * ({@link HammockShape}) mirror about the middle too.
     */
    public static void clothMiddleSitsBetweenTheAnchorsInEveryDirection(GameTestHelper helper) {
        BlockPos a = new BlockPos(-3, 70, 12);
        int count = 0;
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                HammockLayout.Spot spot = new HammockLayout.Spot(a, dx, dz);
                String what = "offset " + dx + "," + dz;
                int main = Math.max(Math.abs(dx), Math.abs(dz));
                helper.assertValueEqual(spot.valid(), main >= 3 && main <= 5, what + ": valid");
                helper.assertValueEqual(HammockLayout.between(a, spot.otherAnchor()).isPresent(), spot.valid(), what + ": between");
                if (!spot.valid()) {
                    continue;
                }
                count++;
                List<BlockPos> rope = spot.ropeCells();
                List<BlockPos> cloth = spot.clothCells();
                helper.assertTrue(rope.size() <= 8 && cloth.size() >= 2 && cloth.size() <= 4, what + ": cell counts " + rope.size() + "/" + cloth.size());
                // the reflection through the middle maps cell (x, z) to (2a + d - x, 2a + d - z)
                for (List<BlockPos> list : List.of(rope, cloth)) {
                    for (BlockPos cell : list) {
                        BlockPos mirror = new BlockPos(2 * a.getX() + dx - cell.getX(), cell.getY(), 2 * a.getZ() + dz - cell.getZ());
                        helper.assertTrue(list.contains(mirror), what + ": " + cell.toShortString() + " has no mirror cell");
                    }
                }
                for (BlockPos cell : cloth) {
                    helper.assertTrue(rope.contains(cell.above()), what + ": cloth cell " + cell.toShortString() + " not under the rope");
                }
                BlockPos previous = a;
                for (BlockPos cell : rope) {
                    helper.assertTrue(Math.abs(cell.getX() - previous.getX()) <= 1 && Math.abs(cell.getZ() - previous.getZ()) <= 1
                            && !cell.equals(previous), what + ": rope chain broken at " + cell.toShortString());
                    previous = cell;
                }
                BlockPos b = spot.otherAnchor();
                helper.assertTrue(Math.abs(b.getX() - previous.getX()) <= 1 && Math.abs(b.getZ() - previous.getZ()) <= 1,
                        what + ": rope chain does not reach the second anchor");
                BlockPos head = spot.clothHead();
                helper.assertTrue(cloth.contains(head), what + ": the cloth head is no cloth cell");
                helper.assertTrue(spot.headX() >= head.getX() - 1.0E-9 && spot.headX() <= head.getX() + 1 + 1.0E-9
                        && spot.headZ() >= head.getZ() - 1.0E-9 && spot.headZ() <= head.getZ() + 1 + 1.0E-9,
                        what + ": head point outside the cloth head");
                double midX = (a.getX() + b.getX()) / 2.0 + 0.5;
                double midZ = (a.getZ() + b.getZ()) / 2.0 + 0.5;
                helper.assertTrue(Math.abs(spot.middleX() - midX) < 1.0E-9 && Math.abs(spot.middleZ() - midZ) < 1.0E-9, what + ": middle");
                helper.assertTrue(Math.abs(Math.hypot(spot.headX() - midX, spot.headZ() - midZ) - 0.5) < 1.0E-9, what + ": head point");
                assertBoxesMirror(helper, spot, what);
            }
        }
        helper.assertValueEqual(count, 96, "allowed offsets");
        helper.succeed();
    }

    /** Every corner of every drawn box has its mirror image (through the vertical axis at the middle) among the corners. */
    private static void assertBoxesMirror(GameTestHelper helper, HammockLayout.Spot spot, String what) {
        List<double[]> pts = new java.util.ArrayList<>();
        List<double[]> mirrored = new java.util.ArrayList<>();
        for (HammockShape.Box box : HammockShape.boxes(spot)) {
            for (double[] p : box.corners()) {
                double t = (p[0] - spot.middleX()) * spot.ux() + (p[2] - spot.middleZ()) * spot.uz();
                double w = -(p[0] - spot.middleX()) * spot.uz() + (p[2] - spot.middleZ()) * spot.ux();
                pts.add(new double[] {t, w, p[1]});
                mirrored.add(new double[] {-t, -w, p[1]});
            }
        }
        java.util.Comparator<double[]> order = java.util.Comparator.<double[]>comparingDouble(v -> Math.round(v[0] * 1.0E6))
                .thenComparingDouble(v -> Math.round(v[1] * 1.0E6)).thenComparingDouble(v -> Math.round(v[2] * 1.0E6));
        pts.sort(order);
        mirrored.sort(order);
        double worst = 0;
        for (int i = 0; i < pts.size(); i++) {
            for (int k = 0; k < 3; k++) {
                worst = Math.max(worst, Math.abs(pts.get(i)[k] - mirrored.get(i)[k]));
            }
        }
        helper.assertTrue(worst < 1.0E-6, what + ": drawn hammock not centred (" + worst + ")");
        // the ropes end where the line leaves the anchor blocks, the cloth stays between the anchors
        double half = spot.length() / 2;
        for (double[] p : pts) {
            helper.assertTrue(Math.abs(p[0]) <= half, what + ": drawn beyond the anchor centres (" + p[0] + ")");
        }
    }

    /**
     * Standing rods hold a hammock (owner 2026-10-04): posts of two stacked standing rods at 2, 3 and 4 free blocks with
     * sticks, and a blaze, breeze, diamond rod and bone on top as anchors; losing a rod brings the hammock down, one item.
     */
    public static void hangsBetweenStandingRodPosts(GameTestHelper helper) {
        if (!McVersion.HAMMOCK || !McVersion.STANDING_RODS) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        List<StandingRodBlock.Rod> rods = List.of(StandingRodBlock.Rod.STICK, StandingRodBlock.Rod.STICK, StandingRodBlock.Rod.STICK,
                StandingRodBlock.Rod.BLAZE_ROD, StandingRodBlock.Rod.BREEZE_ROD, StandingRodBlock.Rod.DIAMOND_ROD, StandingRodBlock.Rod.BONE);
        for (int x = 0; x < rods.size(); x++) {
            int gap = x < 3 ? x + 2 : 2;
            HammockLayout.Spot spot = straight(x, gap);
            for (BlockPos anchor : List.of(spot.anchor(), spot.otherAnchor())) {
                helper.setBlock(anchor.below(2), Blocks.STONE);
                helper.setBlock(anchor.below(), ModBlocks.STANDING_ROD.defaultBlockState());
                helper.setBlock(anchor, ModBlocks.STANDING_ROD.defaultBlockState().setValue(StandingRodBlock.ROD, rods.get(x)));
            }
            helper.assertTrue(HammockLayout.isAnchor(helper.getLevel(), helper.absolutePos(spot.anchor())), rods.get(x) + " is no anchor");
            InteractionResult result = click(helper, player, spot.anchor());
            helper.assertTrue(result.consumesAction(), rods.get(x) + " posts, gap " + gap + ": refused (" + result + ")");
            assertHangs(helper, spot, rods.get(x) + " posts, gap " + gap);
        }
        // the far stick of the gap-3 hammock goes: the hammock falls, one item
        HammockLayout.Spot three = straight(1, 3);
        helper.setBlock(three.otherAnchor(), Blocks.AIR);
        helper.runAfterDelay(2, () -> {
            assertGone(helper, three, "gap 3 after losing a stick");
            helper.assertValueEqual(dropped(helper, ModItems.WHITE_HAMMOCK), 1, "hammocks dropped");
            helper.succeed();
        });
    }

    /** Two clicks: first anchor remembered, second anchor hangs a 45-degree hammock; a lost corner anchor brings it down. */
    public static void diagonalHammockNeedsTwoClicks(GameTestHelper helper) {
        if (!McVersion.HAMMOCK) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        BlockPos a = new BlockPos(1, ROPE_Y, 1);
        BlockPos b = new BlockPos(4, ROPE_Y, 4);
        helper.setBlock(a, Blocks.STONE);
        helper.setBlock(b, Blocks.STONE);
        helper.assertTrue(click(helper, player, a).consumesAction(), "the first click on an anchor was refused");
        helper.assertValueEqual(HammockItem.storedAnchor(player.getMainHandItem(), helper.getLevel()), Optional.of(helper.absolutePos(a)),
                "remembered first anchor");
        helper.assertTrue(click(helper, player, b).consumesAction(), "the second click did not hang the diagonal hammock");
        helper.assertTrue(HammockItem.storedAnchor(player.getMainHandItem(), helper.getLevel()).isEmpty(), "the first anchor stayed remembered");
        HammockLayout.Spot spot = new HammockLayout.Spot(a, 3, 3);
        helper.assertValueEqual(spot.facing(), Direction.EAST, "diagonal facing (south-east line)");
        assertHangs(helper, spot, "diagonal gap 2");
        BlockPos o = new BlockPos(0, 0, 0);
        helper.assertTrue(HammockLayout.between(o, new BlockPos(6, 0, 6)).isEmpty(), "diagonal gap 5 is allowed");
        helper.assertTrue(HammockLayout.between(o, new BlockPos(0, 1, 3)).isEmpty(), "anchors at different heights");
        helper.assertValueEqual(HammockLayout.between(o, new BlockPos(-5, 0, 5)).map(HammockLayout.Spot::gap), Optional.of(4), "diagonal gap 4");
        helper.assertValueEqual(HammockLayout.between(o, new BlockPos(0, 0, -5)).map(HammockLayout.Spot::facing), Optional.of(Direction.NORTH),
                "straight gap 4 to the north");
        // the diagonal anchor touches its rope cell only at an edge: the cloth head's own check brings it down
        helper.setBlock(b, Blocks.AIR);
        helper.runAfterDelay(HammockBlock.CHECK_TICKS + 5, () -> {
            assertGone(helper, spot, "diagonal after losing an anchor");
            helper.assertValueEqual(dropped(helper, ModItems.WHITE_HAMMOCK), 1, "hammocks dropped by the fallen diagonal one");
            helper.succeed();
        });
    }

    /**
     * Any angle (owner 2026-10-04): two clicks hang 3:1, 5:2 and -3:2 hammocks on exactly the cells worked out by hand
     * (and by tools/textures/hammock.py); too far, too near, a blocked cloth cell and different heights are refused
     * without placing anything; resting lies along the hammock's nearest direction.
     */
    public static void slantedHammocksHangWithTwoClicks(GameTestHelper helper) {
        if (!McVersion.HAMMOCK) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        record Case(HammockLayout.Spot spot, int[][] rope, int[][] cloth, int[] head, Direction facing) {
        }
        List<Case> cases = List.of(
                new Case(new HammockLayout.Spot(new BlockPos(0, ROPE_Y, 0), 3, 1), new int[][] {{1, 0}, {2, 1}},
                        new int[][] {{1, 0}, {2, 1}}, new int[] {2, 1}, Direction.EAST),
                new Case(new HammockLayout.Spot(new BlockPos(0, ROPE_Y, 2), 5, 2),
                        new int[][] {{1, 0}, {1, 1}, {2, 1}, {3, 1}, {4, 1}, {4, 2}}, new int[][] {{2, 1}, {3, 1}}, new int[] {3, 1}, Direction.EAST),
                new Case(new HammockLayout.Spot(new BlockPos(7, ROPE_Y, 0), -3, 2), new int[][] {{-1, 0}, {-1, 1}, {-2, 1}, {-2, 2}},
                        new int[][] {{-1, 0}, {-1, 1}, {-2, 1}, {-2, 2}}, new int[] {-2, 1}, Direction.WEST));
        for (Case c : cases) {
            HammockLayout.Spot spot = c.spot();
            String what = "offset " + spot.dx() + "," + spot.dz();
            helper.assertValueEqual(spot.ropeCells(), cells(spot.anchor(), 0, c.rope()), what + ": rope cells");
            helper.assertValueEqual(spot.clothCells(), cells(spot.anchor(), -1, c.cloth()), what + ": cloth cells");
            helper.assertValueEqual(spot.clothHead(), spot.anchor().offset(c.head()[0], -1, c.head()[1]), what + ": cloth head");
            helper.assertValueEqual(spot.facing(), c.facing(), what + ": facing");
            InteractionResult result = twoClicks(helper, player, spot);
            helper.assertTrue(result.consumesAction(), what + ": the second click was refused (" + result + ")");
            assertHangs(helper, spot, what);
        }
        // refusals: nothing placed, the item stays
        HammockLayout.Spot blocked = new HammockLayout.Spot(new BlockPos(7, ROPE_Y, 5), -3, 2);
        helper.setBlock(blocked.clothCells().get(2), Blocks.STONE);
        HammockLayout.Spot far = new HammockLayout.Spot(new BlockPos(0, ROPE_Y, 7), 6, -1);
        HammockLayout.Spot near = new HammockLayout.Spot(new BlockPos(0, ROPE_Y, 5), 2, 1);
        for (HammockLayout.Spot refused : List.of(blocked, far, near)) {
            String what = "refused offset " + refused.dx() + "," + refused.dz();
            ItemStack held = hold(player);
            int before = held.getCount();
            helper.assertFalse(twoClicks(helper, player, refused).consumesAction(), what + ": hung anyway");
            helper.assertValueEqual(held.getCount(), before, what + ": item used up");
            for (BlockPos cell : refused.ropeCells()) {
                helper.assertFalse(isHammock(at(helper, cell)) || isHammock(at(helper, cell.below())), what + ": block placed at " + cell.toShortString());
            }
        }
        helper.assertTrue(HammockLayout.between(new BlockPos(0, 0, 0), new BlockPos(4, 1, 2)).isEmpty(), "anchors at different heights");
        helper.assertTrue(HammockLayout.between(new BlockPos(0, 0, 0), new BlockPos(2, 0, 2)).isEmpty(), "one free cell is allowed");
        // resting: vanilla lies the player along the nearest direction (the client turns the body onto the line)
        BlockPos head = helper.absolutePos(cases.get(1).spot().clothHead());
        helper.assertTrue(HammockBlock.rest(player, head) && player.isSleeping(), "the player did not lie down in the 5:2 hammock");
        helper.assertValueEqual(player.getBedOrientation(), Direction.EAST, "lying direction in the 5:2 hammock");
        player.stopSleepInBed(false, true);
        helper.succeed();
    }

    /** A slanted hammock falls as a whole when its corner anchor (5:3) or one rope cell (4:-1) goes, one item each. */
    public static void slantedHammockFallsOnceWhenItsAnchorOrRopeGoes(GameTestHelper helper) {
        if (!McVersion.HAMMOCK) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        HammockLayout.Spot anchorLost = new HammockLayout.Spot(new BlockPos(0, ROPE_Y, 0), 5, 3);
        HammockLayout.Spot ropeBroken = new HammockLayout.Spot(new BlockPos(0, ROPE_Y, 7), 4, -1);
        for (HammockLayout.Spot spot : List.of(anchorLost, ropeBroken)) {
            helper.assertTrue(twoClicks(helper, player, spot).consumesAction(), "offset " + spot.dx() + "," + spot.dz() + " was refused");
            assertHangs(helper, spot, "offset " + spot.dx() + "," + spot.dz());
        }
        helper.setBlock(anchorLost.otherAnchor(), Blocks.AIR);
        player.gameMode.destroyBlock(helper.absolutePos(ropeBroken.ropeCells().get(2)));
        helper.runAfterDelay(HammockBlock.CHECK_TICKS + 5, () -> {
            assertGone(helper, anchorLost, "5:3 after losing its anchor");
            assertGone(helper, ropeBroken, "4:-1 after breaking a rope");
            helper.assertValueEqual(dropped(helper, ModItems.WHITE_HAMMOCK), 2, "hammocks dropped (one each)");
            helper.succeed();
        });
    }

    /** Removing either anchor (gap 2 and gap 4) brings the whole hammock down, one item each. */
    public static void losingAnAnchorDropsTheHammockOnce(GameTestHelper helper) {
        if (!McVersion.HAMMOCK) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        HammockLayout.Spot shorter = hang(helper, player, 1, 2);
        HammockLayout.Spot longer = hang(helper, player, 3, 4);
        helper.setBlock(shorter.anchor(), Blocks.AIR);
        helper.setBlock(longer.otherAnchor(), Blocks.AIR);
        helper.runAfterDelay(3, () -> {
            assertNoHammockIn(helper, 1, "first anchor removed");
            assertNoHammockIn(helper, 3, "second anchor removed");
            helper.assertValueEqual(dropped(helper, ModItems.WHITE_HAMMOCK), 2, "hammocks dropped for two lost anchors");
            helper.succeed();
        });
    }

    /** Survival: breaking a rope drops the hammock once; creative: breaking a cloth cell drops nothing. */
    public static void breakingOnePartDropsOnceExceptInCreative(GameTestHelper helper) {
        if (!McVersion.HAMMOCK) {
            helper.succeed();
            return;
        }
        ServerPlayer survival = player(helper, GameType.SURVIVAL);
        HammockLayout.Spot first = hang(helper, survival, 1, 2);
        survival.gameMode.destroyBlock(helper.absolutePos(first.ropeCells().get(0)));
        ServerPlayer creative = player(helper, GameType.CREATIVE);
        HammockLayout.Spot second = hang(helper, creative, 4, 2);
        creative.gameMode.destroyBlock(helper.absolutePos(second.clothCells().get(0)));
        helper.runAfterDelay(3, () -> {
            assertNoHammockIn(helper, 1, "survival break");
            assertNoHammockIn(helper, 4, "creative break");
            helper.assertValueEqual(dropped(helper, ModItems.WHITE_HAMMOCK), 1, "hammocks dropped (survival one, creative none)");
            helper.succeed();
        });
    }

    /**
     * Resting, day or night: the player lies down in vanilla's sleeping pose along the hammock, the phantom statistic
     * stays, the sleep counter never reaches "long enough" (no skip to the morning, no respawn point), and the clock gets
     * factor - 1 extra ticks while all active players rest (a standing player counts; mock players cannot be spectators,
     * so that part is vanilla's SleepStatus rule).
     */
    public static void restingKeepsThePhantomTimerAndSpeedsTheClock(GameTestHelper helper) {
        if (!McVersion.HAMMOCK) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        HammockLayout.Spot spot = hang(helper, player, 1, 2);
        BlockPos head = helper.absolutePos(spot.clothHead());
        Stat<?> sinceRest = Stats.CUSTOM.get(Stats.TIME_SINCE_REST);
        player.getStats().setValue(player, sinceRest, 5000);
        helper.assertTrue(HammockTime.restAllowed(level), "a hammock is not usable in the overworld at this time of day");
        helper.assertTrue(HammockBlock.rest(player, head) && player.isSleeping() && HammockTime.inHammock(player),
                "the player did not lie down");
        helper.assertValueEqual(player.getPose(), Pose.SLEEPING, "pose");
        helper.assertValueEqual(player.getBedOrientation(), Direction.SOUTH, "lying direction");
        helper.assertTrue(Math.abs(player.getY() - (head.getY() + HammockBlock.SLEEP_HEIGHT + 0.125)) < 1.0E-3, "lying height " + player.getY());
        helper.assertValueEqual(player.getStats().getValue(sinceRest), 5000, "the hammock reset the phantom statistic");
        helper.assertTrue(level.getBlockState(head).getValue(HammockBlock.OCCUPIED), "the cloth head is not occupied");
        helper.assertFalse(HammockBlock.rest(player(helper, GameType.SURVIVAL), head), "two players in one hammock");
        for (int i = 0; i < 120; i++) {
            player.doTick();
        }
        helper.assertTrue(player.isSleeping(), "the player woke up");
        helper.assertValueEqual(player.getSleepTimer(), HammockTime.DOZE_TICKS, "sleep counter");
        helper.assertFalse(player.isSleepingLongEnough(), "a hammock sleeper counts as sleeping long enough (would skip the night)");
        helper.assertTrue(player.getRespawnConfig() == null || !head.equals(player.getRespawnConfig().respawnData().pos()),
                "the hammock set the respawn point");

        GameRules rules = level.getGameRules();
        boolean advance = rules.get(GameRules.ADVANCE_TIME);
        try {
            rules.set(GameRules.ADVANCE_TIME, true, level.getServer());
            int factor = ServerTuning.hammockTimeFactor();
            helper.assertValueEqual(HammockTime.extraTicks(level, List.of(player)), factor - 1, "extra ticks while resting");
            ServerPlayer standing = player(helper, GameType.SURVIVAL);
            int percentage = rules.get(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
            int expected = HammockTime.restersNeeded(2, percentage) <= 1 ? factor - 1 : 0;
            helper.assertValueEqual(HammockTime.extraTicks(level, List.of(player, standing)), expected,
                    "extra ticks with one of two players resting at " + percentage + " %");
            rules.set(GameRules.ADVANCE_TIME, false, level.getServer());
            helper.assertValueEqual(HammockTime.extraTicks(level, List.of(player)), 0, "extra ticks without advance_time");
        } finally {
            rules.set(GameRules.ADVANCE_TIME, advance, level.getServer());
        }
        player.stopSleepInBed(false, true);
        helper.assertFalse(player.isSleeping(), "leaving the hammock failed");
        helper.assertValueEqual(player.getStats().getValue(sinceRest), 5000, "leaving the hammock changed the phantom statistic");
        helper.succeed();
    }

    /** The share of players like vanilla sleeping, day and night alike; game rules respected, at least one, rounded up. */
    public static void clockSpeedsUpWithEnoughRestersDayAndNight(GameTestHelper helper) {
        helper.assertValueEqual(HammockTime.extraTicks(1, 1, 100, true, true, 8), 7, "one player resting alone");
        helper.assertValueEqual(HammockTime.extraTicks(1, 1, 100, false, true, 8), 0, "a dimension without a day clock");
        helper.assertValueEqual(HammockTime.extraTicks(1, 1, 100, true, false, 8), 0, "advance_time off");
        helper.assertValueEqual(HammockTime.extraTicks(1, 1, 101, true, true, 8), 0, "players_sleeping_percentage above 100");
        helper.assertValueEqual(HammockTime.extraTicks(3, 1, 50, true, true, 8), 0, "1 of 3 at 50 %");
        helper.assertValueEqual(HammockTime.extraTicks(3, 2, 50, true, true, 8), 7, "2 of 3 at 50 %");
        helper.assertValueEqual(HammockTime.extraTicks(4, 3, 100, true, true, 8), 0, "3 of 4 at 100 %");
        helper.assertValueEqual(HammockTime.extraTicks(4, 4, 100, true, true, 8), 7, "4 of 4 at 100 %");
        helper.assertValueEqual(HammockTime.extraTicks(10, 1, 0, true, true, 8), 7, "1 of 10 at 0 % (at least one)");
        helper.assertValueEqual(HammockTime.extraTicks(10, 0, 0, true, true, 8), 0, "nobody resting at 0 %");
        helper.assertValueEqual(HammockTime.extraTicks(2, 2, 100, true, true, 1), 0, "factor 1 is off");
        helper.assertValueEqual(HammockTime.restersNeeded(5, 30), 2, "30 % of 5 rounds up");
        helper.assertValueEqual(HammockTime.restersNeeded(0, 100), 1, "never fewer than one");
        // day and night: the overworld allows resting whatever its time of day; the nether and the end never
        helper.assertTrue(HammockTime.restAllowed(helper.getLevel()), "the overworld does not allow resting now");
        ServerLevel nether = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        if (nether != null) {
            helper.assertFalse(HammockTime.restAllowed(nether), "the nether allows resting");
        }
        helper.succeed();
    }

    /** Recipe: String, Stick, String over three wool of one colour; the old Stick, String, Stick no longer makes one. */
    public static void recipeTakesTwoStringsOneStickAndThreeWool(GameTestHelper helper) {
        if (!McVersion.HAMMOCK) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        ItemStack s = new ItemStack(Items.STRING);
        ItemStack k = new ItemStack(Items.STICK);
        ItemStack w = new ItemStack(Items.WOOL.pick(net.minecraft.world.item.DyeColor.RED));
        CraftingInput grid = CraftingInput.of(3, 2, List.of(s, k, s, w, w, w));
        Optional<RecipeHolder<CraftingRecipe>> match = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
        helper.assertTrue(match.isPresent(), "String, Stick, String over red wool crafts nothing");
        helper.assertTrue(match.get().value().assemble(grid).is(ModItems.RED_HAMMOCK), "the recipe does not make a red hammock");
        CraftingInput old = CraftingInput.of(3, 2, List.of(k, s, k, w, w, w));
        Optional<RecipeHolder<CraftingRecipe>> oldMatch = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, old, level);
        helper.assertFalse(oldMatch.isPresent() && oldMatch.get().value().assemble(old).is(ModItems.RED_HAMMOCK), "the old recipe still works");
        helper.succeed();
    }

    /** server.hammock.timeFactor: default 8, hard limits 1 and 20 whatever the file says. */
    public static void timeFactorIsCappedOnTheServer(GameTestHelper helper) {
        var tuning = ServerTuning.get().hammock;
        int before = tuning.timeFactor;
        try {
            helper.assertValueEqual(new com.simplebuilding.config.ServerTuningConfig.Hammock().timeFactor, 8, "default factor");
            tuning.timeFactor = 1000;
            helper.assertValueEqual(ServerTuning.hammockTimeFactor(), ServerTuning.MAX_HAMMOCK_FACTOR, "factor above the cap");
            helper.assertValueEqual(ServerTuning.MAX_HAMMOCK_FACTOR, 20, "hard cap");
            tuning.timeFactor = -3;
            helper.assertValueEqual(ServerTuning.hammockTimeFactor(), 1, "factor below 1");
            tuning.timeFactor = 6;
            helper.assertValueEqual(ServerTuning.hammockTimeFactor(), 6, "factor inside the limits");
        } finally {
            tuning.timeFactor = before;
        }
        helper.succeed();
    }
}
