package com.simplebuilding.modules.simplelayers;

import com.simplebuilding.modules.simplelayers.world.BackroomsTravel;
import com.simplebuilding.modules.simplelayers.world.CloudTransition;
import com.simplebuilding.modules.simplelayers.world.ListenerAmbience;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;

/** Fabric entry: registries are open during init, so everything registers directly; travel runs on the server tick. */
public final class LayersFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleLayers.init(FabricLoader.getInstance().getConfigDir());
        LayersRegistry.registerSounds();
        LayersRegistry.registerBlocks();
        LayersRegistry.registerItems();
        LayersRegistry.registerTab();
        LayersRegistry.registerWorldgen();
        // The Backrooms entrance in the Overworld bedrock floor (the feature itself rolls the rarity and checks the switches).
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_DECORATION,
                ResourceKey.create(Registries.PLACED_FEATURE, SimpleLayers.id("glitched_bedrock")));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            CloudTransition.tick(server);
            BackroomsTravel.tickVoid(server);
            ListenerAmbience.tick(server);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            CloudTransition.forget(handler.player.getUUID());
            ListenerAmbience.forget(handler.player.getUUID());
        });
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) {
            com.simplebuilding.modules.simplelayers.guide.LayersGuide.installQuests(FabricLoader.getInstance().getConfigDir());
        }
    }
}
