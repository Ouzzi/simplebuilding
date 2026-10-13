package com.simplemobheads.mixin.client;

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
 * A worn mob head replaces the wearer's head instead of sitting over it (owner 2026-10-01): many heads are smaller than a
 * humanoid head, which would show around them. Only the cubes are hidden ({@code skipDraw}); the head pose still places
 * the worn head. Vanilla skulls keep the Vanilla look.
 */
@Mixin(HumanoidModel.class)
public abstract class WornModHeadMixin {
    @Shadow
    @Final
    public ModelPart head;
    @Shadow
    @Final
    public ModelPart hat;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void simplemobheads$replaceHead(HumanoidRenderState state, CallbackInfo ci) {
        // Same rule as SimpleBuilding's twin mixin (every non-Vanilla skull type), so both write the same value.
        boolean modHead = state.wornHeadType != null && !(state.wornHeadType instanceof SkullBlock.Types);
        this.head.skipDraw = modHead;
        this.hat.skipDraw = modHead;
    }
}
