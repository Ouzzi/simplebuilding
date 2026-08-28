package com.simplebuilding.gametest;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.MiningUtils;
import com.simplebuilding.util.SledgehammerUsageEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Server side behaviour of the mod's tools: sledgehammer area mining (including the
 * Override enchantment tiers), chisel/spatula block transformation, the Vein Miner and
 * Strip Miner block selection and the magnet's item attraction.
 *
 * <p>Every test builds a real situation inside the 8x8x8 gametest room and then inspects
 * the world afterwards. The tests pin {@code rotation = Rotation.NONE} because several of
 * them depend on absolute directions (player facing, mining direction).
 */
public final class ToolBehaviourGameTest {

    /** Centre of the horizontal block field used by the sledgehammer tests. */
    private static final BlockPos HAMMER_CENTRE = new BlockPos(3, 1, 3);

    // =====================================================================================
    // SLEDGEHAMMER
    // =====================================================================================

    /**
     * A sledgehammer swing has to take the whole 3x3 face around the mined block with it -
     * no more, no less. The player looks straight down, so the affected face is horizontal.
     */
    @GameTest(rotation = Rotation.NONE)
    public void sledgehammerBreaksThreeByThreeAroundOrigin(GameTestHelper helper) {
        fillLayer(helper, 1, 1, 5, 1, 5, Blocks.STONE);

        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 3.5), 0.0F, 90.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER));

        swingSledgehammer(helper, player, HAMMER_CENTRE);

        // The origin itself is broken by vanilla, the mod only handles the 8 neighbours.
        helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                helper.assertBlockPresent(Blocks.AIR, HAMMER_CENTRE.offset(dx, 0, dz));
            }
        }

        // Ring at distance 2 must survive - otherwise the radius is too large.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) == 2) {
                    helper.assertBlockPresent(Blocks.STONE, HAMMER_CENTRE.offset(dx, 0, dz));
                }
            }
        }

        helper.succeed();
    }

    /**
     * Override changes which neighbours are picked up:
     * level 0 = only the very same block, level 1 = every pickaxe block, level 2 = anything.
     * All three stages are checked against the same layout.
     */
    @GameTest(rotation = Rotation.NONE)
    public void sledgehammerOverrideLevelsWidenBlockSelection(GameTestHelper helper) {
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

        helper.succeed();
    }

    // =====================================================================================
    // CHISEL / SPATULA
    // =====================================================================================

    /**
     * The chisel walks a block forward through the transformation map, the spatula walks the
     * very same step backwards again.
     */
    @GameTest(rotation = Rotation.NONE)
    public void chiselAndSpatulaTransformBlockInBothDirections(GameTestHelper helper) {
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
    @GameTest(rotation = Rotation.NONE)
    public void chiselTierGatesTransformations(GameTestHelper helper) {
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
     */
    @GameTest(rotation = Rotation.NONE)
    public void veinMinerCollectsConnectedOreCluster(GameTestHelper helper) {
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

        helper.succeed();
    }

    /**
     * Strip Miner digs a tunnel along the direction the player looks. It stops at the first
     * gap and at blocks the held tool cannot harvest.
     */
    @GameTest(rotation = Rotation.NONE)
    public void stripMinerFollowsPlayerFacingAndStopsAtGaps(GameTestHelper helper) {
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

        helper.succeed();
    }

    // =====================================================================================
    // MAGNET
    // =====================================================================================

    /**
     * A magnet in the main hand drags loose items to the player until they are picked up,
     * while items outside its range stay where they dropped.
     */
    @GameTest(rotation = Rotation.NONE, maxTicks = 200)
    public void magnetPullsNearbyItemsAndIgnoresDistantOnes(GameTestHelper helper) {
        fillLayer(helper, 0, 0, 7, 0, 7, Blocks.STONE);

        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5), 0.0F, 0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MAGNET));

        // 3 blocks away: inside the magnet's base range, far outside the vanilla pickup radius.
        helper.spawnItem(Items.DIAMOND, new Vec3(4.5, 1.5, 1.5));
        // 6 blocks away on both horizontal axes: outside the magnet's range.
        ItemEntity outOfRange = helper.spawnItem(Items.GOLD_INGOT, new Vec3(7.5, 1.5, 7.5));
        double parkedX = outOfRange.getX();
        double parkedZ = outOfRange.getZ();

        helper.succeedWhen(() -> {
            helper.assertTrue(outOfRange.isAlive(), "the out of range item vanished");
            double drift = Math.max(Math.abs(outOfRange.getX() - parkedX), Math.abs(outOfRange.getZ() - parkedZ));
            helper.assertTrue(drift < 0.5, "the magnet moved an item that is out of range");
            helper.assertTrue(player.getInventory().contains(stack -> stack.is(Items.DIAMOND)),
                    "the magnet did not pull the nearby item into the player");
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

    /** Stone 3x3 around the origin with one cobblestone and one dirt block mixed in. */
    private static void buildMixedField(GameTestHelper helper, BlockPos cobblePos, BlockPos dirtPos) {
        fillLayer(helper, 1, 2, 4, 2, 4, Blocks.STONE);
        helper.setBlock(cobblePos, Blocks.COBBLESTONE);
        helper.setBlock(dirtPos, Blocks.DIRT);
    }

    private static ItemStack sledgehammerWithOverride(GameTestHelper helper, int overrideLevel) {
        ItemStack stack = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);
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
