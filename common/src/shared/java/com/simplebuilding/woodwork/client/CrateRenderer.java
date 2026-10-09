package com.simplebuilding.woodwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplebuilding.version.McClientVersion;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Draws what lies in a crate: the item textures themselves, lying flat in a 3 x 3 layer, so every food shows
 * without textures of its own. Only the two top layers are drawn (the walls hide the rest): the top stack with as
 * many items as its share of a full stack, the stack below it complete. The height follows the fill level (floor
 * at 2 px, full at 15 px). Loader-neutral; registered per loader.
 */
public class CrateRenderer implements BlockEntityRenderer<CrateBlockEntity, CrateRenderer.State> {
    private static final int GRID = 9;
    private static final float FLOOR = 2.0F / 16.0F;
    private static final float DEPTH = 13.0F / 16.0F;

    private final ItemModelResolver itemModelResolver;

    public CrateRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        /** Item counts drawn per layer (top, below) and the surface heights. */
        public final int[] shown = new int[2];
        public final float[] height = new float[2];
        public final ItemStackRenderState[] items = {new ItemStackRenderState(), new ItemStackRenderState()};
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
        float surface = FLOOR + DEPTH * crate.fill();
        int top = -1;
        for (int slot = CrateBlockEntity.SLOTS - 1; slot >= 0; slot--) {
            if (!crate.getItem(slot).isEmpty()) {
                top = slot;
                break;
            }
        }
        float y = surface;
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
            state.height[layer] = y;
            y -= DEPTH / CrateBlockEntity.SLOTS * share;
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
                float x = (4 + (cell % 3) * 4) / 16.0F;
                float z = (4 + (cell / 3) * 4) / 16.0F;
                int hash = state.seed * 31 + layer * 17 + cell * 7;
                poseStack.pushPose();
                poseStack.translate(x, state.height[layer] - 0.02F - layer * 0.01F, z);
                McClientVersion.rotate(poseStack, Axis.YP.rotationDegrees((hash & 0xFF) * 360.0F / 256.0F));
                McClientVersion.rotate(poseStack, Axis.XP.rotationDegrees(-90.0F));
                poseStack.scale(0.42F, 0.42F, 0.42F);
                state.items[layer].submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }
        }
    }

    private static final int[] ORDER = {4, 1, 3, 5, 7, 0, 2, 6, 8};
}
