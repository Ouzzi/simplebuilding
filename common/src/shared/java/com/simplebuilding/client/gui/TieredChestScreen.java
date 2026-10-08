package com.simplebuilding.client.gui;

import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.screen.TieredChestMenu;
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
        super(menu, inventory, title, menu.imageWidth(), menu.imageHeight());
        this.inventoryLabelX = menu.inventoryLeft();
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
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
        super.extractLabels(graphics, xm, ym);
        // Owner 2026-10-08: no stack factor in the menu (the item tooltip still names it).
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
