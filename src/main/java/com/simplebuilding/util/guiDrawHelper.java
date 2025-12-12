package com.simplebuilding.util;

import com.simplebuilding.client.gui.RangefinderHudOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;

public class guiDrawHelper {

    // --- Drawing Methods (Unverändert) ---
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

    // --- Helper Methods ---

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

    // --- HUD Colors ---
    private static final int DEFAULT_BACKGROUND = 0xF0100010;
    private static final int DEFAULT_BORDER_START = 0xF05000FF;
    private static final int DEFAULT_BORDER_END = 0xF028007F;
    private static final int DEFAULT_POS_1 = 0xFFFFAA00;
    private static final int DEFAULT_POS_2 = 0xFFFFFF55;

    public record ColorTheme(int pos1, int pos2, int borderStart, int borderEnd, int background) {}

    /**
     * Gibt ein Farb-Theme für das HUD zurück.
     * - Pos1 (Hell)
     * - Pos2 (Mittel)
     * - Border (Passend zur Farbe)
     * - Background (Sehr dunkel, passend zur Farbe)
     */
    public static ColorTheme getColorTheme(DyeColor color) {
        if (color == null) {
            // Standard: Lila/Schwarz (Vanilla Style)
            return new ColorTheme(DEFAULT_POS_1, DEFAULT_POS_2, DEFAULT_BORDER_START, DEFAULT_BORDER_END, DEFAULT_BACKGROUND);
        }

        // Hintergrundfarben sind immer sehr dunkel (F0 Alpha + dunkle RGB Werte), damit Text lesbar bleibt.

        switch (color) {
            case WHITE -> {
                return new ColorTheme(0xFFFFFF, 0xAAAAAA, 0xF0FFFFFF, 0xF0AAAAAA, 0xF0252525);
            }
            case ORANGE -> {
                return new ColorTheme(0xFFAD33, 0xFF7F00, 0xF0FFAA00, 0xF0AA5500, 0xF02A1500);
            }
            case MAGENTA -> {
                return new ColorTheme(0xFF55FF, 0xAA00AA, 0xF0FF55FF, 0xF0AA00AA, 0xF01A001A);
            }
            case LIGHT_BLUE -> {
                return new ColorTheme(0x66FFFF, 0x3388FF, 0xF066FFFF, 0xF03388FF, 0xF0001020);
            }
            case YELLOW -> {
                return new ColorTheme(0xFFFF66, 0xDDDD00, 0xF0FFFF66, 0xF0DDDD00, 0xF0252500);
            }
            case LIME -> {
                return new ColorTheme(0x88FF33, 0x44CC00, 0xF088FF33, 0xF044CC00, 0xF0102000);
            }
            case PINK -> {
                return new ColorTheme(0xFF99CC, 0xFF6699, 0xF0FF99CC, 0xF0FF6699, 0xF0201015);
            }
            case GRAY -> {
                return new ColorTheme(0xAAAAAA, 0x666666, 0xF0AAAAAA, 0xF0555555, 0xF0151515);
            }
            case LIGHT_GRAY -> {
                return new ColorTheme(0xDDDDDD, 0x999999, 0xF0DDDDDD, 0xF0999999, 0xF0202020);
            }
            case CYAN -> {
                return new ColorTheme(0x00FFFF, 0x00AAAA, 0xF000FFFF, 0xF000AAAA, 0xF0001515);
            }
            case PURPLE -> {
                return new ColorTheme(0xCC66FF, 0x9933CC, 0xF0CC66FF, 0xF09933CC, 0xF0150020);
            }
            case BLUE -> {
                return new ColorTheme(0x6699FF, 0x3344FF, 0xF06699FF, 0xF03344FF, 0xF0000520);
            }
            case BROWN -> {
                return new ColorTheme(0xCC9966, 0x885533, 0xF0CC9966, 0xF0885533, 0xF01A1005);
            }
            case GREEN -> {
                return new ColorTheme(0x66FF66, 0x00AA00, 0xF066FF66, 0xF000AA00, 0xF0001A00);
            }
            case RED -> {
                return new ColorTheme(0xFF6666, 0xCC0000, 0xF0FF6666, 0xF0CC0000, 0xF0200000);
            }
            case BLACK -> {
                return new ColorTheme(0xAAAAAA, 0x555555, 0xF0555555, 0xF0333333, 0xF0050505);
            }
            default -> {
                return new ColorTheme(DEFAULT_POS_1, DEFAULT_POS_2, DEFAULT_BORDER_START, DEFAULT_BORDER_END, DEFAULT_BACKGROUND);
            }
        }
    }

}