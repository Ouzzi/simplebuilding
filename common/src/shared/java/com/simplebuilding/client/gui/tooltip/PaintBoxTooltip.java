package com.simplebuilding.client.gui.tooltip;

import com.simplebuilding.component.PaintBoxContents;
import com.simplebuilding.items.custom.PaintBoxItem;
import com.simplebuilding.items.tooltip.PaintBoxTooltipData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

/**
 * Tooltip image of a paint box (owner 2026-10-09): all 16 dyes in two rows of eight like the bundle grid, each with its
 * count; colours the box does not hold are greyed out, the colour in front (scroll wheel) is framed white. Same factory
 * for all loaders ({@link #create}).
 */
public final class PaintBoxTooltip implements ClientTooltipComponent {
    public static final int COLUMNS = 8, CELL = 18;
    private static final int ROWS = PaintBoxContents.COLORS / COLUMNS, GAP_BELOW = 2;
    /** Grey veil over a colour the box does not hold. */
    public static final int GREYED = 0xC0505050;

    private final PaintBoxContents contents;

    private PaintBoxTooltip(PaintBoxTooltipData data) {
        this.contents = data.contents();
    }

    public static ClientTooltipComponent create(PaintBoxTooltipData data) {
        return new PaintBoxTooltip(data);
    }

    @Override
    public int getHeight(Font font) {
        return ROWS * CELL + GAP_BELOW;
    }

    @Override
    public int getWidth(Font font) {
        return COLUMNS * CELL;
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        int front = this.contents.isEmpty() ? -1 : PaintBoxItem.frontColor(this.contents);
        for (int c = 0; c < PaintBoxContents.COLORS; c++) {
            int cx = x + (c % COLUMNS) * CELL, cy = y + (c / COLUMNS) * CELL;
            graphics.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, 0x40FFFFFF);
            graphics.item(new ItemStack(PaintBoxItem.dyeItem(DyeColor.byId(c))), cx + 1, cy + 1, c);
        }
        graphics.nextStratum(); // veils and counts above the icons
        for (int c = 0; c < PaintBoxContents.COLORS; c++) {
            int cx = x + (c % COLUMNS) * CELL, cy = y + (c / COLUMNS) * CELL;
            int count = this.contents.count(c);
            if (count == 0) {
                graphics.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, GREYED);
            } else {
                graphics.itemDecorations(font, new ItemStack(PaintBoxItem.dyeItem(DyeColor.byId(c))), cx + 1, cy + 1,
                        String.valueOf(count));
            }
            if (c == front) graphics.outline(cx - 1, cy - 1, CELL + 1, CELL + 1, 0xFFFFFFFF);
        }
    }
}
