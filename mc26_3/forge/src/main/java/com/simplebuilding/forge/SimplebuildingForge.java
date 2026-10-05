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
        // FTB Quests (optional): copy the SimpleBuilding chapters into its quest book once.
        com.simplebuilding.compat.FtbQuestsDefaults.installIfPresent(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
        Forge263Events.register();
        ForgeModRegistries.register(modBus);
        // Crucible P5: soul lava needs its Forge fluid type before the fluids are created.
        ForgeSoulLava.install(modBus);
        RegisterEvent.getBus(modBus).addListener(ForgeRegistryBootstrap::onRegister);
        FMLCommonSetupEvent.getBus(modBus).addListener(this::commonSetup);
        if (com.simplebuilding.version.McVersion.TRAINING_DUMMY) {
            net.minecraftforge.event.entity.EntityAttributeCreationEvent.BUS.addListener(event -> {
                event.put(com.simplebuilding.entity.ModEntities.STRAW_ARMOR_STAND, com.simplebuilding.dummy.TrainingDummy.createAttributes().build());
                event.put(com.simplebuilding.entity.ModEntities.TRAINING_DUMMY, com.simplebuilding.dummy.TrainingDummy.createAttributes().build());
            });
        }
        ForgeNetworkRegistration.register();
        com.simplebuilding.tweaks.forge.TweaksForge.register(modBus);
        com.simplebuilding.forge.gametest.ForgeGameTests.register(modBus);
        ForgeItemAutomation.install();
        ForgePistonBreakGuard.install();
        ForgeBuildGuard.install();
        ForgeDataTables.register();
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
        // The Forge 26.3 compatibility entrypoint loads and validates persistent JSON.
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
        for (Item box : com.simplebuilding.util.TieredShulkerBoxes.items()) {
            ((CauldronInteractionDispatcherAccessor) (Object) CauldronInteractions.WATER)
                    .simplebuilding$put(box, com.simplebuilding.util.TieredShulkerBoxes.WASH);
        }
        com.simplebuilding.util.TieredShulkerBoxes.registerDispenserBehavior();
    }
}
