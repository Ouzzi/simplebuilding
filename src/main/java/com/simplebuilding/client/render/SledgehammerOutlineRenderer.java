package com.simplebuilding.client.render;

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

        matrices.push();

        // Farbe für NACHBARN (z.B. Grau/Rötlich)
        float r = 0.0f;
        float g = 0.0f;
        float b = 0.0f;
        float a = 0.3f; // Alpha

        for (BlockPos pos : targetPositions) {
            // WICHTIG: Hauptblock überspringen!
            // Damit rendern wir ihn hier NICHT mit unserer Farbe.
            // Da wir am Ende 'return true' machen, rendert Vanilla ihn danach normal.
            if (pos.equals(centerPos)) continue;

            // Filtern (nur was wirklich abgebaut wird)
            if (!SledgehammerUtils.shouldBreak(client.world, pos, centerPos, stack)) {
                continue;
            }

            BlockState state = client.world.getBlockState(pos);
            if (state.isAir()) continue;

            VoxelShape shape = state.getOutlineShape(client.world, pos);
            if (shape.isEmpty()) continue;

            for (Box box : shape.getBoundingBoxes()) {
                matrices.push();
                matrices.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);
                drawBoxOutline(matrices, lines, box, r, g, b, a);
                matrices.pop();
            }
        }

        matrices.pop();
        return true;
    }

    // --- Hilfsmethoden (aus deiner Referenz) ---

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

        builder.vertex(matrix, (float)x1, (float)y1, (float)z1)
                .color(r, g, b, a)
                .normal(nx, ny, nz);

        builder.vertex(matrix, (float)x2, (float)y2, (float)z2)
                .color(r, g, b, a)
                .normal(nx, ny, nz);
    }
}