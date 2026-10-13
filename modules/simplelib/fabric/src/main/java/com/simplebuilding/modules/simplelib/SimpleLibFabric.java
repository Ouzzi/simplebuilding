package com.simplebuilding.modules.simplelib;

import com.simplelib.SimpleLib;
import com.simplelib.crucible.CrucibleBlockEntity;
import com.simplelib.registry.LibBlockEntities;
import com.simplelib.registry.LibBlocks;
import com.simplelib.registry.LibComponents;
import com.simplelib.registry.LibItems;
import com.simplelib.registry.LibMenus;
import com.simplelib.registry.LibRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class SimpleLibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleLib.init(FabricLoader.getInstance().getConfigDir());
        LibComponents.register();
        LibBlocks.register();
        com.simplelib.registry.LibMaterials.ensureBlocks();
        LibItems.register();
        com.simplelib.registry.LibMaterials.ensureItems();
        MaterialConditionFabric.register();
        // Valid for every crucible/barrel block, including partner tiers registered later (load order free).
        LibBlockEntities.register(new net.minecraft.world.level.block.entity.BlockEntityType<>(CrucibleBlockEntity::new, java.util.Set.of(LibBlocks.crucibles())) {
            @Override public boolean isValid(net.minecraft.world.level.block.state.BlockState state) { return LibBlockEntities.isCrucible(state); }
        });
        LibBlockEntities.registerBarrel(new net.minecraft.world.level.block.entity.BlockEntityType<>(com.simplelib.crucible.CrucibleBarrelBlockEntity::new, java.util.Set.of(LibBlocks.barrels())) {
            @Override public boolean isValid(net.minecraft.world.level.block.state.BlockState state) { return LibBlockEntities.isBarrel(state); }
        });
        LibMenus.register();
        LibRegistry.tab();
        // Crucibles and barrels also go into Vanilla's functional tab (per-mod switch) and SimpleBuilding's own.
        for (var tab : java.util.List.of(net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS,
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, LibRegistry.SB_FUNCTIONAL_TAB))) {
            net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(tab).register(entries -> {
                if (!LibRegistry.wantsStacks(tab, com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FabricLoader.getInstance().getConfigDir(), "simplelib"))) return;
                LibRegistry.tabStacks().forEach(s -> entries.accept(s, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            });
        }
        // In-world upgrade files: SimpleLib's own load and client sync (idle while a partner such as SimpleBuilding loads them).
        net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(net.minecraft.server.packs.PackType.SERVER_DATA).registerReloadListener(
                SimpleLib.id("in_world_upgrades"), com.simplelib.api.InWorldUpgrades.reloadListener());
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
                com.simplelib.net.UpgradeTablePayload.TYPE, com.simplelib.net.UpgradeTablePayload.CODEC);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
            var payload = com.simplelib.net.UpgradeTablePayload.current();
            if (payload != null && net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, com.simplelib.net.UpgradeTablePayload.TYPE)) {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
            }
        });
        // Enderite in End city treasure once a mod requested it and no partner feeds it (MaterialLoot).
        net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY.register((key, table, source, registries) ->
                com.simplelib.material.MaterialLoot.apply(key, table::withPool, registries));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(com.simplelib.api.InWorldStrikes::tick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server ->
                com.simplelib.village.VillageKitchen.inject(server.registryAccess()));
    }
}
