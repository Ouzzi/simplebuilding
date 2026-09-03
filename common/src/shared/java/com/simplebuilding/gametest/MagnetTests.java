package com.simplebuilding.gametest;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Everything the magnet ({@code items/custom/MagnetItem}) does on the server: when it runs at all,
 * how far it reaches, what force it puts on a loose item, which items its filter lets through, and
 * how that filter is cleared again.
 *
 * <p>Until now the magnet had exactly one test - {@link
 * ToolBehaviourTests#magnetPullsNearbyItemsAndIgnoresDistantOnes} - which holds the magnet in the
 * main hand, ticks the player for up to 200 ticks and checks that a nearby item ended up in the
 * inventory while a distant one did not. That covers the happy path and nothing else: every gate in
 * {@code inventoryTick} (off hand, sneaking, the carrier not being a player at all), every constant
 * of the pull curve, the whole filter and the whole {@code use} hook were free to change without a
 * single test going red.
 *
 * <h2>How these tests drive the magnet</h2>
 *
 * <p>All but one call {@code Item#inventoryTick} directly instead of pumping the player's
 * connection, the same way {@code ConsumptionAndDurabilityTests} drives the building wand. That is
 * both closer to what is under test and, more importantly, deterministic: the whole test body then
 * runs inside a single server tick, so no {@code ItemEntity} tick, no gravity, no drag and no
 * concurrently running gametest can touch the entities between the call and the assertion. A
 * measured velocity is therefore exactly the velocity the magnet wrote, with no tolerance budget
 * spent on physics.
 *
 * <p>The one exception is {@link #magnetInTheOffHandDragsLooseItemsIntoTheInventory}, which has to
 * take the real route: only vanilla can answer whether an off hand stack is ticked at all.
 *
 * <h2>Which half is vanilla's</h2>
 *
 * <p>{@code inventoryTick} is handed its {@link EquipmentSlot} by vanilla, from the only two places
 * that call it: {@code Inventory#tick} walks the 36 non equipment slots and passes {@code MAINHAND}
 * for the selected one and {@code null} for the rest, while {@code EntityEquipment#tick} walks the
 * equipment map and passes the real slot - which is the only reason an off hand magnet works.
 * Between them they always name the slot when the stack is in a hand: for a player the main hand
 * stack <em>is</em> the selected inventory slot, and the off hand lives in the equipment map.
 * {@code MagnetItem#isHeldInHand} is the mod's half of that contract, and it is what the slot cases
 * below pin down - including the one case no vanilla caller can produce, which {@link
 * #magnetOnlyRunsForPlayersHoldingItAndStopsWhileSneaking} marks as such.
 *
 * <h2>Not covered</h2>
 *
 * <ul>
 *   <li><b>Everything the player is told.</b> The cleared-filter overlay message
 *       ({@code sendOverlayMessage}), the UI click sound and {@code syncVelocityToNearbyPlayers}
 *       are packets; a mock player's connection swallows them, so there is nothing a server side
 *       test can observe. The velocity sync in particular is pure client smoothing - the
 *       authoritative motion is the one asserted in {@link
 *       #magnetPullFollowsTheAccelerationAndBrakingCurve}.</li>
 *   <li><b>{@code interactLivingEntity} returning {@code PASS}.</b> That is already
 *       {@code Item}'s own default, so the assertion would stay green with the override deleted -
 *       it cannot fail and would only pretend to cover the line.</li>
 *   <li><b>The {@code isRemoved()} guard.</b> {@code Entity#discard} takes the entity out of the
 *       level's entity sections immediately, so {@code getEntitiesOfClass} never hands a removed
 *       entity back in the first place; a test would assert about a state the loop cannot see.</li>
 *   <li><b>The {@code getItem().isEmpty()} guard.</b> An {@code ItemEntity} carrying an empty
 *       stack would be pushed around by the magnet without the guard and nothing else would
 *       happen - no exception, no visible effect (vanilla discards such an entity on its next own
 *       tick anyway). There is no failure to detect.</li>
 *   <li><b>Experience orbs staying put.</b> The loop only ever queries {@code ItemEntity}, so
 *       "orbs are not attracted" is the absence of a feature that was never written; a test for it
 *       would go red the day somebody deliberately adds orb attraction, not the day something
 *       breaks.</li>
 *   <li><b>{@code getMaxDamage() == 0}.</b> A registration property with no behaviour behind it -
 *       the magnet never calls {@code hurtAndBreak}, so nothing observable changes either way.</li>
 *   <li><b>The Range enchantment branch in {@code getCurrentRange}.</b> Range does not support the
 *       magnet, so neither an enchanting table nor an anvil can put it there. The branch is not
 *       dead code though: {@code getMagnetRangeLevel} reads the enchantments component, and
 *       {@code /give} with components, a datapack and {@code ItemStack#enchant} all write that
 *       component without consulting {@code isSupportedItem}. It is left uncovered because nothing
 *       a player can do in a normal world reaches it; the guard in {@link
 *       #magnetReachIsFourBlocksAndConstructorsTouchWidensIt} goes red the day Range starts
 *       supporting the magnet and the branch becomes ordinary live code.</li>
 * </ul>
 */
public final class MagnetTests {

    private MagnetTests() {
    }

    /** Tick budget for {@link #magnetInTheOffHandDragsLooseItemsIntoTheInventory}. */
    public static final int OFF_HAND_MAX_TICKS = 200;

    /**
     * The custom data key {@code MagnetItem} stores the filter under. Spelled out here rather than
     * read from the item, because the point of the filter tests is that this exact string is what
     * ends up on disk: renaming it would silently unfilter every magnet a player already owns.
     * {@code OreGenAndItemFrameTests} repeats it for the same reason.
     */
    private static final String FILTER_KEY = "MagnetFilter";

    /** Where every test parks its player, in room coordinates. */
    private static final Vec3 PLAYER_SPOT = new Vec3(1.5, 1.0, 1.5);

    /** Comfortably inside the unenchanted 4 block reach, far outside the vanilla pickup radius. */
    private static final Vec3 NEAR_SPOT = new Vec3(4.5, 1.0, 1.5);

    /** Second in-range spot on the other horizontal axis, for the two-item filter cases. */
    private static final Vec3 SECOND_NEAR_SPOT = new Vec3(1.5, 1.0, 4.5);

    /** Velocities below this count as "the magnet did not touch it". */
    private static final double AT_REST = 1.0E-8;

    /** Everything the magnet writes is exact arithmetic, so the tolerance only covers rounding. */
    private static final double EXACT = 1.0E-9;

    // =====================================================================================
    // (a) WHEN THE MAGNET RUNS AT ALL
    // =====================================================================================

    /**
     * {@code inventoryTick} guards itself three times before it looks for a single item: the
     * carrier has to be a {@code Player}, the stack has to be in one of the two hands, and the
     * player must not be sneaking. All three are driven here against their opposite, on one and the
     * same item three blocks away, so "it did not move" can only mean the gate held.
     *
     * <p>The slot cases mirror what vanilla actually passes (see the class javadoc):
     * {@code MAINHAND} and {@code null} come from {@code Inventory#tick}, {@code OFFHAND} and
     * {@code HEAD} from {@code EntityEquipment#tick}.
     *
     * <p>The sixth case - a {@code null} slot while the very same stack object is in the hand - is
     * characterization, not coverage, and is marked as such at the call site. No vanilla caller can
     * produce it: {@code Inventory#tick} passes {@code null} only for the 35 slots that are not the
     * selected one, and for a player the main hand stack is precisely the selected slot's while the
     * off hand sits in the equipment map, which {@code EntityEquipment#tick} always ticks with its
     * real slot. The identity fallback in {@code isHeldInHand} therefore never fires in game - and
     * the reverse holds too, since the fallback alone would carry the feature for both hands. What
     * the six cases together really pin is the contract as a whole: held runs, not held does not.
     *
     * <p>What breaks this test: dropping the {@code instanceof Player} check (the armour stand case
     * would then throw a {@code ClassCastException}), widening {@code isHeldInHand} to any slot
     * (the head and backpack cases would start pulling, which is what "a magnet works from the
     * hotbar without being held" looks like), narrowing it to the main hand (the off hand case goes
     * red), and removing or inverting the {@code isShiftKeyDown} guard. Deleting <em>one</em> of
     * the two halves of {@code isHeldInHand} breaks no feature: dropping the identity fallback
     * turns the sixth case red without changing anything a player can see, and dropping the slot
     * branch leaves every case in here green.
     */
    public static void magnetOnlyRunsForPlayersHoldingItAndStopsWhileSneaking(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper);
        ItemEntity target = helper.spawnItem(Items.DIAMOND, NEAR_SPOT);
        ItemStack magnet = new ItemStack(ModItems.MAGNET);

        // --- main hand: the case the old test already covered, kept as the positive control ---
        // It comes first on purpose: every "it did not move" below is only worth something once
        // this very entity has been seen moving.
        clearHands(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, magnet);
        helper.assertTrue(pulls(magnet, level, player, EquipmentSlot.MAINHAND, target),
                "a magnet in the main hand did not pull an item three blocks away");

        // --- the carrier is not a player: an armour stand holding a magnet magnetises nothing ---
        // EntityEquipment#tick runs for every LivingEntity, so this really is reachable in game.
        clearHands(player);
        LivingEntity armourStand = helper.spawn(EntityTypes.ARMOR_STAND, new BlockPos(6, 1, 6));
        armourStand.setItemSlot(EquipmentSlot.MAINHAND, magnet);
        helper.assertTrue(!pulls(magnet, level, armourStand, EquipmentSlot.MAINHAND, target),
                "an armour stand holding a magnet pulled a loose item; the magnet is no longer "
                        + "restricted to players");
        armourStand.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

        // --- off hand: only EntityEquipment#tick reaches this slot ---
        clearHands(player);
        player.setItemInHand(InteractionHand.OFF_HAND, magnet);
        helper.assertTrue(pulls(magnet, level, player, EquipmentSlot.OFFHAND, target),
                "a magnet in the off hand did not pull an item three blocks away");

        // --- an armour slot is not a hand ---
        clearHands(player);
        player.setItemSlot(EquipmentSlot.HEAD, magnet);
        helper.assertTrue(!pulls(magnet, level, player, EquipmentSlot.HEAD, target),
                "a magnet worn in the helmet slot pulled an item; isHeldInHand accepts slots that "
                        + "are not hands");
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);

        // --- a backpack slot: vanilla passes a null slot for every unselected inventory slot ---
        clearHands(player);
        player.getInventory().setItem(9, magnet);
        helper.assertTrue(!pulls(magnet, level, player, null, target),
                "a magnet lying in an unselected inventory slot pulled an item; it only works while "
                        + "it is actually held");
        player.getInventory().setItem(9, ItemStack.EMPTY);

        // --- null slot, but the same stack object is in the hand: the identity fallback ---
        // Characterization, not coverage: neither vanilla caller pairs a null slot with a hand
        // stack, so this branch cannot run in game. See the javadoc.
        clearHands(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, magnet);
        helper.assertTrue(pulls(magnet, level, player, null, target),
                "the isHeldInHand fallback no longer recognises the held stack when the slot "
                        + "argument is null");

        // --- sneaking pauses the magnet completely, and lets go again ---
        player.setShiftKeyDown(true);
        helper.assertTrue(!pulls(magnet, level, player, EquipmentSlot.MAINHAND, target),
                "the magnet kept pulling while the player was sneaking; there is no way left to "
                        + "put an item down in front of a held magnet");
        player.setShiftKeyDown(false);
        helper.assertTrue(pulls(magnet, level, player, EquipmentSlot.MAINHAND, target),
                "the magnet stayed off after sneaking ended");

        helper.succeed();
    }

    /**
     * The one test in this class that goes the whole way through vanilla: a magnet in the
     * <em>off hand</em> has to drag a loose item all the way into the inventory, with an unrelated
     * item in the main hand so nothing else can be responsible.
     *
     * <p>{@link #magnetOnlyRunsForPlayersHoldingItAndStopsWhileSneaking} proves that the mod
     * accepts {@code EquipmentSlot.OFFHAND}; it cannot prove that anything ever hands it that slot.
     * Only {@code EntityEquipment#tick} does, reached through {@code LivingEntity#aiStep}, and the
     * gametest server never pumps a mock player's connection - so, exactly like the main hand test
     * in {@code ToolBehaviourTests}, the connection is ticked from here.
     *
     * <p>What breaks this test: {@code isHeldInHand} no longer accepting {@code OFFHAND}, the
     * pickup delay no longer being reset (the item would hover at the player forever), or the pull
     * becoming too weak to cross three blocks inside the tick budget. It also goes red if a
     * Minecraft update stops ticking equipment stacks, which is the half this test exists to watch.
     */
    public static void magnetInTheOffHandDragsLooseItemsIntoTheInventory(GameTestHelper helper) {
        fillFloor(helper, Blocks.STONE);

        ServerPlayer player = mockPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.MAGNET));

        ItemEntity target = helper.spawnItem(Items.DIAMOND, NEAR_SPOT);
        // A freshly dropped item cannot be picked up for 40 ticks; the magnet is supposed to clear
        // that. Making it "never" turns the reset from a nicety into the only way this can succeed.
        target.setNeverPickUp();

        helper.assertTrue(!player.getInventory().contains(stack -> stack.is(Items.DIAMOND)),
                "test setup broken: the player already carries a diamond, so the assertion below "
                        + "would pass without the magnet doing anything");

        helper.succeedWhen(() -> {
            player.connection.tick();
            helper.assertTrue(player.getInventory().contains(stack -> stack.is(Items.DIAMOND)),
                    "the off hand magnet did not bring the diamond in; it is still at "
                            + target.position() + " with motion " + target.getDeltaMovement());
        });
    }

    // =====================================================================================
    // (b) THE FORCE ITSELF
    // =====================================================================================

    /**
     * The five numbers in {@code applyMagnetForce} plus the point it aims at, each isolated so that
     * one changed constant can only make one assertion fail.
     *
     * <ul>
     *   <li><b>0.10 pull.</b> An item at rest, in the air, more than a block away ends the tick
     *       with a speed of exactly 0.10 - the previous velocity is zero, so what is left is the
     *       pull alone.</li>
     *   <li><b>The aiming point.</b> The item lies at the player's feet height, so a pull aimed at
     *       the feet would have no vertical share at all and a pull aimed at the eyes a clearly
     *       larger one. The asserted band corresponds to an aiming point roughly 0.8 to 1.5 blocks
     *       above the feet, i.e. the chest; it is stated as a band on purpose, because the player
     *       eye height it is derived from belongs to vanilla.</li>
     *   <li><b>0.80 damping.</b> The item is given a velocity along the one axis on which it and
     *       the player share a coordinate, so the pull contributes exactly nothing there and what
     *       remains after the tick is the damped old velocity and nothing else.</li>
     *   <li><b>0.15 lift.</b> Measured as the difference between the grounded and the airborne
     *       result of the same setup, so the aiming geometry cancels out.</li>
     *   <li><b>0.2 braking.</b> Inside one block the magnet must stop feeding the item instead of
     *       accelerating it further; a moving item ends the tick at a fifth of its speed, with the
     *       direction untouched.</li>
     *   <li><b>The 1.0 braking threshold.</b> Two items straight below the aiming point, 1.07 and
     *       0.92 blocks away from it: the far one still has to be fed, the near one has to be
     *       braked already. That clamps {@code distanceSq > 1.0} to the interval (0.85, 1.14);
     *       cases 1 and 4 alone left everything between 0.0144 and 10.25 free.</li>
     * </ul>
     *
     * <p>The pickup delay reset rides along on the first case: the item is set to "never pick up"
     * (32767 ticks, what vanilla uses for items that must not be collected), and one magnet tick
     * has to clear it.
     *
     * <p>What breaks this test: any of 0.10, 0.80, 0.15, 0.2 changing, the aiming point moving to
     * the feet or the eyes, the ground lift being applied while airborne (or not at all), the
     * {@code distanceSq > 1.0} threshold leaving (0.85, 1.14), and dropping
     * {@code setPickUpDelay(0)}.
     */
    public static void magnetPullFollowsTheAccelerationAndBrakingCurve(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper);
        ItemStack magnet = new ItemStack(ModItems.MAGNET);
        player.setItemInHand(InteractionHand.MAIN_HAND, magnet);

        // --- 1. an item at rest in the air, three blocks away on the x axis ---
        ItemEntity far = helper.spawnItem(Items.DIAMOND, NEAR_SPOT);
        far.setDeltaMovement(Vec3.ZERO);
        far.setOnGround(false);
        far.setNeverPickUp();
        helper.assertTrue(far.hasPickUpDelay(),
                "test setup broken: setNeverPickUp left the item collectable, so the pickup delay "
                        + "assertion below would pass without the magnet clearing anything");

        tick(magnet, level, player, EquipmentSlot.MAINHAND);
        Vec3 airborne = far.getDeltaMovement();

        helper.assertTrue(Math.abs(airborne.length() - 0.10) < EXACT,
                "an item at rest has to leave a magnet tick with a speed of exactly 0.10, it has "
                        + airborne.length() + " (" + airborne + ")");
        helper.assertTrue(airborne.x < 0.0,
                "the item sits on the +x side of the player, so the pull has to point back along "
                        + "-x; it is " + airborne);
        helper.assertTrue(Math.abs(airborne.z) < EXACT,
                "the item shares the player's z, so the pull must not have a z component at all; "
                        + "it is " + airborne.z);
        helper.assertTrue(airborne.y > 0.026 && airborne.y < 0.045,
                "the magnet aims at a point about a block above the item, i.e. at the player's "
                        + "chest: from three blocks away that is a vertical share between 0.026 "
                        + "and 0.045, but it is " + airborne.y + ". Near 0 means the magnet now "
                        + "aims at the feet, above 0.047 means it aims at the eyes");
        helper.assertTrue(!far.hasPickUpDelay(),
                "the magnet did not clear the pickup delay; an item it drags in would sit inside "
                        + "the player uncollectable");

        // --- 2. the same item, now carrying speed along the axis the pull cannot touch ---
        far.setDeltaMovement(new Vec3(0.0, 0.0, 1.0));
        tick(magnet, level, player, EquipmentSlot.MAINHAND);
        Vec3 damped = far.getDeltaMovement();

        helper.assertTrue(Math.abs(damped.z - 0.80) < EXACT,
                "existing motion has to be damped to 0.80 of itself before the pull is added; a "
                        + "speed of 1.0 on the axis the pull does not act on came out as " + damped.z);
        helper.assertTrue(Math.abs(damped.x - airborne.x) < EXACT
                        && Math.abs(damped.y - airborne.y) < EXACT,
                "damping changed the pull itself: the same geometry produced " + airborne
                        + " from standstill but " + damped + " from motion");

        // --- 3. the same item again, this time resting on the ground ---
        far.setDeltaMovement(Vec3.ZERO);
        far.setOnGround(true);
        tick(magnet, level, player, EquipmentSlot.MAINHAND);
        Vec3 grounded = far.getDeltaMovement();

        helper.assertTrue(Math.abs((grounded.y - airborne.y) - 0.15) < EXACT,
                "an item lying on the ground has to get an extra 0.15 upwards so it can leave the "
                        + "floor; the difference to the airborne case is " + (grounded.y - airborne.y));
        helper.assertTrue(Math.abs(grounded.x - airborne.x) < EXACT
                        && Math.abs(grounded.z - airborne.z) < EXACT,
                "the ground lift has to be purely vertical, but the horizontal motion changed from "
                        + airborne + " to " + grounded);

        // --- 4. an item that has arrived: inside one block it is braked, not fed ---
        // Spawned only now so the ticks above cannot have touched it.
        ItemEntity arrived = helper.spawnItem(Items.DIAMOND, new Vec3(1.5, 2.0, 1.5));
        arrived.setDeltaMovement(new Vec3(0.5, 0.0, -0.5));
        arrived.setOnGround(false);

        tick(magnet, level, player, EquipmentSlot.MAINHAND);
        Vec3 braked = arrived.getDeltaMovement();

        helper.assertTrue(Math.abs(braked.x - 0.10) < EXACT
                        && Math.abs(braked.y) < EXACT
                        && Math.abs(braked.z + 0.10) < EXACT,
                "within one block the magnet has to brake the item to a fifth of its speed and add "
                        + "nothing: (0.5, 0, -0.5) should become (0.1, 0, -0.1) but became " + braked);

        // --- 5. where exactly that switch sits: both sides of the threshold, half a step apart ---
        // Both stand straight below the aiming point (1.5, 2.12, 1.5), so the distance to it is the
        // only thing that differs: 1.07 blocks (distanceSq 1.14, still fed) against 0.92 blocks
        // (0.85, braked). Spawned only now, for the same reason as case 4.
        ItemEntity beyondBraking = helper.spawnItem(Items.DIAMOND, new Vec3(1.5, 1.05, 1.5));
        beyondBraking.setDeltaMovement(Vec3.ZERO);
        beyondBraking.setOnGround(false);
        ItemEntity withinBraking = helper.spawnItem(Items.DIAMOND, new Vec3(1.5, 1.2, 1.5));
        withinBraking.setDeltaMovement(new Vec3(0.5, 0.0, 0.0));
        withinBraking.setOnGround(false);

        tick(magnet, level, player, EquipmentSlot.MAINHAND);
        Vec3 stillFed = beyondBraking.getDeltaMovement();
        Vec3 alreadyBraked = withinBraking.getDeltaMovement();

        helper.assertTrue(Math.abs(stillFed.y - 0.10) < EXACT
                        && Math.abs(stillFed.x) < EXACT
                        && Math.abs(stillFed.z) < EXACT,
                "an item 1.07 blocks below the aiming point still has to get the full 0.10 pull, "
                        + "so the braking threshold has to stay below a distanceSq of 1.14; the "
                        + "item left the tick with " + stillFed);
        helper.assertTrue(Math.abs(alreadyBraked.x - 0.10) < EXACT
                        && Math.abs(alreadyBraked.y) < EXACT
                        && Math.abs(alreadyBraked.z) < EXACT,
                "an item 0.92 blocks below the aiming point has to be braked and fed nothing, so "
                        + "the braking threshold has to stay above a distanceSq of 0.85; (0.5, 0, "
                        + "0) became " + alreadyBraked);

        helper.succeed();
    }

    // =====================================================================================
    // (c) REACH
    // =====================================================================================

    /**
     * The magnet reaches four blocks, and Constructor's Touch widens that.
     *
     * <p>The reach is a box, not a sphere: {@code getEntitiesOfClass} gets the player's bounding
     * box inflated by the range, and {@code AABB#intersects} is strict. With the player parked at
     * x 1.5 its box ends at x 1.8, and an item's own hull is 0.25 wide, so an unenchanted magnet
     * catches exactly those items whose hull starts before x 5.8. The two controls on that axis sit
     * 0.075 either side of that edge, which pins {@code BASE_RANGE} to the interval (3.925, 4.075]
     * - controls a full block apart would have let it wander anywhere between 3.6 and 4.5. The
     * diagonal item is the same 4.35 blocks out on x <em>and</em> z, i.e. 6.15 blocks away in a
     * straight line, and still has to come; that is the pair that says "box, not sphere".
     *
     * <p>Constructor's Touch is pinned from below by the item at the far wall: its hull starts at
     * x 7.775, so pulling it means {@code BOOSTED_RANGE > 5.975}. An upper bound cannot be had in
     * here - an item that a reach of 8 must miss would have to sit past x 9.925, outside the 8x8x8
     * room, and entities placed outside the room land in whatever gametest is running next door,
     * the failure mode CLAUDE.md warns about.
     *
     * <p>The boosted tick really does search eight blocks in every direction. The rooms of a batch
     * are five blocks apart along x ({@code StructureGridSpawner.SPACE_BETWEEN_COLUMNS}), so
     * inflating the player box by 8 reaches from x 1.2 down to x -6.8: into the last 1.8 blocks of
     * the room in the column before this one. What keeps that from corrupting the single tick tests
     * over there is not a tolerance - {@code AT_REST} lets nothing above a speed of 1e-4 pass,
     * while one magnet impulse is 0.1, a thousand times more - but atomicity: a test body runs from
     * start to finish inside one server tick, so no neighbour ever measures while this test has a
     * hand on its entities. What stays exposed are multi tick item tests in that column. The known
     * one is {@link ToolBehaviourTests#magnetPullsNearbyItemsAndIgnoresDistantOnes}, whose parked
     * gold ingot is allowed 0.5 blocks of drift, while a single 0.1 impulse on an item lying on
     * stone adds up to 0.1 / (1 - 0.98 * 0.6) = 0.24 blocks. A factor of two is the whole margin,
     * which is why the enchanted tick is fired exactly once.
     *
     * <p>What breaks this test: {@code BASE_RANGE} leaving (3.925, 4.075], {@code BOOSTED_RANGE}
     * dropping to 5.975 or below, the Constructor's Touch lookup breaking, and swapping the
     * inflated bounding box for a straight distance check (the diagonal item stops being pulled).
     * The guard at the top goes red if the magnet ever joins
     * {@code simplebuilding:chisel_and_mining_tools} - the Range branch in {@code getCurrentRange}
     * would become reachable in normal play and would need a case of its own.
     */
    public static void magnetReachIsFourBlocksAndConstructorsTouchWidensIt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper);

        // Guard, not coverage: while Range does not support the magnet, no enchanting table and no
        // anvil can put it there. The "+2 blocks per level" branch is still reachable for a command
        // or a datapack - ItemStack#enchant below writes an unsupported enchantment just as
        // happily. See the class javadoc.
        helper.assertTrue(!enchantment(helper, ModEnchantments.RANGE).value()
                        .isSupportedItem(new ItemStack(ModItems.MAGNET)),
                "Range can now be put on the magnet through the enchanting UI, so getCurrentRange's "
                        + "per level branch is reachable in normal play and needs its own test");

        // The unenchanted reach box ends at x 5.8 (player box edge 1.8 plus BASE_RANGE 4.0); an
        // item is caught while its own hull, 0.125 to each side, starts before that. These two sit
        // 0.075 inside and 0.075 outside the edge.
        ItemEntity inReach = helper.spawnItem(Items.DIAMOND, new Vec3(5.85, 1.0, 1.5));
        ItemEntity outOfReach = helper.spawnItem(Items.DIAMOND, new Vec3(6.0, 1.0, 1.5));
        // The same distance out on both horizontal axes at once.
        ItemEntity diagonal = helper.spawnItem(Items.DIAMOND, new Vec3(5.85, 1.0, 5.85));
        // At the far wall of the room: only a boosted magnet can be this wide.
        ItemEntity farOutOfReach = helper.spawnItem(Items.DIAMOND, new Vec3(7.9, 1.0, 1.5));

        // --- plain magnet: up to the edge of its box, and the corners of that box too ---
        ItemStack plain = new ItemStack(ModItems.MAGNET);
        player.setItemInHand(InteractionHand.MAIN_HAND, plain);
        restAll(inReach, outOfReach, diagonal, farOutOfReach);
        tick(plain, level, player, EquipmentSlot.MAINHAND);

        helper.assertTrue(moved(inReach),
                "an item whose hull ends 0.075 inside the reach box was not pulled; BASE_RANGE has "
                        + "shrunk below 3.925");
        helper.assertTrue(moved(diagonal),
                "an item 4.35 blocks away on two axes at once was not pulled; the reach is the "
                        + "player's bounding box inflated by the range, so the corners of that box "
                        + "are in reach even though they are 6.15 blocks away in a straight line");
        helper.assertTrue(!moved(outOfReach),
                "an item whose hull starts 0.075 outside the reach box was pulled; BASE_RANGE has "
                        + "grown past 4.075");
        helper.assertTrue(!moved(farOutOfReach),
                "an unenchanted magnet pulled an item 6.4 blocks away, at the far wall of the room");

        // --- Constructor's Touch: both items the plain magnet had to leave alone ---
        ItemStack enchanted = new ItemStack(ModItems.MAGNET);
        enchanted.enchant(enchantment(helper, ModEnchantments.CONSTRUCTORS_TOUCH), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, enchanted);
        restAll(inReach, outOfReach, diagonal, farOutOfReach);
        tick(enchanted, level, player, EquipmentSlot.MAINHAND);

        helper.assertTrue(moved(farOutOfReach),
                "Constructor's Touch did not reach the item at the far wall, so BOOSTED_RANGE is "
                        + "5.975 or less and the enchantment adds less than two blocks");
        helper.assertTrue(moved(outOfReach),
                "Constructor's Touch did not widen the reach at all: the item just outside the "
                        + "plain magnet's box stayed put");
        helper.assertTrue(moved(inReach),
                "the enchanted magnet stopped pulling the item that the plain one reached");

        helper.succeed();
    }

    // =====================================================================================
    // (d) THE FILTER
    // =====================================================================================

    /**
     * A magnet with a filter set pulls only items whose registry id is exactly that string.
     *
     * <p>Two items lie three blocks away on different axes, and the filter is rewritten between
     * ticks. Both directions are driven - the filtered item comes, the other stays - because a
     * filter that lets nothing through and a filter that lets everything through are both
     * plausible breakages and only the pair of assertions tells them apart.
     *
     * <p>The fifth round is about the exact shape of the comparison rather than about picking a
     * winner: {@code "diamond"} without a namespace must not match {@code minecraft:diamond}, since
     * the code compares the full {@code toString()} of the registry key and a path-only comparison
     * would be a very tempting simplification.
     *
     * <p>The sixth round - a key that is present but empty - is characterization, not coverage: no
     * mod code can produce that state. {@code MagnetItem#setFilterId(stack, null)} removes the key
     * rather than blanking it ({@code MagnetItem.java:167}, which is what {@link
     * #sneakRightClickClearsTheFilterAndTheTooltipFollows} asserts), and the only writer of the key,
     * {@code ItemFrameEntityMixin}, always writes a full registry id. An empty filter can only come
     * from a hand written {@code custom_data} tag, i.e. from a command or a datapack. The round is
     * kept so that the {@code filterId.isEmpty()} half of the guard has a written down meaning, but
     * deleting that half breaks nothing a player can reach.
     *
     * <p>What breaks this test: dropping the {@code null} half of the {@code null || isEmpty} guard
     * (the first round goes red), moving the comparison off the fully qualified id, comparing the
     * filter against the wrong side, and of course renaming {@code MagnetFilter} - every existing
     * magnet in a save would lose its filter, and the second round already goes red. Dropping the
     * {@code isEmpty} half only turns the sixth, characterizing round red.
     */
    public static void magnetFilterMatchesTheFullRegistryIdAndNothingElse(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper);
        ItemStack magnet = new ItemStack(ModItems.MAGNET);
        player.setItemInHand(InteractionHand.MAIN_HAND, magnet);

        ItemEntity diamond = helper.spawnItem(Items.DIAMOND, NEAR_SPOT);
        ItemEntity gold = helper.spawnItem(Items.GOLD_INGOT, SECOND_NEAR_SPOT);

        // --- no filter at all: both come ---
        assertFilterPicks(helper, level, player, magnet, null, diamond, true, gold, true,
                "a magnet without a filter");

        // --- filtered on the diamond ---
        assertFilterPicks(helper, level, player, magnet, "minecraft:diamond", diamond, true, gold, false,
                "a magnet filtered on minecraft:diamond");

        // --- and on the gold ingot, so nothing can pass by being first in the list ---
        assertFilterPicks(helper, level, player, magnet, "minecraft:gold_ingot", diamond, false, gold, true,
                "a magnet filtered on minecraft:gold_ingot");

        // --- an id neither item has: the magnet stays idle ---
        assertFilterPicks(helper, level, player, magnet, "minecraft:emerald", diamond, false, gold, false,
                "a magnet filtered on an item that is not lying around");

        // --- the path without the namespace must not match ---
        assertFilterPicks(helper, level, player, magnet, "diamond", diamond, false, gold, false,
                "a magnet whose filter is missing the namespace");

        // --- present but empty: no mod code writes this, only a command or a datapack can ---
        assertFilterPicks(helper, level, player, magnet, "", diamond, true, gold, true,
                "a magnet whose stored filter is the empty string");

        helper.succeed();
    }

    /**
     * The player facing side of the filter: what the tooltip says it is, and the only way to get
     * rid of it again.
     *
     * <p>{@code MagnetItem#use} is reached by right clicking thin air. It is the mod's only writer
     * of an empty filter, and it is guarded twice - sneaking, and a filter actually being set -
     * with a different {@code InteractionResult} for each outcome. The result matters beyond the
     * test: {@code PASS} is what lets the click fall through to whatever else would have handled
     * it, {@code SUCCESS} swallows it.
     *
     * <p>The clearing is asserted three times over, at three different levels: the stored custom
     * data, the tooltip the player reads, and - the one that actually matters - the magnet's
     * behaviour afterwards, on an item the filter had been blocking a moment earlier. Without that
     * last step this would be a test about strings.
     *
     * <p>This is the one test that needs a player with a live connection: {@code use} sends an
     * overlay message, and a detached mock player would throw on it.
     *
     * <p>What breaks this test: the sneak guard going away (the plain right click would start
     * clearing filters), the "is a filter even set" guard going away (a filterless magnet would
     * swallow every right click instead of passing it on), either result flipping, the clearing
     * writing something other than an absent key, and the tooltip losing the filter line - which is
     * the only place a player can see what their magnet is set to.
     */
    public static void sneakRightClickClearsTheFilterAndTheTooltipFollows(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper);
        ItemStack magnet = new ItemStack(ModItems.MAGNET);
        setFilter(magnet, "minecraft:diamond");
        player.setItemInHand(InteractionHand.MAIN_HAND, magnet);

        ItemEntity gold = helper.spawnItem(Items.GOLD_INGOT, NEAR_SPOT);

        // --- the tooltip names the active filter ---
        List<String> withFilter = tooltipOf(helper, magnet);
        helper.assertValueEqual(withFilter.size(), 2, "tooltip lines on a filtered magnet");
        helper.assertValueEqual(withFilter.get(0), "Filtering: minecraft:diamond",
                "first tooltip line of a filtered magnet");
        helper.assertValueEqual(withFilter.get(1), "Sneak + Right Click to clear",
                "second tooltip line of a filtered magnet");

        // --- and the filter is really doing something ---
        helper.assertTrue(!pulls(magnet, level, player, EquipmentSlot.MAINHAND, gold),
                "test setup broken: the gold ingot was pulled although the magnet is filtered on "
                        + "diamonds, so 'it is pulled after clearing' would prove nothing");

        // --- right clicking without sneaking must not touch the filter ---
        player.setShiftKeyDown(false);
        InteractionResult plainClick = magnet.getItem().use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(plainClick == InteractionResult.PASS,
                "a right click without sneaking returned " + plainClick + " instead of PASS, so the "
                        + "magnet now swallows clicks that should fall through");
        helper.assertValueEqual(filterOf(magnet), "minecraft:diamond",
                "the filter after a right click without sneaking");

        // --- sneak + right click clears it ---
        player.setShiftKeyDown(true);
        InteractionResult sneakClick = magnet.getItem().use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(sneakClick == InteractionResult.SUCCESS,
                "clearing the filter returned " + sneakClick + " instead of SUCCESS");
        helper.assertTrue(!hasFilterKey(magnet),
                "the filter key is still stored after clearing, its value is '" + filterOf(magnet)
                        + "'");

        // --- the tooltip follows ---
        List<String> cleared = tooltipOf(helper, magnet);
        helper.assertValueEqual(cleared.size(), 2, "tooltip lines on a magnet without a filter");
        helper.assertValueEqual(cleared.get(0), "No Filter active",
                "first tooltip line of a magnet without a filter");

        // --- and so does the magnet: the ingot it refused a moment ago is fair game now ---
        player.setShiftKeyDown(false);
        helper.assertTrue(pulls(magnet, level, player, EquipmentSlot.MAINHAND, gold),
                "the gold ingot is still ignored after the filter was cleared");

        // --- clearing again is a no-op that has to fall through ---
        player.setShiftKeyDown(true);
        InteractionResult secondClear = magnet.getItem().use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(secondClear == InteractionResult.PASS,
                "sneak + right click on a magnet without a filter returned " + secondClear
                        + " instead of PASS; it now eats the click for nothing");

        helper.succeed();
    }

    // =====================================================================================
    // (e) GETTING ONE AT ALL
    // =====================================================================================

    /**
     * The magnet's only source: its crafting recipe, resolved through the server's recipe manager
     * exactly the way a crafting table would.
     *
     * <p>{@code data_integrity_game_test_mod_recipes_only_reference_registered_items} already walks
     * every mod recipe and checks that no ingredient dangles, but it never assembles one - a recipe
     * whose pattern was scrambled by a datagen change passes it happily while the magnet has become
     * uncraftable, or craftable from a shape nobody has ever seen.
     *
     * <p>The scrambled grid at the end is what makes the first half mean something: it proves the
     * lookup answers the shape it is given rather than any grid holding the right ingredients.
     *
     * <p>It is a 180 degree rotation, deliberately not the x-mirror. Vanilla shaped recipes match
     * their own mirror on purpose - {@code ShapedRecipePattern#matches} tries the flipped layout
     * first, unless the pattern is symmetrical - so a mirrored grid crafting a magnet is correct
     * behaviour and would make this a false alarm.
     *
     * <p>What breaks this test: any change to {@code recipe/magnet.json} - a different pattern, a
     * different key, a swapped ingredient, a different result or count - and the recipe failing to
     * load at all.
     */
    public static void theMagnetRecipeStillCraftsFromItsDocumentedPattern(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // " IR" / "ILI" / "BI " with I=iron ingot, R=redstone, L=lodestone, B=lapis lazuli.
        CraftingInput grid = grid3x3(
                null, Items.IRON_INGOT, Items.REDSTONE,
                Items.IRON_INGOT, Items.LODESTONE, Items.IRON_INGOT,
                Items.LAPIS_LAZULI, Items.IRON_INGOT, null);

        Optional<RecipeHolder<CraftingRecipe>> match = level.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, grid, level);
        helper.assertTrue(match.isPresent(),
                "the documented magnet pattern does not match any crafting recipe any more, so the "
                        + "magnet cannot be crafted at all");

        RecipeHolder<CraftingRecipe> holder = match.get();
        helper.assertValueEqual(holder.id().identifier().toString(), "simplebuilding:magnet",
                "recipe matched by the documented magnet pattern");

        ItemStack result = holder.value().assemble(grid);
        helper.assertTrue(result.is(ModItems.MAGNET),
                "the magnet recipe produced " + result + " instead of a magnet");
        helper.assertValueEqual(result.getCount(), 1, "magnets produced per craft");

        // --- the same ingredients in the wrong places must not produce a magnet ---
        // Turned by 180 degrees: same ingredient counts, and neither the pattern nor the x-mirror
        // vanilla accepts alongside it.
        CraftingInput scrambled = grid3x3(
                null, Items.IRON_INGOT, Items.LAPIS_LAZULI,
                Items.IRON_INGOT, Items.LODESTONE, Items.IRON_INGOT,
                Items.REDSTONE, Items.IRON_INGOT, null);
        Optional<RecipeHolder<CraftingRecipe>> scrambledMatch = level.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, scrambled, level);
        helper.assertTrue(scrambledMatch.isEmpty()
                        || !scrambledMatch.get().value().assemble(scrambled).is(ModItems.MAGNET),
                "the magnet pattern turned by 180 degrees also crafts a magnet, so the recipe is "
                        + "not shaped the way the data says it is");

        helper.succeed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /**
     * A fully connected mock server player parked in the room.
     *
     * <p>Connected rather than detached on purpose: {@code MagnetItem#use} sends an overlay message
     * and {@code syncVelocityToNearbyPlayers} sends a motion packet to every player in the level,
     * both of which need a connection to swallow them. The magnet has no game mode dependent
     * branch, so the mock's hard wired creative mode costs this file nothing.
     *
     * <p>The player list removal is belt and braces rather than the cleanup it looks like:
     * {@code runBeforeTestEnd} is {@code runAtTickTime(getTimeoutTicks() - 1, ...)}, and
     * {@code GameTestInfo#tick} returns as soon as the test is done, so the runnable only ever runs
     * when a test times out. Every test in here succeeds long before that and leaves its mock
     * player in the list - the same way the rest of this test tree does it. Fixing it here alone
     * would be a deviation without a gain.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(PLAYER_SPOT);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /**
     * Runs one magnet tick, exactly as {@code Inventory#tick} or {@code EntityEquipment#tick}
     * would. Called on {@code stack.getItem()} rather than on {@code ModItems.MAGNET} so the call
     * goes through the same dispatch vanilla uses.
     */
    private static void tick(ItemStack magnet, ServerLevel level, Entity carrier, EquipmentSlot slot) {
        magnet.getItem().inventoryTick(magnet, level, carrier, slot);
    }

    /**
     * Parks {@code target} at a standstill, runs one magnet tick and reports whether the magnet
     * touched it. Only meaningful for items more than one block from the player: inside that
     * distance the magnet multiplies the velocity by 0.2, which leaves a resting item resting.
     */
    private static boolean pulls(ItemStack magnet, ServerLevel level, Entity carrier,
                                 EquipmentSlot slot, ItemEntity target) {
        target.setDeltaMovement(Vec3.ZERO);
        target.setOnGround(false);
        tick(magnet, level, carrier, slot);
        return moved(target);
    }

    /** Whether the last tick left any velocity on the entity at all. */
    private static boolean moved(ItemEntity entity) {
        return entity.getDeltaMovement().lengthSqr() > AT_REST;
    }

    private static void restAll(ItemEntity... entities) {
        for (ItemEntity entity : entities) {
            entity.setDeltaMovement(Vec3.ZERO);
            entity.setOnGround(false);
        }
    }

    /** Sets the filter, runs one tick and checks both items against what the filter should do. */
    private static void assertFilterPicks(GameTestHelper helper, ServerLevel level, ServerPlayer player,
                                          ItemStack magnet, String filter, ItemEntity first,
                                          boolean firstComes, ItemEntity second, boolean secondComes,
                                          String what) {
        setFilter(magnet, filter);
        restAll(first, second);
        tick(magnet, level, player, EquipmentSlot.MAINHAND);

        helper.assertTrue(moved(first) == firstComes,
                what + " should " + (firstComes ? "" : "not ") + "have pulled the "
                        + first.getItem().getItem() + ", but it did " + (moved(first) ? "" : "not"));
        helper.assertTrue(moved(second) == secondComes,
                what + " should " + (secondComes ? "" : "not ") + "have pulled the "
                        + second.getItem().getItem() + ", but it did " + (moved(second) ? "" : "not"));
    }

    /** Writes the filter into custom data, or removes the key entirely for {@code null}. */
    private static void setFilter(ItemStack stack, String filter) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (filter == null) {
            tag.remove(FILTER_KEY);
        } else {
            tag.putString(FILTER_KEY, filter);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** The stored filter, or {@code ""} when the key is absent. */
    private static String filterOf(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getStringOr(FILTER_KEY, "");
    }

    /** Whether the key exists at all - an absent key and an empty one are different states here. */
    private static boolean hasFilterKey(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().contains(FILTER_KEY);
    }

    /** The lines {@code appendHoverText} adds, as plain text. */
    private static List<String> tooltipOf(GameTestHelper helper, ItemStack stack) {
        List<String> lines = new ArrayList<>();
        stack.getItem().appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()),
                TooltipDisplay.DEFAULT, line -> lines.add(line.getString()), TooltipFlag.NORMAL);
        return lines;
    }

    /** A 3x3 crafting grid, row by row; {@code null} stands for an empty slot. */
    private static CraftingInput grid3x3(Item... items) {
        List<ItemStack> stacks = new ArrayList<>(items.length);
        for (Item item : items) {
            stacks.add(item == null ? ItemStack.EMPTY : new ItemStack(item));
        }
        return CraftingInput.of(3, 3, stacks);
    }

    private static void fillFloor(GameTestHelper helper, Block block) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                helper.setBlock(new BlockPos(x, 0, z), block);
            }
        }
    }

    private static void clearHands(ServerPlayer player) {
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }
}
