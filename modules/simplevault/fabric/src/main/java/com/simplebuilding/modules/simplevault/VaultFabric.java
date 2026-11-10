package com.simplebuilding.modules.simplevault;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.levelgen.GenerationStep;

/** Fabric entry of Simple Vault: registries are open during init, so everything registers directly; gates and effect run on the server tick. */
public final class VaultFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleVault.init(FabricLoader.getInstance().getConfigDir());
        VaultRegistry.registerBlocks();
        VaultEffect.register();
        VaultRegistry.registerItems();
        VaultRegistry.registerTab();
        VaultRegistry.registerWorldgen();
        // the rare bedrock gap into the vault (rarity and switches checked by the feature)
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_DECORATION,
                ResourceKey.create(Registries.PLACED_FEATURE, SimpleVault.id("vault_gap")));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            VaultTravel.tick(server);
            VaultEffect.tick(server);
            VaultAmbience.tick(server);
        });
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            VaultEffect.hintOnAttack(player, level, pos);
            return InteractionResult.PASS;
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> VaultAmbience.forget(handler.player.getUUID()));
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) {
            com.simplebuilding.modules.simplevault.guide.VaultGuide.installQuests(FabricLoader.getInstance().getConfigDir());
        }
    }
}
