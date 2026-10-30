package com.simplemobheads.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplemobheads.client.GuiHeadFit;
import com.simplemobheads.client.MobHeadModel;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SkullSpecialRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * IG31-10: a Mobheads head in the inventory (GUI) is fitted into its slot like a Vanilla item: after the item's own transform a
 * uniform scale and a shift in screen space, computed from the model's bounding box ({@link GuiHeadFit}), so that no head is cut
 * off or sits off-centre, whatever its size or pose. Hand, ground, frames and the rest keep their Vanilla transform.
 */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public abstract class GuiHeadFitMixin {
    @Shadow
    @Final
    private ItemStackRenderState this$0;
    @Shadow
    private ItemTransform itemTransform;
    @Shadow
    @Final
    private Matrix4f localTransform;
    @Shadow
    private @Nullable SpecialModelRenderer<Object> specialRenderer;

    @org.spongepowered.asm.mixin.Unique
    private static final Map<SpecialModelRenderer<?>, float[]> FITS = new WeakHashMap<>();

    @Inject(method = "applyTransform", at = @At("TAIL"))
    private void simplemobheads$fitInSlot(PoseStack.Pose pose, CallbackInfo ci) {
        if (!((Object) specialRenderer instanceof SkullSpecialRenderer skull)
                || !(((SkullSpecialRendererAccessor) skull).simplemobheads$model() instanceof MobHeadModel)
                || ((ItemStackRenderStateAccessor) this$0).simplemobheads$displayContext() != ItemDisplayContext.GUI) {
            return;
        }
        float[] fit = FITS.computeIfAbsent(skull, k -> GuiHeadFit.compute(skull, itemTransform, (Matrix4fc) localTransform));
        pose.pose().mulLocal(new Matrix4f().translation(fit[1], fit[2], 0.0F).scale(fit[0]));
    }
}
