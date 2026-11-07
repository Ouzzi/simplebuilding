package com.simplebuildingtools.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Simple Storage's bundles and backpacks by id (no class of it is imported): the wand takes building blocks out of them when they
 * carry Master Builder. Without Simple Storage nothing matches.
 */
public final class StorageRefs {
    public static final String STORAGE_NAMESPACE = "simplestorage";

    private StorageRefs() {
    }

    /** A mod bundle or quiver (a {@code BundleItem} of Simple Storage); never Vanilla's bundles. */
    public static boolean isStorageBundle(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.BundleItem
                && STORAGE_NAMESPACE.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
    }

    /** One of Simple Storage's backpacks. */
    public static boolean isBackpack(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return STORAGE_NAMESPACE.equals(key.getNamespace()) && key.getPath().endsWith("backpack");
    }
}
