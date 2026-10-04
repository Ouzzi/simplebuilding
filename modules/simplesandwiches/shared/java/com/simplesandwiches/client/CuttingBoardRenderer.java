package com.simplesandwiches.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplesandwiches.block.CuttingBoardBlock;
import com.simplesandwiches.block.CuttingBoardBlockEntity;
import com.simplesandwiches.registry.ModItems;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Draws what lies on a cutting board: the loaf, or the opened bread with butter and the ingredients
 * stacked flat on top of each other, or the closed sandwich. Loader-neutral; registered per loader.
 */
public class CuttingBoardRenderer implements BlockEntityRenderer<CuttingBoardBlockEntity, CuttingBoardRenderer.State> {
    private static final float BOARD_TOP = 2.0F / 16.0F;
    private static final float LAYER = 0.035F;
    private static final float SCALE = 0.55F;

    private final ItemModelResolver resolver;

    public CuttingBoardRenderer(BlockEntityRendererProvider.Context context) {
        this.resolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        public float yaw;
        public final List<ItemStackRenderState> layers = new ArrayList<>();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CuttingBoardBlockEntity board, State state, float partialTicks, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(board, state, partialTicks, camera, breakProgress);
        state.layers.clear();
        state.yaw = -board.getBlockState().getValue(CuttingBoardBlock.FACING).toYRot();
        List<ItemStack> stacks = new ArrayList<>();
        switch (board.stage()) {
            case LOAF -> stacks.add(new ItemStack(Items.BREAD));
            case OPEN -> {
                stacks.add(new ItemStack(Items.BREAD));
                if (board.buttered()) stacks.add(new ItemStack(ModItems.BUTTER_SLICE));
                stacks.addAll(board.ingredients());
            }
            case CLOSED -> stacks.add(board.sandwich());
            default -> {}
        }
        int seed = (int) board.getBlockPos().asLong();
        for (ItemStack stack : stacks) {
            ItemStackRenderState layer = new ItemStackRenderState();
            resolver.updateForTopItem(layer, stack, ItemDisplayContext.FIXED, board.getLevel(), null, seed++);
            state.layers.add(layer);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int i = 0; i < state.layers.size(); i++) {
            poseStack.pushPose();
            poseStack.translate(0.5F, BOARD_TOP + 0.01F + i * LAYER, 0.5F);
            poseStack.rotate(Axis.YP.rotationDegrees(state.yaw));
            poseStack.rotate(Axis.XP.rotationDegrees(90.0F));
            float scale = SCALE * (i == 0 ? 1.0F : 0.85F);
            poseStack.scale(scale, scale, scale);
            state.layers.get(i).submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }
}
