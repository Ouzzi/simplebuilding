package com.simpleresonance.client;

import com.simpleresonance.ResonanceMath;
import com.simpleresonance.SimpleResonance;
import com.simpleresonance.net.ResonanceStatsPayload;
import com.simplelib.api.client.ui.UiShiftTooltip;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * The resonance button in the inventory (owner N49; moved from SimpleBuilding's {@code TrimStatsPanel} in MOB27-3 R2): framed like the
 * Vanilla recipe-book button (blue when hovered), showing the heart of the current tier ({@link ResonanceLayout}); the value and its parts
 * are in the tooltip, folded behind SimpleLib's "Hold Shift for more" (owner 2026-10-21). Drawn in the Vanilla inventory by
 * {@code InventoryScreenResonanceMixin} and in rebuilt inventories (Simple Storage's backpack) through SimpleLib's
 * {@code InventoryExtrasClient}. The numbers come from {@link ResonanceClient} (the server's {@code simpleresonance:stats}).
 */
public final class ResonancePanel {
    private static final Identifier FIELD = SimpleResonance.id("resonance_field");
    private static final Identifier FIELD_HIGHLIGHTED = SimpleResonance.id("resonance_field_highlighted");
    /** Each of the three factors lies in 0.1..1.0. */
    public static final double FACTOR_MAX = 1.0;
    /** Lines shown without Shift: "Resonance: 0.21x (max 2.0x)". */
    public static final int BRIEF_LINES = 1;
    private static final String KEY = "gui.simpleresonance.stats.";

    private ResonancePanel() {}

    /** Draws the button with its top left at ({@code x}, {@code y}) and sets the tooltip when hovered. */
    public static void render(GuiGraphicsExtractor graphics, Font font, Minecraft minecraft, int x, int y, int mouseX, int mouseY) {
        if (minecraft == null || minecraft.player == null || !SimpleResonance.panelShown()) return;
        ResonanceStatsPayload stats = ResonanceClient.stats();
        double total = stats.multiplier();
        boolean hovered = ResonanceLayout.over(x, y, mouseX, mouseY);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hovered ? FIELD_HIGHLIGHTED : FIELD, x, y, ResonanceLayout.SIZE, ResonanceLayout.SIZE);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SimpleResonance.id(ResonanceLayout.heartSprite(total, stats.base())),
                x + ResonanceLayout.ICON_INSET, y + ResonanceLayout.ICON_INSET, ResonanceLayout.ICON, ResonanceLayout.ICON);
        if (hovered) graphics.setComponentTooltipForNextFrame(font, shown(minecraft.player, UiShiftTooltip.held()), mouseX, mouseY);
    }

    /** What the hovered button shows: the first line and the Shift hint, with {@code shift} every line of {@link #tooltip}. */
    public static List<Component> shown(Player player, boolean shift) {
        return UiShiftTooltip.collapse(tooltip(player), BRIEF_LINES, shift);
    }

    /** Every line of the tooltip for the client's stats (public for the tests). */
    public static List<Component> tooltip(Player player) {
        return lines(ResonanceClient.stats());
    }

    /** The tooltip lines for these stats. */
    public static List<Component> lines(ResonanceStatsPayload s) {
        double[] f = s.factors();
        double base = s.base();
        List<Component> tooltip = new ArrayList<>();
        // N49: no bold; "Resonance: x" orange, "(max 2.0x)" grey; factor lines with a bold first letter (E, S, C).
        tooltip.add(Component.translatable(KEY + "title", fmt(s.multiplier())).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" ")).append(Component.translatable(KEY + "max", String.format(Locale.ROOT, "%.1f", base))
                        .withStyle(ChatFormatting.GRAY)));
        tooltip.add(Component.translatable(KEY + "formula", String.format(Locale.ROOT, "%.1f", base)).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.empty());
        tooltip.add(factor("level", f[0], ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable(KEY + "xp_gathered", Math.max(0, s.xp()), ResonanceMath.XP_POINTS_FOR_FULL_FACTOR)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(factor("survival", f[1], ChatFormatting.BLUE));
        String partMax = fmt(0.5);
        int metres = Math.max(0, s.metres()), ticks = Math.max(0, s.activeTicks());
        tooltip.add(Component.translatable(KEY + "distance_part", metres, fmt(ResonanceMath.distancePart(metres)), partMax)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(KEY + "time_part", time(ticks), fmt(ResonanceMath.timePart(ticks)), partMax)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(factor("combat", f[2], ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable(KEY + "hostiles", Math.max(0, s.hostile())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(KEY + "passives", Math.max(0, s.passive())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(KEY + "damage_taken", Math.max(0, s.damage()) / 20).withStyle(ChatFormatting.GRAY));
        return tooltip;
    }

    /** "Experience: 0.50" in the factor colour with a bold first letter, then "(max 1.00)" in grey (owner N49). */
    private static Component factor(String key, double value, ChatFormatting colour) {
        String text = Component.translatable(KEY + key, fmt(value)).getString();
        MutableComponent line = Component.empty();
        if (!text.isEmpty()) {
            int first = text.offsetByCodePoints(0, 1);
            line.append(Component.literal(text.substring(0, first)).withStyle(colour, ChatFormatting.BOLD))
                    .append(Component.literal(text.substring(first)).withStyle(colour));
        }
        return line.append(Component.literal(" ")).append(Component.translatable(KEY + "max_factor", fmt(FACTOR_MAX)).withStyle(ChatFormatting.GRAY));
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String time(int ticks) {
        int seconds = ticks / 20, minutes = seconds / 60, hours = minutes / 60;
        if (hours > 0) return String.format(Locale.ROOT, "%dh %dm", hours, minutes % 60);
        return String.format(Locale.ROOT, "%dm %ds", minutes, seconds % 60);
    }
}
