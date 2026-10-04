package com.simplemoney.testing;

import com.simplemoney.*;
import com.mojang.serialization.JsonOps;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.util.Unit;

public final class LinkTests {
    public static void conditions(GameTestHelper h) {
        var registry = h.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
        for (var p : MoneyLinks.PRICES.values()) {
            String mod = Identifier.parse(p.item()).getNamespace();
            boolean expected = SimpleMoney.enabled("links:" + mod) && SimpleMoney.enabled(p.merchant().equals("wandering_trader") ? "enableWanderingTrades" : "enableVillagerTrades");
            h.assertValueEqual(registry.containsKey(Identifier.parse("simplemoney:links/" + p.item().replace(':', '/'))), expected, "conditional registry " + p.item());
            if (MoneyLinks.loaded.test(mod)) h.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.parse(p.item())), "public item exists " + p.item());
        }
        h.assertTrue(!SimpleMoney.enabled("links:missing_test_module"), "absent mod fails closed");
        var old = MoneyLinks.loaded;
        try { MoneyLinks.loaded = id -> false; h.assertTrue(!SimpleMoney.enabled("links:simplebuilding"), "unloaded real mod denied"); }
        finally { MoneyLinks.loaded = old; }
        if (MoneyTests.isModLoaded("simplebuilding")) h.assertTrue(registry.containsKey(Identifier.parse("simplebuilding:librarian/3/emerald_building_book")), "old non-money trades remain");
        h.succeed();
    }
    public static void offers(GameTestHelper h) {
        var level = h.getLevel();
        var villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, villager.position())
                .withParameter(LootContextParams.THIS_ENTITY, villager).withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE).create(LootContextParamSets.VILLAGER_TRADE);
        var context = new LootContext.Builder(params).withOptionalRandomSeed(12345L).create(Optional.empty());
        var registry = level.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
        for (var p : MoneyLinks.PRICES.values()) {
            var trade = registry.getValue(Identifier.parse("simplemoney:links/" + p.item().replace(':', '/')));
            if (trade == null) continue; // conditions() verifies exactly which rows must exist.
            MerchantOffer offer = null;
            for (int i = 0; i < 4096 && offer == null; i++) offer = trade.getOffer(context);
            h.assertTrue(offer != null, "scarce offer eventually appears " + p.item());
            h.assertValueEqual(offer.getCostA().getCount(), MoneyLinks.buy(p), "server formula " + p.item());
            h.assertValueEqual(BuiltInRegistries.ITEM.getKey(offer.getResult().getItem()).toString(), p.item(), "foreign result");
            h.assertValueEqual(offer.getResult().getCount(), 1, "bounded quantity");
            h.assertTrue(offer.getMaxUses() <= 4 && offer.getXp() == 0, "stock and XP bounds");
            int base = offer.getCostA().getCount(); offer.setSpecialPriceDiff(-1000000);
            h.assertValueEqual(offer.getCostA().getCount(), base, "Hero/reputation cannot undercut floor");
            offer.setSpecialPriceDiff(1000000); h.assertValueEqual(offer.getCostA().getCount(), base, "price cannot overflow stack");
            var copy = MerchantOffer.CODEC.parse(JsonOps.INSTANCE, MerchantOffer.CODEC.encodeStart(JsonOps.INSTANCE, offer).getOrThrow()).getOrThrow();
            h.assertValueEqual(copy.getCostA().getCount(), base, "saved offer keeps protected price");
        }
        h.succeed();
    }
    public static void bounds(GameTestHelper h) {
        var config = new SimpleMoney.Links();
        config.billsPerHour = Integer.MIN_VALUE; config.rarityStep = -1; config.craftWeight = -99;
        config.stock = Integer.MAX_VALUE; config.dailyLimit = Integer.MAX_VALUE; config.cooldownTicks = -1; config.normalize();
        h.assertTrue(config.billsPerHour == 1 && config.rarityStep == 2 && config.craftWeight == 1 && config.stock == 4 && config.dailyLimit == 16 && config.cooldownTicks == 20, "hostile config clamped");
        config.billsPerHour = Integer.MAX_VALUE; config.rarityStep = Integer.MAX_VALUE; config.craftWeight = Integer.MAX_VALUE;
        config.stock = -1; config.dailyLimit = -1; config.cooldownTicks = Integer.MAX_VALUE; config.normalize();
        h.assertTrue(config.billsPerHour == 2 && config.rarityStep == 8 && config.craftWeight == 4 && config.stock == 1 && config.dailyLimit == 1 && config.cooldownTicks == 1200, "opposite bounds clamped");
        for (var price : MoneyLinks.PRICES.values()) h.assertTrue(MoneyLinks.buy(price) >= price.safetyFloor() && MoneyLinks.buy(price) <= 64, "price cap");
        h.succeed();
    }
    public static void rarity(GameTestHelper h) {
        int previous = 0;
        for (String material : List.of("copper", "iron", "gold", "diamond", "netherite", "enderite")) {
            var p = MoneyLinks.PRICES.get("simplebuilding:" + material + "_core"); int price = MoneyLinks.buy(p);
            h.assertTrue(price >= previous && p.merchant().equals("wandering_trader"), "core progression and no mason cores");
            previous = price;
        }
        for (int hours = 0; hours <= 45; hours++) for (int craft = 0; craft <= 64; craft++) {
            int last = 0;
            for (int tier = 0; tier <= 5; tier++) {
                int price = MoneyLinks.buy(new MoneyLinks.Price("test:item",tier,hours,craft,8,"mason",1,1,1));
                h.assertTrue(price >= last, "rarity never lowers price"); last = price;
            }
        }
        h.succeed();
    }
    public static void budgets(GameTestHelper h) {
        var budget = new LinkBudget(); UUID player = UUID.randomUUID();
        for (int i = 0; i < SimpleMoney.config.links.dailyLimit; i++) {
            long now = 1000L + (long)i * SimpleMoney.config.links.cooldownTicks;
            h.assertTrue(budget.spend(player,now), "daily purchase " + i);
            h.assertTrue(!budget.spend(player,now) && !budget.spend(player,now-1), "spam and clock rollback denied");
            var restored = LinkBudget.CODEC.parse(JsonOps.INSTANCE, LinkBudget.CODEC.encodeStart(JsonOps.INSTANCE,budget).getOrThrow()).getOrThrow();
            h.assertTrue(!restored.allowed(player,now), "restart/login retains cooldown");
        }
        h.assertTrue(!budget.allowed(player,23999), "changing merchants/items/dimensions cannot refresh player quota");
        h.assertTrue(budget.spend(player,25000), "rolling 24000-tick window refreshes");
        h.assertTrue(budget.allowed(UUID.randomUUID(),25000), "independent player budget");
        h.succeed();
    }
    public static void menu(GameTestHelper h) {
        if (MoneyTests.partnerMissing(h, "simplebuilding", "linked trade menu with a SimpleBuilding item")) return;
        var player = h.makeMockServerPlayerInLevel(); player.setUUID(UUID.randomUUID());
        var merchant = EntityTypes.VILLAGER.create(h.getLevel(),EntitySpawnReason.COMMAND); merchant.setTradingPlayer(player);
        var price = MoneyLinks.PRICES.get("simplebuilding:iron_chisel");
        var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(price.item()));
        var offer = new MerchantOffer(new ItemCost(MoneyItems.ITEMS.get("money_bill"),MoneyLinks.buy(price)),new ItemStack(item),4,0,0);
        var offers = new MerchantOffers(); offers.add(offer); merchant.setOffers(offers);
        var menu = new MerchantMenu(1,player.getInventory(),merchant);
        menu.getSlot(0).set(new ItemStack(MoneyItems.ITEMS.get("money_bill"),64)); menu.setSelectionHint(0); menu.updateSellItem();
        h.assertTrue(menu.getSlot(2).mayPickup(player), "paid normal pickup allowed");
        h.assertTrue(!menu.quickMoveStack(player,2).isEmpty(), "paid quick-move works");
        int remaining = menu.getSlot(0).getItem().getCount();
        h.assertValueEqual(remaining,64-MoneyLinks.buy(price), "one purchase consumes exact protected price");
        h.assertTrue(!menu.getSlot(2).mayPickup(player), "normal click cooldown enforced");
        h.assertTrue(menu.quickMoveStack(player,2).isEmpty(), "shift-click bypass denied before transfer");
        h.assertValueEqual(menu.getSlot(0).getItem().getCount(),remaining,"denied request consumes nothing");
        offer.resetUses(); h.assertTrue(menu.quickMoveStack(player,2).isEmpty(), "restock cannot erase player cooldown");
        var original = SimpleMoney.config.links.enabled;
        try { SimpleMoney.config.links.enabled=false; h.assertTrue(!menu.getSlot(2).mayPickup(player),"live feature disable refuses saved offers"); }
        finally { SimpleMoney.config.links.enabled=original; }
        h.succeed();
    }
    public static void noArbitrage(GameTestHelper h) {
        var registry = h.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
        for (var row : MoneyLinks.PRICES.values()) {
            // No linked sell edge exists, even for farmable heads, materials or containers.
            h.assertTrue(!registry.containsKey(Identifier.parse("simplemoney:links/sell/"+row.item().replace(':','/'))), "buy/sell loop has zero return");
            h.assertTrue(MoneyLinks.buy(row)>0,"buy requires real money");
        }
        var legacy = new MerchantOffer(new ItemCost(Items.EMERALD,5),new ItemStack(Items.BREAD),12,1,.1f);
        legacy.setSpecialPriceDiff(-2); h.assertTrue(MoneyLinks.price(legacy)==null && legacy.getCostA().getCount()==3,"old non-money discounts unchanged");
        h.succeed();
    }
}
