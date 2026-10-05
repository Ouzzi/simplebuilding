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
 * Layout v2 (owner addition 11): centred content, heat column and fire strip as sunken fields with a
 * tooltip (heat, afterglow, source two below), the barrel's 9 fields always shown (placeholders with a
 * hint until a barrel is attached).
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
    private static final int BARREL_RIM = 0xFF8A4A2F, BARREL_FILL = 0xFFC9825F, EMPTY_FIELD = 0x40000000, ICON_VEIL = 0xB08B8B8B;
    private static final java.util.function.Supplier<ItemStack> BARREL_ICON = () -> new ItemStack(com.simplelib.registry.LibItems.COPPER_BARREL);

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
        int fireTop = y0 + CrucibleMenu.fireTop(tier);
        thermometer(g, x0 + CrucibleMenu.thermoLeft(tier), y0 + CrucibleMenu.GRID_TOP, fireTop + CrucibleMenu.FIRE_HEIGHT - y0 - CrucibleMenu.GRID_TOP);
        fire(g, x0 + CrucibleMenu.gridLeft(tier), fireTop, CrucibleMenu.gridsWidth(tier), CrucibleMenu.FIRE_HEIGHT, menu.heat());
        boolean attached = menu.barrelAttached();
        int bx = x0 + CrucibleMenu.barrelLeft(tier), by = y0 + CrucibleMenu.GRID_TOP;
        if (attached) {
            // Copper rim around the barrel's fields, like the flange on the block.
            g.fill(bx - 2, by - 2, bx + 56, by + 56, BARREL_RIM);
            g.fill(bx - 1, by - 1, bx + 55, by + 55, BARREL_FILL);
        }
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            int x = x0 + slot.x, y = y0 + slot.y;
            slotFrame(g, x, y);
            if (slot.index < tier.slots()) slotState(g, slot.index, x, y);
        }
        if (!attached) {
            // Placeholder fields: where an attached barrel's 9 fields go (hint in the tooltip).
            for (int i = 0; i < 9; i++) {
                int x = x0 + CrucibleMenu.barrelX(tier, i), y = y0 + CrucibleMenu.barrelY(i);
                slotFrame(g, x, y);
                g.fill(x, y, x + 16, y + 16, EMPTY_FIELD);
            }
        }
        g.nextStratum();
        if (attached) {
            for (int i = 0; i < 9; i++) {
                ItemStack ghost = menu.barrelGhost(i);
                if (ghost.isEmpty() || menu.slots.get(menu.barrelStart() + i).hasItem()) continue;
                g.fakeItem(ghost, x0 + CrucibleMenu.barrelX(tier, i), y0 + CrucibleMenu.barrelY(i));
            }
        } else {
            g.fakeItem(BARREL_ICON.get(), x0 + CrucibleMenu.barrelX(tier, 4), y0 + CrucibleMenu.barrelY(4));
        }
        for (int i = 0; i < tier.slots(); i++) {
            ItemStack ghost = menu.ghost(i);
            if (ghost.isEmpty() || menu.slots.get(i).hasItem()) continue;
            int x = x0 + CrucibleMenu.slotX(tier, i), y = y0 + CrucibleMenu.slotY(tier, i);
            g.fakeItem(ghost, x, y);
        }
        g.nextStratum();
        if (attached) {
            for (int i = 0; i < 9; i++) {
                if (menu.barrelGhost(i).isEmpty() || menu.slots.get(menu.barrelStart() + i).hasItem()) continue;
                int x = x0 + CrucibleMenu.barrelX(tier, i), y = y0 + CrucibleMenu.barrelY(i);
                g.fill(x, y, x + 16, y + 16, GHOST_VEIL);
            }
        } else {
            int x = x0 + CrucibleMenu.barrelX(tier, 4), y = y0 + CrucibleMenu.barrelY(4);
            g.fill(x, y, x + 16, y + 16, ICON_VEIL);
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

    /** Heat column, sunken like a slot: three segments (medium .. extreme) lit up to the current heat; afterglow blinks. */
    private void thermometer(GuiGraphicsExtractor g, int x, int y, int height) {
        int w = CrucibleMenu.THERMO_WIDTH;
        inset(g, x, y, w, height);
        g.fill(x + 1, y + 1, x + w - 1, y + height - 1, 0xFF222222);
        HeatLevel heat = menu.heat();
        int levels = HeatLevel.values().length - 1;
        int segment = (height - 3 - (levels - 1)) / levels;
        boolean blink = menu.afterglow() > 0 && (Util.getMillis() / 300) % 2 == 0;
        for (int l = 1; l <= levels; l++) {
            int bottom = y + height - 2 - (l - 1) * (segment + 1);
            boolean lit = heat.ordinal() >= l;
            int color = switch (l) {
                case 1 -> 0xFFE07B22;
                case 2 -> 0xFFF2B233;
                default -> 0xFF4FC3E8;
            };
            if (!lit) color = 0xFF3A3A3A;
            else if (blink) color = (color & 0x00FFFFFF) | 0x90000000;
            g.fill(x + 2, bottom - segment, x + w - 2, bottom, color);
        }
    }

    /** Sunken frame (dark top/left, light bottom/right) as Vanilla draws slots and progress fields. */
    static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, SLOT_FILL);
        g.fill(x, y, x + w - 1, y + 1, SLOT_DARK);
        g.fill(x, y, x + 1, y + h - 1, SLOT_DARK);
        g.fill(x + 1, y + h - 1, x + w, y + h, LIGHT);
        g.fill(x + w - 1, y + 1, x + w, y + h, LIGHT);
    }

    /** Animated fire strip: flame height and colour follow the heat (owner 51). */
    private static void fire(GuiGraphicsExtractor g, int x, int y, int width, int height, HeatLevel heat) {
        inset(g, x, y, width, height);
        x += 1;
        y += 1;
        width -= 2;
        height -= 2;
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
        int mx = mouseX - leftPos, my = mouseY - topPos;
        int fireTop = CrucibleMenu.fireTop(tier), fireBottom = fireTop + CrucibleMenu.FIRE_HEIGHT;
        int thermo = CrucibleMenu.thermoLeft(tier), grid = CrucibleMenu.gridLeft(tier);
        boolean overHeat = my >= CrucibleMenu.GRID_TOP && my < fireBottom && mx >= thermo && mx < thermo + CrucibleMenu.THERMO_WIDTH
                || my >= fireTop && my < fireBottom && mx >= grid && mx < grid + CrucibleMenu.gridsWidth(tier);
        if (overHeat) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.simplelib.crucible.heat",
                    Component.translatable("gui.simplelib.crucible.heat." + menu.heat().name().toLowerCase(java.util.Locale.ROOT))));
            if (menu.afterglow() > 0) lines.add(Component.translatable("gui.simplelib.crucible.afterglow",
                    (menu.afterglow() + 19) / 20).withStyle(ChatFormatting.GOLD));
            if (menu.twoBelow()) lines.add(Component.translatable("gui.simplelib.crucible.two_below").withStyle(ChatFormatting.GRAY));
            g.setTooltipForNextFrame(font, lines, java.util.Optional.empty(), mouseX, mouseY);
            return;
        }
        int bx = CrucibleMenu.barrelLeft(tier);
        if (!menu.barrelAttached() && mx >= bx && mx < bx + 54 && my >= CrucibleMenu.GRID_TOP && my < CrucibleMenu.GRID_TOP + 54) {
            g.setTooltipForNextFrame(font, List.of(Component.translatable("gui.simplelib.crucible.barrel"),
                    Component.translatable("gui.simplelib.crucible.barrel_hint").withStyle(ChatFormatting.GRAY)), java.util.Optional.empty(), mouseX, mouseY);
            return;
        }
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
