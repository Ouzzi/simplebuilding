package com.simplebuilding.items;

import net.minecraft.world.item.ItemStack;

/**
 * Ein Geraet, dessen Haltbarkeit eine Ladung ist: es zerbricht nie, wird nur leer und laesst sich im
 * Amboss mit einem Material aufladen - ohne Stufenkosten (Resonanzstab: Amethystscherben, Rotator:
 * Enderperlen). Der Amboss ({@code AnvilScreenHandlerMixin}) verbraucht nur so viel Material, wie bis
 * voll noetig ist, und gibt das Ergebnis auch mit 0 Stufen heraus.
 */
public interface AnvilRechargeable {

    /** Ob {@code material} im rechten Ambossfeld dieses Geraet aufladen darf. */
    boolean isRechargeMaterial(ItemStack material);

    /** Wie viel Ladung (Haltbarkeitspunkte) ein Stueck Material zurueckgibt. */
    int chargePerMaterial();

    /** Wie viel Material (hoechstens {@code available}) {@code stack} bis voll braucht; 0 = schon voll. */
    default int rechargeMaterialNeeded(ItemStack stack, int available) {
        int missing = stack.getDamageValue();
        int per = Math.max(1, chargePerMaterial());
        return Math.max(0, Math.min(available, (missing + per - 1) / per));
    }

    /** Kopie von {@code stack}, mit {@code count} Stueck Material aufgeladen (nie ueber voll). */
    default ItemStack rechargedWith(ItemStack stack, int count) {
        ItemStack result = stack.copy();
        result.setDamageValue(Math.max(0, stack.getDamageValue() - count * chargePerMaterial()));
        return result;
    }

    /** Leer: die ganze Ladung ist verbraucht (Schaden = Haltbarkeit). */
    static boolean isEmpty(ItemStack stack) {
        return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage();
    }
}
