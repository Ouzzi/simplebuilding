package com.simplelib.api.client.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Right-click context menu in the N12 style (docs/ai/UI-BAUSTEINE.md): a small box with one row per action, opened
 * at the cursor and kept inside the screen. A click on a row runs it and closes the menu; any other click closes it
 * and is swallowed. Disabled rows are greyed out. Client only; the owning screen forwards render and clicks.
 */
public final class UiContextMenu {
    public static final int ROW = 12, PAD = 4;
    private record Entry(Component label, boolean enabled, Runnable action) {}

    private final List<Entry> entries = new ArrayList<>();
    private final UiPalette palette;
    private int x, y, width;
    private boolean open;

    public UiContextMenu(UiPalette palette) {
        this.palette = palette;
    }

    public UiContextMenu add(Component label, boolean enabled, Runnable action) {
        entries.add(new Entry(label, enabled, action));
        return this;
    }

    /** Clears the rows and opens at the cursor, clamped into {@code screenWidth x screenHeight}. Add rows afterwards. */
    public UiContextMenu open(int mouseX, int mouseY) {
        entries.clear();
        x = mouseX;
        y = mouseY;
        open = true;
        return this;
    }

    public void close() {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    private int height() {
        return entries.size() * ROW + 2 * PAD;
    }

    public void render(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY) {
        if (!open) return;
        width = 0;
        for (Entry e : entries) width = Math.max(width, font.width(e.label()));
        width += 2 * PAD + 4;
        x = Math.max(0, Math.min(x, g.guiWidth() - width));
        y = Math.max(0, Math.min(y, g.guiHeight() - height()));
        UiBoxes.rounded(g, x, y, width, height(), new int[] {1}, UiBoxes.RIM);
        g.fill(x + 1, y + 1, x + width - 1, y + height() - 1, palette.light());
        g.fill(x + 2, y + 2, x + width - 2, y + height() - 2, palette.fill());
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            int ry = y + PAD + i * ROW;
            boolean hover = e.enabled() && mouseX >= x + 2 && mouseX < x + width - 2 && mouseY >= ry && mouseY < ry + ROW;
            if (hover) g.fill(x + 2, ry, x + width - 2, ry + ROW, palette.shade());
            int color = e.enabled() ? palette.label() : UiPalette.scale(palette.label(), 0.5) & 0x00FFFFFF | 0x99000000;
            g.text(font, e.label(), x + PAD + 2, ry + 2, color, false);
        }
    }

    /** Returns true when the menu was open (the click is consumed either way). */
    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!open) return false;
        open = false;
        if (mouseX < x || mouseX >= x + width || mouseY < y + PAD || mouseY >= y + height() - PAD) return true;
        int i = (int) ((mouseY - y - PAD) / ROW);
        if (i >= 0 && i < entries.size() && entries.get(i).enabled()) entries.get(i).action().run();
        return true;
    }
}
