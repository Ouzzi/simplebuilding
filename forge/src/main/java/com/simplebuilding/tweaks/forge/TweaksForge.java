package com.simplebuilding.tweaks.forge;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksContent;
import com.simplebuilding.tweaks.block.entity.TweaksBlockEntities;
import com.simplebuilding.tweaks.command.TweaksCommands;
import com.simplebuilding.tweaks.network.ElytraBoostPayload;
import com.simplebuilding.tweaks.network.LaserPayload;
import com.simplebuilding.tweaks.network.TweaksNetwork;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkProtocol;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Forge-Anbindung des Simple-Tweaks-Teils (Forge ist geparkt, siehe MULTILOADER_TODO.md, laeuft
 * aber mit): Registrierung, eigener Kanal {@code simplebuilding:tweaks}, Befehle, Server-Ereignisse.
 * Keine Config-Persistenz auf Forge (Shim), Befehle aendern die Werte nur bis zum Neustart.
 */
@Mod.EventBusSubscriber(modid = Simplebuilding.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TweaksForge {
    private static Channel<CustomPacketPayload> channel;

    private TweaksForge() {
    }

    /** Aus dem Mod-Konstruktor ({@code SimplebuildingForge}). */
    public static void register(BusGroup modBus) {
        RegisterEvent.getBus(modBus).addListener(TweaksForge::onRegister);
        channel = ChannelBuilder.named(SimpleTweaks.id("tweaks"))
                .payloadChannel()
                .protocol(NetworkProtocol.PLAY)
                .serverbound()
                    .add(ElytraBoostPayload.ID, ElytraBoostPayload.CODEC,
                            (payload, ctx) -> onServer(ctx, player -> TweaksNetwork.handleBoost(payload, player)))
                    .add(LaserPayload.ID, LaserPayload.CODEC,
                            (payload, ctx) -> onServer(ctx, player -> TweaksNetwork.handleLaser(payload, player)))
                .clientbound()
                    .add(LaserPayload.ID, LaserPayload.CODEC, (payload, ctx) -> {
                        ctx.setPacketHandled(true);
                        ctx.enqueueWork(() -> TweaksNetwork.receiveLaser(payload));
                    })
                .build();
        TweaksNetwork.setSenders(
                (player, payload) -> channel.send(payload, PacketDistributor.PLAYER.with(player)),
                payload -> channel.send(payload, PacketDistributor.SERVER.noArg()));
    }

    private static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.DATA_COMPONENT_TYPE)) {
            TweaksContent.registerComponents();
        } else if (event.getRegistryKey().equals(Registries.BLOCK)) {
            TweaksContent.registerBlocks();
        } else if (event.getRegistryKey().equals(Registries.ITEM)) {
            TweaksContent.registerItems();
        } else if (event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) {
            TweaksBlockEntities.register(new TweaksBlockEntities.Factory() {
                @Override
                public <T extends BlockEntity> BlockEntityType<T> create(BlockEntityType.BlockEntitySupplier<T> supplier, Block... blocks) {
                    return new BlockEntityType<>(supplier, Set.of(blocks));
                }
            });
        }
    }

    private static void onServer(CustomPayloadEvent.Context ctx, Consumer<ServerPlayer> action) {
        ctx.setPacketHandled(true);
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                action.accept(player);
            }
        });
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        TweaksCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        TweaksContent.onServerTick(event.server());
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
