package com.simplebuilding.datagen;

import com.simplebuilding.Simplebuilding;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.util.registry.RegistryKeys;
import net.minecraft.util.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class ModWorldGenerator extends FabricDynamicRegistryProvider {
    public ModWorldGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
        // Hier fÃ¼gen wir die Keys hinzu, die wir in ModWorldGen definiert haben.
        // Da die Bootstraps bereits beim Initialisieren des RegistryBuilders laufen,
        // mÃ¼ssen wir hier nur sicherstellen, dass wir die richtigen Referenzen hinzufÃ¼gen.
        
        // Da wir die Bootstraps in 'DataGeneration.java' registrieren (siehe unten),
        // fÃ¼gt dieser Provider alles automatisch hinzu, was in der Registry ist.
        entries.addAll(registries.getOrThrow(RegistryKeys.CONFIGURED_FEATURE));
        entries.addAll(registries.getOrThrow(RegistryKeys.PLACED_FEATURE));
    }

    @Override
    public String getName() {
        return Simplebuilding.MOD_ID + " World Generator";
    }
}
