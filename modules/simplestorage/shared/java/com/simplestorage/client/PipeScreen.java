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
import net.minecraft.core.Direction;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The pipe's window (ST27-1): the one slot in the middle, the filter key on the left (the same key and caption as every filtering
 * block, {@link UiFilterButton}), six side keys on the right in a hexagon (one is the output, the others take items in; a click on a side makes it the output). In the box colours of the pipe's
 * step like the chests; with the style off, in Vanilla's grey.
 */
public class PipeScreen extends AbstractContainerScreen<PipeMenu> {
    private static final int INVENTORY_HEIGHT = UiBoxes.FRAME + 58 + 17 + UiBoxes.FRAME_BOTTOM;
    private static final int FILTER_X = 30, KEY_Y = 19, KEY_HEIGHT = 18;
    /** The side keys: 11 px squares around this middle (a hexagon: up, down, four slants). */
    private static final int SIDE_SIZE = 11, HEX_X = 139, HEX_Y = 24;

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
        for (Direction side : Direction.values()) {
            int[] at = hexOffset(side);
            addRenderableWidget(new SideButton(this.leftPos + HEX_X + at[0] - SIDE_SIZE / 2, this.topPos + HEX_Y + at[1] - SIDE_SIZE / 2,
                    button -> press(PipeMenu.BUTTON_SIDE + side.ordinal()), this.menu, side, this::palette));
        }
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
        for (Direction side : Direction.values()) {
            int[] at = hexOffset(side);
            int left = HEX_X + at[0] - SIDE_SIZE / 2, top = HEX_Y + at[1] - SIDE_SIZE / 2;
            if (x >= left && x < left + SIDE_SIZE && y >= top && y < top + SIDE_SIZE) {
                String key = !Pipes.allowed(this.menu.tier(), side) ? "locked" : this.menu.output() == side ? "chosen" : "side";
                graphics.setTooltipForNextFrame(Component.translatable("tooltip.simplestorage.pipe." + key, Pipes.directionName(side)), mouseX, mouseY);
                return;
            }
        }
        if (y < KEY_Y || y >= KEY_Y + KEY_HEIGHT) return;
        if (x >= FILTER_X && x < FILTER_X + UiFilterButton.SIZE && this.menu.tier().canFilter()) {
            String mode = switch (this.menu.filterMode()) {
                case 1 -> "exact";
                case 2 -> "kind";
                default -> "off";
            };
            graphics.setTooltipForNextFrame(Component.translatable("tooltip.simplestorage.pipe.filter." + mode), mouseX, mouseY);
        }
    }

    /** Where each side key sits in the hexagon (offset from its middle): up and down in the centre line, north..west on the four slants. */
    static int[] hexOffset(Direction side) {
        return switch (side) {
            case UP -> new int[] {0, -13};
            case DOWN -> new int[] {0, 13};
            case NORTH -> new int[] {-12, -6};
            case EAST -> new int[] {12, -6};
            case SOUTH -> new int[] {12, 6};
            case WEST -> new int[] {-12, 6};
        };
    }

    /** Arrow pictures (7 x 7, '#' = ink): up, and the slant up-left; the other directions mirror them. */
    private static final String[] ARROW_UP = {"...#...", "..###..", ".#.#.#.", "...#...", "...#...", "...#...", "......."};
    private static final String[] ARROW_SLANT = {"#####..", "###....", "##.#...", "#..#...", "#...#..", ".....#.", "......."};

    /** Whether the arrow picture has ink at (x, y) once turned for {@code side}. */
    static boolean ink(Direction side, int x, int y) {
        int mx = 6 - x, my = 6 - y;
        return switch (side) {
            case UP -> ARROW_UP[y].charAt(x) == '#';
            case DOWN -> ARROW_UP[my].charAt(x) == '#';
            case NORTH -> ARROW_SLANT[y].charAt(x) == '#';
            case EAST -> ARROW_SLANT[y].charAt(mx) == '#';
            case SOUTH -> ARROW_SLANT[my].charAt(mx) == '#';
            case WEST -> ARROW_SLANT[my].charAt(x) == '#';
        };
    }

    /** One of the six side keys: a raised key with an arrow; the chosen output sinks in and lights up, a side the step may not point to is dark. */
    private static final class SideButton extends Button {
        private final PipeMenu menu;
        private final Direction side;
        private final Supplier<UiPalette> palette;

        SideButton(int x, int y, OnPress onPress, PipeMenu menu, Direction side, Supplier<UiPalette> palette) {
            super(x, y, SIDE_SIZE, SIDE_SIZE, Component.empty(), onPress, DEFAULT_NARRATION);
            this.menu = menu;
            this.side = side;
            this.palette = palette;
        }

        boolean allowed() {
            return Pipes.allowed(this.menu.tier(), this.side);
        }

        boolean chosen() {
            return this.menu.output() == this.side;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            UiPalette p = this.palette.get();
            boolean chosen = chosen(), allowed = allowed();
            double light = !allowed ? 0.0 : chosen ? -0.25 : isHoveredOrFocused() ? 0.32 : 0.18;
            int face = light < 0 ? UiPalette.mix(p.fill(), 0xFF000000, -light) : UiPalette.mix(p.fill(), 0xFFFFFFFF, light);
            if (chosen) UiBoxes.sunkRect(g, getX(), getY(), getWidth(), getHeight(), p);
            else UiBoxes.raised(g, getX(), getY(), getWidth(), getHeight(), face);
            int ink = !allowed ? 0x66000000 : chosen ? 0xFFFFE070 : UiPalette.luminance(face) > 0.5 ? UiPalette.DARK_LABEL : UiPalette.LIGHT_LABEL;
            int ax = getX() + (getWidth() - 7) / 2, ay = getY() + (getHeight() - 7) / 2;
            for (int y = 0; y < 7; y++) {
                for (int x = 0; x < 7; x++) {
                    if (ink(this.side, x, y)) g.fill(ax + x, ay + y, ax + x + 1, ay + y + 1, ink);
                }
            }
        }
    }
}
