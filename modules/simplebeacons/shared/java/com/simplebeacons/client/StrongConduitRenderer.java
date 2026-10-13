package com.simplebeacons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebeacons.conduit.StrongConduitBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ConduitRenderer;
import net.minecraft.client.renderer.blockentity.state.ConduitRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Strong Conduit in Vanilla's look (concept §4, fallback of question 5): fills Vanilla's conduit render state from
 * the Strong block entity and lets Vanilla's {@link ConduitRenderer} draw shell, cage, wind and eye.
 */
public class StrongConduitRenderer implements BlockEntityRenderer<StrongConduitBlockEntity, ConduitRenderState> {
    private final ConduitRenderer vanilla;

    public StrongConduitRenderer(BlockEntityRendererProvider.Context context) {
        this.vanilla = new ConduitRenderer(context);
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
    public void submit(ConduitRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        this.vanilla.submit(state, poseStack, collector, camera);
    }
}
