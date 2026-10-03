package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.HammockBlock;
import com.simplebuilding.blocks.custom.HammockLayout;
import com.simplebuilding.blocks.custom.HammockRopeBlock;
import com.simplebuilding.blocks.custom.HammockTime;
import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import java.util.List;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Hammock (docs/ai/PLAN-HAENGEMATTE-2026-10-02.md): placement between two anchors 2 or 3 apart, falling and dropping
 * once, resting by day, the clock speed-up and its share of players, the capped server option. Loader-neutral; on lines
 * without {@link McVersion#HAMMOCK} the tests pass at once.
 */
public final class HammockTests {
    private HammockTests() {
    }

    /** Anchors on rope height y=3, the cloth hangs at y=2; columns run south. */
    private static final int ROPE_Y = 3;

    private static ServerPlayer player(GameTestHelper helper, GameType mode) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(mode);
        Vec3 corner = helper.absoluteVec(new Vec3(7.5, 1.0, 7.5));
        player.setPos(corner.x, corner.y, corner.z);
        player.setYRot(0.0F); // facing south
        return player;
    }

    /** Clicks the south face of the block at {@code anchor} (relative) with a white hammock. */
    private static InteractionResult hangFrom(GameTestHelper helper, ServerPlayer player, BlockPos anchor) {
        ItemStack stack = new ItemStack(ModItems.WHITE_HAMMOCK);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = helper.absolutePos(anchor);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).add(0.0, 0.0, 0.5), Direction.SOUTH, abs, false);
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static BlockState at(GameTestHelper helper, int x, int y, int z) {
        return helper.getBlockState(new BlockPos(x, y, z));
    }

    private static boolean isHammock(BlockState state) {
        return state.getBlock() instanceof HammockBlock || state.getBlock() instanceof HammockRopeBlock;
    }

    /** Asserts the white hammock that hangs from (x, ROPE_Y, z0) southwards across {@code gap} blocks. */
    private static void assertHangs(GameTestHelper helper, int x, int z0, int gap) {
        BlockState foot = at(helper, x, ROPE_Y - 1, z0 + 1);
        BlockState head = at(helper, x, ROPE_Y - 1, z0 + 2);
        helper.assertTrue(foot.is(ModBlocks.WHITE_HAMMOCK) && foot.getValue(HammockBlock.PART) == BedPart.FOOT
                && foot.getValue(HammockBlock.FACING) == Direction.SOUTH, "gap " + gap + ": no cloth foot, got " + foot);
        helper.assertTrue(head.is(ModBlocks.WHITE_HAMMOCK) && head.getValue(HammockBlock.PART) == BedPart.HEAD,
                "gap " + gap + ": no cloth head, got " + head);
        BlockState footRope = at(helper, x, ROPE_Y, z0 + 1);
        BlockState headRope = at(helper, x, ROPE_Y, z0 + 2);
        helper.assertTrue(footRope.is(ModBlocks.HAMMOCK_ROPE) && footRope.getValue(HammockRopeBlock.FACING) == Direction.NORTH
                && footRope.getValue(HammockRopeBlock.KIND) == HammockRopeBlock.Kind.END, "gap " + gap + ": foot rope " + footRope);
        helper.assertTrue(headRope.is(ModBlocks.HAMMOCK_ROPE) && headRope.getValue(HammockRopeBlock.FACING) == Direction.SOUTH
                && headRope.getValue(HammockRopeBlock.KIND) == HammockRopeBlock.Kind.END, "gap " + gap + ": head rope " + headRope);
        BlockState span = at(helper, x, ROPE_Y, z0 + 3);
        if (gap == 3) {
            helper.assertTrue(span.is(ModBlocks.HAMMOCK_ROPE) && span.getValue(HammockRopeBlock.KIND) == HammockRopeBlock.Kind.SPAN,
                    "gap 3: no rope span in front of the head anchor, got " + span);
        } else {
            helper.assertTrue(span.is(Blocks.STONE), "gap 2: the head anchor was replaced: " + span);
        }
    }

    private static void assertNoHammockIn(GameTestHelper helper, int x, String what) {
        for (int y = 1; y <= ROPE_Y + 1; y++) {
            for (int z = 0; z < 8; z++) {
                helper.assertFalse(isHammock(at(helper, x, y, z)), what + ": hammock block left at " + x + "," + y + "," + z);
            }
        }
    }

    private static int dropped(GameTestHelper helper, Item item) {
        AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(0.0).expandTowards(8.0, 6.0, 8.0);
        int count = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, box, e -> e.getItem().is(item))) {
            count += entity.getItem().getCount();
        }
        return count;
    }

    /** Builds a hammock between stone anchors at (x, ROPE_Y, 1) and (x, ROPE_Y, 2 + gap) through the item. */
    private static void hang(GameTestHelper helper, ServerPlayer player, int x, int gap) {
        helper.setBlock(new BlockPos(x, ROPE_Y, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(x, ROPE_Y, 2 + gap), Blocks.STONE);
        InteractionResult result = hangFrom(helper, player, new BlockPos(x, ROPE_Y, 1));
        helper.assertTrue(result.consumesAction(), "gap " + gap + ": the hammock was refused (" + result + ")");
    }

    /** Gap 1 and 4 fail, 2 and 3 hang (3 with a rope span); one anchor alone fails; a fence holds like a block. */
    public static void hangsOnlyBetweenTwoAnchorsTwoOrThreeApart(GameTestHelper helper) {
        if (!McVersion.HAMMOCK) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        for (int gap = 1; gap <= 4; gap++) {
            int x = gap;
            helper.setBlock(new BlockPos(x, ROPE_Y, 1), Blocks.STONE);
            helper.setBlock(new BlockPos(x, ROPE_Y, 2 + gap), Blocks.STONE);
            InteractionResult result = hangFrom(helper, player, new BlockPos(x, ROPE_Y, 1));
            if (gap == 2 || gap == 3) {
                helper.assertTrue(result.consumesAction(), "gap " + gap + " was refused: " + result);
                assertHangs(helper, x, 1, gap);
            } else {
                helper.assertTrue(result == InteractionResult.FAIL, "gap " + gap + " was not refused: " + result);
                assertNoHammockIn(helper, x, "gap " + gap);
            }
        }
        // One anchor alone: nothing to tie the head to.
        helper.setBlock(new BlockPos(5, ROPE_Y, 1), Blocks.STONE);
        helper.assertTrue(hangFrom(helper, player, new BlockPos(5, ROPE_Y, 1)) == InteractionResult.FAIL, "one anchor was enough");
        assertNoHammockIn(helper, 5, "one anchor");
        // Fences hold a hammock like full blocks (rods and walls the same: any collision shape).
        helper.setBlock(new BlockPos(6, ROPE_Y, 1), Blocks.OAK_FENCE);
        helper.setBlock(new BlockPos(6, ROPE_Y, 4), Blocks.OAK_FENCE);
        helper.assertTrue(hangFrom(helper, player, new BlockPos(6, ROPE_Y, 1)).consumesAction(), "fences did not hold the hammock");
        helper.assertTrue(at(helper, 6, ROPE_Y - 1, 3).is(ModBlocks.WHITE_HAMMOCK), "no cloth head between the fences");
        helper.assertTrue(HammockLayout.isAnchor(helper.getLevel(), helper.absolutePos(new BlockPos(6, ROPE_Y, 1))), "a fence is no anchor");
        helper.assertFalse(HammockLayout.isAnchor(helper.getLevel(), helper.absolutePos(new BlockPos(7, ROPE_Y, 1))), "air is an anchor");
        helper.succeed();
    }

    /** Removing either anchor (with and without rope span) or breaking a part brings the whole hammock down, one item. */
    public static void losingAnAnchorDropsTheHammockOnce(GameTestHelper helper) {
        if (!McVersion.HAMMOCK) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        hang(helper, player, 1, 2);
        hang(helper, player, 3, 3);
        helper.setBlock(new BlockPos(1, ROPE_Y, 1), Blocks.AIR); // foot anchor of the short one
        helper.setBlock(new BlockPos(3, ROPE_Y, 5), Blocks.AIR); // head anchor of the long one (behind the rope span)
        helper.runAfterDelay(2, () -> {
            assertNoHammockIn(helper, 1, "foot anchor removed");
            assertNoHammockIn(helper, 3, "head anchor removed");
            helper.assertValueEqual(dropped(helper, ModItems.WHITE_HAMMOCK), 2, "hammocks dropped for two lost anchors");
            helper.succeed();
        });
    }

    /** Survival: breaking a rope drops the hammock once; creative: breaking the cloth foot drops nothing. */
    public static void breakingOnePartDropsOnceExceptInCreative(GameTestHelper helper) {
        if (!McVersion.HAMMOCK) {
            helper.succeed();
            return;
        }
        ServerPlayer survival = player(helper, GameType.SURVIVAL);
        hang(helper, survival, 1, 2);
        survival.gameMode.destroyBlock(helper.absolutePos(new BlockPos(1, ROPE_Y, 2)));
        ServerPlayer creative = player(helper, GameType.CREATIVE);
        hang(helper, creative, 4, 2);
        creative.gameMode.destroyBlock(helper.absolutePos(new BlockPos(4, ROPE_Y - 1, 2)));
        helper.runAfterDelay(2, () -> {
            assertNoHammockIn(helper, 1, "survival break");
            assertNoHammockIn(helper, 4, "creative break");
            helper.assertValueEqual(dropped(helper, ModItems.WHITE_HAMMOCK), 1, "hammocks dropped (survival one, creative none)");
            helper.succeed();
        });
    }

    /**
     * Resting: by day the player lies down in vanilla's sleeping pose along the hammock, the phantom statistic stays,
     * the sleep counter never reaches "long enough", and the clock gets factor - 1 extra ticks while all active players
     * rest (a standing player counts; mock players cannot be spectators, so that part is vanilla's SleepStatus rule). At night (the test server's fixed time decides which
     * branch runs) lying down is refused.
     */
    public static void restingByDayKeepsThePhantomTimerAndSpeedsTheClock(GameTestHelper helper) {
        if (!McVersion.HAMMOCK) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        hang(helper, player, 1, 2);
        BlockPos head = helper.absolutePos(new BlockPos(1, ROPE_Y - 1, 3));
        Stat<?> sinceRest = Stats.CUSTOM.get(Stats.TIME_SINCE_REST);
        player.getStats().setValue(player, sinceRest, 5000);
        boolean day = HammockTime.restAllowed(level);
        helper.assertValueEqual(day, level.isBrightOutside() && level.dimensionType().defaultClock().isPresent(), "rest rule");
        boolean rested = HammockBlock.rest(player, head);
        if (!day) {
            helper.assertFalse(rested || player.isSleeping(), "a hammock was usable at night");
            helper.assertValueEqual(HammockTime.extraTicks(level, List.of(player)), 0, "extra ticks at night");
            helper.succeed();
            return;
        }
        helper.assertTrue(rested && player.isSleeping() && HammockTime.inHammock(player), "the player did not lie down by day");
        helper.assertValueEqual(player.getPose(), Pose.SLEEPING, "pose");
        helper.assertValueEqual(player.getBedOrientation(), Direction.SOUTH, "lying direction");
        helper.assertTrue(Math.abs(player.getY() - (head.getY() + HammockBlock.SLEEP_HEIGHT + 0.125)) < 1.0E-3, "lying height " + player.getY());
        helper.assertValueEqual(player.getStats().getValue(sinceRest), 5000, "the hammock reset the phantom statistic");
        helper.assertTrue(level.getBlockState(head).getValue(HammockBlock.OCCUPIED), "the cloth head is not occupied");
        helper.assertFalse(HammockBlock.rest(player(helper, GameType.SURVIVAL), head), "two players in one hammock");
        for (int i = 0; i < 120; i++) {
            player.doTick();
        }
        helper.assertTrue(player.isSleeping(), "the player woke up by day");
        helper.assertValueEqual(player.getSleepTimer(), HammockTime.DOZE_TICKS, "sleep counter");
        helper.assertFalse(player.isSleepingLongEnough(), "a hammock sleeper counts as sleeping long enough (would skip the day)");

        GameRules rules = level.getGameRules();
        boolean advance = rules.get(GameRules.ADVANCE_TIME);
        try {
            rules.set(GameRules.ADVANCE_TIME, true, level.getServer());
            int factor = ServerTuning.hammockTimeFactor();

            helper.assertValueEqual(HammockTime.extraTicks(level, List.of(player)), factor - 1, "extra ticks by day");
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

    /** The share of players like vanilla sleeping: day only, game rules respected, at least one, rounded up. */
    public static void clockSpeedsUpOnlyByDayWithEnoughResters(GameTestHelper helper) {
        helper.assertValueEqual(HammockTime.extraTicks(1, 1, 100, true, true, 8), 7, "one player resting alone by day");
        helper.assertValueEqual(HammockTime.extraTicks(1, 1, 100, false, true, 8), 0, "at night");
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
