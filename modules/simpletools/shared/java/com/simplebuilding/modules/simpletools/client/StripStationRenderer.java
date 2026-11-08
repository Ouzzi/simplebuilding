package com.simplebuilding.modules.simpletools.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.modules.simpletools.strip.StripStationBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the stacks in a strip station cage as 3D blocks (the input left, the output right, a single one in the middle)
 * with the counts as small text above the cage. Loader-neutral; registered per loader.
 */
public class StripStationRenderer implements BlockEntityRenderer<StripStationBlockEntity, StripStationRenderer.State> {
    private final ItemModelResolver resolver;
    private final Font font;

    public StripStationRenderer(BlockEntityRendererProvider.Context context) {
        this.resolver = context.itemModelResolver();
        this.font = context.font();
    }

    public static class State extends BlockEntityRenderState {
        public final ItemStackRenderState input = new ItemStackRenderState();
        public final ItemStackRenderState output = new ItemStackRenderState();
        public boolean hasInput, hasOutput;
        public @Nullable FormattedCharSequence label;
        public float labelWidth;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(StripStationBlockEntity cage, State state, float partialTicks, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(cage, state, partialTicks, camera, breakProgress);
        int seed = (int) cage.getBlockPos().asLong();
        state.hasInput = fill(state.input, cage.input(), cage, seed);
        state.hasOutput = fill(state.output, cage.output(), cage, seed + 1);
        if (state.hasInput || state.hasOutput) {
            String text = state.hasInput && state.hasOutput ? cage.input().getCount() + " > " + cage.output().getCount()
                    : String.valueOf((state.hasInput ? cage.input() : cage.output()).getCount());
            state.label = Component.literal(text).getVisualOrderText();
            state.labelWidth = font.width(state.label);
        } else {
            state.label = null;
        }
    }

    private boolean fill(ItemStackRenderState target, ItemStack stack, StripStationBlockEntity cage, int seed) {
        target.clear();
        if (stack.isEmpty()) return false;
        resolver.updateForTopItem(target, stack, ItemDisplayContext.FIXED, cage.getLevel(), null, seed);
        return true;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        boolean both = state.hasInput && state.hasOutput;
        float scale = both ? 0.62F : 1.0F;
        if (state.hasInput) draw(state.input, state, poseStack, collector, both ? 0.3F : 0.5F, scale);
        if (state.hasOutput) draw(state.output, state, poseStack, collector, both ? 0.7F : 0.5F, scale);
        if (state.label != null) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 1.12F, 0.5F);
            poseStack.rotate(camera.orientation);
            poseStack.scale(0.02F, -0.02F, 0.02F);
            collector.submitText(poseStack, -state.labelWidth / 2.0F, 0.0F, state.label, false, Font.DisplayMode.NORMAL,
                    state.lightCoords, 0xFFFFFFFF, 0x55000000, 0);
            poseStack.popPose();
        }
    }

    private static void draw(ItemStackRenderState item, State state, PoseStack poseStack, SubmitNodeCollector collector, float x, float scale) {
        poseStack.pushPose();
        poseStack.translate(x, 0.5F, 0.5F);
        poseStack.scale(scale, scale, scale);
        item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
