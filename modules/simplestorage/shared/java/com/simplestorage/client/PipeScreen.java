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
    /** The side keys (ROHR33): 11 px squares in a compass cross around this middle (north on top, west left, east right, south below) and an up/down column beside it. */
    private static final int SIDE_SIZE = 11, HEX_X = 126, HEX_Y = 26, COLUMN_X = 158;
    /** Light red output key with dark ink (about 9:1) and the greyed-out off key with dark ink (about 5.5:1); both beat 4.5:1. */
    static final int OUT_FACE = 0xFFFF9A8F, OUT_INK = 0xFF3A0804, OFF_FACE = 0xFFA0A0A0, OFF_INK = 0xFF262626;

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
                graphics.setTooltipForNextFrame(this.font, sideTooltipLines(this.menu.tier(), side, this.menu.sideMode(side)), java.util.Optional.empty(), mouseX, mouseY);
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

    /** The tooltip lines of a side: its state, what it does, what a click does (ROHR33; every line at most 48 characters). */
    static java.util.List<net.minecraft.network.chat.Component> sideTooltipLines(PipeTier tier, Direction side, int mode) {
        String name = switch (mode) {
            case Pipes.OUT -> "out";
            case Pipes.OFF -> "off";
            default -> "in";
        };
        String click = switch (Pipes.nextMode(tier, side, mode)) {
            case Pipes.OUT -> "out";
            case Pipes.OFF -> "off";
            default -> "in";
        };
        java.util.List<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable("tooltip.simplestorage.pipe.side." + name, Pipes.directionName(side)));
        lines.add(Component.translatable("tooltip.simplestorage.pipe.side." + name + ".info").withStyle(net.minecraft.ChatFormatting.GRAY));
        if (!Pipes.allowed(tier, side)) lines.add(Component.translatable("tooltip.simplestorage.pipe.side.up_locked").withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.simplestorage.pipe.click." + click).withStyle(net.minecraft.ChatFormatting.YELLOW));
        return lines;
    }

    /** Where each side key sits (offset from the cross middle; up and down in their own column to the right). */
    static int[] hexOffset(Direction side) {
        return switch (side) {
            case NORTH -> new int[] {0, -12};
            case WEST -> new int[] {-12, 0};
            case EAST -> new int[] {12, 0};
            case SOUTH -> new int[] {0, 12};
            case UP -> new int[] {COLUMN_X - HEX_X, -6};
            case DOWN -> new int[] {COLUMN_X - HEX_X, 6};
        };
    }

    /** The arrow picture (7 x 7, '#' = ink) of the up key; the down key mirrors it. The four compass keys show a letter. */
    private static final String[] ARROW_UP = {"...#...", "..###..", ".#.#.#.", "...#...", "...#...", "...#...", "......."};

    /** Whether the arrow picture has ink at (x, y) for the up or down key. */
    static boolean ink(Direction side, int x, int y) {
        return side == Direction.UP ? ARROW_UP[y].charAt(x) == '#' : ARROW_UP[6 - y].charAt(x) == '#';
    }

    /** One of the six side keys: a raised key (input), a light red sunken key (output) or a greyed-out sunken key (off); a letter for the compass sides, an arrow for up and down. */
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

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            UiPalette p = this.palette.get();
            int mode = this.menu.sideMode(this.side);
            int face, ink;
            if (mode == Pipes.OUT) {
                face = isHoveredOrFocused() ? UiPalette.mix(OUT_FACE, 0xFFFFFFFF, 0.25) : OUT_FACE;
                ink = OUT_INK;
                UiBoxes.insetColored(g, getX(), getY(), getWidth(), getHeight(), p, face);
            } else if (mode == Pipes.OFF) {
                face = isHoveredOrFocused() ? UiPalette.mix(OFF_FACE, 0xFFFFFFFF, 0.2) : OFF_FACE;
                ink = OFF_INK;
                UiBoxes.insetColored(g, getX(), getY(), getWidth(), getHeight(), p, face);
            } else {
                face = UiPalette.mix(p.fill(), 0xFFFFFFFF, isHoveredOrFocused() ? 0.32 : 0.18);
                ink = UiPalette.luminance(face) > 0.5 ? UiPalette.DARK_LABEL : UiPalette.LIGHT_LABEL;
                UiBoxes.raised(g, getX(), getY(), getWidth(), getHeight(), face);
            }
            if (this.side.getAxis().isVertical()) {
                int ax = getX() + (getWidth() - 7) / 2, ay = getY() + (getHeight() - 7) / 2;
                for (int y = 0; y < 7; y++) {
                    for (int x = 0; x < 7; x++) {
                        if (ink(this.side, x, y)) g.fill(ax + x, ay + y, ax + x + 1, ay + y + 1, ink);
                    }
                }
            } else {
                Component letter = Component.translatable("tooltip.simplestorage.pipe.side.short." + this.side.getName());
                var font = net.minecraft.client.Minecraft.getInstance().font;
                g.text(font, letter, getX() + (getWidth() - font.width(letter)) / 2 + 1, getY() + 2, ink, false);
            }
        }
    }
}
