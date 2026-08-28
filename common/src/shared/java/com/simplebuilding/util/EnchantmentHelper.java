package com.simplebuilding.util;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

public class EnchantmentHelper {

    private EnchantmentHelper() {}

    public static int getEnchantmentLevel(ItemStack stack, Level world, ResourceKey<Enchantment> key) {
        if (stack.isEmpty()) return 0;

        if (world != null) {
            var registry = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var entry = registry.get(key);
            return entry.map(e -> net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(e, stack)).orElse(0);
        }

        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null) return 0;

        for (var entry : enchantments.entrySet()) {
            if (entry.getKey().is(key)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    public static boolean hasEnchantment(ItemStack stack, Level world, ResourceKey<Enchantment> key) {
        return getEnchantmentLevel(stack, world, key) > 0;
    }

    public static int getOverrideLevel(ItemStack stack) {
        return getEnchantmentLevel(stack, null, ModEnchantments.OVERRIDE);
    }

    public static boolean hasConstructorsTouch(ItemStack stack, Level world) {
        return hasEnchantment(stack, world, ModEnchantments.CONSTRUCTORS_TOUCH);
    }

    public static int getFastChiselingLevel(ItemStack stack) {
        return getEnchantmentLevel(stack, null, ModEnchantments.FAST_CHISELING);
    }

    public static int getDrawerLevel(ItemStack stack) {
        return getEnchantmentLevel(stack, null, ModEnchantments.DRAWER);
    }

    public static boolean hasColorPalette(ItemStack stack, Level world) {
        return hasEnchantment(stack, world, ModEnchantments.COLOR_PALETTE);
    }

    public static boolean hasMasterBuilder(ItemStack stack, Level world) {
        return hasEnchantment(stack, world, ModEnchantments.MASTER_BUILDER);
    }

    public static int getMagnetRangeLevel(ItemStack stack, Level world) {
        return getEnchantmentLevel(stack, world, ModEnchantments.RANGE);
    }
}