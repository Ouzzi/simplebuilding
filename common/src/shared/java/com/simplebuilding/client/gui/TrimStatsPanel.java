package com.simplebuilding.client.gui;

import com.simplebuilding.util.SurvivalTracerAccessor;
import com.simplebuilding.util.TrimMultiplierLogic;
import com.simplebuilding.util.TrimStatsLayout;
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

/**
 * Die Resonanz der Ruestungsbesatz-Boni im Inventar: ein kleines Feld rechts neben dem
 * Rezeptbuch-Button im Vanilla-Stil dieses Knopfs (Rahmen, Fuellung #C6C6C6, beim Ueberfahren blau wie er) mit einem
 * Vanilla-Herz in Steingrau und dem dunkelgrauen Wert ("0.22x"); ueberfahren zeigt es die Einzelheiten
 * (L, S, C mit ihren Hoechstwerten, woraus sie sich speisen, und wann alles zusammen gedeckelt ist).
 * Seit 2026-09-29 ohne Knopf: der Wert steht immer da (Besitzer).
 *
 * <p>Beide Inventare zeigen es: das normale (E, ueber {@code InventoryScreenMixin}) und das
 * Rucksack-Inventar ({@link BackpackScreen}) - eine gemeinsame Klasse statt einer Kopie.
 */
public final class TrimStatsPanel {
    /** Rahmen des Rezeptbuch-Knopfs ohne Buch, Nine-Slice (tools/textures/resonance_heart_2026_10_09.py). */
    private static final Identifier PANEL_SPRITE = Identifier.fromNamespaceAndPath("simplebuilding", "resonance_field");
    private static final Identifier PANEL_SPRITE_HIGHLIGHTED = Identifier.fromNamespaceAndPath("simplebuilding", "resonance_field_highlighted");
    /** Vanilla-Herzform in Steingrau (9x9, tools/textures/resonance_heart_2026_10_09.py). */
    private static final Identifier HEART_SPRITE = Identifier.fromNamespaceAndPath("simplebuilding", "resonance_heart");
    /** Rezeptbuch-Knopf des Vanilla-Inventars relativ zu leftPos/topPos (x 104, height / 2 - 22 = 61 bei 166 Hoehe). */
    public static final int BOOK_X = 104, BOOK_Y = 61;
    public static final int HEIGHT = TrimStatsLayout.HEIGHT;
    /** Jeder der drei Faktoren liegt in 0,1..1,0. */
    public static final double FACTOR_MIN = 0.1, FACTOR_MAX = 1.0;

    /** Der kompakte Wert, z. B. "0.22x". */
    public static String compact(double resonance) {
        return String.format(java.util.Locale.ROOT, "%.2fx", resonance);
    }

    /** Breite des Felds fuer diesen Text. */
    public static int width(Font font, String text) {
        return TrimStatsLayout.panel(0, 0, font.width(text)).width();
    }

    /** Das Feld fuer diesen Text rechts neben dem Buch-Knopf bei (bookX, bookY); oeffentlich fuer die Client-Tests. */
    public static TrimStatsLayout.Panel panel(Font font, String text, int bookX, int bookY) {
        return TrimStatsLayout.panel(bookX, bookY, font.width(text));
    }

    public void render(GuiGraphicsExtractor context, Font font, Minecraft minecraft, int bookX, int bookY, int mouseX, int mouseY) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        Player player = minecraft.player;
        String text = compact(TrimMultiplierLogic.getMultiplier(player));
        TrimStatsLayout.Panel panel = panel(font, text, bookX, bookY);

        boolean hovered = mouseX >= panel.x() && mouseX < panel.x() + panel.width()
                && mouseY >= panel.y() && mouseY < panel.y() + panel.height();
        context.blitSprite(RenderPipelines.GUI_TEXTURED, hovered ? PANEL_SPRITE_HIGHLIGHTED : PANEL_SPRITE,
                panel.x(), panel.y(), panel.width(), panel.height());
        context.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_SPRITE, panel.iconX(), panel.iconY(),
                TrimStatsLayout.ICON_SIZE, TrimStatsLayout.ICON_SIZE);
        // Dunkelgrau ohne Schatten wie die Vanilla-Beschriftung "Crafting" daneben: auf #C6C6C6 klar lesbar
        // (Weiss mit Schatten war im Client-Screenshot zu blass).
        context.text(font, Component.literal(text), panel.textX(), panel.textY(), 0xFF404040, false);

        if (hovered) {
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
