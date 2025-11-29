package com.simplebuilding.enchantment;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.Simplebuilding;
import net.minecraft.enchantment.effect.EnchantmentEntityEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEnchantmentEffects {


    private static MapCodec<? extends EnchantmentEntityEffect> registerEntityEffect(String name,
                                                                                    MapCodec<? extends EnchantmentEntityEffect> codec) {
        return Registry.register(Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE, Identifier.of(Simplebuilding.MOD_ID, name), codec);
    }

    public static void registerEnchantmentEffects() {
        Simplebuilding.LOGGER.info("Registering Mod Enchantment Effects for " + Simplebuilding.MOD_ID);
    }
}