package com.simplebuilding.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.simplebuilding.client.effect.PerceptionClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Faded (queue N20): the grey post effect {@code simplebuilding:faded} (Vanilla's colour convolve shader with a grey
 * matrix). In play, inventories and chat it runs after the GUI, so world, HUD and inventories are grey; while a menu
 * (Esc, options) is open it runs after the world's own post effects and before the GUI, so the menu keeps its colours.
 */
@Mixin(GameRenderer.class)
public abstract class FadedGameRendererMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private RenderTarget mainRenderTarget;
    @Shadow @Final private CrossFrameResourcePool resourcePool;

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;applyPostEffects()V", shift = At.Shift.AFTER))
    private void simplebuilding$fadeWorld(CallbackInfo ci) {
        if (PerceptionClient.faded() && PerceptionClient.menuOpen()) simplebuilding$fade();
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V", shift = At.Shift.AFTER))
    private void simplebuilding$fadeAll(CallbackInfo ci) {
        if (PerceptionClient.faded() && !PerceptionClient.menuOpen()) simplebuilding$fade();
    }

    @Unique
    private void simplebuilding$fade() {
        PostChain chain = this.minecraft.getShaderManager().getPostChain(PerceptionClient.FADED_EFFECT, LevelTargetBundle.MAIN_TARGETS);
        if (chain != null) {
            chain.process(this.mainRenderTarget, this.resourcePool);
        }
    }
}
