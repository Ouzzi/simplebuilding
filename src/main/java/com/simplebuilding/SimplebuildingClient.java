package com.simplebuilding;

import com.simplebuilding.client.ClientToggleKeys;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.Codec;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.client.gui.*;
import com.simplebuilding.client.gui.tooltip.ReinforcedBundleTooltipSubmenuHandler;
import com.simplebuilding.client.property.EnchantmentModelProperty;
import com.simplebuilding.client.property.TrimIconsModelProperty;
import com.simplebuilding.client.render.BlockHighlightRenderer;
import com.simplebuilding.client.render.BlockOutlineSupport;
import com.simplebuilding.client.render.BuildingWandPreviewRenderer;
import com.simplebuilding.client.render.MultiBlockBreakingSupport;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.entity.ModEntities;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import com.simplebuilding.client.gui.tooltip.ReinforcedBundleTooltips;
import com.simplebuilding.networking.*;
import com.simplebuilding.screen.ModScreenHandlers;
import com.simplebuilding.util.SurvivalTracerAccessor;
import com.simplebuilding.client.BackpackKeyHandler;
import com.simplebuilding.client.ClientState;
import com.simplebuilding.client.DoubleJumpController;
import com.simplebuilding.platform.ClientNetworking;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static com.simplebuilding.util.EnchantmentHelper.getEnchantmentLevel;
import static com.simplebuilding.util.EnchantmentHelper.hasEnchantment;

public class SimplebuildingClient implements ClientModInitializer {

    // Tasten
    public static final KeyMapping.Category KEY_CATEGORY_SIMPLEMODS = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "simplemods"));

    public static SelectItemModelProperty.Type<EnchantmentModelProperty, String> ENCHANTMENT_PROPERTY_TYPE;
    private boolean wasJumpPressed = false;

    @Override
    @SuppressWarnings("deprecation")
    public void onInitializeClient() {
        ClientNetworking.setSender(ClientPlayNetworking::send);

        // Der aufsteigende Block wird wie fallender Sand gezeichnet.
        EntityRendererRegistry.register(ModEntities.LEVITATING_BLOCK, FallingBlockRenderer::new);
        if (com.simplebuilding.version.McVersion.FLETCHING) {
            EntityRendererRegistry.register(ModEntities.CRAFTED_ARROW, com.simplebuilding.fletching.client.CraftedArrowRenderer::new);
        }
        if (com.simplebuilding.version.McVersion.TRAINING_DUMMY) {
            EntityRendererRegistry.register(ModEntities.STRAW_ARMOR_STAND, com.simplebuilding.dummy.client.TrainingDummyRenderer.straw());
            EntityRendererRegistry.register(ModEntities.TRAINING_DUMMY, com.simplebuilding.dummy.client.TrainingDummyRenderer.dummy());
            EntityRendererRegistry.register(ModEntities.MEDIUM_ARMOR_STAND, com.simplebuilding.dummy.client.PartialArmorStandRenderer.medium());
            EntityRendererRegistry.register(ModEntities.SMALL_ARMOR_STAND, com.simplebuilding.dummy.client.PartialArmorStandRenderer.small());
        }
        // Abgelegte Schmiedevorlage: das Item-Modell der Vorlage als flache Platte.
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
                com.simplebuilding.blocks.entity.ModBlockEntities.PLACED_TEMPLATE_BE, com.simplebuilding.client.render.PlacedTemplateRenderer::new);
        // Abgestelltes Buendel: das gezeigte Item schwebt darueber, solange man schleichend hinschaut.
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
                com.simplebuilding.blocks.entity.ModBlockEntities.PLACED_BUNDLE_BE, com.simplebuilding.client.render.PlacedBundleRenderer::new);
        // Kleinteile auf einem Fleck: liegende Teile als Platten, Eier als 3D-Ei.
        if (com.simplebuilding.blocks.entity.ModBlockEntities.STORAGE_CRAFTING_TABLE_BE != null) net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
                com.simplebuilding.blocks.entity.ModBlockEntities.STORAGE_CRAFTING_TABLE_BE, com.simplebuilding.client.render.StorageCraftingTableRenderer::new);
        if (com.simplebuilding.blocks.entity.ModBlockEntities.PLACED_SMALL_PARTS_BE != null) net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
                com.simplebuilding.blocks.entity.ModBlockEntities.PLACED_SMALL_PARTS_BE, com.simplebuilding.client.render.PlacedSmallPartsRenderer::new);
        if (com.simplebuilding.blocks.entity.ModBlockEntities.GOAT_HORN_HOLDER_BE != null) net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
                com.simplebuilding.blocks.entity.ModBlockEntities.GOAT_HORN_HOLDER_BE, com.simplebuilding.client.render.GoatHornHolderRenderer::new);
        if (com.simplebuilding.blocks.entity.ModBlockEntities.CHESS_PIECES_BE != null) net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
                com.simplebuilding.blocks.entity.ModBlockEntities.CHESS_PIECES_BE, com.simplebuilding.client.render.ChessPiecesRenderer::new);
        // Haengematte: das Kopfteil zeichnet die ganze Matte entlang der Ankerlinie (jeder Winkel).
        if (com.simplebuilding.blocks.entity.ModBlockEntities.HAMMOCK_BE != null) net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
                com.simplebuilding.blocks.entity.ModBlockEntities.HAMMOCK_BE, com.simplebuilding.client.render.HammockRenderer::new);
        if (com.simplebuilding.blocks.entity.ModBlockEntities.ASTRAL_ENCHANTING_TABLE_BE != null) net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
                com.simplebuilding.blocks.entity.ModBlockEntities.ASTRAL_ENCHANTING_TABLE_BE, com.simplebuilding.client.render.AstralEnchantingTableRenderer::new);
        if (com.simplebuilding.version.McVersion.END_SYSTEMS) net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
                net.minecraft.world.level.block.entity.BlockEntityTypes.ENDER_CHEST, com.simplebuilding.client.render.AstralVaultRenderer::new);
        // Mod-Truhen: Vanillas Truhenmodell mit den Texturen der Stufe.
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
                com.simplebuilding.blocks.entity.ModBlockEntities.TIERED_CHEST_BE, com.simplebuilding.client.render.TieredChestRenderer::new);
        // Gestufte Shulkerkisten: Vanillas Shulkerkisten-Modell mit der Textur aus Stufe und Farbe.
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
                com.simplebuilding.blocks.entity.ModBlockEntities.TIERED_SHULKER_BOX_BE, com.simplebuilding.client.render.TieredShulkerBoxRenderer::new);
        // Der getragene Rucksack bzw. Koecher auf dem Ruecken - auf jedem Avatar-Renderer (beide Spielermodelle, Mannequins).
        // Abgestellter gefaerbter Rucksack: Leder-Ebene in der Farbe der Block-Entity.
        if (com.simplebuilding.version.McVersion.CRUCIBLE) {
            // Crucible P5: soul lava rendering (turquoise lava textures).
            net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry.register(com.simplebuilding.fluid.ModFluids.SOUL_LAVA,
                    com.simplebuilding.fluid.ModFluids.FLOWING_SOUL_LAVA, com.simplebuilding.client.render.SoulLavaModel.unbaked());
        }
        net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register(
                java.util.List.of(com.simplebuilding.client.render.BackpackBlockTint.INSTANCE),
                com.simplebuilding.blocks.ModBlocks.BACKPACK, com.simplebuilding.blocks.ModBlocks.REINFORCED_BACKPACK,
                com.simplebuilding.blocks.ModBlocks.NETHERITE_BACKPACK, com.simplebuilding.blocks.ModBlocks.ENDERITE_BACKPACK,
                com.simplebuilding.blocks.ModBlocks.PLACED_BUNDLE);
        if (com.simplebuilding.blocks.ModBlocks.GRASS_SLAB != null) {
            // Gras-Stufe (N25) biomgefaerbt wie der Grasblock.
            net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register(java.util.List.of(net.minecraft.client.color.block.BlockTintSources.grassBlock()),
                    com.simplebuilding.blocks.ModBlocks.GRASS_SLAB);
        }
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
            if (renderer instanceof net.minecraft.client.renderer.entity.player.AvatarRenderer<?> avatar) {
                helper.register(new com.simplebuilding.client.render.BackpackLayer(avatar));
                helper.register(new com.simplebuilding.client.render.QuiverLayer(avatar));
            }
        });
        ClientState.highlightToggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.simplebuilding.toggle_highlight",
            InputConstants.KEY_H,
            KEY_CATEGORY_SIMPLEMODS
        ));
        ClientState.octantFigureToggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.simplebuilding.toggle_octant_figure",
            InputConstants.UNKNOWN.getValue(),
            KEY_CATEGORY_SIMPLEMODS
        ));
        ClientState.settingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.simplebuilding.simple_settings",
                InputConstants.KEY_G,
            KEY_CATEGORY_SIMPLEMODS
        ));
        ClientState.backpackKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.simplebuilding.open_backpack",
                InputConstants.KEY_B,
                KEY_CATEGORY_SIMPLEMODS
        ));
        ClientState.hudToggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.simplebuilding.toggle_hud",
                InputConstants.UNKNOWN.getValue(),
                KEY_CATEGORY_SIMPLEMODS
        ));
        // Rucksack-Taste: gemeinsamer Handler mit NeoForge (BackpackKeyHandler).
        ClientTickEvents.END_CLIENT_TICK.register(BackpackKeyHandler::tick);

        // --- Event Loop (Tick) ---
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Umschalttasten: ein gemeinsamer Handler fuer alle Loader (Audit 2026-09-26 #38).
            ClientToggleKeys.tick(client);

            while (ClientState.settingsKey.consumeClick()) {
                if (client.player != null) {
                    ItemStack stack = client.player.getMainHandItem();
                    if (stack.getItem() instanceof OctantItem) {
                        client.gui.setScreen(new OctantScreen(stack));
                    } else if (stack.getItem() instanceof BuildingWandItem) {
                        if (client.level != null && hasEnchantment(stack, client.level, ModEnchantments.CONSTRUCTORS_TOUCH)) {
                            client.gui.setScreen(new BuildingWandScreen(stack));
                        }
                    }
                }
            }
        });

        // --- HUD & Renderer ---
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "rangefinder_hud"),
                (context, tickCounter) -> RangefinderHudOverlay.render(context)
        );
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "speedometer_hud"),
                (context, tickCounter) -> SpeedometerHudOverlay.render(context)
        );
        // Seelenbrand-Vollbildfilter, Nachtrag 11 P1 (2026-10-06): eigene Schicht neben den Anzeigen der Mod.
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "soul_burn_filter"),
                (context, tickCounter) -> SoulBurnOverlay.render(context)
        );
        // The air jump cooldown bar is no HUD element: it takes vanilla's contextual bar slot
        // (HudContextualBarMixin, all loaders), 2026-09-29.
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, outlineRenderState) ->
                !BlockOutlineSupport.suppressVanillaBlockOutline());

        // --- Tooltips ---
        ClientTooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof com.simplebuilding.items.tooltip.GuideTooltipData guide) return com.simplebuilding.client.gui.tooltip.GuideTooltip.create(guide);
            if (data instanceof ReinforcedBundleTooltipData reinforcedData) {
                return ReinforcedBundleTooltips.create(reinforcedData);
            }
            if (data instanceof com.simplebuilding.items.tooltip.BlueprintTooltipData blueprintData) {
                return com.simplebuilding.client.blueprint.BlueprintTooltip.create(blueprintData);
            }
            if (data instanceof com.simplebuilding.items.tooltip.PaintBoxTooltipData paintBox) {
                return com.simplebuilding.client.gui.tooltip.PaintBoxTooltip.create(paintBox);
            }
            if (data instanceof com.simplebuilding.items.tooltip.BackpackTooltipData backpackData) {
                return com.simplebuilding.client.gui.tooltip.BackpackTooltip.create(backpackData);
            }
            return null;
        });

        registerDoubleJumpClient();
        com.simplebuilding.client.blueprint.BlueprintClient.init();
        com.simplebuilding.client.guide.GuideBookClient.init();

        // --- World Render ---
        // Seit MC 26.2 wird Geometrie nicht mehr direkt gezeichnet, sondern über den
        // SubmitNodeCollector eingereicht; END_MAIN/AFTER_TRANSLUCENT_TERRAIN kämen dafür
        // zu spät. Beide Renderer hängen deshalb an COLLECT_SUBMITS, in einem Handler,
        // damit die Reihenfolge (Highlights vor Wand-Vorschau) festgelegt bleibt.
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            BlockHighlightRenderer.renderInWorld(
                    context.submitNodeCollector(),
                    context.poseStack(),
                    Minecraft.getInstance().gameRenderer.mainCamera()
            );
            BuildingWandPreviewRenderer.render(
                    context.submitNodeCollector(),
                    context.poseStack(),
                    context.levelState().cameraRenderState.pos
            );
        });

        // Abbau-Risse auf allen verbundenen Blöcken (Sledgehammer, Strip Miner, Vein Miner)
        LevelRenderEvents.END_EXTRACTION.register(context ->
                MultiBlockBreakingSupport.extractExtraBreakingStates(context.levelState(), context.level()));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            SimplebuildingConfig config = AutoConfig.getConfigHolder(SimplebuildingConfig.class).getConfig();
            boolean wantsBenefits = config.enableArmorTrimBenefits;

            // Paket senden
            ClientNetworking.send(new TrimBenefitPayload(wantsBenefits));
        });

        MenuScreens.register(ModScreenHandlers.NETHERITE_HOPPER_SCREEN_HANDLER, NetheriteHopperScreen::new);
        MenuScreens.register(ModScreenHandlers.BACKPACK_MENU, BackpackScreen::new);
        MenuScreens.register(ModScreenHandlers.TIERED_CHEST_MENU, com.simplebuilding.client.gui.TieredChestScreen::new);
        if (ModScreenHandlers.FLETCHING_MENU != null) {
            MenuScreens.register(ModScreenHandlers.FLETCHING_MENU, com.simplebuilding.client.gui.FletchingScreen::new);
        }
        if (ModScreenHandlers.AUTO_SMITHER_MENU != null) {
            MenuScreens.register(ModScreenHandlers.AUTO_SMITHER_MENU, com.simplebuilding.client.gui.AutoSmitherScreen::new);
        }
        if (ModScreenHandlers.AUTONOMOUS_CRAFTER_MENU != null) {
            MenuScreens.register(ModScreenHandlers.AUTONOMOUS_CRAFTER_MENU, com.simplebuilding.client.gui.AutonomousCrafterScreen::new);
        }
        if (ModScreenHandlers.ASTRAL_ENCHANTING_MENU != null) {
            MenuScreens.register(ModScreenHandlers.ASTRAL_ENCHANTING_MENU, com.simplebuilding.client.gui.AstralEnchantingScreen::new);
        }
        if (ModScreenHandlers.STORAGE_CRAFTING_TABLE_MENU != null) {
            MenuScreens.register(ModScreenHandlers.STORAGE_CRAFTING_TABLE_MENU, com.simplebuilding.client.gui.StorageCraftingScreen::new);
        }

        // --- NETZWERK REGISTRIERUNG CLIENT-SEITE ---
        registerClientReceivers();

        ENCHANTMENT_PROPERTY_TYPE = SelectItemModelProperty.Type.create(EnchantmentModelProperty.CODEC, Codec.STRING);
        EnchantmentModelProperty.PROPERTY_TYPE = ENCHANTMENT_PROPERTY_TYPE;
        SelectItemModelProperties.ID_MAPPER.put(
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "enchant_type"),
                ENCHANTMENT_PROPERTY_TYPE
        );
        TrimIconsModelProperty.PROPERTY_TYPE = SelectItemModelProperty.Type.create(TrimIconsModelProperty.CODEC, Codec.STRING);
        SelectItemModelProperties.ID_MAPPER.put(
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "visible_trim_icons"),
                TrimIconsModelProperty.PROPERTY_TYPE
        );
        // Blaupause: normale, bearbeitete oder signierte Textur (assets/simplebuilding/items/blueprint.json).
        SelectItemModelProperties.ID_MAPPER.put(com.simplebuilding.client.property.BlueprintStateModelProperty.ID,
                com.simplebuilding.client.property.BlueprintStateModelProperty.PROPERTY_TYPE);
        SelectItemModelProperties.ID_MAPPER.put(com.simplebuilding.client.property.BrushInkModelProperty.ID,
                com.simplebuilding.client.property.BrushInkModelProperty.PROPERTY_TYPE);
        // Messuhr: Nadel auf dem Item (assets/simplebuilding/items/velocity_gauge.json).
        net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties.ID_MAPPER.put(
                com.simplebuilding.client.property.GaugeNeedleModelProperty.ID,
                com.simplebuilding.client.property.GaugeNeedleModelProperty.CODEC);
        // 26.3-Rotator: Drehanimation, solange ein Klick den anvisierten Block drehen wuerde (assets/simplebuilding/items/rotator.json).
        net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties.ID_MAPPER.put(
                com.simplebuilding.client.property.TransformHintModelProperty.ID,
                com.simplebuilding.client.property.TransformHintModelProperty.CODEC);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                boolean isJumpPressed = client.options.keyJump.isDown();

                // Nur senden, wenn sich der Status ändert (Bandbreite sparen)
                if (isJumpPressed != wasJumpPressed) {
                    ClientNetworking.send(new SpaceKeyPayload(isJumpPressed));
                    wasJumpPressed = isJumpPressed;
                }
            }
        });
    }

    private void registerClientReceivers() {
        // Kolben-Optionen des Servers (Audit N16), fuer den nachgespielten Kolben
        ClientPlayNetworking.registerGlobalReceiver(com.simplebuilding.networking.PistonConfigPayload.ID,
                (payload, context) -> context.client().execute(payload::apply));

        // Offene Handbuch-Reiter dieses Spielers (P5/P6)
        ClientPlayNetworking.registerGlobalReceiver(com.simplebuilding.networking.GuideStatePayload.ID,
                (payload, context) -> context.client().execute(payload::apply));

        // Trim Data (Hierhin verschoben von ModMessages)
        ClientPlayNetworking.registerGlobalReceiver(TrimDataPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.player() instanceof SurvivalTracerAccessor accessor) {
                    accessor.simplebuilding$setBaseValues(
                            payload.baseDist(), payload.baseTime(),
                            payload.baseHostile(), payload.basePassive(), payload.baseDamage()
                    );
                    accessor.simplebuilding$setBaseXp(payload.baseXp());
                }
                com.simplebuilding.util.TrimMultiplierLogic.setClientSyncedBase(payload.baseMultiplier());
            });
        });

        // Live Data (Hierhin verschoben von ModMessages)
        // Datapack tables of the server (chisel transformations, sledgehammer upgrades).
        ClientPlayNetworking.registerGlobalReceiver(com.simplebuilding.networking.DataTablesSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> com.simplebuilding.data.ModDataTables.receive(payload)));
        ClientPlayNetworking.registerGlobalReceiver(com.simplebuilding.networking.AmplifiedNotePayload.ID,
                (payload, context) -> context.client().execute(() -> com.simplebuilding.client.AmplifiedNoteSound.play(payload)));
        // Baukern: Erz-Umwandlung spielt die lange Hand-Animation (Nachtrag 11).
        ClientPlayNetworking.registerGlobalReceiver(com.simplebuilding.networking.CoreMotionPayload.ID,
                (payload, context) -> context.client().execute(() -> com.simplebuilding.client.CoreMotionClient.apply(payload)));
        ClientPlayNetworking.registerGlobalReceiver(SurvivalSyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.player() instanceof SurvivalTracerAccessor accessor) {
                    accessor.simplebuilding$setCurrentValues(
                            payload.currentDist(), payload.currentTime(),
                            payload.currentHostile(), payload.currentPassive(), payload.currentDamage()
                    );
                }
            });
        });
    }

    private void registerDoubleJumpClient() {
        // Shared air-jump + level-dependent cooldown logic (see DoubleJumpController).
        ClientTickEvents.END_CLIENT_TICK.register(DoubleJumpController::tick);
        com.simplebuilding.items.custom.VelocityGaugeItem.clientAutowalkToggle = com.simplebuilding.client.GaugeAutowalk::toggle;
        ClientTickEvents.END_CLIENT_TICK.register(com.simplebuilding.client.GaugeAutowalk::tick);
    }
}