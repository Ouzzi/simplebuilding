package com.simpleriding.test;

import com.simpleriding.Riding;
import com.simpleriding.RidingBookModels;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class RidingBookTests {
    public static void models(GameTestHelper h) {
        var lookup = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var original = Identifier.withDefaultNamespace("enchanted_book");
        for (var key : java.util.List.of(Riding.LEAPING, Riding.TAILWIND)) {
            for (int level = 1; level <= 3; level++) {
                var book = EnchantmentHelper.createBook(new EnchantmentInstance(lookup.getOrThrow(key), level));
                var before = book.copy();
                var expected = Riding.id("enchanted_book_" + key.identifier().getPath());
                h.assertTrue(RidingBookModels.select(book, original).equals(expected), "Stored Riding book selects " + expected);
                h.assertTrue(ItemStack.matches(book, before), "Rendering does not mutate the book");
                var custom = Riding.id("custom_book");
                h.assertTrue(RidingBookModels.select(book, custom).equals(custom), "Explicit custom models survive");
                h.assertTrue(RidingBookModels.select(book, null) == null, "Removed model stays absent");
            }
        }
        var vanilla = EnchantmentHelper.createBook(new EnchantmentInstance(lookup.getOrThrow(Enchantments.MENDING), 1));
        h.assertTrue(RidingBookModels.select(vanilla, original).equals(original), "Vanilla/SimpleBuilding model selection survives");
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        h.assertTrue(RidingBookModels.select(book, original).equals(original), "Empty book falls back");
        var stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(lookup.getOrThrow(Riding.TAILWIND), 3);
        stored.set(lookup.getOrThrow(Riding.LEAPING), 1);
        stored.set(lookup.getOrThrow(Enchantments.MENDING), 1);
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        h.assertTrue(RidingBookModels.select(book, original).equals(Riding.id("enchanted_book_leaping")), "Leaping wins on mixed books regardless of level");
        var armor = new ItemStack(Items.DIAMOND_HORSE_ARMOR);
        armor.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        h.assertTrue(RidingBookModels.select(armor, original).equals(original), "Non-books are untouched");
        book.remove(DataComponents.STORED_ENCHANTMENTS);
        book.set(DataComponents.ENCHANTMENTS, stored.toImmutable());
        h.assertTrue(RidingBookModels.select(book, original).equals(original), "Only stored enchantments select the book");
        h.succeed();
    }
}
