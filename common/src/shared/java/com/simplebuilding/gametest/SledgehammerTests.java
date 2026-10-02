package com.simplebuilding.gametest;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.SledgehammerItem;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The sledgehammer, end to end: which blocks a swing takes, what it charges for them, what the
 * charged right click reshapes, and how fast it mines and charges. (The trim template
 * upgrade happens on a placed template only - {@link PlacedTemplateTests}; the old item frame route is gone.)
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
 * <h2>The enchantability tag</h2>
 *
 * <p>{@code simplebuilding:sledgehammer_tools} is the {@code supported_items} and
 * {@code primary_items} of Override, Radius and Break Through and part of the Constructor's Touch,
 * Range and Versatility item sets. Until 2026-09 it left the <b>Enderite Sledgehammer</b> out, so
 * the mod's best hammer was the only one that could carry none of its own enchantments; it holds
 * all seven tiers now, and {@code WandEnchantmentTests#theBuildingEnchantmentsReachEveryToolWhoseCodeReadsThem}
 * asserts all three enchantments on every tier. The tests below use the diamond hammer wherever
 * an enchantment is involved, which is a choice of yardstick and not a workaround any more.
 *
 * <h2>Not covered</h2>
 * <ul>
 *   <li><b>The re-entrancy guard {@code HARVESTED_BLOCKS}.</b> It only fires when the loader's
 *       break event runs <em>inside</em> {@code gameMode.destroyBlock} and calls the hook again.
 *       The shared suite invokes the hook directly, so a nested call cannot be produced from here
 *       and every assertion about the guard would pass without executing it. What can be checked -
 *       and is, in {@link #sledgehammerBillsTwoDurabilityPerBlockAndThreeForTheWrongTool} - is the
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
     * every block the hammer breaks costs its base wear ({@code SledgehammerItem#WEAR_PER_BLOCK},
     * charged through vanilla's {@code mineBlock}). Six broken stone blocks must cost exactly six
     * times that - nothing extra for the bedrock or the hole in the face.
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
        helper.assertValueEqual(cleared, NEIGHBOURS - 2, "stone neighbours the swing was meant to clear");

        int modShare = hammer.getDamageValue() - cleared * vanillaPerBlock;
        helper.assertValueEqual(modShare, 0,
                "the swing charged " + modShare + " points for " + cleared + " broken blocks; the "
                        + "bedrock or the hole in the face was billed as if it had been mined "
                        + "(vanilla's own share of " + vanillaPerBlock + " per block is already subtracted)");

        helper.killAllEntitiesOfClass(ItemEntity.class);
        helper.succeed();
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
        helper.assertValueEqual(SledgehammerItem.getBlocksToBeDestroyed(1, origin, player).size(), 9,
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
        helper.succeed();
    }

    /**
     * Every extra block a swing takes costs one point of durability, and two when the hammer is
     * the wrong tool for it - that is the whole price of the Override enchantment: it widens what
     * the hammer may break, and blocks outside its own tool class cost double.
     *
     * <p>Three faces, one hammer:
     * <ul>
     *   <li><b>stone</b>, plain hammer - the pickaxe case, two points each (the hammer wears twice as
     *       fast as a pickaxe, owner 2026-09-28), nothing on top;</li>
     *   <li><b>glass</b>, Override II - glass is in none of the four vanilla {@code mineable/*}
     *       tags, so Override II lets the hammer break it while
     *       {@code SledgehammerItem#isCorrectToolForDrops} still says no: one point more each;</li>
     *   <li><b>dirt</b>, Override II - dirt <em>is</em> in {@code mineable/shovel}, which
     *       Override II adds to the hammer's tool classes, so nothing on top again.</li>
     * </ul>
     * The last two together are the point: Override II is not a blanket "everything is cheap now",
     * it moves exactly the axe, shovel and hoe blocks into the cheap class.
     *
     * <p>Every number is a <em>remainder</em>. {@code ItemStack#mineBlock} charges the base wear of
     * {@code SledgehammerItem#WEAR_PER_BLOCK} per block (vanilla's one point plus the hammer's own),
     * measured on a probe block of the same type first and subtracted; the probe also pins that base
     * wear at two.
     *
     * <p>What breaks this: deleting the {@code hurtAndBreak} in the hook (an area miner that never
     * wears out); making the cost flat, which removes the entire drawback of mining foreign blocks
     * with Override II; and losing the axe/shovel/hoe branch of {@code isCorrectToolForDrops},
     * which would silently double the price of every dirt block an Override II hammer takes. The
     * per-block arithmetic also fails if a neighbour is ever processed twice.
     */
    public static void sledgehammerBillsTwoDurabilityPerBlockAndThreeForTheWrongTool(GameTestHelper helper) {
        ServerPlayer player = inLevelPlayer(helper, ABOVE_CENTRE, 0.0F, 90.0F, false);

        assertSwingCharge(helper, player, new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER), Blocks.STONE, 0,
                "a plain hammer on its own pickaxe blocks");
        assertSwingCharge(helper, player, hammerWith(helper, ModEnchantments.OVERRIDE, 2), Blocks.GLASS, 1,
                "an Override II hammer on glass, which is in none of the mineable tags");
        assertSwingCharge(helper, player, hammerWith(helper, ModEnchantments.OVERRIDE, 2), Blocks.DIRT, 0,
                "an Override II hammer on dirt, which Override II makes a correct tool for");

        helper.killAllEntitiesOfClass(ItemEntity.class);
        helper.succeed();
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
        helper.assertValueEqual(cleared, 1,
                "neighbours a hammer with one point of durability left managed to break");
        helper.assertBlockPresent(Blocks.STONE, CENTRE);

        helper.killAllEntitiesOfClass(ItemEntity.class);
        helper.succeed();
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

        helper.succeed();
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

        // --- a diamond block takes a strike, never a charge (owner 2026-10-02: eight strikes) ---
        helper.setBlock(CENTRE, Blocks.DIAMOND_BLOCK);
        helper.assertValueEqual(useOnTop(helper, player, hammer, CENTRE), InteractionResult.SUCCESS,
                "right clicking a diamond block did not strike it");
        helper.assertFalse(player.isUsingItem(), "the player winds up after a diamond block click");
        helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, CENTRE);

        // --- a full block with a stairs variant starts the reshaping ---
        helper.setBlock(CENTRE, Blocks.STONE);
        helper.assertValueEqual(useOnTop(helper, player, hammer, CENTRE), InteractionResult.CONSUME,
                "right clicking stone did not start the charge");
        helper.assertTrue(player.isUsingItem(), "the player is not winding up after a stone click");
        player.stopUsingItem();

        // --- a full block without one is passed through ---
        helper.setBlock(CENTRE, Blocks.DIRT);
        helper.assertValueEqual(useOnTop(helper, player, hammer, CENTRE), InteractionResult.PASS,
                "the hammer claimed a block it cannot turn into stairs");
        helper.assertFalse(player.isUsingItem(), "the hammer wound up on a block it cannot reshape");

        // --- sneaking without Constructor's Touch is refused outright ---
        // The block has to be a slab: it is the one thing the reverse direction could walk back, so
        // the refusal can only come from the missing enchantment. On a block the reverse branch has
        // no answer for anyway - stone, say - this would read PASS with the guard deleted as well.
        helper.setBlock(CENTRE, Blocks.STONE_SLAB);
        player.setShiftKeyDown(true);
        helper.assertValueEqual(useOnTop(helper, player, hammer, CENTRE), InteractionResult.PASS,
                "the reverse direction started without Constructor's Touch");
        helper.assertFalse(player.isUsingItem(), "the hammer wound up on a reverse click it cannot finish");

        // --- with the enchantment, the very same slab is taken: the A of the A/B above ---
        ItemStack touch = hammerWith(helper, ModEnchantments.CONSTRUCTORS_TOUCH, 1);
        helper.setBlock(CENTRE, Blocks.STONE_SLAB);
        helper.assertValueEqual(useOnTop(helper, player, touch, CENTRE), InteractionResult.CONSUME,
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
        helper.assertValueEqual(released.getDamageValue(), 0, "an aborted right click still cost durability");

        chargeAndFinish(released, helper.getLevel(), player);
        helper.assertBlockPresent(Blocks.STONE_STAIRS, CENTRE);

        helper.succeed();
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
        // Vorwaerts-Ersatzregeln (Gegenstueck zu den Rueckwaerts-Fallbacks): ohne sie formt der Hammer
        // weder Bretter noch Ziegel noch Quarzblock zu Treppen.
        Block[][] forward = {
                {Blocks.OAK_PLANKS, Blocks.OAK_STAIRS}, {Blocks.BRICKS, Blocks.BRICK_STAIRS},
                {Blocks.STONE_BRICKS, Blocks.STONE_BRICK_STAIRS}, {Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_STAIRS}};
        for (Block[] pair : forward) {
            Block got = SledgehammerItem.reshapeTarget(pair[0], false, true).orElse(null);
            if (got != pair[1]) {
                throw helper.assertionException("the hammer reshapes " + pair[0] + " into " + got + " instead of " + pair[1]);
            }
        }
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
        helper.succeed();
    }

    // =====================================================================================
    // SPEED, BLOCK COUNT AND CHARGE TIME
    // =====================================================================================

    /**
     * The hammer item itself carries no speed bonus any more: {@code getDestroySpeed} is the plain
     * material speed - the speed of a pickaxe of the same material - whatever Radius and Break
     * Through say, and Override II lifts the blocks of the other three tool classes out of the
     * bare-hands speed of {@code 1.0} onto that same material speed. The slowdown per block lives
     * one level up, in {@code getDestroyProgress}, where player and position are known; its ladder
     * against the enchantments is pinned here (owner, 2026-09-28): every block of the area takes as
     * long as with the pickaxe one tier below, so Radius (a full 5x5, twenty-five blocks) takes
     * twenty-five iron pickaxe blocks and Radius plus Break Through - fifty blocks - fifty; there is
     * no cap any more.
     *
     * <p>The Override II half checks both sides: a plain hammer on dirt and on hay is exactly
     * {@code 1.0}, and the same hammer with Override II mines them at full material speed. All
     * three tool classes the branch names are measured - dirt for the shovel, an oak log for the
     * axe and hay for the hoe - because {@code isOverrideMineable}, which both
     * {@code getDestroySpeed} and {@code isCorrectToolForDrops} ask, can lose a single tag and would
     * then take that whole tool class with it. Glass stays a wrong-tool block even at Override II.
     *
     * <p>What breaks this: a multiplier coming back into {@code getDestroySpeed} (the old 1.25 to
     * 1.85 bonus), a cap on the block count coming back, and the {@code baseSpeed <= 1.0F} guard or
     * a tag of the Override list going away.
     */
    public static void sledgehammerSpeedAndBlockCountScaleWithItsEnchantments(GameTestHelper helper) {
        SledgehammerItem hammer = ModItems.DIAMOND_SLEDGEHAMMER;
        float material = hammer.getMaterial().speed();
        helper.assertTrue(material > 1.0F, "the diamond hammer's material speed is no longer above bare hands");

        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        BlockState glass = Blocks.GLASS.defaultBlockState();
        // Hay stands for the hoe class: it is in mineable/hoe and in no other mining tag.
        BlockState hay = Blocks.HAY_BLOCK.defaultBlockState();

        ItemStack plain = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);
        ItemStack radius = hammerWith(helper, ModEnchantments.RADIUS, 1);
        ItemStack both = hammerWith(helper, ModEnchantments.RADIUS, 1);
        both.enchant(enchantment(helper, ModEnchantments.BREAK_THROUGH), 1);

        // --- the item speed is the pickaxe speed of the material, field size or not ---
        assertSpeed(helper, hammer.getDestroySpeed(plain, stone), material, "a plain hammer on stone");
        assertSpeed(helper, hammer.getDestroySpeed(plain, stone), new ItemStack(Items.DIAMOND_PICKAXE).getDestroySpeed(stone),
                "a plain diamond hammer against a diamond pickaxe on stone");
        assertSpeed(helper, hammer.getDestroySpeed(radius, stone), material,
                "Radius I on stone; the item speed must not know the field size");
        assertSpeed(helper, hammer.getDestroySpeed(both, stone), material, "Radius I and Break Through I on stone");

        // --- Override II: what the hammer counts as its own tool class ---
        ItemStack override = hammerWith(helper, ModEnchantments.OVERRIDE, 2);
        assertSpeed(helper, hammer.getDestroySpeed(plain, dirt), 1.0F, "a plain hammer on dirt");
        assertSpeed(helper, hammer.getDestroySpeed(override, dirt), material, "an Override II hammer on dirt");
        assertSpeed(helper, hammer.getDestroySpeed(override, log), material, "an Override II hammer on a log");
        assertSpeed(helper, hammer.getDestroySpeed(plain, hay), 1.0F, "a plain hammer on hay");
        assertSpeed(helper, hammer.getDestroySpeed(override, hay), material, "an Override II hammer on hay");
        assertSpeed(helper, hammer.getDestroySpeed(override, glass), 1.0F,
                "an Override II hammer on glass, which is in none of the mineable tags");

        helper.assertTrue(hammer.isCorrectToolForDrops(plain, stone), "a plain hammer stopped harvesting stone");
        helper.assertFalse(hammer.isCorrectToolForDrops(hammerWith(helper, ModEnchantments.OVERRIDE, 1), dirt),
                "Override I already widened the tool classes; that is Override II's job");
        helper.assertFalse(hammer.isCorrectToolForDrops(override, glass),
                "Override II harvests glass, so it no longer names the three tool classes but simply says yes");

        // --- the slowdown ladder: every block as long as with the iron pickaxe, no cap ---
        // One block up, so the Break Through layer sits at y = 1 inside the room, not in its floor.
        BlockPos top = CENTRE.above();
        ServerPlayer player = inLevelPlayer(helper, ABOVE_CENTRE.add(0.0, 1.0, 0.0), 0.0F, 90.0F, true);
        fillSquare(helper, top.below(), 2, Blocks.AIR);
        helper.setBlock(top, Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(com.simplebuilding.version.McVersion.PIECEWISE_HAMMER_TIME ? Items.DIAMOND_PICKAXE : Items.IRON_PICKAXE));
        float ironPickaxe = progress(helper, player, top);
        helper.assertTrue(ironPickaxe > 0.0F, "a lone stone block makes no mining progress at all");

        fillSquare(helper, top, 2, Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, radius);
        assertRatio(helper, progress(helper, player, top), ironPickaxe, com.simplebuilding.version.McVersion.PIECEWISE_HAMMER_TIME ? 19.1F : 25.0F,
                "a full 5x5 with Radius I (25 blocks, each as long as with an iron pickaxe)");

        fillSquare(helper, top.below(), 2, Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, both);
        assertRatio(helper, progress(helper, player, top), ironPickaxe, com.simplebuilding.version.McVersion.PIECEWISE_HAMMER_TIME ? 36.6F : 50.0F,
                "two full 5x5 layers with Radius I and Break Through I (50 blocks, no cap)");

        helper.succeed();
    }

    /**
     * The pickaxe stays the main tool (owner, 2026-09-28). A lone block takes
     * {@code SledgehammerUtils#SINGLE_BLOCK_SLOWDOWN} times as long as with the pickaxe of the same
     * material; the area takes, per block, as long as the pickaxe one tier below: a diamond hammer's
     * full 3x3 as long as nine blocks with an iron pickaxe, an enderite hammer's as long as nine with
     * a netherite pickaxe - the owner's own example. Measured on the real {@code getDestroyProgress}
     * of the origin with a real player - the only place both the position and the player are known,
     * and the value client and server both tick with.
     *
     * <p>Only what really breaks counts: a 3x3 whose four dirt corners a plain hammer leaves standing
     * is five iron pickaxe blocks. Sneaking over the full 3x3 is a single block again.
     *
     * <p>What breaks this: the mixin on {@code BlockStateBase#getDestroyProgress} going missing,
     * counting the pattern instead of the blocks {@code shouldBreak} lets through, the tier ladder
     * pointing at the wrong pickaxe, or sneaking still counting the 3x3.
     */
    public static void sledgehammerAreaMinesEachBlockLikeThePickaxeOneTierBelow(GameTestHelper helper) {
        if (com.simplebuilding.version.McVersion.PIECEWISE_HAMMER_TIME) {
            piecewiseMiningTime(helper);
            return;
        }
        ServerPlayer player = inLevelPlayer(helper, ABOVE_CENTRE, 0.0F, 90.0F, true);
        ItemStack hammer = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);

        // --- lone block: a little slower than the diamond pickaxe ---
        helper.setBlock(CENTRE, Blocks.STONE);
        float diamondPickaxe = progressWith(helper, player, new ItemStack(Items.DIAMOND_PICKAXE), CENTRE);
        float ironPickaxe = progressWith(helper, player, new ItemStack(Items.IRON_PICKAXE), CENTRE);
        float lone = progressWith(helper, player, hammer, CENTRE);
        helper.assertTrue(lone > 0.0F, "a lone stone block makes no mining progress at all");
        assertRatio(helper, lone, diamondPickaxe, com.simplebuilding.util.SledgehammerUtils.SINGLE_BLOCK_SLOWDOWN,
                "a diamond hammer on a lone block against a diamond pickaxe");

        // --- full 3x3: nine iron pickaxe blocks ---
        fillFace(helper, CENTRE, Blocks.STONE);
        assertRatio(helper, progressWith(helper, player, hammer, CENTRE), ironPickaxe, 9.0F,
                "a diamond hammer's full 3x3 of stone against nine blocks with an iron pickaxe");

        // --- the owner's example: an enderite hammer's 3x3 is nine netherite pickaxe blocks ---
        float netheritePickaxe = progressWith(helper, player, new ItemStack(Items.NETHERITE_PICKAXE), CENTRE);
        assertRatio(helper, progressWith(helper, player, new ItemStack(ModItems.ENDERITE_SLEDGEHAMMER), CENTRE),
                netheritePickaxe, 9.0F, "an enderite hammer's full 3x3 against nine blocks with a netherite pickaxe");

        // --- only what really breaks counts: 1 + 4 stone, the 4 dirt corners stay ---
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                helper.setBlock(CENTRE.offset(dx, 0, dz), Blocks.DIRT);
            }
        }
        assertRatio(helper, progressWith(helper, player, hammer, CENTRE), ironPickaxe, 5.0F,
                "a 3x3 with four dirt corners a plain hammer leaves standing (5 blocks)");

        // --- sneaking over the full 3x3: one block again ---
        fillFace(helper, CENTRE, Blocks.STONE);
        player.setShiftKeyDown(true);
        float sneaking = progressWith(helper, player, hammer, CENTRE);
        player.setShiftKeyDown(false);
        assertRatio(helper, sneaking, lone, 1.0F, "sneaking over a full 3x3");

        helper.succeed();
    }

    /** Pin both thresholds using real block counting and the vanilla destroy-progress mixin. */
    private static void piecewiseMiningTime(GameTestHelper helper) {
        ServerPlayer player = inLevelPlayer(helper, ABOVE_CENTRE.add(0, 1, 0), 0.0F, 90.0F, true);
        BlockPos top = CENTRE.above();
        ItemStack hammer = hammerWith(helper, ModEnchantments.BREAK_THROUGH, 1);
        int[] counts = {1, 2, 9, 10, 18};
        float[] times = {1.5F, 2.3F, 7.9F, 8.6F, 14.2F};
        for (int i = 0; i < counts.length; i++) {
            fillFace(helper, top, Blocks.AIR);
            fillFace(helper, top.below(), Blocks.AIR);
            helper.setBlock(top, Blocks.STONE);
            player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
            List<BlockPos> positions = new java.util.ArrayList<>(SledgehammerItem.getBlocksToBeDestroyed(1, helper.absolutePos(top), player));
            positions.remove(helper.absolutePos(top));
            positions.addFirst(helper.absolutePos(top));
            for (int j = 0; j < counts[i]; j++) helper.getLevel().setBlockAndUpdate(positions.get(j), Blocks.STONE.defaultBlockState());
            helper.assertValueEqual(com.simplebuilding.util.SledgehammerUtils.countBlocksBroken(player, helper.absolutePos(top)), counts[i], "actual area count");
            float pickaxe = progressWith(helper, player, new ItemStack(Items.DIAMOND_PICKAXE), top);
            assertRatio(helper, progressWith(helper, player, hammer, top), pickaxe, times[i], "piecewise time for " + counts[i] + " blocks");
            helper.assertTrue(Math.abs(com.simplebuilding.util.SledgehammerUtils.swingTimeFactor(counts[i], true) - 2 * counts[i]) < 0.0001F, "octant must take 2x per block");
        }
        player.setShiftKeyDown(true);
        float pickaxe = progressWith(helper, player, new ItemStack(Items.DIAMOND_PICKAXE), top);
        assertRatio(helper, progressWith(helper, player, hammer, top), pickaxe, 1.5F, "sneaking time");
        helper.succeed();
    }

    /** The mining progress per tick on {@code relativePos} with {@code tool} in the main hand. */
    private static float progressWith(GameTestHelper helper, ServerPlayer player, ItemStack tool, BlockPos relativePos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return progress(helper, player, relativePos);
    }

    /**
     * Sneaking mines a single block: {@code getBlocksToBeDestroyed} hands back the origin and
     * nothing else, so the server hook, the client crack overlay and the highlight - which all
     * read that list - take one block. Before, sneaking only switched Radius and Break Through
     * off and the base 3x3 stayed.
     *
     * <p>Measured with a Radius I + Break Through I hammer over two full 5x5 layers, so a leftover
     * of any of the three widening steps (3x3, radius ring, depth layer) shows up. The positive
     * control swings the very same hammer without sneaking and clears all fifty blocks but the
     * origin, which vanilla takes.
     *
     * <p>What breaks this: the early return for sneaking going away (or checking the wrong flag),
     * which brings back the 3x3 under a sneaking player.
     */
    public static void sledgehammerSneakingBreaksOnlyTheTargetedBlock(GameTestHelper helper) {
        // One block up, so the Break Through layer sits at y = 1 inside the room, not in its floor.
        BlockPos top = CENTRE.above();
        ServerPlayer player = inLevelPlayer(helper, ABOVE_CENTRE.add(0.0, 1.0, 0.0), 0.0F, 90.0F, true);
        ItemStack hammer = hammerWith(helper, ModEnchantments.RADIUS, 1);
        hammer.enchant(enchantment(helper, ModEnchantments.BREAK_THROUGH), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        BlockPos origin = helper.absolutePos(top);

        fillSquare(helper, top, 2, Blocks.STONE);
        fillSquare(helper, top.below(), 2, Blocks.STONE);

        player.setShiftKeyDown(true);
        List<BlockPos> sneaking = SledgehammerItem.getBlocksToBeDestroyed(1, origin, player);
        helper.assertValueEqual(sneaking, List.of(origin), "positions a sneaking swing takes");
        swing(helper, player, top);
        player.setShiftKeyDown(false);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                helper.assertBlockPresent(Blocks.STONE, top.offset(dx, 0, dz));
                helper.assertBlockPresent(Blocks.STONE, top.offset(dx, -1, dz));
            }
        }

        // --- control: the same hammer without sneaking takes both layers ---
        helper.assertValueEqual(SledgehammerItem.getBlocksToBeDestroyed(1, origin, player).size(), 50,
                "positions a non-sneaking Radius I + Break Through I swing takes");
        swing(helper, player, top);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx != 0 || dz != 0) {
                    helper.assertBlockPresent(Blocks.AIR, top.offset(dx, 0, dz));
                }
                helper.assertBlockPresent(Blocks.AIR, top.offset(dx, -1, dz));
            }
        }

        helper.succeed();
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
        helper.assertValueEqual(efficient.getItem().getUseDuration(efficient, player), 6,
                "charge time of a diamond hammer with Efficiency V");

        // Deliberately above the vanilla cap: no legal level gets the raw value under the lower
        // clamp, so this is the only way to prove the clamp is doing anything.
        ItemStack overCharged = new ItemStack(ModItems.GOLD_SLEDGEHAMMER);
        overCharged.enchant(enchantment(helper, Enchantments.EFFICIENCY), 10);
        helper.assertValueEqual(overCharged.getItem().getUseDuration(overCharged, player), 4,
                "charge time of a gold hammer with Efficiency X (raw 3, held at the lower clamp)");

        helper.succeed();
    }

    /** All four aimed quarters, both halves and each step use actual collision shapes. */
    public static void sledgehammerCornersSubtractOnlyTheAimedQuarter(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.TRANSFORM_HINTS_AND_CORNERS) { helper.succeed(); return; }
        var level = helper.getLevel();
        var pos = helper.absolutePos(CENTRE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setShiftKeyDown(true);
        ItemStack stack = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        SledgehammerItem hammer = (SledgehammerItem) stack.getItem();
        for (Half half : Half.values()) for (int first = 0; first < 4; first++) {
            BlockState state = Blocks.STONE.defaultBlockState();
            int mask = 15;
            int[] order = {first, first ^ 1, first ^ 2, first ^ 3};
            for (int step = 0; step < 4; step++) {
                int corner = order[step];
                Vec3 aim = new Vec3((corner & 1) == 0 ? 0.25 : 0.75,
                        half == Half.BOTTOM ? 0.75 : 0.25, (corner & 2) == 0 ? 0.25 : 0.75);
                level.setBlockAndUpdate(pos, state);
                BlockState next = hammer.getTransformationState(state, pos,
                        half == Half.BOTTOM ? Direction.UP : Direction.DOWN, aim, player, stack);
                helper.assertTrue(next != null, "missing corner step " + step + " / " + first + " / " + half);
                var click = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(aim),
                        half == Half.BOTTOM ? Direction.UP : Direction.DOWN, pos, false);
                helper.assertValueEqual(hammer.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, click)),
                        InteractionResult.CONSUME, "corner hit did not use the existing charging path");
                helper.assertValueEqual(hammer.getUseDuration(stack, player),
                        (int) Math.ceil(SledgehammerItem.reshapeTicks(hammer.getMaterial().speed(), 0) / 1.5), "actual corner charge duration");
                hammer.releaseUsing(stack, level, player, 0);
                player.stopUsingItem();
                var encoded = BlockState.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, next).getOrThrow();
                helper.assertValueEqual(BlockState.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, encoded).getOrThrow(),
                        next, "carved geometry did not survive state serialization");
                mask &= ~(1 << corner);
                if (step < 3) {
                    helper.assertValueEqual(com.simplebuilding.util.HammerCorners.mask(next), mask, "wrong removed corner");
                    helper.assertValueEqual(next.getValue(StairBlock.HALF), half, "wrong retained half");
                    helper.assertValueEqual(com.simplebuilding.blueprint.BlueprintMaterials.survivalState(next), next,
                            "survival blueprint lost the carved geometry");
                    BlockState wet = state.getBlock() instanceof StairBlock ? state.setValue(StairBlock.WATERLOGGED, true) : null;
                    if (wet != null) helper.assertTrue(hammer.getTransformationState(wet, pos,
                            half == Half.BOTTOM ? Direction.UP : Direction.DOWN, aim, player, stack).getValue(StairBlock.WATERLOGGED),
                            "corner hit lost waterlogging");
                    int quarters = step == 0 ? 3 : step == 1 ? 2 : 1;
                    helper.assertValueEqual(Integer.bitCount(com.simplebuilding.util.HammerCorners.mask(next)), quarters, "wrong stair size");
                    level.setBlockAndUpdate(pos, next);
                    level.setBlockAndUpdate(pos.east(), Blocks.DIRT.defaultBlockState());
                    level.setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
                    helper.assertValueEqual(level.getBlockState(pos), next, "neighbor update restored a carved corner");
                    helper.assertTrue(hammer.getTransformationState(next, pos, Direction.UP, aim, player, stack) == null,
                            "already missing corner was cut twice");
                } else {
                    helper.assertTrue(next.is(Blocks.STONE_SLAB), "last corner did not become slab");
                    helper.assertValueEqual(next.getValue(net.minecraft.world.level.block.SlabBlock.TYPE),
                            half == Half.BOTTOM ? net.minecraft.world.level.block.state.properties.SlabType.BOTTOM
                                    : net.minecraft.world.level.block.state.properties.SlabType.TOP, "slab half changed");
                }
                state = next;
            }
        }
        for (int ticks = SledgehammerItem.RESHAPE_MIN_TICKS; ticks <= SledgehammerItem.RESHAPE_MAX_TICKS; ticks++)
            helper.assertValueEqual(com.simplebuilding.util.HammerCorners.ticks(ticks), (int) Math.ceil(ticks / 1.5), "corner speed rounding");
        helper.succeed();
    }

    private static Block transformBlock(String id) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(id));
    }

    /** A single read-only predicate handles both hands, tools, materials and negative targets. */
    public static void sledgehammerTransformHintsCoverBothHandsWithoutSideEffects(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.TRANSFORM_HINTS_AND_CORNERS) { helper.succeed(); return; }
        var level = helper.getLevel();
        var pos = helper.absolutePos(CENTRE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
        for (InteractionHand hand : InteractionHand.values()) {
            for (Object[] test : new Object[][]{
                    {Items.SHEARS, transformBlock("white_wool"), true}, {Items.SHEARS, Blocks.STONE, false},
                    {Items.HONEYCOMB, transformBlock("copper_block"), true}, {Items.HONEYCOMB, transformBlock("waxed_copper_block"), false},
                    {Items.DIAMOND_AXE, Blocks.OAK_LOG, true}, {Items.DIAMOND_AXE, Blocks.STRIPPED_OAK_LOG, false},
                    {Items.DIAMOND_AXE, transformBlock("oxidized_copper"), true}, {Items.DIAMOND_AXE, transformBlock("waxed_copper_block"), true},
                    {Items.DIAMOND_SHOVEL, Blocks.GRASS_BLOCK, true}, {Items.DIAMOND_HOE, Blocks.DIRT, true},
                    {Items.SHEARS, Blocks.PUMPKIN, true},
                    // Owner backlog Q4: the core's ore roll is a chance, not a transformation - no hint.
                    {ModItems.IRON_CORE, Blocks.STONE, false},
                    {ModItems.ROTATOR, Blocks.OAK_STAIRS, true},
                    {Items.ECHO_SHARD, Blocks.STONE, false}, {Items.GLOW_INK_SAC, Blocks.STONE, false},
                    {ModItems.DIAMOND_SLEDGEHAMMER, Blocks.STONE, true}, {ModItems.STONE_CHISEL, Blocks.STONE, true}}) {
                ItemStack stack = new ItemStack((Item) test[0]);
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                player.setItemInHand(hand, stack);
                BlockState state = ((Block) test[1]).defaultBlockState();
                level.setBlockAndUpdate(pos, state);
                helper.assertValueEqual(com.simplebuilding.util.TransformTargets.canTransformTarget(level, hit, player, hand),
                        (Boolean) test[2], "hint for " + test[0] + " on " + test[1] + " / " + hand);
                helper.assertValueEqual(level.getBlockState(pos), state, "hint modified world");
                helper.assertValueEqual(stack.getCount(), 1, "hint consumed item");
                helper.assertValueEqual(stack.getDamageValue(), 0, "hint damaged tool");
            }
        }
        for (InteractionHand hand : InteractionHand.values()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            player.setItemInHand(hand, new ItemStack(Items.SHEARS));
            level.setBlockAndUpdate(pos, Blocks.PUMPKIN.defaultBlockState());
            var pumpkinSide = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0, -0.5), Direction.NORTH, pos, false);
            helper.assertTrue(com.simplebuilding.util.TransformTargets.canTransformTarget(level, pumpkinSide, player, hand), "shears did not hint at carveable pumpkin face");
            level.setBlockAndUpdate(pos, Blocks.OAK_SIGN.defaultBlockState());
            var sign = (net.minecraft.world.level.block.entity.SignBlockEntity) level.getBlockEntity(pos);
            com.simplebuilding.version.McVersion.setSignTextFacingPlayer(sign, player, net.minecraft.network.chat.Component.literal("Test"), false);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            player.setItemInHand(hand, new ItemStack(Items.GLOW_INK_SAC));
            // An empty main hand opens the sign editor first, so an applicator in the off hand never gets the click.
            boolean applies = hand == InteractionHand.MAIN_HAND;
            helper.assertValueEqual(com.simplebuilding.util.TransformTargets.canTransformTarget(level, hit, player, hand), applies, "glow ink alone on sign / " + hand);
            com.simplebuilding.version.McVersion.setSignTextFacingPlayer(sign, player, net.minecraft.network.chat.Component.literal("Test"), true);
            helper.assertFalse(com.simplebuilding.util.TransformTargets.canTransformTarget(level, hit, player, hand), "already glowing sign hinted");
            player.setItemInHand(hand, new ItemStack(Items.INK_SAC));
            helper.assertValueEqual(com.simplebuilding.util.TransformTargets.canTransformTarget(level, hit, player, hand), applies, "ink alone on glowing sign / " + hand);
            player.setItemInHand(hand, new ItemStack(Items.HONEYCOMB));
            helper.assertValueEqual(com.simplebuilding.util.TransformTargets.canTransformTarget(level, hit, player, hand), applies, "wax alone on sign / " + hand);
            sign.setWaxed(true);
            helper.assertFalse(com.simplebuilding.util.TransformTargets.canTransformTarget(level, hit, player, hand), "waxed sign hinted");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.NETHERITE_NUGGET));
        level.setBlockAndUpdate(pos, com.simplebuilding.blocks.ModBlocks.REINFORCED_FURNACE.defaultBlockState());
        for (InteractionHand hand : InteractionHand.values()) helper.assertTrue(
                com.simplebuilding.util.TransformTargets.canTransformTarget(level, hit, player, hand), "upgrade material/tool hint missing");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertFalse(com.simplebuilding.util.TransformTargets.canTransformTarget(level, hit, player, InteractionHand.OFF_HAND),
                "nugget advertised a hammer recipe without a hammer");
        helper.succeed();
    }

    private static void holding(ServerPlayer player, ItemStack main, ItemStack off) {
        player.setItemInHand(InteractionHand.MAIN_HAND, main);
        player.setItemInHand(InteractionHand.OFF_HAND, off);
    }

    private static boolean fullHint(GameTestHelper helper, BlockHitResult hit, Player player, InteractionHand hand) {
        return com.simplebuilding.util.TransformTargets.canTransformTarget(helper.getLevel(), hit, player, hand);
    }

    private static boolean halfHint(GameTestHelper helper, BlockHitResult hit, Player player, InteractionHand hand) {
        return com.simplebuilding.util.TransformTargets.partialTransformTarget(helper.getLevel(), hit, player, hand);
    }

    /**
     * The half tilt (owner 2026-10-01) asks the upgrade's own conditions, minus the one counterpart
     * that is missing: a hammer strong enough and not cooling down without the stage's material, or
     * exactly the stage's material without a fitting hammer. A too weak hammer, the material of
     * another stage, a cooldown and adventure mode show nothing; hammer and material together are
     * the full tilt in both hands.
     */
    public static void transformHintPartialFollowsTheUpgradeRules(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.TRANSFORM_HINTS_AND_CORNERS) { helper.succeed(); return; }
        var level = helper.getLevel();
        var pos = helper.absolutePos(CENTRE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
        level.setBlockAndUpdate(pos, com.simplebuilding.blocks.ModBlocks.REINFORCED_FURNACE.defaultBlockState());
        InteractionHand main = InteractionHand.MAIN_HAND;
        InteractionHand off = InteractionHand.OFF_HAND;

        holding(player, new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER), ItemStack.EMPTY);
        helper.assertTrue(halfHint(helper, hit, player, main) && !fullHint(helper, hit, player, main), "hammer without nugget: half tilt only");
        holding(player, new ItemStack(ModItems.STONE_SLEDGEHAMMER), ItemStack.EMPTY);
        helper.assertFalse(halfHint(helper, hit, player, main), "a hammer too weak for the stage advertised it");
        holding(player, ItemStack.EMPTY, new ItemStack(ModItems.NETHERITE_NUGGET));
        helper.assertTrue(halfHint(helper, hit, player, off) && !fullHint(helper, hit, player, off), "nugget without hammer: half tilt only");
        holding(player, ItemStack.EMPTY, new ItemStack(ModItems.ENDERITE_NUGGET));
        helper.assertFalse(halfHint(helper, hit, player, off), "the material of another stage advertised this one");
        holding(player, new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER), new ItemStack(ModItems.NETHERITE_NUGGET));
        for (InteractionHand hand : InteractionHand.values()) {
            helper.assertTrue(fullHint(helper, hit, player, hand) && !halfHint(helper, hit, player, hand), "complete pair is no full tilt / " + hand);
        }
        ItemStack cooling = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);
        holding(player, cooling, ItemStack.EMPTY);
        player.getCooldowns().addCooldown(cooling, 20);
        helper.assertFalse(halfHint(helper, hit, player, main), "a cooling hammer advertised the upgrade");
        holding(player, new ItemStack(ModItems.NETHERITE_SLEDGEHAMMER), ItemStack.EMPTY);
        player.setGameMode(GameType.ADVENTURE);
        helper.assertFalse(halfHint(helper, hit, player, main), "adventure mode advertised the upgrade");
        player.setGameMode(GameType.SURVIVAL);

        // An undamaged Netherite Piston: its next stage takes the Enderite Nugget, so the Netherite one shows nothing.
        level.setBlockAndUpdate(pos, com.simplebuilding.blocks.ModBlocks.NETHERITE_PISTON.defaultBlockState());
        holding(player, ItemStack.EMPTY, new ItemStack(ModItems.NETHERITE_NUGGET));
        helper.assertFalse(halfHint(helper, hit, player, off) || fullHint(helper, hit, player, off),
                "a Netherite Nugget advertised an upgrade of the Netherite Piston");
        helper.succeed();
    }

    /**
     * A damaged breaker piston and the nugget of its tier: the nugget tilts fully (in either hand,
     * also behind a hammer that does nothing there), the real click repairs exactly then, and a
     * wrong nugget, an undamaged piston, a main hand that places a block or adventure mode show nothing.
     */
    public static void transformHintShowsTheBreakerPistonRepair(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.TRANSFORM_HINTS_AND_CORNERS) { helper.succeed(); return; }
        var level = helper.getLevel();
        var pos = helper.absolutePos(CENTRE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
        BlockState netherite = com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock.withDamage(
                com.simplebuilding.blocks.ModBlocks.NETHERITE_PISTON.defaultBlockState(), 10);
        BlockState enderite = com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock.withDamage(
                com.simplebuilding.blocks.ModBlocks.ENDERITE_PISTON.defaultBlockState(), 10);
        for (InteractionHand hand : InteractionHand.values()) {
            level.setBlockAndUpdate(pos, netherite);
            holding(player, ItemStack.EMPTY, ItemStack.EMPTY);
            player.setItemInHand(hand, new ItemStack(ModItems.NETHERITE_NUGGET));
            helper.assertTrue(fullHint(helper, hit, player, hand) && !halfHint(helper, hit, player, hand), "repair hint missing / " + hand);
            player.setItemInHand(hand, new ItemStack(ModItems.ENDERITE_NUGGET));
            helper.assertFalse(fullHint(helper, hit, player, hand), "Enderite Nugget hinted on a Netherite Piston / " + hand);
            level.setBlockAndUpdate(pos, enderite);
            helper.assertTrue(fullHint(helper, hit, player, hand), "Enderite repair hint missing / " + hand);
            player.setItemInHand(hand, new ItemStack(ModItems.NETHERITE_NUGGET));
            helper.assertFalse(fullHint(helper, hit, player, hand), "Netherite Nugget hinted on an Enderite Piston / " + hand);
            level.setBlockAndUpdate(pos, com.simplebuilding.blocks.ModBlocks.NETHERITE_PISTON.defaultBlockState());
            helper.assertFalse(fullHint(helper, hit, player, hand), "an undamaged piston hinted a repair / " + hand);
        }
        level.setBlockAndUpdate(pos, netherite);
        holding(player, new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER), new ItemStack(ModItems.NETHERITE_NUGGET));
        helper.assertTrue(fullHint(helper, hit, player, InteractionHand.OFF_HAND), "a hammer that does nothing here hid the off-hand repair");
        holding(player, new ItemStack(Items.DIRT), new ItemStack(ModItems.NETHERITE_NUGGET));
        helper.assertFalse(fullHint(helper, hit, player, InteractionHand.OFF_HAND), "the off hand hinted although the main hand places a block");

        // The real click agrees: adventure mode refuses and keeps the damage, survival repairs and pays.
        ItemStack nugget = new ItemStack(ModItems.NETHERITE_NUGGET);
        holding(player, nugget, ItemStack.EMPTY);
        player.setGameMode(GameType.ADVENTURE);
        helper.assertFalse(fullHint(helper, hit, player, InteractionHand.MAIN_HAND), "adventure mode hinted a repair");
        level.getBlockState(pos).useItemOn(nugget, level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertValueEqual(com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock.damageOf(level.getBlockState(pos)), 10,
                "adventure mode repaired the piston");
        player.setGameMode(GameType.SURVIVAL);
        helper.assertTrue(level.getBlockState(pos).useItemOn(nugget, level, player, InteractionHand.MAIN_HAND, hit).consumesAction(),
                "the hinted repair did not happen");
        helper.assertValueEqual(com.simplebuilding.blocks.custom.NetheriteBreakerPistonBlock.damageOf(level.getBlockState(pos)), 0, "repair left damage");
        helper.assertValueEqual(nugget.getCount(), 0, "repair did not use the nugget");
        helper.assertFalse(fullHint(helper, hit, player, InteractionHand.MAIN_HAND), "repaired piston still hinted");
        helper.succeed();
    }

    /**
     * Block interactions that transform: a dyed octant on a water cauldron (not a plain octant, not
     * an empty cauldron), honeycomb and axe on the copper pressure plate exactly where
     * {@code CopperPressurePlateBlock#transformWith} changes it - and the real click waxes.
     */
    public static void transformHintCoversCauldronWashAndCopperPlates(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.TRANSFORM_HINTS_AND_CORNERS) { helper.succeed(); return; }
        var level = helper.getLevel();
        var pos = helper.absolutePos(CENTRE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
        Item dyed = ModItems.COLORED_OCTANT_ITEMS.get(net.minecraft.world.item.DyeColor.RED);
        for (InteractionHand hand : InteractionHand.values()) {
            holding(player, ItemStack.EMPTY, ItemStack.EMPTY);
            level.setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
            player.setItemInHand(hand, new ItemStack(dyed));
            helper.assertTrue(fullHint(helper, hit, player, hand), "dyed octant on a water cauldron / " + hand);
            player.setItemInHand(hand, new ItemStack(ModItems.OCTANT));
            helper.assertFalse(fullHint(helper, hit, player, hand), "plain octant hinted a wash / " + hand);
            level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
            player.setItemInHand(hand, new ItemStack(dyed));
            helper.assertFalse(fullHint(helper, hit, player, hand), "empty cauldron hinted a wash / " + hand);

            for (Object[] test : new Object[][]{
                    {com.simplebuilding.tweaks.block.TweaksBlocks.COPPER_PRESSURE_PLATE, Items.HONEYCOMB, true},
                    {com.simplebuilding.tweaks.block.TweaksBlocks.COPPER_PRESSURE_PLATE, Items.IRON_AXE, false},
                    {com.simplebuilding.tweaks.block.TweaksBlocks.EXPOSED_COPPER_PRESSURE_PLATE, Items.IRON_AXE, true},
                    {com.simplebuilding.tweaks.block.TweaksBlocks.WAXED_COPPER_PRESSURE_PLATE, Items.HONEYCOMB, false},
                    {com.simplebuilding.tweaks.block.TweaksBlocks.WAXED_COPPER_PRESSURE_PLATE, Items.IRON_AXE, true},
                    {com.simplebuilding.tweaks.block.TweaksBlocks.COPPER_PRESSURE_PLATE, Items.STICK, false}}) {
                level.setBlockAndUpdate(pos, ((Block) test[0]).defaultBlockState());
                player.setItemInHand(hand, new ItemStack((Item) test[1]));
                helper.assertValueEqual(fullHint(helper, hit, player, hand), (Boolean) test[2], test[1] + " on " + test[0] + " / " + hand);
            }
        }
        level.setBlockAndUpdate(pos, com.simplebuilding.tweaks.block.TweaksBlocks.COPPER_PRESSURE_PLATE.defaultBlockState());
        ItemStack honeycomb = new ItemStack(Items.HONEYCOMB, 2);
        holding(player, honeycomb, ItemStack.EMPTY);
        helper.assertTrue(level.getBlockState(pos).useItemOn(honeycomb, level, player, InteractionHand.MAIN_HAND, hit).consumesAction()
                && level.getBlockState(pos).is(com.simplebuilding.tweaks.block.TweaksBlocks.WAXED_COPPER_PRESSURE_PLATE), "the hinted waxing did not happen");
        helper.assertFalse(fullHint(helper, hit, player, InteractionHand.MAIN_HAND), "waxed plate still hinted honeycomb");
        helper.succeed();
    }

    /**
     * Vanilla's click order: a block's own right-click (a chest's menu, a wooden door) comes before
     * the rotator - no tilt there unless sneaking, while an iron door turns. The off hand only tilts
     * when the main hand would not act; the full server click path agrees for the chest and the log.
     */
    public static void transformHintSkipsClicksTheBlockOrTheMainHandTakes(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.TRANSFORM_HINTS_AND_CORNERS) { helper.succeed(); return; }
        var level = helper.getLevel();
        var pos = helper.absolutePos(CENTRE);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
        InteractionHand main = InteractionHand.MAIN_HAND;
        InteractionHand off = InteractionHand.OFF_HAND;

        level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        holding(player, new ItemStack(ModItems.ROTATOR), ItemStack.EMPTY);
        helper.assertFalse(fullHint(helper, hit, player, main), "rotator hinted on a chest that opens instead");
        player.setShiftKeyDown(true);
        helper.assertTrue(fullHint(helper, hit, player, main), "sneaking rotator did not hint on a chest");
        player.setShiftKeyDown(false);
        BlockState chest = level.getBlockState(pos);
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), main, hit);
        helper.assertValueEqual(level.getBlockState(pos), chest, "the real click turned the chest it should have opened");
        player.closeContainer();

        level.setBlockAndUpdate(pos, Blocks.OAK_DOOR.defaultBlockState());
        helper.assertFalse(fullHint(helper, hit, player, main), "rotator hinted on a wooden door that opens instead");
        level.setBlockAndUpdate(pos, Blocks.IRON_DOOR.defaultBlockState());
        helper.assertTrue(fullHint(helper, hit, player, main), "rotator did not hint on an iron door");

        level.setBlockAndUpdate(pos, Blocks.OAK_LOG.defaultBlockState());
        holding(player, ItemStack.EMPTY, new ItemStack(ModItems.ROTATOR));
        helper.assertTrue(fullHint(helper, hit, player, off), "off-hand rotator behind an empty main hand");
        holding(player, new ItemStack(Items.DIRT), new ItemStack(ModItems.ROTATOR));
        helper.assertFalse(fullHint(helper, hit, player, off), "off hand hinted although the main hand places a block");
        holding(player, new ItemStack(ModItems.ROTATOR), new ItemStack(ModItems.ROTATOR));
        helper.assertTrue(fullHint(helper, hit, player, main), "main-hand rotator on a log");
        helper.assertFalse(fullHint(helper, hit, player, off), "off hand hinted although the main hand turns the log");
        BlockState log = level.getBlockState(pos);
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), main, hit);
        helper.assertTrue(level.getBlockState(pos) != log, "the hinted turn did not happen");
        helper.succeed();
    }

    // ---- owner decisions 2026-09-28 (begin)

    /**
     * With an Octant holding a full selection in the offhand, the hammer breaks the whole selection
     * (owner, 2026-09-28): a swing on a block inside it takes every block of the figure the hammer may
     * mine, costs the durability of mining each of them, and the mining takes, per block, twice as
     * long as the area action - here eighteen blocks at twice the iron pickaxe time each for a
     * diamond hammer. Sneaking is a single block again, and a block outside the figure, or a
     * selection larger than {@code SledgehammerUtils#OCTANT_MAX_EDGE}, gets the normal 3x3.
     *
     * <p>What breaks it: the octant branch in {@code getBlocksToBeDestroyed} going missing, the
     * doubled time per block, or blocks of the selection being broken without paying for them.
     */
    public static void sledgehammerBreaksTheOctantSelectionAtTwiceTheAreaTimePerBlock(GameTestHelper helper) {
        ServerPlayer player = inLevelPlayer(helper, ABOVE_CENTRE, 0.0F, 90.0F, false);
        ItemStack hammer = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);
        BlockPos from = CENTRE.offset(-1, 0, -1);
        BlockPos to = CENTRE.offset(1, 1, 1);
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            helper.setBlock(pos, Blocks.STONE);
        }
        BlockPos origin = helper.absolutePos(CENTRE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(com.simplebuilding.version.McVersion.PIECEWISE_HAMMER_TIME ? Items.DIAMOND_PICKAXE : Items.IRON_PICKAXE));
        float ironPickaxe = progress(helper, player, CENTRE);

        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        player.setItemInHand(InteractionHand.OFF_HAND, octantSelecting(helper.absolutePos(from), helper.absolutePos(to)));
        helper.assertValueEqual(SledgehammerItem.getBlocksToBeDestroyed(1, origin, player).size(), 18,
                "positions a swing inside a 3x2x3 octant selection takes");
        assertRatio(helper, progress(helper, player, CENTRE), ironPickaxe,
                18 * com.simplebuilding.util.SledgehammerUtils.OCTANT_TIME_FACTOR,
                "a diamond hammer breaking an 18 block octant selection against 18 iron pickaxe blocks, doubled");

        // --- sneaking: one block ---
        player.setShiftKeyDown(true);
        helper.assertValueEqual(SledgehammerItem.getBlocksToBeDestroyed(1, origin, player).size(), 1,
                "positions a sneaking swing inside the selection takes");
        player.setShiftKeyDown(false);

        // --- a selection longer than the limit: back to the normal face ---
        player.setItemInHand(InteractionHand.OFF_HAND, octantSelecting(helper.absolutePos(from),
                helper.absolutePos(from).offset(com.simplebuilding.util.SledgehammerUtils.OCTANT_MAX_EDGE, 1, 0)));
        helper.assertValueEqual(SledgehammerItem.getBlocksToBeDestroyed(1, origin, player).size(), 9,
                "positions a swing takes with a selection longer than the limit");

        // --- the swing breaks the selection and pays for every block ---
        player.setItemInHand(InteractionHand.OFF_HAND, octantSelecting(helper.absolutePos(from), helper.absolutePos(to)));
        swing(helper, player, CENTRE);
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            if (!pos.equals(CENTRE)) {
                helper.assertBlockPresent(Blocks.AIR, pos);
            }
        }
        helper.assertValueEqual(hammer.getDamageValue(), 17 * SledgehammerItem.WEAR_PER_BLOCK,
                "durability the octant swing cost for the 17 blocks beside the origin");

        helper.killAllEntitiesOfClass(ItemEntity.class);
        helper.succeed();
    }

    /** An Octant whose selection spans the two absolute corners (a cuboid). */
    private static ItemStack octantSelecting(BlockPos cornerA, BlockPos cornerB) {
        ItemStack octant = new ItemStack(ModItems.OCTANT);
        net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
        nbt.putIntArray("Pos1", new int[]{cornerA.getX(), cornerA.getY(), cornerA.getZ()});
        nbt.putIntArray("Pos2", new int[]{cornerB.getX(), cornerB.getY(), cornerB.getZ()});
        octant.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(nbt));
        return octant;
    }

    // ---- owner decisions 2026-09-28 (end)

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
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
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

    /** The server-side mining progress per tick on {@code relativePos}, as vanilla ticks it. */
    private static float progress(GameTestHelper helper, ServerPlayer player, BlockPos relativePos) {
        BlockPos pos = helper.absolutePos(relativePos);
        return helper.getLevel().getBlockState(pos).getDestroyProgress(player, helper.getLevel(), pos);
    }

    /** Asserts {@code slow * divisor == fast} within one percent. */
    private static void assertRatio(GameTestHelper helper, float slow, float fast, float divisor, String what) {
        float expected = fast / divisor;
        helper.assertTrue(Math.abs(slow - expected) <= expected * 0.01F,
                "mining progress for " + what + ": expected " + expected + " (1/" + divisor
                        + " of " + fast + ") but got " + slow);
    }

    /** The horizontal square of the given half width around {@code centre}. */
    private static void fillSquare(GameTestHelper helper, BlockPos centre, int halfWidth, Block block) {
        for (int dx = -halfWidth; dx <= halfWidth; dx++) {
            for (int dz = -halfWidth; dz <= halfWidth; dz++) {
                helper.setBlock(centre.offset(dx, 0, dz), block);
            }
        }
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
        helper.assertValueEqual(cost, SledgehammerItem.WEAR_PER_BLOCK,
                "durability one block of " + block + " cost the hammer - it wears twice as fast as a pickaxe");
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
        helper.assertValueEqual(modShare, NEIGHBOURS * expectedPerBlock,
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
        helper.assertValueEqual(actual.size(), expected.size(),
                "number of positions a swing takes while " + situation + "; a duplicate would be mined twice");
        helper.assertValueEqual(new HashSet<>(actual), expected,
                "the face a swing takes while " + situation + " (depth " + depth + " towards " + into + ")");
    }

    /**
     * Audit 2026-09-26, P2 #7: a charged right click finishes only on the block it was started on.
     * {@code finishUsingItem} used to re-aim at whatever the player looked at when the charge ran
     * out: start on a stone of your own, turn to a protected diamond block, and it was crushed into
     * 81 pebbles. Now the hammer remembers the block at {@code useOn}; turned away, or with no
     * charge behind the finish at all, nothing happens. The last case is the control: the charge's
     * own diamond block, still aimed at, is crushed as before.
     *
     * <p><strong>What breaks this test:</strong> finishing on the re-picked block instead of the
     * remembered one, or finishing without a remembered block.
     */
    public static void chargedHammerOnlyFinishesOnTheBlockItStartedOn(GameTestHelper helper) {
        ServerPlayer player = inLevelPlayer(helper, ABOVE_CENTRE, 0.0F, 90.0F, false);
        helper.setBlock(CENTRE, Blocks.STONE);
        helper.setBlock(PROBE, Blocks.DIAMOND_BLOCK);
        ItemStack hammer = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);
        net.minecraft.world.phys.AABB around = new net.minecraft.world.phys.AABB(helper.absolutePos(PROBE)).inflate(2.0);

        // --- charged on the stone, let go while looking at the diamond block ---
        InteractionResult started = useOnTop(helper, player, hammer, CENTRE);
        helper.assertTrue(started == InteractionResult.CONSUME, "the hammer did not charge on stone, got " + started);
        Vec3 abovePROBE = helper.absoluteVec(new Vec3(PROBE.getX() + 0.5, 3.0, PROBE.getZ() + 0.5));
        player.snapTo(abovePROBE.x, abovePROBE.y, abovePROBE.z, 0.0F, 90.0F);
        hammer.getItem().finishUsingItem(hammer, helper.getLevel(), player);
        player.stopUsingItem();
        helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, PROBE);
        helper.assertBlockPresent(Blocks.STONE, CENTRE);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).isEmpty(),
                "a charge started on stone crushed the diamond block the player turned to");

        // --- a finish with no charge behind it does nothing either ---
        hammer.getItem().finishUsingItem(hammer, helper.getLevel(), player);
        helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, PROBE);
        helper.assertValueEqual(hammer.getDamageValue(), 0, "a finish that did nothing still cost durability");

        // --- the diamond block itself breaks on eight strikes, not on a charge (owner 2026-10-02) ---
        InteractionResult onDiamond = useOnTop(helper, player, hammer, PROBE);
        helper.assertTrue(onDiamond == InteractionResult.SUCCESS, "the hammer did not strike the diamond block, got " + onDiamond);
        helper.assertFalse(player.isUsingItem(), "a diamond block strike started a charge");
        long now = helper.getLevel().getGameTime();
        for (int strike = 2; strike <= SledgehammerItem.DIAMOND_BLOCK_STRIKES; strike++) {
            helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, PROBE);
            now += SledgehammerItem.DIAMOND_STRIKE_MIN_INTERVAL + 1;
            SledgehammerItem.strikeDiamondBlock(helper.getLevel(), helper.absolutePos(PROBE), player, hammer, now);
        }
        helper.assertBlockPresent(Blocks.AIR, PROBE);
        int pebbles = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, around)) {
            if (entity.getItem().is(ModItems.DIAMOND_PEBBLE)) {
                pebbles += entity.getItem().getCount();
            }
            entity.discard();
        }
        helper.assertValueEqual(pebbles, 81, "the diamond block the charge started on did not give its 81 pebbles");
        helper.succeed();
    }

    /**
     * Owner 2026-09-29: a diamond block is only crushed by sledgehammers from the iron tier up -
     * iron, gold, diamond, netherite and enderite (gold sits above iron in the mod's ages). Stone
     * and copper hammers bounce off: the click is refused ({@code FAIL}, no wind-up), a finish
     * behind it crushes nothing and the block stays. The iron hammer, the weakest allowed one, crushes
     * the block into its 81 pebbles with eight strikes (owner 2026-10-02). JEI and the wiki list exactly the allowed
     * hammers ({@code InWorldTransformations#diamondCrush}).
     *
     * <p><strong>What breaks this test:</strong> dropping the tier gate in {@code useOn} or in
     * {@code crushDiamondBlock}, or a different set of allowed hammers.
     */
    public static void onlyIronOrBetterSledgehammersCrushDiamondBlocks(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        java.util.Map<Item, Boolean> expected = new java.util.LinkedHashMap<>();
        expected.put(ModItems.STONE_SLEDGEHAMMER, false);
        expected.put(ModItems.COPPER_SLEDGEHAMMER, false);
        expected.put(ModItems.IRON_SLEDGEHAMMER, true);
        expected.put(ModItems.GOLD_SLEDGEHAMMER, true);
        expected.put(ModItems.DIAMOND_SLEDGEHAMMER, true);
        expected.put(ModItems.NETHERITE_SLEDGEHAMMER, true);
        expected.put(ModItems.ENDERITE_SLEDGEHAMMER, true);
        expected.forEach((item, can) -> {
            if (SledgehammerItem.canCrushDiamondBlock(item) != can) {
                problems.add(item + (can ? " cannot" : " can") + " crush a diamond block");
            }
        });
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));

        ServerPlayer player = inLevelPlayer(helper, ABOVE_CENTRE, 0.0F, 90.0F, false);
        net.minecraft.world.phys.AABB around = new net.minecraft.world.phys.AABB(helper.absolutePos(CENTRE)).inflate(2.0);
        helper.setBlock(CENTRE, Blocks.DIAMOND_BLOCK);
        for (Item weak : List.of(ModItems.STONE_SLEDGEHAMMER, ModItems.COPPER_SLEDGEHAMMER)) {
            ItemStack hammer = new ItemStack(weak);
            InteractionResult result = useOnTop(helper, player, hammer, CENTRE);
            helper.assertTrue(result == InteractionResult.FAIL, weak + " on a diamond block answered " + result + " instead of FAIL");
            helper.assertFalse(player.isUsingItem(), weak + " winds up on a diamond block");
            hammer.getItem().finishUsingItem(hammer, helper.getLevel(), player);
            player.stopUsingItem();
            helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, CENTRE);
        }
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).isEmpty(),
                "a stone or copper hammer crushed the diamond block");

        // Owner 2026-10-01: single strikes instead of a held charge, eight since 2026-10-02. A repeat inside the
        // minimum interval (held right-click) does not count, the last counted strike crushes.
        ItemStack iron = new ItemStack(ModItems.IRON_SLEDGEHAMMER);
        InteractionResult first = useOnTop(helper, player, iron, CENTRE);
        helper.assertTrue(first == InteractionResult.SUCCESS, "the iron hammer did not strike the diamond block, got " + first);
        helper.assertFalse(player.isUsingItem(), "the iron hammer still winds up on a diamond block");
        useOnTop(helper, player, iron, CENTRE);
        helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, CENTRE);
        strikeLater(helper, player, iron, around, 2);
    }

    /** Strike {@code strike} of {@link SledgehammerItem#DIAMOND_BLOCK_STRIKES}, spaced past the repeat guard. */
    private static void strikeLater(GameTestHelper helper, ServerPlayer player, ItemStack iron, net.minecraft.world.phys.AABB around, int strike) {
        helper.runAfterDelay(SledgehammerItem.DIAMOND_STRIKE_MIN_INTERVAL + 1, () -> {
            useOnTop(helper, player, iron, CENTRE);
            if (strike < SledgehammerItem.DIAMOND_BLOCK_STRIKES) {
                helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, CENTRE);
                strikeLater(helper, player, iron, around, strike + 1);
                return;
            }
            helper.assertBlockPresent(Blocks.AIR, CENTRE);
            int pebbles = 0;
            for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, around)) {
                if (entity.getItem().is(ModItems.DIAMOND_PEBBLE)) {
                    pebbles += entity.getItem().getCount();
                }
                entity.discard();
            }
            helper.assertValueEqual(pebbles, 81, "pebbles from the diamond block crushed with iron hammer strikes");
            helper.succeed();
        });
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
        chargeAndFinish(hammer, helper.getLevel(), player);
    }

    private static void assertStair(GameTestHelper helper, BlockPos relativePos,
                                    Direction facing, Half half, String what) {
        BlockState state = helper.getBlockState(relativePos);
        helper.assertValueEqual(state.getValue(StairBlock.FACING), facing, "facing of " + what);
        helper.assertValueEqual(state.getValue(StairBlock.HALF), half, "half of " + what);
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
        helper.assertValueEqual(stack.getItem().getUseDuration(stack, player), expected,
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

    /**
     * Holds right click on the block the player looks at and lets the charge run out: {@code useOn}
     * on the picked block, then {@code finishUsingItem}. Since 2026-09-26 the hammer only finishes
     * on the block its charge was started on (audit P2 #7), so a bare finish does nothing.
     */
    private static void chargeAndFinish(ItemStack hammer, net.minecraft.world.level.Level level, ServerPlayer player) {
        net.minecraft.world.phys.HitResult hit = player.pick(5.0, 0.0F, false);
        if (hit instanceof net.minecraft.world.phys.BlockHitResult blockHit
                && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            hammer.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, blockHit));
        }
        hammer.getItem().finishUsingItem(hammer, level, player);
        player.stopUsingItem();
    }
}
