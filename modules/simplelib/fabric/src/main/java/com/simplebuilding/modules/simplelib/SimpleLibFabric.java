package com.simplebuilding.modules.simplelib;

import com.simplelib.SimpleLib;
import com.simplelib.registry.LibComponents;
import com.simplelib.registry.LibContent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class SimpleLibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // simplelib-3: features a mod must ask for (free look's default key, legendary moments) - before any client entrypoint.
        LibFeaturesFabric.decide();
        SimpleLib.init(FabricLoader.getInstance().getConfigDir());
        // VANILLA36: the content (blocks, items, components, potion, sound) only while an installed mod asks for it, on a
        // client or with the server switch alwaysRegisterContent - otherwise Vanilla clients may join (LibContent).
        LibContent.decide(com.simplelib.config.LibConfig.alwaysRegisterContent,
                FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT, requesters());
        if (LibContent.enabled()) registerContent();
        // b-guide01: the chapter unlock query of the Simple Modpack guide, and the FTB Quests start chapter when SimpleBuilding
        // is absent (SimpleBuilding's own first quest gives the guide).
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(
                com.simplelib.net.ModpackGuideQueryPayload.TYPE, com.simplelib.net.ModpackGuideQueryPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
                com.simplelib.net.ModpackGuideStatePayload.TYPE, com.simplelib.net.ModpackGuideStatePayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(com.simplelib.net.ModpackGuideQueryPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    var player = context.player();
                    if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, com.simplelib.net.ModpackGuideStatePayload.TYPE)) {
                        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new com.simplelib.net.ModpackGuideStatePayload(
                                com.simplelib.api.ModpackGuide.done(player, payload.advancements())));
                    }
                }));
        var loader = FabricLoader.getInstance();
        if (LibContent.enabled() && loader.isModLoaded("ftbquests") && !loader.isModLoaded("simplebuilding")) {
            com.simplelib.guide.ModpackGuideQuests.install(loader.getConfigDir());
        }
        MaterialConditionFabric.register();
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
        // IG31-29: the server's free look rules on join and after every save of simplelib-server.json.
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
                com.simplelib.net.FreeLookRulesPayload.TYPE, com.simplelib.net.FreeLookRulesPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendFreeLookRules(handler.getPlayer()));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server -> com.simplelib.config.LibConfig.onSaved = () ->
                server.execute(() -> server.getPlayerList().getPlayers().forEach(SimpleLibFabric::sendFreeLookRules)));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> com.simplelib.config.LibConfig.onSaved = () -> {});
        // Warped wart in bastion chests (MaterialLoot); the Enderite loot moved to Simple Ores (IG31-13).
        net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY.register((key, table, source, registries) ->
                com.simplelib.material.MaterialLoot.apply(key, table::withPool, registries));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(com.simplelib.api.InWorldStrikes::tick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(com.simplelib.api.LegendaryMoments::tick);
        // Carried-light blocks that outlived their carrier (crash): swept after their chunk loaded (OwnedLights). BS27-1: never
        // inside CHUNK_LOAD - touching blocks of the loading chunk there deadlocks the server thread.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> com.simplelib.api.OwnedLights.chunkLoaded(level, chunk.getPos()));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(com.simplelib.api.OwnedLights::tick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> com.simplelib.api.OwnedLights.stopped());
        // BS27-1 diagnosis: a stalled server tick (also while saving on quit) writes a thread dump to logs/simplebuilding-hang-*.txt.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(com.simplelib.api.HangWatchdog::start);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> com.simplelib.api.HangWatchdog.beat());
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> com.simplelib.api.HangWatchdog.stopping());
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> com.simplelib.api.HangWatchdog.stop());
        // Per-player static state of the modules (PlayerState): dropped on disconnect and server stop.
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> com.simplelib.api.PlayerState.left(handler.getPlayer()));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> com.simplelib.api.PlayerState.stopped());
        // BACK22 core: portal doors (passage, preloading the other side, fog pulse) and the tracking filters' providers.
        com.simplelib.api.portal.PortalPreload.register();
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
                com.simplelib.net.PortalPulsePayload.TYPE, com.simplelib.net.PortalPulsePayload.CODEC);
        com.simplelib.api.portal.PortalTransit.pulseSender = (player, payload) -> {
            if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, com.simplelib.net.PortalPulsePayload.TYPE)) {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
            }
        };
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(com.simplelib.api.portal.PortalTransit::tick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(com.simplelib.api.portal.PortalPreload::tick);
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                com.simplelib.api.portal.PortalTransit.forget(handler.getPlayer().getUUID()));
        // CMD24: /<modid> config for every announced config, in a late phase so existing /<modid> roots keep their checks.
        var configPhase = SimpleLib.id("config_commands");
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.addPhaseOrdering(net.fabricmc.fabric.api.event.Event.DEFAULT_PHASE, configPhase);
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(configPhase, (dispatcher, registry, environment) ->
                com.simplelib.api.config.ConfigCommands.registerAll(dispatcher, false,
                        net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS),
                        com.simplelib.api.config.ConfigCommands.SERVER_IO));
    }

    /** Everything that puts SimpleLib entries into synced registries, plus their creative tab entries (VANILLA36: only with content). */
    private static void registerContent() {
        LibComponents.register();
        com.simplelib.registry.LibBlocks.register();
        com.simplelib.registry.LibMaterials.ensureBlocks();
        com.simplelib.registry.LibMaterials.ensureItems();
        com.simplelib.registry.LibItems.register();
        com.simplelib.api.ModTabs.register(SimpleLib.MOD_ID, "lib", "itemgroup.simplelib.lib", () -> new net.minecraft.world.item.ItemStack(com.simplelib.registry.LibItems.WARPED_WART), com.simplelib.api.ModpackGuide.ITEM_ID.getPath());
        com.simplelib.registry.LibPotions.register();
        com.simplelib.registry.LibSounds.register();
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.INGREDIENTS).register(entries -> {
            entries.accept(new net.minecraft.world.item.ItemStack(com.simplelib.registry.LibItems.WARPED_WART));
            // WF25-10: the Strong Upgrade (template, shard) while a mod requested it; the tab rules move both into the Simple Lib tab.
            if (com.simplelib.registry.LibMaterials.requested(com.simplelib.material.StrongUpgradeDrops.GROUP)) {
                entries.accept(new net.minecraft.world.item.ItemStack(com.simplelib.registry.LibMaterials.item("strong_upgrade_template")));
                entries.accept(new net.minecraft.world.item.ItemStack(com.simplelib.registry.LibMaterials.item("strong_upgrade_shard")));
            }
        });
        // N48: the material pressure plates in the Redstone tab after the heavy weighted plate, while a mod requested the group.
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> {
            if (!com.simplelib.registry.LibMaterials.requested("material_pressure_plates")) return;
            var plates = com.simplelib.registry.LibMaterials.PLATES.stream()
                    .map(id -> new net.minecraft.world.item.ItemStack(com.simplelib.registry.LibMaterials.item(id))).toList();
            if (entries.getDisplayStacks().stream().anyMatch(s -> s.is(net.minecraft.world.item.Items.HEAVY_WEIGHTED_PRESSURE_PLATE))) {
                entries.insertAfter(net.minecraft.world.item.Items.HEAVY_WEIGHTED_PRESSURE_PLATE, plates,
                        net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            } else {
                plates.forEach(s -> entries.accept(s, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
        });
        // b-guide01: the Simple Modpack guide in the Vanilla Tools tab (SimpleBuilding shows it in its own tab instead, and the
        // tab rules then drop it here).
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries ->
                entries.accept(new net.minecraft.world.item.ItemStack(com.simplelib.registry.LibItems.MODPACK_GUIDE)));
    }

    /** Loaded mods that ask for SimpleLib's content ({@link LibContent#requests}: the custom key, else dependency or a "simple" id). */
    private static java.util.List<String> requesters() {
        var out = new java.util.ArrayList<String>();
        for (var mod : FabricLoader.getInstance().getAllMods()) {
            var meta = mod.getMetadata();
            java.util.List<String> declared = null;
            var custom = meta.getCustomValue(LibContent.KEY);
            if (custom != null) {
                declared = new java.util.ArrayList<>();
                if (custom.getType() == net.fabricmc.loader.api.metadata.CustomValue.CvType.ARRAY) {
                    for (var v : custom.getAsArray()) declared.add(v.getType() == net.fabricmc.loader.api.metadata.CustomValue.CvType.STRING ? v.getAsString() : "?");
                } else {
                    declared.add("?"); // not a list: counts as a request, never as "needs nothing"
                }
            }
            boolean depends = meta.getDependencies().stream().anyMatch(d -> SimpleLib.MOD_ID.equals(d.getModId())
                    && d.getKind() != net.fabricmc.loader.api.metadata.ModDependency.Kind.BREAKS
                    && d.getKind() != net.fabricmc.loader.api.metadata.ModDependency.Kind.CONFLICTS);
            if (LibContent.requests(meta.getId(), depends, declared)) out.add(meta.getId());
        }
        out.sort(null);
        return out;
    }

    private static void sendFreeLookRules(net.minecraft.server.level.ServerPlayer player) {
        if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, com.simplelib.net.FreeLookRulesPayload.TYPE)) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, com.simplelib.net.FreeLookRulesPayload.current());
        }
    }
}
