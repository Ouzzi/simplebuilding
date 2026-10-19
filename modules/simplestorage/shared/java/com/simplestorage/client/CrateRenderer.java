package com.simplestorage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simplestorage.crate.CrateBlock;
import com.simplestorage.crate.CrateBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Draws what lies in a crate (Queue N55, owner reference crate-reference.png): every kind of item is one bulk layer that
 * looks like the item, stacked in the order of {@link CrateBlockEntity#layers()}, seen through the slat gaps at the sides
 * and from above; on the outer walls the kinds also show as small icons ({@link CrateMesh} has the geometry and its
 * rules). A layer is a few dozen quads built once per content change and cached in the block entity; per frame the
 * renderer only copies them. Beyond {@link #NEAR} blocks only the plates (the average colour, no tiles or icons) are
 * drawn. The contents always lie on the lowest inner side: the bottom of an upright crate, the lower side wall of one
 * laid on its side, the ground under an upside-down one ({@link CrateBlock#interior}). Loader-neutral; registered per loader.
 */
public class CrateRenderer implements BlockEntityRenderer<CrateBlockEntity, CrateRenderer.State> {
    /** Within this many blocks tiles and icons are drawn, further away only the plain plates. */
    private static final double NEAR = 16.0;
    /** Plates seen from afar are brightened a little: the tiles' shadow between the pieces is missing there. */
    private static final float FAR_BOOST = 1.25F;

    private final ItemModelResolver itemModelResolver;

    public CrateRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        @Nullable CrateMesh mesh;
        boolean near;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CrateBlockEntity crate, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(crate, state, partialTicks, cameraPosition, breakProgress);
        BlockState block = crate.getBlockState();
        Direction facing = block.hasProperty(CrateBlock.FACING) ? block.getValue(CrateBlock.FACING) : Direction.UP;
        state.mesh = crate.layers().isEmpty() ? null : CrateMesh.of(crate, facing, this.itemModelResolver);
        state.near = cameraPosition.closerThan(Vec3.atCenterOf(crate.getBlockPos()), NEAR);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        CrateMesh mesh = state.mesh;
        if (mesh == null) {
            return;
        }
        int light = state.lightCoords;
        boolean near = state.near;
        for (CrateMesh.Batch batch : mesh.batches) {
            collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(batch.atlas), (pose, buffer) -> {
                emit(buffer, pose, light, batch.plates, near ? 1.0F : FAR_BOOST);
                if (near) {
                    emit(buffer, pose, light, batch.tiles, 1.0F);
                    emit(buffer, pose, light, batch.icons, 1.0F);
                }
            });
        }
    }

    private static void emit(VertexConsumer buffer, PoseStack.Pose pose, int light, CrateMesh.Buf quads, float boost) {
        float[] d = quads.data;
        for (int i = 0; i < quads.size; i += 9) {
            int grey = Math.min(255, (int) (d[i + 8] * boost * 255.0F));
            int color = 0xFF000000 | grey << 16 | grey << 8 | grey;
            buffer.addVertex(pose, d[i], d[i + 1], d[i + 2]).setColor(color).setUv(d[i + 3], d[i + 4])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, d[i + 5], d[i + 6], d[i + 7]);
        }
    }
}
