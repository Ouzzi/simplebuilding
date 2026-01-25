package com.simplebuilding;

import com.mojang.serialization.Codec;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.client.gui.*;
import com.simplebuilding.client.gui.tooltip.ReinforcedBundleTooltipSubmenuHandler;
import com.simplebuilding.client.property.EnchantmentModelProperty;
import com.simplebuilding.client.render.BlockHighlightRenderer;
import com.simplebuilding.client.render.BuildingWandOutlineRenderer;
import com.simplebuilding.client.render.SledgehammerOutlineRenderer;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import com.simplebuilding.networking.DoubleJumpPayload;
import com.simplebuilding.networking.ModMessages;
import com.simplebuilding.networking.SyncHopperGhostItemPayload;
import com.simplebuilding.networking.TrimBenefitPayload;
import com.simplebuilding.screen.ModScreenHandlers;
import com.simplebuilding.util.BundleTooltipAccessor;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.gui.tooltip.BundleTooltipComponent;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.item.model.SelectItemModel;
import net.minecraft.client.render.item.property.select.SelectProperties;
import net.minecraft.client.render.item.property.select.SelectProperty;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class SimplebuildingClient implements ClientModInitializer {

    private boolean jumpKeyPressed = false;
    private int jumpsUsed = 0;
    private boolean wasOnGround = true;

    // Tasten
    public static KeyBinding highlightToggleKey;
    public static boolean showHighlights = true;
    public static KeyBinding settingsKey;
    public static ReinforcedBundleTooltipSubmenuHandler BUNDLE_HANDLER;

    public static SelectProperty.Type<EnchantmentModelProperty, String> ENCHANTMENT_PROPERTY_TYPE;

    @Override
    public void onInitializeClient() {
        BUNDLE_HANDLER = new ReinforcedBundleTooltipSubmenuHandler(MinecraftClient.getInstance());

        // --- HUD & Renderer ---
        HudRenderCallback.EVENT.register(new RangefinderHudOverlay());
        HudRenderCallback.EVENT.register(new SpeedometerHudOverlay());
        SledgehammerOutlineRenderer.register();
        BuildingWandOutlineRenderer.register();

        // --- Tooltips ---
        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof ReinforcedBundleTooltipData reinforcedData) {
                BundleTooltipComponent component = new BundleTooltipComponent(reinforcedData.contents());
                float scale = (float) reinforcedData.maxCapacity() / 64.0f;
                ((BundleTooltipAccessor) component).simplebuilding$setCapacityScale(scale);
                return component;
            }
            return null;
        });


        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof ReinforcedBundleTooltipData bundleData) {
                // Nutze die Vanilla Bundle Komponente für die Anzeige
                return new BundleTooltipComponent(bundleData.contents());
            }
            return null;
        });


        registerDoubleJumpClient();

        // --- Keybindings Registrierung ---
        highlightToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simplebuilding.toggle_highlight",
                GLFW.GLFW_KEY_H,
                KeyBinding.Category.MISC
        ));
        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simplebuilding.simple_settings",
                GLFW.GLFW_KEY_G, // Standard G
                KeyBinding.Category.MISC
        ));

        // --- Event Loop (Tick) ---
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (highlightToggleKey.wasPressed()) {
                showHighlights = !showHighlights;
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Highlights: " + (showHighlights ? "ON" : "OFF")), true);
                }
            }

            while (settingsKey.wasPressed()) {
                if (client.player != null) {
                    ItemStack stack = client.player.getMainHandStack();
                    if (stack.getItem() instanceof OctantItem) {
                        client.setScreen(new OctantScreen(stack));
                    } else if (stack.getItem() instanceof BuildingWandItem) {
                        var registry = client.world.getRegistryManager();
                        var lookup = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
                        var entry = lookup.getOptional(ModEnchantments.CONSTRUCTORS_TOUCH);
                        if (entry.isPresent() && EnchantmentHelper.getLevel(entry.get(), stack) > 0) {
                            client.setScreen(new BuildingWandScreen(stack));
                        }
                    }
                }
            }
        });

        // --- World Render ---
        WorldRenderEvents.END_MAIN.register(context -> {
            BlockHighlightRenderer.render(
                    context.matrices().peek().getPositionMatrix(),
                    MinecraftClient.getInstance().gameRenderer.getCamera()
            );
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            SimplebuildingConfig config = AutoConfig.getConfigHolder(SimplebuildingConfig.class).getConfig();
            boolean wantsBenefits = config.enableArmorTrimBenefits;

            // Paket senden
            ClientPlayNetworking.send(new TrimBenefitPayload(wantsBenefits));
        });

        HandledScreens.register(ModScreenHandlers.NETHERITE_HOPPER_SCREEN_HANDLER, NetheriteHopperScreen::new);

        // --- NETZWERK REGISTRIERUNG CLIENT-SEITE (WICHTIG!) ---

        // Jetzt den Receiver registrieren
        ClientPlayNetworking.registerGlobalReceiver(SyncHopperGhostItemPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().world != null) {
                    if (context.client().world.getBlockEntity(payload.pos()) instanceof ModHopperBlockEntity blockEntity) {
                        blockEntity.setGhostItemClient(payload.slot(), payload.stack());
                    }
                }
            });
        });

        ENCHANTMENT_PROPERTY_TYPE = SelectProperty.Type.create(
                EnchantmentModelProperty.CODEC, // Dein MapCodec
                Codec.STRING                    // Der Wert-Codec (String für "vein_miner" etc.)
        );
        SelectProperties.ID_MAPPER.put(
                Identifier.of(Simplebuilding.MOD_ID, "enchant_type"),
                ENCHANTMENT_PROPERTY_TYPE
        );

    }

    private void registerDoubleJumpClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || !Simplebuilding.getConfig().enableDoubleJump) return;

            boolean isOnGround = client.player.isOnGround();
            boolean isClimbing = client.player.isClimbing();
            boolean isInWater = client.player.isTouchingWater();

            if (isOnGround || isClimbing || isInWater) {
                jumpsUsed = 0;
            } else {
                boolean jumping = client.options.jumpKey.isPressed();
                if (jumping && !jumpKeyPressed && !wasOnGround) {
                    var registry = client.player.getEntityWorld().getRegistryManager();
                    var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
                    var doubleJump = enchantments.getOptional(ModEnchantments.DOUBLE_JUMP);

                    if (doubleJump.isPresent()) {
                        int level = EnchantmentHelper.getEquipmentLevel(doubleJump.get(), client.player);
                        if (level > 0 && jumpsUsed < level && !client.player.getAbilities().flying) {
                            Vec3d velocity = client.player.getVelocity();
                            client.player.setVelocity(velocity.x, 0.5, velocity.z);
                            client.player.fallDistance = 0;
                            jumpsUsed++;
                            ClientPlayNetworking.send(new DoubleJumpPayload());
                        }
                    }
                }
            }
            jumpKeyPressed = client.options.jumpKey.isPressed();
            wasOnGround = isOnGround;
        });
    }
}