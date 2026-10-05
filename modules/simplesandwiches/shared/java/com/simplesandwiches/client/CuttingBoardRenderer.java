package com.simplesandwiches.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplesandwiches.Sandwiches;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Draws what lies on a cutting board: the loaf; the opened bread as two halves side by side with
 * butter and the ingredients stacked flat on the left half; or the closed sandwich. Loader-neutral; registered per loader.
 */
public class CuttingBoardRenderer implements BlockEntityRenderer<CuttingBoardBlockEntity, CuttingBoardRenderer.State> {
    /** Items lie on the plate inside the 1 px rim. */
    private static final float BOARD_TOP = 1.0F / 16.0F;
    private static final float LAYER = 0.035F;
    private static final float SCALE = 0.55F;
    /** Bread halves: each drawn at this scale, this far left/right of the board centre. */
    private static final float HALF_SCALE = 0.5F, HALF_OFFSET = 0.16F, TOPPING_SCALE = 0.38F;

    private final ItemModelResolver resolver;

    public CuttingBoardRenderer(BlockEntityRendererProvider.Context context) {
        this.resolver = context.itemModelResolver();
    }

    /** One flat item on the board: board-local offset (x across, z along), layer height, scale, own yaw. */
    public record Placed(ItemStackRenderState item, float x, float z, int layer, float scale, float spin) {}

    public static class State extends BlockEntityRenderState {
        public float yaw;
        public final List<Placed> items = new ArrayList<>();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** The opened bread: a Vanilla bread stack drawn with the module's bread-half item model. */
    static ItemStack breadHalf() {
        ItemStack half = new ItemStack(Items.BREAD);
        half.set(DataComponents.ITEM_MODEL, Sandwiches.id("board/bread_half"));
        return half;
    }

    @Override
    public void extractRenderState(CuttingBoardBlockEntity board, State state, float partialTicks, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(board, state, partialTicks, camera, breakProgress);
        state.items.clear();
        state.yaw = -board.getBlockState().getValue(CuttingBoardBlock.FACING).toYRot();
        int seed = (int) board.getBlockPos().asLong();
        switch (board.stage()) {
            case LOAF -> add(state, board, new ItemStack(Items.BREAD), 0, 0, 0, SCALE, 0, seed);
            case OPEN -> {
                // Owner 2026-10-05: cut bread lies as two halves side by side; toppings go on the left half.
                add(state, board, breadHalf(), -HALF_OFFSET, 0, 0, HALF_SCALE, 0, seed++);
                add(state, board, breadHalf(), HALF_OFFSET, 0, 0, HALF_SCALE, 180, seed++);
                int layer = 1;
                if (board.buttered()) add(state, board, new ItemStack(ModItems.BUTTER_SLICE), -HALF_OFFSET, 0, layer++, TOPPING_SCALE, 0, seed++);
                for (ItemStack stack : board.ingredients()) add(state, board, stack, -HALF_OFFSET, 0, layer++, TOPPING_SCALE, 0, seed++);
            }
            case CLOSED -> add(state, board, board.sandwich(), 0, 0, 0, SCALE, 0, seed);
            default -> {}
        }
    }

    private void add(State state, CuttingBoardBlockEntity board, ItemStack stack, float x, float z, int layer, float scale, float spin, int seed) {
        ItemStackRenderState item = new ItemStackRenderState();
        resolver.updateForTopItem(item, stack, ItemDisplayContext.FIXED, board.getLevel(), null, seed);
        state.items.add(new Placed(item, x, z, layer, scale, spin));
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (Placed placed : state.items) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            poseStack.rotate(Axis.YP.rotationDegrees(state.yaw));
            poseStack.translate(placed.x(), BOARD_TOP + 0.01F + placed.layer() * LAYER, placed.z());
            poseStack.rotate(Axis.YP.rotationDegrees(placed.spin()));
            poseStack.rotate(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(placed.scale(), placed.scale(), placed.scale());
            placed.item().submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }
}
