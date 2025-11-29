package com.simplebuilding.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.simplebuilding.items.custom.RangefinderItem;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class BlockHighlightRendererBackup implements WorldRenderEvents.EndMain {

    @Override
    public void endMain(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        ItemStack stack = client.player.getMainHandStack();
        boolean isRangefinder = stack.getItem() instanceof RangefinderItem;

        if (!isRangefinder) {
            stack = client.player.getOffHandStack();
            isRangefinder = stack.getItem() instanceof RangefinderItem;
        }

        if (isRangefinder) {
            renderRangefinderHighlights(context, stack);
        }
    }

    private void renderRangefinderHighlights(WorldRenderContext context, ItemStack stack) {
        NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtData.copyNbt();

        BlockPos pos1 = getPos(nbt, "Pos1");
        BlockPos pos2 = getPos(nbt, "Pos2");

        if (pos1 == null && pos2 == null) return;

        MatrixStack matrices = new MatrixStack();
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        if (camera == null) return;
        Vec3d cameraPos = camera.getPos();

        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        BufferAllocator allocator = new BufferAllocator(1536);
        VertexConsumerProvider.Immediate consumers = VertexConsumerProvider.immediate(allocator);

        // --- FARBPALETTE ---
        float r1 = 1.0f; float g1 = 0.5f; float b1 = 0.3f; // Kupfer
        float r2 = 1.0f; float g2 = 0.85f; float b2 = 0.4f; // Messing
        float r3 = 0.8f; float g3 = 0.6f; float b3 = 0.4f; // Bronze

        // 1. OUTLINES
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        if (pos1 != null) drawBoxOutline(matrices, lines, new Box(pos1).expand(0.02), r1, g1, b1, 1.0f);
        if (pos2 != null) drawBoxOutline(matrices, lines, new Box(pos2).expand(0.02), r2, g2, b2, 1.0f);
        if (pos1 != null && pos2 != null) drawBoxOutline(matrices, lines, getFullArea(pos1, pos2).expand(0.02), r3, g3, b3, 1.0f);
        consumers.draw(); // Zeichnet die Linien sofort

        // 2. FÜLLUNG  //TODO Constructors Touch -> when sneaking, show planes
        VertexConsumer fill = consumers.getBuffer(RenderLayer.getDebugFilledBox());
        boolean drawBoxEnabled = true;

        if (drawBoxEnabled) {
            if (pos1 != null) drawBoxFillAsTriangles(matrices, fill, new Box(pos1).expand(0.004), r1, g1, b1, 0.3f);
            if (pos2 != null) drawBoxFillAsTriangles(matrices, fill, new Box(pos2).expand(0.004), r2, g2, b2, 0.3f);
            if (pos1 != null && pos2 != null) drawBoxFillAsTriangles(matrices, fill, getFullArea(pos1, pos2).expand(0.004), r3, g3, b3, 0.15f);
        }
        consumers.draw(); // Zeichnet die Füllung

        matrices.pop();
    }

    private void drawBoxFillAsTriangles(MatrixStack matrices, VertexConsumer builder, Box box, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float x1 = (float)box.minX; float y1 = (float)box.minY; float z1 = (float)box.minZ;
        float x2 = (float)box.maxX; float y2 = (float)box.maxY; float z2 = (float)box.maxZ;

        // Unten
        addTriangle(builder, matrix, x1, y1, z1, x2, y1, z1, x2, y1, z2, r, g, b, a);
        addTriangle(builder, matrix, x1, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a);
        // Oben
        addTriangle(builder, matrix, x1, y2, z2, x2, y2, z2, x2, y2, z1, r, g, b, a);
        addTriangle(builder, matrix, x1, y2, z2, x2, y2, z1, x1, y2, z1, r, g, b, a);
        // Nord
        addTriangle(builder, matrix, x1, y1, z1, x1, y2, z1, x2, y2, z1, r, g, b, a);
        addTriangle(builder, matrix, x1, y1, z1, x2, y2, z1, x2, y1, z1, r, g, b, a);
        // Süd
        addTriangle(builder, matrix, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, a);
        addTriangle(builder, matrix, x2, y1, z2, x1, y2, z2, x1, y1, z2, r, g, b, a);
        // West
        addTriangle(builder, matrix, x1, y1, z2, x1, y2, z2, x1, y2, z1, r, g, b, a);
        addTriangle(builder, matrix, x1, y1, z2, x1, y2, z1, x1, y1, z1, r, g, b, a);
        // Ost
        addTriangle(builder, matrix, x2, y1, z1, x2, y2, z1, x2, y2, z2, r, g, b, a);
        addTriangle(builder, matrix, x2, y1, z1, x2, y2, z2, x2, y1, z2, r, g, b, a);
    }

    private void addTriangle(VertexConsumer builder, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float r, float g, float b, float a) {
        builder.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        builder.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        builder.vertex(matrix, x3, y3, z3).color(r, g, b, a);
    }

    // (Deine anderen Hilfsmethoden getFullArea, drawBoxOutline etc. hier behalten)
    private Box getFullArea(BlockPos p1, BlockPos p2) {
        int minX = Math.min(p1.getX(), p2.getX()); int minY = Math.min(p1.getY(), p2.getY()); int minZ = Math.min(p1.getZ(), p2.getZ());
        int maxX = Math.max(p1.getX(), p2.getX()) + 1; int maxY = Math.max(p1.getY(), p2.getY()) + 1; int maxZ = Math.max(p1.getZ(), p2.getZ()) + 1;
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void drawBoxOutline(MatrixStack matrices, VertexConsumer builder, Box box, float r, float g, float b, float a) {
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

    private void drawLineWithNormal(VertexConsumer builder, Matrix4f matrix, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        float nx = (float)(x2 - x1); float ny = (float)(y2 - y1); float nz = (float)(z2 - z1);
        float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0) { nx /= len; ny /= len; nz /= len; }
        builder.vertex(matrix, (float)x1, (float)y1, (float)z1).color(r, g, b, a).normal(nx, ny, nz);
        builder.vertex(matrix, (float)x2, (float)y2, (float)z2).color(r, g, b, a).normal(nx, ny, nz);
    }

    private BlockPos getPos(NbtCompound nbt, String key) {
        if (nbt.contains(key)) {
            int[] arr = nbt.getIntArray(key).orElse(new int[0]);
            if (arr.length == 3) return new BlockPos(arr[0], arr[1], arr[2]);
        }
        return null;
    }
}