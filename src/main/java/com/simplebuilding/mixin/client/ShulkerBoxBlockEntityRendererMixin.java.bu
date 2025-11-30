package com.simplebuilding.mixin.client;

import com.simplebuilding.util.IEnchantableRenderState;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.ShulkerBoxBlockEntityRenderer;
import net.minecraft.client.render.block.entity.state.ShulkerBoxBlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(ShulkerBoxBlockEntityRenderer.class)
public class ShulkerBoxBlockEntityRendererMixin {

    private Model cachedModel;

    @Inject(method = "render(Lnet/minecraft/client/render/block/entity/state/ShulkerBoxBlockEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("HEAD")) // Wir holen das Modell ganz am Anfang
    private void captureModelAndRenderGlint(ShulkerBoxBlockEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, net.minecraft.client.render.state.CameraRenderState camera, CallbackInfo ci) {

        if (state instanceof IEnchantableRenderState enchantableState && enchantableState.simplebuilding$isEnchanted()) {

            // Reflection, um das private Feld 'model' zu holen (nur einmalig nötig)
            if (this.cachedModel == null) {
                try {
                    // Wir suchen nach dem Feld vom Typ 'ShulkerBoxBlockModel' (oder Model)
                    for (Field field : ShulkerBoxBlockEntityRenderer.class.getDeclaredFields()) {
                        if (field.getType().getName().contains("ShulkerBoxBlockModel") || Model.class.isAssignableFrom(field.getType())) {
                            field.setAccessible(true);
                            this.cachedModel = (Model) field.get(this);
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (this.cachedModel != null) {
                // GLINT RENDERN
                // Wir müssen das Modell manuell transformieren, da wir VOR dem eigentlichen Render sind?
                // NEIN! Wir müssen NACH dem eigentlichen Render injecten (TAIL), aber wir brauchen das Modell.
                // Da wir es jetzt gecached haben, können wir bei TAIL rendern.
            }
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/render/block/entity/state/ShulkerBoxBlockEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("TAIL"))
    private void renderGlintAtTail(ShulkerBoxBlockEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, net.minecraft.client.render.state.CameraRenderState camera, CallbackInfo ci) {
        if (this.cachedModel != null && state instanceof IEnchantableRenderState enchantableState && enchantableState.simplebuilding$isEnchanted()) {
            queue.submitModel(
                    this.cachedModel,
                    state.animationProgress,
                    matrices,
                    RenderLayer.getEntityGlint(),
                    state.lightmapCoordinates,
                    OverlayTexture.DEFAULT_UV,
                    -1,
                    null,
                    0,
                    null
            );
        }
    }
}