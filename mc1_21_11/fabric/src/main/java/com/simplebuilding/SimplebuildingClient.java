package com.simplebuilding;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.Codec;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.client.gui.*;
import com.simplebuilding.client.gui.tooltip.ReinforcedBundleTooltipSubmenuHandler;
import com.simplebuilding.client.property.EnchantmentModelProperty;
import com.simplebuilding.client.render.BlockHighlightRenderer;
import com.simplebuilding.client.render.BlockOutlineSupport;
import com.simplebuilding.client.render.BuildingWandPreviewRenderer;
import com.simplebuilding.client.render.MultiBlockBreakingSupport;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.OctantItem;
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
import com.simplebuilding.entity.ModEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

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
        // Der getragene Rucksack auf dem Ruecken - auf jedem Avatar-Renderer (beide Spielermodelle, Mannequins).
        // Abgestellter gefaerbter Rucksack: Leder-Ebene in der Farbe der Block-Entity; die Ebenen
        // brauchen Cutout (26.2 erkennt das an den Texturen selbst, 1.21.11 nicht).
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register(
                com.simplebuilding.client.render.BackpackBlockTint.INSTANCE,
                com.simplebuilding.blocks.ModBlocks.BACKPACK, com.simplebuilding.blocks.ModBlocks.REINFORCED_BACKPACK,
                com.simplebuilding.blocks.ModBlocks.NETHERITE_BACKPACK, com.simplebuilding.blocks.ModBlocks.ENDERITE_BACKPACK);
        net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlocks(
                net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT,
                com.simplebuilding.blocks.ModBlocks.BACKPACK, com.simplebuilding.blocks.ModBlocks.REINFORCED_BACKPACK,
                com.simplebuilding.blocks.ModBlocks.NETHERITE_BACKPACK, com.simplebuilding.blocks.ModBlocks.ENDERITE_BACKPACK);
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
            if (renderer instanceof net.minecraft.client.renderer.entity.player.AvatarRenderer<?> avatar) {
                helper.register(new com.simplebuilding.client.render.BackpackLayer(avatar));
            }
        });
        ClientState.highlightToggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.simplebuilding.toggle_highlight",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            KEY_CATEGORY_SIMPLEMODS
        ));
        ClientState.octantFigureToggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.simplebuilding.toggle_octant_figure",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            KEY_CATEGORY_SIMPLEMODS
        ));
        ClientState.settingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.simplebuilding.simple_settings",
                GLFW.GLFW_KEY_G,
            KEY_CATEGORY_SIMPLEMODS
        ));
        ClientState.backpackKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.simplebuilding.open_backpack",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                KEY_CATEGORY_SIMPLEMODS
        ));
        // Rucksack-Taste: gemeinsamer Handler mit NeoForge (BackpackKeyHandler).
        ClientTickEvents.END_CLIENT_TICK.register(BackpackKeyHandler::tick);

        // --- Event Loop (Tick) ---
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (ClientState.highlightToggleKey.consumeClick()) {
                ClientState.showHighlights = !ClientState.showHighlights;
                if (client.player != null) {
                    client.player.displayClientMessage(Component.literal("Highlights: " + (ClientState.showHighlights ? "ON" : "OFF")), true);
                }
            }

            while (ClientState.octantFigureToggleKey.consumeClick()) {
                ClientState.showOctantFigure = !ClientState.showOctantFigure;
                if (client.player != null) {
                    client.player.displayClientMessage(Component.literal("Octant Figure: " + (ClientState.showOctantFigure ? "ON" : "OFF")), true);
                }
            }

            while (ClientState.settingsKey.consumeClick()) {
                if (client.player != null) {
                    ItemStack stack = client.player.getMainHandItem();
                    if (stack.getItem() instanceof OctantItem) {
                        client.setScreen(new OctantScreen(stack));
                    } else if (stack.getItem() instanceof BuildingWandItem) {
                        if (client.level != null && hasEnchantment(stack, client.level, ModEnchantments.CONSTRUCTORS_TOUCH)) {
                            client.setScreen(new BuildingWandScreen(stack));
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
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "air_jump_cooldown_hud"),
                (context, tickCounter) -> DoubleJumpHudOverlay.render(context)
        );
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, outlineRenderState) ->
                !BlockOutlineSupport.suppressVanillaBlockOutline());

        // --- Tooltips ---
        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof ReinforcedBundleTooltipData reinforcedData) {
                return ReinforcedBundleTooltips.create(reinforcedData);
            }
            return null;
        });

        registerDoubleJumpClient();

        // --- World Render ---
        // Geometrie wird nicht direkt gezeichnet, sondern über den SubmitNodeCollector
        // eingereicht (WorldRenderContext#commandQueue). 1.21.11 hat kein COLLECT_SUBMITS;
        // BEFORE_ENTITIES ist die letzte Phase, in der die Submit-Nodes noch gesammelt
        // werden ("before entity and block entities are submitted and drawn"). Beide
        // Renderer hängen in einem Handler, damit die Reihenfolge (Highlights vor
        // Wand-Vorschau) festgelegt bleibt.
        WorldRenderEvents.BEFORE_ENTITIES.register(context -> {
            BlockHighlightRenderer.renderInWorld(
                    context.commandQueue(),
                    context.matrices(),
                    Minecraft.getInstance().gameRenderer.getMainCamera()
            );
            BuildingWandPreviewRenderer.render(
                    context.commandQueue(),
                    context.matrices(),
                    context.worldState().cameraRenderState.pos
            );
        });

        // Abbau-Risse auf allen verbundenen Blöcken (Sledgehammer, Strip Miner, Vein Miner)
        WorldRenderEvents.END_EXTRACTION.register(context ->
                MultiBlockBreakingSupport.extractExtraBreakingStates(context.worldState(), context.world()));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            SimplebuildingConfig config = AutoConfig.getConfigHolder(SimplebuildingConfig.class).getConfig();
            boolean wantsBenefits = config.enableArmorTrimBenefits;

            // Paket senden
            ClientNetworking.send(new TrimBenefitPayload(wantsBenefits));
        });

        MenuScreens.register(ModScreenHandlers.NETHERITE_HOPPER_SCREEN_HANDLER, NetheriteHopperScreen::new);
        MenuScreens.register(ModScreenHandlers.BACKPACK_MENU, BackpackScreen::new);

        // --- NETZWERK REGISTRIERUNG CLIENT-SEITE ---
        registerClientReceivers();

        ENCHANTMENT_PROPERTY_TYPE = SelectItemModelProperty.Type.create(EnchantmentModelProperty.CODEC, Codec.STRING);
        EnchantmentModelProperty.PROPERTY_TYPE = ENCHANTMENT_PROPERTY_TYPE;
        SelectItemModelProperties.ID_MAPPER.put(
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "enchant_type"),
                ENCHANTMENT_PROPERTY_TYPE
        );

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
        // Sync Hopper Ghost Item
        ClientPlayNetworking.registerGlobalReceiver(SyncHopperGhostItemPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().level != null) {
                    if (context.client().level.getBlockEntity(payload.pos()) instanceof ModHopperBlockEntity blockEntity) {
                        blockEntity.setGhostItemClient(payload.slot(), payload.stack());
                    }
                }
            });
        });

        // Trim Data (Hierhin verschoben von ModMessages)
        ClientPlayNetworking.registerGlobalReceiver(TrimDataPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.player() instanceof SurvivalTracerAccessor accessor) {
                    accessor.simplebuilding$setBaseValues(
                            payload.baseDist(), payload.baseTime(),
                            payload.baseHostile(), payload.basePassive(), payload.baseDamage()
                    );
                }
            });
        });

        // Live Data (Hierhin verschoben von ModMessages)
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
    }
}