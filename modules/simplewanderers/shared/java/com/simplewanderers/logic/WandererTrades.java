package com.simplewanderers.logic;

import com.simplewanderers.SimpleWanderers;
import com.simplewanderers.config.WandererConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * The wanderer's offers (concept sections 5 and 6): sets are drawn with a seed from village, period and world seed, so
 * every player sees the same stock and a reload never re-rolls. Vanilla has no restock for the Wandering Trader, so the
 * period arithmetic lives here and the entity only asks {@link #period} and {@link #half} once per second.
 */
public final class WandererTrades {
    /** Draw order = the order offers appear in the trading screen. Amounts live in data/simplewanderers/trade_set. */
    public static final List<String> SETS = List.of("wanderer/buying", "wanderer/common", "wanderer/uncommon", "wanderer/rare");
    /** The villager types of Vanilla (VillagerType.byBiome); any other value falls back to plains. */
    public static final List<String> TYPES = List.of("desert", "jungle", "plains", "savanna", "snow", "swamp", "taiga");

    public static ResourceKey<TradeSet> setKey(String path) {
        return ResourceKey.create(Registries.TRADE_SET, SimpleWanderers.id(path));
    }

    public static String biomeSet(String villagerType) {
        return "wanderer/biome/" + (TYPES.contains(villagerType) ? villagerType : "plains");
    }

    /** Index of the reroll period a game time falls into (floor division: never negative drift). */
    public static long period(long gameTime) {
        return Math.floorDiv(gameTime, WandererConfig.rerollTicks());
    }

    /** Index of the restock period a game time falls into. */
    public static long half(long gameTime) {
        return Math.floorDiv(gameTime, WandererConfig.restockTicks());
    }

    public static long seed(long villageId, long period, long worldSeed, int set) {
        long h = villageId * 0x9E3779B97F4A7C15L ^ period * 0xC2B2AE3D27D4EB4FL ^ worldSeed * 0x165667B19E3779F9L ^ (set + 1L) * 0x27D4EB2F165667C5L;
        h ^= h >>> 32;
        h *= 0xD6E8FEB86659FD93L;
        h ^= h >>> 32;
        return h == 0L ? 1L : h;
    }

    /** Draws every set (and the biome set) for one period; duplicates of an already drawn trade are skipped. */
    public static void draw(ServerLevel level, Entity merchant, MerchantOffers offers, long villageId, long period, String villagerType) {
        long worldSeed = level.getSeed();
        int index = 0;
        for (String set : SETS) {
            drawSet(level, merchant, offers, setKey(set), seed(villageId, period, worldSeed, index++));
        }
        if (WandererConfig.biomeAssortment) {
            drawSet(level, merchant, offers, setKey(biomeSet(villagerType)), seed(villageId, period, worldSeed, index));
        }
    }

    /** Like Vanilla's addOffersFromTradeSet, but seeded, and without the same item twice in the whole list. */
    public static void drawSet(ServerLevel level, Entity merchant, MerchantOffers offers, ResourceKey<TradeSet> key, long seed) {
        Optional<TradeSet> found = level.registryAccess().lookupOrThrow(Registries.TRADE_SET).getOptional(key);
        if (found.isEmpty()) return;
        TradeSet set = found.get();
        LootContext context = new LootContext.Builder(new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, merchant.position())
                .withParameter(LootContextParams.THIS_ENTITY, merchant)
                .withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
                .create(LootContextParamSets.VILLAGER_TRADE)).withOptionalRandomSeed(seed).create(set.randomSequence());
        int wanted = set.calculateNumberOfTrades(context);
        List<Holder<VillagerTrade>> pool = new ArrayList<>();
        set.trades().forEach(pool::add);
        int found2 = 0;
        while (found2 < wanted && !pool.isEmpty()) {
            Holder<VillagerTrade> trade = pool.remove(context.getRandom().nextInt(pool.size()));
            MerchantOffer offer = trade.value().getOffer(context);
            if (offer == null || contains(offers, offer)) continue;
            offers.add(offer);
            found2++;
        }
    }


    /** Every offer a set's pool can produce, without the amount limit (wiki and tests): predicate-less offers only appear when their chance passes. */
    public static List<MerchantOffer> poolOffers(ServerLevel level, Entity merchant, ResourceKey<TradeSet> key) {
        List<MerchantOffer> out = new ArrayList<>();
        Optional<TradeSet> found = level.registryAccess().lookupOrThrow(Registries.TRADE_SET).getOptional(key);
        if (found.isEmpty()) return out;
        LootContext context = new LootContext.Builder(new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, merchant.position())
                .withParameter(LootContextParams.THIS_ENTITY, merchant)
                .withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
                .create(LootContextParamSets.VILLAGER_TRADE)).create(found.get().randomSequence());
        found.get().trades().forEach(trade -> {
            MerchantOffer offer = trade.value().getOffer(context);
            if (offer != null) out.add(offer);
        });
        return out;
    }

    private static boolean contains(MerchantOffers offers, MerchantOffer offer) {
        for (MerchantOffer have : offers) {
            if (have.getResult().getItem() == offer.getResult().getItem()
                    && have.getBaseCostA().getItem() == offer.getBaseCostA().getItem()) return true;
        }
        return false;
    }

    private WandererTrades() {}
}
