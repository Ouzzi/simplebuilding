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
import org.jetbrains.annotations.Nullable;

/**
 * The network window of the controller chest (BS29b-3), in the box style of the chests: the list of every kind in the network
 * (counts in the corner, exact total in the tooltip), the sort key and two scroll keys above it (the mouse wheel scrolls too), the
 * number of chests against the limit. A broken controller shows a dark panel with a red cross and its cause instead of the list.
 */
public class ControllerScreen extends AbstractContainerScreen<ControllerMenu> {
    private static final int INVENTORY_HEIGHT = UiBoxes.FRAME + 58 + 17 + UiBoxes.FRAME_BOTTOM;
    private static final int KEY_Y = 15, KEY_HEIGHT = 13, SORT_X = 8, SORT_WIDTH = 52, ARROW_WIDTH = 13, UP_X = 142, DOWN_X = 157;
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
        addRenderableWidget(new Key(this.leftPos + UP_X, this.topPos + KEY_Y, ARROW_WIDTH, KEY_HEIGHT, button -> press(ControllerMenu.BUTTON_UP), palette,
                (g, x, y, w, h, p) -> arrow(g, x + w / 2, y + 3, true, labelColor(p))));
        addRenderableWidget(new Key(this.leftPos + DOWN_X, this.topPos + KEY_Y, ARROW_WIDTH, KEY_HEIGHT, button -> press(ControllerMenu.BUTTON_DOWN), palette,
                (g, x, y, w, h, p) -> arrow(g, x + w / 2, y + 3, false, labelColor(p))));
    }

    private static int labelColor(UiPalette p) {
        return UiPalette.luminance(UiPalette.mix(p.fill(), 0xFFFFFFFF, 0.18)) > 0.5 ? UiPalette.DARK_LABEL : UiPalette.LIGHT_LABEL;
    }

    /** A 7 x 4 arrow head pointing up or down, centred on {@code cx}. */
    private static void arrow(GuiGraphicsExtractor g, int cx, int y, boolean up, int color) {
        for (int row = 0; row < 4; row++) {
            int half = up ? row : 3 - row;
            g.fill(cx - half, y + row + 1, cx + half + 1, y + row + 2, color);
        }
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
        if (this.menu.status() != ChestNetwork.Status.OK) brokenPanel(graphics, left, top);
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
        // Chests against the limit, between the sort key and the scroll keys.
        String count = this.menu.chests() + "/" + this.menu.limit();
        boolean ok = this.menu.status() == ChestNetwork.Status.OK;
        int color = ok ? (styled ? p.label() : 0xFF404040) : RED;
        graphics.text(this.font, count + (this.menu.rejected() > 0 ? " !" : ""), SORT_X + SORT_WIDTH + 6, KEY_Y + 3, color, false);
    }

    @Override
    protected void renderSlotContents(GuiGraphicsExtractor graphics, ItemStack stack, Slot slot, @Nullable String itemCount) {
        if (slot.index < ControllerMenu.CELLS && !stack.isEmpty()) {
            super.renderSlotContents(graphics, stack, slot, shortCount(this.menu.countAt(slot.index)));
        } else {
            super.renderSlotContents(graphics, stack, slot, itemCount);
        }
    }

    /** 1..999, then 1k..999k, then 1M..: the exact total is in the tooltip. */
    static String shortCount(long count) {
        if (count < 1000) return Long.toString(count);
        if (count < 1_000_000) return (count / 1000) + "k";
        return (count / 1_000_000) + "M";
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        Slot hovered = getHoveredSlot();
        if (hovered != null && hovered.index < ControllerMenu.CELLS) {
            lines.add(Component.translatable("tooltip.simplestorage.controller.total", this.menu.countAt(hovered.index)).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        return lines;
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        int x = mouseX - this.leftPos, y = mouseY - this.topPos;
        if (y < KEY_Y || y >= KEY_Y + KEY_HEIGHT) return;
        if (x >= SORT_X && x < SORT_X + SORT_WIDTH) {
            graphics.setTooltipForNextFrame(Component.translatable("tooltip.simplestorage.controller.sort"), mouseX, mouseY);
        } else if (x >= SORT_X + SORT_WIDTH && x < UP_X && this.menu.rejected() > 0) {
            graphics.setTooltipForNextFrame(Component.translatable("tooltip.simplestorage.controller.rejected", this.menu.rejected()), mouseX, mouseY);
        } else if (x >= SORT_X + SORT_WIDTH && x < UP_X) {
            graphics.setTooltipForNextFrame(Component.translatable("tooltip.simplestorage.controller.chests"), mouseX, mouseY);
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
