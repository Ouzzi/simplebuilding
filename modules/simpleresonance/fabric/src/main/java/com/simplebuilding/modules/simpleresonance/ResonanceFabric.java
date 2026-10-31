package com.simplebuilding.modules.simpleresonance;

import com.simpleresonance.ResonanceCommand;
import com.simpleresonance.ResonanceService;
import com.simpleresonance.SimpleResonance;
import com.simpleresonance.config.ResonanceConfig;
import com.simpleresonance.guide.ResonanceGuide;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;

/**
 * Fabric entry (WP-22): config, the service for the framework contract, the events that feed it (ticks, kills, deaths, respawns),
 * {@code /resonance}, the guide book. All gameplay rules live in shared code.
 */
public final class ResonanceFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        var configDir = FabricLoader.getInstance().getConfigDir();
        SimpleResonance.init(configDir);
        // CMD24: /simpleresonance config for every option.
        com.simplelib.api.config.ConfigCommands.server(SimpleResonance.MOD_ID).json(ResonanceConfig::toJson, ResonanceConfig::apply).file(configDir, ResonanceConfig.FILE).register();
        ResonanceGuide.register();
        ResonanceService service = ResonanceService.INSTANCE;
        ServerLifecycleEvents.SERVER_STARTED.register(service::serverStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            service.serverStopped();
            com.simpleresonance.LegacyImport.clear();
        });
        ServerTickEvents.END_SERVER_TICK.register(service::tick);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer dead) {
                service.onDeath(dead);
            } else if (source.getEntity() instanceof ServerPlayer killer) {
                service.onKill(killer, entity);
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> service.onRespawn(newPlayer, alive));
        ServerPlayerEvents.LEAVE.register(player -> service.forget(player.getUUID()));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ResonanceCommand.register(dispatcher));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleResonance.on()) return;
            var book = java.util.List.of(ResonanceGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) ResonanceGuide.installQuests(configDir);
    }
}
