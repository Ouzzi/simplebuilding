package com.simplebuilding.items.custom;

/**
 * Die vier Rucksack-Stufen und ihre Slot-Geometrie.
 *
 * <p>Jede Stufe hat {@link #rows()} Reihen zu je 9 Slots und {@link #extraColumns()} Zusatzspalten.
 * Eine Zusatzspalte ist {@link #columnHeight()} = Reihen + 3 Slots hoch: sie steht neben den
 * Rucksack-Reihen <em>und</em> neben den drei Reihen des Vanilla-Hauptinventars (nicht neben der
 * Hotbar). Daraus folgen 9, 18, 33 und 50 Slots.
 *
 * <p><b>Zwei Nummerierungen.</b> Der Container eines geoeffneten Rucksacks ist dicht
 * nummeriert: erst alle Reihen-Slots, dann Spalte fuer Spalte von oben nach unten
 * ({@code containerIndex}). Im Item-Komponenten-Wert {@code simplebuilding:backpack_contents}
 * steht dagegen eine <em>stufenunabhaengige</em> Slot-Id ({@link #componentSlot(int)}): Reihen
 * belegen 0..35 (vier Reihen sind das Maximum), Spalten beginnen immer bei
 * {@link #COLUMN_SLOT_BASE} und bekommen je {@link #MAX_COLUMN_HEIGHT} Ids. So bleibt der Inhalt
 * der Netherit-Spalte bei der Aufwertung zum Enderit-Rucksack in der Spalte, statt in die neue
 * vierte Reihe zu rutschen.
 */
public enum BackpackTier {
    BASIC(1, 0, 1),
    REINFORCED(2, 0, 2),
    NETHERITE(3, 1, 3),
    ENDERITE(4, 2, 4);

    /** Groesste Reihenzahl aller Stufen; Reihen belegen die Komponenten-Ids 0 bis 9*MAX_ROWS-1. */
    public static final int MAX_ROWS = 4;
    /** Erste Komponenten-Id der Zusatzspalten. */
    public static final int COLUMN_SLOT_BASE = MAX_ROWS * 9;
    /** Hoechste Spaltenhoehe aller Stufen (Enderit: 4 Rucksack-Reihen + 3 Hauptinventar-Reihen). */
    public static final int MAX_COLUMN_HEIGHT = MAX_ROWS + 3;
    /** Anzahl Vanilla-Hauptinventar-Reihen, neben denen eine Zusatzspalte weiterlaeuft. */
    public static final int MAIN_INVENTORY_ROWS = 3;

    private final int rows;
    private final int extraColumns;
    private final int armor;

    BackpackTier(int rows, int extraColumns, int armor) {
        this.rows = rows;
        this.extraColumns = extraColumns;
        this.armor = armor;
    }

    public int rows() {
        return this.rows;
    }

    public int extraColumns() {
        return this.extraColumns;
    }

    /** Ruestungspunkte, die der getragene Rucksack im Brust-Slot gibt. */
    public int armor() {
        return this.armor;
    }

    public int columnHeight() {
        return this.rows + MAIN_INVENTORY_ROWS;
    }

    public int rowSlots() {
        return this.rows * 9;
    }

    /** Alle Slots dieser Stufe: 9, 18, 33 oder 50. */
    public int slotCount() {
        return rowSlots() + this.extraColumns * columnHeight();
    }

    public boolean isColumnSlot(int containerIndex) {
        return containerIndex >= rowSlots() && containerIndex < slotCount();
    }

    /** Spalte (0-basiert) eines Spalten-Slots; -1 fuer Reihen-Slots. */
    public int columnOf(int containerIndex) {
        return isColumnSlot(containerIndex) ? (containerIndex - rowSlots()) / columnHeight() : -1;
    }

    /** Stufenunabhaengige Komponenten-Id zu einem Container-Index dieser Stufe. */
    public int componentSlot(int containerIndex) {
        if (containerIndex < rowSlots()) {
            return containerIndex;
        }
        int offset = containerIndex - rowSlots();
        int column = offset / columnHeight();
        int row = offset % columnHeight();
        return COLUMN_SLOT_BASE + column * MAX_COLUMN_HEIGHT + row;
    }

    /** Container-Index zu einer Komponenten-Id, oder -1, wenn diese Stufe den Slot nicht hat. */
    public int containerIndex(int componentSlot) {
        if (componentSlot < 0) {
            return -1;
        }
        if (componentSlot < COLUMN_SLOT_BASE) {
            return componentSlot < rowSlots() ? componentSlot : -1;
        }
        int offset = componentSlot - COLUMN_SLOT_BASE;
        int column = offset / MAX_COLUMN_HEIGHT;
        int row = offset % MAX_COLUMN_HEIGHT;
        if (column >= this.extraColumns || row >= columnHeight()) {
            return -1;
        }
        return rowSlots() + column * columnHeight() + row;
    }

    public static BackpackTier byId(int id) {
        BackpackTier[] values = values();
        return id >= 0 && id < values.length ? values[id] : BASIC;
    }
}
