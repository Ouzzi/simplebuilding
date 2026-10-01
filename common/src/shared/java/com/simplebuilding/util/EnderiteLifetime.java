package com.simplebuilding.util;

import net.minecraft.world.item.ItemStack;

/**
 * Jeder Enderit-Gegenstand ({@link ModTags.Items#DOUBLE_DESPAWN_TIME}, enthaelt
 * {@code #simplebuilding:enderite_items}) liegt als Item-Entity doppelt so lange wie Vanilla erlaubt:
 * {@link ModTags.Items#DOUBLE_DESPAWN_LIFETIME} Ticks.
 *
 * <p>Auf Fabric prueft {@code ItemEntity#tick} das Literal 6000, das {@code EnderiteItemMixin}
 * ersetzt. NeoForge und Forge patchen diese Stelle auf ein Feld {@code lifespan} und fragen beim
 * Ablauf ein {@code ItemExpireEvent}; deren Handler verlaengern ueber {@link #extraLife}.
 */
public final class EnderiteLifetime {

    private EnderiteLifetime() {
    }

    public static boolean hasDoubleDespawnTime(ItemStack stack) {
        return !stack.isEmpty() && stack.typeHolder().is(ModTags.Items.DOUBLE_DESPAWN_TIME);
    }

    /** Die Lebensdauer fuer {@code stack}, ausgehend von der Vanilla-Grenze {@code vanillaLifetime}. */
    public static int lifetime(ItemStack stack, int vanillaLifetime) {
        if (!stack.isEmpty() && stack.typeHolder().is(ModTags.Items.QUADRUPLE_DESPAWN_TIME)) {
            return ModTags.Items.QUADRUPLE_DESPAWN_LIFETIME;
        }
        return hasDoubleDespawnTime(stack) ? ModTags.Items.DOUBLE_DESPAWN_LIFETIME : vanillaLifetime;
    }

    /** Wie viele Ticks ein ablaufendes Item mit {@code lifespan} noch dazubekommt; 0 = keine. */
    public static int extraLife(ItemStack stack, int lifespan) {
        int target = lifetime(stack, lifespan);
        return target > lifespan ? target - lifespan : 0;
    }
}
