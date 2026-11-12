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
        // a-nihilplanks (owner 12.11.): the Nihilit planks of each End wood are Simple End's, only with SimpleBuilding (Nihilit's owner);
        // their old SimpleBuilding ids (a-end07) move here (SimpleLib IdMoves, Fabric: the NeoForge/Forge builds have no SimpleLib).
        SimpleEnd.setSimpleBuilding(FabricLoader.getInstance().isModLoaded(SimpleEnd.NIHILITH_OWNER));
        for (EndWood w : EndWood.ALL) {
            String path = w.id("nihilith_W_planks");
            for (var registry : java.util.List.of(Registries.BLOCK, Registries.ITEM)) {
                com.simplelib.api.IdMoves.move(registry, net.minecraft.resources.Identifier.fromNamespaceAndPath(SimpleEnd.NIHILITH_OWNER, path), SimpleEnd.id(path));
            }
        }
        // CMD24: /simpleend config for every option.
        // a-audsb31e2: after every saved change the nested Dragon Fight switch follows (it was only set at the server start before)
        com.simplelib.api.config.ConfigCommands.server(SimpleEnd.ID).json(SimpleEndConfig::toJson, SimpleEndConfig::apply).file(FabricLoader.getInstance().getConfigDir(), SimpleEndConfig.FILE)
                .after(SimpleEnd::applySubSwitches).register();
        EndRegistry.blocks();
        EndRegistry.effects();
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
