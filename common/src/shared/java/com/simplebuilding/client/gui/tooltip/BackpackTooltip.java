package com.simplebuilding.client.gui.tooltip;

import com.simplebuilding.items.tooltip.BackpackTooltipData;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Tooltip-Bild eines Rucksacks. Mit gedrueckter Umschalttaste ein kompaktes Raster wie beim
 * Vanilla-Buendel (Icon mit Anzahl, bis zu {@link #COLUMNS} je Reihe, hoechstens {@link #MAX_ROWS}
 * Reihen); passt nicht alles hinein, zeigt die letzte Zelle "+N". Ohne Umschalt nur die Zeile
 * "Umschalt halten fuer Inhalt". Fuer alle Loader dieselbe Fabrik ({@link #create}).
 */
public final class BackpackTooltip implements ClientTooltipComponent {
    static final int COLUMNS = 9;
    static final int MAX_ROWS = 3;
    static final int CELL = 18;
    private static final int GAP_BELOW = 2;
    private static final Component HINT = Component.translatable("tooltip.simplebuilding.backpack.shift_hint")
            .withStyle(ChatFormatting.DARK_GRAY);

    private final List<ItemStack> items;

    private BackpackTooltip(BackpackTooltipData data) {
        this.items = data.itemsInSlotOrder();
    }

    public static ClientTooltipComponent create(BackpackTooltipData data) {
        return new BackpackTooltip(data);
    }

    private static boolean showContents() {
        return Minecraft.getInstance().hasShiftDown();
    }

    /** Gezeichnete Zellen: Items plus ggf. die "+N"-Zelle. */
    private int cells() {
        return Math.min(this.items.size(), COLUMNS * MAX_ROWS);
    }

    /** So viele Items erscheinen als Icon. */
    private int shownItems() {
        int cells = cells();
        return this.items.size() > cells ? cells - 1 : cells;
    }

    private int columns() {
        return Math.max(1, Math.min(COLUMNS, cells()));
    }

    private int rows() {
        return (cells() + COLUMNS - 1) / COLUMNS;
    }

    @Override
    public int getHeight(Font font) {
        return showContents() ? rows() * CELL + GAP_BELOW : 10;
    }

    @Override
    public int getWidth(Font font) {
        return showContents() ? columns() * CELL : font.width(HINT);
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        if (!showContents()) {
            graphics.text(font, HINT, x, y, 0xFFFFFFFF);
            return;
        }
        int cells = cells();
        int shown = shownItems();
        for (int i = 0; i < cells; i++) {
            int cx = x + (i % COLUMNS) * CELL;
            int cy = y + (i / COLUMNS) * CELL;
            graphics.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, 0x40FFFFFF);
            if (i < shown) {
                ItemStack stack = this.items.get(i);
                graphics.item(stack, cx + 1, cy + 1, i);
                graphics.itemDecorations(font, stack, cx + 1, cy + 1);
            } else {
                String more = "+" + (this.items.size() - shown);
                graphics.text(font, more, cx + (CELL - 1 - font.width(more)) / 2, cy + 5, 0xFFFFFFFF);
            }
        }
    }
}
