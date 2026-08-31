package com.simplebuilding.client.gui;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OctantItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class SpeedometerHudOverlay {

    private static final int BACKGROUND_COLOR = 0xF0100010;
    private static final int BORDER_COLOR_START = 0xF03f0073;
    private static final int BORDER_COLOR_END = 0xF0250061;
    private static final int COLOR_SPEED = 0xFF7F4C;
    private static final int COLOR_STATS = 0xFFAAAAAA;
    private static final int COLOR_DANGER = 0xFFFF5555;
    private static final int COLOR_SAFE = 0xFF55FF55;

    private static double topSpeed = 0.0;
    private static double speedSum = 0.0;
    private static long tickCount = 0;
    private static boolean wasHoldingSpeedometer = false;

    public static void render(GuiGraphicsExtractor context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // 1. Prüfen ob Speedometer gehalten wird (Main oder Offhand)
        ItemStack main = client.player.getMainHandItem();
        ItemStack off = client.player.getOffhandItem();
        boolean hasSpeedometer = main.is(ModItems.VELOCITY_GAUGE) || off.is(ModItems.VELOCITY_GAUGE);

        if (!hasSpeedometer) {
            if (wasHoldingSpeedometer) {
                // Reset Stats wenn Item weggelegt wird
                topSpeed = 0.0;
                speedSum = 0.0;
                tickCount = 0;
                wasHoldingSpeedometer = false;
            }
            return;
        }
        wasHoldingSpeedometer = true;

        // 2. Prüfen ob Octant gehalten wird (für Positionierung)
        // Wir prüfen, ob eines der Items eine Instanz von OctantItem ist
        boolean hasOctant = (main.getItem() instanceof OctantItem) || (off.getItem() instanceof OctantItem);

        // --- BERECHNUNG ---
        double velX = client.player.getDeltaMovement().x;
        double velY = client.player.getDeltaMovement().y;
        double velZ = client.player.getDeltaMovement().z;

        // FIX: Wenn der Spieler auf dem Boden steht, ignorieren wir die vertikale Schwerkraft.
        // Sonst wird permanent ca. 1.6 b/s angezeigt.
        if (client.player.onGround()) {
            velY = 0;
        }

        double speedPerTick = Math.sqrt(velX * velX + velY * velY + velZ * velZ);
        double speedBps = speedPerTick * 20.0; // Blocks per Second

        // Rundungsfehler bei sehr kleinen Werten filtern (z.B. 0.00001 b/s beim Stehenbleiben)
        if (speedBps < 0.05) speedBps = 0.0;

        if (speedBps > topSpeed) topSpeed = speedBps;
        if (speedBps > 0.1) {
            speedSum += speedBps;
            tickCount++;
        }
        double avgSpeed = tickCount > 0 ? (speedSum / tickCount) : 0.0;

        // --- TEXT ZUSAMMENSTELLEN ---
        List<Component> lines = new ArrayList<>();

        // Welcher Stack ist der Speedometer? (Für Enchantment Check)
        ItemStack activeStack = main.is(ModItems.VELOCITY_GAUGE) ? main : off;
        boolean isEnchanted = activeStack.isEnchanted();

        // Titel Zeile
        ChatFormatting titleColor = isEnchanted ? ChatFormatting.AQUA : ChatFormatting.WHITE;
        lines.add(Component.literal("Speedometer").withStyle(titleColor));

        // Zeile 1: Aktueller Speed
        lines.add(Component.literal(String.format("%.1f b/s", speedBps))
                .setStyle(Style.EMPTY.withColor(COLOR_SPEED)));

        // Zeile 2 & 3: Extras (Nur wenn Enchanted / Constructor's Touch)
        if (isEnchanted) {
            // FIX: Wir nutzen getFlag(7) statt isFallFlying(), da das immer existiert (Flag 7 = Elytra Glide)
            boolean isFlying = client.player.isFallFlying();

            if (isFlying) {
                // Symbol je nach Gefahr
                boolean danger = speedBps > 15.0;
                Component symbol = Component.literal(danger ? "⚠ " : "✔ ")
                        .setStyle(Style.EMPTY.withColor(danger ? COLOR_DANGER : COLOR_SAFE).withBold(true));

                // Stats Text ("Top: 20.1 Avg: 15.0") in Grau
                Component stats = Component.literal(String.format("Top: %.1f  Avg: %.1f", topSpeed, avgSpeed))
                        .setStyle(Style.EMPTY.withColor(COLOR_STATS));

                // Zusammenbauen: "⚠ Top: ... Avg: ..."
                lines.add(Component.empty().append(symbol).append(stats));
            }
            else {
                lines.add(Component.literal(String.format("Top: %.1f  Avg: %.1f", topSpeed, avgSpeed))
                        .setStyle(Style.EMPTY.withColor(COLOR_STATS)));
                double xBps = Math.abs(velX * 20);
                double zBps = Math.abs(velZ * 20);
                String vecText = String.format("X: %.1f  Z: %.1f", xBps, zBps);
                lines.add(Component.literal(vecText).withStyle(ChatFormatting.GRAY));
            }
        }

        if (lines.isEmpty()) return;

        // --- RENDERING ---
        Font textRenderer = client.font;
        int screenHeight = context.guiHeight();

        // Breite berechnen
        int actualTextWidth = 0;
        for (Component line : lines) {
            int w = textRenderer.width(line);
            if (w > actualTextWidth) actualTextWidth = w;
        }

        int minSafeWidth = 90;
        int finalContentWidth = Math.max(actualTextWidth, minSafeWidth);

        int paddingX = 6;
        int paddingY = 6;
        int lineSpacing = 2;
        int titleSpacing = 4;

        int totalTextHeight = (lines.size() * textRenderer.lineHeight)
                + ((lines.size() - 1) * lineSpacing)
                + titleSpacing;

        int boxWidth = finalContentWidth + (paddingX * 2);
        int boxHeight = totalTextHeight + (paddingY * 2);

        int x = 10;
        int y = (screenHeight / 2) - (boxHeight / 2);

        // POSITIONIERUNG ANPASSEN
        if (hasOctant) {
            y += 35;
        }

        // Draw Box
        context.fill(x + 1, y + 1, x + boxWidth - 1, y + boxHeight - 1, BACKGROUND_COLOR);
        context.fill(x + 1, y, x + boxWidth - 1, y + 1, BACKGROUND_COLOR);

        // Borders
        context.fill(x + 1, y - 1, x + boxWidth - 1, y, BACKGROUND_COLOR);
        context.fill(x + 1, y + boxHeight, x + boxWidth - 1, y + boxHeight + 1, BACKGROUND_COLOR);
        context.fill(x - 1, y + 1, x, y + boxHeight - 1, BACKGROUND_COLOR);
        context.fill(x + boxWidth, y + 1, x + boxWidth + 1, y + boxHeight - 1, BACKGROUND_COLOR);

        // Hintergrund Border Ecken
        context.fillGradient(x + boxWidth - 1, y, x + boxWidth, y + 1, BACKGROUND_COLOR, BACKGROUND_COLOR);
        context.fillGradient(x, y, x + 1, y + 1, BACKGROUND_COLOR, BACKGROUND_COLOR);
        context.fillGradient(x + boxWidth - 1, y + boxHeight - 1, x + boxWidth, y + boxHeight, BACKGROUND_COLOR, BACKGROUND_COLOR);
        context.fillGradient(x, y + boxHeight - 1, x + 1, y + boxHeight, BACKGROUND_COLOR, BACKGROUND_COLOR);

        // Gradient Border
        context.fillGradient(x + 1, y, x + boxWidth - 1, y + 1, BORDER_COLOR_START, BORDER_COLOR_START);
        context.fillGradient(x + 1, y + boxHeight - 1, x + boxWidth - 1, y + boxHeight, BORDER_COLOR_END, BORDER_COLOR_END);
        context.fillGradient(x, y + 1, x + 1, y + boxHeight - 1, BORDER_COLOR_START, BORDER_COLOR_END);
        context.fillGradient(x + boxWidth - 1, y + 1, x + boxWidth, y + boxHeight - 1, BORDER_COLOR_START, BORDER_COLOR_END);

        // Draw Text
        int textY = y + paddingY;
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            context.text(textRenderer, line, x + paddingX, textY, 0xFFFFFFFF);
            textY += textRenderer.lineHeight + lineSpacing;
            if (i == 0) textY += titleSpacing;
        }
    }
}