package com.simplebuilding.modules.simplemobs.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.modules.simplemobs.client.CloakLayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The cloak of deception covers a skin cape (both would hang in the same place). */
@Mixin(CapeLayer.class)
public abstract class CapeLayerCloakMixin {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void simplemobs$cloakCoversCape(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state,
            float yRot, float xRot, CallbackInfo ci) {
        if (CloakLayer.wears(state)) ci.cancel();
    }
}
