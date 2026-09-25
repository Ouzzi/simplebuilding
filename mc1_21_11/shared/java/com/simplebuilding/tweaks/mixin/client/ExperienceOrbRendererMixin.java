package com.simplebuilding.tweaks.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.client.OrbValueHolder;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ExperienceOrbRenderer;
import net.minecraft.client.renderer.entity.state.ExperienceOrbRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** XP-Kugeln nach Wert vergroessern (Simple Tweaks: ExperienceOrbRendererMixin, Config scaleXpOrbs). */
@Mixin(ExperienceOrbRenderer.class)
public class ExperienceOrbRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/ExperienceOrb;Lnet/minecraft/client/renderer/entity/state/ExperienceOrbRenderState;F)V",
            at = @At("TAIL"))
    private void simplebuilding$captureValue(ExperienceOrb entity, ExperienceOrbRenderState state, float partialTicks, CallbackInfo ci) {
        ((OrbValueHolder) state).simplebuilding$setOrbValue(entity.getValue());
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ExperienceOrbRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD"))
    private void simplebuilding$scaleOrb(ExperienceOrbRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                                         CameraRenderState camera, CallbackInfo ci) {
        if (SimpleTweaks.config().optimization.scaleXpOrbs) {
            float scale = OrbValueHolder.scaleFor(((OrbValueHolder) state).simplebuilding$getOrbValue());
            poseStack.scale(scale, scale, scale);
        }
    }
}
