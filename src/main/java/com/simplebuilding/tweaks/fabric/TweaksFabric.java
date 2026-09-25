package com.simplebuilding.tweaks.fabric;

import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksContent;
import com.simplebuilding.tweaks.block.entity.TweaksBlockEntities;
import com.simplebuilding.tweaks.command.TweaksCommands;
import com.simplebuilding.tweaks.network.ElytraBoostPayload;
import com.simplebuilding.tweaks.network.LaserPayload;
import com.simplebuilding.tweaks.network.TweaksNetwork;
import java.util.Set;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Fabric-Anbindung des Simple-Tweaks-Teils. Eigener main-Entrypoint hinter
 * {@code com.simplebuilding.Simplebuilding} (Reihenfolge in fabric.mod.json), damit die Config
 * schon geladen ist.
 */
public class TweaksFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SimpleTweaks.setConfigSaver(() -> AutoConfig.getConfigHolder(SimplebuildingConfig.class).save());
        TweaksContent.init();
        TweaksBlockEntities.register(new TweaksBlockEntities.Factory() {
            @Override
            public <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> create(
                    BlockEntityType.BlockEntitySupplier<T> supplier, net.minecraft.world.level.block.Block... blocks) {
                return new BlockEntityType<>(supplier, Set.of(blocks));
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(TweaksContent::onServerTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> TweaksContent.onPlayerJoin(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> TweaksContent.onPlayerRespawn(newPlayer));
        ServerLevelEvents.LOAD.register((server, level) -> TweaksContent.onLevelLoad(level));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> TweaksCommands.register(dispatcher));

        PayloadTypeRegistry.serverboundPlay().register(ElytraBoostPayload.ID, ElytraBoostPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LaserPayload.ID, LaserPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LaserPayload.ID, LaserPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ElytraBoostPayload.ID,
                (payload, context) -> context.server().execute(() -> TweaksNetwork.handleBoost(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(LaserPayload.ID,
                (payload, context) -> context.server().execute(() -> TweaksNetwork.handleLaser(payload, context.player())));
    }
}
