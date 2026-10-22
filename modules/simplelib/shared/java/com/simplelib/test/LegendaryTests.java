package com.simplelib.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simplelib.api.LegendaryMoments;
import com.simplelib.config.LibConfig;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;

/**
 * Legendary moments (SP22-2): which advancements and enchantments celebrate, through the real Vanilla paths
 * ({@code PlayerAdvancements#award}, the anvil's {@code onTake}, the enchanting trigger). Every body runs synchronously and
 * restores the config, so parallel tests never see changed values.
 */
public final class LegendaryTests {
    private LegendaryTests() {}

    /** "How Did We Get Here?" celebrates once; a normal advancement does not; the switch, the list and "all challenges" work. */
    public static void advancements(GameTestHelper h) {
        try {
            LibConfig.apply(new JsonObject());
            ServerPlayer player = player(h);
            award(h, player, "nether/all_effects");
            h.assertTrue(LegendaryMoments.celebrations(player) == 1, "How Did We Get Here? did not celebrate once: " + LegendaryMoments.celebrations(player));
            award(h, player, "story/mine_stone");
            award(h, player, "adventure/sniper_duel");
            h.assertTrue(LegendaryMoments.celebrations(player) == 1, "a normal advancement or a plain challenge celebrated");

            JsonObject off = new JsonObject();
            off.addProperty("legendaryEffects", false);
            LibConfig.apply(off);
            ServerPlayer quiet = player(h);
            award(h, quiet, "adventure/adventuring_time");
            h.assertTrue(LegendaryMoments.celebrations(quiet) == 0, "switched off but celebrated");

            JsonObject listed = new JsonObject();
            JsonArray extra = new JsonArray();
            extra.add("minecraft:story/mine_stone");
            listed.add("legendaryAdvancements", extra);
            LibConfig.apply(listed);
            ServerPlayer second = player(h);
            award(h, second, "story/mine_stone");
            h.assertTrue(LegendaryMoments.celebrations(second) == 1, "config list did not extend the advancements");

            JsonObject all = new JsonObject();
            all.addProperty("legendaryAllChallenges", true);
            LibConfig.apply(all);
            ServerPlayer third = player(h);
            award(h, third, "adventure/sniper_duel");
            award(h, third, "story/smelt_iron");
            h.assertTrue(LegendaryMoments.celebrations(third) == 1, "every challenge switch: sniper duel should celebrate, smelt iron not");
        } finally {
            LibConfig.reset();
        }
        h.succeed();
    }

    /**
     * Tag {@code #simplelib:legendary}: Swift Sneak III from a book on the anvil celebrates, Swift Sneak II, Sharpness V and an
     * already present level do not; the config list extends the tag; the enchanting table path counts a fresh item.
     */
    public static void enchantments(GameTestHelper h) {
        try {
            LibConfig.apply(new JsonObject());
            var lookup = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            for (var key : List.of(Enchantments.SWIFT_SNEAK, Enchantments.SOUL_SPEED, Enchantments.WIND_BURST)) {
                h.assertTrue(lookup.getOrThrow(key).is(LegendaryMoments.ENCHANTMENTS), key.identifier() + " missing in #simplelib:legendary");
            }
            h.assertFalse(lookup.getOrThrow(Enchantments.MENDING).is(LegendaryMoments.ENCHANTMENTS), "mending must not be legendary by default");
            Holder<Enchantment> sneak = lookup.getOrThrow(Enchantments.SWIFT_SNEAK);
            Holder<Enchantment> sharpness = lookup.getOrThrow(Enchantments.SHARPNESS);

            ServerPlayer player = player(h);
            anvil(h, player, new ItemStack(Items.DIAMOND_LEGGINGS), book(sneak, 2));
            h.assertTrue(LegendaryMoments.celebrations(player) == 0, "Swift Sneak II is below the maximum but celebrated");
            ItemStack result = anvil(h, player, new ItemStack(Items.DIAMOND_LEGGINGS), book(sneak, 3));
            h.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(sneak, result) == 3, "anvil gave no Swift Sneak III: " + result);
            h.assertTrue(LegendaryMoments.celebrations(player) == 1, "Swift Sneak III on the anvil did not celebrate");
            ItemStack worn = result.copy();
            worn.setDamageValue(200);
            anvil(h, player, worn, new ItemStack(Items.DIAMOND_LEGGINGS)); // repair keeps Swift Sneak III: nothing new
            h.assertTrue(LegendaryMoments.celebrations(player) == 1, "an already present Swift Sneak III celebrated again");
            anvil(h, player, new ItemStack(Items.DIAMOND_SWORD), book(sharpness, 5));
            h.assertTrue(LegendaryMoments.celebrations(player) == 1, "Sharpness V is not legendary by default");

            JsonObject listed = new JsonObject();
            JsonArray extra = new JsonArray();
            extra.add("minecraft:sharpness");
            extra.add("not a valid id!");
            listed.add("legendaryEnchantments", extra);
            LibConfig.apply(listed);
            h.assertTrue(LibConfig.legendaryEnchantments.equals(List.of(Identifier.withDefaultNamespace("sharpness"))), "invalid ids kept: " + LibConfig.legendaryEnchantments);
            anvil(h, player, new ItemStack(Items.DIAMOND_SWORD), book(sharpness, 5));
            h.assertTrue(LegendaryMoments.celebrations(player) == 2, "config list did not extend the tag");

            // Enchanting table path: the minecraft:enchanted_item trigger with a freshly enchanted item.
            ItemStack table = book(sharpness, 5);
            net.minecraft.advancements.CriteriaTriggers.ENCHANTED_ITEM.trigger(player, table, 30);
            h.assertTrue(LegendaryMoments.celebrations(player) == 3, "enchanting trigger did not celebrate");
        } finally {
            LibConfig.reset();
        }
        h.succeed();
    }

    private static ItemStack book(Holder<Enchantment> enchantment, int level) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantmentHelper.updateEnchantments(book, m -> m.set(enchantment, level));
        return book;
    }

    /** Puts both inputs into a real anvil menu and takes the result like a player. */
    private static ItemStack anvil(GameTestHelper h, ServerPlayer player, ItemStack left, ItemStack right) {
        player.experienceLevel = 100;
        AnvilMenu menu = new AnvilMenu(0, player.getInventory());
        menu.getSlot(0).set(left);
        menu.getSlot(1).set(right);
        menu.createResult();
        ItemStack result = menu.getSlot(2).getItem();
        h.assertFalse(result.isEmpty(), "anvil gave no result for " + left + " + " + right);
        ItemStack taken = result.copy();
        menu.getSlot(2).onTake(player, result);
        return taken;
    }

    private static void award(GameTestHelper h, ServerPlayer player, String path) {
        var holder = h.getLevel().getServer().getAdvancements().get(Identifier.withDefaultNamespace(path));
        h.assertTrue(holder != null, "advancement missing: " + path);
        for (String criterion : holder.value().criteria().keySet()) player.getAdvancements().award(holder, criterion);
        h.assertTrue(player.getAdvancements().getOrStartProgress(holder).isDone(), "advancement not done: " + path);
    }

    private static ServerPlayer player(GameTestHelper h) {
        Player raw = h.makeMockServerPlayer(GameType.SURVIVAL);
        if (!(raw instanceof ServerPlayer player)) throw h.assertionException("makeMockServerPlayer returned " + raw.getClass().getName());
        return player;
    }
}
