package com.simplebuilding.modules.simpleinterfaces.client;

import com.simplebuilding.modules.simpleinterfaces.client.StyledScreens.Decor;
import com.simplebuilding.modules.simpleinterfaces.style.BoxLayout;
import com.simplebuilding.modules.simpleinterfaces.style.StorageStyles;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiPalette;
import com.simplelib.api.client.ui.UiSymbols;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.NonInteractiveResultSlot;
import net.minecraft.world.inventory.Slot;

/**
 * Screen-specific elements of group "storage" (W1 G1), as in the preview images g1-crafter.png and g1-pferd.png
 * (tools/ui/simpleinterfaces_preview.py: s_crafter, s_horse).
 */
public final class StorageDecors {
    /** Crafter: redstone dust sign (red when powered) and the engraved arrow, where Vanilla's redstone arrow was. */
    static final int REDSTONE_X = 86, REDSTONE_Y = 22, ARROW_X = 103, ARROW_Y = 36;
    /** Crafter result slot (CrafterMenu). */
    static final int RESULT_X = 134, RESULT_Y = 35;
    /** Powered redstone colour. */
    static final int REDSTONE_ON = 0xFFD8261E;
    /** Mount inventories: the field behind the live animal preview (AbstractMountInventoryScreen draws it at 26,18 52x52). */
    static final int PREVIEW_X = 26, PREVIEW_Y = 18, PREVIEW_SIZE = 52;
    /** Chest slots of SimpleBuilding's astral vault below the 27 ender chest slots. */
    static final int ENDER_SLOTS = 27;

    private static final List<BoxLayout.Rect> CRAFTER_ELEMENTS = List.of(
            new BoxLayout.Rect(RESULT_X - 4, RESULT_Y - 4, 25, 25),
            new BoxLayout.Rect(REDSTONE_X - 2, REDSTONE_Y, 16, 12),
            new BoxLayout.Rect(ARROW_X, ARROW_Y, UiSymbols.ARROW.width(), UiSymbols.ARROW.height() + 1));

    private static final Decor MOUNT = new Decor() {
        private final List<BoxLayout.Rect> elements = List.of(new BoxLayout.Rect(PREVIEW_X, PREVIEW_Y, PREVIEW_SIZE + 1, PREVIEW_SIZE + 1));

        @Override public List<BoxLayout.Rect> elements() {
            return elements;
        }

        @Override public void draw(GuiGraphicsExtractor g, int left, int top, UiPalette block) {
            UiBoxes.inset(g, left + PREVIEW_X, top + PREVIEW_Y, PREVIEW_SIZE, PREVIEW_SIZE, block);
        }
    };

    private static final Decor ASTRAL_VAULT = new Decor() {
        @Override public UiPalette slotPalette(Slot slot, UiPalette block) {
            return slot.index >= ENDER_SLOTS && slot.index < 6 * 9 ? StorageStyles.ASTRAL_SLOTS : block;
        }
    };

    private StorageDecors() {}

    /** Generic 9xN screens: SimpleBuilding's astral vault gets violet astral rows, everything else nothing extra. */
    public static Decor chest(AbstractContainerScreen<?> screen) {
        return screen.getMenu() instanceof ChestMenu menu && menu.getRowCount() == 6
                && screen.getTitle().getContents() instanceof TranslatableContents t && StorageStyles.ASTRAL_VAULT_TITLE.equals(t.getKey())
                ? ASTRAL_VAULT : Decor.NONE;
    }

    /** The crafter: big result slot, redstone sign from the menu's power state, engraved arrow. */
    public static Decor crafter(CrafterMenu menu) {
        return new Decor() {
            @Override public List<BoxLayout.Rect> elements() {
                return CRAFTER_ELEMENTS;
            }

            @Override public boolean bigSlot(Slot slot) {
                return slot instanceof NonInteractiveResultSlot;
            }

            @Override public void draw(GuiGraphicsExtractor g, int left, int top, UiPalette block) {
                if (menu.isPowered()) {
                    UiSymbols.engrave(g, UiSymbols.REDSTONE, left + REDSTONE_X, top + REDSTONE_Y, block, REDSTONE_ON, true);
                } else {
                    UiSymbols.engrave(g, UiSymbols.REDSTONE, left + REDSTONE_X, top + REDSTONE_Y, block);
                }
                UiSymbols.engrave(g, UiSymbols.ARROW, left + ARROW_X, top + ARROW_Y, block);
            }
        };
    }

    /** A disabled crafter slot: an engraved cross in the slot's top-line colour instead of Vanilla's red X (relative coords). */
    public static void disabledSlot(GuiGraphicsExtractor g, Slot slot, UiPalette block) {
        UiSymbols.engrave(g, UiSymbols.CROSS, slot.x + 4, slot.y + 4, block, block.slotTop(), false);
    }

    /** Mount inventories: the sunk field behind the animal. */
    public static Decor mount() {
        return MOUNT;
    }
}
