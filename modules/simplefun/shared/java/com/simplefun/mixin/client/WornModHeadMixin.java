package com.simplefun.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.level.block.SkullBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A worn mod head replaces the wearer's head instead of sitting over it (owner 2026-10-01): many mod
 * heads (silverfish, slime, breeze, sheep, chicken, ...) are smaller than a humanoid head, which then
 * showed around them. Applies to every humanoid wearer (players, armor stands, zombies, skeletons,
 * piglins). Vanilla skulls ({@link SkullBlock.Types}) keep the vanilla look.
 *
 * <p>{@code skipDraw} hides only the part's own cubes; the head pose still positions the worn head.
 * Nothing in vanilla touches {@code skipDraw} on these parts, so it is set on every call. SimpleBuilding
 * carries the same mixin for its mob heads; both write the same value.
 */
@Mixin(HumanoidModel.class)
public abstract class WornModHeadMixin {
    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart hat;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void simplefun$replaceHeadWithModHead(HumanoidRenderState state, CallbackInfo ci) {
        boolean modHead = state.wornHeadType != null && !(state.wornHeadType instanceof SkullBlock.Types);
        this.head.skipDraw = modHead;
        this.hat.skipDraw = modHead;
    }
}
