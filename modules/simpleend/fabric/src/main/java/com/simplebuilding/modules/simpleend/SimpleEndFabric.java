package com.simplebuilding.modules.simpleend;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;

public final class SimpleEndFabric implements ModInitializer {
    @Override public void onInitialize() {
        SimpleEnd.init(FabricLoader.getInstance().getConfigDir());
        EndRegistry.blocks();
        EndRegistry.items();
        EndRegistry.entities();
        EndRegistry.featureTypes();
        EndRegistry.sounds();
        EndRegistry.tab();
        EndRegistry.common();
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(EndRegistry.NIBBLEMITE, com.simplebuilding.modules.simpleend.entity.Nibblemite.createAttributes());
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simplebuilding.modules.simpleend.guide.EndGuide.installQuests(FabricLoader.getInstance().getConfigDir());
        ServerLifecycleEvents.SERVER_STARTING.register(server -> { SimpleEnd.applySubSwitches(); com.simplebuilding.modules.simpleend.world.EndBiomes.serverStarting(server); });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> com.simplebuilding.modules.simpleend.world.EndBiomes.serverStopped());
        // Nachtrag 42: sparse End grass on the Vanilla outer islands too (NeoForge/Forge: biome_modifier JSON).
        BiomeModifications.addFeature(BiomeSelectors.includeByKey(Biomes.END_HIGHLANDS, Biomes.END_MIDLANDS),
                GenerationStep.Decoration.VEGETAL_DECORATION, ResourceKey.create(Registries.PLACED_FEATURE, SimpleEnd.id("end_grass_sparse")));
    }
}
