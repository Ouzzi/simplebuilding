package com.simplebuilding.tweaks;

import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.component.TweaksComponents;
import com.simplebuilding.tweaks.item.TweaksItems;
import com.simplebuilding.tweaks.spawn.LaunchSafety;
import com.simplebuilding.tweaks.spawn.SpawnElytra;
import com.simplebuilding.tweaks.spawn.SpawnSetup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registrierung und Server-Ereignisse des Simple-Tweaks-Teils, loader-neutral. Fabric ruft
 * {@link #init()} in einem Rutsch; NeoForge/Forge rufen die drei Registrier-Methoden in ihren
 * RegisterEvents (Komponenten, Bloecke, Items) und die Ereignis-Methoden aus ihren Event-Handlern.
 */
public final class TweaksContent {
    private TweaksContent() {
    }

    public static void init() {
        registerComponents();
        registerBlocks();
        registerItems();
    }

    public static void registerComponents() {
        TweaksComponents.init();
    }

    public static void registerBlocks() {
        TweaksBlocks.init();
    }

    public static void registerItems() {
        TweaksItems.init();
    }

    public static void onServerTick(MinecraftServer server) {
        SpawnElytra.serverTick(server);
        LaunchSafety.serverTick(server);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        SpawnSetup.onPlayerJoin(player);
        SpawnElytra.onJoinOrRespawn(player);
    }

    public static void onPlayerRespawn(ServerPlayer player) {
        SpawnElytra.onJoinOrRespawn(player);
    }

    public static void onLevelLoad(ServerLevel level) {
        SpawnSetup.onLevelLoad(level);
    }
}
