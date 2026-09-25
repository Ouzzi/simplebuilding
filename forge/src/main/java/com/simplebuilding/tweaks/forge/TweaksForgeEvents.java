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

/** Spiel-Ereignisse des Simple-Tweaks-Teils auf Forge (getrennt von TweaksForge: Forges EventBus 7 verlangt in Abonnenten-Klassen an jeder Ereignis-Methode @SubscribeEvent). */
@Mod.EventBusSubscriber(modid = Simplebuilding.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TweaksForgeEvents {
    private TweaksForgeEvents() {
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
