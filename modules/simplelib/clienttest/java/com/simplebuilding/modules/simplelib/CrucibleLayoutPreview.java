package com.simplebuilding.modules.simplelib;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Mockup of the enderite crucible window for the owner's layout choice (queue N34): the real menu is untouched (a
 * slot count change would need a data migration); this screen only draws the variants from flat colours with the
 * shared drawing blocks. Test-only, selected by the test via {@code -Dsimplelib.crucibleLayout=a|b|c}.
 * (a) 3x9 crucible and barrel side by side, inventory below; (b) 4 rows x 7 columns (28 slots) each;
 * (c) 5 rows x 6 columns (30 slots) each.
 */
final class CrucibleLayoutPreview extends Screen {
    private static final int MARGIN = 8, GRID_TOP = 18, FIRE = 12, GAP = 2, INV_BOX = 100;
    private final char variant;

    CrucibleLayoutPreview(char variant) {
        super(Component.literal("Enderite Crucible"));
        this.variant = variant;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        int rows = variant == 'a' ? 3 : variant == 'b' ? 4 : 5;
        int cols = variant == 'a' ? 9 : variant == 'b' ? 7 : 6;
        int boxW = Math.max(176, cols * 18 + 2 * MARGIN);
        int sectionH = GRID_TOP + rows * 18 + FIRE;
        int totalW = boxW * 2 + GAP, totalH = sectionH + GAP + INV_BOX;
        int x0 = (width - totalW) / 2, y0 = (height - totalH) / 2;
        UiBoxes.box(g, x0, y0, boxW, sectionH, UiPalette.ENDERITE);
        UiBoxes.box(g, x0 + boxW + GAP, y0, boxW, sectionH, UiPalette.BARREL);
        int invW = 176, invX = x0 + (totalW - invW) / 2, invY = y0 + sectionH + GAP;
        UiBoxes.box(g, invX, invY, invW, INV_BOX, UiPalette.INVENTORY);
        g.text(font, title, x0 + 8, y0 + 6, UiPalette.ENDERITE.label(), false);
        g.text(font, Component.literal("Barrel"), x0 + boxW + GAP + 8, y0 + 6, UiPalette.BARREL.label(), false);
        g.text(font, Component.literal("Inventory"), invX + 8, invY + 6, UiPalette.INVENTORY.label(), false);
        int n = 0;
        for (int box = 0; box < 2; box++) {
            int gx = x0 + box * (boxW + GAP) + (boxW - cols * 18) / 2 + 1, gy = y0 + GRID_TOP + 1;
            UiPalette p = box == 0 ? UiPalette.ENDERITE : UiPalette.BARREL;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    UiBoxes.slot(g, gx + c * 18, gy + r * 18, p);
                    if (n++ % 5 == 0) g.item(new ItemStack(box == 0 ? Items.RAW_IRON : Items.IRON_INGOT, 12), gx + c * 18, gy + r * 18);
                }
            }
        }
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 9; c++) {
                UiBoxes.slot(g, invX + (invW - 162) / 2 + 1 + c * 18, invY + 17 + r * 18 + (r == 3 ? 4 : 0), UiPalette.INVENTORY);
            }
        }
        g.text(font, Component.literal("Variant " + variant + ": " + rows + "x" + cols + " = " + rows * cols + " slots"), 4, 4, 0xFFFFFFFF, true);
    }
}
