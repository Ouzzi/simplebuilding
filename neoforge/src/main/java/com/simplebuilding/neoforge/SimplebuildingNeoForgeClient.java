package com.simplebuilding.neoforge;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.client.BackpackKeyHandler;
import com.simplebuilding.client.ClientState;
import com.simplebuilding.client.ClientToggleKeys;
import com.simplebuilding.client.DoubleJumpController;
import com.simplebuilding.client.gui.BackpackScreen;
import com.simplebuilding.client.gui.BuildingWandScreen;
import com.simplebuilding.client.gui.NetheriteHopperScreen;
import com.simplebuilding.client.gui.OctantScreen;
import com.simplebuilding.client.property.EnchantmentModelProperty;
import com.simplebuilding.client.property.TrimIconsModelProperty;
import com.simplebuilding.client.render.BlockHighlightRenderer;
import com.simplebuilding.client.render.BuildingWandPreviewRenderer;
import com.simplebuilding.client.render.MultiBlockBreakingSupport;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import com.simplebuilding.client.gui.tooltip.ReinforcedBundleTooltips;
import com.simplebuilding.networking.*;
import com.simplebuilding.platform.ClientNetworking;
import com.simplebuilding.neoforge.NeoForgeModRegistries;
import com.simplebuilding.util.EnchantmentHelper;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.Codec;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigManager;
import me.shedaniel.autoconfig.gui.ConfigScreenProvider;
import me.shedaniel.autoconfig.gui.DefaultGuiProviders;
import me.shedaniel.autoconfig.gui.DefaultGuiTransformers;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import com.simplebuilding.entity.ModEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Simplebuilding.MOD_ID, dist = Dist.CLIENT)
public final class SimplebuildingNeoForgeClient {
    @SuppressWarnings("deprecation") // KeyMapping.Category.register(Identifier) — same call Fabric uses
    public static final KeyMapping.Category KEY_CATEGORY_SIMPLEMODS = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "simplemods"));
    public static SelectItemModelProperty.Type<EnchantmentModelProperty, String> ENCHANTMENT_PROPERTY_TYPE;

    private boolean wasJumpPressed = false;

    public SimplebuildingNeoForgeClient(IEventBus modEventBus, ModContainer modContainer) {
        // Config button in the NeoForge mods list -> opens the cloth AutoConfig GUI.
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> buildConfigScreen(parent));
        modEventBus.addListener(SimplebuildingNeoForgeClient::registerEntityRenderers);
        modEventBus.addListener(SimplebuildingNeoForgeClient::addBackpackLayers);
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(SimplebuildingNeoForgeClient::registerMenus);
        modEventBus.addListener(SimplebuildingNeoForgeClient::registerKeys);
        modEventBus.addListener(SimplebuildingNeoForgeClient::registerHudLayers);
        modEventBus.addListener(SimplebuildingNeoForgeClient::registerSelectItemProperties);
        modEventBus.addListener(SimplebuildingNeoForgeClient::registerRangeSelectItemProperties);
        modEventBus.addListener(SimplebuildingNeoForgeClient::registerConditionalItemProperties);
        modEventBus.addListener(SimplebuildingNeoForgeClient::registerTooltipComponents);
        modEventBus.addListener(SimplebuildingNeoForgeClient::registerBlockTints);
        if (com.simplebuilding.version.McVersion.CRUCIBLE) {
            // Crucible P5: soul lava rendering (turquoise lava textures).
            modEventBus.addListener((net.neoforged.neoforge.client.event.RegisterFluidModelsEvent event) -> event.register(
                    com.simplebuilding.client.render.SoulLavaModel.unbaked(), com.simplebuilding.fluid.ModFluids.SOUL_LAVA,
                    com.simplebuilding.fluid.ModFluids.FLOWING_SOUL_LAVA));
        }
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onSubmitCustomGeometry);
        NeoForge.EVENT_BUS.addListener(this::onExtractLevelRenderState);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientHooks::onBlockOutlineExtract);
    }

    /** Der getragene Rucksack bzw. Koecher auf dem Ruecken: beide Spielermodelle und die Mannequins. */
    public static void addBackpackLayers(EntityRenderersEvent.AddLayers event) {
        for (net.minecraft.world.entity.player.PlayerModelType skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.AvatarRenderer<?> player = event.getPlayerRenderer(skin);
            if (player != null) {
                player.addLayer(new com.simplebuilding.client.render.BackpackLayer(player));
                player.addLayer(new com.simplebuilding.client.render.QuiverLayer(player));
            }
            net.minecraft.client.renderer.entity.player.AvatarRenderer<?> mannequin = event.getMannequinRenderer(skin);
            if (mannequin != null) {
                mannequin.addLayer(new com.simplebuilding.client.render.BackpackLayer(mannequin));
                mannequin.addLayer(new com.simplebuilding.client.render.QuiverLayer(mannequin));
            }
        }
    }

    /** Der aufsteigende Block wird wie fallender Sand gezeichnet. */
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.LEVITATING_BLOCK, FallingBlockRenderer::new);
        if (com.simplebuilding.version.McVersion.FLETCHING) {
            event.registerEntityRenderer(ModEntities.CRAFTED_ARROW, com.simplebuilding.fletching.client.CraftedArrowRenderer::new);
        }
        if (com.simplebuilding.version.McVersion.TRAINING_DUMMY) {
            event.registerEntityRenderer(ModEntities.STRAW_ARMOR_STAND, com.simplebuilding.dummy.client.TrainingDummyRenderer.straw());
            event.registerEntityRenderer(ModEntities.TRAINING_DUMMY, com.simplebuilding.dummy.client.TrainingDummyRenderer.dummy());
            event.registerEntityRenderer(ModEntities.MEDIUM_ARMOR_STAND, com.simplebuilding.dummy.client.PartialArmorStandRenderer.medium());
            event.registerEntityRenderer(ModEntities.SMALL_ARMOR_STAND, com.simplebuilding.dummy.client.PartialArmorStandRenderer.small());
        }
        // Abgelegte Schmiedevorlage: das Item-Modell der Vorlage als flache Platte.
        event.registerBlockEntityRenderer(NeoForgeModRegistries.PLACED_TEMPLATE_BE.get(), com.simplebuilding.client.render.PlacedTemplateRenderer::new);
        // Abgestelltes Buendel: das gezeigte Item schwebt darueber, solange man schleichend hinschaut.
        event.registerBlockEntityRenderer(NeoForgeModRegistries.PLACED_BUNDLE_BE.get(), com.simplebuilding.client.render.PlacedBundleRenderer::new);
        // Kleinteile auf einem Fleck: liegende Teile als Platten, Eier als 3D-Ei.
        if (NeoForgeModRegistries.PLACED_SMALL_PARTS_BE != null) event.registerBlockEntityRenderer(
                NeoForgeModRegistries.PLACED_SMALL_PARTS_BE.get(), com.simplebuilding.client.render.PlacedSmallPartsRenderer::new);
        if (NeoForgeModRegistries.CHESS_PIECES_BE != null) event.registerBlockEntityRenderer(
                NeoForgeModRegistries.CHESS_PIECES_BE.get(), com.simplebuilding.client.render.ChessPiecesRenderer::new);
        if (NeoForgeModRegistries.HAMMOCK_BE != null) event.registerBlockEntityRenderer(
                NeoForgeModRegistries.HAMMOCK_BE.get(), com.simplebuilding.client.render.HammockRenderer::new);
        if (com.simplebuilding.version.McVersion.END_SYSTEMS) event.registerBlockEntityRenderer(
                net.minecraft.world.level.block.entity.BlockEntityTypes.ENDER_CHEST, com.simplebuilding.client.render.AstralVaultRenderer::new);
        // Mod-Truhen: Vanillas Truhenmodell mit den Texturen der Stufe.
        event.registerBlockEntityRenderer(NeoForgeModRegistries.TIERED_CHEST_BE.get(), com.simplebuilding.client.render.TieredChestRenderer::new);
        if (NeoForgeModRegistries.TRAPPED_COPPER_CHEST_BE != null) event.registerBlockEntityRenderer(
                NeoForgeModRegistries.TRAPPED_COPPER_CHEST_BE.get(), com.simplebuilding.client.render.TrappedCopperChestRenderer::new);
        // Gestufte Shulkerkisten: Vanillas Shulkerkisten-Modell mit der Textur aus Stufe und Farbe.
        event.registerBlockEntityRenderer(NeoForgeModRegistries.TIERED_SHULKER_BOX_BE.get(), com.simplebuilding.client.render.TieredShulkerBoxRenderer::new);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        com.simplebuilding.client.blueprint.BlueprintClient.init();
        com.simplebuilding.client.guide.GuideBookClient.init();
        ClientNetworking.setSender(ClientPacketDistributor::sendToServer);
    }

    private static Screen buildConfigScreen(Screen parent) {
        // Builds the AutoConfig GUI directly from the config manager + the default GUI registry -
        // the same thing AutoConfigClient.getConfigScreen does, which the Fabric ModMenu path calls.
        // (AutoConfig itself has no getConfigScreen on 26.2 any more.)
        @SuppressWarnings("unchecked")
        ConfigManager<SimplebuildingConfig> manager =
                (ConfigManager<SimplebuildingConfig>) AutoConfig.getConfigHolder(SimplebuildingConfig.class);
        GuiRegistry registry = DefaultGuiTransformers.apply(DefaultGuiProviders.apply(new GuiRegistry()));
        return new ConfigScreenProvider<>(manager, registry, parent).get();
    }

    public static void registerMenus(RegisterMenuScreensEvent event) {
        event.register(NeoForgeModRegistries.NETHERITE_HOPPER_MENU.get(), NetheriteHopperScreen::new);
        event.register(NeoForgeModRegistries.BACKPACK_MENU.get(), BackpackScreen::new);
        event.register(NeoForgeModRegistries.TIERED_CHEST_MENU.get(), com.simplebuilding.client.gui.TieredChestScreen::new);
        if (NeoForgeModRegistries.FLETCHING_MENU != null) {
            event.register(NeoForgeModRegistries.FLETCHING_MENU.get(), com.simplebuilding.client.gui.FletchingScreen::new);
        }
        if (NeoForgeModRegistries.AUTO_SMITHER_MENU != null) {
            event.register(NeoForgeModRegistries.AUTO_SMITHER_MENU.get(), com.simplebuilding.client.gui.AutoSmitherScreen::new);
        }
        if (NeoForgeModRegistries.AUTONOMOUS_CRAFTER_MENU != null) {
            event.register(NeoForgeModRegistries.AUTONOMOUS_CRAFTER_MENU.get(), com.simplebuilding.client.gui.AutonomousCrafterScreen::new);
        }
    }

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        ClientState.highlightToggleKey = new KeyMapping(
                "key.simplebuilding.toggle_highlight",
                InputConstants.KEY_H,
                KEY_CATEGORY_SIMPLEMODS
        );
        ClientState.octantFigureToggleKey = new KeyMapping(
                "key.simplebuilding.toggle_octant_figure",
                InputConstants.UNKNOWN.getValue(),
                KEY_CATEGORY_SIMPLEMODS
        );
        ClientState.settingsKey = new KeyMapping(
                "key.simplebuilding.simple_settings",
                InputConstants.KEY_G,
                KEY_CATEGORY_SIMPLEMODS
        );
        ClientState.backpackKey = new KeyMapping(
                "key.simplebuilding.open_backpack",
                InputConstants.KEY_B,
                KEY_CATEGORY_SIMPLEMODS
        );
        event.register(ClientState.highlightToggleKey);
        event.register(ClientState.octantFigureToggleKey);
        event.register(ClientState.settingsKey);
        event.register(ClientState.backpackKey);
        ClientState.hudToggleKey = new KeyMapping(
                "key.simplebuilding.toggle_hud",
                InputConstants.UNKNOWN.getValue(),
                KEY_CATEGORY_SIMPLEMODS
        );
        event.register(ClientState.hudToggleKey);
    }

    public static void registerHudLayers(RegisterGuiLayersEvent event) {
        NeoForgeClientHooks.registerHudLayers(event);
    }

    public static void registerSelectItemProperties(RegisterSelectItemModelPropertyEvent event) {
        ENCHANTMENT_PROPERTY_TYPE = SelectItemModelProperty.Type.create(EnchantmentModelProperty.CODEC, Codec.STRING);
        EnchantmentModelProperty.PROPERTY_TYPE = ENCHANTMENT_PROPERTY_TYPE;
        event.register(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "enchant_type"), ENCHANTMENT_PROPERTY_TYPE);
        TrimIconsModelProperty.PROPERTY_TYPE = SelectItemModelProperty.Type.create(TrimIconsModelProperty.CODEC, Codec.STRING);
        event.register(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "visible_trim_icons"), TrimIconsModelProperty.PROPERTY_TYPE);
        event.register(com.simplebuilding.client.property.BlueprintStateModelProperty.ID,
                com.simplebuilding.client.property.BlueprintStateModelProperty.PROPERTY_TYPE);
        event.register(com.simplebuilding.client.property.BrushInkModelProperty.ID,
                com.simplebuilding.client.property.BrushInkModelProperty.PROPERTY_TYPE);
    }

    /** Messuhr: Nadel auf dem Item (simplebuilding:gauge_needle). */
    public static void registerRangeSelectItemProperties(net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(com.simplebuilding.client.property.GaugeNeedleModelProperty.ID,
                com.simplebuilding.client.property.GaugeNeedleModelProperty.CODEC);
    }

    /** 26.3-Rotator: Drehanimation, solange ein Klick den anvisierten Block drehen wuerde (simplebuilding:transform_hint). */
    public static void registerConditionalItemProperties(net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent event) {
        event.register(com.simplebuilding.client.property.TransformHintModelProperty.ID,
                com.simplebuilding.client.property.TransformHintModelProperty.CODEC);
    }

    /** Abgestellter gefaerbter Rucksack: Leder-Ebene in der Farbe der Block-Entity. */
    public static void registerBlockTints(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(java.util.List.of(com.simplebuilding.client.render.BackpackBlockTint.INSTANCE),
                com.simplebuilding.blocks.ModBlocks.BACKPACK, com.simplebuilding.blocks.ModBlocks.REINFORCED_BACKPACK,
                com.simplebuilding.blocks.ModBlocks.NETHERITE_BACKPACK, com.simplebuilding.blocks.ModBlocks.ENDERITE_BACKPACK,
                com.simplebuilding.blocks.ModBlocks.PLACED_BUNDLE);
    }

    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ReinforcedBundleTooltipData.class, ReinforcedBundleTooltips::create);
        event.register(com.simplebuilding.items.tooltip.BlueprintTooltipData.class, com.simplebuilding.client.blueprint.BlueprintTooltip::create);
        event.register(com.simplebuilding.items.tooltip.GuideTooltipData.class, com.simplebuilding.client.gui.tooltip.GuideTooltip::create);
        event.register(com.simplebuilding.items.tooltip.BackpackTooltipData.class, com.simplebuilding.client.gui.tooltip.BackpackTooltip::create);
        event.register(com.simplebuilding.items.tooltip.PaintBoxTooltipData.class, com.simplebuilding.client.gui.tooltip.PaintBoxTooltip::create);
    }

    private void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        SimplebuildingConfig config = AutoConfig.getConfigHolder(SimplebuildingConfig.class).getConfig();
        ClientNetworking.send(new TrimBenefitPayload(config.enableArmorTrimBenefits));
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        // Shared with Fabric and Forge (see ClientToggleKeys); drains the queue even without a player.
        ClientToggleKeys.tick(client);
        if (client.player == null) {
            return;
        }

        // Rucksack-Taste, ebenfalls gemeinsam mit Fabric.
        BackpackKeyHandler.tick(client);

        while (ClientState.settingsKey != null && ClientState.settingsKey.consumeClick()) {
            ItemStack stack = client.player.getMainHandItem();
            if (stack.getItem() instanceof OctantItem) {
                client.setScreenAndShow(new OctantScreen(stack));
            } else if (stack.getItem() instanceof BuildingWandItem && client.level != null
                    && EnchantmentHelper.hasEnchantment(stack, client.level, ModEnchantments.CONSTRUCTORS_TOUCH)) {
                client.setScreenAndShow(new BuildingWandScreen(stack));
            }
        }

        boolean isJumpPressed = client.options.keyJump.isDown();
        if (isJumpPressed != wasJumpPressed) {
            ClientNetworking.send(new SpaceKeyPayload(isJumpPressed));
            wasJumpPressed = isJumpPressed;
        }

        // Shared air-jump + level-dependent cooldown logic (see DoubleJumpController).
        DoubleJumpController.tick(client);
        com.simplebuilding.items.custom.VelocityGaugeItem.clientAutowalkToggle = com.simplebuilding.client.GaugeAutowalk::toggle;
        com.simplebuilding.client.GaugeAutowalk.tick(client);
    }

    // Seit MC 26.2 wird Geometrie über den SubmitNodeCollector eingereicht; RenderLevelStageEvent
    // verweist dafür selbst auf SubmitCustomGeometryEvent.
    private void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        BlockHighlightRenderer.renderInWorldWithCamera(
                event.getSubmitNodeCollector(),
                event.getPoseStack(),
                event.getLevelRenderState().cameraRenderState.pos
        );
        BuildingWandPreviewRenderer.render(
                event.getSubmitNodeCollector(),
                event.getPoseStack(),
                event.getLevelRenderState().cameraRenderState.pos
        );
    }

    // Abbau-Risse auf allen verbundenen Blöcken (Sledgehammer, Strip Miner, Vein Miner);
    // das Event feuert nach der Vanilla-Extraktion der Breaking-States.
    private void onExtractLevelRenderState(ExtractLevelRenderStateEvent event) {
        MultiBlockBreakingSupport.extractExtraBreakingStates(event.getRenderState(), event.getLevel());
    }

}
