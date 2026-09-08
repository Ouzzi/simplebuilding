package com.simplebuilding.gametest;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.util.SledgehammerEntityInteraction;
import com.simplebuilding.util.SledgehammerUsageEvent;
import java.util.ArrayList;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The sledgehammer, end to end: which blocks a swing takes, what it charges for them, what the
 * charged right click reshapes, how fast it mines and charges, and the item frame trick that turns
 * a framed smithing template into a glowing one.
 *
 * <p>What was already covered elsewhere is deliberately <em>not</em> repeated here:
 * {@link ToolBehaviourTests} pins the 3x3 face for a flat and an upright look and the three
 * Override tiers including the material tier gate, {@link EnchantmentEffectTests} pins Radius,
 * Break Through, the sneak suppression and the depth on three of the six hit sides, and
 * {@link ConsumptionAndDurabilityTests#sledgehammerSecondaryUseWearsDownOnlyTheSurvivalPlayer}
 * pins the wear of the charged right click plus the stairs-back-to-block direction and the
 * 81 diamond pebbles. This class starts where those stop; where it comes close to one of them,
 * the test's own javadoc says which part is new and which part lives over there.
 *
 * <h2>Registration</h2>
 *
 * <p>Every test in here has to be registered <b>unrotated</b> ({@code Rotation.NONE}). They all
 * state absolute directions - the player's yaw and pitch decide which plane a swing takes, and
 * several assertions name concrete world axes. Under a rotated structure the expectations and the
 * player's facing would drift apart and the results would depend on the placement.
 *
 * <h2>How a swing is driven</h2>
 *
 * <p>The area mining lives in {@code SledgehammerUsageEvent#handleBeforeBlockBreak}, which each
 * loader hangs on its own "block is about to break" event. The tests call that hook directly, the
 * way {@link ToolBehaviourTests} does. Vanilla breaks the block that was actually hit; the hook is
 * only responsible for the neighbours, so the origin is expected to still stand afterwards.
 *
 * <h2>Reaching the payments</h2>
 *
 * <p>Everything that costs durability is gated by {@code Abilities#instabuild} down in
 * {@code ItemStack#processDurabilityChange}, so the wear tests use the in-level mock player with
 * {@code instabuild} flipped off - the same trick {@link ConsumptionAndDurabilityTests} explains at
 * length. That has a consequence worth knowing: with {@code instabuild} off, vanilla's own
 * {@code ServerPlayerGameMode#destroyBlock} runs {@code ItemStack#mineBlock} for every block it
 * breaks, and that charges the tool as well. The mod's own charge is therefore never read as an
 * absolute number - the vanilla share is <em>measured</em> first on a probe block and subtracted.
 *
 * <h2>Known defect</h2>
 *
 * <p>{@code ModItemTagProvider} builds {@code simplebuilding:sledgehammer_tools} from the stone,
 * copper, iron, gold, diamond and netherite hammers but leaves the <b>Enderite Sledgehammer</b>
 * out, so the generated tag (see {@code data/simplebuilding/tags/item/sledgehammer_tools.json})
 * has six entries. That tag is the {@code supported_items} and {@code primary_items} of Override,
 * Radius and Break Through and part of the Constructor's Touch, Range and Versatility item sets,
 * which makes the mod's best hammer the only one that cannot carry any of its own enchantments.
 * The tests below therefore use the <em>diamond</em> hammer wherever an enchantment is involved
 * and never assert the tag contents, so that fixing the tag does not turn a test red.
 *
 * <h2>Not covered</h2>
 * <ul>
 *   <li><b>The re-entrancy guard {@code HARVESTED_BLOCKS}.</b> It only fires when the loader's
 *       break event runs <em>inside</em> {@code gameMode.destroyBlock} and calls the hook again.
 *       The shared suite invokes the hook directly, so a nested call cannot be produced from here
 *       and every assertion about the guard would pass without executing it. What can be checked -
 *       and is, in {@link #sledgehammerBillsOneDurabilityPerBlockAndTwoForTheWrongTool} - is the
 *       observable consequence: a swing pays for each neighbour exactly once.</li>
 *   <li><b>The second diamond block check inside {@code crushDiamondBlock}.</b> The block is read
 *       once by {@code finishUsingItem} and re-read by the crusher inside the same call; nothing
 *       from outside can change the world between the two, so the guard is unreachable in a
 *       test.</li>
 *   <li><b>The anvil mixin</b> (hammer plus hammer, and the repair rate of one eleventh per
 *       material). The result stack is reachable, but the level cost it writes lives in a private
 *       {@code DataSlot} with no getter in 26.2; reading it needs a container synchroniser harness
 *       this suite does not have, and half the claim is about that number.</li>
 *   <li><b>Loot pools, the traded hammer's random enchantment, the crafting recipes and the
 *       smithing upgrade.</b> Those are assertions about datagen output and about
 *       {@code ConfigOptionTests}' pool recorder / {@code TradeAndMigrationTests}' offer checks,
 *       and belong in those files rather than in a behaviour suite for the item.</li>
 *   <li><b>The declared durability numbers.</b> Asserting them means copying
 *       {@code SledgehammerItem}'s own constants back out of it, which can only fail if somebody
 *       edits the test as well.</li>
 *   <li>Everything client side: the outline renderer, the swing sounds and the block particles.</li>
 * </ul>
 */
public final class SledgehammerTests {

    private SledgehammerTests() {
    }

    /** Centre of the horizontal 3x3 field the swing tests mine. */
    private static final BlockPos CENTRE = new BlockPos(3, 1, 3);

    /** Far corner of the room, used as the probe block for the vanilla wear baseline. */
    private static final BlockPos PROBE = new BlockPos(6, 1, 6);

    /** Straight above {@link #CENTRE}, high enough for {@code player.pick} to reach it. */
    private static final Vec3 ABOVE_CENTRE = new Vec3(3.5, 3.0, 3.5);

    /** Neighbours of the origin inside a plain 3x3 face. */
    private static final int NEIGHBOURS = 8;

    // =====================================================================================
    // WHICH BLOCKS A SWING TAKES
    // =====================================================================================

    /**
     * Air and blocks that cannot be mined at all are skipped rather than swallowed: a bedrock
     * block inside the face stays where it is, the hole in the face stays a hole, and neither of
     * them costs the hammer anything.
     *
     * <p>The hammer carries Override II, which is what makes the assertion sharp: at that tier
     * {@code SledgehammerUtils#shouldBreak} says yes to every block type, so the only thing left
     * that can save the bedrock is the hardness check in front of it. With a plain hammer the
     * bedrock would also survive - for the entirely different reason that it is not the same
     * block as the origin - and the test would prove nothing.
     *
     * <p>The wear half needs the detour through a measured baseline: with {@code instabuild} off,
     * vanilla charges the tool once per block it breaks on top of the mod's own charge. Only the
     * remainder is the mod's, and it has to come out at six blocks - not eight.
     *
     * <p>What breaks this: dropping the {@code getDestroySpeed() < 0} guard in
     * {@code SledgehammerUtils#shouldBreak}, which hands every Override II hammer a bedrock
     * breaker; and dropping the {@code isAir()} guard, which would still not remove a block but
     * would bill the player for the empty slot, because {@code ServerPlayerGameMode#destroyBlock}
     * reports success even when it removed nothing.
     */
    public static void sledgehammerFieldSkipsAirGapsAndUnbreakableBlocks(GameTestHelper helper) {
        ServerPlayer player = inLevelPlayer(helper, ABOVE_CENTRE, 0.0F, 90.0F, false);
        ItemStack hammer = hammerWith(helper, ModEnchantments.OVERRIDE, 2);

        int vanillaPerBlock = measureVanillaWearPerBlock(helper, player, hammer, Blocks.STONE);

        BlockPos bedrock = CENTRE.offset(-1, 0, -1);
        BlockPos gap = CENTRE.offset(1, 0, 1);
        fillFace(helper, CENTRE, Blocks.STONE);
        helper.setBlock(bedrock, Blocks.BEDROCK);
        helper.setBlock(gap, Blocks.AIR);

        hammer.setDamageValue(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        swing(helper, player, CENTRE);

        helper.assertBlockPresent(Blocks.BEDROCK, bedrock);
        helper.assertBlockPresent(Blocks.AIR, gap);
        helper.assertBlockPresent(Blocks.STONE, CENTRE);
        int cleared = 0;
        for (BlockPos pos : faceNeighbours(CENTRE)) {
            if (pos.equals(bedrock) || pos.equals(gap)) {
                continue;
            }
            helper.assertBlockPresent(Blocks.AIR, pos);
            cleared++;
        }
        Assertions.valueEqual(helper, cleared, NEIGHBOURS - 2, "stone neighbours the swing was meant to clear");

        int modShare = hammer.getDamageValue() - cleared * vanillaPerBlock;
        Assertions.valueEqual(helper, modShare, cleared,
                "the swing charged " + modShare + " points for " + cleared + " broken blocks; the "
                        + "bedrock or the hole in the face was billed as if it had been mined "
                        + "(vanilla's own share of " + vanillaPerBlock + " per block is already subtracted)");

        helper.killAllEntitiesOfClass(ItemEntity.class);
        TestCleanup.succeed(helper);
    }

    /**
     * The origin decides for the whole face: if the block that was actually hit is not one this
     * hammer may take, not a single neighbour moves - even when every neighbour on its own would
     * qualify.
     *
     * <p>The layout is chosen so that the origin is the only thing standing in the way: a dirt
     * origin ringed by stone, swung with an Override I hammer. Override I accepts any pickaxe
     * block, so all eight stone neighbours pass {@code shouldBreak} on their own; only
     * {@code canMineOrigin} rejects the dirt in the middle. Swapping just the origin to stone -
     * one block, nothing else - has to bring the whole face down again, which is what proves the
     * first half was not simply inert.
     *
     * <p>The three tails - an origin nobody can mine, an origin that is not there, and a main hand
     * that is not a sledgehammer - are stated at the level of the method: whatever the reason,
     * {@code getBlocksToBeDestroyed} has to hand back an empty list. They are deliberately
     * <em>not</em> sold as coverage of the two early returns at the top of it, because they cannot
     * be: {@code SledgehammerUtils#shouldBreak}, which {@code canMineOrigin} calls with the origin
     * as both arguments, repeats the same two conditions word for word, so deleting either early
     * return leaves all three tails green. Only losing the guard in {@code shouldBreak} <em>as
     * well</em> turns them red. The same goes for the item check twice over - the loader hook in
     * {@code SledgehammerUsageEvent} tests it a third time before it ever calls in here.
     *
     * <p>What breaks this: losing the {@code canMineOrigin} call, which turns every hammer into an
     * area miner for blocks it may not even harvest - hit one dirt block with an Override I hammer
     * and the stone wall behind it goes. That is the load bearing half, and it is carried by the
     * dirt origin with its stone ring and by the positive control right behind it.
     */
    public static void sledgehammerRefusesTheWholeFieldWhenTheOriginIsOutOfReach(GameTestHelper helper) {
        ServerPlayer player = inLevelPlayer(helper, ABOVE_CENTRE, 0.0F, 90.0F, true);
        ItemStack hammer = hammerWith(helper, ModEnchantments.OVERRIDE, 1);
        BlockPos origin = helper.absolutePos(CENTRE);

        // --- a dirt origin blocks the swing although every neighbour would qualify ---
        fillFace(helper, CENTRE, Blocks.STONE);
        helper.setBlock(CENTRE, Blocks.DIRT);
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);

        helper.assertTrue(SledgehammerItem.getBlocksToBeDestroyed(1, origin, player).isEmpty(),
                "a dirt origin still produced a list of blocks to destroy");
        swing(helper, player, CENTRE);
        for (BlockPos pos : faceNeighbours(CENTRE)) {
            helper.assertBlockPresent(Blocks.STONE, pos);
        }

        // --- the very same face with a stone origin comes down, so the setup was live ---
        helper.setBlock(CENTRE, Blocks.STONE);
        Assertions.valueEqual(helper, SledgehammerItem.getBlocksToBeDestroyed(1, origin, player).size(), 9,
                "positions a plain swing on a workable origin returns");
        swing(helper, player, CENTRE);
        for (BlockPos pos : faceNeighbours(CENTRE)) {
            helper.assertBlockPresent(Blocks.AIR, pos);
        }

        // --- the method level contract: nothing comes back for these three origins either. Each of
        // them is refused at least twice over (see the javadoc), so none of the three can single
        // out one guard; what they pin is the empty list the callers rely on. ---
        ItemStack anything = hammerWith(helper, ModEnchantments.OVERRIDE, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, anything);
        fillFace(helper, CENTRE, Blocks.STONE);
        helper.setBlock(CENTRE, Blocks.BEDROCK);
        helper.assertTrue(SledgehammerItem.getBlocksToBeDestroyed(1, origin, player).isEmpty(),
                "a bedrock origin produced a list of blocks to destroy");

        helper.setBlock(CENTRE, Blocks.AIR);
        helper.assertTrue(SledgehammerItem.getBlocksToBeDestroyed(1, origin, player).isEmpty(),
                "an air origin produced a list of blocks to destroy");

        // --- and no other tool is handed a field ---
        helper.setBlock(CENTRE, Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        helper.assertTrue(SledgehammerItem.getBlocksToBeDestroyed(1, origin, player).isEmpty(),
                "a plain diamond pickaxe was handed a sledgehammer field");
        swing(helper, player, CENTRE);
        for (BlockPos pos : faceNeighbours(CENTRE)) {
            helper.assertBlockPresent(Blocks.STONE, pos);
        }

        helper.killAllEntitiesOfClass(ItemEntity.class);
        TestCleanup.succeed(helper);
    }

    /**
     * Every extra block a swing takes costs one point of durability, and two when the hammer is
     * the wrong tool for it - that is the whole price of the Override enchantment: it widens what
     * the hammer may break, and blocks outside its own tool class cost double.
     *
     * <p>Three faces, one hammer:
     * <ul>
     *   <li><b>stone</b>, plain hammer - the pickaxe case, one point each;</li>
     *   <li><b>glass</b>, Override II - glass is in none of the four vanilla {@code mineable/*}
     *       tags, so Override II lets the hammer break it while
     *       {@code SledgehammerItem#isCorrectToolForDrops} still says no: two points each;</li>
     *   <li><b>dirt</b>, Override II - dirt <em>is</em> in {@code mineable/shovel}, which
     *       Override II adds to the hammer's tool classes, so it drops back to one point each.</li>
     * </ul>
     * The last two together are the point: Override II is not a blanket "everything is cheap now",
     * it moves exactly the axe, shovel and hoe blocks into the cheap class.
     *
     * <p>Every number is a <em>remainder</em>. Vanilla's {@code ItemStack#mineBlock} charges the
     * tool once per block on top of whatever the mod does, so the vanilla share is measured on a
     * probe block of the same type first and subtracted. Reading the raw damage value instead
     * would bake vanilla's tool wear into the mod's expectations.
     *
     * <p>What breaks this: deleting the {@code hurtAndBreak} in the hook (an area miner that never
     * wears out); making the cost flat, which removes the entire drawback of mining foreign blocks
     * with Override II; and losing the axe/shovel/hoe branch of {@code isCorrectToolForDrops},
     * which would silently double the price of every dirt block an Override II hammer takes. The
     * per-block arithmetic also fails if a neighbour is ever processed twice.
     */
    public static void sledgehammerBillsOneDurabilityPerBlockAndTwoForTheWrongTool(GameTestHelper helper) {
        ServerPlayer player = inLevelPlayer(helper, ABOVE_CENTRE, 0.0F, 90.0F, false);

        assertSwingCharge(helper, player, new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER), Blocks.STONE, 1,
                "a plain hammer on its own pickaxe blocks");
        assertSwingCharge(helper, player, hammerWith(helper, ModEnchantments.OVERRIDE, 2), Blocks.GLASS, 2,
                "an Override II hammer on glass, which is in none of the mineable tags");
        assertSwingCharge(helper, player, hammerWith(helper, ModEnchantments.OVERRIDE, 2), Blocks.DIRT, 1,
                "an Override II hammer on dirt, which Override II makes a correct tool for");

        helper.killAllEntitiesOfClass(ItemEntity.class);
        TestCleanup.succeed(helper);
    }

    /**
     * A hammer that breaks in the middle of a face stops there. The rest of the field stays
     * standing and the player is left with an empty hand rather than an invisible tool that keeps
     * mining.
     *
     * <p>The hammer starts one point short of its maximum, so the very first neighbour finishes
     * it off. Exactly one neighbour may be gone afterwards; eight would mean the swing carried on
     * with nothing in hand.
     *
     * <p>Be honest about what this pins: two independent things stop the loop once the stack is
     * empty - the explicit {@code break} and, one step earlier, {@code shouldBreak}, which refuses
     * everything because an empty {@code ItemStack} reports {@code Items.AIR} as its item. Removing
     * only the {@code break} would not show up here. What the test does pin is the outcome the
     * player sees, and it goes red the moment a broken hammer keeps clearing blocks - the shape
     * such a regression would take if the emptiness check moved or the loop started working on a
     * copy of the stack instead of the one in the hand.
     *
     * <p>What breaks this: mining the field from a snapshot of the hammer instead of the live main
     * hand stack, or moving the wear behind the loop so the whole face is free once the last point
     * is spent.
     */
    public static void sledgehammerStopsTheSwingWhenTheHammerBreaks(GameTestHelper helper) {
        ServerPlayer player = inLevelPlayer(helper, ABOVE_CENTRE, 0.0F, 90.0F, false);

        ItemStack hammer = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);
        helper.assertTrue(hammer.isDamageableItem(), "the sledgehammer stopped being damageable");
        hammer.setDamageValue(hammer.getMaxDamage() - 1);

        fillFace(helper, CENTRE, Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        swing(helper, player, CENTRE);

        ItemStack inHand = player.getMainHandItem();
        helper.assertTrue(inHand.isEmpty(),
                "the hammer survived a swing that had to break it; the hand still holds " + inHand);

        int cleared = 0;
        for (BlockPos pos : faceNeighbours(CENTRE)) {
            if (helper.getBlockState(pos).isAir()) {
                cleared++;
            }
        }
        Assertions.valueEqual(helper, cleared, 1,
                "neighbours a hammer with one point of durability left managed to break");
        helper.assertBlockPresent(Blocks.STONE, CENTRE);

        helper.killAllEntitiesOfClass(ItemEntity.class);
        TestCleanup.succeed(helper);
    }

    /**
     * The face a swing takes is the one the player is looking at, and the extra layers Break
     * Through adds go <em>away</em> from the player, behind that face.
     *
     * <p>All six hit sides are checked against a plane the test builds itself out of two axes and
     * a depth direction, rather than out of the offsets the mod computes: looking down or up gives
     * a horizontal face, looking along any of the four compass directions gives a vertical one,
     * and the depth always runs in the direction of view. That independence is the point - a sign
     * error in the mod's depth offset flips the extra layer in front of the face, towards the
     * player, and would still look plausible in an offset-for-offset comparison.
     *
     * <p>The two switch-over points get a pair each, at 59 and 61 degrees. Straight looks alone
     * cannot see them: {@code getHitSideFromPlayer} answers the same for a pitch of 0, 90 and -90
     * no matter where in {@code (0, 90)} its thresholds sit, so a suite that only ever looks
     * straight ahead or straight down - which is what every other sledgehammer test does - reports
     * the threshold as covered while never executing it. With the pair, {@code 60} is a number this
     * test measured rather than one it took on faith.
     *
     * <p>What is <em>not</em> here, because it is pinned end to end elsewhere:
     * {@link ToolBehaviourTests#sledgehammerBreaksThreeByThreeAroundOrigin} mines a real wall and a
     * real floor and checks that the layers in front of and behind them survive, and
     * {@link EnchantmentEffectTests#breakThroughAddsLayersBehindTheMinedFace} does the same for the
     * depth on three of the six sides. This test is the complete matrix behind those: all six hit
     * sides in one place, as exact position sets rather than as spot checks, and with the returned
     * list's length checked so that a position handed back twice cannot hide inside a set.
     *
     * <p>What breaks this: the {@code getDirection().getOpposite()} in
     * {@code getHitSideFromPlayer} losing its {@code getOpposite}, which turns the depth of all four
     * wall faces around so the extra layer is dug out towards the player instead of behind the
     * face; the pitch thresholds drifting - narrow them to {@code +/-5} and the 59 degree look
     * takes a horizontal slab out of the floor instead of the wall in front of the player, widen
     * them to {@code +/-89} and the 61 degree look down at the floor takes a vertical slice; and
     * any of the four depth signs flipping, which digs Break Through layers out of the block the
     * player is standing on.
     */
    public static void sledgehammerFieldPlaneAndDepthFollowTheLookDirection(GameTestHelper helper) {
        ServerPlayer player = inLevelPlayer(helper, new Vec3(3.5, 4.0, 3.5), 0.0F, 0.0F, true);
        BlockPos relativeOrigin = new BlockPos(3, 3, 3);
        BlockPos origin = helper.absolutePos(relativeOrigin);
        helper.setBlock(relativeOrigin, Blocks.STONE);

        // A plain hammer first: one flat 3x3, no depth at all.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER));
        look(player, 0.0F, 90.0F);
        assertField(helper, player, origin, 0, Direction.EAST, Direction.SOUTH, Direction.DOWN, "looking down");

        // With Break Through I every face grows one layer behind it.
        player.setItemInHand(InteractionHand.MAIN_HAND, hammerWith(helper, ModEnchantments.BREAK_THROUGH, 1));

        look(player, 0.0F, 90.0F);
        assertField(helper, player, origin, 1, Direction.EAST, Direction.SOUTH, Direction.DOWN, "looking down");
        look(player, 0.0F, -90.0F);
        assertField(helper, player, origin, 1, Direction.EAST, Direction.SOUTH, Direction.UP, "looking up");
        look(player, 0.0F, 0.0F);
        helper.assertTrue(player.getDirection() == Direction.SOUTH, "yaw 0 no longer faces south");
        assertField(helper, player, origin, 1, Direction.EAST, Direction.UP, Direction.SOUTH, "facing south");
        look(player, 180.0F, 0.0F);
        assertField(helper, player, origin, 1, Direction.EAST, Direction.UP, Direction.NORTH, "facing north");
        look(player, 90.0F, 0.0F);
        assertField(helper, player, origin, 1, Direction.SOUTH, Direction.UP, Direction.WEST, "facing west");
        look(player, 270.0F, 0.0F);
        assertField(helper, player, origin, 1, Direction.SOUTH, Direction.UP, Direction.EAST, "facing east");

        // The two switch-over points, one degree either side of them. Only these four cases make the
        // 60 in getHitSideFromPlayer a measured number: every threshold strictly inside (0, 90)
        // answers the same for the straight 0/90/-90 looks above, so those cannot see it move.
        look(player, 0.0F, 59.0F);
        assertField(helper, player, origin, 1, Direction.EAST, Direction.UP, Direction.SOUTH,
                "tilted 59 degrees down, still the wall in front of the player");
        look(player, 0.0F, 61.0F);
        assertField(helper, player, origin, 1, Direction.EAST, Direction.SOUTH, Direction.DOWN,
                "tilted 61 degrees down, now the floor");
        look(player, 0.0F, -59.0F);
        assertField(helper, player, origin, 1, Direction.EAST, Direction.UP, Direction.SOUTH,
                "tilted 59 degrees up, still the wall in front of the player");
        look(player, 0.0F, -61.0F);
        assertField(helper, player, origin, 1, Direction.EAST, Direction.SOUTH, Direction.UP,
                "tilted 61 degrees up, now the ceiling");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // THE CHARGED RIGHT CLICK
    // =====================================================================================

    /**
     * Holding right click only starts the charge-up when there is something to do at the other
     * end of it. A block the hammer can reshape - and a diamond block, which it crushes - answer
     * {@code CONSUME} and put the player into the use animation; anything else answers
     * {@code PASS} and leaves the hand free.
     *
     * <p>{@code PASS} rather than {@code CONSUME} is what keeps the hammer from swallowing right
     * clicks: a hammer that consumed every click would block placing blocks from the off hand and
     * would run a ten-tick animation with no result. Two refusals are checked: a full block with no
     * stairs variant, and a sneaking player without Constructor's Touch. The second one is an A/B on
     * one and the same block - a stone slab is refused to a plain hammer and accepted by the very
     * next click with the enchantment on it, which is the only arrangement in which the guard
     * carries the result. Offer the plain sneaking hammer a stone block instead and it is refused
     * because the reverse branch has no answer for a full block either, guard or no guard.
     *
     * <p>The tail is the early release: letting go before the animation ends has to do nothing at
     * all. It is checked with a positive control right behind it, so "nothing happened" cannot
     * pass because the block was never reshapeable in the first place. Only the absence of side
     * effects is asserted, not the {@code false} the method returns - {@code ItemStack#releaseUsing}
     * throws that value away, so nothing in the game can observe it.
     *
     * <p>{@code stopUsingItem} between the cases is load bearing - {@code startUsingItem} is a
     * no-op while the player is already using something, so without it every case after the first
     * would inherit the first one's answer.
     *
     * <p>What breaks this: the {@code null} check on the transformation result disappearing, so
     * every right click charges; the Constructor's Touch guard at the head of the reverse branch
     * disappearing, which hands every hammer the backward direction - that guard is otherwise only
     * pinned for {@code finishUsingItem}, by
     * {@link ConsumptionAndDurabilityTests#sledgehammerSecondaryUseWearsDownOnlyTheSurvivalPlayer},
     * and not for the {@code CONSUME} path through {@code useOn} that this test drives; the diamond
     * block branch moving behind the transformation lookup, where a diamond block has no stairs
     * variant and would fall through to {@code PASS}; and {@code releaseUsing} growing a body, which
     * would turn the deliberate ten-tick wind-up into a tap.
     */
    public static void sledgehammerRightClickChargesOnlyOnBlocksItCanReshape(GameTestHelper helper) {
        ServerPlayer player = inLevelPlayer(helper, ABOVE_CENTRE, 0.0F, 90.0F, true);
        ItemStack hammer = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);

        // --- a diamond block starts the crush ---
        helper.setBlock(CENTRE, Blocks.DIAMOND_BLOCK);
        Assertions.valueEqual(helper, useOnTop(helper, player, hammer, CENTRE), InteractionResult.CONSUME,
                "right clicking a diamond block did not start the charge");
        helper.assertTrue(player.isUsingItem(), "the player is not winding up after a diamond block click");
        player.stopUsingItem();

        // --- a full block with a stairs variant starts the reshaping ---
        helper.setBlock(CENTRE, Blocks.STONE);
        Assertions.valueEqual(helper, useOnTop(helper, player, hammer, CENTRE), InteractionResult.CONSUME,
                "right clicking stone did not start the charge");
        helper.assertTrue(player.isUsingItem(), "the player is not winding up after a stone click");
        player.stopUsingItem();

        // --- a full block without one is passed through ---
        helper.setBlock(CENTRE, Blocks.DIRT);
        Assertions.valueEqual(helper, useOnTop(helper, player, hammer, CENTRE), InteractionResult.PASS,
                "the hammer claimed a block it cannot turn into stairs");
        helper.assertFalse(player.isUsingItem(), "the hammer wound up on a block it cannot reshape");

        // --- sneaking without Constructor's Touch is refused outright ---
        // The block has to be a slab: it is the one thing the reverse direction could walk back, so
        // the refusal can only come from the missing enchantment. On a block the reverse branch has
        // no answer for anyway - stone, say - this would read PASS with the guard deleted as well.
        helper.setBlock(CENTRE, Blocks.STONE_SLAB);
        player.setShiftKeyDown(true);
        Assertions.valueEqual(helper, useOnTop(helper, player, hammer, CENTRE), InteractionResult.PASS,
                "the reverse direction started without Constructor's Touch");
        helper.assertFalse(player.isUsingItem(), "the hammer wound up on a reverse click it cannot finish");

        // --- with the enchantment, the very same slab is taken: the A of the A/B above ---
        ItemStack touch = hammerWith(helper, ModEnchantments.CONSTRUCTORS_TOUCH, 1);
        helper.setBlock(CENTRE, Blocks.STONE_SLAB);
        Assertions.valueEqual(helper, useOnTop(helper, player, touch, CENTRE), InteractionResult.CONSUME,
                "the reverse direction refused a slab although the hammer has Constructor's Touch");
        player.stopUsingItem();
        player.setShiftKeyDown(false);

        // --- letting go early does nothing, and the very next finish proves the setup was live ---
        helper.setBlock(CENTRE, Blocks.STONE);
        ItemStack released = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);
        player.setItemInHand(InteractionHand.MAIN_HAND, released);
        released.getItem().useOn(topClick(helper, player, CENTRE));
        released.getItem().releaseUsing(released, helper.getLevel(), player, 5);
        player.stopUsingItem();

        helper.assertBlockPresent(Blocks.STONE, CENTRE);
        Assertions.valueEqual(helper, released.getDamageValue(), 0, "an aborted right click still cost durability");

        released.getItem().finishUsingItem(released, helper.getLevel(), player);
        helper.assertBlockPresent(Blocks.STONE_STAIRS, CENTRE);

        TestCleanup.succeed(helper);
    }

    /**
     * The charged right click walks a block one step down the shape ladder and, while sneaking
     * with Constructor's Touch, one step back up it: full block to stairs to slab going forward,
     * slab to stairs to full block going back.
     *
     * <p>Two of those steps have never been executed by any test:
     * <ul>
     *   <li><b>stairs to slab</b> going forward - the existing durability test turns stairs
     *       straight back into a block, so the forward second step was dead;</li>
     *   <li><b>the name fallbacks</b> of the backward third step. {@code brick_stairs} has to fall
     *       back to {@code bricks} and {@code oak_stairs} to {@code oak_planks}, because neither
     *       {@code brick} nor {@code oak} is a block. Those two branches are the entire reason
     *       stone is a bad test case: {@code stone} exists, so the fallbacks are never touched.</li>
     * </ul>
     *
     * <p>The new stairs are also checked for orientation, not just for type. Facing has to follow
     * the player, which is the only thing that makes the tool usable while building; the existing
     * test looked at the block id alone and would have passed on a stair pointing anywhere.
     *
     * <p>What breaks this: the second forward branch disappearing, which leaves the ladder with no
     * way down to a slab; either name fallback disappearing, which turns the hammer into a tool
     * that quietly refuses brick and every wood stair; and
     * {@code ChiselItem#applyIntuitiveOrientation} no longer being applied, which produces stairs
     * facing north no matter where the player stands.
     */
    public static void sledgehammerReshapesFullBlocksStairsAndSlabs(GameTestHelper helper) {
        ServerPlayer player = inLevelPlayer(helper, ABOVE_CENTRE, 0.0F, 90.0F, true);
        ItemStack hammer = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        player.setShiftKeyDown(false);

        // --- forward, step one: the stairs point where the player looks ---
        helper.setBlock(CENTRE, Blocks.STONE);
        finish(helper, player, hammer);
        helper.assertBlockPresent(Blocks.STONE_STAIRS, CENTRE);
        assertStair(helper, CENTRE, Direction.SOUTH, Half.BOTTOM, "a stair cut while facing south");

        // --- forward, step two: stairs become a slab ---
        finish(helper, player, hammer);
        helper.assertBlockPresent(Blocks.STONE_SLAB, CENTRE);

        // --- the facing really is read from the player, not hard coded ---
        helper.setBlock(CENTRE, Blocks.STONE);
        look(player, 180.0F, 90.0F);
        finish(helper, player, hammer);
        helper.assertBlockPresent(Blocks.STONE_STAIRS, CENTRE);
        assertStair(helper, CENTRE, Direction.NORTH, Half.BOTTOM, "a stair cut while facing north");
        look(player, 0.0F, 90.0F);

        // --- backward, with Constructor's Touch: slab to stairs ---
        ItemStack touch = hammerWith(helper, ModEnchantments.CONSTRUCTORS_TOUCH, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, touch);
        player.setShiftKeyDown(true);

        helper.setBlock(CENTRE, Blocks.STONE_SLAB);
        finish(helper, player, touch);
        helper.assertBlockPresent(Blocks.STONE_STAIRS, CENTRE);
        assertStair(helper, CENTRE, Direction.SOUTH, Half.BOTTOM, "a stair recovered from a slab");

        // --- backward, third step, through the two name fallbacks ---
        helper.setBlock(CENTRE, Blocks.BRICK_STAIRS);
        finish(helper, player, touch);
        helper.assertBlockPresent(Blocks.BRICKS, CENTRE);

        helper.setBlock(CENTRE, Blocks.OAK_STAIRS);
        finish(helper, player, touch);
        helper.assertBlockPresent(Blocks.OAK_PLANKS, CENTRE);

        player.setShiftKeyDown(false);
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // SPEED, BLOCK COUNT AND CHARGE TIME
    // =====================================================================================

    /**
     * The hammer mines faster the more blocks a swing is about to take, and Override II lifts the
     * blocks of the other three tool classes out of the bare-hands speed of {@code 1.0}.
     *
     * <p>{@code getBlockCountForSpeed} is the input to that: nine for a plain hammer, twenty-five
     * with Radius, doubled by Break Through, and Break Through does <em>not</em> care about its
     * level. The multiplier is then measured as a ratio against the hammer's own material speed,
     * so the numbers stay statements about the curve (1.45 at nine blocks, 1.85 at the cap) rather
     * than a copy of the material table. Radius plus Break Through - fifty blocks - has to land on
     * the same 1.85 as twenty-five, which is where the cap lives.
     *
     * <p>The Override II half checks both sides: a plain hammer on dirt and on hay is exactly
     * {@code 1.0}, the bare-hand speed with no multiplier at all, and the same hammer with
     * Override II mines them at full material speed times the nine-block multiplier. All three
     * tool classes the branch names are measured - dirt for the shovel, an oak log for the axe and
     * hay for the hoe - because the list in {@code getDestroySpeed} is a second copy of the one in
     * {@code isCorrectToolForDrops} and can lose a single tag on its own. And glass stays a wrong-tool block
     * even at Override II, which is what keeps {@code isCorrectToolForDrops} from degenerating
     * into "true for everything".
     *
     * <p>{@link ToolBehaviourTests#sledgehammerOverrideLevelsWidenBlockSelection} makes the same
     * Override II claim from the block layout and asserts that the speed on dirt merely
     * <em>rises</em>. What is added here is the number it rises to, the whole block-count ladder
     * behind it, and the two limits of the widening: not at level I, and not past the three tool
     * classes it names.
     *
     * <p>What breaks this: the {@code baseSpeed > 1.0F} guard going away, which multiplies the
     * bare-hand speed and lets a plain hammer dig dirt faster than a shovel; the cap at
     * twenty-five disappearing, which makes a fully enchanted hammer accelerate without bound; and
     * Break Through starting to scale with its level, which would double the count twice.
     */
    public static void sledgehammerSpeedAndBlockCountScaleWithItsEnchantments(GameTestHelper helper) {
        SledgehammerItem hammer = ModItems.DIAMOND_SLEDGEHAMMER;
        float material = hammer.getMaterial().speed();
        helper.assertTrue(material > 1.0F, "the diamond hammer's material speed is no longer above bare hands");

        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        BlockState glass = Blocks.GLASS.defaultBlockState();
        // Hay stands for the hoe class: it is in mineable/hoe and in no other mining tag, so its
        // speed can only leave 1.0 through the hoe line of getDestroySpeed. Dirt and a log cover
        // the shovel and axe lines, and getDestroySpeed carries its own copy of that three-tag
        // list - each of the three can be dropped from it without touching the other two.
        BlockState hay = Blocks.HAY_BLOCK.defaultBlockState();

        ItemStack plain = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);
        ItemStack radius = hammerWith(helper, ModEnchantments.RADIUS, 1);
        ItemStack through = hammerWith(helper, ModEnchantments.BREAK_THROUGH, 1);
        ItemStack throughTwo = hammerWith(helper, ModEnchantments.BREAK_THROUGH, 2);
        ItemStack both = hammerWith(helper, ModEnchantments.RADIUS, 1);
        both.enchant(enchantment(helper, ModEnchantments.BREAK_THROUGH), 1);

        Assertions.valueEqual(helper, SledgehammerItem.getBlockCountForSpeed(plain), 9, "block count of a plain hammer");
        Assertions.valueEqual(helper, SledgehammerItem.getBlockCountForSpeed(radius), 25, "block count with Radius I");
        Assertions.valueEqual(helper, SledgehammerItem.getBlockCountForSpeed(through), 18, "block count with Break Through I");
        Assertions.valueEqual(helper, SledgehammerItem.getBlockCountForSpeed(throughTwo), 18,
                "block count with Break Through II; the depth doubling is level independent");
        Assertions.valueEqual(helper, SledgehammerItem.getBlockCountForSpeed(both), 50,
                "block count with Radius I and Break Through I");

        assertSpeed(helper, hammer.getDestroySpeed(plain, stone), material * 1.45F, "nine blocks on stone");
        assertSpeed(helper, hammer.getDestroySpeed(radius, stone), material * 1.85F, "twenty-five blocks on stone");
        assertSpeed(helper, hammer.getDestroySpeed(both, stone), material * 1.85F,
                "fifty blocks on stone; the multiplier is capped at twenty-five");

        // --- Override II: what the hammer counts as its own tool class ---
        ItemStack override = hammerWith(helper, ModEnchantments.OVERRIDE, 2);
        assertSpeed(helper, hammer.getDestroySpeed(plain, dirt), 1.0F, "a plain hammer on dirt");
        assertSpeed(helper, hammer.getDestroySpeed(override, dirt), material * 1.45F, "an Override II hammer on dirt");
        assertSpeed(helper, hammer.getDestroySpeed(override, log), material * 1.45F, "an Override II hammer on a log");
        assertSpeed(helper, hammer.getDestroySpeed(plain, hay), 1.0F, "a plain hammer on hay");
        assertSpeed(helper, hammer.getDestroySpeed(override, hay), material * 1.45F, "an Override II hammer on hay");
        assertSpeed(helper, hammer.getDestroySpeed(override, glass), 1.0F,
                "an Override II hammer on glass, which is in none of the mineable tags");

        // That Override II turns dirt and logs into harvestable blocks is pinned end to end in
        // ToolBehaviourTests#sledgehammerOverrideLevelsWidenBlockSelection. The two cases left over
        // are the ones that keep that widening from becoming a blanket yes: it must not happen at
        // level I, and it must not reach blocks outside the three named tool classes.
        helper.assertTrue(hammer.isCorrectToolForDrops(plain, stone), "a plain hammer stopped harvesting stone");
        helper.assertFalse(hammer.isCorrectToolForDrops(hammerWith(helper, ModEnchantments.OVERRIDE, 1), dirt),
                "Override I already widened the tool classes; that is Override II's job");
        helper.assertFalse(hammer.isCorrectToolForDrops(override, glass),
                "Override II harvests glass, so it no longer names the three tool classes but simply says yes");

        TestCleanup.succeed(helper);
    }

    /**
     * The wind-up before a reshaping click is shorter the better the hammer is, and Efficiency
     * shortens it further - clamped at both ends so that no hammer takes longer than two seconds
     * and none becomes an instant click.
     *
     * <p>The expected tick counts fall out of vanilla's own material speeds (stone 4, copper 5,
     * iron 6, diamond 8, netherite 9, gold 12) and the mod's Enderite material at 10, run through
     * the ten-second-over-speed formula. Two of them are the clamps rather than the formula: the
     * stone hammer computes fifty ticks and has to come out at forty, and a gold hammer with a
     * deliberately over-cap Efficiency X computes three and has to come out at four. Those two are
     * the only assertions here that can distinguish a missing clamp from a working one.
     *
     * <p>Gold is worth its own line: it is the fastest material in the game and the flimsiest, so
     * the gold hammer having the shortest wind-up of all is a real design statement rather than a
     * rounding artefact.
     *
     * <p>What breaks this: the clamp going away, which gives the stone hammer a two-and-a-half
     * second wind-up and an enchanted gold hammer a click so short the animation cannot be seen;
     * Efficiency dropping out of the factor, which makes the enchantment pointless on a hammer;
     * and the formula switching from material speed to a flat number, which erases the difference
     * between the tiers.
     */
    public static void sledgehammerChargeTimeShortensWithMaterialAndEfficiency(GameTestHelper helper) {
        ServerPlayer player = inLevelPlayer(helper, ABOVE_CENTRE, 0.0F, 90.0F, true);

        assertUseDuration(helper, player, ModItems.STONE_SLEDGEHAMMER, 40, "stone (raw 50, held at the upper clamp)");
        assertUseDuration(helper, player, ModItems.COPPER_SLEDGEHAMMER, 40, "copper");
        assertUseDuration(helper, player, ModItems.IRON_SLEDGEHAMMER, 33, "iron");
        assertUseDuration(helper, player, ModItems.DIAMOND_SLEDGEHAMMER, 25, "diamond");
        assertUseDuration(helper, player, ModItems.NETHERITE_SLEDGEHAMMER, 22, "netherite");
        assertUseDuration(helper, player, ModItems.ENDERITE_SLEDGEHAMMER, 20, "enderite");
        assertUseDuration(helper, player, ModItems.GOLD_SLEDGEHAMMER, 16, "gold, the fastest material in the game");

        ItemStack efficient = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);
        efficient.enchant(enchantment(helper, Enchantments.EFFICIENCY), 5);
        Assertions.valueEqual(helper, efficient.getItem().getUseDuration(efficient, player), 6,
                "charge time of a diamond hammer with Efficiency V");

        // Deliberately above the vanilla cap: no legal level gets the raw value under the lower
        // clamp, so this is the only way to prove the clamp is doing anything.
        ItemStack overCharged = new ItemStack(ModItems.GOLD_SLEDGEHAMMER);
        overCharged.enchant(enchantment(helper, Enchantments.EFFICIENCY), 10);
        Assertions.valueEqual(helper, overCharged.getItem().getUseDuration(overCharged, player), 4,
                "charge time of a gold hammer with Efficiency X (raw 3, held at the lower clamp)");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // ITEM FRAMES
    // =====================================================================================

    /**
     * Hitting a framed smithing template with a sledgehammer and a glow ink sac in the off hand
     * turns it into the mod's Glowing Trim Template; glowstone dust turns it into the Emitting
     * one. It is the only way to obtain either item by hand, and nothing else in the suite runs
     * {@code SledgehammerEntityInteraction} at all.
     *
     * <p>Everything around the conversion is checked too, because each of those conditions is what
     * keeps the interaction from firing during ordinary play: the off hand alone must not trigger
     * it, a frame holding something that is not a template must be left alone, and the payment -
     * one ink sac and one point of durability - only happens outside creative.
     *
     * <p>The frame content check is a <em>name</em> heuristic: {@code isTrimTemplate} asks whether
     * the item's registered name contains {@code trim_smithing_template}. A diamond therefore has
     * to be refused, which is what the negative case pins; note that this also means the mod's own
     * {@code glowing_trim_template} is not a template by that rule, so a converted frame cannot be
     * converted again.
     *
     * <p>The survival half uses {@code makeMockPlayer(GameType.SURVIVAL)} with its abilities
     * refreshed from the game type - the same factory {@link OreGenAndItemFrameTests} uses for the
     * frame lock. {@code instabuild} is what actually decides whether any {@code hurtAndBreak} in
     * the game does anything, and that factory does not set it by itself.
     *
     * <p>What breaks this: the {@code MAIN_HAND} check going away, which fires the conversion when
     * the player swaps hands; {@code isTrimTemplate} loosening, which would let a hammer swing
     * replace anything in any frame; and the {@code isCreative()} guard going away in either
     * direction - a survival player getting free templates, or a creative one being billed for
     * them.
     */
    public static void sledgehammerTurnsFramedTrimTemplatesGlowing(GameTestHelper helper) {
        Player creative = mockPlayer(helper, GameType.CREATIVE);
        Player survival = mockPlayer(helper, GameType.SURVIVAL);
        helper.assertTrue(creative.isCreative() && !survival.isCreative(),
                "the two mock players do not report the game modes they were asked for");

        // --- glow ink sac: the glowing template, free in creative ---
        ItemFrame frame = templateFrame(helper, new BlockPos(3, 2, 3));
        ItemStack hammer = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);
        ItemStack sacs = new ItemStack(Items.GLOW_INK_SAC, 4);
        arm(creative, hammer, sacs);

        Assertions.valueEqual(helper, attack(helper, creative, InteractionHand.MAIN_HAND, frame),
                InteractionResult.SUCCESS, "the hammer did not convert a framed smithing template");
        helper.assertTrue(frame.getItem().is(ModItems.GLOWING_TRIM_TEMPLATE),
                "the frame holds " + frame.getItem() + " instead of the glowing trim template");
        Assertions.valueEqual(helper, sacs.getCount(), 4, "glow ink sacs a creative player was charged");
        Assertions.valueEqual(helper, hammer.getDamageValue(), 0, "hammer wear charged to a creative player");

        // --- glowstone dust: the emitting template, and survival pays for it ---
        ItemFrame second = templateFrame(helper, new BlockPos(5, 2, 3));
        ItemStack dust = new ItemStack(Items.GLOWSTONE_DUST, 4);
        arm(survival, hammer, dust);

        Assertions.valueEqual(helper, attack(helper, survival, InteractionHand.MAIN_HAND, second),
                InteractionResult.SUCCESS, "glowstone dust did not convert a framed smithing template");
        helper.assertTrue(second.getItem().is(ModItems.EMITTING_TRIM_TEMPLATE),
                "the frame holds " + second.getItem() + " instead of the emitting trim template");
        Assertions.valueEqual(helper, dust.getCount(), 3, "glowstone dust left after a survival conversion");
        Assertions.valueEqual(helper, hammer.getDamageValue(), 1, "hammer wear after a survival conversion");

        // --- the off hand must not trigger it ---
        ItemFrame third = templateFrame(helper, new BlockPos(3, 2, 5));
        ItemStack fresh = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);
        arm(creative, fresh, new ItemStack(Items.GLOW_INK_SAC, 4));
        Assertions.valueEqual(helper, attack(helper, creative, InteractionHand.OFF_HAND, third),
                InteractionResult.PASS, "an off hand hit converted the template");
        helper.assertTrue(third.getItem().is(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE),
                "the off hand hit changed the frame anyway");

        // --- and a frame that holds something else is left alone ---
        ItemFrame diamondFrame = templateFrame(helper, new BlockPos(5, 2, 5));
        diamondFrame.setItem(new ItemStack(Items.DIAMOND), false);
        Assertions.valueEqual(helper, attack(helper, creative, InteractionHand.MAIN_HAND, diamondFrame),
                InteractionResult.PASS, "the hammer claimed a frame that holds no smithing template");
        helper.assertTrue(diamondFrame.getItem().is(Items.DIAMOND),
                "the hammer replaced a diamond in a frame with a trim template");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /**
     * The ordinary in-level mock player with {@code instabuild} set explicitly. It keeps its
     * connection and its {@code ServerPlayerGameMode}, which the block break hook needs, and it has
     * to be handed back at the end or the gametest server stalls on shutdown. Its {@code gameMode()}
     * is hard-wired to creative and cannot be moved; only {@code instabuild} is reachable, and that
     * is the field every durability guard in the game actually reads.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer inLevelPlayer(GameTestHelper helper, Vec3 relativePos,
                                              float yRot, float xRot, boolean instabuild) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relativePos);
        player.snapTo(pos.x, pos.y, pos.z, yRot, xRot);
        player.getAbilities().instabuild = instabuild;
        player.setShiftKeyDown(false);
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /**
     * A player that really answers with the game mode it was asked for. It is never added to the
     * level or the player list, so it has no connection - fine here, because the frame interaction
     * stays inside the world and the item. The abilities are refreshed by hand: this factory,
     * unlike its {@code ServerPlayer} sibling, does not do it, and {@code instabuild} decides
     * whether the durability charge below happens at all.
     */
    private static Player mockPlayer(GameTestHelper helper, GameType mode) {
        Player player = helper.makeMockPlayer(mode);
        mode.updatePlayerAbilities(player.getAbilities());
        Vec3 pos = helper.absoluteVec(new Vec3(3.5, 2.0, 1.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }

    /** Points the player somewhere without moving it; {@code snapTo} writes the previous tick too. */
    private static void look(ServerPlayer player, float yRot, float xRot) {
        player.snapTo(player.getX(), player.getY(), player.getZ(), yRot, xRot);
    }

    /**
     * Runs the mod's block break hook for the given origin. Vanilla breaks the block that was hit;
     * the hook is only responsible for the surrounding blocks, so the origin stays in place on
     * purpose - which is what makes it visible if the hook ever takes it as well.
     */
    private static void swing(GameTestHelper helper, ServerPlayer player, BlockPos relativeOrigin) {
        BlockPos origin = helper.absolutePos(relativeOrigin);
        SledgehammerUsageEvent.handleBeforeBlockBreak(
                helper.getLevel(), player, origin, helper.getLevel().getBlockState(origin), null);
    }

    /** The 3x3 face around {@code centre} in the horizontal plane. */
    private static void fillFace(GameTestHelper helper, BlockPos centre, Block block) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(centre.offset(dx, 0, dz), block);
            }
        }
    }

    /** The eight positions around {@code centre}, origin excluded. */
    private static List<BlockPos> faceNeighbours(BlockPos centre) {
        List<BlockPos> out = new ArrayList<>(NEIGHBOURS);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    out.add(centre.offset(dx, 0, dz));
                }
            }
        }
        return out;
    }

    /**
     * What one plain {@code gameMode.destroyBlock} costs the hammer, measured on a probe block
     * outside the field.
     *
     * <p>This is not the mod's charge - it is vanilla's. With {@code instabuild} off,
     * {@code ServerPlayerGameMode#destroyBlock} runs {@code ItemStack#mineBlock}, which spends the
     * tool component's {@code damagePerBlock} for every block that has a destroy speed. Every
     * expectation about the mod's own charge is stated as the remainder after this has been taken
     * off, so a vanilla change to tool wear cannot silently rewrite what the mod is supposed to
     * charge.
     */
    private static int measureVanillaWearPerBlock(GameTestHelper helper, ServerPlayer player,
                                                  ItemStack hammer, Block block) {
        helper.setBlock(PROBE, block);
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        int before = hammer.getDamageValue();

        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(PROBE)),
                "the probe block could not be broken at all, so no baseline can be measured");
        helper.assertBlockPresent(Blocks.AIR, PROBE);

        int cost = hammer.getDamageValue() - before;
        helper.assertTrue(cost >= 0, "breaking a block repaired the hammer instead of wearing it");
        return cost;
    }

    /**
     * Clears a fresh 3x3 face of {@code block} with {@code hammer} and asserts what the mod charged
     * per neighbour, vanilla's own share already subtracted.
     */
    private static void assertSwingCharge(GameTestHelper helper, ServerPlayer player, ItemStack hammer,
                                          Block block, int expectedPerBlock, String what) {
        int vanillaPerBlock = measureVanillaWearPerBlock(helper, player, hammer, block);

        fillFace(helper, CENTRE, block);
        hammer.setDamageValue(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        swing(helper, player, CENTRE);

        for (BlockPos pos : faceNeighbours(CENTRE)) {
            helper.assertBlockPresent(Blocks.AIR, pos);
        }
        helper.assertBlockPresent(block, CENTRE);

        int modShare = hammer.getDamageValue() - NEIGHBOURS * vanillaPerBlock;
        Assertions.valueEqual(helper, modShare, NEIGHBOURS * expectedPerBlock,
                "durability the mod charged for " + what + " (" + NEIGHBOURS + " blocks, vanilla's "
                        + vanillaPerBlock + " per block already subtracted)");
    }

    /**
     * Compares the positions a swing would take against a face this method builds itself: the 3x3
     * spanned by {@code axisA} and {@code axisB} around the origin, repeated {@code depth} times
     * along {@code into}.
     *
     * <p>Both the set and the list length are checked, so a position that is returned twice - and
     * would therefore be mined and billed twice - cannot hide inside a matching set.
     */
    private static void assertField(GameTestHelper helper, ServerPlayer player, BlockPos origin, int depth,
                                    Direction axisA, Direction axisB, Direction into, String situation) {
        Set<BlockPos> expected = new HashSet<>();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                for (int d = 0; d <= depth; d++) {
                    expected.add(origin.offset(
                            axisA.getStepX() * a + axisB.getStepX() * b + into.getStepX() * d,
                            axisA.getStepY() * a + axisB.getStepY() * b + into.getStepY() * d,
                            axisA.getStepZ() * a + axisB.getStepZ() * b + into.getStepZ() * d));
                }
            }
        }

        List<BlockPos> actual = SledgehammerItem.getBlocksToBeDestroyed(1, origin, player);
        Assertions.valueEqual(helper, actual.size(), expected.size(),
                "number of positions a swing takes while " + situation + "; a duplicate would be mined twice");
        Assertions.valueEqual(helper, new HashSet<>(actual), expected,
                "the face a swing takes while " + situation + " (depth " + depth + " towards " + into + ")");
    }

    /** Right clicks the centre of a block's top face, server side. */
    private static InteractionResult useOnTop(GameTestHelper helper, ServerPlayer player,
                                              ItemStack stack, BlockPos relativePos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack.getItem().useOn(topClick(helper, player, relativePos));
    }

    private static UseOnContext topClick(GameTestHelper helper, ServerPlayer player, BlockPos relativePos) {
        BlockPos pos = helper.absolutePos(relativePos);
        Vec3 hit = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        return new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(hit, Direction.UP, pos, false));
    }

    /**
     * Ends a charged right click. {@code finishUsingItem} re-picks the target itself through
     * {@code player.pick(5.0, 0.0F, false)}, so the aim has to be set up on the player rather than
     * handed in - the callers all stand straight above the block.
     */
    private static void finish(GameTestHelper helper, ServerPlayer player, ItemStack hammer) {
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        hammer.getItem().finishUsingItem(hammer, helper.getLevel(), player);
    }

    private static void assertStair(GameTestHelper helper, BlockPos relativePos,
                                    Direction facing, Half half, String what) {
        BlockState state = helper.getBlockState(relativePos);
        Assertions.valueEqual(helper, state.getValue(StairBlock.FACING), facing, "facing of " + what);
        Assertions.valueEqual(helper, state.getValue(StairBlock.HALF), half, "half of " + what);
    }

    /**
     * Mining speeds are floats built out of a division, so they are compared with a tolerance. The
     * tolerance is far below the smallest step the curve makes (0.4 of a material speed between
     * nine and twenty-five blocks), so it cannot hide a wrong multiplier.
     */
    private static void assertSpeed(GameTestHelper helper, float actual, float expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < 0.01F,
                "mining speed for " + what + ": expected " + expected + " but got " + actual);
    }

    private static void assertUseDuration(GameTestHelper helper, ServerPlayer player, Item hammer,
                                          int expected, String tier) {
        ItemStack stack = new ItemStack(hammer);
        Assertions.valueEqual(helper, stack.getItem().getUseDuration(stack, player), expected,
                "charge time in ticks of the " + tier + " sledgehammer");
    }

    private static ItemStack hammerWith(GameTestHelper helper, ResourceKey<Enchantment> key, int level) {
        ItemStack stack = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);
        stack.enchant(enchantment(helper, key), level);
        return stack;
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    /** Spawns a frame holding a vanilla smithing template, plus the block it hangs on. */
    private static ItemFrame templateFrame(GameTestHelper helper, BlockPos relativePos) {
        Direction facing = Direction.SOUTH;
        helper.setBlock(relativePos.relative(facing.getOpposite()), Blocks.STONE);
        ItemFrame frame = new ItemFrame(helper.getLevel(), helper.absolutePos(relativePos), facing);
        helper.getLevel().addFreshEntity(frame);
        frame.setItem(new ItemStack(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE), false);
        TestCleanup.before(helper, frame::discard);
        return frame;
    }

    /**
     * Puts the two stacks into the player's hands by reference, so a test can look at the very
     * objects afterwards and see what the interaction took out of them.
     */
    private static void arm(Player player, ItemStack mainHand, ItemStack offHand) {
        player.setItemInHand(InteractionHand.MAIN_HAND, mainHand);
        player.setItemInHand(InteractionHand.OFF_HAND, offHand);
    }

    private static InteractionResult attack(GameTestHelper helper, Player player,
                                            InteractionHand hand, ItemFrame frame) {
        return SledgehammerEntityInteraction.handleAttackEntity(player, helper.getLevel(), hand, frame);
    }
}
