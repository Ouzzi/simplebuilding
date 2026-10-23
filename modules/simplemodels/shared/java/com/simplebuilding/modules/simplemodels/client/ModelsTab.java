package com.simplebuilding.modules.simplemodels.client;

import com.simplelib.api.client.ui.UiBookmarks;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
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
 * The "Models" bookmark on the left edge of the inventory, the creative inventory and the anvil, drawn like the guide
 * book's side tabs (rounded outer end, square inner end tucked under the frame) with a name tag as icon. The position
 * is read from the screen every frame, so the recipe book moving the inventory never leaves it behind. While the
 * recipe book is open its right edge touches the inventory, so the bookmark moves to the top edge (SM21 f).
 *
 * <p>B23-M10 (owner 23.10.): drawn behind the screen like Vanilla's creative tabs - {@link #extractBehind} runs at the end
 * of {@code Screen#extractBackground} (after the dimming, before the container image, {@code ModelScreenMixin}), so the
 * container frame covers the tucked-in end. The widget itself draws nothing; it keeps the click and the tooltip.
 */
public final class ModelsTab extends AbstractButton {
    public static final int W = 24, H = 20, OUT = 20, DOWN = 6;
    private final Runnable action;
    private final java.util.function.IntSupplier left, top;
    private final java.util.function.BooleanSupplier onTop;

    public ModelsTab(Screen parent, java.util.function.IntSupplier left, java.util.function.IntSupplier top, java.util.function.BooleanSupplier onTop) {
        super(0, 0, W, H, Component.translatable("simplemodels.browser.models"));
        this.action = () -> net.minecraft.client.Minecraft.getInstance().setScreenAndShow(new ModelBrowser(parent));
        this.left = left; this.top = top; this.onTop = onTop;
        setTooltip(Tooltip.create(Component.translatable("simplemodels.browser.tab_hint")));
        place();
    }

    /** Whether the bookmark sits on the top edge (recipe book open). */
    public boolean onTop() { return onTop.getAsBoolean(); }

    public void place() {
        if (onTop()) { setSize(H, W); setPosition(left.getAsInt() + DOWN, Math.max(0, top.getAsInt() - OUT)); }
        else { setSize(W, H); setPosition(Math.max(0, left.getAsInt() - OUT), top.getAsInt() + DOWN); }
    }

    @Override public void onPress(InputWithModifiers input) { action.run(); }

    @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        place();
    }

    /** The bookmark itself, drawn in the background pass under the container image (B23-M10). */
    public void extractBehind(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (!visible) return;
        place();
        int x = getX(), y = getY();
        boolean hovered = isFocused() || isMouseOver(mouseX, mouseY);
        if (onTop()) upTab(g, x, y, width, height, UiPalette.INVENTORY, hovered);
        else UiBookmarks.tab(g, x, y, width, height, true, UiPalette.INVENTORY, false, hovered);
        g.item(new ItemStack(Items.NAME_TAG), x + 2, y + 2);
    }

    /** {@link UiBookmarks#tab} turned upright: rounded end at the top, square end tucked under the top frame. */
    private static void upTab(GuiGraphicsExtractor g, int x, int y, int w, int h, UiPalette p, boolean hovered) {
        int fill = hovered ? p.light() : UiPalette.scale(p.fill(), 0.82);
        g.fill(x, y + 2, x + 1, y + h, UiBoxes.RIM);
        g.fill(x + w - 1, y + 2, x + w, y + h, UiBoxes.RIM);
        g.fill(x + 1, y + 1, x + 2, y + h, UiBoxes.RIM);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h, UiBoxes.RIM);
        g.fill(x + 2, y, x + w - 2, y + 1, UiBoxes.RIM);
        g.fill(x + 2, y + 1, x + w - 2, y + h, fill);
        g.fill(x + 1, y + 2, x + 2, y + h, fill);
        g.fill(x + w - 2, y + 2, x + w - 1, y + h, fill);
        g.fill(x + 2, y + 1, x + 3, y + h, p.light());
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
}
