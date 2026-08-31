package com.simplebuilding.enchantment;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.Simplebuilding;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;

public class ModEnchantmentEffects {


    @SuppressWarnings("unused")
    private static MapCodec<? extends EnchantmentEntityEffect> registerEntityEffect(String name,
                                                                                    MapCodec<? extends EnchantmentEntityEffect> codec) {
        return Registry.register(BuiltInRegistries.ENCHANTMENT_ENTITY_EFFECT_TYPE, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name), codec);
    }

    public static void registerEnchantmentEffects() {
        Simplebuilding.LOGGER.info("Registering Mod Enchantment Effects for " + Simplebuilding.MOD_ID);
    }
}