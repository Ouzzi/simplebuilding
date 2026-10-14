package com.simplemobheads.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplemobheads.MobHeadType;
import com.simplemobheads.client.MobHeadModels;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Owner N55: a worn mob head is as big as the head on the mob's own model. Vanilla draws every worn skull at 1.1875 x its
 * model so that it covers the wearer's head; a mod head replaces that head ({@code WornModHeadMixin}), so it only needs the
 * mob's own size ({@link MobHeadModels#worn}, never bigger than before). The scale runs uniformly around the head pivot
 * (the neck), which follows every head rotation and keeps the head's foot on the neck. Vanilla skulls are not touched.
 */
@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerMixin<S extends LivingEntityRenderState, M extends EntityModel<S> & HeadedModel> extends RenderLayer<S, M> {
    @Unique
    private boolean simplemobheads$scaled;

    protected CustomHeadLayerMixin(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At("HEAD"))
    private void simplemobheads$mobHeadSize(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot,
                                            CallbackInfo ci) {
        this.simplemobheads$scaled = false;
        if (!(state.wornHeadType instanceof MobHeadType type)) {
            return;
        }
        float factor = MobHeadModels.worn(type)[0];
        if (factor == 1.0F) {
            return;
        }
        ModelPart pivot = this.getParentModel().getHead();
        poseStack.pushPose();
        poseStack.translate(pivot.x / 16.0F, pivot.y / 16.0F, pivot.z / 16.0F);
        poseStack.scale(factor, factor, factor);
        poseStack.translate(-pivot.x / 16.0F, -pivot.y / 16.0F, -pivot.z / 16.0F);
        this.simplemobheads$scaled = true;
    }

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At("RETURN"))
    private void simplemobheads$restoreSize(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot,
                                            CallbackInfo ci) {
        if (this.simplemobheads$scaled) {
            this.simplemobheads$scaled = false;
            poseStack.popPose();
        }
    }
}
