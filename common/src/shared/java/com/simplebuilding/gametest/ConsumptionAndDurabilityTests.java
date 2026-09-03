package com.simplebuilding.gametest;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.networking.DoubleJumpPayload;
import com.simplebuilding.networking.ModMessageHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * What the mod's tools <em>cost</em>: the durability they spend, the cooldown they impose and the
 * blocks they take out of the inventory.
 *
 * <p>This was the suite's largest blind spot, and the reason is written down in
 * {@link NetworkHandlerTests#doubleJumpNeedsEnchantedBootsAndWearsThem}: every payment the mod
 * makes sits behind {@code !player.getAbilities().instabuild} or {@code !player.isCreative()}, and
 * the usual gametest mock player is creative for good. A regression that deleted a
 * {@code hurtAndBreak}, an {@code addCooldown} or a {@code shrink(1)} would have turned every tool
 * in the mod into a creative-mode tool without a single test going red.
 *
 * <h2>How the two guards are reached</h2>
 * <ul>
 *   <li><b>{@code getAbilities().instabuild}</b> (chisel, octant, building wand) -
 *       {@code Abilities.instabuild} is a plain public field, so {@link #inLevelPlayer} flips it on
 *       the ordinary {@code GameTestHelper#makeMockServerPlayerInLevel()} player. That player keeps
 *       its connection, which the chisel needs (it puts itself on cooldown, and
 *       {@code ServerItemCooldowns} sends a packet) and which the octant needs (its lock refusal
 *       goes out as an overlay message).</li>
 *   <li><b>{@code isCreative()}</b> (sledgehammer, double jump boots) - {@code Player#isCreative()}
 *       reads {@code gameMode()}, and {@code GameTestHelper$3} - the in-level mock - hard-overrides
 *       that method to {@code CREATIVE}; no {@code setGameMode} call can move it. Vanilla's own
 *       {@code GameTestHelper#makeMockServerPlayer(GameType)} builds a <em>second</em> anonymous
 *       subclass ({@code GameTestHelper$2 extends ServerPlayer}) whose {@code gameMode()} answers
 *       the requested mode, and then runs {@code GameType#updatePlayerAbilities} over its
 *       abilities. {@link #detachedPlayer} uses exactly that factory; it only has to narrow the
 *       declared {@code Player} return type back to {@link ServerPlayer}, which every handler under
 *       test demands.</li>
 * </ul>
 *
 * <p>That player is deliberately <em>not</em> placed in the level or the player list: it therefore
 * has no {@code connection}, and every code path that sends a packet to it - above all
 * {@code sendOverlayMessage} - would throw. It is used only where the mod's code stays inside the
 * world and the item. Because it never enters the player list it also cannot stall server shutdown,
 * so it needs no cleanup; the in-level player still does.
 *
 * <h2>What a "creative" half can and cannot prove</h2>
 *
 * <p>Read the creative assertions below with one thing in mind: vanilla itself refuses to damage an
 * item held by a player with {@code instabuild} set ({@code ItemStack#processDurabilityChange}
 * returns 0 for {@code hasInfiniteMaterials()}). So "the tool took no wear in creative" is
 * <em>vanilla-backed</em> and cannot, on its own, prove that the mod's guard is still there. It is
 * still worth asserting, because it fails the moment the mod stops going through
 * {@code hurtAndBreak} - writing {@code setDamageValue} directly, say. The assertions that really
 * pin the mod's own guards are the ones on payments vanilla does not protect:
 * <ul>
 *   <li>the chisel's <b>cooldown</b> - vanilla has no creative exception for {@code addCooldown};</li>
 *   <li>the wand's <b>block consumption</b> - vanilla has no creative exception for
 *       {@code ItemStack#shrink}.</li>
 * </ul>
 * and, for everything behind {@code !isCreative()}, the <b>survival half itself</b>: with the
 * in-level mock that branch never executes at all, so deleting it whole was invisible until now.
 *
 * <p>Every creative half also asserts that the <em>effect</em> still happened - the block really
 * changed, the plane really got built, the air jump really landed. Without that, "no durability was
 * spent" would also pass if the whole interaction had silently stopped working.
 */
public final class ConsumptionAndDurabilityTests {

    private ConsumptionAndDurabilityTests() {
    }

    /** Middle of a block's top face - far enough from every rim for the rotator's rim check. */
    private static final Vec3 TOP_CENTRE = new Vec3(0.5, 1.0, 0.5);

    /** Blocks a radius 1 building wand plane covers: the centre plus its eight neighbours. */
    private static final int WAND_PLANE_BLOCKS = 9;

    // =====================================================================================
    // CHISEL
    // =====================================================================================

    /**
     * A chisel pays one point of durability going forward and two going back, and puts itself on
     * its tier cooldown - but only for a player who is actually paying for their blocks. A click
     * the cooldown swallows costs nothing at all.
     *
     * <p>What breaks this: losing the {@code !instabuild} guard's <em>body</em> (an infinite,
     * cooldown-free chisel in survival); making the expensive reverse direction cost the same as
     * the cheap forward one, which is the only thing that keeps "unchiselling" a deliberate act;
     * moving the {@code hurtAndBreak} above the cooldown check, which would bill a spamming player
     * for clicks that do nothing; and dropping the {@code !instabuild} guard around
     * {@code addCooldown}, which would throttle creative builders. That last one is the assertion
     * with real teeth: vanilla protects a creative player's <em>durability</em> by itself, but
     * nothing except this guard protects them from the cooldown.
     */
    public static void chiselChargesDurabilityAndCooldownOnlyOutsideCreative(GameTestHelper helper) {
        ServerPlayer player = inLevelPlayer(helper, new Vec3(3.5, 2.0, 5.5), false);

        BlockPos target = new BlockPos(3, 1, 3);
        helper.setBlock(target, Blocks.STONE);

        ItemStack chisel = new ItemStack(ModItems.STONE_CHISEL);
        helper.assertTrue(chisel.isDamageableItem(),
                "the stone chisel lost its durability, so nothing below can be measured");

        // --- forward: one point of wear plus the cooldown ---
        player.setShiftKeyDown(false);
        useOn(helper, player, chisel, target, Direction.UP, TOP_CENTRE);

        helper.assertBlockPresent(Blocks.CHISELED_STONE_BRICKS, target);
        helper.assertValueEqual(chisel.getDamageValue(), 1, "wear after one forward chisel");
        helper.assertTrue(player.getCooldowns().isOnCooldown(chisel),
                "the chisel did not go on cooldown in survival, so it can be spammed");

        // --- a click the cooldown swallows changes nothing and costs nothing ---
        InteractionResult swallowed = useOn(helper, player, chisel, target, Direction.UP, TOP_CENTRE);
        helper.assertTrue(swallowed == InteractionResult.PASS,
                "a chisel on cooldown claimed to have acted, result was " + swallowed);
        helper.assertBlockPresent(Blocks.CHISELED_STONE_BRICKS, target);
        helper.assertValueEqual(chisel.getDamageValue(), 1,
                "a click swallowed by the cooldown still cost the player durability");

        clearCooldown(player, chisel);

        // --- reverse (sneaking) is the expensive direction: two points ---
        player.setShiftKeyDown(true);
        useOn(helper, player, chisel, target, Direction.UP, TOP_CENTRE);

        helper.assertBlockPresent(Blocks.STONE, target);
        helper.assertValueEqual(chisel.getDamageValue(), 3,
                "wear after a forward (1) plus a reverse (2) chisel");

        clearCooldown(player, chisel);
        player.setShiftKeyDown(false);

        // --- creative: the block still changes, the tool and above all the cooldown stay untouched ---
        player.getAbilities().instabuild = true;
        useOn(helper, player, chisel, target, Direction.UP, TOP_CENTRE);

        helper.assertBlockPresent(Blocks.CHISELED_STONE_BRICKS, target);
        helper.assertValueEqual(chisel.getDamageValue(), 3, "the chisel wore down in creative");
        helper.assertTrue(!player.getCooldowns().isOnCooldown(chisel),
                "a creative player was put on the chisel cooldown");

        helper.succeed();
    }

    // =====================================================================================
    // OCTANT AND ROTATOR
    // =====================================================================================

    /**
     * Octant and rotator both cost exactly one point per click they accept - and nothing at all for
     * a click they refuse. The refusal half is the interesting one: the rotator returns
     * {@code PASS} on a block it cannot turn, and if the wear were moved out of the success branch
     * it would quietly bill the player for every miss. The same goes for a locked octant, which
     * bails out before it ever reaches its {@code hurtAndBreak}.
     *
     * <p>What breaks this: the {@code !instabuild} guard disappearing from the octant, the
     * rotator's {@code hurtAndBreak} moving outside the {@code newState != state} branch, or the
     * octant charging for a click it rejected because it is locked.
     *
     * <p>The rotator has no creative guard of its own - it relies on vanilla refusing to damage a
     * creative player's tool. Its creative assertion therefore only pins that the wear still goes
     * through {@code ItemStack#hurtAndBreak} rather than being written into the damage component by
     * hand; the assertions that could catch a mod-side regression are the survival ones above it.
     */
    public static void octantAndRotatorSpendOnePointOfWearPerAcceptedClick(GameTestHelper helper) {
        ServerPlayer player = inLevelPlayer(helper, new Vec3(3.5, 2.0, 5.5), false);
        BlockPos corner = new BlockPos(2, 1, 3);
        helper.setBlock(corner, Blocks.STONE);

        // --- octant: one point per stored corner ---
        ItemStack octant = new ItemStack(ModItems.OCTANT);
        player.setShiftKeyDown(false);
        useOn(helper, player, octant, corner, Direction.UP, TOP_CENTRE);
        helper.assertTrue(customData(octant).contains("Pos1"), "the first corner was not stored at all");
        helper.assertValueEqual(octant.getDamageValue(), 1, "octant wear after the first corner");

        player.setShiftKeyDown(true);
        useOn(helper, player, octant, corner, Direction.UP, TOP_CENTRE);
        helper.assertTrue(customData(octant).contains("Pos2"), "the second corner was not stored at all");
        helper.assertValueEqual(octant.getDamageValue(), 2, "octant wear after the second corner");
        player.setShiftKeyDown(false);

        // --- a locked octant refuses the click, so it must not charge for it ---
        CompoundTag locked = customData(octant);
        locked.putBoolean("Locked", true);
        octant.set(DataComponents.CUSTOM_DATA, CustomData.of(locked));
        useOn(helper, player, octant, corner, Direction.UP, TOP_CENTRE);
        helper.assertValueEqual(octant.getDamageValue(), 2,
                "a locked octant billed the player for a click it refused");

        // --- creative: the corner is still stored, the octant stays pristine ---
        player.getAbilities().instabuild = true;
        ItemStack creativeOctant = new ItemStack(ModItems.OCTANT);
        useOn(helper, player, creativeOctant, corner, Direction.UP, TOP_CENTRE);
        helper.assertTrue(customData(creativeOctant).contains("Pos1"),
                "the creative click stored no corner, so the pristine octant proves nothing");
        helper.assertValueEqual(creativeOctant.getDamageValue(), 0, "the octant wore down in creative");

        // --- rotator: one point for a rotation it performed ---
        player.getAbilities().instabuild = false;
        ItemStack rotator = new ItemStack(ModItems.ROTATOR);
        BlockPos log = new BlockPos(5, 1, 3);
        helper.setBlock(log, Blocks.OAK_LOG.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Y));

        InteractionResult turned = useOn(helper, player, rotator, log, Direction.UP, TOP_CENTRE);
        helper.assertTrue(turned == InteractionResult.SUCCESS,
                "the rotator refused an upright log, result was " + turned);
        helper.assertTrue(helper.getBlockState(log).getValue(BlockStateProperties.AXIS) == Direction.Axis.Z,
                "the log did not turn, so the wear below would not be attributable");
        helper.assertValueEqual(rotator.getDamageValue(), 1, "rotator wear after one accepted rotation");

        // --- a block it cannot turn: refused, and free ---
        BlockPos plain = new BlockPos(5, 1, 5);
        helper.setBlock(plain, Blocks.STONE);
        InteractionResult refused = useOn(helper, player, rotator, plain, Direction.UP, TOP_CENTRE);
        helper.assertTrue(refused == InteractionResult.PASS,
                "the rotator claimed to have turned plain stone, result was " + refused);
        helper.assertValueEqual(rotator.getDamageValue(), 1,
                "the rotator billed the player for a block it could not turn");

        // --- creative: the log still turns, the rotator stays pristine ---
        player.getAbilities().instabuild = true;
        ItemStack creativeRotator = new ItemStack(ModItems.ROTATOR);
        useOn(helper, player, creativeRotator, log, Direction.UP, TOP_CENTRE);
        helper.assertTrue(helper.getBlockState(log).getValue(BlockStateProperties.AXIS) == Direction.Axis.Y,
                "the creative click did not turn the log, so the pristine rotator proves nothing");
        helper.assertValueEqual(creativeRotator.getDamageValue(), 0, "the rotator wore down in creative");

        helper.succeed();
    }

    // =====================================================================================
    // BUILDING WAND
    // =====================================================================================

    /**
     * The building wand is the mod's only <em>material</em> consumer, and the price is one block out
     * of the inventory plus one point of wear per block it actually places - no more, no less.
     *
     * <p>{@link ItemBehaviourTests#buildingWandFillsThePlaneItIsPointedAt} already proves that the
     * plane gets paid for at all ("fewer than 64 stone left"). This pins the exact arithmetic
     * instead, which is what catches the three realistic regressions: paying twice per block (once
     * in the material lookup and once in the placement loop); paying for positions that were
     * skipped because something unreplaceable was already standing there - hence the obsidian in
     * the middle of the plane; and charging for a click the wand refused outright because the
     * player had nothing to build from.
     *
     * <p>The creative half is the one assertion in this file that pins an {@code instabuild} guard
     * on a payment vanilla does not protect: nothing in {@code ItemStack#shrink} cares about
     * creative mode, so if the guard around {@code material.consume()} were dropped, a creative
     * builder would be silently billed for every block and this test would go red.
     *
     * <p>The wand builds one ring per inventory tick, so instead of ticking a connection this calls
     * {@code Item#inventoryTick} directly until the wand deactivates itself. The gametest server
     * never pumps a mock player's connection (see
     * {@link ToolBehaviourTests#magnetPullsNearbyItemsAndIgnoresDistantOnes}), so driving the item
     * hook is both closer to what is under test and deterministic inside a single test tick.
     */
    public static void buildingWandBillsOneBlockAndOnePointOfWearPerPlacement(GameTestHelper helper) {
        ServerPlayer player = inLevelPlayer(helper, new Vec3(1.5, 1.0, 1.5), false);

        // --- a survival wand with nothing to build from refuses the click and stays pristine ---
        BlockPos anchor = new BlockPos(3, 1, 3);
        helper.setBlock(anchor, Blocks.STONE);

        ItemStack emptyHanded = radiusOneWand();
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, emptyHanded);

        InteractionResult refused = useOn(helper, player, emptyHanded, anchor, Direction.UP, TOP_CENTRE);
        helper.assertTrue(refused == InteractionResult.FAIL,
                "a wand with no material accepted the click, result was " + refused);
        helper.assertTrue(!wandIsActive(emptyHanded),
                "the wand armed itself even though there was nothing to build from");
        helper.assertValueEqual(emptyHanded.getDamageValue(), 0, "a refused wand click cost durability");
        helper.assertBlockPresent(Blocks.AIR, anchor.above());

        // --- survival: one position of the plane is blocked, so eight get placed and eight get paid for ---
        BlockPos blocked = anchor.offset(1, 1, 1);
        helper.setBlock(blocked, Blocks.OBSIDIAN);

        ItemStack wand = radiusOneWand();
        armWithWandAndStone(player, wand);

        useOn(helper, player, wand, anchor, Direction.UP, TOP_CENTRE);
        runWandToCompletion(helper, player, wand);

        assertPlaneBuilt(helper, anchor, blocked);
        helper.assertBlockPresent(Blocks.OBSIDIAN, blocked);
        helper.assertBlockPresent(Blocks.AIR, anchor.offset(2, 1, 0));

        int placed = WAND_PLANE_BLOCKS - 1;
        helper.assertValueEqual(countInInventory(player, Items.STONE), 64 - placed,
                "stone left after a 3x3 plane with one blocked position; exactly one block per placement, "
                        + "and nothing at all for the position that was skipped");
        helper.assertValueEqual(wand.getDamageValue(), placed,
                "wand wear after a 3x3 plane with one blocked position; one point per placed block");

        // --- creative: the full plane, paid for by nobody ---
        BlockPos creativeAnchor = new BlockPos(3, 4, 3);
        helper.setBlock(creativeAnchor, Blocks.STONE);

        player.getAbilities().instabuild = true;
        ItemStack creativeWand = radiusOneWand();
        armWithWandAndStone(player, creativeWand);

        useOn(helper, player, creativeWand, creativeAnchor, Direction.UP, TOP_CENTRE);
        runWandToCompletion(helper, player, creativeWand);

        assertPlaneBuilt(helper, creativeAnchor, null);
        helper.assertValueEqual(countInInventory(player, Items.STONE), 64,
                "a creative player was billed for the blocks the wand placed");
        helper.assertValueEqual(creativeWand.getDamageValue(), 0, "wand wear after a plane built in creative");

        helper.succeed();
    }

    // =====================================================================================
    // SLEDGEHAMMER SECONDARY USE
    // =====================================================================================

    /**
     * Holding right click with a sledgehammer reshapes the block it is aimed at, and crushing a
     * diamond block turns it into 81 pebbles. Both payments are guarded by
     * {@code !player.isCreative()} - the guard no test in this suite could reach before, because
     * the in-level mock reports creative unconditionally and the whole branch was therefore dead
     * code under test.
     *
     * <p>What breaks this: the guard's body disappearing, so a survival player gets a free
     * reshaping tool and a free diamond crusher; the reverse direction - which needs Constructor's
     * Touch - losing its double price; and the reverse direction happening <em>without</em>
     * Constructor's Touch, which is asserted here as "nothing changed and nothing was charged".
     *
     * <p>The pebble count is asserted in both halves on purpose: it is the proof that the crush
     * actually ran, so "the creative hammer took no damage" cannot pass by the method bailing out
     * early.
     *
     * <p>Both players look straight down at the target, because {@code finishUsingItem} re-picks
     * the block itself through {@code player.pick(5.0, 0.0F, false)} - the aim has to be set up
     * rather than passed in. {@code Entity#snapTo} writes the previous-tick position as well, so
     * the zero partial tick that {@code pick} uses sees the position we just set.
     */
    public static void sledgehammerSecondaryUseWearsDownOnlyTheSurvivalPlayer(GameTestHelper helper) {
        BlockPos target = new BlockPos(3, 1, 3);
        ServerLevel level = helper.getLevel();

        ServerPlayer survival = detachedPlayer(helper, GameType.SURVIVAL, new Vec3(3.5, 3.0, 3.5));
        ServerPlayer creative = detachedPlayer(helper, GameType.CREATIVE, new Vec3(3.5, 3.0, 3.5));

        // --- forward: full block -> stairs, one point of wear ---
        helper.setBlock(target, Blocks.STONE);
        ItemStack hammer = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);
        survival.setShiftKeyDown(false);
        survival.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        hammer.getItem().finishUsingItem(hammer, level, survival);

        helper.assertBlockPresent(Blocks.STONE_STAIRS, target);
        helper.assertValueEqual(hammer.getDamageValue(), 1, "wear for one forward transformation");

        // --- sneaking without Constructor's Touch: no transformation, and nothing charged ---
        survival.setShiftKeyDown(true);
        hammer.getItem().finishUsingItem(hammer, level, survival);

        helper.assertBlockPresent(Blocks.STONE_STAIRS, target);
        helper.assertValueEqual(hammer.getDamageValue(), 1,
                "a plain hammer either reversed the block or charged for trying; the reverse "
                        + "direction is supposed to need Constructor's Touch");

        // --- reverse with Constructor's Touch costs two ---
        ItemStack touchHammer = enchantedStack(helper, ModItems.DIAMOND_SLEDGEHAMMER,
                ModEnchantments.CONSTRUCTORS_TOUCH, 1);
        survival.setItemInHand(InteractionHand.MAIN_HAND, touchHammer);
        touchHammer.getItem().finishUsingItem(touchHammer, level, survival);

        helper.assertBlockPresent(Blocks.STONE, target);
        helper.assertValueEqual(touchHammer.getDamageValue(), 2, "wear for one reverse transformation");
        survival.setShiftKeyDown(false);

        // --- creative: the same forward transformation, free ---
        ItemStack creativeHammer = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);
        creative.setShiftKeyDown(false);
        creative.setItemInHand(InteractionHand.MAIN_HAND, creativeHammer);
        creativeHammer.getItem().finishUsingItem(creativeHammer, level, creative);

        helper.assertBlockPresent(Blocks.STONE_STAIRS, target);
        helper.assertValueEqual(creativeHammer.getDamageValue(), 0, "the hammer wore down in creative");

        // --- crushing a diamond block: 81 pebbles either way, one point of wear in survival ---
        helper.setBlock(target, Blocks.DIAMOND_BLOCK);
        survival.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        hammer.getItem().finishUsingItem(hammer, level, survival);

        helper.assertBlockPresent(Blocks.AIR, target);
        helper.assertItemEntityCountIs(ModItems.DIAMOND_PEBBLE, target, 2.0, 81);
        helper.assertValueEqual(hammer.getDamageValue(), 2, "wear after also crushing a diamond block");

        helper.killAllEntitiesOfClass(ItemEntity.class);

        helper.setBlock(target, Blocks.DIAMOND_BLOCK);
        creative.setItemInHand(InteractionHand.MAIN_HAND, creativeHammer);
        creativeHammer.getItem().finishUsingItem(creativeHammer, level, creative);

        helper.assertBlockPresent(Blocks.AIR, target);
        helper.assertItemEntityCountIs(ModItems.DIAMOND_PEBBLE, target, 2.0, 81);
        helper.assertValueEqual(creativeHammer.getDamageValue(), 0,
                "the hammer wore down crushing a diamond block in creative");

        helper.killAllEntitiesOfClass(ItemEntity.class);
        helper.succeed();
    }

    // =====================================================================================
    // DOUBLE JUMP BOOTS
    // =====================================================================================

    /**
     * Every air jump costs the boots one point of durability. That is the whole balancing of the
     * enchantment - without it Double Jump is free flight - and until a non-creative player could
     * be built it was the one branch {@link NetworkHandlerTests} had to leave as a manual check.
     *
     * <p>What breaks this: the {@code !isCreative()} guard's body disappearing, so the air jump
     * becomes free for everyone; the wear escaping the enchantment check, so unenchanted boots
     * would rot while walking; and the wear being charged once instead of once per jump.
     *
     * <p>The assertions read the stack back out of the armour slot rather than trusting the local
     * reference, because that is the stack {@code handleDoubleJump} actually damages.
     */
    public static void doubleJumpBootsWearDownForTheSurvivalPlayer(GameTestHelper helper) {
        ServerPlayer survival = detachedPlayer(helper, GameType.SURVIVAL, new Vec3(3.5, 1.0, 3.5));

        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(enchantment(helper, ModEnchantments.DOUBLE_JUMP), 1);
        survival.setItemSlot(EquipmentSlot.FEET, boots);

        survival.fallDistance = 7.5F;
        ModMessageHandlers.handleDoubleJump(new DoubleJumpPayload(), survival);

        helper.assertTrue(survival.fallDistance == 0.0,
                "the fall distance survived the air jump, so the jump itself did not happen");
        helper.assertValueEqual(bootWear(survival), 1,
                "the air jump was free; the boots have to take a point of wear in survival");

        // Three more jumps, three more points - the cost is per jump, not once per pair of boots.
        for (int i = 0; i < 3; i++) {
            ModMessageHandlers.handleDoubleJump(new DoubleJumpPayload(), survival);
        }
        helper.assertValueEqual(bootWear(survival), 4, "wear after four air jumps");

        // --- unenchanted boots: no jump and no wear. NetworkHandlerTests asserts the same thing
        //     with a creative player, where the wear branch cannot run in the first place. ---
        ItemStack plain = new ItemStack(Items.DIAMOND_BOOTS);
        survival.setItemSlot(EquipmentSlot.FEET, plain);
        survival.fallDistance = 7.5F;
        ModMessageHandlers.handleDoubleJump(new DoubleJumpPayload(), survival);

        helper.assertTrue(survival.fallDistance == 7.5, "unenchanted boots granted an air jump");
        helper.assertValueEqual(bootWear(survival), 0, "unenchanted boots took wear");

        // --- creative: the jump still works, the boots stay pristine ---
        ServerPlayer creative = detachedPlayer(helper, GameType.CREATIVE, new Vec3(3.5, 1.0, 3.5));
        ItemStack creativeBoots = new ItemStack(Items.DIAMOND_BOOTS);
        creativeBoots.enchant(enchantment(helper, ModEnchantments.DOUBLE_JUMP), 1);
        creative.setItemSlot(EquipmentSlot.FEET, creativeBoots);

        creative.fallDistance = 7.5F;
        ModMessageHandlers.handleDoubleJump(new DoubleJumpPayload(), creative);

        helper.assertTrue(creative.fallDistance == 0.0,
                "the creative player got no air jump, so the pristine boots prove nothing");
        helper.assertValueEqual(bootWear(creative), 0, "the boots wore down in creative");

        helper.succeed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /**
     * The ordinary in-level mock player, with {@code instabuild} set to the wanted value.
     *
     * <p>This player keeps its connection, which everything that sends a packet needs (the chisel
     * cooldown, the octant's overlay message). Its {@code gameMode()} is hard-wired to
     * {@code CREATIVE} and stays that way - only the {@code instabuild} guard is reachable through
     * it. It has to be handed back at the end or the gametest server stalls on shutdown.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer inLevelPlayer(GameTestHelper helper, Vec3 relativePos, boolean instabuild) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relativePos);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.getAbilities().instabuild = instabuild;
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /**
     * A real {@link ServerPlayer} that answers {@code gameMode()} with the mode it was asked for -
     * the only way to reach the mod's {@code !player.isCreative()} branches, because
     * {@code GameTestHelper}'s in-level mock overrides that method to {@code CREATIVE} for good.
     *
     * <p>Built by vanilla's own {@code GameTestHelper#makeMockServerPlayer(GameType)}, which
     * constructs a {@code ServerPlayer} subclass and runs {@code GameType#updatePlayerAbilities}
     * over its abilities. Only its declared return type is {@code Player}, so the narrowing is
     * checked here rather than assumed. The player is never placed in the level or the player list,
     * so it has no {@code connection} - anything that sends it a packet throws - and equally needs
     * no cleanup.
     *
     * <p>The mode and the abilities are verified immediately: this file's whole premise is that a
     * non-creative player is obtainable, and if a Minecraft update ever breaks that, every
     * "no wear in creative" assertion below would start passing for the wrong reason. Failing here,
     * where the cause is obvious, is worth the four lines.
     */
    private static ServerPlayer detachedPlayer(GameTestHelper helper, GameType mode, Vec3 relativePos) {
        Player raw = helper.makeMockServerPlayer(mode);
        if (!(raw instanceof ServerPlayer player)) {
            throw helper.assertionException(
                    "GameTestHelper#makeMockServerPlayer no longer returns a ServerPlayer but a "
                            + raw.getClass().getName() + "; the mod's handlers all take a ServerPlayer");
        }

        Vec3 pos = helper.absoluteVec(relativePos);
        // Straight down, so the sledgehammer's own player.pick() finds the block under the player.
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 90.0F);

        boolean wantCreative = mode == GameType.CREATIVE;
        helper.assertTrue(player.isCreative() == wantCreative,
                "a mock player asked for " + mode + " reports isCreative() == " + player.isCreative()
                        + "; the isCreative() branches are unreachable again");
        helper.assertTrue(player.getAbilities().instabuild == wantCreative,
                "GameType." + mode + " no longer sets instabuild to " + wantCreative);
        return player;
    }

    /** Right clicks a block face at a precise spot on that face, server side. */
    private static InteractionResult useOn(GameTestHelper helper, ServerPlayer player, ItemStack stack,
                                           BlockPos relativePos, Direction face, Vec3 offsetInBlock) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = helper.absolutePos(relativePos);
        Vec3 hit = new Vec3(pos.getX() + offsetInBlock.x, pos.getY() + offsetInBlock.y, pos.getZ() + offsetInBlock.z);
        BlockHitResult hitResult = new BlockHitResult(hit, face, pos, false);
        return stack.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult));
    }

    /**
     * Takes the item off its cooldown again. Without this the next chisel click would be swallowed
     * by the cooldown the previous one just set, and the test would measure nothing.
     */
    private static void clearCooldown(ServerPlayer player, ItemStack stack) {
        player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(stack));
    }

    private static int bootWear(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.FEET).getDamageValue();
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static ItemStack enchantedStack(GameTestHelper helper, Item item,
                                            ResourceKey<Enchantment> key, int level) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(enchantment(helper, key), level);
        return stack;
    }

    /**
     * A wand pinned to radius 1, so the expected plane is a 3x3 on every tier and the test states a
     * shape instead of restating a balancing number.
     */
    private static ItemStack radiusOneWand() {
        ItemStack wand = new ItemStack(ModItems.DIAMOND_BUILDING_WAND);
        CompoundTag settings = customData(wand);
        settings.putInt("SettingsRadius", 1);
        settings.putInt("SettingsAxis", 0);
        wand.set(DataComponents.CUSTOM_DATA, CustomData.of(settings));
        return wand;
    }

    private static void armWithWandAndStone(ServerPlayer player, ItemStack wand) {
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, wand);
        player.getInventory().setItem(1, new ItemStack(Items.STONE, 64));
    }

    /** Everything the player carries, so a consumed block cannot hide in another slot. */
    private static int countInInventory(ServerPlayer player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * Drives the wand's own {@code inventoryTick} until it switches itself off. The gametest server
     * never pumps a mock player's connection, so calling the item hook directly is both closer to
     * what is under test and deterministic - the whole build finishes inside one test tick.
     */
    private static void runWandToCompletion(GameTestHelper helper, ServerPlayer player, ItemStack wand) {
        helper.assertTrue(wandIsActive(wand),
                "the wand did not arm itself on the click, so there is nothing to pay for");

        for (int tick = 0; tick < 200; tick++) {
            wand.getItem().inventoryTick(wand, helper.getLevel(), player, EquipmentSlot.MAINHAND);
            if (!wandIsActive(wand)) {
                return;
            }
        }
        throw helper.assertionException("the building wand was still active after 200 inventory ticks");
    }

    private static boolean wandIsActive(ItemStack wand) {
        return customData(wand).getBooleanOr("Active", false);
    }

    /**
     * The 3x3 the wand builds in the layer above the block that was clicked. {@code skipped}, if
     * given, is the one position that was already occupied and must therefore have been left alone.
     */
    private static void assertPlaneBuilt(GameTestHelper helper, BlockPos anchor, BlockPos skipped) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = anchor.offset(dx, 1, dz);
                if (skipped != null && skipped.equals(pos)) {
                    continue;
                }
                helper.assertBlockPresent(Blocks.STONE, pos);
            }
        }
    }
}
