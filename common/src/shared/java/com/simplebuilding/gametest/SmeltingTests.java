package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.items.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Smelting with the mod: the hour long raw enderite blast, the two engine fixes that make such a
 * cook survive (timers stored as ints, menu data scaled into the short range the client receives)
 * and the rewards of the upper furnace tiers ({@code FurnaceTierPerks}).
 *
 * <p><b>Long cooks cannot be waited for.</b> 72000 ticks is an hour; no gametest runs that long. The
 * timers are therefore injected the way a chunk load hands them over: the block entity's own saved
 * form is taken ({@code saveWithoutMetadata}, so the items come along), the four timers are
 * overwritten with ints, and the tag is loaded back through {@code loadCustomOnly}.
 *
 * <p><b>Loader differences.</b> NeoForge (both lines) already stores the four timers as ints and
 * sends menu data as ints; vanilla, and with it Fabric, writes them as shorts, and the menu data
 * packet carries shorts on every loader's vanilla path. So the persistence assertions are mostly the
 * Fabric path's - on NeoForge they additionally prove that the mod's own save does not overwrite
 * NeoForge's values with the fuel figures NeoForge scales down for its menu - while the menu
 * assertions read what the vanilla packet would carry and hold only with
 * {@code AbstractFurnaceMenuMixin}, on every loader.
 *
 * <p><b>One claim, one assertion.</b> Where several values make up one claim they are compared as
 * one line, so the failure message shows all of them.
 *
 * <p><b>Not covered:</b> the drawn arrow and flame themselves (client side), and NeoForge's own int
 * data payload.
 */
public final class SmeltingTests {

    private SmeltingTests() {
    }

    /** Tick budget for {@link #longCookTimersSurviveTheSaveAndLoadAsInts}. */
    public static final int ROUND_TRIP_MAX_TICKS = 30;

    /** Tick budget for {@link #upperTierFurnacesPayDoubleExperience}. */
    public static final int EXPERIENCE_MAX_TICKS = 200;

    /** Tick budget for {@link #blastFurnaceBonusPaysRawMetalsEveryFourthOrSecondSmelt}. */
    public static final int BONUS_MAX_TICKS = 340;

    /** The raw enderite blast: an hour at 20 ticks per second, in the base device. */
    private static final int SCRAP_COOK_TICKS = 72000;

    /** Four timers above what a short holds, and apart from each other so none can stand in for another. */
    private static final int LIT_REMAINING = 40000;
    private static final int LIT_TOTAL = 50000;
    private static final int COOK_SPENT = 40000;

    private static final String MOD_ID = "simplebuilding";
    private static final String BONUS_PROGRESS_KEY = "simplebuilding:bonus_progress";

    // =====================================================================================
    // THE HOUR LONG BLAST
    // =====================================================================================

    /**
     * {@code simplebuilding:enderite_scrap_from_blasting_raw_enderite} is a blasting recipe of
     * 72000 ticks - an hour in a vanilla blast furnace - worth 10 experience, and raw enderite has no
     * smelting or smoking recipe, so it only goes into blast furnaces. All four of them - vanilla,
     * reinforced, netherite and enderite - take the 72000 over as their total cook time the moment the
     * raw enderite goes in, and still report it when their block entity is saved.
     *
     * <p>What breaks this test: another {@code cookingtime} or {@code experience} in the generated
     * recipe, a smelting or smoking recipe for raw enderite, and - on Fabric - a total that comes back
     * from the save as 6464, i.e. {@code AbstractFurnaceBlockEntityMixin} not writing it as an int.
     */
    public static void rawEnderiteBlastsForAnHourAndPaysTenExperience(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE,
                Identifier.fromNamespaceAndPath(MOD_ID, "enderite_scrap_from_blasting_raw_enderite"));
        SingleRecipeInput rawEnderite = new SingleRecipeInput(new ItemStack(ModItems.RAW_ENDERITE));
        RecipeHolder<?> blasting = level.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.BLASTING, rawEnderite, level).orElse(null);
        helper.assertTrue(blasting != null && blasting.id().equals(key),
                "a blast furnace does not pick " + key.identifier() + " for raw enderite but " + blasting);
        AbstractCookingRecipe recipe = (AbstractCookingRecipe) blasting.value();
        helper.assertValueEqual(recipe.cookingTime() + " ticks, " + recipe.experience() + " experience",
                SCRAP_COOK_TICKS + " ticks, 10.0 experience", "the raw enderite blast");

        helper.assertValueEqual(
                "smelting: " + level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING, rawEnderite, level)
                        .map(found -> found.id().identifier().toString()).orElse("none")
                        + ", smoking: " + level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMOKING, rawEnderite, level)
                        .map(found -> found.id().identifier().toString()).orElse("none"),
                "smelting: none, smoking: none", "the recipes an ordinary furnace or a smoker would use for raw enderite");

        List<Block> blastFurnaces = List.of(Blocks.BLAST_FURNACE, ModBlocks.REINFORCED_BLAST_FURNACE,
                ModBlocks.NETHERITE_BLAST_FURNACE, ModBlocks.ENDERITE_BLAST_FURNACE);
        StringBuilder totals = new StringBuilder();
        for (int index = 0; index < blastFurnaces.size(); index++) {
            BlockPos pos = new BlockPos(1 + 2 * index, 1, 2);
            helper.setBlock(pos, blastFurnaces.get(index));
            furnace(helper, pos).setItem(0, new ItemStack(ModItems.RAW_ENDERITE));
            totals.append(index > 0 ? " " : "").append(persisted(helper, furnace(helper, pos), "cooking_total_time"));
        }
        helper.assertValueEqual(totals.toString(), "72000 72000 72000 72000",
                "total cook time the vanilla, reinforced, netherite and enderite blast furnace save for raw enderite");

        helper.succeed();
    }

    /**
     * A vanilla and a netherite blast furnace are handed four timers that do not fit a short - 40000
     * of 50000 ticks of fuel left, 40000 of 72000 ticks cooked - the way a chunk load would hand them
     * over. Saved again, all four come back unchanged. Then both cook on from where they were, on the
     * injected fuel alone: five ticks later the fuel is at 39995 in both, the vanilla device's
     * progress at 40005 and the netherite one's at 40020, the totals still at 50000 and 72000.
     *
     * <p>On Fabric without the int mixin, vanilla reads the four values back as shorts: the progress
     * as -25536, the total as 6464 and the fuel as none at all - the cook stalls, or in a mod furnace
     * finishes an hour early. On NeoForge the mod's save used to write the fuel through
     * {@code dataAccess}, which NeoForge scales down for its menu: 26213 of 32767 instead of 40000 of
     * 50000.
     *
     * <p>What breaks this test: {@code AbstractFurnaceBlockEntityMixin} dropped on Fabric, its save or
     * load half missing or writing any of the four under another key, a save that reads the fuel
     * through {@code dataAccess} on NeoForge, and a netherite blast furnace boost other than three.
     */
    public static void longCookTimersSurviveTheSaveAndLoadAsInts(GameTestHelper helper) {
        BlockPos vanilla = new BlockPos(2, 1, 2);
        BlockPos netherite = new BlockPos(5, 1, 2);
        helper.setBlock(vanilla, Blocks.BLAST_FURNACE);
        helper.setBlock(netherite, ModBlocks.NETHERITE_BLAST_FURNACE);

        for (BlockPos pos : List.of(vanilla, netherite)) {
            AbstractFurnaceBlockEntity entity = furnace(helper, pos);
            entity.setItem(0, new ItemStack(ModItems.RAW_ENDERITE, 2));
            inject(helper, entity, LIT_REMAINING, LIT_TOTAL, COOK_SPENT, SCRAP_COOK_TICKS, -1);
            helper.assertValueEqual(timers(helper, entity), timerLine(LIT_REMAINING, LIT_TOTAL, COOK_SPENT, SCRAP_COOK_TICKS),
                    "the four timers of the device at " + pos + ", loaded and saved again");
        }

        helper.startSequence()
                .thenExecuteAfter(5, () -> {
                    helper.assertValueEqual(timers(helper, furnace(helper, vanilla)),
                            timerLine(LIT_REMAINING - 5, LIT_TOTAL, COOK_SPENT + 5, SCRAP_COOK_TICKS),
                            "the four timers of the vanilla blast furnace 5 ticks after the load");
                    helper.assertValueEqual(timers(helper, furnace(helper, netherite)),
                            timerLine(LIT_REMAINING - 5, LIT_TOTAL, COOK_SPENT + 5 * 4, SCRAP_COOK_TICKS),
                            "the four timers of the netherite blast furnace 5 ticks after the load");
                })
                .thenSucceed();
    }

    /**
     * The menu of a netherite blast furnace in the middle of the hour long blast (40000 of 72000,
     * fuel 40000 of 50000) is opened on the server and every data value it hands to its listeners -
     * the values {@code ClientboundContainerSetDataPacket} writes as shorts - is recorded. All four
     * have to fit a short. They are then fed, through a short, into a menu built the way the client
     * builds one, and that menu's arrow has to show 40000 / 72000 and its flame 40000 / 50000, each
     * to within one step of the scaled total.
     *
     * <p>A furnace on an ordinary 200 tick smelt is the control: short cooks are handed over
     * unchanged, value for value.
     *
     * <p>Without the wrapper the total goes out as 72000 and arrives as 6464, the progress as -25536:
     * the arrow sits at empty for the rest of the hour.
     *
     * <p>What breaks this test: {@code AbstractFurnaceMenuMixin} dropped, {@code LongCookContainerData}
     * dividing a value and its total by different numbers or not dividing above 32767, and dividing
     * values that fit.
     */
    public static void furnaceMenuScalesLongCooksIntoTheShortRange(GameTestHelper helper) {
        BlockPos longCook = new BlockPos(2, 1, 2);
        BlockPos shortCook = new BlockPos(5, 1, 2);
        helper.setBlock(longCook, ModBlocks.NETHERITE_BLAST_FURNACE);
        helper.setBlock(shortCook, ModBlocks.NETHERITE_FURNACE);
        AbstractFurnaceBlockEntity longEntity = furnace(helper, longCook);
        longEntity.setItem(0, new ItemStack(ModItems.RAW_ENDERITE));
        inject(helper, longEntity, LIT_REMAINING, LIT_TOTAL, COOK_SPENT, SCRAP_COOK_TICKS, -1);
        AbstractFurnaceBlockEntity shortEntity = furnace(helper, shortCook);
        shortEntity.setItem(0, new ItemStack(Items.RAW_IRON));
        inject(helper, shortEntity, 800, 1600, 50, 200, -1);

        Player viewer = helper.makeMockPlayer(GameType.SURVIVAL);

        int[] sent = sentData(helper, longEntity, viewer);
        String sentLine = sent[0] + " / " + sent[1] + " / " + sent[2] + " / " + sent[3];
        helper.assertTrue(fitsShort(sent[0]) && fitsShort(sent[1]) && fitsShort(sent[2]) && fitsShort(sent[3]),
                "the menu of the long cook sends " + sentLine + " (fuel left / fuel total / progress / total), which "
                        + "the shorts in ClientboundContainerSetDataPacket cannot carry");
        AbstractFurnaceMenu client = new BlastFurnaceMenu(2, viewer.getInventory());
        for (int index = 0; index < 4; index++) {
            client.setData(index, (short) sent[index]);
        }
        float trueProgress = COOK_SPENT / (float) SCRAP_COOK_TICKS;
        float trueFuel = LIT_REMAINING / (float) LIT_TOTAL;
        helper.assertTrue(Math.abs(client.getBurnProgress() - trueProgress) <= 1.0F / sent[3],
                "the client's arrow shows " + client.getBurnProgress() + " of the long cook instead of " + trueProgress
                        + " (sent: " + sentLine + ")");
        helper.assertTrue(Math.abs(client.getLitProgress() - trueFuel) <= 1.0F / sent[1],
                "the client's flame shows " + client.getLitProgress() + " of the fuel instead of " + trueFuel
                        + " (sent: " + sentLine + ")");

        int[] control = sentData(helper, shortEntity, viewer);
        helper.assertValueEqual(control[0] + " / " + control[1] + " / " + control[2] + " / " + control[3],
                "800 / 1600 / 50 / 200", "what the menu of a short cook sends (fuel left / fuel total / progress / total)");

        helper.succeed();
    }

    // =====================================================================================
    // THE UPPER TIERS' REWARDS
    // =====================================================================================

    /**
     * One smelt in each of the nine mod devices, and for each how often the recipe was counted in the
     * block entity's saved {@code RecipesUsed}: once in the reinforced tier, twice in the netherite and
     * enderite tiers - in all three families. The count is what the experience is paid from, and on
     * raw gold (exactly 1.0 experience per smelt, so no random rounding) the orbs are popped and added
     * up as well: 1 from the reinforced furnace and blast furnace, 2 from the netherite and enderite
     * ones.
     *
     * <p>Cracked diamond is excluded: in a netherite and in an enderite blast furnace it counts once
     * and pays its single point - doubled, the loss free diamond cycle through the sledgehammer would
     * turn into an experience farm.
     *
     * <p>All eleven cases are compared in one list, so a family that lost its doubling shows next to
     * the two that kept it.
     *
     * <p>What breaks this test: {@code FurnaceTierPerks#isUpperTier} missing a device, the second
     * {@code super.setRecipeUsed} dropped from one of the three block entities, and the
     * {@code furnace_bonus_excluded} check dropped or its tag not generated.
     */
    public static void upperTierFurnacesPayDoubleExperience(GameTestHelper helper) {
        record Case(String label, BlockPos pos, Block block, Item input, String expected) {
        }
        List<Case> cases = List.of(
                new Case("reinforced furnace", new BlockPos(1, 1, 1), ModBlocks.REINFORCED_FURNACE, Items.RAW_GOLD, "1x, 1 xp"),
                new Case("netherite furnace", new BlockPos(3, 1, 1), ModBlocks.NETHERITE_FURNACE, Items.RAW_GOLD, "2x, 2 xp"),
                new Case("enderite furnace", new BlockPos(5, 1, 1), ModBlocks.ENDERITE_FURNACE, Items.RAW_GOLD, "2x, 2 xp"),
                new Case("reinforced smoker", new BlockPos(1, 1, 3), ModBlocks.REINFORCED_SMOKER, Items.BEEF, "1x"),
                new Case("netherite smoker", new BlockPos(3, 1, 3), ModBlocks.NETHERITE_SMOKER, Items.BEEF, "2x"),
                new Case("enderite smoker", new BlockPos(5, 1, 3), ModBlocks.ENDERITE_SMOKER, Items.BEEF, "2x"),
                new Case("reinforced blast furnace", new BlockPos(1, 1, 5), ModBlocks.REINFORCED_BLAST_FURNACE, Items.RAW_GOLD, "1x, 1 xp"),
                new Case("netherite blast furnace", new BlockPos(3, 1, 5), ModBlocks.NETHERITE_BLAST_FURNACE, Items.RAW_GOLD, "2x, 2 xp"),
                new Case("enderite blast furnace", new BlockPos(5, 1, 5), ModBlocks.ENDERITE_BLAST_FURNACE, Items.RAW_GOLD, "2x, 2 xp"),
                new Case("netherite blast furnace, cracked diamond", new BlockPos(1, 1, 7),
                        ModBlocks.NETHERITE_BLAST_FURNACE, ModItems.CRACKED_DIAMOND, "1x, 1 xp"),
                new Case("enderite blast furnace, cracked diamond", new BlockPos(3, 1, 7),
                        ModBlocks.ENDERITE_BLAST_FURNACE, ModItems.CRACKED_DIAMOND, "1x, 1 xp"));

        for (Case c : cases) {
            helper.setBlock(c.pos(), c.block());
            AbstractFurnaceBlockEntity entity = furnace(helper, c.pos());
            entity.setItem(0, new ItemStack(c.input()));
            entity.setItem(1, new ItemStack(Items.COAL));
        }

        helper.startSequence()
                .thenWaitUntil(() -> {
                    for (Case c : cases) {
                        helper.assertTrue(!furnace(helper, c.pos()).getItem(2).isEmpty(),
                                "the " + c.label() + " has not finished its smelt yet");
                    }
                })
                .thenExecute(() -> {
                    StringBuilder actual = new StringBuilder();
                    StringBuilder expected = new StringBuilder();
                    for (Case c : cases) {
                        AbstractFurnaceBlockEntity entity = furnace(helper, c.pos());
                        String line = recipesUsed(helper, entity) + "x";
                        if (c.expected().contains("xp")) {
                            Vec3 popAt = helper.absoluteVec(Vec3.atCenterOf(c.pos()).add(0.0, 1.0, 0.0));
                            entity.getRecipesToAwardAndPopExperience(helper.getLevel(), popAt);
                            line += ", " + experienceAt(helper, popAt) + " xp";
                        }
                        actual.append(c.label()).append(": ").append(line).append("; ");
                        expected.append(c.label()).append(": ").append(c.expected()).append("; ");
                    }
                    helper.assertValueEqual(actual.toString(), expected.toString(),
                            "how often each device counted its one smelt, and the experience it paid");
                })
                .thenSucceed();
    }

    /**
     * Nine raw iron go through a netherite and an enderite blast furnace. The netherite one pays one
     * ingot extra on every fourth smelt and hands out 11, the enderite one on every second and hands
     * out 13 - nine smelts, so a period of three or five would show (12 / 10 and 12 / 18). The
     * controls: four raw iron give four ingots in a reinforced blast furnace (no bonus below netherite)
     * and in an enderite <em>furnace</em> (the bonus is the blast furnace's), and four cracked
     * diamonds give four diamonds in an enderite blast furnace.
     *
     * <p>Raw enderite is excluded too, but its blast is an hour long, so it is shown with an injected
     * cook: an enderite blast furnace one smelt short of its bonus (bonus progress 1 of 2) and two
     * ticks from the end of the blast finishes exactly one scrap, and still owes its bonus afterwards.
     * The same injection with raw iron finishes two ingots - the bonus progress is saved and loaded
     * under {@code simplebuilding:bonus_progress}.
     *
     * <p>All seven devices are compared in one line.
     *
     * <p>What breaks this test: another bonus period for either tier, the bonus reaching the reinforced
     * blast furnace or any furnace, the {@code blast_furnace_bonus} tag gaining raw enderite or cracked
     * diamond or losing raw iron, and the bonus progress not loaded or not saved.
     */
    public static void blastFurnaceBonusPaysRawMetalsEveryFourthOrSecondSmelt(GameTestHelper helper) {
        BlockPos netherite = new BlockPos(1, 1, 1);
        BlockPos enderite = new BlockPos(3, 1, 1);
        BlockPos reinforced = new BlockPos(5, 1, 1);
        BlockPos enderiteFurnace = new BlockPos(1, 1, 4);
        BlockPos crackedDiamond = new BlockPos(3, 1, 4);
        BlockPos rawEnderite = new BlockPos(5, 1, 4);
        BlockPos injectedIron = new BlockPos(3, 1, 7);

        loadSmelt(helper, netherite, ModBlocks.NETHERITE_BLAST_FURNACE, Items.RAW_IRON, 9);
        loadSmelt(helper, enderite, ModBlocks.ENDERITE_BLAST_FURNACE, Items.RAW_IRON, 9);
        loadSmelt(helper, reinforced, ModBlocks.REINFORCED_BLAST_FURNACE, Items.RAW_IRON, 4);
        loadSmelt(helper, enderiteFurnace, ModBlocks.ENDERITE_FURNACE, Items.RAW_IRON, 4);
        loadSmelt(helper, crackedDiamond, ModBlocks.ENDERITE_BLAST_FURNACE, ModItems.CRACKED_DIAMOND, 4);

        helper.setBlock(rawEnderite, ModBlocks.ENDERITE_BLAST_FURNACE);
        furnace(helper, rawEnderite).setItem(0, new ItemStack(ModItems.RAW_ENDERITE));
        inject(helper, furnace(helper, rawEnderite), 800, 1600, SCRAP_COOK_TICKS - 2, SCRAP_COOK_TICKS, 1);
        helper.setBlock(injectedIron, ModBlocks.ENDERITE_BLAST_FURNACE);
        furnace(helper, injectedIron).setItem(0, new ItemStack(Items.RAW_IRON));
        int ironBlast = helper.getLevel().getServer().getRecipeManager()
                .getRecipeFor(RecipeType.BLASTING, new SingleRecipeInput(new ItemStack(Items.RAW_IRON)), helper.getLevel())
                .orElseThrow().value().cookingTime();
        inject(helper, furnace(helper, injectedIron), 800, 1600, ironBlast - 2, ironBlast, 1);

        List<BlockPos> all = List.of(netherite, enderite, reinforced, enderiteFurnace, crackedDiamond, rawEnderite, injectedIron);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    for (BlockPos pos : all) {
                        helper.assertTrue(furnace(helper, pos).getItem(0).isEmpty(),
                                "the device at " + pos + " has not smelted all its input yet");
                    }
                })
                .thenExecute(() -> helper.assertValueEqual(
                        "netherite blast furnace " + output(helper, netherite)
                                + "; enderite blast furnace " + output(helper, enderite)
                                + "; reinforced blast furnace " + output(helper, reinforced)
                                + "; enderite furnace " + output(helper, enderiteFurnace)
                                + "; cracked diamonds " + output(helper, crackedDiamond)
                                + "; raw enderite " + output(helper, rawEnderite) + " owing "
                                + bonusProgress(helper, furnace(helper, rawEnderite))
                                + "; injected raw iron " + output(helper, injectedIron),
                        "netherite blast furnace 11 minecraft:iron_ingot"
                                + "; enderite blast furnace 13 minecraft:iron_ingot"
                                + "; reinforced blast furnace 4 minecraft:iron_ingot"
                                + "; enderite furnace 4 minecraft:iron_ingot"
                                + "; cracked diamonds 4 minecraft:diamond"
                                + "; raw enderite 1 simplebuilding:enderite_scrap owing 1"
                                + "; injected raw iron 2 minecraft:iron_ingot",
                        "what each device put out"))
                .thenSucceed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    private static AbstractFurnaceBlockEntity furnace(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, AbstractFurnaceBlockEntity.class);
    }

    private static void loadSmelt(GameTestHelper helper, BlockPos pos, Block block, Item input, int count) {
        helper.setBlock(pos, block);
        AbstractFurnaceBlockEntity entity = furnace(helper, pos);
        entity.setItem(0, new ItemStack(input, count));
        entity.setItem(1, new ItemStack(Items.COAL, 8));
    }

    /**
     * Loads the four timers - and, if not negative, the blast furnace's bonus progress - into the
     * block entity the way a chunk load would: its own saved form with the numbers overwritten as
     * ints, handed back to {@code loadCustomOnly}. The saved form carries the items, so none are lost.
     */
    private static void inject(GameTestHelper helper, AbstractFurnaceBlockEntity entity, int litRemaining, int litTotal,
                               int spent, int total, int bonusProgress) {
        ServerLevel level = helper.getLevel();
        CompoundTag tag = entity.saveWithoutMetadata(level.registryAccess());
        tag.putInt("lit_time_remaining", litRemaining);
        tag.putInt("lit_total_time", litTotal);
        tag.putInt("cooking_time_spent", spent);
        tag.putInt("cooking_total_time", total);
        if (bonusProgress >= 0) {
            tag.putInt(BONUS_PROGRESS_KEY, bonusProgress);
        }
        entity.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
    }

    /** A persisted int; a missing key fails instead of reading like a plausible number. */
    private static int persisted(GameTestHelper helper, AbstractFurnaceBlockEntity entity, String key) {
        CompoundTag tag = entity.saveWithoutMetadata(helper.getLevel().registryAccess());
        int value = tag.getIntOr(key, Integer.MIN_VALUE);
        helper.assertTrue(value != Integer.MIN_VALUE, "the furnace no longer saves '" + key + "'");
        return value;
    }

    /** The saved bonus progress of a blast furnace; the block entity leaves the key out when it is 0. */
    private static int bonusProgress(GameTestHelper helper, AbstractFurnaceBlockEntity entity) {
        return entity.saveWithoutMetadata(helper.getLevel().registryAccess()).getIntOr(BONUS_PROGRESS_KEY, 0);
    }

    /** The four saved timers as one line. */
    private static String timers(GameTestHelper helper, AbstractFurnaceBlockEntity entity) {
        return timerLine(persisted(helper, entity, "lit_time_remaining"), persisted(helper, entity, "lit_total_time"),
                persisted(helper, entity, "cooking_time_spent"), persisted(helper, entity, "cooking_total_time"));
    }

    private static String timerLine(int litRemaining, int litTotal, int spent, int total) {
        return "fuel " + litRemaining + " of " + litTotal + ", cooked " + spent + " of " + total;
    }

    private static boolean fitsShort(int value) {
        return value >= Short.MIN_VALUE && value <= Short.MAX_VALUE;
    }

    /**
     * Opens the device's menu on the server and records the four data values it hands to a listener
     * - the values the data packet carries.
     */
    private static int[] sentData(GameTestHelper helper, AbstractFurnaceBlockEntity entity, Player viewer) {
        AbstractContainerMenu menu = entity.createMenu(1, viewer.getInventory(), viewer);
        helper.assertTrue(menu != null, "test setup broken: the furnace opened no menu");
        int[] sent = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        menu.addSlotListener(new ContainerListener() {
            @Override
            public void slotChanged(AbstractContainerMenu changed, int slot, ItemStack stack) {
            }

            @Override
            public void dataChanged(AbstractContainerMenu changed, int index, int value) {
                if (index >= 0 && index < sent.length) {
                    sent[index] = value;
                }
            }
        });
        menu.broadcastChanges();
        for (int index = 0; index < sent.length; index++) {
            helper.assertTrue(sent[index] != Integer.MIN_VALUE, "test setup broken: the menu never sent data value " + index);
        }
        return sent;
    }

    /** The count of every recipe in the block entity's saved {@code RecipesUsed}, added up. */
    private static int recipesUsed(GameTestHelper helper, AbstractFurnaceBlockEntity entity) {
        CompoundTag used = entity.saveWithoutMetadata(helper.getLevel().registryAccess()).getCompoundOrEmpty("RecipesUsed");
        int total = 0;
        for (String recipe : used.keySet()) {
            total += used.getIntOr(recipe, 0);
        }
        return total;
    }

    /**
     * The experience in the orbs right at {@code at}, read in the tick they were popped, before they
     * move. Popping two points makes two one-point orbs, and the second merges into the first one
     * time in forty ({@code ExperienceOrb#tryMergeToExisting}) - so each orb's value is multiplied by
     * the count it saves under {@code Count}, the only place that count is readable from.
     */
    private static int experienceAt(GameTestHelper helper, Vec3 at) {
        int total = 0;
        for (ExperienceOrb orb : helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, AABB.ofSize(at, 0.5, 0.5, 0.5))) {
            TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING,
                    helper.getLevel().registryAccess());
            orb.saveWithoutId(saved);
            total += orb.getValue() * saved.buildResult().getIntOr("Count", 1);
            orb.discard();
        }
        return total;
    }

    /** The output slot as "count item id". */
    private static String output(GameTestHelper helper, BlockPos pos) {
        ItemStack output = furnace(helper, pos).getItem(2);
        return output.getCount() + " " + BuiltInRegistries.ITEM.getKey(output.getItem());
    }
}
