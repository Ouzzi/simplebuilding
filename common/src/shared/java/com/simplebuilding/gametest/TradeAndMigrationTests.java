package com.simplebuilding.gametest;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.mojang.authlib.GameProfile;
import com.simplebuilding.util.LegacySpatulaMigration;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.VillagerTradeTags;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.TradeSets;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * In-game coverage for the two mechanics that were rewritten for the 26.2 line:
 *
 * <ul>
 *   <li>villager trades are no longer registered from code but shipped as data
 *       ({@code data/simplebuilding/villager_trade/**}) and merged into the vanilla
 *       trade pools through {@code data/minecraft/tags/villager_trade/**};</li>
 *   <li>{@link LegacySpatulaMigration} rewrites the pre-rename "spatula" items into
 *       chisels, both in player inventories and for loose item entities.</li>
 * </ul>
 *
 * Everything here runs against the live server registries / a live level, so a broken
 * datapack path, a wrong tag id or a codec mismatch fails the test instead of passing
 * silently.
 *
 * <p><strong>On the world scan:</strong> {@link LegacySpatulaMigration#migrateWorlds} runs one
 * {@code AABB(-30000000, -64, -30000000, 30000000, 320, 30000000)} query per level. Its x/z reach
 * is exercised by where these tests themselves stand: {@code GameTestServer} drops the structures
 * at a random x/z inside its {@code TEST_POSITION_RANGE} of +/-14,999,992 blocks (observed runs:
 * {@code -14836127, -59, -13751102} and {@code 11052073, -59, 14884053}), so the case below
 * already sits millions of blocks from the origin and a box shrunk around the origin fails it.
 *
 * <p><strong>Known defect (mod), measured 2026-09-04:</strong> that one query per level is
 * expensive, and under load it stops finding what it should. Vanilla's
 * {@code EntitySectionStorage#forEachAccessibleNonEmptySection} walks <em>every</em> section x
 * from {@code minX} to {@code maxX}, which for this box is 3,750,002 sorted set range lookups per
 * level and per call. An earlier version of the case below put a second spatula in the Nether and
 * force loaded its chunk for that; of six runs, five ended with {@code migrateWorlds} rewriting
 * nothing at all - not even the item entities in the gametest's own level, which
 * {@code getEntitiesOfClass} had returned for a two block box in the same tick, and which a second
 * {@code migrateWorlds} call milliseconds later did rewrite. The five were the runs in which that
 * Nether chunk had to be generated ("Can't keep up! Running 2580ms or 51 ticks behind" in the
 * server log); the one run whose chunk was already on disk passed. The mod is not changed from
 * here, so the case below stays inside the level the gametest runs in and the dimension loop stays
 * uncovered.
 */
public final class TradeAndMigrationTests {

    /** Tick budget for {@link #masonVillagerCanRollAModTrade}. */
    public static final int MASON_VILLAGER_MAX_TICKS = 200;

    /** Tick budget for {@link #legacySpatulaItemEntityIsRewrittenInPlace}. */
    public static final int LEGACY_ITEM_ENTITY_MAX_TICKS = 60;

    /** Hotbar slot the login migration case puts its legacy spatula in. */
    private static final int LOGIN_SLOT = 8;

    private static final String NAMESPACE = "simplebuilding";

    /**
     * One shipped trade file, spelled as the numbers a player reads in the trade window. Every
     * field comes straight out of {@code data/simplebuilding/villager_trade/<path>.json}:
     * {@code wants} (item + count), {@code additional_wants} (the second cost slot, {@code null}
     * when the file has none), {@code gives} (item + count), {@code max_uses}, {@code xp} and
     * {@code reputation_discount}.
     *
     * <p>{@code reputation_discount} arrives as {@link MerchantOffer#getPriceMultiplier()}: vanilla's
     * {@code VillagerTrade#getOffer} feeds the field into that constructor argument. It is the
     * factor a villager's gossip and Hero of the Village discount the price by, so a trade that
     * quietly loses it stays a working trade and just stops rewarding reputation.
     */
    private record TradeRow(String path, Item wants, int wantCount,
                            Item alsoWants, int alsoWantCount,
                            Item gives, int giveCount,
                            int maxUses, int xp, float reputationDiscount) {

        /** A trade with a single cost slot - eighteen of the twenty. */
        static TradeRow of(String path, Item wants, int wantCount, Item gives, int giveCount,
                           int maxUses, int xp, float reputationDiscount) {
            return new TradeRow(path, wants, wantCount, null, 0, gives, giveCount,
                    maxUses, xp, reputationDiscount);
        }

        String id() {
            return NAMESPACE + ":" + path;
        }
    }

    /**
     * All 20 trade files under {@code data/simplebuilding/villager_trade/}, with the content of
     * each one. Read off the json files, not off the wiki - {@link #tradeDefinitionsProduceTheExpectedOffers}
     * builds every row into a real {@link MerchantOffer} and compares it field by field.
     */
    private static final List<TradeRow> TRADE_TABLE = List.of(
            TradeRow.of("librarian/3/emerald_building_book", Items.EMERALD, 25, Items.ENCHANTED_BOOK, 1, 3, 15, 0.3F),
            TradeRow.of("librarian/4/emerald_advanced_book", Items.EMERALD, 25, Items.ENCHANTED_BOOK, 1, 2, 25, 0.5F),
            TradeRow.of("librarian/5/emerald_master_book", Items.EMERALD, 25, Items.ENCHANTED_BOOK, 1, 1, 100, 1.0F),
            TradeRow.of("mason/2/emerald_copper_core", Items.EMERALD, 25, ModItems.COPPER_CORE, 1, 2, 10, 0.1F),
            TradeRow.of("mason/2/netherite_diamond_core", Items.NETHERITE_INGOT, 6, ModItems.DIAMOND_CORE, 1, 2, 15, 0.1F),
            TradeRow.of("mason/4/emerald_copper_building_wand", Items.EMERALD, 62, ModItems.COPPER_BUILDING_WAND, 1, 1, 20, 0.2F),
            TradeRow.of("toolsmith/3/emerald_iron_chisel", Items.EMERALD, 6, ModItems.IRON_CHISEL, 1, 2, 10, 0.2F),
            TradeRow.of("toolsmith/3/emerald_copper_chisel", Items.EMERALD, 6, ModItems.COPPER_CHISEL, 1, 2, 10, 0.2F),
            TradeRow.of("toolsmith/3/emerald_gold_chisel", Items.EMERALD, 6, ModItems.GOLD_CHISEL, 1, 2, 10, 0.2F),
            new TradeRow("toolsmith/4/emerald_diamond_sledgehammer", Items.EMERALD, 28,
                    Items.DIAMOND_PICKAXE, 1, ModItems.DIAMOND_SLEDGEHAMMER, 1, 1, 30, 0.5F),
            new TradeRow("toolsmith/4/emerald_iron_sledgehammer", Items.EMERALD, 16,
                    Items.IRON_PICKAXE, 1, ModItems.IRON_SLEDGEHAMMER, 1, 1, 30, 0.5F),
            TradeRow.of("toolsmith/5/emerald_mining_pickaxe", Items.EMERALD, 15, Items.DIAMOND_PICKAXE, 1, 1, 50, 0.8F),
            TradeRow.of("wandering_trader/emerald_copper_cores", Items.EMERALD, 46, ModItems.COPPER_CORE, 2, 4, 10, 0.1F),
            TradeRow.of("wandering_trader/emerald_iron_cores", Items.EMERALD, 56, ModItems.IRON_CORE, 2, 4, 10, 0.1F),
            TradeRow.of("wandering_trader/emerald_gold_core", Items.EMERALD, 30, ModItems.GOLD_CORE, 1, 1, 5, 0.1F),
            TradeRow.of("wandering_trader/emerald_octant", Items.EMERALD, 10, ModItems.OCTANT, 1, 1, 15, 0.1F),
            TradeRow.of("wandering_trader/emerald_reinforced_bundle", Items.EMERALD, 16, ModItems.REINFORCED_BUNDLE, 1, 1, 15, 0.1F),
            TradeRow.of("wandering_trader/emerald_wand_book", Items.EMERALD, 60, Items.ENCHANTED_BOOK, 1, 1, 10, 0.2F),
            TradeRow.of("wandering_trader/octant_emerald", ModItems.OCTANT, 1, Items.EMERALD, 8, 3, 5, 0.1F),
            TradeRow.of("wandering_trader/reinforced_bundle_emerald", ModItems.REINFORCED_BUNDLE, 1, Items.EMERALD, 12, 1, 10, 0.1F));

    /** The ids of all 20 trade files, derived from {@link #TRADE_TABLE} so both cannot drift apart. */
    private static final List<String> EXPECTED_TRADE_IDS = TRADE_TABLE.stream().map(TradeRow::id).toList();

    /**
     * The {@code weighted_enchant} pool of the three toolsmith chisel trades, as
     * {@code <enchantment id>@<level>} - see
     * {@code data/simplebuilding/villager_trade/toolsmith/3/emerald_*_chisel.json}.
     */
    private static final Set<String> CHISEL_ENCHANT_POOL = Set.of(
            "simplebuilding:fast_chiseling@1",
            "simplebuilding:fast_chiseling@2");

    /** The same for both {@code toolsmith/4} sledgehammers, which also have a second chance. */
    private static final Set<String> SLEDGEHAMMER_ENCHANT_POOL = Set.of(
            "simplebuilding:break_through@1",
            "simplebuilding:override@1",
            "simplebuilding:range@1",
            "minecraft:unbreaking@2",
            "minecraft:efficiency@3");

    /** {@code librarian/3/emerald_building_book}. */
    private static final Set<String> BUILDING_BOOK_POOL = Set.of(
            "simplebuilding:color_palette@1",
            "simplebuilding:fast_chiseling@1",
            "simplebuilding:linear@1");

    /** {@code librarian/4/emerald_advanced_book}. */
    private static final Set<String> ADVANCED_BOOK_POOL = Set.of(
            "simplebuilding:linear@1",
            "simplebuilding:override@1");

    /** {@code librarian/5/emerald_master_book} - the only place Master Builder is sold. */
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

    /** {@code toolsmith/5/emerald_mining_pickaxe}. */
    private static final Set<String> MINING_PICKAXE_POOL = Set.of(
            "simplebuilding:strip_miner@1",
            "simplebuilding:strip_miner@2",
            "simplebuilding:strip_miner@3",
            "simplebuilding:vein_miner@1",
            "simplebuilding:vein_miner@2",
            "simplebuilding:vein_miner@3");

    /** {@code wandering_trader/emerald_wand_book} - the only place Radius is sold. */
    private static final Set<String> WAND_BOOK_POOL = Set.of("simplebuilding:radius@1");

    /**
     * Every trade whose json carries a {@code simplebuilding:weighted_enchant} function, with the
     * enchantment/level pairs that file offers. Ten of the twenty trades hand out an enchanted
     * result; the other ten must not be in here.
     */
    private static final Map<String, Set<String>> ENCHANT_POOLS = Map.ofEntries(
            Map.entry("librarian/3/emerald_building_book", BUILDING_BOOK_POOL),
            Map.entry("librarian/4/emerald_advanced_book", ADVANCED_BOOK_POOL),
            Map.entry("librarian/5/emerald_master_book", MASTER_BOOK_POOL),
            Map.entry("toolsmith/3/emerald_iron_chisel", CHISEL_ENCHANT_POOL),
            Map.entry("toolsmith/3/emerald_copper_chisel", CHISEL_ENCHANT_POOL),
            Map.entry("toolsmith/3/emerald_gold_chisel", CHISEL_ENCHANT_POOL),
            Map.entry("toolsmith/4/emerald_iron_sledgehammer", SLEDGEHAMMER_ENCHANT_POOL),
            Map.entry("toolsmith/4/emerald_diamond_sledgehammer", SLEDGEHAMMER_ENCHANT_POOL),
            Map.entry("toolsmith/5/emerald_mining_pickaxe", MINING_PICKAXE_POOL),
            Map.entry("wandering_trader/emerald_wand_book", WAND_BOOK_POOL));

    // ------------------------------------------------------------------
    // (a) trades
    // ------------------------------------------------------------------

    /**
     * Every shipped trade JSON has to end up in the {@code villager_trade} datapack
     * registry of the running server. A file that fails to parse (wrong codec field,
     * unknown item, broken loot function) is silently dropped by the loader, so this
     * catches exactly that class of regression.
     *
     * <p><strong>What breaks this test:</strong> a renamed or deleted trade file, a json the codec
     * rejects, and a stray namespace typo that adds an entry nobody expects.
     */
    public static void allModTradesAreLoadedIntoTheDatapackRegistry(GameTestHelper helper) {
        Registry<VillagerTrade> trades = helper.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);

        List<String> missing = new ArrayList<>();
        for (String id : EXPECTED_TRADE_IDS) {
            if (trades.getValue(Identifier.parse(id)) == null) {
                missing.add(id);
            }
        }
        helper.assertTrue(missing.isEmpty(), "villager_trade registry is missing " + missing.size()
                + " simplebuilding entries: " + missing);

        // Guard against a stray namespace typo adding entries nobody expects.
        long ownEntries = trades.keySet().stream().filter(key -> NAMESPACE.equals(key.getNamespace())).count();
        helper.assertValueEqual((int) ownEntries, EXPECTED_TRADE_IDS.size(),
                "number of simplebuilding entries in the villager_trade registry");

        helper.succeed();
    }

    /**
     * The tag files under {@code data/minecraft/tags/villager_trade/**} have to <em>merge</em>
     * into the vanilla pools: our entries must be in there, and the vanilla entries must
     * still be in there (a missing {@code "replace": false} would wipe them).
     *
     * <p><strong>What breaks this test:</strong> a trade id dropped from a tag file, a tag file
     * that sets {@code "replace": true}, or a tag written under the wrong id. It says nothing
     * about the <em>content</em> of a trade - that is
     * {@link #tradeDefinitionsProduceTheExpectedOffers}'s job.
     */
    public static void modTradesAreMergedIntoTheVanillaTradePools(GameTestHelper helper) {
        Registry<VillagerTrade> trades = helper.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);

        // The librarian pools are the only ones Trade Rebalance replaces wholesale.
        if (!tradeRebalanceActive(helper)) {
            assertPoolContains(helper, trades, VillagerTradeTags.LIBRARIAN_LEVEL_3,
                    "simplebuilding:librarian/3/emerald_building_book");
            assertPoolContains(helper, trades, VillagerTradeTags.LIBRARIAN_LEVEL_4,
                    "simplebuilding:librarian/4/emerald_advanced_book");
            assertPoolContains(helper, trades, VillagerTradeTags.LIBRARIAN_LEVEL_5,
                    "simplebuilding:librarian/5/emerald_master_book");
        }
        assertPoolContains(helper, trades, VillagerTradeTags.MASON_LEVEL_2,
                "simplebuilding:mason/2/emerald_copper_core",
                "simplebuilding:mason/2/netherite_diamond_core");
        assertPoolContains(helper, trades, VillagerTradeTags.MASON_LEVEL_4,
                "simplebuilding:mason/4/emerald_copper_building_wand");
        assertPoolContains(helper, trades, VillagerTradeTags.TOOLSMITH_LEVEL_3,
                "simplebuilding:toolsmith/3/emerald_iron_chisel",
                "simplebuilding:toolsmith/3/emerald_copper_chisel",
                "simplebuilding:toolsmith/3/emerald_gold_chisel");
        assertPoolContains(helper, trades, VillagerTradeTags.TOOLSMITH_LEVEL_4,
                "simplebuilding:toolsmith/4/emerald_diamond_sledgehammer",
                "simplebuilding:toolsmith/4/emerald_iron_sledgehammer");
        assertPoolContains(helper, trades, VillagerTradeTags.TOOLSMITH_LEVEL_5,
                "simplebuilding:toolsmith/5/emerald_mining_pickaxe");
        assertPoolContains(helper, trades, VillagerTradeTags.WANDERING_TRADER_BUYING,
                "simplebuilding:wandering_trader/reinforced_bundle_emerald",
                "simplebuilding:wandering_trader/octant_emerald");
        assertPoolContains(helper, trades, VillagerTradeTags.WANDERING_TRADER_COMMON,
                "simplebuilding:wandering_trader/emerald_copper_cores",
                "simplebuilding:wandering_trader/emerald_iron_cores");
        assertPoolContains(helper, trades, VillagerTradeTags.WANDERING_TRADER_UNCOMMON,
                "simplebuilding:wandering_trader/emerald_octant",
                "simplebuilding:wandering_trader/emerald_reinforced_bundle",
                "simplebuilding:wandering_trader/emerald_gold_core",
                "simplebuilding:wandering_trader/emerald_wand_book");

        helper.succeed();
    }

    /**
     * Walks the chain a real merchant walks: profession + level -> trade set -> tag ->
     * trade holders. If our tag ids do not match the trade set the profession points at,
     * the entries exist but are never offered by anybody.
     */
    public static void professionTradeSetsResolveTheModTrades(GameTestHelper helper) {
        assertTradeSetForProfession(helper, VillagerProfession.MASON, 2, TradeSets.MASON_LEVEL_2,
                "simplebuilding:mason/2/emerald_copper_core",
                "simplebuilding:mason/2/netherite_diamond_core");
        assertTradeSetForProfession(helper, VillagerProfession.TOOLSMITH, 3, TradeSets.TOOLSMITH_LEVEL_3,
                "simplebuilding:toolsmith/3/emerald_iron_chisel",
                "simplebuilding:toolsmith/3/emerald_copper_chisel",
                "simplebuilding:toolsmith/3/emerald_gold_chisel");
        if (!tradeRebalanceActive(helper)) {
            assertTradeSetForProfession(helper, VillagerProfession.LIBRARIAN, 5, TradeSets.LIBRARIAN_LEVEL_5,
                    "simplebuilding:librarian/5/emerald_master_book");
        }

        helper.succeed();
    }

    /**
     * Turns every shipped trade definition into an actual {@link MerchantOffer} and checks the
     * numbers that came out of the JSON: wanted item + count, the second cost slot (item + count,
     * or the guarantee that there is none), given item + count, max uses, xp, the reputation
     * discount - and the enchantment the offer's {@code given_item_modifiers} put on the result.
     * This is what a player would see in the trade GUI.
     *
     * <p>Every other trade test in this file only ever asks whether a trade id sits in the right
     * pool. That leaves the entire content of a trade file free to change: the price, the number
     * of uses, the experience, and above all the {@code simplebuilding:weighted_enchant} function
     * - a merchant handing out plain, unenchanted tools looks exactly like a working merchant from
     * the outside. So all twenty trades are built here, not a sample of them: a sample leaves the
     * unsampled files free to be repriced, and that is precisely how the wand, the octant and both
     * reinforced bundle trades stayed unpinned.
     *
     * <p>The enchantment is asserted as "at least one, and every one of them out of the pool the
     * json declares, at the level it declares" rather than as one fixed pick. The function draws
     * weighted from that pool; pinning a single outcome would pin {@link #tradeContext}'s seed
     * instead of the trade.
     *
     * <p><strong>What breaks this test:</strong> any edit to a number in any
     * {@code data/simplebuilding/villager_trade/**.json} - price, result count, {@code max_uses},
     * {@code xp}, {@code reputation_discount}, an {@code additional_wants} that appears, vanishes
     * or changes its count - and any change that empties or re-pools a {@code weighted_enchant}
     * modifier.
     */
    public static void tradeDefinitionsProduceTheExpectedOffers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 2, 1));
        LootContext context = tradeContext(helper, villager);

        Map<String, MerchantOffer> offers = new LinkedHashMap<>();
        for (TradeRow expected : TRADE_TABLE) {
            MerchantOffer offer = offerOf(helper, level, expected.id(), context);
            assertOffer(helper, offer, expected);
            offers.put(expected.path(), offer);
        }

        for (Map.Entry<String, Set<String>> enchanted : ENCHANT_POOLS.entrySet()) {
            MerchantOffer offer = offers.get(enchanted.getKey());
            helper.assertTrue(offer != null, "no offer was built for " + enchanted.getKey());
            assertOfferEnchantments(helper, offer, enchanted.getKey(), enchanted.getValue());
        }

        helper.succeed();
    }

    /**
     * End-to-end: a real villager with the mason profession at level 2 has to be able to
     * roll one of our trades. The trade set picks a random subset of the merged pool, so the
     * offers are regenerated a bounded number of times; missing every single time means the
     * mod trades are not part of the pool the merchant draws from.
     */
    public static void masonVillagerCanRollAModTrade(GameTestHelper helper) {
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 2, 1));

        Set<Item> wanted = Set.of(ModItems.COPPER_CORE, ModItems.DIAMOND_CORE);
        Set<Item> seen = new LinkedHashSet<>();
        boolean rolledModTrade = false;
        int emptyRolls = 0;

        for (int attempt = 0; attempt < 64 && !rolledModTrade; attempt++) {
            // Switching the profession clears the cached offers; the next getOffers() re-rolls them.
            setProfession(helper, villager, VillagerProfession.NITWIT, 1);
            setProfession(helper, villager, VillagerProfession.MASON, 2);

            List<MerchantOffer> offers = villager.getOffers();
            if (offers.isEmpty()) {
                emptyRolls++;
                continue;
            }
            for (MerchantOffer offer : offers) {
                Item result = offer.getResult().getItem();
                seen.add(result);
                if (wanted.contains(result)) {
                    rolledModTrade = true;
                }
            }
        }

        helper.assertValueEqual(emptyRolls, 0, "mason level 2 produced empty offer lists");
        helper.assertTrue(rolledModTrade,
                "a mason villager (level 2) never offered copper_core or diamond_core in 64 rolls; "
                        + "results seen were " + seen);

        helper.succeed();
    }

    // ------------------------------------------------------------------
    // (b) legacy spatula -> chisel migration
    // ------------------------------------------------------------------

    /**
     * Player-side migration: stacks in the inventory and in the open container menu are
     * rewritten to the matching chisel, keeping the stack size and the component patch;
     * items that are not legacy spatulas must be left completely alone.
     *
     * <p>"The component patch" is asserted with four components on one stack, not with the two
     * that are easy to reach: {@code LegacySpatulaMigration#convertStack} copies the whole patch
     * in one call, so a rewrite that copies a hand-picked list of components instead would keep
     * damage and name and silently drop the enchantments an old spatula was carrying - and an
     * enchanted spatula is exactly the kind that survives in an old world.
     *
     * <p>The mock player has to be a <em>connected</em> one: writing into the crafting grid makes
     * vanilla run {@code CraftingMenu#slotChangedCraftingGrid}, which unconditionally dereferences
     * {@code ServerPlayer#connection}. That matches production, where the migration only ever runs
     * from the join event and therefore always sees a player with a network handler.
     *
     * <p>The last step puts the player through a second login instead of calling the migration by
     * hand, because that is the part nothing else covers: both loaders register the migration on
     * their "player joined" event ({@code ServerPlayConnectionEvents.JOIN} on Fabric, a
     * {@code PlayerEvent.PlayerLoggedInEvent} handler on NeoForge), both fire it from
     * {@code PlayerList#placeNewPlayer}, and deleting either registration would leave every
     * assertion above green while no old spatula in any world is ever converted again.
     *
     * <p><strong>Not covered:</strong> the second half of that wiring, the server start hook that
     * runs {@link LegacySpatulaMigration#migrateWorlds}. It fires once, long before any gametest
     * body runs, and a shared gametest cannot re-enter a server start on either loader.
     *
     * <p><strong>What breaks this test:</strong> dropping the inventory loop, the menu loop, the
     * component copy or a single entry of the spatula/chisel mapping; converting an item that is
     * not a legacy spatula; and removing either loader's join listener.
     */
    public static void legacySpatulasInPlayerInventoryBecomeChisels(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));

        Holder<Enchantment> fastChiseling = enchantment(helper, ModEnchantments.FAST_CHISELING);

        // Spatulas and chisels are damageable tools, so a damaged one is always a single item;
        // the damage component itself refuses to apply to anything bigger than one.
        ItemStack stoneSpatula = new ItemStack(ModItems.STONE_SPATULA, 1);
        stoneSpatula.set(DataComponents.DAMAGE, 5);
        stoneSpatula.set(DataComponents.CUSTOM_NAME, Component.literal("Grandpa's tool"));
        // Two more components, both of them things a player would notice losing: the enchantment
        // an old spatula was carrying and the anvil cost it has accumulated.
        stoneSpatula.set(DataComponents.REPAIR_COST, 7);
        stoneSpatula.enchant(fastChiseling, 2);
        player.getInventory().setItem(0, stoneSpatula);

        // The stack size is covered separately, on a stack without components to validate.
        ItemStack copperSpatulas = new ItemStack(ModItems.COPPER_SPATULA, 3);
        player.getInventory().setItem(1, copperSpatulas);

        ItemStack netheriteSpatula = new ItemStack(ModItems.NETHERITE_SPATULA, 1);
        player.getInventory().setItem(3, netheriteSpatula);

        // Untouched control: already-migrated item plus a vanilla item.
        ItemStack alreadyChisel = new ItemStack(ModItems.IRON_CHISEL, 1);
        player.getInventory().setItem(4, alreadyChisel);
        ItemStack vanilla = new ItemStack(Items.DIAMOND, 12);
        player.getInventory().setItem(5, vanilla);

        // A spatula that only lives in the open menu (crafting grid slot), not in the inventory.
        ItemStack menuSpatula = new ItemStack(ModItems.GOLD_SPATULA, 1);
        player.containerMenu.getSlot(InventoryMenu.CRAFT_SLOT_START).set(menuSpatula);

        LegacySpatulaMigration.migratePlayer(player);

        ItemStack migrated = player.getInventory().getItem(0);
        helper.assertTrue(migrated.is(ModItems.STONE_CHISEL),
                "stone_spatula should have become stone_chisel, was " + migrated);
        helper.assertValueEqual(migrated.getCount(), 1, "migrated stack size");
        helper.assertValueEqual(migrated.get(DataComponents.DAMAGE), 5, "migrated damage component");
        helper.assertValueEqual(migrated.get(DataComponents.CUSTOM_NAME),
                Component.literal("Grandpa's tool"), "migrated custom name component");
        helper.assertValueEqual(migrated.get(DataComponents.REPAIR_COST), 7,
                "migrated repair cost component");
        helper.assertValueEqual(EnchantmentHelper.getEnchantmentsForCrafting(migrated).getLevel(fastChiseling), 2,
                "Fast Chiseling level on the migrated chisel - an enchanted spatula must not come "
                        + "back as a plain tool");

        ItemStack migratedStack = player.getInventory().getItem(1);
        helper.assertTrue(migratedStack.is(ModItems.COPPER_CHISEL),
                "copper_spatula should have become copper_chisel, was " + migratedStack);
        helper.assertValueEqual(migratedStack.getCount(), 3, "migrated multi item stack size");

        helper.assertTrue(player.getInventory().getItem(3).is(ModItems.NETHERITE_CHISEL),
                "netherite_spatula should have become netherite_chisel, was "
                        + player.getInventory().getItem(3));

        helper.assertTrue(player.getInventory().getItem(4).is(ModItems.IRON_CHISEL),
                "an existing iron_chisel must survive the migration untouched");
        helper.assertTrue(player.getInventory().getItem(5).is(Items.DIAMOND),
                "a vanilla stack must survive the migration untouched");
        helper.assertValueEqual(player.getInventory().getItem(5).getCount(), 12,
                "vanilla stack size after migration");

        ItemStack migratedMenuStack = player.containerMenu.getSlot(InventoryMenu.CRAFT_SLOT_START).getItem();
        helper.assertTrue(migratedMenuStack.is(ModItems.GOLD_CHISEL),
                "gold_spatula in the open menu should have become gold_chisel, was " + migratedMenuStack);
        helper.assertValueEqual(migratedMenuStack.getCount(), 1, "migrated menu stack size");

        // --- and now the hook that is supposed to call all of that in a real game ---
        ServerPlayer joining = logIn(helper, new ItemStack(ModItems.STONE_SPATULA, 2));

        ItemStack afterLogin = joining.getInventory().getItem(LOGIN_SLOT);
        helper.assertTrue(afterLogin.is(ModItems.STONE_CHISEL),
                "logging in left the spatula in the inventory alone (it is " + afterLogin + "); the "
                        + "mod's join listener is gone, so the migration never runs outside this test");
        helper.assertValueEqual(afterLogin.getCount(), 2, "stack size after the login migration");

        helper.succeed();
    }

    /**
     * World-side migration: a loose spatula lying on the ground is rewritten in place, so the
     * same {@link ItemEntity} now carries the chisel with the original count and components.
     *
     * <p>Both entities are asserted to be visible to {@code getEntitiesOfClass} in the same tick
     * the migration runs, because that is the lookup the migration itself uses: without that
     * precondition a red result here could be the harness rather than the mod (see the known
     * defect in the class javadoc).
     *
     * <p><strong>Not covered:</strong> that the scan visits <em>every</em> dimension
     * ({@code server.getAllLevels()}, LegacySpatulaMigration.java:23). A spatula in the Nether
     * needs a chunk force loaded there, and the chunk generation that costs left the very same
     * {@code migrateWorlds} call finding nothing at all - see the known defect above. Restricting
     * the mod to {@code server.overworld()} therefore still passes this test.
     *
     * <p><strong>What breaks this test:</strong> dropping the {@code setItem} write-back, the
     * component copy (damage, name, enchantment and repair cost are all asserted), or a single
     * entry of the spatula/chisel mapping; converting item entities that carry something else.
     */
    public static void legacySpatulaItemEntityIsRewrittenInPlace(GameTestHelper helper) {
        // Two entities on purpose: the damage component only validates on a single item, so
        // "components survive" and "stack size survives" cannot be checked on the same stack.
        ItemEntity damaged = helper.spawnItem(ModItems.DIAMOND_SPATULA, new BlockPos(1, 2, 1));
        ItemStack legacyDamaged = new ItemStack(ModItems.DIAMOND_SPATULA, 1);
        legacyDamaged.set(DataComponents.DAMAGE, 42);
        legacyDamaged.set(DataComponents.CUSTOM_NAME, Component.literal("Old flattener"));
        legacyDamaged.set(DataComponents.REPAIR_COST, 3);
        Holder<Enchantment> fastChiseling = enchantment(helper, ModEnchantments.FAST_CHISELING);
        legacyDamaged.enchant(fastChiseling, 1);
        damaged.setItem(legacyDamaged);

        ItemEntity multiple = helper.spawnItem(ModItems.IRON_SPATULA, new BlockPos(5, 2, 1));
        multiple.setItem(new ItemStack(ModItems.IRON_SPATULA, 3));

        ItemEntity control = helper.spawnItem(Items.STICK, new BlockPos(3, 2, 3));

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(damaged.isAlive(), "the dropped spatula entity vanished before migration");
                    helper.assertTrue(multiple.isAlive(), "the dropped spatula stack vanished before migration");
                    // Findable the way the migration finds them, or a red test below would be
                    // about the harness and not about the migration.
                    assertVisibleToTheLevelLookup(helper, damaged);
                    assertVisibleToTheLevelLookup(helper, multiple);
                    LegacySpatulaMigration.migrateWorlds(helper.getLevel().getServer());
                })
                .thenExecute(() -> {
                    // Checked before the item: a rewritten stack on an entity the level has thrown
                    // away would say nothing, and a dropped entity has to name itself as the cause.
                    helper.assertTrue(damaged.isAlive(),
                            "the level dropped the spatula item entity during the migration, so what "
                                    + "it carries now proves nothing");

                    ItemStack after = damaged.getItem();
                    helper.assertTrue(after.is(ModItems.DIAMOND_CHISEL),
                            "the item entity should now carry a diamond_chisel, was " + after);
                    helper.assertValueEqual(after.getCount(), 1, "item entity stack size after migration");
                    helper.assertValueEqual(after.get(DataComponents.DAMAGE), 42,
                            "item entity damage component after migration");
                    helper.assertValueEqual(after.get(DataComponents.CUSTOM_NAME),
                            Component.literal("Old flattener"), "item entity custom name after migration");
                    helper.assertValueEqual(after.get(DataComponents.REPAIR_COST), 3,
                            "item entity repair cost after migration");
                    helper.assertValueEqual(
                            EnchantmentHelper.getEnchantmentsForCrafting(after).getLevel(fastChiseling), 1,
                            "Fast Chiseling level on the rewritten item entity - a dropped enchanted "
                                    + "spatula must not come back as a plain tool");

                    ItemStack afterStack = multiple.getItem();
                    helper.assertTrue(afterStack.is(ModItems.IRON_CHISEL),
                            "the item entity should now carry an iron_chisel, was " + afterStack);
                    helper.assertValueEqual(afterStack.getCount(), 3,
                            "item entity stack size of the multi item stack after migration");

                    helper.assertTrue(control.getItem().is(Items.STICK),
                            "an unrelated item entity must not be rewritten");
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /**
     * The migration collects its entities with {@code getEntitiesOfClass}. If that lookup cannot
     * see an entity, a case built on it would pass or fail for a reason that is not the mod's - so
     * this is asserted right before the migration runs, in the same tick.
     */
    private static void assertVisibleToTheLevelLookup(GameTestHelper helper, ItemEntity entity) {
        ServerLevel level = (ServerLevel) entity.level();
        List<ItemEntity> visible = level.getEntitiesOfClass(ItemEntity.class,
                entity.getBoundingBox().inflate(2.0), candidate -> candidate == entity);
        helper.assertTrue(!visible.isEmpty(),
                "the item entity in " + level.dimension().identifier() + " is not visible to that "
                        + "level's own entity lookup, so this case would prove nothing");
    }

    /**
     * Builds a player that is already carrying {@code carried} and logs it into the running
     * server. Both loaders raise their "player joined" event from
     * {@code PlayerList#placeNewPlayer}, so this is the loader neutral way to reach the listener
     * the migration is registered on.
     *
     * <p>The ingredients are vanilla's own, copied from
     * {@code GameTestHelper#makeMockServerPlayerInLevel}: a {@link ServerPlayer} on this level, a
     * fresh {@link Connection} on an {@link EmbeddedChannel} and an initial listener cookie. The
     * player is built by hand rather than through that helper only because the item has to be in
     * the inventory <em>before</em> the login, and the helper constructs and logs in in one step.
     * {@code placeNewPlayer} reads no saved player data, so what is set here is exactly what the
     * join event sees.
     *
     * <p>Handed back to the player list at the end of the test like every other mock player in
     * this suite - one that stays behind keeps the list non-empty and stalls the gametest server
     * on shutdown.
     */
    @SuppressWarnings("resource")
    private static ServerPlayer logIn(GameTestHelper helper, ItemStack carried) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "test-migration-player"),
                ClientInformation.createDefault());
        player.getInventory().setItem(LOGIN_SLOT, carried);
        helper.assertTrue(player.getInventory().getItem(LOGIN_SLOT).is(carried.getItem()),
                "the item was not in the inventory before the login, so the login proves nothing");

        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false));
        helper.runBeforeTestEnd(() -> server.getPlayerList().remove(player));

        helper.assertTrue(server.getPlayerList().getPlayers().contains(player),
                "the player never made it into the player list, so no join event was fired");
        return player;
    }

    /**
     * Vanilla's optional "Trade Rebalance" datapack declares {@code "replace": true} on the three
     * librarian trade tags, which discards every earlier contributor -- including this mod. It is
     * off in normal worlds but ON in the gametest environment, which enables all experimental packs.
     * Mason, toolsmith and wandering trader pools are not touched by it.
     */
    private static boolean tradeRebalanceActive(GameTestHelper helper) {
        return helper.getLevel().getServer().getResourceManager().listPacks()
                .anyMatch(pack -> "trade_rebalance".equals(pack.packId()));
    }

    private static void assertPoolContains(GameTestHelper helper, Registry<VillagerTrade> trades,
                                           TagKey<VillagerTrade> tag, String... expectedIds) {
        Set<String> actual = new LinkedHashSet<>();
        for (Holder<VillagerTrade> holder : trades.getTagOrEmpty(tag)) {
            actual.add(holder.getRegisteredName());
        }

        for (String expected : expectedIds) {
            helper.assertTrue(actual.contains(expected),
                    "trade tag " + tag.location() + " does not contain " + expected + "; it holds " + actual);
        }

        boolean hasVanilla = actual.stream().anyMatch(id -> !id.startsWith(NAMESPACE + ":"));
        helper.assertTrue(hasVanilla,
                "trade tag " + tag.location() + " lost its vanilla entries - the tag file most likely "
                        + "replaces instead of merges; it holds " + actual);
    }

    private static void assertTradeSetForProfession(GameTestHelper helper,
                                                    ResourceKey<VillagerProfession> professionKey, int level,
                                                    ResourceKey<TradeSet> expectedTradeSet, String... expectedIds) {
        var registries = helper.getLevel().registryAccess();
        VillagerProfession profession = registries.lookupOrThrow(Registries.VILLAGER_PROFESSION)
                .getOrThrow(professionKey).value();

        ResourceKey<TradeSet> tradeSetKey = profession.getTrades(level);
        helper.assertValueEqual(tradeSetKey, expectedTradeSet,
                "trade set of " + professionKey.identifier() + " at level " + level);

        TradeSet tradeSet = registries.lookupOrThrow(Registries.TRADE_SET).getOrThrow(tradeSetKey).value();
        HolderSet<VillagerTrade> pool = tradeSet.getTrades();

        Set<String> actual = new LinkedHashSet<>();
        for (Holder<VillagerTrade> holder : pool) {
            actual.add(holder.getRegisteredName());
        }
        for (String expected : expectedIds) {
            helper.assertTrue(actual.contains(expected),
                    "trade set " + tradeSetKey.identifier() + " does not offer " + expected + "; it holds " + actual);
        }
    }

    private static LootContext tradeContext(GameTestHelper helper, Villager villager) {
        LootParams params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, villager.position())
                .withParameter(LootContextParams.THIS_ENTITY, villager)
                .withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
                .create(LootContextParamSets.VILLAGER_TRADE);
        // Fixed seed so trades with a weighted enchantment pool stay reproducible.
        return new LootContext.Builder(params).withOptionalRandomSeed(20260828L).create(Optional.empty());
    }

    private static MerchantOffer offerOf(GameTestHelper helper, ServerLevel level, String id, LootContext context) {
        VillagerTrade trade = level.registryAccess()
                .lookupOrThrow(Registries.VILLAGER_TRADE)
                .getValue(Identifier.parse(id));
        helper.assertTrue(trade != null, "trade " + id + " is not registered");

        MerchantOffer offer = trade.getOffer(context);
        helper.assertTrue(offer != null, "trade " + id + " produced no offer");
        return offer;
    }

    /**
     * The enchantments {@code simplebuilding:weighted_enchant} put on a trade's result, as
     * {@code <enchantment id>@<level>}. At least one is required - an empty result is exactly what
     * an emptied {@code WeightedEnchantFunction#run} produces - and each one has to be in the pool
     * the trade json declares.
     */
    private static void assertOfferEnchantments(GameTestHelper helper, MerchantOffer offer, String id,
                                                Set<String> pool) {
        ItemStack result = offer.getResult();
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(result);
        helper.assertTrue(!enchantments.isEmpty(),
                id + ": the merchant hands out an unenchanted " + result.getItem()
                        + "; its weighted_enchant modifier did nothing");

        for (Holder<Enchantment> enchantment : enchantments.keySet()) {
            String drawn = enchantment.getRegisteredName() + "@" + enchantments.getLevel(enchantment);
            helper.assertTrue(pool.contains(drawn),
                    id + ": the merchant put " + drawn + " on the tool, which is not one of the "
                            + "enchantment/level pairs its json offers (" + pool + ")");
        }
    }

    /** Every number of one trade, against the row that was read off its json file. */
    private static void assertOffer(GameTestHelper helper, MerchantOffer offer, TradeRow expected) {
        String id = expected.path();

        ItemStack costA = offer.getBaseCostA();
        helper.assertTrue(costA.is(expected.wants()),
                id + ": expected cost item " + expected.wants() + ", was " + costA);
        helper.assertValueEqual(costA.getCount(), expected.wantCount(), id + ": cost count");

        ItemStack costB = offer.getCostB();
        if (expected.alsoWants() == null) {
            helper.assertTrue(costB.isEmpty(),
                    id + ": must not have a second cost item, but the offer asks for " + costB);
        } else {
            helper.assertTrue(costB.is(expected.alsoWants()),
                    id + ": expected second cost item " + expected.alsoWants() + ", was " + costB);
            helper.assertValueEqual(costB.getCount(), expected.alsoWantCount(), id + ": second cost count");
        }

        ItemStack result = offer.getResult();
        helper.assertTrue(result.is(expected.gives()),
                id + ": expected result item " + expected.gives() + ", was " + result);
        helper.assertValueEqual(result.getCount(), expected.giveCount(), id + ": result count");

        helper.assertValueEqual(offer.getMaxUses(), expected.maxUses(), id + ": max uses");
        helper.assertValueEqual(offer.getXp(), expected.xp(), id + ": trade xp");

        float discount = offer.getPriceMultiplier();
        helper.assertTrue(Math.abs(discount - expected.reputationDiscount()) < 1.0e-6F,
                id + ": expected reputation_discount " + expected.reputationDiscount() + ", was " + discount);
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static void setProfession(GameTestHelper helper, Villager villager,
                                      ResourceKey<VillagerProfession> profession, int level) {
        VillagerData data = villager.getVillagerData()
                .withProfession(helper.getLevel().registryAccess(), profession)
                .withLevel(level);
        villager.setVillagerData(data);
    }
}
