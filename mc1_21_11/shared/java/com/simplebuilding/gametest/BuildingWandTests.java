package com.simplebuilding.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.loot.ModLootTableModifications;
import com.simplebuilding.util.ModTags;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntBinaryOperator;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The building wand itself: which hand may use it, which plane it fills, how big that plane is
 * allowed to get, where its blocks come from, and the data layer that hands the six wands to the
 * player in the first place.
 *
 * <p>{@link ItemBehaviourTests#buildingWandFillsThePlaneItIsPointedAt} owns the geometry: the top
 * face, the north face, all three forced axis modes on a top face click and the copper tier cap,
 * each against an exact set of positions, plus the material search as far as
 * {@code findFirstBuildingBlock} answers it. What is left over is what this file is about - the one
 * face axis that setup never produces, a forced axis on a <em>wall</em> instead of on a floor, the
 * maximum diameters of all six tiers, the second material search and the Master Builder half of
 * both, the tick that runs while the wand is in <em>neither</em> hand, and the data files behind
 * the item.
 *
 * <h2>Deliberately not duplicated here</h2>
 * <ul>
 *   <li><b>The plane, position by position</b> -
 *       {@link ItemBehaviourTests#buildingWandFillsThePlaneItIsPointedAt} drives the top face, the
 *       north face and axis modes 1, 2 and 3 on a top face click, and
 *       {@link ConsumptionAndDurabilityTests#buildingWandBillsOneBlockAndOnePointOfWearPerPlacement}
 *       owns the exact "one block and one point of wear per placement" arithmetic. Nothing below
 *       counts durability again, and {@link #clickedFaceSetsThePlaneUntilAnAxisModeOverridesIt}
 *       keeps only the two cases neither of them can produce.</li>
 *   <li><b>The bundle as a material source</b> -
 *       {@link BundleWiringTests#buildingWandBuildsFromTheBundleAndPaysOnePiecePerBlock} drives all
 *       three cases of the either-or Master Builder rule and counts what leaves the bundle. This
 *       file only covers the <em>loose stack</em> half of the same search, which that test does not
 *       reach.</li>
 *   <li><b>Colour Palette and Linear</b> -
 *       {@link BuildingEnchantmentTests#colorPaletteKeepsTheWandBuildingWhenOneBlockRunsOut} and
 *       {@link BuildingEnchantmentTests#linearOnlyShortensTheWandStepDelay}. The one palette case
 *       below is the creative-with-nothing-to-build-from fallback, which neither of them reaches
 *       because both always carry material.</li>
 *   <li><b>Storing the two settings</b> -
 *       {@link NetworkHandlerTests#buildingWandConfigureStoresRadiusAndAxis} pins that the payload
 *       writes {@code SettingsRadius} and {@code SettingsAxis} into the stack. What those two
 *       numbers then <em>do</em> is what is missing, and is the subject of
 *       {@link #clickedFaceSetsThePlaneUntilAnAxisModeOverridesIt} and
 *       {@link #wandTierCapsTheRadiusSettingAndSizesThePlane}.</li>
 *   <li><b>The mason's wand trade</b> - {@code TradeAndMigrationTests} and {@code TradeOfferTests}
 *       each build {@code mason/4/emerald_copper_building_wand} into a real {@code MerchantOffer}
 *       and compare price, result, {@code max_uses}, xp and reputation discount field by field. A
 *       third copy of those five numbers would say nothing the two of them do not.</li>
 *   <li><b>That both wand loot entries exist</b> - {@code ConfigOptionTests} lists the iron wand
 *       under the woodland mansion and the diamond wand under the end city treasure, as a lower
 *       bound per table. {@link #ironWandDropsInTheMansionAndTheDiamondWandInTheEndCity} adds what
 *       a lower bound cannot say: that neither tier turns up in the other tier's table, and that
 *       the end city entry is the one that is handed out enchanted.</li>
 * </ul>
 *
 * <h2>Not covered, and why</h2>
 * <ul>
 *   <li><b>The preview highlight</b> ({@code BuildingWandPreviewRenderer}) and the settings screen
 *       ({@code BuildingWandScreen}, and the key binding that opens it). Client only; a gametest
 *       server has no renderer and the mock player's connection swallows the screen packets. The
 *       shared half of the preview - {@code getPreviewStates} - is server callable and is already
 *       driven by {@link BuildingEnchantmentTests#colorPaletteSpreadsTheCarriedBlocksOverTheWandPreview}.</li>
 *   <li><b>The placement sound.</b> {@code world.playSound} is fired per placed block; nothing
 *       server side can observe that a client would have heard it.</li>
 *   <li><b>The stored hit location</b> ({@code HitX}/{@code HitY}/{@code HitZ}). The wand writes it
 *       in {@code useOn} and no server side branch ever reads it back - only the renderer does. A
 *       test could only restate the three {@code putFloat} lines.</li>
 * </ul>
 *
 * <h2>Known defects (deliberately not pinned, so no test cements them)</h2>
 * <ul>
 *   <li><b>The enderite wand is missing from {@code simplebuilding:building_wand_enchantable}.</b>
 *       {@code ModItemTagProvider} lists copper through netherite only, so Cover, Bridge and Linear
 *       cannot be put on the top tier wand at all - the tag is both the {@code primary_items} and
 *       the {@code supported_items} of those three enchantments.
 *       {@link #wandEnchantmentsOnlyStickToTheWandsInTheirItemTag} asserts the five that are meant
 *       to work and says nothing about the enderite one.</li>
 *   <li><b>A forced axis mode shifts the plane off centre.</b> {@code calculatePositions} always
 *       starts from {@code originPos.relative(face)}, even when {@code SettingsAxis} forces a build
 *       axis that is not the clicked face's axis. The plane then does not surround the clicked
 *       block but sits one block to the side of it, and one of its nine cells lands on the clicked
 *       block itself, where {@code canBeReplaced} silently drops it - the player asks for a 3x3 and
 *       gets eight blocks, off centre. {@link #clickedFaceSetsThePlaneUntilAnAxisModeOverridesIt}
 *       therefore states the <em>shape</em> and leaves the position open: a filled 3x3 square,
 *       normal to the forced axis, at most one block off the clicked block along that axis, and
 *       with no cell missing but the clicked block itself. Today's eight off centre blocks pass
 *       that, and so does a centred nine once the offset is fixed - while a line, a ring, a hole
 *       or a wrongly oriented plane fails. Counting the eight instead would have frozen the
 *       defect: a centred plane places nine.</li>
 *   <li><b>A negative radius makes the highlight and the click disagree.</b>
 *       {@code handleBuildingWandConfigure} writes {@code SettingsRadius} straight out of the
 *       packet without a lower bound. {@code getPreviewStates} clamps it to 0 and
 *       {@code inventoryTick} ends up placing the centre block anyway, but
 *       {@code getBuildingPositions} - the list the highlight is drawn from - does not clamp and
 *       returns nothing at all. Reachable from a modified client only, and pinned nowhere.</li>
 *   <li><b>Wear is always billed against the main hand.</b> {@code inventoryTick} keeps building
 *       while the wand sits in the off hand (the slot check accepts both hands), but the
 *       {@code hurtAndBreak} call names {@code EquipmentSlot.MAINHAND} unconditionally, so a wand
 *       that breaks in the off hand announces the break on the wrong hand.
 *       {@link #offHandClickIsPassedOnAndTheWandStopsOutsideBothHands} pins that the off hand
 *       keeps building, not which slot pays for it.</li>
 * </ul>
 */
public final class BuildingWandTests {

    private BuildingWandTests() {
    }

    /**
     * Upper bound for every wand tick loop in this file, so a wand that never switches itself off
     * fails the test instead of hanging the run. This is a loop guard, not a tick budget - every
     * test here drives {@code inventoryTick} by hand and finishes inside the first game tick.
     */
    private static final int WAND_TICK_CAP = 200;

    /**
     * The one block every run in this file is clicked on. It sits far enough from all six walls
     * that the biggest plane below (a 5x5 one block away) plus one ring of headroom still fits
     * inside the 8x8x8 room.
     */
    private static final BlockPos ANCHOR = new BlockPos(3, 3, 3);

    /**
     * How far around {@link #ANCHOR} a run is cleared and read back. A wand at radius r only ever
     * touches positions within r+1 blocks of the clicked block, so reach 2 is complete for a 3x3
     * and reach 3 for a 5x5 - and reading a wider box than the wand can reach is what turns "the
     * plane has nine blocks" into "the plane has nine blocks and nothing else was placed".
     */
    private static final int SMALL_SITE = 2;

    private static final int LARGE_SITE = 3;

    // =====================================================================================
    // HANDS
    // =====================================================================================

    /**
     * The wand only ever answers a main hand click, but once it is running it keeps running in
     * either hand and gives up the moment it is in neither.
     *
     * <p>Three things, all of them a single {@code if} in the item and none of them reachable from
     * the existing wand tests, which click with the main hand and always tick with
     * {@code EquipmentSlot.MAINHAND}:
     *
     * <ul>
     *   <li><b>{@code useOn} passes on an off hand click.</b> Asserted as "the wand wrote nothing
     *       into its own NBT", not as "Active is false" - an absent key and a false key read the
     *       same through {@code getBooleanOr}, so only the stronger form can tell "refused" from
     *       "armed and immediately gave up".</li>
     *   <li><b>A wand that leaves both hands stops.</b> The check sits in front of the timer, so
     *       it takes effect on the very next tick, mid plane. Both runs below are interrupted after
     *       the centre block and before the outer ring.</li>
     *   <li><b>And it stops for good.</b> Five further main hand ticks must not finish the plane.
     *       Without this, an implementation that only <em>paused</em> - say by not writing the flag
     *       back - would pass the assertion above and still be wrong.</li>
     * </ul>
     *
     * <p>The interruption is driven twice, with the two slot values vanilla really produces for a
     * stack that is not in a hand. {@code Inventory#tick} walks the 36 non equipment slots and
     * hands {@code inventoryTick} {@code MAINHAND} for the selected one and <em>{@code null}</em>
     * for the other 35 - which is why {@code Item#inventoryTick} declares the parameter nullable at
     * all - so {@code null} is what a wand lying in the backpack is ticked with, and it is the only
     * value the javadoc promise "a wand in the backpack does not keep building" can be measured
     * against. {@code EntityEquipment#tick} passes the real slot for everything it walks, which is
     * where the second value, {@code CHEST}, comes from. Driving both is what keeps the guard a
     * statement about <em>which</em> slots are hands instead of one about {@code null}: written as
     * "not null and not a hand", the guard would still switch the chest run off while letting a
     * wand in the backpack build on for ever, and only the {@code null} run says so.
     *
     * <p>The last case is the mirror image: the same interrupted build driven entirely through
     * {@code EquipmentSlot.OFFHAND} has to finish. That is what makes the two runs above a
     * statement about which slots are allowed rather than about "any slot but the one it was armed
     * in". {@code inventoryTick} reads nothing but the slot argument, so handing it
     * {@code OFFHAND} is exactly what the server does after the player swaps the wand over.
     *
     * <p><strong>What breaks this test:</strong> dropping the {@code getHand() != MAIN_HAND} guard
     * in {@code useOn} (the off hand click would arm the wand), dropping, narrowing or weakening
     * the slot check in {@code inventoryTick} (a wand in the backpack or in an armour slot would
     * keep building, or one in the off hand would stop), or moving that check behind the timer,
     * which would let the wand place one more ring after it had already left the player's hands.
     */
    public static void offHandClickIsPassedOnAndTheWandStopsOutsideBothHands(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, false);

        // --- 1. the off hand click is not the wand's business ---
        resetSite(helper, SMALL_SITE);
        ItemStack offHandWand = tunedWand(ModItems.DIAMOND_BUILDING_WAND, 1, 0);
        stock(player, tunedWand(ModItems.DIAMOND_BUILDING_WAND, 1, 0), new ItemStack(Items.GLASS, 64));

        InteractionResult passed = useOn(helper, player, offHandWand, Direction.UP, InteractionHand.OFF_HAND);
        helper.assertTrue(passed == InteractionResult.PASS,
                "an off hand click with the wand returned " + passed + " instead of PASS, so the wand "
                        + "answers a hand it is not meant to answer");
        helper.assertFalse(customData(offHandWand).contains("Active"),
                "the off hand click still wrote the wand's build state into the stack");
        helper.assertTrue(blocksIn(helper, SMALL_SITE, Blocks.GLASS).isEmpty(),
                "the off hand click built something");

        // --- 2. a wand that leaves both hands stops where it is, in both slots vanilla can pass ---
        // null is the value Inventory#tick hands every unselected inventory slot, so this is
        // literally "the wand went into the backpack"; CHEST is the EntityEquipment#tick shape of
        // the same question and is what separates "not a hand" from "not null".
        assertLeavingTheHandsStopsTheBuild(helper, player, null,
                "lying in an unselected inventory slot");
        assertLeavingTheHandsStopsTheBuild(helper, player, EquipmentSlot.CHEST,
                "worn in the chest slot");

        // --- 3. the off hand, on the other hand, is allowed to finish the job ---
        resetSite(helper, SMALL_SITE);
        ItemStack offHandRun = tunedWand(ModItems.DIAMOND_BUILDING_WAND, 1, 0);
        stock(player, offHandRun, new ItemStack(Items.GLASS, 64));

        InteractionResult armedAgain = useOn(helper, player, offHandRun, Direction.UP, InteractionHand.MAIN_HAND);
        helper.assertTrue(armedAgain == InteractionResult.CONSUME,
                "the second main hand click did not arm the wand, it returned " + armedAgain);
        driveUntilIdle(helper, player, offHandRun, EquipmentSlot.OFFHAND);

        Assertions.valueEqual(helper, blocksIn(helper, SMALL_SITE, Blocks.GLASS),
                square(ANCHOR.above(), Direction.Axis.Y, 1),
                "a wand ticked in the off hand did not finish its plane, although inventoryTick "
                        + "accepts that slot");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // PLANE ORIENTATION
    // =====================================================================================

    /**
     * Which plane the wand fills, in the two cases
     * {@link ItemBehaviourTests#buildingWandFillsThePlaneItIsPointedAt} cannot produce: a click on
     * the third face axis, and a forced axis on a wall instead of on a floor.
     *
     * <p>That test clicks the top and the north face and forces all three axis modes on a top face
     * click. So {@code face.getAxis()} is driven for Y and Z but never for X - EAST is the only
     * face whose axis neither run produces - and the whole "the setting beats the face" half rests
     * on clicks whose face axis is Y, where "perpendicular to the face" and "horizontal" happen to
     * be the same answer for mode 2.
     *
     * <p>The face aligned case is asserted as an exact position set against the block that was
     * clicked, so a plane that is built in the right shape but at the wrong place - one block too
     * far, or on the wrong side of the clicked block - fails. It is built in absolute terms:
     * {@code GameTestHelper#relativePos} is <em>not</em> the inverse of {@code absolutePos} (it
     * applies the opposite rotation, which turns a relative 5 into -5 for {@code Rotation.NONE}),
     * so everything here stays in the relative coordinates the helper's own read and write methods
     * already take.
     *
     * <p>The forced case states the shape instead of the position, and
     * {@link #assertForcedPlane} says exactly what that is worth: one plane normal to the Y axis,
     * no further than one block from the clicked block along that axis, a filled 3x3 whose only
     * empty cell may be the clicked block itself. It does not pin <em>which</em> nine cells,
     * because the wand currently offsets a forced plane by one block towards the clicked face even
     * when that offset lands inside the plane - see the known defect in the class javadoc. An exact
     * set would freeze today's eight off centre blocks.
     *
     * <p><strong>What breaks this test:</strong> dropping the {@code face.getAxis()} default in
     * {@code calculatePositions} (the east plane collapses onto another axis), pointing axis mode 2
     * at any axis but Y, swapping the entries of {@code getOffsetForAxis} (the east click stops
     * building an upright plane, the forced one a flat), losing the {@code relative(face)} step
     * (the east plane would grow inside the clicked block), letting the wand overwrite blocks that
     * cannot be replaced, or a forced plane that stops being a filled square of the configured size
     * - a ring, a line, a plane with a hole, or one built further away than a single block.
     */
    public static void clickedFaceSetsThePlaneUntilAnAxisModeOverridesIt(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, false);

        // --- the one face axis no other wand test clicks: X, so the plane stands in Y/Z ---
        Set<BlockPos> east = buildPlane(helper, player,
                tunedWand(ModItems.DIAMOND_BUILDING_WAND, 1, 0), Direction.EAST, SMALL_SITE);
        Assertions.valueEqual(helper, east, square(ANCHOR.relative(Direction.EAST), Direction.Axis.X, 1),
                "clicking the east face did not fill the upright plane one block east of the "
                        + "clicked block");

        // --- axis mode 2 forces the horizontal plane even on a wall click ---
        Set<BlockPos> forcedFlat = buildPlane(helper, player,
                tunedWand(ModItems.DIAMOND_BUILDING_WAND, 1, 2), Direction.NORTH, SMALL_SITE);
        assertForcedPlane(helper, forcedFlat, Direction.Axis.Y, "axis mode 2 on the north face");
        helper.assertTrue(helper.getBlockState(ANCHOR).is(Blocks.OBSIDIAN),
                "the wand replaced the block it was clicked on");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // RADIUS AND TIER
    // =====================================================================================

    /**
     * How big the plane may get. The six wands carry six maximum diameters, and the player's own
     * {@code SettingsRadius} is capped by the one on the wand in hand.
     *
     * <p>The diameters were only ever passed through: {@code ModItems} hands a
     * {@code BUILDING_WAND_SQUARE_*} constant to {@code setWandSquareDiameter} per tier and nothing
     * read the result back, so two tiers wired to the same constant - or the gold and diamond ones
     * swapped - looked exactly like a working mod. Three properties are asserted together, because
     * each on its own is easy to satisfy by accident: every wand reports the constant meant for its
     * tier, all six differ and grow with the tier, and every one of them is odd (an even diameter
     * has no centre block, and {@code (diameter - 1) / 2} would silently round it down).
     *
     * <p>The cap is then measured rather than restated. Both runs ask for radius 99 - far past
     * every tier - and the expected shape is computed from {@code getWandSquareDiameter()} itself,
     * so this states "the wand builds the square its own tier declares" instead of repeating the
     * numbers 3 and 5. Two tiers are needed for that: with a single one, a cap hard coded to any
     * fixed number would pass. That is why the copper run stays here although
     * {@link ItemBehaviourTests#buildingWandFillsThePlaneItIsPointedAt} also drives a capped copper
     * wand - it is the lower of the two points of one measurement, and the iron wand is the widest
     * tier whose plane still fits into the room with a ring of headroom around it.
     *
     * <p>The read back box is one ring wider than the biggest expected plane, so a plane that is
     * built too large is caught as extra positions rather than going unnoticed outside the window.
     *
     * <p><strong>What breaks this test:</strong> a tier wired to the wrong constant in
     * {@code ModItems}, dropping the {@code userRadius > maxTierRadius} cap in
     * {@code inventoryTick} (the copper wand would keep ringing outwards until its material ran
     * out), capping against a constant instead of {@code this.maxDiameter}, or changing
     * {@code maxTierRadius} to something other than {@code (diameter - 1) / 2}.
     */
    public static void wandTierCapsTheRadiusSettingAndSizesThePlane(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, false);

        List<BuildingWandItem> tiers = List.of(
                ModItems.COPPER_BUILDING_WAND,
                ModItems.IRON_BUILDING_WAND,
                ModItems.GOLD_BUILDING_WAND,
                ModItems.DIAMOND_BUILDING_WAND,
                ModItems.NETHERITE_BUILDING_WAND,
                ModItems.ENDERITE_BUILDING_WAND);
        List<Integer> declared = List.of(
                BuildingWandItem.BUILDING_WAND_SQUARE_COPPER,
                BuildingWandItem.BUILDING_WAND_SQUARE_IRON,
                BuildingWandItem.BUILDING_WAND_SQUARE_GOLD,
                BuildingWandItem.BUILDING_WAND_SQUARE_DIAMOND,
                BuildingWandItem.BUILDING_WAND_SQUARE_NETHERITE,
                BuildingWandItem.BUILDING_WAND_SQUARE_ENDERITE);

        int previous = 0;
        for (int tier = 0; tier < tiers.size(); tier++) {
            BuildingWandItem wandItem = tiers.get(tier);
            int diameter = wandItem.getWandSquareDiameter();
            Assertions.valueEqual(helper, diameter, declared.get(tier),
                    BuiltInRegistries.ITEM.getKey(wandItem) + " was registered with the wrong "
                            + "BUILDING_WAND_SQUARE constant");
            helper.assertTrue(diameter % 2 == 1,
                    BuiltInRegistries.ITEM.getKey(wandItem) + " has an even maximum diameter of "
                            + diameter + ", so its plane has no centre block and (diameter - 1) / 2 "
                            + "quietly loses a ring");
            helper.assertTrue(diameter > previous,
                    BuiltInRegistries.ITEM.getKey(wandItem) + " does not build wider than the tier "
                            + "below it: " + diameter + " against " + previous);
            previous = diameter;
        }

        // --- the cheapest wand, asked for far more than it may give ---
        int copperDiameter = ModItems.COPPER_BUILDING_WAND.getWandSquareDiameter();
        Set<BlockPos> copper = buildPlane(helper, player,
                tunedWand(ModItems.COPPER_BUILDING_WAND, 99, 0), Direction.UP, LARGE_SITE);
        Assertions.valueEqual(helper, copper,
                square(ANCHOR.above(), Direction.Axis.Y, (copperDiameter - 1) / 2),
                "the copper wand did not build the square its own maximum diameter of "
                        + copperDiameter + " allows when it was asked for radius 99");

        // --- and one tier up, so the cap cannot be a fixed number ---
        int ironDiameter = ModItems.IRON_BUILDING_WAND.getWandSquareDiameter();
        Set<BlockPos> iron = buildPlane(helper, player,
                tunedWand(ModItems.IRON_BUILDING_WAND, 99, 0), Direction.UP, LARGE_SITE);
        Assertions.valueEqual(helper, iron,
                square(ANCHOR.above(), Direction.Axis.Y, (ironDiameter - 1) / 2),
                "the iron wand did not build the square its own maximum diameter of " + ironDiameter
                        + " allows when it was asked for radius 99");
        helper.assertTrue(iron.size() > copper.size(),
                "both wands built the same plane out of the same radius 99 setting, so the cap does "
                        + "not follow the tier at all");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // WHERE THE MATERIAL COMES FROM
    // =====================================================================================

    /**
     * The material search: off hand first, then the hotbar, and the rest of the inventory only for
     * a Master Builder wand.
     *
     * <p>That order exists twice in the item, and the two copies answer different questions:
     * {@code findFirstBuildingBlock} picks the block the wand is armed with when the click comes
     * in, {@code findSpecificMaterial} picks the stack that pays for each block once it is
     * building. {@link ItemBehaviourTests#buildingWandFillsThePlaneItIsPointedAt} drives the first
     * of the two - an off hand block beats a hotbar block, a backpack alone gets the click refused
     * - but it holds a different block in each place, so which of the two searches answered is not
     * separable there, and no run in it carries Master Builder at all.
     *
     * <p>Four runs:
     * <ul>
     *   <li><b>Off hand beats hotbar, in both searches.</b> Glass in the off hand, oak planks in
     *       hotbar slot 1, the same glass again in hotbar slot 2. The planks answer for the first
     *       search: a wand that walked the hotbar first would arm itself with planks and build a
     *       plane of planks. The second glass stack answers for the second one: the off hand stack
     *       has to be the one that shrinks, the hotbar stack the one that stays full.</li>
     *   <li><b>The backpack is closed without Master Builder.</b> The only glass in the game sits
     *       in slot 20. The click is refused outright - {@code useOn} returns FAIL when the search
     *       comes back empty and the player is not in creative - and nothing is spent.</li>
     *   <li><b>And open with it.</b> The same slot, the same click, an enchanted wand: the full
     *       plane, paid for out of slot 20.</li>
     *   <li><b>The same pair again, mid plane.</b> One single glass block in the off hand arms the
     *       wand and pays for the centre of the plane; everything after it can only come out of
     *       slot 20. This is the only run that reaches the backpack guard inside
     *       {@code findSpecificMaterial} - run two stops at {@code useOn} and never ticks, so with
     *       that second guard deleted runs one to three stay green while a plain wand quietly
     *       builds on out of the backpack the moment the stack in the hand runs out.</li>
     * </ul>
     *
     * <p>The counts are what make the runs pairs rather than four separate stories. Without them,
     * "nothing was built" and "the plane was built" could both be produced by a wand that never
     * looked at the backpack at all and simply had no material in one of the two runs.
     *
     * <p><strong>What breaks this test:</strong> moving either off hand lookup behind its hotbar
     * loop (run one), letting either hotbar loop run past slot 8 (runs two and four), dropping the
     * {@code hasMasterBuilder} guard in front of either backpack loop (runs two and four would
     * build), reading the enchantment off the wrong stack, or removing the
     * {@code preview == null && !instabuild} early return in {@code useOn}.
     */
    public static void materialSearchPrefersTheOffHandAndOnlyMasterBuilderReachesTheBackpack(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, false);
        Set<BlockPos> plane = square(ANCHOR.above(), Direction.Axis.Y, 1);

        // --- 1. the off hand wins twice: over a different block, and over the same one ---
        // The planks are what findFirstBuildingBlock would trip over, the hotbar glass what
        // findSpecificMaterial would; with only one of the two in the hotbar, one of the two
        // searches would still be free to walk the hotbar first without anything going red.
        resetSite(helper, SMALL_SITE);
        ItemStack wand = tunedWand(ModItems.DIAMOND_BUILDING_WAND, 1, 0);
        ItemStack hotbarPlanks = new ItemStack(Items.OAK_PLANKS, 16);
        ItemStack hotbarGlass = new ItemStack(Items.GLASS, 16);
        stock(player, wand, hotbarPlanks, hotbarGlass);
        ItemStack offHandGlass = new ItemStack(Items.GLASS, 64);
        player.setItemInHand(InteractionHand.OFF_HAND, offHandGlass);

        InteractionResult armed = useOn(helper, player, wand, Direction.UP, InteractionHand.MAIN_HAND);
        helper.assertTrue(armed == InteractionResult.CONSUME,
                "the wand refused a click although both hands held building blocks, it returned " + armed);
        driveUntilIdle(helper, player, wand, EquipmentSlot.MAINHAND);

        Assertions.valueEqual(helper, blocksIn(helper, SMALL_SITE, Blocks.GLASS), plane,
                "the wand did not build the plane out of the glass in the off hand");
        helper.assertTrue(blocksIn(helper, SMALL_SITE, Blocks.OAK_PLANKS).isEmpty(),
                "the wand armed itself with the hotbar planks, so findFirstBuildingBlock reached the "
                        + "hotbar before the off hand");
        Assertions.valueEqual(helper, hotbarPlanks.getCount(), 16,
                "the hotbar planks were spent although the off hand supplied the plane");
        Assertions.valueEqual(helper, offHandGlass.getCount(), 64 - plane.size(),
                "the off hand glass is not what paid for the plane");
        Assertions.valueEqual(helper, hotbarGlass.getCount(), 16,
                "the hotbar glass paid for part of the plane although the off hand held the same "
                        + "block, so findSpecificMaterial searches the hotbar before the off hand");

        // --- 2. material that is neither in a hand nor in the hotbar is invisible ---
        resetSite(helper, SMALL_SITE);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        ItemStack plainWand = tunedWand(ModItems.DIAMOND_BUILDING_WAND, 1, 0);
        stockBackpackOnly(player, plainWand, new ItemStack(Items.GLASS, 64));

        InteractionResult refused = useOn(helper, player, plainWand, Direction.UP, InteractionHand.MAIN_HAND);
        helper.assertTrue(refused == InteractionResult.FAIL,
                "a wand without Master Builder accepted the click while its only glass was in the "
                        + "backpack, it returned " + refused);
        helper.assertFalse(wandIsActive(plainWand),
                "the wand armed itself although the search had come back empty");
        helper.assertTrue(blocksIn(helper, SMALL_SITE, Blocks.GLASS).isEmpty(),
                "the refused click still built something");
        Assertions.valueEqual(helper, countCarried(player, Items.GLASS), 64,
                "the refused click still spent glass out of the backpack");

        // --- 3. Master Builder opens exactly that slot ---
        ItemStack builderWand = tunedWand(ModItems.DIAMOND_BUILDING_WAND, 1, 0);
        builderWand.enchant(enchantment(helper, ModEnchantments.MASTER_BUILDER), 1);
        stockBackpackOnly(player, builderWand, new ItemStack(Items.GLASS, 64));

        InteractionResult accepted = useOn(helper, player, builderWand, Direction.UP, InteractionHand.MAIN_HAND);
        helper.assertTrue(accepted == InteractionResult.CONSUME,
                "the Master Builder wand refused the click although the backpack held glass, it "
                        + "returned " + accepted);
        driveUntilIdle(helper, player, builderWand, EquipmentSlot.MAINHAND);

        Assertions.valueEqual(helper, blocksIn(helper, SMALL_SITE, Blocks.GLASS), plane,
                "Master Builder did not let the wand build out of the backpack");
        Assertions.valueEqual(helper, countCarried(player, Items.GLASS), 64 - plane.size(),
                "the Master Builder run did not pay for its plane out of the backpack slot");

        // --- 4. the same guard once more, this time inside findSpecificMaterial ---
        // The single block in the off hand carries the click and the centre of the plane; the ring
        // after it is the first thing that has to come out of slot 20, and by then the wand is no
        // longer in useOn but in its own tick.
        ItemStack strandedWand = tunedWand(ModItems.DIAMOND_BUILDING_WAND, 1, 0);
        int leftAfterStranding = buildOnOneBlockPlusTheBackpack(helper, player, strandedWand);
        Assertions.valueEqual(helper, blocksIn(helper, SMALL_SITE, Blocks.GLASS), Set.of(ANCHOR.above()),
                "a wand without Master Builder built more than the one block its off hand paid for, "
                        + "so findSpecificMaterial reaches into the backpack as well");
        Assertions.valueEqual(helper, leftAfterStranding, 64,
                "the stranded wand still spent glass out of the backpack slot");

        ItemStack builderMidPlane = tunedWand(ModItems.DIAMOND_BUILDING_WAND, 1, 0);
        builderMidPlane.enchant(enchantment(helper, ModEnchantments.MASTER_BUILDER), 1);
        int leftAfterRing = buildOnOneBlockPlusTheBackpack(helper, player, builderMidPlane);
        Assertions.valueEqual(helper, blocksIn(helper, SMALL_SITE, Blocks.GLASS), plane,
                "Master Builder did not let the wand finish its plane out of the backpack after the "
                        + "one block in its off hand had run out");
        Assertions.valueEqual(helper, leftAfterRing, 64 - (plane.size() - 1),
                "the ring after the centre block was not paid for out of the backpack slot");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // ARMED WITH NOTHING IN HAND
    // =====================================================================================

    /**
     * What a creative wand does when it was armed without any material at all.
     *
     * <p>{@code useOn} lets a creative click through even when the search found nothing, and writes
     * the raw id of {@code minecraft:air} into the stack as the block to build with. Everything
     * that happens afterwards is a branch no test in this tree had entered, because every other
     * wand run carries a stack of something.
     *
     * <ul>
     *   <li><b>It searches once more.</b> Material that appears between the click and the first
     *       tick is picked up: the wand looks the block up again whenever the remembered one is
     *       air. Without this leg, the branch could be replaced by a plain abort and nothing would
     *       notice.</li>
     *   <li><b>And gives up when that search fails too.</b> Measured as "one tick, no blocks", so
     *       a wand that instead placed air, or stayed armed forever, both fail.</li>
     *   <li><b>Unless Colour Palette is on.</b> The palette branch skips the air check entirely and
     *       falls back to plain stone for a creative builder - the one place in the item where a
     *       block is conjured out of nothing.</li>
     *   <li><b>And a wand that remembers a real block does <em>not</em> search again.</b> The
     *       mirror image of the first leg, and the only run in this tree that separates the two.
     *       See below.</li>
     * </ul>
     *
     * <p>The third leg also asserts that the player's inventory is still empty afterwards: the
     * fallback must not be a stone that was taken from somewhere.
     *
     * <p>The fourth leg is what makes the {@code BuildBlockRawId} the click writes observable at
     * all. Every other wand run in the repository holds one kind of block from the click to the
     * last placed position, so the block the click remembered and the block a fresh search would
     * return are the same and the key could be dropped without a single assertion moving. Here they
     * are deliberately pulled apart: the wand is armed on hotbar glass, and oak planks are put into
     * the off hand <em>after</em> the click, where {@code findFirstBuildingBlock} looks first. A
     * wand that builds what it remembered lays glass; one that looks the block up again every tick
     * lays planks. Both halves are asserted, because "the glass plane is complete" alone would also
     * be satisfied by a wand that placed planks somewhere on top of it. The player is creative here,
     * so nothing is consumed and neither stack can run out mid plane - the only thing that differs
     * between the two implementations is which block is chosen.
     *
     * <p><strong>What breaks this test:</strong> dropping the {@code targetBlock == Blocks.AIR}
     * re-search (leg one builds nothing), dropping the {@code Active = false} in its else branch
     * (leg two never ends and hits the loop guard), extending the air check to palette wands (leg
     * three builds nothing), replacing the {@code Blocks.STONE} fallback with the target block,
     * which is air here and would place nothing at all, or dropping the {@code BuildBlockRawId}
     * that {@code useOn} writes, or reading it back under another key - the tick would then fall
     * into the air branch for every wand and leg four would build out of the off hand.
     */
    public static void wandArmedWithoutMaterialSearchesAgainAndPaletteFallsBackToStone(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, true);

        // --- 1. material that turns up after the click is found ---
        resetSite(helper, SMALL_SITE);
        ItemStack lateWand = tunedWand(ModItems.DIAMOND_BUILDING_WAND, 1, 0);
        stock(player, lateWand);

        InteractionResult armed = useOn(helper, player, lateWand, Direction.UP, InteractionHand.MAIN_HAND);
        helper.assertTrue(armed == InteractionResult.CONSUME,
                "a creative click with an empty inventory did not arm the wand, it returned " + armed);
        helper.assertTrue(wandIsActive(lateWand), "the creative click did not switch the wand on");

        player.getInventory().setItem(1, new ItemStack(Items.GLASS, 64));
        driveUntilIdle(helper, player, lateWand, EquipmentSlot.MAINHAND);

        Assertions.valueEqual(helper, blocksIn(helper, SMALL_SITE, Blocks.GLASS),
                square(ANCHOR.above(), Direction.Axis.Y, 1),
                "the wand did not look for a building block again after it had been armed with none");

        // --- 2. and with nothing to find, it switches itself off on the first tick ---
        resetSite(helper, SMALL_SITE);
        ItemStack emptyWand = tunedWand(ModItems.DIAMOND_BUILDING_WAND, 1, 0);
        stock(player, emptyWand);
        useOn(helper, player, emptyWand, Direction.UP, InteractionHand.MAIN_HAND);

        int ticks = driveUntilIdle(helper, player, emptyWand, EquipmentSlot.MAINHAND);
        Assertions.valueEqual(helper, ticks, 1,
                "the wand needed " + ticks + " ticks to notice that it has nothing to build with");
        helper.assertTrue(blocksIn(helper, SMALL_SITE, Blocks.GLASS).isEmpty()
                        && blocksIn(helper, SMALL_SITE, Blocks.STONE).isEmpty(),
                "a creative wand with an empty inventory and no Colour Palette built something");

        // --- 3. Colour Palette conjures stone instead ---
        resetSite(helper, SMALL_SITE);
        ItemStack paletteWand = tunedWand(ModItems.DIAMOND_BUILDING_WAND, 1, 0);
        paletteWand.enchant(enchantment(helper, ModEnchantments.COLOR_PALETTE), 1);
        stock(player, paletteWand);
        useOn(helper, player, paletteWand, Direction.UP, InteractionHand.MAIN_HAND);
        driveUntilIdle(helper, player, paletteWand, EquipmentSlot.MAINHAND);

        Assertions.valueEqual(helper, blocksIn(helper, SMALL_SITE, Blocks.STONE),
                square(ANCHOR.above(), Direction.Axis.Y, 1),
                "a creative Colour Palette wand with an empty inventory did not fall back to stone");
        Assertions.valueEqual(helper, countCarried(player, Items.STONE), 0,
                "the stone fallback came out of the player's inventory instead of out of nothing");

        // --- 4. the other side of the same branch: a remembered block is not looked up again ---
        // The planks arrive after the click and sit in the off hand, which findFirstBuildingBlock
        // reaches before the hotbar. So they are what a wand that re-searched every tick would
        // build with, and the glass is what a wand that kept the id from its click builds with.
        resetSite(helper, SMALL_SITE);
        ItemStack rememberingWand = tunedWand(ModItems.DIAMOND_BUILDING_WAND, 1, 0);
        stock(player, rememberingWand, new ItemStack(Items.GLASS, 64));

        InteractionResult armedOnGlass =
                useOn(helper, player, rememberingWand, Direction.UP, InteractionHand.MAIN_HAND);
        helper.assertTrue(armedOnGlass == InteractionResult.CONSUME,
                "the wand refused a click although its hotbar held glass, it returned " + armedOnGlass);

        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.OAK_PLANKS, 64));
        driveUntilIdle(helper, player, rememberingWand, EquipmentSlot.MAINHAND);

        Assertions.valueEqual(helper, blocksIn(helper, SMALL_SITE, Blocks.GLASS),
                square(ANCHOR.above(), Direction.Axis.Y, 1),
                "the wand did not build the plane out of the glass it was armed with");
        helper.assertTrue(blocksIn(helper, SMALL_SITE, Blocks.OAK_PLANKS).isEmpty(),
                "the wand built out of the planks that turned up in the off hand after the click, so "
                        + "it looks the building block up again every tick instead of keeping the one "
                        + "the click wrote into the stack");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // THE ENCHANTABILITY TAG
    // =====================================================================================

    /**
     * Cover, Bridge and Linear name {@code simplebuilding:building_wand_enchantable} as both their
     * {@code primary_items} and their {@code supported_items}, so that one tag decides which wands
     * can carry a wand enchantment at all. Nothing read it.
     *
     * <p>Both directions are asserted, because either alone is worthless: the five wands the tag is
     * meant to list are in it and are accepted by all three enchantments, and two items that are
     * emphatically not wands - a diamond pickaxe and the diamond sledgehammer, which has an
     * enchantability tag of its very own - are refused by all three. Without the second half a tag
     * that had been widened to "every item" would pass.
     *
     * <p>{@code Enchantment#canEnchant} is vanilla's, and it is what an anvil asks before it lets
     * an enchanted book through; what is under test is the mod's tag file behind it.
     *
     * <p>The enderite wand is left out on purpose - it is missing from the tag today, and asserting
     * either state would cement one of them. See the known defect in the class javadoc.
     *
     * <p><strong>What breaks this test:</strong> dropping a wand from
     * {@code ModItemTagProvider}'s {@code BUILDING_WAND_ENCHANTABLE} builder, pointing one of the
     * three enchantments at a different item tag, or merging the wand tag into a wider one such as
     * {@code extra_inventory_items}.
     */
    public static void wandEnchantmentsOnlyStickToTheWandsInTheirItemTag(GameTestHelper helper) {
        List<Item> taggedWands = List.of(
                ModItems.COPPER_BUILDING_WAND,
                ModItems.IRON_BUILDING_WAND,
                ModItems.GOLD_BUILDING_WAND,
                ModItems.DIAMOND_BUILDING_WAND,
                ModItems.NETHERITE_BUILDING_WAND);
        List<ResourceKey<Enchantment>> wandEnchantments = List.of(
                ModEnchantments.COVER,
                ModEnchantments.BRIDGE,
                ModEnchantments.LINEAR);

        Set<Identifier> tagged = new LinkedHashSet<>();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(ModTags.Items.BUILDING_WAND_ENCHANTABLE)) {
            holder.unwrapKey().ifPresent(key -> tagged.add(key.identifier()));
        }

        for (Item wand : taggedWands) {
            Identifier id = BuiltInRegistries.ITEM.getKey(wand);
            helper.assertTrue(tagged.contains(id),
                    id + " has fallen out of #simplebuilding:building_wand_enchantable, which holds "
                            + tagged);
            ItemStack stack = new ItemStack(wand);
            for (ResourceKey<Enchantment> key : wandEnchantments) {
                helper.assertTrue(enchantment(helper, key).value().canEnchant(stack),
                        key.identifier() + " can no longer be put on " + id);
            }
        }

        for (Item control : List.<Item>of(Items.DIAMOND_PICKAXE, ModItems.DIAMOND_SLEDGEHAMMER)) {
            Identifier id = BuiltInRegistries.ITEM.getKey(control);
            helper.assertFalse(tagged.contains(id),
                    id + " is listed in #simplebuilding:building_wand_enchantable, so the tag no "
                            + "longer says anything about wands");
            ItemStack stack = new ItemStack(control);
            for (ResourceKey<Enchantment> key : wandEnchantments) {
                helper.assertFalse(enchantment(helper, key).value().canEnchant(stack),
                        key.identifier() + " can be put on " + id + ", so its supported_items tag "
                                + "accepts more than the building wands");
            }
        }

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // RECIPES
    // =====================================================================================

    /**
     * How the six wands are made: four shaped recipes that turn a tier core plus two sticks into a
     * wand, and two smithing transforms that walk the top two tiers upwards.
     *
     * <p>{@code DataIntegrityTests#modRecipesOnlyReferenceRegisteredItems} only proves that the
     * ingredient ids resolve, which leaves the pattern, the key mapping and the result free to
     * change. Everything here goes through the live {@code RecipeManager} with real stacks, so it
     * states what a player can actually craft.
     *
     * <p>The negative cases are the point:
     * <ul>
     *   <li>the same three items rearranged - core in the middle, sticks in two corners - must
     *       craft nothing, or the diagonal pattern would be decoration. It is deliberately
     *       <em>not</em> the mirrored pattern: vanilla's shaped matching accepts a mirrored grid,
     *       so that arrangement would be a false alarm;</li>
     *   <li>the enderite transform must refuse a diamond wand as its base, so the netherite tier
     *       cannot be skipped;</li>
     *   <li>and it must refuse a netherite ingot as its addition, so the template and the addition
     *       cannot be confused.</li>
     * </ul>
     *
     * <p><strong>What breaks this test:</strong> a changed pattern or key in one of the four wand
     * recipe files, a tier crafting from the wrong core, a swapped base/addition/template in either
     * smithing file, a wrong result id, or a recipe file that stops loading at all.
     */
    public static void wandRecipesCraftTheLowerTiersAndForgeTheUpperOnes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeManager recipes = level.getServer().getRecipeManager();

        assertWandCraft(helper, level, recipes, ModItems.COPPER_CORE, ModItems.COPPER_BUILDING_WAND);
        assertWandCraft(helper, level, recipes, ModItems.IRON_CORE, ModItems.IRON_BUILDING_WAND);
        assertWandCraft(helper, level, recipes, ModItems.GOLD_CORE, ModItems.GOLD_BUILDING_WAND);
        assertWandCraft(helper, level, recipes, ModItems.DIAMOND_CORE, ModItems.DIAMOND_BUILDING_WAND);

        // The same three items, but the core in the centre instead of a corner. Neither this grid
        // nor its mirror image is the shipped pattern, so a recipe that really checks its shape
        // matches nothing here.
        ItemStack stick = new ItemStack(Items.STICK);
        CraftingInput scrambled = CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, ItemStack.EMPTY, stick,
                ItemStack.EMPTY, new ItemStack(ModItems.COPPER_CORE), ItemStack.EMPTY,
                stick, ItemStack.EMPTY, ItemStack.EMPTY));
        helper.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, scrambled, level).isEmpty(),
                "a copper core between two sticks in the wrong places still crafted something; the "
                        + "wand pattern is not being checked");

        // --- diamond -> netherite ---
        SmithingRecipeInput toNetherite = new SmithingRecipeInput(
                new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new ItemStack(ModItems.DIAMOND_BUILDING_WAND),
                new ItemStack(Items.NETHERITE_INGOT));
        assertSmithingResult(helper, level, recipes, toNetherite, ModItems.NETHERITE_BUILDING_WAND,
                "diamond wand + netherite ingot on the netherite template");

        // --- netherite -> enderite ---
        SmithingRecipeInput toEnderite = new SmithingRecipeInput(
                new ItemStack(ModItems.ENDERITE_UPGRADE_TEMPLATE),
                new ItemStack(ModItems.NETHERITE_BUILDING_WAND),
                new ItemStack(ModItems.ENDERITE_INGOT));
        assertSmithingResult(helper, level, recipes, toEnderite, ModItems.ENDERITE_BUILDING_WAND,
                "netherite wand + enderite ingot on the enderite template");

        SmithingRecipeInput skippedTier = new SmithingRecipeInput(
                new ItemStack(ModItems.ENDERITE_UPGRADE_TEMPLATE),
                new ItemStack(ModItems.DIAMOND_BUILDING_WAND),
                new ItemStack(ModItems.ENDERITE_INGOT));
        helper.assertTrue(recipes.getRecipeFor(RecipeType.SMITHING, skippedTier, level).isEmpty(),
                "the enderite upgrade accepted a diamond wand as its base, so the netherite tier "
                        + "can be skipped");

        SmithingRecipeInput wrongAddition = new SmithingRecipeInput(
                new ItemStack(ModItems.ENDERITE_UPGRADE_TEMPLATE),
                new ItemStack(ModItems.NETHERITE_BUILDING_WAND),
                new ItemStack(Items.NETHERITE_INGOT));
        helper.assertTrue(recipes.getRecipeFor(RecipeType.SMITHING, wrongAddition, level).isEmpty(),
                "the enderite wand upgrade accepted a netherite ingot as its addition");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // CHEST LOOT
    // =====================================================================================

    /**
     * The two chests a wand can come out of: the woodland mansion hands out the iron wand, the end
     * city treasure the diamond one, and the diamond one is the enchanted entry.
     *
     * <p>{@code ConfigOptionTests#lootTableChangesStopWhenTheOptionIsSwitchedOff} only counts the
     * pools the mod offers, which cannot tell a pool of enchanted books from one with a wand in it,
     * and it does not look at the woodland mansion at all. This serialises the pools the mod hands
     * over through {@code LootPool.CODEC} and looks for the wand entry inside them - the entry list
     * of a built pool is private, but its serialised form is exact and needs no dice.
     *
     * <p>Deliberately not asserted: the weights and the roll counts. They are balancing numbers,
     * and pinning them would turn every balance pass red without catching a wiring bug.
     *
     * <p>A vanilla table the mod never edits is recorded with the same helper and has to come back
     * without either wand, so the positive answers cannot come from a recorder that finds a wand
     * everywhere.
     *
     * <p><strong>What breaks this test:</strong> dropping either wand entry, moving one of the two
     * pools behind another table key, swapping the two tiers, or losing the random enchantment on
     * the end city entry.
     */
    public static void ironWandDropsInTheMansionAndTheDiamondWandInTheEndCity(GameTestHelper helper) {
        helper.assertTrue(Simplebuilding.getConfig().worldGen.enableLootTableChanges,
                "worldGen.enableLootTableChanges is switched off for this run, so the mod adds no "
                        + "pools at all and nothing below could be found either way");

        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        String ironId = BuiltInRegistries.ITEM.getKey(ModItems.IRON_BUILDING_WAND).toString();
        String diamondId = BuiltInRegistries.ITEM.getKey(ModItems.DIAMOND_BUILDING_WAND).toString();

        JsonObject mansion = lootEntry(helper, registries, BuiltInLootTables.WOODLAND_MANSION, ironId);
        helper.assertTrue(mansion != null,
                "the iron building wand is no longer in the woodland mansion loot");

        JsonObject endCity = lootEntry(helper, registries, BuiltInLootTables.END_CITY_TREASURE, diamondId);
        helper.assertTrue(endCity != null,
                "the diamond building wand is no longer in the end city treasure loot");
        helper.assertTrue(endCity.has("functions"),
                "the end city wand lost its loot functions; it is the entry that is handed out "
                        + "randomly enchanted, entry is " + endCity);

        // The two tiers are not interchangeable: each belongs to exactly one of the two tables.
        helper.assertTrue(lootEntry(helper, registries, BuiltInLootTables.WOODLAND_MANSION, diamondId) == null,
                "the diamond building wand turned up in the woodland mansion as well");
        helper.assertTrue(lootEntry(helper, registries, BuiltInLootTables.END_CITY_TREASURE, ironId) == null,
                "the iron building wand turned up in the end city treasure as well");

        // Control: a table the mod does not touch must come back without any wand at all.
        helper.assertTrue(lootEntry(helper, registries, BuiltInLootTables.SPAWN_BONUS_CHEST, ironId) == null
                        && lootEntry(helper, registries, BuiltInLootTables.SPAWN_BONUS_CHEST, diamondId) == null,
                "the recorder finds a building wand even in a loot table the mod never edits, so the "
                        + "answers above prove nothing");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // HELPERS - THE PLAYER
    // =====================================================================================

    /**
     * A mock player in the room, with {@code instabuild} set explicitly.
     *
     * <p>{@code GameTestHelper}'s in level mock hard-overrides {@code gameMode()} to
     * {@code CREATIVE}, so {@code isCreative()} cannot be moved - see the class javadoc of
     * {@code ConsumptionAndDurabilityTests}. Every branch the wand reads is on
     * {@code Abilities.instabuild}, which is a plain public field, and that is what is set here.
     * The assertion afterwards fails loudly if a later edit drops the line, instead of letting a
     * whole test measure nothing.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, boolean instabuild) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(3.5, 1.0, 6.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.getAbilities().instabuild = instabuild;
        Assertions.valueEqual(helper, player.getAbilities().instabuild, instabuild,
                "the mock player's instabuild flag could not be set, so every branch this test is "
                        + "about would be taken the other way round");
        // Hand the player back no matter how the test ends; a leaked mock player keeps the player
        // list non-empty and the gametest server then stalls on shutdown.
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /** Wand in the selected hotbar slot, supplies behind it, both hands otherwise empty. */
    private static void stock(ServerPlayer player, ItemStack wand, ItemStack... supplies) {
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, wand);
        for (int i = 0; i < supplies.length; i++) {
            player.getInventory().setItem(i + 1, supplies[i]);
        }
    }

    /**
     * Wand in the selected hotbar slot and the supply in slot 20 - inside the backpack, past the
     * hotbar the plain search is allowed to look at.
     */
    private static void stockBackpackOnly(ServerPlayer player, ItemStack wand, ItemStack supply) {
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, wand);
        player.getInventory().setItem(20, supply);
    }

    /** Everything the player carries, hands included, so a moved stack cannot hide the count. */
    private static int countCarried(ServerPlayer player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        ItemStack offHand = player.getOffhandItem();
        if (offHand.is(item)) {
            total += offHand.getCount();
        }
        return total;
    }

    // =====================================================================================
    // HELPERS - DRIVING THE WAND
    // =====================================================================================

    /** A wand with both of its settings written, so no test depends on an unset default. */
    private static ItemStack tunedWand(Item wandItem, int radius, int axisMode) {
        ItemStack wand = new ItemStack(wandItem);
        CompoundTag settings = customData(wand);
        settings.putInt("SettingsRadius", radius);
        settings.putInt("SettingsAxis", axisMode);
        wand.set(DataComponents.CUSTOM_DATA, CustomData.of(settings));
        return wand;
    }

    /**
     * Right clicks the centre of one face of {@link #ANCHOR} with the given hand. The hit location
     * is the middle of that face, which is where a player aiming at a wall actually clicks.
     */
    private static InteractionResult useOn(GameTestHelper helper, ServerPlayer player, ItemStack stack,
                                           Direction face, InteractionHand hand) {
        player.setItemInHand(hand, stack);
        BlockPos pos = helper.absolutePos(ANCHOR);
        Vec3 hit = new Vec3(
                pos.getX() + 0.5 + face.getStepX() * 0.5,
                pos.getY() + 0.5 + face.getStepY() * 0.5,
                pos.getZ() + 0.5 + face.getStepZ() * 0.5);
        return stack.getItem().useOn(new UseOnContext(player, hand, new BlockHitResult(hit, face, pos, false)));
    }

    /**
     * One call of the wand's own item tick. The gametest server never pumps a mock player's
     * connection, so driving the hook directly is both closer to what is under test and
     * deterministic - a whole plane finishes inside one test tick.
     */
    private static void tickWand(GameTestHelper helper, ServerPlayer player, ItemStack wand, EquipmentSlot slot) {
        wand.getItem().inventoryTick(wand, helper.getLevel(), player, slot);
    }

    /**
     * Ticks the wand in the given slot until it switches itself off and returns how many ticks
     * that took. Bounded, so a wand that never finishes fails the test instead of hanging the run.
     *
     * <p>Stopping on the wand's own {@code Active} flag rather than on "the plane appeared" is what
     * lets the same driver measure a run that deliberately builds nothing; every caller therefore
     * asserts the resulting shape itself.
     */
    private static int driveUntilIdle(GameTestHelper helper, ServerPlayer player, ItemStack wand,
                                      EquipmentSlot slot) {
        helper.assertTrue(wandIsActive(wand),
                "the wand was not armed, so there is nothing to drive");
        int ticks = 0;
        while (wandIsActive(wand) && ticks < WAND_TICK_CAP) {
            tickWand(helper, player, wand, slot);
            ticks++;
        }
        helper.assertTrue(ticks < WAND_TICK_CAP,
                "the wand was still building after " + WAND_TICK_CAP + " inventory ticks");
        return ticks;
    }

    /**
     * Arms a fresh wand on the top face, lets it place the centre of its plane, and then hands it
     * one single tick in a slot that is not a hand. It has to switch itself off there, and it must
     * not pick the build back up when the main hand ticks it again - a wand that only paused would
     * finish the plane in the player's next five ticks.
     *
     * @param away the slot the interrupting tick is driven with; {@code null} is what
     *             {@code Inventory#tick} passes for every unselected inventory slot
     * @param what how that slot reads in a failure message
     */
    private static void assertLeavingTheHandsStopsTheBuild(GameTestHelper helper, ServerPlayer player,
                                                           EquipmentSlot away, String what) {
        resetSite(helper, SMALL_SITE);
        ItemStack wand = tunedWand(ModItems.DIAMOND_BUILDING_WAND, 1, 0);
        stock(player, wand, new ItemStack(Items.GLASS, 64));

        InteractionResult armed = useOn(helper, player, wand, Direction.UP, InteractionHand.MAIN_HAND);
        helper.assertTrue(armed == InteractionResult.CONSUME,
                "the main hand click did not arm the wand, it returned " + armed);

        tickWand(helper, player, wand, EquipmentSlot.MAINHAND);
        Set<BlockPos> centreOnly = Set.of(ANCHOR.above());
        Assertions.valueEqual(helper, blocksIn(helper, SMALL_SITE, Blocks.GLASS), centreOnly,
                "the first tick did not place exactly the centre of the plane, so the interruption "
                        + "below would not happen in the middle of a build");
        helper.assertTrue(wandIsActive(wand),
                "the wand finished the whole plane in one tick; there is nothing left to interrupt");

        tickWand(helper, player, wand, away);
        helper.assertFalse(wandIsActive(wand),
                "the wand kept its build state while it was " + what + " instead of held");

        for (int tick = 0; tick < 5; tick++) {
            tickWand(helper, player, wand, EquipmentSlot.MAINHAND);
        }
        Assertions.valueEqual(helper, blocksIn(helper, SMALL_SITE, Blocks.GLASS), centreOnly,
                "the wand picked its build back up after a tick " + what + "; leaving the hands has "
                        + "to cancel the build, not pause it");
    }

    /**
     * Arms the wand on a single glass block in the off hand, with the rest of the glass in backpack
     * slot 20, and drives it to a stop. That one block is spent on the centre of the plane, so
     * every position after it is a question to {@code findSpecificMaterial} alone - the click has
     * long been answered by then.
     *
     * @return the glass left in slot 20
     */
    private static int buildOnOneBlockPlusTheBackpack(GameTestHelper helper, ServerPlayer player,
                                                      ItemStack wand) {
        resetSite(helper, SMALL_SITE);
        stockBackpackOnly(player, wand, new ItemStack(Items.GLASS, 64));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLASS, 1));

        InteractionResult armed = useOn(helper, player, wand, Direction.UP, InteractionHand.MAIN_HAND);
        helper.assertTrue(armed == InteractionResult.CONSUME,
                "the wand refused a click although its off hand held a glass block, it returned " + armed);
        driveUntilIdle(helper, player, wand, EquipmentSlot.MAINHAND);
        helper.assertTrue(player.getOffhandItem().isEmpty(),
                "the one glass block in the off hand was not spent, so the run never got to the "
                        + "point where only the backpack is left: " + player.getOffhandItem());
        return player.getInventory().getItem(20).getCount();
    }

    /**
     * Clears the working site, arms the wand on one face of the anchor and drives it to a stop.
     * Returns every glass block inside the site, which is the finished plane.
     */
    private static Set<BlockPos> buildPlane(GameTestHelper helper, ServerPlayer player, ItemStack wand,
                                            Direction face, int reach) {
        resetSite(helper, reach);
        stock(player, wand, new ItemStack(Items.GLASS, 64));

        InteractionResult armed = useOn(helper, player, wand, face, InteractionHand.MAIN_HAND);
        helper.assertTrue(armed == InteractionResult.CONSUME,
                "the wand did not arm itself on the " + face + " face, it returned " + armed);
        driveUntilIdle(helper, player, wand, EquipmentSlot.MAINHAND);
        return blocksIn(helper, reach, Blocks.GLASS);
    }

    private static boolean wandIsActive(ItemStack wand) {
        return customData(wand).getBooleanOr("Active", false);
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    // =====================================================================================
    // HELPERS - THE WORKING SITE
    // =====================================================================================

    /**
     * Empties the cube around the anchor and puts an unreplaceable block back in the middle of it.
     *
     * <p>Obsidian rather than stone, and glass rather than stone as the building material: the
     * clicked block must not be mistaken for a placed one when the plane is read back, and a run
     * that overwrites the block it was clicked on has to be visible. The emptiness check afterwards
     * is what keeps two runs in the same room from counting each other's blocks.
     */
    private static void resetSite(GameTestHelper helper, int reach) {
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dy = -reach; dy <= reach; dy++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    helper.setBlock(ANCHOR.offset(dx, dy, dz), Blocks.AIR);
                }
            }
        }
        helper.setBlock(ANCHOR, Blocks.OBSIDIAN);
        helper.assertTrue(blocksIn(helper, reach, Blocks.GLASS).isEmpty()
                        && blocksIn(helper, reach, Blocks.STONE).isEmpty(),
                "the working site still holds building material after it was cleared, so this run "
                        + "would count the previous one's blocks");
    }

    /** Every position in the working site that holds {@code block}. */
    private static Set<BlockPos> blocksIn(GameTestHelper helper, int reach, Block block) {
        Set<BlockPos> found = new LinkedHashSet<>();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dy = -reach; dy <= reach; dy++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    BlockPos pos = ANCHOR.offset(dx, dy, dz);
                    if (helper.getBlockState(pos).is(block)) {
                        found.add(pos);
                    }
                }
            }
        }
        return found;
    }

    /** The filled square of side {@code 2 * radius + 1} around {@code centre}, normal to {@code axis}. */
    private static Set<BlockPos> square(BlockPos centre, Direction.Axis axis, int radius) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        for (int u = -radius; u <= radius; u++) {
            for (int v = -radius; v <= radius; v++) {
                positions.add(switch (axis) {
                    case X -> centre.offset(0, u, v);
                    case Y -> centre.offset(u, 0, v);
                    case Z -> centre.offset(u, v, 0);
                });
            }
        }
        return positions;
    }

    /** The distinct values one coordinate takes over a set of positions. */
    private static Set<Integer> coordinates(Set<BlockPos> positions, ToIntFunction<BlockPos> axis) {
        Set<Integer> values = new LinkedHashSet<>();
        for (BlockPos pos : positions) {
            values.add(axis.applyAsInt(pos));
        }
        return values;
    }

    /** One coordinate of a position, named by the axis it belongs to. */
    private static int along(BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }

    /** One corner of the bounding box of {@code positions}: {@code Math::min} or {@code Math::max}. */
    private static BlockPos corner(Set<BlockPos> positions, IntBinaryOperator pick) {
        Iterator<BlockPos> walk = positions.iterator();
        BlockPos corner = walk.next();
        while (walk.hasNext()) {
            BlockPos pos = walk.next();
            corner = new BlockPos(
                    pick.applyAsInt(corner.getX(), pos.getX()),
                    pick.applyAsInt(corner.getY(), pos.getY()),
                    pick.applyAsInt(corner.getZ(), pos.getZ()));
        }
        return corner;
    }

    /**
     * What a forced axis run has to look like: one plane normal to {@code normal}, no more than a
     * block away from the clicked block along that axis, three cells wide on each of the two other
     * axes, and with every cell of that 3x3 filled except at most the clicked block itself, which
     * is obsidian and cannot be replaced.
     *
     * <p>The box is read back out of the placed positions instead of being computed from
     * {@link #ANCHOR}, because the wand builds a forced plane off centre today - see the known
     * defect in the class javadoc. Both that plane and a centred one satisfy every line below,
     * while a line, a ring, a plane with a hole, a wider or narrower square and a plane built
     * elsewhere in the room all fail. Since every placed position lies on the one level and between
     * the two corners, "the box is 3x3 and misses at most the anchor" is the whole shape: the set
     * can only be those nine cells or those nine minus the anchor.
     */
    private static void assertForcedPlane(GameTestHelper helper, Set<BlockPos> placed,
                                          Direction.Axis normal, String what) {
        helper.assertFalse(placed.isEmpty(), what + " placed nothing at all");

        Set<Integer> levels = coordinates(placed, pos -> along(pos, normal));
        helper.assertTrue(levels.size() == 1,
                what + " did not build a single plane normal to the " + normal + " axis; its blocks "
                        + "sit on the " + normal + " levels " + levels + ": " + placed);
        int level = levels.iterator().next();
        int anchorLevel = along(ANCHOR, normal);
        helper.assertTrue(Math.abs(level - anchorLevel) <= 1,
                what + " built its plane at " + normal + " = " + level + ", " + Math.abs(level - anchorLevel)
                        + " blocks off the clicked block at " + normal + " = " + anchorLevel + "; the "
                        + "wand builds against the block it was clicked on, not elsewhere in the room");

        BlockPos min = corner(placed, Math::min);
        BlockPos max = corner(placed, Math::max);
        for (Direction.Axis free : Direction.Axis.values()) {
            if (free == normal) {
                continue;
            }
            int span = along(max, free) - along(min, free) + 1;
            helper.assertTrue(span == 3,
                    what + " is " + span + " blocks wide along the " + free + " axis instead of 3, so "
                            + "it is not the filled square the radius 1 setting asks for: " + placed);
        }

        BlockPos centre = new BlockPos(
                (min.getX() + max.getX()) / 2,
                (min.getY() + max.getY()) / 2,
                (min.getZ() + max.getZ()) / 2);
        Set<BlockPos> missing = new LinkedHashSet<>(square(centre, normal, 1));
        missing.removeAll(placed);
        helper.assertTrue(missing.isEmpty() || missing.equals(Set.of(ANCHOR)),
                what + " left " + missing + " empty inside its own 3x3 footprint; the only cell the "
                        + "wand may skip is the clicked block itself, which cannot be replaced");
    }

    // =====================================================================================
    // HELPERS - DATA
    // =====================================================================================

    /** Crafts one wand tier from its core and two sticks in the shipped diagonal pattern. */
    private static void assertWandCraft(GameTestHelper helper, ServerLevel level, RecipeManager recipes,
                                        Item core, Item wand) {
        ItemStack stick = new ItemStack(Items.STICK);
        // "  C" / " S " / "S  "
        CraftingInput input = CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(core),
                ItemStack.EMPTY, stick, ItemStack.EMPTY,
                stick, ItemStack.EMPTY, ItemStack.EMPTY));

        Optional<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> crafted =
                recipes.getRecipeFor(RecipeType.CRAFTING, input, level);
        helper.assertTrue(crafted.isPresent(),
                BuiltInRegistries.ITEM.getKey(core) + " with two sticks on the wand pattern crafts "
                        + "nothing at all");

        // assemble takes the registries alongside the input on this line; 26.2 dropped that parameter.
        ItemStack result = crafted.get().value().assemble(input, level.registryAccess());
        helper.assertTrue(result.is(wand),
                "the wand pattern with " + BuiltInRegistries.ITEM.getKey(core) + " crafts " + result
                        + " instead of " + BuiltInRegistries.ITEM.getKey(wand));
        Assertions.valueEqual(helper, result.getCount(), 1,
                BuiltInRegistries.ITEM.getKey(wand) + ": wands produced per craft");
    }

    private static void assertSmithingResult(GameTestHelper helper, ServerLevel level, RecipeManager recipes,
                                             SmithingRecipeInput input, Item expected, String what) {
        Optional<RecipeHolder<net.minecraft.world.item.crafting.SmithingRecipe>> holder =
                recipes.getRecipeFor(RecipeType.SMITHING, input, level);
        helper.assertTrue(holder.isPresent(), what + " matches no smithing recipe at all");

        ItemStack forged = holder.get().value().assemble(input, level.registryAccess());
        helper.assertTrue(forged.is(expected),
                what + " forges " + forged + " instead of " + BuiltInRegistries.ITEM.getKey(expected));
        Assertions.valueEqual(helper, forged.getCount(), 1, what + ": items produced per upgrade");
    }

    /**
     * Runs the mod's loot hook for one table, serialises every pool it hands over and returns the
     * entry for {@code itemId} out of the first pool that holds one, or {@code null}. The entry
     * list of a built {@code LootPool} is private, but its codec output is exact - and unlike
     * rolling the pool it needs no dice and no statistics.
     *
     * <p>A loot item entry carries its item id under {@code "name"} and its loot functions under
     * {@code "functions"}; both spellings come from vanilla's own entry codecs.
     */
    private static JsonObject lootEntry(GameTestHelper helper, HolderLookup.Provider registries,
                                        ResourceKey<LootTable> table, String itemId) {
        PoolCollector collector = new PoolCollector();
        ModLootTableModifications.apply(table, collector, registries);

        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        for (LootPool pool : collector.pools) {
            JsonElement encoded = LootPool.CODEC.encodeStart(ops, pool)
                    .getOrThrow(message -> helper.assertionException(
                            "a loot pool the mod adds to " + table.identifier()
                                    + " cannot be serialised: " + message));
            if (!encoded.isJsonObject()) {
                continue;
            }
            JsonElement entries = encoded.getAsJsonObject().get("entries");
            if (entries == null || !entries.isJsonArray()) {
                continue;
            }
            for (JsonElement entry : entries.getAsJsonArray()) {
                if (!entry.isJsonObject()) {
                    continue;
                }
                JsonObject object = entry.getAsJsonObject();
                JsonElement name = object.get("name");
                if (name != null && name.isJsonPrimitive() && itemId.equals(name.getAsString())) {
                    return object;
                }
            }
        }
        return null;
    }

    /** Collects the pools the mod offers for one table, from both editor paths. */
    private static final class PoolCollector implements ModLootTableModifications.Editor {
        private final List<LootPool> pools = new ArrayList<>();

        @Override
        public void addPool(LootPool.Builder pool) {
            this.pools.add(pool.build());
        }

        @Override
        public void addBuiltPool(LootPool pool) {
            this.pools.add(pool);
        }
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }
}
