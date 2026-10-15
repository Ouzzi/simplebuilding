package com.simpleshops.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simpleshops.block.DisplayCaseBlock;
import com.simpleshops.block.DisplayCaseBlockEntity;
import com.simpleshops.block.SalesBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the goods of a display case as small spinning items inside the glass: one in the middle, two side by side, a
 * triangle, a square, a square plus the middle (concept 1.3). Inactive (no sales block below, or no price): no spin and
 * dimmed. The glow case draws active goods fully bright. Only the goods stacks and the price flag reach the client.
 */
public class DisplayCaseRenderer implements BlockEntityRenderer<DisplayCaseBlockEntity, DisplayCaseRenderer.State> {
    /** Block-space x/z of the item {@code i} of {@code n} (1..5). */
    static final float[][][] LAYOUT = {
            {},
            {{0.5F, 0.5F}},
            {{0.3F, 0.5F}, {0.7F, 0.5F}},
            {{0.5F, 0.3F}, {0.3F, 0.7F}, {0.7F, 0.7F}},
            {{0.3F, 0.3F}, {0.7F, 0.3F}, {0.3F, 0.7F}, {0.7F, 0.7F}},
            {{0.3F, 0.3F}, {0.7F, 0.3F}, {0.3F, 0.7F}, {0.7F, 0.7F}, {0.5F, 0.5F}},
    };

    private final ItemModelResolver itemModelResolver;

    public DisplayCaseRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        public int count;
        public boolean active, glow;
        public float time;
        public final ItemStackRenderState[] items = new ItemStackRenderState[DisplayCaseBlockEntity.SIZE];

        public State() {
            for (int i = 0; i < this.items.length; i++) this.items[i] = new ItemStackRenderState();
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DisplayCaseBlockEntity entity, State state, float partialTicks, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, camera, breakProgress);
        var level = entity.getLevel();
        state.glow = entity.getBlockState().getBlock() instanceof DisplayCaseBlock block && block.glow();
        state.active = level != null && level.getBlockEntity(entity.getBlockPos().below()) instanceof SalesBlockEntity sales && sales.hasPrice();
        state.time = level == null ? 0.0F : level.getGameTime() + partialTicks;
        int seed = (int) entity.getBlockPos().asLong();
        int n = 0;
        for (ItemStack goods : entity.goods()) {
            if (goods.isEmpty() || n >= state.items.length) continue;
            this.itemModelResolver.updateForTopItem(state.items[n], goods, ItemDisplayContext.FIXED, level, null, seed + n);
            n++;
        }
        for (int i = n; i < state.items.length; i++) state.items[i].clear();
        state.count = n;
        // Inactive goods are dimmed through the light value; active glow goods are fully bright.
        if (!state.active) state.lightCoords = LightCoordsUtil.pack(Math.min(LightCoordsUtil.block(state.lightCoords), 3), Math.min(LightCoordsUtil.sky(state.lightCoords), 3));
        else if (state.glow) state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        int n = state.count;
        if (n <= 0) return;
        float scale = n == 1 ? 1.0F : 0.6F;
        for (int i = 0; i < n; i++) {
            if (state.items[i].isEmpty()) continue;
            float[] at = LAYOUT[n][i];
            float bob = state.active ? (float) Math.sin((state.time + i * 13) / 14.0F) * 0.02F : 0.0F;
            poseStack.pushPose();
            poseStack.translate(at[0], (n == 1 ? 0.44F : 0.31F) + (i == 4 ? 0.12F : 0.0F) + bob, at[1]);
            if (state.active) poseStack.mulPose(new org.joml.Matrix4f().rotate(Axis.YP.rotationDegrees(state.time * 2.5F + i * 40.0F)));
            poseStack.scale(scale, scale, scale);
            state.items[i].submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    @Override
    public int getViewDistance() {
        return 24;
    }
}
