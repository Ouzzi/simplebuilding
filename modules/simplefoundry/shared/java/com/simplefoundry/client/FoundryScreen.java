package com.simplefoundry.client;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.simplefoundry.Layers;
import com.simplefoundry.Metal;
import com.simplefoundry.Tier;
import com.simplefoundry.block.DeskBlockEntity;
import com.simplefoundry.block.FoundryMenu;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/**
 * The desk window (owner FOUNDRY32-UI = A, 02.11.): the places on the left (3 / 6 / 9 / 12 by tier, the others locked), the section of
 * the tank in the middle (one band per level, the melts drawn on top of each other, the arrow below moves the selected level), the stack
 * of melts on the right with the fuel tank and its slot. Boxes, slots and insets are the shared blocks of {@code com.simplelib.api.client.ui};
 * all text sits on a dark panel or on the palette label colour (contrast at least 4.5:1).
 */
public class FoundryScreen extends AbstractContainerScreen<FoundryMenu> {
    /** Brick-grey box of the foundry: light enough for the dark label. */
    private static final UiPalette FOUNDRY = new UiPalette(0xFFB09585, 0xFFD1BBAE, 0xFF8D7466, 0xFF8A6F61, 0xFF6B5348, 0xFF24160F);
    private static final UiPalette INVENTORY = UiPalette.INVENTORY;
    private static final int PANEL = 0xFF2B2B2B, TEXT_LIGHT = 0xFFFFFFFF, TEXT_DIM = 0xFFC8C8C8;
    private static final int[] METAL_TOP = {0xFFF6D04D, 0xFFE0834F, 0xFFD9DADD};
    private static final int[] METAL_LOW = {0xFFC99F2B, 0xFFB35F35, 0xFFA9ABB0};
    private static final int FUEL_COLOR = 0xFFE8742A, FUEL_LIT = 0xFFFFA23A, GREEN = 0xFF52B13C, RED = 0xFFD8402F;
    /** Section of the tank (relative to the window). */
    static final int SEC_X = 70, SEC_Y = 22, SEC_W = 36, SEC_H = 62;
    static final int ARROW_X = 70, ARROW_Y = 87, ARROW_W = 36, ARROW_H = 12;
    static final int STACK_X = 112, STACK_Y = 22, STACK_W = 56, STACK_H = 41;
    static final int GAUGE_X = 112, GAUGE_Y = 68, GAUGE_W = 56, GAUGE_H = 10;

    public FoundryScreen(FoundryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, FoundryMenu.WIDTH, FoundryMenu.HEIGHT);
        titleLabelX = 8;
        titleLabelY = 8;
        inventoryLabelX = 8;
        inventoryLabelY = 116;
    }

    /** The arrow under the section is a real Vanilla button (a click on it is the safest input path); a click on a level of the section is the shortcut. */
    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("\u25BC"), button -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, FoundryMenu.ARROW_BUTTON);
        }).bounds(leftPos + ARROW_X, topPos + ARROW_Y, ARROW_W, ARROW_H).build());
    }

    /** Level (0 = lowest) of the section under {@code mx, my} (relative to the window), or -1. */
    private int levelAt(double mx, double my) {
        if (mx < SEC_X || mx >= SEC_X + SEC_W || my < SEC_Y || my >= SEC_Y + SEC_H) return -1;
        int levels = menu.levels();
        return Math.max(0, Math.min(levels - 1, (int) ((SEC_Y + SEC_H - 1 - my) * levels / SEC_H)));
    }

    private boolean overArrow(double mx, double my) {
        return mx >= ARROW_X && mx < ARROW_X + ARROW_W && my >= ARROW_Y && my < ARROW_Y + ARROW_H;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x0 = leftPos, y0 = topPos;
        UiBoxes.box(g, x0, y0, FoundryMenu.WIDTH, 108, FOUNDRY);
        UiBoxes.box(g, x0, y0 + 110, FoundryMenu.WIDTH, FoundryMenu.HEIGHT - 110, INVENTORY);
        int mx = mouseX - x0, my = mouseY - y0;
        // places on the left: active ones are sunk slots, the rest locked
        Tier tier = menu.tier();
        for (int i = 0; i < DeskBlockEntity.INPUT_SLOTS; i++) {
            int sx = x0 + FoundryMenu.SLOT_X + (i % FoundryMenu.COLUMNS) * 18, sy = y0 + FoundryMenu.SLOT_Y + (i / FoundryMenu.COLUMNS) * 18;
            UiBoxes.slot(g, sx, sy, FOUNDRY);
            if (i >= tier.slots) {
                UiBoxes.veil(g, sx, sy, FOUNDRY);
                for (int d = 4; d < 12; d++) {
                    g.fill(sx + d, sy + d, sx + d + 1, sy + d + 1, TEXT_DIM);
                    g.fill(sx + 15 - d, sy + d, sx + 16 - d, sy + d + 1, TEXT_DIM);
                }
            }
        }
        // section of the tank
        int sx = x0 + SEC_X, sy = y0 + SEC_Y;
        UiBoxes.inset(g, sx, sy, SEC_W, SEC_H, FOUNDRY);
        g.fill(sx + 1, sy + 1, sx + SEC_W - 1, sy + SEC_H - 1, PANEL);
        int[] melt = menu.melt();
        int capacity = menu.capacity();
        int levels = menu.levels();
        for (int row = 0; row < SEC_H - 2; row++) {
            // row 0 = bottom; the unit at the middle of the pixel row decides the metal
            Metal metal = capacity <= 0 ? null : Layers.atUnit(melt, (long) ((row + 0.5) * capacity / (SEC_H - 2)));
            if (metal == null) continue;
            int top = METAL_TOP[metal.ordinal()], low = METAL_LOW[metal.ordinal()];
            int yy = sy + SEC_H - 2 - row;
            for (int col = 0; col < SEC_W - 2; col++) g.fill(sx + 1 + col, yy, sx + 2 + col, yy + 1, (col + row) % 7 == 0 ? low : top);
        }
        for (int level = 1; level < levels; level++) {
            int yy = sy + SEC_H - 1 - level * (SEC_H - 2) / levels;
            g.fill(sx + 1, yy, sx + SEC_W - 1, yy + 1, 0x80101010);
        }
        int selected = Math.min(levels - 1, menu.selected());
        int selTop = sy + SEC_H - 1 - (selected + 1) * (SEC_H - 2) / levels, selBottom = sy + SEC_H - 1 - selected * (SEC_H - 2) / levels;
        g.outline(sx, selTop, SEC_W, Math.max(2, selBottom - selTop + 1), TEXT_LIGHT);
        // pointer left of the selected band
        int pointer = (selTop + selBottom) / 2;
        for (int d = 0; d < 4; d++) g.fill(sx - 5 + d, pointer - 3 + d, sx - 4 + d, pointer + 4 - d, FOUNDRY.label());
        // stack on the right: iron on top, gold at the bottom, like the tank
        UiBoxes.inset(g, x0 + STACK_X, y0 + STACK_Y, STACK_W, STACK_H, FOUNDRY);
        g.fill(x0 + STACK_X + 1, y0 + STACK_Y + 1, x0 + STACK_X + STACK_W - 1, y0 + STACK_Y + STACK_H - 1, PANEL);
        int[] waiting = menu.waiting();
        for (int row = 0; row < 3; row++) {
            Metal metal = Metal.values()[2 - row];
            int ry = y0 + STACK_Y + 3 + row * 13;
            g.fill(x0 + STACK_X + 3, ry, x0 + STACK_X + 13, ry + 10, 0xFF101010);
            g.fill(x0 + STACK_X + 4, ry + 1, x0 + STACK_X + 12, ry + 9, METAL_TOP[metal.ordinal()]);
            g.fill(x0 + STACK_X + 4, ry + 6, x0 + STACK_X + 12, ry + 9, METAL_LOW[metal.ordinal()]);
            String text = String.valueOf(melt[metal.ordinal()] / Metal.INGOT) + (waiting[metal.ordinal()] > 0 ? " +" + waiting[metal.ordinal()] / Metal.INGOT : "");
            g.text(font, text, x0 + STACK_X + 16, ry + 1, waiting[metal.ordinal()] > 0 ? TEXT_DIM : TEXT_LIGHT, false);
        }
        // fuel tank gauge
        UiBoxes.inset(g, x0 + GAUGE_X, y0 + GAUGE_Y, GAUGE_W, GAUGE_H, FOUNDRY);
        g.fill(x0 + GAUGE_X + 1, y0 + GAUGE_Y + 1, x0 + GAUGE_X + GAUGE_W - 1, y0 + GAUGE_Y + GAUGE_H - 1, PANEL);
        int filled = (int) ((long) Math.min(menu.fuel(), DeskBlockEntity.FUEL_CAPACITY) * (GAUGE_W - 2) / DeskBlockEntity.FUEL_CAPACITY);
        if (menu.fuel() > 0) filled = Math.max(1, filled);
        g.fill(x0 + GAUGE_X + 1, y0 + GAUGE_Y + 1, x0 + GAUGE_X + 1 + filled, y0 + GAUGE_Y + GAUGE_H - 1, menu.lit() ? FUEL_LIT : FUEL_COLOR);
        for (int quarter = 1; quarter < 4; quarter++) g.fill(x0 + GAUGE_X + quarter * GAUGE_W / 4, y0 + GAUGE_Y + 1, x0 + GAUGE_X + quarter * GAUGE_W / 4 + 1, y0 + GAUGE_Y + GAUGE_H - 1, 0xA0101010);
        // fuel slot
        UiBoxes.slot(g, x0 + FoundryMenu.FUEL_X, y0 + FoundryMenu.FUEL_Y, FOUNDRY);
        // status mark: green dot with a check, red with a cross (also for colour-blind players)
        boolean ok = menu.status().equals("ok");
        int bx = x0 + 160, by = y0 + 7;
        g.fill(bx, by, bx + 8, by + 8, 0xFF101010);
        g.fill(bx + 1, by + 1, bx + 7, by + 7, ok ? GREEN : RED);
        if (ok) {
            g.fill(bx + 2, by + 4, bx + 3, by + 5, TEXT_LIGHT);
            g.fill(bx + 3, by + 5, bx + 4, by + 6, TEXT_LIGHT);
            g.fill(bx + 4, by + 3, bx + 5, by + 4, TEXT_LIGHT);
            g.fill(bx + 5, by + 2, bx + 6, by + 3, TEXT_LIGHT);
        } else {
            for (int d = 2; d < 6; d++) {
                g.fill(bx + d, by + d, bx + d + 1, by + d + 1, TEXT_LIGHT);
                g.fill(bx + 7 - d, by + d, bx + 8 - d, by + d + 1, TEXT_LIGHT);
            }
        }
        if (levelAt(mx, my) >= 0) g.requestCursor(CursorTypes.POINTING_HAND);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, FOUNDRY.label(), false);
        g.text(font, Component.translatable("gui.simplefoundry.fuel_hint"), FoundryMenu.FUEL_X + 20, FoundryMenu.FUEL_Y + 4, FOUNDRY.label(), false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, INVENTORY.label(), false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (hoveredSlot != null) return;
        int mx = mouseX - leftPos, my = mouseY - topPos;
        List<Component> lines = new ArrayList<>();
        Tier tier = menu.tier();
        int place = placeAt(mx, my);
        int level = levelAt(mx, my);
        if (place >= 0 && place >= tier.slots) {
            Tier need = Tier.STONE;
            for (Tier t : Tier.values()) if (t.slots > place) { need = t; break; }
            lines.add(Component.translatable("gui.simplefoundry.locked", need.slots));
            if (need.upgradeItem() != null) lines.add(Component.translatable("gui.simplefoundry.locked_item", need.upgradeItem().getDefaultInstance().getHoverName()));
        } else if (level >= 0) {
            Metal metal = Layers.windowPart(menu.melt(), menu.capacity(), menu.levels(), level, 0, 1);
            lines.add(Component.translatable("gui.simplefoundry.level", level + 1, metal == null ? Component.translatable("simplefoundry.desk.layer_empty") : Component.translatable("simplefoundry.metal." + metal.id)));
            lines.add(Component.translatable("gui.simplefoundry.filled", used(), menu.capacity() / Metal.INGOT));
        } else if (overArrow(mx, my)) {
            lines.add(Component.translatable("gui.simplefoundry.arrow"));
        } else if (mx >= GAUGE_X && mx < GAUGE_X + GAUGE_W && my >= GAUGE_Y && my < GAUGE_Y + GAUGE_H) {
            lines.add(Component.translatable("gui.simplefoundry.fuel", menu.fuel(), DeskBlockEntity.FUEL_CAPACITY));
            lines.add(Component.translatable("gui.simplefoundry.fuel_ways"));
        } else if (mx >= STACK_X && mx < STACK_X + STACK_W && my >= STACK_Y && my < STACK_Y + STACK_H) {
            int row = (my - STACK_Y - 3) / 13;
            if (row >= 0 && row < 3) {
                Metal metal = Metal.values()[2 - row];
                lines.add(Component.translatable("gui.simplefoundry.metal", Component.translatable("simplefoundry.metal." + metal.id), menu.melt()[metal.ordinal()] / Metal.INGOT, menu.waiting()[metal.ordinal()] / Metal.INGOT));
            }
        } else if (mx >= 160 && mx < 168 && my >= 7 && my < 15) {
            lines.add(Component.translatable("simplefoundry.status." + menu.status()));
        }
        if (!lines.isEmpty()) g.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY);
    }

    private int used() {
        int sum = 0;
        for (int v : menu.melt()) sum += v;
        for (int v : menu.waiting()) sum += v;
        return sum / Metal.INGOT;
    }

    /** Index of the input place under {@code mx, my} (relative to the window), or -1. */
    private int placeAt(int mx, int my) {
        int col = (mx - FoundryMenu.SLOT_X) / 18, row = (my - FoundryMenu.SLOT_Y) / 18;
        if (mx < FoundryMenu.SLOT_X || my < FoundryMenu.SLOT_Y || col >= FoundryMenu.COLUMNS || row >= DeskBlockEntity.INPUT_SLOTS / FoundryMenu.COLUMNS) return -1;
        return (mx - FoundryMenu.SLOT_X) % 18 < 16 && (my - FoundryMenu.SLOT_Y) % 18 < 16 ? row * FoundryMenu.COLUMNS + col : -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x() - leftPos, my = event.y() - topPos;
        int level = levelAt(mx, my);
        if (level >= 0 && minecraft != null && minecraft.gameMode != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, FoundryMenu.LEVEL_BUTTON + level);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
