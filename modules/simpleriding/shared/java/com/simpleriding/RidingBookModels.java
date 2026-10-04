package com.simpleriding;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Stored-enchantment model selection, kept free of client classes for GameTests. */
public final class RidingBookModels {
    private static final Identifier VANILLA_BOOK = Identifier.withDefaultNamespace("enchanted_book");

    private RidingBookModels() {}

    public static Identifier select(ItemStack stack, Identifier original) {
        if (!stack.is(Items.ENCHANTED_BOOK) || !VANILLA_BOOK.equals(original)) return original;
        var enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchantments == null) return original;
        // A stable priority also covers combined books, independent of map iteration order.
        for (var key : List.of(Riding.LEAPING, Riding.TAILWIND)) {
            for (var holder : enchantments.keySet()) {
                if (holder.is(key) && enchantments.getLevel(holder) > 0) {
                    return Riding.id("enchanted_book_" + key.identifier().getPath());
                }
            }
        }
        return original;
    }
}
