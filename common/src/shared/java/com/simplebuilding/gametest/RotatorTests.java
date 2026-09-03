package com.simplebuilding.gametest;

import com.simplebuilding.items.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The rotator's geometry, in full: which block state a right click produces, for every branch
 * {@code RotatorItem#calculateNewState} can take.
 *
 * <p>{@code ItemBehaviourTests} already pins two of them - the axis cycle seen from the top face
 * and a clockwise/counter clockwise turn around Y - and
 * {@code ConsumptionAndDurabilityTests#octantAndRotatorSpendOnePointOfWearPerAcceptedClick} pins
 * the one point of wear per accepted click. Everything else in this item is pure arithmetic on
 * directions with no test behind it: the two hand written rotation tables for the X and Z axes,
 * three of the four rim branches on every face, the priority start directions, the two fallbacks
 * when a block cannot hold the direction that was computed, and the 16 step rotation blocks.
 * That is the kind of code that keeps compiling across a port while quietly turning blocks the
 * wrong way, which is why it is written out case by case here.
 *
 * <p>Two conventions run through the whole file:
 * <ul>
 *   <li>Every click states the hit position <em>inside</em> the clicked block (0..1 per axis),
 *       because that is exactly the number {@code getRimDirection} subtracts and reads.</li>
 *   <li>Every rim assertion is paired with, or contrasted against, the centre click on the same
 *       face from the same starting state. A rim case whose expected result happens to equal the
 *       centre result would pass even with the rim detection deleted, so the starting axis is
 *       chosen per case to make the two differ.</li>
 * </ul>
 *
 * <h2>Known defect</h2>
 *
 * <p><b>On every block that answers a right click itself, the rotator only works while sneaking,
 * so half of its behaviour is unreachable there.</b> {@code ServerPlayerGameMode#useItemOn}
 * computes {@code suppressUsingBlock = player.isSecondaryUseActive() && haveSomethingInOurHands}
 * and, unless that holds, offers the click to the block before the item: a furnace opens its menu
 * in {@code AbstractFurnaceBlock#useWithoutItem}, a hopper in {@code HopperBlock#useWithoutItem},
 * a sign opens its editor in {@code SignBlock#useWithoutItem} (a rotator is no
 * {@code SignApplicator}, so {@code SignBlock#useItemOn} hands it straight on), and each of those
 * consumes the action. {@code RotatorItem#useOn} is never entered, so the sneak flag it reads in
 * {@code RotatorItem} line 34 is true in every call that reaches it from such a block.
 *
 * <p>In the game that means: a furnace or a hopper can only ever be turned counter clockwise, a
 * sign only backwards, one step per click - the forward half of {@code rotateAroundAxis} and the
 * {@code change = +1} of the 16 step branch are dead for exactly the blocks a player most often
 * wants to turn. Logs and pistons are unaffected; they have no use action to swallow the click.
 * The tests below call {@code Item#useOn} directly and therefore reach both senses, which is
 * deliberate - pinning only the reachable half would leave the tables half unchecked - but where
 * a case can only exist in the unreachable half, the javadoc says so on the spot.
 *
 * <h2>Not covered</h2>
 * <ul>
 *   <li><b>The click sound.</b> {@code world.playSound(null, ...)} on the server only queues a
 *       packet for nearby players; a mock player's connection swallows it, so there is nothing to
 *       observe.</li>
 *   <li><b>The unused lang key {@code tooltip.simplebuilding.rotator}.</b> {@code RotatorItem}
 *       does not override {@code appendHoverText} at all, so a test asserting "no tooltip line
 *       carries that key" would be asserting a property of vanilla's default {@code Item}, not of
 *       mod code - a tautology from this mod's point of view. On top of that the tooltip is
 *       assembled and translated client side, and the lang file is a client asset the server test
 *       environment never loads, so such a test would pass for the wrong reason as well.</li>
 *   <li><b>The inner {@code cycleDirectionList} call inside {@code handleFacingRotation}</b> (the
 *       one guarded by {@code nextFacing == currentFacing} <em>after</em>
 *       {@code getStandardRotationStart}). It is unreachable for every vanilla direction property:
 *       to get there the block must already point along the clicked axis, and each of the three
 *       priority lists starts with directions perpendicular to that same axis, so the start
 *       direction can never come back equal to the current one. The <em>outer</em> fallback at the
 *       end of the method is reachable and is covered by
 *       {@link #rimAimsFacingBlocksAtTheRimItsOppositeOrTheNextValidValue}.</li>
 * </ul>
 *
 * <h2>Deliberately left out</h2>
 *
 * <p>The audit also asked for the rotator's position in the creative tab (directly behind the
 * magnet). {@code ModItemGroupsContent#populate} is plain server side code and could be driven
 * from here, but the assertion would pin a curation decision rather than a defect: reordering the
 * tab on purpose would turn the test red without anything being broken. It is left to the eye.
 */
public final class RotatorTests {

    private RotatorTests() {
    }

    /** Centre of a top face - outside the rim on both axes the rim check reads there. */
    private static final Vec3 TOP_CENTRE = new Vec3(0.5, 1.0, 0.5);

    /** The block every test turns; a fresh 8x8x8 room per test means one position is enough. */
    private static final BlockPos TARGET = new BlockPos(2, 1, 2);

    /** The rotator's rated durability, as set in {@code ModItems.ROTATOR}. */
    private static final int RATED_DURABILITY = 1024;

    /** {@code ENCHANTABILITY_NETHERITE}, the enchantability the rotator is registered with. */
    private static final int RATED_ENCHANTABILITY = 15;

    // =====================================================================================
    // AXIS BLOCKS (logs, pillars)
    // =====================================================================================

    /**
     * A centre click on a block with an axis has exactly two outcomes, and this pins both plus the
     * full cycle: if the block does not already lie along the clicked face's axis it snaps to that
     * axis, and if it does, it advances one step through X -&gt; Y -&gt; Z -&gt; X. Sneaking changes
     * nothing at all here - {@code calculateNewState} does not even hand {@code isSneaking} to
     * {@code handleAxisRotation}.
     *
     * <p>What breaks this: reversing or reshuffling {@code nextAxis} (only one of the three steps
     * was pinned before, so a reversed cycle went half unnoticed); swapping the two arms of the
     * {@code currentAxis != clickedAxis} test, which would make every click a cycle step; and
     * starting to pass sneaking into the axis branch, which would give the tool two different
     * answers for the same click depending on a key nobody thinks about while placing logs.
     */
    public static void logAxisCyclesThroughAllThreeAxesAndIgnoresSneaking(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, true);
        ItemStack rotator = new ItemStack(ModItems.ROTATOR);
        player.setShiftKeyDown(false);

        // --- the log already lies along the clicked face's axis: one step through the cycle ---
        assertLogTurns(helper, player, rotator, Direction.Axis.X, Direction.EAST, new Vec3(1.0, 0.5, 0.5),
                Direction.Axis.Y, "east face of a log that already lies along X");
        assertLogTurns(helper, player, rotator, Direction.Axis.Y, Direction.UP, TOP_CENTRE,
                Direction.Axis.Z, "top face of an upright log");
        assertLogTurns(helper, player, rotator, Direction.Axis.Z, Direction.NORTH, new Vec3(0.5, 0.5, 0.0),
                Direction.Axis.X, "north face of a log that already lies along Z");

        // --- a different axis: the log simply lies down along the face that was clicked ---
        assertLogTurns(helper, player, rotator, Direction.Axis.Y, Direction.EAST, new Vec3(1.0, 0.5, 0.5),
                Direction.Axis.X, "east face of an upright log");
        assertLogTurns(helper, player, rotator, Direction.Axis.X, Direction.NORTH, new Vec3(0.5, 0.5, 0.0),
                Direction.Axis.Z, "north face of a log lying along X");

        // --- sneaking: the same two clicks have to give the same two answers ---
        player.setShiftKeyDown(true);
        assertLogTurns(helper, player, rotator, Direction.Axis.X, Direction.EAST, new Vec3(1.0, 0.5, 0.5),
                Direction.Axis.Y, "sneaking on the east face of a log lying along X");
        assertLogTurns(helper, player, rotator, Direction.Axis.Y, Direction.EAST, new Vec3(1.0, 0.5, 0.5),
                Direction.Axis.X, "sneaking on the east face of an upright log");
        player.setShiftKeyDown(false);

        helper.succeed();
    }

    /**
     * Where the rim is, and which direction each of the twelve rim branches stands for.
     * {@code getRimDirection} reads the hit position inside the block and calls the outer 0.125 of
     * a face a rim, in the two directions that are left once the face's own axis is taken out - a
     * different pair of directions for each of the three face axes, and in a fixed order (X before
     * Z on a horizontal face, Y before the rest on a vertical one).
     *
     * <p>It takes two probes to see the whole answer, and both halves are needed:
     * <ul>
     *   <li>A <b>log</b> shows <em>where</em> the rim is. A rim click sets the axis <em>of the rim
     *       direction</em> while a centre click follows the axis rule instead, so every case starts
     *       from an axis for which those two answers differ; otherwise it would still pass with the
     *       rim detection deleted. What a log cannot show is which of the two ends of that axis the
     *       code picked - {@code handleAxisRotation} only ever reads
     *       {@code rimDirection.getAxis()}, so on a log DOWN and UP, or NORTH and SOUTH, are the
     *       same answer.</li>
     *   <li>A <b>piston</b> shows exactly that, one case per branch. {@code FACING} takes all six
     *       directions, so the rim direction is written into the block unchanged and swapping the
     *       two ends of a rim pair turns the case red. Each piston case also starts from a
     *       direction for which the ordinary rotation would land somewhere else than the rim does,
     *       so none of them survives the rim branch being dropped either.</li>
     * </ul>
     *
     * <p>The last pair pins the constant 0.125 itself from both sides: 0.124 still counts as rim,
     * 0.13 no longer does.
     *
     * <p>What breaks this: dropping any one of the twelve branches; swapping the two ends of any
     * one rim pair (DOWN against UP on a side face, NORTH against SOUTH on the top face, ...);
     * mixing up the axes inside one of the three blocks, e.g. returning WEST/EAST instead of
     * NORTH/SOUTH on a north face; reordering the checks inside a block so that a corner is read
     * on the other axis; and widening or narrowing the margin, which the two boundary cases catch
     * in either direction.
     */
    public static void rimIsTheOuterEighthOfEveryFaceAndNowhereInside(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, true);
        ItemStack rotator = new ItemStack(ModItems.ROTATOR);
        player.setShiftKeyDown(false);

        // --- top face (axis Y): x is read first, then z; the rim directions are W/E and N/S ---
        assertLogTurns(helper, player, rotator, Direction.Axis.X, Direction.UP, TOP_CENTRE,
                Direction.Axis.Y, "centre of the top face (the baseline the rim cases differ from)");
        assertLogTurns(helper, player, rotator, Direction.Axis.Y, Direction.UP, new Vec3(0.95, 1.0, 0.5),
                Direction.Axis.X, "east rim of the top face");
        assertLogTurns(helper, player, rotator, Direction.Axis.X, Direction.UP, new Vec3(0.5, 1.0, 0.95),
                Direction.Axis.Z, "south rim of the top face");
        assertLogTurns(helper, player, rotator, Direction.Axis.X, Direction.UP, new Vec3(0.5, 1.0, 0.05),
                Direction.Axis.Z, "north rim of the top face");

        // --- east face (axis X): y is read first, then z; the rim directions are D/U and N/S ---
        assertLogTurns(helper, player, rotator, Direction.Axis.Z, Direction.EAST, new Vec3(1.0, 0.5, 0.5),
                Direction.Axis.X, "centre of the east face (baseline)");
        assertLogTurns(helper, player, rotator, Direction.Axis.Z, Direction.EAST, new Vec3(1.0, 0.05, 0.5),
                Direction.Axis.Y, "lower rim of the east face");
        assertLogTurns(helper, player, rotator, Direction.Axis.Z, Direction.EAST, new Vec3(1.0, 0.95, 0.5),
                Direction.Axis.Y, "upper rim of the east face");
        assertLogTurns(helper, player, rotator, Direction.Axis.Y, Direction.EAST, new Vec3(1.0, 0.5, 0.05),
                Direction.Axis.Z, "north rim of the east face");
        assertLogTurns(helper, player, rotator, Direction.Axis.Y, Direction.EAST, new Vec3(1.0, 0.5, 0.95),
                Direction.Axis.Z, "south rim of the east face");

        // --- north face (axis Z): y is read first, then x; the rim directions are D/U and W/E ---
        assertLogTurns(helper, player, rotator, Direction.Axis.Z, Direction.NORTH, new Vec3(0.5, 0.5, 0.0),
                Direction.Axis.X, "centre of the north face (baseline)");
        assertLogTurns(helper, player, rotator, Direction.Axis.Z, Direction.NORTH, new Vec3(0.5, 0.05, 0.0),
                Direction.Axis.Y, "lower rim of the north face");
        assertLogTurns(helper, player, rotator, Direction.Axis.Z, Direction.NORTH, new Vec3(0.5, 0.95, 0.0),
                Direction.Axis.Y, "upper rim of the north face");
        assertLogTurns(helper, player, rotator, Direction.Axis.Y, Direction.NORTH, new Vec3(0.05, 0.5, 0.0),
                Direction.Axis.X, "west rim of the north face");
        assertLogTurns(helper, player, rotator, Direction.Axis.Y, Direction.NORTH, new Vec3(0.95, 0.5, 0.0),
                Direction.Axis.X, "east rim of the north face");

        // --- the same twelve rim spots with a piston, which stores the direction and not just its
        //     axis. Every start below is picked so that the ordinary rotation for that face would
        //     answer something other than the rim direction, so each case fails both ways: if the
        //     rim branch stops firing, and if it fires with the two ends of the pair swapped. ---

        // top face (axis Y): x is read first, then z; the rim directions are W/E and N/S
        assertPistonTurnsFrom(helper, player, rotator, Direction.NORTH, Direction.UP, new Vec3(0.05, 1.0, 0.5),
                Direction.WEST, "west rim of the top face, aimed at a piston");
        assertPistonTurnsFrom(helper, player, rotator, Direction.SOUTH, Direction.UP, new Vec3(0.95, 1.0, 0.5),
                Direction.EAST, "east rim of the top face, aimed at a piston");
        assertPistonTurnsFrom(helper, player, rotator, Direction.EAST, Direction.UP, new Vec3(0.5, 1.0, 0.05),
                Direction.NORTH, "north rim of the top face, aimed at a piston");
        assertPistonTurnsFrom(helper, player, rotator, Direction.WEST, Direction.UP, new Vec3(0.5, 1.0, 0.95),
                Direction.SOUTH, "south rim of the top face, aimed at a piston");

        // east face (axis X): y is read first, then z; the rim directions are D/U and N/S
        assertPistonTurnsFrom(helper, player, rotator, Direction.UP, Direction.EAST, new Vec3(1.0, 0.05, 0.5),
                Direction.DOWN, "lower rim of the east face, aimed at a piston");
        assertPistonTurnsFrom(helper, player, rotator, Direction.NORTH, Direction.EAST, new Vec3(1.0, 0.95, 0.5),
                Direction.UP, "upper rim of the east face, aimed at a piston");
        assertPistonTurnsFrom(helper, player, rotator, Direction.DOWN, Direction.EAST, new Vec3(1.0, 0.5, 0.05),
                Direction.NORTH, "north rim of the east face, aimed at a piston");
        assertPistonTurnsFrom(helper, player, rotator, Direction.UP, Direction.EAST, new Vec3(1.0, 0.5, 0.95),
                Direction.SOUTH, "south rim of the east face, aimed at a piston");

        // north face (axis Z): y is read first, then x; the rim directions are D/U and W/E
        assertPistonTurnsFrom(helper, player, rotator, Direction.UP, Direction.NORTH, new Vec3(0.5, 0.05, 0.0),
                Direction.DOWN, "lower rim of the north face, aimed at a piston");
        assertPistonTurnsFrom(helper, player, rotator, Direction.EAST, Direction.NORTH, new Vec3(0.5, 0.95, 0.0),
                Direction.UP, "upper rim of the north face, aimed at a piston");
        assertPistonTurnsFrom(helper, player, rotator, Direction.UP, Direction.NORTH, new Vec3(0.05, 0.5, 0.0),
                Direction.WEST, "west rim of the north face, aimed at a piston");
        assertPistonTurnsFrom(helper, player, rotator, Direction.DOWN, Direction.NORTH, new Vec3(0.95, 0.5, 0.0),
                Direction.EAST, "east rim of the north face, aimed at a piston");

        // --- the margin itself: 0.124 is inside the rim, 0.13 is already past it ---
        // Both start from an axis for which "rim" and "centre" disagree, so each case can only be
        // satisfied by the side of the boundary it claims.
        assertLogTurns(helper, player, rotator, Direction.Axis.Y, Direction.UP, new Vec3(0.124, 1.0, 0.5),
                Direction.Axis.X, "0.124 from the west edge, still inside the rim");
        assertLogTurns(helper, player, rotator, Direction.Axis.X, Direction.UP, new Vec3(0.13, 1.0, 0.5),
                Direction.Axis.Y, "0.13 from the west edge, no longer a rim");

        helper.succeed();
    }

    // =====================================================================================
    // FACING BLOCKS (pistons, furnaces, hoppers)
    // =====================================================================================

    /**
     * A centre click on a block with a facing turns it a quarter turn clockwise around the axis of
     * the face that was clicked, and counter clockwise while sneaking. Only the Y axis had a test;
     * the X and Z tables in {@code rotateAroundAxis} are written out by hand and were completely
     * unchecked.
     *
     * <p>Each axis is driven all the way round rather than one step: four clicks have to walk the
     * four directions perpendicular to that axis in order and land back on the start. A single
     * step could still pass with two entries of a table swapped; a full lap cannot.
     *
     * <p>Both laps are run in both senses. The clockwise and the counter clockwise answer of every
     * line in {@code rotateAroundAxis} are two separate literals inside one ternary, so a forward
     * lap says nothing at all about the backward one; four sneaking clicks per axis are what it
     * takes to read the other eight literals.
     *
     * <p>The last part covers the case the tables deliberately do not answer: a block already
     * pointing along the clicked axis. {@code rotateAroundAxis} hands such a direction back
     * unchanged, and {@code getStandardRotationStart} then supplies the axis' priority start. Both
     * ends of those lists are exercised: a piston can hold the first entry of every list (NORTH for
     * Y, UP for X and Z), while a furnace and a hopper cannot point up and therefore land on the
     * second one (EAST for Z, NORTH for X). Without this branch the click would be a no-op and the
     * rotator would refuse the most natural gesture there is, clicking the face a block points at.
     *
     * <p>A piston is used for the laps because {@code BlockStateProperties.FACING} allows all six
     * directions, so nothing is filtered out and the tables are read exactly as written. Note that
     * the furnace and hopper clicks here are centre clicks without sneaking, which the game itself
     * never delivers to the item - see
     * {@link #rimAimsFacingBlocksAtTheRimItsOppositeOrTheNextValidValue} for why; they are driven
     * anyway because {@code getStandardRotationStart} is reached the same way from either sense,
     * and it is the priority list and not the sneak flag that is under test here.
     *
     * <p>What breaks this: any swapped or dropped entry in either arm of the X or Z table in
     * {@code rotateAroundAxis}; tying the sneak flag to the wrong sense (the reverse laps expect
     * the opposite neighbour, not just "something else"); and changing, reordering or removing any
     * of the first two entries of a priority list in {@code getStandardRotationStart}.
     */
    public static void facingBlocksTurnOneQuarterAroundTheClickedAxisOrJumpToItsStart(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, true);
        ItemStack rotator = new ItemStack(ModItems.ROTATOR);
        player.setShiftKeyDown(false);

        Vec3 eastCentre = new Vec3(1.0, 0.5, 0.5);
        Vec3 northCentre = new Vec3(0.5, 0.5, 0.0);

        // --- one full lap around X: UP -> NORTH -> DOWN -> SOUTH -> UP ---
        setPistonFacing(helper, Direction.UP);
        assertPistonTurns(helper, player, rotator, Direction.EAST, eastCentre, Direction.NORTH, "X lap step 1");
        assertPistonTurns(helper, player, rotator, Direction.EAST, eastCentre, Direction.DOWN, "X lap step 2");
        assertPistonTurns(helper, player, rotator, Direction.EAST, eastCentre, Direction.SOUTH, "X lap step 3");
        assertPistonTurns(helper, player, rotator, Direction.EAST, eastCentre, Direction.UP, "X lap step 4");

        // --- one full lap around Z: UP -> EAST -> DOWN -> WEST -> UP ---
        setPistonFacing(helper, Direction.UP);
        assertPistonTurns(helper, player, rotator, Direction.NORTH, northCentre, Direction.EAST, "Z lap step 1");
        assertPistonTurns(helper, player, rotator, Direction.NORTH, northCentre, Direction.DOWN, "Z lap step 2");
        assertPistonTurns(helper, player, rotator, Direction.NORTH, northCentre, Direction.WEST, "Z lap step 3");
        assertPistonTurns(helper, player, rotator, Direction.NORTH, northCentre, Direction.UP, "Z lap step 4");

        // --- sneaking runs both laps backwards, and is driven all the way round as well: the
        //     counter clockwise arm of every line in the two tables is a literal of its own, so a
        //     single reverse step would leave three of the four untouched per axis ---
        player.setShiftKeyDown(true);
        setPistonFacing(helper, Direction.UP);
        assertPistonTurns(helper, player, rotator, Direction.EAST, eastCentre, Direction.SOUTH, "reverse X lap step 1");
        assertPistonTurns(helper, player, rotator, Direction.EAST, eastCentre, Direction.DOWN, "reverse X lap step 2");
        assertPistonTurns(helper, player, rotator, Direction.EAST, eastCentre, Direction.NORTH, "reverse X lap step 3");
        assertPistonTurns(helper, player, rotator, Direction.EAST, eastCentre, Direction.UP, "reverse X lap step 4");

        setPistonFacing(helper, Direction.UP);
        assertPistonTurns(helper, player, rotator, Direction.NORTH, northCentre, Direction.WEST, "reverse Z lap step 1");
        assertPistonTurns(helper, player, rotator, Direction.NORTH, northCentre, Direction.DOWN, "reverse Z lap step 2");
        assertPistonTurns(helper, player, rotator, Direction.NORTH, northCentre, Direction.EAST, "reverse Z lap step 3");
        assertPistonTurns(helper, player, rotator, Direction.NORTH, northCentre, Direction.UP, "reverse Z lap step 4");
        player.setShiftKeyDown(false);

        // --- the block already points along the clicked axis: the axis' priority start wins ---
        setPistonFacing(helper, Direction.UP);
        assertPistonTurns(helper, player, rotator, Direction.UP, TOP_CENTRE, Direction.NORTH,
                "top face of a piston pointing up");
        setPistonFacing(helper, Direction.DOWN);
        assertPistonTurns(helper, player, rotator, Direction.UP, TOP_CENTRE, Direction.NORTH,
                "top face of a piston pointing down");
        setPistonFacing(helper, Direction.EAST);
        assertPistonTurns(helper, player, rotator, Direction.EAST, eastCentre, Direction.UP,
                "east face of a piston pointing east");
        setPistonFacing(helper, Direction.NORTH);
        assertPistonTurns(helper, player, rotator, Direction.NORTH, northCentre, Direction.UP,
                "north face of a piston pointing north");

        // --- and the second entry of a list, which is what a block that cannot point up gets. A
        //     piston always takes the first entry, so on its own it would leave NORTH (X list) and
        //     EAST (Z list) as untested literals - and those two are the ones the game actually
        //     reaches, since furnaces and hoppers are far more common than pistons. ---
        setFurnaceFacing(helper, Direction.NORTH);
        useOn(helper, player, rotator, TARGET, Direction.NORTH, northCentre);
        helper.assertTrue(furnaceFacing(helper) == Direction.EAST,
                "a furnace facing north, clicked in the middle of the very face it points at, has "
                        + "to fall through UP to the Z list's second entry EAST, but it faces "
                        + furnaceFacing(helper));

        setHopperFacing(helper, Direction.EAST);
        useOn(helper, player, rotator, TARGET, Direction.EAST, eastCentre);
        helper.assertTrue(hopperFacing(helper) == Direction.NORTH,
                "a hopper facing east, clicked in the middle of the very face it points at, has to "
                        + "fall through UP to the X list's second entry NORTH, but it faces "
                        + hopperFacing(helper));

        helper.succeed();
    }

    /**
     * What a rim click does to a block with a facing, through all three of the answers the code can
     * give, in the order it tries them:
     * <ol>
     *   <li>the rim direction itself, when the block may hold it - a furnace aimed straight at the
     *       edge that was touched;</li>
     *   <li>its opposite, when it may not - a hopper has every direction but UP, so touching the
     *       upper rim of a side face aims it DOWN;</li>
     *   <li>neither, when the property allows neither the rim nor its opposite - a horizontal
     *       furnace can hold no vertical direction at all, so the rim is dropped and the ordinary
     *       rotation runs. That rotation then wants DOWN, which the furnace cannot hold either, and
     *       the method's last fallback walks the property's own list of allowed values instead.
     *       That fallback line is only reachable through exactly this combination.</li>
     * </ol>
     *
     * <p>The last case reads the order vanilla stores the allowed values in, so that order is
     * asserted first as a precondition: if a Minecraft update ever reorders
     * {@code HORIZONTAL_FACING}, the failure says so instead of blaming the mod. It is driven
     * twice, once sneaking and once not, because the sneak flag reaches that fallback and decides
     * which way along the list it walks.
     *
     * <p>Which of those two the game can actually produce is worth writing down, because it is only
     * one of them: {@code ServerPlayerGameMode#useItemOn} computes
     * {@code suppressUsingBlock = player.isSecondaryUseActive() && haveSomethingInOurHands} and,
     * unless that holds, offers the click to the block first. A furnace answers it in
     * {@code AbstractFurnaceBlock#useWithoutItem} by opening its menu, a hopper in
     * {@code HopperBlock#useWithoutItem}, and a sign in {@code SignBlock#useWithoutItem} by opening
     * the editor (a rotator is no {@code SignApplicator}, so {@code SignBlock#useItemOn} passes it
     * on) - each of them consuming the action, so {@code RotatorItem#useOn} is never called. On
     * those three kinds of block the tool therefore only ever runs with {@code isSneaking} true;
     * see the "Known defect" paragraph on the class. The tests here call {@code useOn} directly and
     * so can drive both senses, which is the point: a case that existed only in the non-sneaking
     * half would be pinning code the game cannot reach.
     *
     * <p>The closing scan pins something the code says but the game does not: besides
     * {@code facing}, {@code getFacingProperty} also accepts properties named
     * {@code horizontal_facing} and {@code hopper_facing}. No block in the registry - vanilla or
     * this mod - carries either name, so those two branches never run; the rotator's entire
     * support for directional blocks rests on the single name {@code facing}. Pinning it the way
     * {@code EnchantmentEffectTests#coverAndBridgeAreInertAndThisIsDeliberatelyPinnedDown} pins the
     * inert enchantments means that the day such a block does appear, this test says so and the
     * branch gets real coverage instead of staying decorative.
     *
     * <p>What breaks this: dropping the rim branch, or the opposite fallback inside it (a hopper
     * would stop taking aim from its rim); letting an impossible rim direction through instead of
     * ignoring it, which would throw on {@code setValue}; removing the closing
     * {@code cycleDirectionList} fallback, which would leave a furnace unturnable from a vertical
     * face; and renaming the {@code facing} literal in {@code getFacingProperty}, which would
     * silently strip every directional block of its support.
     */
    public static void rimAimsFacingBlocksAtTheRimItsOppositeOrTheNextValidValue(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, true);
        ItemStack rotator = new ItemStack(ModItems.ROTATOR);
        player.setShiftKeyDown(false);

        // --- 1. the rim direction is allowed: the furnace looks straight at it ---
        setFurnaceFacing(helper, Direction.NORTH);
        useOn(helper, player, rotator, TARGET, Direction.UP, new Vec3(0.05, 1.0, 0.5));
        helper.assertTrue(furnaceFacing(helper) == Direction.WEST,
                "the west rim of the top face did not aim the furnace west, it faces " + furnaceFacing(helper));

        setFurnaceFacing(helper, Direction.NORTH);
        useOn(helper, player, rotator, TARGET, Direction.UP, new Vec3(0.5, 1.0, 0.95));
        helper.assertTrue(furnaceFacing(helper) == Direction.SOUTH,
                "the south rim of the top face did not aim the furnace south, it faces " + furnaceFacing(helper));

        // --- 2. the rim is not allowed but its opposite is: a hopper cannot point up ---
        setHopperFacing(helper, Direction.NORTH);
        useOn(helper, player, rotator, TARGET, Direction.NORTH, new Vec3(0.5, 0.95, 0.0));
        helper.assertTrue(hopperFacing(helper) == Direction.DOWN,
                "the upper rim of a hopper's side did not fall back to DOWN, it faces " + hopperFacing(helper));

        // --- 3. neither the rim nor its opposite fits: the rim is dropped, and the rotation the
        //        tables produce (DOWN) does not fit either, so the value list decides ---
        helper.assertValueEqual(BlockStateProperties.HORIZONTAL_FACING.getPossibleValues(),
                List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST),
                "vanilla reordered HORIZONTAL_FACING; the expected value of the list fallback below "
                        + "is derived from that order, so re-derive it before touching the mod");
        setFurnaceFacing(helper, Direction.EAST);
        useOn(helper, player, rotator, TARGET, Direction.NORTH, new Vec3(0.5, 0.95, 0.0));
        helper.assertTrue(furnaceFacing(helper) == Direction.NORTH,
                "the vertical rim of a furnace's side should have fallen through to the next allowed "
                        + "value NORTH, but it faces " + furnaceFacing(helper));

        // The same click while sneaking - the only version of it a player can actually perform, see
        // above - has to walk the list the other way. This is what holds the isSneaking argument of
        // that last cycleDirectionList call in place; with a hard coded false both clicks would
        // answer NORTH and nothing here would notice.
        player.setShiftKeyDown(true);
        setFurnaceFacing(helper, Direction.EAST);
        useOn(helper, player, rotator, TARGET, Direction.NORTH, new Vec3(0.5, 0.95, 0.0));
        helper.assertTrue(furnaceFacing(helper) == Direction.WEST,
                "sneaking, the same fallback has to walk the value list backwards to WEST, but the "
                        + "furnace faces " + furnaceFacing(helper));
        player.setShiftKeyDown(false);

        // --- the two other property names the lookup accepts exist nowhere in the game ---
        List<String> unexpected = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            for (Property<?> property : block.getStateDefinition().getProperties()) {
                if (property.getValueClass() != Direction.class) {
                    continue;
                }
                if (property.getName().equals("horizontal_facing") || property.getName().equals("hopper_facing")) {
                    unexpected.add(BuiltInRegistries.BLOCK.getKey(block) + "#" + property.getName());
                }
            }
        }
        helper.assertTrue(unexpected.isEmpty(),
                "getFacingProperty's horizontal_facing / hopper_facing branches were dead by "
                        + "assumption, but these blocks now carry such a property and are therefore "
                        + "turnable without any test behind them: " + unexpected);

        helper.succeed();
    }

    // =====================================================================================
    // 16 STEP BLOCKS (standing signs, banners, skulls)
    // =====================================================================================

    /**
     * Blocks that store a rotation from 0 to 15 are the third and last branch. A centre click moves
     * one step, sneaking moves one step back, and a rim click multiplies the step by four - a
     * quarter turn - whichever rim was touched; the direction is thrown away here, only its
     * presence counts.
     *
     * <p>Two of the cases below step BELOW zero (0 - 1 and 2 - 4). That is what makes the
     * {@code + 16} in {@code (current + change + 16) % 16} load bearing: Java's {@code %} keeps
     * the sign of the left operand, so without it those two clicks would ask the property for
     * -1 and -2 and throw. Wrapping over 15 upwards - which the other cases do - never needs it,
     * so a test that only walks upwards proves nothing about that term.
     *
     * <p>What breaks this: dropping the {@code + 16} (a sneaking click at 0 would produce -1 and
     * throw, since the property does not accept it); losing the {@code *= 4} for rim clicks, which
     * is the only way to place a sign at a right angle in one click; tying the factor to a
     * particular rim direction; and swapping the sign of the sneak step.
     */
    public static void sixteenStepBlocksStepOnceInTheMiddleAndFourTimesAtTheRim(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, true);
        ItemStack rotator = new ItemStack(ModItems.ROTATOR);

        // A standing sign needs something underneath, or the neighbour update from setBlock pops it.
        BlockPos sign = TARGET.above();
        helper.setBlock(TARGET, Blocks.STONE);

        Vec3 westRim = new Vec3(0.05, 1.0, 0.5);
        Vec3 southRim = new Vec3(0.5, 1.0, 0.95);

        // --- centre: one step forward, one step back while sneaking, both wrapping over 15 ---
        player.setShiftKeyDown(false);
        setSignRotation(helper, sign, 15);
        useOn(helper, player, rotator, sign, Direction.UP, TOP_CENTRE);
        helper.assertValueEqual(signRotation(helper, sign), 0, "a centre click on a sign at 15");

        player.setShiftKeyDown(true);
        setSignRotation(helper, sign, 15);
        useOn(helper, player, rotator, sign, Direction.UP, TOP_CENTRE);
        helper.assertValueEqual(signRotation(helper, sign), 14, "a sneaking centre click on a sign at 15");

        // --- rim: four steps, in both senses ---
        player.setShiftKeyDown(false);
        setSignRotation(helper, sign, 15);
        useOn(helper, player, rotator, sign, Direction.UP, westRim);
        helper.assertValueEqual(signRotation(helper, sign), 3, "a rim click on a sign at 15");

        player.setShiftKeyDown(true);
        setSignRotation(helper, sign, 15);
        useOn(helper, player, rotator, sign, Direction.UP, westRim);
        helper.assertValueEqual(signRotation(helper, sign), 11, "a sneaking rim click on a sign at 15");

        // --- a different rim, same four steps: the rim's direction plays no part here ---
        player.setShiftKeyDown(false);
        setSignRotation(helper, sign, 0);
        useOn(helper, player, rotator, sign, Direction.UP, southRim);
        helper.assertValueEqual(signRotation(helper, sign), 4, "a click on the south rim instead of the west one");

        // --- below zero: the only two cases that need the + 16 ---
        player.setShiftKeyDown(true);
        setSignRotation(helper, sign, 0);
        useOn(helper, player, rotator, sign, Direction.UP, TOP_CENTRE);
        helper.assertValueEqual(signRotation(helper, sign), 15,
                "a sneaking centre click on a sign at 0, which computes 0 - 1");

        player.setShiftKeyDown(true);
        setSignRotation(helper, sign, 2);
        useOn(helper, player, rotator, sign, Direction.UP, westRim);
        helper.assertValueEqual(signRotation(helper, sign), 14,
                "a sneaking rim click on a sign at 2, which computes 2 - 4");

        helper.succeed();
    }

    // =====================================================================================
    // THE ITEM ITSELF
    // =====================================================================================

    /**
     * The rotator is registered as a single, netherite grade tool: it stacks to one, it may be
     * enchanted with value 15, it sits in {@code #minecraft:enchantable/durability} so those
     * enchantments are actually offered for it, and it survives exactly 1024 accepted clicks.
     *
     * <p>The durability is pinned through behaviour rather than through {@code getMaxDamage}
     * alone: the click that takes it to one point short of the limit leaves a working tool in the
     * hand, the next one leaves an empty slot. That half is driven off the measured
     * {@code getMaxDamage} and not off {@link #RATED_DURABILITY}, so that a deliberate balance
     * change moves exactly one assertion - the balance pin above it, which is the whole reason the
     * number is named at all - instead of also reddening a wear test that is about
     * {@code hurtAndBreak} and not about the number.
     *
     * <p>The click that changes nothing is checked in the same place, because it is the other half
     * of the {@code newState != state} guard in {@code RotatorItem#useOn} (the
     * {@code newState != null} half belongs to {@code ConsumptionAndDurabilityTests}): aiming a log
     * at the axis it already lies along has to answer PASS and cost nothing. Without that guard the
     * click would spend a point of durability, play the sound and answer SUCCESS - and a consumed
     * action also swallows whatever else the player would have done with that block.
     *
     * <p>The tag membership is a claim about the mod's generated data, not about vanilla: whether
     * an enchanting table then offers Unbreaking is vanilla's business, and enchanting the stack by
     * hand would work with or without the tag, so there is nothing further to assert here that
     * would not be a tautology.
     *
     * <p>What breaks this: changing the durability, the stack size or the enchantability in
     * {@code ModItems.ROTATOR}; dropping the rotator from the durability tag in
     * {@code ModItemTagProvider}, which would make the enchantability number dead weight; anything
     * that stops the item from breaking at its limit, e.g. writing {@code setDamageValue} instead
     * of going through {@code hurtAndBreak}; and dropping the {@code newState != state} guard,
     * which would turn every wasted click into a point of wear.
     */
    public static void wearsOutAtItsRatedDurabilityAndTakesDurabilityEnchantments(GameTestHelper helper) {
        ItemStack probe = new ItemStack(ModItems.ROTATOR);
        helper.assertValueEqual(probe.getMaxDamage(), RATED_DURABILITY, "the rotator's rated durability");
        helper.assertValueEqual(probe.getMaxStackSize(), 1, "the rotator's stack size");

        Enchantable enchantable = probe.get(DataComponents.ENCHANTABLE);
        helper.assertTrue(enchantable != null,
                "the rotator lost its ENCHANTABLE component, so it cannot be enchanted at all");
        helper.assertValueEqual(enchantable.value(), RATED_ENCHANTABILITY, "the rotator's enchantability");
        helper.assertTrue(probe.is(ItemTags.DURABILITY_ENCHANTABLE),
                "the rotator left #minecraft:enchantable/durability, so no durability enchantment "
                        + "is offered for it any more");

        // A player who pays for their tools: the in-level mock is creative, and vanilla refuses to
        // damage anything held by a player with instabuild set.
        ServerPlayer player = mockPlayer(helper, false);
        ItemStack rotator = new ItemStack(ModItems.ROTATOR);

        // --- a click whose computed state is the one the block is already in: refused, and free ---
        setLogAxis(helper, Direction.Axis.X);
        InteractionResult noChange =
                useOn(helper, player, rotator, TARGET, Direction.UP, new Vec3(0.05, 1.0, 0.5));
        helper.assertTrue(noChange == InteractionResult.PASS,
                "aiming a log at the axis it already lies along has to be refused - a consumed "
                        + "action would cost a point of wear and swallow the rest of the "
                        + "interaction - but the result was " + noChange);
        helper.assertTrue(logAxis(helper) == Direction.Axis.X,
                "the log moved off X, so this click was not the no-op this case is about");
        helper.assertValueEqual(rotator.getDamageValue(), 0,
                "wear taken by a click that changed nothing");

        // --- and the two clicks that straddle the limit ---
        int max = probe.getMaxDamage();
        rotator.setDamageValue(max - 2);
        setLogAxis(helper, Direction.Axis.Y);

        InteractionResult secondToLast = useOn(helper, player, rotator, TARGET, Direction.UP, TOP_CENTRE);
        helper.assertTrue(secondToLast == InteractionResult.SUCCESS,
                "the click before the last one was refused, result was " + secondToLast);
        helper.assertTrue(logAxis(helper) == Direction.Axis.Z,
                "the log did not turn, so the wear below cannot be attributed to this click");
        helper.assertTrue(!rotator.isEmpty(),
                "the rotator broke one click early, i.e. before reaching " + max);
        helper.assertValueEqual(rotator.getDamageValue(), max - 1,
                "wear after the second to last click");

        useOn(helper, player, rotator, TARGET, Direction.UP, TOP_CENTRE);
        helper.assertTrue(logAxis(helper) == Direction.Axis.Y,
                "the last click did not turn the log, so a broken rotator would prove nothing");
        helper.assertTrue(rotator.isEmpty(),
                "the rotator survived its " + max + "th point of wear; it is at "
                        + rotator.getDamageValue());

        helper.succeed();
    }

    /**
     * The rotator is obtainable: five iron ingots around an ender pearl, in that shape, resolve
     * through the loaded recipe book to exactly one rotator, and the recipe advancement that puts
     * it into the recipe book is loaded with it.
     *
     * <p>The shape is asserted by looking the grid up in the recipe book rather than by reading the
     * pattern back out of the recipe file - the same statement, but made where it matters. Two near misses hold the ingredient
     * list in place: the same shape with an iron ingot in the middle must not produce a rotator
     * (the ender pearl is load bearing), and the shape with one arm missing must not either (five
     * ingots, not four). Note that a mirrored layout would legitimately match, since vanilla shaped
     * recipes match mirrored, which is why neither near miss is a mirror.
     *
     * <p>What breaks this: changing the pattern or the ingredients in the recipe provider; the
     * recipe not being loaded at all (a broken data generation run, a renamed file); the result
     * item or its count changing; and losing the recipe advancement or its
     * {@code has_iron_ingot} criterion, which would leave the recipe craftable but invisible in the
     * recipe book.
     */
    public static void craftingTakesFiveIronAndOneEnderPearlInThatShape(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        RecipeManager recipes = server.getRecipeManager();
        Identifier recipeId = Identifier.fromNamespaceAndPath(SimpleBuildingGameTests.MOD_ID, "rotator");

        ItemStack empty = ItemStack.EMPTY;
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack pearl = new ItemStack(Items.ENDER_PEARL);

        //  I
        // IEI
        // II
        CraftingInput exact = CraftingInput.of(3, 3, List.of(
                empty, iron, empty,
                iron, pearl, iron,
                iron, iron, empty));

        Optional<RecipeHolder<CraftingRecipe>> found =
                recipes.getRecipeFor(RecipeType.CRAFTING, exact, helper.getLevel());
        helper.assertTrue(found.isPresent(),
                "no crafting recipe at all matches five iron ingots around an ender pearl");
        helper.assertTrue(found.get().id().identifier().equals(recipeId),
                "that layout resolves to " + found.get().id().identifier() + ", not to " + recipeId);

        List<ItemStack> results = resultsOf(helper, found.get());
        helper.assertValueEqual(results.size(), 1,
                "how many results the rotator recipe advertises: " + results);
        helper.assertTrue(results.get(0).is(ModItems.ROTATOR),
                "the recipe produces " + results.get(0) + " instead of a rotator");
        helper.assertValueEqual(results.get(0).getCount(), 1, "how many rotators one craft yields");

        // --- the ender pearl carries the recipe ---
        CraftingInput allIron = CraftingInput.of(3, 3, List.of(
                empty, iron, empty,
                iron, iron, iron,
                iron, iron, empty));
        helper.assertTrue(!resolvesToTheRotatorRecipe(helper, recipes, allIron, recipeId),
                "the same shape with an iron ingot in the middle also crafts a rotator, so the "
                        + "ender pearl is not actually required");

        // --- and so does the fifth ingot ---
        CraftingInput missingArm = CraftingInput.of(3, 3, List.of(
                empty, iron, empty,
                iron, pearl, iron,
                empty, iron, empty));
        helper.assertTrue(!resolvesToTheRotatorRecipe(helper, recipes, missingArm, recipeId),
                "four iron ingots around the pearl already craft a rotator, so the shape is not "
                        + "the one the provider writes");

        // --- the recipe advancement that unlocks it in the recipe book ---
        Identifier advancementId = Identifier.fromNamespaceAndPath(
                SimpleBuildingGameTests.MOD_ID, "recipes/tools/rotator");
        AdvancementHolder unlock = server.getAdvancements().get(advancementId);
        helper.assertTrue(unlock != null,
                "the recipe advancement " + advancementId + " is not loaded, so the recipe never "
                        + "shows up in the recipe book");
        helper.assertTrue(unlock.value().criteria().containsKey("has_iron_ingot"),
                "the unlock no longer triggers on owning an iron ingot; its criteria are "
                        + unlock.value().criteria().keySet());
        ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(Registries.RECIPE, recipeId);
        helper.assertTrue(unlock.value().rewards().recipes().contains(recipeKey),
                "the advancement no longer hands out " + recipeId + "; it grants "
                        + unlock.value().rewards().recipes());

        helper.succeed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /**
     * The ordinary in-level mock player, positioned inside the room and handed back at the end.
     *
     * <p>{@code instabuild} decides whether the rotator's {@code hurtAndBreak} does anything:
     * every test but the durability one sets it, so a long run of clicks cannot wear the tool out
     * mid test and turn a geometry failure into a durability failure.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, boolean instabuild) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.getAbilities().instabuild = instabuild;
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /**
     * Right clicks a block face at a precise spot on that face. The offset is given inside the
     * block (0..1 per axis), which is exactly what {@code getRimDirection} subtracts and reads.
     */
    private static InteractionResult useOn(GameTestHelper helper, ServerPlayer player, ItemStack stack,
                                           BlockPos relativePos, Direction face, Vec3 offsetInBlock) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = helper.absolutePos(relativePos);
        Vec3 hit = new Vec3(pos.getX() + offsetInBlock.x, pos.getY() + offsetInBlock.y, pos.getZ() + offsetInBlock.z);
        BlockHitResult hitResult = new BlockHitResult(hit, face, pos, false);
        return stack.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult));
    }

    /** Puts a log on {@link #TARGET} along {@code start}, clicks it once and states the axis it ends on. */
    private static void assertLogTurns(GameTestHelper helper, ServerPlayer player, ItemStack rotator,
                                       Direction.Axis start, Direction face, Vec3 hit,
                                       Direction.Axis expected, String what) {
        setLogAxis(helper, start);
        useOn(helper, player, rotator, TARGET, face, hit);
        Direction.Axis actual = logAxis(helper);
        helper.assertTrue(actual == expected,
                what + ": a log along " + start + " should have ended up along " + expected
                        + ", but it lies along " + actual);
    }

    private static void setLogAxis(GameTestHelper helper, Direction.Axis axis) {
        helper.setBlock(TARGET, Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, axis));
    }

    private static Direction.Axis logAxis(GameTestHelper helper) {
        return helper.getBlockState(TARGET).getValue(BlockStateProperties.AXIS);
    }

    /** Clicks the piston on {@link #TARGET} once and states the direction it ends up facing. */
    private static void assertPistonTurns(GameTestHelper helper, ServerPlayer player, ItemStack rotator,
                                          Direction face, Vec3 hit, Direction expected, String what) {
        Direction before = pistonFacing(helper);
        useOn(helper, player, rotator, TARGET, face, hit);
        Direction actual = pistonFacing(helper);
        helper.assertTrue(actual == expected,
                what + ": a piston facing " + before + " clicked on its " + face + " face should now "
                        + "face " + expected + ", but it faces " + actual);
    }

    private static void setPistonFacing(GameTestHelper helper, Direction facing) {
        helper.setBlock(TARGET, Blocks.PISTON.defaultBlockState()
                .setValue(BlockStateProperties.FACING, facing));
    }

    private static Direction pistonFacing(GameTestHelper helper) {
        return helper.getBlockState(TARGET).getValue(BlockStateProperties.FACING);
    }

    /**
     * Puts a piston on {@link #TARGET} facing {@code start}, clicks the given spot on the given face
     * once and states the direction it has to end up pointing at.
     */
    private static void assertPistonTurnsFrom(GameTestHelper helper, ServerPlayer player, ItemStack rotator,
                                              Direction start, Direction face, Vec3 hit,
                                              Direction expected, String what) {
        setPistonFacing(helper, start);
        assertPistonTurns(helper, player, rotator, face, hit, expected, what);
    }

    private static void setFurnaceFacing(GameTestHelper helper, Direction facing) {
        helper.setBlock(TARGET, Blocks.FURNACE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
    }

    private static Direction furnaceFacing(GameTestHelper helper) {
        return helper.getBlockState(TARGET).getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    private static void setHopperFacing(GameTestHelper helper, Direction facing) {
        helper.setBlock(TARGET, Blocks.HOPPER.defaultBlockState()
                .setValue(BlockStateProperties.FACING_HOPPER, facing));
    }

    private static Direction hopperFacing(GameTestHelper helper) {
        return helper.getBlockState(TARGET).getValue(BlockStateProperties.FACING_HOPPER);
    }

    private static void setSignRotation(GameTestHelper helper, BlockPos pos, int rotation) {
        helper.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState()
                .setValue(BlockStateProperties.ROTATION_16, rotation));
    }

    private static int signRotation(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos).getValue(BlockStateProperties.ROTATION_16);
    }

    /** Whether the loaded recipe book answers this grid with the rotator's own recipe. */
    private static boolean resolvesToTheRotatorRecipe(GameTestHelper helper, RecipeManager recipes,
                                                      CraftingInput input, Identifier recipeId) {
        Optional<RecipeHolder<CraftingRecipe>> found =
                recipes.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        return found.isPresent() && found.get().id().identifier().equals(recipeId);
    }

    /**
     * The stacks a loaded recipe advertises as its result. Read through {@code RecipeDisplay}
     * rather than through {@code Recipe#assemble}, because {@code assemble}'s parameter list
     * differs between the Minecraft lines this shared source tree is compiled against, while the
     * display path is the same on both - the same reason {@code DataIntegrityTests} uses it.
     */
    private static List<ItemStack> resultsOf(GameTestHelper helper, RecipeHolder<?> holder) {
        ContextMap context = SlotDisplayContext.fromLevel(helper.getLevel());
        List<ItemStack> stacks = new ArrayList<>();
        for (RecipeDisplay display : holder.value().display()) {
            stacks.addAll(display.result().resolveForStacks(context));
        }
        return stacks;
    }
}
