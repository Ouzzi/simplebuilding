package com.simpleblocks.saw.client;

import com.simpleblocks.saw.SawPalette;
import com.simpleblocks.saw.Sawing;
import com.simpleblocks.saw.ShapingSawMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Screen of the Shaping Saw: the stonecutter's layout (input left, cuts in the middle, result), but 6 x 4 visible cuts,
 * a scroll bar (wheel or drag) and a search field over the cuts (name or id). Drawn in the inventory palette
 * ({@link SawPalette}); the cut tiles and the scroller are Vanilla's stonecutter sprites (referenced, not copied).
 */
public class ShapingSawScreen extends AbstractContainerScreen<ShapingSawMenu> {
    private static final Identifier SCROLLER = Identifier.withDefaultNamespace("container/stonecutter/scroller");
    private static final Identifier SCROLLER_DISABLED = Identifier.withDefaultNamespace("container/stonecutter/scroller_disabled");
    private static final Identifier TILE = Identifier.withDefaultNamespace("container/stonecutter/recipe");
    private static final Identifier TILE_SELECTED = Identifier.withDefaultNamespace("container/stonecutter/recipe_selected");
    private static final Identifier TILE_HOVERED = Identifier.withDefaultNamespace("container/stonecutter/recipe_highlighted");
    public static final int COLUMNS = 6, ROWS = 4, CELL_W = 16, CELL_H = 18;
    public static final int GRID_X = 46, GRID_Y = 30, SCROLL_X = 146, SCROLL_W = 12, SCROLL_H = ROWS * CELL_H + 2, KNOB_H = 15;
    public static final int SEARCH_X = 46, SEARCH_Y = 15, SEARCH_W = 112, SEARCH_H = 12;

    private EditBox search;
    private String query = "";
    private float scroll;
    private boolean scrolling;
    private int startRow;
    private List<Integer> visible = List.of();
    private List<Sawing.Entry> visibleFor;
    private String visibleQuery;

    public ShapingSawScreen(ShapingSawMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ShapingSawMenu.WIDTH, ShapingSawMenu.HEIGHT);
        menu.registerUpdateListener(this::containerChanged);
        this.inventoryLabelY = ShapingSawMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        this.search = new EditBox(this.font, this.leftPos + SEARCH_X, this.topPos + SEARCH_Y, SEARCH_W, SEARCH_H,
                Component.translatable("container.simpleblocks.shaping_saw.search"));
        this.search.setMaxLength(50);
        this.search.setHint(Component.translatable("container.simpleblocks.shaping_saw.search"));
        this.search.setValue(this.query);
        this.search.setResponder(text -> {
            this.query = text;
            this.resetScroll();
        });
        this.addRenderableWidget(this.search);
    }

    @Override
    public void resize(int width, int height) {
        String text = this.query;
        this.init(width, height);
        this.search.setValue(text);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            this.minecraft.player.closeContainer();
            return true;
        }
        if (this.search != null && this.search.isFocused()) {
            return this.search.keyPressed(event) || this.search.canConsumeInput() || super.keyPressed(event);
        }
        return super.keyPressed(event);
    }

    // ------------------------------------------------------------ the list (filtered by the search text)

    /** Indices into {@link ShapingSawMenu#cuts()} that match the search text. */
    List<Integer> visible() {
        List<Sawing.Entry> cuts = this.menu.cuts();
        if (cuts != this.visibleFor || !this.query.equals(this.visibleQuery)) {
            String q = this.query.trim().toLowerCase(Locale.ROOT);
            List<Integer> out = new ArrayList<>();
            for (int i = 0; i < cuts.size(); i++) {
                ItemStack stack = cuts.get(i).resultStack();
                if (q.isEmpty() || stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)
                        || BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().contains(q)) out.add(i);
            }
            this.visible = out;
            this.visibleFor = cuts;
            this.visibleQuery = this.query;
        }
        return this.visible;
    }

    private int offscreenRows() {
        return Math.max(0, (this.visible().size() + COLUMNS - 1) / COLUMNS - ROWS);
    }

    private boolean scrollable() {
        return this.menu.hasInput() && this.offscreenRows() > 0;
    }

    private void resetScroll() {
        this.scroll = 0;
        this.startRow = 0;
    }

    private void containerChanged() {
        this.resetScroll();
    }

    // ------------------------------------------------------------ drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x = this.leftPos, y = this.topPos;
        panel(g, x, y, this.imageWidth, this.imageHeight);
        for (var slot : this.menu.slots) {
            if (slot.isActive()) sunk(g, x + slot.x - 1, y + slot.y - 1, 18, 18, SawPalette.SLOT);
        }
        // Arrow from the input down to the result.
        int ax = x + ShapingSawMenu.INPUT_X + 7, top = y + ShapingSawMenu.INPUT_Y + 21, bottom = y + ShapingSawMenu.RESULT_Y - 5;
        g.fill(ax, top, ax + 2, bottom - 3, SawPalette.SHADOW);
        for (int i = 0; i < 4; i++) g.fill(ax - 3 + i, bottom - 4 + i, ax + 5 - i, bottom - 3 + i, SawPalette.SHADOW);
        // Cut list well and scroll track.
        sunk(g, x + GRID_X - 1, y + GRID_Y - 1, COLUMNS * CELL_W + 2, SCROLL_H + 2, SawPalette.DARK);
        sunk(g, x + SCROLL_X - 1, y + GRID_Y - 1, SCROLL_W + 2, SCROLL_H + 2, SawPalette.SLOT);
        int knob = (int) ((SCROLL_H - KNOB_H) * this.scroll);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, this.scrollable() ? SCROLLER : SCROLLER_DISABLED, x + SCROLL_X, y + GRID_Y + knob, SCROLL_W, KNOB_H);
        List<Integer> shown = this.visible();
        List<Sawing.Entry> cuts = this.menu.cuts();
        if (!this.menu.hasInput()) return;
        int start = this.startRow * COLUMNS;
        for (int i = start; i < Math.min(shown.size(), start + COLUMNS * ROWS); i++) {
            int index = shown.get(i), pos = i - start;
            int tx = x + GRID_X + pos % COLUMNS * CELL_W, ty = y + GRID_Y + pos / COLUMNS * CELL_H + 1;
            boolean hovered = mouseX >= tx && mouseY >= ty && mouseX < tx + CELL_W && mouseY < ty + CELL_H;
            Identifier sprite = index == this.menu.selectedIndex() ? TILE_SELECTED : hovered ? TILE_HOVERED : TILE;
            g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, tx, ty, CELL_W, CELL_H);
            ItemStack stack = cuts.get(index).resultStack();
            g.item(stack, tx, ty + 1);
            g.itemDecorations(this.font, stack, tx, ty + 1, null);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        g.text(this.font, this.title, this.titleLabelX, this.titleLabelY, SawPalette.LABEL, false);
        g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, SawPalette.LABEL, false);
        String key = !this.menu.getSlot(ShapingSawMenu.INPUT_SLOT).hasItem() ? "container.simpleblocks.shaping_saw.empty"
                : this.menu.cuts().isEmpty() ? "container.simpleblocks.shaping_saw.no_cuts"
                : this.visible().isEmpty() ? "container.simpleblocks.shaping_saw.no_match" : null;
        if (key != null) {
            List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(Component.translatable(key), COLUMNS * CELL_W - 8);
            int ty = GRID_Y + (SCROLL_H - lines.size() * 10) / 2;
            for (var line : lines) {
                g.text(this.font, line, GRID_X + (COLUMNS * CELL_W - this.font.width(line)) / 2, ty, SawPalette.FIELD_TEXT, false);
                ty += 10;
            }
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int hit = this.cutAt(mouseX, mouseY);
        if (hit >= 0) g.setTooltipForNextFrame(this.font, this.menu.cuts().get(hit).resultStack(), mouseX, mouseY);
    }

    /** Index into the menu's cuts under the mouse, or -1. */
    private int cutAt(double mouseX, double mouseY) {
        if (!this.menu.hasInput()) return -1;
        double dx = mouseX - (this.leftPos + GRID_X), dy = mouseY - (this.topPos + GRID_Y + 1);
        if (dx < 0 || dy < 0 || dx >= COLUMNS * CELL_W || dy >= ROWS * CELL_H) return -1;
        int i = this.startRow * COLUMNS + (int) dy / CELL_H * COLUMNS + (int) dx / CELL_W;
        List<Integer> shown = this.visible();
        return i < shown.size() ? shown.get(i) : -1;
    }

    /** Vanilla container frame: black outline with stepped corners, light top-left, dark bottom-right, grey fill. */
    static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 2, y, x + w - 2, y + 1, SawPalette.BORDER);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, SawPalette.BORDER);
        g.fill(x, y + 2, x + 1, y + h - 2, SawPalette.BORDER);
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, SawPalette.BORDER);
        g.fill(x + 1, y + 1, x + 2, y + 2, SawPalette.BORDER);
        g.fill(x + w - 2, y + 1, x + w - 1, y + 2, SawPalette.BORDER);
        g.fill(x + 1, y + h - 2, x + 2, y + h - 1, SawPalette.BORDER);
        g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, SawPalette.BORDER);
        g.fill(x + 2, y + 1, x + w - 2, y + h - 1, SawPalette.FILL);
        g.fill(x + 1, y + 2, x + w - 1, y + h - 2, SawPalette.FILL);
        g.fill(x + 2, y + 1, x + w - 3, y + 3, SawPalette.LIGHT);
        g.fill(x + 1, y + 2, x + 3, y + h - 3, SawPalette.LIGHT);
        g.fill(x + 3, y + h - 3, x + w - 2, y + h - 1, SawPalette.SHADOW);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 2, SawPalette.SHADOW);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, SawPalette.FILL);
    }

    /** A sunk box (slot style): dark top-left, light bottom-right, given inner colour. */
    static void sunk(GuiGraphicsExtractor g, int x, int y, int w, int h, int inner) {
        g.fill(x, y, x + w - 1, y + 1, SawPalette.DARK);
        g.fill(x, y, x + 1, y + h - 1, SawPalette.DARK);
        g.fill(x + 1, y + h - 1, x + w, y + h, SawPalette.LIGHT);
        g.fill(x + w - 1, y + 1, x + w, y + h, SawPalette.LIGHT);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, inner);
    }

    // ------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int hit = this.cutAt(event.x(), event.y());
        if (hit >= 0 && this.menu.clickMenuButton(this.minecraft.player, hit)) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, hit);
            return true;
        }
        int sx = this.leftPos + SCROLL_X, sy = this.topPos + GRID_Y;
        if (this.scrollable() && event.x() >= sx && event.x() < sx + SCROLL_W && event.y() >= sy && event.y() < sy + SCROLL_H) {
            this.scrolling = true;
            this.dragTo(event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.scrolling && this.scrollable()) {
            this.dragTo(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    private void dragTo(double mouseY) {
        int top = this.topPos + GRID_Y;
        this.scroll = Mth.clamp(((float) mouseY - top - KNOB_H / 2.0F) / (SCROLL_H - KNOB_H), 0.0F, 1.0F);
        this.startRow = (int) (this.scroll * this.offscreenRows() + 0.5F);
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
            this.startRow = Mth.clamp(this.startRow - (int) Math.signum(scrollY), 0, rows);
            this.scroll = (float) this.startRow / rows;
        }
        return true;
    }
}
