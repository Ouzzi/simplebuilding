package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.HammockBlock;
import com.simplebuilding.blocks.custom.HammockLayout;
import com.simplebuilding.blocks.custom.HammockRopeBlock;
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
 * Hammock (docs/ai/PLAN-HAENGEMATTE-2026-10-02.md, v2): placement between two anchors 2 to 4 apart, straight in one click
 * or diagonal in two, the cloth in the middle, falling and dropping once, resting day and night, the clock speed-up and
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

    /** Asserts the white hammock {@code spot} (relative): rope cells everywhere, cloth only under the cloth, head in place. */
    private static void assertHangs(GameTestHelper helper, HammockLayout.Spot spot, String what) {
        for (int i = 0; i < spot.gap(); i++) {
            BlockState rope = at(helper, spot.rope(i));
            helper.assertTrue(rope.is(ModBlocks.HAMMOCK_ROPE) && rope.getValue(HammockRopeBlock.FACING) == spot.facing()
                    && rope.getValue(HammockLayout.DIAGONAL) == spot.diagonal() && rope.getValue(HammockLayout.GAP) == spot.gap()
                    && rope.getValue(HammockLayout.INDEX) == i, what + ": rope cell " + i + " is " + rope);
            BlockState below = at(helper, spot.rope(i).below());
            boolean cloth = HammockLayout.clothCells(spot.gap()).contains(i);
            if (cloth) {
                boolean head = i == HammockLayout.headCell(spot.gap());
                helper.assertTrue(below.is(ModBlocks.WHITE_HAMMOCK) && below.getValue(HammockLayout.INDEX) == i
                        && (below.getValue(HammockBlock.PART) == BedPart.HEAD) == head, what + ": cloth cell " + i + " is " + below);
            } else {
                helper.assertTrue(below.isAir(), what + ": cell " + i + " under the ropes is not free: " + below);
            }
        }
        helper.assertTrue(HammockLayout.intact(helper.getLevel(), helper.absolutePos(spot.clothHead()),
                helper.getLevel().getBlockState(helper.absolutePos(spot.clothHead()))), what + ": not intact");
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
        return new HammockLayout.Spot(new BlockPos(x, ROPE_Y, 1), Direction.SOUTH, false, gap);
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

    /** The cloth hangs in the middle at every gap: cloth cells, head cell and the lying player's shift along the line. */
    public static void clothHangsInTheMiddleAtEveryGap(GameTestHelper helper) {
        helper.assertValueEqual(HammockLayout.clothCells(2), List.of(0, 1), "cloth cells, gap 2");
        helper.assertValueEqual(HammockLayout.clothCells(3), List.of(0, 1, 2), "cloth cells, gap 3");
        helper.assertValueEqual(HammockLayout.clothCells(4), List.of(1, 2), "cloth cells, gap 4");
        helper.assertValueEqual(HammockLayout.headCell(2), 1, "head, gap 2");
        helper.assertValueEqual(HammockLayout.headCell(3), 1, "head, gap 3");
        helper.assertValueEqual(HammockLayout.headCell(4), 2, "head, gap 4");
        // the head point is the cloth middle + half a block: on the head cell's centre for 2 and 4, half a block on for 3
        helper.assertTrue(Math.abs(HammockLayout.headShift(2, false)) < 1.0E-9, "shift, gap 2");
        helper.assertTrue(Math.abs(HammockLayout.headShift(3, false) - 0.5) < 1.0E-9, "shift, gap 3");
        helper.assertTrue(Math.abs(HammockLayout.headShift(4, false)) < 1.0E-9, "shift, gap 4");
        helper.assertTrue(Math.abs(HammockLayout.headShift(3, true) - 0.5) < 1.0E-9, "shift, diagonal gap 3");
        helper.assertTrue(Math.abs(HammockLayout.headShift(2, true) - (0.5 - 0.5 * Math.sqrt(2.0))) < 1.0E-9, "shift, diagonal gap 2");
        // symmetry: the cells under the cloth mirror around the middle of the gap
        for (int gap = HammockLayout.MIN_GAP; gap <= HammockLayout.MAX_GAP; gap++) {
            for (int i : HammockLayout.clothCells(gap)) {
                helper.assertTrue(HammockLayout.clothCells(gap).contains(gap - 1 - i), "cloth cells not symmetric for gap " + gap);
            }
        }
        helper.succeed();
    }

    /**
     * Symmetry in the world (owner 2026-10-04: "still not centred"): for every facing, straight and diagonal, 2 to 4 free
     * cells, the middle between the two anchor centres plus half a block towards the second anchor (the head point) is
     * exactly where the lying player is drawn - the centre of the cloth head cell moved by {@code headShift} along the
     * line - and the cloth cells lie symmetrically around that middle.
     */
    public static void clothMiddleSitsBetweenTheAnchorsInEveryDirection(GameTestHelper helper) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (boolean diagonal : new boolean[] {false, true}) {
                for (int gap = HammockLayout.MIN_GAP; gap <= HammockLayout.MAX_GAP; gap++) {
                    HammockLayout.Spot spot = new HammockLayout.Spot(new BlockPos(10, 64, -7), facing, diagonal, gap);
                    BlockPos step = spot.step();
                    double len = Math.sqrt(step.getX() * step.getX() + step.getZ() * step.getZ());
                    double ux = step.getX() / len;
                    double uz = step.getZ() / len;
                    double midX = (spot.anchor().getX() + spot.otherAnchor().getX()) / 2.0 + 0.5;
                    double midZ = (spot.anchor().getZ() + spot.otherAnchor().getZ()) / 2.0 + 0.5;
                    double shift = HammockLayout.headShift(gap, diagonal);
                    double drawnX = spot.clothHead().getX() + 0.5 + shift * ux;
                    double drawnZ = spot.clothHead().getZ() + 0.5 + shift * uz;
                    String what = facing + (diagonal ? " diagonal " : " straight ") + gap;
                    helper.assertTrue(Math.abs(drawnX - (midX + 0.5 * ux)) < 1.0E-9 && Math.abs(drawnZ - (midZ + 0.5 * uz)) < 1.0E-9,
                            what + ": head point " + drawnX + "," + drawnZ + " is not the middle + half a block " + midX + "," + midZ);
                    // cloth cells mirror around the middle: their centres average to it
                    double sx = 0;
                    double sz = 0;
                    for (int i : HammockLayout.clothCells(gap)) {
                        sx += spot.rope(i).getX() + 0.5;
                        sz += spot.rope(i).getZ() + 0.5;
                    }
                    int n = HammockLayout.clothCells(gap).size();
                    helper.assertTrue(Math.abs(sx / n - midX) < 1.0E-9 && Math.abs(sz / n - midZ) < 1.0E-9, what + ": cloth cells off the middle");
                    helper.assertValueEqual(spot.otherAnchor(), spot.rope(gap), what + ": second anchor");
                }
            }
        }
        helper.succeed();
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
        for (int i = 0; i < 3; i++) {
            helper.assertFalse(isHammock(at(helper, three.rope(i))) || isHammock(at(helper, three.rope(i).below())),
                    "the hammock still hangs at cell " + i);
        }
        helper.assertValueEqual(dropped(helper, ModItems.WHITE_HAMMOCK), 1, "hammocks dropped");
        helper.succeed();
    }

    /** Two clicks: first anchor remembered, second anchor hangs a 45-degree hammock; knight moves and gap 5 do not. */
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
        HammockLayout.Spot spot = new HammockLayout.Spot(a, Direction.EAST, true, 2);
        helper.assertValueEqual(spot.otherAnchor(), b, "diagonal line");
        assertHangs(helper, spot, "diagonal gap 2");
        BlockPos o = new BlockPos(0, 0, 0);
        helper.assertTrue(HammockLayout.between(o, new BlockPos(2, 0, 3)).isEmpty(), "a knight move is a hammock line");
        helper.assertTrue(HammockLayout.between(o, new BlockPos(6, 0, 6)).isEmpty(), "diagonal gap 5 is allowed");
        helper.assertTrue(HammockLayout.between(o, new BlockPos(0, 1, 3)).isEmpty(), "anchors at different heights");
        helper.assertValueEqual(HammockLayout.between(o, new BlockPos(-5, 0, 5)).map(HammockLayout.Spot::gap), Optional.of(4), "diagonal gap 4");
        helper.assertValueEqual(HammockLayout.between(o, new BlockPos(0, 0, -5)).map(HammockLayout.Spot::facing), Optional.of(Direction.NORTH),
                "straight gap 4 to the north");
        // the diagonal anchor touches its rope cell only at an edge: the cloth head's own check brings it down
        helper.setBlock(b, Blocks.AIR);
        helper.runAfterDelay(HammockBlock.CHECK_TICKS + 5, () -> {
            for (int i = 0; i < 2; i++) {
                helper.assertFalse(isHammock(at(helper, spot.rope(i))) || isHammock(at(helper, spot.rope(i).below())),
                        "diagonal hammock still hangs at cell " + i);
            }
            helper.assertValueEqual(dropped(helper, ModItems.WHITE_HAMMOCK), 1, "hammocks dropped by the fallen diagonal one");
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
        survival.gameMode.destroyBlock(helper.absolutePos(first.rope(0)));
        ServerPlayer creative = player(helper, GameType.CREATIVE);
        HammockLayout.Spot second = hang(helper, creative, 4, 2);
        creative.gameMode.destroyBlock(helper.absolutePos(second.rope(0).below()));
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
