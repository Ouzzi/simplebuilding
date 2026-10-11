package com.simplebuilding.forge;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.client.ClientState;
import com.simplebuilding.client.gui.NetheriteHopperScreen;
import com.simplebuilding.client.gui.RangefinderHudOverlay;
import com.simplebuilding.client.gui.SoulBurnOverlay;
import com.simplebuilding.client.gui.SpeedometerHudOverlay;
import com.simplebuilding.forge.networking.ForgeNetworkRegistration;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import com.simplebuilding.client.gui.tooltip.ReinforcedBundleTooltips;
import com.simplebuilding.platform.ClientNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Forge client wiring on the mod bus (client dist): networking sender, key
 * mappings, menu screen, and tooltip components. Game-bus client events (tick,
 * login, highlight) live in {@link ForgeClientGameEvents}.
 *
 * In-world highlights (sledgehammer field, vein/strip miner, building wand preview)
 * and the extra breaking cracks have no Forge 65 event (no extract/submit hook like
 * NeoForge's ExtractLevelRenderStateEvent / SubmitCustomGeometryEvent); they are wired
 * by the client mixins LevelExtractorMixin and LevelRendererMixin in
 * com.simplebuilding.mixin.forge (simplebuilding.forge.mixins.json).
 *
 * HUD overlays (octant rangefinder, speedometer) hang in Forge's layered HUD via
 * AddGuiOverlayLayersEvent, like Fabric's HudElementRegistry and NeoForge's RegisterGuiLayersEvent.
 * The enchant_type select item-model property has no Forge registration event and is
 * registered by SelectItemModelPropertiesMixin.
 */
@Mod.EventBusSubscriber(modid = Simplebuilding.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SimplebuildingForgeClient {
    @SuppressWarnings("deprecation")
    public static final KeyMapping.Category KEY_CATEGORY_SIMPLEMODS = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "simplemods"));

    private SimplebuildingForgeClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        com.simplebuilding.client.blueprint.BlueprintClient.init();
        com.simplebuilding.client.guide.GuideBookClient.init();
        ClientNetworking.setSender(ForgeNetworkRegistration::sendToServer);
        event.enqueueWork(() -> {
            MenuScreens.register(ForgeModRegistries.NETHERITE_HOPPER_MENU.get(), NetheriteHopperScreen::new);
            MenuScreens.register(ForgeModRegistries.BACKPACK_MENU.get(), com.simplebuilding.client.gui.BackpackScreen::new);
            MenuScreens.register(ForgeModRegistries.TIERED_CHEST_MENU.get(), com.simplebuilding.client.gui.TieredChestScreen::new);
            if (ForgeModRegistries.FLETCHING_MENU != null) {
                MenuScreens.register(ForgeModRegistries.FLETCHING_MENU.get(), com.simplebuilding.client.gui.FletchingScreen::new);
            }
            if (ForgeModRegistries.AUTO_SMITHER_MENU != null) {
                MenuScreens.register(ForgeModRegistries.AUTO_SMITHER_MENU.get(), com.simplebuilding.client.gui.AutoSmitherScreen::new);
            }
            if (ForgeModRegistries.AUTONOMOUS_CRAFTER_MENU != null) {
                MenuScreens.register(ForgeModRegistries.AUTONOMOUS_CRAFTER_MENU.get(), com.simplebuilding.client.gui.AutonomousCrafterScreen::new);
            }
            if (ForgeModRegistries.ASTRAL_ENCHANTING_MENU != null) {
                MenuScreens.register(ForgeModRegistries.ASTRAL_ENCHANTING_MENU.get(), com.simplebuilding.client.gui.AstralEnchantingScreen::new);
            }
            if (ForgeModRegistries.STORAGE_CRAFTING_TABLE_MENU != null) {
                MenuScreens.register(ForgeModRegistries.STORAGE_CRAFTING_TABLE_MENU.get(), com.simplebuilding.client.gui.StorageCraftingScreen::new);
            }
        });
    }

    /**
     * Forge 65 fuehrt Tasten- und Tooltip-Registrierung auf dem Standard-Bus, nicht auf dem Mod-Bus -
     * ein Mod-Bus-Abonnent dafuer bricht das Laden ab. Darum eine eigene Klasse ohne bus-Parameter.
     */
    @Mod.EventBusSubscriber(modid = Simplebuilding.MOD_ID, value = Dist.CLIENT)
    public static final class DefaultBusEvents {
        private DefaultBusEvents() {
        }

    /** Wie Fabric/NeoForge: schwebender Sand/Kies nutzt den Vanilla-FallingBlockRenderer. */
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(com.simplebuilding.entity.ModEntities.LEVITATING_BLOCK,
                net.minecraft.client.renderer.entity.FallingBlockRenderer::new);
        if (com.simplebuilding.version.McVersion.FLETCHING) {
            event.registerEntityRenderer(com.simplebuilding.entity.ModEntities.CRAFTED_ARROW, com.simplebuilding.fletching.client.CraftedArrowRenderer::new);
        }
        if (com.simplebuilding.version.McVersion.TRAINING_DUMMY) {
            event.registerEntityRenderer(com.simplebuilding.entity.ModEntities.STRAW_ARMOR_STAND, com.simplebuilding.dummy.client.TrainingDummyRenderer.straw());
            event.registerEntityRenderer(com.simplebuilding.entity.ModEntities.TRAINING_DUMMY, com.simplebuilding.dummy.client.TrainingDummyRenderer.dummy());
            event.registerEntityRenderer(com.simplebuilding.entity.ModEntities.SMALL_ARMOR_STAND, com.simplebuilding.dummy.client.SmallArmorStandRenderer::new);
        }
        // Fahrzeug-Stufen (Queue N19/N23).
        com.simplebuilding.client.render.VehicleRenderers.register(new com.simplebuilding.client.render.VehicleRenderers.Registrar() {
            @Override
            public <T extends net.minecraft.world.entity.Entity> void register(net.minecraft.world.entity.EntityType<? extends T> type,
                    net.minecraft.client.renderer.entity.EntityRendererProvider<T> provider) {
                event.registerEntityRenderer(type, provider);
            }
        });
        // Abgelegte Schmiedevorlage: das Item-Modell der Vorlage als flache Platte.
        event.registerBlockEntityRenderer(com.simplebuilding.forge.ForgeModRegistries.PLACED_TEMPLATE_BE.get(),
                com.simplebuilding.client.render.PlacedTemplateRenderer::new);
        // Abgestelltes Buendel: das gezeigte Item schwebt darueber, solange man schleichend hinschaut.
        event.registerBlockEntityRenderer(com.simplebuilding.forge.ForgeModRegistries.PLACED_BUNDLE_BE.get(), com.simplebuilding.client.render.PlacedBundleRenderer::new);
        // Kleinteile auf einem Fleck: liegende Teile als Platten, Eier als 3D-Ei.
        if (com.simplebuilding.forge.ForgeModRegistries.STORAGE_CRAFTING_TABLE_BE != null) event.registerBlockEntityRenderer(
                com.simplebuilding.forge.ForgeModRegistries.STORAGE_CRAFTING_TABLE_BE.get(), com.simplebuilding.client.render.StorageCraftingTableRenderer::new);
        if (com.simplebuilding.forge.ForgeModRegistries.PLACED_SMALL_PARTS_BE != null) event.registerBlockEntityRenderer(
                com.simplebuilding.forge.ForgeModRegistries.PLACED_SMALL_PARTS_BE.get(), com.simplebuilding.client.render.PlacedSmallPartsRenderer::new);
        if (com.simplebuilding.forge.ForgeModRegistries.GOAT_HORN_HOLDER_BE != null) event.registerBlockEntityRenderer(
                com.simplebuilding.forge.ForgeModRegistries.GOAT_HORN_HOLDER_BE.get(), com.simplebuilding.client.render.GoatHornHolderRenderer::new);
        if (com.simplebuilding.forge.ForgeModRegistries.CHESS_PIECES_BE != null) event.registerBlockEntityRenderer(
                com.simplebuilding.forge.ForgeModRegistries.CHESS_PIECES_BE.get(), com.simplebuilding.client.render.ChessPiecesRenderer::new);
        if (com.simplebuilding.forge.ForgeModRegistries.CRATE_BE != null) event.registerBlockEntityRenderer(
                com.simplebuilding.forge.ForgeModRegistries.CRATE_BE.get(), com.simplebuilding.woodwork.client.CrateRenderer::new);
        // Haengematte: das Kopfteil zeichnet die ganze Matte entlang der Ankerlinie (jeder Winkel).
        if (com.simplebuilding.forge.ForgeModRegistries.HAMMOCK_BE != null) event.registerBlockEntityRenderer(
                com.simplebuilding.forge.ForgeModRegistries.HAMMOCK_BE.get(), com.simplebuilding.client.render.HammockRenderer::new);
        if (com.simplebuilding.forge.ForgeModRegistries.ASTRAL_ENCHANTING_TABLE_BE != null) event.registerBlockEntityRenderer(
                com.simplebuilding.forge.ForgeModRegistries.ASTRAL_ENCHANTING_TABLE_BE.get(), com.simplebuilding.client.render.AstralEnchantingTableRenderer::new);
        // Mod-Truhen: Vanillas Truhenmodell mit den Texturen der Stufe.
        event.registerBlockEntityRenderer(com.simplebuilding.forge.ForgeModRegistries.TIERED_CHEST_BE.get(),
                com.simplebuilding.client.render.TieredChestRenderer::new);
        if (com.simplebuilding.forge.ForgeModRegistries.TRAPPED_COPPER_CHEST_BE != null) event.registerBlockEntityRenderer(
                com.simplebuilding.forge.ForgeModRegistries.TRAPPED_COPPER_CHEST_BE.get(), com.simplebuilding.client.render.TrappedCopperChestRenderer::new);
        event.registerBlockEntityRenderer(com.simplebuilding.forge.ForgeModRegistries.TIERED_SHULKER_BOX_BE.get(),
                com.simplebuilding.client.render.TieredShulkerBoxRenderer::new);
    }

    /** Der getragene Rucksack bzw. Koecher auf dem Ruecken: beide Spielermodelle und die Mannequins. */
    @SubscribeEvent
    public static void onAddLayers(net.minecraftforge.client.event.EntityRenderersEvent.AddLayers event) {
        for (net.minecraft.world.entity.player.PlayerModelType type : event.getModelTypes()) {
            if (event.getPlayerRenderer(type) instanceof net.minecraft.client.renderer.entity.player.AvatarRenderer<?> player) {
                player.addLayer(new com.simplebuilding.client.render.BackpackLayer(player));
                player.addLayer(new com.simplebuilding.client.render.QuiverLayer(player));
            }
            if (event.getMannequinRenderer(type) instanceof net.minecraft.client.renderer.entity.player.AvatarRenderer<?> mannequin) {
                mannequin.addLayer(new com.simplebuilding.client.render.BackpackLayer(mannequin));
                mannequin.addLayer(new com.simplebuilding.client.render.QuiverLayer(mannequin));
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
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

    /**
     * HUD-Overlays wie auf NeoForge ueber dem Chat, im Post-Sleep-Stapel von Forges Schicht-HUD.
     * Das Event feuert einmal beim Aufbau des Hud (Minecraft-Konstruktor), wie die Tastenregistrierung.
     */
    @SubscribeEvent
    public static void onAddGuiLayers(AddGuiOverlayLayersEvent event) {
        Identifier rangefinder = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "rangefinder_hud");
        Identifier speedometer = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "speedometer_hud");
        Identifier soulBurn = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "soul_burn_filter");
        ForgeLayeredDraw root = event.getLayeredDraw();
        Identifier stack = ForgeLayeredDraw.POST_SLEEP_STACK;
        root.addAbove(stack, rangefinder, ForgeLayeredDraw.CHAT_OVERLAY, (gg, dt) -> RangefinderHudOverlay.render(gg));
        root.addAbove(stack, speedometer, rangefinder, (gg, dt) -> SpeedometerHudOverlay.render(gg));
        // Seelenbrand-Vollbildfilter, Nachtrag 11 P1 (2026-10-06).
        root.addAbove(stack, soulBurn, speedometer, (gg, dt) -> SoulBurnOverlay.render(gg));
        // The air jump cooldown bar is no layer: it takes vanilla's contextual bar slot
        // (HudContextualBarMixin on Hud#updateContextualInfo / extractContextualInfoState), 2026-09-29.
    }

    /** Abgestellter gefaerbter Rucksack: Leder-Ebene in der Farbe der Block-Entity. */
    @SubscribeEvent
    public static void onRegisterBlockColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register(java.util.List.of(com.simplebuilding.client.render.BackpackBlockTint.INSTANCE),
                com.simplebuilding.blocks.ModBlocks.BACKPACK, com.simplebuilding.blocks.ModBlocks.REINFORCED_BACKPACK,
                com.simplebuilding.blocks.ModBlocks.NETHERITE_BACKPACK, com.simplebuilding.blocks.ModBlocks.ENDERITE_BACKPACK,
                com.simplebuilding.blocks.ModBlocks.PLACED_BUNDLE);
        if (com.simplebuilding.blocks.ModBlocks.GRASS_SLAB != null) {
            // Gras-Stufe (N25) biomgefaerbt wie der Grasblock.
            event.register(java.util.List.of(net.minecraft.client.color.block.BlockTintSources.grassBlock()),
                    com.simplebuilding.blocks.ModBlocks.GRASS_SLAB);
        }
        if (!com.simplebuilding.blocks.ModBlocks.WOOD_FAMILIES.isEmpty()) event.register(
                java.util.List.of(com.simplebuilding.woodwork.client.WoodenCauldronTint.INSTANCE), com.simplebuilding.woodwork.WoodBlocks.cauldrons());
    }

    @SubscribeEvent
    public static void onRegisterTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ReinforcedBundleTooltipData.class, ReinforcedBundleTooltips::create);
        event.register(com.simplebuilding.items.tooltip.BlueprintTooltipData.class, com.simplebuilding.client.blueprint.BlueprintTooltip::create);
        event.register(com.simplebuilding.items.tooltip.BackpackTooltipData.class, com.simplebuilding.client.gui.tooltip.BackpackTooltip::create);
        event.register(com.simplebuilding.items.tooltip.PaintBoxTooltipData.class, com.simplebuilding.client.gui.tooltip.PaintBoxTooltip::create);
    }
    }
}
