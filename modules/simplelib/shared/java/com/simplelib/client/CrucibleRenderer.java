package com.simplelib.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplelib.crucible.CrucibleBlock;
import com.simplelib.crucible.CrucibleBlockEntity;
import com.simplelib.crucible.CrucibleTier;
import java.util.ArrayList;
import java.util.List;
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
 * Shows the contents inside the open crucible, stacked in 3D (owner 21/22): one small item per filled
 * slot on the crucible floor in grid order, a second (and third) layer for larger tiers, fuller
 * stacks as up to three offset copies. Culling (owner 53): only within {@link #VIEW_DISTANCE} blocks
 * and only when the player can see into the pot (not from far below).
 */
public class CrucibleRenderer implements BlockEntityRenderer<CrucibleBlockEntity, CrucibleRenderer.State> {
    public static final int VIEW_DISTANCE = 16;
    /** Inner floor of the kettle model (gen_resources.py FLOOR top). */
    private static final float FLOOR = 4.0F / 16.0F;
    private static final float SCALE = 0.22F;
    private static final float LAYER = 0.12F;

    private final ItemModelResolver resolver;

    public CrucibleRenderer(BlockEntityRendererProvider.Context context) {
        this.resolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        public float yaw;
        public final List<ItemStackRenderState> items = new ArrayList<>();
        public final List<float[]> positions = new ArrayList<>();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CrucibleBlockEntity crucible, State state, float partialTicks, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(crucible, state, partialTicks, camera, breakProgress);
        state.items.clear();
        state.positions.clear();
        state.yaw = -crucible.getBlockState().getValue(CrucibleBlock.FACING).toYRot();
        CrucibleTier tier = crucible.tier();
        int seed = (int) crucible.getBlockPos().asLong();
        for (int i = 0; i < tier.slots(); i++) {
            ItemStack stack = crucible.getItem(i);
            if (stack.isEmpty()) continue;
            int copies = stack.getCount() >= stack.getMaxStackSize() / 2 ? (stack.getCount() >= stack.getMaxStackSize() ? 3 : 2) : 1;
            int cell = (tier.row(i) * 3 + tier.column(i)) % 9;
            float cx = -0.22F + (cell % 3) * 0.22F;
            float cz = -0.22F + (cell / 3) * 0.22F;
            // A fourth row (reinforced, 3x4 since N30) lies one layer up, like a further grid.
            float layer = (tier.grid(i) + tier.row(i) / 3) * LAYER;
            for (int c = 0; c < copies; c++) {
                ItemStackRenderState render = new ItemStackRenderState();
                resolver.updateForTopItem(render, stack, ItemDisplayContext.FIXED, crucible.getLevel(), null, seed + i);
                state.items.add(render);
                state.positions.add(new float[]{cx + c * 0.02F, FLOOR + 0.02F + layer + c * 0.03F, cz - c * 0.02F, (i * 37 + c * 23) % 360});
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int i = 0; i < state.items.size(); i++) {
            float[] p = state.positions.get(i);
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            poseStack.rotate(Axis.YP.rotationDegrees(state.yaw));
            poseStack.translate(p[0], p[1], p[2]);
            poseStack.rotate(Axis.XP.rotationDegrees(90.0F));
            poseStack.rotate(Axis.ZP.rotationDegrees(p[3]));
            poseStack.scale(SCALE, SCALE, SCALE);
            state.items.get(i).submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    /** Owner 53: skip pots nobody can look into (camera well below the rim). */
    @Override
    public boolean shouldRender(CrucibleBlockEntity crucible, Vec3 camera) {
        return BlockEntityRenderer.super.shouldRender(crucible, camera) && camera.y > crucible.getBlockPos().getY() - 1.0;
    }
}
