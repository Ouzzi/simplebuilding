package com.simplebuilding.loot;

import com.mojang.serialization.MapCodec;
import com.simplebuilding.Simplebuilding;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

public class ModLootFunctions {
    public static final MapCodec<? extends LootItemFunction> WEIGHTED_ENCHANT = Registry.register(
            BuiltInRegistries.LOOT_FUNCTION_TYPE,
            Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "weighted_enchant"),
            WeightedEnchantFunction.MAP_CODEC);

    // Fabric: Aufruf aus ModTradeOffers während der Mod-Initialisierung.
    // NeoForge: Aufruf aus NeoForgeRegistryBootstrap während des RegisterEvent für loot_function_type.
    public static void registerLootFunctions() {
        Simplebuilding.LOGGER.info("Registering Loot Functions for " + Simplebuilding.MOD_ID);
    }
}
