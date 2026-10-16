package com.simplebuilding.modules.simplelib;

import com.simplelib.SimpleLib;
import com.simplelib.crucible.CrucibleBlockEntity;
import com.simplelib.registry.LibBlockEntities;
import com.simplelib.registry.LibBlocks;
import com.simplelib.registry.LibComponents;
import com.simplelib.registry.LibItems;
import com.simplelib.registry.LibMenus;
import com.simplelib.registry.LibRegistry;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry: Vanilla Registry.register works inside the matching RegisterEvent. */
@Mod(SimpleLib.MOD_ID)
public final class SimpleLibNeoForge {
    public SimpleLibNeoForge(IEventBus bus, Dist dist) {
        SimpleLib.init(FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.DATA_COMPONENT_TYPE)) LibComponents.register();
            if (key.equals(Registries.BLOCK)) {
                LibBlocks.register();
                com.simplelib.registry.LibMaterials.ensureBlocks();
            }
            if (key.equals(Registries.ITEM)) {
                LibItems.register();
                com.simplelib.registry.LibMaterials.ensureItems();
            }
            if (key.equals(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.CONDITION_CODECS)) {
                event.register(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.CONDITION_CODECS, SimpleLib.id("material_requested"),
                        () -> MaterialConditionNeo.CODEC);
            }
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                LibBlockEntities.register(new BlockEntityType<>(CrucibleBlockEntity::new, Set.of(LibBlocks.crucibles()), false) {
                    @Override public boolean isValid(net.minecraft.world.level.block.state.BlockState state) { return LibBlockEntities.isCrucible(state); }
                });
                LibBlockEntities.registerBarrel(new BlockEntityType<>(com.simplelib.crucible.CrucibleBarrelBlockEntity::new, Set.of(LibBlocks.barrels()), false) {
                    @Override public boolean isValid(net.minecraft.world.level.block.state.BlockState state) { return LibBlockEntities.isBarrel(state); }
                });
            }
            if (key.equals(Registries.POTION)) com.simplelib.registry.LibPotions.register();
            if (key.equals(Registries.MENU)) LibMenus.register();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) LibRegistry.tab();
        });
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (!com.simplelib.registry.LibRegistry.wantsStacks(event.getTabKey(),
                    com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), "simplelib"))) return;
            com.simplelib.registry.LibRegistry.tabStacks().forEach(s -> event.accept(s,
                    net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) ->
                com.simplelib.api.InWorldStrikes.tick(event.getServer()));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerAboutToStartEvent event) ->
                com.simplelib.village.VillageKitchen.inject(event.getServer().registryAccess()));
        // In-world upgrade files: SimpleLib's own load and client sync (idle while a partner such as SimpleBuilding loads them).
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.AddServerReloadListenersEvent event) ->
                event.addListener(SimpleLib.id("in_world_upgrades"), com.simplelib.api.InWorldUpgrades.reloadListener()));
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) ->
                event.registrar("1").optional().playToClient(com.simplelib.net.UpgradeTablePayload.TYPE, com.simplelib.net.UpgradeTablePayload.CODEC,
                        (payload, context) -> context.enqueueWork(() -> com.simplelib.api.InWorldUpgrades.receive(payload.files()))));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.OnDatapackSyncEvent event) -> {
            var payload = com.simplelib.net.UpgradeTablePayload.current();
            if (payload == null) return;
            event.getRelevantPlayers().forEach(player -> {
                if (player.connection != null && player.connection.hasChannel(payload.type())) {
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
                }
            });
        });
        // Enderite in End city treasure once a mod requested it and no partner feeds it (MaterialLoot).
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.LootTableLoadEvent event) -> {
            if (event.getName() == null) return;
            com.simplelib.material.MaterialLoot.apply(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE, event.getName()),
                    pool -> event.getTable().addPool(pool.build()), event.getRegistries());
        });
        if (dist == Dist.CLIENT) SimpleLibNeoForgeClient.init(bus);
    }
}
