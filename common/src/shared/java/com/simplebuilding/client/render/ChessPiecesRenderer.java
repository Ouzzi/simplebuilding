package com.simplebuilding.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simplebuilding.blocks.custom.ChessPiecesBlock;
import com.simplebuilding.blocks.entity.custom.ChessPiecesBlockEntity;
import com.simplebuilding.version.McClientVersion;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Zeichnet die Figuren einer Figurenzelle ({@link ChessPiecesBlock}): je Platz das Item-Modell der Figur ohne
 * Anzeige-Transformation ({@link ItemDisplayContext#NONE}). Die Modelle ({@code tools/textures/chess_2026_10_06.py})
 * stehen mittig im Block mit der Front nach Norden; hier werden sie auf ihr Viertel geschoben und in ihre
 * Blickrichtung gedreht. Loader-neutral; registriert wird der Renderer je Loader.
 */
public class ChessPiecesRenderer implements BlockEntityRenderer<ChessPiecesBlockEntity, ChessPiecesRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public ChessPiecesRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    /** Je Platz: Modell und Blickrichtung in Grad (0 = Norden, im Uhrzeigersinn). */
    public static class State extends BlockEntityRenderState {
        public final ItemStackRenderState[] items = new ItemStackRenderState[ChessPiecesBlockEntity.SLOTS];
        public final float[] yaw = new float[ChessPiecesBlockEntity.SLOTS];

        public State() {
            for (int i = 0; i < this.items.length; i++) {
                this.items[i] = new ItemStackRenderState();
            }
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ChessPiecesBlockEntity blockEntity, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = blockEntity.getBlockState();
        Direction facing = blockState.getBlock() instanceof ChessPiecesBlock ? blockState.getValue(ChessPiecesBlock.FACING) : Direction.NORTH;
        int seed = (int) blockEntity.getBlockPos().asLong();
        for (int slot = 0; slot < state.items.length; slot++) {
            var piece = blockEntity.piece(slot);
            if (piece.isEmpty()) {
                state.items[slot].clear();
                continue;
            }
            Direction look = Direction.from2DDataValue(facing.get2DDataValue() + blockEntity.rotation(slot));
            // Modellfront Nord -> Blickrichtung: Drehung gegen den Uhrzeigersinn um 180 - yRot (S 0, W 90, N 180, E 270).
            state.yaw[slot] = 180.0F - look.toYRot();
            this.itemModelResolver.updateForTopItem(state.items[slot], piece, ItemDisplayContext.NONE, blockEntity.getLevel(), null, seed + slot);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int slot = 0; slot < state.items.length; slot++) {
            if (state.items[slot].isEmpty()) {
                continue;
            }
            poseStack.pushPose();
            placePiece(poseStack, slot, state.yaw[slot]);
            state.items[slot].submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    /**
     * Pose einer Figur relativ zum Block: Mitte ihres Viertels, dort um die Hochachse gedreht. Oeffentlich fuer den
     * Client-Test, der damit nachrechnet, wo das Modell landet.
     */
    public static void placePiece(PoseStack poseStack, int slot, float yaw) {
        poseStack.translate((slot & 1) * 0.5F + 0.25F, 0.0F, (slot >> 1) * 0.5F + 0.25F);
        McClientVersion.rotate(poseStack, Axis.YP.rotationDegrees(yaw));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
    }
}
