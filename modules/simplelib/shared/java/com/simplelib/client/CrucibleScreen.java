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

    /** Box colours: fill, light inner line, shade, slot, slot top line, label. */
    record Palette(int fill, int light, int shade, int slot, int slotTop, int label) {}

    static final int RIM = 0xFF1E1F23;
    static final Palette INVENTORY = new Palette(0xFFE3E6E9, 0xFFF8F9FA, 0xFFC5CACE, 0xFFB4BABF, 0xFF979DA3, 0xFF404040);
    static final Palette BARREL = new Palette(0xFFB9774F, 0xFFD08F68, 0xFF955839, 0xFF94573A, 0xFF74412B, 0xFF404040);
    /** Furnace-like fills (image 4): body, tongues, bright base line. */
    private static final int[] FILL_COOK = {0xFFFFAE1E, 0xFFF26B12, 0xFFFFE34A};
    private static final int[] FILL_COLD = {0xFF4A86DA, 0xFF2C5DB0, 0xFFBFE0FF};
    private static final int[] FILL_BLOCKED = {0xFFC9503E, 0xFF962A1E, 0xFFFF9A80};
    private static final int TRACK = 0xFF3A3A3A, COOK = 0xFFF0901E, RED = 0xFFD8402F, BLUE = 0xFF4A86DA, GREEN = 0xFF52B13C;
    private static final int GREEN_LIGHT = 0xFF9BE07F, GREY = 0x80505050;

    private final CrucibleTier tier;

    public CrucibleScreen(CrucibleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, CrucibleMenu.imageWidth(menu.tier()), CrucibleMenu.imageHeight(menu.tier()));
        this.tier = menu.tier();
        applyLabels(layout());
    }

    /** The crucible box colours of a tier (image 3: one colour per block). */
    static Palette palette(CrucibleTier tier) {
        return switch (tier) {
            case IRON -> new Palette(0xFF9A9DA2, 0xFFC4C7CB, 0xFF7E8186, 0xFF7B7E83, 0xFF64676C, 0xFF2E3034);
            case REINFORCED -> new Palette(0xFF6F9095, 0xFF9DBCC1, 0xFF587378, 0xFF55737A, 0xFF425C61, 0xFFF0F6F6);
            case NETHERITE -> new Palette(0xFF5F524C, 0xFF867870, 0xFF4A3F3A, 0xFF473C37, 0xFF352C28, 0xFFEFE4DA);
            case ENDERITE -> new Palette(0xFF8E6CB0, 0xFFB99AD6, 0xFF735693, 0xFF70538E, 0xFF594073, 0xFFF7F0FF);
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
        boolean attached = menu.barrelAttached();
        box(g, px, py, l.crucibleWidth(), l.sectionHeight(), top);
        // N12c: the barrel gets its own box with the same frame as the others (owner: no thicker edges).
        if (attached) box(g, x0 + l.barrelBox(), py, CrucibleMenu.barrelBoxWidth(tier), l.sectionHeight(), BARREL);
        box(g, px, y0 + l.inventoryBoxTop(), l.width(), l.y() + l.height() - l.inventoryBoxTop(), INVENTORY);
        long now = Util.getMillis();
        CrucibleFlames.draw(g, px + FRAME, py + l.sectionHeight() - FRAME_BOTTOM, l.crucibleWidth() - 2 * FRAME, l.sectionHeight(),
                menu.heat(), menu.afterglow() > 0, now);
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            int x = x0 + slot.x, y = y0 + slot.y;
            int index = menu.crucibleIndex(slot);
            Palette p = index >= 0 ? top : slot.container == menu.slots.get(menu.barrelStart()).container ? BARREL : INVENTORY;
            slot(g, x, y, p);
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
                veil(g, x0 + l.barrelX(tier, i), y0 + l.barrelY(tier, i), BARREL);
            }
        }
        for (int i = 0; i < tier.slots(); i++) {
            if (menu.ghost(i).isEmpty() || menu.slots.get(i).hasItem()) continue;
            veil(g, x0 + l.slotX(tier, i), y0 + l.slotY(tier, i), top);
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

    /** A crucible slot's state behind its item: furnace-like fill or gap bar, green frame, grey veil, corner marks. */
    private void slotState(GuiGraphicsExtractor g, int slot, int x, int y, long now) {
        int state = menu.slotState(slot);
        int percent = state == CrucibleBlockEntity.BLOCKED ? 100 : menu.slotPercent(slot);
        if (state == CrucibleBlockEntity.COOKING || state == CrucibleBlockEntity.COLD || state == CrucibleBlockEntity.BLOCKED) {
            int level = Math.max(state == CrucibleBlockEntity.COOKING ? 1 : 0, percent * 16 / 100);
            if (PROGRESS_STYLE == ProgressStyle.FILL) {
                int[] colors = state == CrucibleBlockEntity.COOKING ? FILL_COOK : state == CrucibleBlockEntity.COLD ? FILL_COLD : FILL_BLOCKED;
                furnaceFill(g, x, y, level, colors, state == CrucibleBlockEntity.COOKING ? now : 0);
            } else {
                gapBar(g, x, y, level, state == CrucibleBlockEntity.COOKING ? COOK : state == CrucibleBlockEntity.COLD ? BLUE : RED);
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
                    gapBar(g, x, y, 16, GREEN);
                }
            }
            case CrucibleBlockEntity.NO_RECIPE -> g.fill(x, y + 1, x + 16, y + 15, GREY);
            default -> {}
        }
    }

    /**
     * Image 4's fuel slot: the slot fills {@code level} px (of 16) from below - body colour, darker flickering tongues
     * ({@code millis} 0 = still), a bright base line - inside the slot's rounded corners.
     */
    static void furnaceFill(GuiGraphicsExtractor g, int x, int y, int level, int[] colors, long millis) {
        if (level <= 0) return;
        int top = y + 16 - level;
        g.fill(x, Math.max(top, y + 1), x + 16, y + 15, colors[0]);
        if (top <= y) g.fill(x + 1, y, x + 15, y + 1, colors[0]);
        g.fill(x + 1, y + 15, x + 15, y + 16, colors[2]);
        int flick = (int) (millis / 160);
        for (int i = 0; i < 3; i++) {
            int tx = x + 2 + i * 5;
            int h = Math.min(level - 3, 4 + (CrucibleFlames.hash(i, flick) & 3));
            for (int k = 0; k < h; k++) {
                int dx = ((k + i + flick) & 2) == 0 ? 0 : 1;
                g.fill(tx + dx, y + 13 - k, tx + dx + 2, y + 14 - k, colors[1]);
            }
        }
    }

    /** Version 2: a 2 px bar in the gap right of the slot, filling {@code level} px (of 16) from below. */
    static void gapBar(GuiGraphicsExtractor g, int x, int y, int level, int color) {
        g.fill(x + 16, y, x + 18, y + 16, TRACK);
        if (level > 0) g.fill(x + 16, y + 16 - level, x + 18, y + 16, color);
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
        Palette top = palette(tier);
        g.text(font, title, titleLabelX, titleLabelY, top.label(), false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, INVENTORY.label(), false);
        if (tier.stackMultiplier() > 1) {
            CrucibleMenu.Layout l = layout();
            Component bonus = Component.translatable("gui.simplelib.crucible.stack_bonus", tier.stackMultiplier());
            g.text(font, bonus, l.x() + l.crucibleWidth() - 8 - font.width(bonus), titleLabelY, top.label(), false);
        }
    }

    /** Frame thickness of a box at the top and the sides, and at the bottom (with the shadow). */
    static final int FRAME = 5, FRAME_BOTTOM = 7;

    /** {@code color} with its RGB scaled by {@code f}. */
    static int scale(int color, double f) {
        int r = (int) Math.min(255, ((color >> 16) & 255) * f), gr = (int) Math.min(255, ((color >> 8) & 255) * f),
                b = (int) Math.min(255, (color & 255) * f);
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }

    /** A filled rectangle whose corner rows are shortened by {@code cuts[k]} px on both sides (k = 0 outermost row). */
    static void rounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int[] cuts, int color) {
        int n = cuts.length;
        for (int k = 0; k < n; k++) {
            g.fill(x + cuts[k], y + k, x + w - cuts[k], y + k + 1, color);
            g.fill(x + cuts[k], y + h - 1 - k, x + w - cuts[k], y + h - k, color);
        }
        g.fill(x, y + n, x + w, y + h - n, color);
    }

    private static final int[] CUT_OUTER = {3, 1, 1}, CUT_BEVEL = {2, 1}, CUT_INNER = {1}, CUT_NONE = {};

    /**
     * Image 3/4 box (measured on image 4): outline, bevel line (0.82 x fill), 2 px band (0.64 x fill), light inner line,
     * fill - 5 px frame; at the bottom 2 px shadow (0.40 x fill) between bevel and outline - 7 px; corners rounded by 2 px.
     */
    static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, Palette p) {
        rounded(g, x, y, w, h, CUT_OUTER, RIM);
        rounded(g, x + 1, y + 1, w - 2, h - 2, CUT_BEVEL, scale(p.fill(), 0.40));
        rounded(g, x + 1, y + 1, w - 2, h - 4, CUT_BEVEL, scale(p.fill(), 0.82));
        rounded(g, x + 2, y + 2, w - 4, h - 6, CUT_INNER, scale(p.fill(), 0.64));
        rounded(g, x + 4, y + 4, w - 8, h - 10, CUT_INNER, p.light());
        rounded(g, x + 5, y + 5, w - 10, h - 12, CUT_NONE, p.fill());
    }

    /**
     * Slot like image 3 (16x16, corners rounded by 1 px, 2 px apart) sunk in like image 4: dark top line, darker left
     * line, light edge below and right of it.
     */
    static void slot(GuiGraphicsExtractor g, int x, int y, Palette p) {
        g.fill(x + 1, y + 16, x + 16, y + 17, p.light());
        g.fill(x + 16, y + 1, x + 17, y + 16, p.light());
        g.fill(x + 1, y, x + 15, y + 16, p.slot());
        g.fill(x, y + 1, x + 1, y + 15, scale(p.slotTop(), 1.12));
        g.fill(x + 15, y + 1, x + 16, y + 15, p.slot());
        g.fill(x + 1, y, x + 15, y + 1, p.slotTop());
    }
}
