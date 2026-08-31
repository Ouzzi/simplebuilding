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
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

        helper.assertValueEqual(emptyRolls, 0, "mason level 2 produced empty offer lists");
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

        helper.assertValueEqual(emptyRolls, 0, "a wandering trader produced an empty offer list");
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

        LegacySpatulaMigration.migratePlayer(player);

        ItemStack migrated = player.getInventory().getItem(0);
        helper.assertTrue(migrated.is(ModItems.STONE_CHISEL),
                "stone_spatula should have become stone_chisel, was " + migrated);
        helper.assertValueEqual(migrated.getCount(), 1, "migrated stack size");
        helper.assertValueEqual(migrated.get(DataComponents.DAMAGE), 5, "migrated damage component");
        helper.assertValueEqual(migrated.get(DataComponents.CUSTOM_NAME),
                Component.literal("Grandpa's tool"), "migrated custom name component");

        ItemStack migratedStack = player.getInventory().getItem(1);
        helper.assertTrue(migratedStack.is(ModItems.COPPER_CHISEL),
                "copper_spatula should have become copper_chisel, was " + migratedStack);
        helper.assertValueEqual(migratedStack.getCount(), 3, "migrated multi item stack size");

        helper.assertTrue(player.getInventory().getItem(3).is(ModItems.NETHERITE_CHISEL),
                "netherite_spatula should have become netherite_chisel, was "
                        + player.getInventory().getItem(3));

        helper.assertTrue(player.getInventory().getItem(4).is(ModItems.IRON_CHISEL),
                "an existing iron_chisel must survive the migration untouched");
        helper.assertTrue(player.getInventory().getItem(5).is(net.minecraft.world.item.Items.DIAMOND),
                "a vanilla stack must survive the migration untouched");
        helper.assertValueEqual(player.getInventory().getItem(5).getCount(), 12,
                "vanilla stack size after migration");

        ItemStack migratedMenuStack = player.containerMenu.getSlot(InventoryMenu.CRAFT_SLOT_START).getItem();
        helper.assertTrue(migratedMenuStack.is(ModItems.GOLD_CHISEL),
                "gold_spatula in the open menu should have become gold_chisel, was " + migratedMenuStack);
        helper.assertValueEqual(migratedMenuStack.getCount(), 1, "migrated menu stack size");

        MockPlayers.remove(helper, player);
        helper.succeed();
    }

    /**
     * World-side migration: a loose spatula lying on the ground is rewritten in place, so the
     * same {@link ItemEntity} now carries the chisel with the original count and components.
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

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(damaged.isAlive(), "the dropped spatula entity vanished before migration");
                    helper.assertTrue(multiple.isAlive(), "the dropped spatula stack vanished before migration");
                    LegacySpatulaMigration.migrateWorlds(helper.getLevel().getServer());
                })
                .thenExecute(() -> {
                    ItemStack after = damaged.getItem();
                    helper.assertTrue(after.is(ModItems.DIAMOND_CHISEL),
                            "the item entity should now carry a diamond_chisel, was " + after);
                    helper.assertValueEqual(after.getCount(), 1, "item entity stack size after migration");
                    helper.assertValueEqual(after.get(DataComponents.DAMAGE), 42,
                            "item entity damage component after migration");
                    helper.assertValueEqual(after.get(DataComponents.CUSTOM_NAME),
                            Component.literal("Old flattener"), "item entity custom name after migration");

                    ItemStack afterStack = multiple.getItem();
                    helper.assertTrue(afterStack.is(ModItems.IRON_CHISEL),
                            "the item entity should now carry an iron_chisel, was " + afterStack);
                    helper.assertValueEqual(afterStack.getCount(), 3,
                            "item entity stack size of the multi item stack after migration");

                    helper.assertTrue(control.getItem().is(net.minecraft.world.item.Items.STICK),
                            "an unrelated item entity must not be rewritten");
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

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
        helper.assertValueEqual(costA.getCount(), wantedCount, id + ": cost count");

        ItemStack result = offer.getResult();
        helper.assertTrue(result.is(givenItem),
                id + ": expected result item " + givenItem + ", was " + result);
        helper.assertValueEqual(result.getCount(), givenCount, id + ": result count");

        helper.assertValueEqual(offer.getMaxUses(), maxUses, id + ": max uses");
        helper.assertValueEqual(offer.getXp(), xp, id + ": trade xp");
    }

    private static void setProfession(GameTestHelper helper, Villager villager,
                                      ResourceKey<VillagerProfession> profession, int level) {
        VillagerData data = villager.getVillagerData()
                .withProfession(helper.getLevel().registryAccess(), profession)
                .withLevel(level);
        villager.setVillagerData(data);
    }
}
