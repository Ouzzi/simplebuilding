package com.simplebuilding;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.command.ModCommands;
import com.simplebuilding.common.SimplebuildingBootstrap;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.common.SimplebuildingCommon;
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
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
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
        LOGGER.info("Starting Simplebuilding initialization...");
        LOGGER.info(SimplebuildingBootstrap.initialize("fabric", buildStartupPlan()));
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
        LegacySpatulaMigration.register();
        registerCauldronBehavior();
    }

    private void registerGameplayEvents() {
        PlayerBlockBreakEvents.BEFORE.register(new SledgehammerUsageEvent());
        SledgehammerEntityInteraction.register();
        PlayerBlockBreakEvents.BEFORE.register(new StripMinerUsageEvent());
        PlayerBlockBreakEvents.BEFORE.register(new VeinMinerUsageEvent());
        AttackBlockCallback.EVENT.register(new VersatilityUsageEvent());

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (server.getTicks() % 2 == 0) {
                    DynamicLightHandler.tick(player);
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            DynamicLightHandler.onDisconnect(handler.player);
        });
    }

    private void registerNetworking() {
        ModMessages.registerC2SPackets();
    }

    private void registerFinalBootstrap() {
        ModCommands.register();
        ModOreGeneration.generateOres();
    }

    private void registerCauldronBehavior() {
        // Rangefinder reinigen:
        CauldronBehavior cleanRangefinder = (state, world, pos, player, hand, stack) -> {
            Item item = stack.getItem();
            if (!(item instanceof OctantItem) || item == ModItems.OCTANT) {return ActionResult.PASS;}
            if (!world.isClient()) {
                ItemStack newStack = new ItemStack(ModItems.OCTANT);
                if (stack.contains(DataComponentTypes.CUSTOM_DATA)) {
                    newStack.set(DataComponentTypes.CUSTOM_DATA, stack.get(DataComponentTypes.CUSTOM_DATA));
                }
                player.setStackInHand(hand, newStack);
                player.incrementStat(Stats.CLEAN_ARMOR);
                LeveledCauldronBlock.decrementFluidLevel(state, world, pos);
            }
            return ActionResult.SUCCESS;
        };

        for (DyeColor color : DyeColor.values()) {
            Item coloredItem = ModItems.COLORED_OCTANT_ITEMS.get(color);
            if (coloredItem != null) {
                CauldronBehavior.WATER_CAULDRON_BEHAVIOR.map().put(coloredItem, cleanRangefinder);
            }
        }
    }

    public static SimplebuildingConfig getConfig() {
        return CONFIG;
    }
}