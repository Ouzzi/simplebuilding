package com.simplelib.api;

import net.minecraft.world.item.ItemStack;

/**
 * A gadget whose durability is a charge (OW24-2 G2, Simple Gadgets: Transposer, Resonance Rod): it never breaks, it only runs empty
 * and an anvil recharges it with one material, without level cost. SimpleLib's {@code AnvilRechargeMixin} uses only as much
 * material as the gadget needs to be full and hands out the result even at 0 levels. Same contract as SimpleBuilding's own
 * interface of the same name (which stays for its 26.2 line); a gadget implements exactly one of them.
 */
public interface AnvilRechargeable {

    /** Whether {@code material} in the right anvil slot may recharge this gadget. */
    boolean isRechargeMaterial(ItemStack material);

    /** How much charge (durability points) one piece of material gives back. */
    int chargePerMaterial();

    /** How much material (at most {@code available}) {@code stack} needs to be full; 0 = already full. */
    default int rechargeMaterialNeeded(ItemStack stack, int available) {
        int missing = stack.getDamageValue();
        int per = Math.max(1, chargePerMaterial());
        return Math.max(0, Math.min(available, (missing + per - 1) / per));
    }

    /** A copy of {@code stack} recharged with {@code count} pieces of material (never above full). */
    default ItemStack rechargedWith(ItemStack stack, int count) {
        ItemStack result = stack.copy();
        result.setDamageValue(Math.max(0, stack.getDamageValue() - count * chargePerMaterial()));
        return result;
    }

    /** Empty: the whole charge is used up (damage = durability). */
    static boolean isEmpty(ItemStack stack) {
        return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage();
    }
}
