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
 * The resonance of the armour-trim bonuses in the inventory (owner N49, 2026-10-12): a slot-sized button between the
 * extra head slot and the off-hand slot, framed like the Vanilla recipe-book button (blue when hovered), showing only
 * the heart of the current tier ({@link TrimStatsLayout#heartSprite}); the value and its parts are in the tooltip.
 *
 * <p>Both inventories show it: the normal one (E, via {@code InventoryScreenMixin}) and the backpack inventory
 * ({@link BackpackScreen}) - one shared class instead of a copy.
 */
public final class TrimStatsPanel {
    /** Frame of the recipe-book button without the book, nine-slice (tools/textures/resonance_heart_2026_10_09.py). */
    private static final Identifier PANEL_SPRITE = Identifier.fromNamespaceAndPath("simplebuilding", "resonance_field");
    private static final Identifier PANEL_SPRITE_HIGHLIGHTED = Identifier.fromNamespaceAndPath("simplebuilding", "resonance_field_highlighted");
    public static final int HEIGHT = TrimStatsLayout.HEIGHT;
    /** Each of the three factors lies in 0.1..1.0. */
    public static final double FACTOR_MIN = 0.1, FACTOR_MAX = 1.0;

    /** The compact value, e.g. "0.22x". */
    public static String compact(double resonance) {
        return String.format(java.util.Locale.ROOT, "%.2fx", resonance);
    }

    /** Draws the button with its top left at ({@code x}, {@code y}) and sets the tooltip when hovered. */
    public void render(GuiGraphicsExtractor context, Font font, Minecraft minecraft, int x, int y, int mouseX, int mouseY) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        Player player = minecraft.player;
        double total = TrimMultiplierLogic.getMultiplier(player);
        double base = TrimMultiplierLogic.baseMultiplier(true);
        boolean hovered = TrimStatsLayout.hovered(x, y, mouseX, mouseY);
        context.blitSprite(RenderPipelines.GUI_TEXTURED, hovered ? PANEL_SPRITE_HIGHLIGHTED : PANEL_SPRITE, x, y,
                TrimStatsLayout.SIZE, TrimStatsLayout.SIZE);
        context.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.fromNamespaceAndPath("simplebuilding", TrimStatsLayout.heartSprite(total, base)),
                x + TrimStatsLayout.ICON_INSET, y + TrimStatsLayout.ICON_INSET, TrimStatsLayout.ICON_WIDTH, TrimStatsLayout.ICON_HEIGHT);
        if (hovered) {
            context.setComponentTooltipForNextFrame(font, tooltip(player), mouseX, mouseY);
        }
    }

    /** An empty Vanilla slot frame (18x18 around the 16x16 item at {@code x + 1, y + 1}) on a plain background. */
    public static void slotFrame(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        g.fill(x, y, x + 17, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + 17, 0xFF373737);
        g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
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

        // N49: no bold; "Resonance: x" orange, "(max 2.0x)" grey; factor lines with a bold first letter (E, S, C).
        tooltip.add(Component.translatable("gui.simplebuilding.trim_stats.title", fmt(total)).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" ")).append(Component.translatable("gui.simplebuilding.trim_stats.max",
                        String.format(java.util.Locale.ROOT, "%.1f", base)).withStyle(ChatFormatting.GRAY)));
        // Resonanz = Mittelwert der drei Faktoren mal konfigurierte Basis (TrimMultiplierLogic)
        tooltip.add(Component.translatable("gui.simplebuilding.trim_stats.formula", String.format(java.util.Locale.ROOT, "%.1f", base))
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.empty());

        tooltip.add(factor("level", fmt(xp), fmt(FACTOR_MAX), ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("gui.simplebuilding.trim_stats.xp_gathered", gatheredXp,
                TrimMultiplierLogic.XP_POINTS_FOR_FULL_FACTOR).withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.empty());
        tooltip.add(factor("survival", fmt(survival), fmt(FACTOR_MAX), ChatFormatting.BLUE));
        String partMax = fmt(TrimMultiplierLogic.SURVIVAL_PART_MAX);
        tooltip.add(Component.translatable("gui.simplebuilding.trim_stats.distance_part", distance,
                fmt(TrimMultiplierLogic.distancePart(distance)), partMax).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("gui.simplebuilding.trim_stats.time_part", formatTime(time),
                fmt(TrimMultiplierLogic.timePart(time)), partMax).withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.empty());
        tooltip.add(factor("combat", fmt(combat), fmt(FACTOR_MAX), ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("gui.simplebuilding.trim_stats.hostiles", hostiles).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("gui.simplebuilding.trim_stats.passives", passives).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("gui.simplebuilding.trim_stats.damage_taken", damage / 20).withStyle(ChatFormatting.GRAY));
        return tooltip;
    }

    /** "Experience: 0.50" in the factor colour with a bold first letter, then "(max 1.00)" in grey (owner N49). */
    private static Component factor(String key, String value, String max, ChatFormatting colour) {
        return boldInitial(Component.translatable("gui.simplebuilding.trim_stats." + key, value).getString(), colour)
                .append(Component.literal(" ")).append(Component.translatable("gui.simplebuilding.trim_stats.max_factor", max)
                        .withStyle(ChatFormatting.GRAY));
    }

    /** {@code text} in {@code colour}, only its first letter bold - the letter is the factor's short name (E, S, C). */
    static net.minecraft.network.chat.MutableComponent boldInitial(String text, ChatFormatting colour) {
        if (text.isEmpty()) {
            return Component.empty();
        }
        int first = text.offsetByCodePoints(0, 1);
        // an empty root, so the rest does not inherit the bold of the first letter
        return Component.empty().append(Component.literal(text.substring(0, first)).withStyle(colour, ChatFormatting.BOLD))
                .append(Component.literal(text.substring(first)).withStyle(colour));
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
