package com.simplebuilding.gametest;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.trade.ModTradeDefinitions;
import com.simplebuilding.trade.ModTradeDefinitions.VillagerTradeGroup;
import com.simplebuilding.trade.ModTradeDefinitions.WanderingTradeGroup;
import com.simplebuilding.trade.ModTradeDefinitions.WanderingTraderPool;
import com.simplebuilding.trade.TradeDefinition;
import com.simplebuilding.util.LegacySpatulaMigration;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import org.apache.commons.lang3.tuple.Pair;

/**
 * In-game coverage for the two mechanics whose implementation differs the most between the two
 * supported Minecraft lines:
 *
 * <ul>
 *   <li>villager trades. On 26.2 they are data ({@code data/simplebuilding/villager_trade/**})
 *       merged through tags; MC 1.21.11 has no such system, so the very same table
 *       ({@link ModTradeDefinitions}) is turned into {@code VillagerTrades.ItemListing}s and
 *       pushed into the vanilla pools - by {@code TradeOfferHelper} on Fabric and by
 *       {@code VillagerTradesEvent} / {@code WandererTradesEvent} on NeoForge. Both loaders end up
 *       mutating the same public vanilla fields, which is what these tests read.</li>
 *   <li>{@link LegacySpatulaMigration} rewrites the pre-rename "spatula" items into chisels, both
 *       in player inventories and for loose item entities. That code is version independent, so
 *       those two tests are identical to the 26.2 ones.</li>
 * </ul>
 */
public final class TradeAndMigrationTests {

    /** Tick budget for {@link #masonVillagerCanRollAModTrade}. */
    public static final int MASON_VILLAGER_MAX_TICKS = 200;

    /** Tick budget for {@link #wanderingTraderCanRollAModTrade}. */
    public static final int WANDERING_TRADER_MAX_TICKS = 200;

    /** Tick budget for {@link #legacySpatulaItemEntityIsRewrittenInPlace}. */
    public static final int LEGACY_ITEM_ENTITY_MAX_TICKS = 60;

    /** Fixed seed so an offer built here matches the offer built from the registered listing. */
    private static final long OFFER_SEED = 20260828L;

    /**
     * The one inventory slot no {@link InventoryMenu} slot points at: body armour sits at container
     * index 41, the menu stops after the off hand at 40. Every other slot a spatula could be in is
     * reachable from both loops of {@code LegacySpatulaMigration#migratePlayer}, so only a stack
     * here says whether the inventory loop walks the whole container - see
     * {@link #legacySpatulasInPlayerInventoryBecomeChisels}.
     */
    private static final int SLOT_OUTSIDE_THE_MENU = Inventory.SLOT_BODY_ARMOR;

    /**
     * Absolute height of the probe entity in {@link #legacySpatulaItemEntityIsRewrittenInPlace} -
     * the only thing in this file standing in the upper half of the migration's scan box.
     */
    private static final int PROBE_Y = 60;

    /**
     * How often {@link #assertEveryDeclaredEnchantmentIsDrawn} re-rolls each enchanted trade. Sized
     * for the rarest entry the table declares, the master book's {@code range@3} at weight 3 of 120;
     * {@code TradeOfferTests} uses the same number for its master book statistics.
     */
    private static final int ENCHANT_ROLLS = 1000;

    /** {@code toolsmith/3} chisels - {@code chiselPool()} in {@link ModTradeDefinitions}. */
    private static final Set<String> CHISEL_ENCHANT_POOL = Set.of(
            "simplebuilding:fast_chiseling@1",
            "simplebuilding:fast_chiseling@2");

    /** Both {@code toolsmith/4} sledgehammers - {@code sledgehammerEntries()}. */
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
     * Index of each mod pool inside {@code VillagerTrades.WANDERING_TRADER_TRADES}.
     *
     * <p>Read off the vanilla list and matched by both loader adapters: index 0 is the buying pool
     * (player gives an item, gets emeralds), index 1 the small "special" sell pool and index 2 the
     * large everyday sell pool. Fabric names them {@code BUY_ITEMS_POOL} /
     * {@code SELL_SPECIAL_ITEMS_POOL} / {@code SELL_COMMON_ITEMS_POOL}, NeoForge
     * {@code buying} / {@code rare} / {@code generic}.
     */
    private static int poolIndex(WanderingTraderPool pool) {
        return switch (pool) {
            case BUY -> 0;
            case UNCOMMON -> 1;
            case COMMON -> 2;
        };
    }

    /** Everything a {@link MerchantOffer} carries that a {@link TradeDefinition} determines. */
    private record OfferKey(Item costA, int costACount, Item costB, int costBCount,
                            Item result, int resultCount, int maxUses, int xp, float priceMultiplier) {
    }

    // ------------------------------------------------------------------
    // (a) trades
    // ------------------------------------------------------------------

    /**
     * <strong>Not present on this Minecraft line.</strong> The 26.2 copy of this class carries two
     * further tests, {@code allModTradesAreLoadedIntoTheDatapackRegistry} and
     * {@code professionTradeSetsResolveTheModTrades}. Both walk machinery that arrived with MC
     * 26.1: a {@code villager_trade} datapack registry the shipped jsons are parsed into, and
     * {@code TradeSet} objects a profession and level resolve to.
     *
     * <p>This line has neither. Its offers are built in code -- see
     * {@code com.simplebuilding.trade.ModTradeDefinitions} -- so there is no file that could fail
     * to parse and no trade set that could point at the wrong tag. What those two tests protect
     * is a way of shipping trades that does not exist here.
     *
     * <p>The two pool tests below are the counterparts of 26.2's single
     * {@code modTradesAreMergedIntoTheVanillaTradePools}: villager and wandering trader offers
     * live in separate lists on this line, so the same statement takes two tests. The parity gate
     * in {@code tools/testrunner/run.py} records that pairing in {@code LINE_DIFFERENCES}.
     */

    /**
     * Every villager offer of the mod table has to sit in the very pool a real villager of that
     * profession and level draws from - including the experimental/rebalanced map when the world
     * has the trade rebalance feature enabled, because that is the map
     * {@code Villager#updateTrades} then reads.
     *
     * <p>The pools must also keep their vanilla listings: an adapter that replaces instead of
     * appends would still make our own offers show up.
     */
    public static void modTradesAreMergedIntoTheVillagerTradePools(GameTestHelper helper) {
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(1, 2, 1));
        boolean rebalance = tradeRebalanceActive(helper);
        List<String> problems = new ArrayList<>();
        int checked = 0;

        for (VillagerTradeGroup group : ModTradeDefinitions.villagerTrades()) {
            ResourceKey<VillagerProfession> profession = group.profession();

            // Vanilla's trade rebalance replaces the librarian pools wholesale; the 26.2 line skips
            // the same three checks for the same reason.
            if (rebalance && VillagerProfession.LIBRARIAN.equals(profession)) {
                continue;
            }

            VillagerTrades.ItemListing[] pool = effectiveVillagerPool(helper, profession, group.level());
            String where = profession.identifier() + " level " + group.level();
            if (pool == null) {
                problems.add(where + ": no trade pool at all");
                continue;
            }

            Set<OfferKey> actual = offersOf(helper, pool, villager);
            for (TradeDefinition trade : group.trades()) {
                checked++;
                OfferKey expected = expectedKey(helper, trade, villager);
                if (!actual.contains(expected)) {
                    problems.add(where + ": pool does not offer " + expected + "; it offers " + actual);
                }
            }
            if (pool.length <= group.trades().size()) {
                problems.add(where + ": pool holds only " + pool.length + " listings, so it lost its vanilla "
                        + "entries - the adapter replaces instead of appends");
            }
        }

        helper.assertTrue(problems.isEmpty(), "villager trade pool problems: " + problems);
        helper.assertTrue(checked > 0, "no villager trade pool was checked at all (rebalance=" + rebalance + ")");
        helper.succeed();
    }

    /**
     * Same for the three wandering trader pools. The draw counts must stay untouched as well - an
     * adapter that grew a pool but dropped its count would silently change how many offers a
     * trader shows.
     */
    public static void modTradesAreMergedIntoTheWanderingTraderPools(GameTestHelper helper) {
        WanderingTrader trader = helper.spawnWithNoFreeWill(EntityType.WANDERING_TRADER, new BlockPos(1, 2, 1));
        List<Pair<VillagerTrades.ItemListing[], Integer>> pools = VillagerTrades.WANDERING_TRADER_TRADES;
        List<String> problems = new ArrayList<>();

        helper.assertTrue(pools.size() >= 3,
                "the wandering trader should still have its three vanilla pools, found " + pools.size());

        for (WanderingTradeGroup group : ModTradeDefinitions.wanderingTraderTrades()) {
            int index = poolIndex(group.pool());
            Pair<VillagerTrades.ItemListing[], Integer> pool = pools.get(index);
            String where = "wandering trader pool " + index + " (" + group.pool() + ")";

            Set<OfferKey> actual = offersOf(helper, pool.getLeft(), trader);
            for (TradeDefinition trade : group.trades()) {
                OfferKey expected = expectedKey(helper, trade, trader);
                if (!actual.contains(expected)) {
                    problems.add(where + ": pool does not offer " + expected + "; it offers " + actual);
                }
            }
            if (pool.getLeft().length <= group.trades().size()) {
                problems.add(where + ": pool holds only " + pool.getLeft().length
                        + " listings, so it lost its vanilla entries");
            }
            if (pool.getRight() <= 0) {
                problems.add(where + ": draw count is " + pool.getRight());
            }
        }

        trader.discard();
        helper.assertTrue(problems.isEmpty(), "wandering trader pool problems: " + problems);
        helper.succeed();
    }

    /**
     * Turns three trade definitions into actual {@link MerchantOffer}s and checks the numbers:
     * wanted item + count, optional second cost, given item + count, max uses and xp. This is what
     * a player would see in the trade GUI, and the same three trades the 26.2 line pins down.
     *
     * <p>It then rolls every enchanted trade of the table {@link #ENCHANT_ROLLS} times and asserts
     * the draws in both directions: nothing outside the pool this file declares may come out, and
     * everything it declares has to come out at least once. The literal pools here are the point -
     * comparing what a trade hands out against {@link ModTradeDefinitions}' own pool would compare
     * the table with itself and hold for any edit to it. A single offer per trade states neither
     * direction: an entry <em>added</em> to a pool is drawn far too rarely to be seen (one offer in
     * twenty-one for the wandering trader's book), and an entry <em>deleted</em> from one cannot be
     * seen at all, because what is left is still a subset of what is declared. Both edits leave
     * every other number of the trade alone, so nothing else in this suite moves either.
     *
     * <p>All rolls share one {@link net.minecraft.util.RandomSource}, which moves on with every
     * offer: one long but completely fixed sequence of draws, so this cannot be red on one run and
     * green on the next. A run of consecutive seeds would be reproducible too, but a fresh
     * {@code LegacyRandomSource} per seed walks its first draw in near constant steps, which is the
     * kind of sample that can miss a low weight entry for reasons that have nothing to do with the
     * pool.
     */
    public static void tradeDefinitionsProduceTheExpectedOffers(GameTestHelper helper) {
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(1, 2, 1));

        // mason/2/emerald_copper_core: 25 emeralds -> 1 copper core, 2 uses, 10 xp.
        MerchantOffer core = offerOf(helper, villager,
                villagerTrade(helper, VillagerProfession.MASON, 2, ModItems.COPPER_CORE));
        assertOffer(helper, core, "mason/2/emerald_copper_core",
                net.minecraft.world.item.Items.EMERALD, 25, ModItems.COPPER_CORE, 1, 2, 10);
        helper.assertTrue(core.getCostB().isEmpty(),
                "mason/2/emerald_copper_core must not have a second cost item");

        // wandering_trader/octant_emerald is a buying trade: 1 octant -> 8 emeralds.
        MerchantOffer buying = offerOf(helper, villager,
                wanderingTrade(helper, WanderingTraderPool.BUY, ModItems.OCTANT));
        assertOffer(helper, buying, "wandering_trader/octant_emerald",
                ModItems.OCTANT, 1, net.minecraft.world.item.Items.EMERALD, 8, 3, 5);

        // toolsmith/4/emerald_iron_sledgehammer uses a second cost slot.
        MerchantOffer sledgehammer = offerOf(helper, villager,
                villagerTrade(helper, VillagerProfession.TOOLSMITH, 4, ModItems.IRON_SLEDGEHAMMER));
        assertOffer(helper, sledgehammer, "toolsmith/4/emerald_iron_sledgehammer",
                net.minecraft.world.item.Items.EMERALD, 16, ModItems.IRON_SLEDGEHAMMER, 1, 1, 30);
        helper.assertTrue(sledgehammer.getCostB().is(net.minecraft.world.item.Items.IRON_PICKAXE),
                "toolsmith/4/emerald_iron_sledgehammer second cost should be an iron pickaxe, was "
                        + sledgehammer.getCostB());

        // And the enchantment every one of those results is supposed to carry - the half of a trade
        // that looks the same from the outside whether it works or not.
        RandomSource rolls = RandomSource.create(OFFER_SEED);
        assertEveryDeclaredEnchantmentIsDrawn(helper, villager, rolls, "librarian/3/emerald_building_book",
                villagerTrade(helper, VillagerProfession.LIBRARIAN, 3,
                        net.minecraft.world.item.Items.ENCHANTED_BOOK), BUILDING_BOOK_POOL);
        assertEveryDeclaredEnchantmentIsDrawn(helper, villager, rolls, "librarian/4/emerald_advanced_book",
                villagerTrade(helper, VillagerProfession.LIBRARIAN, 4,
                        net.minecraft.world.item.Items.ENCHANTED_BOOK), ADVANCED_BOOK_POOL);
        assertEveryDeclaredEnchantmentIsDrawn(helper, villager, rolls, "librarian/5/emerald_master_book",
                villagerTrade(helper, VillagerProfession.LIBRARIAN, 5,
                        net.minecraft.world.item.Items.ENCHANTED_BOOK), MASTER_BOOK_POOL);
        assertEveryDeclaredEnchantmentIsDrawn(helper, villager, rolls, "toolsmith/3/emerald_iron_chisel",
                villagerTrade(helper, VillagerProfession.TOOLSMITH, 3, ModItems.IRON_CHISEL),
                CHISEL_ENCHANT_POOL);
        assertEveryDeclaredEnchantmentIsDrawn(helper, villager, rolls, "toolsmith/3/emerald_copper_chisel",
                villagerTrade(helper, VillagerProfession.TOOLSMITH, 3, ModItems.COPPER_CHISEL),
                CHISEL_ENCHANT_POOL);
        assertEveryDeclaredEnchantmentIsDrawn(helper, villager, rolls, "toolsmith/3/emerald_gold_chisel",
                villagerTrade(helper, VillagerProfession.TOOLSMITH, 3, ModItems.GOLD_CHISEL),
                CHISEL_ENCHANT_POOL);
        assertEveryDeclaredEnchantmentIsDrawn(helper, villager, rolls, "toolsmith/4/emerald_diamond_sledgehammer",
                villagerTrade(helper, VillagerProfession.TOOLSMITH, 4, ModItems.DIAMOND_SLEDGEHAMMER),
                SLEDGEHAMMER_ENCHANT_POOL);
        assertEveryDeclaredEnchantmentIsDrawn(helper, villager, rolls, "toolsmith/4/emerald_iron_sledgehammer",
                villagerTrade(helper, VillagerProfession.TOOLSMITH, 4, ModItems.IRON_SLEDGEHAMMER),
                SLEDGEHAMMER_ENCHANT_POOL);
        assertEveryDeclaredEnchantmentIsDrawn(helper, villager, rolls, "toolsmith/5/emerald_mining_pickaxe",
                villagerTrade(helper, VillagerProfession.TOOLSMITH, 5,
                        net.minecraft.world.item.Items.DIAMOND_PICKAXE), MINING_PICKAXE_POOL);
        assertEveryDeclaredEnchantmentIsDrawn(helper, villager, rolls, "wandering_trader/emerald_wand_book",
                wanderingTrade(helper, WanderingTraderPool.UNCOMMON,
                        net.minecraft.world.item.Items.ENCHANTED_BOOK), WAND_BOOK_POOL);

        helper.succeed();
    }

    /**
     * End-to-end: a real villager with the mason profession at level 2 has to be able to roll one
     * of our trades. The villager draws a random subset of the merged pool, so the offers are
     * regenerated a bounded number of times; missing every single time means the mod trades are
     * not part of the pool the merchant draws from.
     */
    public static void masonVillagerCanRollAModTrade(GameTestHelper helper) {
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(1, 2, 1));

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

        Assertions.valueEqual(helper, emptyRolls, 0, "mason level 2 produced empty offer lists");
        helper.assertTrue(rolledModTrade,
                "a mason villager (level 2) never offered copper_core or diamond_core in 64 rolls; "
                        + "results seen were " + seen);

        helper.succeed();
    }

    /**
     * End-to-end for the other merchant: a freshly spawned wandering trader has to be able to roll
     * one of our sell offers. A wandering trader draws its offers once on creation, so every
     * attempt needs its own entity.
     */
    public static void wanderingTraderCanRollAModTrade(GameTestHelper helper) {
        Set<Item> wanted = Set.of(ModItems.COPPER_CORE, ModItems.IRON_CORE, ModItems.GOLD_CORE,
                ModItems.OCTANT, ModItems.REINFORCED_BUNDLE);
        Set<Item> seen = new LinkedHashSet<>();
        boolean rolledModTrade = false;
        int emptyRolls = 0;

        for (int attempt = 0; attempt < 64 && !rolledModTrade; attempt++) {
            WanderingTrader trader = helper.spawnWithNoFreeWill(EntityType.WANDERING_TRADER, new BlockPos(1, 2, 1));
            List<MerchantOffer> offers = trader.getOffers();
            if (offers.isEmpty()) {
                emptyRolls++;
            }
            for (MerchantOffer offer : offers) {
                Item result = offer.getResult().getItem();
                seen.add(result);
                if (wanted.contains(result)) {
                    rolledModTrade = true;
                }
            }
            trader.discard();
        }

        Assertions.valueEqual(helper, emptyRolls, 0, "a wandering trader produced an empty offer list");
        helper.assertTrue(rolledModTrade,
                "a wandering trader never offered one of " + wanted + " in 64 rolls; results seen were " + seen);

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
     * <p>"The inventory" means the whole container, not the hotbar: one stack is put in
     * {@link #SLOT_OUTSIDE_THE_MENU}, the single container index the player's own menu has no slot
     * for. Everything else here is in slots 0..8, and those the menu loop would convert on its own
     * even if the inventory loop stopped after the hotbar - so they cannot say how far that loop
     * runs, and a shortened loop stayed green on all of them.
     *
     * <p>The mock player has to be a <em>connected</em> one: writing into the crafting grid makes
     * vanilla run {@code CraftingMenu#slotChangedCraftingGrid}, which unconditionally dereferences
     * {@code ServerPlayer#connection}. That matches production, where the migration only ever runs
     * from the join event and therefore always sees a player with a network handler.
     */
    public static void legacySpatulasInPlayerInventoryBecomeChisels(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.create(helper);

        // Spatulas and chisels are damageable tools, so a damaged one is always a single item;
        // the damage component itself refuses to apply to anything bigger than one.
        ItemStack stoneSpatula = new ItemStack(ModItems.STONE_SPATULA, 1);
        stoneSpatula.set(DataComponents.DAMAGE, 5);
        stoneSpatula.set(DataComponents.CUSTOM_NAME, Component.literal("Grandpa's tool"));
        player.getInventory().setItem(0, stoneSpatula);

        // The stack size is covered separately, on a stack without components to validate.
        ItemStack copperSpatulas = new ItemStack(ModItems.COPPER_SPATULA, 3);
        player.getInventory().setItem(1, copperSpatulas);

        ItemStack netheriteSpatula = new ItemStack(ModItems.NETHERITE_SPATULA, 1);
        player.getInventory().setItem(3, netheriteSpatula);

        // Untouched control: already-migrated item plus a vanilla item.
        ItemStack alreadyChisel = new ItemStack(ModItems.IRON_CHISEL, 1);
        player.getInventory().setItem(4, alreadyChisel);
        ItemStack vanilla = new ItemStack(net.minecraft.world.item.Items.DIAMOND, 12);
        player.getInventory().setItem(5, vanilla);

        // A spatula that only lives in the open menu (crafting grid slot), not in the inventory.
        ItemStack menuSpatula = new ItemStack(ModItems.GOLD_SPATULA, 1);
        player.containerMenu.getSlot(InventoryMenu.CRAFT_SLOT_START).set(menuSpatula);

        // And one the other way round: in the inventory but out of reach of the open menu. Every
        // stack above sits in the hotbar, and migratePlayer walks the menu as well as the
        // inventory - the player's own menu has a slot for all of 0..40, so those stacks would
        // still be converted by the menu loop alone and say nothing about how far the inventory
        // loop runs. Only a stack in a container slot no menu slot points at does.
        ItemStack outsideTheMenu = new ItemStack(ModItems.IRON_SPATULA, 2);
        player.getInventory().setItem(SLOT_OUTSIDE_THE_MENU, outsideTheMenu);

        LegacySpatulaMigration.migratePlayer(player);

        ItemStack migrated = player.getInventory().getItem(0);
        helper.assertTrue(migrated.is(ModItems.STONE_CHISEL),
                "stone_spatula should have become stone_chisel, was " + migrated);
        Assertions.valueEqual(helper, migrated.getCount(), 1, "migrated stack size");
        Assertions.valueEqual(helper, migrated.get(DataComponents.DAMAGE), 5, "migrated damage component");
        Assertions.valueEqual(helper, migrated.get(DataComponents.CUSTOM_NAME),
                Component.literal("Grandpa's tool"), "migrated custom name component");

        ItemStack migratedStack = player.getInventory().getItem(1);
        helper.assertTrue(migratedStack.is(ModItems.COPPER_CHISEL),
                "copper_spatula should have become copper_chisel, was " + migratedStack);
        Assertions.valueEqual(helper, migratedStack.getCount(), 3, "migrated multi item stack size");

        helper.assertTrue(player.getInventory().getItem(3).is(ModItems.NETHERITE_CHISEL),
                "netherite_spatula should have become netherite_chisel, was "
                        + player.getInventory().getItem(3));

        helper.assertTrue(player.getInventory().getItem(4).is(ModItems.IRON_CHISEL),
                "an existing iron_chisel must survive the migration untouched");
        helper.assertTrue(player.getInventory().getItem(5).is(net.minecraft.world.item.Items.DIAMOND),
                "a vanilla stack must survive the migration untouched");
        Assertions.valueEqual(helper, player.getInventory().getItem(5).getCount(), 12,
                "vanilla stack size after migration");

        ItemStack migratedMenuStack = player.containerMenu.getSlot(InventoryMenu.CRAFT_SLOT_START).getItem();
        helper.assertTrue(migratedMenuStack.is(ModItems.GOLD_CHISEL),
                "gold_spatula in the open menu should have become gold_chisel, was " + migratedMenuStack);
        Assertions.valueEqual(helper, migratedMenuStack.getCount(), 1, "migrated menu stack size");

        ItemStack migratedOutsideTheMenu = player.getInventory().getItem(SLOT_OUTSIDE_THE_MENU);
        helper.assertTrue(migratedOutsideTheMenu.is(ModItems.IRON_CHISEL),
                "the spatula in inventory slot " + SLOT_OUTSIDE_THE_MENU + " is still "
                        + migratedOutsideTheMenu + "; the inventory loop no longer walks the whole "
                        + "container, and no menu slot covers that one for it");
        Assertions.valueEqual(helper, migratedOutsideTheMenu.getCount(), 2,
                "stack size of the migrated slot outside the menu");

        MockPlayers.remove(helper, player);
        helper.succeed();
    }

    /**
     * World-side migration: a loose spatula lying on the ground is rewritten in place, so the
     * same {@link ItemEntity} now carries the chisel with the original count and components.
     *
     * <p>The scan box the migration uses reaches from y=-64 to y=320, and a gametest structure
     * stands far below y=0, so everything {@code spawnItem} puts down only ever exercises the lower
     * half of that range - pulling the ceiling down to y=0 changed nothing here while it stopped the
     * scan from finding any spatula lying on the surface. The third entity is therefore placed at
     * y={@link #PROBE_Y} by hand.
     */
    public static void legacySpatulaItemEntityIsRewrittenInPlace(GameTestHelper helper) {
        // Two entities on purpose: the damage component only validates on a single item, so
        // "components survive" and "stack size survives" cannot be checked on the same stack.
        ItemEntity damaged = helper.spawnItem(ModItems.DIAMOND_SPATULA, new BlockPos(1, 2, 1));
        ItemStack legacyDamaged = new ItemStack(ModItems.DIAMOND_SPATULA, 1);
        legacyDamaged.set(DataComponents.DAMAGE, 42);
        legacyDamaged.set(DataComponents.CUSTOM_NAME, Component.literal("Old flattener"));
        damaged.setItem(legacyDamaged);

        ItemEntity multiple = helper.spawnItem(ModItems.IRON_SPATULA, new BlockPos(5, 2, 1));
        multiple.setItem(new ItemStack(ModItems.IRON_SPATULA, 3));

        ItemEntity control = helper.spawnItem(net.minecraft.world.item.Items.STICK, new BlockPos(3, 2, 3));

        // The one entity in the upper half of the scan box - see the method javadoc.
        ItemEntity aboveSeaLevel = spawnAboveTheStructure(helper, new ItemStack(ModItems.GOLD_SPATULA, 1));

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(damaged.isAlive(), "the dropped spatula entity vanished before migration");
                    helper.assertTrue(multiple.isAlive(), "the dropped spatula stack vanished before migration");
                    helper.assertTrue(aboveSeaLevel.isAlive(),
                            "the spatula above the test room vanished before migration");
                    LegacySpatulaMigration.migrateWorlds(helper.getLevel().getServer());
                })
                .thenExecute(() -> {
                    ItemStack after = damaged.getItem();
                    helper.assertTrue(after.is(ModItems.DIAMOND_CHISEL),
                            "the item entity should now carry a diamond_chisel, was " + after);
                    Assertions.valueEqual(helper, after.getCount(), 1, "item entity stack size after migration");
                    Assertions.valueEqual(helper, after.get(DataComponents.DAMAGE), 42,
                            "item entity damage component after migration");
                    Assertions.valueEqual(helper, after.get(DataComponents.CUSTOM_NAME),
                            Component.literal("Old flattener"), "item entity custom name after migration");

                    ItemStack afterStack = multiple.getItem();
                    helper.assertTrue(afterStack.is(ModItems.IRON_CHISEL),
                            "the item entity should now carry an iron_chisel, was " + afterStack);
                    Assertions.valueEqual(helper, afterStack.getCount(), 3,
                            "item entity stack size of the multi item stack after migration");

                    ItemStack afterAbove = aboveSeaLevel.getItem();
                    helper.assertTrue(afterAbove.is(ModItems.GOLD_CHISEL),
                            "the spatula at y=" + PROBE_Y + " still carries " + afterAbove
                                    + "; the scan box no longer covers the height players build at, "
                                    + "so every spatula above y=0 survives the migration untouched");

                    helper.assertTrue(control.getItem().is(net.minecraft.world.item.Items.STICK),
                            "an unrelated item entity must not be rewritten");

                    // This line has no runBeforeTestEnd hook (see TestCleanup), and the probe
                    // stands outside the structure, so the framework's own clean-up never sees it.
                    aboveSeaLevel.discard();
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /**
     * Puts one item entity high above the test structure, in the half of the migration's scan box
     * the gametest structures themselves never reach - they stand far below y=0, so every other
     * entity in this file does too.
     *
     * <p>Gravity is switched off so it stays at {@link #PROBE_Y} for the two ticks the case needs
     * instead of falling down through a neighbouring test's room. The position is derived from
     * {@code absolutePos} rather than from a relative one, because which relative y lands above y=0
     * depends on where the server happened to drop the structure.
     */
    private static ItemEntity spawnAboveTheStructure(GameTestHelper helper, ItemStack carried) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        ItemEntity entity = new ItemEntity(level, anchor.getX() + 0.5, PROBE_Y, anchor.getZ() + 0.5, carried);
        entity.setNoGravity(true);
        helper.assertTrue(level.addFreshEntity(entity),
                "the level refused the item entity above the test room, so this case would prove nothing");

        helper.assertTrue(entity.getY() > 0.0,
                "the probe has to stand above y=0 to say anything about the upper half of the scan "
                        + "box; it is at y=" + entity.getY());
        return entity;
    }

    /**
     * Vanilla's optional "Trade Rebalance" experiment swaps the librarian pools for
     * {@code VillagerTrades.EXPERIMENTAL_TRADES}. It is off in normal worlds but tends to be on in
     * a game test world, which enables the experimental packs. Mason, toolsmith and wandering
     * trader pools are not affected by it.
     */
    private static boolean tradeRebalanceActive(GameTestHelper helper) {
        return helper.getLevel().enabledFeatures().contains(FeatureFlags.TRADE_REBALANCE);
    }

    /** The exact listing array {@code Villager#updateTrades} would read for that profession/level. */
    private static VillagerTrades.ItemListing[] effectiveVillagerPool(GameTestHelper helper,
                                                                     ResourceKey<VillagerProfession> profession,
                                                                     int level) {
        Int2ObjectMap<VillagerTrades.ItemListing[]> byLevel = null;
        if (tradeRebalanceActive(helper)) {
            byLevel = VillagerTrades.EXPERIMENTAL_TRADES.get(profession);
        }
        if (byLevel == null) {
            byLevel = VillagerTrades.TRADES.get(profession);
        }
        return byLevel == null ? null : byLevel.get(level);
    }

    /**
     * Builds one offer per listing of a pool. Vanilla listings may legitimately produce no offer
     * (or need world state a test room does not have); those are skipped, they are not what this
     * test is about.
     */
    private static Set<OfferKey> offersOf(GameTestHelper helper, VillagerTrades.ItemListing[] listings,
                                          Entity merchant) {
        Set<OfferKey> keys = new LinkedHashSet<>();
        for (VillagerTrades.ItemListing listing : listings) {
            MerchantOffer offer;
            try {
                offer = listing.getOffer(helper.getLevel(), merchant, RandomSource.create(OFFER_SEED));
            } catch (RuntimeException e) {
                continue;
            }
            if (offer != null) {
                keys.add(keyOf(offer));
            }
        }
        return keys;
    }

    /**
     * Rolls one enchanted trade {@link #ENCHANT_ROLLS} times and asserts both directions of its
     * pool: every {@code enchantment@level} a roll hands out is one this file declares, and every
     * pair this file declares is handed out at least once. The first direction catches an entry
     * added to the table, the second one an entry taken out of it - neither is visible in a single
     * offer.
     *
     * <p>{@code random} is shared by every call, so the draws are one long deterministic walk
     * instead of a set of first draws from freshly seeded generators.
     */
    private static void assertEveryDeclaredEnchantmentIsDrawn(GameTestHelper helper, Entity merchant,
                                                              RandomSource random, String id,
                                                              TradeDefinition trade, Set<String> pool) {
        VillagerTrades.ItemListing listing = trade.toListing();
        Set<String> drawn = new LinkedHashSet<>();

        for (int roll = 0; roll < ENCHANT_ROLLS; roll++) {
            MerchantOffer offer = listing.getOffer(helper.getLevel(), merchant, random);
            helper.assertTrue(offer != null, id + ": produced no offer on roll " + roll);

            ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(offer.getResult());
            helper.assertTrue(!enchantments.isEmpty(),
                    id + ": the merchant hands out an unenchanted " + offer.getResult().getItem()
                            + "; its enchantment pool did nothing on roll " + roll);

            for (Holder<Enchantment> enchantment : enchantments.keySet()) {
                String pair = enchantment.getRegisteredName() + "@" + enchantments.getLevel(enchantment);
                helper.assertTrue(pool.contains(pair),
                        id + ": the merchant put " + pair + " on the result, which is not one of the "
                                + "enchantment/level pairs the trade declares (" + pool + ")");
                drawn.add(pair);
            }
        }

        Set<String> neverDrawn = new LinkedHashSet<>(pool);
        neverDrawn.removeAll(drawn);
        helper.assertTrue(neverDrawn.isEmpty(), id + ": " + neverDrawn + " never came out of "
                + ENCHANT_ROLLS + " offers, so the trade stopped handing out what it declares "
                + "(it drew " + drawn + ")");
    }

    private static OfferKey expectedKey(GameTestHelper helper, TradeDefinition trade, Entity merchant) {
        MerchantOffer offer = trade.toListing()
                .getOffer(helper.getLevel(), merchant, RandomSource.create(OFFER_SEED));
        return keyOf(offer);
    }

    private static OfferKey keyOf(MerchantOffer offer) {
        ItemStack costA = offer.getBaseCostA();
        ItemStack costB = offer.getCostB();
        ItemStack result = offer.getResult();
        return new OfferKey(costA.getItem(), costA.getCount(),
                costB.isEmpty() ? null : costB.getItem(), costB.getCount(),
                result.getItem(), result.getCount(),
                offer.getMaxUses(), offer.getXp(), offer.getPriceMultiplier());
    }

    /** The one definition of that profession/level whose result is the given item. */
    private static TradeDefinition villagerTrade(GameTestHelper helper, ResourceKey<VillagerProfession> profession,
                                                 int level, Item result) {
        for (VillagerTradeGroup group : ModTradeDefinitions.villagerTrades()) {
            if (!group.profession().equals(profession) || group.level() != level) {
                continue;
            }
            for (TradeDefinition trade : group.trades()) {
                if (trade.result().is(result)) {
                    return trade;
                }
            }
        }
        helper.fail("no " + profession.identifier() + " level " + level + " trade gives " + result);
        throw new IllegalStateException("unreachable");
    }

    /** The one definition of that wandering trader pool whose cost or result is the given item. */
    private static TradeDefinition wanderingTrade(GameTestHelper helper, WanderingTraderPool pool, Item item) {
        for (WanderingTradeGroup group : ModTradeDefinitions.wanderingTraderTrades()) {
            if (group.pool() != pool) {
                continue;
            }
            for (TradeDefinition trade : group.trades()) {
                if (trade.cost().item().value() == item || trade.result().is(item)) {
                    return trade;
                }
            }
        }
        helper.fail("no wandering trader " + pool + " trade mentions " + item);
        throw new IllegalStateException("unreachable");
    }

    private static MerchantOffer offerOf(GameTestHelper helper, Entity merchant, TradeDefinition trade) {
        MerchantOffer offer = trade.toListing()
                .getOffer(helper.getLevel(), merchant, RandomSource.create(OFFER_SEED));
        helper.assertTrue(offer != null, "trade definition " + trade + " produced no offer");
        return offer;
    }

    private static void assertOffer(GameTestHelper helper, MerchantOffer offer, String id,
                                    Item wantedItem, int wantedCount,
                                    Item givenItem, int givenCount,
                                    int maxUses, int xp) {
        ItemStack costA = offer.getBaseCostA();
        helper.assertTrue(costA.is(wantedItem),
                id + ": expected cost item " + wantedItem + ", was " + costA);
        Assertions.valueEqual(helper, costA.getCount(), wantedCount, id + ": cost count");

        ItemStack result = offer.getResult();
        helper.assertTrue(result.is(givenItem),
                id + ": expected result item " + givenItem + ", was " + result);
        Assertions.valueEqual(helper, result.getCount(), givenCount, id + ": result count");

        Assertions.valueEqual(helper, offer.getMaxUses(), maxUses, id + ": max uses");
        Assertions.valueEqual(helper, offer.getXp(), xp, id + ": trade xp");
    }

    private static void setProfession(GameTestHelper helper, Villager villager,
                                      ResourceKey<VillagerProfession> profession, int level) {
        VillagerData data = villager.getVillagerData()
                .withProfession(helper.getLevel().registryAccess(), profession)
                .withLevel(level);
        villager.setVillagerData(data);
    }
}
