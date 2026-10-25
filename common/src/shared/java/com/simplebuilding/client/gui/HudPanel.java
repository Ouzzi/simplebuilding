package com.simplebuilding.client.gui;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.tweaks.item.LaserPointerItem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The one look every gadget readout of the mod shares (owner 2026-09-29: "all overlays one
 * consistent style, the octant box is the reference"; N64 2026-10-15: flat dark box, fixed 78 % opacity; N67: on
 * 26.3 a triple frame like the other mod UIs - outline, bevel, band, corners a rounder 4-2-1-1 staircase (N68) - see {@link HudFrame}), placed and scaled by {@link ModHud}, 4 px of
 * padding, the item's own name as the title (aqua when enchanted, as the tooltip does), 3 px under
 * the title, 2 px between lines, values in the original colour style (the octant's dye theme, orange, grey; lifted only as far as the 4,5:1 contrast needs, N68) and secondary lines grey. The
 * Octant ({@link RangefinderHudOverlay}), the Gauge ({@link SpeedometerHudOverlay}) and the
 * Resonance Rod's distance readout ({@code LaserRenderer#renderHud}) draw through here -
 * nothing else writes free text next to the crosshair any more.
 *
 * <p>Several panels at once stack around the configured anchor in a fixed order (Octant, Gauge,
 * Rod), {@link #STACK_PITCH} pixels apart; with two panels that is +/-38 px (was 70 / +/-35 before the 3 px frame of N67 made the tallest panel, the octant with a volume, 71 px high).
 */
public final class HudPanel {
    private HudPanel() {
    }

    /** Distance from the box's outer edge (its frame included) to the content: the frame plus 3 px of air. */
    public static final int PADDING = HudFrame.THICKNESS + 3;
    /** Gap under the title line. */
    public static final int TITLE_GAP = 3;
    /** Gap between two text lines. */
    public static final int LINE_GAP = 2;
    /** Distance between the centres of two stacked panels. */
    public static final int STACK_PITCH = 76;

    /** Fixed box opacity (owner O4: 78 %). */
    public static final int ALPHA = 0xC8;
    /** Box fill (0.20 x the bronze) at {@link #ALPHA}. */
    private static final int BOX_FILL = ALPHA << 24 | (scale(ModScreenLayout.OCTANT_FILL, 0.20) & 0xFFFFFF);
    /**
     * Frame layers, outermost first, all at {@link #ALPHA}: the dark outline of the container boxes (#1E1F23), the
     * bevel (0.82 x the bronze) and the band (0.64 x, the single frame colour of N64), the ratios of {@code UiBoxes.box}.
     */
    private static final int[] FRAME_LAYERS = {
            ALPHA << 24 | 0x1E1F23,
            ALPHA << 24 | (scale(ModScreenLayout.OCTANT_FILL, 0.82) & 0xFFFFFF),
            ALPHA << 24 | (scale(ModScreenLayout.OCTANT_FILL, 0.64) & 0xFFFFFF)};
    /**
     * The box over the worst world behind it (pure white sky): the background every HUD text colour is checked against.
     * Over a dark world the box is darker still, so a colour that reads here reads there too.
     */
    private static final int WORST_BACKGROUND = over(BOX_FILL, 0xFFFFFF);
    /** Smallest accepted text contrast (AGENTS.md rule 15, same bar as simplelib's {@code UiContrast.MIN}). */
    public static final double MIN_CONTRAST = 4.5;
    /** Primary value tone (the rod's distance, the Gauge's speed): the original orange #FF7F4C, lifted to {@link #MIN_CONTRAST}. */
    public static final int COLOR_VALUE = ink(0xFFFF7F4C);
    /** Secondary lines: the original grey #AAAAAA, lifted just enough (it had 4,05:1 over a white world). */
    public static final int COLOR_SECONDARY = ink(0xFFAAAAAA);
    /** Warnings (danger zone, lethal fall): the original red #FF5555, lifted just enough (it had 3,0:1 over a white world). */
    public static final int COLOR_DANGER = ink(0xFFFF5555);
    /** Orange impact damage of the Gauge: the original #FF7F4C, lifted just enough (3,8:1 before). */
    public static final int COLOR_IMPACT = COLOR_VALUE;

    /**
     * {@code rgb} (alpha ignored) itself when it reads on the panel with at least {@link #MIN_CONTRAST} over the worst
     * world, otherwise mixed towards white in 1 % steps only as far as needed, so the old colour style survives. Opaque.
     */
    public static int ink(int rgb) {
        int color = 0xFF000000 | rgb & 0xFFFFFF;
        for (int percent = 0; percent <= 100; percent++) {
            int candidate = mix(color, 0xFFFFFFFF, percent / 100.0);
            if (contrast(candidate, WORST_BACKGROUND) >= MIN_CONTRAST) {
                return candidate;
            }
        }
        return 0xFFFFFFFF;
    }

    /** WCAG contrast ratio of an opaque text colour on the panel over the worst world (white) when {@code worldRgb} is white, or over any other world. */
    public static double contrastOver(int text, int worldRgb) {
        return contrast(text, over(BOX_FILL, worldRgb));
    }

    private static int over(int argb, int worldRgb) {
        double a = (argb >>> 24) / 255.0;
        int r = (int) Math.round(((argb >> 16) & 255) * a + ((worldRgb >> 16) & 255) * (1 - a));
        int g = (int) Math.round(((argb >> 8) & 255) * a + ((worldRgb >> 8) & 255) * (1 - a));
        int b = (int) Math.round((argb & 255) * a + (worldRgb & 255) * (1 - a));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static double luminance(int c) {
        double[] v = {(c >> 16) & 255, (c >> 8) & 255, c & 255};
        for (int i = 0; i < 3; i++) {
            v[i] /= 255.0;
            v[i] = v[i] <= 0.03928 ? v[i] / 12.92 : Math.pow((v[i] + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * v[0] + 0.7152 * v[1] + 0.0722 * v[2];
    }

    private static double contrast(int a, int b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    /** The panels that can be on screen, in stacking order from top to bottom. */
    public enum Slot {
        OCTANT, GAUGE, ROD
    }

    /** The panel title for a gadget: its hover name, aqua when enchanted, white otherwise. */
    public static Component title(ItemStack stack) {
        return stack.getHoverName().copy().withStyle(stack.isEnchanted() ? ChatFormatting.AQUA : ChatFormatting.WHITE);
    }

    /** Whether the panel {@code slot} is shown this frame (the same questions its overlay asks). */
    public static boolean active(Slot slot, Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            return false;
        }
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return switch (slot) {
            case OCTANT -> !(client.gui.screen() instanceof OctantScreen)
                    && (main.getItem() instanceof OctantItem || off.getItem() instanceof OctantItem);
            case GAUGE -> com.simplebuilding.util.GadgetRefs.isGauge(main) || com.simplebuilding.util.GadgetRefs.isGauge(off);
            case ROD -> com.simplebuilding.tweaks.client.TweaksClient.isAimingLaser(player)
                    && LaserPointerItem.measures(player.getUseItem(), player.level());
        };
    }

    /**
     * Vertical offset (unscaled GUI pixels) of {@code slot} around the anchor: the shown panels are
     * spread evenly, {@link #STACK_PITCH} apart, centred on the anchor.
     */
    public static int stackOffset(Slot slot, Minecraft client) {
        Integer shared = com.simplebuilding.client.LibClientServices.gadgetHudOffset(slot, client); // 26.3: with Simple Gadgets' boxes
        if (shared != null) {
            return shared;
        }
        int shown = 0;
        int index = -1;
        for (Slot other : Slot.values()) {
            if (other == slot || active(other, client)) {
                if (other == slot) {
                    index = shown;
                }
                shown++;
            }
        }
        return stackOffset(index, shown);
    }

    /** Offset of panel {@code index} (0 = top) among {@code shown} panels. */
    public static int stackOffset(int index, int shown) {
        return (int) Math.round((index - (shown - 1) / 2.0) * STACK_PITCH);
    }

    /**
     * Draws a text-only panel: {@code title}, then {@code lines}, at least {@code minContentWidth}
     * wide, in stacking slot {@code slot}. The caller has checked {@link ModHud#visible()} and the
     * hidden HUD.
     */
    public static void draw(GuiGraphicsExtractor context, Component title, List<Component> lines, int minContentWidth, Slot slot) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        int contentWidth = Math.max(minContentWidth, font.width(title));
        for (Component line : lines) {
            contentWidth = Math.max(contentWidth, font.width(line));
        }
        int contentHeight = font.lineHeight + (lines.isEmpty() ? 0 : TITLE_GAP + lines.size() * (font.lineHeight + LINE_GAP) - LINE_GAP);
        ModHud.begin(context, contentWidth + PADDING * 2, contentHeight + PADDING * 2, stackOffset(slot, client));
        background(context, PADDING, PADDING, contentWidth, contentHeight);
        context.text(font, title, PADDING, PADDING, 0xFFFFFFFF, true);
        int y = PADDING + font.lineHeight + TITLE_GAP;
        for (Component line : lines) {
            context.text(font, line, PADDING, y, 0xFFFFFFFF, true);
            y += font.lineHeight + LINE_GAP;
        }
        ModHud.end(context);
    }

    /**
     * The panel box around content at ({@code x}, {@code y}) of the given size: a flat dark box at {@link #ALPHA} with
     * the {@link HudFrame} frame; it reaches {@link #PADDING} px beyond the content on every side. Text sits on the
     * fill only, never on the frame.
     */
    public static void background(GuiGraphicsExtractor context, int x, int y, int contentWidth, int contentHeight) {
        HudFrame.draw(context, x - PADDING, y - PADDING, contentWidth + PADDING * 2, contentHeight + PADDING * 2, FRAME_LAYERS, BOX_FILL);
    }

    /** {@code color} (RGB) with each channel scaled by {@code f}. */
    private static int scale(int color, double f) {
        int r = (int) Math.min(255, ((color >> 16) & 255) * f), g = (int) Math.min(255, ((color >> 8) & 255) * f),
                b = (int) Math.min(255, (color & 255) * f);
        return r << 16 | g << 8 | b;
    }

    /** Opaque mix of {@code a} towards {@code b} by {@code t} (0..1), truncating like the palette's own mix. */
    private static int mix(int a, int b, double t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }
}
