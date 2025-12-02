package com.simplebuilding.client.render;

import com.simplebuilding.enchantment.ModEnchantments; // Import nicht vergessen!
import com.simplebuilding.items.custom.OctantItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;

public class BlockHighlightRenderer {

    // Wird vom Mixin aufgerufen
    public static void render(Matrix4f positionMatrix, Camera camera) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        ItemStack stack = client.player.getMainHandStack();
        boolean isRangefinder = stack.getItem() instanceof OctantItem;

        if (!isRangefinder) {
            stack = client.player.getOffHandStack();
            isRangefinder = stack.getItem() instanceof OctantItem;
        }

        if (isRangefinder) {
            renderHighlights(positionMatrix, camera, stack, client.player.isSneaking());
        }
    }

    private static void renderHighlights(Matrix4f positionMatrix, Camera camera, ItemStack stack, boolean isSneaking) {
        // [Daten laden ...]
        NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtData.copyNbt();

        BlockPos pos1 = getPos(nbt, "Pos1");
        BlockPos pos2 = getPos(nbt, "Pos2");

        if (pos1 == null && pos2 == null) return;

        boolean hasConstructorsTouch = hasEnchantment(stack, MinecraftClient.getInstance(), ModEnchantments.CONSTRUCTORS_TOUCH);
        boolean showFill;
        boolean isInverted = false; //TODO Config für Invertierung hinzufügen

        if (hasConstructorsTouch) {
            showFill = isInverted ? isSneaking : !isSneaking;
        } else {
            showFill = false;
        }

        // --- Rendering Setup ---
        double camX = camera.getPos().x;
        double camY = camera.getPos().y;
        double camZ = camera.getPos().z;

        MatrixStack matrices = new MatrixStack();
        matrices.push();
        matrices.multiplyPositionMatrix(positionMatrix);
        matrices.translate(-camX, -camY, -camZ);

        BufferAllocator allocator = new BufferAllocator(1536);
        VertexConsumerProvider.Immediate consumers = VertexConsumerProvider.immediate(allocator);

        float r1 = 1.0f; float g1 = 0.5f; float b1 = 0.3f;
        float r2 = 1.0f; float g2 = 0.85f; float b2 = 0.4f;
        float r3 = 0.8f; float g3 = 0.6f; float b3 = 0.4f;

        // 1. OUTLINES (Immer sichtbar)
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        float lineAlpha = 0.6f;
        if (pos1 != null) drawBoxOutline(matrices, lines, new Box(pos1).expand(0.02), r1, g1, b1, lineAlpha);
        if (pos2 != null) drawBoxOutline(matrices, lines, new Box(pos2).expand(0.02), r2, g2, b2, lineAlpha);
        if (pos1 != null && pos2 != null && showFill) drawBoxOutline(matrices, lines, getFullArea(pos1, pos2).expand(0.02), r3, g3, b3, lineAlpha);

        consumers.draw(RenderLayer.getLines());

        // 2. FÜLLUNGEN (Bedingt sichtbar durch showFill)
        if (showFill) {
            VertexConsumer fill = consumers.getBuffer(RenderLayer.getDebugQuads());
            float alpha = 0.15f;
            if (pos1 != null) drawBoxFill(matrices, fill, new Box(pos1).expand(0.003), r1, g1, b1, alpha);
            if (pos2 != null) drawBoxFill(matrices, fill, new Box(pos2).expand(0.006), r2, g2, b2, alpha);
            if (pos1 != null && pos2 != null) drawBoxFill(matrices, fill, getFullArea(pos1, pos2).expand(0.009), r3, g3, b3, alpha);

            consumers.draw(RenderLayer.getDebugQuads());
        }

        matrices.pop();
    }

    // --- Hilfsmethoden ---
    private static boolean hasEnchantment(ItemStack stack, MinecraftClient client, net.minecraft.registry.RegistryKey<net.minecraft.enchantment.Enchantment> key) {
        if (client.world == null) return false;
        var registry = client.world.getRegistryManager();
        var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var entry = enchantments.getOptional(key);
        return entry.isPresent() && EnchantmentHelper.getLevel(entry.get(), stack) > 0;
    }

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

    private static Box getFullArea(BlockPos p1, BlockPos p2) {
        int minX = Math.min(p1.getX(), p2.getX()); int minY = Math.min(p1.getY(), p2.getY()); int minZ = Math.min(p1.getZ(), p2.getZ());
        int maxX = Math.max(p1.getX(), p2.getX()) + 1; int maxY = Math.max(p1.getY(), p2.getY()) + 1; int maxZ = Math.max(p1.getZ(), p2.getZ()) + 1;
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void drawBoxOutline(MatrixStack matrices, VertexConsumer builder, Box box, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        double x1 = box.minX; double y1 = box.minY; double z1 = box.minZ;
        double x2 = box.maxX; double y2 = box.maxY; double z2 = box.maxZ;
        drawLineWithNormal(builder, matrix, x1, y1, z1, x2, y1, z1, r, g, b, a);
        drawLineWithNormal(builder, matrix, x2, y1, z1, x2, y1, z2, r, g, b, a);
        drawLineWithNormal(builder, matrix, x2, y1, z2, x1, y1, z2, r, g, b, a);
        drawLineWithNormal(builder, matrix, x1, y1, z2, x1, y1, z1, r, g, b, a);
        drawLineWithNormal(builder, matrix, x1, y2, z1, x2, y2, z1, r, g, b, a);
        drawLineWithNormal(builder, matrix, x2, y2, z1, x2, y2, z2, r, g, b, a);
        drawLineWithNormal(builder, matrix, x2, y2, z2, x1, y2, z2, r, g, b, a);
        drawLineWithNormal(builder, matrix, x1, y2, z2, x1, y2, z1, r, g, b, a);
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

    private static BlockPos getPos(NbtCompound nbt, String key) {
        if (nbt.contains(key)) {
            int[] arr = nbt.getIntArray(key).orElse(new int[0]);
            if (arr.length == 3) return new BlockPos(arr[0], arr[1], arr[2]);
        }
        return null;
    }
}