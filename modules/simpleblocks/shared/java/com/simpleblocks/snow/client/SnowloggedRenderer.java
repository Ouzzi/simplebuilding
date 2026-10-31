package com.simpleblocks.snow.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simpleblocks.snow.SnowloggedBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the block a snowlogged cell holds (SN30-1) with its own baked model, like Simple Octets' mixed cell draws its
 * materials ({@code submitMovingBlock}: world light, biome tint for grass and ferns, the block's own random offset). The snow
 * itself is the cell's chunk model (Vanilla's snow layers). Loader-neutral, registered per loader.
 */
public class SnowloggedRenderer implements BlockEntityRenderer<SnowloggedBlockEntity, SnowloggedRenderer.State> {
    public SnowloggedRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static class State extends BlockEntityRenderState {
        public @Nullable MovingBlockRenderState block;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SnowloggedBlockEntity blockEntity, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.block = null;
        BlockState held = blockEntity.held();
        if (held == null || !(blockEntity.getLevel() instanceof ClientLevel level)) {
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        MovingBlockRenderState block = new MovingBlockRenderState();
        block.randomSeedPos = pos;
        block.blockPos = pos;
        block.blockState = held;
        block.biome = level.getBiome(pos);
        block.cardinalLighting = level.cardinalLighting();
        block.lightEngine = level.getLightEngine();
        state.block = block;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.block != null) {
            collector.submitMovingBlock(poseStack, state.block, 0);
        }
    }
}
