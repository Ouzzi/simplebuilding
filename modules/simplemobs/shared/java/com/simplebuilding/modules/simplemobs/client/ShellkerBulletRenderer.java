package com.simplebuilding.modules.simplemobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplebuilding.modules.simplemobs.ShellkerBullet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.ShulkerBulletModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ShulkerBulletRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Shellker bullet: Vanilla's spinning spark with its own grey-violet texture. */
public class ShellkerBulletRenderer extends EntityRenderer<ShellkerBullet, ShulkerBulletRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/shellker/spark.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(TEXTURE);
    private final ShulkerBulletModel model;

    public ShellkerBulletRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ShulkerBulletModel(context.bakeLayer(ModelLayers.SHULKER_BULLET));
    }

    @Override
    protected int getBlockLightLevel(ShellkerBullet entity, BlockPos blockPos) {
        return 15;
    }

    @Override
    public void submit(ShulkerBulletRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        float tc = state.ageInTicks;
        poseStack.translate(0.0F, 0.15F, 0.0F);
        poseStack.rotateDegrees(Axis.YP, Mth.sin(tc * 0.15F) * 180.0F);
        poseStack.rotateDegrees(Axis.XP, Mth.cos(tc * 0.15F) * 180.0F);
        poseStack.rotateDegrees(Axis.ZP, Mth.sin(tc * 0.2F) * 360.0F);
        poseStack.scale(-0.5F, -0.5F, 0.5F);
        collector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        collector.order(1).submitModel(this.model, state, poseStack, RENDER_TYPE, state.lightCoords, OverlayTexture.NO_OVERLAY, 654311423, null, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    public ShulkerBulletRenderState createRenderState() {
        return new ShulkerBulletRenderState();
    }

    @Override
    public void extractRenderState(ShellkerBullet entity, ShulkerBulletRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot(partialTicks);
        state.xRot = entity.getXRot(partialTicks);
    }
}
