package com.simplebuilding.modules.simplemobs.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.modules.simplemobs.client.DisguiseLooks;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cloak of deception (owner 12.10.): in first person the hands show the imitated shape, not the own skin. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandDisguiseMixin {
    @Inject(method = "renderPlayerHand", at = @At("HEAD"), cancellable = true, require = 0)
    private void simplemobs$disguisedHand(PoseStack poseStack, SubmitNodeCollector collector, int light, HumanoidArm arm,
            PlayerRenderState state, CallbackInfo ci) {
        if (DisguiseLooks.renderHand(poseStack, collector, light, arm)) ci.cancel();
    }
}
