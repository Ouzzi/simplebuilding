package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.ModTags;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * The enderite tier of the machines: the enderite hopper, furnace, smoker and blast furnace that
 * exist only as the second hammer step ({@code SledgehammerUpgrades}), plus the enderite piston's item.
 *
 * <p>They reuse the classes and block entities of the two tiers below and differ in a handful of
 * appended branches: a transfer cooldown of one tick in {@code ModHopperBlockEntity}, seven extra
 * points of cooking progress per tick in the three furnace block entities, their own container
 * titles, and their registration - fire resistant EPIC items, a loot table each, and a place in the
 * block entity type of their family on every loader. {@code HopperTests} and {@code FurnaceTests}
 * already cover strength, sound and mining tag, and the furnaces' drops; this file measures what the
 * enderite branches do.
 *
 * <p><b>Not covered:</b> textures and models (client side), and the creative tab entries (restating
 * {@code ModItemGroupsContent}; {@code DataIntegrityTests} already requires every item to be offered).
 */
public final class EnderiteMachineTests {

    private EnderiteMachineTests() {
    }

    /** Tick budget for {@link #enderiteHopperMovesAnItemEveryTick}. */
    public static final int HOPPER_MAX_TICKS = 60;

    /** Tick budget for {@link #enderiteFurnacesCookEightTimesAsFastAsVanilla}. */
    public static final int FURNACE_MAX_TICKS = 80;

    /** Tick budget for {@link #enderiteHopperAndPistonDropThemselvesWhenBroken}. */
    public static final int DROP_MAX_TICKS = 40;

    /** Items that must arrive before a hopper's cadence is read. */
    private static final int HOPPER_SAMPLE_SIZE = 5;

    /** Cooking speed of the enderite tier as a multiple of vanilla: one point plus seven. */
    private static final int ENDERITE_SPEED = 8;

    /** Search radius around each dropped machine; the machines stand four blocks apart. */
    private static final double DROP_RADIUS = 1.0;

    // =====================================================================================
    // WHAT THE ENDERITE BRANCHES DO
    // =====================================================================================

    /**
     * An enderite hopper moves cobblestone from a chest above into a chest below and the tick of
     * every arrival is recorded. A hopper moves one item per cooldown, so the distance between two
     * arrivals is its cooldown, measured in the room: the enderite hopper has to deliver on every
     * single tick. A netherite hopper beside it is the control that the rig measures cooldowns at all
     * - it has to deliver every second tick.
     *
     * <p>What breaks this test: any other cooldown than 1 in the enderite branch of
     * {@code ModHopperBlockEntity#insertAndExtract}, or the branch missing (the enderite hopper then
     * falls back to vanilla's 8).
     */
    public static void enderiteHopperMovesAnItemEveryTick(GameTestHelper helper) {
        BlockPos netherite = new BlockPos(2, 2, 2);
        BlockPos enderite = new BlockPos(5, 2, 2);
        buildHopperStack(helper, netherite, ModBlocks.NETHERITE_HOPPER);
        buildHopperStack(helper, enderite, ModBlocks.ENDERITE_HOPPER);

        int[][] arrivals = {newTimings(HOPPER_SAMPLE_SIZE), newTimings(HOPPER_SAMPLE_SIZE)};
        helper.onEachTick(() -> {
            recordArrivals(helper, netherite.below(), arrivals[0]);
            recordArrivals(helper, enderite.below(), arrivals[1]);
        });

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(allTimed(arrivals[0]) && allTimed(arrivals[1]),
                        "both hoppers should have delivered " + HOPPER_SAMPLE_SIZE + " items, arrival ticks: "
                                + Arrays.deepToString(arrivals)))
                .thenExecute(() -> {
                    helper.assertTrue(gaps(arrivals[0]).equals("2 2 2 2"),
                            "control broken: the netherite hopper does not deliver every second tick, arrival ticks "
                                    + Arrays.toString(arrivals[0]));
                    helper.assertValueEqual(gaps(arrivals[1]), "1 1 1 1",
                            "ticks between the items from the enderite hopper, arrival ticks " + Arrays.toString(arrivals[1]));
                })
                .thenSucceed();
    }

    /**
     * The enderite furnace, smoker and blast furnace are timed from the tick they light to the tick
     * their first result appears. Vanilla needs the recipe's own cooking time for that - read from
     * the recipe, not assumed. A device that adds seven points of progress per tick on top of
     * vanilla's one climbs eight points a tick until the boost's cap holds it one short of the total,
     * and vanilla's own next point finishes the item: {@code ceil((cookingTime - 1) / 8)} ticks from
     * lighting to result - 25 for raw iron in the furnace, 13 for beef in the smoker and for raw iron
     * in the blast furnace. Six or eight extra points land on 29 / 15 and 23 / 11, so the counts pin
     * the step exactly, in each of the three copied tick methods; all three are compared in one line,
     * so a broken family shows next to the two others.
     *
     * <p>What breaks this test: the enderite branch missing from, or adding anything but 7 in, any of
     * {@code ModFurnaceBlockEntity#tick}, {@code ModSmokerBlockEntity#tick} and
     * {@code ModBlastFurnaceBlockEntity#tick}.
     */
    public static void enderiteFurnacesCookEightTimesAsFastAsVanilla(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos furnace = new BlockPos(1, 1, 1);
        BlockPos smoker = new BlockPos(3, 1, 1);
        BlockPos blast = new BlockPos(5, 1, 1);
        helper.setBlock(furnace, ModBlocks.ENDERITE_FURNACE);
        helper.setBlock(smoker, ModBlocks.ENDERITE_SMOKER);
        helper.setBlock(blast, ModBlocks.ENDERITE_BLAST_FURNACE);
        load(helper, furnace, Items.RAW_IRON);
        load(helper, smoker, Items.BEEF);
        load(helper, blast, Items.RAW_IRON);

        int smeltTime = level.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(new ItemStack(Items.RAW_IRON)), level)
                .orElseThrow().value().cookingTime();
        int smokeTime = level.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.SMOKING, new SingleRecipeInput(new ItemStack(Items.BEEF)), level)
                .orElseThrow().value().cookingTime();
        int blastTime = level.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.BLASTING, new SingleRecipeInput(new ItemStack(Items.RAW_IRON)), level)
                .orElseThrow().value().cookingTime();

        List<BlockPos> devices = List.of(furnace, smoker, blast);
        int[] lit = newTimings(3);
        int[] finished = newTimings(3);
        helper.onEachTick(() -> {
            for (int index = 0; index < devices.size(); index++) {
                BlockPos pos = devices.get(index);
                if (lit[index] < 0 && helper.getBlockState(pos).getValue(AbstractFurnaceBlock.LIT)) {
                    lit[index] = (int) helper.getTick();
                }
                if (finished[index] < 0 && !furnace(helper, pos).getItem(2).isEmpty()) {
                    finished[index] = (int) helper.getTick();
                }
            }
        });

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(allTimed(finished),
                        "all three enderite devices should have finished a result, timings: " + Arrays.toString(finished)))
                .thenExecute(() -> helper.assertValueEqual(
                        "furnace " + (finished[0] - lit[0]) + ", smoker " + (finished[1] - lit[1])
                                + ", blast furnace " + (finished[2] - lit[2]),
                        "furnace " + ceilDiv(smeltTime - 1, ENDERITE_SPEED) + ", smoker " + ceilDiv(smokeTime - 1, ENDERITE_SPEED)
                                + ", blast furnace " + ceilDiv(blastTime - 1, ENDERITE_SPEED),
                        "ticks the enderite devices need for a " + smeltTime + " / " + smokeTime + " / " + blastTime
                                + " tick recipe"))
                .thenSucceed();
    }

    // =====================================================================================
    // REGISTRATION
    // =====================================================================================

    /**
     * The five enderite machine items - hopper, furnace, smoker, blast furnace and piston - survive
     * lava, are EPIC like the other enderite equipment, and are in {@code simplebuilding:void_protected},
     * the tag the void rescue reads. All fifteen answers are compared in one list. Their netherite
     * counterparts are the controls: fire resistant too, but neither EPIC nor void protected.
     *
     * <p>What breaks this test: {@code fireResistant()} or {@code rarity(Rarity.EPIC)} dropped from
     * one of the five registrations in {@code ModItems}, and a void protection tag that was not
     * regenerated after the items were added.
     */
    public static void enderiteMachineItemsAreFireResistantEpicAndVoidProtected(GameTestHelper helper) {
        List<Item> enderite = List.of(ModItems.ENDERITE_HOPPER, ModItems.ENDERITE_FURNACE, ModItems.ENDERITE_SMOKER,
                ModItems.ENDERITE_BLAST_FURNACE, ModItems.ENDERITE_PISTON);
        List<Item> netherite = List.of(ModItems.NETHERITE_HOPPER, ModItems.NETHERITE_FURNACE, ModItems.NETHERITE_SMOKER,
                ModItems.NETHERITE_BLAST_FURNACE, ModItems.NETHERITE_PISTON);

        StringBuilder actual = new StringBuilder();
        StringBuilder expected = new StringBuilder();
        for (Item item : enderite) {
            actual.append(traits(helper, item)).append("; ");
            expected.append(BuiltInRegistries.ITEM.getKey(item)).append(": fire resistant, epic, void protected; ");
        }
        helper.assertValueEqual(actual.toString(), expected.toString(), "what the enderite machine items are");

        for (Item item : netherite) {
            helper.assertTrue(traits(helper, item).equals(BuiltInRegistries.ITEM.getKey(item) + ": fire resistant, common, lost to the void"),
                    "control broken: " + traits(helper, item));
        }

        helper.succeed();
    }

    /**
     * The enderite hopper and the enderite piston are broken in the level and each leaves exactly one
     * of its own item on the ground, and nothing else. (The three enderite furnaces are broken the same
     * way in {@code FurnaceTests}; every loot table is rolled in {@code DataIntegrityTests}.)
     *
     * <p>What breaks this test: either loot table missing, or naming another item.
     */
    public static void enderiteHopperAndPistonDropThemselvesWhenBroken(GameTestHelper helper) {
        BlockPos hopper = new BlockPos(1, 1, 3);
        BlockPos piston = new BlockPos(5, 1, 3);
        helper.setBlock(hopper, ModBlocks.ENDERITE_HOPPER);
        helper.setBlock(piston, ModBlocks.ENDERITE_PISTON);

        helper.assertTrue(helper.getLevel().destroyBlock(helper.absolutePos(hopper), true), "could not break the enderite hopper");
        helper.assertTrue(helper.getLevel().destroyBlock(helper.absolutePos(piston), true), "could not break the enderite piston");

        helper.runAfterDelay(3, () -> {
            helper.assertValueEqual("hopper: " + dropsAround(helper, hopper) + ", piston: " + dropsAround(helper, piston),
                    "hopper: 1 simplebuilding:enderite_hopper, piston: 1 simplebuilding:enderite_piston",
                    "what the broken enderite hopper and piston left on the ground");
            helper.succeed();
        });
    }

    /**
     * Each family's block entity type accepts all three of its tiers. This is the registration the
     * hammer upgrade depends on: the kept block entity is re-validated against the new block, so a
     * type that does not list the enderite block would crash the upgrade, and placing the block would
     * crash as well. Checked on whichever loader runs the test - the lists are kept per loader
     * (Fabric, NeoForge, Forge). A block from another family is the control that the check can say
     * no.
     *
     * <p>The three enderite furnaces also carry their own container title,
     * {@code container.simplebuilding.enderite_*}, rather than the netherite sibling's.
     *
     * <p>What breaks this test: an {@code ENDERITE_*} block missing from one of the four block entity
     * types in {@code ModBlockEntities} / {@code NeoForgeModRegistries}, and a {@code getDefaultName}
     * without its enderite branch.
     */
    public static void enderiteMachinesFitTheirBlockEntityTypesAndTitles(GameTestHelper helper) {
        StringBuilder refused = new StringBuilder();
        refused.append(refusedBy(ModBlockEntities.MOD_HOPPER_BE,
                ModBlocks.REINFORCED_HOPPER, ModBlocks.NETHERITE_HOPPER, ModBlocks.ENDERITE_HOPPER));
        refused.append(refusedBy(ModBlockEntities.MOD_FURNACE_BE,
                ModBlocks.REINFORCED_FURNACE, ModBlocks.NETHERITE_FURNACE, ModBlocks.ENDERITE_FURNACE));
        refused.append(refusedBy(ModBlockEntities.MOD_SMOKER_BE,
                ModBlocks.REINFORCED_SMOKER, ModBlocks.NETHERITE_SMOKER, ModBlocks.ENDERITE_SMOKER));
        refused.append(refusedBy(ModBlockEntities.MOD_BLAST_FURNACE_BE,
                ModBlocks.REINFORCED_BLAST_FURNACE, ModBlocks.NETHERITE_BLAST_FURNACE, ModBlocks.ENDERITE_BLAST_FURNACE));
        helper.assertValueEqual(refused.toString(), "",
                "tiers their family's block entity type refuses (placing them, or hammering the tier below into them, crashes)");
        helper.assertTrue(!ModBlockEntities.MOD_FURNACE_BE.isValid(ModBlocks.ENDERITE_SMOKER.defaultBlockState()),
                "control broken: the furnace block entity type accepts the enderite smoker");

        BlockPos furnace = new BlockPos(1, 1, 1);
        BlockPos smoker = new BlockPos(3, 1, 1);
        BlockPos blast = new BlockPos(5, 1, 1);
        helper.setBlock(furnace, ModBlocks.ENDERITE_FURNACE);
        helper.setBlock(smoker, ModBlocks.ENDERITE_SMOKER);
        helper.setBlock(blast, ModBlocks.ENDERITE_BLAST_FURNACE);
        helper.assertValueEqual(titleKey(helper, furnace) + ", " + titleKey(helper, smoker) + ", " + titleKey(helper, blast),
                "container.simplebuilding.enderite_furnace, container.simplebuilding.enderite_smoker, "
                        + "container.simplebuilding.enderite_blast_furnace",
                "container titles of the enderite furnace, smoker and blast furnace");

        helper.succeed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    private static AbstractFurnaceBlockEntity furnace(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, AbstractFurnaceBlockEntity.class);
    }

    private static void load(GameTestHelper helper, BlockPos pos, Item input) {
        AbstractFurnaceBlockEntity entity = furnace(helper, pos);
        entity.setItem(0, new ItemStack(input, 8));
        entity.setItem(1, new ItemStack(Items.COAL, 8));
    }

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

    private static int ceilDiv(int dividend, int divisor) {
        return (dividend + divisor - 1) / divisor;
    }

    /** Source chest, a hopper pointing down, destination chest. */
    private static void buildHopperStack(GameTestHelper helper, BlockPos hopperPos, Block hopper) {
        helper.setBlock(hopperPos.below(), Blocks.CHEST);
        helper.setBlock(hopperPos, hopper.defaultBlockState()
                .setValue(HopperBlock.FACING, Direction.DOWN)
                .setValue(HopperBlock.ENABLED, Boolean.TRUE));
        helper.setBlock(hopperPos.above(), Blocks.CHEST);
        helper.getBlockEntity(hopperPos.above(), ChestBlockEntity.class).setItem(0, new ItemStack(Items.COBBLESTONE, 64));
    }

    private static void recordArrivals(GameTestHelper helper, BlockPos chestPos, int[] arrivals) {
        ChestBlockEntity chest = helper.getBlockEntity(chestPos, ChestBlockEntity.class);
        int delivered = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            delivered += chest.getItem(slot).getCount();
        }
        for (int item = 0; item < arrivals.length && item < delivered; item++) {
            if (arrivals[item] < 0) {
                arrivals[item] = (int) helper.getTick();
            }
        }
    }

    /** The ticks between consecutive arrivals, e.g. "2 2 2 2" for a hopper that delivers every second tick. */
    private static String gaps(int[] arrivals) {
        StringBuilder gaps = new StringBuilder();
        for (int item = 1; item < arrivals.length; item++) {
            gaps.append(item > 1 ? " " : "").append(arrivals[item] - arrivals[item - 1]);
        }
        return gaps.toString();
    }

    /** What an item is, in the words the assertions compare: fire resistance, rarity, void protection. */
    private static String traits(GameTestHelper helper, Item item) {
        ItemStack stack = new ItemStack(item);
        DamageResistant resistant = stack.get(DataComponents.DAMAGE_RESISTANT);
        boolean fireResistant = resistant != null && resistant.isResistantTo(helper.getLevel().damageSources().lava());
        return BuiltInRegistries.ITEM.getKey(item) + ": "
                + (fireResistant ? "fire resistant" : "burns") + ", "
                + (stack.get(DataComponents.RARITY) == Rarity.EPIC ? "epic" : "common") + ", "
                + (stack.typeHolder().is(ModTags.Items.VOID_PROTECTED) ? "void protected" : "lost to the void");
    }

    /** The items lying within {@link #DROP_RADIUS} of {@code pos}, e.g. "1 simplebuilding:enderite_hopper". */
    private static String dropsAround(GameTestHelper helper, BlockPos pos) {
        StringBuilder drops = new StringBuilder();
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(helper.absolutePos(pos)).inflate(DROP_RADIUS))) {
            drops.append(drops.length() > 0 ? " + " : "").append(entity.getItem().getCount()).append(" ")
                    .append(BuiltInRegistries.ITEM.getKey(entity.getItem().getItem()));
        }
        return drops.length() == 0 ? "nothing" : drops.toString();
    }

    private static String refusedBy(BlockEntityType<?> type, Block... tiers) {
        StringBuilder refused = new StringBuilder();
        for (Block tier : tiers) {
            if (!type.isValid(tier.defaultBlockState())) {
                refused.append(" ").append(BuiltInRegistries.BLOCK.getKey(tier));
            }
        }
        return refused.toString();
    }

    private static String titleKey(GameTestHelper helper, BlockPos pos) {
        Component title = furnace(helper, pos).getDisplayName();
        return title.getContents() instanceof TranslatableContents contents ? contents.getKey() : "literal " + title.getString();
    }
}
