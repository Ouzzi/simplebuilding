package com.simplebuilding.mixin.client;

import com.simplebuilding.util.SurvivalTracerAccessor;
import com.simplebuilding.util.TrimMultiplierLogic;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends HandledScreen<PlayerScreenHandler> {

    @Unique
    private ButtonWidget trimInfoButton;
    @Unique
    private boolean isStatsVisible = false; // Standardmäßig ausgeblendet

    public InventoryScreenMixin(PlayerScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
        super(screenHandler, playerInventory, text);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void initTrimUI(CallbackInfo ci) {
        int btnX = this.x - 24;
        int btnY = this.y + 10;

        // Button toggelt die Sichtbarkeit der Stats
        this.trimInfoButton = ButtonWidget.builder(Text.empty(), button -> {
                    this.isStatsVisible = !this.isStatsVisible;
                })
                .dimensions(btnX, btnY, 20, 20)
                .tooltip(Tooltip.of(
                        Text.empty()
                                .append(Text.literal("Toggle Resonance Stats").formatted(Formatting.AQUA, Formatting.BOLD))
                                .append(Text.literal("\n"))
                                .append(Text.literal("Click to show/hide trim multipliers.").formatted(Formatting.GRAY))
                ))
                .build();

        this.addDrawableChild(this.trimInfoButton);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderTrimStats(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.client == null || this.client.player == null) return;

        // Icon auf den Button zeichnen (immer sichtbar)
        if (this.trimInfoButton != null) {
            context.drawItem(new ItemStack(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE), this.trimInfoButton.getX() + 2, this.trimInfoButton.getY() + 2);
        }

        // Statistik-Panel nur rendern, wenn aktiviert
        if (!isStatsVisible) return;

        double xpMult = TrimMultiplierLogic.calculateXPMultiplier(this.client.player);
        double survMult = TrimMultiplierLogic.calculateSurvivalMultiplier(this.client.player);
        double combatMult = TrimMultiplierLogic.calculateCombatMultiplier(this.client.player);
        double totalMult = TrimMultiplierLogic.getMultiplier(this.client.player);

        int boxWidth = 84;
        int boxHeight = 64;
        int startX = this.x - boxWidth - 30; // Etwas weiter links vom Button
        int startY = this.y + 10;

        boolean isBoxHovered = mouseX >= startX && mouseX <= startX + boxWidth && mouseY >= startY && mouseY <= startY + boxHeight;

        drawVanillaPanel(context, startX, startY, boxWidth, boxHeight);

        int colLabelX = startX + 6;
        int colOpX = startX + 18;
        int colValX = startX + 30;
        int currentY = startY + 6;
        int lineHeight = 13;
        int colorText = 0xFF404040;

        // --- L (Grün) ---
        context.drawText(this.textRenderer, Text.literal("L").formatted(Formatting.DARK_GREEN, Formatting.BOLD), colLabelX, currentY, 0xFFFFFFFF, false);
        context.drawText(this.textRenderer, String.format("%.2f", xpMult), colValX, currentY, colorText, false);
        currentY += lineHeight;

        // --- S (Blau - guter Kontrast) ---
        context.drawText(this.textRenderer, Text.literal("S").formatted(Formatting.BLUE, Formatting.BOLD), colLabelX, currentY, 0xFFFFFFFF, false);
        context.drawText(this.textRenderer, "x", colOpX, currentY, 0xFF707070, false);
        context.drawText(this.textRenderer, String.format("%.2f", survMult), colValX, currentY, colorText, false);
        currentY += lineHeight;

        // --- C (Rot) ---
        context.drawText(this.textRenderer, Text.literal("C").formatted(Formatting.DARK_RED, Formatting.BOLD), colLabelX, currentY, 0xFFFFFFFF, false);
        context.drawText(this.textRenderer, "x", colOpX, currentY, 0xFF707070, false);
        context.drawText(this.textRenderer, String.format("%.2f", combatMult), colValX, currentY, colorText, false);
        currentY += lineHeight - 2;

        context.fill(startX + 4, currentY, startX + boxWidth - 4, currentY + 1, 0xFFA0A0A0);
        context.fill(startX + 4, currentY + 1, startX + boxWidth - 4, currentY + 2, 0xFFFFFFFF);
        currentY += 4;

        context.drawText(this.textRenderer, "=", colLabelX, currentY, colorText, false);
        context.drawText(this.textRenderer, Text.literal(String.format("%.2fx", totalMult)).formatted(Formatting.DARK_GREEN, Formatting.BOLD), colValX - 4, currentY, 0xFFFFFFFF, false);

        if (isBoxHovered) {
            context.drawStrokedRectangle(startX - 1, startY - 1, boxWidth + 2, boxHeight + 2, 0xFFFFFFFF);
            renderDetailedTooltip(context, mouseX, mouseY, xpMult, survMult, combatMult, totalMult);
        }
    }

    @Unique
    private void drawVanillaPanel(DrawContext context, int x, int y, int width, int height) {
        int colorBg = 0xFFC6C6C6;
        int light = 0xFFFFFFFF;
        int dark = 0xFF555555;
        int black = 0xFF000000;
        context.fill(x, y, x + width, y + height, colorBg);
        context.fill(x, y, x + width - 1, y + 1, light);
        context.fill(x, y, x + 1, y + height - 1, light);
        context.fill(x + width - 1, y, x + width, y + height, dark);
        context.fill(x, y + height - 1, x + width, y + height, dark);
        context.drawStrokedRectangle(x - 1, y - 1, width + 2, height + 2, black);
    }

    @Unique
    private void renderDetailedTooltip(DrawContext context, int mouseX, int mouseY, double xp, double surv, double combat, double total) {
        List<Text> tooltip = new ArrayList<>();
        SurvivalTracerAccessor accessor = (SurvivalTracerAccessor) this.client.player;

        int distDiff = Math.max(0, accessor.simplebuilding$getCurrentDistance() - accessor.simplebuilding$getBaseDistance());
        int timeDiff = Math.max(0, accessor.simplebuilding$getCurrentTime() - accessor.simplebuilding$getBaseTime());
        int hostileDiff = Math.max(0, accessor.simplebuilding$getCurrentHostileKills() - accessor.simplebuilding$getBaseHostileKills());
        int passiveDiff = Math.max(0, accessor.simplebuilding$getCurrentPassiveKills() - accessor.simplebuilding$getBasePassiveKills());
        int damageDiff = Math.max(0, accessor.simplebuilding$getCurrentDamageTaken() - accessor.simplebuilding$getBaseDamageTaken());

        tooltip.add(Text.literal("Statistic Details").formatted(Formatting.BLUE, Formatting.UNDERLINE));
        tooltip.add(Text.empty());

        // Level
        tooltip.add(Text.literal("L: Experience").formatted(Formatting.DARK_GREEN, Formatting.BOLD));
        tooltip.add(Text.literal(" Current Level: " + this.client.player.experienceLevel).formatted(Formatting.GRAY));

        // Survival
        tooltip.add(Text.empty());
        tooltip.add(Text.literal("S: Survival").formatted(Formatting.BLUE, Formatting.BOLD));
        tooltip.add(Text.literal(" Distance: " + distDiff + "m").formatted(Formatting.GRAY));
        tooltip.add(Text.literal(" Time Alive: " + formatTime(timeDiff)).formatted(Formatting.GRAY));

        // Combat
        tooltip.add(Text.empty());
        tooltip.add(Text.literal("C: Combat").formatted(Formatting.DARK_RED, Formatting.BOLD));
        tooltip.add(Text.literal(" Hostiles: " + hostileDiff).formatted(Formatting.GRAY));
        tooltip.add(Text.literal(" Passives: " + passiveDiff).formatted(Formatting.GRAY));
        // Damage Taken Anzeige
        tooltip.add(Text.literal(" Dmg Taken: " + (damageDiff / 10) + " Hearts").formatted(Formatting.GRAY));

        context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
    }

    @Unique
    private String formatTime(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        int hours = minutes / 60;
        if (hours > 0) return String.format("%dh %dm", hours, minutes % 60);
        return String.format("%dm %ds", minutes, seconds % 60);
    }
}