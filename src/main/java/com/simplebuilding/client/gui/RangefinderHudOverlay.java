package com.simplebuilding.client.gui;

import com.simplebuilding.items.custom.RangefinderItem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

// HUD Overlay for Rangefinder Item
// (Textbox)
public class RangefinderHudOverlay implements HudRenderCallback {

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Is Rangefinder in hand?
        ItemStack stack = client.player.getMainHandStack();
        if (!(stack.getItem() instanceof RangefinderItem)) {
            stack = client.player.getOffHandStack();
            if (!(stack.getItem() instanceof RangefinderItem)) {
                return;
            }
        }

        NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtData.copyNbt();

        List<String> lines = new ArrayList<>();
        BlockPos pos1 = null;
        BlockPos pos2 = null;

        if (nbt.contains("Pos1")) {
            int[] p1 = nbt.getIntArray("Pos1").orElse(new int[0]);
            if (p1.length == 3) {
                pos1 = new BlockPos(p1[0], p1[1], p1[2]);
                lines.add("§ePos 1: §f" + pos1.getX() + ", " + pos1.getY() + ", " + pos1.getZ());
            }
        } else {
            lines.add("§7Right-Click block to set Pos 1");
        }

        if (nbt.contains("Pos2")) {
            int[] p2 = nbt.getIntArray("Pos2").orElse(new int[0]);
            if (p2.length == 3) {
                pos2 = new BlockPos(p2[0], p2[1], p2[2]);
                lines.add("§aPos 2: §f" + pos2.getX() + ", " + pos2.getY() + ", " + pos2.getZ());
            }
        } else if (pos1 != null) {
            lines.add("§7Sneak + R-Click to set Pos 2");
        }

        // Distance / Area / Volume
        if (pos1 != null && pos2 != null) {
            int dx = Math.abs(pos1.getX() - pos2.getX()) + 1;
            int dy = Math.abs(pos1.getY() - pos2.getY()) + 1;
            int dz = Math.abs(pos1.getZ() - pos2.getZ()) + 1;

            lines.add(""); // Leerzeile
            if (dy == 1 && (dx == 1 || dz == 1)) {
                lines.add("§bDistance: §f" + Math.max(dx, dz) + " blocks");
            } else if (dy == 1) {
                lines.add("§bArea: §f" + (dx * dz) + " blocks²");
                lines.add("§7(" + dx + " x " + dz + ")");
            } else {
                lines.add("§bVolume: §f" + (dx * dy * dz) + " blocks³");
                lines.add("§7(" + dx + " x " + dy + " x " + dz + ")");
            }
        }

        // --- RENDERING ---
        if (lines.isEmpty()) return;

        TextRenderer textRenderer = client.textRenderer;
        int screenHeight = context.getScaledWindowHeight();

        // Box Dimensionen
        int padding = 6;
        int lineHeight = textRenderer.fontHeight + 2;
        int totalTextHeight = lines.size() * lineHeight;

        int minWidth = 120;
        int maxTextWidth = 0;

        for (String line : lines) {
            int w = textRenderer.getWidth(line);
            if (w > maxTextWidth) maxTextWidth = w;
        }

        int boxWidth = Math.max(minWidth, maxTextWidth + (padding * 2));
        int boxHeight = totalTextHeight + (padding * 2) - 2;

        // Position: Links, vertikal zentriert
        int x = 10;
        int y = (screenHeight / 2) - (boxHeight / 2);

        // HIER WAREN DIE FEHLERHAFTEN MATRIX BEFEHLE - WIR BRAUCHEN SIE NICHT!
        // DrawContext zeichnet automatisch auf der korrekten HUD-Ebene.

        // 1. Hintergrund (Dunkelgrau, transparent)
        context.fill(x, y, x + boxWidth, y + boxHeight, 0x90000000);

        // 2. Rahmen (Weiß, opak)
        int borderColor = 0xFFFFFFFF;
        context.fill(x, y, x + boxWidth, y + 1, borderColor); // Oben
        context.fill(x, y + boxHeight - 1, x + boxWidth, y + boxHeight, borderColor); // Unten
        context.fill(x, y, x + 1, y + boxHeight, borderColor); // Links
        context.fill(x + boxWidth - 1, y, x + boxWidth, y + boxHeight, borderColor); // Rechts

        // 3. Text zeichnen
        int textY = y + padding;
        for (String line : lines) {
            // Farbe 0xFFFFFFFF (Voll sichtbar Weiß)
            context.drawTextWithShadow(textRenderer, line, x + padding, textY, 0xFFFFFFFF);
            textY += lineHeight;
        }
    }
}