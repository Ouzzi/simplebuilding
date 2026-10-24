package com.simplegadgets;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * SimpleBuilding's enchantments the gadgets read, by id (principle 3): Range (Gauge: deeper altimeter) and Constructor's
 * Touch (Gauge: top/average speed, auto-walk follows paths). SimpleBuilding's item tags {@code range_enchantable} and
 * {@code constructors_touch_enchantable} name the gadgets; without SimpleBuilding no item has them and every level is 0.
 */
public final class GadgetEnchantments {
    public static final ResourceKey<Enchantment> RANGE = key("simplebuilding", "range");
    public static final ResourceKey<Enchantment> CONSTRUCTORS_TOUCH = key("simplebuilding", "constructors_touch");

    private GadgetEnchantments() {}

    private static ResourceKey<Enchantment> key(String namespace, String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(namespace, path));
    }

    /** The level of {@code key} on {@code stack}; with a level through its registry, without one from the stack's component. */
    public static int level(ItemStack stack, @Nullable Level level, ResourceKey<Enchantment> key) {
        if (stack.isEmpty()) return 0;
        if (level != null) {
            var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            return registry.get(key).map(e -> net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(e, stack)).orElse(0);
        }
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null) return 0;
        for (var entry : enchantments.entrySet()) {
            if (entry.getKey().is(key)) return entry.getIntValue();
        }
        return 0;
    }

    public static boolean hasConstructorsTouch(ItemStack stack, @Nullable Level level) {
        return level(stack, level, CONSTRUCTORS_TOUCH) > 0;
    }
}
