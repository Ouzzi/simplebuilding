package com.simplelib.client;

import com.simplelib.crucible.CrucibleBlockEntity;
import com.simplelib.crucible.CrucibleMenu;
import com.simplelib.crucible.CrucibleTier;
import com.simplelib.crucible.HeatLevel;
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
 * Slot backgrounds show each slot's state (owner 19/20): orange fill rising with the progress while
 * cooking, red when the result has no room, blue when the heat is too low, a green rim on finished
 * results, grey for items without a recipe; reserved places show the coming result faintly. A
 * corner mark repeats every state for colour-blind players. Under the grids an animated "cozy" fire
 * shows the heat (owner 51): low orange flames for medium, tall flames for high, blue for extreme.
 */
public class CrucibleScreen extends AbstractContainerScreen<CrucibleMenu> {
    private static final int BACKGROUND = 0xFFC6C6C6, OUTLINE = 0xFF000000, LIGHT = 0xFFFFFFFF, SHADE = 0xFF555555;
    private static final int SLOT_DARK = 0xFF373737, SLOT_FILL = 0xFF8B8B8B;
    private static final int COOK_BASE = 0xFFB0A090, COOK_FILL = 0xC0E8892A;
    private static final int RED = 0xC0C83C32, BLUE = 0xC0467FD2, GREEN = 0xFF4FA13B, GREY = 0x80505050;
    private static final int GHOST_VEIL = 0xA88B8B8B;
    private static final int LABEL = 0xFF404040;

    private final CrucibleTier tier;

    public CrucibleScreen(CrucibleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, CrucibleMenu.imageWidth(menu.tier()), CrucibleMenu.imageHeight(menu.tier()));
        this.tier = menu.tier();
        this.inventoryLabelX = CrucibleMenu.inventoryLeft(tier);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x0 = leftPos, y0 = topPos;
        panel(g, x0, y0, imageWidth, imageHeight);
        thermometer(g, x0 + 8, y0 + CrucibleMenu.GRID_TOP, tier.rows() * 18);
        int fireTop = y0 + CrucibleMenu.GRID_TOP + tier.rows() * 18 + 2;
        fire(g, x0 + CrucibleMenu.GRID_LEFT, fireTop, CrucibleMenu.gridsWidth(tier), CrucibleMenu.FIRE_HEIGHT, menu.heat());
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            int x = x0 + slot.x, y = y0 + slot.y;
            slotFrame(g, x, y);
            if (slot.index < tier.slots()) slotState(g, slot.index, x, y);
        }
        if (menu.barrelAttached()) {
            int bx = x0 + CrucibleMenu.barrelX(tier, 0) - 3, by = y0 + CrucibleMenu.barrelY(0) - 3;
            g.fill(bx, by, bx + 60, by + 60, 0xFFB4684D);
            g.fill(bx + 1, by + 1, bx + 59, by + 59, 0xFFD88A6A);
            for (int i = 0; i < 9; i++) slotFrame(g, x0 + CrucibleMenu.barrelX(tier, i), y0 + CrucibleMenu.barrelY(i));
        }
        g.nextStratum();
        if (menu.barrelAttached()) {
            for (int i = 0; i < 9; i++) {
                ItemStack ghost = menu.barrelGhost(i);
                if (ghost.isEmpty() || menu.slots.get(menu.barrelStart() + i).hasItem()) continue;
                g.fakeItem(ghost, x0 + CrucibleMenu.barrelX(tier, i), y0 + CrucibleMenu.barrelY(i));
            }
        }
        for (int i = 0; i < tier.slots(); i++) {
            ItemStack ghost = menu.ghost(i);
            if (ghost.isEmpty() || menu.slots.get(i).hasItem()) continue;
            int x = x0 + CrucibleMenu.slotX(tier, i), y = y0 + CrucibleMenu.slotY(tier, i);
            g.fakeItem(ghost, x, y);
        }
        g.nextStratum();
        if (menu.barrelAttached()) {
            for (int i = 0; i < 9; i++) {
                if (menu.barrelGhost(i).isEmpty() || menu.slots.get(menu.barrelStart() + i).hasItem()) continue;
                int x = x0 + CrucibleMenu.barrelX(tier, i), y = y0 + CrucibleMenu.barrelY(i);
                g.fill(x, y, x + 16, y + 16, GHOST_VEIL);
            }
        }
        for (int i = 0; i < tier.slots(); i++) {
            if (menu.ghost(i).isEmpty() || menu.slots.get(i).hasItem()) continue;
            int x = x0 + CrucibleMenu.slotX(tier, i), y = y0 + CrucibleMenu.slotY(tier, i);
            g.fill(x, y, x + 16, y + 16, GHOST_VEIL);
        }
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

    /** Heat column: five segments (cold .. extreme), lit up to the current heat; afterglow blinks. */
    private void thermometer(GuiGraphicsExtractor g, int x, int y, int height) {
        g.fill(x, y, x + CrucibleMenu.THERMO_WIDTH, y + height, SLOT_DARK);
        g.fill(x + 1, y + 1, x + CrucibleMenu.THERMO_WIDTH - 1, y + height - 1, 0xFF222222);
        HeatLevel heat = menu.heat();
        int levels = HeatLevel.values().length - 1;
        int segment = (height - 2) / levels;
        boolean blink = menu.afterglow() > 0 && (Util.getMillis() / 300) % 2 == 0;
        for (int l = 1; l <= levels; l++) {
            if (heat.ordinal() < l) break;
            int color = switch (l) {
                case 1 -> 0xFFE07B22;
                case 2 -> 0xFFF2B233;
                default -> 0xFF4FC3E8;
            };
            if (blink) color = (color & 0x00FFFFFF) | 0x90000000;
            int bottom = y + height - 1 - (l - 1) * segment;
            g.fill(x + 2, bottom - segment + 1, x + CrucibleMenu.THERMO_WIDTH - 2, bottom, color);
        }
    }

    /** Animated fire strip: flame height and colour follow the heat (owner 51). */
    private static void fire(GuiGraphicsExtractor g, int x, int y, int width, int height, HeatLevel heat) {
        g.fill(x, y, x + width, y + height, 0xFF2A2420);
        g.fill(x, y + height - 2, x + width, y + height, 0xFF4A3A30);
        if (heat == HeatLevel.NONE) return;
        long t = Util.getMillis() / 90;
        int max = switch (heat) {
            case MEDIUM -> height / 2;
            case HIGH -> height - 2;
            default -> height - 1;
        };
        int outer = heat == HeatLevel.EXTREME ? 0xFF2E7FD6 : 0xFFD8521E;
        int inner = heat == HeatLevel.EXTREME ? 0xFF8FE3FF : 0xFFF6C24A;
        for (int col = 0; col < width; col += 2) {
            int seed = (int) ((col * 7919L + t * 31L + (col / 2) * (t % 7)) % 97);
            int h = Math.max(1, max - (seed % (max / 2 + 2)));
            g.fill(x + col, y + height - 2 - h, x + col + 2, y + height - 2, outer);
            if (h > 2) g.fill(x + col, y + height - 2 - h / 2, x + col + 2, y + height - 2, inner);
        }
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        if (hoveredSlot != null && hoveredSlot.index < tier.slots()) {
            Component state = stateLine(hoveredSlot.index);
            if (state != null) lines.add(state);
        }
        return lines;
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (hoveredSlot != null && hoveredSlot.index < tier.slots() && !hoveredSlot.hasItem()) {
            ItemStack ghost = menu.ghost(hoveredSlot.index);
            if (!ghost.isEmpty()) {
                g.setTooltipForNextFrame(font, List.of(ghost.getHoverName(),
                        Component.translatable("gui.simplelib.crucible.reserved").withStyle(ChatFormatting.GRAY)), java.util.Optional.empty(), mouseX, mouseY);
            }
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
            Component bonus = Component.translatable("gui.simplelib.crucible.stack_bonus", tier.stackMultiplier());
            g.text(font, bonus, imageWidth - 8 - font.width(bonus), titleLabelY, LABEL, false);
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
