package com.simplebuilding.compat.accessory;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 26.3 twin of common/src/mc26_2/java/.../AccessorySlots.java: since split stage 8c-1 (docs/ai/PLAN-B-S8C1-STORAGE-2026-10-20.md)
 * the hook list is SimpleLib's ({@link com.simplelib.api.AccessorySlots}), so Simple Storage's quivers see the accessory
 * slots SimpleBuilding's Trinkets/Curios hooks report. This class only forwards; behaviour and lookup order are the same.
 */
public final class AccessorySlots {
    private AccessorySlots() {
    }

    /** One accessory mod's view of an entity's worn accessories (see SimpleLib's {@code AccessorySlots.Hook}). */
    public interface Hook extends com.simplelib.api.AccessorySlots.Hook {
    }

    public static void register(Hook hook) {
        com.simplelib.api.AccessorySlots.register(hook);
    }

    public static void unregister(Hook hook) {
        com.simplelib.api.AccessorySlots.unregister(hook);
    }

    public static boolean active() {
        return com.simplelib.api.AccessorySlots.active();
    }

    public static ItemStack findFirst(LivingEntity entity, Predicate<ItemStack> test) {
        return com.simplelib.api.AccessorySlots.findFirst(entity, test);
    }

    public static ItemStack findFirstVisible(LivingEntity entity, Predicate<ItemStack> test) {
        return com.simplelib.api.AccessorySlots.findFirstVisible(entity, test);
    }

    public static List<ItemStack> findAll(LivingEntity entity, Predicate<ItemStack> test) {
        return com.simplelib.api.AccessorySlots.findAll(entity, test);
    }

    public static boolean isWorn(LivingEntity entity, ItemStack stack) {
        return com.simplelib.api.AccessorySlots.isWorn(entity, stack);
    }
}
