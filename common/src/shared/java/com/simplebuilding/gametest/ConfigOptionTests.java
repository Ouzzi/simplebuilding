package com.simplebuilding.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.loot.ModLootTableModifications;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.Vec3;

/**
 * The config options, and whether the mod still reads them.
 *
 * <p>Before this class the only covered options were the two trade switches, and those only
 * indirectly, through the datapack conditions that consume them. An option is a promise to the
 * player, and it is a promise that breaks quietly: the field stays in {@code simplebuilding.json},
 * the GUI keeps offering the toggle, and nothing anywhere reports that the last call site that read
 * it was deleted or renamed. The tests here flip an option and check that the behaviour behind it
 * actually changes.
 *
 * <p><b>Restoring the options.</b> Every test that writes an option captures the previous value
 * first and restores it in a {@code finally} block. A failing assertion throws, so the option is
 * put back on the way out of the very same call - which is what makes these tests independent of
 * each other no matter which one fails. The original value is re-applied a second time from
 * {@link GameTestHelper#runBeforeTestEnd}, purely as a belt and braces for a failure mode that
 * unwinds past the {@code finally}.
 *
 * <p>The suite runs as one batch in a single world and shares one config object, so a flipped
 * option is global while it lasts. It cannot leak into a neighbouring test all the same: none of
 * these bodies yields - no {@code succeedWhen}, no delayed callback - so each runs from its first
 * write to its restore inside one server tick on the server thread, and no other test body can be
 * running in that window.
 *
 * <p>Only two options gate behaviour a server side gametest can reach at all - every other read of
 * {@code Simplebuilding.getConfig()} in the mod sits in {@code client/} or in a client mixin. That
 * is written down per option in the javadoc below rather than papered over with a test that would
 * pass either way.
 */
public final class ConfigOptionTests {

    private ConfigOptionTests() {
    }

    /** Resource folder the shipped trades live in: {@code data/simplebuilding/villager_trade/**}. */
    private static final String TRADE_DIRECTORY = "villager_trade";

    /** Sub folder of {@link #TRADE_DIRECTORY} that belongs to the wandering trader switch. */
    private static final String WANDERING_DIRECTORY = "wandering_trader";

    /** Id both loaders register their "read a config flag" datapack condition under. */
    private static final String CONFIG_CONDITION = SimpleBuildingGameTests.MOD_ID + ":config";

    /** Fully qualified name of Cloth Config's {@code @Config}; read reflectively, see below. */
    private static final String CONFIG_ANNOTATION = "me.shedaniel.autoconfig.annotation.Config";

    private static final String VILLAGER_FLAG = "enableVillagerTrades";
    private static final String WANDERING_FLAG = "enableWanderingTrades";

    /**
     * The two classes that turn the flag string of a trade json into a config read, one per loader.
     * Looked up by name so this class stays in the loader neutral tree; only the running loader's
     * copy is ever on the classpath, and the suite runs on both.
     */
    private static final List<String> CONDITION_CLASSES = List.of(
            "com.simplebuilding.condition.ConfigResourceCondition",
            "com.simplebuilding.neoforge.ConfigLoadCondition");

    /**
     * Every vanilla loot table the mod hands pools to - all sixteen keys
     * {@code ModLootTableModifications.apply} names, not a sample of them.
     *
     * <p>The list has to be complete, because it is what the switched-off half of
     * {@link #lootTableChangesStopWhenTheOptionIsSwitchedOff} walks. A table missing from here is
     * a table whose block could be moved above the config guard - or deleted outright - without
     * anything going red.
     */
    private static final List<ResourceKey<LootTable>> MODIFIED_TABLES = List.of(
            BuiltInLootTables.STRONGHOLD_LIBRARY,
            BuiltInLootTables.END_CITY_TREASURE,
            BuiltInLootTables.ANCIENT_CITY,
            BuiltInLootTables.BASTION_TREASURE,
            BuiltInLootTables.BASTION_OTHER,
            BuiltInLootTables.NETHER_BRIDGE,
            BuiltInLootTables.PILLAGER_OUTPOST,
            BuiltInLootTables.WOODLAND_MANSION,
            BuiltInLootTables.BURIED_TREASURE,
            BuiltInLootTables.SIMPLE_DUNGEON,
            BuiltInLootTables.SHIPWRECK_TREASURE,
            BuiltInLootTables.IGLOO_CHEST,
            BuiltInLootTables.ABANDONED_MINESHAFT,
            BuiltInLootTables.TRIAL_CHAMBERS_REWARD_COMMON,
            BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE,
            BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS);

    /** The bastion pool, which one condition hands to both {@code BASTION_*} keys. */
    private static final Set<String> BASTION_LOOT = Set.of(
            book(ModEnchantments.FUNNEL, 1),
            book(ModEnchantments.BREAK_THROUGH, 1),
            id(ModItems.GOLD_SLEDGEHAMMER),
            id(ModItems.GOLD_CORE),
            id(ModItems.NETHERITE_CORE),
            id(ModItems.NETHERITE_NUGGET),
            id(ModItems.NETHERITE_CARROT),
            id(ModItems.ENCHANTED_NETHERITE_APPLE));

    /** The vault pool behind {@code TRIAL_CHAMBERS_REWARD_COMMON} - and the rare vault. */
    private static final Set<String> VAULT_COMMON_LOOT = Set.of(
            book(ModEnchantments.CONSTRUCTORS_TOUCH, 1),
            book(ModEnchantments.FAST_CHISELING, 2));

    /** The vault pool behind {@code TRIAL_CHAMBERS_REWARD_OMINOUS} - and the rare vault. */
    private static final Set<String> VAULT_OMINOUS_LOOT = Set.of(
            book(ModEnchantments.MASTER_BUILDER, 1),
            book(ModEnchantments.DOUBLE_JUMP, 1),
            id(ModItems.DIAMOND_CORE),
            id(ModItems.NETHERITE_APPLE),
            id(ModItems.ENCHANTED_NETHERITE_APPLE));

    /**
     * What each of those tables has to be able to hand out, written as
     * {@code <enchantment id>@<level>} for an enchanted book and as the plain registry id for
     * every other item.
     *
     * <p>Counting pools is only half a promise. A pool that quietly lost an entry is still a pool,
     * and outside a handful of items with a home of their own - the reinforced bundle
     * ({@code BundleWiringTests}), the mining books here - nothing in the suite ever looked inside
     * a mod loot pool. Every entry below is therefore <em>rolled</em> out of the table: deleting
     * one {@code .add(...)} line, swapping the item behind it or moving an enchanted book down a
     * level turns this red, while the pool count and the config switch stay exactly as they were.
     *
     * <p>For the plain items the sets are lower bounds: adding an item to a pool is not a
     * regression and does not have to be listed here. For the enchanted books they are exact in
     * both directions - a book that is rolled out of a mod pool without being written down here
     * fails just as a listed one that never appears does. Which enchantments a chest can hand out
     * is the mod's progression, and adding a book moves nothing else this test watches: not the
     * pool count, not the config switch, and not one of the listed entries.
     *
     * <p>For the two mining enchantments these tables are the entire supply in the game - neither
     * is in the enchanting table, and only Strip Miner appears in a trade at all.
     */
    private static final Map<ResourceKey<LootTable>, Set<String>> EXPECTED_LOOT = Map.ofEntries(
            Map.entry(BuiltInLootTables.STRONGHOLD_LIBRARY, Set.of(
                    book(ModEnchantments.RANGE, 2),
                    book(ModEnchantments.MASTER_BUILDER, 1),
                    book(ModEnchantments.VERSATILITY, 1),
                    book(ModEnchantments.VERSATILITY, 2))),
            Map.entry(BuiltInLootTables.END_CITY_TREASURE, Set.of(
                    // the two pre built pools - the only place addBuiltPool is used at all
                    id(ModItems.ENDERITE_SCRAP),
                    id(ModItems.ENDERITE_UPGRADE_TEMPLATE),
                    book(ModEnchantments.RANGE, 3),
                    book(ModEnchantments.MASTER_BUILDER, 1),
                    book(ModEnchantments.OVERRIDE, 2),
                    book(ModEnchantments.DOUBLE_JUMP, 2),
                    book(ModEnchantments.VERSATILITY, 1),
                    book(ModEnchantments.VERSATILITY, 2),
                    id(ModItems.DIAMOND_BUILDING_WAND),
                    id(ModItems.DIAMOND_SLEDGEHAMMER),
                    id(ModItems.ENCHANTED_ENDERITE_APPLE))),
            Map.entry(BuiltInLootTables.ANCIENT_CITY, Set.of(
                    book(ModEnchantments.DEEP_POCKETS, 2),
                    book(ModEnchantments.RADIUS, 1),
                    id(ModItems.OCTANT),
                    id(ModItems.DIAMOND_SLEDGEHAMMER),
                    id(ModItems.QUIVER),
                    id(ModItems.NETHERITE_APPLE),
                    id(ModItems.ENCHANTED_NETHERITE_APPLE),
                    id(ModItems.NETHERITE_NUGGET))),
            Map.entry(BuiltInLootTables.BASTION_TREASURE, BASTION_LOOT),
            // The same pool, reached through the second half of the same condition. Rolling it is
            // what makes deleting "|| BASTION_OTHER.equals(key)" a red test.
            Map.entry(BuiltInLootTables.BASTION_OTHER, BASTION_LOOT),
            Map.entry(BuiltInLootTables.NETHER_BRIDGE, Set.of(
                    book(ModEnchantments.STRIP_MINER, 1),
                    book(ModEnchantments.STRIP_MINER, 2),
                    book(ModEnchantments.FUNNEL, 1),
                    book(ModEnchantments.BREAK_THROUGH, 1),
                    id(ModItems.GOLD_CORE),
                    id(ModItems.OCTANT),
                    id(ModItems.NETHERITE_NUGGET),
                    id(ModItems.NETHERITE_CARROT))),
            Map.entry(BuiltInLootTables.PILLAGER_OUTPOST, Set.of(
                    book(ModEnchantments.COLOR_PALETTE, 1),
                    book(ModEnchantments.COVER, 1),
                    book(ModEnchantments.LINEAR, 1),
                    id(ModItems.OCTANT),
                    id(ModItems.QUIVER))),
            Map.entry(BuiltInLootTables.WOODLAND_MANSION, Set.of(
                    book(ModEnchantments.COLOR_PALETTE, 1),
                    book(ModEnchantments.COVER, 1),
                    book(ModEnchantments.LINEAR, 1),
                    book(ModEnchantments.VEIN_MINER, 4),
                    book(ModEnchantments.VEIN_MINER, 5),
                    id(ModItems.IRON_BUILDING_WAND),
                    id(ModItems.IRON_CORE),
                    id(ModItems.QUIVER))),
            Map.entry(BuiltInLootTables.BURIED_TREASURE, Set.of(
                    book(ModEnchantments.CONSTRUCTORS_TOUCH, 1),
                    book(ModEnchantments.FAST_CHISELING, 2),
                    id(ModItems.GOLD_CHISEL),
                    id(ModItems.DIAMOND_CHISEL))),
            Map.entry(BuiltInLootTables.SIMPLE_DUNGEON, Set.of(
                    book(ModEnchantments.FAST_CHISELING, 1),
                    book(ModEnchantments.FUNNEL, 1),
                    book(ModEnchantments.BREAK_THROUGH, 1),
                    book(ModEnchantments.VEIN_MINER, 2),
                    book(ModEnchantments.VEIN_MINER, 3),
                    book(ModEnchantments.VEIN_MINER, 4),
                    id(ModItems.REINFORCED_BUNDLE),
                    id(ModItems.BASIC_UPGRADE_TEMPLATE))),
            Map.entry(BuiltInLootTables.SHIPWRECK_TREASURE, Set.of(
                    book(ModEnchantments.FAST_CHISELING, 1),
                    id(ModItems.REINFORCED_BUNDLE))),
            Map.entry(BuiltInLootTables.IGLOO_CHEST, Set.of(
                    book(ModEnchantments.CONSTRUCTORS_TOUCH, 1),
                    book(ModEnchantments.FAST_CHISELING, 1),
                    id(ModItems.DIAMOND_CHISEL))),
            Map.entry(BuiltInLootTables.ABANDONED_MINESHAFT, Set.of(
                    book(ModEnchantments.FAST_CHISELING, 1),
                    book(ModEnchantments.STRIP_MINER, 1),
                    book(ModEnchantments.STRIP_MINER, 3),
                    book(ModEnchantments.VEIN_MINER, 3),
                    book(ModEnchantments.VEIN_MINER, 4),
                    id(ModItems.REINFORCED_BUNDLE))),
            Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_COMMON, VAULT_COMMON_LOOT),
            Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS, VAULT_OMINOUS_LOOT),
            // The rare vault is the one key both vault conditions match, so it gets both pools.
            Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE, union(VAULT_COMMON_LOOT, VAULT_OMINOUS_LOOT)));

    /** Separator {@link #book} puts between an enchantment id and its level. */
    private static final String BOOK_MARKER = "@";

    /**
     * The enchantments the mod deliberately gives no chest at all: Drawer and Kinetic Protection
     * are meant to be reachable from the creative inventory and nowhere else, so no mod loot pool
     * may ever hand out a book for them.
     *
     * <p>This is the one thing about the loot tables that cannot be pinned by listing something,
     * because it is a statement about what is absent. Adding one {@code .add(enchantedBook(...))}
     * line for either of them turns them into chest enchantments and leaves the pool counts, the
     * config switch and every listed entry exactly where they were, so it is checked against what
     * the pools actually roll - and against {@link #EXPECTED_LOOT} itself, so that writing such a
     * book into both lists at once is not a way past it either.
     */
    private static final Set<String> ENCHANTMENTS_WITHOUT_A_CHEST = Set.of(
            ModEnchantments.DRAWER.identifier().toString(),
            ModEnchantments.KINETIC_PROTECTION.identifier().toString());

    /**
     * How often each recorded pool is rolled when looking for the entries above.
     *
     * <p>The thinnest wanted entry decides this number. That is the enchanted netherite apple in
     * the ominous vault: 1 of that pool's 57 weight, drawn {@code between(0, 1)} times, so this
     * many rolls are worth about 18 expected hits and missing it by chance is one in a hundred
     * million. Runner up is the basic upgrade template in the simple dungeon (1 of 96, rolled
     * {@code between(0, 2)}) at about 21. Everything else in {@link #EXPECTED_LOOT} sits far above
     * that - the mansion's iron building wand is worth about 320.
     *
     * <p>Those margins are the reason a thin entry may be listed at all; they are computed from
     * the weights in {@code ModLootTableModifications}, so a balance change that makes an entry
     * much rarer has to be reflected here.
     */
    private static final int POOL_ROLLS = 2048;

    /** Seed for the loot rolls, so a failure is reproducible instead of a coin flip. */
    private static final long POOL_ROLL_SEED = 20260904L;

    /** Vanilla loot tables the mod must never touch - the control group for the recorder. */
    private static final List<ResourceKey<LootTable>> UNTOUCHED_TABLES = List.of(
            BuiltInLootTables.SPAWN_BONUS_CHEST,
            BuiltInLootTables.DESERT_PYRAMID);

    /**
     * Every option the config persists, as {@code group.name type=default}. Statics carry no
     * default because they are not part of the saved file - see
     * {@link #everyConfigOptionKeepsItsPersistedNameAndDefault}.
     */
    private static final Set<String> EXPECTED_OPTIONS = Set.of(
            "root.tools group:Tools",
            "root.worldGen group:WorldGen",
            "root.enableDoubleJump boolean=true",
            "root.airJumpCooldownTicks int=100",
            "root.enableArmorTrimBenefits boolean=true",
            "root.trimBenefitBaseMultiplier double runtime-only(static)",
            "root.maxMultiplierLimit double runtime-only(static)",
            "tools.invertOctantSneak boolean=false",
            "tools.buildingHighlightOpacity int=40",
            "tools.enableToolAnimations boolean=true",
            "tools.enableChiselAnimation boolean=true",
            "tools.invertBundleInteractions boolean=false",
            "worldGen.enableVillagerTrades boolean=true",
            "worldGen.enableWanderingTrades boolean=true",
            "worldGen.enableLootTableChanges boolean=true");

    // =====================================================================================
    // tools.invertBundleInteractions
    // =====================================================================================

    /**
     * {@code tools.invertBundleInteractions} swaps the two mouse buttons of the reinforced bundle:
     * off, left click stuffs an item in and right click takes one out; on, it is the other way
     * round. The option is read by two helpers, {@code getInsertClick} and {@code getRemoveClick},
     * and those two are consulted from two independent places: {@code overrideStackedOnOther} (the
     * bundle is on the cursor, the item lies in a slot) and {@code overrideOtherStackedOnMe} (the
     * item is on the cursor, the bundle lies in a slot). Both interactions are driven here in both
     * settings and in both directions - eight combinations - because the player meets both of them
     * and losing either leaves a bundle that can only be filled or only be emptied.
     *
     * <p>What breaks it: dropping either config read, so that one of the two buttons keeps its
     * vanilla meaning while the other flips; flipping the branches in only one of the two
     * interaction methods, so the toggle works when the bundle is picked up but not when it lies
     * in the inventory; or a gate that stops seeing config changes at all, in which case the
     * inverted half fails exactly as the player's toggle would.
     *
     * <p><b>The insert click on a bundle that is nearly full</b> is driven once per binding on top
     * of that, through both interaction methods. Every other case here offers eight stone to an
     * empty bundle, where "the bundle took everything" and "the bundle took what fit" look the
     * same; that hid the one line in each method that decides what happens to the rest. Both count
     * the insert with {@code shrink(added)}, and {@code insertItemIntoBundle} returns less than the
     * offered amount as soon as the room runs out. With the whole offer swallowed instead, a click
     * on a 64 stack with room for 32 would delete the other 32 in front of the player - so the
     * partial case checks the leftover in the slot and on the cursor, both derived from a measured
     * capacity rather than a copied number.
     */
    public static void bundleClickInversionFollowsTheConfiguredOption(GameTestHelper helper) {
        SimplebuildingConfig config = liveConfig(helper);
        ServerPlayer player = mockPlayer(helper);
        boolean original = config.tools.invertBundleInteractions;
        helper.runBeforeTestEnd(() -> Simplebuilding.getConfig().tools.invertBundleInteractions = original);

        try {
            // --- switched off: left click fills, right click empties ---
            setBundleInversion(helper, false);
            assertSlotFillClick(helper, player, ClickAction.PRIMARY, true, "off / left click");
            assertSlotFillClick(helper, player, ClickAction.SECONDARY, false, "off / right click");
            assertSlotEmptyClick(helper, player, ClickAction.SECONDARY, true, "off / right click");
            assertSlotEmptyClick(helper, player, ClickAction.PRIMARY, false, "off / left click");

            assertCursorFillClick(helper, player, ClickAction.PRIMARY, true, "off / cursor / left click");
            assertCursorFillClick(helper, player, ClickAction.SECONDARY, false, "off / cursor / right click");
            assertCursorEmptyClick(helper, player, ClickAction.SECONDARY, true, "off / cursor / right click");
            assertCursorEmptyClick(helper, player, ClickAction.PRIMARY, false, "off / cursor / left click");

            assertPartialFillClick(helper, player, ClickAction.PRIMARY, "off");

            // --- switched on: exactly the other way round ---
            setBundleInversion(helper, true);
            assertSlotFillClick(helper, player, ClickAction.SECONDARY, true, "inverted / right click");
            assertSlotFillClick(helper, player, ClickAction.PRIMARY, false, "inverted / left click");
            assertSlotEmptyClick(helper, player, ClickAction.PRIMARY, true, "inverted / left click");
            assertSlotEmptyClick(helper, player, ClickAction.SECONDARY, false, "inverted / right click");

            assertCursorFillClick(helper, player, ClickAction.SECONDARY, true, "inverted / cursor / right click");
            assertCursorFillClick(helper, player, ClickAction.PRIMARY, false, "inverted / cursor / left click");
            assertCursorEmptyClick(helper, player, ClickAction.PRIMARY, true, "inverted / cursor / left click");
            assertCursorEmptyClick(helper, player, ClickAction.SECONDARY, false, "inverted / cursor / right click");

            assertPartialFillClick(helper, player, ClickAction.SECONDARY, "inverted");
        } finally {
            Simplebuilding.getConfig().tools.invertBundleInteractions = original;
        }

        helper.succeed();
    }

    // =====================================================================================
    // worldGen.enableLootTableChanges
    // =====================================================================================

    /**
     * {@code worldGen.enableLootTableChanges} is the player's only way to keep the mod out of the
     * vanilla chests. It is the very first thing {@code ModLootTableModifications.apply} looks at,
     * and it guards both ways the mod hands a pool to the loader - the builder path
     * ({@code addPool}) and the pre built path ({@code addBuiltPool}).
     *
     * <p>Production calls {@code apply} once per loot table while the datapacks load, which has
     * long happened by the time a gametest runs, so the test drives that entry point directly with
     * a recording editor instead of reloading the world.
     *
     * <p>All sixteen tables are driven rather than a sample, and the end city is checked on both
     * editor paths. That is what separates "the guard is gone" from "the guard moved into one
     * branch": a gate that only still covers the four tables an earlier version of this test knew
     * about would let the nether fortress, the mineshaft, the igloo and the trial chambers through
     * and fail here. The two vanilla tables the mod never touches are recorded in the switched-on
     * state and have to come back empty - they prove the recorder reports zero when nothing is
     * added, so the switched-off half cannot pass merely because the recorder broke.
     *
     * <p>Counting pools is only half the promise, though. A pool that lost an entry is still a
     * pool, so every table is additionally <em>rolled</em>: {@link #EXPECTED_LOOT} names the books
     * and items each chest has to be able to hand out, and {@link #POOL_ROLLS} draws have to
     * produce every one of them. For most of those entries this is their only coverage - deleting
     * a single {@code .add(...)} line leaves the pool non-empty, the option still switches it off
     * and on, and nothing else in the suite ever looks inside.
     *
     * <p>The books are held to the list in both directions on top of that. An entry that is
     * <em>added</em> to a pool is normally nobody's business here, but an enchanted book is not an
     * item like any other: it decides whether an enchantment can be found in the world at all.
     * {@link #ENCHANTMENTS_WITHOUT_A_CHEST} names the two the mod documents as having no chest
     * anywhere, and the same reading catches every other undeclared book with it.
     *
     * <p>What breaks it: deleting the guard, so a player who switched the mod's loot off finds mod
     * books in a stronghold library anyway; moving the guard inside one of the branches, or
     * putting an ungated table block above it; a table quietly losing its pools while the option
     * is on; any listed entry losing its item, its level or its whole chest; or a pool growing a
     * book that is not written down - a Drawer book in the ancient city would make a creative-only
     * enchantment findable while every count and every switch above stayed as it is.
     */
    public static void lootTableChangesStopWhenTheOptionIsSwitchedOff(GameTestHelper helper) {
        SimplebuildingConfig config = liveConfig(helper);
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        boolean original = config.worldGen.enableLootTableChanges;
        helper.runBeforeTestEnd(() -> Simplebuilding.getConfig().worldGen.enableLootTableChanges = original);

        try {
            // --- switched on: the mod puts its pools in ---
            setLootTableChanges(helper, true);

            for (ResourceKey<LootTable> key : MODIFIED_TABLES) {
                PoolRecorder recorder = recordPools(key, registries);
                helper.assertTrue(recorder.total() > 0,
                        tableName(key) + " got no mod pool although the loot table option is on");
            }

            PoolRecorder endCity = recordPools(BuiltInLootTables.END_CITY_TREASURE, registries);
            helper.assertTrue(endCity.builders > 0,
                    "the end city treasure got no builder pool although the loot table option is on");
            helper.assertTrue(endCity.built > 0,
                    "the end city treasure got no pre built pool although the loot table option is on");

            for (ResourceKey<LootTable> key : UNTOUCHED_TABLES) {
                helper.assertValueEqual(recordPools(key, registries).total(), 0,
                        "pools added to " + tableName(key) + ", a table the mod does not touch");
            }

            // --- and what is in those pools, rolled out of them for real ---
            List<String> missing = new ArrayList<>();
            List<String> stray = new ArrayList<>();
            for (ResourceKey<LootTable> key : MODIFIED_TABLES) {
                Set<String> wanted = EXPECTED_LOOT.get(key);
                helper.assertTrue(wanted != null,
                        tableName(key) + " is driven for its pool count but nothing is expected out of "
                                + "it; add its entries to EXPECTED_LOOT or the pool could be emptied");
                Set<String> rolled = rollContents(helper, recordPools(key, registries));
                for (String entry : wanted) {
                    if (!rolled.contains(entry)) {
                        missing.add(tableName(key) + " never handed out " + entry + " in " + POOL_ROLLS
                                + " rolls; what it did hand out was " + rolled);
                    }
                }
                stray.addAll(strayBooks(key, wanted, rolled));
            }
            helper.assertTrue(missing.isEmpty(),
                    "the mod loot pools no longer contain what they are documented to contain:\n"
                            + String.join("\n", missing));
            helper.assertTrue(stray.isEmpty(),
                    "the mod loot pools hand out enchanted books nobody wrote down for them. An added "
                            + "book is the one pool change that moves nothing else this test watches - "
                            + "the pool count, the config switch and every listed entry stay as they "
                            + "are - while it decides whether an enchantment can be found in the world "
                            + "at all:\n" + String.join("\n", stray));

            // Guards the list above against the obvious way around it: writing a banned enchantment
            // into EXPECTED_LOOT would make its book "expected" and silence the check.
            List<String> bannedButListed = new ArrayList<>();
            for (Map.Entry<ResourceKey<LootTable>, Set<String>> expected : EXPECTED_LOOT.entrySet()) {
                for (String entry : expected.getValue()) {
                    if (bannedEnchantment(entry) != null) {
                        bannedButListed.add(tableName(expected.getKey()) + " lists " + entry);
                    }
                }
            }
            helper.assertTrue(bannedButListed.isEmpty(),
                    "an enchantment out of ENCHANTMENTS_WITHOUT_A_CHEST was written into EXPECTED_LOOT, "
                            + "which would let a chest hand it out with every assertion above satisfied; "
                            + "the two lists have to keep disagreeing:\n"
                            + String.join("\n", bannedButListed));

            // --- switched off: nothing at all, on either path ---
            setLootTableChanges(helper, false);

            for (ResourceKey<LootTable> key : MODIFIED_TABLES) {
                helper.assertValueEqual(recordPools(key, registries).total(), 0,
                        "pools were added to " + tableName(key)
                                + " although the loot table option is switched off");
            }
        } finally {
            Simplebuilding.getConfig().worldGen.enableLootTableChanges = original;
        }

        helper.succeed();
    }

    // =====================================================================================
    // worldGen.enableVillagerTrades / enableWanderingTrades
    // =====================================================================================

    /**
     * The two trade switches are not read from code at all - each shipped trade json names the flag
     * itself, once for Fabric ({@code fabric:load_conditions}) and once for NeoForge
     * ({@code neoforge:conditions}), and the loader hands that string to the mod's condition. Both
     * conditions resolve the string in a hard coded {@code switch} over exactly two cases and
     * answer anything else with {@code true} plus a log line, so a typo, a copy pasted folder or a
     * trade gated on a third flag does not fail anything: the switch simply stops working and the
     * trade ships regardless.
     *
     * <p>So this test reads the shipped files back out of the server's resource manager and checks
     * the wiring end to end: every mod trade carries both loaders' conditions, both name the same
     * flag, the flag matches the folder the trade lives in, and the set of flags in use is exactly
     * the two the conditions can actually resolve. The reflective look up on
     * {@code SimplebuildingConfig.WorldGen} that follows is the secondary check - it says whether
     * the flag still corresponds to a readable, non static boolean option, which is what the
     * conditions ultimately return.
     *
     * <p><b>The switch itself is driven, not described.</b> The two flag names above are literals
     * in this file, so comparing the jsons against them says nothing about the {@code case} labels
     * the loader's condition is written with. Rename the constant one of those cases uses - the
     * json and the config field keep their spelling, so everything else here still lines up - and
     * every trade in that folder falls into the {@code default} branch, ships regardless of the
     * player's toggle and leaves a log line as the only trace. Each flag is therefore handed to the
     * loader's real condition object twice: once with its own option switched off, where the
     * condition has to answer {@code false}, and once with the <em>other</em> flag's option
     * switched off, where it has to stay {@code true}. The second half is what separates a working
     * switch from one whose two cases end up reading the same field. Only the running loader's
     * class can be reached this way - the suite runs on both, and a run that finds neither class
     * says so instead of passing on an empty loop.
     *
     * <p>What breaks it: adding a trade with only the Fabric condition, which then ignores the
     * switch on NeoForge (and vice versa); putting a wandering trader offer behind the villager
     * flag, so the wrong toggle turns it off; gating a trade on a flag the two condition classes do
     * not have a {@code case} for - the file would keep shipping with the toggle off, and only a
     * log line would say so. A new trade that is meant to be unconditional fails here too -
     * deliberately, because "this one is not covered by the switch any more" is exactly the
     * decision that should not be made by accident.
     */
    public static void tradeSwitchConditionsStillNameRealConfigFieldsOnBothLoaders(GameTestHelper helper) {
        SimplebuildingConfig config = liveConfig(helper);
        boolean villagerOriginal = config.worldGen.enableVillagerTrades;
        boolean wanderingOriginal = config.worldGen.enableWanderingTrades;
        helper.runBeforeTestEnd(() -> {
            Simplebuilding.getConfig().worldGen.enableVillagerTrades = villagerOriginal;
            Simplebuilding.getConfig().worldGen.enableWanderingTrades = wanderingOriginal;
        });
        Map<Identifier, Resource> tradeFiles = helper.getLevel().getServer().getResourceManager()
                .listResources(TRADE_DIRECTORY, id -> id.getPath().endsWith(".json"));

        List<String> problems = new ArrayList<>();
        Set<String> flagsSeen = new TreeSet<>();
        int checked = 0;

        for (Map.Entry<Identifier, Resource> entry : tradeFiles.entrySet()) {
            Identifier id = entry.getKey();
            if (!SimpleBuildingGameTests.MOD_ID.equals(id.getNamespace())) {
                continue; // vanilla or another mod's trades, not ours to police
            }
            checked++;

            JsonObject json;
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                json = GsonHelper.parse(reader);
            } catch (IOException | RuntimeException e) {
                problems.add(id + ": could not be read as json (" + e + ")");
                continue;
            }

            String fabricFlag = configFlag(json, "fabric:load_conditions", "condition");
            String neoFlag = configFlag(json, "neoforge:conditions", "type");
            String expected = id.getPath().startsWith(TRADE_DIRECTORY + "/" + WANDERING_DIRECTORY + "/")
                    ? WANDERING_FLAG
                    : VILLAGER_FLAG;

            if (fabricFlag == null) {
                problems.add(id + ": no \"fabric:load_conditions\" entry of type " + CONFIG_CONDITION
                        + " - on Fabric this trade ships no matter how the config switch is set");
            } else {
                flagsSeen.add(fabricFlag);
            }
            if (neoFlag == null) {
                problems.add(id + ": no \"neoforge:conditions\" entry of type " + CONFIG_CONDITION
                        + " - on NeoForge this trade ships no matter how the config switch is set");
            } else {
                flagsSeen.add(neoFlag);
            }
            if (fabricFlag != null && neoFlag != null && !fabricFlag.equals(neoFlag)) {
                problems.add(id + ": the two loaders are gated on different flags (Fabric " + fabricFlag
                        + ", NeoForge " + neoFlag + ")");
            }
            if (fabricFlag != null && !expected.equals(fabricFlag)) {
                problems.add(id + ": gated on " + fabricFlag + ", but the folder it lives in belongs to "
                        + expected);
            }
        }

        helper.assertTrue(checked > 0,
                "no " + SimpleBuildingGameTests.MOD_ID + " trade json was found under data/"
                        + SimpleBuildingGameTests.MOD_ID + "/" + TRADE_DIRECTORY
                        + "; without files to read this test proves nothing, so the listing itself is broken");

        for (String flag : flagsSeen) {
            problems.addAll(worldGenFlagProblems(flag));
        }
        problems.addAll(conditionSwitchProblems(flagsSeen));

        helper.assertTrue(problems.isEmpty(),
                "the trade files and the config no longer line up:\n" + String.join("\n", problems));
        helper.assertValueEqual(flagsSeen, Set.of(VILLAGER_FLAG, WANDERING_FLAG),
                "the set of config flags the shipped trades are gated on - the two loader conditions "
                        + "resolve exactly these two strings and answer every other one with \"true\"");

        helper.succeed();
    }

    // =====================================================================================
    // THE OPTION SET ITSELF
    // =====================================================================================

    /**
     * Pins the saved config's identity: the file it is written to, and every option's name, group,
     * type and - for the persisted ones - default. Renaming or removing an option is silent by
     * construction. The code that reads it is renamed with it, so nothing fails to compile and
     * nothing fails at runtime; what happens instead is that the key in every player's
     * {@code config/simplebuilding.json} stops matching, their setting is dropped on the next load
     * and the option quietly reverts to its default. The file name behaves the same way, one level
     * up: change the {@code name} in the {@code @Config} annotation and every existing
     * {@code simplebuilding.json} is orphaned in place, with the mod happily writing a new file
     * beside it. This test is the place where both have to be a deliberate act.
     *
     * <p>The annotation is read by its fully qualified name instead of importing Cloth Config, so
     * this stays in the loader neutral tree and does not care which loader supplies
     * {@code me.shedaniel.autoconfig} (the Forge module ships a shim of its own).
     *
     * <p>The two multiplier fields are static, and the serializer both loaders use is Gson based,
     * which skips static fields. They are therefore runtime only: the {@code /simplebuilding}
     * command can change them for the session, but nothing writes them to disk and nothing reads
     * them back at startup. They are listed as such rather than with a default, so making one of
     * them an instance field (or making a persisted option static, which would silently stop saving
     * it) fails here. It also keeps this test independent of the trim tests, which write those two
     * statics while they run.
     *
     * <p>What breaks it: renaming, removing, retyping or re-grouping an option, changing a default,
     * adding a new one without listing it, or renaming the config file. All of those are fine
     * changes to make - they just have to be made on purpose, in one place, next to this
     * explanation.
     */
    public static void everyConfigOptionKeepsItsPersistedNameAndDefault(GameTestHelper helper) {
        SimplebuildingConfig defaults = new SimplebuildingConfig();

        List<String> problems = new ArrayList<>();
        Set<String> found = new TreeSet<>();
        collectOptions(found, problems, "root", SimplebuildingConfig.class, defaults);
        collectOptions(found, problems, "tools", SimplebuildingConfig.Tools.class, defaults.tools);
        collectOptions(found, problems, "worldGen", SimplebuildingConfig.WorldGen.class, defaults.worldGen);

        helper.assertTrue(problems.isEmpty(),
                "config options could not be read by reflection:\n" + String.join("\n", problems));
        helper.assertValueEqual(found, new TreeSet<>(EXPECTED_OPTIONS),
                "the set of config options (name, group, type, default)");

        String fileName = declaredConfigFileName();
        helper.assertTrue(fileName != null,
                "SimplebuildingConfig no longer carries a runtime visible " + CONFIG_ANNOTATION
                        + " annotation, so nothing here can tell which file the options are saved to");
        helper.assertValueEqual(fileName, SimpleBuildingGameTests.MOD_ID,
                "the name in @Config on SimplebuildingConfig - it is the base name of the file under "
                        + "config/, so changing it orphans every existing simplebuilding.json");

        // Every option test in this class writes through Simplebuilding.getConfig() and expects the
        // code under test to read the same object back. If that ever stopped being one live object,
        // those tests would fail somewhere far away with a confusing message; this says it plainly.
        SimplebuildingConfig live = liveConfig(helper);
        helper.assertTrue(Simplebuilding.getConfig() == live,
                "getConfig() handed out a different config object on the second call, so an option "
                        + "written through it would never reach the code that reads it");
        helper.assertTrue(Simplebuilding.getConfig().tools == live.tools
                        && Simplebuilding.getConfig().worldGen == live.worldGen,
                "the option groups behind getConfig() are rebuilt per call, so writing an option in a "
                        + "group has no effect on the code that reads it");

        helper.succeed();
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /** The config object the mod actually reads, with the two groups proven to exist. */
    private static SimplebuildingConfig liveConfig(GameTestHelper helper) {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        helper.assertTrue(config != null,
                "Simplebuilding.getConfig() handed out no config at all; every option below would be untestable");
        helper.assertTrue(config.tools != null, "the config has no \"tools\" group");
        helper.assertTrue(config.worldGen != null, "the config has no \"worldGen\" group");
        return config;
    }

    /**
     * Creates a mock server player and makes sure it leaves the server again once the test is over.
     * A leaked mock player keeps the player list non-empty and the gametest server then stalls on
     * shutdown - a failing test would cost minutes of wall clock instead of seconds.
     */
    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(3.5, 1.0, 3.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /**
     * Writes an option and reads it straight back through the same accessor the mod uses. If
     * {@code getConfig()} ever started handing out copies instead of the live object, the tests
     * would otherwise fail somewhere far away with a confusing message; this fails right here.
     */
    private static void setBundleInversion(GameTestHelper helper, boolean value) {
        Simplebuilding.getConfig().tools.invertBundleInteractions = value;
        helper.assertTrue(Simplebuilding.getConfig().tools.invertBundleInteractions == value,
                "tools.invertBundleInteractions did not keep the value it was just set to; "
                        + "getConfig() is not handing out the live config object");
    }

    /** See {@link #setBundleInversion}. */
    private static void setLootTableChanges(GameTestHelper helper, boolean value) {
        Simplebuilding.getConfig().worldGen.enableLootTableChanges = value;
        helper.assertTrue(Simplebuilding.getConfig().worldGen.enableLootTableChanges == value,
                "worldGen.enableLootTableChanges did not keep the value it was just set to; "
                        + "getConfig() is not handing out the live config object");
    }

    // --- interaction 1: bundle on the cursor, item in the slot (overrideStackedOnOther) ---

    /**
     * One click with the bundle on the cursor while the slot below holds eight stone. Off, that is
     * what fills the bundle; inverted, it is what does nothing at all.
     */
    private static void assertSlotFillClick(GameTestHelper helper, ServerPlayer player, ClickAction action,
                                            boolean expectFill, String what) {
        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ReinforcedBundleItem item = (ReinforcedBundleItem) bundle.getItem();
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, new ItemStack(Items.STONE, 8));
        Slot slot = new Slot(container, 0, 0, 0);

        boolean handled = item.overrideStackedOnOther(bundle, slot, action, player);

        if (expectFill) {
            helper.assertTrue(handled, what + ": the click that should fill the bundle was not handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 8,
                    what + ": stone that ended up inside the bundle");
            helper.assertTrue(slot.getItem().isEmpty(),
                    what + ": the slot should be empty afterwards but holds " + slot.getItem());
        } else {
            helper.assertTrue(!handled, what + ": this button must do nothing here, but the click was handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 0,
                    what + ": stone that ended up inside the bundle");
            helper.assertValueEqual(slot.getItem().getCount(), 8, what + ": stone left in the slot");
        }
    }

    /**
     * One click with a bundle that holds eight stone on the cursor, over an empty slot. Off, that
     * is what takes the stone back out; inverted, it is what does nothing at all.
     */
    private static void assertSlotEmptyClick(GameTestHelper helper, ServerPlayer player, ClickAction action,
                                             boolean expectEmpty, String what) {
        ItemStack bundle = filledBundle(helper, player, what);
        ReinforcedBundleItem item = (ReinforcedBundleItem) bundle.getItem();
        SimpleContainer container = new SimpleContainer(1);
        Slot slot = new Slot(container, 0, 0, 0);

        boolean handled = item.overrideStackedOnOther(bundle, slot, action, player);

        if (expectEmpty) {
            helper.assertTrue(handled, what + ": the click that should empty the bundle was not handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 0,
                    what + ": stone still inside the bundle");
            helper.assertValueEqual(slot.getItem().getCount(), 8, what + ": stone handed back to the slot");
        } else {
            helper.assertTrue(!handled, what + ": this button must do nothing here, but the click was handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 8,
                    what + ": stone still inside the bundle");
            helper.assertTrue(slot.getItem().isEmpty(),
                    what + ": the slot should have stayed empty but holds " + slot.getItem());
        }
    }

    // --- interaction 2: item on the cursor, bundle in the slot (overrideOtherStackedOnMe) ---

    /**
     * The same fill, from the other side: eight stone on the cursor, clicked onto the bundle lying
     * in a slot. This is the second pair of config reads and the one the player uses most.
     */
    private static void assertCursorFillClick(GameTestHelper helper, ServerPlayer player, ClickAction action,
                                              boolean expectFill, String what) {
        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ReinforcedBundleItem item = (ReinforcedBundleItem) bundle.getItem();
        ItemStack cursor = new ItemStack(Items.STONE, 8);
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, bundle);
        Slot slot = new Slot(container, 0, 0, 0);
        ItemStack[] cursorSlot = {cursor};

        boolean handled = item.overrideOtherStackedOnMe(bundle, cursor, slot, action, player,
                SlotAccess.of(() -> cursorSlot[0], stack -> cursorSlot[0] = stack));

        if (expectFill) {
            helper.assertTrue(handled, what + ": the click that should fill the bundle was not handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 8,
                    what + ": stone that ended up inside the bundle");
            helper.assertTrue(cursor.isEmpty(),
                    what + ": the cursor should be empty afterwards but holds " + cursor);
        } else {
            helper.assertTrue(!handled, what + ": this button must do nothing here, but the click was handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 0,
                    what + ": stone that ended up inside the bundle");
            helper.assertValueEqual(cursor.getCount(), 8, what + ": stone left on the cursor");
        }
    }

    /**
     * The same emptying, from the other side: an empty cursor clicked onto a filled bundle lying in
     * a slot. The stack comes back onto the cursor through the {@code SlotAccess}, which is what
     * this records.
     */
    private static void assertCursorEmptyClick(GameTestHelper helper, ServerPlayer player, ClickAction action,
                                               boolean expectEmpty, String what) {
        ItemStack bundle = filledBundle(helper, player, what);
        ReinforcedBundleItem item = (ReinforcedBundleItem) bundle.getItem();
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, bundle);
        Slot slot = new Slot(container, 0, 0, 0);
        ItemStack[] cursorSlot = {ItemStack.EMPTY};

        boolean handled = item.overrideOtherStackedOnMe(bundle, ItemStack.EMPTY, slot, action, player,
                SlotAccess.of(() -> cursorSlot[0], stack -> cursorSlot[0] = stack));

        if (expectEmpty) {
            helper.assertTrue(handled, what + ": the click that should empty the bundle was not handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 0,
                    what + ": stone still inside the bundle");
            helper.assertValueEqual(cursorSlot[0].getCount(), 8,
                    what + ": stone handed back onto the cursor");
        } else {
            helper.assertTrue(!handled, what + ": this button must do nothing here, but the click was handled");
            helper.assertValueEqual(countInBundle(bundle, Items.STONE), 8,
                    what + ": stone still inside the bundle");
            helper.assertTrue(cursorSlot[0].isEmpty(),
                    what + ": the cursor should have stayed empty but holds " + cursorSlot[0]);
        }
    }

    // --- the partial insert, on both interaction paths ---

    /**
     * One insert click on a bundle that has room for only part of the offered stack, driven
     * through both interaction methods. Everything else in this test offers eight stone to an
     * empty bundle and therefore never reaches the branch where the bundle takes less than it was
     * given.
     *
     * <p>Nothing is copied out of the item: the capacity is measured on a scratch bundle first,
     * the room left after one 64 stack and the expected leftover are derived from that
     * measurement, and the setup guard states the assumption that derivation rests on. A balance
     * change to the capacity table moves the numbers with it instead of turning this red.
     */
    private static void assertPartialFillClick(GameTestHelper helper, ServerPlayer player,
                                               ClickAction insertClick, String what) {
        int capacity = measureStoneCapacity(helper, player);
        helper.assertTrue(capacity > 64 && capacity < 128,
                what + " / partial: setup guard - a reinforced bundle takes " + capacity + " stone, so "
                        + "one vanilla stack no longer leaves a remainder between 1 and 63 and this case "
                        + "would stop being a partial insert at all");
        int room = capacity - 64;
        int leftOver = 64 - room;

        // --- bundle on the cursor, the offered stack lying in the slot ---
        ItemStack onCursor = nearlyFullBundle(helper, player, what);
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, new ItemStack(Items.STONE, 64));
        Slot slot = new Slot(container, 0, 0, 0);

        helper.assertTrue(((ReinforcedBundleItem) onCursor.getItem())
                        .overrideStackedOnOther(onCursor, slot, insertClick, player),
                what + " / partial / slot: the insert click was not handled although " + room
                        + " more stone still fit");
        helper.assertValueEqual(countInBundle(onCursor, Items.STONE), capacity,
                what + " / partial / slot: stone inside the bundle - it may take only the " + room
                        + " that fit");
        helper.assertValueEqual(slot.getItem().getCount(), leftOver,
                what + " / partial / slot: stone left in the slot - the bundle took " + room + " of 64, "
                        + "so the other " + leftOver + " have to stay where the player put them");

        // --- the offered stack on the cursor, the bundle lying in the slot ---
        ItemStack inSlot = nearlyFullBundle(helper, player, what);
        ItemStack cursor = new ItemStack(Items.STONE, 64);
        SimpleContainer holder = new SimpleContainer(1);
        holder.setItem(0, inSlot);
        Slot bundleSlot = new Slot(holder, 0, 0, 0);
        ItemStack[] cursorSlot = {cursor};

        helper.assertTrue(((ReinforcedBundleItem) inSlot.getItem()).overrideOtherStackedOnMe(
                        inSlot, cursor, bundleSlot, insertClick, player,
                        SlotAccess.of(() -> cursorSlot[0], stack -> cursorSlot[0] = stack)),
                what + " / partial / cursor: the insert click was not handled although " + room
                        + " more stone still fit");
        helper.assertValueEqual(countInBundle(inSlot, Items.STONE), capacity,
                what + " / partial / cursor: stone inside the bundle - it may take only the " + room
                        + " that fit");
        helper.assertValueEqual(cursor.getCount(), leftOver,
                what + " / partial / cursor: stone left on the cursor - the bundle took " + room
                        + " of 64, so the other " + leftOver + " have to stay on the cursor");
    }

    /**
     * How much stone a fresh reinforced bundle holds, measured by filling one until it refuses.
     * The world pickup path is used because it carries no config gate, so the measurement cannot
     * depend on the option under test.
     */
    private static int measureStoneCapacity(GameTestHelper helper, ServerPlayer player) {
        ItemStack scratch = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ReinforcedBundleItem item = (ReinforcedBundleItem) scratch.getItem();
        for (int offer = 0; offer < 16; offer++) {
            if (!item.tryInsertStackFromWorld(scratch, new ItemStack(Items.STONE, 64), player)) {
                break;
            }
        }
        int capacity = countInBundle(scratch, Items.STONE);
        helper.assertTrue(capacity > 0,
                "a fresh reinforced bundle took no stone at all, so nothing below can be measured");
        return capacity;
    }

    /** A reinforced bundle holding exactly one vanilla stack of stone, so a little room is left. */
    private static ItemStack nearlyFullBundle(GameTestHelper helper, ServerPlayer player, String what) {
        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ReinforcedBundleItem item = (ReinforcedBundleItem) bundle.getItem();
        helper.assertTrue(item.tryInsertStackFromWorld(bundle, new ItemStack(Items.STONE, 64), player),
                what + " / partial: could not put the first 64 stone into the bundle to begin with");
        helper.assertValueEqual(countInBundle(bundle, Items.STONE), 64,
                what + " / partial: stone in the bundle after the world pickup that sets this case up");
        return bundle;
    }

    /**
     * A reinforced bundle holding eight stone. Filled through the world pickup path on purpose:
     * that one carries no config gate, so the setup cannot quietly depend on the option under test.
     */
    private static ItemStack filledBundle(GameTestHelper helper, ServerPlayer player, String what) {
        ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
        ReinforcedBundleItem item = (ReinforcedBundleItem) bundle.getItem();
        helper.assertTrue(item.tryInsertStackFromWorld(bundle, new ItemStack(Items.STONE, 8), player),
                what + ": could not put stone into the bundle to begin with");
        helper.assertValueEqual(countInBundle(bundle, Items.STONE), 8,
                what + ": stone in the bundle after the world pickup that set the test up");
        return bundle;
    }

    /** How many of {@code item} the bundle holds, across all of its stacks. */
    private static int countInBundle(ItemStack bundle, Item item) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
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

    /** Runs the mod's loot table hook for one table and counts what it handed over. */
    private static PoolRecorder recordPools(ResourceKey<LootTable> key, HolderLookup.Provider registries) {
        PoolRecorder recorder = new PoolRecorder();
        ModLootTableModifications.apply(key, recorder, registries);
        return recorder;
    }

    /** Readable name of a loot table for assertion messages. */
    private static String tableName(ResourceKey<LootTable> key) {
        return "the loot table " + key.identifier();
    }

    /**
     * Rolls every pool the mod handed to one table and returns what came out: the registry id of
     * each item, plus {@code <enchantment id>@<level>} for every enchantment stored on an
     * enchanted book.
     *
     * <p>Rolling rather than reading: {@code LootPool} keeps its entries private, and going
     * through {@code addRandomItems} is what a chest does anyway - it covers the entry, its
     * {@code set_components} function and the level inside that component in one step. The seed
     * is fixed and the context is rebuilt per table, so the same pool always produces the same
     * answer here, on every machine.
     */
    private static Set<String> rollContents(GameTestHelper helper, PoolRecorder recorder) {
        LootParams params = new LootParams.Builder(helper.getLevel()).create(LootContextParamSets.EMPTY);
        LootContext context = new LootContext.Builder(params)
                .withOptionalRandomSeed(POOL_ROLL_SEED)
                .create(Optional.empty());

        Set<String> seen = new TreeSet<>();
        for (LootPool pool : recorder.pools) {
            for (int roll = 0; roll < POOL_ROLLS; roll++) {
                pool.addRandomItems(stack -> {
                    Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    if (itemId != null) {
                        seen.add(itemId.toString());
                    }
                    ItemEnchantments stored = stack.getOrDefault(
                            DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
                    for (Holder<Enchantment> enchantment : stored.keySet()) {
                        seen.add(enchantment.getRegisteredName() + BOOK_MARKER + stored.getLevel(enchantment));
                    }
                }, context);
            }
        }
        return seen;
    }

    /** How {@link #EXPECTED_LOOT} spells a plain item: its registry id. */
    private static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    /** How {@link #EXPECTED_LOOT} spells an enchanted book: {@code <enchantment id>@<level>}. */
    private static String book(ResourceKey<Enchantment> enchantment, int level) {
        return enchantment.identifier() + BOOK_MARKER + level;
    }

    /**
     * The enchanted books one table rolled that {@link #EXPECTED_LOOT} does not name, as messages.
     *
     * <p>The books are singled out from the plain items because the two are not the same kind of
     * promise. An extra item in a chest is a balance decision; an extra book changes where an
     * enchantment can be obtained, and for
     * {@link #ENCHANTMENTS_WITHOUT_A_CHEST} it changes whether it can be obtained outside creative
     * mode at all. Only the {@code enchantedBook(...)} entries carry stored enchantments - the
     * randomly enchanted tools and quivers in these pools write the ordinary enchantment component
     * instead - so reading the stored ones reads exactly the book entries.
     */
    private static List<String> strayBooks(ResourceKey<LootTable> key, Set<String> wanted, Set<String> rolled) {
        List<String> problems = new ArrayList<>();
        for (String entry : rolled) {
            if (!entry.contains(BOOK_MARKER) || wanted.contains(entry)) {
                continue;
            }
            String banned = bannedEnchantment(entry);
            if (banned != null) {
                problems.add(tableName(key) + " handed out a " + banned + " book (" + entry
                        + "), an enchantment the mod documents as having no source outside the "
                        + "creative inventory");
            } else {
                problems.add(tableName(key) + " handed out the enchanted book " + entry
                        + ", which is not one of the books written down for it (" + wanted + ")");
            }
        }
        return problems;
    }

    /**
     * The banned enchantment behind an {@code <enchantment id>@<level>} entry, or {@code null} if
     * the entry is a plain item or an enchantment that is allowed in a chest.
     */
    private static String bannedEnchantment(String entry) {
        int marker = entry.indexOf(BOOK_MARKER);
        if (marker < 0) {
            return null;
        }
        String enchantment = entry.substring(0, marker);
        return ENCHANTMENTS_WITHOUT_A_CHEST.contains(enchantment) ? enchantment : null;
    }

    /** The two vault pools the rare vault receives together. */
    private static Set<String> union(Set<String> first, Set<String> second) {
        Set<String> both = new TreeSet<>(first);
        both.addAll(second);
        return Set.copyOf(both);
    }

    /**
     * Counts the pools the mod offers, keeping the two editor paths apart, and keeps them so
     * their contents can be rolled afterwards.
     */
    private static final class PoolRecorder implements ModLootTableModifications.Editor {
        private final List<LootPool> pools = new ArrayList<>();
        private int builders;
        private int built;

        @Override
        public void addPool(LootPool.Builder pool) {
            this.builders++;
            this.pools.add(pool.build());
        }

        @Override
        public void addBuiltPool(LootPool pool) {
            this.built++;
            this.pools.add(pool);
        }

        int total() {
            return this.builders + this.built;
        }
    }

    /**
     * The flag of this file's {@code simplebuilding:config} condition, or {@code null} if it has
     * none. Fabric spells the condition id {@code condition}, NeoForge spells it {@code type};
     * both keys sit in the same file and the foreign one is ignored by each loader.
     */
    private static String configFlag(JsonObject json, String arrayKey, String typeKey) {
        JsonElement array = json.get(arrayKey);
        if (array == null || !array.isJsonArray()) {
            return null;
        }
        for (JsonElement child : array.getAsJsonArray()) {
            if (!child.isJsonObject()) {
                continue;
            }
            JsonObject condition = child.getAsJsonObject();
            JsonElement type = condition.get(typeKey);
            if (type == null || !type.isJsonPrimitive() || !CONFIG_CONDITION.equals(type.getAsString())) {
                continue;
            }
            JsonElement flag = condition.get("flag");
            if (flag != null && flag.isJsonPrimitive()) {
                return flag.getAsString();
            }
        }
        return null;
    }

    /** Everything wrong with a flag name the trade files use, as messages; empty means it is fine. */
    private static List<String> worldGenFlagProblems(String flag) {
        List<String> problems = new ArrayList<>();
        Field field;
        try {
            field = SimplebuildingConfig.WorldGen.class.getField(flag);
        } catch (NoSuchFieldException e) {
            problems.add("the trades are gated on \"" + flag + "\", but SimplebuildingConfig.WorldGen has no "
                    + "such field - both loaders answer a flag they cannot resolve with \"true\", so that "
                    + "switch is dead and only a log line says so");
            return problems;
        }
        if (field.getType() != boolean.class) {
            problems.add("worldGen." + flag + " is a " + field.getType().getSimpleName()
                    + ", but the datapack conditions read it as a boolean");
            return problems;
        }
        if (Modifier.isStatic(field.getModifiers())) {
            problems.add("worldGen." + flag + " became static, so it is no longer part of the saved config");
        }
        try {
            field.getBoolean(Simplebuilding.getConfig().worldGen);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            problems.add("worldGen." + flag + " cannot be read off the live config (" + e + ")");
        }
        return problems;
    }

    /**
     * Feeds every flag the trade files use to the running loader's own {@code simplebuilding:config}
     * condition, so the string in the json is followed all the way into the config field.
     *
     * <p>Everything else about the flags is a comparison against literals spelled out in this file.
     * That leaves the link that does the actual switching unobserved: both conditions resolve the
     * flag in a hard coded {@code switch} and answer an unknown string with {@code true}, so the
     * label of a {@code case} can drift away from the json without anything failing. Driving the
     * condition object is the only way to see that from a test.
     *
     * <p>The classes are looked up by name, and the one belonging to the other loader is simply
     * absent - that is expected and skipped. A run in which <em>neither</em> is present is not: it
     * would mean this whole check quietly did nothing, so it is reported as a problem of its own.
     */
    private static List<String> conditionSwitchProblems(Set<String> flags) {
        List<String> problems = new ArrayList<>();
        int driven = 0;

        for (String className : CONDITION_CLASSES) {
            Class<?> conditionClass;
            try {
                conditionClass = Class.forName(className);
            } catch (ClassNotFoundException | LinkageError e) {
                continue; // the other loader's condition; it is driven when the suite runs there
            }

            Constructor<?> constructor;
            Method test;
            try {
                constructor = conditionClass.getConstructor(String.class);
                test = singleArgumentTest(conditionClass);
            } catch (NoSuchMethodException | RuntimeException e) {
                problems.add(className + " no longer offers a (String flag) constructor plus a one "
                        + "argument test(...) method, so the switch behind the trade flags cannot be "
                        + "driven from here at all (" + e + ")");
                continue;
            }
            driven++;

            for (String flag : flags) {
                problems.addAll(switchProblems(className, constructor, test, flag, flags));
            }
        }

        if (driven == 0) {
            problems.add("none of " + CONDITION_CLASSES + " is on the classpath, so the switch that "
                    + "turns a flag into a config read was never driven; a trade could be gated on a "
                    + "string no case matches and every other assertion here would still hold");
        }
        return problems;
    }

    /**
     * Whether {@code flag} switches its own option and only its own, asked of the real condition.
     *
     * <p>Both directions matter. Its own option off has to shut the trade out, or the {@code case}
     * that resolves this string is gone and the toggle is dead; another flag's option off has to
     * leave it alone, or the two cases have ended up reading one field and one toggle silences the
     * other's trades as well.
     */
    private static List<String> switchProblems(String className, Constructor<?> constructor, Method test,
                                               String flag, Set<String> flags) {
        List<String> problems = new ArrayList<>();
        Object condition;
        try {
            condition = constructor.newInstance(flag);
        } catch (ReflectiveOperationException | RuntimeException e) {
            problems.add(className + " could not be built for the flag \"" + flag + "\" (" + e + ")");
            return problems;
        }

        for (String off : flags) {
            Boolean answer = answerWithOneFlagOff(problems, flags, off, test, condition);
            if (answer == null) {
                problems.add(className + " gave no boolean answer for the flag \"" + flag
                        + "\" while worldGen." + off + " was switched off");
            } else if (off.equals(flag) && answer) {
                problems.add(className + " lets a trade gated on \"" + flag + "\" load while "
                        + "worldGen." + flag + " is switched off - no case of its switch resolves "
                        + "that string any more, so the player's toggle does nothing and only a "
                        + "log line says so");
            } else if (!off.equals(flag) && !answer) {
                problems.add(className + " refuses a trade gated on \"" + flag + "\" although only "
                        + "worldGen." + off + " is switched off - the two flags no longer read "
                        + "separate options, so one toggle also turns off the other's trades");
            }
        }
        return problems;
    }

    /**
     * The condition's answer while exactly one of the trade flags is off and every other one is on.
     *
     * <p>All of them are written rather than just the one being switched off, so the answer says
     * something about the switch instead of about whatever the config file happened to hold when
     * the suite started. They are put back in a {@code finally}, and the caller collects messages
     * instead of throwing, precisely so that no failure can leave a trade switch flipped for the
     * rest of the batch.
     */
    private static Boolean answerWithOneFlagOff(List<String> problems, Set<String> flags, String off,
                                                Method test, Object condition) {
        SimplebuildingConfig.WorldGen worldGen = Simplebuilding.getConfig().worldGen;
        Map<Field, Boolean> restore = new LinkedHashMap<>();
        try {
            for (String flag : flags) {
                Field field;
                try {
                    field = SimplebuildingConfig.WorldGen.class.getField(flag);
                    restore.put(field, field.getBoolean(worldGen));
                } catch (ReflectiveOperationException | RuntimeException e) {
                    return null; // worldGenFlagProblems already reports a flag without such a field
                }
                try {
                    field.setBoolean(worldGen, !flag.equals(off));
                } catch (ReflectiveOperationException | RuntimeException e) {
                    problems.add("worldGen." + flag + " could not be written (" + e + ")");
                    return null;
                }
            }
            return callTest(test, condition);
        } finally {
            for (Map.Entry<Field, Boolean> entry : restore.entrySet()) {
                try {
                    entry.getKey().setBoolean(worldGen, entry.getValue());
                } catch (ReflectiveOperationException | RuntimeException e) {
                    problems.add("a trade switch could not be restored (" + e + ")");
                }
            }
        }
    }

    /**
     * The condition's own {@code test} method. Declared methods only, so the loader interface's
     * default overloads cannot be picked up by mistake.
     */
    private static Method singleArgumentTest(Class<?> conditionClass) throws NoSuchMethodException {
        for (Method method : conditionClass.getDeclaredMethods()) {
            if ("test".equals(method.getName()) && method.getParameterCount() == 1
                    && !method.isSynthetic() && !Modifier.isStatic(method.getModifiers())
                    && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
                return method;
            }
        }
        throw new NoSuchMethodException("no one argument test(...) declared on " + conditionClass.getName());
    }

    /**
     * Calls the condition with a {@code null} argument. Both conditions answer from the config
     * alone and never touch the lookup they are handed, which is what makes this callable without
     * a datapack load in flight; {@code null} instead of a stand in is deliberate, so a condition
     * that started reading it fails loudly here rather than silently answering from nothing.
     */
    private static Boolean callTest(Method test, Object condition) {
        try {
            Object answer = test.invoke(condition, new Object[] {null});
            return answer instanceof Boolean value ? value : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /**
     * The base name of the file the config is saved under, taken from Cloth Config's
     * {@code @Config} annotation, or {@code null} if the annotation is gone or unreadable. Looked
     * up by name so this class needs no cloth-config import and stays loader neutral.
     */
    private static String declaredConfigFileName() {
        for (Annotation annotation : SimplebuildingConfig.class.getAnnotations()) {
            if (!CONFIG_ANNOTATION.equals(annotation.annotationType().getName())) {
                continue;
            }
            try {
                Object name = annotation.annotationType().getMethod("name").invoke(annotation);
                return name instanceof String text ? text : null;
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Describes every option declared by {@code owner} as {@code group.name type=default}, reading
     * the values off {@code instance}. Nested option groups - the classes declared inside
     * {@link SimplebuildingConfig} itself - are described by their type instead of a value, statics
     * by the fact that they are static: the serializer leaves those out of the saved file, so they
     * carry no persisted default to pin.
     */
    private static void collectOptions(Set<String> into, List<String> problems, String group,
                                       Class<?> owner, Object instance) {
        if (instance == null) {
            problems.add(group + ": the option group is null");
            return;
        }
        for (Field field : owner.getFields()) {
            if (field.getDeclaringClass() != owner || field.isSynthetic()) {
                continue;
            }
            String name = group + "." + field.getName();
            String type = field.getType().getSimpleName();
            if (Modifier.isStatic(field.getModifiers())) {
                into.add(name + " " + type + " runtime-only(static)");
            } else if (field.getType().getEnclosingClass() == SimplebuildingConfig.class) {
                into.add(name + " group:" + type);
            } else {
                try {
                    into.add(name + " " + type + "=" + field.get(instance));
                } catch (IllegalAccessException | IllegalArgumentException e) {
                    problems.add(name + " could not be read (" + e + ")");
                }
            }
        }
    }
}
