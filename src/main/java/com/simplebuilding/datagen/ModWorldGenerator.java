package com.simplebuilding.datagen;

import com.simplebuilding.Simplebuilding;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import java.util.concurrent.CompletableFuture;

public class ModWorldGenerator extends FabricDynamicRegistryProvider {
    public ModWorldGenerator(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(HolderLookup.Provider registries, Entries entries) {
        // Hier fügen wir die Keys hinzu, die wir in ModWorldGen definiert haben.
        // Da die Bootstraps bereits beim Initialisieren des RegistryBuilders laufen,
        // müssen wir hier nur sicherstellen, dass wir die richtigen Referenzen hinzufügen.
        
        // Da wir die Bootstraps in 'DataGeneration.java' registrieren (siehe unten),
        // fügt dieser Provider alles automatisch hinzu, was in der Registry ist.
        entries.addAll(registries.lookupOrThrow(Registries.CONFIGURED_FEATURE));
        entries.addAll(registries.lookupOrThrow(Registries.PLACED_FEATURE));
    }

    @Override
    public String getName() {
        return Simplebuilding.MOD_ID + " World Generator";
    }
}