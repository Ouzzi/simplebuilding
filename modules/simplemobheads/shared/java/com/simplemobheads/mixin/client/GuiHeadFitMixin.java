package com.simplemobheads.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplemobheads.HeadSlotFit;
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
 * IG31-10: a Mobheads head in the inventory (GUI: creative tab, inventory, hotbar, head slot) is fitted into its slot like a
 * Vanilla item: a uniform scale and a shift in slot units, computed from the model ({@link GuiHeadFit}), so that no head is cut
 * off or sits off-centre, whatever its size or pose. Hand, ground, frames and the rest keep their Vanilla transform.
 * IG31-39: applied at HEAD, on the right of the incoming slot pose (slot centre, 16 px), i.e. before the item's own transform.
 * Multiplied on the left (as before) it also scaled the slot's position in the GUI item atlas: heads landed in other slots or
 * nowhere and most slots of the creative tab stayed empty.
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

    @Inject(method = "applyTransform", at = @At("HEAD"))
    private void simplemobheads$fitInSlot(PoseStack.Pose pose, CallbackInfo ci) {
        if (!((Object) specialRenderer instanceof SkullSpecialRenderer skull)
                || !(((SkullSpecialRendererAccessor) skull).simplemobheads$model() instanceof MobHeadModel model)
                || ((ItemStackRenderStateAccessor) this$0).simplemobheads$displayContext() != ItemDisplayContext.GUI) {
            return;
        }
        float[] fit = FITS.computeIfAbsent(skull, k -> GuiHeadFit.compute(model, itemTransform, (Matrix4fc) localTransform));
        pose.mulPose(HeadSlotFit.matrix(fit));
    }
}
