package com.simplebuilding.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.loot.WeightedEnchantFunction;
import com.simplebuilding.util.LegacySpatulaMigration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * The random draw that puts an enchantment on a traded item, the loot function behind it, and the
 * corners of the legacy spatula story that nothing else looks at.
 *
 * <p>{@code TradeAndMigrationTests} already answers "does the trade exist, does it sit in the
 * right pool, and does it hand over the numbers its json declares" - its
 * {@code tradeDefinitionsProduceTheExpectedOffers} builds all twenty shipped trades into real
 * {@link MerchantOffer}s and compares price, second cost, result count, {@code max_uses},
 * {@code xp} and {@code reputation_discount} field by field. None of that is repeated here. What
 * is left over is:
 *
 * <ul>
 *   <li>the two trades whose result is enchanted, driven over a large, fixed set of seeds, so that
 *       the whole pool, the fixed levels and the {@code second_chance} rate become observable -
 *       the neighbouring test draws each trade once and only asks whether what came out is
 *       <em>somewhere</em> in the pool;</li>
 *   <li>{@code simplebuilding:weighted_enchant} called directly with hand built pools, for the
 *       branches no shipped file reaches: a {@code second_chance} of 1.0, a plain book, and a pool
 *       whose weights add up to zero;</li>
 *   <li>the boundary of {@link LegacySpatulaMigration}: the world scan stops at a container, the
 *       player scan does not - and no recipe hands out a retired spatula in the first place.</li>
 * </ul>
 *
 * <p><strong>Randomness:</strong> every draw runs on a {@link LootContext} built with an explicit
 * seed ({@code SEED_BASE + i}), so a run either always passes or always fails - there is no
 * flakiness to chase. The sample sizes and the tolerated bands below were measured against
 * {@code LegacyRandomSource} (the source {@code RandomSource.create(long)} hands out), not
 * guessed: with {@link #MASTER_BOOK_SAMPLES} draws the rarest pool entry of the master book
 * (weight 3 of 120) comes up ~26 times and the second enchantment ~101 times.
 *
 * <p><strong>Not covered:</strong>
 * <ul>
 *   <li>the numbers in the trade files - they belong to
 *       {@code TradeAndMigrationTests#tradeDefinitionsProduceTheExpectedOffers}, which reads every
 *       one of them back off a real offer and with a tighter float tolerance than this file used
 *       to; the completeness of that table is held from the other side by
 *       {@code TradeRegistryTests#allModTradesReachTheRegistry}, which compares the shipped files
 *       and the registry in both directions;</li>
 *   <li>vanilla's optional "Trade Rebalance" datapack. This environment enables the experimental
 *       packs, so it is on - the server crash reports under {@code neoforge/run/crash-reports/}
 *       list it under "Active Data Packs" - and it declares {@code "replace": true} on the three
 *       librarian trade tags, which drops every other contributor including this mod. So the mod's
 *       librarian entries are simply absent from those pools here. Asserting that absence would be
 *       asserting vanilla's own guarantee: it holds whether or not the mod ships
 *       {@code data/minecraft/tags/villager_trade/librarian/level_3|4|5.json} at all, so such an
 *       assertion could never go red for anything the mod does. The membership half - the half
 *       that can - is in
 *       {@code TradeAndMigrationTests#modTradesAreMergedIntoTheVanillaTradePools}, behind its own
 *       rebalance check;</li>
 *   <li>the trade GUI itself and everything a client sees of an offer (price strike-through,
 *       tooltips, the villager's trading sounds) - no client runs in a gametest;</li>
 *   <li>the server start hook that calls {@link LegacySpatulaMigration#migrateWorlds} - it fires
 *       long before any gametest body runs and cannot be re-entered from a test.</li>
 * </ul>
 */
public final class TradeOfferTests {

    private static final String NAMESPACE = "simplebuilding";

    /** Base for every explicit loot seed in this file; never 0, which vanilla ignores. */
    private static final long SEED_BASE = 20260904L;

    /** Draws of {@code librarian/5/emerald_master_book}; see the class javadoc for the measurement. */
    private static final int MASTER_BOOK_SAMPLES = 1000;

    /** Draws of {@code toolsmith/3/emerald_copper_chisel}; enough for the 50/30 split to show. */
    private static final int CHISEL_SAMPLES = 200;

    /** Draws per hand built pool in the {@code second_chance} case below. */
    private static final int POOL_SAMPLES = 200;

    private static final BlockPos CHEST_POS = new BlockPos(2, 2, 2);
    private static final BlockPos VILLAGER_POS = new BlockPos(1, 2, 1);

    /**
     * The {@code weighted_enchant} pool of {@code librarian/5/emerald_master_book}, as
     * {@code <enchantment id>@<level>}. Eleven entries, total weight 120.
     */
    private static final Set<String> MASTER_BOOK_POOL = Set.of(
            "simplebuilding:master_builder@1",
            "simplebuilding:range@1",
            "simplebuilding:range@2",
            "simplebuilding:range@3",
            "simplebuilding:funnel@1",
            "simplebuilding:strip_miner@1",
            "simplebuilding:strip_miner@2",
            "simplebuilding:strip_miner@3",
            "simplebuilding:vein_miner@1",
            "simplebuilding:vein_miner@2",
            "simplebuilding:vein_miner@3");

    /** The pool of all three {@code toolsmith/3} chisel trades: fast chiselling at 50/30. */
    private static final Set<String> CHISEL_POOL = Set.of(
            "simplebuilding:fast_chiseling@1",
            "simplebuilding:fast_chiseling@2");

    /**
     * {@code second_chance} of the master book is 0.1, so ~10% of the draws carry a second
     * enchantment. Measured over {@link #MASTER_BOOK_SAMPLES} seeded draws: 101, and 103 and 103
     * for two other seed bases. The band is wide enough that the deterministic seed set can never
     * drift out of it, and narrow enough that changing the json to 0.05 or 0.15 fails.
     */
    private static final int MASTER_BOOK_MIN_SECOND = 70;
    private static final int MASTER_BOOK_MAX_SECOND = 140;

    /** All six items the migration is supposed to have retired. */
    private static final List<Item> LEGACY_SPATULAS = List.of(
            ModItems.STONE_SPATULA,
            ModItems.COPPER_SPATULA,
            ModItems.IRON_SPATULA,
            ModItems.GOLD_SPATULA,
            ModItems.DIAMOND_SPATULA,
            ModItems.NETHERITE_SPATULA);

    // ==================================================================================
    // (a) the enchantment a shipped trade hands out
    // ==================================================================================

    /**
     * Drives the two trades whose result is enchanted over a large, fixed set of seeds and looks at
     * what actually landed on the item.
     *
     * <p>For {@code librarian/5/emerald_master_book}: after {@link #MASTER_BOOK_SAMPLES} draws every
     * one of the eleven {@code enchantment@level} pairs its pool declares has to have come up at
     * least once, nothing outside the pool may appear, and no draw may leave the book unenchanted.
     * That is what pins the weighted draw: a {@code pickWeighted} that always returned the first
     * entry, or one that lost the level, keeps every existing test green.
     *
     * <p>The share of draws with a <em>second</em> enchantment pins {@code second_chance}, which
     * nothing else reads. The rarest entry of that pool has weight 3 of 120; the sample size and
     * the band {@link #MASTER_BOOK_MIN_SECOND}..{@link #MASTER_BOOK_MAX_SECOND} were measured
     * against the same {@code LegacyRandomSource} the loot context uses, over three seed bases.
     *
     * <p>For {@code toolsmith/3/emerald_copper_chisel}: both declared levels of Fast Chiselling
     * have to come up (weights 50 and 30), nothing else may, and every single result carries
     * exactly one enchantment.
     *
     * <p>That last assertion is <em>not</em> a guard against a {@code second_chance} appearing in
     * the chisel files. Both entries of that pool carry the same enchantment (Fast Chiselling, at
     * level 1 and 2), so the second draw is always enchantment-equal to the first, the retry loop
     * in {@code WeightedEnchantFunction#run} runs out its ten attempts and the closing
     * "second must differ from first" check sets it back to {@code null}. A {@code second_chance}
     * written into those files today is a no-op: the result is bit-identical, because
     * {@code first} is drawn before any of that. It would only become observable if the
     * inequality condition fell as well - then the second draw would overwrite the level.
     *
     * <p>What breaks this test: ignoring the weights or the levels in
     * {@code WeightedEnchantFunction}, editing a pool entry in the two json files, or changing the
     * master book's {@code second_chance} - see {@link #MASTER_BOOK_MIN_SECOND} for the band and
     * what it was measured against.
     */
    public static void masterBookTradeDrawsEveryEnchantmentInItsPool(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, VILLAGER_POS);
        LootParams params = tradeParams(helper, villager);

        VillagerTrade masterBook = tradeOf(helper, level, NAMESPACE + ":librarian/5/emerald_master_book");
        Map<String, Integer> bookDraws = new TreeMap<>();
        int booksWithoutEnchantment = 0;
        int booksWithTwo = 0;
        for (int i = 0; i < MASTER_BOOK_SAMPLES; i++) {
            MerchantOffer offer = masterBook.getOffer(context(params, SEED_BASE + i));
            helper.assertTrue(offer != null,
                    "librarian/5/emerald_master_book produced no offer on seed " + (SEED_BASE + i));
            ItemEnchantments drawn = EnchantmentHelper.getEnchantmentsForCrafting(offer.getResult());
            if (drawn.isEmpty()) {
                booksWithoutEnchantment++;
            }
            if (drawn.size() == 2) {
                booksWithTwo++;
            }
            collect(drawn, bookDraws);
        }

        helper.assertValueEqual(booksWithoutEnchantment, 0,
                "master books the merchant handed over unenchanted (out of " + MASTER_BOOK_SAMPLES + ")");
        assertPoolCoverage(helper, "librarian/5/emerald_master_book", MASTER_BOOK_POOL, bookDraws);
        helper.assertTrue(booksWithTwo >= MASTER_BOOK_MIN_SECOND && booksWithTwo <= MASTER_BOOK_MAX_SECOND,
                "librarian/5/emerald_master_book declares second_chance 0.1, so between "
                        + MASTER_BOOK_MIN_SECOND + " and " + MASTER_BOOK_MAX_SECOND + " of "
                        + MASTER_BOOK_SAMPLES + " seeded draws should carry a second enchantment - "
                        + booksWithTwo + " did");

        VillagerTrade chisel = tradeOf(helper, level, NAMESPACE + ":toolsmith/3/emerald_copper_chisel");
        Map<String, Integer> chiselDraws = new TreeMap<>();
        for (int i = 0; i < CHISEL_SAMPLES; i++) {
            MerchantOffer offer = chisel.getOffer(context(params, SEED_BASE + i));
            helper.assertTrue(offer != null,
                    "toolsmith/3/emerald_copper_chisel produced no offer on seed " + (SEED_BASE + i));
            ItemEnchantments drawn = EnchantmentHelper.getEnchantmentsForCrafting(offer.getResult());
            helper.assertValueEqual(drawn.size(), 1,
                    "toolsmith/3/emerald_copper_chisel declares no second_chance, so every chisel "
                            + "carries exactly one enchantment; seed " + (SEED_BASE + i)
                            + " produced " + drawn);
            collect(drawn, chiselDraws);
        }
        assertPoolCoverage(helper, "toolsmith/3/emerald_copper_chisel", CHISEL_POOL, chiselDraws);

        helper.succeed();
    }

    // ==================================================================================
    // (b) the weighted_enchant loot function itself
    // ==================================================================================

    /**
     * Four hand built pools drive the parts of {@code WeightedEnchantFunction#run} that no shipped
     * trade file reaches. All four are decoded through {@code LootItemFunctions.TYPED_CODEC}, so
     * they take the same road as a datapack: an unregistered function id fails here too.
     *
     * <ul>
     *   <li><em>Two entries, {@code second_chance} 0.0</em> (the default): every single one of
     *       {@link #POOL_SAMPLES} draws must carry exactly one enchantment. This is the branch that
     *       breaks loudest - an inverted comparison or a default of 1.0 turns it red immediately.</li>
     *   <li><em>Two entries, {@code second_chance} 1.0</em>: nearly every draw carries both entries
     *       at their declared levels. Not <em>every</em> draw on purpose: when the retry keeps
     *       drawing the enchantment that is already on the item, the function is supposed to give
     *       up and stay at one, so the assertion allows a handful of those.</li>
     *   <li><em>One entry, {@code second_chance} 1.0</em>: a single entry pool hands out exactly one
     *       enchantment, at the level it declares.</li>
     *   <li><em>Two entries with a hugely lopsided weight, {@code second_chance} 1.0</em>: the
     *       second draw almost always repeats the first, so this is the case the ten attempt cap
     *       exists for. Without the cap the loop never ends and the test dies in its tick limit.</li>
     * </ul>
     *
     * <p>Weights are deliberately not powers of two. {@code nextInt} takes a different road for
     * those, and for a run of consecutive seeds that road returns the same value every time, which
     * would silently reduce the sample to a single case.
     *
     * <p><strong>Two things this cannot pin</strong>, said plainly so nobody reads more into a green
     * run than is in it:
     * <ul>
     *   <li>the {@code pool.size() > 1} guard in front of the {@code second_chance} roll. Case three
     *       above states the invariant "one entry, one enchantment", not the guard: without the
     *       guard a single entry pool draws that same single entry again, the retry loop spends its
     *       ten attempts on it and the closing check sets the second pick to {@code null}, so the
     *       result is identical either way, on every seed. The guard is a saved dice roll, not a
     *       behaviour - and no shipped file even reaches it, the only single entry pool
     *       ({@code wandering_trader/emerald_wand_book}) declares no {@code second_chance};</li>
     *   <li>the closing {@code second = null} that ends the retry loop. Each enchantment appears at
     *       most once in each of the pools below, so a second pick that repeats the first is
     *       necessarily the same entry at the same level, and {@code ItemEnchantments.Mutable#set}
     *       writes the same key twice with the same value. Dropping that line leaves all four cases
     *       green. It becomes observable only over a pool that carries one enchantment at two
     *       levels - the chisel trades do, see
     *       {@link #masterBookTradeDrawsEveryEnchantmentInItsPool}.</li>
     * </ul>
     *
     * <p>What breaks this test: dropping the {@code second_chance} roll or the attempt cap.
     */
    public static void weightedEnchantHonoursItsSecondChanceSetting(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, VILLAGER_POS);
        LootParams params = tradeParams(helper, villager);

        String twoEntries = """
                { "enchantment": "minecraft:sharpness", "level": 1, "weight": 5 },
                { "enchantment": "minecraft:unbreaking", "level": 2, "weight": 5 }""";
        Set<String> twoEntryPool = Set.of("minecraft:sharpness@1", "minecraft:unbreaking@2");

        // --- second_chance left out: exactly one enchantment, always ---
        LootItemFunction never = weightedEnchant(helper, twoEntries, null);
        Map<String, Integer> neverDraws = new TreeMap<>();
        for (int i = 0; i < POOL_SAMPLES; i++) {
            ItemEnchantments drawn = enchantmentsAfter(helper, never, params, SEED_BASE + i,
                    new ItemStack(Items.DIAMOND_PICKAXE));
            helper.assertValueEqual(drawn.size(), 1,
                    "a pool without second_chance must put exactly one enchantment on the item; "
                            + "seed " + (SEED_BASE + i) + " produced " + drawn);
            collect(drawn, neverDraws);
        }
        assertPoolCoverage(helper, "hand built pool without second_chance", twoEntryPool, neverDraws);

        // --- second_chance 1.0: both entries, except for the give-up case ---
        LootItemFunction always = weightedEnchant(helper, twoEntries, 1.0F);
        int withBoth = 0;
        for (int i = 0; i < POOL_SAMPLES; i++) {
            ItemEnchantments drawn = enchantmentsAfter(helper, always, params, SEED_BASE + i,
                    new ItemStack(Items.DIAMOND_PICKAXE));
            helper.assertTrue(drawn.size() == 1 || drawn.size() == 2,
                    "a two entry pool can only ever produce one or two enchantments, seed "
                            + (SEED_BASE + i) + " produced " + drawn);
            for (Holder<Enchantment> enchantment : drawn.keySet()) {
                String pair = enchantment.getRegisteredName() + "@" + drawn.getLevel(enchantment);
                helper.assertTrue(twoEntryPool.contains(pair),
                        "second_chance drew " + pair + ", which is not in the pool " + twoEntryPool);
            }
            if (drawn.size() == 2) {
                withBoth++;
            }
        }
        helper.assertTrue(withBoth >= POOL_SAMPLES - 10,
                "second_chance 1.0 has to draw a second, different enchantment on virtually every "
                        + "roll, but only " + withBoth + " of " + POOL_SAMPLES + " results carried two");

        // --- one entry, one enchantment; this does NOT pin the pool.size() > 1 guard, see javadoc ---
        LootItemFunction single = weightedEnchant(helper,
                "{ \"enchantment\": \"minecraft:unbreaking\", \"level\": 3, \"weight\": 5 }", 1.0F);
        for (int i = 0; i < POOL_SAMPLES; i++) {
            ItemEnchantments drawn = enchantmentsAfter(helper, single, params, SEED_BASE + i,
                    new ItemStack(Items.DIAMOND_PICKAXE));
            helper.assertValueEqual(drawn.size(), 1,
                    "a single entry pool can never hand out two enchantments; seed "
                            + (SEED_BASE + i) + " produced " + drawn);
            helper.assertValueEqual(drawn.getLevel(unbreaking(helper)), 3,
                    "the single pool entry declares level 3, the function put on " + drawn);
        }

        // --- the ten attempt cap: the second draw practically always repeats the first ---
        LootItemFunction lopsided = weightedEnchant(helper, """
                { "enchantment": "minecraft:sharpness", "level": 1, "weight": 1000000 },
                { "enchantment": "minecraft:unbreaking", "level": 2, "weight": 3 }""", 1.0F);
        int gaveUp = 0;
        for (int i = 0; i < POOL_SAMPLES; i++) {
            ItemEnchantments drawn = enchantmentsAfter(helper, lopsided, params, SEED_BASE + i,
                    new ItemStack(Items.DIAMOND_PICKAXE));
            if (drawn.size() == 1) {
                gaveUp++;
            }
        }
        helper.assertTrue(gaveUp >= POOL_SAMPLES - 5,
                "with a pool weighted 1000000:3 the retry can hardly ever find a different second "
                        + "enchantment, so it has to give up and stay at one - but only " + gaveUp
                        + " of " + POOL_SAMPLES + " results had a single enchantment");

        helper.succeed();
    }

    /**
     * A plain {@code minecraft:book} handed to the function comes back as a
     * {@code minecraft:enchanted_book} carrying {@code stored_enchantments}. No shipped trade file
     * reaches this branch - all of them already hand over an enchanted book - so without this test
     * the transmute could be deleted and nothing would notice until somebody wrote a trade that
     * needs it.
     *
     * <p>The second half is the contrast that gives the first one meaning: the same function on a
     * pickaxe must leave the item alone and write into {@code enchantments} instead. Both component
     * choices are vanilla's, made by {@code EnchantmentHelper#updateEnchantments} off the item type,
     * which is exactly why the transmute has to happen before the enchantment is set.
     *
     * <p>What breaks this test: removing the {@code Items.BOOK} branch from
     * {@code WeightedEnchantFunction#run}, or moving it after the enchantment is applied - a book
     * enchanted first would take {@code enchantments} and lose it all on the transmute.
     */
    public static void weightedEnchantTurnsPlainBooksIntoEnchantedBooks(GameTestHelper helper) {
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, VILLAGER_POS);
        LootParams params = tradeParams(helper, villager);
        LootItemFunction function = weightedEnchant(helper,
                "{ \"enchantment\": \"minecraft:unbreaking\", \"level\": 3, \"weight\": 5 }", null);

        ItemStack book = new ItemStack(Items.BOOK);
        ItemStack enchanted = function.apply(book, context(params, SEED_BASE));

        helper.assertTrue(enchanted.is(Items.ENCHANTED_BOOK),
                "a plain book should have been turned into an enchanted_book, it is " + enchanted);
        helper.assertTrue(book.is(Items.BOOK) && !EnchantmentHelper.hasAnyEnchantments(book),
                "the book that was handed in must not be modified in place, it is now " + book);

        ItemEnchantments stored = enchanted.get(DataComponents.STORED_ENCHANTMENTS);
        helper.assertTrue(stored != null && !stored.isEmpty(),
                "the enchanted_book carries no stored_enchantments at all: " + stored);
        helper.assertValueEqual(stored.getLevel(unbreaking(helper)), 3,
                "the level stored on the book");
        ItemEnchantments onBook = enchanted.get(DataComponents.ENCHANTMENTS);
        helper.assertTrue(onBook == null || onBook.isEmpty(),
                "a book's enchantment belongs in stored_enchantments, not in enchantments: " + onBook);

        // Contrast: a normal tool is not transmuted and takes the ordinary component.
        ItemStack pickaxe = function.apply(new ItemStack(Items.DIAMOND_PICKAXE), context(params, SEED_BASE));
        helper.assertTrue(pickaxe.is(Items.DIAMOND_PICKAXE),
                "only books may be transmuted, the pickaxe came back as " + pickaxe);
        ItemEnchantments onPickaxe = pickaxe.get(DataComponents.ENCHANTMENTS);
        helper.assertTrue(onPickaxe != null && onPickaxe.getLevel(unbreaking(helper)) == 3,
                "the pickaxe should carry unbreaking 3 in its enchantments component, it has "
                        + onPickaxe);

        helper.succeed();
    }

    /**
     * A pool whose weights add up to zero leaves the stack exactly as it was - the very same
     * object, unenchanted.
     *
     * <p>This is the guard in {@code pickWeighted}, and it is not cosmetic: the draw ends in
     * {@code RandomSource#nextInt(int)}, which throws {@code IllegalArgumentException} for a bound
     * of zero. Without the guard a datapack with a zero weight pool would not hand out a plain
     * item, it would blow up in the middle of a villager building its offers.
     *
     * <p>What breaks this test: replacing the {@code totalWeight <= 0} check with a bare
     * {@code nextInt(totalWeight)} (the test then fails with that exception instead of an
     * assertion), or making the function fall back to the first pool entry.
     */
    public static void weightedEnchantIgnoresPoolsWithoutAnyWeight(GameTestHelper helper) {
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, VILLAGER_POS);
        LootParams params = tradeParams(helper, villager);
        LootItemFunction function = weightedEnchant(helper, """
                { "enchantment": "minecraft:sharpness", "level": 1, "weight": 0 },
                { "enchantment": "minecraft:unbreaking", "level": 2, "weight": 0 }""", 1.0F);

        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack result = function.apply(pickaxe, context(params, SEED_BASE));

        helper.assertTrue(result == pickaxe,
                "a pool with no weight at all has to hand the very same stack back, got " + result);
        helper.assertTrue(!EnchantmentHelper.hasAnyEnchantments(result),
                "a pool with no weight at all must not enchant anything, the stack carries "
                        + EnchantmentHelper.getEnchantmentsForCrafting(result));

        helper.succeed();
    }

    // ==================================================================================
    // (c) the legacy spatula migration
    // ==================================================================================

    /**
     * Where the two halves of the migration stop and start. {@code migrateWorlds} only ever looks at
     * loose {@code ItemEntity}s, so a spatula sitting in a chest is left alone; {@code migratePlayer}
     * walks the open container menu, so the same spatula is converted the moment a player has that
     * chest open.
     *
     * <p>The first half is the documented limitation of the migration, pinned here so that changing
     * it stays a decision instead of an accident - and so that the world scan is shown to walk past
     * containers without touching or corrupting them.
     *
     * <p>The second half is the contrast that gives the first one meaning: without it, "the chest
     * still holds a spatula" would read the same as a migration that does nothing at all. It is not
     * an additional path through the mod. {@code LegacySpatulaMigration#migratePlayer} has exactly
     * one menu loop, and {@code TradeAndMigrationTests#legacySpatulasInPlayerInventoryBecomeChisels}
     * already drives it over the crafting grid slot. What differs here is vanilla's: a
     * {@link ChestMenu} slot's {@code Slot#setByPlayer} writes back into a block entity instead of
     * into a {@code TransientCraftingContainer}. So losing the menu loop turns this test red
     * together with that one, not instead of it.
     *
     * <p>What breaks this test: extending the world scan to block entities (first half), or losing
     * the container menu loop in {@code migratePlayer} (second half, alongside the neighbouring
     * test).
     */
    public static void spatulasInContainersSurviveTheWorldScan(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.runBeforeTestEnd(() -> server.getPlayerList().remove(player));

        helper.setBlock(CHEST_POS, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(CHEST_POS, ChestBlockEntity.class);
        ItemStack legacy = new ItemStack(ModItems.IRON_SPATULA, 1);
        legacy.set(DataComponents.DAMAGE, 11);
        chest.setItem(0, legacy);

        // --- the world scan walks past containers ---
        LegacySpatulaMigration.migrateWorlds(server);
        ItemStack afterWorldScan = chest.getItem(0);
        helper.assertTrue(afterWorldScan.is(ModItems.IRON_SPATULA),
                "the world scan looks at loose item entities only, so a spatula in a chest has to "
                        + "stay a spatula; it is now " + afterWorldScan + ". If the scan was "
                        + "deliberately extended to containers, this test is the place to say so");

        // --- but the same chest, opened by a player, is reached ---
        ChestMenu menu = ChestMenu.threeRows(1, player.getInventory(), chest);
        player.containerMenu = menu;
        LegacySpatulaMigration.migratePlayer(player);
        // Closed by hand: opening the menu bumped the chest's opener count, and the block entity
        // would keep rechecking for a player that is about to be taken off the server.
        menu.removed(player);
        player.containerMenu = player.inventoryMenu;

        ItemStack afterPlayerScan = chest.getItem(0);
        helper.assertTrue(afterPlayerScan.is(ModItems.IRON_CHISEL),
                "an open chest menu is part of the player migration, so the spatula in it should "
                        + "have become an iron_chisel; the chest holds " + afterPlayerScan);
        helper.assertValueEqual(afterPlayerScan.getCount(), 1, "chest stack size after migration");
        helper.assertValueEqual(afterPlayerScan.get(DataComponents.DAMAGE), 11,
                "chest stack damage component after migration");

        helper.succeed();
    }

    /**
     * The six spatulas are retired: they stay registered so old saves can be read and migrated, but
     * nothing may hand a player a new one. This walks the whole recipe manager and asserts that no
     * recipe - the mod's own or anybody else's - produces a spatula or asks for one as an
     * ingredient.
     *
     * <p>{@code DataIntegrityTests} only asks whether the items a recipe references are registered,
     * which a re-added spatula recipe would pass with flying colours.
     *
     * <p><strong>Not covered:</strong> the result of a recipe type that exposes no
     * {@code RecipeDisplay} - {@code simplebuilding:count_based_smithing} is one - cannot be read
     * back generically. Such a recipe is still caught through its ingredients, which do go through
     * {@code placementInfo}.
     *
     * <p>What breaks this test: adding any recipe file under {@code data/**\/recipe/} whose result
     * or ingredient is one of the six spatulas.
     */
    // Ingredient#items() is marked deprecated ("display only"), but it is the only way to see the
    // item holders a recipe references - which is precisely the question here.
    @SuppressWarnings("deprecation")
    public static void noRecipeReferencesTheLegacySpatulas(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        RecipeManager recipeManager = server.getRecipeManager();
        ContextMap displayContext = SlotDisplayContext.fromLevel(helper.getLevel());
        Set<Item> spatulas = Set.copyOf(LEGACY_SPATULAS);

        List<String> offenders = new ArrayList<>();
        int recipesSeen = 0;
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            recipesSeen++;
            Identifier recipeId = holder.id().identifier();

            for (RecipeDisplay display : holder.value().display()) {
                for (ItemStack stack : display.result().resolveForStacks(displayContext)) {
                    if (!stack.isEmpty() && spatulas.contains(stack.getItem())) {
                        offenders.add(recipeId + " produces " + stack.getItem());
                    }
                }
            }

            for (Ingredient ingredient : holder.value().placementInfo().ingredients()) {
                if (ingredient.isEmpty()) {
                    continue;
                }
                for (Holder<Item> itemHolder : ingredient.items().toList()) {
                    if (itemHolder.isBound() && spatulas.contains(itemHolder.value())) {
                        offenders.add(recipeId + " uses " + itemHolder.getRegisteredName()
                                + " as an ingredient");
                    }
                }
            }
        }

        helper.assertTrue(recipesSeen > 0, "no recipes were loaded at all, so nothing was checked");
        helper.assertTrue(offenders.isEmpty(),
                "the spatulas are retired and only exist so old saves can be migrated, but "
                        + offenders.size() + " recipe(s) still reference them: " + offenders);

        helper.succeed();
    }

    // ==================================================================================
    // helpers
    // ==================================================================================

    private static VillagerTrade tradeOf(GameTestHelper helper, ServerLevel level, String id) {
        VillagerTrade trade = level.registryAccess()
                .lookupOrThrow(Registries.VILLAGER_TRADE)
                .getValue(Identifier.parse(id));
        helper.assertTrue(trade != null, "trade " + id + " is not in the villager_trade registry");
        return trade;
    }

    /** The parameter set a villager trade is built with; the same one vanilla uses. */
    private static LootParams tradeParams(GameTestHelper helper, Villager villager) {
        return new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, villager.position())
                .withParameter(LootContextParams.THIS_ENTITY, villager)
                .withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
                .create(LootContextParamSets.VILLAGER_TRADE);
    }

    /**
     * A loot context on an explicit seed. {@code withOptionalRandomSeed} ignores 0, so every caller
     * has to stay away from it - {@link #SEED_BASE} and its offsets do.
     */
    private static LootContext context(LootParams params, long seed) {
        return new LootContext.Builder(params).withOptionalRandomSeed(seed).create(Optional.empty());
    }

    /**
     * Decodes a {@code simplebuilding:weighted_enchant} function from the json a datapack would
     * carry, through vanilla's own dispatch codec. {@code poolEntries} is the body of the
     * {@code pool} array, {@code secondChance} may be {@code null} to leave the field out and take
     * the codec's default.
     */
    private static LootItemFunction weightedEnchant(GameTestHelper helper, String poolEntries,
                                                    Float secondChance) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);

        String json = "{ \"function\": \"" + NAMESPACE + ":weighted_enchant\""
                + (secondChance == null ? "" : ", \"second_chance\": " + secondChance)
                + ", \"pool\": [" + poolEntries + "] }";
        LootItemFunction function = LootItemFunctions.TYPED_CODEC
                .parse(ops, JsonParser.parseString(json))
                .getOrThrow(message -> helper.assertionException(
                        "the test's own weighted_enchant json does not decode: " + message
                                + " (json was " + json + ")"));

        helper.assertTrue(function instanceof WeightedEnchantFunction,
                "simplebuilding:weighted_enchant decoded into " + function.getClass()
                        + " instead of a WeightedEnchantFunction");
        return function;
    }

    /** Runs one function over a fresh stack on one seed and reports what ended up on the result. */
    private static ItemEnchantments enchantmentsAfter(GameTestHelper helper, LootItemFunction function,
                                                      LootParams params, long seed, ItemStack input) {
        ItemStack result = function.apply(input, context(params, seed));
        helper.assertTrue(!result.isEmpty(),
                "the weighted_enchant function emptied the stack on seed " + seed);
        return EnchantmentHelper.getEnchantmentsForCrafting(result);
    }

    /** Adds every {@code <enchantment id>@<level>} of one result to the running tally. */
    private static void collect(ItemEnchantments enchantments, Map<String, Integer> tally) {
        for (Holder<Enchantment> enchantment : enchantments.keySet()) {
            String pair = enchantment.getRegisteredName() + "@" + enchantments.getLevel(enchantment);
            tally.merge(pair, 1, Integer::sum);
        }
    }

    /**
     * Every declared {@code enchantment@level} pair has to have been drawn at least once and no
     * other one may show up. Missing entries mean the weights or the levels are not being used;
     * extra ones mean the function invented something the json never offered.
     */
    private static void assertPoolCoverage(GameTestHelper helper, String what, Set<String> pool,
                                           Map<String, Integer> drawn) {
        Map<String, Integer> counts = new LinkedHashMap<>(drawn);
        List<String> missing = pool.stream().filter(pair -> !counts.containsKey(pair)).sorted().toList();
        helper.assertTrue(missing.isEmpty(),
                what + ": these pool entries were never drawn, so their weight or their level is "
                        + "being ignored: " + missing + " (drawn: " + counts + ")");

        List<String> unexpected = counts.keySet().stream().filter(pair -> !pool.contains(pair))
                .sorted().toList();
        helper.assertTrue(unexpected.isEmpty(),
                what + ": drew " + unexpected + ", which the json's pool does not offer at all");
    }

    /** {@code minecraft:unbreaking} as a holder, for reading a level back off a result. */
    private static Holder<Enchantment> unbreaking(GameTestHelper helper) {
        return helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING);
    }

    private TradeOfferTests() {
    }
}
