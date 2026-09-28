package com.simplebuilding.gametest;

import com.simplebuilding.blueprint.BlueprintBuilder;
import com.simplebuilding.blueprint.ShapeFill;
import com.simplebuilding.dev.testcentre.TcContext;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
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
        helper.assertValueEqual(placedGlass(helper), expected,
                "Linear + sneaking did not build the four free blocks of the line above the clicked face");
        helper.assertValueEqual(previewed, expected, "the Linear preview does not show the line the click builds");
        helper.assertValueEqual(countIn(player, Items.GLASS), 64 - 4, "the line did not pay one glass per block");
        helper.succeed();
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
        helper.assertValueEqual(placedGlass(helper), expected,
                "Bridge did not build the four free blocks east of the block underfoot, at its height");
        helper.assertValueEqual(previewed, expected, "the Bridge preview does not show the bridge the click builds");

        // --- in the air there is no block to start from ---
        Vec3 air = helper.absoluteVec(new Vec3(3.5, 5.0, 5.5));
        player.snapTo(air.x, air.y, air.z, 270.0F, 0.0F);
        helper.assertTrue(wand.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL,
                "Bridge started a bridge although the player stands on nothing");
        helper.succeed();
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
        helper.assertValueEqual(placedGlass(helper), expected,
                "Cover did not cover exactly the stone connected to the clicked block (x 1..3)");
        helper.assertValueEqual(previewed, expected, "the Cover preview does not show what the click builds");
        helper.succeed();
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
            helper.assertValueEqual(helper.getLevel().getBlockState(entry.getKey()), entry.getValue(),
                    "the preview drew a different state than the click placed at " + entry.getKey());
        }
        helper.succeed();
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
        helper.assertValueEqual(countIn(player, Items.GLASS), 64 - 18, "the two planes did not cost 18 glass");
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
        helper.assertValueEqual(countIn(player, Items.GLASS), 64 - 18 + 8,
                "undo did not hand back exactly the eight glass still standing");
        helper.assertValueEqual(wand.getDamageValue(), damage, "undo refunded durability");

        // --- nothing left to undo: the earlier plane stays ---
        wand.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
        helper.assertBlockPresent(Blocks.GLASS, first.above());
        helper.assertValueEqual(countIn(player, Items.GLASS), 64 - 18 + 8, "a second undo gave something back");
        helper.assertFalse(WandUndo.hasUndo(player), "the record survived its undo");
        helper.succeed();
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
        helper.assertValueEqual(glassIn(helper, 2, 1, 2, 4, 3, 4), 26, "the hollow cube is not its 26 shell blocks");
        helper.assertTrue(helper.getBlockState(new BlockPos(3, 2, 3)).isAir(), "the hollow cube has a filled centre");
        helper.assertValueEqual(countIn(player, Items.GLASS), 64 - 26, "the fill did not pay one glass per block");
        helper.assertValueEqual(wand.getDamageValue(), 26, "the fill did not wear the wand once per block");

        // --- undo takes the whole fill back ---
        player.setShiftKeyDown(true);
        wand.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
        helper.assertValueEqual(glassIn(helper, 2, 1, 2, 4, 3, 4), 0, "undo left part of the octant fill standing");
        helper.assertValueEqual(countIn(player, Items.GLASS), 64, "undo did not hand the 26 glass back");

        // --- layer mode: one layer per click, bottom up ---
        ItemStack layered = octant(helper, new BlockPos(2, 1, 2), new BlockPos(4, 3, 4), "CUBOID", false, true);
        player.setItemInHand(InteractionHand.OFF_HAND, layered);
        click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        BlueprintBuilder.completeJob(player);
        helper.assertValueEqual(glassIn(helper, 2, 1, 2, 4, 1, 4), 9, "layer mode did not build the bottom layer");
        helper.assertValueEqual(glassIn(helper, 2, 2, 2, 4, 3, 4), 0, "layer mode built more than one layer");
        click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        BlueprintBuilder.completeJob(player);
        helper.assertValueEqual(glassIn(helper, 2, 2, 2, 4, 2, 4), 9, "the second layer-mode click did not build layer two");
        helper.assertValueEqual(glassIn(helper, 2, 3, 2, 4, 3, 4), 0, "the second layer-mode click built layer three too");

        // --- missing material: the first click warns, the second builds what is there ---
        clearRoom(helper);
        helper.setBlock(anchor, Blocks.STONE);
        ItemStack solid = octant(helper, new BlockPos(2, 1, 2), new BlockPos(3, 1, 3), "CUBOID", false, false);
        stock(player, wand, new ItemStack(Items.GLASS, 3));
        player.setItemInHand(InteractionHand.OFF_HAND, solid);
        click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        BlueprintBuilder.completeJob(player);
        helper.assertValueEqual(glassIn(helper, 2, 1, 2, 3, 1, 3), 0, "the warning click already built");
        click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        BlueprintBuilder.completeJob(player);
        helper.assertValueEqual(glassIn(helper, 2, 1, 2, 3, 1, 3), 3, "the confirming click did not build the three there are");

        // --- the tier caps the longest edge: a copper wand builds at most 16 ---
        ItemStack copper = wand(helper, ModItems.COPPER_BUILDING_WAND, 1, null);
        stock(player, copper, new ItemStack(Items.GLASS, 64));
        ItemStack tooLong = octant(helper, new BlockPos(0, 1, 0), new BlockPos(16, 1, 0), "CUBOID", false, false);
        player.setItemInHand(InteractionHand.OFF_HAND, tooLong);
        InteractionResult refused = click(helper, player, copper, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        helper.assertTrue(refused == InteractionResult.FAIL, "a copper wand accepted a 17 block long selection: " + refused);
        helper.assertValueEqual(countIn(player, Items.GLASS), 64, "the refused fill spent glass");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        helper.succeed();
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
        helper.assertValueEqual(countIn(player, Items.OAK_STAIRS), 64 - 10, "the roof did not use ten stairs");
        helper.assertValueEqual(countIn(player, Items.OAK_SLAB), 64 - 5, "the ridge did not use five slabs");
        helper.succeed();
    }

    /**
     * Bridge through the real use path, the way the owner met it: the player stands on a floor that
     * still runs on for two blocks, looks east into the air and right-clicks. The client sends a
     * {@code ServerboundUseItemPacket}; the server hands it to {@code ServerPlayerGameMode#useItem} and
     * so to {@code BuildingWandItem#use}. The bridge starts at the edge of the floor ahead and spans the
     * gap up to the obstacle.
     *
     * <p>Before 2026-09-25 the bridge only counted the cells right in front of the block underfoot:
     * floor there meant length 0, and the click did nothing and said nothing - on the everywhere-flat
     * test centre the Bridge wand looked broken.
     *
     * <p><strong>What breaks this test:</strong> a bridge that stops at the floor ahead instead of
     * starting at its edge, a use path that never reaches {@code use} (the packet handler, a cooldown,
     * a client-only gate), or a bridge running past the obstacle.
     */
    public static void bridgeFromTheUsePacketStartsAtTheEdgeOfTheFloorAhead(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        clearRoom(helper);
        for (int x = 0; x <= 2; x++) {
            helper.setBlock(new BlockPos(x, 1, 3), Blocks.STONE); // the floor: underfoot at x = 0, two more ahead
        }
        helper.setBlock(new BlockPos(7, 1, 3), Blocks.STONE); // far bank: the gap is x = 3..6
        Vec3 feet = helper.absoluteVec(new Vec3(0.5, 2.0, 3.5));
        player.snapTo(feet.x, feet.y, feet.z, 0.0F, 0.0F);

        ItemStack wand = wand(helper, ModItems.ENDERITE_BUILDING_WAND, 1, ModEnchantments.BRIDGE);
        stock(player, wand, new ItemStack(Items.GLASS, 64));
        // yaw 270 = facing east, as the client puts it into the packet
        player.connection.handleUseItem(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 1, 270.0F, 0.0F));
        runUntilIdle(helper, player, wand);

        Set<BlockPos> expected = new HashSet<>();
        for (int x = 3; x <= 6; x++) {
            expected.add(new BlockPos(x, 1, 3));
        }
        Set<BlockPos> placed = placedGlass(helper);
        helper.assertTrue(placed.equals(expected), "the use packet did not bridge the gap x=3..6 beyond the floor ahead: " + placed);

        // --- a floor without a gap in reach: refused, nothing built ---
        clearRoom(helper);
        for (int x = 0; x <= 7; x++) {
            helper.setBlock(new BlockPos(x, 1, 3), Blocks.STONE);
        }
        stock(player, wand, new ItemStack(Items.GLASS, 64));
        helper.assertTrue(wand.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL,
                "Bridge accepted a click on a floor that has no gap within reach");
        helper.assertTrue(placedGlass(helper).isEmpty(), "Bridge built on a floor without a gap");
        helper.succeed();
    }

    /**
     * Linear through the server's real click path ({@code ServerPlayerGameMode#useItemOn}, which also
     * decides that a sneaking player's click skips the block and goes to the item) and the real tick
     * path ({@code ServerPlayer#doTick} -> {@code Inventory#tick} -> {@code inventoryTick}), with the use
     * key held the way a player holds it: vanilla repeats a held key every 4 ticks
     * ({@code Minecraft#startUseItem}, {@code rightClickDelay}), and by then the crosshair rests on the
     * pillar's first block. The repeat must not restart the build from there.
     *
     * <p>Before 2026-09-28 the repeat overwrote the running line: the pillar stopped after two blocks
     * and a beam grew out of its side towards the player - "Linear does not really work".
     *
     * <p><strong>What breaks this test:</strong> letting a click restart a build that is still running,
     * or a Linear line that does not run through the vanilla click path.
     */
    public static void aHeldUseKeyDoesNotRestartTheRunningLinearLine(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        clearRoom(helper);
        BlockPos anchor = new BlockPos(3, 1, 3);
        helper.setBlock(anchor, Blocks.STONE);
        helper.setBlock(anchor.above(5), Blocks.STONE); // the pillar ends below this one: four cells
        Vec3 feet = helper.absoluteVec(new Vec3(3.5, 1.0, 0.5));
        player.snapTo(feet.x, feet.y, feet.z, 0.0F, 30.0F);
        ItemStack wand = wand(helper, ModItems.DIAMOND_BUILDING_WAND, 4, ModEnchantments.LINEAR);
        stock(player, wand, new ItemStack(Items.GLASS, 64));
        player.setShiftKeyDown(true);

        InteractionResult first = serverClick(helper, player, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        helper.assertTrue(first.consumesAction(), "the sneaking Linear click did not arm through useItemOn, got " + first);
        for (int tick = 0; tick < 4; tick++) {
            player.doTick();
        }
        helper.assertTrue(helper.getBlockState(anchor.above()).is(Blocks.GLASS), "the line had not started after four ticks");
        // the held key fires again - at the pillar's first block, the face towards the player
        serverClick(helper, player, anchor.above(), Direction.NORTH, new Vec3(0.5, 0.5, 0.0));
        tickUntilIdle(helper, player, wand);
        player.setShiftKeyDown(false);

        Set<BlockPos> expected = new HashSet<>();
        for (int dy = 1; dy <= 4; dy++) {
            expected.add(anchor.above(dy));
        }
        helper.assertValueEqual(placedGlass(helper), expected,
                "the repeated click restarted the running line (pillar cut short, beam out of its side)");
        helper.succeed();
    }

    /**
     * Bridge when the player, standing at the edge, looks across the gap: the crosshair rests on the
     * far bank (or the bottom of the gap), not on the air, so the click is a block click. With Bridge
     * and without sneaking such a click builds the bridge - the test centre sign says "look across" -
     * and the preview shows the bridge. Sneaking, the same click still builds the plane there.
     *
     * <p>Before 2026-09-28 the far bank got a plane and the bridge only came from a click into the air.
     *
     * <p><strong>What breaks this test:</strong> dropping the Bridge branch from {@code Plan.forClick},
     * aiming it at blocks before the edge or above the bridge, a preview that disagrees, or a bridge
     * that also takes a sneaking click.
     */
    public static void bridgeAlsoStartsWhenTheClickAimsAcrossTheGap(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        BlockPos farBank = new BlockPos(6, 1, 3);
        BlockPos gapFloor = new BlockPos(3, 0, 3);
        Set<BlockPos> bridge = new HashSet<>();
        for (int x = 2; x <= 5; x++) {
            bridge.add(new BlockPos(x, 1, 3));
        }
        for (BlockPos target : java.util.List.of(farBank, gapFloor)) {
            clearRoom(helper);
            helper.setBlock(new BlockPos(0, 1, 3), Blocks.STONE);
            helper.setBlock(new BlockPos(1, 1, 3), Blocks.STONE); // the floor: its edge is x = 1
            helper.setBlock(farBank, Blocks.STONE);
            helper.setBlock(gapFloor, Blocks.STONE);
            Vec3 feet = helper.absoluteVec(new Vec3(0.5, 2.0, 3.5));
            player.snapTo(feet.x, feet.y, feet.z, 270.0F, 20.0F); // facing east, looking a little down
            ItemStack wand = wand(helper, ModItems.DIAMOND_BUILDING_WAND, 1, ModEnchantments.BRIDGE);
            stock(player, wand, new ItemStack(Items.GLASS, 64));

            Set<BlockPos> previewed = absoluteToRelative(helper, BuildingWandItem.getPreviewStates(helper.getLevel(), player, wand,
                    helper.absolutePos(target), Direction.UP, new Vec3(0.5, 1.0, 0.5),
                    ModItems.DIAMOND_BUILDING_WAND.getWandSquareDiameter()).keySet());
            InteractionResult used = serverClick(helper, player, target, Direction.UP, new Vec3(0.5, 1.0, 0.5));
            helper.assertTrue(used.consumesAction(), "the click across the gap at " + target + " was refused: " + used);
            tickUntilIdle(helper, player, wand);
            helper.assertValueEqual(placedGlass(helper), bridge,
                    "a click across the gap at " + target + " did not build the bridge x=2..5 (a plane there instead?)");
            helper.assertValueEqual(previewed, bridge, "the preview for the click at " + target + " does not show the bridge");
        }

        // --- sneaking, the far bank gets its plane as before ---
        clearRoom(helper);
        helper.setBlock(new BlockPos(0, 1, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 1, 3), Blocks.STONE);
        helper.setBlock(farBank, Blocks.STONE);
        ItemStack wand = wand(helper, ModItems.DIAMOND_BUILDING_WAND, 1, ModEnchantments.BRIDGE);
        stock(player, wand, new ItemStack(Items.GLASS, 64));
        player.setShiftKeyDown(true);
        serverClick(helper, player, farBank, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        tickUntilIdle(helper, player, wand);
        player.setShiftKeyDown(false);
        helper.assertTrue(helper.getBlockState(farBank.above()).is(Blocks.GLASS), "sneaking, the far bank did not get its plane");
        helper.assertTrue(helper.getBlockState(new BlockPos(3, 1, 3)).isAir(), "sneaking, the click still built the bridge");
        helper.succeed();
    }

    /** A block click the way the server handles the packet's click: {@code ServerPlayerGameMode#useItemOn}. */
    private static InteractionResult serverClick(GameTestHelper helper, ServerPlayer player, BlockPos relative, Direction face, Vec3 hitRel) {
        BlockPos pos = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(new Vec3(pos.getX() + hitRel.x, pos.getY() + hitRel.y, pos.getZ() + hitRel.z), face, pos, false);
        return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }

    /** Ticks the player the way the server does ({@code doTick} runs the inventory) until the wand is idle. */
    private static void tickUntilIdle(GameTestHelper helper, ServerPlayer player, ItemStack wand) {
        int ticks = 0;
        while (wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr("Active", false) && ticks < TICK_CAP) {
            player.doTick();
            ticks++;
        }
        helper.assertTrue(ticks < TICK_CAP, "the wand never finished within " + TICK_CAP + " ticks");
    }

    /**
     * Roof mode with the enderite wand exactly as the test centre kit hands it out (every enchantment
     * the wand can carry at once - Color Palette and Master Builder among them), stairs and slabs in
     * the hotbar because the off hand holds the octant. The roof is the same as with a bare wand, and
     * the action bar says so; with a non-stair block first it says why there is no roof.
     *
     * <p>Before 2026-09-25 Color Palette switched the roof off: the kit wand filled the prism with a
     * random mix of everything in the hotbar (owner report).
     *
     * <p><strong>What breaks this test:</strong> any enchantment on the wand that disables the roof, the
     * roof stair taken from anywhere but the first building block, or a missing or wrong hint.
     */
    public static void roofModeWorksWithTheTestCentreKitEnderiteWand(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        clearRoom(helper);
        BlockPos anchor = new BlockPos(6, 1, 6);
        helper.setBlock(anchor, Blocks.STONE);
        ItemStack wand = new TcContext(helper.getLevel().registryAccess()).maxEnchanted(new ItemStack(ModItems.ENDERITE_BUILDING_WAND));
        helper.assertTrue(wand.isEnchanted(), "the kit enderite wand carries no enchantments - the case would test a bare wand");
        ItemStack prism = octant(helper, new BlockPos(2, 1, 1), new BlockPos(4, 2, 5), "TRIANGLE", false, false);

        // --- stone first: no roof, and the hint says why ---
        stock(player, wand, new ItemStack(Items.STONE, 64), new ItemStack(Items.OAK_STAIRS, 64), new ItemStack(Items.OAK_SLAB, 64));
        player.setItemInHand(InteractionHand.OFF_HAND, prism);
        String refused = hintKey(ShapeFill.roofHint(player, wand, prism));
        helper.assertTrue("simplebuilding.wand.roof.needs_stairs".equals(refused), "with stone first the roof hint is " + refused);

        // --- stairs first: a roof ---
        stock(player, wand, new ItemStack(Items.OAK_STAIRS, 64), new ItemStack(Items.OAK_SLAB, 64), new ItemStack(Items.STONE, 64));
        player.setItemInHand(InteractionHand.OFF_HAND, prism);
        String active = hintKey(ShapeFill.roofHint(player, wand, prism));
        helper.assertTrue("simplebuilding.wand.roof.active".equals(active), "with stairs first the roof hint is " + active);
        click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        BlueprintBuilder.completeJob(player);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

        for (int z = 1; z <= 5; z++) {
            BlockState west = helper.getBlockState(new BlockPos(2, 1, z));
            BlockState east = helper.getBlockState(new BlockPos(4, 1, z));
            BlockState ridge = helper.getBlockState(new BlockPos(3, 2, z));
            helper.assertTrue(west.is(Blocks.OAK_STAIRS) && west.getValue(StairBlock.FACING) == Direction.EAST,
                    "kit wand " + wand.getEnchantments() + ": the west slope at z=" + z + " is not a stair rising east: " + west);
            helper.assertTrue(east.is(Blocks.OAK_STAIRS) && east.getValue(StairBlock.FACING) == Direction.WEST,
                    "kit wand: the east slope at z=" + z + " is not a stair rising west: " + east);
            helper.assertTrue(ridge.is(Blocks.OAK_SLAB) && ridge.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.BOTTOM,
                    "kit wand: the ridge at z=" + z + " is not a bottom oak slab: " + ridge);
            helper.assertTrue(helper.getBlockState(new BlockPos(3, 1, z)).isAir(), "kit wand: the roof was filled underneath at z=" + z);
        }
        helper.succeed();
    }

    // =====================================================================================
    // PROTECTION AND MATERIAL (audit 2026-09-26)
    // =====================================================================================

    /**
     * Audit 2026-09-26, P2 #6: the wand asks for every cell whether this player may build there,
     * the way {@code BlueprintBuilder} does. Before the fix only vanilla's check on the clicked
     * block ran, so a plane (and a bridge, which runs through the same loop) grew into claimed land
     * and spawn protection. The "claim" here is a player whose {@code mayUseItemAt} refuses the
     * east column - the hook protection mods answer through.
     *
     * <p><strong>What breaks this test:</strong> dropping {@code mayBuildAt} from
     * {@code inventoryTick}, or paying for a refused cell.
     */
    public static void wandSkipsEveryCellThePlayerMayNotBuildOn(GameTestHelper helper) {
        clearRoom(helper);
        BlockPos anchor = new BlockPos(3, 1, 3);
        helper.setBlock(anchor, Blocks.STONE);
        int claimedFromX = helper.absolutePos(anchor).getX() + 1;
        ServerPlayer player = claimPlayer(helper, pos -> pos.getX() >= claimedFromX);
        ItemStack wand = wand(helper, ModItems.DIAMOND_BUILDING_WAND, 1, null);
        stock(player, wand, new ItemStack(Items.GLASS, 64));

        InteractionResult armed = click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        helper.assertTrue(armed == InteractionResult.CONSUME, "the wand did not arm on its own block, got " + armed);
        runUntilIdle(helper, player, wand);

        Set<BlockPos> expected = new HashSet<>();
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                expected.add(anchor.offset(dx, 1, dz));
            }
        }
        helper.assertValueEqual(placedGlass(helper), expected,
                "the wand built into the claimed east column, or skipped cells it may build on");
        helper.assertValueEqual(countIn(player, Items.GLASS), 64 - 6, "the wand did not pay exactly the six cells it built");
        helper.succeed();
    }

    /**
     * Audit 2026-09-26, P2 #6: without build rights (adventure or spectator mode) the wand neither
     * starts a build nor takes one back. Vanilla only guards {@code useOn}; the undo on
     * sneak + air click (and the bridge on a plain air click) ran through {@code use}, which it
     * does not guard, so an adventure player could clear a build with it.
     *
     * <p><strong>What breaks this test:</strong> dropping the {@code mayBuild} check from
     * {@code use} or from {@code useOn}.
     */
    public static void wandNeitherBuildsNorUndoesWithoutBuildRights(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        clearRoom(helper);
        WandUndo.forget(player.getUUID());
        BlockPos anchor = new BlockPos(3, 1, 3);
        helper.setBlock(anchor, Blocks.STONE);
        ItemStack wand = wand(helper, ModItems.DIAMOND_BUILDING_WAND, 1, null);
        stock(player, wand, new ItemStack(Items.GLASS, 64));
        click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        runUntilIdle(helper, player, wand);
        helper.assertValueEqual(placedGlass(helper).size(), 9, "the set-up plane was not built");

        player.getAbilities().mayBuild = false;
        player.setShiftKeyDown(true);
        InteractionResult undo = wand.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
        InteractionResult build = click(helper, player, wand, anchor.above(3), Direction.UP, new Vec3(0.5, 1.0, 0.5));
        boolean armed = wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr("Active", false);
        player.getAbilities().mayBuild = true;
        WandUndo.forget(player.getUUID());

        helper.assertTrue(undo == InteractionResult.FAIL, "sneak + air click without build rights returned " + undo);
        helper.assertValueEqual(placedGlass(helper).size(), 9, "the wand took a build back without build rights");
        helper.assertValueEqual(countIn(player, Items.GLASS), 64 - 9, "the refused undo refunded glass");
        helper.assertTrue(build == InteractionResult.FAIL, "a click on a block without build rights returned " + build);
        helper.assertFalse(armed, "the wand armed a build without build rights");
        helper.succeed();
    }

    /**
     * Audit 2026-09-26, P2 #10 (the wand's part): the blueprint build mode draws its material
     * through {@code findSupply}/{@code countSupply}, and those took any stack of the item - a named
     * or filled shulker box, a patterned banner, a head with a profile - and placed it as a bare
     * block, its contents gone. Only stacks without their own components count now.
     *
     * <p><strong>What breaks this test:</strong> dropping {@code isPlainSupply} from either method.
     */
    public static void wandSupplyPassesOverStacksWithComponents(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack wand = wand(helper, ModItems.DIAMOND_BUILDING_WAND, 1, null);
        ItemStack named = new ItemStack(Items.SHULKER_BOX);
        named.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Loot"));
        stock(player, wand, named, new ItemStack(Items.SHULKER_BOX, 2));

        helper.assertValueEqual(BuildingWandItem.countSupply(player, wand, Items.SHULKER_BOX), 2,
                "countSupply counted the named shulker box as building material");
        Runnable take = BuildingWandItem.findSupply(player, wand, Items.SHULKER_BOX);
        helper.assertTrue(take != null, "findSupply found no plain shulker box although two are carried");
        take.run();
        helper.assertTrue(player.getInventory().getItem(1).getCount() == 1
                        && player.getInventory().getItem(1).has(DataComponents.CUSTOM_NAME),
                "findSupply spent the named shulker box: " + player.getInventory().getItem(1));
        helper.assertValueEqual(player.getInventory().getItem(2).getCount(), 1,
                "findSupply did not take its piece from the plain stack");

        player.getInventory().setItem(2, ItemStack.EMPTY);
        helper.assertTrue(BuildingWandItem.findSupply(player, wand, Items.SHULKER_BOX) == null,
                "with only the named shulker box left, findSupply still offered it");
        helper.assertValueEqual(BuildingWandItem.countSupply(player, wand, Items.SHULKER_BOX), 0,
                "with only the named shulker box left, countSupply still counted it");
        helper.succeed();
    }

    /**
     * Audit 2026-09-26, P3 #29: command, structure and jigsaw blocks come out of the wand only for
     * a player who could place them by hand ({@code canUseGameMasterBlocks}, what vanilla's
     * {@code GameMasterBlockItem} asks). The mock player builds for free here and is still no
     * operator, so it is refused; stone is the control that the same call places at all.
     *
     * <p><strong>What breaks this test:</strong> dropping the {@code GameMasterBlock} check from
     * {@code WandPlacement#baseState}.
     */
    public static void wandPlacesGameMasterBlocksOnlyForOperators(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        clearRoom(helper);
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
        BlockPos cell = helper.absolutePos(new BlockPos(3, 2, 3));
        player.getAbilities().instabuild = true;
        boolean operator = player.canUseGameMasterBlocks();
        BlockState stone = com.simplebuilding.util.WandPlacement.stateFor(helper.getLevel(), player,
                new ItemStack(Items.STONE), cell, Direction.UP, com.simplebuilding.util.WandPlacement.TOP_CENTER, null);
        java.util.List<String> placed = new java.util.ArrayList<>();
        for (Item item : java.util.List.of(Items.COMMAND_BLOCK, Items.CHAIN_COMMAND_BLOCK, Items.STRUCTURE_BLOCK, Items.JIGSAW)) {
            if (com.simplebuilding.util.WandPlacement.stateFor(helper.getLevel(), player, new ItemStack(item), cell,
                    Direction.UP, com.simplebuilding.util.WandPlacement.TOP_CENTER, null) != null) {
                placed.add(item.toString());
            }
        }
        player.getAbilities().instabuild = false;

        helper.assertFalse(operator, "the mock player may use game master blocks, so this test cannot tell anything");
        helper.assertTrue(stone != null, "the wand would not even place stone here, so the refusals prove nothing");
        helper.assertTrue(placed.isEmpty(), "the wand places game master blocks for a non-operator: " + placed);
        helper.succeed();
    }

    /**
     * Audit 2026-09-26, P3 #31: undo refunded by block type. A slab the wand set and the player
     * doubled by hand since was cleared - two slabs gone - and one slab came back. A block whose
     * amount ({@code type}, {@code candles}, {@code pickles}, ...) is no longer what the wand set
     * now stays standing and is not paid for; the untouched rest is cleared and refunded as before.
     *
     * <p><strong>What breaks this test:</strong> comparing only the block in {@code WandUndo}.
     */
    public static void undoLeavesTheSlabThatWasDoubledSinceStanding(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        clearRoom(helper);
        WandUndo.forget(player.getUUID());
        BlockPos anchor = new BlockPos(3, 1, 3);
        helper.setBlock(anchor, Blocks.STONE);
        ItemStack wand = wand(helper, ModItems.DIAMOND_BUILDING_WAND, 1, null);
        stock(player, wand, new ItemStack(Items.STONE_SLAB, 64));
        click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        runUntilIdle(helper, player, wand);
        helper.assertValueEqual(countIn(player, Items.STONE_SLAB), 64 - 9, "the slab plane did not cost nine slabs");

        BlockPos doubled = anchor.offset(1, 1, 1);
        BlockState doubleSlab = Blocks.STONE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE);
        helper.setBlock(doubled, doubleSlab);

        player.setShiftKeyDown(true);
        wand.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
        WandUndo.forget(player.getUUID());

        for (BlockPos pos : BlockPos.betweenClosed(anchor.offset(-1, 1, -1), anchor.offset(1, 1, 1))) {
            if (pos.equals(doubled)) {
                helper.assertTrue(helper.getBlockState(pos) == doubleSlab,
                        "undo cleared the slab the player doubled by hand, it is now " + helper.getBlockState(pos));
            } else {
                helper.assertTrue(helper.getBlockState(pos).isAir(), "undo left the wand's slab at " + pos);
            }
        }
        helper.assertValueEqual(countIn(player, Items.STONE_SLAB), 64 - 9 + 8,
                "undo did not hand back exactly the eight slabs it cleared");
        helper.succeed();
    }

    /**
     * The octant fill plans without touching the world (audit 2026-09-26 #8): a selection in chunks
     * that are not loaded is refused - and stays unloaded - and whether a block has support is only
     * decided when the planner visits the cell, inside its per-tick budget. A torch cell over air
     * therefore still builds when the floor appears between planning and building; the old planner
     * had already read (and dropped) it at click time, for every one of up to 4 million cells.
     *
     * <p><strong>What breaks this test:</strong> a planner that reads blocks (and so loads or
     * generates chunks) for the whole box when the wand is clicked, or no check for unloaded chunks.
     */
    public static void octantFillPlansLazilyAndRefusesUnloadedChunks(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        clearRoom(helper);
        ItemStack wand = wand(helper, ModItems.DIAMOND_BUILDING_WAND, 1, null);
        stock(player, wand, new ItemStack(Items.TORCH, 8));
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);

        // --- far away, nothing loaded there: refused without loading the chunk ---
        BlockPos far = helper.absolutePos(new BlockPos(2, 1, 2)).offset(200_000, 0, 200_000);
        ItemStack distant = new ItemStack(ModItems.OCTANT);
        CompoundTag nbt = new CompoundTag();
        nbt.putIntArray("Pos1", new int[]{far.getX(), far.getY(), far.getZ()});
        nbt.putIntArray("Pos2", new int[]{far.getX() + 1, far.getY(), far.getZ() + 1});
        distant.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        helper.assertFalse(helper.getLevel().hasChunk(far.getX() >> 4, far.getZ() >> 4), "setup: the far chunk is already loaded");
        ShapeFill.Plan refused = ShapeFill.plan(helper.getLevel(), player, wand, distant);
        helper.assertTrue(refused.problem() != null
                        && "simplebuilding.wand.shape.unloaded".equals(hintKey(refused.problem())),
                "a fill in unloaded chunks was not refused: " + (refused.problem() == null ? "planned" : hintKey(refused.problem())));
        helper.assertFalse(helper.getLevel().hasChunk(far.getX() >> 4, far.getZ() >> 4), "planning loaded the far chunk");

        // --- support is decided when the cell is visited, not at click time ---
        ItemStack single = octant(helper, new BlockPos(2, 2, 2), new BlockPos(2, 2, 2), "CUBOID", false, false);
        player.setItemInHand(InteractionHand.OFF_HAND, single);
        ShapeFill.Plan plan = ShapeFill.plan(helper.getLevel(), player, wand, single);
        helper.assertTrue(plan.problem() == null && plan.layout() != null, "the one-cell fill was not planned: " + plan.problem());
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE); // the floor arrives after the plan
        BlueprintBuilder.buildLayout(helper.getLevel(), player, wand, single, plan.layout());
        BlueprintBuilder.completeJob(player);
        helper.assertTrue(helper.getBlockState(new BlockPos(2, 2, 2)).is(Blocks.TORCH),
                "the torch was dropped at planning time although its floor was there when the planner got to it: "
                        + helper.getBlockState(new BlockPos(2, 2, 2)));
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        clearRoom(helper);
        helper.succeed();
    }

    // =====================================================================================
    // FOLLOW-UP AUDIT 2026-09-27
    // =====================================================================================

    /**
     * Follow-up audit 2026-09-27 N2 (the rest of #29): the octant fill cannot hand a non-operator
     * command blocks either. The wand's own placement already refused them, but the fill fell back to
     * the block's default state when {@code WandPlacement#baseState} said no - a creative non-op could
     * fill a figure with command blocks. The fill layout now leaves such a cell out.
     *
     * <p><strong>What breaks this test:</strong> falling back to {@code defaultBlockState()} for a
     * {@code GameMasterBlock} in {@code ShapeFill.FillLayout#rawState}.
     */
    public static void octantFillLeavesGameMasterBlocksOutForNonOperators(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        clearRoom(helper);
        BlockPos anchor = new BlockPos(6, 1, 6);
        helper.setBlock(anchor, Blocks.STONE);
        ItemStack wand = wand(helper, ModItems.DIAMOND_BUILDING_WAND, 1, null);
        stock(player, wand, new ItemStack(Items.COMMAND_BLOCK, 4));
        player.getAbilities().instabuild = true;
        ItemStack octant = octant(helper, new BlockPos(2, 1, 2), new BlockPos(3, 1, 2), "CUBOID", false, false);
        player.setItemInHand(InteractionHand.OFF_HAND, octant);
        boolean operator = player.canUseGameMasterBlocks();

        ShapeFill.Plan plan = ShapeFill.plan(helper.getLevel(), player, wand, octant);
        BlockState offered = plan.layout() == null ? null : plan.layout().state(0);
        click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        BlueprintBuilder.completeJob(player);
        player.getAbilities().instabuild = false;
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

        helper.assertFalse(operator, "the mock player may use game master blocks, so this test cannot tell anything");
        helper.assertTrue(plan.problem() == null, "the command block fill was refused before it got to the cells: " + plan.problem());
        helper.assertTrue(offered == null, "the fill layout offers a non-operator a command block cell: " + offered);
        helper.assertTrue(helper.getBlockState(new BlockPos(2, 1, 2)).isAir() && helper.getBlockState(new BlockPos(3, 1, 2)).isAir(),
                "the octant fill placed command blocks for a non-operator");
        helper.succeed();
    }

    /**
     * Follow-up audit 2026-09-27 N8: the simulated material check (and the preview, which is the same
     * planner) passed over cells that cannot stand yet because the layer under them is not built -
     * the upper carpet of a two-carpet column over air. With one carpet the first click then built
     * without a warning and the preview showed nothing above the first layer. A cell next to one the
     * simulation already set counts now.
     *
     * <p><strong>What breaks this test:</strong> {@code FillLayout#state(int, LongPredicate)} ignoring
     * the simulated cells, or the planner calling the variant without them.
     */
    public static void octantFillCheckCountsCellsOnLayersItHasNotBuiltYet(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        clearRoom(helper);
        BlockPos anchor = new BlockPos(6, 1, 6);
        helper.setBlock(anchor, Blocks.STONE);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE); // the carpet column stands on this
        ItemStack wand = wand(helper, ModItems.DIAMOND_BUILDING_WAND, 1, null);
        stock(player, wand, new ItemStack(Items.MOSS_CARPET, 1));
        ItemStack column = octant(helper, new BlockPos(2, 2, 2), new BlockPos(2, 3, 2), "CUBOID", false, false);
        player.setItemInHand(InteractionHand.OFF_HAND, column);

        BlueprintBuilder.Preview preview = ShapeFill.preview(helper.getLevel(), player, wand, column);
        BlockPos lower = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos upper = helper.absolutePos(new BlockPos(2, 3, 2));
        click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        BlueprintBuilder.completeJob(player);
        boolean builtWithoutWarning = helper.getBlockState(new BlockPos(2, 2, 2)).is(Blocks.MOSS_CARPET);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

        helper.assertTrue(preview.placed().containsKey(lower), "the preview does not show the lower carpet: " + preview.placed().keySet());
        helper.assertTrue(preview.missing().containsKey(upper),
                "the preview does not mark the upper carpet (one layer up, over the unbuilt one) as missing: " + preview.missing().keySet());
        helper.assertFalse(builtWithoutWarning, "one carpet for two cells: the first click built instead of warning");
        helper.assertValueEqual(countIn(player, Items.MOSS_CARPET), 1, "the warning click spent the carpet");
        clearRoom(helper);
        helper.succeed();
    }

    /**
     * Follow-up audit 2026-09-27 N9: the loaded-chunk check of the fill covers the box plus one block
     * around it - support and shape checks at the edge read the neighbour cells, and a read in an
     * unloaded chunk loads (or generates) it. A two-cell box at the east edge of the only loaded chunk
     * far away is refused and the chunk east of it stays unloaded.
     *
     * <p><strong>What breaks this test:</strong> the chunk loop in {@code ShapeFill#plan} without the
     * one-block margin.
     */
    public static void octantFillNeedsTheChunksAroundItsBoxLoaded(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        ItemStack wand = wand(helper, ModItems.DIAMOND_BUILDING_WAND, 1, null);
        stock(player, wand, new ItemStack(Items.GLASS, 8));
        BlockPos far = helper.absolutePos(new BlockPos(2, 1, 2)).offset(300_000, 0, -300_000);
        int cx = far.getX() >> 4, cz = far.getZ() >> 4;
        helper.getLevel().getChunk(cx, cz); // exactly this chunk, fully loaded
        boolean setup = helper.getLevel().hasChunk(cx, cz) && !helper.getLevel().hasChunk(cx + 1, cz);
        int edgeX = (cx << 4) + 15, z = (cz << 4) + 8;
        ItemStack edge = new ItemStack(ModItems.OCTANT);
        CompoundTag nbt = new CompoundTag();
        nbt.putIntArray("Pos1", new int[]{edgeX - 1, far.getY(), z});
        nbt.putIntArray("Pos2", new int[]{edgeX, far.getY(), z});
        edge.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

        ShapeFill.Plan plan = ShapeFill.plan(helper.getLevel(), player, wand, edge);

        helper.assertTrue(setup, "setup: the far chunk did not load on its own (or its east neighbour came with it)");
        helper.assertTrue(plan.problem() != null && "simplebuilding.wand.shape.unloaded".equals(hintKey(plan.problem())),
                "a box at the edge of the unloaded chunk east of it was planned: "
                        + (plan.problem() == null ? "planned" : hintKey(plan.problem())));
        helper.assertFalse(helper.getLevel().hasChunk(cx + 1, cz), "planning loaded the chunk east of the box");
        helper.succeed();
    }

    /**
     * After the lazy {@code ShapeFill} rewrite (audit 2026-09-26 #8), the test gap of the follow-up
     * audit (N16): fill order, the per-tick bound, layer mode with hollow, and layer mode on a finished
     * figure. Top down starts with the centre of the top layer; a player with the small build budget
     * gets exactly three cells per tick; layer mode builds bottom, hollow ring, top, one per click;
     * a fourth click finds nothing to do and says so instead of "0 placed".
     *
     * <p><strong>What breaks this test:</strong> a wrong layer direction or centre-out order in
     * {@code FillLayout}, a job that exceeds its visits per tick, Hollow ignored in layer mode, or a
     * finished layer-mode figure answered with SUCCESS.
     */
    public static void octantFillHonoursOrderLayersHollowAndTheTickBudget(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        clearRoom(helper);
        WandUndo.forget(player.getUUID());
        BlockPos anchor = new BlockPos(6, 1, 6);
        helper.setBlock(anchor, Blocks.STONE);
        ItemStack wand = wand(helper, ModItems.DIAMOND_BUILDING_WAND, 1, null);

        // --- top down, three cells per tick ---
        player.addTag(com.simplebuilding.blueprint.BlueprintScanner.SMALL_BUDGET_TAG);
        stock(player, wand, new ItemStack(Items.GLASS, 64));
        ItemStack topDown = withOrder(octant(helper, new BlockPos(2, 1, 2), new BlockPos(4, 2, 4), "CUBOID", false, false), "TOP_DOWN");
        player.setItemInHand(InteractionHand.OFF_HAND, topDown);
        click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
        int topAfterClick = glassIn(helper, 2, 2, 2, 4, 2, 4);
        int bottomAfterClick = glassIn(helper, 2, 1, 2, 4, 1, 4);
        boolean centreFirst = helper.getBlockState(new BlockPos(3, 2, 3)).is(Blocks.GLASS);
        helper.assertValueEqual(topAfterClick, 3, "the first slice of a top-down fill is not three cells of the top layer");
        helper.assertValueEqual(bottomAfterClick, 0, "a top-down fill started at the bottom");
        helper.assertTrue(centreFirst, "the top-down fill did not start with the centre of its layer");

        helper.runAfterDelay(1, () -> {
            wand.getItem().inventoryTick(wand, helper.getLevel(), player, EquipmentSlot.MAINHAND);
            helper.assertValueEqual(glassIn(helper, 2, 1, 2, 4, 2, 4), 6, "one more tick did not place exactly three more cells");
            BlueprintBuilder.completeJob(player);
            player.removeTag(com.simplebuilding.blueprint.BlueprintScanner.SMALL_BUDGET_TAG);
            helper.assertValueEqual(glassIn(helper, 2, 1, 2, 4, 2, 4), 18, "the top-down fill did not finish the 3x2x3 box");

            // --- layer mode + hollow: bottom, ring, top - then nothing to do ---
            clearRoom(helper);
            helper.setBlock(anchor, Blocks.STONE);
            stock(player, wand, new ItemStack(Items.GLASS, 64));
            ItemStack layered = octant(helper, new BlockPos(2, 1, 2), new BlockPos(4, 3, 4), "CUBOID", true, true);
            player.setItemInHand(InteractionHand.OFF_HAND, layered);
            click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
            BlueprintBuilder.completeJob(player);
            helper.assertValueEqual(glassIn(helper, 2, 1, 2, 4, 1, 4), 9, "layer mode + hollow: the bottom layer is not full");
            helper.assertValueEqual(glassIn(helper, 2, 2, 2, 4, 3, 4), 0, "layer mode + hollow built more than the bottom layer");
            click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
            BlueprintBuilder.completeJob(player);
            helper.assertValueEqual(glassIn(helper, 2, 2, 2, 4, 2, 4), 8, "layer mode + hollow: the middle layer is not the ring of eight");
            helper.assertTrue(helper.getBlockState(new BlockPos(3, 2, 3)).isAir(), "layer mode filled the hollow centre");
            click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
            BlueprintBuilder.completeJob(player);
            helper.assertValueEqual(glassIn(helper, 2, 3, 2, 4, 3, 4), 9, "layer mode + hollow: the top layer is not full");
            InteractionResult done = click(helper, player, wand, anchor, Direction.UP, new Vec3(0.5, 1.0, 0.5));
            BlueprintBuilder.completeJob(player);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            helper.assertTrue(done == InteractionResult.FAIL,
                    "a layer-mode click on the finished figure did not answer 'nothing to do', it returned " + done);
            helper.assertValueEqual(countIn(player, Items.GLASS), 64 - 26, "the hollow cube did not cost its 26 shell blocks");
            clearRoom(helper);
            helper.succeed();
        });
    }

    /**
     * Follow-up audit 2026-09-27 N13: undo only spared container block entities - a lectern the wand
     * set and the player put a book on since was cleared, the book gone; a glow lichen the player
     * spread over a second face was cleared and one piece refunded. Undo also asks the same build
     * right as building ({@code mayBuildAt}), not only spawn protection. Untouched cells of the same
     * kinds are still cleared and paid back.
     *
     * <p><strong>What breaks this test:</strong> dropping {@code unchangedContent},
     * {@code sameFaces} or the {@code mayBuildAt} check from {@code WandUndo#undo}.
     */
    public static void undoKeepsBlockEntityContentsSpreadFacesAndProtectedCells(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        clearRoom(helper);
        WandUndo.forget(player.getUUID());
        ItemStack wand = wand(helper, ModItems.DIAMOND_BUILDING_WAND, 1, null);
        stock(player, wand);

        // --- without the right to build there, nothing is cleared ---
        BlockPos guarded = new BlockPos(1, 1, 1);
        WandUndo.begin(player, helper.getLevel());
        placeRecorded(helper, player, guarded, Blocks.STONE.defaultBlockState(), Items.STONE);
        player.getAbilities().mayBuild = false;
        WandUndo.undo(player, helper.getLevel());
        player.getAbilities().mayBuild = true;
        helper.assertBlockPresent(Blocks.STONE, guarded);
        helper.assertValueEqual(countIn(player, Items.STONE), 0, "undo without build rights refunded the stone");

        // --- block entity contents and spread faces stay, the untouched copies go ---
        BlockPos bookLectern = new BlockPos(2, 1, 2);
        BlockPos bareLectern = new BlockPos(4, 1, 2);
        BlockPos spreadLichen = new BlockPos(2, 2, 4);
        BlockPos sameLichen = new BlockPos(4, 2, 4);
        helper.setBlock(spreadLichen.below(), Blocks.STONE);
        helper.setBlock(spreadLichen.north(), Blocks.STONE);
        helper.setBlock(sameLichen.below(), Blocks.STONE);
        BlockState floorLichen = Blocks.GLOW_LICHEN.defaultBlockState().setValue(net.minecraft.world.level.block.MultifaceBlock.getFaceProperty(Direction.DOWN), true);
        WandUndo.begin(player, helper.getLevel());
        placeRecorded(helper, player, bookLectern, Blocks.LECTERN.defaultBlockState(), Items.LECTERN);
        placeRecorded(helper, player, bareLectern, Blocks.LECTERN.defaultBlockState(), Items.LECTERN);
        placeRecorded(helper, player, spreadLichen, floorLichen, Items.GLOW_LICHEN);
        placeRecorded(helper, player, sameLichen, floorLichen, Items.GLOW_LICHEN);
        BlockPos bookAt = helper.absolutePos(bookLectern);
        net.minecraft.world.level.block.LecternBlock.tryPlaceBook(player, helper.getLevel(), bookAt,
                helper.getLevel().getBlockState(bookAt), new ItemStack(Items.WRITABLE_BOOK));
        BlockState spread = floorLichen.setValue(net.minecraft.world.level.block.MultifaceBlock.getFaceProperty(Direction.NORTH), true);
        helper.setBlock(spreadLichen, spread);

        WandUndo.undo(player, helper.getLevel());
        WandUndo.forget(player.getUUID());

        BlockState keptLectern = helper.getBlockState(bookLectern);
        helper.assertTrue(keptLectern.is(Blocks.LECTERN) && keptLectern.getValue(BlockStateProperties.HAS_BOOK),
                "undo cleared the lectern the player put a book on since, it is now " + keptLectern);
        helper.assertTrue(helper.getBlockState(spreadLichen) == spread,
                "undo cleared the glow lichen the player spread over a second face, it is now " + helper.getBlockState(spreadLichen));
        helper.assertTrue(helper.getBlockState(bareLectern).isAir(), "undo left the untouched lectern standing");
        helper.assertTrue(helper.getBlockState(sameLichen).isAir(), "undo left the untouched glow lichen standing");
        helper.assertValueEqual(countIn(player, Items.LECTERN), 1, "undo did not refund exactly the one lectern it cleared");
        helper.assertValueEqual(countIn(player, Items.GLOW_LICHEN), 1, "undo did not refund exactly the one glow lichen it cleared");
        clearRoom(helper);
        helper.succeed();
    }

    /** Sets a block the way a wand action does and records it for undo (one item paid). */
    private static void placeRecorded(GameTestHelper helper, ServerPlayer player, BlockPos relative, BlockState state, Item item) {
        BlockPos pos = helper.absolutePos(relative);
        helper.getLevel().setBlock(pos, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
        WandUndo.record(player, helper.getLevel(), pos, state, item, 1);
    }

    private static ItemStack withOrder(ItemStack octant, String order) {
        CompoundTag nbt = octant.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        nbt.putString("FillOrder", order);
        octant.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        return octant;
    }

    private static String hintKey(net.minecraft.network.chat.Component hint) {
        return hint != null && hint.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t ? t.getKey() : String.valueOf(hint);
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
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /**
     * A survival player whose {@code mayUseItemAt} refuses the cells {@code claimed} selects - what a
     * claim or protection mod does. Never added to the level or the player list, so it needs no
     * cleanup; it has no connection either, which the wand's plane path never needs.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer claimPlayer(GameTestHelper helper, java.util.function.Predicate<BlockPos> claimed) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "test-claim-player"),
                net.minecraft.server.level.ClientInformation.createDefault()) {
            @Override
            public boolean mayUseItemAt(BlockPos pos, Direction face, ItemStack stack) {
                return !claimed.test(pos) && super.mayUseItemAt(pos, face, stack);
            }
        };
        Vec3 pos = helper.absoluteVec(new Vec3(0.5, 5.0, 7.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        // The gametest server hands new players its default game mode (creative); this one pays.
        player.getAbilities().instabuild = false;
        player.getAbilities().mayBuild = true;
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
