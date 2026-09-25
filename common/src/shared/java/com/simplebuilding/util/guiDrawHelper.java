package com.simplebuilding.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public class guiDrawHelper {

    // --- Drawing Methods (Unverändert) ---

    /**
     * Zeichnet eine Linie.
     * Reihenfolge: Vertex -> Color -> Normal.
     * Das ist der Standard für 1.21 BufferBuilder.
     */
    public static void drawLineWithNormal(VertexConsumer builder, Matrix4f matrix, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        float nx = (float)(x2 - x1); float ny = (float)(y2 - y1); float nz = (float)(z2 - z1);
        float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0) { nx /= len; ny /= len; nz /= len; }

        float lineWidth = 4.0f;

        builder.addVertex(matrix, (float)x1, (float)y1, (float)z1)
                .setColor(r, g, b, a)
                .setNormal(nx, ny, nz)
                .setLineWidth(lineWidth);

        builder.addVertex(matrix, (float)x2, (float)y2, (float)z2)
                .setColor(r, g, b, a)
                .setNormal(nx, ny, nz)
                .setLineWidth(lineWidth);
    }

    public static void drawBoxOutline(PoseStack matrices, VertexConsumer builder, AABB box, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.last().pose();
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

    public static void drawBoxFill(PoseStack matrices, VertexConsumer builder, AABB box, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.last().pose();
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
        builder.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
        builder.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
        builder.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a);
        builder.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a);
    }


    public static void drawQuadFace(PoseStack matrices, VertexConsumer builder, AABB box, Direction face, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.last().pose();
        float x1 = (float)box.minX; float y1 = (float)box.minY; float z1 = (float)box.minZ;
        float x2 = (float)box.maxX; float y2 = (float)box.maxY; float z2 = (float)box.maxZ;

        switch (face) {
            case DOWN ->  addQuad(builder, matrix, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a);
            case UP ->    addQuad(builder, matrix, x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1, r, g, b, a);
            case NORTH -> addQuad(builder, matrix, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, b, a);
            case SOUTH -> addQuad(builder, matrix, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, r, g, b, a);
            case WEST ->  addQuad(builder, matrix, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, r, g, b, a);
            case EAST ->  addQuad(builder, matrix, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a);
        }
    }

    // --- Helper Methods ---

    public static boolean hasEnchantment(ItemStack stack, Minecraft client, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        if (client.level == null) return false;
        var registry = client.level.registryAccess();
        var enchantments = registry.lookupOrThrow(Registries.ENCHANTMENT);
        var entry = enchantments.get(key);
        return entry.isPresent() && EnchantmentHelper.getItemEnchantmentLevel(entry.get(), stack) > 0;
    }

    public static AABB getFullArea(BlockPos p1, BlockPos p2) {
        int minX = Math.min(p1.getX(), p2.getX()); int minY = Math.min(p1.getY(), p2.getY()); int minZ = Math.min(p1.getZ(), p2.getZ());
        int maxX = Math.max(p1.getX(), p2.getX()) + 1; int maxY = Math.max(p1.getY(), p2.getY()) + 1; int maxZ = Math.max(p1.getZ(), p2.getZ()) + 1;
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static BlockPos getPos(CompoundTag nbt, String key) {
        if (nbt.contains(key)) {
            int[] arr = nbt.getIntArray(key).orElse(new int[0]);
            if (arr.length == 3) return new BlockPos(arr[0], arr[1], arr[2]);
        }
        return null;
    }

    // Helper Record für RGB Farben (3 Sets: Pos1, Pos2, Area)
    public record RenderColors(float r1, float g1, float b1, float r2, float g2, float b2, float r3, float g3, float b3) {}

    /**
     * Konvertiert die DyeColor in RGB Floats mit STARKEM KONTRAST.
     * Schema:
     * - Pos1 (Hell): Sehr hell, fast Pastell/Weiß.
     * - Pos2 (Dunkel): Dunkel, gesättigt.
     * - Area (Mittel): Die "echte" Farbe.
     */
    public static RenderColors getRenderColors(DyeColor color) {
        if (color == null) {
            // Default: Hell-Orange (Pos1), Dunkel-Orange (Pos2), Gold-Mix (Area)
            return new RenderColors(1.0f, 0.8f, 0.4f, 0.8f, 0.4f, 0.0f, 1.0f, 0.6f, 0.0f);
        }

        // Helper Funktion zum Umrechnen von Hex (0xFF0000) zu r,g,b floats
        return switch (color) {
            case WHITE -> fromHex3(0xFFFFFF, 0x555555, 0xAAAAAA);       // Weiß -> Dunkelgrau -> Grau
            case ORANGE -> fromHex3(0xFFDDBB, 0xCC5500, 0xFF8800);      // Hellorange -> Dunkelorange -> Orange
            case MAGENTA -> fromHex3(0xFFCCFF, 0x770077, 0xCC00CC);     // Hellmagenta -> Dunkelmagenta -> Magenta
            case LIGHT_BLUE -> fromHex3(0xCCFFFF, 0x0044AA, 0x3388FF);  // Hellblau -> Dunkelblau -> Blau
            case YELLOW -> fromHex3(0xFFFFCC, 0xAA8800, 0xFFDD00);      // Hellgelb -> Dunkelgold -> Gelb
            case LIME -> fromHex3(0xCCFF99, 0x228800, 0x66CC00);        // Hellgrün -> Dunkelgrün -> Lime
            case PINK -> fromHex3(0xFFDDEE, 0xCC3366, 0xFF6699);        // Hellpink -> Dunkelpink -> Pink
            case GRAY -> fromHex3(0xEEEEEE, 0x333333, 0x888888);        // Hellgrau -> Dunkelgrau -> Grau
            case LIGHT_GRAY -> fromHex3(0xFFFFFF, 0x666666, 0xBBBBBB);  // Weiß -> Grau -> Hellgrau
            case CYAN -> fromHex3(0xCCFFFF, 0x006666, 0x00CCCC);        // Helltürkis -> Dunkeltürkis -> Türkis
            case PURPLE -> fromHex3(0xEEDDFF, 0x440088, 0x9933CC);      // Helllila -> Dunkellila -> Lila
            case BLUE -> fromHex3(0xBBDDFF, 0x0000AA, 0x3366FF);        // Hellblau -> Tiefblau -> Blau
            case BROWN -> fromHex3(0xEEDDBB, 0x553311, 0x885533);       // Beige -> Dunkelbraun -> Braun
            case GREEN -> fromHex3(0xBBFFBB, 0x005500, 0x00AA00);       // Pastellgrün -> Tiefgrün -> Grün
            case RED -> fromHex3(0xFFCCCC, 0x880000, 0xCC0000);         // Hellrot -> Dunkelrot -> Rot
            case BLACK -> fromHex3(0xAAAAAA, 0x000000, 0x333333);       // Grau -> Schwarz -> Dunkelgrau
            default -> new RenderColors(1.0f, 1.0f, 1.0f, 0.5f, 0.5f, 0.5f, 0.75f, 0.75f, 0.75f);
        };
    }

    // Helper für 3 Farben (Pos1, Pos2, Area)
    private static RenderColors fromHex3(int hex1, int hex2, int hex3) {
        float r1 = ((hex1 >> 16) & 0xFF) / 255.0f;
        float g1 = ((hex1 >> 8) & 0xFF) / 255.0f;
        float b1 = (hex1 & 0xFF) / 255.0f;

        float r2 = ((hex2 >> 16) & 0xFF) / 255.0f;
        float g2 = ((hex2 >> 8) & 0xFF) / 255.0f;
        float b2 = (hex2 & 0xFF) / 255.0f;

        float r3 = ((hex3 >> 16) & 0xFF) / 255.0f;
        float g3 = ((hex3 >> 8) & 0xFF) / 255.0f;
        float b3 = (hex3 & 0xFF) / 255.0f;

        return new RenderColors(r1, g1, b1, r2, g2, b2, r3, g3, b3);
    }

    // --- HUD-Farben des Entfernungsmessers ---
    // Nur noch die Textfarben: der HUD-Kasten ist seit dem Umbau Vanillas Tooltip-Hintergrund
    // (RangefinderHudOverlay), die frueheren Rahmen- und Hintergrundfarben je Farbstoff las niemand mehr.
    private static final int DEFAULT_POS_1 = 0xFFFFAA00;
    private static final int DEFAULT_POS_2 = 0xFFFFFF55;

    /** Textfarben der HUD-Zeilen: Pos 1 (hell, auch das Ergebnis) und Pos 2. */
    public record ColorTheme(int pos1, int pos2) {}

    /** Das Farb-Theme fuer das HUD eines (gefaerbten) Entfernungsmessers; {@code null} = ungefaerbt. */
    public static ColorTheme getColorTheme(DyeColor color) {
        if (color == null) {
            return new ColorTheme(DEFAULT_POS_1, DEFAULT_POS_2);
        }
        return switch (color) {
            case WHITE -> new ColorTheme(0xFFFFFF, 0xAAAAAA);
            case ORANGE -> new ColorTheme(0xFFAD33, 0xFF7F00);
            case MAGENTA -> new ColorTheme(0xFF55FF, 0xAA00AA);
            case LIGHT_BLUE -> new ColorTheme(0x66FFFF, 0x3388FF);
            case YELLOW -> new ColorTheme(0xFFFF66, 0xDDDD00);
            case LIME -> new ColorTheme(0x88FF33, 0x44CC00);
            case PINK -> new ColorTheme(0xFF99CC, 0xFF6699);
            case GRAY -> new ColorTheme(0xAAAAAA, 0x666666);
            case LIGHT_GRAY -> new ColorTheme(0xDDDDDD, 0x999999);
            case CYAN -> new ColorTheme(0x00FFFF, 0x00AAAA);
            case PURPLE -> new ColorTheme(0xCC66FF, 0x9933CC);
            case BLUE -> new ColorTheme(0x6699FF, 0x3344FF);
            case BROWN -> new ColorTheme(0xCC9966, 0x885533);
            case GREEN -> new ColorTheme(0x66FF66, 0x00AA00);
            case RED -> new ColorTheme(0xFF6666, 0xCC0000);
            case BLACK -> new ColorTheme(0xAAAAAA, 0x555555);
        };
    }

}