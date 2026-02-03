package com.simplebuilding.util;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public class EnchantmentHelper {

    /**
     * Ermittelt das Level eines Enchantments auf einem Item.
     * Funktioniert sicher, auch wenn world null ist (via Component Check),
     * ist aber präziser mit World (via Registry Lookup).
     */
    public static int getEnchantmentLevel(ItemStack stack, World world, RegistryKey<Enchantment> key) {
        if (stack.isEmpty()) return 0;

        // 1. Wenn wir eine World haben, nutzen wir den sicheren Registry-Weg
        if (world != null) {
            var registry = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            var entry = registry.getOptional(key);
            return entry.map(enchantmentReference -> net.minecraft.enchantment.EnchantmentHelper.getLevel(enchantmentReference, stack)).orElse(0);
        }

        // 2. Fallback: Wenn wir keine World haben (z.B. Client Tooltips oder MiningSpeed Berechnung),
        // iterieren wir direkt über die Komponente. Das ist schneller als Registry Lookups.
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null) return 0;

        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesKey(key)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    /**
     * Prüft, ob ein Enchantment vorhanden ist (Level > 0).
     */
    public static boolean hasEnchantment(ItemStack stack, World world, RegistryKey<Enchantment> key) {
        return getEnchantmentLevel(stack, world, key) > 0;
    }

    // --- Komfort-Methoden für spezifische Enchantments ---

    public static int getOverrideLevel(ItemStack stack) {
        return getEnchantmentLevel(stack, null, ModEnchantments.OVERRIDE);
    }

    public static boolean hasConstructorsTouch(ItemStack stack, World world) {
        return hasEnchantment(stack, world, ModEnchantments.CONSTRUCTORS_TOUCH);
    }

}