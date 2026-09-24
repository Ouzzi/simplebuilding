package com.simplebuilding.forge;

import com.simplebuilding.util.OctantCauldronWash;
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
import com.simplebuilding.mixin.CauldronInteractionDispatcherAccessor;
import com.simplebuilding.platform.ModEnvironment;
import com.simplebuilding.recipe.ModRecipes;
import com.simplebuilding.screen.ModScreenHandlers;
import com.simplebuilding.util.ModRegistries;
import com.simplebuilding.forge.networking.ForgeNetworkRegistration;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.RegisterEvent;

@Mod(Simplebuilding.MOD_ID)
public final class SimplebuildingForge {
    // Forge 65 sucht nur einen Konstruktor mit FMLJavaModLoadingContext oder einen ohne Parameter
    // (FMLModContainer#constructMod); ein ModContainer-Parameter endet in NoSuchMethodException.
    public SimplebuildingForge(FMLJavaModLoadingContext context) {
        BusGroup modBus = context.getModBusGroup();
        ModEnvironment.setModLoadedCheck(ModList::isLoaded);
        ModEnvironment.setDevelopmentEnvironment(!net.minecraftforge.fml.loading.FMLEnvironment.production);
        ForgeModRegistries.register(modBus);
        RegisterEvent.getBus(modBus).addListener(ForgeRegistryBootstrap::onRegister);
        FMLCommonSetupEvent.getBus(modBus).addListener(this::commonSetup);
        ForgeNetworkRegistration.register();
        com.simplebuilding.forge.gametest.ForgeGameTests.register(modBus);
        ForgeItemAutomation.install();
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
        // Gefaerbte Oktanten im Wasserkessel waschen - Interaktion und Liste in OctantCauldronWash,
        // aus derselben Quelle lesen Wiki und JEI.
        for (Item coloredItem : OctantCauldronWash.washableOctants()) {
            ((CauldronInteractionDispatcherAccessor) (Object) CauldronInteractions.WATER).simplebuilding$put(coloredItem, OctantCauldronWash.INTERACTION);
        }
    }
}
