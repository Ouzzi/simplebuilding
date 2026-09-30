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
        registerTriggers();
        registerStats();
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

    /**
     * Schmiede-Schritte der versteckten Kette ueber den Endstufen ({@link EasterEggs}) und das formlose Rezept mit
     * verlangter Verzauberung (Flypad I: Elytra mit Reparatur, {@link com.simplebuilding.recipe.EnchantedShapelessRecipe}).
     */
    public static void registerRecipeSerializers() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, SimpleTweaks.id("guide_upgrade"), com.simplebuilding.recipe.GuideUpgradeRecipe.SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, SimpleTweaks.id("easter_smithing"), EasterSmithingRecipe.SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, SimpleTweaks.id("enchanted_shapeless"),
                com.simplebuilding.recipe.EnchantedShapelessRecipe.SERIALIZER);
    }

    /**
     * Advancement-Trigger der Mod ({@link com.simplebuilding.advancement.ModTriggers}); haengt hier,
     * weil dies die loader-neutrale Registrier-Stelle ist, die alle Loader schon rufen.
     */
    public static void registerTriggers() {
        com.simplebuilding.advancement.ModTriggers.register();
    }

    /** Player statistics ({@link com.simplebuilding.stats.ModStats}); NeoForge/Forge: RegisterEvent for CUSTOM_STAT. */
    public static void registerStats() {
        com.simplebuilding.stats.ModStats.register();
    }

    public static void onServerTick(MinecraftServer server) {
        SpawnElytra.serverTick(server);
        // Chunk-Loader wecken bzw. anhalten (Besitzer kommt/geht, Schalter), siehe ChunkLoaderRegistry.
        if (server.getTickCount() % com.simplebuilding.tweaks.block.entity.ChunkLoaderBlockEntity.CHECK_INTERVAL == 0) {
            com.simplebuilding.tweaks.block.entity.ChunkLoaderRegistry.reconcile(server);
        }
        LaunchSafety.serverTick(server);
        // Testzentrale: faelliger Neubau einer veralteten Zentrale (nur in der Entwicklungswelt geplant).
        com.simplebuilding.dev.testcentre.TestCentreCommand.serverTick(server);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        TweaksNetwork.sendConfig(player);
        SpawnSetup.onPlayerJoin(player);
        // Einsteiger-Handbuch: einmal je Spieler, Config giveGuideBookOnFirstJoin (kein Tweak, aber
        // dies ist der gemeinsame Beitritts-Einstieg aller Loader).
        com.simplebuilding.guide.GuideBooks.onPlayerJoin(player);
        SpawnElytra.onJoinOrRespawn(player);
    }

    public static void onPlayerRespawn(ServerPlayer player) {
        SpawnElytra.onJoinOrRespawn(player);
    }

    public static void onLevelLoad(ServerLevel level) {
        SpawnSetup.onLevelLoad(level);
    }
}
