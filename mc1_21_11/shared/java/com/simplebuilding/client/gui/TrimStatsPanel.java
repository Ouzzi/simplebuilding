package com.simplebuilding.client.gui;

import com.simplebuilding.util.SurvivalTracerAccessor;
import com.simplebuilding.util.TrimMultiplierLogic;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Der Knopf "Toggle Resonance Stats" und das Multiplikator-Panel der Ruestungsbesatz-Boni.
 *
 * <p>Beide Inventare zeigen ihn: das normale (E, ueber {@code InventoryScreenMixin}) und das
 * Rucksack-Inventar ({@link BackpackScreen}). Frueher lag der Code direkt im Mixin; der
 * Rucksack-Bildschirm erbt nicht von {@code InventoryScreen}, der Mixin griffe dort also nicht.
 * Eine gemeinsame Klasse statt einer Kopie, damit die zwei Stellen nicht auseinanderlaufen.
 *
 * <p>MC 1.21.11: {@code GuiGraphics} mit {@code renderItem}/{@code drawString}/{@code renderOutline}
 * (auf 26.2 {@code GuiGraphicsExtractor} mit {@code item}/{@code text}/{@code outline}).
 */
public final class TrimStatsPanel {
    private Button button;
    private boolean statsVisible = false; // Standardmäßig ausgeblendet

    /** Der Knopf links neben dem Inventar; {@code leftPos}/{@code topPos} sind die Bildecke. */
    public Button createButton(int leftPos, int topPos) {
        this.button = Button.builder(Component.empty(), pressed -> this.statsVisible = !this.statsVisible)
                .bounds(leftPos - 24, topPos + 10, 20, 20)
                .tooltip(Tooltip.create(
                        Component.empty()
                                .append(Component.literal("Toggle Resonance Stats").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                                .append(Component.literal("\n"))
                                .append(Component.literal("Click to show/hide trim multipliers.").withStyle(ChatFormatting.GRAY))
                ))
                .build();
        return this.button;
    }

    public void render(GuiGraphics context, Font font, Minecraft minecraft, int leftPos, int topPos, int mouseX, int mouseY) {
        if (minecraft == null || minecraft.player == null) return;

        // Icon auf den Button zeichnen (immer sichtbar)
        if (this.button != null) {
            context.renderItem(new ItemStack(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE), this.button.getX() + 2, this.button.getY() + 2);
        }

        // Statistik-Panel nur rendern, wenn aktiviert
        if (!this.statsVisible) return;

        double xpMult = TrimMultiplierLogic.calculateXPMultiplier(minecraft.player);
        double survMult = TrimMultiplierLogic.calculateSurvivalMultiplier(minecraft.player);
        double combatMult = TrimMultiplierLogic.calculateCombatMultiplier(minecraft.player);
        double totalMult = TrimMultiplierLogic.getMultiplier(minecraft.player);

        int boxWidth = 84;
        int boxHeight = 64;
        int startX = leftPos - boxWidth - 30; // Etwas weiter links vom Button
        int startY = topPos + 10;

        boolean isBoxHovered = mouseX >= startX && mouseX <= startX + boxWidth && mouseY >= startY && mouseY <= startY + boxHeight;

        drawVanillaPanel(context, startX, startY, boxWidth, boxHeight);

        int colLabelX = startX + 6;
        int colOpX = startX + 18;
        int colValX = startX + 30;
        int currentY = startY + 6;
        int lineHeight = 13;
        int colorText = 0xFF404040;

        // --- L (Grün) ---
        context.drawString(font, Component.literal("L").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD), colLabelX, currentY, 0xFFFFFFFF, false);
        context.drawString(font, String.format("%.2f", xpMult), colValX, currentY, colorText, false);
        currentY += lineHeight;

        // --- S (Blau - guter Kontrast) ---
        context.drawString(font, Component.literal("S").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD), colLabelX, currentY, 0xFFFFFFFF, false);
        context.drawString(font, "x", colOpX, currentY, 0xFF707070, false);
        context.drawString(font, String.format("%.2f", survMult), colValX, currentY, colorText, false);
        currentY += lineHeight;

        // --- C (Rot) ---
        context.drawString(font, Component.literal("C").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), colLabelX, currentY, 0xFFFFFFFF, false);
        context.drawString(font, "x", colOpX, currentY, 0xFF707070, false);
        context.drawString(font, String.format("%.2f", combatMult), colValX, currentY, colorText, false);
        currentY += lineHeight - 2;

        context.fill(startX + 4, currentY, startX + boxWidth - 4, currentY + 1, 0xFFA0A0A0);
        context.fill(startX + 4, currentY + 1, startX + boxWidth - 4, currentY + 2, 0xFFFFFFFF);
        currentY += 4;

        context.drawString(font, "=", colLabelX, currentY, colorText, false);
        context.drawString(font, Component.literal(String.format("%.2fx", totalMult)).withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD), colValX - 4, currentY, 0xFFFFFFFF, false);

        if (isBoxHovered) {
            context.renderOutline(startX - 1, startY - 1, boxWidth + 2, boxHeight + 2, 0xFFFFFFFF);
            renderDetailedTooltip(context, font, minecraft, mouseX, mouseY);
        }
    }

    private static void drawVanillaPanel(GuiGraphics context, int x, int y, int width, int height) {
        int colorBg = 0xFFC6C6C6;
        int light = 0xFFFFFFFF;
        int dark = 0xFF555555;
        int black = 0xFF000000;
        context.fill(x, y, x + width, y + height, colorBg);
        context.fill(x, y, x + width - 1, y + 1, light);
        context.fill(x, y, x + 1, y + height - 1, light);
        context.fill(x + width - 1, y, x + width, y + height, dark);
        context.fill(x, y + height - 1, x + width, y + height, dark);
        context.renderOutline(x - 1, y - 1, width + 2, height + 2, black);
    }

    private static void renderDetailedTooltip(GuiGraphics context, Font font, Minecraft minecraft, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        SurvivalTracerAccessor accessor = (SurvivalTracerAccessor) minecraft.player;

        int distDiff = Math.max(0, accessor.simplebuilding$getCurrentDistance() - accessor.simplebuilding$getBaseDistance());
        int timeDiff = Math.max(0, accessor.simplebuilding$getCurrentTime() - accessor.simplebuilding$getBaseTime());
        int hostileDiff = Math.max(0, accessor.simplebuilding$getCurrentHostileKills() - accessor.simplebuilding$getBaseHostileKills());
        int passiveDiff = Math.max(0, accessor.simplebuilding$getCurrentPassiveKills() - accessor.simplebuilding$getBasePassiveKills());
        int damageDiff = Math.max(0, accessor.simplebuilding$getCurrentDamageTaken() - accessor.simplebuilding$getBaseDamageTaken());

        tooltip.add(Component.literal("Statistic Details").withStyle(ChatFormatting.BLUE, ChatFormatting.UNDERLINE));
        tooltip.add(Component.empty());

        // Level
        tooltip.add(Component.literal("L: Experience").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
        tooltip.add(Component.literal(" Current Level: " + minecraft.player.experienceLevel).withStyle(ChatFormatting.GRAY));

        // Survival
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("S: Survival").withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD));
        tooltip.add(Component.literal(" Distance: " + distDiff + "m").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(" Time Alive: " + formatTime(timeDiff)).withStyle(ChatFormatting.GRAY));

        // Combat
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("C: Combat").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal(" Hostiles: " + hostileDiff).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(" Passives: " + passiveDiff).withStyle(ChatFormatting.GRAY));
        // Damage Taken Anzeige
        tooltip.add(Component.literal(" Dmg Taken: " + (damageDiff / 10) + " Hearts").withStyle(ChatFormatting.GRAY));

        context.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
    }

    private static String formatTime(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        int hours = minutes / 60;
        if (hours > 0) return String.format("%dh %dm", hours, minutes % 60);
        return String.format("%dm %ds", minutes, seconds % 60);
    }
}
