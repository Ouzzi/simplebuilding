package com.simplebuilding;

import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.client.gui.*;
import com.simplebuilding.client.render.BlockHighlightRenderer;
import com.simplebuilding.client.render.BuildingWandOutlineRenderer;
import com.simplebuilding.client.render.SledgehammerOutlineRenderer;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import com.simplebuilding.networking.*;
import com.simplebuilding.screen.ModScreenHandlers;
import com.simplebuilding.util.BundleTooltipAccessor;
import com.simplebuilding.util.SurvivalTracerAccessor;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.gui.tooltip.BundleTooltipComponent;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import static com.simplebuilding.util.EnchantmentHelper.getEnchantmentLevel;
import static com.simplebuilding.util.EnchantmentHelper.hasEnchantment;

public class SimplebuildingClient implements ClientModInitializer {

    private boolean jumpKeyPressed = false;
    private int jumpsUsed = 0;
    private boolean wasOnGround = true;

    // Tasten
    public static final KeyBinding.Category KEY_CATEGORY_SIMPLEMODS = KeyBinding.Category.create(new Identifier(Simplebuilding.MOD_ID, "simplemods"));
    public static KeyBinding highlightToggleKey;
    public static KeyBinding octantFigureToggleKey;
    public static boolean showHighlights = true;
    public static KeyBinding settingsKey;
    private boolean wasJumpPressed = false;

    @Override
    @SuppressWarnings("deprecation")
    public void onInitializeClient() {
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

        // Dieser zweite Handler ist redundant, da der erste bereits greift, wenn data instanceof ReinforcedBundleTooltipData ist.
        // Ich lasse ihn hier stehen, falls du eine Fallback-Logik hattest, aber eigentlich reicht einer.
        /* TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof ReinforcedBundleTooltipData bundleData) {
                // Nutze die Vanilla Bundle Komponente fÃ¼r die Anzeige
                return new BundleTooltipComponent(bundleData.contents());
            }
            return null;
        });
        */

        registerDoubleJumpClient();

        // --- Keybindings Registrierung ---
        highlightToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simplebuilding.toggle_highlight",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            KEY_CATEGORY_SIMPLEMODS
        ));
        octantFigureToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.simplebuilding.toggle_octant_figure",
            InputUtil.Type.KEYSYM,
            InputUtil.UNKNOWN_KEY.getCode(),
            KEY_CATEGORY_SIMPLEMODS
        ));
        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simplebuilding.simple_settings",
                GLFW.GLFW_KEY_G, // Standard G
            KEY_CATEGORY_SIMPLEMODS
        ));

        // --- Event Loop (Tick) ---
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (highlightToggleKey.wasPressed()) {
                showHighlights = !showHighlights;
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Highlights: " + (showHighlights ? "ON" : "OFF")), true);
                }
            }

            while (octantFigureToggleKey.wasPressed()) {
                showHighlights = !showHighlights;
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Octant Figure: " + (showHighlights ? "ON" : "OFF")), true);
                }
            }

            while (settingsKey.wasPressed()) {
                if (client.player != null) {
                    ItemStack stack = client.player.getMainHandStack();
                    if (stack.getItem() instanceof OctantItem) {
                        client.setScreen(new OctantScreen(stack));
                    } else if (stack.getItem() instanceof BuildingWandItem) {
                        if (client.world != null && hasEnchantment(stack, client.world, ModEnchantments.CONSTRUCTORS_TOUCH)) {
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

        // --- NETZWERK REGISTRIERUNG CLIENT-SEITE ---
        registerClientReceivers();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                boolean isJumpPressed = client.options.jumpKey.isPressed();

                // Nur senden, wenn sich der Status Ã¤ndert (Bandbreite sparen)
                if (isJumpPressed != wasJumpPressed) {
                    ClientPlayNetworking.send(new SpaceKeyPayload(isJumpPressed));
                    wasJumpPressed = isJumpPressed;
                }
            }
        });
    }

    private void registerClientReceivers() {
        // Sync Hopper Ghost Item
        ClientPlayNetworking.registerGlobalReceiver(SyncHopperGhostItemPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().world != null) {
                    if (context.client().world.getBlockEntity(payload.pos()) instanceof ModHopperBlockEntity blockEntity) {
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
                    int level = getDoubleJumpLevel(client.player);
                    if (level > 0 && jumpsUsed < level && !client.player.getAbilities().flying) {
                        Vec3d velocity = client.player.getVelocity();
                        client.player.setVelocity(velocity.x, 0.5, velocity.z);
                        client.player.fallDistance = 0;
                        jumpsUsed++;
                        ClientPlayNetworking.send(new DoubleJumpPayload());
                    }
                }
            }
            jumpKeyPressed = client.options.jumpKey.isPressed();
            wasOnGround = isOnGround;
        });
    }

    private static int getDoubleJumpLevel(net.minecraft.entity.player.PlayerEntity player) {
        int maxLevel = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack equipped = player.getEquippedStack(slot);
            int level = getEnchantmentLevel(equipped, player.getEntityWorld(), ModEnchantments.DOUBLE_JUMP);
            if (level > maxLevel) {
                maxLevel = level;
            }
        }
        return maxLevel;
    }
}
