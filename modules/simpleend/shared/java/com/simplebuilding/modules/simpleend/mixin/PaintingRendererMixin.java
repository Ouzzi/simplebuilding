package com.simplebuilding.modules.simpleend.mixin;

import com.simplebuilding.modules.simpleend.client.EndPaintingAnim;
import net.minecraft.client.renderer.entity.PaintingRenderer;
import net.minecraft.client.renderer.entity.state.PaintingRenderState;
import net.minecraft.world.entity.decoration.painting.Painting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** W11: swaps an End painting's picture for the animation frame (a variant with the frame's asset id) while it animates. */
@Mixin(PaintingRenderer.class)
public abstract class PaintingRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/painting/Painting;Lnet/minecraft/client/renderer/entity/state/PaintingRenderState;F)V", at = @At("TAIL"))
    private void simpleend$animate(Painting entity, PaintingRenderState state, float partialTicks, CallbackInfo ci) {
        if (state.variant != null) state.variant = EndPaintingAnim.animated(entity, state.variant);
    }
}
