package com.simplebuilding.gametest;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.MiningUtils;
import com.simplebuilding.util.SledgehammerUsageEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Server side behaviour of the mod's tools: sledgehammer area mining (including the
 * Override enchantment tiers), chisel/spatula block transformation, the Vein Miner and
 * Strip Miner block selection and the magnet's item attraction.
 *
 * <p>Every test builds a real situation inside the 8x8x8 gametest room and then inspects
 * the world afterwards. Every one of them is registered with an unrotated structure (see
 * {@link SimpleBuildingGameTests}) because several depend on absolute directions such as
 * player facing and mining direction.
 *
 * <h2>Why several tests run the same call more than once</h2>
 *
 * <p>The sledgehammer and the two miner enchantments are all built as "one formula, several
 * inputs": a tool tier decides which neighbours are legal, a look direction decides which
 * plane is taken, an enchantment level decides how deep or how wide the effect reaches. A
 * single sample can only ever prove that the formula returns <em>something</em>. Each of the
 * tests below therefore drives the same entry point once per branch it claims to cover -
 * every tool tier that is supposed to behave differently, every look direction the code
 * distinguishes, and every enchantment level the enchantment can actually be rolled at.
 * Sampling only the strongest tool or only one camera angle is what let earlier versions of
 * these tests stay green while whole conditions were deleted from the mod.
 */
public final class ToolBehaviourTests {

    /** Tick budget for {@link #magnetPullsNearbyItemsAndIgnoresDistantOnes}. */
    public static final int MAGNET_MAX_TICKS = 200;

    /** Centre of the horizontal block field used by the sledgehammer tests. */
    private static final BlockPos HAMMER_CENTRE = new BlockPos(3, 1, 3);

    // =====================================================================================
    // SLEDGEHAMMER
    // =====================================================================================

    /**
     * A sledgehammer swing has to take the whole 3x3 face around the mined block with it -
     * no more, no less - and that face has to stand in the plane the player is looking at.
     *
     * <p>The three phases drive the three branches of {@code SledgehammerItem}'s own
     * {@code getHitSideFromPlayer}: looking down (pitch &gt; 60) and looking up (pitch &lt;
     * -60) both give a horizontal face, while any flatter angle gives an upright face
     * perpendicular to the player's facing. Only the third phase can tell those two shapes
     * apart, so it is the one that keeps the whole method honest.
     *
     * <p>What breaks this: a radius that is not exactly 1; a hook that also destroys the
     * origin (vanilla already does that, which is why the origin is expected to survive
     * here); and any collapse of {@code getHitSideFromPlayer} - hard-wiring it to
     * {@code Direction.UP} used to leave this test completely green even though a swing
     * against a wall then peeled off the floor instead of the wall.
     */
    public static void sledgehammerBreaksThreeByThreeAroundOrigin(GameTestHelper helper) {
        fillLayer(helper, 1, 1, 5, 1, 5, Blocks.STONE);

        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 3.5), 0.0F, 90.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER));

        // --- Looking straight down: the face lies flat. ---
        swingSledgehammer(helper, player, HAMMER_CENTRE);

        // The origin itself is broken by vanilla, the mod only handles the 8 neighbours.
        helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE);
        assertHorizontalFaceCleared(helper, HAMMER_CENTRE);

        // Ring at distance 2 must survive - otherwise the radius is too large.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) == 2) {
                    helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE.offset(dx, 0, dz));
                }
            }
        }

        // A solid 3x3x3 block of stone from here on: only that way can a flat face and an
        // upright face be told apart, because both shapes have blocks available to take.
        // The player is parked above the cube so it does not end up inside a wall.
        Vec3 clearOfTheCube = helper.absoluteVec(new Vec3(3.5, 5.0, 3.5));

        // --- Looking horizontally: the face stands upright, perpendicular to the facing. ---
        fillCube(helper, HAMMER_CENTRE.offset(-1, -1, -1), HAMMER_CENTRE.offset(1, 1, 1), Blocks.STONE);
        player.snapTo(clearOfTheCube.x, clearOfTheCube.y, clearOfTheCube.z, 0.0F, 0.0F);
        helper.assertTrue(player.getDirection() == Direction.SOUTH, "mock player is not facing south");

        swingSledgehammer(helper, player, HAMMER_CENTRE);

        helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE);
        assertUprightFaceCleared(helper, HAMMER_CENTRE);
        // The layers in front of and behind the wall have to be untouched - a horizontal
        // face would have eaten into exactly these two.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE.offset(dx, dy, -1));
                helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE.offset(dx, dy, 1));
            }
        }

        // --- Looking straight up: flat again, mirroring the first phase. ---
        fillCube(helper, HAMMER_CENTRE.offset(-1, -1, -1), HAMMER_CENTRE.offset(1, 1, 1), Blocks.STONE);
        player.snapTo(clearOfTheCube.x, clearOfTheCube.y, clearOfTheCube.z, 0.0F, -90.0F);

        swingSledgehammer(helper, player, HAMMER_CENTRE);

        helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE);
        assertHorizontalFaceCleared(helper, HAMMER_CENTRE);
        // Straight above and below the origin has to survive: that is what separates the
        // "looking up" branch from the upright face the flat-angle branch would have cut.
        helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE.above());
        helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE.below());

        helper.succeed();
    }

    /**
     * Override changes which neighbours are picked up:
     * level 0 = only the very same block, level 1 = every pickaxe block, level 2 = anything.
     * All three stages are checked against the same layout.
     *
     * <p>Levels 0 and 1 carry a second condition that the block layout alone cannot show: the
     * neighbour also has to be one the hammer is <em>allowed</em> to harvest
     * ({@code SledgehammerUtils#shouldBreak}). A diamond hammer passes that check for every
     * block in the layout, so the second half of the test repeats the same two stages with a
     * stone hammer against gold ore - a pickaxe block above its tier. Without those phases the
     * tier condition could be deleted outright and every assertion here would still hold,
     * while in game a stone hammer would sweep whole ore faces away without dropping anything.
     *
     * <p>The last phase asserts against the item instead of the world, and on purpose: from
     * override 2 on, {@code SledgehammerUtils#shouldBreak} returns {@code true} before it ever
     * asks about the tool, so the extra axe/shovel/hoe tiers that
     * {@code SledgehammerItem#isCorrectToolForDrops} hands out at that level are invisible in
     * the block layout. They are still what decides whether the broken block drops anything
     * and whether the swing costs one durability point or two, so they are measured directly.
     *
     * <p>What breaks this: dropping the "same block" condition at level 0 or the "pickaxe
     * block" condition at level 1; dropping either tier check
     * ({@code isCorrectToolForDrops}) from those two branches; and raising the override level
     * at which the hammer starts counting as an axe, shovel and hoe.
     */
    public static void sledgehammerOverrideLevelsWidenBlockSelection(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 3.5), 0.0F, 90.0F);
        BlockPos cobblePos = HAMMER_CENTRE.offset(-1, 0, 0);
        BlockPos dirtPos = HAMMER_CENTRE.offset(1, 0, 0);
        BlockPos stonePos = HAMMER_CENTRE.offset(0, 0, -1);

        // --- Override 0: only stone (same block as the origin) is taken. ---
        buildMixedField(helper, cobblePos, dirtPos);
        player.setItemInHand(InteractionHand.MAIN_HAND, sledgehammerWithOverride(helper, 0));
        swingSledgehammer(helper, player, HAMMER_CENTRE);

        helper.assertBlockPresent(Blocks.AIR, stonePos);
        helper.assertBlockPresent(Blocks.COBBLESTONE, cobblePos);
        helper.assertBlockPresent(Blocks.DIRT, dirtPos);

        // --- Override 1: every pickaxe block, but still no dirt. ---
        buildMixedField(helper, cobblePos, dirtPos);
        player.setItemInHand(InteractionHand.MAIN_HAND, sledgehammerWithOverride(helper, 1));
        swingSledgehammer(helper, player, HAMMER_CENTRE);

        helper.assertBlockPresent(Blocks.AIR, stonePos);
        helper.assertBlockPresent(Blocks.AIR, cobblePos);
        helper.assertBlockPresent(Blocks.DIRT, dirtPos);

        // --- Override 2: everything breakable goes, dirt included. ---
        buildMixedField(helper, cobblePos, dirtPos);
        player.setItemInHand(InteractionHand.MAIN_HAND, sledgehammerWithOverride(helper, 2));
        swingSledgehammer(helper, player, HAMMER_CENTRE);

        helper.assertBlockPresent(Blocks.AIR, stonePos);
        helper.assertBlockPresent(Blocks.AIR, cobblePos);
        helper.assertBlockPresent(Blocks.AIR, dirtPos);

        // =================================================================================
        // The same two gated stages again, this time with a hammer that is too weak.
        // =================================================================================

        ItemStack stoneHammer = sledgehammerWithOverride(helper, ModItems.STONE_SLEDGEHAMMER, 0);
        BlockState goldOre = Blocks.GOLD_ORE.defaultBlockState();
        BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
        // The premise of both phases, asserted instead of assumed. Gold ore has to be a
        // pickaxe block that a stone hammer may not harvest, because that is exactly the
        // gap the two phases below aim at: everything the tier check is bolted onto
        // ("same block", "pickaxe block") is true for this field, so the tier check is the
        // only thing left that can keep the field standing. If the vanilla tags ever move,
        // the phases would silently stop testing anything and had better fail here.
        helper.assertTrue(goldOre.is(BlockTags.MINEABLE_WITH_PICKAXE),
                "gold ore is no longer a pickaxe block; the tier phases would pass without "
                        + "the tier check being involved at all");
        helper.assertTrue(!stoneHammer.getItem().isCorrectToolForDrops(stoneHammer, goldOre),
                "a stone sledgehammer may suddenly harvest gold ore; the tier phases need a "
                        + "pickaxe block that is out of its reach");
        helper.assertTrue(stoneHammer.getItem().isCorrectToolForDrops(stoneHammer, cobble),
                "a stone sledgehammer may no longer harvest cobblestone; the tier phases need a "
                        + "pickaxe block that is within its reach");

        // --- Override 0 with a hammer below the tier: same block, still nothing happens. ---
        fillLayer(helper, 1, 2, 4, 2, 4, Blocks.GOLD_ORE);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                sledgehammerWithOverride(helper, ModItems.STONE_SLEDGEHAMMER, 0));
        swingSledgehammer(helper, player, HAMMER_CENTRE);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.assertBlockPresent(Blocks.GOLD_ORE, HAMMER_CENTRE.offset(dx, 0, dz));
            }
        }

        // Control for the phase above: the very same hammer does clear a field it is allowed
        // to harvest. Without this, "nothing happened" would also pass if the swing had
        // stopped working for an unrelated reason.
        fillLayer(helper, 1, 2, 4, 2, 4, Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                sledgehammerWithOverride(helper, ModItems.STONE_SLEDGEHAMMER, 0));
        swingSledgehammer(helper, player, HAMMER_CENTRE);

        assertHorizontalFaceCleared(helper, HAMMER_CENTRE);

        // --- Override 1 with a hammer below the tier: cobblestone yes, gold ore no. ---
        // The east slot that held dirt in the phases above now holds the out-of-tier block.
        BlockPos gatedPos = dirtPos;
        fillLayer(helper, 1, 2, 4, 2, 4, Blocks.STONE);
        helper.setBlock(cobblePos, Blocks.COBBLESTONE);
        helper.setBlock(gatedPos, Blocks.GOLD_ORE);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                sledgehammerWithOverride(helper, ModItems.STONE_SLEDGEHAMMER, 1));
        swingSledgehammer(helper, player, HAMMER_CENTRE);

        helper.assertBlockPresent(Blocks.AIR, stonePos);
        // Override 1 really did widen past "same block as the origin"...
        helper.assertBlockPresent(Blocks.AIR, cobblePos);
        // ...but not past what the hammer is allowed to harvest.
        helper.assertBlockPresent(Blocks.GOLD_ORE, gatedPos);

        // =================================================================================
        // Override 2 makes the hammer count as an axe, a shovel and a hoe as well.
        // =================================================================================

        ItemStack plainHammer = sledgehammerWithOverride(helper, 0);
        ItemStack overriddenHammer = sledgehammerWithOverride(helper, 2);
        BlockState shovelBlock = Blocks.DIRT.defaultBlockState();
        BlockState axeBlock = Blocks.OAK_LOG.defaultBlockState();

        // Vanilla backs the two negatives: a pickaxe tool component simply has no rule for
        // dirt or logs. The two positives are the mod's, and they are the assertions with
        // teeth here.
        helper.assertTrue(!plainHammer.getItem().isCorrectToolForDrops(plainHammer, shovelBlock),
                "an unenchanted sledgehammer already counts as a shovel");
        helper.assertTrue(!plainHammer.getItem().isCorrectToolForDrops(plainHammer, axeBlock),
                "an unenchanted sledgehammer already counts as an axe");
        helper.assertTrue(overriddenHammer.getItem().isCorrectToolForDrops(overriddenHammer, shovelBlock),
                "Override 2 does not make the sledgehammer the correct tool for shovel blocks, "
                        + "so breaking dirt with it drops nothing and costs double durability");
        helper.assertTrue(overriddenHammer.getItem().isCorrectToolForDrops(overriddenHammer, axeBlock),
                "Override 2 does not make the sledgehammer the correct tool for axe blocks, "
                        + "so breaking logs with it drops nothing and costs double durability");

        // The same tier claim, seen from the mining speed: without it the hammer is stuck at
        // the vanilla "wrong tool" speed of 1.0 on those blocks.
        float plainSpeed = ModItems.DIAMOND_SLEDGEHAMMER.getDestroySpeed(plainHammer, shovelBlock);
        float overriddenSpeed = ModItems.DIAMOND_SLEDGEHAMMER.getDestroySpeed(overriddenHammer, shovelBlock);
        helper.assertValueEqual(plainSpeed, 1.0F, "unenchanted sledgehammer speed on dirt");
        helper.assertTrue(overriddenSpeed > plainSpeed,
                "Override 2 left the sledgehammer at the bare-hands speed of " + overriddenSpeed
                        + " on dirt; digging a shovel block would take as long as with a fist");

        helper.succeed();
    }

    // =====================================================================================
    // CHISEL / SPATULA
    // =====================================================================================

    /**
     * The chisel walks a block forward through the transformation map, the spatula walks the
     * very same step backwards again.
     */
    public static void chiselAndSpatulaTransformBlockInBothDirections(GameTestHelper helper) {
        BlockPos target = new BlockPos(3, 1, 3);
        helper.setBlock(target, Blocks.STONE);
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 5.5), 0.0F, 30.0F);

        InteractionResult forward = useItemOnBlock(helper, player, new ItemStack(ModItems.STONE_CHISEL), target);
        helper.assertTrue(forward != InteractionResult.PASS, "stone chisel did not act on stone");
        helper.assertBlockPresent(Blocks.CHISELED_STONE_BRICKS, target);

        InteractionResult backward = useItemOnBlock(helper, player, new ItemStack(ModItems.STONE_SPATULA), target);
        helper.assertTrue(backward != InteractionResult.PASS, "stone spatula did not act on chiseled stone bricks");
        helper.assertBlockPresent(Blocks.STONE, target);

        helper.succeed();
    }

    /**
     * Transformations are gated by the tool material: polished deepslate belongs to the
     * diamond tier, so a stone chisel has to leave it alone while a diamond chisel converts it.
     */
    public static void chiselTierGatesTransformations(GameTestHelper helper) {
        BlockPos stoneTierTarget = new BlockPos(2, 1, 3);
        BlockPos diamondTierTarget = new BlockPos(5, 1, 3);
        helper.setBlock(stoneTierTarget, Blocks.POLISHED_DEEPSLATE);
        helper.setBlock(diamondTierTarget, Blocks.POLISHED_DEEPSLATE);
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 5.5), 0.0F, 30.0F);

        InteractionResult tooWeak = useItemOnBlock(helper, player, new ItemStack(ModItems.STONE_CHISEL), stoneTierTarget);
        helper.assertTrue(tooWeak == InteractionResult.PASS, "stone chisel accepted a diamond tier block");
        helper.assertBlockPresent(Blocks.POLISHED_DEEPSLATE, stoneTierTarget);

        useItemOnBlock(helper, player, new ItemStack(ModItems.DIAMOND_CHISEL), diamondTierTarget);
        helper.assertBlockPresent(Blocks.CHISELED_DEEPSLATE, diamondTierTarget);

        helper.succeed();
    }

    // =====================================================================================
    // VEIN MINER / STRIP MINER
    // =====================================================================================

    /**
     * Vein Miner has to follow a connected ore cluster (diagonals included), stop at foreign
     * blocks, respect the per-level block budget and refuse non-ore blocks entirely.
     *
     * <p>The budget is one number per enchantment level, so the second half walks all five
     * levels the enchantment can be rolled at against a 27 block blob - big enough that no
     * level runs out of ore and every result is capped by the budget alone. Measuring only
     * the ends of that ladder (as this test used to) leaves the three levels in between free
     * to collapse onto each other without a single assertion noticing.
     *
     * <p>Which copy this pins: the mod carries the budget table twice. Everything here goes
     * through {@code MiningUtils}, which is what the client's break preview reads
     * ({@code MultiBlockBreakingSupport}); the blocks that really get broken are picked by a
     * second, hand-copied version of the same formula in {@code VeinMinerUsageEvent}, and that
     * one is driven by {@link VeinAndStripMinerTests}. Both suites are needed - a change made
     * to only one of the two copies is a preview that stops matching what is mined.
     *
     * <p>What breaks this: a flood fill that stops at diagonals or crosses foreign blocks; a
     * budget that no longer grows with the level; the origin creeping back into the returned
     * list (it is mined by vanilla itself, so it would be broken twice); and the ore check
     * that keeps a pickaxe from vein mining plain stone.
     */
    public static void veinMinerCollectsConnectedOreCluster(GameTestHelper helper) {
        BlockPos start = new BlockPos(3, 1, 3);
        BlockPos[] veinTail = {
                new BlockPos(4, 1, 3),
                new BlockPos(4, 1, 4),
                new BlockPos(3, 1, 5),
                new BlockPos(2, 1, 2)
        };
        helper.setBlock(start, Blocks.COAL_ORE);
        for (BlockPos pos : veinTail) {
            helper.setBlock(pos, Blocks.COAL_ORE);
        }
        // Neighbour that is not part of the vein plus a detached ore that must not be reached.
        helper.setBlock(new BlockPos(2, 1, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(6, 1, 6), Blocks.COAL_ORE);

        BlockPos absStart = helper.absolutePos(start);
        BlockState oreState = helper.getBlockState(start);
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);

        // Level 5 has a budget of 18 blocks, so the whole cluster (minus the origin) is returned.
        Set<BlockPos> expected = new HashSet<>();
        for (BlockPos pos : veinTail) {
            expected.add(helper.absolutePos(pos));
        }
        Set<BlockPos> found = new HashSet<>(
                MiningUtils.getVeinMinerBlocks(helper.getLevel(), absStart, oreState, 5, pickaxe));
        helper.assertValueEqual(found, expected, "vein miner block selection");

        // Level 1 has a budget of 3 blocks (origin included) -> exactly 2 extra blocks.
        List<BlockPos> capped = MiningUtils.getVeinMinerBlocks(helper.getLevel(), absStart, oreState, 1, pickaxe);
        helper.assertValueEqual(capped.size(), 2, "vein miner block budget at level 1");
        helper.assertTrue(expected.containsAll(capped), "vein miner left the ore cluster");

        // A pickaxe must not vein mine plain stone.
        BlockPos absStone = helper.absolutePos(new BlockPos(2, 1, 3));
        List<BlockPos> stoneVein = MiningUtils.getVeinMinerBlocks(
                helper.getLevel(), absStone, helper.getBlockState(new BlockPos(2, 1, 3)), 5, pickaxe);
        helper.assertTrue(stoneVein.isEmpty(), "vein miner accepted a non-ore block");

        // --- The whole budget ladder, one call per level Vein Miner can be rolled at. ---
        // A 3x3x3 blob of 27 ore blocks, three layers above the cluster above (so the flood
        // fill cannot bridge to it) and far larger than the biggest budget. The origin counts
        // against the budget but is stripped from the result, so a budget of N shows up as
        // N-1 returned blocks.
        BlockPos blobCentre = new BlockPos(2, 5, 2);
        Set<BlockPos> blob = fillCube(helper, blobCentre.offset(-1, -1, -1), blobCentre.offset(1, 1, 1),
                Blocks.COAL_ORE);
        BlockPos absBlob = helper.absolutePos(blobCentre);
        BlockState blobState = helper.getBlockState(blobCentre);

        int[] budgetPerLevel = {3, 6, 9, 12, 18};
        for (int level = 1; level <= budgetPerLevel.length; level++) {
            List<BlockPos> mined =
                    MiningUtils.getVeinMinerBlocks(helper.getLevel(), absBlob, blobState, level, pickaxe);
            helper.assertValueEqual(mined.size(), budgetPerLevel[level - 1] - 1,
                    "vein miner block budget at level " + level);
            helper.assertTrue(!mined.contains(absBlob),
                    "the origin is back in the vein miner result at level " + level
                            + "; vanilla breaks it itself");
            helper.assertTrue(blob.containsAll(mined),
                    "the vein miner left the ore blob at level " + level);
        }

        helper.succeed();
    }

    /**
     * Strip Miner digs a tunnel along the direction the player looks. It stops at the first
     * gap and at blocks the held tool cannot harvest.
     *
     * <p>Both halves of the formula are sampled more than once, because both are lookups that
     * can be flattened without changing a single sample: the depth is the enchantment level
     * except at level 3, where it jumps to 4 - so all three levels the enchantment can be
     * rolled at are measured on the same column - and the direction comes from the pitch, with
     * a separate branch for looking up, looking down and everything in between.
     *
     * <p>Which copy this pins: as with Vein Miner, the formula exists twice. This test calls
     * {@code MiningUtils}, the copy behind the client's break preview
     * ({@code MultiBlockBreakingSupport}); the tunnel that is actually dug is computed again,
     * by hand, in {@code StripMinerUsageEvent}, and that copy belongs to
     * {@link VeinAndStripMinerTests}. A wrong answer here is a wrong highlight in game, not
     * necessarily a wrong tunnel - and the two drifting apart is its own kind of bug.
     *
     * <p>What breaks this: a depth that no longer follows the level (including the level 3
     * jump); a tunnel that keeps going through air or through blocks the tool cannot harvest;
     * and any of the three direction branches disappearing.
     */
    public static void stripMinerFollowsPlayerFacingAndStopsAtGaps(GameTestHelper helper) {
        BlockPos start = new BlockPos(3, 4, 3);
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 6.0, 3.5), 0.0F, 90.0F);
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        BlockPos absStart = helper.absolutePos(start);

        // --- Looking down: a solid column is mined down to the level 3 depth of 4. ---
        for (int y = 0; y <= 3; y++) {
            helper.setBlock(new BlockPos(3, y, 3), Blocks.STONE);
        }
        List<BlockPos> downwards = MiningUtils.getStripMinerBlocks(helper.getLevel(), absStart, player, pickaxe, 3);
        helper.assertValueEqual(downwards, List.of(
                helper.absolutePos(new BlockPos(3, 3, 3)),
                helper.absolutePos(new BlockPos(3, 2, 3)),
                helper.absolutePos(new BlockPos(3, 1, 3)),
                helper.absolutePos(new BlockPos(3, 0, 3))), "strip miner column looking down");

        // --- The two shallower levels, on the very same untouched column. ---
        List<BlockPos> levelOne = MiningUtils.getStripMinerBlocks(helper.getLevel(), absStart, player, pickaxe, 1);
        helper.assertValueEqual(levelOne, List.of(
                helper.absolutePos(new BlockPos(3, 3, 3))), "strip miner column at level 1");

        List<BlockPos> levelTwo = MiningUtils.getStripMinerBlocks(helper.getLevel(), absStart, player, pickaxe, 2);
        helper.assertValueEqual(levelTwo, List.of(
                helper.absolutePos(new BlockPos(3, 3, 3)),
                helper.absolutePos(new BlockPos(3, 2, 3))), "strip miner column at level 2");

        // --- A block the pickaxe cannot harvest ends the tunnel. ---
        helper.setBlock(new BlockPos(3, 2, 3), Blocks.DIRT);
        List<BlockPos> blocked = MiningUtils.getStripMinerBlocks(helper.getLevel(), absStart, player, pickaxe, 3);
        helper.assertValueEqual(blocked, List.of(helper.absolutePos(new BlockPos(3, 3, 3))),
                "strip miner stopped at the wrong block");

        // --- Looking horizontally: the tunnel follows the facing and stops at the gap. ---
        player.snapTo(player.getX(), player.getY(), player.getZ(), 0.0F, 0.0F);
        helper.assertTrue(player.getDirection() == Direction.SOUTH, "mock player is not facing south");
        helper.setBlock(new BlockPos(3, 4, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 4, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 4, 6), Blocks.AIR);
        List<BlockPos> forward = MiningUtils.getStripMinerBlocks(helper.getLevel(), absStart, player, pickaxe, 3);
        helper.assertValueEqual(forward, List.of(
                helper.absolutePos(new BlockPos(3, 4, 4)),
                helper.absolutePos(new BlockPos(3, 4, 5))), "strip miner tunnel looking south");

        // --- Looking up: the third direction branch. The facing stays south, so a tunnel
        //     that followed it would run into the empty room instead of into the column. ---
        BlockPos upStart = new BlockPos(5, 1, 5);
        for (int y = 2; y <= 4; y++) {
            helper.setBlock(new BlockPos(5, y, 5), Blocks.STONE);
        }
        player.snapTo(player.getX(), player.getY(), player.getZ(), 0.0F, -90.0F);
        // Pinned so the phase cannot pass by accident: along the facing there is nothing to
        // mine, so a direction lookup that ignored the pitch would come back empty here.
        helper.assertBlockPresent(Blocks.AIR, upStart.relative(player.getDirection()));
        List<BlockPos> upwards = MiningUtils.getStripMinerBlocks(
                helper.getLevel(), helper.absolutePos(upStart), player, pickaxe, 2);
        // Three stone blocks are available, the level 2 depth takes two of them.
        helper.assertValueEqual(upwards, List.of(
                helper.absolutePos(new BlockPos(5, 2, 5)),
                helper.absolutePos(new BlockPos(5, 3, 5))), "strip miner column looking up");

        helper.succeed();
    }

    // =====================================================================================
    // MAGNET
    // =====================================================================================

    /**
     * A magnet in the main hand drags loose items to the player until they are picked up,
     * while items outside its range stay where they dropped.
     *
     * <p>The magnet runs in {@code Item#inventoryTick}, which vanilla only reaches through
     * {@code ServerGamePacketListenerImpl#tick()} -> {@code ServerPlayer#doTick()} ->
     * {@code Player#aiStep()} -> {@code Inventory#tick()} - the same call chain that also
     * performs the item pickup. A gametest mock player is added to the level (so
     * {@code ServerPlayer#tick()} runs), but its connection is never registered with the
     * {@code ServerConnectionListener}, so nobody pumps that listener and the player half of
     * the tick never happens. The test therefore ticks the connection itself, exactly like a
     * real server would; without it neither the inventory nor the pickup would ever run.
     */
    public static void magnetPullsNearbyItemsAndIgnoresDistantOnes(GameTestHelper helper) {
        fillLayer(helper, 0, 0, 7, 0, 7, Blocks.STONE);

        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5), 0.0F, 0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MAGNET));

        // 3 blocks away: inside the magnet's base range, far outside the vanilla pickup radius.
        ItemEntity nearby = helper.spawnItem(Items.DIAMOND, new Vec3(4.5, 1.5, 1.5));
        // 6 blocks away on both horizontal axes: outside the magnet's range.
        ItemEntity outOfRange = helper.spawnItem(Items.GOLD_INGOT, new Vec3(7.5, 1.5, 7.5));
        double parkedX = outOfRange.getX();
        double parkedZ = outOfRange.getZ();

        helper.succeedWhen(() -> {
            // See the javadoc: the mock player's connection is never pumped by the gametest
            // server, so the vanilla player tick has to be driven from here.
            player.connection.tick();

            helper.assertTrue(outOfRange.isAlive(), "the out of range item vanished");
            double drift = Math.max(Math.abs(outOfRange.getX() - parkedX), Math.abs(outOfRange.getZ() - parkedZ));
            helper.assertTrue(drift < 0.5, "the magnet moved an item that is out of range");
            helper.assertTrue(player.getInventory().contains(stack -> stack.is(Items.DIAMOND)),
                    "the magnet did not pull the nearby item into the player; the item is still at "
                            + nearby.position() + " with motion " + nearby.getDeltaMovement());
        });
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /**
     * Creates a fully connected mock server player, moves it into the test room and makes
     * sure it leaves the server again once the test is over.
     */
    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relativePos, float yRot, float xRot) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relativePos);
        player.snapTo(pos.x, pos.y, pos.z, yRot, xRot);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    private static void fillLayer(GameTestHelper helper, int y, int minX, int maxX, int minZ, int maxZ, Block block) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                helper.setBlock(new BlockPos(x, y, z), block);
            }
        }
    }

    /**
     * Fills the closed box between the two corners and hands back the <em>absolute</em>
     * positions it wrote, which is the form the mining utilities answer in.
     */
    private static Set<BlockPos> fillCube(GameTestHelper helper, BlockPos min, BlockPos max, Block block) {
        Set<BlockPos> filled = new HashSet<>();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    helper.setBlock(pos, block);
                    filled.add(helper.absolutePos(pos));
                }
            }
        }
        return filled;
    }

    /** The 8 neighbours in the horizontal plane through the origin are gone, the origin is not. */
    private static void assertHorizontalFaceCleared(GameTestHelper helper, BlockPos centre) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                helper.assertBlockPresent(Blocks.AIR, centre.offset(dx, 0, dz));
            }
        }
    }

    /** The same for the upright plane a player looking along the z axis faces. */
    private static void assertUprightFaceCleared(GameTestHelper helper, BlockPos centre) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                helper.assertBlockPresent(Blocks.AIR, centre.offset(dx, dy, 0));
            }
        }
    }

    /** Stone 3x3 around the origin with one cobblestone and one dirt block mixed in. */
    private static void buildMixedField(GameTestHelper helper, BlockPos cobblePos, BlockPos dirtPos) {
        fillLayer(helper, 1, 2, 4, 2, 4, Blocks.STONE);
        helper.setBlock(cobblePos, Blocks.COBBLESTONE);
        helper.setBlock(dirtPos, Blocks.DIRT);
    }

    private static ItemStack sledgehammerWithOverride(GameTestHelper helper, int overrideLevel) {
        return sledgehammerWithOverride(helper, ModItems.DIAMOND_SLEDGEHAMMER, overrideLevel);
    }

    private static ItemStack sledgehammerWithOverride(GameTestHelper helper, Item hammer, int overrideLevel) {
        ItemStack stack = new ItemStack(hammer);
        if (overrideLevel > 0) {
            stack.enchant(enchantment(helper, ModEnchantments.OVERRIDE), overrideLevel);
        }
        return stack;
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    /**
     * Runs the mod's block break hook for the given origin. Vanilla would break the origin
     * itself; the hook is only responsible for the surrounding blocks, so the origin is left
     * in place on purpose - that makes it visible if the hook ever destroys it twice.
     */
    private static void swingSledgehammer(GameTestHelper helper, ServerPlayer player, BlockPos relativeOrigin) {
        BlockPos origin = helper.absolutePos(relativeOrigin);
        SledgehammerUsageEvent.handleBeforeBlockBreak(
                helper.getLevel(), player, origin, helper.getLevel().getBlockState(origin), null);
    }

    /** Right clicks the top face of the given block with the stack, server side. */
    private static InteractionResult useItemOnBlock(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos relativePos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = helper.absolutePos(relativePos);
        Vec3 hit = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hit, Direction.UP, pos, false);
        return stack.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(
                player, InteractionHand.MAIN_HAND, hitResult));
    }
}
