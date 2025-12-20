package com.simplebuilding.mixin.client;

import com.simplebuilding.items.custom.ChiselItem;
import me.shedaniel.autoconfig.AutoConfig;
import com.simplebuilding.config.SimplebuildingConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Shadow @Final private MinecraftClient client;

    // Wir speichern den Animations-Fortschritt (0.0 bis 1.0) für beide Hände
    @Unique private float mainHandChiselProgress = 0.0F;
    @Unique private float offHandChiselProgress = 0.0F;

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"
            )
    )
    private void onRenderFirstPersonItem(
            AbstractClientPlayerEntity player,
            float tickProgress,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            MatrixStack matrices,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            int light,
            CallbackInfo ci
    ) {
        // 1. Config Check
        SimplebuildingConfig config = AutoConfig.getConfigHolder(SimplebuildingConfig.class).getConfig();
        // Wenn Animationen aus sind, verhalten wir uns so, als wäre das Ziel immer 0
        boolean animationsEnabled = config.tools.enableToolAnimations && config.tools.enableChiselAnimation;

        // 2. Ziel-Zustand ermitteln (1.0 = Aktiv/Valid, 0.0 = Inaktiv)
        float targetProgress = 0.0F;

        if (animationsEnabled && item.getItem() instanceof ChiselItem chiselItem) {
            HitResult hit = this.client.crosshairTarget;
            if (hit instanceof BlockHitResult blockHit) {
                // Prüfen ob der Block gechiselt werden kann
                if (chiselItem.canChisel(this.client.world, blockHit.getBlockPos(), item, player)) {
                    targetProgress = 1.0F;
                }
            }
        }

        // 3. Interpolation (Smoothness)
        // Wir nähern den aktuellen Wert langsam dem Zielwert an.
        // 0.15F bestimmt die Geschwindigkeit (höher = schneller).
        float smoothingSpeed = 0.15F;

        if (hand == Hand.MAIN_HAND) {
            this.mainHandChiselProgress += (targetProgress - this.mainHandChiselProgress) * smoothingSpeed;
            // Transformation anwenden, wenn Fortschritt sichtbar ist (> 0.001)
            if (this.mainHandChiselProgress > 0.001F) {
                this.applyChiselTransform(matrices, this.mainHandChiselProgress);
            }
        } else {
            this.offHandChiselProgress += (targetProgress - this.offHandChiselProgress) * smoothingSpeed;
            if (this.offHandChiselProgress > 0.001F) {
                this.applyChiselTransform(matrices, this.offHandChiselProgress);
            }
        }
    }

    /**
     * Wendet die Rotation und Position basierend auf dem Fortschritt an.
     */
    @Unique
    private void applyChiselTransform(MatrixStack matrices, float progress) {
        // Die Werte werden mit 'progress' multipliziert, um von 0 bis zum vollen Effekt zu blenden
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-15.0F * progress));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F * progress));
        matrices.translate(0.05 * progress, 0.05 * progress, 0.05 * progress);
    }
}