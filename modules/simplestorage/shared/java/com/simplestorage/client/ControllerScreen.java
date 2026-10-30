package com.simplestorage.client;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
import com.simplelib.api.client.ui.UiStyleToggle;
import com.simplelib.api.client.ui.UiSymbols;
import com.simplestorage.network.ChestNetwork;
import com.simplestorage.network.ControllerLevel;
import com.simplestorage.network.ControllerMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The network window of the controller chest (BS29b-3), in the box style of the chests: the list of every kind in the network
 * (compact counts in the corner like "32k", exact total in the tooltip), the sort key and the search field above it, a scrollbar
 * (drag it, or use the mouse wheel), the number of chests against the limit. A broken controller shows a dark panel with a red cross and its cause instead of the list.
 */
public class ControllerScreen extends AbstractContainerScreen<ControllerMenu> {
    private static final int INVENTORY_HEIGHT = UiBoxes.FRAME + 58 + 17 + UiBoxes.FRAME_BOTTOM;
    private static final int KEY_Y = 15, KEY_HEIGHT = 13, SORT_X = 8, SORT_WIDTH = 52, SEARCH_X = 64, SEARCH_WIDTH = 100;
    /** The scrollbar right of the list: track and thumb. */
    private static final int BAR_X = ControllerMenu.VIEW_X + 9 * 18 + 4, BAR_WIDTH = 10, BAR_Y = ControllerMenu.VIEW_Y, BAR_HEIGHT = ControllerMenu.ROWS * 18;
    private net.minecraft.client.gui.components.EditBox search;
    private String sentSearch = "";
    private boolean draggingBar;
    private static final int RED = 0xFFFF6B5E, PANEL_TEXT = 0xFFEFE4DA;

    public ControllerScreen(ControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ControllerMenu.IMAGE_WIDTH, ControllerMenu.IMAGE_HEIGHT);
        this.inventoryLabelY = ControllerMenu.INVENTORY_Y - 11;
    }

    private UiPalette palette() {
        return switch (this.menu.step()) {
            case WOODEN -> UiPalette.BARREL;
            case REINFORCED -> UiPalette.REINFORCED;
            case NETHERITE -> UiPalette.NETHERITE;
            case ENDERITE -> UiPalette.ENDERITE;
        };
    }

    @Override
    protected void init() {
        super.init();
        Supplier<UiPalette> palette = this::palette;
        addRenderableWidget(new Key(this.leftPos + SORT_X, this.topPos + KEY_Y, SORT_WIDTH, KEY_HEIGHT, button -> press(ControllerMenu.BUTTON_SORT), palette,
                (g, x, y, w, h, p) -> {
                    Component label = Component.translatable("gui.simplestorage.controller.sort." + this.menu.sort().name().toLowerCase(java.util.Locale.ROOT));
                    g.text(this.font, label, x + (w - this.font.width(label)) / 2, y + 3, labelColor(p), false);
                }));
        this.search = new net.minecraft.client.gui.components.EditBox(this.font, this.leftPos + SEARCH_X, this.topPos + KEY_Y, SEARCH_WIDTH, KEY_HEIGHT,
                Component.translatable("gui.simplestorage.controller.search")) {
            @Override
            public void setFocused(boolean focused) {
                super.setFocused(focused);
                net.minecraft.client.Minecraft.getInstance().onTextInputFocusChange(this, focused);
            }
        };
        this.search.setMaxLength(ChestNetwork.MAX_SEARCH);
        this.search.setHint(Component.translatable("gui.simplestorage.controller.search").withStyle(net.minecraft.ChatFormatting.GRAY));
        this.search.setResponder(this::sendSearch);
        addRenderableWidget(this.search);
        this.sentSearch = "";
    }

    private static int labelColor(UiPalette p) {
        return UiPalette.luminance(UiPalette.mix(p.fill(), 0xFFFFFFFF, 0.18)) > 0.5 ? UiPalette.DARK_LABEL : UiPalette.LIGHT_LABEL;
    }

    /**
     * Brings the server's search text to {@code text}: the menu has no text channel, so the difference goes as menu clicks (clear / back /
     * one click per character, {@link ControllerMenu#CHAR} + code point). A search is a few characters, so this is a few tiny packets.
     */
    private void sendSearch(String text) {
        int common = 0;
        while (common < text.length() && common < this.sentSearch.length() && text.charAt(common) == this.sentSearch.charAt(common)) common++;
        if (common == 0 && !this.sentSearch.isEmpty()) {
            press(ControllerMenu.BUTTON_SEARCH_CLEAR);
        } else {
            for (int i = this.sentSearch.codePointCount(common, this.sentSearch.length()); i > 0; i--) press(ControllerMenu.BUTTON_SEARCH_BACK);
        }
        text.substring(common == 0 ? 0 : common).codePoints().forEach(cp -> press(ControllerMenu.CHAR + cp));
        this.sentSearch = text;
    }

    private void press(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY != 0 && this.menu.status() == ChestNetwork.Status.OK) {
            press(scrollY > 0 ? ControllerMenu.BUTTON_UP : ControllerMenu.BUTTON_DOWN);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    private boolean overBar(double mx, double my) {
        double x = mx - this.leftPos, y = my - this.topPos;
        return x >= BAR_X && x < BAR_X + BAR_WIDTH && y >= BAR_Y && y < BAR_Y + BAR_HEIGHT;
    }

    /** The thumb height and its top: proportional to the rows shown out of all rows. */
    private int thumbHeight() {
        int total = this.menu.maxScroll() + ControllerMenu.ROWS;
        return Math.max(10, BAR_HEIGHT * ControllerMenu.ROWS / total);
    }

    private void barTo(double my) {
        int max = this.menu.maxScroll();
        if (max <= 0) return;
        int thumb = thumbHeight();
        double t = (my - this.topPos - BAR_Y - thumb / 2.0) / (BAR_HEIGHT - thumb);
        int row = (int) Math.round(Math.max(0, Math.min(1, t)) * max);
        if (row != this.menu.scrollRow()) press(ControllerMenu.SCROLL_TO + row);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && this.menu.status() == ChestNetwork.Status.OK && overBar(event.x(), event.y())) {
            this.draggingBar = true;
            barTo(event.y());
            return true;
        }
        // A right click on the search field clears it, like the ME terminal.
        if (event.button() == 1 && this.search != null && this.search.isMouseOver(event.x(), event.y())) {
            this.search.setValue("");
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        if (this.draggingBar) {
            barTo(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        this.draggingBar = false;
        return super.mouseReleased(event);
    }

    /** While the search field has the focus, keys type into it (the inventory key must not close the window). */
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (this.search != null && this.search.isFocused() && event.key() != com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
            this.search.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int left = this.leftPos, top = this.topPos;
        if (UiStyleToggle.isEnabled()) {
            UiPalette p = palette();
            UiBoxes.box(graphics, left, top, this.imageWidth, ControllerMenu.INVENTORY_Y - UiBoxes.FRAME - 2, p);
            UiBoxes.box(graphics, left, top + ControllerMenu.INVENTORY_Y - UiBoxes.FRAME, this.imageWidth, INVENTORY_HEIGHT, UiPalette.INVENTORY);
            for (Slot slot : this.menu.slots) {
                UiBoxes.slot(graphics, left + slot.x, top + slot.y, slot.index < ControllerMenu.CELLS ? p : UiPalette.INVENTORY);
            }
        } else {
            TieredChestScreen.panel(graphics, left, top, this.imageWidth, this.imageHeight);
            for (Slot slot : this.menu.slots) TieredChestScreen.slot(graphics, left + slot.x, top + slot.y);
        }
        scrollbar(graphics, left, top);
        if (this.menu.status() != ChestNetwork.Status.OK) brokenPanel(graphics, left, top);
    }

    /** The scrollbar: an inset track with a raised thumb in the box colour (the thumb fills the track when everything fits). */
    private void scrollbar(GuiGraphicsExtractor g, int left, int top) {
        UiPalette p = UiStyleToggle.isEnabled() ? palette() : UiPalette.INVENTORY;
        int x = left + BAR_X, y = top + BAR_Y;
        UiBoxes.inset(g, x - 1, y - 1, BAR_WIDTH + 2, BAR_HEIGHT + 2, p);
        int max = this.menu.maxScroll();
        int thumb = max <= 0 ? BAR_HEIGHT : thumbHeight();
        int at = max <= 0 ? 0 : (BAR_HEIGHT - thumb) * this.menu.scrollRow() / max;
        UiBoxes.raised(g, x, y + at, BAR_WIDTH, thumb, UiPalette.mix(p.fill(), 0xFFFFFFFF, max <= 0 ? 0.08 : this.draggingBar ? 0.38 : 0.2));
    }

    /** The "broken" panel over the list: dark inset, a red cross, the cause. */
    private void brokenPanel(GuiGraphicsExtractor g, int left, int top) {
        UiPalette p = palette();
        int x = left + ControllerMenu.VIEW_X - 2, y = top + ControllerMenu.VIEW_Y - 2, w = 9 * 18 + 3, h = ControllerMenu.ROWS * 18 + 3;
        UiBoxes.insetColored(g, x, y, w, h, p, 0xFF2A1F1D);
        UiSymbols.engrave(g, UiSymbols.CROSS, x + (w - 7) / 2, y + 12, p, RED, false);
        List<Component> lines = brokenLines();
        int ty = y + 28;
        for (Component line : lines) {
            for (var part : this.font.split(line, w - 12)) {
                g.text(this.font, part, x + (w - this.font.width(part)) / 2, ty, line == lines.get(0) ? RED : PANEL_TEXT, false);
                ty += 10;
            }
            ty += 3;
        }
    }

    private List<Component> brokenLines() {
        List<Component> lines = new ArrayList<>();
        ChestNetwork.Status status = this.menu.status();
        lines.add(Component.translatable("gui.simplestorage.controller.broken"));
        lines.add(switch (status) {
            case TOO_MANY -> Component.translatable("gui.simplestorage.controller.broken.too_many", this.menu.chests(), this.menu.limit());
            case CONFLICT -> Component.translatable("gui.simplestorage.controller.broken.conflict");
            default -> Component.translatable("gui.simplestorage.controller.broken.too_big", ChestNetwork.MAX_CONDUCTORS);
        });
        lines.add(Component.translatable("gui.simplestorage.controller.broken.fix"));
        return lines;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        boolean styled = UiStyleToggle.isEnabled();
        UiPalette p = palette();
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, styled ? p.label() : 0xFF404040, false);
        // Chests against the limit, top right in the title line.
        String count = this.menu.chests() + "/" + this.menu.limit();
        boolean ok = this.menu.status() == ChestNetwork.Status.OK;
        int color = ok ? (styled ? p.label() : 0xFF404040) : RED;
        String text = count + (this.menu.rejected() > 0 ? " !" : "");
        graphics.text(this.font, text, this.imageWidth - 8 - this.font.width(text), this.titleLabelY, color, false);
    }

    /** The list cells show the item; the total goes into the corner like a stack count (the exact number is in the tooltip). */
    @Override
    protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        super.extractSlot(graphics, slot, mouseX, mouseY);
        if (slot.index < ControllerMenu.CELLS && slot.hasItem()) {
            graphics.itemDecorations(this.font, slot.getItem(), slot.x, slot.y, ChestNetwork.shortCount(this.menu.countAt(slot.index)));
        }
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        Slot hovered = this.hoveredSlot;
        if (hovered != null && hovered.index < ControllerMenu.CELLS) {
            lines.add(Component.translatable("tooltip.simplestorage.controller.total", this.menu.countAt(hovered.index)).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        return lines;
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        int x = mouseX - this.leftPos, y = mouseY - this.topPos;
        if (y < KEY_Y - 12 || y >= KEY_Y + KEY_HEIGHT) return;
        if (y < KEY_Y && x >= this.imageWidth - 50) {
            graphics.setTooltipForNextFrame(Component.translatable(this.menu.rejected() > 0 ? "tooltip.simplestorage.controller.rejected" : "tooltip.simplestorage.controller.chests",
                    this.menu.rejected()), mouseX, mouseY);
        } else if (y >= KEY_Y && x >= SORT_X && x < SORT_X + SORT_WIDTH) {
            graphics.setTooltipForNextFrame(Component.translatable("tooltip.simplestorage.controller.sort"), mouseX, mouseY);
        } else if (y >= KEY_Y && x >= SEARCH_X && x < SEARCH_X + SEARCH_WIDTH) {
            graphics.setTooltipForNextFrame(Component.translatable("tooltip.simplestorage.controller.search"), mouseX, mouseY);
        }
    }

    /** Draws its face and content from the caller (sort label, arrows). */
    private interface Face {
        void draw(GuiGraphicsExtractor g, int x, int y, int w, int h, UiPalette p);
    }

    private static final class Key extends Button {
        private final Supplier<UiPalette> palette;
        private final Face face;

        Key(int x, int y, int width, int height, OnPress onPress, Supplier<UiPalette> palette, Face face) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.palette = palette;
            this.face = face;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            UiPalette p = this.palette.get();
            UiBoxes.raised(g, getX(), getY(), getWidth(), getHeight(), UiPalette.mix(p.fill(), 0xFFFFFFFF, isHoveredOrFocused() ? 0.32 : 0.18));
            this.face.draw(g, getX(), getY(), getWidth(), getHeight(), p);
        }
    }
}
