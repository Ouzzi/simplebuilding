package com.simplebuilding.gametest;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.QuiverItem;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The quiver: the arrow-only bundle, its capacity table, and the bow that shoots out of it.
 *
 * <p>Before this class the quiver had exactly one test - "arrows go in, stone does not", through
 * {@code tryInsertStackFromWorld} ({@link ItemBehaviourTests#quiverTakesArrowsAndRefusesEverythingElse}).
 * Everything the item is actually <em>for</em> was unreachable from the suite: the two inventory
 * click paths, the deliberately dead {@code use}/{@code useOn}, the capacity override that takes
 * the bundle's 1.5x bonus away again, and the whole bow half - the search order over offhand,
 * chest, hotbar and backpack, the arrow that is consumed, and the {@code BowItemMixin} that wires
 * the two together. A port could have deleted any of it and the suite would have stayed green.
 *
 * <h2>Which player</h2>
 *
 * <p>Everything here needs a player that is <em>not</em> {@code instabuild}: the master builder
 * branch in {@code ReinforcedBundleItem#useOn} only pays for its block then, and
 * {@code BowItemMixin} refuses to take an arrow out of the quiver for a player with
 * {@code getAbilities().instabuild}. That flag is a plain public field, so {@link #mockPlayer}
 * flips it on the ordinary in-level mock, which keeps its connection - the bundle's insert and
 * remove sounds and the bow's shot sound all go through it. No quiver code reads
 * {@code isCreative()}, so the in-level mock's hard-wired {@code CREATIVE} game mode costs this
 * class nothing; see {@link ConsumptionAndDurabilityTests} for the cases where it does.
 *
 * <h2>Known defect</h2>
 *
 * <p><b>A quiver cannot be worn, so the chest slot stage of the bow search is out of reach in
 * normal play.</b> {@code QuiverItem#findProjectileForBow} and {@code #consumeProjectileForBow}
 * both read {@code player.getItemBySlot(EquipmentSlot.CHEST)} as their second step, but none of
 * the three quivers is registered with an {@code EQUIPPABLE} component ({@code ModItems}, the
 * {@code quiver}, {@code netherite_quiver} and {@code enderite_quiver} lines), and vanilla's
 * armour slot only accepts what {@code LivingEntity#isEquippableInSlot} answers for. In game the
 * player therefore has no way of putting a quiver on the chest short of {@code /item replace}, and
 * the "wear it" half of the feature does nothing. The tests below drive that stage all the same,
 * by writing the quiver into the slot directly - it is live code, and a port can still break it.
 *
 * <p><b>The Drawer capacity multiplier is twice what its own comment says.</b>
 * {@code ReinforcedBundleItem#getMaxCapacity} documents "Multiplikator = 1 + (level/8) =
 * (8+level)/8" and then computes {@code Fraction.getFraction(16 + level, 8)}; all four copies of
 * the formula ({@code getMaxCapacity} and {@code getMaxCapacityForVisuals} in
 * {@code ReinforcedBundleItem} and in {@code QuiverItem}) carry the same line. In game Drawer I
 * more than doubles a container instead of adding 12.5%: a plain quiver goes from 64 arrows to
 * 136, not to 72. The Drawer numbers below are marked "PINNED CURRENT BEHAVIOUR" for that reason -
 * they hold what the code produces today and go red when the formula is straightened out.
 *
 * <h2>Not covered, and why</h2>
 * <ul>
 *   <li><b>The loot table entries</b> ({@code ModLootTableModifications}: the quiver in the ancient
 *       city, the pillager outpost and the woodland mansion). {@code LootPool#entries} is private,
 *       so a recorder like the one in {@link ConfigOptionTests} can only count the pools the mod
 *       hands over, not read the items inside them - and rolling a weight-3 entry out of a vanilla
 *       chest table is statistics, not an assertion. What is testable there (the mod adds pools at
 *       all, and stops when the config switch is off) already is, in {@code ConfigOptionTests}.</li>
 *   <li><b>{@code stacksTo(1)} and {@code fireResistant()}</b> on the three quivers. Both are
 *       vanilla-evaluated item properties; a test on them would restate the registration line in
 *       {@code ModItems} and could only ever go red for that line.</li>
 *   <li><b>Item tag membership</b> ({@code bundle_enchantable},
 *       {@code constructors_touch_enchantable} - the enderite quiver is in neither) and the
 *       crafting and smithing recipes. Those are datapack claims evaluated by vanilla's enchanting
 *       and recipe machinery; the data layer of this mod is checked in {@link DataIntegrityTests}.</li>
 *   <li><b>Everything the player sees</b>: the tooltip submenu, the bar colour, the shot sound.
 *       Client side, or swallowed by the mock player's connection.</li>
 *   <li><b>The two capacity copies as such.</b> {@code QuiverItem#getMaxCapacity} and
 *       {@code QuiverItem#getMaxCapacityForVisuals} are line for line the same computation as the
 *       inherited ones - the only override that changes an outcome is {@code getBaseCapacity},
 *       which drops the bundle's 1.5x. Deleting either copy therefore changes nothing observable,
 *       and no test can claim otherwise.
 *       {@link #capacityDropsTheBundleBonusAndFollowsTierAndEnchantments} and
 *       {@link #barWidthFollowsTheSameCapacityTheFillingUses} pin the numbers the copies produce,
 *       which is what breaks when one of them is edited.</li>
 *   <li><b>The {@code usedQuiver} flag in {@code BowItemMixin}.</b> It is defensive only. The flag
 *       is set exactly when {@code findProjectileForBow} came back with something, and
 *       {@code consumeProjectileForBow} then walks the same slots in the same order under the same
 *       Constructor's Touch gate and touches nothing that is not a quiver. Deleting the flag and
 *       consuming unconditionally on a successful shot gives the identical result in every state
 *       the game can reach, so no assertion can hold it - the same situation as the two capacity
 *       copies above.</li>
 * </ul>
 */
public final class QuiverTests {

    private QuiverTests() {
    }

    /** Arrows a plain quiver holds: one vanilla stack, i.e. the bundle's 1.5x bonus dropped. */
    private static final int QUIVER_ARROWS = 64;

    /** Arrows the netherite quiver holds - tier factor 2. */
    private static final int NETHERITE_QUIVER_ARROWS = 128;

    /** Arrows the enderite quiver holds - tier factor 3. */
    private static final int ENDERITE_QUIVER_ARROWS = 192;

    /** Arrows a reinforced bundle holds: 1.5 stacks, the bonus the quiver gives up. */
    private static final int BUNDLE_ARROWS = 96;

    /** Full bar width vanilla draws, i.e. what a filled container has to report. */
    private static final int FULL_BAR = 13;

    /** Inventory slot outside the hotbar used for the "backpack" quiver. */
    private static final int BACKPACK_SLOT = 20;

    /** Highest Drawer level the enchantment data allows; the second Drawer case sits on it. */
    private static final int DRAWER_MAX_LEVEL = 8;

    /** Arrows a Drawer I quiver takes today - 64 x (16 + 1) / 8. See the known defect above. */
    private static final int DRAWER_1_ARROWS = 136;

    /** Arrows a Drawer VIII quiver takes today - 64 x (16 + 8) / 8. See the known defect above. */
    private static final int DRAWER_MAX_ARROWS = 192;

    // =====================================================================================
    // THE ITEM IN THE HAND
    // =====================================================================================

    /**
     * A quiver in the hand is inert: {@code use} and {@code useOn} both return {@code PASS} and
     * change nothing. Both overrides are two-liners that look deletable, and both hide a real
     * bundle behaviour underneath - the parent throws the selected stack on the ground on a right
     * click, and with Master Builder it places blocks out of itself. A quiver doing either would
     * spit the player's arrows onto the floor or turn into a building tool.
     *
     * <p>The control halves run the identical setup on the {@code REINFORCED_BUNDLE}, which proves
     * the setup can produce the behaviour at all: without them "nothing happened" would also pass
     * for a player standing in the wrong place or a hit result the block item rejects.
     *
     * <p>The Master Builder quiver is filled by writing {@code BUNDLE_CONTENTS} directly, because
     * no supported path puts stone into a quiver - the arrow filter is exactly what
     * {@link #arrowFilterHoldsForClicksAndTheInvertedBindingSlipsPastIt} is about. That is a
     * legitimate state all the same: a quiver that already holds blocks (from an older world, or
     * through the inverted binding pinned in that test) must still refuse to place them.
     *
     * <p>What breaks it: deleting {@code QuiverItem#use} or {@code QuiverItem#useOn}, or changing
     * either to fall through to {@code super}.
     */
    public static void rightClicksDoNothingEvenWithMasterBuilder(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper);

        // --- use(): a filled quiver must not throw an arrow out ---
        ItemStack quiver = filledContainer(helper, player, ModItems.QUIVER, Items.ARROW, 8);
        player.setItemInHand(InteractionHand.MAIN_HAND, quiver);
        InteractionResult quiverUse = quiver.getItem().use(level, player, InteractionHand.MAIN_HAND);

        helper.assertTrue(quiverUse == InteractionResult.PASS,
                "right clicking a filled quiver answered " + quiverUse + " instead of PASS");
        helper.assertValueEqual(countInBundle(quiver, Items.ARROW), 8,
                "arrows left in the quiver after a right click");
        helper.assertValueEqual(droppedItems(helper).size(), 0,
                "items lying in the room after right clicking the quiver");

        // --- control: the bundle it inherits from does throw the stack out ---
        ItemStack bundle = filledContainer(helper, player, ModItems.REINFORCED_BUNDLE, Items.ARROW, 8);
        player.setItemInHand(InteractionHand.MAIN_HAND, bundle);
        InteractionResult bundleUse = bundle.getItem().use(level, player, InteractionHand.MAIN_HAND);

        helper.assertTrue(bundleUse == InteractionResult.SUCCESS,
                "the reinforced bundle answered " + bundleUse + " instead of SUCCESS; the control half "
                        + "of this test no longer proves that a right click can empty a container item");
        helper.assertValueEqual(countInBundle(bundle, Items.ARROW), 0,
                "arrows left in the reinforced bundle after a right click");
        helper.assertValueEqual(droppedItems(helper).size(), 1,
                "items lying in the room after the reinforced bundle was right clicked");
        for (ItemEntity dropped : droppedItems(helper)) {
            dropped.discard();
        }

        // --- useOn(): Master Builder must not build out of a quiver ---
        BlockPos anchor = new BlockPos(5, 1, 5);
        helper.setBlock(anchor, Blocks.STONE);
        helper.assertBlockPresent(Blocks.AIR, anchor.above());

        ItemStack builderQuiver = enchanted(helper, ModItems.QUIVER, ModEnchantments.MASTER_BUILDER, 1);
        setContents(builderQuiver, new ItemStack(Items.STONE, 8));
        InteractionResult quiverPlace = useOn(helper, player, builderQuiver, anchor);

        helper.assertTrue(quiverPlace == InteractionResult.PASS,
                "clicking a block with a Master Builder quiver answered " + quiverPlace + " instead of PASS");
        helper.assertBlockPresent(Blocks.AIR, anchor.above());
        helper.assertValueEqual(countInBundle(builderQuiver, Items.STONE), 8,
                "stone left in the Master Builder quiver after it was clicked at a block");

        // --- control: the same stack on a reinforced bundle does build, and pays for it ---
        ItemStack builderBundle =
                enchanted(helper, ModItems.REINFORCED_BUNDLE, ModEnchantments.MASTER_BUILDER, 1);
        setContents(builderBundle, new ItemStack(Items.STONE, 8));
        useOn(helper, player, builderBundle, anchor);

        helper.assertBlockPresent(Blocks.STONE, anchor.above());
        helper.assertValueEqual(countInBundle(builderBundle, Items.STONE), 7,
                "stone left in the Master Builder bundle after it placed one block");

        helper.succeed();
    }

    // =====================================================================================
    // THE ARROW FILTER
    // =====================================================================================

    /**
     * The quiver's arrow filter guards three entry points, and it guards them unevenly. Both
     * inventory click paths ({@code overrideStackedOnOther} with the quiver on the cursor,
     * {@code overrideOtherStackedOnMe} with the quiver in the slot) check the item they are handed
     * - but only for {@link ClickAction#PRIMARY}, while the click that actually fills a bundle is
     * whatever {@code tools.invertBundleInteractions} says it is.
     *
     * <p>So the second half of this test is a <b>pinned finding, not a wish</b>: with the option
     * on, the insert click is {@code SECONDARY}, the filter's {@code PRIMARY} condition never
     * matches, and a player with the inverted binding can put stone - anything - into a quiver.
     * The assertion states today's behaviour so that a fix (a filter keyed to the configured insert
     * click, as {@code ReinforcedBundleItem#getInsertClick} computes it) shows up here as a red
     * test to be updated, instead of passing unnoticed.
     *
     * <p>The config option is restored in a {@code finally} and once more from
     * {@code runBeforeTestEnd}; see {@link ConfigOptionTests} for why that is enough to keep the
     * shared config from leaking into a neighbouring test.
     *
     * <p>Every case checks all three things one click leaves behind: whether the item reported the
     * click as handled, what is inside the quiver, and what is left outside it. The "handled" half
     * is the one that catches a filter turned into {@code return true}: the stone would still stay
     * out of the quiver, but the click would be eaten, and a player holding a quiver could no
     * longer pick up or swap the stack he clicked on.
     *
     * <p>What breaks it: deleting either filter override in {@code QuiverItem}, narrowing them to
     * one of the two click paths - the quiver would then take stone on the default binding too -
     * or answering {@code true} instead of {@code false} when the filter turns an item away.
     */
    public static void arrowFilterHoldsForClicksAndTheInvertedBindingSlipsPastIt(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        boolean original = Simplebuilding.getConfig().tools.invertBundleInteractions;
        helper.runBeforeTestEnd(() -> Simplebuilding.getConfig().tools.invertBundleInteractions = original);

        try {
            // --- default binding: left click inserts, and only arrows get through ---
            Simplebuilding.getConfig().tools.invertBundleInteractions = false;

            assertClick(helper, slotClick(player, ClickAction.PRIMARY, new ItemStack(Items.STONE, 8)), false,
                    "left clicking a quiver onto a slot holding stone");
            assertClick(helper, slotClick(player, ClickAction.PRIMARY, new ItemStack(Items.ARROW, 8)), true,
                    "left clicking a quiver onto a slot holding arrows");

            assertClick(helper, cursorClick(player, ClickAction.PRIMARY, new ItemStack(Items.STONE, 8)), false,
                    "left clicking stone onto a quiver");
            assertClick(helper, cursorClick(player, ClickAction.PRIMARY, new ItemStack(Items.ARROW, 8)), true,
                    "left clicking arrows onto a quiver");

            // --- inverted binding: the insert click moves to SECONDARY, the filter does not ---
            Simplebuilding.getConfig().tools.invertBundleInteractions = true;

            assertClick(helper, slotClick(player, ClickAction.SECONDARY, new ItemStack(Items.ARROW, 8)), true,
                    "right clicking a quiver onto a slot holding arrows with "
                            + "tools.invertBundleInteractions on");
            assertClick(helper, slotClick(player, ClickAction.SECONDARY, new ItemStack(Items.STONE, 8)), true,
                    "PINNED CURRENT BEHAVIOUR: with tools.invertBundleInteractions on the insert click is "
                            + "SECONDARY while QuiverItem's filter only inspects PRIMARY, so stone lands in the "
                            + "quiver. If the stone now stays outside, the filter was fixed - update this case");
            assertClick(helper, cursorClick(player, ClickAction.SECONDARY, new ItemStack(Items.STONE, 8)), true,
                    "PINNED CURRENT BEHAVIOUR: the same hole from the other side - stone on the cursor, right "
                            + "clicked onto the quiver with the inverted binding");
        } finally {
            Simplebuilding.getConfig().tools.invertBundleInteractions = original;
        }

        helper.succeed();
    }

    // =====================================================================================
    // CAPACITY
    // =====================================================================================

    /**
     * How much a quiver holds. Three claims in one, because they come out of the same two methods:
     * <ul>
     *   <li>a quiver does <b>not</b> get the bundle's 1.5x - it holds exactly one, two, three
     *       vanilla stacks by tier, where a reinforced bundle of the same tier holds 1.5;</li>
     *   <li>Drawer and Deep Pockets multiply that, with the same factors the bundle uses;</li>
     *   <li>{@code getBaseCapacityItems()} - the number the wiki export prints, computed without an
     *       {@code ItemStack} - agrees with what the player can actually push in.</li>
     * </ul>
     *
     * <p>Exact numbers are pinned here rather than the "grows with tier" ordering
     * {@link ItemBehaviourTests#bundleCapacityGrowsWithTierAndEnchantments} settles for, because
     * for the quiver the exact number <em>is</em> the behaviour: an ordering assertion cannot tell
     * 64 from 96, and 96 is precisely what a deleted {@code QuiverItem#getBaseCapacity} would
     * produce.
     *
     * <p>The enchanted cases use the plain quiver on purpose - the enderite quiver is in no
     * enchantable item tag, so a Drawer enderite quiver is not a state the game can reach.
     *
     * <p>Drawer is measured twice, on level 1 and on its highest level, because the two numbers
     * together pin the shape of the multiplier and not just one point of it. Both are pinned
     * <em>current</em> behaviour: the formula in the code is not the one its own comment describes,
     * see the known defect in the class Javadoc.
     *
     * <p>What breaks it: deleting {@code QuiverItem#getBaseCapacity} (every quiver would hold 1.5
     * stacks per tier), changing the tier table, dropping a factor from the Drawer or Deep Pockets
     * branch, or letting the wiki export drift away from the live formula.
     */
    public static void capacityDropsTheBundleBonusAndFollowsTierAndEnchantments(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        // Setup guard: the Deep Pockets case below enchants to level 2. ItemStack#enchant does not
        // clamp, so without this a shrunken max level would leave the number passing while the
        // level being measured had become unobtainable in game.
        helper.assertValueEqual(enchantment(helper, ModEnchantments.DEEP_POCKETS).value().getMaxLevel(), 2,
                "max level of Deep Pockets");

        // Same guard for the second Drawer case: its whole point is that it sits on the highest
        // level the enchantment can reach, so a shrunken max level has to fail here instead of
        // leaving a number passing for a level no player can get.
        helper.assertValueEqual(enchantment(helper, ModEnchantments.DRAWER).value().getMaxLevel(),
                DRAWER_MAX_LEVEL, "max level of Drawer");

        // --- tier, and the bonus the quiver gives up ---
        helper.assertValueEqual(fillWithArrows(helper, player, new ItemStack(ModItems.QUIVER)),
                QUIVER_ARROWS, "arrows a plain quiver takes");
        helper.assertValueEqual(fillWithArrows(helper, player, new ItemStack(ModItems.NETHERITE_QUIVER)),
                NETHERITE_QUIVER_ARROWS, "arrows a netherite quiver takes");
        helper.assertValueEqual(fillWithArrows(helper, player, new ItemStack(ModItems.ENDERITE_QUIVER)),
                ENDERITE_QUIVER_ARROWS, "arrows an enderite quiver takes");
        helper.assertValueEqual(fillWithArrows(helper, player, new ItemStack(ModItems.REINFORCED_BUNDLE)),
                BUNDLE_ARROWS, "arrows a reinforced bundle takes - the 1.5x the quiver gives up");

        // --- the same table again, through the stackless path the wiki export uses ---
        helper.assertValueEqual(baseCapacityItems(ModItems.QUIVER), QUIVER_ARROWS,
                "getBaseCapacityItems() of the quiver");
        helper.assertValueEqual(baseCapacityItems(ModItems.NETHERITE_QUIVER), NETHERITE_QUIVER_ARROWS,
                "getBaseCapacityItems() of the netherite quiver");
        helper.assertValueEqual(baseCapacityItems(ModItems.ENDERITE_QUIVER), ENDERITE_QUIVER_ARROWS,
                "getBaseCapacityItems() of the enderite quiver");
        helper.assertValueEqual(baseCapacityItems(ModItems.REINFORCED_BUNDLE), BUNDLE_ARROWS,
                "getBaseCapacityItems() of the reinforced bundle");

        // --- enchantments, on the tier that can actually carry them ---
        helper.assertValueEqual(
                fillWithArrows(helper, player, enchanted(helper, ModItems.QUIVER, ModEnchantments.DRAWER, 1)),
                DRAWER_1_ARROWS,
                "PINNED CURRENT BEHAVIOUR: arrows a Drawer I quiver takes. The multiplier the code applies is "
                        + "(16 + level) / 8, although the comment right above the line in "
                        + "ReinforcedBundleItem#getMaxCapacity documents (8 + level) / 8. If that is "
                        + "straightened out the number here is 72 - update this case");
        helper.assertValueEqual(
                fillWithArrows(helper, player,
                        enchanted(helper, ModItems.QUIVER, ModEnchantments.DRAWER, DRAWER_MAX_LEVEL)),
                DRAWER_MAX_ARROWS,
                "PINNED CURRENT BEHAVIOUR: arrows a Drawer VIII quiver takes - 64 x (16 + 8) / 8. With the "
                        + "documented (8 + level) / 8 this would be 128");
        helper.assertValueEqual(
                fillWithArrows(helper, player, enchanted(helper, ModItems.QUIVER, ModEnchantments.DEEP_POCKETS, 1)),
                128, "arrows a Deep Pockets I quiver takes - 64 x 2");
        helper.assertValueEqual(
                fillWithArrows(helper, player, enchanted(helper, ModItems.QUIVER, ModEnchantments.DEEP_POCKETS, 2)),
                256, "arrows a Deep Pockets II quiver takes - 64 x 4");

        helper.succeed();
    }

    /**
     * The durability-style bar under the quiver is drawn from a <em>second</em> capacity formula:
     * {@code getMaxCapacityForVisuals} reads the enchantments straight off the stack and matches
     * their ids by substring, where the filling path resolves them through the enchantment
     * registry. Two formulas for one number is exactly the pairing that drifts apart, and the
     * player sees the drift as a bar that is full at half a quiver, or never fills up at all.
     *
     * <p>{@code getBarWidth} itself is server reachable ({@code Item#getBarWidth} is plain
     * arithmetic on the stack); only the drawing is client side.
     *
     * <p>Both directions of the drift are covered: 64 arrows in every variant pins the widths the
     * enchantment branches have to produce, and a Drawer I quiver filled to its brim has to report
     * a full bar - which is the assertion that catches a visual capacity that grew <em>larger</em>
     * than the real one, because that direction is clamped to 13 in the fixed fill case.
     *
     * <p>Drawer appears twice for the same reason as in
     * {@link #capacityDropsTheBundleBonusAndFollowsTierAndEnchantments}: with one level only, the
     * two formulas could disagree everywhere above it and nothing here would notice. Both widths
     * follow the capacity the code computes today, not the one its comment documents - see the
     * known defect in the class Javadoc.
     *
     * <p>What breaks it: dropping an enchantment branch from the visuals formula, renaming an
     * enchantment so the substring match stops matching (the filling path would keep working -
     * that is the split this test exists for), or letting the visuals use the bundle base capacity.
     */
    public static void barWidthFollowsTheSameCapacityTheFillingUses(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        // --- one vanilla stack in each variant: a plain quiver is full, the others are not ---
        helper.assertValueEqual(barWidthWith(helper, player, new ItemStack(ModItems.QUIVER), 64), FULL_BAR,
                "bar width of a quiver holding 64 arrows (its whole capacity)");
        helper.assertValueEqual(
                barWidthWith(helper, player,
                        enchanted(helper, ModItems.QUIVER, ModEnchantments.DEEP_POCKETS, 1), 64), 7,
                "bar width of a Deep Pockets I quiver holding 64 of its 128 arrows");
        helper.assertValueEqual(
                barWidthWith(helper, player,
                        enchanted(helper, ModItems.QUIVER, ModEnchantments.DEEP_POCKETS, 2), 64), 3,
                "bar width of a Deep Pockets II quiver holding 64 of its 256 arrows");
        helper.assertValueEqual(
                barWidthWith(helper, player,
                        enchanted(helper, ModItems.QUIVER, ModEnchantments.DRAWER, 1), 64), 6,
                "bar width of a Drawer I quiver holding 64 of its " + DRAWER_1_ARROWS + " arrows");
        helper.assertValueEqual(
                barWidthWith(helper, player,
                        enchanted(helper, ModItems.QUIVER, ModEnchantments.DRAWER, DRAWER_MAX_LEVEL), 64), 4,
                "bar width of a Drawer VIII quiver holding 64 of its " + DRAWER_MAX_ARROWS + " arrows");
        helper.assertValueEqual(
                barWidthWith(helper, player, new ItemStack(ModItems.REINFORCED_BUNDLE), 64), 9,
                "bar width of a reinforced bundle holding 64 of its 96 arrows - the quiver's 13 next to "
                        + "this one is the 1.5x bonus the quiver does not get");

        // --- and a quiver that refuses one more arrow has to show a full bar ---
        ItemStack drawer = enchanted(helper, ModItems.QUIVER, ModEnchantments.DRAWER, 1);
        int filled = fillWithArrows(helper, player, drawer);
        helper.assertValueEqual(drawer.getItem().getBarWidth(drawer), FULL_BAR,
                "bar width of a Drawer I quiver that took " + filled + " arrows and then refused more");

        helper.succeed();
    }

    // =====================================================================================
    // THE BOW HALF
    // =====================================================================================

    /**
     * Where the bow looks for a quiver, and which arrow it picks out of it. The search runs
     * offhand, chest slot, hotbar, and only then the rest of the inventory - and that last step is
     * gated on Constructor's Touch, which is the quiver's "reach into the backpack" feature.
     *
     * <p>Four quivers are laid out at once, each holding a different arrow type, so every step
     * proves it stopped at the right one rather than merely finding something. The offhand quiver
     * holds two types with the tipped arrows stacked in last: the bow has to hand back the topmost
     * stack, which is what the player sees at the front of the quiver.
     *
     * <p>What breaks it: reordering the search (the chest slot ahead of the offhand, say), dropping
     * a step, having {@code findFirstArrow} return the last stack instead of the first, or losing
     * the {@code isRemoteQuiver} gate - a quiver in the backpack would then feed the bow without
     * the enchantment, and Constructor's Touch on a quiver would be worth nothing.
     */
    public static void bowTakesTheTopmostArrowAndSearchesOffhandChestHotbarThenBackpack(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        ItemStack offhand = filledContainer(helper, player, ModItems.QUIVER, Items.ARROW, 8);
        insertArrows(helper, player, offhand, Items.TIPPED_ARROW, 8);
        ItemStack chest = filledContainer(helper, player, ModItems.QUIVER, Items.SPECTRAL_ARROW, 8);
        ItemStack hotbar = filledContainer(helper, player, ModItems.QUIVER, Items.ARROW, 8);
        ItemStack backpack = filledContainer(helper, player, ModItems.QUIVER, Items.TIPPED_ARROW, 8);

        player.setItemInHand(InteractionHand.OFF_HAND, offhand);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        player.getInventory().setItem(3, hotbar);
        player.getInventory().setItem(BACKPACK_SLOT, backpack);

        // --- offhand wins, and inside it the stack that went in last ---
        assertBowFinds(helper, player, Items.TIPPED_ARROW,
                "with quivers in the offhand, the chest slot, the hotbar and the backpack");
        helper.assertValueEqual(countInBundle(offhand, Items.ARROW), 8,
                "plain arrows still under the tipped ones in the offhand quiver - if they are gone, the "
                        + "'topmost stack' half of this test proves nothing");

        // --- chest slot is next ---
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        assertBowFinds(helper, player, Items.SPECTRAL_ARROW, "with the offhand quiver taken away");

        // --- then the hotbar ---
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        assertBowFinds(helper, player, Items.ARROW, "with only the hotbar and backpack quivers left");

        // --- and the backpack only with Constructor's Touch ---
        player.getInventory().setItem(3, ItemStack.EMPTY);
        ItemStack found = QuiverItem.findProjectileForBow(player);
        helper.assertTrue(found.isEmpty(),
                "a quiver in inventory slot " + BACKPACK_SLOT + " fed the bow " + found
                        + " without Constructor's Touch");

        backpack.enchant(enchantment(helper, ModEnchantments.CONSTRUCTORS_TOUCH), 1);
        assertBowFinds(helper, player, Items.TIPPED_ARROW,
                "with Constructor's Touch on the quiver in inventory slot " + BACKPACK_SLOT);

        helper.succeed();
    }

    /**
     * What a shot costs: exactly one arrow, out of the very quiver the arrow was found in. The
     * consuming walk repeats the search order in a second, independent method, so the two can drift
     * - a bow that draws from the offhand and charges the hotbar is a duplication bug, not a typo,
     * and the player would only notice by counting arrows.
     *
     * <p>The first case narrows that from "the same quiver" to "the same stack". A quiver holding
     * two arrow types is the only setup in which the two methods can be told apart at all:
     * {@code findFirstArrow} hands the bow the topmost arrow stack, and {@code tryConsumeArrow}
     * has to shrink that very stack. If one of them ever iterates the other way round, or prefers a
     * type, the player shoots a tipped arrow and pays with a plain one - a swap no counter over the
     * whole quiver could see.
     *
     * <p>Emptying the offhand quiver completely also covers the stack bookkeeping: the arrow stack
     * has to disappear from the contents when its last arrow is spent, otherwise an empty stack
     * would sit at the front of the quiver and every further shot would find nothing.
     *
     * <p>The chest slot is walked here as the second step, with an arrow type of its own so it
     * cannot be confused with the offhand or hotbar quiver. That stage is live code but, as the
     * class Javadoc records, not reachable in normal play - a quiver has no {@code EQUIPPABLE}
     * component, so only a command can put one there. The test writes it into the slot directly.
     *
     * <p>What breaks it: consuming from the first quiver found instead of the one the search
     * returned, consuming out of a different stack than the search offered, consuming more than one
     * arrow, leaving an empty stack behind, dropping the chest slot from the consuming walk, or
     * dropping the Constructor's Touch gate on the backpack half of it.
     */
    public static void bowConsumesOneArrowFromTheQuiverThatSuppliedIt(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        // --- the stack the search handed over is the stack that pays ---
        ItemStack mixed = filledContainer(helper, player, ModItems.QUIVER, Items.ARROW, 8);
        insertArrows(helper, player, mixed, Items.TIPPED_ARROW, 8);
        player.setItemInHand(InteractionHand.OFF_HAND, mixed);

        ItemStack supplied = QuiverItem.findProjectileForBow(player);
        helper.assertTrue(supplied.is(Items.TIPPED_ARROW),
                "test setup broken: the search handed back " + supplied + " instead of the tipped arrows that "
                        + "went into the quiver last, so the two halves of this case cannot be compared");
        QuiverItem.consumeProjectileForBow(player);
        helper.assertValueEqual(countInBundle(mixed, Items.TIPPED_ARROW), 7,
                "tipped arrows left in the quiver - findFirstArrow offered the bow this stack, so this is the "
                        + "stack that has to pay");
        helper.assertValueEqual(countInBundle(mixed, Items.ARROW), 8,
                "plain arrows lying under the tipped ones; if this is 7, tryConsumeArrow picked a different "
                        + "stack than findFirstArrow did and the player shoots one arrow while paying another");

        // --- offhand, chest slot, hotbar: the walk pays in that order ---
        ItemStack offhand = filledContainer(helper, player, ModItems.QUIVER, Items.ARROW, 8);
        ItemStack chest = filledContainer(helper, player, ModItems.QUIVER, Items.SPECTRAL_ARROW, 8);
        ItemStack hotbar = filledContainer(helper, player, ModItems.QUIVER, Items.ARROW, 8);
        player.setItemInHand(InteractionHand.OFF_HAND, offhand);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        player.getInventory().setItem(4, hotbar);

        QuiverItem.consumeProjectileForBow(player);
        helper.assertValueEqual(countInBundle(offhand, Items.ARROW), 7,
                "arrows left in the offhand quiver after one shot");
        helper.assertValueEqual(countInBundle(chest, Items.SPECTRAL_ARROW), 8,
                "arrows left in the chest quiver - the offhand one is walked before it");
        helper.assertValueEqual(countInBundle(hotbar, Items.ARROW), 8,
                "arrows left in the hotbar quiver - the offhand one supplied the shot and has to pay for it");

        // --- empty the offhand quiver: the spent stack has to disappear, not linger at count 0 ---
        for (int shot = 0; shot < 7; shot++) {
            QuiverItem.consumeProjectileForBow(player);
        }
        helper.assertValueEqual(countInBundle(offhand, Items.ARROW), 0,
                "arrows left in the offhand quiver after eight shots");
        helper.assertTrue(contentsOf(offhand).isEmpty(),
                "the offhand quiver still carries " + contentsOf(offhand).size()
                        + " stack(s) after its last arrow was spent");
        helper.assertValueEqual(countInBundle(chest, Items.SPECTRAL_ARROW), 8,
                "arrows left in the chest quiver while the offhand one still had arrows");
        helper.assertValueEqual(countInBundle(hotbar, Items.ARROW), 8,
                "arrows left in the hotbar quiver while the offhand one still had arrows");

        // --- the chest quiver is next, and it comes before the hotbar ---
        QuiverItem.consumeProjectileForBow(player);
        helper.assertValueEqual(countInBundle(chest, Items.SPECTRAL_ARROW), 7,
                "arrows left in the chest quiver after the emptied offhand quiver handed the shot on to it");
        helper.assertValueEqual(countInBundle(hotbar, Items.ARROW), 8,
                "arrows left in the hotbar quiver - the chest slot is walked before it, so it must not pay "
                        + "while the chest quiver still has arrows");

        // --- the hotbar quiver pays once neither of the two above can ---
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        QuiverItem.consumeProjectileForBow(player);
        helper.assertValueEqual(countInBundle(hotbar, Items.ARROW), 7,
                "arrows left in the hotbar quiver after it had to supply the shot");

        // --- the backpack pays only with Constructor's Touch ---
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.getInventory().setItem(4, ItemStack.EMPTY);
        ItemStack backpack = filledContainer(helper, player, ModItems.QUIVER, Items.ARROW, 8);
        player.getInventory().setItem(BACKPACK_SLOT, backpack);

        QuiverItem.consumeProjectileForBow(player);
        helper.assertValueEqual(countInBundle(backpack, Items.ARROW), 8,
                "a quiver in inventory slot " + BACKPACK_SLOT + " paid for a shot without Constructor's Touch");

        backpack.enchant(enchantment(helper, ModEnchantments.CONSTRUCTORS_TOUCH), 1);
        QuiverItem.consumeProjectileForBow(player);
        helper.assertValueEqual(countInBundle(backpack, Items.ARROW), 7,
                "arrows left in the Constructor's Touch quiver in inventory slot " + BACKPACK_SLOT);

        helper.succeed();
    }

    /**
     * The whole feature end to end, through vanilla's own {@code BowItem}: a player with a bow, a
     * quiver and not a single loose arrow draws and shoots. Both halves of {@code BowItemMixin} are
     * on this path - the redirect in {@code use} that decides whether the bow may be drawn at all,
     * and the redirect plus the {@code RETURN} inject in {@code releaseUsing} that shoot the arrow
     * and then bill the quiver for it.
     *
     * <p>The refusal control comes first: with no quiver and no arrows, the call has to answer
     * {@code FAIL}. That is what makes the quiver shot meaningful - vanilla alone finds no
     * ammunition for this player, so a drawn bow can only come from the mixin.
     *
     * <p>Both redirects have a second branch, and two cases run before the quiver appears to cover
     * them. With loose arrows and no quiver the bow has to behave exactly as vanilla does: that is
     * the {@code return player.getProjectile(stack)} fall-back at the end of each redirect, and
     * deleting it would stop every bow in the game for every player who is not carrying a quiver -
     * a much larger breakage than anything else this class measures, and one the quiver-only cases
     * cannot see, because they expect a refusal for an empty inventory anyway. With loose arrows
     * <em>and</em> a quiver, the quiver has to pay while the loose arrows are left untouched; that
     * is the priority the mixin's own comment claims, and nothing else in the suite measures it.
     *
     * <p>Three things are asserted per shot: the bow reports a shot, exactly one arrow entity is in
     * the room, and the quiver is one arrow lighter. The creative half then shows the guard on the
     * inject - vanilla cannot protect the quiver there, because vanilla knows nothing about it, so
     * "creative shoots for free" is entirely the mod's own doing.
     *
     * <p>The last case is a <b>pinned finding</b>: Infinity is not consulted anywhere in the mixin,
     * so an infinity bow shooting out of a quiver still eats an arrow, where the same bow shooting
     * out of the inventory does not. The assertion states that; if the mixin learns about Infinity
     * it turns red and gets updated.
     *
     * <p>What breaks it: removing either redirect (the bow would refuse to draw, or shoot vanilla's
     * "no ammunition"), dropping either redirect's vanilla fall-back (bows without a quiver would
     * stop working), asking {@code player.getProjectile} before the quiver (loose arrows would be
     * spent first), removing the inject (quiver arrows would become infinite), or dropping the
     * {@code instabuild} guard (creative would eat the quiver). The {@code usedQuiver} flag is
     * deliberately not on this list - see the class Javadoc for why no assertion can hold it.
     */
    public static void bowShootsFromTheQuiverAndBillsItOutsideCreativeOnly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = mockPlayer(helper);

        ItemStack bow = new ItemStack(Items.BOW);
        ItemStack quiver = filledContainer(helper, player, ModItems.QUIVER, Items.ARROW, 8);
        player.setItemInHand(InteractionHand.MAIN_HAND, bow);

        // Setup guard: nothing the bow could shoot may be lying in the inventory, or vanilla would
        // find ammunition on its own and the refusal control below would say nothing.
        helper.assertValueEqual(player.getInventory().countItem(Items.ARROW), 0,
                "loose arrows in the inventory");

        // --- control: no quiver, no arrows, no draw ---
        InteractionResult withNothing = bow.getItem().use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(withNothing == InteractionResult.FAIL,
                "a survival player with an empty inventory could draw the bow anyway (" + withNothing
                        + "); the quiver shots below would then prove nothing");

        // --- the vanilla fall-back: loose arrows, no quiver, and the bow works as it always did ---
        player.getInventory().add(new ItemStack(Items.ARROW, 8));
        InteractionResult withLooseArrows = bow.getItem().use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(withLooseArrows != InteractionResult.FAIL,
                "a player with eight loose arrows and no quiver could not draw the bow (" + withLooseArrows
                        + "); BowItemMixin lost its fall-back to player.getProjectile, which takes every bow "
                        + "in the game away from every player without a quiver");
        helper.assertTrue(releaseBow(helper, player, bow),
                "the bow reported no shot although eight loose arrows were lying in the inventory");
        helper.assertValueEqual(flyingArrows(helper).size(), 1,
                "arrows flying in the room after the shot fed from the inventory");
        helper.assertValueEqual(player.getInventory().countItem(Items.ARROW), 7,
                "loose arrows left after a shot vanilla itself paid for");
        discardArrows(helper);

        // --- priority: with both at hand the quiver pays and the loose arrows are left alone ---
        player.getInventory().add(new ItemStack(Items.ARROW, 1));
        helper.assertValueEqual(player.getInventory().countItem(Items.ARROW), 8,
                "test setup broken: loose arrows in the inventory before the priority shot");
        player.setItemInHand(InteractionHand.OFF_HAND, quiver);

        InteractionResult withBoth = bow.getItem().use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(withBoth != InteractionResult.FAIL,
                "the bow refused to be drawn with a quiver and loose arrows both at hand");
        helper.assertTrue(releaseBow(helper, player, bow),
                "the bow reported no shot with a quiver and loose arrows both at hand");
        helper.assertValueEqual(flyingArrows(helper).size(), 1,
                "arrows flying in the room after the priority shot");
        helper.assertValueEqual(countInBundle(quiver, Items.ARROW), 7,
                "arrows left in the quiver - it is asked before player.getProjectile, so it pays");
        helper.assertValueEqual(player.getInventory().countItem(Items.ARROW), 8,
                "loose arrows left after the priority shot; if this is 7 the redirect asked "
                        + "player.getProjectile first and the quiver has lost its priority");
        discardArrows(helper);

        // From here on every shot has to come out of the quiver, so the loose arrows go away again.
        // clearContent() empties the hands along with everything else, hence both are handed back.
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, bow);
        player.setItemInHand(InteractionHand.OFF_HAND, quiver);
        helper.assertValueEqual(player.getInventory().countItem(Items.ARROW), 0,
                "loose arrows left over from the fall-back cases");

        // --- with the quiver alone the bow draws ---
        InteractionResult withQuiver = bow.getItem().use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(withQuiver != InteractionResult.FAIL,
                "the bow refused to be drawn although the offhand quiver held arrows");
        helper.assertTrue(player.isUsingItem(),
                "the bow was not put into use although it accepted the click");

        // --- and shooting takes exactly one arrow out of that quiver ---
        helper.assertTrue(releaseBow(helper, player, bow),
                "the bow reported no shot although the offhand quiver held arrows");
        helper.assertValueEqual(flyingArrows(helper).size(), 1, "arrows flying in the room after one shot");
        helper.assertValueEqual(countInBundle(quiver, Items.ARROW), 6,
                "arrows left in the quiver after the survival shot fed by the quiver alone");
        discardArrows(helper);

        // --- creative shoots the same arrow for free ---
        player.getAbilities().instabuild = true;
        bow.getItem().use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(releaseBow(helper, player, bow),
                "a creative player could not shoot from the quiver");
        helper.assertValueEqual(flyingArrows(helper).size(), 1,
                "arrows flying in the room after the creative shot");
        helper.assertValueEqual(countInBundle(quiver, Items.ARROW), 6,
                "arrows left in the quiver after a creative shot - creative must not pay");
        discardArrows(helper);
        player.getAbilities().instabuild = false;

        // --- Infinity does not spare the quiver (pinned) ---
        ItemStack infinityBow = new ItemStack(Items.BOW);
        infinityBow.enchant(enchantment(helper, Enchantments.INFINITY), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, infinityBow);
        infinityBow.getItem().use(level, player, InteractionHand.MAIN_HAND);

        helper.assertTrue(releaseBow(helper, player, infinityBow),
                "an Infinity bow could not shoot from the quiver");
        // The arrow is counted here for the same reason as in the two cases above: BowItem's
        // releaseUsing answers true even when draw() came back empty - the jump around the empty
        // list skips shoot(), the sound and the statistic, and the method still returns 1. Without
        // this line a quiver billed for a shot that never left the bow would pass unnoticed.
        helper.assertValueEqual(flyingArrows(helper).size(), 1,
                "arrows flying in the room after the Infinity shot");
        helper.assertValueEqual(countInBundle(quiver, Items.ARROW), 5,
                "PINNED CURRENT BEHAVIOUR: BowItemMixin never looks at Infinity, so an Infinity bow spends a "
                        + "quiver arrow all the same. If this is now 6 the mixin learned about the enchantment "
                        + "- update this assertion");
        discardArrows(helper);

        helper.succeed();
    }

    // =====================================================================================
    // THE EXPLOSION BOUNDARY
    // =====================================================================================

    /**
     * {@code ItemEntityMixin#ignoreExplosion} makes exactly one item survive a blast: the netherite
     * bundle. The quivers are not on that list, not even the netherite one, and this pins that
     * boundary from both sides - the immunity really works, and it really is limited to the one
     * item named in the mixin.
     *
     * <p>The netherite quiver is the right control precisely because it is fireproof like the
     * bundle: if it survived here, the reason could only be the explosion hook, not its fire
     * resistance.
     *
     * <p>What breaks it: deleting the mixin (the bundle would burn with everything else), or
     * widening its condition to {@code ReinforcedBundleItem} or to the quivers, which would hand
     * the quiver an immunity the mod never granted it.
     */
    public static void netheriteQuiverBurnsInAnExplosionWhileTheNetheriteBundleSurvives(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // Setup guard: vanilla's ItemEntity ignores every explosion while mobGriefing is off, so
        // without this the test would "pass" with both items alive and prove nothing at all.
        helper.assertTrue(level.getGameRules().get(GameRules.MOB_GRIEFING),
                "test setup broken: mobGriefing is off, so vanilla's ItemEntity#ignoreExplosion returns true "
                        + "for every item and the mixin's own condition is never reached");

        ItemEntity bundle = helper.spawnItem(ModItems.NETHERITE_BUNDLE, 3.5F, 2.0F, 3.5F);
        ItemEntity quiver = helper.spawnItem(ModItems.NETHERITE_QUIVER, 4.5F, 2.0F, 3.5F);
        helper.runBeforeTestEnd(bundle::discard);
        helper.runBeforeTestEnd(quiver::discard);

        Vec3 centre = helper.absoluteVec(new Vec3(4.0, 2.0, 3.5));
        level.explode(null, centre.x, centre.y, centre.z, 3.0F, Level.ExplosionInteraction.NONE);

        helper.assertTrue(bundle.isAlive(),
                "the netherite bundle was destroyed by an explosion; ItemEntityMixin no longer shields it");
        helper.assertTrue(quiver.isRemoved(),
                "the netherite quiver survived an explosion that killed the item next to it; the explosion "
                        + "immunity is meant to be the netherite bundle's alone");

        helper.succeed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /**
     * The usual in-level mock player, moved into the room, with an empty inventory and
     * {@code instabuild} switched off - the flag the master builder branch and
     * {@code BowItemMixin} read. It keeps its connection, which the bundle sounds and the bow's
     * shot sound go through, and it is handed back to the player list at the end of the test.
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

    /** Right clicks the top face of a block, dead centre, with {@code stack} in the main hand. */
    private static InteractionResult useOn(GameTestHelper helper, ServerPlayer player, ItemStack stack,
                                           BlockPos relativePos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = helper.absolutePos(relativePos);
        Vec3 hit = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hit, Direction.UP, pos, false);
        return stack.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult));
    }

    /**
     * Releases a fully drawn bow. The draw time is taken from the item rather than hard coded, so
     * this stays a "full charge" even if vanilla moves the number.
     */
    private static boolean releaseBow(GameTestHelper helper, ServerPlayer player, ItemStack bow) {
        int drawTicks = bow.getItem().getUseDuration(bow, player) - 20;
        boolean shot = bow.getItem().releaseUsing(bow, helper.getLevel(), player, drawTicks);
        // The game ends the use itself after releaseUsing. Without this the player would still
        // count as using an item at the next case, and "the bow was put into use" would be an
        // assertion that can no longer fail.
        player.stopUsingItem();
        return shot;
    }

    private static void assertBowFinds(GameTestHelper helper, ServerPlayer player, Item expected, String what) {
        ItemStack found = QuiverItem.findProjectileForBow(player);
        helper.assertTrue(found.is(expected),
                what + ": the bow was handed " + found + " instead of " + expected);
    }

    /**
     * Everything one inventory click leaves behind: the {@code boolean} the {@code override*}
     * method hands back to the container screen, what is inside the quiver afterwards, and how much
     * of the eight-item source stack is still outside it. The return value is carried along because
     * a filter that answers {@code true} instead of {@code false} keeps the item out of the quiver
     * just the same - only {@code handled} shows that it swallowed the click on the way out.
     */
    private record ClickOutcome(boolean handled, int inQuiver, int leftOutside) {
    }

    /**
     * Checks one click outcome against the eight items it started with; {@code expectInsert} says
     * whether this click is the one that is meant to move them into the quiver.
     */
    private static void assertClick(GameTestHelper helper, ClickOutcome outcome, boolean expectInsert,
                                    String what) {
        if (expectInsert) {
            helper.assertTrue(outcome.handled(),
                    what + ": the click filled the quiver but was reported as not handled");
            helper.assertValueEqual(outcome.inQuiver(), 8, what + ": items inside the quiver");
            helper.assertValueEqual(outcome.leftOutside(), 0, what + ": items left outside the quiver");
        } else {
            helper.assertTrue(!outcome.handled(),
                    what + ": the filter kept the item out but reported the click as handled, which eats it "
                            + "- a player holding a quiver could no longer pick that stack up or swap it");
            helper.assertValueEqual(outcome.inQuiver(), 0, what + ": items inside the quiver");
            helper.assertValueEqual(outcome.leftOutside(), 8, what + ": items left outside the quiver");
        }
    }

    /** One click with the quiver on the cursor onto a slot holding {@code inSlot}. */
    private static ClickOutcome slotClick(ServerPlayer player, ClickAction action, ItemStack inSlot) {
        ItemStack quiver = new ItemStack(ModItems.QUIVER);
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, inSlot);
        Slot slot = new Slot(container, 0, 0, 0);

        boolean handled =
                ((ReinforcedBundleItem) quiver.getItem()).overrideStackedOnOther(quiver, slot, action, player);
        return new ClickOutcome(handled, totalInBundle(quiver), slot.getItem().getCount());
    }

    /**
     * The same click from the other side: {@code onCursor} clicked onto a quiver lying in a slot.
     * That is the path the player uses most, and it is a second copy of the filter.
     */
    private static ClickOutcome cursorClick(ServerPlayer player, ClickAction action, ItemStack onCursor) {
        ItemStack quiver = new ItemStack(ModItems.QUIVER);
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, quiver);
        Slot slot = new Slot(container, 0, 0, 0);
        ItemStack[] cursorSlot = {onCursor};

        boolean handled = ((ReinforcedBundleItem) quiver.getItem()).overrideOtherStackedOnMe(quiver, onCursor,
                slot, action, player, SlotAccess.of(() -> cursorSlot[0], stack -> cursorSlot[0] = stack));
        return new ClickOutcome(handled, totalInBundle(quiver), cursorSlot[0].getCount());
    }

    /**
     * Pushes 64-arrow stacks into a fresh container until it refuses one, and answers how many
     * arrows really went in - counted from what the source stacks lost, not from the number of
     * accepted calls, because the last stack usually only fits partly. Bounded, so a container that
     * never fills up fails the test instead of hanging the run.
     */
    private static int fillWithArrows(GameTestHelper helper, ServerPlayer player, ItemStack container) {
        ReinforcedBundleItem item = (ReinforcedBundleItem) container.getItem();

        int inserted = 0;
        for (int attempt = 0; attempt < 64; attempt++) {
            ItemStack arrows = new ItemStack(Items.ARROW, 64);
            if (!item.tryInsertStackFromWorld(container, arrows, player)) {
                helper.assertValueEqual(countInBundle(container, Items.ARROW), inserted,
                        "arrows really stored in " + container.getItem() + " versus the amount it reported "
                                + "taking - an insert answered true without storing everything");
                return inserted;
            }
            inserted += 64 - arrows.getCount();
        }
        helper.fail(container.getItem() + " never reported itself full");
        return inserted;
    }

    /** Bar width of {@code container} once {@code arrows} plain arrows have been put in it. */
    private static int barWidthWith(GameTestHelper helper, ServerPlayer player, ItemStack container, int arrows) {
        insertArrows(helper, player, container, Items.ARROW, arrows);
        return container.getItem().getBarWidth(container);
    }

    private static ItemStack filledContainer(GameTestHelper helper, ServerPlayer player, Item containerItem,
                                             Item arrow, int count) {
        ItemStack container = new ItemStack(containerItem);
        insertArrows(helper, player, container, arrow, count);
        return container;
    }

    /** Fills through the world pickup path, i.e. the one entry point that carries no config gate. */
    private static void insertArrows(GameTestHelper helper, ServerPlayer player, ItemStack container,
                                     Item arrow, int count) {
        int before = countInBundle(container, arrow);
        ItemStack arrows = new ItemStack(arrow, count);
        helper.assertTrue(
                ((ReinforcedBundleItem) container.getItem()).tryInsertStackFromWorld(container, arrows, player),
                "test setup broken: " + container.getItem() + " refused " + count + " " + arrow);
        helper.assertValueEqual(countInBundle(container, arrow), before + count,
                "test setup broken: " + arrow + " inside " + container.getItem() + " after filling it");
    }

    private static void setContents(ItemStack container, ItemStack... stacks) {
        List<ItemStackTemplate> templates = new ArrayList<>();
        for (ItemStack stack : stacks) {
            templates.add(ItemStackTemplate.fromNonEmptyStack(stack));
        }
        container.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(templates));
    }

    private static BundleContents contentsOf(ItemStack container) {
        BundleContents contents = container.get(DataComponents.BUNDLE_CONTENTS);
        return contents == null ? BundleContents.EMPTY : contents;
    }

    /** How many of {@code item} the container holds, across all of its stacks. */
    private static int countInBundle(ItemStack container, Item item) {
        int total = 0;
        for (ItemStack stack : contentsOf(container).itemCopyStream().toList()) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Everything the container holds, whatever it is - used where the point is that stone got in. */
    private static int totalInBundle(ItemStack container) {
        int total = 0;
        for (ItemStack stack : contentsOf(container).itemCopyStream().toList()) {
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

    /**
     * Entities inside the test structure only. The bounds are deliberately <em>not</em> inflated:
     * the test structures stand a few blocks apart, and a widened box finds the neighbouring
     * test's entities, which makes the result depend on the order the suite runs in.
     */
    private static List<ItemEntity> droppedItems(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds());
    }

    /** See {@link #droppedItems} on why the bounds stay as they are. */
    private static List<AbstractArrow> flyingArrows(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(AbstractArrow.class, helper.getBounds());
    }

    private static void discardArrows(GameTestHelper helper) {
        for (AbstractArrow arrow : flyingArrows(helper)) {
            arrow.discard();
        }
    }
}
