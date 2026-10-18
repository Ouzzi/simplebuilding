package com.simplehammers.engine;

import com.simplehammers.SimpleHammers;
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
 * The hammer enchantments (data driven, {@code data/simplehammers/enchantment}; moved from SimpleBuilding, ids migrate):
 * Break Through (depth), Override (I: any pickaxe block of the area, II: axe/shovel/hoe blocks too) and Radius (5x5).
 * Constructor's Touch (reshaping backwards) stays SimpleBuilding's enchantment and is read by id - without SimpleBuilding
 * no item has it.
 */
public final class HammerEnchantments {
    public static final ResourceKey<Enchantment> BREAK_THROUGH = key(SimpleHammers.MOD_ID, "break_through");
    public static final ResourceKey<Enchantment> OVERRIDE = key(SimpleHammers.MOD_ID, "override");
    public static final ResourceKey<Enchantment> RADIUS = key(SimpleHammers.MOD_ID, "radius");
    /** SimpleBuilding's Constructor's Touch (chisels, wands, hammer): Sneak + hammer reshapes backwards. */
    public static final ResourceKey<Enchantment> CONSTRUCTORS_TOUCH = key("simplebuilding", "constructors_touch");

    private HammerEnchantments() {}

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

    public static int overrideLevel(ItemStack stack) {
        return level(stack, null, OVERRIDE);
    }

    public static boolean hasConstructorsTouch(ItemStack stack, @Nullable Level level) {
        return level(stack, level, CONSTRUCTORS_TOUCH) > 0;
    }
}
