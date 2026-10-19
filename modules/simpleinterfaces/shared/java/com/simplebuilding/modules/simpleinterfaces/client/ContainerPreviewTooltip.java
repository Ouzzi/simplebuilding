package com.simplebuilding.modules.simpleinterfaces.client;

import com.simplebuilding.modules.simpleinterfaces.SimpleInterfaces;
import com.simplebuilding.modules.simpleinterfaces.tooltip.ContainerPreview;
import com.simplebuilding.modules.simpleinterfaces.tooltip.ContainerTooltips;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Draws a {@link ContainerPreview}: a Vanilla-like panel (gray, tinted by the box dye or ender teal) with Vanilla slots
 * like the shulker box screen, items with their counts; without the grid one gray note line.
 */
public final class ContainerPreviewTooltip implements ClientTooltipComponent {
    static final int SLOT = 18;
    static final int PAD = 3;
    static final int GAP_BELOW = 2;
    /** Note line on the Vanilla tooltip background (#100010): 8.8:1. */
    static final int NOTE_TEXT = 0xFFAAAAAA;
    /** Vanilla tooltip background (lint reference for the note colour). */
    static final int TOOLTIP_BG = 0xFF100010;
    static final int SLOT_GRAY = 0xFF8B8B8B;
    static final int SLOT_DARK = 0xFF373737;
    static final int LIGHT_EDGE = 0xFFFFFFFF;
    static final int EDGE_SHADE = 0xFF555555;
    /** "+n" in the last cell, no shadow, on the slot gray #8B8B8B: 4.8:1. */
    static final int MORE_INK = 0xFF1F1F1F; // contrast-ok (drawn on SLOT_GRAY, not on the tooltip background)

    private final ContainerPreview preview;

    public ContainerPreviewTooltip(ContainerPreview preview) {
        this.preview = preview;
    }

    /** Client: the preview for a hovered stack, null when Vanilla stays unchanged. */
    public static @Nullable ContainerPreview of(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || !SimpleInterfaces.active(mc.level)) return null;
        return ContainerTooltips.preview(stack, mc.hasShiftDown(), ContainersClient.config(),
                SimpleInterfaces.configFor(mc.level).enableEnderChestTooltip, ContainerTooltips.enderSnapshot).orElse(null);
    }

    @Override
    public int getHeight(Font font) {
        return preview.grid() ? preview.rows() * SLOT + 2 * PAD + GAP_BELOW : 10;
    }

    @Override
    public int getWidth(Font font) {
        return preview.grid() ? ContainerTooltips.COLUMNS * SLOT + 2 * PAD : preview.note() == null ? 0 : font.width(preview.note());
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        if (!preview.grid()) {
            if (preview.note() != null) graphics.text(font, preview.note(), x, y, NOTE_TEXT);
            return;
        }
        int width = ContainerTooltips.COLUMNS * SLOT + 2 * PAD;
        int height = preview.rows() * SLOT + 2 * PAD;
        graphics.fill(x, y, x + width, y + height, 0xFF000000 | preview.face());
        graphics.fill(x, y, x + width - 1, y + 1, LIGHT_EDGE);
        graphics.fill(x, y, x + 1, y + height - 1, LIGHT_EDGE);
        graphics.fill(x + 1, y + height - 1, x + width, y + height, EDGE_SHADE);
        graphics.fill(x + width - 1, y + 1, x + width, y + height, EDGE_SHADE);
        int last = preview.slots().size() - 1;
        for (int i = 0; i <= last; i++) {
            int sx = x + PAD + i % ContainerTooltips.COLUMNS * SLOT;
            int sy = y + PAD + i / ContainerTooltips.COLUMNS * SLOT;
            graphics.fill(sx, sy, sx + SLOT - 1, sy + 1, SLOT_DARK);
            graphics.fill(sx, sy, sx + 1, sy + SLOT - 1, SLOT_DARK);
            graphics.fill(sx + 1, sy + SLOT - 1, sx + SLOT, sy + SLOT, LIGHT_EDGE);
            graphics.fill(sx + SLOT - 1, sy + 1, sx + SLOT, sy + SLOT, LIGHT_EDGE);
            graphics.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, SLOT_GRAY);
            if (i == last && preview.more() > 0) {
                String more = "+" + preview.more();
                graphics.text(font, net.minecraft.network.chat.Component.literal(more), sx + (SLOT - font.width(more)) / 2, sy + 5, MORE_INK, false);
                continue;
            }
            ItemStack stack = preview.slots().get(i);
            if (stack.isEmpty()) continue;
            graphics.item(stack, sx + 1, sy + 1, i);
            graphics.itemDecorations(font, stack, sx + 1, sy + 1);
        }
    }
}
