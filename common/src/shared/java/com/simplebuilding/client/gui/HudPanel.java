package com.simplebuilding.client.gui;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.tweaks.item.LaserPointerItem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The one look every gadget readout of the mod shares (owner 2026-09-29: "all overlays one
 * consistent style, the octant box is the reference"): vanilla's tooltip panel (sprites
 * {@code tooltip/background} + {@code tooltip/frame}), placed and scaled by {@link ModHud}, 6 px of
 * padding, the item's own name as the title (aqua when enchanted, as the tooltip does), 4 px under
 * the title, 2 px between lines, values in a highlight colour and secondary lines grey. The
 * Octant ({@link RangefinderHudOverlay}), the Gauge ({@link SpeedometerHudOverlay}) and the
 * Resonance Rod's distance readout ({@code LaserRenderer#renderHud}) draw through here -
 * nothing else writes free text next to the crosshair any more.
 *
 * <p>Several panels at once stack around the configured anchor in a fixed order (Octant, Gauge,
 * Rod), {@link #STACK_PITCH} pixels apart; with two panels that is the former +/-35 px.
 */
public final class HudPanel {
    private HudPanel() {
    }

    /** Inner padding of the panel box (the tooltip frame adds its own 3 px outside it). */
    public static final int PADDING = 6;
    /** Gap under the title line. */
    public static final int TITLE_GAP = 4;
    /** Gap between two text lines. */
    public static final int LINE_GAP = 2;
    /** Distance between the centres of two stacked panels. */
    public static final int STACK_PITCH = 70;

    /** Highlight colour for measured values (the gauge's speed orange). */
    public static final int COLOR_VALUE = 0xFFFF7F4C;
    /** Secondary lines. */
    public static final int COLOR_SECONDARY = 0xFFAAAAAA;
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

    /** The panel box around content at ({@code x}, {@code y}) of the given size. */
    public static void background(GuiGraphicsExtractor context, int x, int y, int contentWidth, int contentHeight) {
        TooltipRenderUtil.extractTooltipBackground(context, x, y, contentWidth, contentHeight, null);
    }
}
