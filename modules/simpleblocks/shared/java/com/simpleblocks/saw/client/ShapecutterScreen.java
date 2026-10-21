package com.simpleblocks.saw.client;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.simpleblocks.saw.Sawing;
import com.simpleblocks.saw.ShapecutterMenu;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen of the Shapecutter: Vanilla's StonecutterScreen (owner 21.10.: "die gleiche ui wie stonecutter mit mehr rezepten").
 * Same size, slots, 4 x 3 cut tiles with a scroll bar, click / scroll / drag and sounds; only the list is longer. The
 * background and the sprites are Vanilla's own files, referenced and never copied (no Mojang pixels in the repository,
 * resource packs apply). No search field: even the longest list (a stone family with its octets) is a few scroll rows.
 */
public class ShapecutterScreen extends AbstractContainerScreen<ShapecutterMenu> {
    private static final Identifier SCROLLER = Identifier.withDefaultNamespace("container/stonecutter/scroller");
    private static final Identifier SCROLLER_DISABLED = Identifier.withDefaultNamespace("container/stonecutter/scroller_disabled");
    private static final Identifier TILE = Identifier.withDefaultNamespace("container/stonecutter/recipe");
    private static final Identifier TILE_SELECTED = Identifier.withDefaultNamespace("container/stonecutter/recipe_selected");
    private static final Identifier TILE_HOVERED = Identifier.withDefaultNamespace("container/stonecutter/recipe_highlighted");
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/stonecutter.png");
    /** Vanilla StonecutterScreen constants. */
    public static final int COLUMNS = 4, ROWS = 3, CELL_W = 16, CELL_H = 18;
    public static final int GRID_X = 52, GRID_Y = 14, SCROLL_X = 119, SCROLL_Y = 15, SCROLL_W = 12, SCROLL_H = 54, KNOB_H = 15;

    private float scroll;
    private boolean scrolling;
    private int startIndex;
    private boolean displayCuts;

    public ShapecutterScreen(ShapecutterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ShapecutterMenu.WIDTH, ShapecutterMenu.HEIGHT);
        menu.registerUpdateListener(this::containerChanged);
        this.displayCuts = menu.hasInput(); // a menu opened with an input already in it (client smoke)
        this.titleLabelY--;
    }

    private List<Sawing.Entry> cuts() {
        return this.menu.cuts();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x = this.leftPos, y = this.topPos;
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        int knob = (int) ((SCROLL_H - KNOB_H + 2) * this.scroll); // Vanilla: 41 px of travel
        int sx = x + SCROLL_X, sy = y + SCROLL_Y;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, this.scrollable() ? SCROLLER : SCROLLER_DISABLED, sx, sy + knob, SCROLL_W, KNOB_H);
        if (mouseX >= sx && mouseY >= sy && mouseX < sx + SCROLL_W && mouseY < sy + SCROLL_H) {
            g.requestCursor(this.scrollable() ? (this.scrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND) : CursorTypes.NOT_ALLOWED);
        }
        if (!this.displayCuts) return;
        List<Sawing.Entry> cuts = this.cuts();
        int end = Math.min(cuts.size(), this.startIndex + COLUMNS * ROWS);
        for (int index = this.startIndex; index < end; index++) {
            int pos = index - this.startIndex;
            int tx = x + GRID_X + pos % COLUMNS * CELL_W, ty = y + GRID_Y + pos / COLUMNS * CELL_H + 2;
            boolean hovered = mouseX >= tx && mouseY >= ty && mouseX < tx + CELL_W && mouseY < ty + CELL_H;
            Identifier sprite = index == this.menu.selectedIndex() ? TILE_SELECTED : hovered ? TILE_HOVERED : TILE;
            g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, tx, ty - 1, CELL_W, CELL_H);
            if (mouseX >= tx && mouseY >= ty - 1 && mouseX < tx + CELL_W && mouseY < ty - 1 + CELL_H) g.requestCursor(CursorTypes.POINTING_HAND);
            var stack = cuts.get(index).resultStack();
            g.item(stack, tx, ty);
            g.itemDecorations(this.font, stack, tx, ty, null);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int hit = this.cutAt(mouseX, mouseY, 2);
        if (hit >= 0) g.setTooltipForNextFrame(this.font, this.cuts().get(hit).resultStack(), mouseX, mouseY);
    }

    /** Index into the menu's cuts under the mouse, or -1; {@code dy} is Vanilla's offset (2 for drawing/tooltips, 0 for clicks). */
    private int cutAt(double mouseX, double mouseY, int dy) {
        if (!this.displayCuts) return -1;
        double rx = mouseX - (this.leftPos + GRID_X), ry = mouseY - (this.topPos + GRID_Y + dy);
        if (rx < 0 || ry < 0 || rx >= COLUMNS * CELL_W || ry >= ROWS * CELL_H) return -1;
        int index = this.startIndex + (int) ry / CELL_H * COLUMNS + (int) rx / CELL_W;
        return index < this.cuts().size() ? index : -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.displayCuts) {
            int hit = this.cutAt(event.x(), event.y(), 0);
            if (hit >= 0 && this.menu.clickMenuButton(this.minecraft.player, hit)) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, hit);
                return true;
            }
            int sx = this.leftPos + SCROLL_X, sy = this.topPos + 9; // Vanilla's grab area starts at y 9
            if (event.x() >= sx && event.x() < sx + SCROLL_W && event.y() >= sy && event.y() < sy + SCROLL_H) this.scrolling = true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.scrolling && this.scrollable()) {
            int top = this.topPos + GRID_Y;
            this.scroll = Mth.clamp(((float) event.y() - top - KNOB_H / 2.0F) / (SCROLL_H - KNOB_H), 0.0F, 1.0F);
            this.startIndex = (int) (this.scroll * this.offscreenRows() + 0.5) * COLUMNS;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.scrolling = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (super.mouseScrolled(x, y, scrollX, scrollY)) return true;
        if (this.scrollable()) {
            int rows = this.offscreenRows();
            this.scroll = Mth.clamp(this.scroll - (float) scrollY / rows, 0.0F, 1.0F);
            this.startIndex = (int) (this.scroll * rows + 0.5) * COLUMNS;
        }
        return true;
    }

    private boolean scrollable() {
        return this.displayCuts && this.cuts().size() > COLUMNS * ROWS;
    }

    private int offscreenRows() {
        return (this.cuts().size() + COLUMNS - 1) / COLUMNS - ROWS;
    }

    private void containerChanged() {
        this.displayCuts = this.menu.hasInput();
        this.scroll = 0.0F;
        this.startIndex = 0;
    }
}
