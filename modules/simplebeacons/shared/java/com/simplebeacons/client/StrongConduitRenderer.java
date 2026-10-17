package com.simplebeacons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplebeacons.conduit.StrongConduitBlockEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ConduitRenderer;
import net.minecraft.client.renderer.blockentity.state.ConduitRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * The Strong Conduit: Vanilla's conduit geometry and textures (shell, cage, wind, eye) with a star-blue tint on shell, cage
 * and wind, so it reads apart from a Vanilla conduit at a glance (concept §4). Vanilla's {@link ConduitRenderer#submit} has
 * no tint hook, so its drawing is repeated here with the tint as the colour argument; the textures stay Vanilla's.
 */
public class StrongConduitRenderer implements BlockEntityRenderer<StrongConduitBlockEntity, ConduitRenderState> {
    /** ARGB multiplied onto the Vanilla textures. */
    private static final int TINT = 0xFF9FE6FF;

    private final SpriteGetter sprites;
    private final ModelPart eye;
    private final ModelPart wind;
    private final ModelPart shell;
    private final ModelPart cage;

    public StrongConduitRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.eye = context.bakeLayer(ModelLayers.CONDUIT_EYE);
        this.wind = context.bakeLayer(ModelLayers.CONDUIT_WIND);
        this.shell = context.bakeLayer(ModelLayers.CONDUIT_SHELL);
        this.cage = context.bakeLayer(ModelLayers.CONDUIT_CAGE);
    }

    @Override
    public ConduitRenderState createRenderState() {
        return new ConduitRenderState();
    }

    @Override
    public void extractRenderState(StrongConduitBlockEntity entity, ConduitRenderState state, float partialTicks, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, camera, breakProgress);
        state.isActive = entity.isActive();
        state.activeRotation = entity.getActiveRotation(entity.isActive() ? partialTicks : 0.0F);
        state.animTime = entity.tickCount + partialTicks;
        state.animationPhase = entity.tickCount / 66 % 3;
        state.isHunting = entity.isHunting();
    }

    @Override
    public void submit(ConduitRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        if (!state.isActive) {
            pose.pushPose();
            pose.translate(0.5F, 0.5F, 0.5F);
            pose.rotateDegrees(Axis.YP, state.activeRotation);
            SpriteId shellId = ConduitRenderer.SHELL_TEXTURE;
            var shellType = shellId.renderType(RenderTypes::entitySolid);
            collector.submitModelPart(this.shell, pose, shellType, light, OverlayTexture.NO_OVERLAY, this.sprites.get(shellId), TINT);
            if (state.breakProgress != null) {
                collector.order(1).submitCrumblingOverlay(this.shell, pose, shellType, light, OverlayTexture.NO_OVERLAY, -1, state.breakProgress);
            }
            pose.popPose();
            return;
        }
        float rotation = state.activeRotation * (180.0F / (float) Math.PI);
        float bob = Mth.sin(state.animTime * 0.1F) / 2.0F + 0.5F;
        bob = bob * bob + bob;
        pose.pushPose();
        pose.translate(0.5F, 0.3F + bob * 0.2F, 0.5F);
        pose.rotate(new Quaternionf().rotationAxis(rotation * (float) (Math.PI / 180.0), new Vector3f(0.5F, 1.0F, 0.5F).normalize()));
        SpriteId cageId = ConduitRenderer.ACTIVE_SHELL_TEXTURE;
        var cageType = cageId.renderType(RenderTypes::entityCutout);
        collector.submitModelPart(this.cage, pose, cageType, light, OverlayTexture.NO_OVERLAY, this.sprites.get(cageId), TINT);
        if (state.breakProgress != null) {
            collector.order(1).submitCrumblingOverlay(this.cage, pose, cageType, light, OverlayTexture.NO_OVERLAY, -1, state.breakProgress);
        }
        pose.popPose();

        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        if (state.animationPhase == 1) {
            pose.rotate(Axis.XP, (float) (Math.PI / 2));
        } else if (state.animationPhase == 2) {
            pose.rotate(Axis.ZP, (float) (Math.PI / 2));
        }
        SpriteId windId = state.animationPhase == 1 ? ConduitRenderer.VERTICAL_WIND_TEXTURE : ConduitRenderer.WIND_TEXTURE;
        var windType = windId.renderType(RenderTypes::entityCutout);
        TextureAtlasSprite windSprite = this.sprites.get(windId);
        collector.submitModelPart(this.wind, pose, windType, light, OverlayTexture.NO_OVERLAY, windSprite, TINT);
        pose.popPose();

        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.scale(0.875F, 0.875F, 0.875F);
        pose.rotate(new Quaternionf().rotationXYZ((float) Math.PI, 0.0F, (float) Math.PI));
        collector.submitModelPart(this.wind, pose, windType, light, OverlayTexture.NO_OVERLAY, windSprite, TINT);
        pose.popPose();

        pose.pushPose();
        pose.translate(0.5F, 0.3F + bob * 0.2F, 0.5F);
        pose.scale(0.5F, 0.5F, 0.5F);
        pose.rotate(camera.orientation);
        pose.rotate(new Quaternionf().rotationZ((float) Math.PI).rotateY((float) Math.PI));
        pose.scale(1.3333334F, 1.3333334F, 1.3333334F);
        SpriteId eyeId = state.isHunting ? ConduitRenderer.OPEN_EYE_TEXTURE : ConduitRenderer.CLOSED_EYE_TEXTURE;
        collector.submitModelPart(this.eye, pose, eyeId.renderType(RenderTypes::entityCutout), light, OverlayTexture.NO_OVERLAY, this.sprites.get(eyeId));
        pose.popPose();
    }
}
