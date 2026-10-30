package com.simplestorage.client;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiFilterButton;
import com.simplelib.api.client.ui.UiPalette;
import com.simplelib.api.client.ui.UiStyleToggle;
import com.simplestorage.pipe.Pipes;
import com.simplestorage.pipe.PipeMenu;
import com.simplestorage.pipe.PipeTier;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The pipe's window (ST27-1): the one slot in the middle, the filter key on the left (the same key and caption as every filtering
 * block, {@link UiFilterButton}), the output key on the right (names the side, a click turns it). In the box colours of the pipe's
 * step like the chests; with the style off, in Vanilla's grey.
 */
public class PipeScreen extends AbstractContainerScreen<PipeMenu> {
    private static final int INVENTORY_HEIGHT = UiBoxes.FRAME + 58 + 17 + UiBoxes.FRAME_BOTTOM;
    private static final int FILTER_X = 30, KEY_Y = 19, OUTPUT_X = 112, OUTPUT_WIDTH = 56, KEY_HEIGHT = 18;

    private UiFilterButton filterButton;

    public PipeScreen(PipeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PipeMenu.IMAGE_WIDTH, 133);
        this.inventoryLabelY = PipeMenu.INVENTORY_Y - 11;
    }

    static UiPalette palette(PipeTier tier) {
        return switch (tier) {
            case BASIC -> UiPalette.BARREL;
            case REINFORCED -> UiPalette.REINFORCED;
            case NETHERITE -> UiPalette.NETHERITE;
            case ENDERITE -> UiPalette.ENDERITE;
        };
    }

    private UiPalette palette() {
        return palette(this.menu.tier());
    }

    @Override
    protected void init() {
        super.init();
        this.filterButton = addRenderableWidget(new UiFilterButton(this.leftPos + FILTER_X, this.topPos + KEY_Y, button -> press(PipeMenu.BUTTON_FILTER),
                this.menu::filterMode, this::palette));
        addRenderableWidget(new OutputButton(this.leftPos + OUTPUT_X, this.topPos + KEY_Y, button -> press(PipeMenu.BUTTON_OUTPUT), this.menu, this::palette));
    }

    private void press(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        // The first step has no filter (the tier data reaches the client after init, so the key follows it every frame).
        if (this.filterButton != null) this.filterButton.visible = this.menu.tier().canFilter();
        int left = this.leftPos, top = this.topPos;
        if (UiStyleToggle.isEnabled()) {
            UiPalette p = palette();
            UiBoxes.box(graphics, left, top, this.imageWidth, PipeMenu.INVENTORY_Y - UiBoxes.FRAME - 2, p);
            UiBoxes.box(graphics, left, top + PipeMenu.INVENTORY_Y - UiBoxes.FRAME, this.imageWidth, INVENTORY_HEIGHT, UiPalette.INVENTORY);
            for (Slot slot : this.menu.slots) {
                UiBoxes.slot(graphics, left + slot.x, top + slot.y, slot.container instanceof Inventory ? UiPalette.INVENTORY : p);
            }
        } else {
            TieredChestScreen.panel(graphics, left, top, this.imageWidth, this.imageHeight);
            for (Slot slot : this.menu.slots) TieredChestScreen.slot(graphics, left + slot.x, top + slot.y);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (UiStyleToggle.isEnabled()) {
            // Only the title in the box's label colour and the filter caption (the inventory needs no label).
            graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, palette().label(), false);
            if (this.menu.tier().canFilter()) UiFilterButton.label(graphics, FILTER_X - UiFilterButton.LABEL_WIDTH - 2, KEY_Y + 3, palette());
            return;
        }
        super.extractLabels(graphics, mouseX, mouseY);
        if (this.menu.tier().canFilter()) UiFilterButton.label(graphics, FILTER_X - UiFilterButton.LABEL_WIDTH - 2, KEY_Y + 3, UiPalette.INVENTORY);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        int x = mouseX - this.leftPos, y = mouseY - this.topPos;
        if (y < KEY_Y || y >= KEY_Y + KEY_HEIGHT) return;
        if (x >= FILTER_X && x < FILTER_X + UiFilterButton.SIZE && this.menu.tier().canFilter()) {
            String mode = switch (this.menu.filterMode()) {
                case 1 -> "exact";
                case 2 -> "kind";
                default -> "off";
            };
            graphics.setTooltipForNextFrame(Component.translatable("tooltip.simplestorage.pipe.filter." + mode), mouseX, mouseY);
        } else if (x >= OUTPUT_X && x < OUTPUT_X + OUTPUT_WIDTH) {
            graphics.setTooltipForNextFrame(Component.translatable("tooltip.simplestorage.pipe.output", Pipes.directionName(this.menu.output())), mouseX, mouseY);
        }
    }

    /** The output key: a raised key naming the side the pipe pushes to. */
    private static final class OutputButton extends Button {
        private final PipeMenu menu;
        private final Supplier<UiPalette> palette;

        OutputButton(int x, int y, OnPress onPress, PipeMenu menu, Supplier<UiPalette> palette) {
            super(x, y, OUTPUT_WIDTH, KEY_HEIGHT, Component.empty(), onPress, DEFAULT_NARRATION);
            this.menu = menu;
            this.palette = palette;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            UiPalette p = this.palette.get();
            UiBoxes.raised(g, getX(), getY(), getWidth(), getHeight(), UiPalette.mix(p.fill(), 0xFFFFFFFF, isHoveredOrFocused() ? 0.32 : 0.18));
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            Component name = Pipes.directionName(this.menu.output());
            int color = UiPalette.luminance(UiPalette.mix(p.fill(), 0xFFFFFFFF, 0.18)) > 0.5 ? UiPalette.DARK_LABEL : UiPalette.LIGHT_LABEL;
            g.text(mc.font, name, getX() + (getWidth() - mc.font.width(name)) / 2, getY() + 5, color, false);
        }
    }
}
