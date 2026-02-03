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

    private EnchantmentHelper() {}

    public static int getEnchantmentLevel(ItemStack stack, World world, RegistryKey<Enchantment> key) {
        if (stack.isEmpty()) return 0;

        if (world != null) {
            var registry = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            var entry = registry.getOptional(key);
            return entry.map(e -> net.minecraft.enchantment.EnchantmentHelper.getLevel(e, stack)).orElse(0);
        }

        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null) return 0;

        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesKey(key)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    public static boolean hasEnchantment(ItemStack stack, World world, RegistryKey<Enchantment> key) {
        return getEnchantmentLevel(stack, world, key) > 0;
    }

    public static int getOverrideLevel(ItemStack stack) {
        return getEnchantmentLevel(stack, null, ModEnchantments.OVERRIDE);
    }

    public static boolean hasConstructorsTouch(ItemStack stack, World world) {
        return hasEnchantment(stack, world, ModEnchantments.CONSTRUCTORS_TOUCH);
    }

    public static int getFastChiselingLevel(ItemStack stack) {
        return getEnchantmentLevel(stack, null, ModEnchantments.FAST_CHISELING);
    }

    public static int getDrawerLevel(ItemStack stack) {
        return getEnchantmentLevel(stack, null, ModEnchantments.DRAWER);
    }

    public static boolean hasColorPalette(ItemStack stack, World world) {
        return hasEnchantment(stack, world, ModEnchantments.COLOR_PALETTE);
    }

    public static boolean hasMasterBuilder(ItemStack stack, World world) {
        return hasEnchantment(stack, world, ModEnchantments.MASTER_BUILDER);
    }

    public static int getMagnetRangeLevel(ItemStack stack, World world) {
        return getEnchantmentLevel(stack, world, ModEnchantments.RANGE);
    }
}