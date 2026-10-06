package com.simplelib.client;

import com.simplelib.crucible.CrucibleBlockEntity;
import com.simplelib.crucible.CrucibleMenu;
import com.simplelib.crucible.CrucibleTier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Crucible screen, drawn from flat colors like SimpleBuilding's chest screens (no texture per size).
 * Layout v3 (owner feedback 2026-10-06): one contiguous chest-like grid, compact and centred; the 9
 * fields of an attached barrel appear beside it (with a gap) only while a barrel is attached. No heat
 * column: pixel flames ({@link CrucibleFlames}) rise behind the slots over the lower part of the
 * crucible background - lower for medium heat, about a third for high, blue for extreme; hovering
 * them names the heat. Slot backgrounds show each slot's state (owner 19/20): orange fill rising with
 * the progress while cooking, red when the result has no room, blue when the heat is too low, a green
 * rim on finished results, grey for items without a recipe; reserved places show the coming result
 * faintly. A corner mark repeats every state for colour-blind players.
 * The image is the larger of both layouts; the current panel is drawn centred inside it.
 */
public class CrucibleScreen extends AbstractContainerScreen<CrucibleMenu> {
    private static final int BACKGROUND = 0xFFC6C6C6, OUTLINE = 0xFF000000, LIGHT = 0xFFFFFFFF, SHADE = 0xFF555555;
    private static final int SLOT_DARK = 0xFF373737, SLOT_FILL = 0xFF8B8B8B;
    private static final int COOK_BASE = 0xFFB0A090, COOK_FILL = 0xC0E8892A;
    private static final int RED = 0xC0C83C32, BLUE = 0xC0467FD2, GREEN = 0xFF4FA13B, GREY = 0x80505050;
    private static final int GHOST_VEIL = 0xA88B8B8B;
    private static final int LABEL = 0xFF404040;
    private static final int BARREL_RIM = 0xFF8A4A2F, BARREL_FILL = 0xFFC9825F;

    private final CrucibleTier tier;

    public CrucibleScreen(CrucibleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, CrucibleMenu.imageWidth(menu.tier()), CrucibleMenu.imageHeight(menu.tier()));
        this.tier = menu.tier();
        applyLabels(layout());
    }

    private CrucibleMenu.Layout layout() {
        return CrucibleMenu.layout(tier, menu.barrelAttached());
    }

    private void applyLabels(CrucibleMenu.Layout l) {
        titleLabelX = l.x() + 8;
        titleLabelY = l.y() + 6;
        inventoryLabelX = l.inventoryLeft();
        inventoryLabelY = l.inventoryTop() - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        CrucibleMenu.Layout l = layout();
        applyLabels(l);
        int x0 = leftPos, y0 = topPos;
        int px = x0 + l.x(), py = y0 + l.y();
        panel(g, px, py, l.width(), l.height());
        CrucibleFlames.draw(g, px + 3, py + l.sectionHeight(), l.width() - 6, l.sectionHeight(), menu.heat(), Util.getMillis());
        boolean attached = menu.barrelAttached();
        if (attached) {
            // Copper rim around the barrel's fields, like the flange on the block.
            int bx = x0 + l.barrelLeft(), by = py + CrucibleMenu.GRID_TOP;
            g.fill(bx - 2, by - 2, bx + 56, by + 56, BARREL_RIM);
            g.fill(bx - 1, by - 1, bx + 55, by + 55, BARREL_FILL);
        }
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            int x = x0 + slot.x, y = y0 + slot.y;
            slotFrame(g, x, y);
            int index = menu.crucibleIndex(slot);
            if (index >= 0) slotState(g, index, x, y);
        }
        g.nextStratum();
        if (attached) {
            for (int i = 0; i < 9; i++) {
                ItemStack ghost = menu.barrelGhost(i);
                if (ghost.isEmpty() || menu.slots.get(menu.barrelStart() + i).hasItem()) continue;
                g.fakeItem(ghost, x0 + l.barrelX(i), y0 + l.barrelY(i));
            }
        }
        for (int i = 0; i < tier.slots(); i++) {
            ItemStack ghost = menu.ghost(i);
            if (ghost.isEmpty() || menu.slots.get(i).hasItem()) continue;
            g.fakeItem(ghost, x0 + l.slotX(tier, i), y0 + l.slotY(tier, i));
        }
        g.nextStratum();
        if (attached) {
            for (int i = 0; i < 9; i++) {
                if (menu.barrelGhost(i).isEmpty() || menu.slots.get(menu.barrelStart() + i).hasItem()) continue;
                int x = x0 + l.barrelX(i), y = y0 + l.barrelY(i);
                g.fill(x, y, x + 16, y + 16, GHOST_VEIL);
            }
        }
        for (int i = 0; i < tier.slots(); i++) {
            if (menu.ghost(i).isEmpty() || menu.slots.get(i).hasItem()) continue;
            int x = x0 + l.slotX(tier, i), y = y0 + l.slotY(tier, i);
            g.fill(x, y, x + 16, y + 16, GHOST_VEIL);
        }
    }

    /** Clicks in the reserved box but outside the current panel count as outside (drop the carried stack). */
    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        CrucibleMenu.Layout l = layout();
        return mx < xo + l.x() || my < yo + l.y() || mx >= xo + l.x() + l.width() || my >= yo + l.y() + l.height();
    }

    private void slotState(GuiGraphicsExtractor g, int slot, int x, int y) {
        int state = menu.slotState(slot);
        switch (state) {
            case CrucibleBlockEntity.COOKING -> {
                g.fill(x, y, x + 16, y + 16, COOK_BASE);
                int h = Math.max(1, menu.slotPercent(slot) * 16 / 100);
                g.fill(x, y + 16 - h, x + 16, y + 16, COOK_FILL);
                corner(g, x, y, 0xFFFFC040);
            }
            case CrucibleBlockEntity.BLOCKED -> {
                g.fill(x, y, x + 16, y + 16, RED);
                g.fill(x + 12, y, x + 13, y + 4, 0xFFFFFFFF);
                g.fill(x + 13, y + 1, x + 14, y + 3, 0xFFFFFFFF);
                g.fill(x + 14, y, x + 15, y + 4, 0xFFFFFFFF);
            }
            case CrucibleBlockEntity.COLD -> {
                g.fill(x, y, x + 16, y + 16, BLUE);
                int h = menu.slotPercent(slot) * 16 / 100;
                if (h > 0) g.fill(x, y + 16 - h, x + 16, y + 16, 0x60E8892A);
                g.fill(x + 13, y, x + 14, y + 5, 0xFFFFFFFF);
                g.fill(x + 11, y + 2, x + 16, y + 3, 0xFFFFFFFF);
            }
            case CrucibleBlockEntity.RESULT -> {
                g.fill(x, y, x + 16, y + 1, GREEN);
                g.fill(x, y + 15, x + 16, y + 16, GREEN);
                g.fill(x, y, x + 1, y + 16, GREEN);
                g.fill(x + 15, y, x + 16, y + 16, GREEN);
            }
            case CrucibleBlockEntity.NO_RECIPE -> g.fill(x, y, x + 16, y + 16, GREY);
            default -> {}
        }
    }

    private static void corner(GuiGraphicsExtractor g, int x, int y, int color) {
        g.fill(x + 13, y + 1, x + 15, y + 2, color);
        g.fill(x + 12, y + 2, x + 15, y + 4, color);
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        int index = hoveredSlot == null ? -1 : menu.crucibleIndex(hoveredSlot);
        if (index >= 0) {
            Component state = stateLine(index);
            if (state != null) lines.add(state);
        }
        return lines;
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int index = hoveredSlot == null ? -1 : menu.crucibleIndex(hoveredSlot);
        if (index >= 0 && !hoveredSlot.hasItem()) {
            ItemStack ghost = menu.ghost(index);
            if (!ghost.isEmpty()) {
                g.setTooltipForNextFrame(font, List.of(ghost.getHoverName(),
                        Component.translatable("gui.simplelib.crucible.reserved").withStyle(ChatFormatting.GRAY)), java.util.Optional.empty(), mouseX, mouseY);
            }
            return;
        }
        if (hoveredSlot != null) return;
        // Over the flames (or where they would be): the heat, afterglow, source two below.
        CrucibleMenu.Layout l = layout();
        int mx = mouseX - leftPos, my = mouseY - topPos;
        int flames = Math.max(CrucibleFlames.targetCells(com.simplelib.crucible.HeatLevel.HIGH, l.sectionHeight()) * CrucibleFlames.CELL, 8);
        if (mx >= l.x() + 3 && mx < l.x() + l.width() - 3 && my >= l.y() + l.sectionHeight() - flames && my < l.y() + l.sectionHeight()) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.simplelib.crucible.heat",
                    Component.translatable("gui.simplelib.crucible.heat." + menu.heat().name().toLowerCase(java.util.Locale.ROOT))));
            if (menu.afterglow() > 0) lines.add(Component.translatable("gui.simplelib.crucible.afterglow",
                    (menu.afterglow() + 19) / 20).withStyle(ChatFormatting.GOLD));
            if (menu.twoBelow()) lines.add(Component.translatable("gui.simplelib.crucible.two_below").withStyle(ChatFormatting.GRAY));
            g.setTooltipForNextFrame(font, lines, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    private Component stateLine(int slot) {
        return switch (menu.slotState(slot)) {
            case CrucibleBlockEntity.COOKING -> Component.translatable("gui.simplelib.crucible.cooking", menu.slotPercent(slot)).withStyle(ChatFormatting.GOLD);
            case CrucibleBlockEntity.BLOCKED -> Component.translatable("gui.simplelib.crucible.blocked").withStyle(ChatFormatting.RED);
            case CrucibleBlockEntity.COLD -> Component.translatable("gui.simplelib.crucible.cold").withStyle(ChatFormatting.AQUA);
            case CrucibleBlockEntity.RESULT -> Component.translatable("gui.simplelib.crucible.result").withStyle(ChatFormatting.GREEN);
            case CrucibleBlockEntity.NO_RECIPE -> Component.translatable("gui.simplelib.crucible.no_recipe").withStyle(ChatFormatting.GRAY);
            default -> null;
        };
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        if (tier.stackMultiplier() > 1) {
            CrucibleMenu.Layout l = layout();
            Component bonus = Component.translatable("gui.simplelib.crucible.stack_bonus", tier.stackMultiplier());
            g.text(font, bonus, l.x() + l.width() - 8 - font.width(bonus), titleLabelY, LABEL, false);
        }
    }

    static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 2, y, x + w - 2, y + 1, OUTLINE);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, OUTLINE);
        g.fill(x, y + 2, x + 1, y + h - 2, OUTLINE);
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, OUTLINE);
        g.fill(x + 1, y + 1, x + 2, y + 2, OUTLINE);
        g.fill(x + w - 2, y + 1, x + w - 1, y + 2, OUTLINE);
        g.fill(x + 1, y + h - 2, x + 2, y + h - 1, OUTLINE);
        g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, OUTLINE);
        g.fill(x + 1, y + 2, x + w - 1, y + h - 2, BACKGROUND);
        g.fill(x + 2, y + 1, x + w - 2, y + h - 1, BACKGROUND);
        g.fill(x + 2, y + 1, x + w - 3, y + 3, LIGHT);
        g.fill(x + 1, y + 2, x + 3, y + h - 3, LIGHT);
        g.fill(x + 3, y + h - 3, x + w - 2, y + h - 1, SHADE);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 2, SHADE);
    }

    static void slotFrame(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_FILL);
        g.fill(x - 1, y - 1, x + 16, y, SLOT_DARK);
        g.fill(x - 1, y - 1, x, y + 16, SLOT_DARK);
        g.fill(x, y + 16, x + 17, y + 17, LIGHT);
        g.fill(x + 16, y, x + 17, y + 17, LIGHT);
    }
}
