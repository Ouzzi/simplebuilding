package com.simplebuilding.forge;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.common.SimplebuildingBootstrap;
import com.simplebuilding.common.SimplebuildingLoader;
import com.simplebuilding.common.SimplebuildingStartupPlan;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.datagen.ModLootTableProvider;
import com.simplebuilding.datagen.ModTradeOffers;
import com.simplebuilding.enchantment.ModEnchantmentEffects;
import com.simplebuilding.items.ModItemGroups;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.mixin.CauldronInteractionDispatcherAccessor;
import com.simplebuilding.platform.ModEnvironment;
import com.simplebuilding.recipe.ModRecipes;
import com.simplebuilding.screen.ModScreenHandlers;
import com.simplebuilding.util.ModRegistries;
import com.simplebuilding.forge.networking.ForgeNetworkRegistration;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.RegisterEvent;

@Mod(Simplebuilding.MOD_ID)
public final class SimplebuildingForge {
    public SimplebuildingForge(ModContainer modContainer) {
        BusGroup modBus = modContainer.getModBusGroup();
        ModEnvironment.setModLoadedCheck(ModList::isLoaded);
        ForgeModRegistries.register(modBus);
        RegisterEvent.getBus(modBus).addListener(ForgeRegistryBootstrap::onRegister);
        FMLCommonSetupEvent.getBus(modBus).addListener(this::commonSetup);
        ForgeNetworkRegistration.register();
        configure();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ForgeModRegistries.assignStaticFields();
            Simplebuilding.LOGGER.info(SimplebuildingBootstrap.initialize(SimplebuildingLoader.FORGE, buildStartupPlan()));
        });
    }

    private SimplebuildingStartupPlan buildStartupPlan() {
        return SimplebuildingStartupPlan.builder()
                .registerContent(this::registerContent)
                .registerEvents(() -> {})
                .registerNetworking(() -> {})
                .finalizeBootstrap(() -> {})
                .build();
    }

    private void configure() {
        // cloth-config/AutoConfig has no MinecraftForge build for MC 26.x; the bundled
        // shim returns default config values (no disk persistence / config GUI on Forge).
        Simplebuilding.setConfig(AutoConfig.getConfigHolder(SimplebuildingConfig.class).getConfig());
    }

    private void registerContent() {
        ModScreenHandlers.registerScreenHandlers();
        ModItemGroups.registerItemGroups();
        ModBlockEntities.registerBlockEntities();
        ModLootTableProvider.modifyLootTables();
        ModTradeOffers.registerModTradeOffers();
        ModEnchantmentEffects.registerEnchantmentEffects();
        ModRecipes.registerRecipes();
        ModRegistries.registerModStuffs();
        registerCauldronBehavior();
    }

    private void registerCauldronBehavior() {
        CauldronInteraction cleanRangefinder = (state, world, pos, player, hand, stack) -> {
            Item item = stack.getItem();
            if (!(item instanceof OctantItem) || item == ModItems.OCTANT) {
                return InteractionResult.PASS;
            }
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
                ((CauldronInteractionDispatcherAccessor) (Object) CauldronInteractions.WATER).simplebuilding$put(coloredItem, cleanRangefinder);
            }
        }
    }
}
