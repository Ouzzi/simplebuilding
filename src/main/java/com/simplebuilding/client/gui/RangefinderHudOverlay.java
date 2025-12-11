package com.simplebuilding.client.gui;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OctantItem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class RangefinderHudOverlay implements HudRenderCallback {

    // Standard Farben (Vanilla Tooltip Style)
    private static final int BACKGROUND_COLOR = 0xF0100010;

    // Default (Keine Farbe / Original Octant)
    private static final int DEFAULT_BORDER_START = 0xF03f0073;
    private static final int DEFAULT_BORDER_END = 0xF0250061;
    private static final int DEFAULT_POS_1 = 0xFF7F4C; // Hell-Orange
    private static final int DEFAULT_POS_2 = 0xFFD866; // Hell-Gelb
    private static final int DEFAULT_RESULT = 0xCC9966;

    // Helper Klasse für Farb-Sets
    private record ColorTheme(int pos1, int pos2, int borderStart, int borderEnd) {}

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        ItemStack stack = client.player.getMainHandStack();
        boolean hasOctant = stack.getItem() instanceof OctantItem;

        if (!hasOctant) {
            stack = client.player.getOffHandStack();
            if (stack.getItem() instanceof OctantItem) {
                hasOctant = true;
            }
        }

        if (!hasOctant) return;

        // Octant Item und Farbe holen
        OctantItem octantItem = (OctantItem) stack.getItem();
        DyeColor dyeColor = octantItem.getColor();

        // Farben bestimmen
        ColorTheme theme = getColorTheme(dyeColor);

        // Check für Speedometer (für Positionierung)
        ItemStack main = client.player.getMainHandStack();
        ItemStack off = client.player.getOffHandStack();
        boolean hasSpeedometer = main.isOf(ModItems.VELOCITY_GAUGE) || off.isOf(ModItems.VELOCITY_GAUGE);

        // --- DATA ---
        NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtData.copyNbt();
        List<Text> lines = new ArrayList<>();

        Formatting titleColor = stack.hasEnchantments() ? Formatting.AQUA : Formatting.WHITE;
        lines.add(Text.literal("Octant").formatted(titleColor));

        BlockPos pos1 = null;
        BlockPos pos2 = null;

        if (nbt.contains("Pos1")) {
            int[] p1 = nbt.getIntArray("Pos1").orElse(new int[0]);
            if (p1.length == 3) {
                pos1 = new BlockPos(p1[0], p1[1], p1[2]);
                lines.add(Text.literal("Pos 1: " + pos1.getX() + ", " + pos1.getY() + ", " + pos1.getZ())
                        .setStyle(Style.EMPTY.withColor(theme.pos1)));
            }
        } else {
            lines.add(Text.literal("Right-Click block to set Pos 1").formatted(Formatting.GRAY));
        }

        if (nbt.contains("Pos2")) {
            int[] p2 = nbt.getIntArray("Pos2").orElse(new int[0]);
            if (p2.length == 3) {
                pos2 = new BlockPos(p2[0], p2[1], p2[2]);
                lines.add(Text.literal("Pos 2: " + pos2.getX() + ", " + pos2.getY() + ", " + pos2.getZ())
                        .setStyle(Style.EMPTY.withColor(theme.pos2)));
            }
        } else if (pos1 != null) {
            lines.add(Text.literal("Sneak + R-Click to set Pos 2").formatted(Formatting.GRAY));
        }

        if (pos1 != null && pos2 != null) {
            int dx = Math.abs(pos1.getX() - pos2.getX()) + 1;
            int dy = Math.abs(pos1.getY() - pos2.getY()) + 1;
            int dz = Math.abs(pos1.getZ() - pos2.getZ()) + 1;

            // Resultat nutzt Pos1 Farbe (hell) oder Standard
            int resultColor = theme.pos1;

            if (dy == 1 && (dx == 1 || dz == 1)) {
                lines.add(Text.literal("Distance: " + Math.max(dx, dz) + " blocks").setStyle(Style.EMPTY.withColor(resultColor)));
            } else if (dy == 1) {
                lines.add(Text.literal("Area: " + (dx * dz) + " blocks²").setStyle(Style.EMPTY.withColor(resultColor)));
                lines.add(Text.literal("(" + dx + " x " + dz + ")").formatted(Formatting.GRAY));
            } else {
                lines.add(Text.literal("Volume: " + (dx * dy * dz) + " blocks³").setStyle(Style.EMPTY.withColor(resultColor)));
                lines.add(Text.literal("(" + dx + " x " + dy + " x " + dz + ")").formatted(Formatting.GRAY));
            }
        }

        if (lines.isEmpty()) return;

        // --- RENDER ---
        TextRenderer textRenderer = client.textRenderer;
        int screenHeight = context.getScaledWindowHeight();

        int actualTextWidth = 0;
        for (Text line : lines) {
            int w = textRenderer.getWidth(line);
            if (w > actualTextWidth) actualTextWidth = w;
        }
        String sampleString = "Pos 2: -8888, 888, -8888";
        int minSafeWidth = textRenderer.getWidth(sampleString);
        int finalContentWidth = Math.max(actualTextWidth, minSafeWidth);

        int paddingX = 6;
        int paddingY = 6;
        int lineSpacing = 2;
        int titleSpacing = 4;

        int totalTextHeight = (lines.size() * textRenderer.fontHeight) + ((lines.size() - 1) * lineSpacing) + titleSpacing * (lines.size() > 3 ? 2 : 1);
        int boxWidth = finalContentWidth + (paddingX * 2);
        int boxHeight = totalTextHeight + (paddingY * 2);

        int x = 10;
        int y = (screenHeight / 2) - (boxHeight / 2);

        // POSITIONIERUNG: Hochrutschen wenn Speedometer da ist
        if (hasSpeedometer) {
            y -= 35;
        }

        // Background
        context.fill(x + 1, y + 1, x + boxWidth - 1, y + boxHeight - 1, BACKGROUND_COLOR);
        context.fill(x + 1, y, x + boxWidth - 1, y + 1, BACKGROUND_COLOR);

        // 1. Hintergrund Border (schwarz/dunkel)
        context.fill(x + 1, y - 1, x + boxWidth - 1, y, BACKGROUND_COLOR);
        context.fill(x + 1, y + boxHeight, x + boxWidth - 1, y + boxHeight + 1, BACKGROUND_COLOR);
        context.fill(x - 1, y + 1, x, y + boxHeight - 1, BACKGROUND_COLOR);
        context.fill(x + boxWidth, y + 1, x + boxWidth + 1, y + boxHeight - 1, BACKGROUND_COLOR);

        // Hintergrund Border Ecken
        context.fillGradient(x + boxWidth - 1, y, x + boxWidth, y + 1, BACKGROUND_COLOR, BACKGROUND_COLOR);
        context.fillGradient(x, y, x + 1, y + 1, BACKGROUND_COLOR, BACKGROUND_COLOR);
        context.fillGradient(x + boxWidth - 1, y + boxHeight - 1, x + boxWidth, y + boxHeight, BACKGROUND_COLOR, BACKGROUND_COLOR);
        context.fillGradient(x, y + boxHeight - 1, x + 1, y + boxHeight, BACKGROUND_COLOR, BACKGROUND_COLOR);

        // 2. Border (Farbig, angepasst an Octant)
        context.fillGradient(x + 1, y, x + boxWidth - 1, y + 1, theme.borderStart, theme.borderStart);
        context.fillGradient(x + 1, y + boxHeight - 1, x + boxWidth - 1, y + boxHeight, theme.borderEnd, theme.borderEnd);
        context.fillGradient(x, y + 1, x + 1, y + boxHeight - 1, theme.borderStart, theme.borderEnd);
        context.fillGradient(x + boxWidth - 1, y + 1, x + boxWidth, y + boxHeight - 1, theme.borderStart, theme.borderEnd);

        // --- TEXT ---
        int textY = y + paddingY;

        for (int i = 0; i < lines.size(); i++) {
            Text line = lines.get(i);
            context.drawTextWithShadow(textRenderer, line, x + paddingX, textY, 0xFFFFFFFF);
            textY += textRenderer.fontHeight + lineSpacing;
            if (i == 0 || i == 2) textY += titleSpacing;
        }
    }

    /**
     * Gibt ein Farb-Theme zurück basierend auf der Octant Farbe.
     * Format: Hell (Pos1), Dunkel (Pos2), BorderStart, BorderEnd (basierend auf Dunkel)
     */
    private ColorTheme getColorTheme(DyeColor color) {
        if (color == null) {
            return new ColorTheme(DEFAULT_POS_1, DEFAULT_POS_2, DEFAULT_BORDER_START, DEFAULT_BORDER_END);
        }

        switch (color) {
            case WHITE -> { return new ColorTheme(0xFFFFFF, 0xAAAAAA, 0xF0AAAAAA, 0xF0555555); }
            case ORANGE -> { return new ColorTheme(0xFFAD33, 0xFF7F00, 0xF0FF7F00, 0xF0994C00); }
            case MAGENTA -> { return new ColorTheme(0xFF55FF, 0xAA00AA, 0xF0AA00AA, 0xF0550055); }
            case LIGHT_BLUE -> { return new ColorTheme(0x66FFFF, 0x3388FF, 0xF03388FF, 0xF00044AA); }
            case YELLOW -> { return new ColorTheme(0xFFFF66, 0xDDDD00, 0xF0DDDD00, 0xF0888800); }
            case LIME -> { return new ColorTheme(0x88FF33, 0x44CC00, 0xF044CC00, 0xF0226600); }
            case PINK -> { return new ColorTheme(0xFF99CC, 0xFF6699, 0xF0FF6699, 0xF0AA4466); }
            case GRAY -> { return new ColorTheme(0xAAAAAA, 0x666666, 0xF0666666, 0xF0333333); }
            case LIGHT_GRAY -> { return new ColorTheme(0xDDDDDD, 0x999999, 0xF0999999, 0xF0555555); }
            case CYAN -> { return new ColorTheme(0x00FFFF, 0x00AAAA, 0xF000AAAA, 0xF0005555); }
            case PURPLE -> { return new ColorTheme(0xCC66FF, 0x9933CC, 0xF09933CC, 0xF0551188); }
            case BLUE -> { return new ColorTheme(0x6699FF, 0x3344FF, 0xF03344FF, 0xF0001199); }
            case BROWN -> { return new ColorTheme(0xCC9966, 0x885533, 0xF0885533, 0xF0442211); }
            case GREEN -> { return new ColorTheme(0x66FF66, 0x00AA00, 0xF000AA00, 0xF0005500); }
            case RED -> { return new ColorTheme(0xFF6666, 0xCC0000, 0xF0CC0000, 0xF0660000); }
            case BLACK -> { return new ColorTheme(0xAAAAAA, 0x555555, 0xF0333333, 0xF0111111); } // Schwarz braucht hellen Text
            default -> { return new ColorTheme(DEFAULT_POS_1, DEFAULT_POS_2, DEFAULT_BORDER_START, DEFAULT_BORDER_END); }
        }
    }
}