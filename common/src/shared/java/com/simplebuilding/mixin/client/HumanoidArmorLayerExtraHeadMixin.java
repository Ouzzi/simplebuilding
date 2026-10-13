package com.simplebuilding.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.client.render.ExtraHeadLayer;
import com.simplebuilding.util.ExtraHead;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Owner N49: a helmet over the head in the extra head slot ({@link ExtraHeadLayer}) is scaled around the neck pivot by
 * the head's {@link ExtraHead.Fit#helmetScale} - a uniform scale around the pivot follows every head rotation - so it
 * encloses the pumpkin or skull, stays visible and never shares a face with it (no z-fighting).
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerExtraHeadMixin<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
    @Unique
    private boolean simplebuilding$scaledHelmet;

    protected HumanoidArmorLayerExtraHeadMixin(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    @Inject(method = "renderArmorPiece", at = @At("HEAD"))
    private void simplebuilding$scaleHelmetOverHead(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack, EquipmentSlot slot,
                                                    int light, HumanoidRenderState state, CallbackInfo ci) {
        this.simplebuilding$scaledHelmet = false;
        if (slot != EquipmentSlot.HEAD || !(state instanceof AvatarRenderState avatar)) {
            return;
        }
        ItemStack head = ExtraHeadLayer.shownHead(avatar);
        float scale = head.isEmpty() ? 1.0F : ExtraHead.fit(head).helmetScale();
        if (scale == 1.0F) {
            return;
        }
        ModelPart pivot = this.getParentModel().head;
        poseStack.pushPose();
        poseStack.translate(pivot.x / 16.0F, pivot.y / 16.0F, pivot.z / 16.0F);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-pivot.x / 16.0F, -pivot.y / 16.0F, -pivot.z / 16.0F);
        this.simplebuilding$scaledHelmet = true;
    }

    @Inject(method = "renderArmorPiece", at = @At("RETURN"))
    private void simplebuilding$restoreHelmetScale(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack, EquipmentSlot slot,
                                                   int light, HumanoidRenderState state, CallbackInfo ci) {
        if (this.simplebuilding$scaledHelmet) {
            this.simplebuilding$scaledHelmet = false;
            poseStack.popPose();
        }
    }
}
