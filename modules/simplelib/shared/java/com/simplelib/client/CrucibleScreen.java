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
 * Crucible screen, drawn from flat colours (no texture per size).
 * N12 (owner feedback, images 3/4 in previews/refs-n12): two rounded boxes like image 3 - the crucible box on top in
 * the tier's colour, the light inventory box below it (the gap between them is the divider); flat faces, a dark rim,
 * a faint light/shade edge; slots 16x16 with rounded corners, sunk in (dark top line, light edge below/right), 2 px
 * apart; an attached barrel's 9 fields sit in their own copper box. The heat shows as a flame strip behind the slots
 * ({@link CrucibleFlames}) or, with {@link CrucibleMenu#HEAT_STYLE} SLOT, as an inset slot filling with flames
 * (image 4). Slot state shows in the slot's bottom two pixel rows ({@link #PROGRESS_STYLE}): orange progress while
 * cooking, blue (paused progress) when the heat is too low, red when the result has no room, green on finished
 * results; grey veil for items without a recipe; reserved places show the coming result faintly. A corner mark
 * repeats cooking/blocked/cold for colour-blind players.
 * The image is the larger of both layouts; the current panel is drawn centred inside it.
 */
public class CrucibleScreen extends AbstractContainerScreen<CrucibleMenu> {
    /** Where the state bar of a crucible slot goes (owner N12 proposals, preview crucible-n12-fortschritt.png). */
    public enum ProgressStyle {
        /** A: the slot's bottom two pixel rows, drawn over the item (like a durability bar). */
        OVER_ITEM,
        /** B: the same rows, behind the item. */
        UNDER_ITEM,
        /** C: the 2 px gap under the slot. */
        BELOW_SLOT
    }

    /** Switch for the slot state bar. */
    public static final ProgressStyle PROGRESS_STYLE = ProgressStyle.OVER_ITEM;

    /** Box colours: fill, light edge, shade edge, slot, slot top line, label. */
    record Palette(int fill, int light, int shade, int slot, int slotTop, int label) {}

    static final int RIM = 0xFF2B2D31;
    static final Palette INVENTORY = new Palette(0xFFE3E6E9, 0xFFF6F7F8, 0xFFC5CACE, 0xFFB4BABF, 0xFF979DA3, 0xFF404040);
    static final Palette BARREL = new Palette(0xFFB9774F, 0xFFD08F68, 0xFF955839, 0xFF94573A, 0xFF74412B, 0xFF404040);
    private static final int TRACK = 0xB0262626, COOK = 0xFFF0901E, RED = 0xFFD8402F, BLUE = 0xFF4A86DA, GREEN = 0xFF52B13C;
    private static final int GREY = 0x80505050;

    private final CrucibleTier tier;

    public CrucibleScreen(CrucibleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, CrucibleMenu.imageWidth(menu.tier()), CrucibleMenu.imageHeight(menu.tier()));
        this.tier = menu.tier();
        applyLabels(layout());
    }

    /** The crucible box colours of a tier (image 3: one colour per block). */
    static Palette palette(CrucibleTier tier) {
        return switch (tier) {
            case IRON -> new Palette(0xFF9A9DA2, 0xFFB5B8BC, 0xFF7E8186, 0xFF7B7E83, 0xFF64676C, 0xFF2E3034);
            case REINFORCED -> new Palette(0xFF6F9095, 0xFF8AAAAF, 0xFF587378, 0xFF55737A, 0xFF425C61, 0xFFF0F6F6);
            case NETHERITE -> new Palette(0xFF5F524C, 0xFF766860, 0xFF4A3F3A, 0xFF473C37, 0xFF352C28, 0xFFEFE4DA);
            case ENDERITE -> new Palette(0xFF8E6CB0, 0xFFA888C7, 0xFF735693, 0xFF70538E, 0xFF594073, 0xFFF7F0FF);
        };
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
        Palette top = palette(tier);
        int x0 = leftPos, y0 = topPos;
        int px = x0 + l.x(), py = y0 + l.y();
        box(g, px, py, l.width(), l.sectionHeight(), top);
        box(g, px, y0 + l.inventoryBoxTop(), l.width(), l.y() + l.height() - l.inventoryBoxTop(), INVENTORY);
        long now = Util.getMillis();
        if (CrucibleMenu.HEAT_STYLE == CrucibleMenu.HeatStyle.BAND) {
            CrucibleFlames.draw(g, px + 2, py + l.sectionHeight() - 1, l.width() - 4, l.sectionHeight(), menu.heat(), now);
        } else {
            int hx = x0 + l.heatX(), hy = y0 + l.heatY(tier);
            slot(g, hx, hy, top);
            CrucibleFlames.drawSlot(g, hx, hy, menu.heat(), now);
        }
        boolean attached = menu.barrelAttached();
        if (attached) {
            int bx = x0 + l.barrelLeft(), by = py + CrucibleMenu.GRID_TOP;
            box(g, bx - 2, by - 2, 59, 59, BARREL);
        }
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            int x = x0 + slot.x, y = y0 + slot.y;
            int index = menu.crucibleIndex(slot);
            Palette p = index >= 0 ? top : slot.container == menu.slots.get(menu.barrelStart()).container ? BARREL : INVENTORY;
            slot(g, x, y, p);
            if (index >= 0) {
                slotState(g, index, x, y);
                if (PROGRESS_STYLE == ProgressStyle.UNDER_ITEM) stateBar(g, index, x, y + 14);
                else if (PROGRESS_STYLE == ProgressStyle.BELOW_SLOT) stateBar(g, index, x, y + 16);
            }
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
                veil(g, x0 + l.barrelX(i), y0 + l.barrelY(i), BARREL);
            }
        }
        for (int i = 0; i < tier.slots(); i++) {
            if (menu.ghost(i).isEmpty() || menu.slots.get(i).hasItem()) continue;
            veil(g, x0 + l.slotX(tier, i), y0 + l.slotY(tier, i), top);
        }
    }

    /** Style A: the state bars go over the items (coordinates are relative to the screen's image here). */
    @Override
    protected void extractSlots(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractSlots(g, mouseX, mouseY);
        if (PROGRESS_STYLE != ProgressStyle.OVER_ITEM) return;
        g.nextStratum();
        for (Slot slot : menu.slots) {
            int index = menu.crucibleIndex(slot);
            if (slot.isActive() && index >= 0) stateBar(g, index, slot.x, slot.y + 14);
        }
    }

    /** Clicks in the reserved box but outside the current panel count as outside (drop the carried stack). */
    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        CrucibleMenu.Layout l = layout();
        return mx < xo + l.x() || my < yo + l.y() || mx >= xo + l.x() + l.width() || my >= yo + l.y() + l.height();
    }

    private static void veil(GuiGraphicsExtractor g, int x, int y, Palette p) {
        int color = 0xA8000000 | (p.slot() & 0xFFFFFF);
        g.fill(x + 1, y, x + 15, y + 16, color);
        g.fill(x, y + 1, x + 1, y + 15, color);
        g.fill(x + 15, y + 1, x + 16, y + 15, color);
    }

    /** Background part of a slot's state: grey veil without a recipe, corner marks for colour-blind players. */
    private void slotState(GuiGraphicsExtractor g, int slot, int x, int y) {
        switch (menu.slotState(slot)) {
            case CrucibleBlockEntity.COOKING -> corner(g, x, y, 0xFFFFC040);
            case CrucibleBlockEntity.BLOCKED -> {
                g.fill(x + 12, y + 1, x + 13, y + 5, 0xFFFFFFFF);
                g.fill(x + 13, y + 2, x + 14, y + 4, 0xFFFFFFFF);
                g.fill(x + 14, y + 1, x + 15, y + 5, 0xFFFFFFFF);
            }
            case CrucibleBlockEntity.COLD -> {
                g.fill(x + 13, y + 1, x + 14, y + 6, 0xFFFFFFFF);
                g.fill(x + 11, y + 3, x + 16, y + 4, 0xFFFFFFFF);
            }
            case CrucibleBlockEntity.NO_RECIPE -> g.fill(x, y + 1, x + 16, y + 15, GREY);
            default -> {}
        }
    }

    /** The two-row state bar of a crucible slot at {@code x, y} (14 px wide, inside the slot's rounded corners). */
    private void stateBar(GuiGraphicsExtractor g, int slot, int x, int y) {
        int state = menu.slotState(slot);
        int x1 = x + 1, x2 = x + 15;
        switch (state) {
            case CrucibleBlockEntity.COOKING, CrucibleBlockEntity.COLD -> {
                int w = menu.slotPercent(slot) * 14 / 100;
                if (state == CrucibleBlockEntity.COOKING) w = Math.max(1, w);
                g.fill(x1, y, x2, y + 2, TRACK);
                if (w > 0) g.fill(x1, y, x1 + w, y + 2, state == CrucibleBlockEntity.COOKING ? COOK : BLUE);
            }
            case CrucibleBlockEntity.BLOCKED -> g.fill(x1, y, x2, y + 2, RED);
            case CrucibleBlockEntity.RESULT -> g.fill(x1, y, x2, y + 2, GREEN);
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
        if (!overHeat(mouseX - leftPos, mouseY - topPos)) return;
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.simplelib.crucible.heat",
                Component.translatable("gui.simplelib.crucible.heat." + menu.heat().name().toLowerCase(java.util.Locale.ROOT))));
        if (menu.afterglow() > 0) lines.add(Component.translatable("gui.simplelib.crucible.afterglow",
                (menu.afterglow() + 19) / 20).withStyle(ChatFormatting.GOLD));
        if (menu.twoBelow()) lines.add(Component.translatable("gui.simplelib.crucible.two_below").withStyle(ChatFormatting.GRAY));
        g.setTooltipForNextFrame(font, lines, java.util.Optional.empty(), mouseX, mouseY);
    }

    /** Whether {@code mx, my} (relative to the image) is over the heat display (or where the flames would be). */
    private boolean overHeat(int mx, int my) {
        CrucibleMenu.Layout l = layout();
        if (CrucibleMenu.HEAT_STYLE == CrucibleMenu.HeatStyle.SLOT) {
            return mx >= l.heatX() && mx < l.heatX() + 16 && my >= l.heatY(tier) && my < l.heatY(tier) + 16;
        }
        int flames = Math.max(CrucibleFlames.targetPixels(HeatLevel.HIGH, l.sectionHeight()), 8);
        return mx >= l.x() + 2 && mx < l.x() + l.width() - 2 && my >= l.y() + l.sectionHeight() - flames && my < l.y() + l.sectionHeight();
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
        Palette top = palette(tier);
        g.text(font, title, titleLabelX, titleLabelY, top.label(), false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, INVENTORY.label(), false);
        if (tier.stackMultiplier() > 1) {
            CrucibleMenu.Layout l = layout();
            Component bonus = Component.translatable("gui.simplelib.crucible.stack_bonus", tier.stackMultiplier());
            g.text(font, bonus, l.x() + l.width() - 8 - font.width(bonus), titleLabelY, top.label(), false);
        }
    }

    /** Image 3 box: dark rim with rounded corners, flat fill, light top/left and shaded bottom/right edge. */
    static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, Palette p) {
        g.fill(x + 2, y, x + w - 2, y + 1, RIM);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, RIM);
        g.fill(x, y + 2, x + 1, y + h - 2, RIM);
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, RIM);
        g.fill(x + 1, y + 1, x + 2, y + 2, RIM);
        g.fill(x + w - 2, y + 1, x + w - 1, y + 2, RIM);
        g.fill(x + 1, y + h - 2, x + 2, y + h - 1, RIM);
        g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, RIM);
        g.fill(x + 2, y + 1, x + w - 2, y + h - 1, p.fill());
        g.fill(x + 1, y + 2, x + 2, y + h - 2, p.fill());
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, p.fill());
        g.fill(x + 2, y + 1, x + w - 2, y + 2, p.light());
        g.fill(x + 1, y + 2, x + 2, y + h - 2, p.light());
        g.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, p.shade());
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, p.shade());
    }

    /** Image 3/4 slot: 16x16 with rounded corners, a dark top line (sunk in) and a light edge below/right. */
    static void slot(GuiGraphicsExtractor g, int x, int y, Palette p) {
        g.fill(x + 1, y + 16, x + 16, y + 17, p.light());
        g.fill(x + 16, y + 1, x + 17, y + 16, p.light());
        g.fill(x + 1, y, x + 15, y + 16, p.slot());
        g.fill(x, y + 1, x + 1, y + 15, p.slot());
        g.fill(x + 15, y + 1, x + 16, y + 15, p.slot());
        g.fill(x + 1, y, x + 15, y + 1, p.slotTop());
    }
}
