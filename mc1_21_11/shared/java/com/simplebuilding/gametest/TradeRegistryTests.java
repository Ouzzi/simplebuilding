package com.simplebuilding.gametest;

import com.simplebuilding.trade.EnchantmentPool;
import com.simplebuilding.trade.ModTradeDefinitions;
import com.simplebuilding.trade.ModTradeDefinitions.VillagerTradeGroup;
import com.simplebuilding.trade.ModTradeDefinitions.WanderingTradeGroup;
import com.simplebuilding.trade.TradeDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.trading.ItemCost;

/**
 * The mod ships 20 villager trades. On the 26.2 line those are data files and the test asks the
 * {@code villager_trade} datapack registry for them; MC 1.21.11 has no such registry - trades are
 * plain {@code VillagerTrades.ItemListing}s built from the loader-neutral table in
 * {@link ModTradeDefinitions}.
 *
 * <p>The equivalent question here is therefore: does that table still describe all 20 offers, and
 * does every item / enchantment it names actually exist in the running server? A typo in an
 * enchantment key or an item that lost its registration would otherwise only blow up when a
 * merchant happens to roll that offer.
 *
 * <p>Whether the offers really reach the merchant pools is a separate question and is covered by
 * {@link TradeAndMigrationTests}.
 */
public final class TradeRegistryTests {

    private static final int EXPECTED_VILLAGER_TRADES = 12;
    private static final int EXPECTED_WANDERING_TRADES = 8;
    private static final int EXPECTED_TRADES = EXPECTED_VILLAGER_TRADES + EXPECTED_WANDERING_TRADES;

    public static void allModTradesResolveAgainstTheServerRegistries(GameTestHelper helper) {
        Registry<Enchantment> enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<String> problems = new ArrayList<>();

        int villagerTrades = 0;
        for (VillagerTradeGroup group : ModTradeDefinitions.villagerTrades()) {
            String where = group.profession().identifier() + "/" + group.level();
            for (TradeDefinition trade : group.trades()) {
                villagerTrades++;
                checkTrade(where, trade, enchantments, problems);
            }
        }

        int wanderingTrades = 0;
        for (WanderingTradeGroup group : ModTradeDefinitions.wanderingTraderTrades()) {
            String where = "wandering_trader/" + group.pool().name().toLowerCase(Locale.ROOT);
            for (TradeDefinition trade : group.trades()) {
                wanderingTrades++;
                checkTrade(where, trade, enchantments, problems);
            }
        }

        helper.assertTrue(problems.isEmpty(), "unresolvable trade definitions: " + problems);
        helper.assertValueEqual(villagerTrades, EXPECTED_VILLAGER_TRADES, "number of villager trade definitions");
        helper.assertValueEqual(wanderingTrades, EXPECTED_WANDERING_TRADES, "number of wandering trader trade definitions");
        helper.assertValueEqual(villagerTrades + wanderingTrades, EXPECTED_TRADES, "total number of mod trade definitions");
        helper.succeed();
    }

    private static void checkTrade(String where, TradeDefinition trade, Registry<Enchantment> enchantments,
                                   List<String> problems) {
        checkCost(where, "wants", trade.cost(), problems);
        trade.additionalCost().ifPresent(cost -> checkCost(where, "additional_wants", cost, problems));

        if (trade.result().isEmpty()) {
            problems.add(where + ": trade gives an empty stack");
        } else {
            Item item = trade.result().getItem();
            if (BuiltInRegistries.ITEM.getKey(item) == null) {
                problems.add(where + ": result item " + item + " is not in BuiltInRegistries.ITEM");
            }
        }
        if (trade.maxUses() <= 0) {
            problems.add(where + ": maxUses must be positive but is " + trade.maxUses());
        }
        if (trade.xp() < 0) {
            problems.add(where + ": xp must not be negative but is " + trade.xp());
        }

        for (EnchantmentPool.Entry entry : trade.enchantments().entries()) {
            if (!enchantments.containsKey(entry.enchantment())) {
                problems.add(where + ": unknown enchantment " + entry.enchantment().identifier());
            }
            if (entry.weight() <= 0) {
                problems.add(where + ": enchantment " + entry.enchantment().identifier() + " has weight " + entry.weight());
            }
            if (entry.level() <= 0) {
                problems.add(where + ": enchantment " + entry.enchantment().identifier() + " has level " + entry.level());
            }
        }
    }

    private static void checkCost(String where, String slot, ItemCost cost, List<String> problems) {
        Holder<Item> item = cost.item();
        if (!item.isBound()) {
            problems.add(where + ": " + slot + " points at the unbound item " + item.getRegisteredName());
        } else if (BuiltInRegistries.ITEM.getKey(item.value()) == null) {
            problems.add(where + ": " + slot + " item is not in BuiltInRegistries.ITEM");
        }
        if (cost.count() <= 0) {
            problems.add(where + ": " + slot + " count must be positive but is " + cost.count());
        }
    }
}
