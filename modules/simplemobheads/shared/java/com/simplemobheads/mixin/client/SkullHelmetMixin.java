package com.simplemobheads.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplemobheads.MobHeads;
import com.simplemobheads.client.HeadHelmetRender;
import com.simplemobheads.client.HelmetState;
import com.simplemobheads.helmet.HeadHelmets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.blockentity.state.SkullBlockRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** BS28b-1: a placed head draws the helmet it wears (components of the block entity) over its model, in the head's own pose. */
@Mixin(SkullBlockRenderer.class)
public abstract class SkullHelmetMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/SkullBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/SkullBlockRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At("TAIL"))
    private void simplemobheads$helmet(SkullBlockEntity entity, SkullBlockRenderState state, float partialTicks, Vec3 camera,
                                       ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress, CallbackInfo ci) {
        ItemStack helmet = entity.components().get(MobHeads.HELMET);
        ((HelmetState) state).simplemobheads$setHelmet(helmet == null ? ItemStack.EMPTY : helmet,
                helmet == null ? 0.0F : HeadHelmets.size(entity.getBlockState()));
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/SkullBlockRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/client/renderer/blockentity/SkullBlockRenderer;submitSkull(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/model/object/skull/SkullModelBase;Lnet/minecraft/client/renderer/rendertype/RenderType;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
    private void simplemobheads$drawHelmet(SkullBlockRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                                           CameraRenderState camera, CallbackInfo ci) {
        HelmetState helmet = (HelmetState) state;
        if (!helmet.simplemobheads$helmet().isEmpty() && helmet.simplemobheads$helmetSize() > 0.0F) {
            HeadHelmetRender.draw(helmet.simplemobheads$helmet(), helmet.simplemobheads$helmetSize(), poseStack, collector, state.lightCoords);
        }
    }
}
