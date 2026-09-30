package com.simplefun.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Renders a pig head on a living entity (player) while the piggy effect is active. Loader-agnostic
 * - registered by each loader's client setup.
 */
public class PigHeadFeatureRenderer<
        S extends LivingEntityRenderState, M extends EntityModel<? super S>>
    extends RenderLayer<S, M> {

  private static final Identifier PIG_TEXTURE =
      Identifier.fromNamespaceAndPath("simplefun", "textures/entity/pig/pig.png");

  private final PigHeadModel pigHeadModel;

  public PigHeadFeatureRenderer(RenderLayerParent<S, M> context, EntityModelSet models) {
    super(context);
    ModelPart root = models.bakeLayer(ModelLayers.PIG);
    this.pigHeadModel = new PigHeadModel(root.getChild("head"));
  }

  @Override
  public void submit(
      PoseStack poseStack,
      SubmitNodeCollector collector,
      int light,
      S state,
      float yRot,
      float xRot) {
    if (state instanceof PiggyStateExtension piggy && !piggy.simplefun$isPiggy()) return;
    if (state.isInvisible) return;

    poseStack.pushPose();

    // Position at the entity's head, following the parent (player) model's head transform.
    if (this.getParentModel() instanceof HumanoidModel<?> humanoid) {
      ModelPart headPart = humanoid.head;
      poseStack.translate(headPart.x / 16.0F, headPart.y / 16.0F, headPart.z / 16.0F);
      if (headPart.zRot != 0.0F) poseStack.rotate(Axis.ZP.rotation(headPart.zRot));
      if (headPart.yRot != 0.0F) poseStack.rotate(Axis.YP.rotation(headPart.yRot));
      if (headPart.xRot != 0.0F) poseStack.rotate(Axis.XP.rotation(headPart.xRot));
    }

    poseStack.scale(1.05F, 1.05F, 1.05F);
    poseStack.translate(0.0F, -0.2F, 0.1F);

    this.pigHeadModel.setupAnim(state);
    renderColoredCutoutModel(
        this.pigHeadModel,
        PIG_TEXTURE,
        poseStack,
        collector,
        light,
        state,
        -1,
        OverlayTexture.NO_OVERLAY);

    poseStack.popPose();
  }

  public static class PigHeadModel extends EntityModel<LivingEntityRenderState> {
    private final ModelPart head;

    public PigHeadModel(ModelPart head) {
      super(head);
      this.head = head;
      this.head.setPos(0.0F, 0.0F, 0.0F);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
      // Parent (player) matrix drives placement, so keep the head's own rotations neutral.
      this.head.xRot = 0.0F;
      this.head.yRot = 0.0F;
      this.head.zRot = 0.0F;
    }
  }
}
