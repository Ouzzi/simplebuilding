package com.simplemobheads.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplemobheads.MobHeadType;
import com.simplemobheads.easter.Technoblade;
import net.minecraft.client.model.animal.pig.PigModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.PigRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * Technoblade's crown on a living pig (EH21 TO-1): the server puts the crowned pig head into the pig's head slot, Vanilla syncs
 * it and names it in {@code wornHeadType}; pigs have no head layer of their own, so this layer draws only the crown, on the
 * head (it follows every head turn). Adults only - the server never crowns a piglet.
 */
public final class TechnobladeCrownLayer extends RenderLayer<PigRenderState, PigModel> {
    private final ModelPart crown = CustomHeadModels.crownLayer().bakeRoot();

    public TechnobladeCrownLayer(RenderLayerParent<PigRenderState, PigModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, PigRenderState state, float yRot, float xRot) {
        if (state.isInvisible || state.isBaby || !(state.wornHeadType instanceof MobHeadType type) || !type.head().id().equals(Technoblade.HEAD)) {
            return;
        }
        ModelPart root = this.getParentModel().root();
        if (!root.hasChild("head")) {
            return;
        }
        poseStack.pushPose();
        root.translateAndRotate(poseStack);
        root.getChild("head").translateAndRotate(poseStack);
        // Skull space (head y -8..0, z -4..4) -> the pig's head box (y -4..4, z -8..0).
        poseStack.translate(0.0F, 4.0F / 16.0F, -4.0F / 16.0F);
        collector.submitModelPart(this.crown, poseStack, RenderTypes.entityCutout(CustomHeadModels.CROWN), lightCoords,
                LivingEntityRenderer.getOverlayCoords(state, 0.0F), null);
        poseStack.popPose();
    }
}
