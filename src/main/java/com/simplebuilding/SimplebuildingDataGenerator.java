package com.simplebuilding;

import com.simplebuilding.datagen.*;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.trim.ModTrimMaterials;
import com.simplebuilding.util.ModWorldGen;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.core.RegistrySetBuilder;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;

public class SimplebuildingDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

        pack.addProvider(ModBlockTagProvider::new);
        pack.addProvider(ModItemTagProvider::new);
        pack.addProvider(ModLootTableProvider::new);
        pack.addProvider(ModModelProvider::new);
        pack.addProvider(ModRecipeProvider::new);
        pack.addProvider(ModRegistryDataGenerator::new);
        pack.addProvider(ModEnchantmentTagProvider::new);
        pack.addProvider(ModWorldGenerator::new);
        // Item-Eigenschaften aus der Registry fuer wiki/generate.py
        // Explizit typisiert: eine Methodenreferenz waere mehrdeutig, weil der
        // Konstruktor auf PackOutput sowohl auf DataProvider.Factory als auch
        // auf Pack.Factory passt.
        pack.addProvider((FabricPackOutput out, CompletableFuture<HolderLookup.Provider> reg) -> new WikiDataProvider(out, reg));
    }

    @Override
    public void buildRegistry(RegistrySetBuilder registryBuilder) {
        registryBuilder.add(Registries.ENCHANTMENT, ModEnchantments::bootstrap);

        registryBuilder.add(Registries.CONFIGURED_FEATURE, ModWorldGen::bootstrapConfiguredFeatures);
        registryBuilder.add(Registries.PLACED_FEATURE, ModWorldGen::bootstrapPlacedFeatures);

        // NEU: Trim Materials registrieren!
        registryBuilder.add(Registries.TRIM_MATERIAL, ModTrimMaterials::bootstrap);
    }
}
