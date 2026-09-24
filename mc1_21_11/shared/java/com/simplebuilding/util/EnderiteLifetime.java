package com.simplebuilding.util;

import net.minecraft.world.item.ItemStack;

/**
 * Enderit ab dem Barren ({@link ModTags.Items#ENDERITE_INGOT_TIER}) liegt als Item-Entity doppelt so
 * lange wie Vanilla erlaubt: {@link ModTags.Items#ENDERITE_INGOT_TIER_LIFETIME} Ticks.
 *
 * <p>Auf Fabric prueft {@code ItemEntity#tick} das Literal 6000, das {@code EnderiteItemMixin}
 * ersetzt. NeoForge und Forge patchen diese Stelle auf ein Feld {@code lifespan} und fragen beim
 * Ablauf ein {@code ItemExpireEvent}; deren Handler verlaengern ueber {@link #extraLife}.
 */
public final class EnderiteLifetime {

    private EnderiteLifetime() {
    }

    public static boolean isEnderiteIngotTier(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModTags.Items.ENDERITE_INGOT_TIER);
    }

    /** Die Lebensdauer fuer {@code stack}, ausgehend von der Vanilla-Grenze {@code vanillaLifetime}. */
    public static int lifetime(ItemStack stack, int vanillaLifetime) {
        return isEnderiteIngotTier(stack) ? ModTags.Items.ENDERITE_INGOT_TIER_LIFETIME : vanillaLifetime;
    }

    /** Wie viele Ticks ein ablaufendes Item mit {@code lifespan} noch dazubekommt; 0 = keine. */
    public static int extraLife(ItemStack stack, int lifespan) {
        return isEnderiteIngotTier(stack) && lifespan < ModTags.Items.ENDERITE_INGOT_TIER_LIFETIME
                ? ModTags.Items.ENDERITE_INGOT_TIER_LIFETIME - lifespan
                : 0;
    }
}
