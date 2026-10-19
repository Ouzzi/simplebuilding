package com.simplebuilding.modules.simpleoptrender.mixin.more_culling;

import com.simplebuilding.modules.simpleoptrender.MoreCulling;
import com.simplebuilding.modules.simpleoptrender.RenderOpt;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** O3: an item frame seen from behind its opaque supporting block is not drawn (MoreCulling#behindFrame); glowing ones stay. */
@Mixin(EntityRenderer.class)
abstract class EntityRendererMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void simpleoptrender$frameBehindWall(Entity entity, Frustum frustum, double camX, double camY, double camZ, float partialTicks,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof ItemFrame frame) || !RenderOpt.live(RenderOpt.MORE_CULLING) || !RenderOpt.culling().itemFrames()
                || frame.isCurrentlyGlowing()) return;
        Direction facing = frame.getDirection();
        if (MoreCulling.behindFrame(camX - frame.getX(), camY - frame.getY(), camZ - frame.getZ(), facing.getStepX(), facing.getStepY(), facing.getStepZ())
                && frame.level().getBlockState(frame.getPos().relative(facing.getOpposite())).isSolidRender())
            cir.setReturnValue(false);
    }
}
