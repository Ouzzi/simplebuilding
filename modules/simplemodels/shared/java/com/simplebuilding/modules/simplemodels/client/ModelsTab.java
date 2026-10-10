package com.simplebuilding.modules.simplemodels.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The "Models" bookmark on the left edge of the inventory and the anvil, drawn like the guide book's side tabs
 * (rounded outer end, square inner end tucked under the frame) with a name tag as icon. The position is read from
 * the screen every frame, so the recipe book moving the inventory never leaves it behind.
 */
public final class ModelsTab extends AbstractButton {
    public static final int W = 24, H = 20, OUT = 20, DOWN = 6;
    private static final int RIM = 0xFF1E1F23, LIGHT = 0xFFFFFFFF, SHADED = 0xFFA2A2A2;
    private final Runnable action;
    private final java.util.function.IntSupplier left, top;

    public ModelsTab(Screen parent, java.util.function.IntSupplier left, java.util.function.IntSupplier top) {
        super(0, 0, W, H, Component.translatable("simplemodels.browser.models"));
        this.action = () -> net.minecraft.client.Minecraft.getInstance().setScreenAndShow(new ModelBrowser(parent));
        this.left = left; this.top = top;
        setTooltip(Tooltip.create(Component.translatable("simplemodels.browser.tab_hint")));
        place();
    }

    private void place() { setPosition(Math.max(0, left.getAsInt() - OUT), top.getAsInt() + DOWN); }

    @Override public void onPress(InputWithModifiers input) { action.run(); }

    @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        place();
        int x = getX(), y = getY(), w = width, h = height;
        int body = isHoveredOrFocused() ? LIGHT : SHADED;
        // Rounded outer (left) end: two stepped corner rows, rim all around, square inner end under the frame.
        g.fill(x + 2, y, x + w, y + 1, RIM);
        g.fill(x + 2, y + h - 1, x + w, y + h, RIM);
        g.fill(x + 1, y + 1, x + 2, y + 2, RIM);
        g.fill(x + 1, y + h - 2, x + 2, y + h - 1, RIM);
        g.fill(x, y + 2, x + 1, y + h - 2, RIM);
        g.fill(x + 1, y + 2, x + w, y + h - 2, body);
        g.fill(x + 2, y + 1, x + w, y + 2, body);
        g.fill(x + 2, y + h - 2, x + w, y + h - 1, body);
        g.fill(x + 1, y + 2, x + w, y + 3, LIGHT);
        g.item(new ItemStack(Items.NAME_TAG), x + 2, y + 2);
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
}
