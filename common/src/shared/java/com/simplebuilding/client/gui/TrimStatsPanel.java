package com.simplebuilding.client.gui;

import com.simplebuilding.util.SurvivalTracerAccessor;
import com.simplebuilding.util.TrimMultiplierLogic;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Die Resonanz der Ruestungsbesatz-Boni im Inventar: ein kleines Feld links neben dem Inventar mit
 * dem Symbol der Ward-Schmiedevorlage und dem Wert ("0.22x"); ueberfahren zeigt es die Einzelheiten
 * (L, S, C mit ihren Hoechstwerten, woraus sie sich speisen, und wann alles zusammen gedeckelt ist).
 * Seit 2026-09-29 ohne Knopf: der Wert steht immer da (Besitzer).
 *
 * <p>Beide Inventare zeigen es: das normale (E, ueber {@code InventoryScreenMixin}) und das
 * Rucksack-Inventar ({@link BackpackScreen}) - eine gemeinsame Klasse statt einer Kopie.
 */
public final class TrimStatsPanel {
    private static final Identifier PANEL_SPRITE = Identifier.withDefaultNamespace("popup/background");
    /** Abstand des Felds zur linken Inventarkante und zur Oberkante (wie der Besatz-Knopf am Schmiedetisch). */
    public static final int GAP_LEFT = 5, OFFSET_Y = 5;
    /** Feldhoehe; die Breite waechst mit dem Text ({@link #width}). */
    public static final int HEIGHT = 20;
    /** Symbol: 2 px vom linken Rand und von der Oberkante. */
    public static final int ICON_INSET = 2;
    private static final int TEXT_X = 20, TEXT_PAD_RIGHT = 5;
    /** Jeder der drei Faktoren liegt in 0,1..1,0. */
    public static final double FACTOR_MIN = 0.1, FACTOR_MAX = 1.0;

    /** Der kompakte Wert, z. B. "0.22x". */
    public static String compact(double resonance) {
        return String.format(java.util.Locale.ROOT, "%.2fx", resonance);
    }

    /** Breite des Felds fuer diesen Text. */
    public static int width(Font font, String text) {
        return TEXT_X + font.width(text) + TEXT_PAD_RIGHT;
    }

    /** Linke Kante des Felds bei dieser Breite. */
    public static int x(int leftPos, int width) {
        return leftPos - GAP_LEFT - width;
    }

    public void render(GuiGraphicsExtractor context, Font font, Minecraft minecraft, int leftPos, int topPos, int mouseX, int mouseY) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        Player player = minecraft.player;
        String text = compact(TrimMultiplierLogic.getMultiplier(player));
        int w = width(font, text);
        int x = x(leftPos, w);
        int y = topPos + OFFSET_Y;

        context.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL_SPRITE, x, y, w, HEIGHT);
        context.item(new ItemStack(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE), x + ICON_INSET, y + ICON_INSET);
        context.text(font, Component.literal(text).withStyle(ChatFormatting.DARK_GREEN), x + TEXT_X, y + 6, 0xFFFFFFFF, false);

        if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + HEIGHT) {
            context.setComponentTooltipForNextFrame(font, tooltip(player), mouseX, mouseY);
        }
    }

    /** Die Einzelheiten beim Ueberfahren; oeffentlich fuer die Client-Tests. */
    public static List<Component> tooltip(Player player) {
        List<Component> tooltip = new ArrayList<>();
        double xp = TrimMultiplierLogic.calculateXPMultiplier(player);
        double survival = TrimMultiplierLogic.calculateSurvivalMultiplier(player);
        double combat = TrimMultiplierLogic.calculateCombatMultiplier(player);
        double base = TrimMultiplierLogic.baseMultiplier(true);
        double total = TrimMultiplierLogic.getMultiplier(player);

        int gatheredXp = 0, distance = 0, time = 0, hostiles = 0, passives = 0, damage = 0;
        if (player instanceof SurvivalTracerAccessor a) {
            gatheredXp = Math.max(0, player.totalExperience - a.simplebuilding$getBaseXp());
            distance = Math.max(0, a.simplebuilding$getCurrentDistance() - a.simplebuilding$getBaseDistance());
            time = Math.max(0, a.simplebuilding$getCurrentTime() - a.simplebuilding$getBaseTime());
            hostiles = Math.max(0, a.simplebuilding$getCurrentHostileKills() - a.simplebuilding$getBaseHostileKills());
            passives = Math.max(0, a.simplebuilding$getCurrentPassiveKills() - a.simplebuilding$getBasePassiveKills());
            damage = Math.max(0, a.simplebuilding$getCurrentDamageTaken() - a.simplebuilding$getBaseDamageTaken());
        }

        tooltip.add(Component.translatable("gui.simplebuilding.trim_stats.title", fmt(total), fmt(base))
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        // Resonanz = Mittelwert der drei Faktoren mal konfigurierte Basis (TrimMultiplierLogic)
        tooltip.add(Component.literal("(L + S + C) / 3 x " + String.format(java.util.Locale.ROOT, "%.1f", base)).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.empty());

        tooltip.add(factor("level", xp, ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("gui.simplebuilding.trim_stats.xp_gathered", gatheredXp,
                TrimMultiplierLogic.XP_POINTS_FOR_FULL_FACTOR).withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.empty());
        tooltip.add(factor("survival", survival, ChatFormatting.BLUE));
        tooltip.add(Component.translatable("gui.simplebuilding.trim_stats.distance", distance).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("gui.simplebuilding.trim_stats.time_alive", formatTime(time)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("gui.simplebuilding.trim_stats.survival_hint").withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(Component.empty());
        tooltip.add(factor("combat", combat, ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("gui.simplebuilding.trim_stats.hostiles", hostiles).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("gui.simplebuilding.trim_stats.passives", passives).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("gui.simplebuilding.trim_stats.damage_taken", damage / 20).withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.empty());
        boolean capped = total >= base - 0.005;
        tooltip.add(Component.translatable(capped ? "gui.simplebuilding.trim_stats.capped" : "gui.simplebuilding.trim_stats.cap", fmt(base))
                .withStyle(capped ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        tooltip.add(Component.translatable("gui.simplebuilding.trim_stats.death").withStyle(ChatFormatting.DARK_GRAY));
        return tooltip;
    }

    private static Component factor(String key, double value, ChatFormatting colour) {
        return Component.translatable("gui.simplebuilding.trim_stats." + key, fmt(value), fmt(FACTOR_MAX)).withStyle(colour, ChatFormatting.BOLD);
    }

    private static String fmt(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static String formatTime(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        int hours = minutes / 60;
        if (hours > 0) return String.format("%dh %dm", hours, minutes % 60);
        return String.format("%dm %ds", minutes, seconds % 60);
    }
}
