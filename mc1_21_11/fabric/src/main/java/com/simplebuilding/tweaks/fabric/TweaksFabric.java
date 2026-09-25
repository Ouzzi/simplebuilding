package com.simplebuilding.tweaks.fabric;

import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksContent;
import com.simplebuilding.tweaks.block.entity.TweaksBlockEntities;
import com.simplebuilding.tweaks.command.TweaksCommands;
import com.simplebuilding.tweaks.network.ElytraBoostPayload;
import com.simplebuilding.tweaks.network.LaserPayload;
import com.simplebuilding.tweaks.network.TweaksNetwork;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Fabric-Anbindung des Simple-Tweaks-Teils, MC 1.21.11 (Gegenstueck zu src/main/.../TweaksFabric). */
public class TweaksFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        SimpleTweaks.setConfigSaver(() -> AutoConfig.getConfigHolder(SimplebuildingConfig.class).save());
        TweaksContent.init();
        // Der BlockEntityType-Konstruktor ist auf 1.21.11 privat: Fabrics Builder.
        TweaksBlockEntities.register(new TweaksBlockEntities.Factory() {
            @Override
            public <T extends BlockEntity> BlockEntityType<T> create(TweaksBlockEntities.Supplier<T> supplier, Block... blocks) {
                return FabricBlockEntityTypeBuilder.create(supplier::create, blocks).build();
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(TweaksContent::onServerTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> TweaksContent.onPlayerJoin(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> TweaksContent.onPlayerRespawn(newPlayer));
        ServerWorldEvents.LOAD.register((server, level) -> TweaksContent.onLevelLoad(level));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> TweaksCommands.register(dispatcher));

        PayloadTypeRegistry.playC2S().register(ElytraBoostPayload.ID, ElytraBoostPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(LaserPayload.ID, LaserPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LaserPayload.ID, LaserPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ElytraBoostPayload.ID,
                (payload, context) -> context.server().execute(() -> TweaksNetwork.handleBoost(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(LaserPayload.ID,
                (payload, context) -> context.server().execute(() -> TweaksNetwork.handleLaser(payload, context.player())));
    }
}
