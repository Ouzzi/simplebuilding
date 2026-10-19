package com.simplebuilding.modules.simpleoptrender.mixin.leaf_culling;

import com.simplebuilding.modules.simpleoptrender.LeafCulling;
import com.simplebuilding.modules.simpleoptrender.RenderOpt;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** O1: the per-face cull check of the Vanilla block mesher also hides leaf faces enclosed by leaves (LeafCulling). */
@Mixin(ModelBlockRenderer.class)
abstract class ModelBlockRendererMixin {
    @Shadow @Final private boolean cull;

    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private void simpleoptrender$cullLeaves(BlockAndTintGetter level, BlockState state, Direction direction, BlockPos neighborPos,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (cull && RenderOpt.live(RenderOpt.LEAF_CULLING) && LeafCulling.cull(level, state, direction, neighborPos, RenderOpt.leafDepth())) cir.setReturnValue(false);
    }
}
