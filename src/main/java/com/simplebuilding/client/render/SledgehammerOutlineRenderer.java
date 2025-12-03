package com.simplebuilding.client.render;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.util.SledgehammerUtils;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.state.OutlineRenderState; // WICHTIG: Neuer Import
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;

import java.util.List;

public class SledgehammerOutlineRenderer {

    public static void register() {
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register(SledgehammerOutlineRenderer::onBlockOutline);
    }

    // FIX 1: Parameter ist OutlineRenderState, nicht BlockOutlineContext
    private static boolean onBlockOutline(WorldRenderContext context, OutlineRenderState outlineRenderState) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null || client.world == null) return true;

        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof SledgehammerItem)) return true;

        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return true;

        BlockPos centerPos = blockHit.getBlockPos();

        List<BlockPos> targetPositions = SledgehammerItem.getBlocksToBeDestroyed(1, centerPos, player);
        if (targetPositions.isEmpty()) return true;

        // --- Rendering Setup ---
        MatrixStack matrices = context.matrices();
        Camera camera = client.gameRenderer.getCamera();
        double camX = camera.getPos().x;
        double camY = camera.getPos().y;
        double camZ = camera.getPos().z;

        VertexConsumer lines = context.consumers().getBuffer(RenderLayer.getLines());
        VertexConsumer fill = context.consumers().getBuffer(RenderLayer.getDebugQuads()); // Für Füllung

        matrices.push();

        // --- CONFIG ZUGRIFF & FARBEN ---

        // Alpha aus Config laden (0-100) und in 0.0-1.0 umrechnen
        int opacityPercent = Simplebuilding.getConfig().tools.buildingHighlightOpacity;
        // Clamping sicherheitshalber (falls Config manuell editiert wurde)
        opacityPercent = Math.max(0, Math.min(100, opacityPercent));

        float baseAlpha = opacityPercent / 100.0f;

        float r = 0.0f; float g = 0.0f; float b = 0.0f; float a = 0.3f;
        float r1 = 1.0f; float g1 = 0.5f; float b1 = 0.3f; float a1 = 0.2f * baseAlpha;

        Box totalBounds = null;

        for (BlockPos pos : targetPositions) {
            if (!SledgehammerUtils.shouldBreak(client.world, pos, centerPos, stack)) {
                continue;
            }

            Box blockBox = new Box(pos);
            if (totalBounds == null) totalBounds = blockBox;
            else totalBounds = totalBounds.union(blockBox);

            if (pos.equals(centerPos)) continue; // Hauptblock überspringen (Vanilla)

            BlockState state = client.world.getBlockState(pos);
            if (state.isAir()) continue;

            VoxelShape shape = state.getOutlineShape(client.world, pos);
            if (shape.isEmpty()) continue;

            for (Box box : shape.getBoundingBoxes()) {
                matrices.push();
                matrices.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);

                // 1. Outline der kleinen Box
                drawBoxOutline(matrices, lines, box, r, g, b, a);
                drawBoxFill(matrices, fill, box.expand(0.003), r1, g1, b1, a1);

                matrices.pop();
            }
        }

        matrices.pop();
        return true;
    }

    // --- Hilfsmethoden ---

    private static void drawBoxFill(MatrixStack matrices, VertexConsumer builder, Box box, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float x1 = (float)box.minX; float y1 = (float)box.minY; float z1 = (float)box.minZ;
        float x2 = (float)box.maxX; float y2 = (float)box.maxY; float z2 = (float)box.maxZ;

        addQuad(builder, matrix, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a); // Unten
        addQuad(builder, matrix, x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1, r, g, b, a); // Oben
        addQuad(builder, matrix, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, b, a); // Nord
        addQuad(builder, matrix, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, r, g, b, a); // Süd
        addQuad(builder, matrix, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, r, g, b, a); // West
        addQuad(builder, matrix, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a); // Ost
    }

    private static void addQuad(VertexConsumer builder, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a) {
        builder.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        builder.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        builder.vertex(matrix, x3, y3, z3).color(r, g, b, a);
        builder.vertex(matrix, x4, y4, z4).color(r, g, b, a);
    }

    private static void drawBoxOutline(MatrixStack matrices, VertexConsumer builder, Box box, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        double x1 = box.minX; double y1 = box.minY; double z1 = box.minZ;
        double x2 = box.maxX; double y2 = box.maxY; double z2 = box.maxZ;

        // Unten
        drawLineWithNormal(builder, matrix, x1, y1, z1, x2, y1, z1, r, g, b, a);
        drawLineWithNormal(builder, matrix, x2, y1, z1, x2, y1, z2, r, g, b, a);
        drawLineWithNormal(builder, matrix, x2, y1, z2, x1, y1, z2, r, g, b, a);
        drawLineWithNormal(builder, matrix, x1, y1, z2, x1, y1, z1, r, g, b, a);

        // Oben
        drawLineWithNormal(builder, matrix, x1, y2, z1, x2, y2, z1, r, g, b, a);
        drawLineWithNormal(builder, matrix, x2, y2, z1, x2, y2, z2, r, g, b, a);
        drawLineWithNormal(builder, matrix, x2, y2, z2, x1, y2, z2, r, g, b, a);
        drawLineWithNormal(builder, matrix, x1, y2, z2, x1, y2, z1, r, g, b, a);

        // Vertikal
        drawLineWithNormal(builder, matrix, x1, y1, z1, x1, y2, z1, r, g, b, a);
        drawLineWithNormal(builder, matrix, x2, y1, z1, x2, y2, z1, r, g, b, a);
        drawLineWithNormal(builder, matrix, x2, y1, z2, x2, y2, z2, r, g, b, a);
        drawLineWithNormal(builder, matrix, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    private static void drawLineWithNormal(VertexConsumer builder, Matrix4f matrix, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        float nx = (float)(x2 - x1); float ny = (float)(y2 - y1); float nz = (float)(z2 - z1);
        float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0) { nx /= len; ny /= len; nz /= len; }
        builder.vertex(matrix, (float)x1, (float)y1, (float)z1).color(r, g, b, a).normal(nx, ny, nz);
        builder.vertex(matrix, (float)x2, (float)y2, (float)z2).color(r, g, b, a).normal(nx, ny, nz);
    }
}