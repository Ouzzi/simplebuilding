package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.doors.DoorFormat;
import com.simplebuilding.doors.WideDoorBlock;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Simple Doors (WP-17 / BS28-2, docs/ai/KONZEPT-A-WP17-2026-10-29.md): placing every cell, opening by hand and redstone, the
 * swing-area rule (D1 A: the whole width x height area in front must be free), half doors swinging inside their own block (D2),
 * blocked doors staying shut, breaking as a unit with one drop, immovable for pistons, the config switch. Facing north, hinge
 * LEFT (hinge at the west edge, the door extends east, the leaf swings north); all coordinates are test-relative (8x8x8).
 */
public final class WideDoorTests {
    private static final BlockPos ORIGIN = new BlockPos(1, 2, 5);

    private WideDoorTests() {
    }

    private static Block block(DoorFormat f) {
        return switch (f) {
            case FULL_2X2 -> ModBlocks.OAK_DOOR_2X2;
            case FULL_2X3 -> ModBlocks.OAK_DOOR_2X3;
            case WIDE_1_5X2 -> ModBlocks.OAK_DOOR_1_5X2;
            case WIDE_1_5X3 -> ModBlocks.OAK_DOOR_1_5X3;
            case HALF_2 -> ModBlocks.OAK_DOOR_HALF_2;
            case HALF_3 -> ModBlocks.OAK_DOOR_HALF_3;
        };
    }

    private static Item item(DoorFormat f) {
        return block(f).asItem();
    }

    private static void clear(GameTestHelper h) {
        for (int x = 0; x <= 7; x++) {
            for (int y = 0; y <= 7; y++) {
                for (int z = 0; z <= 7; z++) {
                    h.setBlock(new BlockPos(x, y, z), y == 1 ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
    }

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        Vec3 spot = h.absoluteVec(new Vec3(6.5, 2.0, 7.0));
        player.setPos(spot.x, spot.y, spot.z);
        player.setYRot(180.0F); // looks north
        player.setYHeadRot(180.0F);
        player.setXRot(0.0F);
        return player;
    }

    /** Places the door item on the floor block below {@code at} (a click in the middle of its top: hinge LEFT unless a wall decides). */
    private static boolean place(GameTestHelper h, ServerPlayer player, DoorFormat f, BlockPos at) {
        ItemStack stack = new ItemStack(item(f));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos floor = h.absolutePos(at.below());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction();
    }

    private static BlockState at(GameTestHelper h, BlockPos pos) {
        return h.getBlockState(pos);
    }

    private static boolean cell(GameTestHelper h, BlockPos pos, Block door, int col, int row, boolean swing, boolean open) {
        BlockState s = at(h, pos);
        return s.is(door) && s.getValue(WideDoorBlock.COL) == col && s.getValue(WideDoorBlock.ROW) == row && s.getValue(WideDoorBlock.SWING) == swing
                && s.getValue(WideDoorBlock.OPEN) == open && s.getValue(WideDoorBlock.FACING) == Direction.NORTH
                && s.getValue(WideDoorBlock.HINGE) == DoorHingeSide.LEFT;
    }

    /** The cell positions of a door placed at ORIGIN: hinge column x=1, free column x=2, rows upwards, swing cells one north. */
    private static BlockPos hinge(int row) {
        return ORIGIN.above(row);
    }

    private static BlockPos free(int row) {
        return ORIGIN.east().above(row);
    }

    private static BlockPos swing(int row, int col) {
        return ORIGIN.east(col).above(row).north();
    }

    private static boolean closedAndWhole(GameTestHelper h, DoorFormat f) {
        Block door = block(f);
        for (int c = 0; c < f.cols; c++) {
            for (int r = 0; r < f.rows; r++) {
                if (!cell(h, ORIGIN.east(c).above(r), door, c, r, false, false)) return false;
            }
        }
        return true;
    }

    private static boolean openAndWhole(GameTestHelper h, DoorFormat f) {
        Block door = block(f);
        for (int r = 0; r < f.rows; r++) {
            if (!cell(h, hinge(r), door, 0, r, false, true)) return false;
            if (!f.half) {
                if (!cell(h, swing(r, 0), door, 0, r, true, true)) return false;
                if (!at(h, free(r)).isAir()) return false;
            }
        }
        return true;
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1.0E-6;
    }

    private static List<DoorFormat> formats() {
        return List.of(DoorFormat.values());
    }

    // ---------------------------------------------------------------- tests

    public static void registeredAndCraftable(GameTestHelper h) {
        if (!McVersion.DOORS) {
            h.succeed();
            return;
        }
        List<String> problems = new ArrayList<>();
        for (DoorFormat f : formats()) {
            if (!(block(f) instanceof WideDoorBlock door) || door.format() != f) problems.add("block " + f);
            if (!(item(f) instanceof net.minecraft.world.item.BlockItem)) problems.add("item " + f);
            if (McVersion.pushReaction(block(f).defaultBlockState()) != McVersion.PUSH_BLOCKED) problems.add("movable " + f);
        }
        var recipes = h.getLevel().getServer().getRecipeManager();
        ItemStack door = new ItemStack(Items.OAK_DOOR), planks = new ItemStack(Items.OAK_PLANKS), stick = new ItemStack(Items.STICK);
        record Case(String what, CraftingInput input, Item expect) {}
        List<Case> cases = List.of(
                new Case("2x2", CraftingInput.of(2, 2, List.of(door, door, door, door)), item(DoorFormat.FULL_2X2)),
                new Case("2x3", CraftingInput.of(2, 3, List.of(door, door, door, door, door, door)), item(DoorFormat.FULL_2X3)),
                new Case("1.5x2", CraftingInput.of(2, 2, List.of(door, planks, planks, stick)), item(DoorFormat.WIDE_1_5X2)),
                new Case("1.5x3", CraftingInput.of(2, 1, List.of(new ItemStack(item(DoorFormat.WIDE_1_5X2)), planks)), item(DoorFormat.WIDE_1_5X3)));
        for (Case c : cases) {
            ItemStack out = recipes.getRecipeFor(RecipeType.CRAFTING, c.input(), h.getLevel()).map(r -> r.value().assemble(c.input())).orElse(ItemStack.EMPTY);
            if (!out.is(c.expect()) || out.getCount() != 1) problems.add("recipe " + c.what() + " gave " + out);
        }
        h.assertTrue(problems.isEmpty(), "door registration problems: " + problems);
        h.succeed();
    }

    /** Placing fills every cell of the door plane (hinge column first), and needs all of them free. */
    public static void placesEveryCell(GameTestHelper h) {
        if (!McVersion.DOORS) {
            h.succeed();
            return;
        }
        ServerPlayer player = player(h);
        List<String> problems = new ArrayList<>();
        for (DoorFormat f : formats()) {
            clear(h);
            if (!place(h, player, f, ORIGIN)) problems.add(f + " not placed");
            else if (!closedAndWhole(h, f)) problems.add(f + " cells wrong: " + at(h, ORIGIN) + " / " + at(h, ORIGIN.east()));
            // a blocked cell: nothing is placed
            clear(h);
            h.setBlock(ORIGIN.east().above(f.rows - 1), Blocks.STONE);
            h.setBlock(ORIGIN.west().above(f.rows - 1), Blocks.STONE); // both hinge sides blocked: the other hinge is no way out
            if (f.cols == 2 && place(h, player, f, ORIGIN)) problems.add(f + " placed although a cell is taken");
            // no ground: nothing is placed
            clear(h);
            h.setBlock(ORIGIN.below(), Blocks.AIR);
            // IG31-9: the item may now put the door a cell or two lower (tolerance), but never without support under its hinge cell
            if (at(h, ORIGIN.below()).isAir() && place(h, player, f, ORIGIN)) {
                for (int x = 0; x <= 7; x++) {
                    for (int y = 0; y <= 7; y++) {
                        for (int z = 0; z <= 7; z++) {
                            BlockPos p = new BlockPos(x, y, z);
                            BlockState st = at(h, p);
                            if (st.is(block(f)) && st.getValue(WideDoorBlock.COL) == 0 && st.getValue(WideDoorBlock.ROW) == 0 && !st.getValue(WideDoorBlock.SWING)
                                    && !at(h, p.below()).isFaceSturdy(h.getLevel(), h.absolutePos(p.below()), Direction.UP)) {
                                problems.add(f + " floats at " + p);
                            }
                        }
                    }
                }
            }
        }
        h.assertTrue(problems.isEmpty(), "placing problems: " + problems);
        h.succeed();
    }

    /**
     * IG31-9: a click that lands one cell too high (on the side of a stone one above the floor) still places the door on the floor
     * (the item tries the cells within two down and one sideways before it refuses); the half doors have no recipe any more.
     */
    public static void placementToleratesAClickOneCellTooHigh(GameTestHelper h) {
        if (!McVersion.DOORS) {
            h.succeed();
            return;
        }
        ServerPlayer player = player(h);
        clear(h);
        BlockPos wall = new BlockPos(3, 3, 3);
        h.setBlock(wall.below(), Blocks.STONE);
        h.setBlock(wall, Blocks.STONE);
        ItemStack stack = new ItemStack(item(DoorFormat.FULL_2X2));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = h.absolutePos(wall);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).add(0.5, 0, 0), Direction.EAST, abs, false);
        boolean placed = stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction();
        BlockPos onFloor = wall.east().below();
        h.assertTrue(placed && at(h, onFloor).is(block(DoorFormat.FULL_2X2)) && at(h, onFloor).getValue(WideDoorBlock.ROW) == 0,
                "the door was not placed on the floor: " + placed + " / " + at(h, onFloor) + " / " + at(h, wall.east()));
        h.succeed();
    }

    /** By hand: a two-wide door swings its leaf into the area in front and leaves the free column open; closing restores it. */
    public static void openAndCloseByHand(GameTestHelper h) {
        if (!McVersion.DOORS) {
            h.succeed();
            return;
        }
        ServerPlayer player = player(h);
        List<String> problems = new ArrayList<>();
        for (DoorFormat f : formats()) {
            clear(h);
            place(h, player, f, ORIGIN);
            h.useBlock(hinge(0), player);
            if (!openAndWhole(h, f)) problems.add(f + " open state wrong: " + at(h, hinge(0)) + " / " + at(h, free(0)) + " / " + at(h, swing(0, 0)));
            // the other half: use a swing cell (a cell of the same door) closes it too
            h.useBlock(f.half ? hinge(f.rows - 1) : swing(f.rows - 1, 0), player);
            if (!closedAndWhole(h, f)) problems.add(f + " close state wrong: " + at(h, hinge(0)) + " / " + at(h, free(0)));
        }
        h.assertTrue(problems.isEmpty(), "open/close problems: " + problems);
        h.succeed();
    }

    /** D1 A: any block in the width x height area in front keeps a wide door shut; a block in the opening keeps it open. */
    public static void blockedSwingAndOpening(GameTestHelper h) {
        if (!McVersion.DOORS) {
            h.succeed();
            return;
        }
        ServerPlayer player = player(h);
        List<String> problems = new ArrayList<>();
        for (DoorFormat f : List.of(DoorFormat.FULL_2X2, DoorFormat.FULL_2X3, DoorFormat.WIDE_1_5X2, DoorFormat.WIDE_1_5X3)) {
            for (int blockedCol = 0; blockedCol < 2; blockedCol++) {
                clear(h);
                place(h, player, f, ORIGIN);
                h.setBlock(swing(f.rows - 1, blockedCol), Blocks.STONE);
                h.useBlock(hinge(0), player);
                if (!closedAndWhole(h, f)) problems.add(f + " opened with the area blocked at col " + blockedCol);
            }
            clear(h);
            place(h, player, f, ORIGIN);
            h.useBlock(hinge(0), player);
            h.setBlock(free(0), Blocks.STONE); // someone builds into the opening
            h.useBlock(hinge(0), player);
            if (!cell(h, hinge(0), block(f), 0, 0, false, true)) problems.add(f + " closed through a block in the opening: " + at(h, hinge(0)));
            h.setBlock(free(0), Blocks.AIR);
            h.useBlock(hinge(0), player);
            if (!closedAndWhole(h, f)) problems.add(f + " did not close once the opening was free");
        }
        h.assertTrue(problems.isEmpty(), "blocked door problems: " + problems);
        h.succeed();
    }

    /** D2: a half door needs no space, changes no cell but its open state, and its shape stays inside its own block. */
    public static void halfDoorsSwingInsideTheirBlock(GameTestHelper h) {
        if (!McVersion.DOORS) {
            h.succeed();
            return;
        }
        ServerPlayer player = player(h);
        List<String> problems = new ArrayList<>();
        for (DoorFormat f : List.of(DoorFormat.HALF_2, DoorFormat.HALF_3)) {
            clear(h);
            place(h, player, f, ORIGIN);
            h.setBlock(ORIGIN.north(), Blocks.STONE); // the area in front is full: a half door opens anyway
            h.setBlock(ORIGIN.north().above(), Blocks.STONE);
            AABB closed = at(h, ORIGIN).getShape(h.getLevel(), h.absolutePos(ORIGIN)).bounds();
            h.useBlock(hinge(0), player);
            if (!openAndWhole(h, f)) problems.add(f + " did not open in place: " + at(h, hinge(0)));
            AABB open = at(h, ORIGIN).getShape(h.getLevel(), h.absolutePos(ORIGIN)).bounds();
            if (!(near(closed.getXsize(), 0.5) && near(closed.minX, 0.0) && closed.minZ > 0.8)) problems.add(f + " closed leaf is not 8 px at the hinge edge: " + closed);
            if (!(near(open.minX, 0.0) && open.maxX < 0.2 && near(open.minZ, 0.5) && near(open.maxZ, 1.0))) problems.add(f + " open leaf is not 8 px deep at the hinge edge: " + open);
            h.useBlock(hinge(0), player);
            if (!closedAndWhole(h, f)) problems.add(f + " did not close");
        }
        h.assertTrue(problems.isEmpty(), "half door problems: " + problems);
        h.succeed();
    }

    /** The open leaf of a wide door lies along the hinge edge in the hinge cell and in the swing cell in front of it. */
    public static void openLeafShape(GameTestHelper h) {
        if (!McVersion.DOORS) {
            h.succeed();
            return;
        }
        ServerPlayer player = player(h);
        clear(h);
        place(h, player, DoorFormat.FULL_2X2, ORIGIN);
        AABB closed = at(h, ORIGIN).getShape(h.getLevel(), h.absolutePos(ORIGIN)).bounds();
        h.useBlock(hinge(0), player);
        AABB hingeLeaf = at(h, hinge(0)).getShape(h.getLevel(), h.absolutePos(hinge(0))).bounds();
        AABB swingLeaf = at(h, swing(0, 0)).getShape(h.getLevel(), h.absolutePos(swing(0, 0))).bounds();
        h.assertTrue(near(closed.getXsize(), 1.0) && near(closed.getZsize(), 3.0 / 16.0) && near(closed.minZ, 13.0 / 16.0), "closed slab: " + closed);
        h.assertTrue(near(hingeLeaf.minX, 0.0) && near(hingeLeaf.maxX, 3.0 / 16.0) && near(hingeLeaf.getZsize(), 1.0), "leaf in the hinge cell: " + hingeLeaf);
        h.assertTrue(near(swingLeaf.minX, 0.0) && near(swingLeaf.maxX, 3.0 / 16.0) && near(swingLeaf.getZsize(), 1.0), "leaf in the swing cell: " + swingLeaf);
        h.succeed();
    }

    /**
     * ABN30-5: the 1.5 wide door. Closed, the second column holds an 8 px stub on the hinge side (the leaf ends in the middle of the
     * block); open, the swing cell holds an 8 px stub next to the hinge cell; the swing area stays the full width x height.
     */
    public static void wide15LeafEndsInTheBlockMiddle(GameTestHelper h) {
        if (!McVersion.DOORS) {
            h.succeed();
            return;
        }
        ServerPlayer player = player(h);
        for (DoorFormat f : List.of(DoorFormat.WIDE_1_5X2, DoorFormat.WIDE_1_5X3)) {
            clear(h);
            place(h, player, f, ORIGIN);
            AABB first = at(h, ORIGIN).getShape(h.getLevel(), h.absolutePos(ORIGIN)).bounds();
            AABB second = at(h, ORIGIN.east()).getShape(h.getLevel(), h.absolutePos(ORIGIN.east())).bounds();
            h.assertTrue(near(first.getXsize(), 1.0) && near(second.getXsize(), 0.5) && near(second.minX, 0.0) && near(second.minZ, 13.0 / 16.0),
                    f + " closed: first " + first + ", second " + second);
            h.useBlock(hinge(0), player);
            AABB hingeLeaf = at(h, hinge(0)).getShape(h.getLevel(), h.absolutePos(hinge(0))).bounds();
            AABB swingLeaf = at(h, swing(0, 0)).getShape(h.getLevel(), h.absolutePos(swing(0, 0))).bounds();
            h.assertTrue(openAndWhole(h, f), f + " open state wrong: " + at(h, hinge(0)) + " / " + at(h, swing(0, 0)));
            h.assertTrue(near(hingeLeaf.getZsize(), 1.0) && near(swingLeaf.getZsize(), 0.5) && near(swingLeaf.minZ, 0.5) && near(swingLeaf.minX, 0.0),
                    f + " open: hinge leaf " + hingeLeaf + ", swing leaf " + swingLeaf);
            h.useBlock(hinge(0), player);
            h.assertTrue(closedAndWhole(h, f), f + " did not close");
        }
        h.succeed();
    }

    /** Redstone next to any cell opens the door on the rising edge and closes it on the falling edge; a hand moves only its own door. */
    public static void followRedstone(GameTestHelper h) {
        if (!McVersion.DOORS) {
            h.succeed();
            return;
        }
        ServerPlayer player = player(h);
        List<String> problems = new ArrayList<>();
        for (DoorFormat f : formats()) {
            clear(h);
            place(h, player, f, ORIGIN);
            BlockPos power = hinge(f.rows - 1).west(); // next to the top cell of the hinge column
            h.setBlock(power.below(), Blocks.STONE);
            h.setBlock(power, Blocks.REDSTONE_BLOCK);
            if (!openAndWhole(h, f)) problems.add(f + " did not open with redstone: " + at(h, hinge(0)));
            h.setBlock(power, Blocks.AIR);
            if (!closedAndWhole(h, f)) problems.add(f + " did not close without redstone: " + at(h, hinge(0)));
        }
        // two half doors side by side with one redstone block between: both open; a hand opens only the one it clicks
        clear(h);
        BlockPos a = new BlockPos(1, 2, 5), b = new BlockPos(3, 2, 5);
        place(h, player, DoorFormat.HALF_2, a);
        place(h, player, DoorFormat.HALF_2, b);
        h.setBlock(new BlockPos(2, 2, 5), Blocks.REDSTONE_BLOCK);
        if (!(at(h, a).getValue(WideDoorBlock.OPEN) && at(h, b).getValue(WideDoorBlock.OPEN))) {
            problems.add("redstone in the middle did not open both half doors: " + at(h, a) + " / " + at(h, b));
        }
        h.setBlock(new BlockPos(2, 2, 5), Blocks.AIR);
        h.useBlock(a, player);
        if (!(at(h, a).getValue(WideDoorBlock.OPEN) && !at(h, b).getValue(WideDoorBlock.OPEN))) problems.add("a hand did not open exactly one half door");
        h.assertTrue(problems.isEmpty(), "redstone problems: " + problems);
        h.succeed();
    }

    /**
     * A signal next to the free column (air while the door is open) still counts: the door opens at once, and closes by itself
     * a few ticks after the signal is gone (it polls while it is powered, the air cell cannot tell it).
     */
    public static void redstoneAtTheOpeningStillWorks(GameTestHelper h) {
        if (!McVersion.DOORS) {
            h.succeed();
            return;
        }
        ServerPlayer player = player(h);
        clear(h);
        place(h, player, DoorFormat.FULL_2X2, ORIGIN);
        BlockPos power = free(1).east();
        h.setBlock(power, Blocks.REDSTONE_BLOCK);
        h.assertTrue(openAndWhole(h, DoorFormat.FULL_2X2), "did not open: " + at(h, hinge(0)));
        h.runAfterDelay(20, () -> {
            h.assertTrue(openAndWhole(h, DoorFormat.FULL_2X2), "closed although still powered: " + at(h, hinge(0)));
            h.setBlock(power, Blocks.AIR);
        });
        h.runAfterDelay(45, () -> {
            h.assertTrue(closedAndWhole(h, DoorFormat.FULL_2X2), "did not close after the signal went: " + at(h, hinge(0)));
            h.succeed();
        });
    }

    /** Breaking any cell (closed or open, a swing cell too) removes the whole door and drops exactly one. */
    public static void breakAsUnit(GameTestHelper h) {
        if (!McVersion.DOORS) {
            h.succeed();
            return;
        }
        ServerPlayer player = player(h);
        ServerLevel level = h.getLevel();
        List<String> problems = new ArrayList<>();
        for (DoorFormat f : formats()) {
            for (boolean open : new boolean[] {false, true}) {
                clear(h);
                for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(ORIGIN)).inflate(10))) e.discard();
                place(h, player, f, ORIGIN);
                if (open) h.useBlock(hinge(0), player);
                BlockPos target = open && !f.half ? swing(f.rows - 1, 0) : ORIGIN.east(f.cols - 1).above(f.rows - 1);
                level.destroyBlock(h.absolutePos(target), true);
                for (int x = 0; x <= 7; x++) {
                    for (int y = 2; y <= 6; y++) {
                        for (int z = 3; z <= 6; z++) {
                            if (at(h, new BlockPos(x, y, z)).getBlock() instanceof WideDoorBlock) problems.add(f + (open ? " open" : " closed") + " left a cell at " + x + "," + y + "," + z);
                        }
                    }
                }
                int drops = 0;
                for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(ORIGIN)).inflate(10))) {
                    if (e.getItem().is(item(f))) drops += e.getItem().getCount();
                    else problems.add(f + " dropped " + e.getItem());
                }
                if (drops != 1) problems.add(f + (open ? " open" : " closed") + " dropped " + drops);
            }
        }
        h.assertTrue(problems.isEmpty(), "break problems: " + problems.subList(0, Math.min(6, problems.size())));
        h.succeed();
    }

    /** Pistons cannot push a door (a half pushed door would tear): every format is immovable. */
    public static void pistonsLeaveDoorsAlone(GameTestHelper h) {
        if (!McVersion.DOORS) {
            h.succeed();
            return;
        }
        ServerPlayer player = player(h);
        clear(h);
        place(h, player, DoorFormat.FULL_2X2, ORIGIN);
        for (DoorFormat f : formats()) {
            h.assertTrue(!PistonBaseBlock.isPushable(block(f).defaultBlockState(), h.getLevel(), h.absolutePos(ORIGIN), Direction.EAST, false, Direction.EAST),
                    f + " is pushable");
        }
        h.setBlock(ORIGIN.west().below(), Blocks.STONE);
        h.setBlock(ORIGIN.west(), Blocks.PISTON.defaultBlockState().setValue(net.minecraft.world.level.block.DirectionalBlock.FACING, Direction.EAST));
        h.setBlock(ORIGIN.west().north(), Blocks.REDSTONE_BLOCK); // powers the piston, touches no door cell
        h.runAfterDelay(8, () -> {
            h.assertTrue(closedAndWhole(h, DoorFormat.FULL_2X2), "the door moved: " + at(h, ORIGIN) + " / " + at(h, ORIGIN.east()));
            h.succeed();
        });
    }

    /** server.features.simpleDoors off: the doors stay shut under hand and redstone. */
    public static void switchOffKeepsDoorsShut(GameTestHelper h) {
        if (!McVersion.DOORS) {
            h.succeed();
            return;
        }
        ServerPlayer player = player(h);
        clear(h);
        place(h, player, DoorFormat.FULL_2X2, ORIGIN);
        boolean before = ServerTuning.get().features.simpleDoors;
        ServerTuning.get().features.simpleDoors = false;
        try {
            h.useBlock(hinge(0), player);
            h.setBlock(free(1).east(), Blocks.REDSTONE_BLOCK);
            h.assertTrue(closedAndWhole(h, DoorFormat.FULL_2X2), "the door opened with the switch off: " + at(h, hinge(0)));
        } finally {
            ServerTuning.get().features.simpleDoors = before;
        }
        h.succeed();
    }
}
