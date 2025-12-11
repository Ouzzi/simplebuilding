package com.simplebuilding.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;

public class guiDrawHelper {

    public static void drawBoxOutline(MatrixStack matrices, VertexConsumer builder, Box box, float r, float g, float b, float a) {
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

    public static void drawBoxFill(MatrixStack matrices, VertexConsumer builder, Box box, float r, float g, float b, float a) {
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

    public static void addQuad(VertexConsumer builder, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a) {
        builder.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        builder.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        builder.vertex(matrix, x3, y3, z3).color(r, g, b, a);
        builder.vertex(matrix, x4, y4, z4).color(r, g, b, a);
    }

    public static void drawLineWithNormal(VertexConsumer builder, Matrix4f matrix, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        float nx = (float)(x2 - x1); float ny = (float)(y2 - y1); float nz = (float)(z2 - z1);
        float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0) { nx /= len; ny /= len; nz /= len; }

        float lineWidth = 4.0f;

        builder.vertex(matrix, (float)x1, (float)y1, (float)z1)
                .color(r, g, b, a)
                .normal(nx, ny, nz)
                .lineWidth(lineWidth);

        builder.vertex(matrix, (float)x2, (float)y2, (float)z2)
                .color(r, g, b, a)
                .normal(nx, ny, nz)
                .lineWidth(lineWidth);
    }


    public static boolean hasEnchantment(ItemStack stack, MinecraftClient client, net.minecraft.registry.RegistryKey<net.minecraft.enchantment.Enchantment> key) {
        if (client.world == null) return false;
        var registry = client.world.getRegistryManager();
        var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var entry = enchantments.getOptional(key);
        return entry.isPresent() && EnchantmentHelper.getLevel(entry.get(), stack) > 0;
    }

    public static Box getFullArea(BlockPos p1, BlockPos p2) {
        int minX = Math.min(p1.getX(), p2.getX()); int minY = Math.min(p1.getY(), p2.getY()); int minZ = Math.min(p1.getZ(), p2.getZ());
        int maxX = Math.max(p1.getX(), p2.getX()) + 1; int maxY = Math.max(p1.getY(), p2.getY()) + 1; int maxZ = Math.max(p1.getZ(), p2.getZ()) + 1;
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static BlockPos getPos(NbtCompound nbt, String key) {
        if (nbt.contains(key)) {
            int[] arr = nbt.getIntArray(key).orElse(new int[0]);
            if (arr.length == 3) return new BlockPos(arr[0], arr[1], arr[2]);
        }
        return null;
    }
}