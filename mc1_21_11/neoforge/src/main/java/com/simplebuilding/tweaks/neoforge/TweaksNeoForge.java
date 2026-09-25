package com.simplebuilding.tweaks.neoforge;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksContent;
import com.simplebuilding.tweaks.block.entity.TweaksBlockEntities;
import com.simplebuilding.tweaks.command.TweaksCommands;
import com.simplebuilding.tweaks.network.ElytraBoostPayload;
import com.simplebuilding.tweaks.network.LaserPayload;
import com.simplebuilding.tweaks.network.TweaksNetwork;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * NeoForge-Anbindung des Simple-Tweaks-Teils: Registrierung in den RegisterEvents, Pakete,
 * Befehle und Server-Ereignisse. Eigene Klasse, damit die bestehenden NeoForge-Registrierungen
 * unberuehrt bleiben.
 */
@EventBusSubscriber(modid = Simplebuilding.MOD_ID)
public final class TweaksNeoForge {
    private TweaksNeoForge() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.DATA_COMPONENT_TYPE)) {
            TweaksContent.registerComponents();
        } else if (event.getRegistryKey().equals(Registries.BLOCK)) {
            TweaksContent.registerBlocks();
        } else if (event.getRegistryKey().equals(Registries.ITEM)) {
            TweaksContent.registerItems();
        } else if (event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) {
            TweaksBlockEntities.register(new TweaksBlockEntities.Factory() {
                @Override
                public <T extends BlockEntity> BlockEntityType<T> create(TweaksBlockEntities.Supplier<T> supplier, Block... blocks) {
                    return new BlockEntityType<>(supplier::create, blocks);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        SimpleTweaks.setConfigSaver(() -> AutoConfig.getConfigHolder(SimplebuildingConfig.class).save());
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(ElytraBoostPayload.ID, ElytraBoostPayload.CODEC,
                (payload, context) -> onServer(context, player -> TweaksNetwork.handleBoost(payload, player)));
        registrar.playBidirectional(LaserPayload.ID, LaserPayload.CODEC, (payload, context) -> {
            if (context.flow().isServerbound()) {
                onServer(context, player -> TweaksNetwork.handleLaser(payload, player));
            } else {
                context.enqueueWork(() -> TweaksNetwork.receiveLaser(payload));
            }
        });
    }

    private static void onServer(IPayloadContext context, java.util.function.Consumer<ServerPlayer> action) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                action.accept(player);
            }
        });
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        TweaksCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        TweaksContent.onServerTick(event.getServer());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TweaksContent.onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TweaksContent.onPlayerRespawn(player);
        }
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TweaksContent.onLevelLoad(level);
        }
    }
}
