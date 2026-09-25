package com.simplebuilding.gametest;

import com.simplebuilding.blueprint.BlueprintBuilder;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.util.WandUndo;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The building wand's shapes and helpers decided by the owner on 2026-09-25: Linear (a line while
 * sneaking), Bridge (a right click into the air), Cover (only in front of the clicked kind),
 * placing blocks the way a player would, undo of the last action, and the octant fill and roof
 * modes that run through the blueprint planner.
 *
 * <p>Each test drives the real item hooks ({@code useOn}, {@code use}, {@code inventoryTick}) on a
 * survival-paying mock player and compares the world afterwards cell by cell. Where the client
 * preview has a shared half ({@code getPreviewStates}, {@code getBridgePreview}), the test also
 * states that the preview shows exactly the cells and states the click then builds.
 */
public final class WandModeTests {

    private WandModeTests() {
    }

    /** Loop guard for the wand tick driver, not a budget: every run here ends in a few ticks. */
    private static final int TICK_CAP = 200;

    // =====================================================================================
    // LINEAR, BRIDGE, COVER
    // =====================================================================================

    /**
     * Linear while sneaking builds a straight line away from the clicked face, twice the diameter
     * of the configured plane (radius 1 = 6 blocks), and ends in front of the first occupied block.
     * Without sneaking the same wand still builds the square (pinned elsewhere).
     *
     * <p><strong>What breaks this test:</strong> dropping the sneak check or the Linear check in
     * {@code Plan.forClick}, stepping the line along the wrong direction, ignoring the obstacle
     * ({@code freeRun}), or a preview that disagrees with the build.
     */
    public static void linearWhileSneakingBuildsTheLineAwayFromTheClickedFace(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        BlockPos anchor = new BlockPos(3, 1, 3);
        clearRoom(helper);
        helper.setBlock(anchor, Blocks.STONE);
        helper.setBlock(anchor.above(5), Blocks.STONE); // the line has to stop below this one

        ItemStack wand = wand(helper, ModItems.DIAMOND_BUILDING_WAND, 1, ModEnchantments.LINEAR);
        stock(player, wand, new ItemStack(Items.GLASS, 64));
        player.setShiftKeyDown(true);

        Set<BlockPos> previewed = absoluteToRelative(helper, BuildingWandItem.getPreviewStates(helper.getLevel(), player, wand,
                helper.absolutePos(anchor), Direction.UP, ModItems.DIAMOND_BUILDING_WAND.getWandSquareDiameter()).keySet());
        InteractionResult armed = click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        helper.assertTrue(armed == InteractionResult.CONSUME, "the sneaking Linear wand did not arm, it returned " + armed);
        runUntilIdle(helper, player, wand);
        player.setShiftKeyDown(false);

        Set<BlockPos> expected = new HashSet<>();
        for (int dy = 1; dy <= 4; dy++) {
            expected.add(anchor.above(dy));
        }
        Assertions.valueEqual(helper, placedGlass(helper), expected,
                "Linear + sneaking did not build the four free blocks of the line above the clicked face");
        Assertions.valueEqual(helper, previewed, expected, "the Linear preview does not show the line the click builds");
        Assertions.valueEqual(helper, countIn(player, Items.GLASS), 64 - 4, "the line did not pay one glass per block");
        TestCleanup.succeed(helper);
    }

    /**
     * Bridge: a right click into the air (not sneaking) builds from the block under the player's
     * feet straight ahead in the facing direction, at that block's height, up to twice the plane
     * diameter and in front of the first occupied block. Without the enchantment the click is
     * passed on; standing in the air there is nothing to start from.
     *
     * <p><strong>What breaks this test:</strong> dropping the Bridge check in {@code use}, starting
     * from the player's position instead of the block underfoot, a wrong direction, ignoring the
     * obstacle, or a bridge preview that disagrees with the build.
     */
    public static void bridgeRunsFromTheBlockUnderfootInTheFacingDirection(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        clearRoom(helper);
        BlockPos ground = new BlockPos(1, 1, 3);
        helper.setBlock(ground, Blocks.STONE);
        helper.setBlock(new BlockPos(6, 1, 3), Blocks.STONE); // obstacle: the bridge ends at x = 5
        Vec3 feet = helper.absoluteVec(new Vec3(1.5, 2.0, 3.5));
        player.snapTo(feet.x, feet.y, feet.z, 270.0F, 0.0F); // yaw 270 = facing east

        ItemStack plainWand = wand(helper, ModItems.DIAMOND_BUILDING_WAND, 1, null);
        stock(player, plainWand, new ItemStack(Items.GLASS, 64));
        helper.assertTrue(plainWand.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND) == InteractionResult.PASS,
                "a wand without Bridge answered a right click into the air");

        ItemStack wand = wand(helper, ModItems.DIAMOND_BUILDING_WAND, 1, ModEnchantments.BRIDGE);
        stock(player, wand, new ItemStack(Items.GLASS, 64));
        Set<BlockPos> previewed = absoluteToRelative(helper, BuildingWandItem.getBridgePreview(helper.getLevel(), player, wand,
                ModItems.DIAMOND_BUILDING_WAND.getWandSquareDiameter()).keySet());
        InteractionResult used = wand.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(used == InteractionResult.SUCCESS, "the Bridge wand refused the air click, it returned " + used);
        runUntilIdle(helper, player, wand);

        Set<BlockPos> expected = new HashSet<>();
        for (int x = 2; x <= 5; x++) {
            expected.add(new BlockPos(x, 1, 3));
        }
        Assertions.valueEqual(helper, placedGlass(helper), expected,
                "Bridge did not build the four free blocks east of the block underfoot, at its height");
        Assertions.valueEqual(helper, previewed, expected, "the Bridge preview does not show the bridge the click builds");

        // --- in the air there is no block to start from ---
        Vec3 air = helper.absoluteVec(new Vec3(3.5, 5.0, 5.5));
        player.snapTo(air.x, air.y, air.z, 270.0F, 0.0F);
        helper.assertTrue(wand.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL,
                "Bridge started a bridge although the player stands on nothing");
        TestCleanup.succeed(helper);
    }

    /**
     * Cover: the plane only grows in front of blocks of the clicked kind, and only as far as those
     * cells hang together with the centre (4-neighbourhood). A stone floor with a dirt strip through
     * it and air at its other edge: clicked stone covers the stone part up to the strip, neither the
     * dirt nor the air beyond the floor, although both lie inside the 5x5 the plain wand would fill.
     *
     * <p><strong>What breaks this test:</strong> dropping the Cover mode (the whole 5x5 is built),
     * testing the support against the wrong block or the wrong side, or flooding across cells whose
     * support is not the clicked kind.
     */
    public static void coverOnlyGrowsTheSurfaceOfTheClickedKind(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        clearRoom(helper);
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 1, z), x == 4 ? Blocks.DIRT : Blocks.STONE);
            }
        }
        ItemStack wand = wand(helper, ModItems.DIAMOND_BUILDING_WAND, 2, ModEnchantments.COVER);
        stock(player, wand, new ItemStack(Items.GLASS, 64));
        BlockPos anchor = new BlockPos(2, 1, 3);

        Set<BlockPos> previewed = absoluteToRelative(helper, BuildingWandItem.getPreviewStates(helper.getLevel(), player, wand,
                helper.absolutePos(anchor), Direction.UP, ModItems.DIAMOND_BUILDING_WAND.getWandSquareDiameter()).keySet());
        helper.assertTrue(click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5)) == InteractionResult.CONSUME,
                "the Cover wand did not arm");
        runUntilIdle(helper, player, wand);

        Set<BlockPos> expected = new HashSet<>();
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 5; z++) {
                expected.add(new BlockPos(x, 2, z));
            }
        }
        Assertions.valueEqual(helper, placedGlass(helper), expected,
                "Cover did not cover exactly the stone connected to the clicked block (x 1..3)");
        Assertions.valueEqual(helper, previewed, expected, "the Cover preview does not show what the click builds");
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // ORIENTATION
    // =====================================================================================

    /**
     * The wand sets blocks the way a player clicking the same face would: stairs face the player's
     * direction and take the upper half when the click was in the upper half of a side face, logs
     * lie along the axis of the clicked face - unless the clicked block is of the same kind, whose
     * orientation is then carried on.
     *
     * <p><strong>What breaks this test:</strong> placing default states again, dropping the
     * relative hit position (every stair bottom), losing the player direction, or dropping
     * {@code copyOrientation}.
     */
    public static void wandSetsStairsAndLogsLikeThePlayerWould(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        clearRoom(helper);
        ItemStack wand = wand(helper, ModItems.DIAMOND_BUILDING_WAND, 1, null);

        // --- stairs on a top face: facing the player's direction (north), bottom half ---
        Vec3 at = player.position();
        player.snapTo(at.x, at.y, at.z, 180.0F, 0.0F);
        BlockPos floor = new BlockPos(3, 1, 3);
        helper.setBlock(floor, Blocks.STONE);
        stock(player, wand, new ItemStack(Items.OAK_STAIRS, 64));
        click(helper, player, wand, floor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        runUntilIdle(helper, player, wand);
        for (BlockPos pos : BlockPos.betweenClosed(floor.offset(-1, 1, -1), floor.offset(1, 1, 1))) {
            BlockState state = helper.getBlockState(pos);
            helper.assertTrue(state.is(Blocks.OAK_STAIRS) && state.getValue(StairBlock.FACING) == Direction.NORTH
                            && state.getValue(StairBlock.HALF) == Half.BOTTOM,
                    "a stair from a top face click is not a bottom stair facing north at " + pos + ": " + state);
        }

        // --- stairs on the upper half of a side face: top half ---
        clearRoom(helper);
        BlockPos wall = new BlockPos(2, 3, 3);
        helper.setBlock(wall, Blocks.STONE);
        stock(player, wand, new ItemStack(Items.OAK_STAIRS, 64));
        click(helper, player, wand, wall, Direction.EAST, new Vec3(1.0, 0.8, 0.5));
        runUntilIdle(helper, player, wand);
        BlockState upper = helper.getBlockState(wall.east());
        helper.assertTrue(upper.is(Blocks.OAK_STAIRS) && upper.getValue(StairBlock.HALF) == Half.TOP,
                "a click into the upper half of a side face did not give a top stair: " + upper);

        // --- logs lie along the clicked face's axis ---
        clearRoom(helper);
        helper.setBlock(wall, Blocks.STONE);
        stock(player, wand, new ItemStack(Items.OAK_LOG, 64));
        click(helper, player, wand, wall, Direction.EAST, new Vec3(1.0, 0.5, 0.5));
        runUntilIdle(helper, player, wand);
        BlockState log = helper.getBlockState(wall.east());
        helper.assertTrue(log.is(Blocks.OAK_LOG) && log.getValue(BlockStateProperties.AXIS) == Direction.Axis.X,
                "a log set against an east face does not lie along x: " + log);

        // --- ... unless the clicked block is a log itself: its axis is carried on ---
        clearRoom(helper);
        helper.setBlock(floor, Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Z));
        stock(player, wand, new ItemStack(Items.OAK_LOG, 64));
        click(helper, player, wand, floor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        runUntilIdle(helper, player, wand);
        BlockState carried = helper.getBlockState(floor.above());
        helper.assertTrue(carried.is(Blocks.OAK_LOG) && carried.getValue(BlockStateProperties.AXIS) == Direction.Axis.Z,
                "logs set on a lying log did not carry its axis on: " + carried);

        // --- the preview shows the orientation the click builds ---
        clearRoom(helper);
        helper.setBlock(floor, Blocks.STONE);
        stock(player, wand, new ItemStack(Items.OAK_STAIRS, 64));
        Map<BlockPos, BlockState> preview = BuildingWandItem.getPreviewStates(helper.getLevel(), player, wand,
                helper.absolutePos(floor), Direction.UP, new Vec3(0.5, 1.0, 0.5), ModItems.DIAMOND_BUILDING_WAND.getWandSquareDiameter());
        click(helper, player, wand, floor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        runUntilIdle(helper, player, wand);
        for (Map.Entry<BlockPos, BlockState> entry : preview.entrySet()) {
            Assertions.valueEqual(helper, helper.getLevel().getBlockState(entry.getKey()), entry.getValue(),
                    "the preview drew a different state than the click placed at " + entry.getKey());
        }
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // UNDO
    // =====================================================================================

    /**
     * Sneak + right click into the air takes back the last action only: its blocks disappear where
     * they are still the placed block, their items come back, a block changed since stays and is
     * not paid for, the action before is untouched, durability is not refunded and a second undo
     * finds nothing.
     *
     * <p><strong>What breaks this test:</strong> not recording placements, recording across actions,
     * clearing changed cells, refunding the changed cell or the durability, or keeping the record
     * after an undo.
     */
    public static void undoTakesBackOnlyTheLastActionAndOnlyUnchangedBlocks(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        clearRoom(helper);
        WandUndo.forget(player.getUUID());
        ItemStack wand = wand(helper, ModItems.DIAMOND_BUILDING_WAND, 1, null);
        stock(player, wand, new ItemStack(Items.GLASS, 64));
        BlockPos first = new BlockPos(2, 1, 2);
        BlockPos second = new BlockPos(2, 4, 2);
        helper.setBlock(first, Blocks.STONE);
        helper.setBlock(second, Blocks.STONE);

        click(helper, player, wand, first, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        runUntilIdle(helper, player, wand);
        click(helper, player, wand, second, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        runUntilIdle(helper, player, wand);
        Assertions.valueEqual(helper, countIn(player, Items.GLASS), 64 - 18, "the two planes did not cost 18 glass");
        int damage = wand.getDamageValue();

        BlockPos changed = second.offset(1, 1, 1);
        helper.setBlock(changed, Blocks.OAK_PLANKS);

        player.setShiftKeyDown(true);
        InteractionResult undone = wand.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(undone == InteractionResult.SUCCESS, "sneak + air click with the wand returned " + undone);
        for (BlockPos pos : BlockPos.betweenClosed(second.offset(-1, 1, -1), second.offset(1, 1, 1))) {
            if (pos.equals(changed)) {
                helper.assertBlockPresent(Blocks.OAK_PLANKS, pos);
            } else {
                helper.assertTrue(helper.getBlockState(pos).isAir(), "undo left the wand's glass at " + pos);
            }
        }
        for (BlockPos pos : BlockPos.betweenClosed(first.offset(-1, 1, -1), first.offset(1, 1, 1))) {
            helper.assertBlockPresent(Blocks.GLASS, pos);
        }
        Assertions.valueEqual(helper, countIn(player, Items.GLASS), 64 - 18 + 8,
                "undo did not hand back exactly the eight glass still standing");
        Assertions.valueEqual(helper, wand.getDamageValue(), damage, "undo refunded durability");

        // --- nothing left to undo: the earlier plane stays ---
        wand.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
        helper.assertBlockPresent(Blocks.GLASS, first.above());
        Assertions.valueEqual(helper, countIn(player, Items.GLASS), 64 - 18 + 8, "a second undo gave something back");
        helper.assertFalse(WandUndo.hasUndo(player), "the record survived its undo");
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // OCTANT FILL AND ROOF
    // =====================================================================================

    /**
     * Wand in the main hand, octant with a selection in the off hand: a click fills the octant's
     * shape through the blueprint planner - Hollow keeps only the shell, Layer Mode builds one
     * layer per click, a missing block first warns (nothing placed) and a second click builds what
     * is there, the tier caps the edge, and undo takes the whole fill back.
     *
     * <p><strong>What breaks this test:</strong> not routing the click to {@code ShapeFill}, ignoring
     * Hollow or Layer Mode, dropping the two-click rule or the edge cap, or planner placements that
     * the undo record does not see.
     */
    public static void octantInTheOffHandFillsItsShapeWithTheWand(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        clearRoom(helper);
        WandUndo.forget(player.getUUID());
        BlockPos anchor = new BlockPos(6, 1, 6);
        helper.setBlock(anchor, Blocks.STONE);
        ItemStack wand = wand(helper, ModItems.DIAMOND_BUILDING_WAND, 1, null);

        // --- hollow 3x3x3: the shell of 26 ---
        ItemStack octant = octant(helper, new BlockPos(2, 1, 2), new BlockPos(4, 3, 4), "CUBOID", true, false);
        stock(player, wand, new ItemStack(Items.GLASS, 64));
        player.setItemInHand(InteractionHand.OFF_HAND, octant);
        click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        BlueprintBuilder.completeJob(player);
        Assertions.valueEqual(helper, glassIn(helper, 2, 1, 2, 4, 3, 4), 26, "the hollow cube is not its 26 shell blocks");
        helper.assertTrue(helper.getBlockState(new BlockPos(3, 2, 3)).isAir(), "the hollow cube has a filled centre");
        Assertions.valueEqual(helper, countIn(player, Items.GLASS), 64 - 26, "the fill did not pay one glass per block");
        Assertions.valueEqual(helper, wand.getDamageValue(), 26, "the fill did not wear the wand once per block");

        // --- undo takes the whole fill back ---
        player.setShiftKeyDown(true);
        wand.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
        Assertions.valueEqual(helper, glassIn(helper, 2, 1, 2, 4, 3, 4), 0, "undo left part of the octant fill standing");
        Assertions.valueEqual(helper, countIn(player, Items.GLASS), 64, "undo did not hand the 26 glass back");

        // --- layer mode: one layer per click, bottom up ---
        ItemStack layered = octant(helper, new BlockPos(2, 1, 2), new BlockPos(4, 3, 4), "CUBOID", false, true);
        player.setItemInHand(InteractionHand.OFF_HAND, layered);
        click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        BlueprintBuilder.completeJob(player);
        Assertions.valueEqual(helper, glassIn(helper, 2, 1, 2, 4, 1, 4), 9, "layer mode did not build the bottom layer");
        Assertions.valueEqual(helper, glassIn(helper, 2, 2, 2, 4, 3, 4), 0, "layer mode built more than one layer");
        click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        BlueprintBuilder.completeJob(player);
        Assertions.valueEqual(helper, glassIn(helper, 2, 2, 2, 4, 2, 4), 9, "the second layer-mode click did not build layer two");
        Assertions.valueEqual(helper, glassIn(helper, 2, 3, 2, 4, 3, 4), 0, "the second layer-mode click built layer three too");

        // --- missing material: the first click warns, the second builds what is there ---
        clearRoom(helper);
        helper.setBlock(anchor, Blocks.STONE);
        ItemStack solid = octant(helper, new BlockPos(2, 1, 2), new BlockPos(3, 1, 3), "CUBOID", false, false);
        stock(player, wand, new ItemStack(Items.GLASS, 3));
        player.setItemInHand(InteractionHand.OFF_HAND, solid);
        click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        BlueprintBuilder.completeJob(player);
        Assertions.valueEqual(helper, glassIn(helper, 2, 1, 2, 3, 1, 3), 0, "the warning click already built");
        click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        BlueprintBuilder.completeJob(player);
        Assertions.valueEqual(helper, glassIn(helper, 2, 1, 2, 3, 1, 3), 3, "the confirming click did not build the three there are");

        // --- the tier caps the longest edge: a copper wand builds at most 16 ---
        ItemStack copper = wand(helper, ModItems.COPPER_BUILDING_WAND, 1, null);
        stock(player, copper, new ItemStack(Items.GLASS, 64));
        ItemStack tooLong = octant(helper, new BlockPos(0, 1, 0), new BlockPos(16, 1, 0), "CUBOID", false, false);
        player.setItemInHand(InteractionHand.OFF_HAND, tooLong);
        InteractionResult refused = click(helper, player, copper, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        helper.assertTrue(refused == InteractionResult.FAIL, "a copper wand accepted a 17 block long selection: " + refused);
        Assertions.valueEqual(helper, countIn(player, Items.GLASS), 64, "the refused fill spent glass");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        TestCleanup.succeed(helper);
    }

    /**
     * With stairs as material, a prism (tip up) becomes a roof: stairs on both slopes with their
     * high side towards the ridge, a one block wide ridge of bottom slabs of the same wood, and
     * nothing inside.
     *
     * <p><strong>What breaks this test:</strong> filling the prism instead, stairs facing away from
     * the ridge, a missing slab ridge, or a slab looked up from the wrong block.
     */
    public static void roofModeLaysStairsTowardsTheRidgeWithSlabsOnTop(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        clearRoom(helper);
        BlockPos anchor = new BlockPos(6, 1, 6);
        helper.setBlock(anchor, Blocks.STONE);
        ItemStack wand = wand(helper, ModItems.DIAMOND_BUILDING_WAND, 1, null);
        // x 2..4 (3 wide), z 1..5 (5 long): the ridge runs along z at x = 3, height 2.
        ItemStack prism = octant(helper, new BlockPos(2, 1, 1), new BlockPos(4, 2, 5), "TRIANGLE", false, false);
        stock(player, wand, new ItemStack(Items.OAK_STAIRS, 64), new ItemStack(Items.OAK_SLAB, 64));
        player.setItemInHand(InteractionHand.OFF_HAND, prism);
        click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        BlueprintBuilder.completeJob(player);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

        for (int z = 1; z <= 5; z++) {
            BlockState west = helper.getBlockState(new BlockPos(2, 1, z));
            BlockState east = helper.getBlockState(new BlockPos(4, 1, z));
            BlockState ridge = helper.getBlockState(new BlockPos(3, 2, z));
            helper.assertTrue(west.is(Blocks.OAK_STAIRS) && west.getValue(StairBlock.FACING) == Direction.EAST
                    && west.getValue(StairBlock.HALF) == Half.BOTTOM, "the west slope at z=" + z + " is not a stair rising east: " + west);
            helper.assertTrue(east.is(Blocks.OAK_STAIRS) && east.getValue(StairBlock.FACING) == Direction.WEST,
                    "the east slope at z=" + z + " is not a stair rising west: " + east);
            helper.assertTrue(ridge.is(Blocks.OAK_SLAB) && ridge.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.BOTTOM,
                    "the ridge at z=" + z + " is not a bottom oak slab: " + ridge);
            helper.assertTrue(helper.getBlockState(new BlockPos(3, 1, z)).isAir(), "the roof was filled underneath at z=" + z);
        }
        Assertions.valueEqual(helper, countIn(player, Items.OAK_STAIRS), 64 - 10, "the roof did not use ten stairs");
        Assertions.valueEqual(helper, countIn(player, Items.OAK_SLAB), 64 - 5, "the ridge did not use five slabs");
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        // Out of the way of every cell the tests build, so no placement is blocked by the player.
        Vec3 pos = helper.absoluteVec(new Vec3(0.5, 5.0, 7.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.getAbilities().instabuild = false;
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    private static void clearRoom(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(0, 1, 0), new BlockPos(7, 7, 7))) {
            helper.setBlock(pos, Blocks.AIR);
        }
    }

    private static ItemStack wand(GameTestHelper helper, BuildingWandItem item, int radius, ResourceKey<Enchantment> enchantment) {
        ItemStack wand = new ItemStack(item);
        CompoundTag settings = new CompoundTag();
        settings.putInt("SettingsRadius", radius);
        settings.putInt("SettingsAxis", 0);
        wand.set(DataComponents.CUSTOM_DATA, CustomData.of(settings));
        if (enchantment != null) {
            wand.enchant(enchantment(helper, enchantment), 1);
        }
        return wand;
    }

    private static ItemStack octant(GameTestHelper helper, BlockPos from, BlockPos to, String shape, boolean hollow, boolean layer) {
        ItemStack octant = new ItemStack(ModItems.OCTANT);
        CompoundTag nbt = new CompoundTag();
        BlockPos a = helper.absolutePos(from);
        BlockPos b = helper.absolutePos(to);
        nbt.putIntArray("Pos1", new int[]{a.getX(), a.getY(), a.getZ()});
        nbt.putIntArray("Pos2", new int[]{b.getX(), b.getY(), b.getZ()});
        nbt.putString("Shape", shape);
        nbt.putBoolean("Hollow", hollow);
        nbt.putBoolean("LayerMode", layer);
        nbt.putString("FillOrder", "BOTTOM_UP");
        octant.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        return octant;
    }

    /** Wand in the selected slot 0, supplies from slot 1 on, empty off hand. */
    private static void stock(ServerPlayer player, ItemStack wand, ItemStack... supplies) {
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, wand);
        for (int i = 0; i < supplies.length; i++) {
            player.getInventory().setItem(i + 1, supplies[i]);
        }
    }

    private static InteractionResult click(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos relative,
                                           Direction face, Vec3 hitRel) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(new Vec3(pos.getX() + hitRel.x, pos.getY() + hitRel.y, pos.getZ() + hitRel.z), face, pos, false);
        return stack.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static void runUntilIdle(GameTestHelper helper, ServerPlayer player, ItemStack wand) {
        BuildingWandItem item = (BuildingWandItem) wand.getItem();
        int ticks = 0;
        while (wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr("Active", false) && ticks < TICK_CAP) {
            item.inventoryTick(wand, helper.getLevel(), player, EquipmentSlot.MAINHAND);
            ticks++;
        }
        helper.assertTrue(ticks < TICK_CAP, "the wand never finished within " + TICK_CAP + " ticks");
    }

    private static Set<BlockPos> placedGlass(GameTestHelper helper) {
        Set<BlockPos> out = new HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(0, 1, 0), new BlockPos(7, 7, 7))) {
            if (helper.getBlockState(pos).is(Blocks.GLASS)) {
                out.add(pos.immutable());
            }
        }
        return out;
    }

    private static int glassIn(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1) {
        int n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(x0, y0, z0), new BlockPos(x1, y1, z1))) {
            if (helper.getBlockState(pos).is(Blocks.GLASS)) {
                n++;
            }
        }
        return n;
    }

    /** The preview's absolute cells as room coordinates (the room is unrotated, so a plain offset). */
    private static Set<BlockPos> absoluteToRelative(GameTestHelper helper, Set<BlockPos> absolute) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        Set<BlockPos> out = new HashSet<>();
        for (BlockPos pos : absolute) {
            out.add(pos.subtract(origin));
        }
        return out;
    }

    private static int countIn(ServerPlayer player, Item item) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }
}
