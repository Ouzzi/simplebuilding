package com.simplefun.registry;

import com.simplefun.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

public class ModEnchantments {

    // Data-driven enchantment (defined in data/simplefun/enchantment/no_damage.json). Only the key is needed in code.
    public static final ResourceKey<Enchantment> NO_DAMAGE =
            ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "no_damage"));
}
