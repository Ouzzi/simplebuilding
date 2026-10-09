package com.simplebuilding;

import com.simplebuilding.util.OctantCauldronWash;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.command.ModCommands;
import com.simplebuilding.common.SimplebuildingBootstrap;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.common.SimplebuildingCommon;
import com.simplebuilding.common.SimplebuildingLoader;
import com.simplebuilding.common.SimplebuildingStartupPlan;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.datagen.ModLootTableProvider;
import com.simplebuilding.datagen.ModTradeOffers;
import com.simplebuilding.enchantment.ModEnchantmentEffects;
import com.simplebuilding.items.ModItemGroups;
import com.simplebuilding.entity.ModEntities;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.networking.ModMessages;
import com.simplebuilding.recipe.ModRecipes;
import com.simplebuilding.screen.ModScreenHandlers;
import com.simplebuilding.util.*;
import com.simplebuilding.world.gen.ModOreGeneration;
import com.simplebuilding.platform.ModEnvironment;
import com.simplebuilding.platform.PlatformServices;
import com.simplebuilding.platform.PlayerPacketSender;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.simplebuilding.mixin.CauldronInteractionDispatcherAccessor;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Die Hauptklasse für den Simplebuilding Mod.
 */
public class Simplebuilding implements ModInitializer {
    public static final String MOD_ID = SimplebuildingCommon.MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static SimplebuildingConfig CONFIG;

    @Override
    public void onInitialize() {
        ModEnvironment.setModLoadedCheck(modId -> FabricLoader.getInstance().isModLoaded(modId));
        ModEnvironment.setDevelopmentEnvironment(FabricLoader.getInstance().isDevelopmentEnvironment());
        // FTB Quests (optional): copy the SimpleBuilding chapters into its quest book once.
        com.simplebuilding.compat.FtbQuestsDefaults.installIfPresent(FabricLoader.getInstance().getConfigDir());
        // Trinkets (optional): backpacks and quivers in the back/belt accessory slots count as worn.
        // The literal id keeps TrinketsCompat (and the Trinkets API behind it) unloaded without Trinkets.
        if (FabricLoader.getInstance().isModLoaded("trinkets_updated")) {
            com.simplebuilding.compat.TrinketsCompat.register();
        }
        LOGGER.info("Starting Simplebuilding initialization...");
        LOGGER.info(SimplebuildingBootstrap.initialize(SimplebuildingLoader.FABRIC, buildStartupPlan()));
    }

    private SimplebuildingStartupPlan buildStartupPlan() {
        return SimplebuildingStartupPlan.builder()
            .configure(this::configure)
            .registerContent(this::registerContent)
            .registerEvents(this::registerGameplayEvents)
            .registerNetworking(this::registerNetworking)
            .finalizeBootstrap(this::registerFinalBootstrap)
            .build();
    }

    private void configure() {
        AutoConfig.register(SimplebuildingConfig.class, GsonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(SimplebuildingConfig.class).getConfig();
    }

    private void registerContent() {
        ModScreenHandlers.registerScreenHandlers();
        ModItemGroups.registerItemGroups();
        ModBlocks.registerModBlocks();
        com.simplebuilding.util.ModSounds.registerSounds();
        ModItems.registerModItems();
        ModEntities.registerModEntities();
        com.simplebuilding.effect.ModEffects.registerEffects();
        com.simplebuilding.effect.ModEffects.registerPotions();
        if (com.simplebuilding.version.McVersion.TRAINING_DUMMY) {
            net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(ModEntities.STRAW_ARMOR_STAND,
                    com.simplebuilding.dummy.TrainingDummy.createAttributes());
            net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(ModEntities.TRAINING_DUMMY,
                    com.simplebuilding.dummy.TrainingDummy.createAttributes());
        }
        ModBlockEntities.registerBlockEntities();
        ModLootTableProvider.modifyLootTables();
        ModTradeOffers.registerModTradeOffers();
        ModDataComponentTypes.registerDataComponentTypes();
        ModEnchantmentEffects.registerEnchantmentEffects();
        ModRecipes.registerRecipes();
        ModRegistries.registerModStuffs();
        ServerLifecycleEvents.SERVER_STARTED.register(LegacySpatulaMigration::migrateWorlds);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> LegacySpatulaMigration.migratePlayer(handler.player));
        // Testzentrale: eine Welt namens SB-Testzentrale baut sich in der Entwicklungsumgebung beim ersten Betreten selbst.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> com.simplebuilding.dev.testcentre.TestCentreCommand.onPlayerJoin(
                handler.player, com.simplebuilding.platform.ModEnvironment.isDevelopmentEnvironment()));
        registerCauldronBehavior();
    }

    private void registerGameplayEvents() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                SledgehammerUsageEvent.handleBeforeBlockBreak(world, player, pos, state, blockEntity));
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                StripMinerUsageEvent.handleBeforeBlockBreak(world, player, pos, state, blockEntity));
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                VeinMinerUsageEvent.handleBeforeBlockBreak(world, player, pos, state, blockEntity));
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
                VersatilityUsageEvent.handleAttackBlock(player, world, hand, pos, direction));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            com.simplebuilding.util.SledgehammerProgress.tick(server);
            com.simplebuilding.items.custom.BuildingCoreItem.tickAnimations(server);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (server.getTickCount() % 2 == 0) {
                    DynamicLightHandler.tick(player);
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            DynamicLightHandler.onDisconnect(handler.player);
            com.simplebuilding.util.SledgehammerUpgrades.onDisconnect(handler.player);
            com.simplebuilding.tweaks.item.EchoCompassItem.onDisconnect(handler.player);
        });
    }

    private void registerNetworking() {
        PlatformServices.setPlayerPacketSender(new PlayerPacketSender() {
            @Override
            public boolean canSend(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<?> type) {
                return net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, type);
            }

            @Override
            public void send(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
            }
        });
        PlatformServices.setItemAutomation(new com.simplebuilding.platform.FabricItemAutomation());
        com.simplebuilding.platform.FabricPistonBreakGuard.install();
        com.simplebuilding.platform.FabricBuildGuard.install();
        // Datapack tables (chisel transformations, sledgehammer upgrades): load on every datapack
        // (re)load, send to each client on join and after /reload.
        com.simplebuilding.platform.FabricDataTables.register();
        ModMessages.registerC2SPackets();
    }

    private void registerFinalBootstrap() {
        ModCommands.register();
        ModOreGeneration.generateOres();
    }

    private void registerCauldronBehavior() {
        // Gefaerbte Oktanten im Wasserkessel waschen - Interaktion und Liste in OctantCauldronWash,
        // aus derselben Quelle lesen Wiki und JEI.
        for (Item coloredItem : OctantCauldronWash.washableOctants()) {
            ((CauldronInteractionDispatcherAccessor) (Object) CauldronInteractions.WATER).simplebuilding$put(coloredItem, OctantCauldronWash.INTERACTION);
        }
        // Gefaerbte gestufte Shulkerkisten waschen wie Vanillas; Werfer stellen sie auf wie Vanillas.
        for (Item box : com.simplebuilding.util.TieredShulkerBoxes.items()) {
            ((CauldronInteractionDispatcherAccessor) (Object) CauldronInteractions.WATER).simplebuilding$put(box, com.simplebuilding.util.TieredShulkerBoxes.WASH);
        }
        com.simplebuilding.util.TieredShulkerBoxes.registerDispenserBehavior();
    }

    public static SimplebuildingConfig getConfig() {
        return CONFIG;
    }
}