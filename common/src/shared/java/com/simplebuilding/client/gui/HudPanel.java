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
 * 26.3 a triple frame like the other mod UIs - outline, bevel, band, corners a 3-2-1 staircase - see {@link HudFrame}), placed and scaled by {@link ModHud}, 4 px of
 * padding, the item's own name as the title (aqua when enchanted, as the tooltip does), 3 px under
 * the title, 2 px between lines, values in tones of the octant bronze and secondary lines grey. The
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
     * Primary value tone (Pos 1, results, the rod's distance): the bronze 80 % towards white. Pos 2 uses
     * {@link #COLOR_VALUE_DIM}, the same hue 60 % towards white - one brightness step apart. WCAG contrast on the
     * box over the worst world (pure white sky): 7,2:1 and 5,2:1; over a dark world 14,5:1 and 10,5:1.
     */
    public static final int COLOR_VALUE = mix(ModScreenLayout.OCTANT_FILL, 0xFFFFFFFF, 0.8);
    /** Dimmer step of {@link #COLOR_VALUE} (Pos 2). */
    public static final int COLOR_VALUE_DIM = mix(ModScreenLayout.OCTANT_FILL, 0xFFFFFFFF, 0.6);
    /** Secondary lines (4,8:1 over a pure white world, 9+ over a dark one). */
    public static final int COLOR_SECONDARY = 0xFFB8B8B8;
    /** Warnings (danger zone, lethal fall). */
    public static final int COLOR_DANGER = 0xFFFF5555;

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
            case GAUGE -> main.is(ModItems.VELOCITY_GAUGE) || off.is(ModItems.VELOCITY_GAUGE);
            case ROD -> com.simplebuilding.tweaks.client.TweaksClient.isAimingLaser(player)
                    && LaserPointerItem.measures(player.getUseItem(), player.level());
        };
    }

    /**
     * Vertical offset (unscaled GUI pixels) of {@code slot} around the anchor: the shown panels are
     * spread evenly, {@link #STACK_PITCH} apart, centred on the anchor.
     */
    public static int stackOffset(Slot slot, Minecraft client) {
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
