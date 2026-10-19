package com.simplebuilding.modules.simpleoptrender.mixin.more_culling;

import com.simplebuilding.modules.simpleoptrender.MoreCulling;
import com.simplebuilding.modules.simpleoptrender.RenderOpt;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** O3: the text side facing away from the camera is not extracted (MoreCulling#signSides; the opaque board hides it). */
@Mixin(AbstractSignRenderer.class)
abstract class AbstractSignRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At("TAIL"))
    private void simpleoptrender$cullHiddenText(SignBlockEntity sign, SignRenderState state, float partialTicks, Vec3 camera,
                                                ModelFeatureRenderer.CrumblingOverlay breakProgress, CallbackInfo ci) {
        if (!RenderOpt.live(RenderOpt.MORE_CULLING) || !RenderOpt.culling().signText()) return;
        BlockState blockState = sign.getBlockState();
        if (!(blockState.getBlock() instanceof SignBlock block)) return;
        Vec3 center = block.getSignHitboxCenterPosition(blockState);
        double dx = camera.x() - (sign.getBlockPos().getX() + center.x());
        double dz = camera.z() - (sign.getBlockPos().getZ() + center.z());
        switch (MoreCulling.signSides(dx, dz, block.getYRotationDegrees(blockState))) {
            case FRONT -> state.backText = null;
            case BACK -> state.frontText = null;
            case BOTH -> { }
        }
    }
}
