package com.simplebuilding.modules.simplelib;

import com.simplelib.SimpleLib;
import com.simplelib.registry.LibComponents;
import net.minecraft.core.registries.Registries;
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
            if (key.equals(Registries.BLOCK)) com.simplelib.registry.LibMaterials.ensureBlocks();
            if (key.equals(Registries.ITEM)) com.simplelib.registry.LibMaterials.ensureItems();
            if (key.equals(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.CONDITION_CODECS)) {
                event.register(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.CONDITION_CODECS, SimpleLib.id("material_requested"),
                        () -> MaterialConditionNeo.CODEC);
            }
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) ->
                com.simplelib.api.InWorldStrikes.tick(event.getServer()));
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
