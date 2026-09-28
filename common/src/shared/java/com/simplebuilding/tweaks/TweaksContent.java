package com.simplebuilding.tweaks;

import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.component.TweaksComponents;
import com.simplebuilding.tweaks.easter.EasterEggs;
import com.simplebuilding.tweaks.easter.EasterSmithingRecipe;
import com.simplebuilding.tweaks.item.TweaksItems;
import com.simplebuilding.tweaks.network.TweaksNetwork;
import com.simplebuilding.tweaks.spawn.LaunchSafety;
import com.simplebuilding.tweaks.spawn.SpawnElytra;
import com.simplebuilding.tweaks.spawn.SpawnSetup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registrierung und Server-Ereignisse des Simple-Tweaks-Teils, loader-neutral. Fabric ruft
 * {@link #init()} in einem Rutsch; NeoForge/Forge rufen die Registrier-Methoden in ihren
 * RegisterEvents (Komponenten, Bloecke, Items, Rezept-Serializer) und die Ereignis-Methoden aus ihren Event-Handlern.
 */
public final class TweaksContent {
    private TweaksContent() {
    }

    public static void init() {
        registerComponents();
        registerBlocks();
        registerItems();
        registerRecipeSerializers();
    }

    public static void registerComponents() {
        TweaksComponents.init();
        EasterEggs.registerComponents();
    }

    public static void registerBlocks() {
        TweaksBlocks.init();
    }

    public static void registerItems() {
        TweaksItems.init();
        EasterEggs.registerItems();
    }

    /** Schmiede-Schritte der versteckten Kette ueber den Endstufen ({@link EasterEggs}). */
    public static void registerRecipeSerializers() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, SimpleTweaks.id("easter_smithing"), EasterSmithingRecipe.SERIALIZER);
    }

    public static void onServerTick(MinecraftServer server) {
        SpawnElytra.serverTick(server);
        LaunchSafety.serverTick(server);
        // Testzentrale: faelliger Neubau einer veralteten Zentrale (nur in der Entwicklungswelt geplant).
        com.simplebuilding.dev.testcentre.TestCentreCommand.serverTick(server);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        TweaksNetwork.sendConfig(player);
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
