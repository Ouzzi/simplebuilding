package com.simplebuilding;

import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.client.gui.RangefinderHudOverlay;
import com.simplebuilding.client.gui.SpeedometerHudOverlay;
import com.simplebuilding.client.render.BuildingWandOutlineRenderer;
import com.simplebuilding.client.render.ModChestBlockEntityRenderer;
import com.simplebuilding.client.render.SledgehammerOutlineRenderer;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import com.simplebuilding.networking.DoubleJumpPayload;
import com.simplebuilding.util.BundleTooltipAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.client.gui.tooltip.BundleTooltipComponent;
import net.minecraft.client.render.block.entity.ChestBlockEntityRenderer;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.Vec3d;

public class SimplebuildingClient implements ClientModInitializer {

    private boolean jumpKeyPressed = false;
    private int jumpsUsed = 0; // Zähler für Sprünge in der Luft
    private boolean wasOnGround = true; // Status des Spielers im vorherigen Tick

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(new RangefinderHudOverlay());
        HudRenderCallback.EVENT.register(new SpeedometerHudOverlay());

        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof ReinforcedBundleTooltipData reinforcedData) {
                BundleTooltipComponent component = new BundleTooltipComponent(reinforcedData.contents());
                float scale = (float) reinforcedData.maxCapacity() / 64.0f;
                ((BundleTooltipAccessor) component).simplebuilding$setCapacityScale(scale);
                return component;
            }
            return null;
        });

        SledgehammerOutlineRenderer.register();
        BuildingWandOutlineRenderer.register();

        registerDoubleJumpClient();

        BlockEntityRendererRegistry.register(ModBlockEntities.MOD_CHEST_BE, ChestBlockEntityRenderer::new);
        //BlockEntityRendererRegistry.register(ModBlockEntities.MOD_CHEST_BE, ModChestBlockEntityRenderer::new);
    }

    private void registerDoubleJumpClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            boolean isOnGround = client.player.isOnGround();
            boolean isClimbing = client.player.isClimbing();
            boolean isInWater = client.player.isTouchingWater();

            // Reset Zähler wenn am Boden, kletternd oder im Wasser
            if (isOnGround || isClimbing || isInWater) {
                jumpsUsed = 0;
            } else {
                // Input check
                boolean jumping = client.options.jumpKey.isPressed();

                // Logik:
                // 1. Taste wurde gerade frisch gedrückt (!jumpKeyPressed)
                // 2. Spieler war im VORHERIGEN Tick NICHT am Boden (!wasOnGround).
                //    -> Das verhindert, dass der normale Sprung (Boden -> Luft) sofort als Doppelsprung zählt.
                if (jumping && !jumpKeyPressed && !wasOnGround) {
                    var registry = client.player.getEntityWorld().getRegistryManager();
                    var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
                    var doubleJump = enchantments.getOptional(ModEnchantments.DOUBLE_JUMP);

                    if (doubleJump.isPresent()) {
                        int level = EnchantmentHelper.getEquipmentLevel(doubleJump.get(), client.player);

                        // Wenn Level > 0 und wir noch Sprünge übrig haben
                        if (level > 0 && jumpsUsed < level && !client.player.getAbilities().flying) {

                            // Logik für den Sprung
                            Vec3d velocity = client.player.getVelocity();
                            client.player.setVelocity(velocity.x, 0.5, velocity.z);

                            // Fallschaden-Zähler clientseitig resetten für Smoothness
                            client.player.fallDistance = 0;

                            // Zähler erhöhen
                            jumpsUsed++;

                            // Paket an Server senden (für Fallschaden-Reset & Durability)
                            ClientPlayNetworking.send(new DoubleJumpPayload());
                        }
                    }
                }
            }

            // Status für den nächsten Tick speichern
            jumpKeyPressed = client.options.jumpKey.isPressed();
            wasOnGround = isOnGround;
        });
    }
}
