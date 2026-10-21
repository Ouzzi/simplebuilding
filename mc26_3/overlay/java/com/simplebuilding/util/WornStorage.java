package com.simplebuilding.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * 26.3 twin of common/src/mc26_2/java/.../WornStorage.java (split stage 8c-2, docs/ai/PLAN-B-S8C2-STORAGE-2026-10-21.md): the
 * backpacks are Simple Storage's; its worn backpack offers its entries through SimpleLib
 * {@link com.simplelib.api.CarriedStacks}, each naming the backpack it sits in. Only entries of a container that carries
 * {@code key} count (Master Builder for the wand and the pick key, Constructor's Touch for the hand refill) - SimpleBuilding's
 * enchantments, asked for on the backpack by key; no class of Simple Storage is imported.
 */
public final class WornStorage {
    private WornStorage() {
    }

    /** One stack in the worn backpack: a copy, and how a changed copy (empty = taken out) goes back in. */
    public record Entry(ItemStack stack, Consumer<ItemStack> writeBack) {
        public void write(ItemStack changed) {
            writeBack.accept(changed);
        }
    }

    /** The entries of the worn backpack with {@code key}, in its order; empty without such a backpack. */
    public static List<Entry> entries(Player player, ResourceKey<Enchantment> key) {
        List<Entry> out = new ArrayList<>();
        for (com.simplelib.api.CarriedStacks.Entry entry : com.simplelib.api.CarriedStacks.drawable(player)) {
            if (entry.container().isEmpty() || !EnchantmentHelper.hasEnchantment(entry.container(), player.level(), key)) continue;
            out.add(new Entry(entry.stack(), entry::write));
        }
        return out;
    }
}
