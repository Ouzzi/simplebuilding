package com.simplebuilding.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.client.render.ExtraHeadLayer;
import com.simplebuilding.util.ExtraHead;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Owner N55: a mod head in the extra head slot replaces the wearer's head exactly like one in the helmet slot (owner
 * 2026-10-01, {@code WornModHeadMixin}): the head and hat cubes are not drawn. The twin mixins only look at the render
 * state's worn head (the helmet slot), so this one runs after them (priority) and switches the cubes off for the extra
 * head; Vanilla skulls keep the player's head visible. Armor models are left alone, so a helmet over the extra head stays
 * visible.
 */
@Mixin(value = HumanoidModel.class, priority = 1200)
public abstract class HumanoidModelExtraHeadMixin {
    @Shadow
    @Final
    public ModelPart head;
    @Shadow
    @Final
    public ModelPart hat;

    /** 0 = not measured, 1 = the player's own model (head cube 8 px), 2 = an armor model (head cube inflated). */
    @Unique
    private int simplebuilding$kind;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void simplebuilding$extraModHeadReplacesHead(HumanoidRenderState state, CallbackInfo ci) {
        if (state instanceof AvatarRenderState avatar && simplebuilding$isOwnModel() && ExtraHead.replacesHead(ExtraHeadLayer.shownHead(avatar))) {
            this.head.skipDraw = true;
            this.hat.skipDraw = true;
        }
    }

    /**
     * Only the player's own model: the armor layer runs the same {@code setupAnim} on its armor models - also
     * {@code PlayerModel}s - whose head must stay or a helmet over the extra head would vanish. The armor model's head cube
     * is inflated (10 px), the player's is 8 px; measured once per model.
     */
    @Unique
    private boolean simplebuilding$isOwnModel() {
        if (this.simplebuilding$kind == 0) {
            float[] width = {0.0F};
            this.head.visit(new PoseStack(), (pose, path, index, cube) -> {
                if (path.isEmpty()) {
                    width[0] = Math.max(width[0], cube.maxX - cube.minX);
                }
            });
            this.simplebuilding$kind = width[0] > 9.0F ? 2 : 1;
        }
        return this.simplebuilding$kind == 1;
    }
}
