package com.simplebuilding.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import com.simplebuilding.loot.ModLootTableModifications;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;

/**
 * The storage enchantments - Drawer, Deep Pockets, Funnel - where they meet each other, where they
 * meet the item entity that sweeps drops off the floor, and where a player is supposed to find
 * them in the first place.
 *
 * <p>The neighbouring classes each take one slice of this and stop at its edge.
 * {@link ReinforcedBundleTests} measures one enchantment at a time on a bundle,
 * {@link QuiverTests} does the same on the quiver's own copy of the capacity formula,
 * {@link EnchantmentEffectTests#funnelDecidesWhatTheBundlePicksUp} calls
 * {@code canAutoPickup} directly, and
 * {@link BundleWiringTests#funnelBundleSweepsUpDropsOnTouchUnlessThePlayerSneaks} drives the
 * mixin with a Funnel II bundle that accepts everything. What none of them touches is the
 * <em>combinations</em>: two enchantments on one container, a filter and a kind limit reached
 * through the pickup path instead of through a click, and the chests the books come out of.
 *
 * <h2>Which player</h2>
 *
 * <p>{@link #mockPlayer} is the plain in-level mock. Nothing in this class reads
 * {@code isCreative()} or {@code instabuild} - the insert path, {@code canAutoPickup} and
 * {@code ItemEntityMixin#playerTouch} never ask - so the mock's hard-wired {@code CREATIVE} game
 * mode costs nothing here. It keeps its connection, which the bundle's insert sound and
 * {@code Player#take} go through.
 *
 * <h2>Known defects</h2>
 *
 * <p><b>The enderite bundle and the enderite quiver are in none of the enchantable tags.</b>
 * {@code ModItemTagProvider} puts only the reinforced and netherite variants into
 * {@code simplebuilding:bundle_enchantable}, {@code simplebuilding:extra_inventory_items} and
 * {@code simplebuilding:constructors_touch_enchantable}, although
 * {@code ReinforcedBundleItem#getTierCapacityMultiplier} and {@code QuiverItem}'s override both
 * know the enderite tier and hand it a factor of 3. So the top tier of both containers is the one
 * that can carry no container enchantment at all.
 * {@link #lootQuiversCarryOneOfTheContainerEnchantments} therefore asserts the accepted set for
 * the two quivers that are in the tags and says nothing about the enderite one - asserting either
 * state would freeze it.
 *
 * <p><b>{@code MagnetItem#getCurrentRange} adds two blocks per Range level to a magnet that can
 * never carry Range.</b> Range's supported items are
 * {@code simplebuilding:chisel_and_mining_tools}, which resolves to the chisel tools, the
 * sledgehammer tools, the octants and {@code #minecraft:enchantable/mining}; the magnet is in none
 * of them (it is only in {@code constructors_touch_enchantable}). The branch is live code that no
 * legally obtainable stack can reach. Nothing below asserts it in either direction: pinning the
 * tag as it stands would cement the hole, and pinning the branch would need a stack the game
 * cannot produce.
 *
 * <h2>Not covered, and why</h2>
 * <ul>
 *   <li><b>The sneak lock, the hands-before-inventory order and the pickup delay</b> in
 *       {@code ItemEntityMixin}. All three are
 *       {@link BundleWiringTests#funnelBundleSweepsUpDropsOnTouchUnlessThePlayerSneaks}. The
 *       drops below are all touched by an upright player with the container in the main hand, so
 *       none of those three branches decides anything here.</li>
 *   <li><b>{@code canAutoPickup} on its own</b> - {@link EnchantmentEffectTests}. What the tests
 *       here add is that the mixin really asks it and really honours a "no".</li>
 *   <li><b>The single-enchantment capacity numbers</b> (tier factors, Deep Pockets 2x/4x, the
 *       Drawer slope) - {@link ReinforcedBundleTests#capacityFollowsTierAndEnchantmentsAndMatchesTheWikiExport}
 *       and {@link QuiverTests#capacityDropsTheBundleBonusAndFollowsTierAndEnchantments}.</li>
 *   <li><b>The Drawer kind limit reached through a click or a direct insert</b> -
 *       {@link ReinforcedBundleTests#drawerCapsTheBundleAtFiveKinds}. Only the way through the
 *       floor is here.</li>
 *   <li><b>{@code worldGen.enableLootTableChanges}</b>, which switches every pool below off -
 *       {@link ConfigOptionTests#lootTableChangesStopWhenTheOptionIsSwitchedOff}. The two loot
 *       tests here switch it on for their run and put it back afterwards, the same way
 *       {@link MiningEnchantmentTests} does.</li>
 *   <li><b>The other mod books in the same pools.</b> The mining books are
 *       {@link MiningEnchantmentTests#miningEnchantmentBooksAndTheDiamondHammerSitInTheirLootPools},
 *       the building books and the double jump books belong to the wand and the player classes.
 *       {@link #storageBooksSitInTheChestsTheyAreMeantFor} restricts itself to Deep Pockets,
 *       Funnel and Range so that two classes never assert the same line.</li>
 *   <li><b>The weights of the loot entries.</b> Balancing numbers: pinning them turns every
 *       balance pass red without catching a wiring bug. The one thing asserted about an entry
 *       here is the function hanging off it.</li>
 *   <li><b>Range's attribute effect</b> (block interaction range per level) and the trade sources
 *       of the books. The first is a data-driven enchantment component checked where the items
 *       that carry it are tested; the second is the villager trade layer.</li>
 *   <li><b>Everything the player sees</b>: the tooltip grid, the insert sound, the pickup
 *       animation. Client side, or swallowed by the mock player's connection. The two numbers
 *       behind the picture - {@code getBarWidth} and the tooltip's capacity - are plain
 *       arithmetic on the stack and are checked.</li>
 * </ul>
 */
public final class StorageEnchantmentTests {

    private StorageEnchantmentTests() {
    }

    /** Highest Drawer level the enchantment data allows; guarded against the registry. */
    private static final int DRAWER_MAX_LEVEL = 8;

    /** Highest Deep Pockets level the enchantment data allows; guarded against the registry. */
    private static final int DEEP_POCKETS_MAX_LEVEL = 2;

    /** Highest Funnel level the enchantment data allows; guarded against the registry. */
    private static final int FUNNEL_MAX_LEVEL = 2;

    /** Kinds a Drawer container may hold at once - {@code DRAWER_MAX_TYPES} in the item. */
    private static final int DRAWER_KINDS = 5;

    /** Five kinds that fill a Drawer container up to its limit. All stack to 64. */
    private static final Item[] FIVE_KINDS = {
            Items.STONE, Items.DIRT, Items.OAK_PLANKS, Items.COBBLESTONE, Items.SAND,
    };

    /** The kind that arrives once the five above are inside - a sixth one the Drawer must refuse. */
    private static final Item SIXTH_KIND = Items.GRAVEL;

    /** How much of each kind is stocked - small, so the refusal below can never be about capacity. */
    private static final int KIND_STOCK = 8;

    /** Full bar width vanilla draws, i.e. what a container filled to its brim has to report. */
    private static final int FULL_BAR = 13;

    /** Bar width of a container filled to exactly half: {@code Math.round(0.5f * 13)}. */
    private static final int HALF_BAR = 7;

    /** Room left in the quiver before the oversized drop hits it, in arrows. */
    private static final int BRIM_GAP = 10;

    /**
     * The enchantments a quiver is a legal target for, i.e. everything
     * {@link #lootQuiversCarryOneOfTheContainerEnchantments} may find on one that came out of a
     * chest. Three item tags meet on the quiver: {@code bundle_enchantable} (Drawer, Deep Pockets,
     * Funnel), {@code extra_inventory_items} (Master Builder, Colour Palette) and
     * {@code constructors_touch_enchantable} (Constructor's Touch).
     */
    private static final List<ResourceKey<Enchantment>> QUIVER_ENCHANTMENTS = List.of(
            ModEnchantments.DRAWER,
            ModEnchantments.DEEP_POCKETS,
            ModEnchantments.FUNNEL,
            ModEnchantments.MASTER_BUILDER,
            ModEnchantments.COLOR_PALETTE,
            ModEnchantments.CONSTRUCTORS_TOUCH);

    /** The three enchantments whose loot supply this class owns; see the class javadoc. */
    private static final List<ResourceKey<Enchantment>> OWNED_BOOKS = List.of(
            ModEnchantments.DEEP_POCKETS, ModEnchantments.FUNNEL, ModEnchantments.RANGE);

    /** Where the mod's serialised pools keep the enchantments of an enchanted book. */
    private static final String STORED_ENCHANTMENTS_KEY = "minecraft:stored_enchantments";

    /** Serialised id of the loot function that hands out a random enchantment. */
    private static final String ENCHANT_RANDOMLY = "minecraft:enchant_randomly";

    /** Seed for the loot rolls, so a failure is reproducible instead of a coin flip. */
    private static final long QUIVER_ROLL_SEED = 20260904L;

    /**
     * How often each recorded pool is rolled when looking for quivers. The thinner of the two
     * entries is the ancient city's (weight 3 of that pool's 30, rolled 0 to 3 times), which is
     * worth about 75 quivers over this many rolls - far more than {@link #MIN_QUIVER_SAMPLE}
     * asks for, and the seed above pins the outcome to the same answer on every run anyway.
     */
    private static final int QUIVER_ROLLS = 512;

    /**
     * How many quivers a roll has to produce before its answer counts. Below this the run says
     * "the sample is too thin" instead of passing on an empty search - a pool that lost its quiver
     * entry must not look like a pool whose quivers all came out clean.
     */
    private static final int MIN_QUIVER_SAMPLE = 20;

    /**
     * One chest and the books of {@link #OWNED_BOOKS} it has to hand out, as enchantment to
     * levels. An empty map means "this table carries none of the three".
     *
     * <p>{@link #storageBooksSitInTheChestsTheyAreMeantFor} walks every built-in loot table and
     * expects an empty map for everything not listed in {@link #BOOK_CASES}, so this list is the
     * complete positive side of the statement. The explicitly empty entries are kept because they
     * are chests the mod does edit with its <em>other</em> books, and a reader of this list has to
     * see that they were considered rather than forgotten.
     */
    private record BookCase(ResourceKey<LootTable> table, Map<ResourceKey<Enchantment>, Set<Integer>> books) {
    }

    /**
     * Fewest built-in loot tables {@link #storageBooksSitInTheChestsTheyAreMeantFor} has to walk
     * before its "and in no other chest" half says anything. Vanilla registers well over a hundred
     * on both lines; this only has to notice an empty or gutted {@code BuiltInLootTables.all()},
     * which would otherwise turn the closed comparison into a comparison of nothing.
     */
    private static final int MIN_TABLES_WALKED = 60;

    private static final List<BookCase> BOOK_CASES = List.of(
            new BookCase(BuiltInLootTables.ANCIENT_CITY, Map.of(ModEnchantments.DEEP_POCKETS, Set.of(2))),
            new BookCase(BuiltInLootTables.BASTION_TREASURE, Map.of(ModEnchantments.FUNNEL, Set.of(1))),
            new BookCase(BuiltInLootTables.BASTION_OTHER, Map.of(ModEnchantments.FUNNEL, Set.of(1))),
            new BookCase(BuiltInLootTables.NETHER_BRIDGE, Map.of(ModEnchantments.FUNNEL, Set.of(1))),
            new BookCase(BuiltInLootTables.SIMPLE_DUNGEON, Map.of(ModEnchantments.FUNNEL, Set.of(1))),
            new BookCase(BuiltInLootTables.STRONGHOLD_LIBRARY, Map.of(ModEnchantments.RANGE, Set.of(2))),
            new BookCase(BuiltInLootTables.END_CITY_TREASURE, Map.of(ModEnchantments.RANGE, Set.of(3))),
            new BookCase(BuiltInLootTables.PILLAGER_OUTPOST, Map.of()),
            new BookCase(BuiltInLootTables.WOODLAND_MANSION, Map.of()),
            new BookCase(BuiltInLootTables.ABANDONED_MINESHAFT, Map.of()),
            new BookCase(BuiltInLootTables.BURIED_TREASURE, Map.of()),
            new BookCase(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS, Map.of()),
            new BookCase(BuiltInLootTables.SPAWN_BONUS_CHEST, Map.of()));

    // =====================================================================================
    // WHAT THE FUNNEL SWEEPS UP
    // =====================================================================================

    /**
     * Funnel I is a filter - it only takes what the container already holds - and the filter has
     * to survive the trip through {@code ItemEntityMixin#playerTouch}, which is the only way a
     * player ever meets it.
     *
     * <p>{@link EnchantmentEffectTests#funnelDecidesWhatTheBundlePicksUp} asks
     * {@code ReinforcedBundleItem#canAutoPickup} directly, and
     * {@link BundleWiringTests#funnelBundleSweepsUpDropsOnTouchUnlessThePlayerSneaks} drives the
     * mixin with Funnel II bundles that accept everything. Between the two, the mixin could stop
     * calling {@code canAutoPickup} altogether - hard-coding "a bundle takes it" - and both would
     * stay green while every Funnel I bundle in the game turned into a vacuum cleaner. That
     * composition is what this test covers.
     *
     * <p>The refusal is asserted from both sides: the drop is not in the bundle <em>and</em> it is
     * in the player's inventory. The second half is the important one - it says the mixin stepped
     * aside and let vanilla's own pickup finish the touch, rather than swallowing the item into
     * nothing. That vanilla half ({@code ItemEntity#playerTouch} adding to the inventory and
     * discarding the entity) is not mod code; it is used here as the observable that separates
     * "refused" from "eaten".
     *
     * <p>The block at the top pins Funnel's own definition numbers - weight, anvil cost and both
     * enchanting cost curves - the way
     * {@link #drawerAndDeepPocketsMultiplyOnTheSameContainer} does for the other two storage
     * enchantments. This class owns all three, and no assertion anywhere in the tree used to read
     * one of those four numbers, so a rebalance of them moved silently. The maximum level goes
     * with them because the two levels below have to be levels a player can actually reach.
     *
     * <p>What breaks it: collapsing the {@code level == 1} branch in {@code canAutoPickup} into
     * "return true", so a Funnel I bundle takes foreign kinds; the mixin ignoring the answer;
     * {@code tryInsertStackFromWorld} reporting success without storing anything, which would
     * make the drop disappear from both places; or one of Funnel's definition numbers moving.
     */
    public static void funnelFilterDecidesWhatTheTouchSweepsUp(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        Assertions.valueEqual(helper, enchantment(helper, ModEnchantments.FUNNEL).value().getMaxLevel(),
                FUNNEL_MAX_LEVEL, "max level of Funnel");
        assertDefinitionNumbers(helper, ModEnchantments.FUNNEL, "Funnel",
                /* weight */ 2, /* anvil cost */ 4, FUNNEL_MAX_LEVEL,
                /* min cost */ 15, 15, /* max cost */ 55, 15);

        // --- Funnel I, holding stone: stone yes ---
        ItemStack filtering = enchanted(helper, ModItems.REINFORCED_BUNDLE, ModEnchantments.FUNNEL, 1);
        insertExactly(helper, player, filtering, Items.STONE, KIND_STOCK);
        armHand(player, filtering);

        ItemEntity known = drop(helper, new ItemStack(Items.STONE, KIND_STOCK), new Vec3(2.5, 2.0, 2.5));
        known.playerTouch(player);

        Assertions.valueEqual(helper, countIn(filtering, Items.STONE), KIND_STOCK * 2,
                "stone in a Funnel I bundle after it was touched by a stone drop it already held");
        Assertions.valueEqual(helper, looseCount(player, Items.STONE), 0,
                "the stone reached the bundle and the inventory, so the drop exists twice");
        helper.assertTrue(known.isRemoved(),
                "the bundle took the whole stack but the item entity is still lying there");

        // --- the same bundle, a kind it does not hold: refused, and left to vanilla ---
        ItemEntity foreign = drop(helper, new ItemStack(Items.DIRT, KIND_STOCK), new Vec3(3.5, 2.0, 2.5));
        foreign.playerTouch(player);

        Assertions.valueEqual(helper, countIn(filtering, Items.DIRT), 0,
                "a Funnel I bundle holding only stone swallowed a dirt drop; the filter is gone");
        Assertions.valueEqual(helper, looseCount(player, Items.DIRT), KIND_STOCK,
                "the dirt is neither in the bundle nor in the inventory - the mixin swallowed the touch "
                        + "instead of stepping aside and letting the vanilla pickup finish it");

        // --- control: Funnel II takes the very same kind, so "refused" was about the filter ---
        ItemStack open = enchanted(helper, ModItems.REINFORCED_BUNDLE, ModEnchantments.FUNNEL, 2);
        armHand(player, open);
        ItemEntity forOpen = drop(helper, new ItemStack(Items.DIRT, KIND_STOCK), new Vec3(4.5, 2.0, 2.5));
        forOpen.playerTouch(player);

        Assertions.valueEqual(helper, countIn(open, Items.DIRT), KIND_STOCK,
                "control: a Funnel II bundle did not take the dirt either, so the refusal above says "
                        + "nothing about the filter");

        // --- control: without the enchantment nothing is swept up at all ---
        ItemStack plain = new ItemStack(ModItems.REINFORCED_BUNDLE);
        armHand(player, plain);
        ItemEntity forPlain = drop(helper, new ItemStack(Items.DIRT, KIND_STOCK), new Vec3(5.5, 2.0, 2.5));
        forPlain.playerTouch(player);

        Assertions.valueEqual(helper, countIn(plain, Items.DIRT), 0,
                "a bundle without Funnel hoovered a drop off the floor");
        Assertions.valueEqual(helper, looseCount(player, Items.DIRT), KIND_STOCK,
                "the dirt an unenchanted bundle refused did not reach the inventory either");

        TestCleanup.succeed(helper);
    }

    /**
     * The Drawer limit of five kinds has to hold when the sixth kind arrives from the floor, not
     * only when it is clicked in. Funnel and Drawer are compatible - nothing stops a player from
     * putting both on one bundle - and a bundle that hoovers everything is exactly the bundle that
     * meets a sixth kind.
     *
     * <p>{@link ReinforcedBundleTests#drawerCapsTheBundleAtFiveKinds} drives the same limit
     * through {@code tryInsertStackFromWorld} directly. What is added here is the route: the
     * pickup path decides with {@code canAutoPickup} first, which knows nothing about kinds and
     * answers "yes" for everything at Funnel II, and only the insert underneath it refuses. A
     * bundle that reported the refused pickup as a success would delete the drop.
     *
     * <p>Two controls carry the case:
     * <ul>
     *   <li>the same drop, offered to an identical bundle <em>without</em> Drawer, has to be
     *       taken - so the refusal is the kind limit and not the container being full;</li>
     *   <li>more of a kind the bundle already holds is still swept up afterwards - so the limit
     *       counts kinds and does not simply close the bundle once it holds five.</li>
     * </ul>
     *
     * <p>What breaks it: dropping the {@code uniqueTypesCount >= DRAWER_MAX_TYPES} check, moving
     * it behind the capacity check so a full bundle answers first, letting it block kinds the
     * bundle already holds, or the mixin treating a refused insert as a successful pickup.
     */
    public static void drawerKindCapHoldsAgainstTheFunnelToo(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        // Setup guard: the pairing under test has to be one an anvil would allow. Drawer is
        // exclusive with the builder set, so a future exclusivity change could quietly make this
        // bundle unbuildable while every assertion below still passed.
        helper.assertTrue(Enchantment.areCompatible(enchantment(helper, ModEnchantments.DRAWER),
                        enchantment(helper, ModEnchantments.FUNNEL)),
                "Drawer and Funnel have become mutually exclusive, so the bundle this test builds is "
                        + "no longer a state the game can reach");

        ItemStack drawer = enchanted(helper, ModItems.REINFORCED_BUNDLE, ModEnchantments.FUNNEL, 2);
        drawer.enchant(enchantment(helper, ModEnchantments.DRAWER), 1);
        ItemStack control = enchanted(helper, ModItems.REINFORCED_BUNDLE, ModEnchantments.FUNNEL, 2);

        for (Item kind : FIVE_KINDS) {
            insertExactly(helper, player, drawer, kind, KIND_STOCK);
            insertExactly(helper, player, control, kind, KIND_STOCK);
        }
        Assertions.valueEqual(helper, kindsIn(drawer), DRAWER_KINDS,
                "kinds inside the Drawer bundle after the setup filled it");

        // --- the sixth kind is turned away, and the mixin leaves it to vanilla ---
        armHand(player, drawer);
        ItemEntity sixth = drop(helper, new ItemStack(SIXTH_KIND, KIND_STOCK), new Vec3(2.5, 2.0, 2.5));
        sixth.playerTouch(player);

        Assertions.valueEqual(helper, countIn(drawer, SIXTH_KIND), 0,
                "a Drawer bundle that already holds " + DRAWER_KINDS + " kinds swept up a sixth one");
        Assertions.valueEqual(helper, looseCount(player, SIXTH_KIND), KIND_STOCK,
                "the refused drop is in neither the bundle nor the inventory; the mixin reported the "
                        + "failed insert as a successful pickup and the items are gone");

        // --- but a kind it already holds still goes in, so the limit counts kinds ---
        ItemEntity known = drop(helper, new ItemStack(FIVE_KINDS[0], KIND_STOCK), new Vec3(3.5, 2.0, 2.5));
        known.playerTouch(player);

        Assertions.valueEqual(helper, countIn(drawer, FIVE_KINDS[0]), KIND_STOCK * 2,
                "a Drawer bundle at its kind limit refused more of a kind it already holds");

        // --- control: without Drawer the very same drop is taken ---
        armHand(player, control);
        ItemEntity forControl = drop(helper, new ItemStack(SIXTH_KIND, KIND_STOCK), new Vec3(4.5, 2.0, 2.5));
        forControl.playerTouch(player);

        Assertions.valueEqual(helper, countIn(control, SIXTH_KIND), KIND_STOCK,
                "control: a bundle with the same contents but without Drawer refused the sixth kind too, "
                        + "so the refusal above was about room and not about the kind limit");

        TestCleanup.succeed(helper);
    }

    /**
     * A Funnel quiver is two filters in series: {@code canAutoPickup} says yes to everything at
     * Funnel II, and {@code QuiverItem#tryInsertStackFromWorld} underneath it still only accepts
     * {@code #minecraft:arrows}. Without the second one a quiver with Funnel would hoover stone
     * off the floor and stop being a quiver.
     *
     * <p>The second half of the test is the brim: the quiver is filled to
     * {@code capacity - BRIM_GAP} and then touched by a full stack of arrows. Exactly the gap may
     * go in, and the remainder has to survive - the pickup path is the one place where a
     * miscounted insert does not leave the surplus in a slot the player can see, it deletes it.
     * The capacity is measured on a quiver of its own first, so a balance change moves the numbers
     * with it instead of turning this red for the wrong reason.
     *
     * <p>{@code ItemEntity#playerTouch} finishing a partly emptied drop into the inventory is
     * vanilla, not mod code; it is what makes the leftover countable.
     *
     * <p>What breaks it: dropping the {@code ItemTags.ARROWS} check from
     * {@code QuiverItem#tryInsertStackFromWorld}; losing the {@code remainingSpace} clamp in
     * {@code insertItemIntoBundle}, so the whole stack is swallowed and the surplus vanishes; or
     * {@code tryInsertStackFromWorld} answering true without shrinking the offered stack, which
     * would duplicate the leftover into the inventory.
     */
    public static void funnelQuiverSweepsArrowsOnlyAndStopsAtItsBrim(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        // --- arrows in, everything else past it ---
        ItemStack quiver = enchanted(helper, ModItems.QUIVER, ModEnchantments.FUNNEL, 2);
        armHand(player, quiver);

        ItemEntity arrows = drop(helper, new ItemStack(Items.ARROW, 16), new Vec3(2.5, 2.0, 2.5));
        arrows.playerTouch(player);
        Assertions.valueEqual(helper, countIn(quiver, Items.ARROW), 16,
                "arrows in a Funnel II quiver after it was touched by an arrow drop");
        Assertions.valueEqual(helper, looseCount(player, Items.ARROW), 0,
                "the arrows reached the quiver and the inventory, so the drop exists twice");

        ItemEntity stone = drop(helper, new ItemStack(Items.STONE, KIND_STOCK), new Vec3(3.5, 2.0, 2.5));
        stone.playerTouch(player);
        Assertions.valueEqual(helper, countIn(quiver, Items.STONE), 0,
                "a Funnel II quiver swept stone off the floor; the arrow filter is gone");
        Assertions.valueEqual(helper, looseCount(player, Items.STONE), KIND_STOCK,
                "the stone the quiver refused is in neither the quiver nor the inventory");

        // --- the brim: only what fits goes in, the rest stays with the player ---
        int capacity = fillWith(helper, player, new ItemStack(ModItems.QUIVER), Items.ARROW);
        helper.assertTrue(capacity > BRIM_GAP,
                "setup guard: a quiver holds " + capacity + " arrows, which is not more than the "
                        + BRIM_GAP + " this case wants to leave free");

        ItemStack nearlyFull = enchanted(helper, ModItems.QUIVER, ModEnchantments.FUNNEL, 2);
        insertExactly(helper, player, nearlyFull, Items.ARROW, capacity - BRIM_GAP);
        armHand(player, nearlyFull);

        int offered = new ItemStack(Items.ARROW).getMaxStackSize();
        ItemEntity overflow = drop(helper, new ItemStack(Items.ARROW, offered), new Vec3(4.5, 2.0, 2.5));
        overflow.playerTouch(player);

        Assertions.valueEqual(helper, countIn(nearlyFull, Items.ARROW), capacity,
                "a quiver with room for " + BRIM_GAP + " arrows took a different amount out of a stack of "
                        + offered);
        Assertions.valueEqual(helper, looseCount(player, Items.ARROW), offered - BRIM_GAP,
                "the arrows the quiver had no room for did not reach the inventory; the surplus of the "
                        + "partly absorbed drop was destroyed");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // CAPACITY, WITH BOTH ENCHANTMENTS AT ONCE
    // =====================================================================================

    /**
     * Drawer and Deep Pockets sit on the same container and have to multiply. In
     * {@code getMaxCapacity} they are two consecutive {@code capacity = capacity.multiplyBy(...)}
     * lines; the second one reading {@code capacity = base.multiplyBy(...)} instead would throw the
     * first away, and every single-enchantment test in the suite would stay green.
     *
     * <p>The assertion is the cross product {@code both * plain == drawerOnly * deepOnly}, i.e.
     * the combined factor is the product of the two measured single factors. Nothing about the
     * factors themselves is written down here - which is deliberate twice over: a balance change
     * moves all four measurements together, and the Drawer multiplier is a known defect (see the
     * class javadoc) whose exact value this test does not have to take a side on. The guard above
     * it makes sure both single factors really are larger than one, otherwise the identity would
     * hold trivially.
     *
     * <p>Both classes are measured, because {@code QuiverItem} carries its own copy of
     * {@code getMaxCapacity} with the same two branches in it - a fix applied to one copy and not
     * the other is exactly the drift this repeats for.
     *
     * <p>The last block is the second formula behind the same number, and it too is run against
     * both copies. {@code getMaxCapacityForVisuals} recomputes the capacity from the stack's
     * enchantments by matching their ids by substring, and it is the only thing the bar and the
     * tooltip read. Neither copy was covered for the pair before: the bundle's is measured one
     * enchantment at a time by
     * {@link ReinforcedBundleTests#barAndTooltipReadTheSameCapacityTheFillingUses}, the quiver's by
     * {@link QuiverTests#barWidthFollowsTheSameCapacityTheFillingUses} - and that one reads the bar
     * only, so before this test no assertion in the tree ever asked a quiver for its tooltip
     * capacity at all.
     *
     * <p>The guard block at the top is also where the two enchantments' <em>definition</em>
     * numbers are pinned: the weight they are drawn with, what an anvil charges to combine them,
     * and both enchanting cost curves at every level. Nothing else in the tree reads any of the
     * four - Drawer's anvil cost could go from 8 to 1 and Deep Pockets' weight from 2 to 30, both
     * of which a player meets at every enchanting table and every anvil, with the whole suite
     * still green. (This is the enchantment's own weight in the enchanting table's pool. The
     * class javadoc's note about not pinning balancing numbers is about the weights of the loot
     * entries, which are a different number in a different file.) Moving these is a review
     * decision, and this is the line that makes the reviewer look.
     *
     * <p>What breaks it: one of the two branches overwriting the other instead of multiplying,
     * either branch disappearing from either copy, the visuals formula and the filling path
     * disagreeing about the pair, or one of the definition numbers moving.
     */
    public static void drawerAndDeepPocketsMultiplyOnTheSameContainer(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);

        // Setup guards: the levels below have to be levels a player can reach. ItemStack#enchant
        // does not clamp, so without these a shrunken maximum would leave the numbers passing for
        // a state no anvil can produce.
        Assertions.valueEqual(helper, enchantment(helper, ModEnchantments.DRAWER).value().getMaxLevel(),
                DRAWER_MAX_LEVEL, "max level of Drawer");
        Assertions.valueEqual(helper, enchantment(helper, ModEnchantments.DEEP_POCKETS).value().getMaxLevel(),
                DEEP_POCKETS_MAX_LEVEL, "max level of Deep Pockets");
        helper.assertTrue(Enchantment.areCompatible(enchantment(helper, ModEnchantments.DRAWER),
                        enchantment(helper, ModEnchantments.DEEP_POCKETS)),
                "Drawer and Deep Pockets have become mutually exclusive, so a container carrying both "
                        + "is no longer a state the game can reach");

        // The numbers behind "how often does a player meet this book and what does combining it
        // cost". Read nowhere else in the tree; see the javadoc above.
        assertDefinitionNumbers(helper, ModEnchantments.DRAWER, "Drawer",
                /* weight */ 1, /* anvil cost */ 8, DRAWER_MAX_LEVEL,
                /* min cost */ 25, 25, /* max cost */ 75, 25);
        assertDefinitionNumbers(helper, ModEnchantments.DEEP_POCKETS, "Deep Pockets",
                /* weight */ 2, /* anvil cost */ 4, DEEP_POCKETS_MAX_LEVEL,
                /* min cost */ 15, 10, /* max cost */ 65, 10);

        assertFactorsMultiply(helper, player, ModItems.REINFORCED_BUNDLE, Items.STONE, "the reinforced bundle");
        assertFactorsMultiply(helper, player, ModItems.QUIVER, Items.ARROW, "the quiver");

        // --- and the second formula, the one the bar and the tooltip read - in both copies ---
        assertVisualsMatchTheFilling(helper, player, ModItems.REINFORCED_BUNDLE, Items.STONE,
                "the combined bundle");
        assertVisualsMatchTheFilling(helper, player, ModItems.QUIVER, Items.ARROW,
                "the combined quiver");

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // WHERE THE BOOKS COME FROM
    // =====================================================================================

    /**
     * None of these three is in {@code minecraft:in_enchanting_table} (which
     * {@link MiningEnchantmentTests} pins as holding exactly one mod entry, and that entry is Fast
     * Chiseling), so a chest and a villager are the only two supplies there are. Deep Pockets has
     * no trade at all; Funnel and Range are in the master librarian's pool
     * ({@code librarian/5/emerald_master_book.json}), which belongs to the trade tests. Which
     * chest hands out which book, and at which level, is one {@code enchantedBook(...)} line each
     * in {@code ModLootTableModifications} - and nothing above that line notices when one goes
     * missing.
     * {@link ConfigOptionTests#lootTableChangesStopWhenTheOptionIsSwitchedOff} counts the pools
     * the mod offers, which cannot tell a pool that lost a book from a full one.
     *
     * <p>The pools are serialised through {@code LootPool.CODEC} rather than rolled: the codec
     * output is exact and needs no dice, and it carries the level inside the
     * {@code stored_enchantments} component with it. The first assertion is a shape guard, so a
     * changed serialisation is reported as "this test reads the wrong shape" instead of silently
     * passing on an empty search.
     *
     * <p>Every table is checked in both directions, and "every table" means every built-in one,
     * not only the ones named in {@link #BOOK_CASES}. That list states, for each chest, the
     * complete set of the three enchantments it may hand out, and every other table in
     * {@code BuiltInLootTables.all()} has to hand out none of them - so a book that wanders into
     * another chest is as red as a book that disappears, and a level that changes is red as well.
     * Walking the listed chests alone was the hole: {@code ModLootTableModifications#apply} edits
     * sixteen tables and only twelve of them are named below, which left the igloo, the shipwreck
     * treasure and the common and rare trial chamber vaults free to grow a storage book without a
     * single assertion moving. The mod's other books are deliberately out of scope; see
     * the class javadoc for who owns them.
     *
     * <p>What breaks it: a book moved to another chest, a book appearing in a chest that carried
     * none of the three, a changed level, a table losing its pool, or the bastion branch losing
     * the {@code BASTION_OTHER} half of its condition.
     */
    public static void storageBooksSitInTheChestsTheyAreMeantFor(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        boolean original = Simplebuilding.getConfig().worldGen.enableLootTableChanges;
        TestCleanup.before(helper, () -> Simplebuilding.getConfig().worldGen.enableLootTableChanges = original);

        List<String> problems = new ArrayList<>();
        try {
            Simplebuilding.getConfig().worldGen.enableLootTableChanges = true;

            helper.assertTrue(!storedEnchantments(helper, registries, BuiltInLootTables.ANCIENT_CITY).isEmpty(),
                    "no stored enchantments could be read out of the ancient city pools at all; the loot "
                            + "pool serialisation changed shape and this test has to be rewritten - that is "
                            + "not a mod regression");

            Map<ResourceKey<LootTable>, Map<Identifier, Set<Integer>>> expectedByTable = new LinkedHashMap<>();
            for (BookCase bookCase : BOOK_CASES) {
                Map<Identifier, Set<Integer>> expected = new LinkedHashMap<>();
                for (Map.Entry<ResourceKey<Enchantment>, Set<Integer>> entry : bookCase.books().entrySet()) {
                    expected.put(entry.getKey().identifier(), entry.getValue());
                }
                expectedByTable.put(bookCase.table(), expected);

                // A case naming a table the walk below never visits would be compared against
                // nothing at all, which is the failure mode this whole test exists to rule out.
                helper.assertTrue(BuiltInLootTables.all().contains(bookCase.table()),
                        bookCase.table().identifier() + " is not one of the built-in loot tables, so this "
                                + "case would never be compared against anything");
            }

            int walked = 0;
            for (ResourceKey<LootTable> table : BuiltInLootTables.all()) {
                walked++;
                Map<Identifier, Set<Integer>> found = storedEnchantments(helper, registries, table);

                Map<Identifier, Set<Integer>> mine = new LinkedHashMap<>();
                for (ResourceKey<Enchantment> key : OWNED_BOOKS) {
                    Set<Integer> levels = found.get(key.identifier());
                    if (levels != null) {
                        mine.put(key.identifier(), levels);
                    }
                }

                // Unlisted tables are the "and in no other chest" half: they have to hand out none
                // of the three.
                Map<Identifier, Set<Integer>> expected = expectedByTable.getOrDefault(table, Map.of());
                if (!mine.equals(expected)) {
                    problems.add(table.identifier() + " hands out " + mine + " instead of " + expected);
                }
            }

            helper.assertTrue(walked >= MIN_TABLES_WALKED,
                    "only " + walked + " built-in loot tables came out of BuiltInLootTables.all(), fewer "
                            + "than the " + MIN_TABLES_WALKED + " this test needs before \"and in no other "
                            + "chest\" means anything");
        } finally {
            Simplebuilding.getConfig().worldGen.enableLootTableChanges = original;
        }

        helper.assertTrue(problems.isEmpty(), "storage enchantment loot problems: " + problems);
        TestCleanup.succeed(helper);
    }

    /**
     * The ancient city hands out a quiver that is already enchanted, and the entry does not say
     * with what: {@code EnchantRandomlyFunction.randomEnchantment()} carries no enchantment list,
     * so vanilla picks uniformly among every enchantment whose {@code canEnchant} accepts the
     * item. For the quiver that means the mod's own item tags decide what a chest can produce -
     * the loot entry and the tag files are one feature, not two.
     *
     * <p>Three steps, and each one carries a different half of it:
     * <ol>
     *   <li><b>Which enchantments accept a quiver at all</b>, computed by walking the enchantment
     *       registry rather than by reading the tag files. The expected set is
     *       {@link #QUIVER_ENCHANTMENTS} - three item tags meeting on one item - and it is
     *       asserted as a closed set, so a quiver falling out of a tag and a foreign enchantment
     *       reaching it are both red. This is the assertion that gives step 3 its meaning.</li>
     *   <li><b>The entry itself</b>, read out of the serialised pool: it has to carry the
     *       {@code enchant_randomly} function, and that function must not name an
     *       {@code options} list - a list would cut the tags out of the decision.</li>
     *   <li><b>What really comes out</b>, by rolling the pool with a fixed seed. Every quiver from
     *       the ancient city has to carry exactly one enchantment, and it has to be one of the six.
     *       The pillager outpost is the control: its quiver entry has no function at all, so every
     *       quiver from there has to come out clean - otherwise "enchanted" would say nothing.</li>
     * </ol>
     *
     * <p>The enderite quiver is left out on purpose: it is in none of the three tags, and pinning
     * that would freeze a known defect (see the class javadoc).
     *
     * <p>What breaks it: the quiver dropping out of one of its item tags (step 1 and 3), the
     * random enchantment function disappearing from the ancient city entry or being narrowed to a
     * fixed list (step 2 and 3), or a new mod enchantment quietly landing on the container tags -
     * which is a review decision, not an accident, and is meant to show up here.
     */
    public static void lootQuiversCarryOneOfTheContainerEnchantments(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        boolean original = Simplebuilding.getConfig().worldGen.enableLootTableChanges;
        TestCleanup.before(helper, () -> Simplebuilding.getConfig().worldGen.enableLootTableChanges = original);

        try {
            Simplebuilding.getConfig().worldGen.enableLootTableChanges = true;

            // --- 1. the closed set of enchantments a quiver can carry ---
            Set<Identifier> expected = new TreeSet<>();
            for (ResourceKey<Enchantment> key : QUIVER_ENCHANTMENTS) {
                expected.add(key.identifier());
            }
            for (Item quiver : List.of(ModItems.QUIVER, ModItems.NETHERITE_QUIVER)) {
                Set<Identifier> accepting = enchantmentsAccepting(helper, quiver);
                helper.assertTrue(accepting.equals(expected),
                        BuiltInRegistries.ITEM.getKey(quiver) + " is a legal target for " + accepting
                                + " instead of " + expected + "; that set is what an anvil allows and what "
                                + "the chest below draws from");
            }

            // --- 2. the entry: a random enchantment, drawn from everything the tags allow ---
            JsonObject ancientEntry = lootEntry(helper, registries, BuiltInLootTables.ANCIENT_CITY,
                    ModItems.QUIVER);
            helper.assertTrue(ancientEntry != null,
                    "the ancient city chest has no quiver entry at all");
            JsonObject randomEnchant = functionNamed(ancientEntry, ENCHANT_RANDOMLY);
            helper.assertTrue(randomEnchant != null,
                    "the ancient city quiver carries no " + ENCHANT_RANDOMLY + " function; its entry is "
                            + ancientEntry);
            helper.assertTrue(!randomEnchant.has("options"),
                    "the ancient city quiver names its own enchantment list (" + randomEnchant + "), so the "
                            + "item tags no longer decide what a chest can produce");

            JsonObject outpostEntry = lootEntry(helper, registries, BuiltInLootTables.PILLAGER_OUTPOST,
                    ModItems.QUIVER);
            helper.assertTrue(outpostEntry != null,
                    "the pillager outpost has no quiver entry at all, so the control below is empty");
            helper.assertTrue(functionNamed(outpostEntry, ENCHANT_RANDOMLY) == null,
                    "the pillager outpost quiver became enchanted too, so a quiver coming out enchanted "
                            + "says nothing about the ancient city entry any more");

            // --- 3. and what the chest really hands over ---
            List<ItemStack> ancientQuivers = rollFor(helper, registries, BuiltInLootTables.ANCIENT_CITY,
                    ModItems.QUIVER);
            helper.assertTrue(ancientQuivers.size() >= MIN_QUIVER_SAMPLE,
                    "only " + ancientQuivers.size() + " quivers came out of " + QUIVER_ROLLS + " rolls of the "
                            + "ancient city pools; that sample is too thin to judge - raise QUIVER_ROLLS "
                            + "unless the entry itself is gone");
            for (ItemStack quiver : ancientQuivers) {
                Set<Identifier> found = enchantmentIds(quiver);
                Assertions.valueEqual(helper, found.size(), 1,
                        "a quiver out of the ancient city carries the enchantments " + found
                                + "; the entry hands out exactly one");
                helper.assertTrue(expected.containsAll(found),
                        "a quiver out of the ancient city carries " + found + ", which is outside the "
                                + expected + " its item tags allow");
            }

            List<ItemStack> outpostQuivers = rollFor(helper, registries, BuiltInLootTables.PILLAGER_OUTPOST,
                    ModItems.QUIVER);
            helper.assertTrue(outpostQuivers.size() >= MIN_QUIVER_SAMPLE,
                    "only " + outpostQuivers.size() + " quivers came out of " + QUIVER_ROLLS + " rolls of the "
                            + "pillager outpost pool; the control is too thin to say anything");
            for (ItemStack quiver : outpostQuivers) {
                helper.assertTrue(enchantmentIds(quiver).isEmpty(),
                        "control: a quiver out of the pillager outpost came with "
                                + enchantmentIds(quiver) + ", although only the ancient city entry is "
                                + "supposed to enchant one");
            }
        } finally {
            Simplebuilding.getConfig().worldGen.enableLootTableChanges = original;
        }

        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // HELPERS - THE PLAYER AND THE CONTAINERS
    // =====================================================================================

    /**
     * The usual in-level mock player, moved into the room with an empty inventory and handed back
     * to the player list when the test ends - a leaked mock player keeps the list non-empty and
     * stalls the gametest server on shutdown. It keeps its connection, which the insert sound and
     * {@code Player#take} go through.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.getInventory().clearContent();
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /** Empties the inventory and puts one stack into the selected hotbar slot. */
    private static void armHand(ServerPlayer player, ItemStack stack) {
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
    }

    /**
     * Drops one stack at a room relative position, ready to be picked up. The random launch
     * velocity the {@code ItemEntity} constructor hands out is cleared so the drop stays where the
     * test put it, the pickup delay is zero because the delay branch belongs to
     * {@link BundleWiringTests}, and the entity is removed again when the test ends.
     */
    private static ItemEntity drop(GameTestHelper helper, ItemStack stack, Vec3 relativePos) {
        Vec3 pos = helper.absoluteVec(relativePos);
        ItemEntity entity = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, stack);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setPickUpDelay(0);
        helper.getLevel().addFreshEntity(entity);
        TestCleanup.before(helper, entity::discard);
        return entity;
    }

    /** One container with one enchantment on it. */
    private static ItemStack enchanted(GameTestHelper helper, Item item, ResourceKey<Enchantment> key,
                                       int level) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(enchantment(helper, key), level);
        return stack;
    }

    /** One container carrying Drawer and Deep Pockets at their highest levels. */
    private static ItemStack bothEnchantments(GameTestHelper helper, Item item) {
        ItemStack stack = enchanted(helper, item, ModEnchantments.DRAWER, DRAWER_MAX_LEVEL);
        stack.enchant(enchantment(helper, ModEnchantments.DEEP_POCKETS), DEEP_POCKETS_MAX_LEVEL);
        return stack;
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static ReinforcedBundleItem containerItem(ItemStack stack) {
        return (ReinforcedBundleItem) stack.getItem();
    }

    /**
     * Pins the four numbers of one enchantment's definition that decide how a player comes by it:
     * the weight it is drawn with in the enchanting table's pool, what an anvil charges to combine
     * it, and both enchanting cost curves.
     *
     * <p>The curves are walked level by level instead of sampled at one point, so a changed slope
     * is as red as a changed base - a single sample at level 1 would let
     * {@code dynamicCost(25, 25)} become {@code dynamicCost(25, 1)} unnoticed, and for an
     * eight-level enchantment that is the difference between a book a player can afford and one
     * they cannot.
     */
    private static void assertDefinitionNumbers(GameTestHelper helper, ResourceKey<Enchantment> key,
                                                String name, int weight, int anvilCost, int maxLevel,
                                                int minCostBase, int minCostStep,
                                                int maxCostBase, int maxCostStep) {
        Enchantment definition = enchantment(helper, key).value();
        Assertions.valueEqual(helper, definition.getWeight(), weight,
                "weight of " + name + ", i.e. how often the enchanting table offers it against the "
                        + "other enchantments a container accepts");
        Assertions.valueEqual(helper, definition.getAnvilCost(), anvilCost,
                "anvil cost of " + name + ", i.e. what combining two of its books costs");
        for (int level = 1; level <= maxLevel; level++) {
            Assertions.valueEqual(helper, definition.getMinCost(level),
                    minCostBase + minCostStep * (level - 1),
                    "minimum enchanting cost of " + name + " " + level);
            Assertions.valueEqual(helper, definition.getMaxCost(level),
                    maxCostBase + maxCostStep * (level - 1),
                    "maximum enchanting cost of " + name + " " + level);
        }
    }

    /**
     * Measures the three single cases and the combined one and asserts that the combined factor is
     * the product of the two single factors - written as a cross product so no division has to
     * round.
     */
    private static void assertFactorsMultiply(GameTestHelper helper, ServerPlayer player, Item container,
                                              Item filler, String what) {
        int plain = fillWith(helper, player, new ItemStack(container), filler);
        int drawerOnly = fillWith(helper, player,
                enchanted(helper, container, ModEnchantments.DRAWER, DRAWER_MAX_LEVEL), filler);
        int deepOnly = fillWith(helper, player,
                enchanted(helper, container, ModEnchantments.DEEP_POCKETS, DEEP_POCKETS_MAX_LEVEL), filler);
        int both = fillWith(helper, player, bothEnchantments(helper, container), filler);

        // Without this the identity below would also hold for a container that ignores both
        // enchantments: 1 x 1 = 1.
        helper.assertTrue(drawerOnly > plain && deepOnly > plain,
                what + " takes " + plain + " without enchantments, " + drawerOnly + " with Drawer "
                        + DRAWER_MAX_LEVEL + " and " + deepOnly + " with Deep Pockets "
                        + DEEP_POCKETS_MAX_LEVEL + "; at least one of the two enchantments does nothing at "
                        + "all, so nothing can be said about the pair");
        Assertions.valueEqual(helper, both * plain, drawerOnly * deepOnly,
                what + " with both enchantments takes " + both + " where the two single factors ("
                        + drawerOnly + " and " + deepOnly + " against a base of " + plain + ") multiply to "
                        + (drawerOnly * deepOnly / plain) + "; one of the two branches is throwing the "
                        + "other one away instead of multiplying with it");
    }

    /**
     * Holds {@code getMaxCapacityForVisuals} against {@code getMaxCapacity} on one container
     * carrying both enchantments. The real capacity is measured first by filling a copy, so nothing
     * here is written down; the container is then filled to exactly half of it, which has to draw
     * half a bar, to its brim, which has to draw a full one, and its tooltip has to hand the client
     * that same measured number.
     *
     * <p>Called once per copy of the formula. {@code QuiverItem} overrides
     * {@code getMaxCapacityForVisuals} with its own pair of branches, and no test in the tree used
     * to reach it with more than one enchantment on the stack - so a fix or a break applied to one
     * copy and not the other went unnoticed, which is the same drift
     * {@link #assertFactorsMultiply} repeats for one method higher up.
     *
     * <p>Its reach has one limit worth naming. A branch that <em>disappears</em> from either copy
     * is caught here every run. A branch rewritten to recompute from the base instead of
     * multiplying - {@code capacity = getBaseCapacity(...).multiplyBy(bonus)} - is only caught when
     * the rewritten one happens to be visited second, because both copies walk
     * {@code stack.getEnchantments()}, and vanilla backs {@code ItemEnchantments} with an
     * {@code Object2IntOpenHashMap} whose keys are {@code Holder.Reference}s that inherit identity
     * hashing. That order is not stable across JVM runs, which is also what such a rewrite would do
     * to the game: the tooltip of a Drawer plus Deep Pockets container would report a different
     * capacity depending on the launch. Half the runs red is the honest ceiling for that one
     * mutation; there is no ordering hook to pin it any harder.
     */
    private static void assertVisualsMatchTheFilling(GameTestHelper helper, ServerPlayer player,
                                                     Item container, Item filler, String what) {
        ItemStack both = bothEnchantments(helper, container);
        int capacity = fillWith(helper, player, both.copy(), filler);
        helper.assertTrue(capacity % 2 == 0,
                "setup guard: " + what + " holds an odd " + capacity + " " + filler + ", so it cannot be "
                        + "filled to exactly half");

        ItemStack half = both.copy();
        insertExactly(helper, player, half, filler, capacity / 2);
        Assertions.valueEqual(helper, half.getItem().getBarWidth(half), HALF_BAR,
                "bar width of " + what + " - Drawer " + DRAWER_MAX_LEVEL + " plus Deep Pockets "
                        + DEEP_POCKETS_MAX_LEVEL + " - holding " + (capacity / 2) + " of its " + capacity
                        + " items; the visuals formula is missing one of the two enchantments");

        ItemStack filled = both.copy();
        insertExactly(helper, player, filled, filler, capacity);
        Assertions.valueEqual(helper, filled.getItem().getBarWidth(filled), FULL_BAR,
                "bar width of " + what + " filled to its brim with " + capacity + " items");

        Optional<TooltipComponent> image = filled.getItem().getTooltipImage(filled);
        helper.assertTrue(image.isPresent() && image.get() instanceof ReinforcedBundleTooltipData,
                "the tooltip of " + what + " is " + image + " instead of a ReinforcedBundleTooltipData");
        Assertions.valueEqual(helper, ((ReinforcedBundleTooltipData) image.get()).maxCapacity(), capacity,
                "the capacity the tooltip of " + what + " hands the client, against the " + capacity
                        + " items it really takes");
    }

    /**
     * Fills {@code container} to its brim and answers how many items really went in - counted from
     * what the offered stacks lost, because the last one usually only fits partly. Bounded, so a
     * container that never fills up fails the test instead of hanging the run.
     */
    private static int fillWith(GameTestHelper helper, ServerPlayer player, ItemStack container,
                                Item filler) {
        ReinforcedBundleItem item = containerItem(container);
        int stackSize = new ItemStack(filler).getMaxStackSize();

        int inserted = 0;
        for (int attempt = 0; attempt < 64; attempt++) {
            ItemStack offered = new ItemStack(filler, stackSize);
            if (!item.tryInsertStackFromWorld(container, offered, player)) {
                Assertions.valueEqual(helper, countIn(container, filler), inserted,
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
     * Puts exactly {@code count} of {@code item} in and fails the test if any of it stayed outside
     * - a setup step that has to be loud, because a half-filled container would turn into a
     * confusing assertion further down.
     */
    private static void insertExactly(GameTestHelper helper, ServerPlayer player, ItemStack container,
                                      Item item, int count) {
        int before = countIn(container, item);
        int stackSize = new ItemStack(item).getMaxStackSize();
        int left = count;
        while (left > 0) {
            ItemStack offered = new ItemStack(item, Math.min(stackSize, left));
            int offeredCount = offered.getCount();
            helper.assertTrue(containerItem(container).tryInsertStackFromWorld(container, offered, player),
                    "test setup broken: " + container.getItem() + " refused " + offeredCount + " " + item);
            Assertions.valueEqual(helper, offered.getCount(), 0,
                    "test setup broken: " + container.getItem() + " only took part of " + offeredCount + " "
                            + item);
            left -= offeredCount;
        }
        Assertions.valueEqual(helper, countIn(container, item), before + count,
                "test setup broken: " + item + " inside " + container.getItem() + " after filling it");
    }

    /** How many of {@code item} sit inside the container, across all of its stacks. */
    private static int countIn(ItemStack container, Item item) {
        BundleContents contents = container.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : contents.itemCopyStream().toList()) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * How many different kinds the container holds. Counted by item only, which is what the
     * setup below needs; the item's own check compares components as well, and no case here puts
     * two stacks of the same item with different components into one container.
     */
    private static int kindsIn(ItemStack container) {
        BundleContents contents = container.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) {
            return 0;
        }
        Set<Item> kinds = new LinkedHashSet<>();
        for (ItemStack stack : contents.itemCopyStream().toList()) {
            kinds.add(stack.getItem());
        }
        return kinds.size();
    }

    /** How many of {@code item} lie loose in the inventory - not counting anything in a container. */
    private static int looseCount(ServerPlayer player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    // =====================================================================================
    // HELPERS - ENCHANTMENTS AND LOOT
    // =====================================================================================

    /** Every enchantment in the registry that would accept {@code item} in an anvil. */
    private static Set<Identifier> enchantmentsAccepting(GameTestHelper helper, Item item) {
        Registry<Enchantment> registry =
                helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemStack stack = new ItemStack(item);

        Set<Identifier> accepting = new TreeSet<>();
        for (Map.Entry<ResourceKey<Enchantment>, Enchantment> entry : registry.entrySet()) {
            if (entry.getValue().canEnchant(stack)) {
                accepting.add(entry.getKey().identifier());
            }
        }
        return accepting;
    }

    /** The enchantments on one stack, by id. */
    private static Set<Identifier> enchantmentIds(ItemStack stack) {
        Set<Identifier> ids = new TreeSet<>();
        for (Holder<Enchantment> holder : stack.getEnchantments().keySet()) {
            holder.unwrapKey().ifPresent(key -> ids.add(key.identifier()));
        }
        return ids;
    }

    /**
     * Rolls every pool the mod hands to one table {@link #QUIVER_ROLLS} times and returns the
     * stacks of {@code item} that came out. The seed is fixed, so the same pool always produces
     * the same answer here.
     */
    private static List<ItemStack> rollFor(GameTestHelper helper, HolderLookup.Provider registries,
                                           ResourceKey<LootTable> table, Item item) {
        PoolCollector collector = new PoolCollector();
        ModLootTableModifications.apply(table, collector, registries);

        LootParams params = new LootParams.Builder(helper.getLevel()).create(LootContextParamSets.EMPTY);
        LootContext context = new LootContext.Builder(params)
                .withOptionalRandomSeed(QUIVER_ROLL_SEED)
                .create(Optional.empty());

        List<ItemStack> found = new ArrayList<>();
        for (LootPool pool : collector.pools) {
            for (int roll = 0; roll < QUIVER_ROLLS; roll++) {
                pool.addRandomItems(stack -> {
                    if (stack.is(item)) {
                        found.add(stack);
                    }
                }, context);
            }
        }
        return found;
    }

    /**
     * Every {@code stored_enchantments} the mod's pools for one table carry, as
     * {@code enchantment id -> levels}. Serialised rather than rolled: the codec output is exact
     * and needs no dice.
     */
    private static Map<Identifier, Set<Integer>> storedEnchantments(GameTestHelper helper,
                                                                    HolderLookup.Provider registries,
                                                                    ResourceKey<LootTable> table) {
        Map<Identifier, Set<Integer>> found = new LinkedHashMap<>();
        for (JsonElement pool : encodePools(helper, registries, table)) {
            collectStoredEnchantments(pool, found);
        }
        return found;
    }

    /**
     * The serialised loot entry for {@code item} in the mod's pools for one table, or
     * {@code null}. A loot item entry carries its item id under {@code "name"} and its functions
     * under {@code "functions"}; both spellings come from vanilla's own entry codecs.
     */
    private static JsonObject lootEntry(GameTestHelper helper, HolderLookup.Provider registries,
                                        ResourceKey<LootTable> table, Item item) {
        String wanted = BuiltInRegistries.ITEM.getKey(item).toString();
        for (JsonElement pool : encodePools(helper, registries, table)) {
            JsonObject entry = findEntry(pool, wanted);
            if (entry != null) {
                return entry;
            }
        }
        return null;
    }

    /** The loot function of the given id hanging off one entry, or {@code null}. */
    private static JsonObject functionNamed(JsonObject entry, String id) {
        JsonElement functions = entry.get("functions");
        if (functions == null || !functions.isJsonArray()) {
            return null;
        }
        for (JsonElement element : functions.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject function = element.getAsJsonObject();
            JsonElement type = function.get("function");
            if (type != null && type.isJsonPrimitive() && id.equals(type.getAsString())) {
                return function;
            }
        }
        return null;
    }

    private static List<JsonElement> encodePools(GameTestHelper helper, HolderLookup.Provider registries,
                                                 ResourceKey<LootTable> table) {
        PoolCollector collector = new PoolCollector();
        ModLootTableModifications.apply(table, collector, registries);

        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        List<JsonElement> encoded = new ArrayList<>();
        for (LootPool pool : collector.pools) {
            encoded.add(LootPool.CODEC.encodeStart(ops, pool)
                    .getOrThrow(message -> helper.assertionException(
                            "a loot pool the mod adds to " + table.identifier()
                                    + " cannot be serialised: " + message)));
        }
        return encoded;
    }

    /** The first object below {@code element} whose {@code name} is {@code itemId}. */
    private static JsonObject findEntry(JsonElement element, String itemId) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                JsonObject entry = findEntry(child, itemId);
                if (entry != null) {
                    return entry;
                }
            }
            return null;
        }
        if (!element.isJsonObject()) {
            return null;
        }
        JsonObject object = element.getAsJsonObject();
        JsonElement name = object.get("name");
        if (name != null && name.isJsonPrimitive() && itemId.equals(name.getAsString())) {
            return object;
        }
        for (Map.Entry<String, JsonElement> child : object.entrySet()) {
            JsonObject entry = findEntry(child.getValue(), itemId);
            if (entry != null) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Walks the serialised pool for objects stored under {@link #STORED_ENCHANTMENTS_KEY} and
     * collects the {@code id -> level} pairs below them. The search below that key is recursive on
     * purpose: whether the component encodes as a bare map or wraps its levels in another object
     * is vanilla's business, and either shape has to keep working.
     */
    private static void collectStoredEnchantments(JsonElement element, Map<Identifier, Set<Integer>> out) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectStoredEnchantments(child, out);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            if (STORED_ENCHANTMENTS_KEY.equals(entry.getKey())) {
                collectLevels(entry.getValue(), out);
            } else {
                collectStoredEnchantments(entry.getValue(), out);
            }
        }
    }

    private static void collectLevels(JsonElement element, Map<Identifier, Set<Integer>> out) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectLevels(child, out);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            JsonElement value = entry.getValue();
            Optional<Identifier> id = Identifier.read(entry.getKey()).result();
            if (id.isPresent() && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                out.computeIfAbsent(id.get(), unused -> new TreeSet<>()).add(value.getAsInt());
            } else {
                collectLevels(value, out);
            }
        }
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
}
