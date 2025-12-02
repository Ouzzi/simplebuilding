package com.simplebuilding.client.gui;

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
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class RangefinderHudOverlay implements HudRenderCallback {

    // Farben (Vanilla Tooltip Style)
    private static final int BACKGROUND_COLOR = 0xF0100010;
    private static final int BORDER_COLOR_START = 0xF03f0073;
    private static final int BORDER_COLOR_END = 0xF0250061;

    // Custom Text Farben (Deine RGB Werte als Hex)
    private static final int COLOR_POS_1 = 0xFF7F4C;  // Orange
    private static final int COLOR_POS_2 = 0xFFD866;  // Gelb
    private static final int COLOR_RESULT = 0xCC9966; // Mix

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        ItemStack stack = client.player.getMainHandStack();
        if (!(stack.getItem() instanceof OctantItem)) {
            stack = client.player.getOffHandStack();
            if (!(stack.getItem() instanceof OctantItem)) {
                return;
            }
        }

        NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtData.copyNbt();

        List<Text> lines = new ArrayList<>();

        // --- TITEL LOGIK (Enchantment Check) ---
        Formatting titleColor = stack.hasEnchantments() ? Formatting.AQUA : Formatting.WHITE;
        lines.add(Text.literal("Rangefinder").formatted(titleColor));

        BlockPos pos1 = null;
        BlockPos pos2 = null;

        if (nbt.contains("Pos1")) {
            int[] p1 = nbt.getIntArray("Pos1").orElse(new int[0]);
            if (p1.length == 3) {
                pos1 = new BlockPos(p1[0], p1[1], p1[2]);
                lines.add(Text.literal("Pos 1: " + pos1.getX() + ", " + pos1.getY() + ", " + pos1.getZ())
                        .setStyle(Style.EMPTY.withColor(COLOR_POS_1)));
            }
        } else {
            lines.add(Text.literal("Right-Click block to set Pos 1").formatted(Formatting.GRAY));
        }

        if (nbt.contains("Pos2")) {
            int[] p2 = nbt.getIntArray("Pos2").orElse(new int[0]);
            if (p2.length == 3) {
                pos2 = new BlockPos(p2[0], p2[1], p2[2]);
                lines.add(Text.literal("Pos 2: " + pos2.getX() + ", " + pos2.getY() + ", " + pos2.getZ())
                        .setStyle(Style.EMPTY.withColor(COLOR_POS_2)));
            }
        } else if (pos1 != null) {
            lines.add(Text.literal("Sneak + R-Click to set Pos 2").formatted(Formatting.GRAY));
        }

        if (pos1 != null && pos2 != null) {
            int dx = Math.abs(pos1.getX() - pos2.getX()) + 1;
            int dy = Math.abs(pos1.getY() - pos2.getY()) + 1;
            int dz = Math.abs(pos1.getZ() - pos2.getZ()) + 1;

            if (dy == 1 && (dx == 1 || dz == 1)) {
                // Distance
                lines.add(Text.literal("Distance: " + Math.max(dx, dz) + " blocks")
                        .setStyle(Style.EMPTY.withColor(COLOR_RESULT)));
            } else if (dy == 1) {
                // Area
                lines.add(Text.literal("Area: " + (dx * dz) + " blocks²")
                        .setStyle(Style.EMPTY.withColor(COLOR_RESULT)));
                lines.add(Text.literal("(" + dx + " x " + dz + ")").formatted(Formatting.GRAY));
            } else {
                // Volume
                lines.add(Text.literal("Volume: " + (dx * dy * dz) + " blocks³")
                        .setStyle(Style.EMPTY.withColor(COLOR_RESULT)));
                lines.add(Text.literal("(" + dx + " x " + dy + " x " + dz + ")").formatted(Formatting.GRAY));
            }
        }

        if (lines.isEmpty()) return;

        // --- RENDERING ---
        TextRenderer textRenderer = client.textRenderer;
        int screenHeight = context.getScaledWindowHeight();

        // Maße berechnen
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
        int titleSpacing = 4; // EXTRA ABSTAND UNTER TITEL

        // Box Höhe anpassen (Extra Platz für Titel)
        int totalTextHeight = (lines.size() * textRenderer.fontHeight)
                            + ((lines.size() - 1) * lineSpacing)
                            + titleSpacing* (lines.size() > 3 ? 2 : 1); // Hier addieren wir den Extra-Abstand

        int boxWidth = finalContentWidth + (paddingX * 2);
        int boxHeight = totalTextHeight + (paddingY * 2);

        int x = 10;
        int y = (screenHeight / 2) - (boxHeight / 2);
        int z = 400; // Z-Level (Wichtig für Sortierung, auch wenn drawContext 2D ist, nutzen wir die Reihenfolge)

        context.fill(x + 1, y + 1, x + boxWidth - 1, y + boxHeight - 1, BACKGROUND_COLOR);
        context.fill(x + 1, y, x + boxWidth - 1, y + 1, BACKGROUND_COLOR); // Verschiebung y-1 war falsch im Kontext von fill(y, y+1)

        // 1. Hintergrund Border
        context.fill(x + 1, y - 1, x + boxWidth - 1, y, BACKGROUND_COLOR);
        context.fill(x + 1, y + boxHeight, x + boxWidth - 1, y + boxHeight + 1, BACKGROUND_COLOR);
        context.fill(x - 1, y + 1, x, y + boxHeight - 1, BACKGROUND_COLOR);
        context.fill(x + boxWidth, y + 1, x + boxWidth + 1, y + boxHeight - 1, BACKGROUND_COLOR);

        // Hintergrund Border Ecken
        context.fillGradient(x + boxWidth - 1, y, x + boxWidth, y + 1, BACKGROUND_COLOR, BACKGROUND_COLOR);
        context.fillGradient(x, y, x + 1, y + 1, BACKGROUND_COLOR, BACKGROUND_COLOR);
        context.fillGradient(x + boxWidth - 1, y + boxHeight - 1, x + boxWidth, y + boxHeight, BACKGROUND_COLOR, BACKGROUND_COLOR);
        context.fillGradient(x, y + boxHeight - 1, x + 1, y + boxHeight, BACKGROUND_COLOR, BACKGROUND_COLOR);

        // 2. Border
        context.fillGradient(x + 1, y, x + boxWidth - 1, y + 1, BORDER_COLOR_START, BORDER_COLOR_START);
        context.fillGradient(x + 1, y + boxHeight - 1, x + boxWidth - 1, y + boxHeight, BORDER_COLOR_END, BORDER_COLOR_END);
        context.fillGradient(x, y + 1, x + 1, y + boxHeight - 1, BORDER_COLOR_START, BORDER_COLOR_END);
        context.fillGradient(x + boxWidth - 1, y + 1, x + boxWidth, y + boxHeight - 1, BORDER_COLOR_START, BORDER_COLOR_END);

        // --- TEXT ---
        int textY = y + paddingY;

        for (int i = 0; i < lines.size(); i++) {
            Text line = lines.get(i);
            context.drawTextWithShadow(textRenderer, line, x + paddingX, textY, 0xFFFFFFFF);
            textY += textRenderer.fontHeight + lineSpacing;
            if (i == 0 || i == 2) textY += titleSpacing;
        }
    }
}