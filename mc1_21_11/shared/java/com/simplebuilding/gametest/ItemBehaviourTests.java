package com.simplebuilding.gametest;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OreDetectorItem;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Server side behaviour of the mod's remaining tools: the rotator, the quiver, the reinforced
 * bundle's capacity, the ore detector's modes, the octant's corner selection and the building
 * wand's actual placement.
 *
 * <p>These items were covered only by the blanket data checks ("is registered", "recipe is
 * valid") before, so a port could break what any of them <em>does</em> without a single test
 * turning red.
 *
 * <p>The mock player is handed back to the player list through {@code runBeforeTestEnd}, the way
 * every other MC 26.2 suite in this package does it. A hand back written at the end of the test
 * body - which is what this file claimed to do and never did - is skipped by the first failing
 * assertion, and a leaked mock player keeps the player list non-empty, which stalls the gametest
 * server on shutdown. The MC 1.21.11 copy of this file has no such hook and hands its player back
 * explicitly through {@code MockPlayers}; that one difference between the two copies is
 * deliberate.
 *
 * <h2>Known defects touched by this file</h2>
 * <ul>
 *   <li>A locked octant answers a click with an overlay message, and overlay messages are packets
 *       to the client that a mock player's connection discards. What a test can see of that branch
 *       is asserted in {@link #octantStoresBothCornersAndRespectsTheLock}; the message itself is
 *       not observable from here.</li>
 * </ul>
 */
public final class ItemBehaviourTests {

    private ItemBehaviourTests() {
    }

    /** Tick budget for {@link #buildingWandFillsThePlaneItIsPointedAt}. */
    public static final int WAND_MAX_TICKS = 200;

    /** Axis mode 0: the wand builds perpendicular to the axis of the face that was clicked. */
    private static final int AXIS_FROM_CLICKED_FACE = 0;
    /** Axis modes 1, 2 and 3 override that with a fixed build axis. */
    private static final int AXIS_MODE_X = 1;
    private static final int AXIS_MODE_Y = 2;
    private static final int AXIS_MODE_Z = 3;

    /** How many ticks one wand run may take before the drivers below give up on it. */
    private static final int WAND_DRIVE_CAP = 60;

    /**
     * The part of the 8x8x8 room the wand runs are read out of: everything but the floor layer at
     * {@code y == 0} and the two topmost layers, none of which any run below builds into.
     */
    private static final int ROOM_MIN_XZ = 0;
    private static final int ROOM_MAX_XZ = 7;
    private static final int ROOM_MIN_Y = 1;
    private static final int ROOM_MAX_Y = 6;

    /** Hit points for the two faces the wand runs click, in block local coordinates. */
    private static final Vec3 TOP_CENTRE = new Vec3(0.5, 1.0, 0.5);
    private static final Vec3 NORTH_CENTRE = new Vec3(0.5, 0.5, 0.0);

    // =====================================================================================
    // ROTATOR
    // =====================================================================================

    /**
     * On a block with an axis (logs, pillars) the rotator has two distinct behaviours: clicking
     * the middle of a face cycles the axis, clicking within two pixels of a rim snaps the axis
     * parallel to that rim. Both are pure geometry, which is exactly the kind of code that
     * survives a port compiling but stops matching the old feel.
     */
    public static void rotatorTurnsLogsByClickedFaceAndRim(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack rotator = new ItemStack(ModItems.ROTATOR);
        BlockPos log = new BlockPos(2, 1, 2);

        // --- centre of the top face: the log already points along Y, so it advances Y -> Z ---
        setLogAxis(helper, log, Direction.Axis.Y);
        useOn(helper, player, rotator, log, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        helper.assertTrue(logAxis(helper, log) == Direction.Axis.Z,
                "clicking the top of an upright log did not advance the axis to Z, got " + logAxis(helper, log));

        // --- centre of a side face: the axis jumps to the axis of that face ---
        setLogAxis(helper, log, Direction.Axis.Y);
        useOn(helper, player, rotator, log, Direction.NORTH, new Vec3(0.5, 0.5, 0.0));
        helper.assertTrue(logAxis(helper, log) == Direction.Axis.Z,
                "clicking the north face did not lay the log down along Z, got " + logAxis(helper, log));

        // --- rim of the top face: the axis snaps parallel to the rim, not to the face ---
        setLogAxis(helper, log, Direction.Axis.Y);
        useOn(helper, player, rotator, log, Direction.UP, new Vec3(0.05, 1.0, 0.5));
        helper.assertTrue(logAxis(helper, log) == Direction.Axis.X,
                "clicking the west rim did not align the log along X, got " + logAxis(helper, log));

        setLogAxis(helper, log, Direction.Axis.Y);
        useOn(helper, player, rotator, log, Direction.UP, new Vec3(0.5, 1.0, 0.05));
        helper.assertTrue(logAxis(helper, log) == Direction.Axis.Z,
                "clicking the north rim did not align the log along Z, got " + logAxis(helper, log));

        MockPlayers.remove(helper, player);
        TestCleanup.succeed(helper);
    }

    /**
     * On a block with a facing (furnaces, pistons) the rotator turns it clockwise around the
     * axis of the clicked face, and counter clockwise while sneaking. A block with neither
     * property has to be left alone and the interaction has to report that nothing happened -
     * otherwise the rotator would swallow the click and consume durability for nothing.
     */
    public static void rotatorCyclesFacingBlocksAndLeavesPlainBlocksAlone(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack rotator = new ItemStack(ModItems.ROTATOR);
        BlockPos furnace = new BlockPos(4, 1, 2);

        // --- clockwise around Y: north -> east ---
        setFurnaceFacing(helper, furnace, Direction.NORTH);
        player.setShiftKeyDown(false);
        useOn(helper, player, rotator, furnace, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        helper.assertTrue(furnaceFacing(helper, furnace) == Direction.EAST,
                "the furnace did not turn clockwise, it faces " + furnaceFacing(helper, furnace));

        // --- sneaking reverses it: north -> west ---
        setFurnaceFacing(helper, furnace, Direction.NORTH);
        player.setShiftKeyDown(true);
        useOn(helper, player, rotator, furnace, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        helper.assertTrue(furnaceFacing(helper, furnace) == Direction.WEST,
                "sneaking did not reverse the rotation, the furnace faces " + furnaceFacing(helper, furnace));
        player.setShiftKeyDown(false);

        // --- a block without any rotation property must be refused, not silently accepted ---
        BlockPos stone = new BlockPos(6, 1, 2);
        helper.setBlock(stone, Blocks.STONE);
        InteractionResult result = useOn(helper, player, rotator, stone, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        helper.assertTrue(result == InteractionResult.PASS,
                "the rotator claimed to have rotated plain stone, result was " + result);
        helper.assertBlockPresent(Blocks.STONE, stone);

        MockPlayers.remove(helper, player);
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // QUIVER AND REINFORCED BUNDLE
    // =====================================================================================

    /**
     * The quiver is a bundle that only takes arrows. Every arrow type has to fit, everything
     * else has to bounce off - that restriction is the only thing separating it from the
     * reinforced bundle it inherits from.
     *
     * <p>Only one of the three entry points that filter is exercised here: the pickup path,
     * {@code tryInsertStackFromWorld}. The other two are the inventory clicks
     * ({@code overrideStackedOnOther} and {@code overrideOtherStackedOnMe}), and both are covered
     * on both mouse bindings by
     * {@link QuiverTests#arrowFilterHoldsForClicksAndTheInvertedBindingSlipsPastIt} - which also
     * holds them to the click {@code tools.invertBundleInteractions} configures, so the filter
     * cannot drift back to a fixed {@code ClickAction.PRIMARY}.
     */
    public static void quiverTakesArrowsAndRefusesEverythingElse(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        ItemStack quiver = new ItemStack(ModItems.QUIVER);
        ReinforcedBundleItem item = (ReinforcedBundleItem) quiver.getItem();

        helper.assertTrue(item.tryInsertStackFromWorld(quiver, new ItemStack(Items.ARROW, 8), player),
                "the quiver refused plain arrows");
        helper.assertTrue(item.tryInsertStackFromWorld(quiver, new ItemStack(Items.SPECTRAL_ARROW, 8), player),
                "the quiver refused spectral arrows");
        helper.assertTrue(item.tryInsertStackFromWorld(quiver, new ItemStack(Items.TIPPED_ARROW, 8), player),
                "the quiver refused tipped arrows");

        helper.assertTrue(!item.tryInsertStackFromWorld(quiver, new ItemStack(Items.STONE, 8), player),
                "the quiver accepted stone, which makes it an ordinary bundle");
        helper.assertTrue(!item.tryInsertStackFromWorld(quiver, new ItemStack(Items.BOW), player),
                "the quiver accepted a bow");

        // The reinforced bundle it inherits from has no such restriction.
        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        helper.assertTrue(((ReinforcedBundleItem) bundle.getItem())
                        .tryInsertStackFromWorld(bundle, new ItemStack(Items.STONE, 8), player),
                "the reinforced bundle refused stone");

        MockPlayers.remove(helper, player);
        TestCleanup.succeed(helper);
    }

    /**
     * Capacity is the reinforced bundle's whole point, and it comes from three independent
     * sources: the material tier, Deep Pockets and Drawer. This fills each variant until it
     * refuses and asserts that all three sources are still read at all.
     *
     * <p>What it deliberately does <em>not</em> do is pin the factors: an ordering cannot tell a
     * doubling from a 1.1x.
     * {@link ReinforcedBundleTests#capacityFollowsTierAndEnchantmentsAndMatchesTheWikiExport} is
     * the test that pins them - as ratios against the measured base, and against the number the
     * wiki export prints - and
     * {@link ReinforcedBundleTests#insertionStopsAtTheBrimAndWeighsByStackSize} pins how much of an
     * offered stack is taken. Restating those numbers here would only mean two files to correct
     * for one balance change, so this stays the item level check that none of the three sources
     * has fallen out of the wiring.
     *
     * <p>The numbers the assertion messages print are measured on the offered stacks, not counted
     * as a full 64 per accepted call - see {@link #fillUntilFull}, which used to report a capacity
     * the bundle does not have.
     *
     * <p>What breaks this test: the tier factor or either enchantment no longer being read in
     * {@code getMaxCapacity} - each of the three orderings collapses the moment its branch is
     * dropped.
     */
    public static void bundleCapacityGrowsWithTierAndEnchantments(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        int plain = fillUntilFull(helper, player, ModItems.REINFORCED_BUNDLE, null, 0);
        int netherite = fillUntilFull(helper, player, ModItems.NETHERITE_BUNDLE, null, 0);
        int enderite = fillUntilFull(helper, player, ModItems.ENDERITE_BUNDLE, null, 0);

        helper.assertTrue(plain > 0, "the plain reinforced bundle took nothing at all");
        helper.assertTrue(netherite > plain,
                "the netherite bundle (" + netherite + ") holds no more than the reinforced one (" + plain + ")");
        helper.assertTrue(enderite > netherite,
                "the enderite bundle (" + enderite + ") holds no more than the netherite one (" + netherite + ")");

        int deepPockets1 = fillUntilFull(helper, player, ModItems.REINFORCED_BUNDLE, ModEnchantments.DEEP_POCKETS, 1);
        int deepPockets2 = fillUntilFull(helper, player, ModItems.REINFORCED_BUNDLE, ModEnchantments.DEEP_POCKETS, 2);
        int drawer = fillUntilFull(helper, player, ModItems.REINFORCED_BUNDLE, ModEnchantments.DRAWER, 1);

        helper.assertTrue(deepPockets1 > plain,
                "Deep Pockets I did not raise the capacity (" + deepPockets1 + " vs " + plain + ")");
        helper.assertTrue(deepPockets2 > deepPockets1,
                "Deep Pockets II is no better than level I (" + deepPockets2 + " vs " + deepPockets1 + ")");
        helper.assertTrue(drawer > plain,
                "Drawer did not raise the capacity (" + drawer + " vs " + plain + ")");

        MockPlayers.remove(helper, player);
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // ORE DETECTOR
    // =====================================================================================

    /**
     * Sneak-using the detector steps through its modes and wraps around; sneak-using it on a
     * block teaches it that block as a custom target <em>and</em> switches to the mode that
     * searches for it. The sonar ping itself is sound and particles, which a headless test cannot
     * observe - so this covers the state the player actually configures plus the one consequence
     * of it that is visible server side. Whether the beam is drawn is a client concern and is
     * left to the renderer tests.
     *
     * <p>The calibration is checked through {@code findTarget}, the method the item's tick asks
     * which block it would ping, rather than through the {@code Mode} value alone: the mode switch
     * and the stored block are two writes in two lines, and only together do they make the
     * detector hunt what it was pointed at. A calibration that stores the block but leaves the
     * mode at Iron looks perfectly configured in NBT and searches for something else.
     *
     * <p>What breaks this test: the mode cycle no longer wrapping, {@code setMode(CUSTOM)}
     * disappearing from the calibration click, or the click no longer storing the block it was
     * aimed at.
     */
    public static void oreDetectorCyclesModesAndLearnsACustomBlock(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack detector = new ItemStack(ModItems.ORE_DETECTOR);
        player.setItemInHand(InteractionHand.MAIN_HAND, detector);
        player.setShiftKeyDown(true);

        int first = customData(detector).getIntOr("Mode", 0);
        detector.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        int second = customData(detector).getIntOr("Mode", 0);
        helper.assertTrue(second != first,
                "sneak-using the ore detector did not change its mode, it stayed at " + first);

        // Walk the whole cycle: it has to come back to where it started, not run off the end.
        int guard = 0;
        while (customData(detector).getIntOr("Mode", 0) != first && guard++ < 32) {
            detector.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        }
        helper.assertTrue(guard < 32, "the ore detector mode cycle never wrapped back to the start");

        // Sneak-using it on a block adopts that block as the custom target.
        BlockPos target = new BlockPos(3, 1, 5);
        helper.setBlock(target, Blocks.DIAMOND_ORE);
        useOn(helper, player, detector, target, Direction.UP, new Vec3(0.5, 1.0, 0.5));

        CompoundTag nbt = customData(detector);
        helper.assertTrue(nbt.contains("CustomBlock"),
                "sneak-clicking a block did not teach the detector a custom target");
        helper.assertTrue(nbt.getCompoundOrEmpty("CustomBlock").getString("Name").orElse("")
                        .contains("diamond_ore"),
                "the detector stored the wrong custom block: " + nbt.getCompoundOrEmpty("CustomBlock"));

        // ... and the search follows it. The mode is what makes the stored block reachable at all:
        // for any other mode the custom target is never even looked up, and the diamond ore two
        // steps from the player's eyes is not what Iron, Gold or Netherite hunt for.
        BlockPos found = ((OreDetectorItem) detector.getItem())
                .findTarget(helper.getLevel(), detector, player.getEyePosition());
        Assertions.valueEqual(helper, found, helper.absolutePos(target),
                "the block the calibrated detector pings; a calibration that stores the target but "
                        + "leaves the mode alone searches for iron and finds this ore never");

        player.setShiftKeyDown(false);
        MockPlayers.remove(helper, player);
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // OCTANT
    // =====================================================================================

    /**
     * The octant is a pure selection tool: a click sets the first corner, a sneak-click the
     * second, and once it is locked nothing about the selection may move - neither corner, not
     * through a click, not through the sneak-use in the air that otherwise throws the selection
     * away. Sneak-using it in the air while unlocked clears it again.
     *
     * <p>The locked clicks are aimed at a third block, one that is in neither corner: clicking a
     * position that is already stored would leave the same NBT behind whether the lock held or
     * not.
     *
     * <p>What a test cannot see here is the one thing the locked branch actually tells the player:
     * {@code player.sendOverlayMessage(...)} is a packet, and a mock player's connection discards
     * it. What is left of that branch and asserted below is everything else it does - it eats the
     * click (so the block underneath is not used), it writes nothing, and it returns before the
     * durability cost, so a locked octant does not grind itself down on clicks it refuses.
     *
     * <p>What breaks this test: the lock check disappearing from {@code useOn} or from
     * {@code use}, the sneak branch writing to {@code Pos1} (or the plain branch to {@code Pos2}),
     * the locked branch losing its early return and paying durability anyway, or the air
     * sneak-use no longer clearing an unlocked selection.
     */
    public static void octantStoresBothCornersAndRespectsTheLock(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack octant = new ItemStack(ModItems.OCTANT);
        player.setItemInHand(InteractionHand.MAIN_HAND, octant);

        BlockPos firstRelative = new BlockPos(2, 1, 6);
        BlockPos secondRelative = new BlockPos(5, 1, 6);
        BlockPos lockedRelative = new BlockPos(2, 1, 4);
        helper.setBlock(firstRelative, Blocks.STONE);
        helper.setBlock(secondRelative, Blocks.STONE);
        helper.setBlock(lockedRelative, Blocks.STONE);
        BlockPos first = helper.absolutePos(firstRelative);
        BlockPos second = helper.absolutePos(secondRelative);

        player.setShiftKeyDown(false);
        useOn(helper, player, octant, firstRelative, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        helper.assertTrue(cornerEquals(customData(octant), "Pos1", first),
                "the plain click did not store the first corner");

        player.setShiftKeyDown(true);
        useOn(helper, player, octant, secondRelative, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        helper.assertTrue(cornerEquals(customData(octant), "Pos2", second),
                "the sneak click did not store the second corner");
        helper.assertTrue(cornerEquals(customData(octant), "Pos1", first),
                "the sneak click overwrote the first corner");

        // The two clicks above were paid for. Without that the "no durability while locked" check
        // below would hold for a tool that never costs anything in the first place.
        int damageAfterTwoClicks = octant.getDamageValue();
        helper.assertTrue(damageAfterTwoClicks > 0,
                "two selection clicks cost the octant no durability at all, so the locked clicks "
                        + "below cannot show that a refused click is free");

        // --- locked: neither corner may move any more ---
        CompoundTag locked = customData(octant);
        locked.putBoolean("Locked", true);
        octant.set(DataComponents.CUSTOM_DATA, CustomData.of(locked));

        player.setShiftKeyDown(false);
        InteractionResult lockedClick = useOn(helper, player, octant, lockedRelative, Direction.UP,
                new Vec3(0.5, 1.0, 0.5));
        helper.assertTrue(lockedClick == InteractionResult.SUCCESS,
                "a locked octant let the click fall through to the block underneath, it returned "
                        + lockedClick);
        helper.assertTrue(cornerEquals(customData(octant), "Pos1", first),
                "a locked octant let the first corner be moved");

        player.setShiftKeyDown(true);
        useOn(helper, player, octant, lockedRelative, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        helper.assertTrue(cornerEquals(customData(octant), "Pos2", second),
                "a locked octant let the second corner be moved by a sneak click");
        helper.assertTrue(cornerEquals(customData(octant), "Pos1", first),
                "a locked octant moved the first corner on a sneak click");
        Assertions.valueEqual(helper, octant.getDamageValue(), damageAfterTwoClicks,
                "durability the octant spent on two clicks it refused because it is locked");

        // --- and the lock survives the sneak-use that otherwise wipes the selection ---
        InteractionResult lockedAir = octant.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(lockedAir == InteractionResult.PASS,
                "sneak-using a locked octant in the air did not pass the interaction on, it returned "
                        + lockedAir);
        helper.assertTrue(cornerEquals(customData(octant), "Pos1", first)
                        && cornerEquals(customData(octant), "Pos2", second),
                "sneak-using a locked octant in the air cleared the selection it is supposed to "
                        + "protect, the item data is " + customData(octant));

        // --- sneak use in the air clears the selection once the lock is off ---
        CompoundTag unlocked = customData(octant);
        unlocked.putBoolean("Locked", false);
        octant.set(DataComponents.CUSTOM_DATA, CustomData.of(unlocked));

        player.setShiftKeyDown(true);
        octant.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(customData(octant).isEmpty(),
                "sneak-using the octant in the air did not clear the selection");

        player.setShiftKeyDown(false);
        MockPlayers.remove(helper, player);
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // BUILDING WAND
    // =====================================================================================

    /**
     * The wand's whole purpose: click a face, get a filled plane one block in front of that face,
     * paid for out of the inventory. Six runs, because the shape the wand builds has three inputs
     * and a suite that only ever clicks a top face with axis mode 0 and a radius the tier allows
     * pins none of them.
     *
     * <ol>
     *   <li><b>The top face.</b> The 3x3 above the anchor, driven through the player tick, with
     *       exactly one item paid per placed block.</li>
     *   <li><b>The tier cap.</b> A copper wand configured for a bigger radius than its tier allows
     *       still builds the copper plane.</li>
     *   <li><b>A side face.</b> Clicking north builds the upright plane in front of that face, not
     *       a floor above the block.</li>
     *   <li><b>The three axis modes.</b> 1, 2 and 3 override the clicked face with a build axis of
     *       X, Y and Z, and each of the three planes is asserted position by position.</li>
     *   <li><b>The off hand</b> is where the wand looks for material first.</li>
     *   <li><b>The backpack</b> is no material source at all without Master Builder.</li>
     * </ol>
     *
     * <p>Every run compares the whole set of positions the wand touched against the set it was
     * supposed to touch, so a block outside the intended plane fails as loudly as a missing one
     * inside it. The anchor is dirt while the material is stone, so "where is there stone" answers
     * exactly "what did the wand place".
     *
     * <p>The runs stop on the wand's own {@code Active} flag rather than on "the plane looks
     * finished". The wand builds one ring per tick batch with {@code DELAY_TICKS} ticks of pause in
     * between, so between ring 1 and ring 2 there is always a tick at which the 3x3 stands and
     * everything further out is still air: a criterion that is retried every tick - which is what
     * {@code succeedWhen} did here before - succeeds in that gap no matter what radius the wand is
     * really working towards.
     *
     * <p>Run 1 goes through {@code player.connection.tick()} because that is the only place in the
     * suite that proves the wand advances on its own in a player's hand; the gametest server does
     * not pump a mock player's connection by itself. The other runs call the item hook directly,
     * the way {@code BundleWiringTests} and {@code BuildingEnchantmentTests} do: what they are
     * about is geometry, and driving it directly keeps every run inside one test tick.
     *
     * <p>What breaks this test: building from {@code originPos.above()} instead of from the face
     * that was clicked, any change to the axis mode to build axis mapping or to the plane
     * {@code getOffsetForAxis} spans for one axis, dropping the tier clamp on the configured
     * radius, no longer paying one item per block, or the material search skipping the off hand or
     * reaching into the backpack without Master Builder.
     */
    public static void buildingWandFillsThePlaneItIsPointedAt(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        helper.assertFalse(player.getAbilities().instabuild,
                "setup guard: the mock player still builds for free, so nothing below is paid for "
                        + "out of the inventory and the material runs would prove nothing");

        BlockPos anchor = new BlockPos(3, 2, 3);

        // --- 1. the top face: the 3x3 above the anchor, one item spent per block ---
        ItemStack wand = wandWithSettings(ModItems.DIAMOND_BUILDING_WAND, 1, AXIS_FROM_CLICKED_FACE);
        armWand(helper, player, wand, anchor, 3);
        ItemStack stone = new ItemStack(Items.STONE, 64);
        player.getInventory().setItem(1, stone);

        InteractionResult armed = useOn(helper, player, wand, anchor, Direction.UP, TOP_CENTRE);
        helper.assertTrue(armed == InteractionResult.CONSUME,
                "the wand did not arm itself on the clicked face, it returned " + armed);
        driveThroughThePlayerTick(helper, player, wand);

        Set<BlockPos> topPlane = expectedPlane(anchor, Direction.UP, AXIS_FROM_CLICKED_FACE, 1);
        Assertions.valueEqual(helper, blocksAt(helper, anchor, 3, Blocks.STONE), topPlane,
                "the stone a top face click left behind - expected the 3x3 one block above the anchor "
                        + "and nothing further out");
        Assertions.valueEqual(helper, stone.getCount(), 64 - topPlane.size(),
                "stone left in the hotbar; the wand pays one item per placed block");
        helper.assertBlockPresent(Blocks.DIRT, anchor);

        // --- 2. the tier maximum caps the radius the player configured ---
        int copperTierRadius = (ModItems.COPPER_BUILDING_WAND.getWandSquareDiameter() - 1) / 2;
        Assertions.valueEqual(helper, copperTierRadius, 1,
                "setup guard: the copper wand's own maximum radius. This run asks for a plane two "
                        + "rings wider than the tier allows and reads the result out of the 8x8x8 "
                        + "room, which only works while that maximum is 1");
        ItemStack copperWand = wandWithSettings(
                ModItems.COPPER_BUILDING_WAND, copperTierRadius + 2, AXIS_FROM_CLICKED_FACE);
        armWand(helper, player, copperWand, anchor, 3);
        ItemStack copperStone = new ItemStack(Items.STONE, 64);
        player.getInventory().setItem(1, copperStone);

        useOn(helper, player, copperWand, anchor, Direction.UP, TOP_CENTRE);
        driveItemTick(helper, player, copperWand);

        Set<BlockPos> cappedPlane = expectedPlane(anchor, Direction.UP, AXIS_FROM_CLICKED_FACE, copperTierRadius);
        Assertions.valueEqual(helper, blocksAt(helper, anchor, 3, Blocks.STONE), cappedPlane,
                "the stone a copper wand configured for radius " + (copperTierRadius + 2)
                        + " left behind; its tier caps it at radius " + copperTierRadius
                        + ", and the 64 stone it was given would have covered the wider plane");

        // --- 3. a side face: the plane stands in front of the face that was clicked ---
        ItemStack wallWand = wandWithSettings(ModItems.DIAMOND_BUILDING_WAND, 1, AXIS_FROM_CLICKED_FACE);
        armWand(helper, player, wallWand, anchor, 2);
        ItemStack wallStone = new ItemStack(Items.STONE, 64);
        player.getInventory().setItem(1, wallStone);

        useOn(helper, player, wallWand, anchor, Direction.NORTH, NORTH_CENTRE);
        driveItemTick(helper, player, wallWand);

        Set<BlockPos> wall = expectedPlane(anchor, Direction.NORTH, AXIS_FROM_CLICKED_FACE, 1);
        Assertions.valueEqual(helper, blocksAt(helper, anchor, 2, Blocks.STONE), wall,
                "the stone a north face click left behind - expected the upright 3x3 in front of that "
                        + "face, in the layer the player is looking at, not a floor above the block");

        // --- 4. the axis mode overrides the clicked face, one plane per mode ---
        int[] axisModes = {AXIS_MODE_X, AXIS_MODE_Y, AXIS_MODE_Z};
        String[] axisPlanes = {"the Y/Z plane", "the X/Z plane", "the X/Y plane"};
        for (int i = 0; i < axisModes.length; i++) {
            ItemStack axisWand = wandWithSettings(ModItems.DIAMOND_BUILDING_WAND, 1, axisModes[i]);
            armWand(helper, player, axisWand, anchor, 2);
            ItemStack axisStone = new ItemStack(Items.STONE, 64);
            player.getInventory().setItem(1, axisStone);

            useOn(helper, player, axisWand, anchor, Direction.UP, TOP_CENTRE);
            driveItemTick(helper, player, axisWand);

            Set<BlockPos> plane = expectedPlane(anchor, Direction.UP, axisModes[i], 1);
            Assertions.valueEqual(helper, blocksAt(helper, anchor, 2, Blocks.STONE), plane,
                    "the stone axis mode " + axisModes[i] + " left behind on a top face click - that "
                            + "mode has to build " + axisPlanes[i]);
            Assertions.valueEqual(helper, axisStone.getCount(), 64 - plane.size(),
                    "stone spent in axis mode " + axisModes[i] + "; an upright plane runs through the "
                            + "clicked block itself, which cannot be replaced and is not paid for");
        }

        // --- 5. the off hand is the first place the wand looks for material ---
        ItemStack offHandWand = wandWithSettings(ModItems.DIAMOND_BUILDING_WAND, 1, AXIS_FROM_CLICKED_FACE);
        armWand(helper, player, offHandWand, anchor, 2);
        ItemStack planks = new ItemStack(Items.OAK_PLANKS, 64);
        ItemStack hotbarStone = new ItemStack(Items.STONE, 64);
        player.setItemInHand(InteractionHand.OFF_HAND, planks);
        player.getInventory().setItem(1, hotbarStone);

        useOn(helper, player, offHandWand, anchor, Direction.UP, TOP_CENTRE);
        driveItemTick(helper, player, offHandWand);

        Set<BlockPos> plankPlane = expectedPlane(anchor, Direction.UP, AXIS_FROM_CLICKED_FACE, 1);
        Assertions.valueEqual(helper, blocksAt(helper, anchor, 2, Blocks.OAK_PLANKS), plankPlane,
                "the planks the wand built out of the off hand");
        Assertions.valueEqual(helper, planks.getCount(), 64 - plankPlane.size(),
                "planks left in the off hand after the plane was paid for");
        Assertions.valueEqual(helper, hotbarStone.getCount(), 64,
                "the wand spent the hotbar stone although the off hand comes first");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

        // --- 6. without Master Builder the backpack is no material source at all ---
        ItemStack backpackWand = wandWithSettings(ModItems.DIAMOND_BUILDING_WAND, 1, AXIS_FROM_CLICKED_FACE);
        armWand(helper, player, backpackWand, anchor, 2);
        ItemStack backpackStone = new ItemStack(Items.STONE, 64);
        player.getInventory().setItem(9, backpackStone);

        InteractionResult refused = useOn(helper, player, backpackWand, anchor, Direction.UP, TOP_CENTRE);
        helper.assertTrue(refused == InteractionResult.FAIL,
                "a wand without Master Builder accepted the click while its only material sat in the "
                        + "backpack, it returned " + refused);
        helper.assertTrue(!wandIsActive(backpackWand),
                "the wand armed itself although it refused the click");
        for (int tick = 0; tick < 8; tick++) {
            backpackWand.getItem().inventoryTick(backpackWand, helper.getLevel(), player, EquipmentSlot.MAINHAND);
        }
        Assertions.valueEqual(helper, blocksAt(helper, anchor, 2, Blocks.STONE), Set.of(),
                "the wand built out of a backpack slot, which only Master Builder reaches");
        Assertions.valueEqual(helper, backpackStone.getCount(), 64, "stone left in the backpack slot");

        MockPlayers.remove(helper, player);
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        // A mock player does not start in survival, and the wand test checks that the blocks are
        // paid for - a creative player never pays. setGameMode alone leaves isCreative() reporting
        // true, so the game mode is changed directly as well.
        player.setGameMode(GameType.SURVIVAL);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        // Hand the player back however the test ends; a leaked mock player keeps the player list
        // non-empty and the gametest server then stalls on shutdown.
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }


    /**
     * Right clicks a block face at a precise spot on that face. The offset is given inside the
     * block (0..1 per axis), which is what the rotator's rim detection reads.
     */
    private static InteractionResult useOn(GameTestHelper helper, ServerPlayer player, ItemStack stack,
                                           BlockPos relativePos, Direction face, Vec3 offsetInBlock) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = helper.absolutePos(relativePos);
        Vec3 hit = new Vec3(pos.getX() + offsetInBlock.x, pos.getY() + offsetInBlock.y, pos.getZ() + offsetInBlock.z);
        BlockHitResult hitResult = new BlockHitResult(hit, face, pos, false);
        return stack.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult));
    }

    private static void setLogAxis(GameTestHelper helper, BlockPos pos, Direction.Axis axis) {
        helper.setBlock(pos, Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, axis));
    }

    private static Direction.Axis logAxis(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(BlockStateProperties.AXIS);
    }

    private static void setFurnaceFacing(GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.setBlock(pos, Blocks.FURNACE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
    }

    private static Direction furnaceFacing(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static boolean cornerEquals(CompoundTag nbt, String key, BlockPos expected) {
        int[] stored = nbt.getIntArray(key).orElse(new int[0]);
        return stored.length == 3
                && stored[0] == expected.getX()
                && stored[1] == expected.getY()
                && stored[2] == expected.getZ();
    }

    /**
     * Pushes 64 item stacks into a fresh bundle until it refuses one, and reports how many items
     * really went in.
     *
     * <p>The amount is read off the offered stack rather than counted as a full 64 per accepted
     * call: the last stack a bundle accepts is normally a partial one - a bundle that holds 96
     * stone takes 64 and then 32 - so counting calls reports a capacity the bundle does not have,
     * and hides a bundle that swallows a whole stack it has no room for.
     *
     * <p>Bounded, so a bundle that never fills up fails the test instead of hanging the run.
     */
    private static int fillUntilFull(GameTestHelper helper, ServerPlayer player, Item bundleItem,
                                     ResourceKey<Enchantment> enchantmentKey, int level) {
        ItemStack bundle = new ItemStack(bundleItem);
        if (enchantmentKey != null) {
            bundle.enchant(helper.getLevel().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantmentKey), level);
        }
        ReinforcedBundleItem item = (ReinforcedBundleItem) bundle.getItem();

        int inserted = 0;
        for (int i = 0; i < 512; i++) {
            ItemStack offered = new ItemStack(Items.STONE, 64);
            if (!item.tryInsertStackFromWorld(bundle, offered, player)) {
                return inserted;
            }
            inserted += 64 - offered.getCount();
        }
        helper.fail("the bundle " + bundleItem + " never reported itself full");
        return inserted;
    }

    // ------------------------------------------------------------------
    // BUILDING WAND HELPERS
    // ------------------------------------------------------------------

    /** A wand of the given tier with its radius and axis mode written into its own settings. */
    private static ItemStack wandWithSettings(Item wandItem, int radius, int axisMode) {
        ItemStack wand = new ItemStack(wandItem);
        CompoundTag settings = customData(wand);
        settings.putInt("SettingsRadius", radius);
        settings.putInt("SettingsAxis", axisMode);
        wand.set(DataComponents.CUSTOM_DATA, CustomData.of(settings));
        return wand;
    }

    /** Whether the wand's own NBT still says it has building left to do. */
    private static boolean wandIsActive(ItemStack wand) {
        return customData(wand).getBooleanOr("Active", false);
    }

    /**
     * Clears the window one run reads out of, puts the anchor back and arms the player with the
     * wand in the selected slot and nothing else. The caller places the material afterwards, which
     * is what the runs differ in.
     *
     * <p>The anchor is dirt, not stone: the material every run builds with is stone, so
     * "which positions hold stone" is exactly "which positions did the wand fill".
     */
    private static void armWand(GameTestHelper helper, ServerPlayer player, ItemStack wand,
                                BlockPos anchor, int clearReach) {
        for (BlockPos pos : roomPositionsAround(anchor, clearReach)) {
            helper.setBlock(pos, Blocks.AIR);
        }
        helper.setBlock(anchor, Blocks.DIRT);

        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, wand);
    }

    /**
     * Drives the vanilla player tick until the wand switches itself off. The gametest server never
     * pumps a mock player's connection, so this is what makes a wand in a player's hand advance -
     * and it is the only run in the suite that goes through the player rather than calling the
     * item hook.
     */
    private static void driveThroughThePlayerTick(GameTestHelper helper, ServerPlayer player, ItemStack wand) {
        int ticks = 0;
        while (wandIsActive(wand) && ticks < WAND_DRIVE_CAP) {
            player.connection.tick();
            ticks++;
        }
        helper.assertTrue(ticks < WAND_DRIVE_CAP,
                "the wand was still building after " + WAND_DRIVE_CAP + " player ticks");
    }

    /** The same, but calling the item hook directly: deterministic and inside one test tick. */
    private static void driveItemTick(GameTestHelper helper, ServerPlayer player, ItemStack wand) {
        helper.assertTrue(wandIsActive(wand), "the wand did not arm itself on the click");
        int ticks = 0;
        while (wandIsActive(wand) && ticks < WAND_DRIVE_CAP) {
            wand.getItem().inventoryTick(wand, helper.getLevel(), player, EquipmentSlot.MAINHAND);
            ticks++;
        }
        helper.assertTrue(ticks < WAND_DRIVE_CAP,
                "the wand was still building after " + WAND_DRIVE_CAP + " item ticks");
    }

    /**
     * The plane the wand is supposed to fill for one clicked face, axis mode and radius, as room
     * relative positions.
     *
     * <p>This restates the specification rather than asking the item: the base point is one block
     * in front of the face that was clicked, the plane stands perpendicular to the build axis, and
     * that axis is the one the axis mode names (1, 2, 3 = X, Y, Z) or, in mode 0, the axis of the
     * clicked face.
     *
     * <p>The anchor itself drops out of the result. Axis modes 1 and 3 span an upright plane
     * through the clicked block, and the wand skips every position that cannot be replaced - so it
     * leaves a hole exactly there.
     */
    private static Set<BlockPos> expectedPlane(BlockPos anchor, Direction face, int axisMode, int radius) {
        BlockPos placeOrigin = anchor.relative(face);
        Direction.Axis buildAxis = switch (axisMode) {
            case AXIS_MODE_X -> Direction.Axis.X;
            case AXIS_MODE_Y -> Direction.Axis.Y;
            case AXIS_MODE_Z -> Direction.Axis.Z;
            default -> face.getAxis();
        };

        Set<BlockPos> plane = new HashSet<>();
        for (int u = -radius; u <= radius; u++) {
            for (int v = -radius; v <= radius; v++) {
                plane.add(switch (buildAxis) {
                    case X -> placeOrigin.offset(0, u, v);
                    case Y -> placeOrigin.offset(u, 0, v);
                    case Z -> placeOrigin.offset(u, v, 0);
                });
            }
        }
        plane.remove(anchor);
        return plane;
    }

    /** Every position within {@code reach} of {@code centre} that holds the given block. */
    private static Set<BlockPos> blocksAt(GameTestHelper helper, BlockPos centre, int reach, Block block) {
        Set<BlockPos> found = new HashSet<>();
        for (BlockPos pos : roomPositionsAround(centre, reach)) {
            if (helper.getBlockState(pos).is(block)) {
                found.add(pos);
            }
        }
        return found;
    }

    /**
     * The positions of the cube of the given reach around {@code centre} that lie inside the test
     * room, floor layer excluded. Every wand run stays inside this window, so a plane that grows
     * beyond it - a radius that is no longer capped, say - shows up as blocks the assertions did
     * not expect.
     */
    private static List<BlockPos> roomPositionsAround(BlockPos centre, int reach) {
        List<BlockPos> positions = new ArrayList<>();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dy = -reach; dy <= reach; dy++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    BlockPos pos = centre.offset(dx, dy, dz);
                    if (pos.getX() < ROOM_MIN_XZ || pos.getX() > ROOM_MAX_XZ) continue;
                    if (pos.getZ() < ROOM_MIN_XZ || pos.getZ() > ROOM_MAX_XZ) continue;
                    if (pos.getY() < ROOM_MIN_Y || pos.getY() > ROOM_MAX_Y) continue;
                    positions.add(pos);
                }
            }
        }
        return positions;
    }
}
