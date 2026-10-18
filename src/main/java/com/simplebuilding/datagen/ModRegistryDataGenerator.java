package com.simplebuilding.datagen;

import com.simplebuilding.util.ModWorldGen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import java.util.concurrent.CompletableFuture;

public class ModRegistryDataGenerator extends FabricDynamicRegistryProvider {
    public ModRegistryDataGenerator(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(HolderLookup.Provider registries, Entries entries) {
        // 26.3 (split stage 7): Break Through, Override and Radius are Simple Hammers' (its own files); the bootstrap keeps
        // them under simplehammers: only so the other generated data (loot tables, trades) can name them.
        var enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
        if (com.simplebuilding.version.McVersion.HAMMERS_IN_SB) {
            entries.addAll(enchantments);
        } else {
            enchantments.listElements().filter(e -> !e.key().identifier().getNamespace().equals(com.simplebuilding.version.McVersion.HAMMERS_NAMESPACE))
                    .forEach(entries::add);
        }

        entries.addAll(registries.lookupOrThrow(Registries.TRIM_MATERIAL));
        entries.addAll(registries.lookupOrThrow(Registries.TRIM_PATTERN));
        entries.addAll(registries.lookupOrThrow(ModWorldGen.FEATURE_REGISTRY));
        entries.addAll(registries.lookupOrThrow(Registries.PLACED_FEATURE));
        if (com.simplebuilding.version.McVersion.MUSIC_DISCS) {
            entries.addAll(registries.lookupOrThrow(Registries.JUKEBOX_SONG));
        }
    }

    @Override
    public String getName() {
        return "";
    }
}
