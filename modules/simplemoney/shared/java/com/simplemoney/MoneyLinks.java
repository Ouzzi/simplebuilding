package com.simplemoney;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

/** Public registry ids only. No dependency on another module's implementation. */
public final class MoneyLinks {
    public static final int MAX_PRICE = 64, MAX_STOCK = 4, MAX_DAILY = 16;
    public static Predicate<String> loaded = mod -> false;
    public record Price(String item, int tier, double hours, int craft, int safetyFloor,
                        String merchant, int level, int stock, double chance) {}
    private record Table(List<Price> prices) {}
    public static final Map<String, Price> PRICES;
    static {
        try (var stream = Objects.requireNonNull(MoneyLinks.class.getResourceAsStream("/data/simplemoney/money/prices.json"));
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var prices = new LinkedHashMap<String, Price>();
            for (Price price : new Gson().fromJson(reader, Table.class).prices()) {
                if (price.tier() < 0 || price.tier() > 5 || !Double.isFinite(price.hours()) || price.hours() < 0 || price.hours() > 45
                        || price.craft() < 0 || price.craft() > 64 || price.safetyFloor() < 8 || price.safetyFloor() > MAX_PRICE
                        || price.stock() < 1 || price.stock() > MAX_STOCK || prices.put(price.item(), price) != null)
                    throw new IllegalArgumentException("Invalid linked price: " + price.item());
            }
            PRICES = Collections.unmodifiableMap(prices);
        } catch (Exception e) { throw new IllegalStateException("Cannot load linked prices", e); }
    }
    public static int buy(Price price) {
        var c = SimpleMoney.config.links;
        c.normalize();
        return Math.clamp((int)Math.ceil(price.hours() * c.billsPerHour + price.tier() * c.rarityStep + price.craft() * c.craftWeight),
                price.safetyFloor(), MAX_PRICE);
    }
    public static Price price(MerchantOffer offer) {
        if (offer == null || !offer.getBaseCostA().is(MoneyItems.ITEMS.get("money_bill"))) return null;
        return PRICES.get(BuiltInRegistries.ITEM.getKey(offer.getResult().getItem()).toString());
    }
    public static boolean enabled(String module) {
        return SimpleMoney.config.links.enabled && loaded.test(module);
    }
    public static boolean allowed(ServerPlayer player, MerchantOffer offer) {
        Price price = price(offer);
        if (price == null) return true;
        String mod = Identifier.parse(price.item()).getNamespace();
        return SimpleMoney.enabled(price.merchant().equals("wandering_trader") ? "enableWanderingTrades" : "enableVillagerTrades") && enabled(mod)
                && offer.getResult().getCount() == 1 && offer.getCostB().isEmpty()
                && LinkBudget.get(player).allowed(player.getUUID(), player.level().getServer().overworld().getGameTime());
    }
    public static ItemStack cost(Price price) { return new ItemStack(MoneyItems.ITEMS.get("money_bill"), buy(price)); }
    private MoneyLinks() {}
}
