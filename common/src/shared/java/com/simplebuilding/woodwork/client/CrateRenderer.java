package com.simplebuilding.woodwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplebuilding.version.McClientVersion;
import com.simplebuilding.woodwork.CrateBlock;
import com.simplebuilding.woodwork.CrateBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Draws what lies in a crate: the item textures themselves, lying flat in a 3 x 3 grid, so every food shows without
 * textures of its own. Only the two top layers are drawn (the walls hide the rest): the top stack with as many items
 * as its share of a full stack, the stack below it complete. Every item has its own cell (4 px, never touching a
 * wall or a neighbour) and a quarter turn as its only rotation, and the two layers lie a full pixel apart, so nothing
 * overlaps and nothing flickers (Queue N31). The food always lies on the lowest inner side: the bottom of an upright
 * crate, the lower side wall of a crate laid on its side, the ground under an upside-down one
 * ({@link CrateBlock#interior}). The height follows the fill level. Loader-neutral; registered per loader.
 */
public class CrateRenderer implements BlockEntityRenderer<CrateBlockEntity, CrateRenderer.State> {
    private static final int GRID = 9;
    private static final float PX = 1.0F / 16.0F;
    /** Item size: a 16 px sprite drawn 3.8 px wide fits its 4 px cell. */
    private static final float SCALE = 0.24F;

    private final ItemModelResolver itemModelResolver;

    public CrateRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        /** Item counts drawn per layer (top, below) and the heights they lie at. */
        public final int[] shown = new int[2];
        public final float[] height = new float[2];
        public final ItemStackRenderState[] items = {new ItemStackRenderState(), new ItemStackRenderState()};
        /** Centre of the grid (block-relative). */
        public float centreX;
        public float centreZ;
        public int seed;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CrateBlockEntity crate, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(crate, state, partialTicks, cameraPosition, breakProgress);
        state.seed = (int) crate.getBlockPos().asLong();
        BlockState block = crate.getBlockState();
        double[] in = CrateBlock.interior(block.hasProperty(CrateBlock.FACING) ? block.getValue(CrateBlock.FACING) : Direction.UP);
        state.centreX = (float) (in[0] + in[3]) / 2.0F * PX;
        state.centreZ = (float) (in[2] + in[5]) / 2.0F * PX;
        float floor = (float) in[1] * PX;
        // Full at one pixel below the rim; the top layer never sinks below 1.5 px above the floor.
        float depth = (float) (in[4] - 1.0 - in[1]) * PX;
        float surface = Math.max(floor + depth * crate.fill(), floor + 1.5F * PX);
        int top = -1;
        for (int slot = CrateBlockEntity.SLOTS - 1; slot >= 0; slot--) {
            if (!crate.getItem(slot).isEmpty()) {
                top = slot;
                break;
            }
        }
        for (int layer = 0; layer < 2; layer++) {
            int slot = top - layer;
            ItemStack stack = slot >= 0 ? crate.getItem(slot) : ItemStack.EMPTY;
            if (stack.isEmpty()) {
                state.shown[layer] = 0;
                state.items[layer].clear();
                continue;
            }
            float share = (float) stack.getCount() / crate.getMaxStackSize(stack);
            state.shown[layer] = layer == 0 ? Math.max(1, (int) Math.ceil(GRID * share)) : GRID;
            state.height[layer] = surface - layer * PX;
            this.itemModelResolver.updateForTopItem(state.items[layer], stack, ItemDisplayContext.NONE, crate.getLevel(), null, state.seed + slot);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int layer = 1; layer >= 0; layer--) {
            if (state.items[layer].isEmpty()) {
                continue;
            }
            for (int i = 0; i < state.shown[layer]; i++) {
                // Fill order: centre first, then the edges, then the corners.
                int cell = ORDER[i];
                float x = state.centreX + ((cell % 3) - 1) * 4.0F * PX;
                float z = state.centreZ + ((cell / 3) - 1) * 4.0F * PX;
                int turn = ((state.seed * 31 + layer * 17 + cell * 7) >>> 3) & 3;
                poseStack.pushPose();
                poseStack.translate(x, state.height[layer], z);
                McClientVersion.rotate(poseStack, Axis.YP.rotationDegrees(turn * 90.0F));
                McClientVersion.rotate(poseStack, Axis.XP.rotationDegrees(-90.0F));
                poseStack.scale(SCALE, SCALE, SCALE);
                state.items[layer].submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }
        }
    }

    private static final int[] ORDER = {4, 1, 3, 5, 7, 0, 2, 6, 8};
}
