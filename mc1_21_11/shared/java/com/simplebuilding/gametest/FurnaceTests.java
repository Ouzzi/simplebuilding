package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.platform.PlatformServices;
import com.simplebuilding.platform.ItemAutomation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import java.util.function.Function;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The nine reinforced, netherite and enderite cooking devices: furnace, smoker and blast furnace.
 *
 * <p>The speed-up is one short block of code, copied verbatim into three files:
 * {@code ModFurnaceBlockEntity#tick}, {@code ModSmokerBlockEntity#tick} and
 * {@code ModBlastFurnaceBlockEntity#tick}. After vanilla's {@code serverTick} has run, extra cooking
 * progress is written back into the container data - one tick per server tick for the reinforced
 * tier, three for the netherite one, seven for the enderite one - but only
 * {@code if (isBurning && cookTime > 0 && totalTime > 0)}, and never past {@code totalTime - 1}.
 * The upper tiers' rewards (double experience, more output from raw metals in the netherite and
 * enderite blast furnace, see {@code FurnaceTierPerks}) sit in {@code setRecipeUsed}; this file
 * does not measure them yet. The netherite and enderite devices have no crafting recipe - they are
 * hammered in the world ({@code SledgehammerUpgrades}). Everything else about these blocks is
 * registration data.
 *
 * <p><b>Three copies, not one helper.</b> That is why
 * {@link #progressCoolsDownAtTheVanillaRateOnceTheFuelIsSpent} drives one device of every family
 * rather than three furnaces: the guard exists three times, and deleting {@code isBurning &&} from
 * any single one of them leaves the other two untouched. A suite that only ever loads
 * {@code ModBlocks#REINFORCED_FURNACE} and {@code ModBlocks#NETHERITE_FURNACE} watches one third of
 * the guard and reports the other two thirds as green. {@code BlockBehaviourTests} cannot close that
 * gap either: its {@code loadFurnace} puts eight coal in every device, so {@code isBurning} is true
 * there from the first tick to the last and the guard is never asked.
 *
 * <p>{@code BlockBehaviourTests} already proves the coarse claim, that the six reinforced and
 * netherite devices finish a real recipe measurably earlier than their vanilla counterpart (the
 * enderite tier has no timing test yet). This file looks at the parts of the same code
 * that a faster stopwatch cannot see: the guard, the cap, and what the blocks are made of. One
 * timing comparison is deliberately repeated -
 * {@link #oneCoalFeedsSeveralNetheriteSmeltsWhereVanillaManagesOne} - because it is run on a single
 * piece of fuel rather than a full slot; its javadoc says which half of it is load bearing and which
 * half is only a control.
 *
 * <h2>How the cooking numbers are read</h2>
 *
 * <p>{@code AbstractFurnaceBlockEntity#dataAccess} is {@code protected}, so a test outside the
 * block entity's package cannot read it directly. The numbers are therefore taken out of the block
 * entity's own persisted form ({@code saveWithoutMetadata}, keys {@code cooking_time_spent} and
 * {@code cooking_total_time}) - the same two fields the container data exposes at indices 2 and 3,
 * read through an interface that is public and that the game itself depends on.
 *
 * <h2>Built from the vanilla block, not from glass</h2>
 *
 * <p>Until 2026-09 {@code ModBlocks#registerBlock} handed the six factories of the time
 * {@code BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)}: no tool requirement (a bare hand
 * dropped them), no light while burning, no occlusion, glass's map colour. Each is now copied
 * from its vanilla counterpart and only overrides strength and sound, and
 * {@link #furnaceBlocksCarryTheirRegisteredHardnessResistanceAndTags} pins the three things that
 * copy brings against that counterpart: the tool requirement, light level 13 while lit and none
 * while cold, and full occlusion.
 *
 * <h2>Not covered, and why</h2>
 * <ul>
 *   <li><b>Creative tab membership.</b> {@code ModItemGroupsContent} lists the nine with plain
 *       {@code entries.accept(...)} calls; an assertion on them would restate the source line it
 *       guards rather than pin a behaviour. The same call was declined for the same reason in
 *       {@code BundleWiringTests} and {@code RotatorTests}. That the nine are registered and each
 *       has its {@code BlockItem} is already covered by
 *       {@code DataIntegrityTests#everyModBlockIsRegisteredAndHasItsBlockItem}.</li>
 *   <li><b>The loot tables themselves</b> and <b>the three crafting patterns</b>.
 *       {@code DataIntegrityTests} drives both to the ground already - it rolls every table and
 *       compares the items that fall out, and it matches every pattern through the live
 *       {@code RecipeManager} including the result count. {@link #allNineFurnacesDropThemselvesWhenBroken}
 *       and {@link #furnaceRecipesKeepTheirBookCategoryAndRejectNearMissGrids} are cut back to what
 *       is left over there, and each says so in its own javadoc.</li>
 *   <li><b>The screens themselves</b>, the lit texture, the fire and smoke particles and the
 *       crackling sound. All client side.</li>
 * </ul>
 */
public final class FurnaceTests {

    private FurnaceTests() {
    }

    /** Tick budget for {@link #boostOnlyRunsWhileTheFurnaceBurnsAndCooks}. */
    public static final int BOOST_GUARD_MAX_TICKS = 60;

    /** Tick budget for {@link #progressCoolsDownAtTheVanillaRateOnceTheFuelIsSpent}. */
    public static final int COOL_DOWN_MAX_TICKS = 140;

    /** Tick budget for {@link #boostNeverPushesCookingProgressToTheFullCookTime}. */
    public static final int COOK_CAP_MAX_TICKS = 200;

    /** Tick budget for {@link #allNineFurnacesDropThemselvesWhenBroken}. */
    public static final int DROP_MAX_TICKS = 60;

    /** Tick budget for {@link #oneCoalFeedsSeveralNetheriteSmeltsWhereVanillaManagesOne}. */
    public static final int FUEL_PARITY_MAX_TICKS = 300;

    /** Tick budget for both hopper automation tests: a 200 tick smelt at double speed plus the transfers. */
    public static final int HOPPER_RIG_MAX_TICKS = 240;

    /** Slot layout of {@link AbstractFurnaceBlockEntity}: input / fuel / result. */
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_FUEL = 1;
    private static final int SLOT_RESULT = 2;

    /** Vanilla's {@code AbstractFurnaceBlockEntity.BURN_COOL_SPEED}: progress lost per idle tick. */
    private static final int BURN_COOL_SPEED = 2;

    /** How long {@link #boostOnlyRunsWhileTheFurnaceBurnsAndCooks} watches its three furnaces. */
    private static final int GUARD_OBSERVATION_TICKS = 40;

    /** Ticks between the two samples in {@link #progressCoolsDownAtTheVanillaRateOnceTheFuelIsSpent}. */
    private static final int COOL_DOWN_SAMPLE_GAP = 10;

    /** This mod's namespace, for the recipe lookups by id. */
    private static final String MOD_ID = "simplebuilding";

    /** The nine devices, tier by tier, with the item each of them drops. */
    private record Device(String label, Block block, Item item, Block vanilla) {
    }

    private static final List<Device> ALL_DEVICES = List.of(
            new Device("reinforced furnace", ModBlocks.REINFORCED_FURNACE, ModItems.REINFORCED_FURNACE, Blocks.FURNACE),
            new Device("netherite furnace", ModBlocks.NETHERITE_FURNACE, ModItems.NETHERITE_FURNACE, Blocks.FURNACE),
            new Device("reinforced smoker", ModBlocks.REINFORCED_SMOKER, ModItems.REINFORCED_SMOKER, Blocks.SMOKER),
            new Device("netherite smoker", ModBlocks.NETHERITE_SMOKER, ModItems.NETHERITE_SMOKER, Blocks.SMOKER),
            new Device("reinforced blast furnace", ModBlocks.REINFORCED_BLAST_FURNACE, ModItems.REINFORCED_BLAST_FURNACE, Blocks.BLAST_FURNACE),
            new Device("netherite blast furnace", ModBlocks.NETHERITE_BLAST_FURNACE, ModItems.NETHERITE_BLAST_FURNACE, Blocks.BLAST_FURNACE),
            new Device("enderite furnace", ModBlocks.ENDERITE_FURNACE, ModItems.ENDERITE_FURNACE, Blocks.FURNACE),
            new Device("enderite smoker", ModBlocks.ENDERITE_SMOKER, ModItems.ENDERITE_SMOKER, Blocks.SMOKER),
            new Device("enderite blast furnace", ModBlocks.ENDERITE_BLAST_FURNACE, ModItems.ENDERITE_BLAST_FURNACE, Blocks.BLAST_FURNACE));

    /** Nine spots, at least two blocks apart, that all fit inside the 8x8x8 test room. */
    private static final List<BlockPos> NINE_SPOTS = List.of(
            new BlockPos(1, 1, 1), new BlockPos(3, 1, 1), new BlockPos(5, 1, 1),
            new BlockPos(1, 1, 4), new BlockPos(3, 1, 4), new BlockPos(5, 1, 4),
            new BlockPos(1, 1, 7), new BlockPos(3, 1, 7), new BlockPos(5, 1, 7));

    /**
     * One device family's cool down case: which of the three copied {@code tick} methods it runs
     * through, where it stands, and what it cooks.
     */
    private record CoolDownCase(String label, String tickMethod, BlockPos pos, Block block, Item input) {
    }

    /**
     * One reinforced device of every family, because the guard under test exists three times over.
     * The inputs are chosen so that the cook cannot finish before the fuel does: raw iron smelts in
     * 200 ticks, and both the smoking and the blasting recipe take 100, while a single bamboo - the
     * shortest lived vanilla fuel at 50 ticks - carries a reinforced device only 50 ticks at two
     * points of progress per tick. See {@link #progressCoolsDownAtTheVanillaRateOnceTheFuelIsSpent}.
     */
    private static final List<CoolDownCase> COOL_DOWN_CASES = List.of(
            new CoolDownCase("reinforced furnace", "ModFurnaceBlockEntity#tick",
                    new BlockPos(1, 1, 1), ModBlocks.REINFORCED_FURNACE, Items.RAW_IRON),
            new CoolDownCase("reinforced smoker", "ModSmokerBlockEntity#tick",
                    new BlockPos(3, 1, 1), ModBlocks.REINFORCED_SMOKER, Items.BEEF),
            new CoolDownCase("reinforced blast furnace", "ModBlastFurnaceBlockEntity#tick",
                    new BlockPos(5, 1, 1), ModBlocks.REINFORCED_BLAST_FURNACE, Items.RAW_IRON));

    /** The three vanilla tool tiers a block can be gated behind, cheapest first. */
    private static final List<TagKey<Block>> NEEDS_TOOL_TAGS = List.of(
            BlockTags.NEEDS_STONE_TOOL, BlockTags.NEEDS_IRON_TOOL, BlockTags.NEEDS_DIAMOND_TOOL);

    // =====================================================================================
    // THE THREE GUARDS IN FRONT OF THE BOOST
    // =====================================================================================

    /**
     * The extra progress is written only {@code if (isBurning && cookTime > 0 && totalTime > 0)}.
     * Three reinforced furnaces are ticked side by side for {@value #GUARD_OBSERVATION_TICKS}
     * ticks and the HIGHEST cooking progress each of them ever reaches is recorded:
     *
     * <ul>
     *   <li><b>no fuel</b> - raw iron in the input, fuel slot empty. Vanilla never lights it, so
     *       nothing may ever be cooked. Note that its total cook time is <em>not</em> zero:
     *       {@code AbstractFurnaceBlockEntity#setItem} fills {@code cooking_total_time} the moment
     *       the input slot is written, so the {@code totalTime > 0} guard does not cover this
     *       case - only {@code isBurning} and {@code cookTime > 0} do.</li>
     *   <li><b>nothing to cook</b> - coal in the fuel slot and a diamond, which no smelting recipe
     *       accepts, in the input. Fuel is present, but the cook never starts.</li>
     *   <li><b>working</b> - raw iron and coal, the positive control. Without it a mod that stopped
     *       ticking altogether, or a furnace that simply never ran, would pass the first two
     *       assertions.</li>
     * </ul>
     *
     * <p>The sampling is deliberately a maximum over every tick rather than a single reading at the
     * end: with the guards gone the boost writes progress that vanilla immediately decays again by
     * {@value #BURN_COOL_SPEED} per tick, so a snapshot could easily land on a zero.
     *
     * <p>This one runs on the furnace family only, i.e. on {@code ModFurnaceBlockEntity#tick}. The
     * combination it isolates - fuelled but with nothing to smelt - is the same three lines in all
     * three files; the copy that is genuinely worth driving per family is the {@code isBurning}
     * half, and {@link #progressCoolsDownAtTheVanillaRateOnceTheFuelIsSpent} does that.
     *
     * <p>What breaks this test: dropping {@code isBurning} and {@code cookTime > 0} together from
     * {@code ModFurnaceBlockEntity#tick} - a point of progress is then written into the two idle
     * furnaces on every tick and their maximum stops being zero. Dropping only {@code isBurning} is
     * deliberately NOT caught here, because both idle furnaces sit at a cooking progress of zero and
     * the other half of the guard still holds; that case is what
     * {@link #progressCoolsDownAtTheVanillaRateOnceTheFuelIsSpent} is for. Dropping only
     * {@code cookTime > 0} is caught by neither, and nothing in this file claims otherwise: it would
     * merely let a fresh cook start one boost step above zero.
     */
    public static void boostOnlyRunsWhileTheFurnaceBurnsAndCooks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos noFuel = new BlockPos(1, 1, 1);
        BlockPos nothingToCook = new BlockPos(3, 1, 1);
        BlockPos working = new BlockPos(5, 1, 1);

        helper.setBlock(noFuel, ModBlocks.REINFORCED_FURNACE);
        helper.setBlock(nothingToCook, ModBlocks.REINFORCED_FURNACE);
        helper.setBlock(working, ModBlocks.REINFORCED_FURNACE);

        // The "nothing to cook" case only says something as long as its input really is unsmeltable.
        boolean diamondSmelts = level.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(new ItemStack(Items.DIAMOND)), level)
                .isPresent();
        helper.assertFalse(diamondSmelts,
                "a diamond has gained a smelting recipe, so the 'nothing to cook' furnace below "
                        + "would start cooking and this test would prove nothing");

        furnace(helper, noFuel).setItem(SLOT_INPUT, new ItemStack(Items.RAW_IRON, 8));
        furnace(helper, nothingToCook).setItem(SLOT_INPUT, new ItemStack(Items.DIAMOND, 8));
        furnace(helper, nothingToCook).setItem(SLOT_FUEL, new ItemStack(Items.COAL, 8));
        furnace(helper, working).setItem(SLOT_INPUT, new ItemStack(Items.RAW_IRON, 8));
        furnace(helper, working).setItem(SLOT_FUEL, new ItemStack(Items.COAL, 8));

        // Precondition, so the first bullet above stays true if vanilla ever changes setItem.
        helper.assertTrue(cookingTotalTime(helper, noFuel) > 0,
                "the fuel-less furnace has a total cook time of 0, so the totalTime guard alone "
                        + "would already block the boost and this case would prove nothing");

        int[] highest = new int[3];
        helper.onEachTick(() -> {
            highest[0] = Math.max(highest[0], cookingProgress(helper, noFuel));
            highest[1] = Math.max(highest[1], cookingProgress(helper, nothingToCook));
            highest[2] = Math.max(highest[2], cookingProgress(helper, working));
        });

        helper.startSequence()
                .thenExecuteAfter(GUARD_OBSERVATION_TICKS, () -> {
                    Assertions.valueEqual(helper, highest[0], 0,
                            "highest cooking progress of a reinforced furnace without any fuel");
                    Assertions.valueEqual(helper, highest[1], 0,
                            "highest cooking progress of a fuelled reinforced furnace whose input "
                                    + "has no smelting recipe");
                    helper.assertTrue(highest[2] > 0,
                            "the control furnace never cooked anything at all, so the two zeroes "
                                    + "above say nothing; highest progress was " + highest[2]);

                    // The lit flag is vanilla's, and it is what "isBurning" reads: state it here so
                    // a failure above can be told apart from a furnace that simply refused to light.
                    helper.assertBlockProperty(noFuel, AbstractFurnaceBlock.LIT, Boolean.FALSE);
                    helper.assertBlockProperty(nothingToCook, AbstractFurnaceBlock.LIT, Boolean.FALSE);
                    helper.assertBlockProperty(working, AbstractFurnaceBlock.LIT, Boolean.TRUE);
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    /**
     * The {@code isBurning} guard on its own, which the test above cannot isolate - driven once for
     * each of the three copies of it, because they are three separate files and not one helper.
     *
     * <p>One reinforced device of every family is given a single unit of input and a single bamboo.
     * Bamboo is the shortest lived vanilla fuel (50 ticks: {@code FuelValues} registers
     * {@code Blocks.BAMBOO} at a quarter of the 200 tick smelting unit), so each device lights,
     * cooks for a while at the boosted rate and then goes out well before its recipe is done -
     * leaving it in the one state that reaches the {@code isBurning} guard and nothing else: cooking
     * progress above zero, a total cook time above zero, and no fire.
     *
     * <p>The numbers, measured rather than copied and re-checked by the two preconditions below:
     * the reinforced tier adds one point on top of vanilla's, so the counter climbs by two per tick
     * and stands at 99 or 100 when the bamboo runs out on tick 51. The smoker and the blast furnace
     * cook in 100 ticks and are held one short of it by the mod's own cap, the furnace needs 200 and
     * is nowhere near, so no device finishes and every one of them is left with far more progress
     * than the {@value #COOL_DOWN_SAMPLE_GAP} tick sample below can consume.
     *
     * <p>From there vanilla cools the progress down by exactly {@value #BURN_COOL_SPEED} per tick
     * ({@code cookingTimer = Mth.clamp(cookingTimer - 2, 0, cookingTotalTime)}). The test samples
     * the progress on the first tick a device is dark and again {@value #COOL_DOWN_SAMPLE_GAP} ticks
     * later and requires the difference to be exactly that vanilla rate. The absolute values do not
     * matter, only the drop, so it does not depend on which tick the sample lands on.
     *
     * <p>What breaks this test: dropping {@code isBurning} from the condition in
     * <em>any one</em> of {@code ModFurnaceBlockEntity}, {@code ModSmokerBlockEntity} and
     * {@code ModBlastFurnaceBlockEntity}. That file's device would add its point back on every idle
     * tick, the observed drop would be 1 per tick instead of 2, and the failure message names the
     * tick method to look in. In the game the same edit means a netherite smoker counts three
     * cooking ticks per tick while it stands cold and dark. Should bamboo ever outlast one of these
     * cooks, the device would be sitting on a finished result at zero progress instead; the two
     * preconditions in front of the measurement report that rather than let the test pass on a state
     * it was not written for.
     */
    public static void progressCoolsDownAtTheVanillaRateOnceTheFuelIsSpent(GameTestHelper helper) {
        for (CoolDownCase device : COOL_DOWN_CASES) {
            helper.setBlock(device.pos(), device.block());
            AbstractFurnaceBlockEntity blockEntity = furnace(helper, device.pos());
            blockEntity.setItem(SLOT_INPUT, new ItemStack(device.input(), 1));
            blockEntity.setItem(SLOT_FUEL, new ItemStack(Items.BAMBOO, 1));
        }

        int[] samples = new int[COOL_DOWN_CASES.size()];

        helper.startSequence()
                .thenWaitUntil(() -> {
                    for (CoolDownCase device : COOL_DOWN_CASES) {
                        helper.assertBlockProperty(device.pos(), AbstractFurnaceBlock.LIT, Boolean.TRUE);
                    }
                })
                .thenWaitUntil(() -> {
                    for (CoolDownCase device : COOL_DOWN_CASES) {
                        helper.assertBlockProperty(device.pos(), AbstractFurnaceBlock.LIT, Boolean.FALSE);
                    }
                })
                .thenExecute(() -> {
                    for (int index = 0; index < COOL_DOWN_CASES.size(); index++) {
                        CoolDownCase device = COOL_DOWN_CASES.get(index);
                        samples[index] = cookingProgress(helper, device.pos());
                        helper.assertTrue(furnace(helper, device.pos()).getItem(SLOT_RESULT).isEmpty(),
                                "the single bamboo outlasted the whole cook in the " + device.label()
                                        + ", so it is not in the 'cold but half cooked' state this "
                                        + "test needs");
                        // Enough head room that the sample below cannot hit vanilla's clamp at 0.
                        helper.assertTrue(samples[index] > BURN_COOL_SPEED * COOL_DOWN_SAMPLE_GAP,
                                "the " + device.label() + " went out with only " + samples[index]
                                        + " cooking progress, which is too little to measure a cool "
                                        + "down against");
                    }
                })
                .thenExecuteAfter(COOL_DOWN_SAMPLE_GAP, () -> {
                    for (int index = 0; index < COOL_DOWN_CASES.size(); index++) {
                        CoolDownCase device = COOL_DOWN_CASES.get(index);
                        int now = cookingProgress(helper, device.pos());
                        int lost = samples[index] - now;
                        Assertions.valueEqual(helper, lost, BURN_COOL_SPEED * COOL_DOWN_SAMPLE_GAP,
                                "cooking progress lost by an unlit " + device.label() + " over "
                                        + COOL_DOWN_SAMPLE_GAP + " ticks (" + samples[index] + " -> "
                                        + now + "); anything smaller means " + device.tickMethod()
                                        + " is still adding progress to a device that is not burning");
                    }
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    /**
     * The cap: the boost may raise the progress to at most {@code totalTime - 1}, and the last step
     * onto {@code totalTime} belongs to vanilla.
     *
     * <p>This is not cosmetic. Vanilla finishes a smelt on {@code cookingTimer == cookingTotalTime},
     * an equality, not a {@code >=}. A boost that stepped over the total would never hit it again,
     * and the device would cook for ever. Both boosted tiers are watched here, because the two step
     * sizes land differently: the reinforced tier adds one per tick, the netherite tier three.
     *
     * <p>Like the guard test above this one runs on {@code ModFurnaceBlockEntity#tick} only. The cap
     * is the same three lines in all three files, and the family that is actually worth repeating -
     * the {@code isBurning} guard - is repeated in
     * {@link #progressCoolsDownAtTheVanillaRateOnceTheFuelIsSpent}.
     *
     * <p>The comparison runs on every single tick and fails on the spot, so the report names the
     * tick and the two numbers rather than a summary at the end. The run is only accepted once the
     * netherite furnace has finished two ingots and the reinforced one has finished one, so the
     * region right in front of the cap really has been crossed - three times in total, rather than
     * the test ending somewhere in the middle of a cook.
     *
     * <p>What breaks this test: removing the {@code newCookTime >= totalTime} cap. The per tick
     * comparison fails the moment the progress lands on the total, which is roughly 50 ticks in.
     */
    public static void boostNeverPushesCookingProgressToTheFullCookTime(GameTestHelper helper) {
        BlockPos netherite = new BlockPos(2, 1, 2);
        BlockPos reinforced = new BlockPos(5, 1, 2);

        helper.setBlock(netherite, ModBlocks.NETHERITE_FURNACE);
        helper.setBlock(reinforced, ModBlocks.REINFORCED_FURNACE);

        loadForSmelting(helper, netherite);
        loadForSmelting(helper, reinforced);

        // Whether a cook time above zero was ever observed at all - without one the comparison
        // below never ran and could not have failed.
        boolean[] sawACookTime = new boolean[2];

        helper.onEachTick(() -> {
            assertProgressStaysBelowTotal(helper, netherite, "netherite furnace", sawACookTime, 0);
            assertProgressStaysBelowTotal(helper, reinforced, "reinforced furnace", sawACookTime, 1);
        });

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(resultCount(helper, netherite) >= 2,
                            "the netherite furnace has produced " + resultCount(helper, netherite)
                                    + " ingots, waiting for 2");
                    helper.assertTrue(resultCount(helper, reinforced) >= 1,
                            "the reinforced furnace has produced " + resultCount(helper, reinforced)
                                    + " ingots, waiting for 1");
                })
                .thenExecute(() -> helper.assertTrue(sawACookTime[0] && sawACookTime[1],
                        "no total cook time was ever observed, so the per tick cap comparison never "
                                + "ran and could not have failed"))
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    // =====================================================================================
    // MENU AND TITLE
    // =====================================================================================

    /**
     * Right clicking any of the nine has to open the very menu its vanilla counterpart opens - a
     * furnace screen, a smoker screen or a blast furnace screen - because that menu type is what
     * decides which screen the client puts up and which recipe book tab it shows.
     *
     * <p>The block is used the way a player uses it: {@code GameTestHelper#useBlock} with an empty
     * handed mock server player, which walks the same {@code useItemOn} / {@code useWithoutItem}
     * path the server takes for a real interaction, and then {@code player.containerMenu} is read.
     * Nothing about {@code createMenu} or {@code useWithoutItem} was called by any test before.
     *
     * <p>The container title is checked in the same pass: every device has to carry a translatable
     * title from this mod's namespace, that key has to be the one named after the device itself
     * ({@code container.simplebuilding.netherite_smoker} for a netherite smoker), and all nine keys
     * have to differ from one another - the three tiers of one family included, which is where the
     * netherite devices used to answer with their reinforced sibling's key.
     *
     * <p>The namespace prefix on its own said nothing about the rest of the key:
     * {@code container.simplebuilding.a} carries no translation in either language file, so the
     * screen title would be the raw key, and three such keys are still three different ones. That is
     * why the key is compared against the block's own id rather than only against its prefix.
     *
     * <p>What breaks this test: a device that opens the wrong screen (a smoker built on
     * {@code FurnaceMenu}, say - the recipe book would then offer smelting recipes a smoker cannot
     * run), a {@code useWithoutItem} that stops opening anything, and a {@code getDefaultName} that
     * returns a literal, a vanilla key, a key from this namespace that no language file defines, or
     * the same key for two of the nine devices.
     */
    public static void everyTierOpensTheMenuOfItsVanillaCounterpart(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        List<MenuType<?>> expectedMenus = List.<MenuType<?>>of(
                MenuType.FURNACE, MenuType.FURNACE,
                MenuType.SMOKER, MenuType.SMOKER,
                MenuType.BLAST_FURNACE, MenuType.BLAST_FURNACE,
                MenuType.FURNACE, MenuType.SMOKER, MenuType.BLAST_FURNACE);
        List<String> titleKeys = new ArrayList<>();

        for (int index = 0; index < ALL_DEVICES.size(); index++) {
            Device device = ALL_DEVICES.get(index);
            BlockPos pos = NINE_SPOTS.get(index);
            helper.setBlock(pos, device.block());

            player.containerMenu = player.inventoryMenu;
            helper.useBlock(pos, player);

            AbstractContainerMenu menu = player.containerMenu;
            helper.assertTrue(menu != player.inventoryMenu,
                    "using a " + device.label() + " opened no menu at all");
            MenuType<?> expected = expectedMenus.get(index);
            helper.assertTrue(menu.getType() == expected,
                    "a " + device.label() + " opened " + BuiltInRegistries.MENU.getKey(menu.getType())
                            + " instead of " + BuiltInRegistries.MENU.getKey(expected));

            titleKeys.add(containerTitleKey(helper, device, pos));
        }

        player.containerMenu = player.inventoryMenu;

        // Tiers as well as families: no two of the nine may answer with the same key, so a netherite
        // device falling back on its reinforced sibling's title is caught here too.
        for (int first = 0; first < titleKeys.size(); first++) {
            for (int second = first + 1; second < titleKeys.size(); second++) {
                helper.assertTrue(!titleKeys.get(first).equals(titleKeys.get(second)),
                        "the " + ALL_DEVICES.get(first).label() + " and the "
                                + ALL_DEVICES.get(second).label() + " share the container title "
                                + titleKeys.get(first));
            }
        }

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // REGISTRATION DATA
    // =====================================================================================

    /**
     * The two {@code strength(...)} calls in {@code ModBlocks} and the block tags that go with them.
     *
     * <p>Hardness is measured through a real block state at a real position, and the reinforced
     * tier's 3.5 is stated as "the same as vanilla's furnace" rather than as a copied number, so a
     * change on either side shows up. The netherite tier's blast resistance of 1200 is compared
     * against vanilla's furnace as well - the whole point of that number is that it is out of
     * proportion. The enderite tier is built to 6.0 / 1500, like the enderite piston.
     *
     * <p>The tag half is a real datapack lookup, so it also proves the generated
     * {@code mineable/pickaxe} JSON is inside the jar and loaded; a vanilla furnace is the positive
     * control and levitating sand the negative one, so the lookup is known to be able to answer both
     * ways.
     *
     * <p>The properties the vanilla copy brings are asserted against the vanilla block itself:
     * {@code requiresCorrectToolForDrops()} (so a bare hand no longer drops them, exactly like a
     * vanilla furnace), light level 13 in the {@code LIT} state and 0 without it, and
     * {@code canOcclude()}. None of the nine is in a {@code needs_*_tool} tag - neither is the
     * vanilla furnace - and the rule that ties the two together is checked as well: a block listed
     * in {@code needs_stone_tool}, {@code needs_iron_tool} or {@code needs_diamond_tool} while
     * {@code requiresCorrectToolForDrops()} is false has a tag that gates nothing at all.
     * {@code ModBlocks#CRACKED_DIAMOND_BLOCK} is the mod block that satisfies the rule today and is
     * checked alongside, so the rule is known to be more than an empty implication.
     *
     * <p>What breaks this test: editing any of the three {@code strength(...)} calls, dropping a block from
     * {@code ModBlockTagProvider} or failing to regenerate the data, building a device from any
     * other base than its vanilla counterpart (glass, as it used to be, fails all three property
     * checks), and adding any block covered here to a {@code needs_*_tool} tag without also giving
     * it {@code requiresCorrectToolForDrops()}.
     */
    public static void furnaceBlocksCarryTheirRegisteredHardnessResistanceAndTags(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos probe = helper.absolutePos(new BlockPos(1, 1, 1));

        float vanillaHardness = Blocks.FURNACE.defaultBlockState().getDestroySpeed(level, probe);
        float vanillaResistance = Blocks.FURNACE.getExplosionResistance();

        for (Device device : ALL_DEVICES) {
            BlockState state = device.block().defaultBlockState();
            boolean netherite = device.label().startsWith("netherite");
            boolean enderite = device.label().startsWith("enderite");
            float hardness = state.getDestroySpeed(level, probe);
            float resistance = device.block().getExplosionResistance();

            if (enderite) {
                Assertions.valueEqual(helper, hardness, 6.0F, "the " + device.label() + "'s hardness");
                Assertions.valueEqual(helper, resistance, 1500.0F,
                        "the " + device.label() + "'s blast resistance");
            } else if (netherite) {
                Assertions.valueEqual(helper, hardness, 5.0F, "the " + device.label() + "'s hardness");
                Assertions.valueEqual(helper, resistance, 1200.0F,
                        "the " + device.label() + "'s blast resistance");
                helper.assertTrue(resistance > vanillaResistance * 100.0F,
                        "the " + device.label() + " is supposed to be the blast proof tier, but its "
                                + "resistance of " + resistance + " is no better than a vanilla "
                                + "furnace's " + vanillaResistance);
            } else {
                Assertions.valueEqual(helper, hardness, vanillaHardness,
                        "the " + device.label() + "'s hardness, against a vanilla furnace's");
                Assertions.valueEqual(helper, resistance, vanillaResistance,
                        "the " + device.label() + "'s blast resistance, against a vanilla furnace's");
            }

            assertInTag(helper, device, BlockTags.MINEABLE_WITH_PICKAXE, true);
            assertToolTagMatchesToolRequirement(helper, device.label(), state);

            // What the vanilla copy brings, against the vanilla block itself.
            BlockState vanilla = device.vanilla().defaultBlockState();
            helper.assertTrue(state.requiresCorrectToolForDrops() && vanilla.requiresCorrectToolForDrops(),
                    "the " + device.label() + " drops to a bare hand (requiresCorrectToolForDrops is "
                            + state.requiresCorrectToolForDrops() + "), unlike the vanilla block it is built from");
            Assertions.valueEqual(helper, state.setValue(AbstractFurnaceBlock.LIT, true).getLightEmission(),
                    vanilla.setValue(AbstractFurnaceBlock.LIT, true).getLightEmission(),
                    "the light a burning " + device.label() + " gives off, against its vanilla counterpart's");
            Assertions.valueEqual(helper, state.setValue(AbstractFurnaceBlock.LIT, true).getLightEmission(), 13,
                    "the light a burning " + device.label() + " gives off");
            Assertions.valueEqual(helper, state.setValue(AbstractFurnaceBlock.LIT, false).getLightEmission(), 0,
                    "the light a cold " + device.label() + " gives off");
            helper.assertTrue(state.canOcclude(),
                    "the " + device.label() + " does not occlude its neighbours' faces - it still carries "
                            + "glass's noOcclusion instead of the vanilla block's properties");
        }

        // The tag lookup has to be able to answer in both directions, or the mineable/pickaxe
        // results above would mean nothing.
        helper.assertTrue(Blocks.FURNACE.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE),
                "a vanilla furnace is not in mineable/pickaxe, so the tag lookup is broken");
        helper.assertFalse(ModBlocks.LEVITATING_SAND.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE),
                "levitating sand is pickaxe mineable, so the tag lookup says yes to everything");
        // The same for the three needs_*_tool tags: a tag that failed to load would answer "no" for
        // every block, and the rule checked above would then never be reached.
        assertVanillaTagMember(helper, Blocks.IRON_ORE, BlockTags.NEEDS_STONE_TOOL);
        assertVanillaTagMember(helper, Blocks.DIAMOND_ORE, BlockTags.NEEDS_IRON_TOOL);
        assertVanillaTagMember(helper, Blocks.ANCIENT_DEBRIS, BlockTags.NEEDS_DIAMOND_TOOL);

        // None of the nine is in a needs_*_tool tag today, so the rule is quiet on them. This is the
        // mod block that IS in one: running the same rule over it keeps the check from being an
        // implication that no block in this mod has ever had to satisfy.
        BlockState crackedDiamond = ModBlocks.CRACKED_DIAMOND_BLOCK.defaultBlockState();
        helper.assertTrue(crackedDiamond.is(BlockTags.NEEDS_IRON_TOOL),
                "the cracked diamond block has left needs_iron_tool, so the tool tag rule above has "
                        + "lost the one mod block that demonstrates it");
        assertToolTagMatchesToolRequirement(helper, "cracked diamond block", crackedDiamond);

        TestCleanup.succeed(helper);
    }

    /**
     * Only the three netherite and the three enderite block items are registered
     * {@code fireResistant()}.
     *
     * <p>The component is compared against a netherite ingot's rather than asserted by shape, and
     * then driven: a dropped netherite or enderite device has to survive lava but not drowning, so
     * the claim is about behaviour and not only about a stored tag name. The three reinforced items
     * are required to carry no such component at all, which is what gives the other half something
     * to say.
     *
     * <p>What breaks this test: dropping {@code fireResistant()} from one of the six netherite or
     * enderite items in {@code ModItems}, or adding it to a reinforced one.
     */
    public static void onlyNetheriteAndEnderiteFurnaceItemsSurviveLava(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        DamageResistant netheriteIngot = new ItemStack(Items.NETHERITE_INGOT).get(DataComponents.DAMAGE_RESISTANT);
        helper.assertTrue(netheriteIngot != null,
                "a netherite ingot no longer carries a DAMAGE_RESISTANT component, so this test has "
                        + "lost its yardstick");

        for (Device device : ALL_DEVICES) {
            DamageResistant resistant = new ItemStack(device.item()).get(DataComponents.DAMAGE_RESISTANT);
            if (device.label().startsWith("netherite") || device.label().startsWith("enderite")) {
                helper.assertTrue(resistant != null,
                        "the " + device.label() + " item carries no DAMAGE_RESISTANT component at "
                                + "all, so fireResistant() is gone from its registration and a "
                                + "dropped one burns");
                // On this line DamageResistant#types() is the TagKey itself and not a HolderSet, so
                // the two are compared directly - a TagKey is a record and implements equals. The
                // 26.2 twin has to unwrap the named set first to get at the same key.
                Assertions.valueEqual(helper, resistant.types(), netheriteIngot.types(),
                        "the damage types the " + device.label() + " resists, against a netherite "
                                + "ingot's");
                helper.assertTrue(resistant.isResistantTo(level.damageSources().lava()),
                        "a dropped " + device.label() + " should survive lava");
                helper.assertFalse(resistant.isResistantTo(level.damageSources().drown()),
                        "the " + device.label() + " resists drowning as well, so its resistance is "
                                + "not the fire tag any more and the lava assertion proves nothing");
            } else {
                helper.assertTrue(resistant == null,
                        "the " + device.label() + " item is fire resistant too, which makes the "
                                + "netherite and enderite assertions meaningless - it is those tiers "
                                + "that are supposed to survive lava");
            }
        }

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // DROPS AND RECIPES
    // =====================================================================================

    /**
     * The one thing about the nine loot tables that rolling them cannot show: breaking the block in a
     * live world really does put an item entity on the ground.
     *
     * <p>The tables themselves are not claimed here.
     * {@code DataIntegrityTests#everyModBlockLootTableLoads} covers them harder than its name
     * suggests: it walks every {@code ModBlocks} field by reflection - the nine are in, the only
     * exemption is {@code netherite_piston_head} - rolls each table several times with an empty tool
     * through its {@code rollBlockLoot}, and requires the set of items that comes out to be exactly
     * the block's own {@code BlockItem}. A table that rolls nothing and a table regenerated with the
     * wrong item inside are both already red over there.
     *
     * <p>What is left over is the step from the table to the floor: {@code ServerLevel#destroyBlock}
     * takes the real block state and block entity through {@code Block#dropResources} and has to
     * spawn an item entity for it, where the roll over there builds its own {@code LootParams} with
     * nothing but an origin, a block state and an empty tool. So a block that stopped taking part in
     * that path - a {@code noLootTable()} added by accident, a drop condition that needs a breaking
     * entity or the block entity to be present - shows up here as nothing on the floor while its
     * table still rolls correctly. {@code GameTestHelper#destroyBlock} deliberately drops nothing,
     * hence the level call.
     *
     * <p>What breaks this test: one of the nine losing its loot table entirely, or gaining a
     * condition that a break with no player and no tool does not satisfy.
     */
    public static void allNineFurnacesDropThemselvesWhenBroken(GameTestHelper helper) {
        for (int index = 0; index < ALL_DEVICES.size(); index++) {
            BlockPos pos = NINE_SPOTS.get(index);
            helper.setBlock(pos, ALL_DEVICES.get(index).block());
            helper.assertBlockPresent(ALL_DEVICES.get(index).block(), pos);
        }

        for (BlockPos pos : NINE_SPOTS) {
            boolean destroyed = helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
            helper.assertTrue(destroyed, "could not break the device at " + pos);
        }

        helper.runAfterDelay(3, () -> {
            for (int index = 0; index < ALL_DEVICES.size(); index++) {
                Device device = ALL_DEVICES.get(index);
                BlockPos pos = NINE_SPOTS.get(index);
                helper.assertBlockNotPresent(device.block(), pos);
                // The nine spots are at least two blocks apart, so the search radius stays at one block:
                // anything wider would reach into the neighbouring spot's drop.
                helper.assertItemEntityPresent(device.item(), pos, 1.0D);
            }
            TestCleanup.succeed(helper);
        });
    }

    /**
     * What the three reinforced recipe files say beyond their pattern, and that the netherite tier
     * has none any more.
     *
     * <p>The patterns are not claimed here.
     * {@code DataIntegrityTests#modRecipesOnlyReferenceRegisteredItems} already pins all three
     * through the live {@code RecipeManager}: the pattern, the ingredient key, the recipe id, the
     * result item and the count of three - plus that no crafting recipe at all produces a netherite
     * or enderite machine. Repeating any of that here would be a second copy of the same assertion,
     * not a second claim.
     *
     * <p>Three things are left over:
     * <ul>
     *   <li><b>The recipe book category</b> - {@code redstone} for the three reinforced files. Read
     *       straight off the loaded recipe by id rather than through a grid, so it does not restate
     *       a pattern to get at it.</li>
     *   <li><b>A reinforced grid one cracked diamond short</b>, which must craft nothing.</li>
     *   <li><b>The old netherite 2x2</b> - one nugget top left, three reinforced furnaces - must
     *       craft nothing: since 2026-09 the netherite and enderite devices are hammered in the
     *       world ({@code SledgehammerUpgrades}), and the three {@code netherite_*_bulk} recipes are
     *       gone.</li>
     * </ul>
     *
     * <p>A vanilla grid that has to match carries the two negative controls: without it they would
     * also pass against a recipe manager that answered "nothing" to every lookup. It is deliberately
     * a vanilla recipe, so it restates none of the mod's.
     *
     * <p>What breaks this test: editing {@code "category"} in one of the three generated files, or
     * regenerating them from a provider that passes a different {@code RecipeCategory}; a deleted
     * recipe file; a change that widens the reinforced pattern into accepting a grid it should not;
     * and a crafting recipe for the netherite furnace coming back.
     */
    public static void furnaceRecipesKeepTheirBookCategoryAndRejectNearMissGrids(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        assertRecipeCategory(helper, level, "reinforced_furnace", CraftingBookCategory.REDSTONE);
        assertRecipeCategory(helper, level, "reinforced_smoker", CraftingBookCategory.REDSTONE);
        assertRecipeCategory(helper, level, "reinforced_blast_furnace", CraftingBookCategory.REDSTONE);

        // The grid lookup has to be able to answer "yes", or the two "crafts nothing" controls
        // below would pass against a recipe manager that had stopped matching altogether. A plain
        // vanilla furnace ring, so nothing about the mod's recipes is restated.
        ItemStack cobble = new ItemStack(Items.COBBLESTONE);
        assertCrafts(helper, level, CraftingInput.of(3, 3, List.of(
                        cobble.copy(), cobble.copy(), cobble.copy(),
                        cobble.copy(), ItemStack.EMPTY, cobble.copy(),
                        cobble.copy(), cobble.copy(), cobble.copy())),
                Items.FURNACE, "the vanilla cobblestone furnace grid");

        // One cracked diamond short: nothing may step in and craft the reinforced furnace anyway.
        Item diamond = ModItems.CRACKED_DIAMOND;
        assertCraftsNothing(helper, level, CraftingInput.of(3, 3, List.of(
                        new ItemStack(diamond), new ItemStack(diamond), new ItemStack(diamond),
                        new ItemStack(Items.FURNACE), new ItemStack(Items.FURNACE), new ItemStack(Items.FURNACE),
                        new ItemStack(diamond), new ItemStack(diamond), ItemStack.EMPTY)),
                "a reinforced furnace grid with only five cracked diamonds");

        // The old netherite furnace 2x2: one nugget top left, three reinforced furnaces. The
        // netherite tier is hammered in the world now, so the grid has to craft nothing.
        assertCraftsNothing(helper, level, CraftingInput.of(2, 2, List.of(
                        new ItemStack(ModItems.NETHERITE_NUGGET), new ItemStack(ModItems.REINFORCED_FURNACE),
                        new ItemStack(ModItems.REINFORCED_FURNACE), new ItemStack(ModItems.REINFORCED_FURNACE))),
                "the old netherite furnace grid (one nugget, three reinforced furnaces)");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // FUEL
    // =====================================================================================

    /**
     * The boost is free: it speeds the cook up without touching what the fuel slot pays.
     *
     * <p>A vanilla furnace and a netherite furnace are each given eight raw iron and exactly ONE
     * piece of coal, then ticked side by side. That single piece is the whole design. Coal burns for
     * 1600 ticks, far longer than the run, so any change that made the boost reach for fuel a second
     * time would leave the netherite furnace with an empty slot and no fire, and its ingot count
     * would collapse. {@code BlockBehaviourTests} cannot see that: its {@code loadFurnace} fills the
     * slot with eight coal, so a device that burned through several of them would still finish on
     * time.
     *
     * <p><b>Which half is load bearing.</b> Only the output ratio is. The mod's tick writes
     * container data index 2 and nothing else - it never touches the fuel slot and never touches
     * {@code litTimeRemaining} - so "both fuel slots are empty" and "both blocks are still lit" are
     * vanilla's doing, and no edit to the four lines under test can turn either of them red on its
     * own. They are kept as controls, not as coverage: when the ratio fails they say at a glance
     * whether the run went wrong because a furnace never lit, went out early or reached for a second
     * piece of coal.
     *
     * <p><b>Against {@code BlockBehaviourTests}.</b> That file measures the same speed-up as a first
     * ingot timing and requires the netherite furnace to be three times as fast
     * ({@code finished[0] >= finished[2] * 3}). The ratio here is drawn tighter, at four, and taken
     * as an output count over a fixed window rather than as a timing: the netherite tier adds three
     * points on top of vanilla's one, is held one short of the total by its own cap and so finishes
     * an ingot every 51 ticks against vanilla's 200. In the 230 tick window that is four ingots
     * against one - the boost dropping from three extra ticks to two would already fail it.
     *
     * <p><b>Why the count is pinned and not only bounded from below.</b> "At least four times as
     * much" is one sided: a step grown from three extra points to six finishes an ingot every 30
     * ticks, hands in seven of them and sails through, so the documented four times the vanilla
     * speed would quietly have become seven times with the whole suite still green. The window is
     * therefore read as an exact count. Four points of progress per tick put the 200 tick smelt on
     * the cap at 199 on tick 50 and let vanilla finish it on tick 51, so the fourth ingot lands on
     * tick 204 and the fifth is not due before tick 255 - the sample at 230 sits some 25 ticks clear
     * of either edge, and every neighbouring step size lands on a different count outright (two
     * extra points give three ingots, four give five, six give seven).
     *
     * <p>What breaks this test: the netherite step size moving in either direction, the boost
     * stopping altogether, and any change that makes the boost cost fuel - a second
     * {@code consumeFuel}, a shortened burn duration for the mod tiers - because with one piece of
     * coal in the slot there is nothing to fall back on.
     */
    public static void oneCoalFeedsSeveralNetheriteSmeltsWhereVanillaManagesOne(GameTestHelper helper) {
        BlockPos vanilla = new BlockPos(1, 1, 1);
        BlockPos netherite = new BlockPos(5, 1, 1);

        helper.setBlock(vanilla, Blocks.FURNACE);
        helper.setBlock(netherite, ModBlocks.NETHERITE_FURNACE);

        for (BlockPos pos : List.of(vanilla, netherite)) {
            AbstractFurnaceBlockEntity blockEntity = furnace(helper, pos);
            blockEntity.setItem(SLOT_INPUT, new ItemStack(Items.RAW_IRON, 8));
            blockEntity.setItem(SLOT_FUEL, new ItemStack(Items.COAL, 1));
        }

        // 230 ticks: vanilla needs 200 per ingot, so it is one ingot in and 170 ticks away from the
        // next, which keeps the ratio below from depending on the exact tick the sample lands on.
        helper.startSequence()
                .thenExecuteAfter(230, () -> {
                    int vanillaIngots = resultCount(helper, vanilla);
                    int netheriteIngots = resultCount(helper, netherite);

                    helper.assertTrue(vanillaIngots >= 1,
                            "the vanilla furnace produced nothing in 230 ticks, so there is nothing "
                                    + "to compare the netherite furnace against");
                    helper.assertTrue(netheriteIngots >= 4 * vanillaIngots,
                            "on the same single piece of coal the netherite furnace produced "
                                    + netheriteIngots + " ingots against the vanilla furnace's "
                                    + vanillaIngots + "; it is meant to be at least four times as "
                                    + "much");
                    // The ratio above only bounds the boost from below - a bigger step just makes
                    // more ingots and passes. The window is sized so that exactly one step size
                    // fits, so pin the count itself; see the javadoc for the tick arithmetic.
                    Assertions.valueEqual(helper, netheriteIngots, 4,
                            "ingots the netherite furnace finished in 230 ticks on a single piece "
                                    + "of coal; more or fewer means its boost is no longer exactly "
                                    + "three points of cooking progress per tick on top of "
                                    + "vanilla's one");

                    for (BlockPos pos : List.of(vanilla, netherite)) {
                        // Controls, not coverage - see the javadoc. They report why a ratio failure
                        // happened rather than catching a regression of their own.
                        helper.assertTrue(furnace(helper, pos).getItem(SLOT_FUEL).isEmpty(),
                                "the furnace at " + pos + " still holds fuel, so it never lit the "
                                        + "single piece of coal");
                        helper.assertBlockProperty(pos, AbstractFurnaceBlock.LIT, Boolean.TRUE);
                    }
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    private static AbstractFurnaceBlockEntity furnace(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, AbstractFurnaceBlockEntity.class);
    }

    private static void loadForSmelting(GameTestHelper helper, BlockPos pos) {
        AbstractFurnaceBlockEntity blockEntity = furnace(helper, pos);
        blockEntity.setItem(SLOT_INPUT, new ItemStack(Items.RAW_IRON, 8));
        blockEntity.setItem(SLOT_FUEL, new ItemStack(Items.COAL, 8));
    }

    private static int resultCount(GameTestHelper helper, BlockPos pos) {
        return furnace(helper, pos).getItem(SLOT_RESULT).getCount();
    }

    /**
     * Cooking progress of the furnace at {@code pos}, i.e. the value the mod writes to container
     * data index 2. That data is {@code protected} in {@code AbstractFurnaceBlockEntity}, so the
     * number is taken out of the block entity's persisted form instead - the same field, through an
     * interface the game itself depends on. A missing key answers {@code -1} rather than a
     * plausible looking zero.
     */
    private static int cookingProgress(GameTestHelper helper, BlockPos pos) {
        return persistedInt(helper, pos, "cooking_time_spent");
    }

    /** Total cook time of the furnace at {@code pos}; see {@link #cookingProgress}. */
    private static int cookingTotalTime(GameTestHelper helper, BlockPos pos) {
        return persistedInt(helper, pos, "cooking_total_time");
    }

    /**
     * A renamed or dropped key would otherwise answer with a default that reads like a perfectly
     * plausible zero and would make every assertion above pass for the wrong reason, so a missing
     * key fails here instead.
     */
    private static int persistedInt(GameTestHelper helper, BlockPos pos, String key) {
        CompoundTag tag = furnace(helper, pos).saveWithoutMetadata(helper.getLevel().registryAccess());
        int value = tag.getIntOr(key, Integer.MIN_VALUE);
        helper.assertTrue(value != Integer.MIN_VALUE,
                "the furnace at " + pos + " no longer persists '" + key + "', so this test cannot "
                        + "read its cooking numbers any more");
        return value;
    }

    /**
     * Fails on the tick a furnace's cooking progress reaches its own total cook time. A furnace
     * without a cook time yet is skipped, and {@code sawACookTime} records that the comparison ran
     * at all.
     */
    private static void assertProgressStaysBelowTotal(GameTestHelper helper, BlockPos pos, String what,
                                                      boolean[] sawACookTime, int index) {
        int total = cookingTotalTime(helper, pos);
        if (total <= 0) {
            return;
        }
        sawACookTime[index] = true;
        int progress = cookingProgress(helper, pos);
        helper.assertTrue(progress < total,
                "the " + what + " reached a cooking progress of " + progress + " with a total cook "
                        + "time of " + total + "; the boost has to stop one tick short of the total, "
                        + "because vanilla finishes on an equality and would never see it again");
    }

    /**
     * The device's container title key, checked against the one spelling that is shipped for this
     * very device before it is handed back.
     *
     * <p>The namespace prefix alone is satisfied by any invented key, {@code .a} included, and an
     * invented key is not a name: neither language file defines it, so the screen title becomes the
     * raw key. The key therefore has to be {@code container.simplebuilding.} plus this block's own
     * id. A netherite device answering with its reinforced sibling's key - which every one of them
     * did until {@code getDefaultName()} learned to read the block state - fails right here.
     */
    private static String containerTitleKey(GameTestHelper helper, Device device, BlockPos pos) {
        Component title = furnace(helper, pos).getDisplayName();
        helper.assertTrue(title.getContents() instanceof TranslatableContents,
                "the " + device.label() + "'s container title is not translatable at all: " + title);
        String key = ((TranslatableContents) title.getContents()).getKey();
        helper.assertTrue(key.startsWith("container.simplebuilding."),
                "the " + device.label() + "'s container title uses the foreign key " + key);

        String ownKey = "container.simplebuilding."
                + BuiltInRegistries.BLOCK.getKey(device.block()).getPath();
        helper.assertTrue(key.equals(ownKey),
                "the " + device.label() + "'s container title is " + key + " instead of " + ownKey
                        + " - every device has to be named after itself; another device's key names "
                        + "the wrong block on the screen, and a key no language file defines is "
                        + "printed to the player verbatim as the screen title");
        return key;
    }

    private static void assertInTag(GameTestHelper helper, Device device, TagKey<Block> tag, boolean expected) {
        Assertions.valueEqual(helper, device.block().defaultBlockState().is(tag), expected,
                "the " + device.label() + " in " + tag.location());
    }

    /**
     * The half of the tool story that is defect neutral: whichever way
     * {@code requiresCorrectToolForDrops()} is set, a block listed in one of the
     * {@code needs_*_tool} tags has to have it, because those tags are read only after that flag
     * has already decided that a tool matters at all. A block in {@code needs_iron_tool} without it
     * still drops to a bare hand, and the tag then advertises a mining tier that does not exist.
     *
     * <p>Says nothing when the block is in none of the three tags - which is where the nine devices
     * stand, like their vanilla counterparts.
     */
    private static void assertToolTagMatchesToolRequirement(GameTestHelper helper, String label, BlockState state) {
        for (TagKey<Block> tag : NEEDS_TOOL_TAGS) {
            if (!state.is(tag)) {
                continue;
            }
            helper.assertTrue(state.requiresCorrectToolForDrops(),
                    "the " + label + " is listed in " + tag.location() + " but does not require the "
                            + "correct tool for drops, so that tag gates nothing: the block still "
                            + "drops when it is punched by hand");
        }
    }

    /** Proves a tag lookup can still answer "yes", so an absence measured against it means something. */
    private static void assertVanillaTagMember(GameTestHelper helper, Block block, TagKey<Block> tag) {
        helper.assertTrue(block.defaultBlockState().is(tag),
                BuiltInRegistries.BLOCK.getKey(block) + " is no longer in " + tag.location()
                        + ", so that tag did not load and the absences measured against it are "
                        + "worthless");
    }

    /**
     * Reads one of this mod's recipes out of the live {@code RecipeManager} by id and pins its
     * recipe book category. Deliberately not a grid lookup: the pattern is
     * {@code DataIntegrityTests}' business, and going in through the id keeps this assertion from
     * quietly becoming a second copy of it.
     */
    private static void assertRecipeCategory(GameTestHelper helper, ServerLevel level, String path,
                                             CraftingBookCategory category) {
        Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, path);
        RecipeHolder<?> found = null;
        for (RecipeHolder<?> holder : level.getServer().getRecipeManager().getRecipes()) {
            if (id.equals(holder.id().identifier())) {
                found = holder;
                break;
            }
        }

        helper.assertTrue(found != null, id + " is not loaded at all");
        helper.assertTrue(found.value() instanceof CraftingRecipe,
                id + " is no longer a crafting recipe, so it has no recipe book category: "
                        + found.value());
        Assertions.valueEqual(helper, ((CraftingRecipe) found.value()).category(), category,
                id + ": recipe book category");
    }

    /** The positive control for {@link #assertCraftsNothing}: this grid has to match something. */
    private static void assertCrafts(GameTestHelper helper, ServerLevel level, CraftingInput grid,
                                     Item expected, String what) {
        RecipeHolder<CraftingRecipe> match = level.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, grid, level).orElse(null);
        helper.assertTrue(match != null,
                what + " crafts nothing at all, so the recipe manager has stopped matching grids "
                        + "and every 'crafts nothing' check below is worthless");
        // assemble() still takes the registry lookup on this line; 26.2 dropped that parameter.
        ItemStack result = match.value().assemble(grid, level.registryAccess());
        helper.assertTrue(result.is(expected),
                what + " crafts " + result + ", so the grid lookup used as a control is answering "
                        + "with something other than the recipe it was built for");
    }

    private static void assertCraftsNothing(GameTestHelper helper, ServerLevel level, CraftingInput grid,
                                            String what) {
        level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level)
                .ifPresent(match -> helper.fail(what + " crafts " + match.id().identifier()));
    }

    /**
     * Hopper automation with vanilla semantics on all nine devices, fed by vanilla hoppers: a hopper
     * above pushes the input into the input slot, a hopper at the side pushes coal into the fuel slot,
     * and a hopper below pulls the result out of the result slot into a chest. The devices are
     * {@code WorldlyContainer}s through {@link AbstractFurnaceBlockEntity}, so this is
     * {@code getSlotsForFace} / {@code canPlaceItemThroughFace} / {@code canTakeItemThroughFace}
     * as the loader's hopper code sees it. Each device reports what reached its chest and what the
     * two feeding hoppers still hold, all nine in one line.
     *
     * <p>What breaks this test: a device whose block entity stops being a {@code Container} to the
     * hopper, a face mapping that sends coal into the input slot or input into the fuel slot, or a
     * result slot the hopper below may not take from.
     */
    public static void everyTierIsFedAndEmptiedByVanillaHoppers(GameTestHelper helper) {
        runHopperRigs(helper, device -> Blocks.HOPPER);
    }

    /**
     * The same rig as {@link #everyTierIsFedAndEmptiedByVanillaHoppers}, but every device is served by
     * the hopper of its own tier - reinforced, netherite or enderite. These hoppers are
     * {@code ModHopperBlockEntity} with their own push code, so a furnace that a vanilla hopper can
     * feed is no proof that the mod hoppers can.
     *
     * <p>What breaks this test: {@code ModHopperBlockEntity#insert} ignoring the target's face
     * mapping, or its pull ({@code HopperBlockEntity#suckInItems}) no longer taking the result.
     */
    public static void everyTierIsFedAndEmptiedByItsOwnTierOfHopper(GameTestHelper helper) {
        runHopperRigs(helper, device -> device.label().startsWith("enderite") ? ModBlocks.ENDERITE_HOPPER
                : device.label().startsWith("netherite") ? ModBlocks.NETHERITE_HOPPER : ModBlocks.REINFORCED_HOPPER);
    }

    /** Machine at y 3, input hopper above, fuel hopper east of it, result hopper below over a chest. */
    private static void runHopperRigs(GameTestHelper helper, Function<Device, Block> hopperFor) {
        List<BlockPos> machines = new ArrayList<>();
        StringBuilder expected = new StringBuilder();
        for (int index = 0; index < ALL_DEVICES.size(); index++) {
            Device device = ALL_DEVICES.get(index);
            BlockPos spot = NINE_SPOTS.get(index);
            BlockPos machine = new BlockPos(spot.getX(), 3, Math.min(spot.getZ(), 6));
            machines.add(machine);
            Block hopper = hopperFor.apply(device);
            helper.setBlock(machine.below(2), Blocks.CHEST);
            helper.setBlock(machine.below(), hopper.defaultBlockState()
                    .setValue(HopperBlock.FACING, Direction.DOWN).setValue(HopperBlock.ENABLED, Boolean.TRUE));
            helper.setBlock(machine, device.block());
            helper.setBlock(machine.above(), hopper.defaultBlockState()
                    .setValue(HopperBlock.FACING, Direction.DOWN).setValue(HopperBlock.ENABLED, Boolean.TRUE));
            helper.setBlock(machine.east(), hopper.defaultBlockState()
                    .setValue(HopperBlock.FACING, Direction.WEST).setValue(HopperBlock.ENABLED, Boolean.TRUE));
            ((Container) helper.getBlockEntity(machine.above(), net.minecraft.world.level.block.entity.BlockEntity.class))
                    .setItem(0, new ItemStack(rigInput(device)));
            ((Container) helper.getBlockEntity(machine.east(), net.minecraft.world.level.block.entity.BlockEntity.class))
                    .setItem(0, new ItemStack(Items.COAL));
            expected.append(index > 0 ? "; " : "").append(device.label()).append(": 1 ")
                    .append(BuiltInRegistries.ITEM.getKey(rigResult(device)).getPath())
                    .append(" in the chest, input hopper 0, fuel hopper 0");
        }
        helper.startSequence()
                .thenWaitUntil(() -> Assertions.valueEqual(helper, rigReport(helper, machines), expected.toString(),
                        "what hoppers moved into and out of the nine devices (" + hopperFor.apply(ALL_DEVICES.get(0)) + ")"))
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
    }

    /**
     * Pipes and other mods reach a block through the loader's item transfer API, not through the
     * {@code Container} a vanilla hopper talks to - NeoForge's {@code Capabilities.Item.BLOCK},
     * Fabric's {@code ItemStorage.SIDED}. NeoForge attaches that capability to the vanilla block entity
     * types only, so the mod's four block entity types need it registered explicitly. Every one of the
     * twelve machines is asked through that API, face by face, with vanilla's furnace semantics: the
     * input goes in from above, coal from the side, the input is refused at the side, the result comes
     * out at the bottom and the fuel does not. The hoppers take an item from above and give it back at
     * the bottom. Each machine reports the counts and its slots afterwards, all twelve in one line.
     *
     * <p>What breaks this test: a machine block entity type missing from the capability registration
     * on NeoForge ({@code NeoForgeItemAutomation}), a registration that ignores the face (an unsided
     * wrapper lets the input in from the side), or a loader that installs no {@code ItemAutomation}.
     */
    public static void everyMachineOffersItsSlotsToPipesThroughTheLoaderApi(GameTestHelper helper) {
        ItemAutomation pipes = PlatformServices.itemAutomation();
        ServerLevel level = helper.getLevel();
        StringBuilder actual = new StringBuilder();
        StringBuilder expected = new StringBuilder();
        for (int index = 0; index < ALL_DEVICES.size(); index++) {
            Device device = ALL_DEVICES.get(index);
            BlockPos pos = NINE_SPOTS.get(index);
            helper.setBlock(pos, device.block());
            BlockPos abs = helper.absolutePos(pos);
            Item input = rigInput(device);
            Item result = rigResult(device);
            int inputUp = pipes.insert(level, abs, Direction.UP, new ItemStack(input));
            int fuelSide = pipes.insert(level, abs, Direction.NORTH, new ItemStack(Items.COAL));
            int inputSide = pipes.insert(level, abs, Direction.EAST, new ItemStack(input));
            AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(pos, AbstractFurnaceBlockEntity.class);
            furnace.setItem(SLOT_RESULT, new ItemStack(result));
            int resultDown = pipes.extract(level, abs, Direction.DOWN, result, 1);
            int fuelDown = pipes.extract(level, abs, Direction.DOWN, Items.COAL, 1);
            actual.append(index > 0 ? "; " : "").append(device.label())
                    .append(": input up ").append(inputUp).append(", coal side ").append(fuelSide)
                    .append(", input side ").append(inputSide).append(", result down ").append(resultDown)
                    .append(", coal down ").append(fuelDown).append(", slots ")
                    .append(slotNames(furnace));
            expected.append(index > 0 ? "; " : "").append(device.label())
                    .append(": input up 1, coal side 1, input side 0, result down 1, coal down 0, slots ")
                    .append(BuiltInRegistries.ITEM.getKey(input).getPath()).append(" / coal / -");
        }
        List<Block> hoppers = List.of(ModBlocks.REINFORCED_HOPPER, ModBlocks.NETHERITE_HOPPER, ModBlocks.ENDERITE_HOPPER);
        for (int index = 0; index < hoppers.size(); index++) {
            BlockPos pos = NINE_SPOTS.get(index).above(3);
            helper.setBlock(pos, hoppers.get(index));
            BlockPos abs = helper.absolutePos(pos);
            String label = BuiltInRegistries.BLOCK.getKey(hoppers.get(index)).getPath();
            int in = pipes.insert(level, abs, Direction.UP, new ItemStack(Items.COBBLESTONE));
            int out = pipes.extract(level, abs, Direction.DOWN, Items.COBBLESTONE, 1);
            actual.append("; ").append(label).append(": in ").append(in).append(", out ").append(out);
            expected.append("; ").append(label).append(": in 1, out 1");
        }
        Assertions.valueEqual(helper, actual.toString(), expected.toString(),
                "what the loader's item transfer API moves through each face of the twelve machines");
        TestCleanup.succeed(helper);
    }

    private static String slotNames(AbstractFurnaceBlockEntity furnace) {
        StringBuilder names = new StringBuilder();
        for (int slot = 0; slot < 3; slot++) {
            ItemStack stack = furnace.getItem(slot);
            names.append(slot > 0 ? " / " : "")
                    .append(stack.isEmpty() ? "-" : BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
        }
        return names.toString();
    }

    private static Item rigInput(Device device) {
        return device.vanilla() == Blocks.SMOKER ? Items.BEEF : Items.RAW_IRON;
    }

    private static Item rigResult(Device device) {
        return device.vanilla() == Blocks.SMOKER ? Items.COOKED_BEEF : Items.IRON_INGOT;
    }

    private static String rigReport(GameTestHelper helper, List<BlockPos> machines) {
        StringBuilder report = new StringBuilder();
        for (int index = 0; index < machines.size(); index++) {
            Device device = ALL_DEVICES.get(index);
            BlockPos machine = machines.get(index);
            ChestBlockEntity chest = helper.getBlockEntity(machine.below(2), ChestBlockEntity.class);
            int results = 0;
            StringBuilder strays = new StringBuilder();
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                ItemStack stack = chest.getItem(slot);
                if (stack.is(rigResult(device))) {
                    results += stack.getCount();
                } else if (!stack.isEmpty()) {
                    strays.append(" and ").append(stack);
                }
            }
            report.append(index > 0 ? "; " : "").append(device.label()).append(": ").append(results).append(' ')
                    .append(BuiltInRegistries.ITEM.getKey(rigResult(device)).getPath()).append(strays)
                    .append(" in the chest, input hopper ").append(countIn(helper, machine.above()))
                    .append(", fuel hopper ").append(countIn(helper, machine.east()));
        }
        return report.toString();
    }

    private static int countIn(GameTestHelper helper, BlockPos pos) {
        Container container = (Container) helper.getBlockEntity(pos, net.minecraft.world.level.block.entity.BlockEntity.class);
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            count += container.getItem(slot).getCount();
        }
        return count;
    }

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(3.5, 1.0, 2.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        // Hand the player back no matter how the test ends. A leaked mock player keeps the player
        // list non-empty and the gametest server then stalls on shutdown.
        TestCleanup.before(helper, () -> {
            player.containerMenu = player.inventoryMenu;
            helper.getLevel().getServer().getPlayerList().remove(player);
        });
        return player;
    }
}
