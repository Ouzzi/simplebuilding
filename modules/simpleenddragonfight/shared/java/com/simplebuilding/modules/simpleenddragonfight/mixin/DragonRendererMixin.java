package com.simplebuilding.modules.simpleenddragonfight.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.modules.simpleenddragonfight.DragonTinted;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * WI26-6 (variant A, chorus violet): a tinted dragon gets a second, glowing pass of the Vanilla dragon texture in
 * chorus violet. Nothing is shipped: the texture is Vanilla's, the colour is applied at run time.
 */
@Mixin(EnderDragonRenderer.class)
public abstract class DragonRendererMixin {
    /** Chorus violet, alpha = strength of the additive glow pass (variant A of the preview). */
    private static final int SIMPLEENDDRAGONFIGHT_TINT = 0x66B070E0;
    /** Soul blue (level 2 pulses between this and the violet, so both colours are never missing). */
    private static final int SIMPLEENDDRAGONFIGHT_SOUL = 0x5CC8FF;
    private static final Identifier SIMPLEENDDRAGONFIGHT_TEXTURE = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png");

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void simpleenddragonfight$extract(EnderDragon entity, EnderDragonRenderState state, float partialTicks, CallbackInfo ci) {
        ((DragonTinted) state).simpleenddragonfight$setTintLevel(((DragonTinted) entity).simpleenddragonfight$tintLevel());
    }

    /** The eyes pass is the only RenderType-overload submitModel in submit(); the violet glow pass goes right behind it. */
    @WrapOperation(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;III)V"))
    private void simpleenddragonfight$glow(SubmitNodeCollector collector, Model model, Object state, PoseStack poseStack, RenderType type, int light, int overlay, int outline, Operation<Void> original) {
        original.call(collector, model, state, poseStack, type, light, overlay, outline);
        if (state instanceof EnderDragonRenderState s && s.deathTime <= 0.0F) {
            int level = ((DragonTinted) s).simpleenddragonfight$tintLevel();
            if (level <= 0) return;
            RenderType glow = RenderTypes.eyes(SIMPLEENDDRAGONFIGHT_TEXTURE);
            if (level == 1) {
                collector.submitModel(model, state, poseStack, glow, light, OverlayTexture.NO_OVERLAY, SIMPLEENDDRAGONFIGHT_TINT, null, outline);
                return;
            }
            // level 2 (Ender Gem fight): violet and soul blue glow in turn, a two-coloured pulse
            float w = 0.5F + 0.5F * (float) Math.sin(s.ageInTicks * 0.12F);
            int violet = ((int) (0x66 * (1.0F - w * 0.7F)) << 24) | 0xB070E0;
            int blue = ((int) (0x66 * (0.3F + w * 0.7F)) << 24) | SIMPLEENDDRAGONFIGHT_SOUL;
            collector.submitModel(model, state, poseStack, glow, light, OverlayTexture.NO_OVERLAY, violet, null, outline);
            collector.submitModel(model, state, poseStack, glow, light, OverlayTexture.NO_OVERLAY, blue, null, outline);
        }
    }
}
