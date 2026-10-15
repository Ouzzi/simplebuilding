package com.simplebuilding.modules.simplestats;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Which items keep which statistic. Decided by components and vanilla item tags, never by class, so gear of other mods
 * with vanilla behavior counts for free.
 */
public final class StatItems {
    private StatItems() {}

    /** Anything with a tool component mines: pickaxes, axes, shovels, hoes, shears, hammers. */
    public static boolean mines(ItemStack stack) { return !stack.isEmpty() && stack.has(DataComponents.TOOL); }

    /** Melee and ranged weapons: weapon or tool component, bow, crossbow, trident, mace. */
    public static boolean fights(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.has(DataComponents.WEAPON) || stack.has(DataComponents.TOOL) || stack.is(Items.BOW) || stack.is(Items.CROSSBOW)
                || stack.is(Items.TRIDENT) || stack.is(Items.MACE);
    }

    public static boolean shears(ItemStack stack) { return stack.is(Items.SHEARS); }
    public static boolean glider(ItemStack stack) { return !stack.isEmpty() && stack.has(DataComponents.GLIDER); }
    public static boolean shield(ItemStack stack) { return !stack.isEmpty() && stack.has(DataComponents.BLOCKS_ATTACKS); }
    public static boolean rod(ItemStack stack) { return stack.is(Items.FISHING_ROD); }
    public static boolean brush(ItemStack stack) { return stack.is(Items.BRUSH); }
    public static boolean lighter(ItemStack stack) { return stack.is(Items.FLINT_AND_STEEL); }

    /** Armor points the stack gives in this slot (0 for items that are no armor). */
    public static double armorPoints(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) return 0.0;
        return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).compute(Attributes.ARMOR, 0.0, slot);
    }

    /** True for armor and mount armor worn in this slot. */
    public static boolean armor(ItemStack stack, EquipmentSlot slot) { return armorPoints(stack, slot) > 0.0; }

    /** Boots: an item that is worn on the feet (also without armor points). */
    public static boolean boots(ItemStack stack) {
        Equippable equippable = stack.isEmpty() ? null : stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.FEET;
    }

    /** Pieces that collect wearing time: armor and the glider in the chest slot. */
    public static boolean worn(ItemStack stack, EquipmentSlot slot) {
        return armor(stack, slot) || slot == EquipmentSlot.CHEST && glider(stack);
    }
}
