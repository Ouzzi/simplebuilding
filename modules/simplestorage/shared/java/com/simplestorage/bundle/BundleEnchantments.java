package com.simplestorage.bundle;

import com.simplestorage.SimpleStorage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

/**
 * The storage enchantments (split stage 8c-1, docs/ai/PLAN-B-S8C1-STORAGE-2026-10-20.md): Drawer, Deep Pockets and Funnel
 * are Simple Storage's ({@code data/simplestorage/enchantment}, old {@code simplebuilding:} ids migrate). Master Builder,
 * Color Palette and Constructor's Touch stay SimpleBuilding's; the bundles and quivers read them by key only, so
 * without SimpleBuilding they simply never sit on an item.
 */
public final class BundleEnchantments {
    public static final ResourceKey<Enchantment> DRAWER = key(SimpleStorage.MOD_ID, "drawer");
    public static final ResourceKey<Enchantment> DEEP_POCKETS = key(SimpleStorage.MOD_ID, "deep_pockets");
    public static final ResourceKey<Enchantment> FUNNEL = key(SimpleStorage.MOD_ID, "funnel");
    /** SimpleBuilding's (by id): place blocks out of the bundle, a quiver inside a worn backpack feeds the bow. */
    public static final ResourceKey<Enchantment> MASTER_BUILDER = key(SimpleStorage.OLD_NAMESPACE, "master_builder");
    public static final ResourceKey<Enchantment> COLOR_PALETTE = key(SimpleStorage.OLD_NAMESPACE, "color_palette");
    public static final ResourceKey<Enchantment> CONSTRUCTORS_TOUCH = key(SimpleStorage.OLD_NAMESPACE, "constructors_touch");

    private BundleEnchantments() {}

    private static ResourceKey<Enchantment> key(String namespace, String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(namespace, path));
    }

    /** The level of {@code key} on {@code stack}: through the registry when a level is given, else from the stack's own component. */
    public static int level(ItemStack stack, Level world, ResourceKey<Enchantment> key) {
        if (stack.isEmpty()) return 0;
        if (world != null) {
            var registry = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            return registry.get(key).map(e -> net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(e, stack)).orElse(0);
        }
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null) return 0;
        for (var entry : enchantments.entrySet()) {
            if (entry.getKey().is(key)) return entry.getIntValue();
        }
        return 0;
    }

    public static boolean has(ItemStack stack, Level world, ResourceKey<Enchantment> key) {
        return level(stack, world, key) > 0;
    }

    public static int drawerLevel(ItemStack stack) {
        return level(stack, null, DRAWER);
    }
}
