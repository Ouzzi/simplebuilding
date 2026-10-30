package com.simplelib.api.client.ui;

import com.simplelib.api.DevVariants;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * The "Dev-Config" next to the Vanilla inventory (ABN30-9): a small button beside the UI-style switch opens a box with one
 * click row per registered {@link DevVariants.Variant}; a click selects the option at once. Shown only while
 * {@link DevVariants#enabled()}. Hooked in by the loader entry via {@link #decorate}.
 */
public final class DevVariantsPanel extends AbstractWidget {
    public static final int WIDTH = 150, ROW = 12, PAD = 5;
    private static boolean open;

    private final List<DevVariants.Variant> variants = DevVariants.all();

    private DevVariantsPanel(int x, int y) {
        super(x, y, WIDTH, height(DevVariants.all()), Component.literal("Dev-Config"));
    }

    /** Pure layout: the box height for the variants (one mod heading per group, one name line and one option row each). */
    public static int height(List<DevVariants.Variant> variants) {
        int rows = 0;
        String mod = null;
        for (DevVariants.Variant v : variants) {
            if (!v.mod().equals(mod)) { rows++; mod = v.mod(); }
            rows += 2;
        }
        return PAD * 2 + 14 + rows * ROW;
    }

    /** Adds the toggle button (and, when open, the panel) to the Vanilla inventory. */
    public static void decorate(Screen screen) {
        if (!(screen instanceof InventoryScreen inv) || !DevVariants.enabled() || DevVariants.all().isEmpty()) return;
        var access = (com.simplelib.mixin.client.ContainerScreenAccessor) inv;
        int x = access.simplelib$leftPos() + access.simplelib$imageWidth() + 4, y = access.simplelib$topPos();
        var widgets = net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(screen);
        DevVariantsPanel panel = new DevVariantsPanel(x, y);
        if (open) widgets.add(panel);
        widgets.add(Button.builder(Component.literal("Dev"), b -> {
            open = !open;
            if (open) widgets.add(panel); else widgets.remove(panel);
        }).bounds(x + 20, y - 18, 24, 16).build());
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        UiPalette p = UiPalette.INVENTORY;
        UiBoxes.box(g, getX(), getY(), width, height, p);
        Font font = Minecraft.getInstance().font;
        g.text(font, "Dev-Config", getX() + PAD, getY() + PAD, p.label(), false);
        int y = getY() + PAD + 14;
        String mod = null;
        for (DevVariants.Variant v : variants) {
            if (!v.mod().equals(mod)) {
                mod = v.mod();
                g.text(font, mod, getX() + PAD, y + 2, p.label(), false);
                y += ROW;
            }
            g.text(font, v.name(), getX() + PAD + 4, y + 2, p.label(), false);
            y += ROW;
            int n = v.options().size(), bw = (width - PAD * 2 - 4) / n;
            for (int i = 0; i < n; i++) {
                DevVariants.Option o = v.options().get(i);
                int bx = getX() + PAD + 4 + i * bw;
                boolean sel = DevVariants.is(v.id(), o.id()), hover = mouseX >= bx && mouseX < bx + bw - 2 && mouseY >= y && mouseY < y + ROW - 1;
                int face = sel ? UiPalette.mix(p.fill(), 0xFF7FD27F, 0.55) : UiPalette.mix(p.fill(), 0xFFFFFFFF, hover ? 0.32 : 0.12);
                UiBoxes.raised(g, bx, y, bw - 2, ROW - 1, face);
                g.text(font, o.label(), bx + 3, y + 2, UiPalette.DARK_LABEL, false);
            }
            y += ROW;
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        int y = getY() + PAD + 14;
        String mod = null;
        for (DevVariants.Variant v : variants) {
            if (!v.mod().equals(mod)) { mod = v.mod(); y += ROW; }
            y += ROW;
            int n = v.options().size(), bw = (width - PAD * 2 - 4) / n;
            if (event.y() >= y && event.y() < y + ROW - 1) {
                for (int i = 0; i < n; i++) {
                    int bx = getX() + PAD + 4 + i * bw;
                    if (event.x() >= bx && event.x() < bx + bw - 2) DevVariants.select(v.id(), v.options().get(i).id());
                }
            }
            y += ROW;
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}
