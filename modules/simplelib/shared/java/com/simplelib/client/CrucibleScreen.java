package com.simplelib.client;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
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
 * N12/N12b (owner feedback, images 3/4 in previews/refs-n12, measured pixel by pixel - see
 * docs/ai/PLAN-CRUCIBLE-N12B-2026-10-06.md): two boxes like image 3 - the crucible box on top in the tier's colour,
 * the light inventory box below it (the gap between them is the divider). Each box has image 4's thick frame: dark
 * outline, darker bevel line, 2 px dark band, a light inner line, corners rounded by 2 px; at the bottom two more
 * pixels of shadow (5 px frame at the top and sides, 7 px at the bottom). Slots 16x16 like image 3 (rounded corners,
 * 2 px apart), sunk in like image 4 (dark top and left line, light edge below/right); an attached barrel's fields (as
 * many as the crucible's, N12c) sit in their own copper box beside the crucible box, framed exactly like the others. The heat shows as a flame strip behind the slots ({@link CrucibleFlames}).
 * Each crucible slot shows its progress like the furnace (image 4, {@link #PROGRESS_STYLE} FILL): the slot fills up
 * from below with flames while cooking - blue and standing still when the heat is too low, red and full when the result
 * has no room; finished results get a green frame, items without a recipe a grey veil. GAP_BAR puts the same progress
 * into a vertical bar in the gap right of the slot instead. Reserved places show the coming result faintly. A corner
 * mark repeats cooking/blocked/cold for colour-blind players.
 * The image is the larger of both layouts; the current panel is drawn centred inside it.
 * The drawing blocks (box, slot, palette, progress fill, flames) live in {@code com.simplelib.api.client.ui} and are
 * shared with simpleinterfaces and other mod screens.
 */
public class CrucibleScreen extends AbstractContainerScreen<CrucibleMenu> {
    /** How a crucible slot shows its progress (owner N12b, preview crucible-n12b-fortschritt.png). */
    public enum ProgressStyle {
        /** Version 1: the slot fills up from below like the furnace's fuel slot in image 4 (behind the item). */
        FILL,
        /** Version 2: a vertical bar in the 2 px gap right of the slot. */
        GAP_BAR
    }

    /** Switch for the slot progress. */
    public static final ProgressStyle PROGRESS_STYLE = ProgressStyle.FILL;

    private static final UiPalette INVENTORY = UiPalette.INVENTORY, BARREL = UiPalette.BARREL;
    private static final int FRAME = UiBoxes.FRAME, FRAME_BOTTOM = UiBoxes.FRAME_BOTTOM;
    private static final int COOK = 0xFFF0901E, RED = 0xFFD8402F, BLUE = 0xFF4A86DA, GREEN = 0xFF52B13C;
    private static final int GREEN_LIGHT = 0xFF9BE07F, GREY = 0x80505050;

    private final CrucibleTier tier;

    public CrucibleScreen(CrucibleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, CrucibleMenu.imageWidth(menu.tier()), CrucibleMenu.imageHeight(menu.tier()));
        this.tier = menu.tier();
        applyLabels(layout());
    }

    /** The crucible box colours of a tier (image 3: one colour per block). */
    static UiPalette palette(CrucibleTier tier) {
        return switch (tier) {
            case IRON -> UiPalette.IRON;
            case REINFORCED -> UiPalette.REINFORCED;
            case NETHERITE -> UiPalette.NETHERITE;
            case ENDERITE -> UiPalette.ENDERITE;
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
        UiPalette top = palette(tier);
        int x0 = leftPos, y0 = topPos;
        int px = x0 + l.x(), py = y0 + l.y();
        boolean attached = menu.barrelAttached();
        UiBoxes.box(g, px, py, l.crucibleWidth(), l.sectionHeight(), top);
        // N12c: the barrel gets its own box with the same frame as the others (owner: no thicker edges).
        if (attached) UiBoxes.box(g, x0 + l.barrelBox(), y0 + l.barrelBoxTop(), l.barrelBoxWidth(), l.barrelBoxHeight(), BARREL);
        UiBoxes.box(g, x0 + l.inventoryBoxLeft(), y0 + l.inventoryBoxTop(), l.inventoryBoxWidth(), l.inventoryBoxHeight(), INVENTORY);
        long now = Util.getMillis();
        CrucibleFlames.draw(g, px + FRAME, py + l.sectionHeight() - FRAME_BOTTOM, l.crucibleWidth() - 2 * FRAME, l.sectionHeight(),
                menu.heat(), menu.afterglow() > 0, now);
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            int x = x0 + slot.x, y = y0 + slot.y;
            int index = menu.crucibleIndex(slot);
            UiPalette p = index >= 0 ? top : slot.container == menu.slots.get(menu.barrelStart()).container ? BARREL : INVENTORY;
            UiBoxes.slot(g, x, y, p);
            if (index >= 0) slotState(g, index, x, y, now);
        }
        g.nextStratum();
        if (attached) {
            for (int i = 0; i < tier.slots(); i++) {
                ItemStack ghost = menu.barrelGhost(i);
                if (ghost.isEmpty() || menu.slots.get(menu.barrelStart() + i).hasItem()) continue;
                g.fakeItem(ghost, x0 + l.barrelX(tier, i), y0 + l.barrelY(tier, i));
            }
        }
        for (int i = 0; i < tier.slots(); i++) {
            ItemStack ghost = menu.ghost(i);
            if (ghost.isEmpty() || menu.slots.get(i).hasItem()) continue;
            g.fakeItem(ghost, x0 + l.slotX(tier, i), y0 + l.slotY(tier, i));
        }
        g.nextStratum();
        if (attached) {
            for (int i = 0; i < tier.slots(); i++) {
                if (menu.barrelGhost(i).isEmpty() || menu.slots.get(menu.barrelStart() + i).hasItem()) continue;
                UiBoxes.veil(g, x0 + l.barrelX(tier, i), y0 + l.barrelY(tier, i), BARREL);
            }
        }
        for (int i = 0; i < tier.slots(); i++) {
            if (menu.ghost(i).isEmpty() || menu.slots.get(i).hasItem()) continue;
            UiBoxes.veil(g, x0 + l.slotX(tier, i), y0 + l.slotY(tier, i), top);
        }
    }

    /** Clicks in the reserved box but outside the current panel count as outside (drop the carried stack). */
    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        CrucibleMenu.Layout l = layout();
        return mx < xo + l.x() || my < yo + l.y() || mx >= xo + l.x() + l.width() || my >= yo + l.y() + l.height();
    }

    /** A crucible slot's state behind its item: furnace-like fill or gap bar, green frame, grey veil, corner marks. */
    private void slotState(GuiGraphicsExtractor g, int slot, int x, int y, long now) {
        int state = menu.slotState(slot);
        int percent = state == CrucibleBlockEntity.BLOCKED ? 100 : menu.slotPercent(slot);
        if (state == CrucibleBlockEntity.COOKING || state == CrucibleBlockEntity.COLD || state == CrucibleBlockEntity.BLOCKED) {
            int level = Math.max(state == CrucibleBlockEntity.COOKING ? 1 : 0, percent * 16 / 100);
            if (PROGRESS_STYLE == ProgressStyle.FILL) {
                UiBoxes.ProgressColors colors = state == CrucibleBlockEntity.COOKING ? UiBoxes.ProgressColors.FIRE
                        : state == CrucibleBlockEntity.COLD ? UiBoxes.ProgressColors.COLD : UiBoxes.ProgressColors.BLOCKED;
                UiBoxes.progressFill(g, x, y, level, colors, state == CrucibleBlockEntity.COOKING ? now : 0);
            } else {
                UiBoxes.progressBar(g, x, y, level, state == CrucibleBlockEntity.COOKING ? COOK : state == CrucibleBlockEntity.COLD ? BLUE : RED);
            }
        }
        switch (state) {
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
            case CrucibleBlockEntity.RESULT -> {
                if (PROGRESS_STYLE == ProgressStyle.FILL) {
                    g.fill(x + 1, y, x + 15, y + 1, GREEN);
                    g.fill(x, y + 1, x + 1, y + 15, GREEN);
                    g.fill(x + 1, y + 16, x + 16, y + 17, GREEN_LIGHT);
                    g.fill(x + 16, y + 1, x + 17, y + 16, GREEN_LIGHT);
                } else {
                    UiBoxes.progressBar(g, x, y, 16, GREEN);
                }
            }
            case CrucibleBlockEntity.NO_RECIPE -> g.fill(x, y + 1, x + 16, y + 15, GREY);
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
        int flames = Math.max(CrucibleFlames.targetPixels(HeatLevel.HIGH, false, l.sectionHeight()), 8);
        int bottom = l.y() + l.sectionHeight() - FRAME_BOTTOM;
        return mx >= l.x() + FRAME && mx < l.x() + l.crucibleWidth() - FRAME && my >= bottom - flames && my < bottom;
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
        UiPalette top = palette(tier);
        g.text(font, title, titleLabelX, titleLabelY, top.label(), false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, INVENTORY.label(), false);
    }
}
