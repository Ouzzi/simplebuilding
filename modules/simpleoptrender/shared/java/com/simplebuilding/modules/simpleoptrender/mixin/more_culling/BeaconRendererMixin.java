package com.simplebuilding.modules.simpleoptrender.mixin.more_culling;

import com.simplebuilding.modules.simpleoptrender.MoreCulling;
import com.simplebuilding.modules.simpleoptrender.RenderOpt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** O3: beams are rendered globally without a view-cone check; a beam column outside the cull frustum is skipped. */
@Mixin(BeaconRenderer.class)
abstract class BeaconRendererMixin {
    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
    private void simpleoptrender$beamInView(BlockEntity beacon, Vec3 camera, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || !RenderOpt.live(RenderOpt.MORE_CULLING) || !RenderOpt.culling().beaconBeams()) return;
        Frustum frustum = Minecraft.getInstance().gameRenderer.mainCamera().getCullFrustum();
        BlockPos p = beacon.getBlockPos();
        if (frustum != null && !frustum.isVisible(new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + 1, MoreCulling.BEAM_TOP, p.getZ() + 1)))
            cir.setReturnValue(false);
    }
}
