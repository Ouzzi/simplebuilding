package com.simplebuilding;

import com.simplebuilding.client.gui.OctantScreen;
import com.simplebuilding.client.gui.RangefinderHudOverlay;
import com.simplebuilding.client.gui.SpeedometerHudOverlay;
import com.simplebuilding.client.render.BlockHighlightRenderer;
import com.simplebuilding.client.render.BuildingWandOutlineRenderer;
import com.simplebuilding.client.render.SledgehammerOutlineRenderer;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import com.simplebuilding.networking.DoubleJumpPayload;
import com.simplebuilding.util.BundleTooltipAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.BundleTooltipComponent;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class SimplebuildingClient implements ClientModInitializer {

    private boolean jumpKeyPressed = false;
    private int jumpsUsed = 0;
    private boolean wasOnGround = true;

    // Tasten
    public static KeyBinding highlightToggleKey;
    public static boolean showHighlights = true;
    public static KeyBinding configureOctantKey;

    @Override
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


        registerDoubleJumpClient();

        // --- Keybindings Registrierung ---

        // 1. Highlight Toggle (Taste H)
        highlightToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simplebuilding.toggle_highlight",
                GLFW.GLFW_KEY_H,
                KeyBinding.Category.MISC
        ));

        // 2. Octant Config (Taste G) - NEU
        configureOctantKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simplebuilding.configure_octant",
                GLFW.GLFW_KEY_G, // Standard G
                KeyBinding.Category.MISC
        ));

        // --- Event Loop (Tick) ---
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            // Logik für Highlight Toggle
            while (highlightToggleKey.wasPressed()) {
                showHighlights = !showHighlights;
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Highlights: " + (showHighlights ? "ON" : "OFF")), true);
                }
            }

            // Logik für Octant Menü (Taste G)
            while (configureOctantKey.wasPressed()) {
                System.out.println("DEBUG: Taste G wurde gedrückt!"); // Check Konsole!

                if (client.player != null) {
                    var stack = client.player.getMainHandStack();
                    System.out.println("DEBUG: Item in Hand: " + stack.getItem().getName().getString());

                    // Prüfung: Ist das Item ein Octant?
                    if (stack.getItem() instanceof OctantItem) {
                        System.out.println("DEBUG: Es ist ein Octant! Öffne GUI...");
                        // Öffne den Screen
                        client.setScreen(new OctantScreen(stack));
                    } else {
                        System.out.println("DEBUG: KEIN Octant erkannt.");
                        client.player.sendMessage(Text.literal("Du musst einen Octant halten!"), true);
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