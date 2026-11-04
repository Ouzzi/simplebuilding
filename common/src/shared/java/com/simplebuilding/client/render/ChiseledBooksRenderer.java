package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.ModItems;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * DW04C-3 (docs/ai/PLAN-B-SHELVES01-2026-11-04.md): a Blaze or Nihilit Book in a chiseled bookshelf shows as such. Vanilla draws
 * every occupied slot with the shelf's own "occupied" books (each slot another colour); for a slot holding a mod book this renderer
 * lays that slot's spines in the book's colours over the front (texture {@code block/chiseled_books_<kind>}, the same slot rectangles
 * as Vanilla's {@code template_chiseled_bookshelf_slot_*}, a hair in front of the face). Works for Vanilla's and every mod chiseled
 * bookshelf (they share Vanilla's block entity); the books reach the client through {@code ChiseledBookShelfSyncMixin}.
 */
public class ChiseledBooksRenderer implements BlockEntityRenderer<ChiseledBookShelfBlockEntity, ChiseledBooksRenderer.State> {
    /** The mod books with spines of their own, in texture order. */
    private static final String[] KINDS = {"blaze", "nihilith"};
    private static final Identifier[] TEXTURES = {
            Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "textures/block/chiseled_books_blaze.png"),
            Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "textures/block/chiseled_books_nihilith.png")};
    /** Slot rectangles on the front texture (u0, v0, u1, v1), Vanilla's slot order 0-5. */
    static final float[][] SLOTS = {{0, 0, 6, 8}, {6, 0, 11, 8}, {11, 0, 16, 8}, {0, 8, 6, 16}, {6, 8, 11, 16}, {11, 8, 16, 16}};
    /** How far the spines stand in front of the face (blocks). */
    private static final float LIFT = 0.003F;

    public ChiseledBooksRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** Facing, front light and the mod book of each slot (-1: none, else index into {@link #KINDS}). */
    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public int frontLight;
        public final int[] kinds = new int[6];
        public boolean any;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** Index of the mod book in {@link #KINDS}, or -1. */
    public static int kind(ItemStack stack) {
        if (stack.isEmpty() || ModItems.BLAZE_BOOK == null) return -1;
        Item item = stack.getItem();
        return item == ModItems.BLAZE_BOOK ? 0 : item == ModItems.NIHILITH_BOOK ? 1 : -1;
    }

    @Override
    public void extractRenderState(ChiseledBookShelfBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);
        state.any = false;
        var blockState = entity.getBlockState();
        state.facing = blockState.hasProperty(ChiseledBookShelfBlock.FACING) ? blockState.getValue(ChiseledBookShelfBlock.FACING) : Direction.NORTH;
        for (int slot = 0; slot < 6; slot++) {
            state.kinds[slot] = kind(entity.getItem(slot));
            state.any |= state.kinds[slot] >= 0;
        }
        state.frontLight = entity.getLevel() == null ? state.lightCoords
                : LightCoordsUtil.getLightCoords(entity.getLevel(), entity.getBlockPos().relative(state.facing));
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.any) return;
        poseStack.pushPose();
        // The slot geometry is that of the north-facing model; turn it like the blockstate's y rotation (north 0, east 90, ...).
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.facing.toYRot()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        for (int k = 0; k < KINDS.length; k++) {
            final int kind = k;
            boolean used = false;
            for (int slotKind : state.kinds) used |= slotKind == kind;
            if (!used) continue;
            collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURES[kind]), (pose, buffer) -> {
                for (int slot = 0; slot < 6; slot++) {
                    if (state.kinds[slot] == kind) quad(buffer, pose, state.frontLight, SLOTS[slot]);
                }
            });
        }
        poseStack.popPose();
    }

    /** One slot of the north face: texture u runs from east (x = 1) to west, v from top to bottom. */
    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, int light, float[] r) {
        float u0 = r[0] / 16.0F, v0 = r[1] / 16.0F, u1 = r[2] / 16.0F, v1 = r[3] / 16.0F;
        float xa = 1.0F - u0, xb = 1.0F - u1, ya = 1.0F - v0, yb = 1.0F - v1, z = -LIFT;
        vertex(buffer, pose, light, xa, ya, z, u0, v0);
        vertex(buffer, pose, light, xa, yb, z, u0, v1);
        vertex(buffer, pose, light, xb, yb, z, u1, v1);
        vertex(buffer, pose, light, xb, ya, z, u1, v0);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, int light, float x, float y, float z, float u, float v) {
        buffer.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, 0.0F, 0.0F, -1.0F);
    }
}
