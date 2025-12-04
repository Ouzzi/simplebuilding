package com.simplebuilding.client.gui;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OctantItem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class SpeedometerHudOverlay implements HudRenderCallback {

    // Farben (Lila Theme wie Octant)
    private static final int BACKGROUND_COLOR = 0xF0100010;
    private static final int BORDER_COLOR_START = 0xF03f0073;
    private static final int BORDER_COLOR_END = 0xF0250061;

    // Text Farben
    private static final int COLOR_SPEED = 0xFF7F4C; // Orange
    private static final int COLOR_STATS = 0xFFAAAAAA; // Grau
    private static final int COLOR_DANGER = 0xFFFF5555; // Rot
    private static final int COLOR_SAFE   = 0xFF55FF55; // Grün

    // --- SESSION DATEN ---
    private static double topSpeed = 0.0;
    private static double speedSum = 0.0;
    private static long tickCount = 0;
    private static boolean wasHoldingSpeedometer = false;

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // 1. Prüfen ob Speedometer gehalten wird (Main oder Offhand)
        ItemStack main = client.player.getMainHandStack();
        ItemStack off = client.player.getOffHandStack();
        boolean hasSpeedometer = main.isOf(ModItems.SPEEDOMETER) || off.isOf(ModItems.SPEEDOMETER);

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
        double velX = client.player.getVelocity().x;
        double velY = client.player.getVelocity().y;
        double velZ = client.player.getVelocity().z;

        double speedPerTick = Math.sqrt(velX * velX + velY * velY + velZ * velZ);
        double speedBps = speedPerTick * 20.0; // Blocks per Second

        if (speedBps > topSpeed) topSpeed = speedBps;
        if (speedBps > 0.1) {
            speedSum += speedBps;
            tickCount++;
        }
        double avgSpeed = tickCount > 0 ? (speedSum / tickCount) : 0.0;

        // --- TEXT ZUSAMMENSTELLEN ---
        List<Text> lines = new ArrayList<>();

        // Welcher Stack ist der Speedometer? (Für Enchantment Check)
        ItemStack activeStack = main.isOf(ModItems.SPEEDOMETER) ? main : off;
        boolean isEnchanted = activeStack.hasEnchantments();

        // Titel Zeile
        Formatting titleColor = isEnchanted ? Formatting.AQUA : Formatting.WHITE;
        lines.add(Text.literal("Speedometer").formatted(titleColor));

        // Zeile 1: Aktueller Speed
        lines.add(Text.literal(String.format("%.1f b/s", speedBps))
                .setStyle(Style.EMPTY.withColor(COLOR_SPEED)));

        // Zeile 2 & 3: Extras (Nur wenn Enchanted / Constructor's Touch)
        if (isEnchanted) {
            // FIX: Wir nutzen getFlag(7) statt isFallFlying(), da das immer existiert (Flag 7 = Elytra Glide)
            boolean isFlying = client.player.isGliding();

            if (isFlying) {
                // Symbol je nach Gefahr
                boolean danger = speedBps > 15.0;
                Text symbol = Text.literal(danger ? "⚠ " : "✔ ")
                        .setStyle(Style.EMPTY.withColor(danger ? COLOR_DANGER : COLOR_SAFE).withBold(true));

                // Stats Text ("Top: 20.1 Avg: 15.0") in Grau
                Text stats = Text.literal(String.format("Top: %.1f  Avg: %.1f", topSpeed, avgSpeed))
                        .setStyle(Style.EMPTY.withColor(COLOR_STATS));

                // Zusammenbauen: "⚠ Top: ... Avg: ..."
                lines.add(Text.empty().append(symbol).append(stats));
            }
            else {
                lines.add(Text.literal(String.format("Top: %.1f  Avg: %.1f", topSpeed, avgSpeed))
                        .setStyle(Style.EMPTY.withColor(COLOR_STATS)));
                double xBps = Math.abs(velX * 20);
                double zBps = Math.abs(velZ * 20);
                String vecText = String.format("X: %.1f  Z: %.1f", xBps, zBps);
                lines.add(Text.literal(vecText).formatted(Formatting.GRAY));
            }
        }

        if (lines.isEmpty()) return;

        // --- RENDERING ---
        TextRenderer textRenderer = client.textRenderer;
        int screenHeight = context.getScaledWindowHeight();

        // Breite berechnen
        int actualTextWidth = 0;
        for (Text line : lines) {
            int w = textRenderer.getWidth(line);
            if (w > actualTextWidth) actualTextWidth = w;
        }

        int minSafeWidth = 90;
        int finalContentWidth = Math.max(actualTextWidth, minSafeWidth);

        int paddingX = 6;
        int paddingY = 6;
        int lineSpacing = 2;
        int titleSpacing = 4;

        int totalTextHeight = (lines.size() * textRenderer.fontHeight)
                + ((lines.size() - 1) * lineSpacing)
                + titleSpacing;

        int boxWidth = finalContentWidth + (paddingX * 2);
        int boxHeight = totalTextHeight + (paddingY * 2);

        int x = 10;
        int y = (screenHeight / 2) - (boxHeight / 2);

        // POSITIONIERUNG ANPASSEN
        if (hasOctant) {
            // Wenn Octant da ist, rutschen wir ein Stück runter
            // Nicht zu tief, damit sie schön untereinander kleben
            y += 35;
        }

        // Draw Box
        context.fill(x + 1, y + 1, x + boxWidth - 1, y + boxHeight - 1, BACKGROUND_COLOR);
        context.fill(x + 1, y, x + boxWidth - 1, y + 1, BACKGROUND_COLOR); // Top fix

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
            Text line = lines.get(i);
            context.drawTextWithShadow(textRenderer, line, x + paddingX, textY, 0xFFFFFFFF);
            textY += textRenderer.fontHeight + lineSpacing;
            if (i == 0) textY += titleSpacing;
        }
    }
}