package com.simplestorage.client;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiMotifs;
import com.simplelib.api.client.ui.UiPalette;
import com.simplelib.api.client.ui.UiStyleToggle;
import com.simplestorage.chest.ChestTier;
import com.simplestorage.chest.TieredChestMenu;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Der Bildschirm der Mod-Truhen. Ein Vanilla-Truhenfenster in beliebiger Breite, aus Flaechen
 * gezeichnet (Vanillas Farben und Rand, abgerundete Ecken), damit 9 bis 18 Spalten ohne eigene
 * Hintergrund-Textur je Groesse auskommen. Groesstes Fenster: Enderit-Doppeltruhe mit 338 x 222
 * Punkten - passt bei GUI-Skala 4 auf 1920x1080 (480 x 270 Punkte), ohne Blaettern.
 *
 * <p>Netherit- und Enderit-Plaetze tragen eine leichte Toenung (sie fassen doppelte bzw.
 * vierfache Stapel), rechts in der Titelzeile steht der Faktor.
 *
 * <p>Im Kasten-Stil von SimpleLib ({@code com.simplelib.api.client.ui}, Palette und Motiv je Stufe), wie SimpleBuilding ihn
 * bis Stufe 8b zeichnete; ist der Stil aus ({@link UiStyleToggle}), bleiben die Flaechen hier.
 */
public class TieredChestScreen extends AbstractContainerScreen<TieredChestMenu> {
    private static final int BACKGROUND = 0xFFC6C6C6;
    private static final int OUTLINE = 0xFF000000;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SHADE = 0xFF555555;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    /** Toenung der uebergrossen Plaetze: Netherit leicht warm, Enderit leicht violett. */
    private static final int TINT_NETHERITE = 0x28A0602A;
    private static final int TINT_ENDERITE = 0x306A3FC8;

    public TieredChestScreen(TieredChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, displayTitle(menu, title), menu.imageWidth(), menu.imageHeight());
        this.inventoryLabelX = menu.inventoryLeft();
        this.inventoryLabelY = this.imageHeight - 94;
    }

    private static Component displayTitle(TieredChestMenu menu, Component title) {
        if (menu.isShulkerBox()) return title;
        Component block = Component.translatable("block.simplestorage." + menu.tier().textureName() + "_chest");
        return menu.isDouble() ? Component.translatable("container.simplestorage.double_chest", block) : block;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        if (styled(graphics, this.leftPos, this.topPos)) {
            return;
        }
        panel(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        int tint = tint(this.menu.tier());
        for (Slot slot : this.menu.slots) {
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            slot(graphics, x, y);
            if (tint != 0 && slot.index < this.menu.chestSlotCount()) {
                graphics.fill(x, y, x + 16, y + 16, tint);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        if (UiStyleToggle.isEnabled()) {
            // Only the title in the box's label colour: no stack factor, no inventory label (owner N23).
            graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, palette(this.menu.tier()).label(), false);
            return;
        }
        super.extractLabels(graphics, xm, ym);
        // Owner 2026-10-08: no stack factor in the menu (the item tooltip still names it).
    }

    /** Container box in the tier's palette with its motif, the light inventory box, the slots. */
    private boolean styled(GuiGraphicsExtractor g, int left, int top) {
        if (!UiStyleToggle.isEnabled()) return false;
        UiPalette p = palette(this.menu.tier());
        int invX = Integer.MAX_VALUE, invY = Integer.MAX_VALUE;
        for (Slot slot : this.menu.slots) {
            if (!(slot.container instanceof Inventory) || slot.getContainerSlot() < 9 || slot.getContainerSlot() >= 36) continue;
            invX = Math.min(invX, slot.x);
            invY = Math.min(invY, slot.y);
        }
        int containerHeight = invY - UiBoxes.FRAME - INVENTORY_GAP;
        UiBoxes.box(g, left, top, this.imageWidth, containerHeight, p);
        UiBoxes.box(g, left + invX - 8, top + invY - UiBoxes.FRAME, 176, INVENTORY_HEIGHT, UiPalette.INVENTORY);
        List<int[]> avoid = new ArrayList<>();
        for (Slot slot : this.menu.slots) {
            if (!slot.isActive() || slot.container instanceof Inventory) continue;
            avoid.add(new int[] {left + slot.x, top + slot.y, left + slot.x + 17, top + slot.y + 17});
        }
        avoid.add(new int[] {left + 4, top + 4, left + 12 + this.font.width(this.title), top + 15});
        UiMotifs.draw(g, 0, 0, motif(this.menu.tier()), left + UiBoxes.FRAME, top + UiBoxes.FRAME, this.imageWidth - 2 * UiBoxes.FRAME,
                containerHeight - UiBoxes.FRAME - UiBoxes.FRAME_BOTTOM, p, avoid, 3);
        for (Slot slot : this.menu.slots) {
            if (slot.isActive()) UiBoxes.slot(g, left + slot.x, top + slot.y, slot.container instanceof Inventory ? UiPalette.INVENTORY : p);
        }
        return true;
    }

    private static final int INVENTORY_GAP = 2;
    private static final int INVENTORY_HEIGHT = UiBoxes.FRAME + 58 + 17 + UiBoxes.FRAME_BOTTOM;

    static UiPalette palette(ChestTier tier) {
        return switch (tier) {
            case REINFORCED -> UiPalette.REINFORCED;
            case NETHERITE -> UiPalette.NETHERITE;
            case ENDERITE -> UiPalette.ENDERITE;
        };
    }

    static UiMotifs.Kind motif(ChestTier tier) {
        return switch (tier) {
            case REINFORCED -> UiMotifs.Kind.METAL;
            case NETHERITE -> UiMotifs.Kind.NETHER;
            case ENDERITE -> UiMotifs.Kind.ENDER;
        };
    }

    /** Toenung der Truhenplaetze je Stufe (0 = keine). */
    public static int tint(ChestTier tier) {
        return switch (tier) {
            case NETHERITE -> TINT_NETHERITE;
            case ENDERITE -> TINT_ENDERITE;
            default -> 0;
        };
    }

    /** Vanillas Fensterrahmen: schwarze Kontur mit runden Ecken, heller Rand oben links, dunkler unten rechts. */
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

    /** Ein Vanilla-Platz (18x18 um die 16x16 grosse Itemflaeche). */
    static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_FILL);
        g.fill(x - 1, y - 1, x + 16, y, SLOT_DARK);
        g.fill(x - 1, y - 1, x, y + 16, SLOT_DARK);
        g.fill(x, y + 16, x + 17, y + 17, LIGHT);
        g.fill(x + 16, y, x + 17, y + 17, LIGHT);
    }
}
