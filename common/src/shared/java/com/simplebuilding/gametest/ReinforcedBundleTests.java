package com.simplebuilding.gametest;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The reinforced bundle itself: what it takes in, in what order it keeps it, what comes back out,
 * and what it builds with when Master Builder is on it.
 *
 * <p>{@code ReinforcedBundleItem} is a full re-implementation of vanilla's bundle - its own
 * insert, its own merge and sort, its own removal, its own capacity table - and almost none of
 * that was reachable from the suite. What existed before covered the neighbours rather than the
 * item: {@link ItemBehaviourTests#bundleCapacityGrowsWithTierAndEnchantments} only asserts that
 * the three capacity sources are ordered (and counts a partly accepted 64-stack as a full 64,
 * so its numbers are upper bounds, not capacities);
 * {@link ConfigOptionTests#bundleClickInversionFollowsTheConfiguredOption} drives both click
 * methods but is about the config switch; {@link QuiverTests} exercises the subclass, which
 * overrides {@code getMaxCapacity} and {@code getMaxCapacityForVisuals} with copies of its own, so
 * every enchantment branch it pins belongs to {@code QuiverItem} and not to the class here. The
 * two places where QuiverTests does touch this class - a right click that empties a bundle of
 * arrows, and one Master Builder placement as the control for the quiver's refusal - are noted at
 * the tests below that widen them.
 *
 * <h2>Which player</h2>
 *
 * <p>{@link #mockPlayer} is the in-level mock with {@code instabuild} switched off, the same
 * choice {@link QuiverTests} makes and for the same reason: the master builder branch in
 * {@code useOn} only pays for its block when that flag is down, and it is a plain public field, so
 * the mock's hard-wired {@code CREATIVE} game mode does not stand in the way. No bundle code reads
 * {@code isCreative()}. The player keeps its connection, which the insert and remove sounds go
 * through.
 *
 * <h2>Known defects</h2>
 *
 * <p><b>The Drawer multiplier is twice what its own comment says</b> - {@code (16 + level) / 8}
 * against a documented {@code (8 + level) / 8}; see the known defect in {@link QuiverTests} for
 * the full description. Rather than pin a number that a fix would have to rewrite,
 * {@link #capacityFollowsTierAndEnchantmentsAndMatchesTheWikiExport} asserts the <em>slope</em> of
 * the multiplier - one eighth of the base capacity per level - which is what both readings of the
 * formula agree on and what a change to the divisor breaks.
 *
 * <p><b>The merge comment describes the opposite of what the merge does.</b>
 * {@code insertItemIntoBundle} says it wants the part-filled stack on top ("LIFO"), then builds
 * {@code itemsToAddBack} full-stacks-first and inserts that block at index 0, so the full stack
 * ends up above the remainder. {@link #insertionMergesEqualStacksAndPushesThemToTheTop} therefore
 * asserts the part the code and the comment agree on - one merged block of stacks at the top, no
 * fragments, everything else pushed down - and deliberately does not pin which of the two merged
 * stacks is first.
 *
 * <p><b>{@code addToBundleList} is dead.</b> Nothing calls it (the sorting rewrite above replaced
 * it), and it encodes the <em>other</em> stacking rule - top up the topmost stack, push the rest
 * in front - so a maintainer reading it will get the live behaviour wrong.
 *
 * <p><b>A nested bundle is charged on the way in and free afterwards.</b> The admission check
 * weighs an item as {@code 1 / maxStackSize}, i.e. a whole vanilla stack for a bundle
 * ({@code stacksTo(1)}), but the occupancy it compares against on the next insert is vanilla's
 * {@code BundleContents#weight}, which charges a bundle inside a bundle only
 * {@code BUNDLE_IN_BUNDLE_WEIGHT} (1/16) plus its contents. The two disagree, so a bundle takes
 * far more bundles than its capacity suggests - a reinforced bundle admits one when empty and
 * keeps admitting more as long as sixteenths are left. {@link
 * #insertionTurnsAwayWhatCannotGoIntoContainerItems} stores exactly one nested bundle and asserts
 * nothing about how many more would fit, so it neither cements the hole nor trips over it.
 *
 * <h2>Not covered, and why</h2>
 * <ul>
 *   <li><b>{@code canFitInsideContainerItems() == true}.</b> In MC 26.2 that is already the
 *       default: {@code Item} returns {@code true} and {@code BundleItem} does not override it -
 *       only {@code BlockItem} does, for shulker boxes. Deleting the override changes no outcome,
 *       so an assertion on it could never go red. The half that <em>is</em> mod code - honouring
 *       that answer at the three entry points - is
 *       {@link #insertionTurnsAwayWhatCannotGoIntoContainerItems}.</li>
 *   <li><b>"Every insert clears the selection".</b> True, but carried by vanilla: every write in
 *       this class goes through {@code new BundleContents(list)}, and that constructor hard-codes
 *       {@code NO_SELECTED_ITEM_INDEX}. The {@code BundleItem.toggleSelectedItem(bundle, -1)} line
 *       in {@code insertItemIntoBundle} is dead next to it, and a test on the cleared selection
 *       would stay green with that line deleted.</li>
 *   <li><b>The {@code selectedIndex >= size} fallbacks</b> in {@code removeSelectedOrFirstItem},
 *       {@code use} and {@code useOn}. A stale index cannot be produced: vanilla's
 *       {@code BundleContents.Mutable#toggleSelectedItem} maps every out-of-bounds index to -1 on
 *       the way in, and every write of the component resets the selection anyway (see above). The
 *       branches are defensive only.</li>
 *   <li><b>The click inversion</b> ({@code getInsertClick} / {@code getRemoveClick}). Driven in
 *       both settings, through both click methods and in both directions by
 *       {@link ConfigOptionTests#bundleClickInversionFollowsTheConfiguredOption}. The tests here
 *       pin the option to "off" so their clicks mean what their names say.</li>
 *   <li><b>Everything the player sees</b>: the tooltip's grid and submenu, the bar being drawn,
 *       the insert and remove sounds. Client side, or swallowed by the mock player's connection.
 *       {@code getBarWidth}, {@code getBarColor} and {@code getTooltipImage} are plain arithmetic
 *       on the stack and are checked here; only their rendering is out of reach.</li>
 *   <li><b>{@code isBarVisible}.</b> The override answers "the contents are not empty", and
 *       so does the {@code BundleItem} method it replaces ("the weight is above zero") - for
 *       every stack the game can build the two agree, so deleting the override changes no
 *       answer and an assertion on it would be green either way. {@code getBarWidth} is a
 *       different matter and is checked: on an empty bundle the override says 0 where
 *       vanilla's formula says 1.</li>
 *   <li><b>{@code canAutoPickup}</b> and the item entity that calls it. That is the Funnel half of
 *       the bundle and lives in {@code ItemEntityMixin}; it belongs to the wiring tests, not
 *       here.</li>
 * </ul>
 */
public final class ReinforcedBundleTests {

    private ReinforcedBundleTests() {
    }

    /** Full bar width vanilla draws, i.e. what a container filled to its brim has to report. */
    private static final int FULL_BAR = 13;

    /** Bar width of a container filled to exactly half: {@code Math.round(0.5f * 13)}. */
    private static final int HALF_BAR = 7;

    /** Highest Drawer level the enchantment data allows. */
    private static final int DRAWER_MAX_LEVEL = 8;

    /** Kinds a Drawer bundle may hold at once - {@code DRAWER_MAX_TYPES} in the item. */
    private static final int DRAWER_KINDS = 5;

    /** Six block items, so that the sixth kind meets a full drawer. All stack to 64. */
    private static final Item[] SIX_KINDS = {
            Items.STONE, Items.DIRT, Items.OAK_PLANKS, Items.COBBLESTONE, Items.SAND, Items.GRAVEL,
    };

    /**
     * One kind in an amount the merge has to split into two entries - more than a vanilla stack,
     * and not a multiple of one. That is what pulls the entry count ahead of the kind count in
     * {@link #drawerCapsTheBundleAtFiveKinds}.
     */
    private static final int SPLIT_STOCK = 80;

    /** Placements the Color Palette loop makes. */
    private static final int PALETTE_ROUNDS = 40;

    /**
     * How much of each kind the Color Palette bundles are stocked with - comfortably more than
     * {@link #PALETTE_ROUNDS}, so that no entry can run empty mid-loop. An entry that emptied would
     * move the top of the pile on and hand out a second kind for a reason that has nothing to do
     * with the palette, and the loop below would then also pass with the palette branch deleted.
     */
    private static final int PALETTE_STOCK = 64;

    // =====================================================================================
    // WHAT GOES IN
    // =====================================================================================

    /**
     * How much of an offered stack the bundle takes. Three answers in one, because they all come
     * out of the same six lines of {@code insertItemIntoBundle}: everything while there is room,
     * exactly the remainder when the room runs out, nothing at all afterwards - and "room" is
     * counted in vanilla stacks, not in items, so an item that stacks to 16 eats four times the
     * space per piece.
     *
     * <p>Nothing here is copied out of the item. The capacity is measured first on a bundle of its
     * own, and the expected leftover is derived from that measurement, so a balance change to the
     * capacity table moves this test's numbers with it instead of turning it red for the wrong
     * reason. The setup guard makes sure the derivation still makes sense: the arithmetic below
     * assumes one 64-stack fits and two do not.
     *
     * <p>What breaks it: dropping the {@code remainingSpace} clamp, so a 64-stack is swallowed
     * whole and the surplus vanishes; taking the weight from a constant 1/64 instead of the item's
     * own {@code getMaxStackSize}, which would let a bundle hold as many ender pearls as stone; or
     * answering {@code true} from {@code tryInsertStackFromWorld} without storing anything, which
     * would delete the caller's stack.
     */
    public static void insertionStopsAtTheBrimAndWeighsByStackSize(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        int capacity = fillWith(helper, player, new ItemStack(ModItems.REINFORCED_BUNDLE), Items.STONE);
        helper.assertTrue(capacity > 64 && capacity < 128,
                "setup guard: a reinforced bundle takes " + capacity + " stone, so it no longer holds "
                        + "between one and two vanilla stacks - the leftovers this test derives from that "
                        + "no longer say anything");

        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ReinforcedBundleItem item = bundleItem(bundle);

        // --- first stack: room for all of it ---
        ItemStack first = new ItemStack(Items.STONE, 64);
        helper.assertTrue(item.tryInsertStackFromWorld(bundle, first, player),
                "an empty reinforced bundle refused the first 64 stone");
        helper.assertValueEqual(first.getCount(), 0, "stone left over from the first stack");
        helper.assertValueEqual(totalIn(bundle), 64, "stone inside the bundle after the first stack");

        // --- second stack: only the remainder is taken, and the rest stays in the player's hand ---
        ItemStack second = new ItemStack(Items.STONE, 64);
        helper.assertTrue(item.tryInsertStackFromWorld(bundle, second, player),
                "the bundle refused a second stack although " + (capacity - 64) + " items still fit");
        helper.assertValueEqual(second.getCount(), 128 - capacity,
                "stone left over from the second stack - the bundle holds " + capacity + ", so it may only "
                        + "take " + (capacity - 64) + " of the 64 offered");
        helper.assertValueEqual(totalIn(bundle), capacity, "stone inside the bundle once it is full");

        // --- third stack: full is full, and a refused insert must not eat anything ---
        ItemStack third = new ItemStack(Items.STONE, 64);
        helper.assertTrue(!item.tryInsertStackFromWorld(bundle, third, player),
                "a full bundle reported that it took another stack");
        helper.assertValueEqual(third.getCount(), 64, "stone left over from the refused third stack");
        helper.assertValueEqual(totalIn(bundle), capacity, "stone inside the bundle after the refused stack");

        // --- the same room, measured with an item that stacks to 16 ---
        int stoneStack = new ItemStack(Items.STONE).getMaxStackSize();
        int pearlStack = new ItemStack(Items.ENDER_PEARL).getMaxStackSize();
        helper.assertTrue(pearlStack < stoneStack,
                "setup guard: ender pearls stack to " + pearlStack + " and stone to " + stoneStack
                        + ", so the two no longer tell a per-item weight from a per-stack one");

        int pearls = fillWith(helper, player, new ItemStack(ModItems.REINFORCED_BUNDLE), Items.ENDER_PEARL);
        helper.assertValueEqual(pearls * stoneStack, capacity * pearlStack,
                "the bundle took " + pearls + " ender pearls and " + capacity + " stone; in vanilla stacks "
                        + "those two have to be the same amount of room");

        helper.succeed();
    }

    /**
     * What may go into a bundle at all. A shulker box has to bounce off all three entry points -
     * the two inventory clicks and the world pickup - and the click has to bounce off
     * <em>unhandled</em>, because a click reported as handled is eaten: the player would be unable
     * to pick the shulker box up or swap it while a bundle is on the cursor.
     *
     * <p>The "no" itself is vanilla's ({@code BlockItem#canFitInsideContainerItems} is false for
     * shulker boxes and true for everything else); what the mod contributes is asking. So every
     * case is paired with a stack that must get through, and one of those pairs is a
     * <em>netherite bundle</em>: containers nesting inside containers is the answer this item's
     * own {@code canFitInsideContainerItems} override gives, and the nested bundle keeps its own
     * contents while it sits inside. Only one is stored on purpose - see the known defect about
     * nested weight in the class Javadoc.
     *
     * <p>What breaks it: deleting the guard in {@code tryInsertStackFromWorld} or in either
     * {@code override*} method - shulker boxes would then disappear into bundles, which is how
     * container recursion turns into item loss - or answering {@code true} while turning the item
     * away.
     */
    public static void insertionTurnsAwayWhatCannotGoIntoContainerItems(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        pinDefaultClickBinding(helper);

        // --- world pickup ---
        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        // Read through vanilla's own helper, which asks the same flag the bundle asks - and is not
        // deprecated, unlike the Item method behind it.
        helper.assertTrue(!BundleContents.canItemBeInBundle(new ItemStack(Items.SHULKER_BOX)),
                "setup guard: vanilla now lets shulker boxes into container items, so this test no longer "
                        + "has an item the bundle has to turn away");
        helper.assertTrue(!bundleItem(bundle).tryInsertStackFromWorld(bundle, shulker, player),
                "the bundle picked a shulker box up off the ground");
        helper.assertValueEqual(totalIn(bundle), 0, "items inside the bundle after the shulker box");
        helper.assertValueEqual(shulker.getCount(), 1, "shulker boxes left outside the bundle");

        // --- bundle on the cursor, shulker box in the slot ---
        ItemStack cursorBundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        Slot slot = slotHolding(new ItemStack(Items.SHULKER_BOX));
        helper.assertTrue(!bundleItem(cursorBundle)
                        .overrideStackedOnOther(cursorBundle, slot, ClickAction.PRIMARY, player),
                "left clicking a bundle onto a shulker box reported the click as handled, which eats it - "
                        + "the player can no longer pick that shulker box up");
        helper.assertValueEqual(totalIn(cursorBundle), 0, "items inside the bundle after the slot click");
        helper.assertValueEqual(slot.getItem().getCount(), 1, "shulker boxes left in the slot");

        // --- shulker box on the cursor, bundle in the slot ---
        ItemStack slotBundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ItemStack onCursor = new ItemStack(Items.SHULKER_BOX);
        helper.assertTrue(!bundleItem(slotBundle).overrideOtherStackedOnMe(slotBundle, onCursor,
                        slotHolding(slotBundle), ClickAction.PRIMARY, player, cursorAccess(onCursor)),
                "left clicking a shulker box onto a bundle reported the click as handled");
        helper.assertValueEqual(totalIn(slotBundle), 0, "items inside the bundle after the cursor click");
        helper.assertValueEqual(onCursor.getCount(), 1, "shulker boxes left on the cursor");

        // --- control: an ordinary stack gets through the same three doors ---
        ItemStack takes = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ItemStack stone = new ItemStack(Items.STONE, 8);
        helper.assertTrue(bundleItem(takes).tryInsertStackFromWorld(takes, stone, player),
                "control: the bundle refused stone from the ground, so the refusals above prove nothing");
        Slot stoneSlot = slotHolding(new ItemStack(Items.STONE, 8));
        helper.assertTrue(bundleItem(takes).overrideStackedOnOther(takes, stoneSlot, ClickAction.PRIMARY, player),
                "control: left clicking the bundle onto a slot holding stone did nothing");
        ItemStack stoneOnCursor = new ItemStack(Items.STONE, 8);
        helper.assertTrue(bundleItem(takes).overrideOtherStackedOnMe(takes, stoneOnCursor,
                        slotHolding(takes), ClickAction.PRIMARY, player, cursorAccess(stoneOnCursor)),
                "control: left clicking stone onto the bundle did nothing");
        helper.assertValueEqual(totalIn(takes), 24, "stone inside the bundle after all three control clicks");

        // --- a bundle inside a bundle, contents and all ---
        ItemStack inner = new ItemStack(ModItems.NETHERITE_BUNDLE);
        insertExactly(helper, player, inner, Items.DIRT, 16);
        ItemStack outer = new ItemStack(ModItems.ENDERITE_BUNDLE);
        helper.assertTrue(bundleItem(outer).tryInsertStackFromWorld(outer, inner.copy(), player),
                "an enderite bundle refused to hold a netherite bundle");

        List<ItemStack> stored = entries(outer);
        helper.assertValueEqual(stored.size(), 1, "entries in the enderite bundle");
        helper.assertTrue(stored.getFirst().is(ModItems.NETHERITE_BUNDLE),
                "the enderite bundle holds " + stored.getFirst() + " instead of the netherite bundle");
        helper.assertValueEqual(totalIn(stored.getFirst()), 16,
                "dirt inside the netherite bundle after it was stored in the enderite one");

        helper.succeed();
    }

    /**
     * Where a stack ends up inside the bundle. Equal items are pulled out of wherever they were,
     * added together and put back as one block of full stacks at the front, and everything else is
     * pushed down - that is what the player reads as "what I just put in is on top, and my stone
     * is in one place".
     *
     * <p>Stone, dirt, stone: without the merge the bundle would end up holding two stone entries
     * with the dirt wedged between them, without the sort the merged block would sit behind the
     * dirt, and without the re-stacking the two stone entries would stay 40 and 40 instead of a
     * full stack and a remainder. All three are asserted; which of the two merged stacks comes
     * first is not, because the code and its own comment disagree about that - see the known
     * defects in the class Javadoc.
     *
     * <p>What breaks it: dropping the merge loop (stone in two places), appending the merged block
     * instead of inserting it at index 0 (the dirt would be on top), or building the block out of
     * the offered count only, which would leave the earlier stone where it was.
     */
    public static void insertionMergesEqualStacksAndPushesThemToTheTop(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        insertExactly(helper, player, bundle, Items.STONE, 40);
        insertExactly(helper, player, bundle, Items.DIRT, 10);
        insertExactly(helper, player, bundle, Items.STONE, 40);

        List<ItemStack> stored = entries(bundle);
        helper.assertValueEqual(stored.size(), 3,
                "entries in the bundle after 40 stone, 10 dirt and 40 more stone - 80 stone have to sit in "
                        + "two entries (a full stack and a remainder) and the dirt in a third");
        helper.assertTrue(stored.get(0).is(Items.STONE) && stored.get(1).is(Items.STONE),
                "the two entries at the top are " + stored.get(0) + " and " + stored.get(1)
                        + " instead of the merged stone");
        helper.assertTrue(stored.get(2).is(Items.DIRT),
                "the last entry is " + stored.get(2) + " instead of the dirt that was pushed down");

        helper.assertValueEqual(stored.get(0).getCount() + stored.get(1).getCount(), 80,
                "stone in the two merged entries");
        helper.assertValueEqual(Math.max(stored.get(0).getCount(), stored.get(1).getCount()), 64,
                "the larger of the two stone entries - the merge has to fill whole stacks before it opens "
                        + "a second entry, otherwise 80 stone stay as 40 and 40");
        helper.assertValueEqual(stored.get(2).getCount(), 10, "dirt in the bundle");

        helper.succeed();
    }

    /**
     * The Drawer restriction: a bundle carrying it holds five <em>kinds</em> - not five entries,
     * and not five stacks.
     *
     * <p>The fixture is built so that entries and kinds are different numbers at the moment the
     * limit is asked. One of the kinds goes in 80 items at a time, and the merge stores those as a
     * full stack plus a remainder, so the bundle sits at <em>four</em> kinds in <em>five</em>
     * entries when the fifth kind is offered. A limit on entries turns that fifth kind away; the
     * limit on kinds has to take it and only refuse the sixth. Both answers are asserted, which is
     * what makes the difference between the two readings load-bearing here - with five kinds in
     * five entries the two count the same and either implementation passes.
     *
     * <p>A kind is item <em>and</em> components, which the second half pins: five stone stacks that
     * differ only in their custom name are five kinds, not one, so a sixth named stone is refused
     * although its item has been in the bundle all along. Comparing by item alone would let it in.
     *
     * <p>Both halves pair every refusal with an insert that has to succeed - more of something the
     * bundle already holds - because a refusal on its own is also what a full bundle looks like.
     * The control at the end runs the same six kinds into a bundle without Drawer: the enchantment
     * is what forbids the sixth kind, not the item.
     *
     * <p>What breaks it: deleting the type count, counting entries instead of kinds, comparing by
     * item only rather than by item and components, or applying the restriction to bundles without
     * the enchantment.
     */
    public static void drawerCapsTheBundleAtFiveKinds(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        // Four kinds, five entries: the 80 of the first kind are stored as 64 + 16 by the merge.
        ItemStack bundle = enchanted(helper, ModItems.REINFORCED_BUNDLE, ModEnchantments.DRAWER, 1);
        insertExactly(helper, player, bundle, SIX_KINDS[0], SPLIT_STOCK);
        for (int kind = 1; kind < DRAWER_KINDS - 1; kind++) {
            insertExactly(helper, player, bundle, SIX_KINDS[kind], 1);
        }
        helper.assertValueEqual(kindsIn(bundle), DRAWER_KINDS - 1,
                "kinds inside the Drawer bundle before the fifth kind is offered");
        helper.assertValueEqual(entries(bundle).size(), DRAWER_KINDS,
                "setup guard: entries inside the Drawer bundle - the " + SPLIT_STOCK + " " + SIX_KINDS[0]
                        + " have to sit in two of them, otherwise entries and kinds are the same number and "
                        + "this test cannot tell which of the two the limit counts");

        // Five entries, four kinds - and the fifth kind still has to get in.
        ItemStack fifth = new ItemStack(SIX_KINDS[DRAWER_KINDS - 1], 1);
        helper.assertTrue(bundleItem(bundle).tryInsertStackFromWorld(bundle, fifth, player),
                "a Drawer bundle holding " + (DRAWER_KINDS - 1) + " kinds in " + DRAWER_KINDS
                        + " entries refused the fifth kind - the limit is counting entries, not kinds");
        helper.assertValueEqual(fifth.getCount(), 0, "the fifth kind left outside the bundle");
        helper.assertValueEqual(kindsIn(bundle), DRAWER_KINDS,
                "kinds inside the Drawer bundle after the fifth one went in");

        // Now it really is five kinds, and the sixth is the one that has to bounce.
        ItemStack sixth = new ItemStack(SIX_KINDS[DRAWER_KINDS], 1);
        helper.assertTrue(!bundleItem(bundle).tryInsertStackFromWorld(bundle, sixth, player),
                "a Drawer bundle already holding " + DRAWER_KINDS + " kinds accepted a sixth one");
        helper.assertValueEqual(sixth.getCount(), 1, "the sixth kind left outside the bundle");
        helper.assertValueEqual(kindsIn(bundle), DRAWER_KINDS,
                "kinds inside the Drawer bundle after the sixth was refused");

        // The refusal has to be the kind limit, not a full bundle: more of a kind it holds fits.
        ItemStack more = new ItemStack(SIX_KINDS[0], 64);
        helper.assertTrue(bundleItem(bundle).tryInsertStackFromWorld(bundle, more, player),
                "the Drawer bundle refused more of a kind it already holds, so the refusal of the sixth "
                        + "kind was a full bundle and this test proves nothing about the kind limit");
        helper.assertValueEqual(countIn(bundle, SIX_KINDS[0]), SPLIT_STOCK + 64,
                SIX_KINDS[0] + " inside the Drawer bundle after it took a second helping");

        // --- a kind is item plus components, not item ---
        ItemStack byName = enchanted(helper, ModItems.REINFORCED_BUNDLE, ModEnchantments.DRAWER, 1);
        for (int n = 0; n < DRAWER_KINDS; n++) {
            ItemStack named = namedStone("drawer stone " + n);
            helper.assertTrue(bundleItem(byName).tryInsertStackFromWorld(byName, named, player),
                    "test setup broken: the Drawer bundle refused named stone number " + n);
        }
        helper.assertValueEqual(kindsIn(byName), DRAWER_KINDS,
                DRAWER_KINDS + " stone stacks that differ only in their custom name have to count as "
                        + DRAWER_KINDS + " kinds; the bundle holds " + entries(byName));

        ItemStack sixthName = namedStone("drawer stone " + DRAWER_KINDS);
        helper.assertTrue(!bundleItem(byName).tryInsertStackFromWorld(byName, sixthName, player),
                "the bundle took a sixth differently named stone although it already held " + DRAWER_KINDS
                        + " of them - the kind count compares by item alone, so every named stone looks "
                        + "like the stone that is already inside");
        helper.assertValueEqual(sixthName.getCount(), 1, "the sixth named stone left outside the bundle");

        ItemStack knownName = namedStone("drawer stone 0");
        helper.assertTrue(bundleItem(byName).tryInsertStackFromWorld(byName, knownName, player),
                "the bundle refused a second helping of a name it already holds, so the refusal above was "
                        + "a full bundle and says nothing about the kind limit");
        helper.assertValueEqual(kindsIn(byName), DRAWER_KINDS,
                "kinds inside the bundle after a name it already held came back");

        // --- control: without Drawer the sixth kind goes in ---
        ItemStack plain = new ItemStack(ModItems.REINFORCED_BUNDLE);
        for (int kind = 0; kind <= DRAWER_KINDS; kind++) {
            insertExactly(helper, player, plain, SIX_KINDS[kind], 1);
        }
        helper.assertValueEqual(kindsIn(plain), DRAWER_KINDS + 1,
                "kinds inside a bundle without Drawer - the kind limit is the enchantment's, not the item's");

        helper.succeed();
    }

    // =====================================================================================
    // WHAT COMES OUT
    // =====================================================================================

    /**
     * Which entry leaves the bundle on a remove click. The selected one, as a whole stack, and
     * with the rest of the bundle untouched; with nothing selected, the topmost one.
     *
     * <p>The bundle is filled with three kinds in a known order (each insert goes to the front, so
     * the last one in is on top), which is what makes "the third entry" and "the top entry"
     * distinguishable at all - with one kind, a removal that ignores the selection would look
     * exactly like one that honours it.
     *
     * <p>The click is pinned to the default binding; the binding itself belongs to
     * {@link ConfigOptionTests#bundleClickInversionFollowsTheConfiguredOption}.
     *
     * <p>What breaks it: {@code removeSelectedOrFirstItem} ignoring the selection, handing back one
     * item instead of the whole entry, deleting an entry other than the one it hands back, or
     * losing the "nothing selected means the top" fallback.
     */
    public static void theSelectedEntryIsTheOneThatComesOut(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        pinDefaultClickBinding(helper);

        ItemStack bundle = new ItemStack(ModItems.ENDERITE_BUNDLE);
        insertExactly(helper, player, bundle, Items.SAND, 16);
        insertExactly(helper, player, bundle, Items.DIRT, 32);
        insertExactly(helper, player, bundle, Items.STONE, 64);
        helper.assertValueEqual(entries(bundle).size(), 3, "entries in the bundle before anything is taken out");
        helper.assertTrue(entries(bundle).getFirst().is(Items.STONE),
                "setup guard: the last insert is not on top, so 'the selected entry' and 'the top entry' "
                        + "can no longer be told apart");

        // --- the selected entry, all 16 of it ---
        ReinforcedBundleItem.setBundleSelectedItem(bundle, 2);
        helper.assertValueEqual(selectedIndex(bundle), 2, "the index the bundle reports as selected");

        Slot empty = slotHolding(ItemStack.EMPTY);
        helper.assertTrue(bundleItem(bundle).overrideStackedOnOther(bundle, empty, ClickAction.SECONDARY, player),
                "right clicking a filled bundle onto an empty slot took nothing out");
        helper.assertTrue(empty.getItem().is(Items.SAND),
                "the slot holds " + empty.getItem() + " instead of the selected sand");
        helper.assertValueEqual(empty.getItem().getCount(), 16,
                "sand in the slot - the whole entry has to come out, not one item");
        helper.assertValueEqual(entries(bundle).size(), 2, "entries left in the bundle");
        helper.assertValueEqual(countIn(bundle, Items.SAND), 0, "sand left in the bundle");
        helper.assertValueEqual(countIn(bundle, Items.STONE), 64, "stone left in the bundle");
        helper.assertValueEqual(countIn(bundle, Items.DIRT), 32, "dirt left in the bundle");

        // --- nothing selected: the top entry ---
        helper.assertValueEqual(selectedIndex(bundle), -1,
                "the selection the bundle reports after an entry was taken out");
        Slot second = slotHolding(ItemStack.EMPTY);
        helper.assertTrue(bundleItem(bundle).overrideStackedOnOther(bundle, second, ClickAction.SECONDARY, player),
                "right clicking the bundle onto a second empty slot took nothing out");
        helper.assertTrue(second.getItem().is(Items.STONE),
                "with nothing selected the bundle handed out " + second.getItem() + " instead of the top entry");
        helper.assertValueEqual(second.getItem().getCount(), 64, "stone in the second slot");
        helper.assertValueEqual(entries(bundle).size(), 1, "entries left in the bundle");
        helper.assertValueEqual(countIn(bundle, Items.DIRT), 32, "dirt left in the bundle at the end");

        helper.succeed();
    }

    /**
     * The right click in the hand. It throws the selected entry on the ground - and refuses, with
     * {@code FAIL}, when that entry is a block, because a block in a Master Builder bundle is
     * building material and is not to be spat on the floor.
     *
     * <p>{@link QuiverTests#rightClicksDoNothingEvenWithMasterBuilder} already drives the throwing
     * half on a bundle holding nothing but arrows, as the control for a quiver that must not do
     * it. What is added here is the part that needs a <em>mixed</em> bundle: the decision is made
     * about the selected entry and not about the bundle as a whole, so the same bundle answers
     * {@code SUCCESS} or {@code FAIL} depending only on what is selected.
     *
     * <p>What breaks it: deleting the {@code BlockItem} branch (the bundle would drop its building
     * material), keying it to the whole contents instead of the selected entry, or dropping from
     * index 0 regardless of the selection.
     */
    public static void rightClickThrowsTheSelectedStackButNeverBlocks(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        // Arrows first, stone on top of them: index 0 is the stone, index 1 the arrows.
        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        insertExactly(helper, player, bundle, Items.ARROW, 16);
        insertExactly(helper, player, bundle, Items.STONE, 32);
        player.setItemInHand(InteractionHand.MAIN_HAND, bundle);

        // --- selected entry is a block: nothing happens, and the click says so ---
        InteractionResult onStone = bundle.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(onStone == InteractionResult.FAIL,
                "right clicking a bundle whose selected entry is stone answered " + onStone + " instead of FAIL");
        helper.assertValueEqual(countIn(bundle, Items.STONE), 32, "stone left in the bundle after the refusal");
        helper.assertValueEqual(droppedItems(helper).size(), 0, "items lying in the room after the refusal");

        // --- select the arrows in the same bundle: now it throws ---
        ReinforcedBundleItem.setBundleSelectedItem(bundle, 1);
        helper.assertValueEqual(selectedIndex(bundle), 1, "the index the bundle reports as selected");

        InteractionResult onArrows = bundle.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(onArrows == InteractionResult.SUCCESS,
                "right clicking the same bundle with the arrows selected answered " + onArrows
                        + " instead of SUCCESS");
        helper.assertValueEqual(countIn(bundle, Items.ARROW), 0, "arrows left in the bundle");
        helper.assertValueEqual(countIn(bundle, Items.STONE), 32,
                "stone left in the bundle after the arrows were thrown out");

        List<ItemEntity> dropped = droppedItems(helper);
        helper.assertValueEqual(dropped.size(), 1, "items lying in the room after the arrows were thrown");
        helper.assertTrue(dropped.getFirst().getItem().is(Items.ARROW),
                "the bundle threw out " + dropped.getFirst().getItem() + " instead of the selected arrows");
        helper.assertValueEqual(dropped.getFirst().getItem().getCount(), 16,
                "arrows in the dropped stack - the whole entry has to leave");
        for (ItemEntity entity : dropped) {
            entity.discard();
        }

        // --- and the stone that is left is still refused ---
        InteractionResult again = bundle.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(again == InteractionResult.FAIL,
                "right clicking the bundle that now only holds stone answered " + again + " instead of FAIL");
        helper.assertValueEqual(droppedItems(helper).size(), 0, "items lying in the room at the end");

        helper.succeed();
    }

    // =====================================================================================
    // BUILDING OUT OF THE BUNDLE
    // =====================================================================================

    /**
     * The Master Builder bundle as a building tool: a click at a block face places the selected
     * entry straight out of the bundle, charges one item for it outside creative, and charges
     * nothing inside - and with Color Palette on top, it stops taking the selected entry and picks
     * a random one instead, which is how a wall gets mixed out of one bundle.
     *
     * <p>The unenchanted case comes first, because it is what makes the rest mean anything: an
     * ordinary bundle falls through to vanilla, which builds nothing.
     * {@link QuiverTests#rightClicksDoNothingEvenWithMasterBuilder} runs one survival placement as
     * the control for the quiver; the creative half, the selection, and the whole Color Palette
     * branch are only here.
     *
     * <p>The Color Palette half is a loop rather than a single click: the choice is a die roll, so
     * a single click cannot show it. Two things keep "both kinds appeared" from meaning something
     * else. Both entries are stocked well past the round count (see {@link #PALETTE_STOCK}), and
     * only the first half of the run is asked for both kinds, so no entry can have run empty and
     * shifted the top of the pile by then - a bundle that always builds with its top entry
     * produces exactly one kind there. And every placement is balanced against what left the
     * bundle, so a bundle that placed one kind while billing the other is caught as well. The
     * unenchanted control right after it runs the identical bundle for the identical number of
     * rounds and has to produce one kind every time.
     *
     * <p>What breaks it: falling through to vanilla with Master Builder on (nothing gets built),
     * paying for the block on {@code isCreative()} rather than {@code instabuild} (the creative
     * player would be billed), removing more or less than one item, placing index 0 regardless of
     * the selection, or dropping the Color Palette branch, which would leave the wall single
     * coloured.
     */
    public static void masterBuilderPlacesFromTheBundleAndColorPaletteScattersIt(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        BlockPos anchor = new BlockPos(4, 1, 4);

        // --- no enchantment: a bundle is not a building tool ---
        ItemStack plain = new ItemStack(ModItems.REINFORCED_BUNDLE);
        insertExactly(helper, player, plain, Items.STONE, 8);
        InteractionResult plainResult = placeFrom(helper, player, plain, anchor);
        helper.assertTrue(!plainResult.consumesAction(),
                "an unenchanted bundle answered " + plainResult + " when it was clicked at a block face");
        helper.assertBlockPresent(Blocks.AIR, anchor.above());
        helper.assertValueEqual(countIn(plain, Items.STONE), 8, "stone left in the unenchanted bundle");

        // --- Master Builder, survival: the selected entry is placed and paid for ---
        ItemStack builder = enchanted(helper, ModItems.REINFORCED_BUNDLE, ModEnchantments.MASTER_BUILDER, 1);
        insertExactly(helper, player, builder, Items.STONE, 8);
        insertExactly(helper, player, builder, Items.DIRT, 8);
        ReinforcedBundleItem.setBundleSelectedItem(builder, 1);

        InteractionResult placed = placeFrom(helper, player, builder, anchor);
        helper.assertTrue(placed.consumesAction(),
                "a Master Builder bundle answered " + placed + " instead of placing its stone");
        helper.assertBlockPresent(Blocks.STONE, anchor.above());
        helper.assertValueEqual(countIn(builder, Items.STONE), 7,
                "stone left after one block was placed out of the bundle");
        helper.assertValueEqual(countIn(builder, Items.DIRT), 8,
                "dirt left after the selected stone was placed - the bundle billed the wrong entry");

        // --- nothing selected: the topmost entry is the one that gets built with ---
        helper.assertValueEqual(selectedIndex(builder), -1,
                "the selection the bundle reports after it placed a block");
        helper.assertTrue(entries(builder).getFirst().is(Items.DIRT),
                "setup guard: the top entry is " + entries(builder).getFirst() + ", so the case below no "
                        + "longer tells 'index 0' from 'the previous selection'");
        placeFrom(helper, player, builder, anchor);
        helper.assertBlockPresent(Blocks.DIRT, anchor.above());
        helper.assertValueEqual(countIn(builder, Items.DIRT), 7, "dirt left after the second placement");

        // --- creative: the same placement, free of charge ---
        player.getAbilities().instabuild = true;
        try {
            InteractionResult free = placeFrom(helper, player, builder, anchor);
            helper.assertTrue(free.consumesAction(),
                    "a creative player got " + free + " out of the Master Builder bundle");
            helper.assertBlockPresent(Blocks.DIRT, anchor.above());
            helper.assertValueEqual(countIn(builder, Items.DIRT), 7,
                    "dirt left after a creative placement - creative building must not empty the bundle");
        } finally {
            player.getAbilities().instabuild = false;
        }

        // --- Color Palette: the entry is drawn, not selected ---
        // Both entries are stocked past the round count on purpose - see PALETTE_STOCK.
        ItemStack palette = enchanted(helper, ModItems.ENDERITE_BUNDLE, ModEnchantments.MASTER_BUILDER, 1);
        palette.enchant(enchantment(helper, ModEnchantments.COLOR_PALETTE), 1);
        insertExactly(helper, player, palette, Items.STONE, PALETTE_STOCK);
        insertExactly(helper, player, palette, Items.DIRT, PALETTE_STOCK);

        List<Block> drawn = placeRepeatedly(helper, player, palette, anchor, PALETTE_ROUNDS);

        // Only the first half is asked for both kinds. Both entries are certainly still full there
        // (half the rounds cannot empty an entry of PALETTE_STOCK), so "both kinds appeared" cannot
        // be the pile shifting under a bundle that always builds with its top entry.
        List<Block> early = drawn.subList(0, PALETTE_ROUNDS / 2);
        helper.assertTrue(early.contains(Blocks.STONE) && early.contains(Blocks.DIRT),
                "the first " + early.size() + " placements out of a Color Palette bundle holding stone and "
                        + "dirt produced only " + early.stream().distinct().toList()
                        + "; the palette is not drawing an entry at random");

        // Every placement was billed to the entry it came out of. Without this, a bundle that
        // placed one kind while paying for the other would still pass the check above.
        int stoneDrawn = Collections.frequency(drawn, Blocks.STONE);
        int dirtDrawn = Collections.frequency(drawn, Blocks.DIRT);
        helper.assertValueEqual(stoneDrawn + dirtDrawn, PALETTE_ROUNDS,
                "placements that produced one of the two kinds in the palette bundle - it also built "
                        + drawn.stream().distinct().toList());
        helper.assertValueEqual(countIn(palette, Items.STONE), PALETTE_STOCK - stoneDrawn,
                "stone left in the palette bundle after it placed stone " + stoneDrawn + " times");
        helper.assertValueEqual(countIn(palette, Items.DIRT), PALETTE_STOCK - dirtDrawn,
                "dirt left in the palette bundle after it placed dirt " + dirtDrawn + " times");

        // --- control: the same bundle without the palette places one kind, every time ---
        // Identical stock, identical round count - the enchantment is the only difference.
        ItemStack single = enchanted(helper, ModItems.ENDERITE_BUNDLE, ModEnchantments.MASTER_BUILDER, 1);
        insertExactly(helper, player, single, Items.STONE, PALETTE_STOCK);
        insertExactly(helper, player, single, Items.DIRT, PALETTE_STOCK);
        List<Block> steady = placeRepeatedly(helper, player, single, anchor, PALETTE_ROUNDS);
        helper.assertValueEqual(steady.stream().distinct().count(), 1L,
                "control: a Master Builder bundle without Color Palette placed "
                        + steady.stream().distinct().toList() + " - the loop above therefore says nothing "
                        + "about the palette");

        helper.succeed();
    }

    // =====================================================================================
    // CAPACITY, AND THE TWO OTHER PLACES IT IS COMPUTED
    // =====================================================================================

    /**
     * The capacity table, measured rather than read: fill each variant until it refuses, and check
     * the numbers against each other and against {@code getBaseCapacityItems}, the stackless copy
     * the wiki export prints.
     *
     * <p>Ratios instead of constants, on purpose: the tier factors (2 and 3) and Deep Pockets
     * (2 and 4) are asserted against the measured base, so a balance change to the base moves them
     * all together and only a change to a <em>factor</em> turns this red.
     * {@link ItemBehaviourTests#bundleCapacityGrowsWithTierAndEnchantments} settles for an
     * ordering, which cannot tell a doubling from a 1.1x.
     *
     * <p>Drawer is asserted as a slope - one eighth of the base capacity per level - because the
     * offset in that formula is a known defect (see the class Javadoc) and both readings of it
     * agree on the slope. That still catches the divisor moving, the level being ignored, or the
     * enchantment being read at one level only.
     *
     * <p>The enchanted cases run on {@code ReinforcedBundleItem}; {@code QuiverItem} overrides
     * both capacity methods with copies of its own, so the numbers {@link QuiverTests} pins say
     * nothing about the ones here.
     *
     * <p>What breaks it: changing a tier factor, dropping the Drawer or Deep Pockets branch out of
     * {@code getMaxCapacity}, letting the Deep Pockets levels collapse into one factor, or letting
     * {@code getBaseCapacityItems} drift away from the capacity the filling really uses - which is
     * how the wiki starts printing a number the game does not honour.
     */
    public static void capacityFollowsTierAndEnchantmentsAndMatchesTheWikiExport(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        // Setup guards: the levels below have to be levels a player can actually reach.
        helper.assertValueEqual(enchantment(helper, ModEnchantments.DEEP_POCKETS).value().getMaxLevel(), 2,
                "max level of Deep Pockets");
        helper.assertValueEqual(enchantment(helper, ModEnchantments.DRAWER).value().getMaxLevel(),
                DRAWER_MAX_LEVEL, "max level of Drawer");

        int plain = fillWith(helper, player, new ItemStack(ModItems.REINFORCED_BUNDLE), Items.STONE);
        int netherite = fillWith(helper, player, new ItemStack(ModItems.NETHERITE_BUNDLE), Items.STONE);
        int enderite = fillWith(helper, player, new ItemStack(ModItems.ENDERITE_BUNDLE), Items.STONE);

        helper.assertValueEqual(netherite, plain * 2,
                "stone a netherite bundle takes - tier factor 2 against the " + plain + " of the plain one");
        helper.assertValueEqual(enderite, plain * 3,
                "stone an enderite bundle takes - tier factor 3 against the " + plain + " of the plain one");

        // --- the same three numbers through the stackless path the wiki export uses ---
        helper.assertValueEqual(baseCapacityItems(ModItems.REINFORCED_BUNDLE), plain,
                "getBaseCapacityItems() of the reinforced bundle against the stone it really takes");
        helper.assertValueEqual(baseCapacityItems(ModItems.NETHERITE_BUNDLE), netherite,
                "getBaseCapacityItems() of the netherite bundle against the stone it really takes");
        helper.assertValueEqual(baseCapacityItems(ModItems.ENDERITE_BUNDLE), enderite,
                "getBaseCapacityItems() of the enderite bundle against the stone it really takes");

        // --- Deep Pockets doubles, then quadruples ---
        int deep1 = fillWith(helper, player,
                enchanted(helper, ModItems.REINFORCED_BUNDLE, ModEnchantments.DEEP_POCKETS, 1), Items.STONE);
        int deep2 = fillWith(helper, player,
                enchanted(helper, ModItems.REINFORCED_BUNDLE, ModEnchantments.DEEP_POCKETS, 2), Items.STONE);
        helper.assertValueEqual(deep1, plain * 2, "stone a Deep Pockets I bundle takes - twice the base");
        helper.assertValueEqual(deep2, plain * 4, "stone a Deep Pockets II bundle takes - four times the base");

        // --- Drawer grows by an eighth of the base per level ---
        int drawer1 = fillWith(helper, player,
                enchanted(helper, ModItems.REINFORCED_BUNDLE, ModEnchantments.DRAWER, 1), Items.STONE);
        int drawerMax = fillWith(helper, player,
                enchanted(helper, ModItems.REINFORCED_BUNDLE, ModEnchantments.DRAWER, DRAWER_MAX_LEVEL),
                Items.STONE);
        helper.assertTrue(drawer1 > plain,
                "a Drawer I bundle takes " + drawer1 + " stone, no more than the " + plain + " of a plain one");
        helper.assertValueEqual((drawerMax - drawer1) * 8, plain * (DRAWER_MAX_LEVEL - 1),
                "the step from Drawer I (" + drawer1 + ") to Drawer " + DRAWER_MAX_LEVEL + " (" + drawerMax
                        + "): every level has to add an eighth of the " + plain + " item base capacity");

        helper.succeed();
    }

    /**
     * The bar under the item and the number in the tooltip both come out of
     * {@code getMaxCapacityForVisuals} - a <em>second</em> copy of the capacity formula that reads
     * the enchantments off the stack by matching their ids by substring, where the filling path
     * resolves them through the registry. Two formulas for one number is the pairing that drifts,
     * and the player sees the drift as a bar that is full at half a bundle.
     *
     * <p>Every variant is measured first and then filled to exactly half of what it measured, so
     * the expected width is the same 7 for all of them however the capacity table is balanced. The
     * half-full case catches the drift in both directions (a visual capacity that is too small
     * over-fills the bar, one that is too large under-fills it); the full case is what catches a
     * bar that never reaches its end. {@link QuiverTests#barWidthFollowsTheSameCapacityTheFillingUses}
     * does this for {@code QuiverItem}'s own copy of the formula and for the plain bundle; the
     * enchanted bundle branches are only here.
     *
     * <p>What breaks it: dropping an enchantment out of the visuals formula, renaming an
     * enchantment so the substring match stops matching (the filling path would go on working -
     * that is the split this test is about), letting the tooltip hand out the base capacity while
     * the bundle really holds more, or losing the "no bar on an empty bundle" rule.
     */
    public static void barAndTooltipReadTheSameCapacityTheFillingUses(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        // An empty bundle reports width 0 where vanilla's own formula would report 1, so this one
        // really does belong to the override.
        ItemStack empty = new ItemStack(ModItems.REINFORCED_BUNDLE);
        helper.assertValueEqual(empty.getItem().getBarWidth(empty), 0, "bar width of an empty bundle");

        assertBarAndTooltip(helper, player, new ItemStack(ModItems.REINFORCED_BUNDLE), "a plain bundle");
        assertBarAndTooltip(helper, player, new ItemStack(ModItems.ENDERITE_BUNDLE), "an enderite bundle");
        // Both Deep Pockets levels, because the visuals formula answers them in two separate
        // branches (level == 1 and level >= 2): with only the second one here, deleting the first
        // would leave a Deep Pockets I bundle drawing a full bar at half its real capacity.
        assertBarAndTooltip(helper, player,
                enchanted(helper, ModItems.REINFORCED_BUNDLE, ModEnchantments.DEEP_POCKETS, 1),
                "a Deep Pockets I bundle");
        assertBarAndTooltip(helper, player,
                enchanted(helper, ModItems.REINFORCED_BUNDLE, ModEnchantments.DEEP_POCKETS, 2),
                "a Deep Pockets II bundle");
        assertBarAndTooltip(helper, player,
                enchanted(helper, ModItems.REINFORCED_BUNDLE, ModEnchantments.DRAWER, 1), "a Drawer I bundle");
        assertBarAndTooltip(helper, player,
                enchanted(helper, ModItems.REINFORCED_BUNDLE, ModEnchantments.DRAWER, DRAWER_MAX_LEVEL),
                "a Drawer " + DRAWER_MAX_LEVEL + " bundle");

        // --- the bar runs from green to red as the bundle fills up ---
        // Three levels, not two: vanilla's fallback knows one colour below a full vanilla stack and
        // one at or above it, so a pair of readings would still look like a ramp with the override
        // deleted. A third reading in between has to differ from both, which only a real ramp does.
        int capacity = fillWith(helper, player, new ItemStack(ModItems.REINFORCED_BUNDLE), Items.STONE);
        int lowColour = barColourAt(helper, player, 1);
        int midColour = barColourAt(helper, player, capacity / 2);
        int highColour = barColourAt(helper, player, capacity);

        helper.assertTrue(lowColour != midColour && midColour != highColour && lowColour != highColour,
                "the bar colour of a bundle holding 1, " + (capacity / 2) + " and " + capacity
                        + " items is " + lowColour + " / " + midColour + " / " + highColour
                        + "; a colour that runs over the hue circle has to give three different answers");
        helper.assertTrue(red(highColour) > red(lowColour),
                "the bar of a full bundle is no redder than that of a nearly empty one (" + highColour
                        + " against " + lowColour + ")");
        helper.assertTrue(green(lowColour) > green(highColour),
                "the bar of a nearly empty bundle is no greener than that of a full one (" + lowColour
                        + " against " + highColour + ")");

        helper.succeed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /**
     * The usual in-level mock player, moved into the room, with an empty inventory and
     * {@code instabuild} switched off - the flag the master builder branch reads. It keeps its
     * connection, which the insert and remove sounds go through, and it is handed back to the
     * player list at the end of the test; a leaked mock player stalls the gametest server on
     * shutdown.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.getAbilities().instabuild = false;
        player.getInventory().clearContent();
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /**
     * Pins {@code tools.invertBundleInteractions} to "off" for the rest of the test, so that
     * "left click" means insert and "right click" means remove. The option is shared mutable state
     * and another test may have left it on; it is restored at the end of this one.
     */
    private static void pinDefaultClickBinding(GameTestHelper helper) {
        boolean original = Simplebuilding.getConfig().tools.invertBundleInteractions;
        helper.runBeforeTestEnd(() -> Simplebuilding.getConfig().tools.invertBundleInteractions = original);
        Simplebuilding.getConfig().tools.invertBundleInteractions = false;
        helper.assertTrue(!Simplebuilding.getConfig().tools.invertBundleInteractions,
                "tools.invertBundleInteractions did not keep the value it was just set to; getConfig() is "
                        + "not handing out the live config object");
    }

    /**
     * Fills {@code container} to its brim with {@code filler} and answers how many items really
     * went in - counted from what the source stacks lost, not from the number of accepted calls,
     * because the last stack usually only fits partly. Bounded, so a container that never fills up
     * fails the test instead of hanging the run.
     */
    private static int fillWith(GameTestHelper helper, ServerPlayer player, ItemStack container, Item filler) {
        ReinforcedBundleItem item = bundleItem(container);
        int stackSize = new ItemStack(filler).getMaxStackSize();

        int inserted = 0;
        for (int attempt = 0; attempt < 64; attempt++) {
            ItemStack offered = new ItemStack(filler, stackSize);
            if (!item.tryInsertStackFromWorld(container, offered, player)) {
                helper.assertValueEqual(countIn(container, filler), inserted,
                        "items really stored in " + container.getItem() + " against the amount it reported "
                                + "taking - an insert answered true without storing everything");
                return inserted;
            }
            inserted += stackSize - offered.getCount();
        }
        helper.fail(container.getItem() + " never reported itself full");
        return inserted;
    }

    /**
     * Puts exactly {@code count} of {@code item} in, in stack-sized helpings, and fails the test if
     * any of it stayed outside - a setup step that has to be loud, because a half-filled bundle
     * would turn into a confusing assertion further down.
     */
    private static void insertExactly(GameTestHelper helper, ServerPlayer player, ItemStack container,
                                      Item item, int count) {
        int before = countIn(container, item);
        int stackSize = new ItemStack(item).getMaxStackSize();
        int left = count;
        while (left > 0) {
            ItemStack offered = new ItemStack(item, Math.min(stackSize, left));
            int offeredCount = offered.getCount();
            helper.assertTrue(bundleItem(container).tryInsertStackFromWorld(container, offered, player),
                    "test setup broken: " + container.getItem() + " refused " + offeredCount + " " + item);
            helper.assertValueEqual(offered.getCount(), 0,
                    "test setup broken: " + container.getItem() + " only took part of " + offeredCount + " "
                            + item);
            left -= offeredCount;
        }
        helper.assertValueEqual(countIn(container, item), before + count,
                "test setup broken: " + item + " inside " + container.getItem() + " after filling it");
    }

    /**
     * Measures what {@code container} holds, then checks the bar at half and at full, and the
     * capacity the tooltip hands the client. All three have to agree with the measurement.
     */
    private static void assertBarAndTooltip(GameTestHelper helper, ServerPlayer player, ItemStack container,
                                            String what) {
        int capacity = fillWith(helper, player, container.copy(), Items.STONE);
        helper.assertTrue(capacity % 2 == 0,
                "setup guard: " + what + " holds an odd " + capacity + " stone, so it cannot be filled to "
                        + "exactly half");

        ItemStack half = container.copy();
        insertExactly(helper, player, half, Items.STONE, capacity / 2);
        helper.assertValueEqual(half.getItem().getBarWidth(half), HALF_BAR,
                "bar width of " + what + " holding " + (capacity / 2) + " of its " + capacity + " items");

        ItemStack filled = container.copy();
        insertExactly(helper, player, filled, Items.STONE, capacity);
        helper.assertValueEqual(filled.getItem().getBarWidth(filled), FULL_BAR,
                "bar width of " + what + " filled to its brim with " + capacity + " items");

        Optional<TooltipComponent> image = filled.getItem().getTooltipImage(filled);
        helper.assertTrue(image.isPresent() && image.get() instanceof ReinforcedBundleTooltipData,
                "the tooltip of " + what + " is " + image + " instead of a ReinforcedBundleTooltipData");
        ReinforcedBundleTooltipData data = (ReinforcedBundleTooltipData) image.get();
        helper.assertValueEqual(data.maxCapacity(), capacity,
                "the capacity the tooltip of " + what + " hands the client, against the " + capacity
                        + " items it really takes");
    }

    /** Bar colour of a plain reinforced bundle holding exactly {@code stone} stone. */
    private static int barColourAt(GameTestHelper helper, ServerPlayer player, int stone) {
        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        insertExactly(helper, player, bundle, Items.STONE, stone);
        return bundle.getItem().getBarColor(bundle);
    }

    /**
     * Right clicks the top face of {@code anchor} with {@code bundle} in hand, after making sure
     * the anchor is solid and the space above it is free.
     */
    private static InteractionResult placeFrom(GameTestHelper helper, ServerPlayer player, ItemStack bundle,
                                               BlockPos anchor) {
        helper.setBlock(anchor, Blocks.STONE);
        helper.setBlock(anchor.above(), Blocks.AIR);
        player.setItemInHand(InteractionHand.MAIN_HAND, bundle);

        BlockPos pos = helper.absolutePos(anchor);
        Vec3 hit = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hit, Direction.UP, pos, false);
        return bundle.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult));
    }

    /**
     * Places {@code rounds} times out of the same bundle at the same spot and answers which block
     * came out each time. Used for the Color Palette, whose choice is a die roll and therefore
     * needs a run of placements to show at all.
     */
    private static List<Block> placeRepeatedly(GameTestHelper helper, ServerPlayer player, ItemStack bundle,
                                               BlockPos anchor, int rounds) {
        List<Block> placed = new ArrayList<>();
        for (int round = 0; round < rounds; round++) {
            InteractionResult result = placeFrom(helper, player, bundle, anchor);
            helper.assertTrue(result.consumesAction(),
                    "placement " + round + " out of the bundle answered " + result + " instead of building");
            placed.add(helper.getBlockState(anchor.above()).getBlock());
        }
        helper.setBlock(anchor.above(), Blocks.AIR);
        return placed;
    }

    private static ReinforcedBundleItem bundleItem(ItemStack stack) {
        return (ReinforcedBundleItem) stack.getItem();
    }

    private static BundleContents contentsOf(ItemStack container) {
        BundleContents contents = container.get(DataComponents.BUNDLE_CONTENTS);
        return contents == null ? BundleContents.EMPTY : contents;
    }

    /** The container's entries, in the order it keeps them - index 0 is the top of the pile. */
    private static List<ItemStack> entries(ItemStack container) {
        return contentsOf(container).itemCopyStream().toList();
    }

    private static int selectedIndex(ItemStack container) {
        return contentsOf(container).getSelectedItemIndex();
    }

    /**
     * How many distinct kinds the container holds, where "kind" is item <em>and</em> components -
     * the definition the Drawer restriction is meant to use. Deliberately not the same number as
     * {@code entries(container).size()}: 80 stone are one kind in two entries.
     */
    private static int kindsIn(ItemStack container) {
        List<ItemStack> distinct = new ArrayList<>();
        for (ItemStack stack : entries(container)) {
            boolean seen = false;
            for (ItemStack known : distinct) {
                if (ItemStack.isSameItemSameComponents(stack, known)) {
                    seen = true;
                    break;
                }
            }
            if (!seen) {
                distinct.add(stack);
            }
        }
        return distinct.size();
    }

    /**
     * One stone carrying {@code name}. Two of these are the same item and different kinds, which is
     * what separates a comparison by item from one by item and components.
     */
    private static ItemStack namedStone(String name) {
        ItemStack stack = new ItemStack(Items.STONE, 1);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    /** How many of {@code item} the container holds, across all of its entries. */
    private static int countIn(ItemStack container, Item item) {
        int total = 0;
        for (ItemStack stack : entries(container)) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Everything the container holds, whatever it is. */
    private static int totalIn(ItemStack container) {
        int total = 0;
        for (ItemStack stack : entries(container)) {
            total += stack.getCount();
        }
        return total;
    }

    /** The capacity the wiki export prints, in items, without ever building an {@code ItemStack}. */
    private static int baseCapacityItems(Item item) {
        return ((ReinforcedBundleItem) item).getBaseCapacityItems();
    }

    private static ItemStack enchanted(GameTestHelper helper, Item item, ResourceKey<Enchantment> key, int level) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(enchantment(helper, key), level);
        return stack;
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    /** A one-slot container holding {@code stack}, i.e. the slot an inventory click acts on. */
    private static Slot slotHolding(ItemStack stack) {
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, stack);
        return new Slot(container, 0, 0, 0);
    }

    /** The cursor an {@code overrideOtherStackedOnMe} click writes its leftovers back to. */
    private static SlotAccess cursorAccess(ItemStack onCursor) {
        ItemStack[] cursor = {onCursor};
        return SlotAccess.of(() -> cursor[0], stack -> cursor[0] = stack);
    }

    /**
     * Entities inside the test structure only. The bounds are deliberately <em>not</em> inflated:
     * the test structures stand a few blocks apart, and a widened box finds the neighbouring
     * test's entities, which makes the result depend on the order the suite runs in.
     */
    private static List<ItemEntity> droppedItems(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds());
    }

    private static int red(int colour) {
        return (colour >> 16) & 0xFF;
    }

    private static int green(int colour) {
        return (colour >> 8) & 0xFF;
    }
}
