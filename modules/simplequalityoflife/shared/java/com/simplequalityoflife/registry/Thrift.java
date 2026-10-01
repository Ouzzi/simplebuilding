package com.simplequalityoflife.registry;

import com.simplequalityoflife.Simplequalityoflife;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Die Verzauberung Sparsamkeit (data/simplequalityoflife/enchantment/thrift.json): nur Schluessel und Rechnung. */
public final class Thrift {
    public static final ResourceKey<Enchantment> KEY = ResourceKey.create(Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath(Simplequalityoflife.MOD_ID, "thrift"));
    public static final int MAX_LEVEL = 3;

    private Thrift() {
    }

    /** Stufe auf dem Stapel, 0 ohne. Liest die Komponente direkt (ohne Registry), weil es in getMaxDamage laeuft. */
    public static int level(ItemStack stack) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) {
            return 0;
        }
        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            if (holder.is(KEY)) {
                return Math.min(MAX_LEVEL, entry.getIntValue());
            }
        }
        return 0;
    }

    /** Maximale Haltbarkeit mit Sparsamkeit: +1/3 je Stufe, Stufe III = doppelt. */
    public static int scaled(int max, int level) {
        int clamped = Math.max(0, Math.min(MAX_LEVEL, level));
        return max + max * clamped / MAX_LEVEL;
    }
}
