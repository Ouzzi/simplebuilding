package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * Server side behaviour tests for the mod's blocks and block entities.
 *
 * <p>Every test measures a difference against the vanilla counterpart instead of
 * re-stating a constant from the mod source: the furnace tests time how long a real
 * smelt takes, the hopper test times how long real item transfers take, and the
 * piston test actually pushes a block line that exceeds the vanilla limit of 12.
 */
public final class BlockBehaviourGameTest {

    /** Slot layout of {@link AbstractFurnaceBlockEntity}: input / fuel / result. */
    private static final int FURNACE_INPUT = 0;
    private static final int FURNACE_FUEL = 1;
    private static final int FURNACE_RESULT = 2;

    /** Number of items that must arrive in the destination chest before a hopper is "timed". */
    private static final int HOPPER_SAMPLE_SIZE = 5;

    // ------------------------------------------------------------------
    // Furnaces
    // ------------------------------------------------------------------

    /**
     * Reinforced and netherite furnaces add extra cook ticks per server tick, so the very
     * same recipe has to finish measurably earlier than in a vanilla furnace.
     * The test records the tick at which each furnace produces its first result and then
     * compares those three timings against each other.
     */
    @GameTest(maxTicks = 320)
    public void reinforcedAndNetheriteFurnacesSmeltFasterThanVanilla(GameTestHelper helper) {
        BlockPos vanilla = new BlockPos(1, 1, 1);
        BlockPos reinforced = new BlockPos(3, 1, 1);
        BlockPos netherite = new BlockPos(5, 1, 1);

        helper.setBlock(vanilla, Blocks.FURNACE);
        helper.setBlock(reinforced, ModBlocks.REINFORCED_FURNACE);
        helper.setBlock(netherite, ModBlocks.NETHERITE_FURNACE);

        loadFurnace(helper, vanilla, Items.RAW_IRON);
        loadFurnace(helper, reinforced, Items.RAW_IRON);
        loadFurnace(helper, netherite, Items.RAW_IRON);

        int[] finished = newTimings(3);
        helper.onEachTick(() -> {
            recordFurnaceFinish(helper, vanilla, finished, 0);
            recordFurnaceFinish(helper, reinforced, finished, 1);
            recordFurnaceFinish(helper, netherite, finished, 2);
        });

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(allTimed(finished),
                        "all three furnaces should have produced an iron ingot, timings: " + Arrays.toString(finished)))
                .thenExecute(() -> {
                    // The result item itself must be the smelting output, not the input.
                    assertResultIs(helper, vanilla, Items.IRON_INGOT);
                    assertResultIs(helper, reinforced, Items.IRON_INGOT);
                    assertResultIs(helper, netherite, Items.IRON_INGOT);

                    helper.assertTrue(finished[2] < finished[1],
                            "netherite furnace should smelt faster than the reinforced one, timings: "
                                    + Arrays.toString(finished));
                    helper.assertTrue(finished[1] < finished[0],
                            "reinforced furnace should smelt faster than the vanilla one, timings: "
                                    + Arrays.toString(finished));
                    // extraTicks 3 means roughly four times the vanilla speed; require at least 3x.
                    helper.assertTrue(finished[0] >= finished[2] * 3,
                            "netherite furnace should be at least three times as fast as vanilla, timings: "
                                    + Arrays.toString(finished));
                    // extraTicks 1 means roughly twice the vanilla speed; require at least 1.5x.
                    helper.assertTrue(finished[0] * 2 >= finished[1] * 3,
                            "reinforced furnace should be at least 1.5 times as fast as vanilla, timings: "
                                    + Arrays.toString(finished));
                })
                .thenSucceed();
    }

    /**
     * The same speed-up has to apply to the blast furnace and the smoker variants, which use
     * the blasting / smoking recipe types with their own (shorter) vanilla cook time.
     */
    @GameTest(maxTicks = 220)
    public void reinforcedAndNetheriteBlastFurnacesAndSmokersOutpaceVanilla(GameTestHelper helper) {
        BlockPos vanillaBlast = new BlockPos(1, 1, 1);
        BlockPos reinforcedBlast = new BlockPos(3, 1, 1);
        BlockPos netheriteBlast = new BlockPos(5, 1, 1);
        BlockPos vanillaSmoker = new BlockPos(1, 1, 4);
        BlockPos reinforcedSmoker = new BlockPos(3, 1, 4);
        BlockPos netheriteSmoker = new BlockPos(5, 1, 4);

        helper.setBlock(vanillaBlast, Blocks.BLAST_FURNACE);
        helper.setBlock(reinforcedBlast, ModBlocks.REINFORCED_BLAST_FURNACE);
        helper.setBlock(netheriteBlast, ModBlocks.NETHERITE_BLAST_FURNACE);
        helper.setBlock(vanillaSmoker, Blocks.SMOKER);
        helper.setBlock(reinforcedSmoker, ModBlocks.REINFORCED_SMOKER);
        helper.setBlock(netheriteSmoker, ModBlocks.NETHERITE_SMOKER);

        loadFurnace(helper, vanillaBlast, Items.RAW_IRON);
        loadFurnace(helper, reinforcedBlast, Items.RAW_IRON);
        loadFurnace(helper, netheriteBlast, Items.RAW_IRON);
        loadFurnace(helper, vanillaSmoker, Items.BEEF);
        loadFurnace(helper, reinforcedSmoker, Items.BEEF);
        loadFurnace(helper, netheriteSmoker, Items.BEEF);

        int[] finished = newTimings(6);
        helper.onEachTick(() -> {
            recordFurnaceFinish(helper, vanillaBlast, finished, 0);
            recordFurnaceFinish(helper, reinforcedBlast, finished, 1);
            recordFurnaceFinish(helper, netheriteBlast, finished, 2);
            recordFurnaceFinish(helper, vanillaSmoker, finished, 3);
            recordFurnaceFinish(helper, reinforcedSmoker, finished, 4);
            recordFurnaceFinish(helper, netheriteSmoker, finished, 5);
        });

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(allTimed(finished),
                        "all six machines should have produced their result, timings: " + Arrays.toString(finished)))
                .thenExecute(() -> {
                    assertResultIs(helper, netheriteBlast, Items.IRON_INGOT);
                    assertResultIs(helper, netheriteSmoker, Items.COOKED_BEEF);

                    helper.assertTrue(finished[2] < finished[1] && finished[1] < finished[0],
                            "blast furnaces should be ordered netherite < reinforced < vanilla, timings: "
                                    + Arrays.toString(finished));
                    helper.assertTrue(finished[5] < finished[4] && finished[4] < finished[3],
                            "smokers should be ordered netherite < reinforced < vanilla, timings: "
                                    + Arrays.toString(finished));
                    // Blasting/smoking only take 100 vanilla ticks, so use a slightly softer
                    // factor of 2.5 than in the smelting test to stay robust.
                    helper.assertTrue(finished[0] * 2 >= finished[2] * 5,
                            "netherite blast furnace should be at least 2.5 times as fast as vanilla, timings: "
                                    + Arrays.toString(finished));
                    helper.assertTrue(finished[3] * 2 >= finished[5] * 5,
                            "netherite smoker should be at least 2.5 times as fast as vanilla, timings: "
                                    + Arrays.toString(finished));
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------------
    // Hoppers
    // ------------------------------------------------------------------

    /**
     * Three identical chest -> hopper -> chest stacks are built next to each other. The test
     * times how long each hopper needs to move {@value #HOPPER_SAMPLE_SIZE} items into the
     * lower chest, which exposes the shorter transfer cooldown of the mod hoppers.
     */
    @GameTest(maxTicks = 200)
    public void reinforcedAndNetheriteHoppersMoveItemsFasterThanVanilla(GameTestHelper helper) {
        BlockPos vanilla = new BlockPos(1, 2, 1);
        BlockPos reinforced = new BlockPos(3, 2, 1);
        BlockPos netherite = new BlockPos(5, 2, 1);

        buildHopperStack(helper, vanilla, Blocks.HOPPER);
        buildHopperStack(helper, reinforced, ModBlocks.REINFORCED_HOPPER);
        buildHopperStack(helper, netherite, ModBlocks.NETHERITE_HOPPER);

        int[] finished = newTimings(3);
        helper.onEachTick(() -> {
            recordHopperThroughput(helper, vanilla.below(), finished, 0);
            recordHopperThroughput(helper, reinforced.below(), finished, 1);
            recordHopperThroughput(helper, netherite.below(), finished, 2);
        });

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(allTimed(finished),
                        "every hopper should have delivered " + HOPPER_SAMPLE_SIZE + " items, timings: "
                                + Arrays.toString(finished)))
                .thenExecute(() -> {
                    helper.assertTrue(finished[2] < finished[1],
                            "netherite hopper should be faster than the reinforced one, timings: "
                                    + Arrays.toString(finished));
                    helper.assertTrue(finished[1] < finished[0],
                            "reinforced hopper should be faster than the vanilla one, timings: "
                                    + Arrays.toString(finished));
                    // Cooldown 2 vs. 8 ticks; require at least a factor of two to stay robust.
                    helper.assertTrue(finished[0] >= finished[2] * 2,
                            "netherite hopper should need at most half the time of the vanilla hopper, timings: "
                                    + Arrays.toString(finished));
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------------
    // Pistons
    // ------------------------------------------------------------------

    /**
     * The mod raises the push limit for the reinforced piston from 12 to 18 blocks.
     * Two identical 13 block columns are built, one on a vanilla piston and one on a
     * reinforced piston; only the reinforced one may move.
     */
    @GameTest(maxTicks = 60, skyAccess = true)
    public void reinforcedPistonPushesThirteenBlocksWhereVanillaPistonRefuses(GameTestHelper helper) {
        int columnHeight = 13; // one more than the vanilla limit of 12
        BlockPos reinforcedPiston = new BlockPos(1, 1, 1);
        BlockPos vanillaPiston = new BlockPos(5, 1, 1);

        setUpUpwardPiston(helper, reinforcedPiston, ModBlocks.REINFORCED_PISTON, columnHeight);
        setUpUpwardPiston(helper, vanillaPiston, Blocks.PISTON, columnHeight);

        // Power both pistons in the same tick.
        helper.setBlock(reinforcedPiston.east(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(vanillaPiston.east(), Blocks.REDSTONE_BLOCK);

        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    // Reinforced piston: extended, head in front of it, column shifted up by one.
                    helper.assertBlockProperty(reinforcedPiston, PistonBaseBlock.EXTENDED, Boolean.TRUE);
                    helper.assertBlockPresent(Blocks.PISTON_HEAD, reinforcedPiston.above());
                    helper.assertBlockPresent(Blocks.STONE, reinforcedPiston.above(columnHeight + 1));
                    helper.assertBlockPresent(Blocks.STONE, reinforcedPiston.above(2));

                    // Vanilla piston: 13 blocks are over its limit, so nothing moved at all.
                    helper.assertBlockProperty(vanillaPiston, PistonBaseBlock.EXTENDED, Boolean.FALSE);
                    helper.assertBlockNotPresent(Blocks.STONE, vanillaPiston.above(columnHeight + 1));
                    helper.assertBlockPresent(Blocks.STONE, vanillaPiston.above());
                })
                .thenSucceed();
    }

    /**
     * The netherite piston breaks the block in front of it instead of pushing it, as long as the
     * redstone signal is strong enough for the block's hardness. A vanilla piston in the identical
     * setup simply pushes the same block one block further.
     */
    @GameTest(maxTicks = 60)
    public void netheritePistonBreaksTheBlockInFrontWhileVanillaPistonPushesIt(GameTestHelper helper) {
        BlockPos netheritePiston = new BlockPos(1, 1, 1);
        BlockPos vanillaPiston = new BlockPos(5, 1, 1);

        helper.setBlock(netheritePiston,
                ModBlocks.NETHERITE_PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.UP));
        helper.setBlock(netheritePiston.above(), Blocks.STONE);
        helper.setBlock(vanillaPiston,
                Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.UP));
        helper.setBlock(vanillaPiston.above(), Blocks.STONE);

        // Full strength signal: threshold is (15/15)*50 = 50, far above stone's hardness of 1.5.
        helper.setBlock(netheritePiston.east(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(vanillaPiston.east(), Blocks.REDSTONE_BLOCK);

        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    // Netherite piston: the stone was destroyed, so nothing was moved upwards.
                    helper.assertBlockPresent(Blocks.PISTON_HEAD, netheritePiston.above());
                    helper.assertBlockNotPresent(Blocks.STONE, netheritePiston.above(2));

                    // Vanilla piston: the very same stone block simply travelled one block up.
                    helper.assertBlockPresent(Blocks.PISTON_HEAD, vanillaPiston.above());
                    helper.assertBlockPresent(Blocks.STONE, vanillaPiston.above(2));
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------------
    // Suspended / levitating sand and gravel
    // ------------------------------------------------------------------

    /**
     * Suspended sand and gravel are plain blocks and must stay where they are put, while the
     * vanilla counterparts fall down onto the floor. Both are placed in the very same tick at
     * the same height so the comparison is meaningful.
     */
    @GameTest(maxTicks = 60)
    public void suspendedSandAndGravelStayInPlaceWhileVanillaOnesFall(GameTestHelper helper) {
        BlockPos vanillaSand = new BlockPos(1, 4, 1);
        BlockPos suspendedSand = new BlockPos(3, 4, 1);
        BlockPos vanillaGravel = new BlockPos(1, 4, 3);
        BlockPos suspendedGravel = new BlockPos(3, 4, 3);

        // Give every column a solid landing surface so a falling block ends up at y = 1.
        helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 0, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 0, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 0, 3), Blocks.STONE);

        helper.setBlock(vanillaSand, Blocks.SAND);
        helper.setBlock(suspendedSand, ModBlocks.SUSPENDED_SAND);
        helper.setBlock(vanillaGravel, Blocks.GRAVEL);
        helper.setBlock(suspendedGravel, ModBlocks.SUSPENDED_GRAVEL);

        helper.startSequence()
                .thenExecuteAfter(40, () -> {
                    // Vanilla gravity blocks left their spot and landed on the floor.
                    helper.assertBlockNotPresent(Blocks.SAND, vanillaSand);
                    helper.assertBlockPresent(Blocks.SAND, new BlockPos(1, 1, 1));
                    helper.assertBlockNotPresent(Blocks.GRAVEL, vanillaGravel);
                    helper.assertBlockPresent(Blocks.GRAVEL, new BlockPos(1, 1, 3));

                    // The suspended variants never moved.
                    helper.assertBlockPresent(ModBlocks.SUSPENDED_SAND, suspendedSand);
                    helper.assertBlockPresent(ModBlocks.SUSPENDED_GRAVEL, suspendedGravel);
                    helper.assertBlockNotPresent(ModBlocks.SUSPENDED_SAND, new BlockPos(3, 1, 1));
                    helper.assertBlockNotPresent(ModBlocks.SUSPENDED_GRAVEL, new BlockPos(3, 1, 3));
                })
                .thenSucceed();
    }

    /**
     * Levitating sand and gravel climb upwards on their own scheduled ticks, leaving air behind.
     * A vanilla sand block placed on the same floor is used as the control that does not move.
     */
    @GameTest(maxTicks = 80)
    public void levitatingSandAndGravelRiseUpwardsInsteadOfStayingPut(GameTestHelper helper) {
        BlockPos levitatingSandStart = new BlockPos(1, 1, 1);
        BlockPos levitatingGravelStart = new BlockPos(3, 1, 1);
        BlockPos controlSand = new BlockPos(5, 1, 1);

        helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 0, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 0, 1), Blocks.STONE);

        helper.setBlock(levitatingSandStart, ModBlocks.LEVITATING_SAND);
        helper.setBlock(levitatingGravelStart, ModBlocks.LEVITATING_GRAVEL);
        helper.setBlock(controlSand, Blocks.SAND);

        helper.startSequence()
                .thenExecuteAfter(40, () -> {
                    helper.assertBlockNotPresent(ModBlocks.LEVITATING_SAND, levitatingSandStart);
                    helper.assertBlockNotPresent(ModBlocks.LEVITATING_GRAVEL, levitatingGravelStart);

                    int sandY = findBlockInColumn(helper, ModBlocks.LEVITATING_SAND, 1, 1);
                    int gravelY = findBlockInColumn(helper, ModBlocks.LEVITATING_GRAVEL, 3, 1);

                    helper.assertTrue(sandY >= levitatingSandStart.getY() + 3,
                            "levitating sand should have climbed at least three blocks, found at y=" + sandY);
                    helper.assertTrue(gravelY >= levitatingGravelStart.getY() + 3,
                            "levitating gravel should have climbed at least three blocks, found at y=" + gravelY);

                    // Control: an ordinary sand block on the same floor does not move.
                    helper.assertBlockPresent(Blocks.SAND, controlSand);
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static int[] newTimings(int size) {
        int[] timings = new int[size];
        Arrays.fill(timings, -1);
        return timings;
    }

    private static boolean allTimed(int[] timings) {
        for (int timing : timings) {
            if (timing < 0) {
                return false;
            }
        }
        return true;
    }

    private static void loadFurnace(GameTestHelper helper, BlockPos pos, Item input) {
        AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(pos, AbstractFurnaceBlockEntity.class);
        furnace.setItem(FURNACE_INPUT, new ItemStack(input, 8));
        furnace.setItem(FURNACE_FUEL, new ItemStack(Items.COAL, 8));
    }

    private static void recordFurnaceFinish(GameTestHelper helper, BlockPos pos, int[] timings, int index) {
        if (timings[index] >= 0) {
            return;
        }
        AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(pos, AbstractFurnaceBlockEntity.class);
        if (!furnace.getItem(FURNACE_RESULT).isEmpty()) {
            timings[index] = (int) helper.getTick();
        }
    }

    private static void assertResultIs(GameTestHelper helper, BlockPos pos, Item expected) {
        AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(pos, AbstractFurnaceBlockEntity.class);
        helper.assertTrue(furnace.getItem(FURNACE_RESULT).is(expected),
                "furnace at " + pos + " should contain the smelting result");
    }

    /** Builds source chest -> hopper (facing down) -> destination chest and fills the source. */
    private static void buildHopperStack(GameTestHelper helper, BlockPos hopperPos, Block hopper) {
        helper.setBlock(hopperPos.below(), Blocks.CHEST);
        helper.setBlock(hopperPos, hopper.defaultBlockState()
                .setValue(HopperBlock.FACING, Direction.DOWN)
                .setValue(HopperBlock.ENABLED, Boolean.TRUE));
        helper.setBlock(hopperPos.above(), Blocks.CHEST);

        ChestBlockEntity source = helper.getBlockEntity(hopperPos.above(), ChestBlockEntity.class);
        source.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
    }

    private static void recordHopperThroughput(GameTestHelper helper, BlockPos chestPos, int[] timings, int index) {
        if (timings[index] >= 0) {
            return;
        }
        if (countItems(helper, chestPos) >= HOPPER_SAMPLE_SIZE) {
            timings[index] = (int) helper.getTick();
        }
    }

    private static int countItems(GameTestHelper helper, BlockPos chestPos) {
        ChestBlockEntity chest = helper.getBlockEntity(chestPos, ChestBlockEntity.class);
        int total = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            total += chest.getItem(slot).getCount();
        }
        return total;
    }

    /** Places an upward facing piston with a solid column of the given height on top of it. */
    private static void setUpUpwardPiston(GameTestHelper helper, BlockPos pistonPos, Block piston, int columnHeight) {
        // Make sure the whole travel path is empty, it reaches above the 8 block test structure.
        for (int y = 1; y <= columnHeight + 3; y++) {
            helper.setBlock(pistonPos.above(y), Blocks.AIR);
        }
        helper.setBlock(pistonPos, piston.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.UP));
        for (int y = 1; y <= columnHeight; y++) {
            helper.setBlock(pistonPos.above(y), Blocks.STONE);
        }
    }

    /** Returns the lowest y (test relative) above y=1 at which the given block sits, or -1. */
    private static int findBlockInColumn(GameTestHelper helper, Block block, int x, int z) {
        for (int y = 2; y <= 24; y++) {
            BlockState state = helper.getBlockState(new BlockPos(x, y, z));
            if (state.is(block)) {
                return y;
            }
        }
        return -1;
    }
}
