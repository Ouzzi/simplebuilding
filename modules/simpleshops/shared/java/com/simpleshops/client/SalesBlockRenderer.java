package com.simpleshops.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simpleshops.block.SalesBlock;
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
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The price tag: the first price item as a small flat icon on the plate of the sales block's front. Display only. */
public class SalesBlockRenderer implements BlockEntityRenderer<SalesBlockEntity, SalesBlockRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public SalesBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public final ItemStackRenderState item = new ItemStackRenderState();
        public boolean shown;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SalesBlockEntity entity, State state, float partialTicks, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, camera, breakProgress);
        state.facing = entity.getBlockState().getBlock() instanceof SalesBlock ? entity.getBlockState().getValue(SalesBlock.FACING) : Direction.NORTH;
        ItemStack first = ItemStack.EMPTY;
        for (ItemStack price : entity.prices()) {
            if (!price.isEmpty()) {
                first = price;
                break;
            }
        }
        state.shown = !first.isEmpty();
        if (state.shown) this.itemModelResolver.updateForTopItem(state.item, first, ItemDisplayContext.FIXED, entity.getLevel(), null, (int) entity.getBlockPos().asLong());
        else state.item.clear();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.shown || state.item.isEmpty()) return;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(new org.joml.Matrix4f().rotate(Axis.YP.rotationDegrees(-yaw(state.facing))));
        // The brass price plate on the north-facing front: 5/16 from the east edge, 10.5/16 up, a hair in front of the face.
        poseStack.translate(5.0F / 16.0F - 0.5F, 10.5F / 16.0F, -0.5F - 0.004F);
        poseStack.mulPose(new org.joml.Matrix4f().rotate(Axis.YP.rotationDegrees(180.0F)));
        poseStack.scale(0.2F, 0.2F, 0.02F);
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private static float yaw(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }

    @Override
    public int getViewDistance() {
        return 24;
    }
}
