package com.simplelib.registry;

import com.simplelib.SimpleLib;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.alchemy.Potion;

/**
 * The base potion of the warped wart (owner N71): water bottle + warped wart. Like the awkward potion it has no effect; the
 * mod potions of the Simple mods are brewed from it (Vanilla potions stay with the nether wart).
 */
public final class LibPotions {
    public static Holder<Potion> WARPED;

    public static void register() {
        if (WARPED == null) {
            WARPED = Registry.registerForHolder(BuiltInRegistries.POTION, SimpleLib.id("warped"), new Potion("warped"));
        }
    }

    private LibPotions() {}
}
