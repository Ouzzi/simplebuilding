package com.simplebuilding.screen;

import com.simplebuilding.items.custom.BackpackTier;

/**
 * Die Geometrie des Rucksack-Bildschirms, ohne Client-Klassen, damit Menue (Server) und
 * Bildschirm (Client) dieselben Zahlen benutzen.
 *
 * <p>Der Vanilla-Teil ist das Spielerinventar 1:1 ({@code InventoryMenu}: Ergebnis 154/28,
 * 2x2-Raster 98/18, Ruestung 8/8.., Nebenhand 77/62). Die Rucksack-Reihen liegen direkt unter dem
 * oberen Bereich (y = 84 + 18r), das Hauptinventar folgt darunter und die Hotbar mit Vanillas
 * 4-Pixel-Luecke. Zusatzspalten stehen neben Rucksack-Reihen und Hauptinventar (nicht neben der
 * Hotbar): die erste rechts vom Raster, die zweite (nur Enderit) links davon. Hat eine Stufe eine
 * linke Spalte, rueckt der ganze Vanilla-Teil um 18 Pixel nach rechts ({@link #vanillaX()}).
 */
public final class BackpackLayout {
    public static final int VANILLA_WIDTH = 176;
    public static final int VANILLA_HEIGHT = 166;
    public static final int SLOT = 18;
    /** Obere Kante der ersten Slot-Reihe unter dem Crafting-/Ruestungsbereich. */
    public static final int FIRST_ROW_Y = 84;

    private final BackpackTier tier;

    public BackpackLayout(BackpackTier tier) {
        this.tier = tier;
    }

    public BackpackTier tier() {
        return this.tier;
    }

    public boolean hasLeftColumn() {
        return this.tier.extraColumns() >= 2;
    }

    /** X-Versatz des Vanilla-Teils im Bild. */
    public int vanillaX() {
        return hasLeftColumn() ? SLOT : 0;
    }

    public int imageWidth() {
        return VANILLA_WIDTH + SLOT * this.tier.extraColumns();
    }

    public int imageHeight() {
        return VANILLA_HEIGHT + SLOT * this.tier.rows();
    }

    /** Y der Rucksack-Reihe {@code row}. */
    public int backpackRowY(int row) {
        return FIRST_ROW_Y + SLOT * row;
    }

    /** Y der Hauptinventar-Reihe {@code row} (0..2). */
    public int mainRowY(int row) {
        return FIRST_ROW_Y + SLOT * (this.tier.rows() + row);
    }

    public int hotbarY() {
        return mainRowY(2) + SLOT + 4;
    }

    public int gridX(int column) {
        return vanillaX() + 8 + SLOT * column;
    }

    /** X der Zusatzspalte {@code column}: 0 rechts vom Raster, 1 links davon. */
    public int extraColumnX(int column) {
        return column == 0 ? vanillaX() + 170 : vanillaX() - 10;
    }

    /** Y des Slots {@code index} (0 = oben) einer Zusatzspalte. */
    public int extraColumnY(int index) {
        return FIRST_ROW_Y + SLOT * index;
    }

    /** X eines Rucksack-Container-Slots. */
    public int backpackSlotX(int containerIndex) {
        if (this.tier.isColumnSlot(containerIndex)) {
            return extraColumnX(this.tier.columnOf(containerIndex));
        }
        return gridX(containerIndex % 9);
    }

    /** Y eines Rucksack-Container-Slots. */
    public int backpackSlotY(int containerIndex) {
        if (this.tier.isColumnSlot(containerIndex)) {
            int offset = containerIndex - this.tier.rowSlots();
            return extraColumnY(offset % this.tier.columnHeight());
        }
        return backpackRowY(containerIndex / 9);
    }
}
