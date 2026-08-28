package com.simplebuilding.loot;

import com.simplebuilding.Simplebuilding;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;

public class ModLootFunctions {
    // MC 1.21.11: BuiltInRegistries.LOOT_FUNCTION_TYPE haelt LootItemFunctionType<?>-Instanzen,
    // die den MapCodec kapseln (ab 26.2 liegt der MapCodec direkt in der Registry).
    public static final LootItemFunctionType<WeightedEnchantFunction> WEIGHTED_ENCHANT = Registry.register(
            BuiltInRegistries.LOOT_FUNCTION_TYPE,
            Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "weighted_enchant"),
            new LootItemFunctionType<>(WeightedEnchantFunction.MAP_CODEC));

    // Fabric: Aufruf aus ModTradeOffers während der Mod-Initialisierung.
    // NeoForge: Aufruf aus NeoForgeRegistryBootstrap während des RegisterEvent für loot_function_type.
    public static void registerLootFunctions() {
        Simplebuilding.LOGGER.info("Registering Loot Functions for " + Simplebuilding.MOD_ID);
    }
}
