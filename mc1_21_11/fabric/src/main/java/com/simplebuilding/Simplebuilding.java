package com.simplebuilding;

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
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.networking.ModMessages;
import com.simplebuilding.recipe.ModRecipes;
import com.simplebuilding.screen.ModScreenHandlers;
import com.simplebuilding.util.*;
import com.simplebuilding.world.gen.ModOreGeneration;
import com.simplebuilding.platform.ModEnvironment;
import com.simplebuilding.platform.PlatformServices;
import com.simplebuilding.platform.PlayerPacketSender;
import com.simplebuilding.networking.SyncHopperGhostItemPayload;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LayeredCauldronBlock;
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
        ModItems.registerModItems();
        ModBlockEntities.registerBlockEntities();
        ModLootTableProvider.modifyLootTables();
        ModTradeOffers.registerModTradeOffers();
        ModDataComponentTypes.registerDataComponentTypes();
        ModEnchantmentEffects.registerEnchantmentEffects();
        ModRecipes.registerRecipes();
        ModRegistries.registerModStuffs();
        ServerLifecycleEvents.SERVER_STARTED.register(LegacySpatulaMigration::migrateWorlds);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> LegacySpatulaMigration.migratePlayer(handler.player));
        registerCauldronBehavior();
    }

    private void registerGameplayEvents() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                SledgehammerUsageEvent.handleBeforeBlockBreak(world, player, pos, state, blockEntity));
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                SledgehammerEntityInteraction.handleAttackEntity(player, world, hand, entity));
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                StripMinerUsageEvent.handleBeforeBlockBreak(world, player, pos, state, blockEntity));
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                VeinMinerUsageEvent.handleBeforeBlockBreak(world, player, pos, state, blockEntity));
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
                VersatilityUsageEvent.handleAttackBlock(player, world, hand, pos, direction));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (server.getTickCount() % 2 == 0) {
                    DynamicLightHandler.tick(player);
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            DynamicLightHandler.onDisconnect(handler.player);
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
        PlatformServices.setHopperSync((blockEntity, slot, stack) -> net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(blockEntity)
                .forEach(player -> net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                        player, new SyncHopperGhostItemPayload(blockEntity.getBlockPos(), slot, stack))));
        ModMessages.registerC2SPackets();
    }

    private void registerFinalBootstrap() {
        ModCommands.register();
        ModOreGeneration.generateOres();
    }

    private void registerCauldronBehavior() {
        // Rangefinder reinigen:
        CauldronInteraction cleanRangefinder = (state, world, pos, player, hand, stack) -> {
            Item item = stack.getItem();
            if (!(item instanceof OctantItem) || item == ModItems.OCTANT) {return InteractionResult.PASS;}
            if (!world.isClientSide()) {
                ItemStack newStack = new ItemStack(ModItems.OCTANT);
                if (stack.has(DataComponents.CUSTOM_DATA)) {
                    newStack.set(DataComponents.CUSTOM_DATA, stack.get(DataComponents.CUSTOM_DATA));
                }
                player.setItemInHand(hand, newStack);
                player.awardStat(Stats.CLEAN_ARMOR);
                LayeredCauldronBlock.lowerFillLevel(state, world, pos);
            }
            return InteractionResult.SUCCESS;
        };

        for (DyeColor color : DyeColor.values()) {
            Item coloredItem = ModItems.COLORED_OCTANT_ITEMS.get(color);
            if (coloredItem != null) {
                // MC 1.21.11: CauldronInteraction.WATER ist eine InteractionMap mit oeffentlich
                // zugaenglicher, veraenderbarer map() -- der Dispatcher-Accessor-Mixin aus 26.2 entfaellt.
                CauldronInteraction.WATER.map().put(coloredItem, cleanRangefinder);
            }
        }
    }

    public static SimplebuildingConfig getConfig() {
        return CONFIG;
    }
}